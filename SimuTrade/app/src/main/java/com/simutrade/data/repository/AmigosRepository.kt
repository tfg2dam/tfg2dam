package com.simutrade.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.SolicitudAmistad
import kotlinx.coroutines.tasks.await

class AmigosRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid

    companion object {
        const val USUARIOS = "Usuarios"
        const val AMIGOS = "amigos"
        const val SOLICITUDES = "solicitudes"
        private const val TAG = "AmigosRepository"
    }

    // Buscar usuario por codigo
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
                    codigoUsuario = doc.getString("codigo_usuario") ?: "",
                    idRango = doc.getString("id_rango") ?: "bronce"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscarPorCodigo", e)
            null
        }
    }

    // Enviar solicitud de amistad
    suspend fun enviarSolicitud(amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""
            val miCodigo = miDoc.getString("codigo_usuario") ?: ""

            firestore.collection(USUARIOS)
                .document(amigoUid)
                .collection(SOLICITUDES)
                .document(miUid)
                .set(mapOf(
                    "uid"          to miUid,
                    "nombre_usuario" to miNombre,
                    "codigo_usuario" to miCodigo,
                    "enviado_en"   to System.currentTimeMillis()
                )).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviarSolicitud", e)
            false
        }
    }

    // Aceptar solicitud
    suspend fun aceptarSolicitud(solicitanteUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miNombre = miDoc.getString("nombre_usuario") ?: ""
            val miCodigo = miDoc.getString("codigo_usuario") ?: ""
            val miRango = miDoc.getString("id_rango") ?: "bronce"

            val amigoDoc = firestore.collection(USUARIOS).document(solicitanteUid).get().await()
            val amigoNombre = amigoDoc.getString("nombre_usuario") ?: ""
            val amigoCodigo = amigoDoc.getString("codigo_usuario") ?: ""
            val amigoRango = amigoDoc.getString("id_rango") ?: "bronce"

            // Añadir a mi lista de amigos
            firestore.collection(USUARIOS).document(miUid)
                .collection(AMIGOS).document(solicitanteUid)
                .set(mapOf(
                    "uid"            to solicitanteUid,
                    "nombre_usuario" to amigoNombre,
                    "codigo_usuario" to amigoCodigo,
                    "id_rango"       to amigoRango
                )).await()

            // Añadir a la lista del solicitante
            firestore.collection(USUARIOS).document(solicitanteUid)
                .collection(AMIGOS).document(miUid)
                .set(mapOf(
                    "uid"            to miUid,
                    "nombre_usuario" to miNombre,
                    "codigo_usuario" to miCodigo,
                    "id_rango"       to miRango
                )).await()

            // Eliminar la solicitud
            rechazarSolicitud(solicitanteUid)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error aceptarSolicitud", e)
            false
        }
    }

    // Rechazar o cancelar solicitud
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

    // Eliminar amigo
    suspend fun eliminarAmigo(amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            firestore.collection(USUARIOS).document(miUid)
                .collection(AMIGOS).document(amigoUid)
                .delete().await()

            firestore.collection(USUARIOS).document(amigoUid)
                .collection(AMIGOS).document(miUid)
                .delete().await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminarAmigo", e)
            false
        }
    }

    // Obtener lista de amigos
    suspend fun getAmigos(): List<Amigo> {
        val miUid = uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USUARIOS).document(miUid)
                .collection(AMIGOS).get().await()

            snapshot.documents.map { doc ->
                Amigo(
                    uid          = doc.getString("uid") ?: "",
                    nombreUsuario= doc.getString("nombre_usuario") ?: "",
                    codigoUsuario= doc.getString("codigo_usuario") ?: "",
                    idRango      = doc.getString("id_rango") ?: "bronce"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getAmigos", e)
            emptyList()
        }
    }

    // Obtener solicitudes recibidas
    suspend fun getSolicitudes(): List<SolicitudAmistad> {
        val miUid = uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection(USUARIOS).document(miUid)
                .collection(SOLICITUDES).get().await()

            snapshot.documents.map { doc ->
                SolicitudAmistad(
                    uid          = doc.getString("uid") ?: "",
                    nombreUsuario= doc.getString("nombre_usuario") ?: "",
                    codigoUsuario= doc.getString("codigo_usuario") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getSolicitudes", e)
            emptyList()
        }
    }

    // Ranking de amigos
    suspend fun getRankingAmigos(): List<EntradaRanking> {
        val miUid = uid ?: return emptyList()
        return try {
            val amigos = getAmigos()
            val resultado = mutableListOf<EntradaRanking>()

            val miDoc = firestore.collection(USUARIOS).document(miUid).get().await()
            val miSaldoInicial = miDoc.getDouble("saldo_inicial") ?: 100.0
            val miBeneficio = miDoc.getDouble("beneficio") ?: 0.0

            resultado.add(
                EntradaRanking(
                    id           = miUid,
                    nombreUsuario= miDoc.getString("nombre_usuario") ?: "",
                    beneficio    = miBeneficio,
                    valorTotal   = miSaldoInicial + miBeneficio,
                    valorCartera = miDoc.getDouble("valor_cartera") ?: 0.0,
                    saldo        = miDoc.getDouble("saldo") ?: 0.0
                )
            )

            amigos.forEach { amigo ->
                val doc = firestore.collection(USUARIOS).document(amigo.uid).get().await()
                val saldoInicial = doc.getDouble("saldo_inicial") ?: 100.0
                val beneficio = doc.getDouble("beneficio") ?: 0.0

                resultado.add(
                    EntradaRanking(
                        id           = amigo.uid,
                        nombreUsuario= amigo.nombreUsuario,
                        beneficio    = beneficio,
                        valorTotal   = saldoInicial + beneficio,
                        valorCartera = doc.getDouble("valor_cartera") ?: 0.0,
                        saldo        = doc.getDouble("saldo") ?: 0.0
                    )
                )
            }

            resultado.sortedByDescending { it.beneficio }

        } catch (e: Exception) {
            Log.e(TAG, "Error getRankingAmigos", e)
            emptyList()
        }
    }

    // Comprobar si ya son amigos
    suspend fun esAmigo(amigoUid: String): Boolean {
        val miUid = uid ?: return false
        return try {
            val doc = firestore.collection(USUARIOS).document(miUid)
                .collection(AMIGOS).document(amigoUid).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Comprobar si ya hay solicitud pendiente
    suspend fun tieneSolicitudPendiente(amigoUid: String): Boolean {
        return try {
            val doc = firestore.collection(USUARIOS).document(amigoUid)
                .collection(SOLICITUDES).document(uid ?: return false).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}