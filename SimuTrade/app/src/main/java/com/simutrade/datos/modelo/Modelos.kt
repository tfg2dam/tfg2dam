package com.simutrade.datos.modelo

import com.google.firebase.auth.FirebaseUser

// ================= ACTIVOS =================

// Representa un activo del mercado (acción o cripto)
data class Activo(
    val id: String,
    val simbolo: String,
    val nombre: String,
    val tipo: TipoActivo,
    val precioActual: Double,
    val cambioPrecio24h: Double,
    val cambioPorcentaje24h: Double
) {
    val esCripto: Boolean get() = tipo == TipoActivo.CRIPTO
    val esAccion: Boolean get() = tipo == TipoActivo.ACCION
}

// Tipos de activo disponibles
enum class TipoActivo { ACCION, CRIPTO }

// ================= CARTERA =================

// Activo que el usuario tiene en su cartera
data class ActivoEnCartera(
    val idActivo: String,
    val simbolo: String,
    val nombre: String,
    val tipo: TipoActivo,
    val cantidad: Double,
    val precioPromedio: Double,
    val precioActual: Double
) {
    val valorActual: Double get() = cantidad * precioActual
    val valorInvertido: Double get() = cantidad * precioPromedio
    val beneficio: Double get() = valorActual - valorInvertido
    val porcentajeBeneficio: Double get() = if (valorInvertido > 0) (beneficio / valorInvertido) * 100 else 0.0
}

// ================= TRANSACCIONES =================

// Registro de una compra o venta
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

// Tipos de transacción posibles
enum class TipoTransaccion { COMPRA, VENTA }

// ================= USUARIO =================

// Datos del usuario autenticado
data class DatosUsuario(
    val idUsuario: String = "",
    val nombreUsuario: String = "",
    val email: String = "",
    val saldo: Double = 100.0,
    val saldoInicial: Double = 100.0,
    val saldoBonus: Double = 0.0,
    val creadoEn: Long = 0L,
    val ultimoLogin: Long = 0L,
    val codigoUsuario: String = ""
)

// ================= RANKING =================

// Nivel/rango del usuario según beneficio
data class Rango(
    val nombre: String,
    val beneficioMinimo: Double
)

// Entrada en la tabla de clasificación
data class EntradaRanking(
    val id: String = "",
    val nombreUsuario: String = "",
    val beneficio: Double = 0.0,
    val valorTotal: Double = 0.0,
    val valorCartera: Double = 0.0,
    val saldo: Double = 0.0
)

// ================= RETOS =================

// Estado de los retos y racha del usuario
data class DatosRetos(
    val rachaActual: Int = 0,
    val rachaMaxima: Int = 0,
    val ultimaVez: Long = 0L,
    val retosCompletados: List<String> = emptyList(),
    val retosDelDia: List<String> = emptyList(),
    val diaActual: Long = 0L
)

// Definición de un reto
data class Reto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val recompensa: Double
)

// ================= AMIGOS =================

// Amigo del usuario
data class Amigo(
    val uid: String = "",
    val nombreUsuario: String = "",
    val codigoUsuario: String = ""
)

// Solicitud de amistad pendiente
data class SolicitudAmistad(
    val uid: String = "",
    val nombreUsuario: String = "",
    val codigoUsuario: String = ""
)

// ================= LIGAS =================

// Estado de un miembro dentro de una liga
enum class EstadoMiembro { PENDIENTE, ACEPTADO }

// Liga de competición entre usuarios
data class Liga(
    val id: String = "",
    val nombre: String = "",
    val creadoPor: String = "",
    val creadoEn: Long = 0L,
    val miembros: List<MiembroLiga> = emptyList()
)

// Miembro dentro de una liga
data class MiembroLiga(
    val uid: String = "",
    val nombreUsuario: String = "",
    val codigoUsuario: String = "",
    val estado: EstadoMiembro = EstadoMiembro.PENDIENTE,
    val invitadoPor: String = ""
)

// Invitación a una liga recibida por el usuario
data class InvitacionLiga(
    val ligaId: String = "",
    val nombreLiga: String = "",
    val invitadoPor: String = "",
    val creadoEn: Long = 0L
)

// ================= RESULTADOS =================

// Resultado genérico de una operación
sealed class ResultadoOperacion {
    data class Exito(val mensaje: String) : ResultadoOperacion()
    data class Error(val mensaje: String) : ResultadoOperacion()
}

// Resultado específico del login y registro
sealed class ResultadoAutenticacion {
    data class Exito(val usuario: FirebaseUser) : ResultadoAutenticacion()
    data class Error(val mensaje: String) : ResultadoAutenticacion()
}