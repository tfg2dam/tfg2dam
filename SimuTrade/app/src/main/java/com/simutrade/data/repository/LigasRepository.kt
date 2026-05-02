package com.simutrade.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.EstadoMiembro
import com.simutrade.data.model.InvitacionLiga
import com.simutrade.data.model.Liga
import com.simutrade.data.model.MiembroLiga
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class LigasRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "LigasRepository"
        private const val USUARIOS = "Usuarios"
        private const val LIGAS = "Ligas"
        private const val MIEMBROS = "miembros"
        private const val INVITACIONES = "invitacionesLiga"
    }

    // ================= CREAR =================

    suspend fun crearLiga(nombre: String): String? {
        val miUid = uid ?: return null
        return try {
            val miDoc = firestore.collection(USUARIOS)
                .document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""
            val miCodigo = miDoc.getString("codigo_usuario") ?: ""

            val ligaRef = firestore.collection(LIGAS).document()
            val ligaId = ligaRef.id

            val batch = firestore.batch()

            batch.set(ligaRef, mapOf(
                "nombre" to nombre,
                "creado_por" to miUid,
                "creado_en" to System.currentTimeMillis()
            ))

            batch.set(
                ligaRef.collection(MIEMBROS).document(miUid),
                mapOf(
                    "uid" to miUid,
                    "nombre_usuario" to miNombre,
                    "codigo_usuario" to miCodigo,
                    "estado" to "aceptado",
                    "invitado_por" to miUid
                )
            )

            batch.commit().await()

            firestore.collection(USUARIOS).document(miUid)
                .update("mis_ligas", FieldValue.arrayUnion(ligaId))
                .await()

            ligaId
        } catch (e: Exception) {
            Log.e(TAG, "Error crearLiga", e)
            null
        }
    }

    // ================= INVITAR =================

    suspend fun invitarAmigo(ligaId: String, amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS)
                .document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""

            val amigoDoc = firestore.collection(USUARIOS)
                .document(amigoUid).get().await()
            val amigoNombre = amigoDoc.getString("nombre_usuario") ?: ""
            val amigoCodigo = amigoDoc.getString("codigo_usuario") ?: ""

            val ligaDoc = firestore.collection(LIGAS)
                .document(ligaId).get().await()
            val nombreLiga = ligaDoc.getString("nombre") ?: ""

            val batch = firestore.batch()

            batch.set(
                firestore.collection(LIGAS).document(ligaId)
                    .collection(MIEMBROS).document(amigoUid),
                mapOf(
                    "uid" to amigoUid,
                    "nombre_usuario" to amigoNombre,
                    "codigo_usuario" to amigoCodigo,
                    "estado" to "pendiente",
                    "invitado_por" to miUid
                )
            )

            batch.set(
                firestore.collection(USUARIOS).document(amigoUid)
                    .collection(INVITACIONES).document(ligaId),
                mapOf(
                    "liga_id" to ligaId,
                    "nombre_liga" to nombreLiga,
                    "invitado_por" to miNombre,
                    "creado_en" to System.currentTimeMillis()
                )
            )

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error invitarAmigo", e)
            false
        }
    }

    // ================= ACEPTAR / RECHAZAR =================

    suspend fun aceptarInvitacion(ligaId: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val batch = firestore.batch()

            batch.update(
                firestore.collection(LIGAS).document(ligaId)
                    .collection(MIEMBROS).document(miUid),
                "estado", "aceptado"
            )

            batch.delete(
                firestore.collection(USUARIOS).document(miUid)
                    .collection(INVITACIONES).document(ligaId)
            )

            batch.commit().await()

            firestore.collection(USUARIOS).document(miUid)
                .update("mis_ligas", FieldValue.arrayUnion(ligaId))
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error aceptarInvitacion", e)
            false
        }
    }

    suspend fun rechazarInvitacion(ligaId: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val batch = firestore.batch()

            batch.delete(
                firestore.collection(LIGAS).document(ligaId)
                    .collection(MIEMBROS).document(miUid)
            )

            batch.delete(
                firestore.collection(USUARIOS).document(miUid)
                    .collection(INVITACIONES).document(ligaId)
            )

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error rechazarInvitacion", e)
            false
        }
    }

    // ================= SALIR =================

    suspend fun salirDeLiga(ligaId: String): Boolean {
        val miUid = uid ?: return false
        return try {
            firestore.collection(LIGAS).document(ligaId)
                .collection(MIEMBROS).document(miUid)
                .delete().await()

            firestore.collection(USUARIOS).document(miUid)
                .update("mis_ligas", FieldValue.arrayRemove(ligaId))
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error salirDeLiga", e)
            false
        }
    }

    // ================= OBTENER =================

    suspend fun obtenerMisLigas(): List<Liga> {
        val miUid = uid ?: return emptyList()
        return try {
            val miDoc = firestore.collection(USUARIOS)
                .document(miUid).get().await()

            val misLigaIds = (miDoc.get("mis_ligas") as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList()

            if (misLigaIds.isEmpty()) return emptyList()

            misLigaIds.map { ligaId ->
                val ligaDoc = firestore.collection(LIGAS)
                    .document(ligaId).get().await()
                val miembros = obtenerMiembrosLiga(ligaId)
                Liga(
                    id = ligaDoc.id,
                    nombre = ligaDoc.getString("nombre") ?: "",
                    creadoPor = ligaDoc.getString("creado_por") ?: "",
                    creadoEn = ligaDoc.getLong("creado_en") ?: 0L,
                    miembros = miembros
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtenerMisLigas", e)
            emptyList()
        }
    }

    // ✅ Devuelve TODOS los miembros — aceptados y pendientes
    // Así el filtro de invitar no muestra usuarios con invitación pendiente
    suspend fun obtenerMiembrosLiga(ligaId: String): List<MiembroLiga> {
        return try {
            val snapshot = firestore.collection(LIGAS)
                .document(ligaId)
                .collection(MIEMBROS)
                .get().await() // ✅ Sin whereEqualTo — devuelve todos

            snapshot.documents.map { doc ->
                MiembroLiga(
                    uid = doc.getString("uid") ?: "",
                    nombreUsuario = doc.getString("nombre_usuario") ?: "",
                    codigoUsuario = doc.getString("codigo_usuario") ?: "",
                    estado = when (doc.getString("estado")) {
                        "aceptado" -> EstadoMiembro.ACEPTADO
                        else -> EstadoMiembro.PENDIENTE
                    },
                    invitadoPor = doc.getString("invitado_por") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtenerMiembrosLiga", e)
            emptyList()
        }
    }

    suspend fun obtenerInvitaciones(): List<InvitacionLiga> {
        val miUid = uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USUARIOS)
                .document(miUid)
                .collection(INVITACIONES)
                .get().await()

            snapshot.documents.map { doc ->
                InvitacionLiga(
                    ligaId = doc.getString("liga_id") ?: "",
                    nombreLiga = doc.getString("nombre_liga") ?: "",
                    invitadoPor = doc.getString("invitado_por") ?: "",
                    creadoEn = doc.getLong("creado_en") ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtenerInvitaciones", e)
            emptyList()
        }
    }

    // ================= RANKING =================

    // ✅ El ranking solo incluye miembros aceptados — los pendientes no compiten
    suspend fun obtenerRankingLiga(ligaId: String): List<EntradaRanking> =
        coroutineScope {
            try {
                val miembros = obtenerMiembrosLiga(ligaId)
                    .filter { it.estado == EstadoMiembro.ACEPTADO }

                miembros.map { miembro ->
                    async {
                        val doc = firestore.collection(USUARIOS)
                            .document(miembro.uid).get().await()
                        val beneficio = doc.getDouble("beneficio") ?: 0.0
                        EntradaRanking(
                            id = miembro.uid,
                            nombreUsuario = miembro.nombreUsuario,
                            beneficio = beneficio,
                            valorTotal = (doc.getDouble("saldo") ?: 0.0) +
                                    (doc.getDouble("valor_cartera") ?: 0.0),
                            valorCartera = doc.getDouble("valor_cartera") ?: 0.0,
                            saldo = doc.getDouble("saldo") ?: 0.0
                        )
                    }
                }.awaitAll().sortedByDescending { it.beneficio }

            } catch (e: Exception) {
                Log.e(TAG, "Error obtenerRankingLiga", e)
                emptyList()
            }
        }
}