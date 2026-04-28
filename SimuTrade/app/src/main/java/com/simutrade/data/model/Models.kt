package com.simutrade.data.model

// ================= ACTIVOS =================

data class Activo(
    val id: String,
    val simbolo: String,
    val nombre: String,
    val tipo: TipoActivo,
    val precioActual: Double,
    val cambioPrecio24h: Double,
    val cambioPorcentaje24h: Double
)

enum class TipoActivo {
    ACCION,
    CRIPTO
}

// ================= CARTERA =================

data class ActivoEnCartera(
    val idActivo: String,
    val simbolo: String,
    val nombre: String,
    val tipo: TipoActivo,
    val cantidad: Double,
    val precioPromedio: Double,
    val precioActual: Double
)

// ================= TRANSACCIONES =================

data class Transaccion(
    val id: String,
    val fecha: Long,
    val tipo: TipoTransaccion,
    val idActivo: String,
    val simbolo: String,
    val cantidad: Double,
    val precio: Double,
    val total: Double
)

enum class TipoTransaccion {
    COMPRA,
    VENTA
}

// ================= USUARIO =================

data class DatosUsuario(
    val idUsuario: String = "",
    val nombreUsuario: String = "",
    val email: String = "",
    val saldo: Double = 100.0,
    val saldoInicial: Double = 100.0,
    val saldoBonus: Double = 0.0,
    val idRango: String = "bronce",
    val creadoEn: Long = 0L,
    val ultimoLogin: Long = 0L
)

// ================= RANKING =================

data class Rango(
    val nombre: String,
    val beneficioMinimo: Double,
    val color: String,
    val descripcion: String
)

data class EntradaRanking(
    val id: String = "",
    val nombreUsuario: String = "",
    val beneficio: Double = 0.0,
    val valorTotal: Double = 0.0,
    val valorCartera: Double = 0.0,
    val saldo: Double = 0.0
)

// ================= RETOS =================

data class DatosRetos(
    val rachaActual: Int = 0,
    val rachaMaxima: Int = 0,
    val ultimaVez: Long = 0L,
    val retosCompletados: List<String> = emptyList(),
    val retosDelDia: List<String> = emptyList(),
    val diaActual: Long = 0L
)

data class Reto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val recompensa: Double
)

// ================= RESULTADOS =================

sealed class ResultadoOperacion {

    data class Exito(
        val mensaje: String,
        val datosUsuario: DatosUsuario
    ) : ResultadoOperacion()

    data class Error(
        val mensaje: String
    ) : ResultadoOperacion()
}