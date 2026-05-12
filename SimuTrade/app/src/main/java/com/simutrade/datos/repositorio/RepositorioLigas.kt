package com.simutrade.datos.repositorio

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.simutrade.datos.modelo.MensajeChat
import com.simutrade.datos.modelo.EntradaRanking
import com.simutrade.datos.modelo.EstadoMiembro
import com.simutrade.datos.modelo.InvitacionLiga
import com.simutrade.datos.modelo.Liga
import com.simutrade.datos.modelo.MiembroLiga
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RepositorioLigas {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "RepositorioLigas"
        private const val USUARIOS = "Usuarios"
        private const val LIGAS = "Ligas"
        private const val MIEMBROS = "miembros"
        private const val INVITACIONES = "invitacionesLiga"
    }

    // ================= CREAR =================

    suspend fun crearLiga(nombre: String): String? {
        val miUid = uid ?: return null
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
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
                .update("mis_ligas", FieldValue.arrayUnion(ligaId)).await()

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
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""

            val amigoDoc = firestore.collection(USUARIOS).document(amigoUid).get().await()
            val amigoNombre = amigoDoc.getString("nombre_usuario") ?: ""
            val amigoCodigo = amigoDoc.getString("codigo_usuario") ?: ""

            val ligaDoc = firestore.collection(LIGAS).document(ligaId).get().await()
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
                .update("mis_ligas", FieldValue.arrayUnion(ligaId)).await()

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
            val ligaDoc = firestore.collection(LIGAS).document(ligaId).get().await()
            val creadoPor = ligaDoc.getString("creado_por") ?: ""
            val miembrosAceptados = obtenerMiembrosLiga(ligaId)
                .filter { it.estado == EstadoMiembro.ACEPTADO }

            val batch = firestore.batch()

            when {
                miembrosAceptados.size <= 1 -> {
                    miembrosAceptados.forEach { miembro ->
                        batch.delete(
                            firestore.collection(LIGAS).document(ligaId)
                                .collection(MIEMBROS).document(miembro.uid)
                        )
                    }
                    batch.delete(firestore.collection(LIGAS).document(ligaId))
                }

                creadoPor == miUid -> {
                    val nuevoCreador = miembrosAceptados.first { it.uid != miUid }
                    batch.update(
                        firestore.collection(LIGAS).document(ligaId),
                        "creado_por", nuevoCreador.uid
                    )
                    batch.delete(
                        firestore.collection(LIGAS).document(ligaId)
                            .collection(MIEMBROS).document(miUid)
                    )
                }

                else -> {
                    batch.delete(
                        firestore.collection(LIGAS).document(ligaId)
                            .collection(MIEMBROS).document(miUid)
                    )
                }
            }

            batch.commit().await()

            firestore.collection(USUARIOS).document(miUid)
                .update("mis_ligas", FieldValue.arrayRemove(ligaId)).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error salirDeLiga", e)
            false
        }
    }

    // ================= OBSERVAR EN TIEMPO REAL =================

    // Escucha las ligas del usuario en tiempo real
    fun observarMisLigas(): Flow<List<Liga>> = callbackFlow {
        val miUid = uid ?: run { trySend(emptyList()); close(); return@callbackFlow }

        val listener = firestore.collection(USUARIOS).document(miUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observarMisLigas", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val misLigaIds = (snapshot?.get("mis_ligas") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()

                if (misLigaIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Carga cada liga
                val ligas = mutableListOf<Liga>()
                var pendientes = misLigaIds.size
                misLigaIds.forEach { ligaId ->
                    firestore.collection(LIGAS).document(ligaId).get()
                        .addOnSuccessListener { ligaDoc ->
                            firestore.collection(LIGAS).document(ligaId)
                                .collection(MIEMBROS).get()
                                .addOnSuccessListener { miembrosSnap ->
                                    val miembros = miembrosSnap.documents.map { doc ->
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
                                    ligas.add(Liga(
                                        id = ligaDoc.id,
                                        nombre = ligaDoc.getString("nombre") ?: "",
                                        creadoPor = ligaDoc.getString("creado_por") ?: "",
                                        creadoEn = ligaDoc.getLong("creado_en") ?: 0L,
                                        miembros = miembros
                                    ))
                                    pendientes--
                                    if (pendientes == 0) trySend(ligas.toList())
                                }
                        }
                        .addOnFailureListener { pendientes--; if (pendientes == 0) trySend(ligas.toList()) }
                }
            }

        awaitClose { listener.remove() }
    }

    // Escucha las invitaciones del usuario en tiempo real
    fun observarInvitaciones(): Flow<List<InvitacionLiga>> = callbackFlow {
        val miUid = uid ?: run { trySend(emptyList()); close(); return@callbackFlow }

        val listener = firestore.collection(USUARIOS).document(miUid)
            .collection(INVITACIONES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observarInvitaciones", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitaciones = snapshot?.documents?.map { doc ->
                    InvitacionLiga(
                        ligaId = doc.getString("liga_id") ?: "",
                        nombreLiga = doc.getString("nombre_liga") ?: "",
                        invitadoPor = doc.getString("invitado_por") ?: "",
                        creadoEn = doc.getLong("creado_en") ?: 0L
                    )
                } ?: emptyList()
                trySend(invitaciones)
            }

        awaitClose { listener.remove() }
    }

    // ================= OBTENER =================

    suspend fun obtenerMisLigas(): List<Liga> {
        val miUid = uid ?: return emptyList()
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val misLigaIds = (miDoc.get("mis_ligas") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            if (misLigaIds.isEmpty()) return emptyList()

            misLigaIds.map { ligaId ->
                val ligaDoc = firestore.collection(LIGAS).document(ligaId).get().await()
                Liga(
                    id = ligaDoc.id,
                    nombre = ligaDoc.getString("nombre") ?: "",
                    creadoPor = ligaDoc.getString("creado_por") ?: "",
                    creadoEn = ligaDoc.getLong("creado_en") ?: 0L,
                    miembros = obtenerMiembrosLiga(ligaId)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtenerMisLigas", e)
            emptyList()
        }
    }

    suspend fun obtenerMiembrosLiga(ligaId: String): List<MiembroLiga> {
        return try {
            val snapshot = firestore.collection(LIGAS).document(ligaId)
                .collection(MIEMBROS).get().await()

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
            val snapshot = firestore.collection(USUARIOS).document(miUid)
                .collection(INVITACIONES).get().await()

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

    suspend fun obtenerRankingLiga(ligaId: String): List<EntradaRanking> = coroutineScope {
        try {
            val miembros = obtenerMiembrosLiga(ligaId)
                .filter { it.estado == EstadoMiembro.ACEPTADO }

            miembros.map { miembro ->
                async {
                    val doc = firestore.collection(USUARIOS).document(miembro.uid).get().await()
                    EntradaRanking(
                        id = miembro.uid,
                        nombreUsuario = miembro.nombreUsuario,
                        beneficio = doc.getDouble("beneficio") ?: 0.0,
                        valorTotal = (doc.getDouble("saldo") ?: 0.0) + (doc.getDouble("valor_cartera") ?: 0.0),
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

    // Enviar mensaje al chat de la liga
    suspend fun enviarMensaje(ligaId: String, texto: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""

            firestore.collection(LIGAS).document(ligaId)
                .collection("mensajes")
                .add(mapOf(
                    "uid"            to miUid,
                    "nombre_usuario" to miNombre,
                    "texto"          to texto,
                    "enviado_en"     to System.currentTimeMillis()
                )).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviarMensaje", e)
            false
        }
    }

    // Escuchar mensajes en tiempo real (devuelve la función para cancelar el listener)
    fun escucharMensajes(
        ligaId: String,
        onMensajes: (List<MensajeChat>) -> Unit
    ): () -> Unit {
        val listener = firestore.collection(LIGAS).document(ligaId)
            .collection("mensajes")
            .orderBy("enviado_en", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val mensajes = snapshot.documents.map { doc ->
                    MensajeChat(
                        id            = doc.id,
                        uid           = doc.getString("uid") ?: "",
                        nombreUsuario = doc.getString("nombre_usuario") ?: "",
                        texto         = doc.getString("texto") ?: "",
                        enviadoEn     = doc.getLong("enviado_en") ?: 0L
                    )
                }
                onMensajes(mensajes)
            }
        return { listener.remove() }
    }
}