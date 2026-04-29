package com.simutrade.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.InvitacionLiga
import com.simutrade.data.model.Liga
import com.simutrade.data.model.MiembroLiga
import kotlinx.coroutines.tasks.await

class LigasRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid

    companion object {
        const val USUARIOS = "Usuarios"
        const val LIGAS = "Ligas"
        const val MIEMBROS = "miembros"
        const val INVITACIONES = "invitacionesLiga"
        private const val TAG = "LigasRepository"
    }

    // Crear liga
    suspend fun crearLiga(nombre: String): String? {
        val miUid = uid ?: return null
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""
            val miCodigo = miDoc.getString("codigo_usuario") ?: ""

            val ligaRef = firestore.collection(LIGAS).document()
            val ligaId = ligaRef.id

            ligaRef.set(mapOf(
                "nombre"     to nombre,
                "creado_por" to miUid,
                "creado_en"  to System.currentTimeMillis()
            )).await()

            // El creador entra directamente como aceptado
            ligaRef.collection(MIEMBROS).document(miUid).set(mapOf(
                "uid"             to miUid,
                "nombre_usuario"  to miNombre,
                "codigo_usuario"  to miCodigo,
                "estado"          to "aceptado",
                "invitado_por"    to miUid
            )).await()

            ligaId
        } catch (e: Exception) {
            Log.e(TAG, "Error crearLiga", e)
            null
        }
    }

    // Invitar amigo a liga
    suspend fun invitarAmigo(ligaId: String, amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""

            val amigoDoc = firestore.collection(USUARIOS).document(amigoUid).get().await()
            val amigoNombre = amigoDoc.getString("nombre_usuario") ?: ""
            val amigoCodigo = amigoDoc.getString("codigo_usuario") ?: ""

            val ligaDoc = firestore.collection(LIGAS).document(ligaId).get().await()
            val nombreLiga = ligaDoc.getString("nombre") ?: ""

            // Añadir miembro pendiente en la liga
            firestore.collection(LIGAS).document(ligaId)
                .collection(MIEMBROS).document(amigoUid)
                .set(mapOf(
                    "uid"            to amigoUid,
                    "nombre_usuario" to amigoNombre,
                    "codigo_usuario" to amigoCodigo,
                    "estado"         to "pendiente",
                    "invitado_por"   to miUid
                )).await()

            // Guardar invitacion en el usuario invitado
            firestore.collection(USUARIOS).document(amigoUid)
                .collection(INVITACIONES).document(ligaId)
                .set(mapOf(
                    "liga_id"      to ligaId,
                    "nombre_liga"  to nombreLiga,
                    "invitado_por" to miNombre,
                    "creado_en"    to System.currentTimeMillis()
                )).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error invitarAmigo", e)
            false
        }
    }

    // Aceptar invitacion
    suspend fun aceptarInvitacion(ligaId: String): Boolean {
        val miUid = uid ?: return false
        return try {
            firestore.collection(LIGAS).document(ligaId)
                .collection(MIEMBROS).document(miUid)
                .update("estado", "aceptado").await()

            // Eliminar la invitacion
            firestore.collection(USUARIOS).document(miUid)
                .collection(INVITACIONES).document(ligaId)
                .delete().await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error aceptarInvitacion", e)
            false
        }
    }

    // Rechazar invitacion
    suspend fun rechazarInvitacion(ligaId: String): Boolean {
        val miUid = uid ?: return false
        return try {
            // Eliminar miembro pendiente de la liga
            firestore.collection(LIGAS).document(ligaId)
                .collection(MIEMBROS).document(miUid)
                .delete().await()

            // Eliminar la invitacion
            firestore.collection(USUARIOS).document(miUid)
                .collection(INVITACIONES).document(ligaId)
                .delete().await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error rechazarInvitacion", e)
            false
        }
    }

    // Salir de una liga
    suspend fun salirDeLiga(ligaId: String): Boolean {
        val miUid = uid ?: return false
        return try {
            firestore.collection(LIGAS).document(ligaId)
                .collection(MIEMBROS).document(miUid)
                .delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error salirDeLiga", e)
            false
        }
    }

    // Obtener mis ligas
    suspend fun getMisLigas(): List<Liga> {
        val miUid = uid ?: return emptyList()
        return try {
            // Buscar ligas donde soy miembro aceptado
            val snapshot = firestore.collection(LIGAS).get().await()

            val ligas = mutableListOf<Liga>()
            snapshot.documents.forEach { ligaDoc ->
                val miembroDoc = firestore.collection(LIGAS)
                    .document(ligaDoc.id)
                    .collection(MIEMBROS)
                    .document(miUid)
                    .get().await()

                if (miembroDoc.exists() && miembroDoc.getString("estado") == "aceptado") {
                    val miembros = getMiembrosLiga(ligaDoc.id)
                    ligas.add(
                        Liga(
                            id = ligaDoc.id,
                            nombre = ligaDoc.getString("nombre") ?: "",
                            creadoPor = ligaDoc.getString("creado_por") ?: "",
                            creadoEn = ligaDoc.getLong("creado_en") ?: 0L,
                            miembros = miembros
                        )
                    )
                }
            }
            ligas
        } catch (e: Exception) {
            Log.e(TAG, "Error getMisLigas", e)
            emptyList()
        }
    }

    // Obtener miembros aceptados de una liga
    suspend fun getMiembrosLiga(ligaId: String): List<MiembroLiga> {
        return try {
            val snapshot = firestore.collection(LIGAS)
                .document(ligaId)
                .collection(MIEMBROS)
                .whereEqualTo("estado", "aceptado")
                .get().await()

            snapshot.documents.map { doc ->
                MiembroLiga(
                    uid           = doc.getString("uid") ?: "",
                    nombreUsuario = doc.getString("nombre_usuario") ?: "",
                    codigoUsuario = doc.getString("codigo_usuario") ?: "",
                    estado        = doc.getString("estado") ?: "pendiente",
                    invitadoPor   = doc.getString("invitado_por") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getMiembrosLiga", e)
            emptyList()
        }
    }

    // Obtener invitaciones pendientes
    suspend fun getInvitaciones(): List<InvitacionLiga> {
        val miUid = uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USUARIOS)
                .document(miUid)
                .collection(INVITACIONES)
                .get().await()

            snapshot.documents.map { doc ->
                InvitacionLiga(
                    ligaId      = doc.getString("liga_id") ?: "",
                    nombreLiga  = doc.getString("nombre_liga") ?: "",
                    invitadoPor = doc.getString("invitado_por") ?: "",
                    creadoEn    = doc.getLong("creado_en") ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getInvitaciones", e)
            emptyList()
        }
    }

    // Ranking de una liga
    suspend fun getRankingLiga(ligaId: String): List<EntradaRanking> {
        return try {
            val miembros = getMiembrosLiga(ligaId)
            val resultado = mutableListOf<EntradaRanking>()

            miembros.forEach { miembro ->
                val doc = firestore.collection(USUARIOS)
                    .document(miembro.uid).get().await()
                val saldoInicial = doc.getDouble("saldo_inicial") ?: 100.0
                val beneficio = doc.getDouble("beneficio") ?: 0.0

                resultado.add(
                    EntradaRanking(
                        id           = miembro.uid,
                        nombreUsuario= miembro.nombreUsuario,
                        beneficio    = beneficio,
                        valorTotal   = saldoInicial + beneficio,
                        valorCartera = doc.getDouble("valor_cartera") ?: 0.0,
                        saldo        = doc.getDouble("saldo") ?: 0.0
                    )
                )
            }

            resultado.sortedByDescending { it.beneficio }
        } catch (e: Exception) {
            Log.e(TAG, "Error getRankingLiga", e)
            emptyList()
        }
    }
}