package com.simutrade.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.simutrade.data.model.ActivoEnCartera
import com.simutrade.data.model.DatosRetos
import com.simutrade.data.model.DatosUsuario
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.TipoActivo
import com.simutrade.data.model.TipoTransaccion
import com.simutrade.data.model.Transaccion
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class RepositorioUsuario {

    private val autenticacion = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val idUsuarioActual
        get() = autenticacion.currentUser?.uid

    companion object {
        private const val TAG = "RepositorioUsuario"

        // ================= COLECCIONES =================

        private const val USUARIOS = "Usuarios"
        private const val CARTERA = "cartera"
        private const val TRANSACCIONES = "transacciones"
        private const val RETOS = "retos"

        // ================= DOCUMENTOS =================

        private const val DOCUMENTO_INICIAL = "inicial"
        private const val DOCUMENTO_RETOS = "datos"
    }

    // ================= REFERENCIA USUARIO =================

    private fun referenciaUsuarioONulo() =
        idUsuarioActual?.let {
            firestore
                .collection(USUARIOS)
                .document(it)
        }

    // ================= UTILIDADES =================

    private fun redondear2(
        valor: Double
    ): Double {
        return round(valor * 100) / 100
    }

    // ================= USUARIO =================

    suspend fun obtenerDatosUsuario(): DatosUsuario {
        val referencia = referenciaUsuarioONulo()
            ?: return DatosUsuario()

        return try {
            val documento = referencia.get().await()

            DatosUsuario(
                idUsuario = documento.id,
                nombreUsuario = documento.getString("nombre_usuario") ?: "",
                email = documento.getString("email") ?: "",
                saldo = documento.getDouble("saldo") ?: 100.0,
                saldoInicial = documento.getDouble("saldo_inicial") ?: 100.0,
                saldoBonus = documento.getDouble("saldo_bonus") ?: 0.0,
                idRango = documento.getString("id_rango") ?: "bronce",
                creadoEn = documento.getLong("creado_en") ?: 0L,
                ultimoLogin = documento.getLong("ultimo_login") ?: 0L,
                codigoUsuario = documento.getString("codigo_usuario") ?: "",
            )

        } catch (e: Exception) {
            Log.e(TAG, "obtenerDatosUsuario", e)
            DatosUsuario()
        }
    }

    suspend fun actualizarSaldo(
        nuevoSaldo: Double
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            referencia.update(
                "saldo",
                redondear2(nuevoSaldo)
            ).await()

        } catch (e: Exception) {
            Log.e(TAG, "actualizarSaldo", e)
        }
    }

    suspend fun actualizarSaldoBonus(
        incremento: Double
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            val documento = referencia.get().await()

            val saldoActual =
                documento.getDouble("saldo") ?: 0.0

            val bonusActual =
                documento.getDouble("saldo_bonus") ?: 0.0

            referencia.update(
                mapOf(
                    "saldo" to redondear2(saldoActual + incremento),
                    "saldo_bonus" to redondear2(bonusActual + incremento)
                )
            ).await()

        } catch (e: Exception) {
            Log.e(TAG, "actualizarSaldoBonus", e)
        }
    }

    suspend fun actualizarEstadisticasUsuario(
        valorCartera: Double,
        beneficio: Double
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            referencia.update(
                mapOf(
                    "valor_cartera" to redondear2(valorCartera),
                    "beneficio" to redondear2(beneficio)
                )
            ).await()

        } catch (e: Exception) {
            Log.e(TAG, "actualizarEstadisticasUsuario", e)
        }
    }

    // ================= CARTERA =================

    suspend fun obtenerCartera(): List<ActivoEnCartera> {
        val referencia = referenciaUsuarioONulo()
            ?: return emptyList()

        return try {
            val snapshot = referencia
                .collection(CARTERA)
                .get()
                .await()

            snapshot.documents
                .filter { it.id != DOCUMENTO_INICIAL }
                .map { documento ->

                    ActivoEnCartera(
                        idActivo = documento.id,
                        simbolo = documento.getString("simbolo") ?: "",
                        nombre = documento.getString("nombre") ?: "",
                        tipo = TipoActivo.valueOf(
                            documento.getString("tipo") ?: "ACCION"
                        ),
                        cantidad = documento.getDouble("cantidad") ?: 0.0,
                        precioPromedio = documento.getDouble("precio_promedio") ?: 0.0,
                        precioActual = documento.getDouble("precio_actual") ?: 0.0
                    )
                }

        } catch (e: Exception) {
            Log.e(TAG, "obtenerCartera", e)
            emptyList()
        }
    }

    suspend fun guardarActivoEnCartera(
        activo: ActivoEnCartera
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            referencia
                .collection(CARTERA)
                .document(activo.idActivo)
                .set(
                    mapOf(
                        "simbolo" to activo.simbolo,
                        "nombre" to activo.nombre,
                        "tipo" to activo.tipo.name,
                        "cantidad" to activo.cantidad,
                        "precio_promedio" to activo.precioPromedio,
                        "precio_actual" to activo.precioActual,
                        "actualizado_en" to System.currentTimeMillis()
                    )
                )
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "guardarActivoEnCartera", e)
        }
    }

    suspend fun eliminarActivoDeCartera(
        idActivo: String
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            referencia
                .collection(CARTERA)
                .document(idActivo)
                .delete()
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "eliminarActivoDeCartera", e)
        }
    }

    // ================= TRANSACCIONES =================

    suspend fun obtenerTransacciones(): List<Transaccion> {
        val referencia = referenciaUsuarioONulo()
            ?: return emptyList()

        return try {
            val snapshot = referencia
                .collection(TRANSACCIONES)
                .orderBy(
                    "ejecutado_en",
                    Query.Direction.DESCENDING
                )
                .get()
                .await()

            snapshot.documents
                .filter { it.id != DOCUMENTO_INICIAL }
                .map { documento ->

                    Transaccion(
                        id = documento.id,
                        fecha = documento.getLong("ejecutado_en") ?: 0L,
                        tipo = TipoTransaccion.valueOf(
                            documento.getString("tipo") ?: "COMPRA"
                        ),
                        idActivo = documento.getString("id_activo") ?: "",
                        simbolo = documento.getString("simbolo") ?: "",
                        cantidad = documento.getDouble("cantidad") ?: 0.0,
                        precio = documento.getDouble("precio") ?: 0.0,
                        total = documento.getDouble("total") ?: 0.0
                    )
                }

        } catch (e: Exception) {
            Log.e(TAG, "obtenerTransacciones", e)
            emptyList()
        }
    }

    suspend fun guardarTransaccion(
        transaccion: Transaccion
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            referencia
                .collection(TRANSACCIONES)
                .add(
                    mapOf(
                        "id_activo" to transaccion.idActivo,
                        "simbolo" to transaccion.simbolo,
                        "tipo" to transaccion.tipo.name,
                        "cantidad" to transaccion.cantidad,
                        "precio" to transaccion.precio,
                        "total" to transaccion.total,
                        "ejecutado_en" to transaccion.fecha
                    )
                )
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "guardarTransaccion", e)
        }
    }

    // ================= RANKING =================

    suspend fun obtenerRanking(): List<EntradaRanking> {
        return try {
            val snapshot = firestore
                .collection(USUARIOS)
                .orderBy(
                    "beneficio",
                    Query.Direction.DESCENDING
                )
                .get()
                .await()

            snapshot.documents.mapNotNull { documento ->

                val nombreUsuario =
                    documento.getString("nombre_usuario")
                        ?: return@mapNotNull null

                val saldo =
                    documento.getDouble("saldo") ?: 0.0

                val saldoInicial =
                    documento.getDouble("saldo_inicial") ?: 100.0

                val snapshotCartera =
                    firestore
                        .collection(USUARIOS)
                        .document(documento.id)
                        .collection(CARTERA)
                        .get()
                        .await()

                val valorCartera =
                    snapshotCartera.documents
                        .filter { it.id != DOCUMENTO_INICIAL }
                        .sumOf { activo ->

                            val cantidad =
                                activo.getDouble("cantidad") ?: 0.0

                            val precioActual =
                                activo.getDouble("precio_actual") ?: 0.0

                            cantidad * precioActual
                        }

                val valorTotal =
                    saldo + valorCartera

                val beneficio =
                    valorTotal - saldoInicial

                EntradaRanking(
                    id = documento.id,
                    nombreUsuario = nombreUsuario,
                    beneficio = redondear2(beneficio),
                    valorTotal = redondear2(valorTotal),
                    valorCartera = redondear2(valorCartera),
                    saldo = redondear2(saldo)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "obtenerRanking", e)
            emptyList()
        }
    }

    // ================= RETOS =================

    suspend fun obtenerDatosRetos(): DatosRetos {
        val referencia = referenciaUsuarioONulo()
            ?: return DatosRetos()

        return try {
            val documento = referencia
                .collection(RETOS)
                .document(DOCUMENTO_RETOS)
                .get()
                .await()

            DatosRetos(
                rachaActual = documento.getLong("racha_actual")?.toInt() ?: 0,
                rachaMaxima = documento.getLong("racha_maxima")?.toInt() ?: 0,
                ultimaVez = documento.getLong("ultima_vez") ?: 0L,

                retosCompletados =
                    (documento.get("retos_completados") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList(),

                retosDelDia =
                    (documento.get("retos_del_dia") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList(),

                diaActual = documento.getLong("dia_actual") ?: 0L
            )

        } catch (e: Exception) {
            Log.e(TAG, "obtenerDatosRetos", e)
            DatosRetos()
        }
    }

    suspend fun guardarDatosRetos(
        datos: DatosRetos
    ) {
        val referencia = referenciaUsuarioONulo() ?: return

        try {
            referencia
                .collection(RETOS)
                .document(DOCUMENTO_RETOS)
                .set(
                    mapOf(
                        "racha_actual" to datos.rachaActual,
                        "racha_maxima" to datos.rachaMaxima,
                        "ultima_vez" to datos.ultimaVez,
                        "retos_completados" to datos.retosCompletados,
                        "retos_del_dia" to datos.retosDelDia,
                        "dia_actual" to datos.diaActual
                    )
                )
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "guardarDatosRetos", e)
        }
    }

    // ================= CÁLCULOS =================

    fun calcularValorCartera(
        cartera: List<ActivoEnCartera>
    ): Double {
        return cartera.sumOf {
            it.cantidad * it.precioActual
        }
    }

    fun calcularValorTotal(
        saldo: Double,
        valorCartera: Double
    ): Double {
        return saldo + valorCartera
    }

    fun calcularBeneficio(
        total: Double,
        inicial: Double
    ): Double {
        return total - inicial
    }
}