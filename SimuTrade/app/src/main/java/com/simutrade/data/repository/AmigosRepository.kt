package com.simutrade.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.SolicitudAmistad
import kotlinx.coroutines.tasks.await

class AmigosRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "AmigosRepository"
        private const val USUARIOS = "Usuarios"
        private const val AMIGOS = "amigos"
        private const val SOLICITUDES = "solicitudes"
    }

    // ================= BUSCAR =================

    suspend fun buscarPorCodigo(codigo: String): Amigo? {
        return try {
            val codigoLimpio = codigo.removePrefix("#").uppercase().trim()
            val snapshot = firestore.collection(USUARIOS)
                .whereEqualTo("codigo_usuario", codigoLimpio)
                .limit(1)
                .get().await()

            snapshot.documents.firstOrNull()?.let { doc ->
                Amigo(
                    uid = doc.id,
                    nombreUsuario = doc.getString("nombre_usuario") ?: "",
                    codigoUsuario = doc.getString("codigo_usuario") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscarPorCodigo", e)
            null
        }
    }

    // ================= SOLICITUDES =================

    suspend fun enviarSolicitud(amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS)
                .document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""
            val miCodigo = miDoc.getString("codigo_usuario") ?: ""

            firestore.collection(USUARIOS)
                .document(amigoUid)
                .collection(SOLICITUDES)
                .document(miUid)
                .set(mapOf(
                    "uid" to miUid,
                    "nombre_usuario" to miNombre,
                    "codigo_usuario" to miCodigo,
                    "enviado_en" to System.currentTimeMillis()
                )).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviarSolicitud", e)
            false
        }
    }

    suspend fun aceptarSolicitud(solicitanteUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS)
                .document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""
            val miCodigo = miDoc.getString("codigo_usuario") ?: ""

            val amigoDoc = firestore.collection(USUARIOS)
                .document(solicitanteUid).get().await()
            val amigoNombre = amigoDoc.getString("nombre_usuario") ?: ""
            val amigoCodigo = amigoDoc.getString("codigo_usuario") ?: ""

            val batch = firestore.batch()

            batch.set(
                firestore.collection(USUARIOS).document(miUid)
                    .collection(AMIGOS).document(solicitanteUid),
                mapOf(
                    "uid" to solicitanteUid,
                    "nombre_usuario" to amigoNombre,
                    "codigo_usuario" to amigoCodigo
                )
            )

            batch.set(
                firestore.collection(USUARIOS).document(solicitanteUid)
                    .collection(AMIGOS).document(miUid),
                mapOf(
                    "uid" to miUid,
                    "nombre_usuario" to miNombre,
                    "codigo_usuario" to miCodigo
                )
            )

            batch.delete(
                firestore.collection(USUARIOS).document(miUid)
                    .collection(SOLICITUDES).document(solicitanteUid)
            )

            batch.commit().await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error aceptarSolicitud", e)
            false
        }
    }

    suspend fun rechazarSolicitud(solicitanteUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            firestore.collection(USUARIOS).document(miUid)
                .collection(SOLICITUDES).document(solicitanteUid)
                .delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error rechazarSolicitud", e)
            false
        }
    }

    // ================= AMIGOS =================

    suspend fun eliminarAmigo(amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val batch = firestore.batch()

            batch.delete(
                firestore.collection(USUARIOS).document(miUid)
                    .collection(AMIGOS).document(amigoUid)
            )
            batch.delete(
                firestore.collection(USUARIOS).document(amigoUid)
                    .collection(AMIGOS).document(miUid)
            )

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminarAmigo", e)
            false
        }
    }

    suspend fun obtenerAmigos(): List<Amigo> {
        val miUid = uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USUARIOS).document(miUid)
                .collection(AMIGOS).get().await()

            snapshot.documents.map { doc ->
                Amigo(
                    uid = doc.getString("uid") ?: "",
                    nombreUsuario = doc.getString("nombre_usuario") ?: "",
                    codigoUsuario = doc.getString("codigo_usuario") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtenerAmigos", e)
            emptyList()
        }
    }

    suspend fun obtenerSolicitudes(): List<SolicitudAmistad> {
        val miUid = uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USUARIOS).document(miUid)
                .collection(SOLICITUDES).get().await()

            snapshot.documents.map { doc ->
                SolicitudAmistad(
                    uid = doc.getString("uid") ?: "",
                    nombreUsuario = doc.getString("nombre_usuario") ?: "",
                    codigoUsuario = doc.getString("codigo_usuario") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtenerSolicitudes", e)
            emptyList()
        }
    }

    // ================= COMPROBACIONES =================

    suspend fun esAmigo(amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val doc = firestore.collection(USUARIOS).document(miUid)
                .collection(AMIGOS).document(amigoUid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun tieneSolicitudPendiente(amigoUid: String): Boolean {
        return try {
            val doc = firestore.collection(USUARIOS).document(amigoUid)
                .collection(SOLICITUDES)
                .document(uid ?: return false).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }
}