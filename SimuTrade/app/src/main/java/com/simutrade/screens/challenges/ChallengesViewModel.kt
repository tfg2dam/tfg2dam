package com.simutrade.screens.challenges

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.DatosRetos
import com.simutrade.data.model.Reto
import com.simutrade.data.model.TipoActivo
import com.simutrade.data.repository.RepositorioUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class ValidacionReto(
    val completado: Boolean,
    val mensaje: String
)

class ChallengesViewModel : ViewModel() {

    private val repositorio = RepositorioUsuario()

    // ================= STATE =================

    data class UiState(
        val datosRetos: DatosRetos = DatosRetos(),
        val retosDelDia: List<Reto> = emptyList(),
        val cargando: Boolean = false,
        val milisegundosHastaReset: Long = 0L
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        cargarRetos()
    }

    // ================= CARGAR =================

    fun cargarRetos() {
        viewModelScope.launch {

            _uiState.update { it.copy(cargando = true) }

            try {
                val datosGuardados = repositorio.obtenerDatosRetos()

                val datosSeguros = if (datosGuardados.diaActual == 0L) {
                    val datosIniciales = datosGuardados.copy(
                        diaActual = obtenerInicioDelDia(),
                        retosDelDia = generarRetosAleatorios()
                    )
                    repositorio.guardarDatosRetos(datosIniciales)
                    datosIniciales
                } else {
                    datosGuardados
                }

                val datosActualizados = verificarResetDiario(datosSeguros)
                val retos = construirRetos(datosActualizados)

                _uiState.value = UiState(
                    datosRetos = datosActualizados,
                    retosDelDia = retos,
                    cargando = false,
                    milisegundosHastaReset = calcularTiempoReset()
                )

            } catch (e: Exception) {
                Log.e("ChallengesViewModel", "Error cargando retos", e)
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    // ================= RESET DIARIO =================

    private suspend fun verificarResetDiario(datos: DatosRetos): DatosRetos {
        val inicioHoy = obtenerInicioDelDia()

        if (datos.diaActual == inicioHoy) return datos

        // ✅ Fix DST — usar Calendar en vez de milisegundos fijos
        val ayer = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = inicioHoy
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        val nuevaRacha = if (
            datos.diaActual == ayer &&
            todosLosRetosCompletados(datos)
        ) {
            datos.rachaActual + 1
        } else {
            0
        }

        val datosReiniciados = datos.copy(
            rachaActual = nuevaRacha,
            rachaMaxima = maxOf(datos.rachaMaxima, nuevaRacha),
            retosCompletados = emptyList(),
            retosDelDia = generarRetosAleatorios(),
            diaActual = inicioHoy,
            ultimaVez = System.currentTimeMillis()
        )

        repositorio.guardarDatosRetos(datosReiniciados)
        return datosReiniciados
    }

    private fun obtenerInicioDelDia(): Long {
        return Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calcularTiempoReset(): Long {
        val ahora = Calendar.getInstance(TimeZone.getDefault())
        val manana = (ahora.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return manana.timeInMillis - ahora.timeInMillis
    }

    // ================= GENERADOR =================

    private fun generarRetosAleatorios(): List<String> {
        return listOf(
            "operacion", "diversificar", "trader", "multimercado", "beneficio"
        ).shuffled().take(3)
    }

    // ================= CONSTRUIR RETOS =================

    private fun construirRetos(datos: DatosRetos): List<Reto> {
        return datos.retosDelDia.mapIndexed { index, tipo ->
            val id = "reto_${tipo}_${datos.diaActual}_${index + 1}"
            when (tipo) {
                "operacion" -> Reto(
                    id = id,
                    titulo = "Haz tu primera operación",
                    descripcion = "Compra o vende cualquier activo hoy",
                    recompensa = 1.0
                )
                "diversificar" -> Reto(
                    id = id,
                    titulo = "Diversifica tu cartera",
                    descripcion = "Ten al menos 2 inversiones diferentes",
                    recompensa = 1.5
                )
                "multimercado" -> Reto(
                    id = id,
                    titulo = "Invierte en varios mercados",
                    descripcion = "Invierte en acciones y criptomonedas",
                    recompensa = 2.0
                )
                "trader" -> Reto(
                    id = id,
                    titulo = "Actividad alta",
                    descripcion = "Realiza 3 operaciones hoy",
                    recompensa = 2.5
                )
                "beneficio" -> Reto(
                    id = id,
                    titulo = "Consigue beneficios",
                    descripcion = "Haz que tu cartera esté en positivo",
                    recompensa = 3.0
                )
                else -> Reto(
                    id = "error",
                    titulo = "Error",
                    descripcion = "No se pudo cargar el reto",
                    recompensa = 0.0
                )
            }
        }
    }

    private fun todosLosRetosCompletados(datos: DatosRetos): Boolean {
        val idsRetos = datos.retosDelDia.mapIndexed { index, tipo ->
            "reto_${tipo}_${datos.diaActual}_${index + 1}"
        }
        return idsRetos.all { it in datos.retosCompletados }
    }

    // ================= VALIDACIÓN =================

    private suspend fun validarReto(id: String): ValidacionReto {
        val tipo = obtenerTipoDesdeId(id)
        val inicioHoy = obtenerInicioDelDia()

        return when (tipo) {

            "operacion" -> {
                val transacciones = repositorio.obtenerTransacciones()
                val hoy = transacciones.filter { it.fecha >= inicioHoy }
                if (hoy.isNotEmpty())
                    ValidacionReto(true, "Ya has hecho una operación hoy")
                else
                    ValidacionReto(false, "Haz una compra o una venta")
            }

            "trader" -> {
                val transacciones = repositorio.obtenerTransacciones()
                val hoy = transacciones.filter { it.fecha >= inicioHoy }
                val restantes = 3 - hoy.size
                if (hoy.size >= 3)
                    ValidacionReto(true, "Has completado las 3 operaciones")
                else
                    ValidacionReto(false, "Te faltan $restantes operaciones")
            }

            "diversificar" -> {
                val cartera = repositorio.obtenerCartera()
                val distintos = cartera.map { it.idActivo }.distinct().size
                if (distintos >= 2)
                    ValidacionReto(true, "Ya tienes varias inversiones")
                else
                    ValidacionReto(false, "Te falta ${2 - distintos} inversión más")
            }

            "multimercado" -> {
                val cartera = repositorio.obtenerCartera()
                val tieneAcciones = cartera.any { it.tipo == TipoActivo.ACCION }
                val tieneCripto = cartera.any { it.tipo == TipoActivo.CRIPTO }
                when {
                    tieneAcciones && tieneCripto ->
                        ValidacionReto(true, "Ya inviertes en ambos mercados")
                    !tieneAcciones && !tieneCripto ->
                        ValidacionReto(false, "Empieza invirtiendo en cualquier activo")
                    !tieneAcciones ->
                        ValidacionReto(false, "Te falta invertir en acciones")
                    else ->
                        ValidacionReto(false, "Te falta invertir en criptomonedas")
                }
            }

            "beneficio" -> {
                val cartera = repositorio.obtenerCartera()
                val usuario = repositorio.obtenerDatosUsuario()
                val valorCartera = cartera.sumOf { it.valorActual }
                val beneficio = (usuario.saldo + valorCartera) - usuario.saldoInicial
                if (beneficio > 0)
                    ValidacionReto(true, "Vas ganando €${"%.2f".format(beneficio)}")
                else
                    ValidacionReto(false, "Tu cartera aún no está en positivo")
            }

            else -> ValidacionReto(false, "Reto desconocido")
        }
    }

    private fun obtenerTipoDesdeId(id: String): String {
        return when {
            id.contains("operacion") -> "operacion"
            id.contains("diversificar") -> "diversificar"
            id.contains("trader") -> "trader"
            id.contains("multimercado") -> "multimercado"
            id.contains("beneficio") -> "beneficio"
            else -> ""
        }
    }

    // ================= COMPLETAR =================

    fun completarReto(
        id: String,
        recompensa: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {

            // ✅ Comprueba localmente primero — evita petición a Firestore
            if (id in _uiState.value.datosRetos.retosCompletados) {
                onResult(false, "Ya has completado este reto")
                return@launch
            }

            val validacion = validarReto(id)

            if (!validacion.completado) {
                onResult(false, validacion.mensaje)
                return@launch
            }

            repositorio.actualizarSaldoBonus(recompensa)

            val nuevosDatos = _uiState.value.datosRetos.copy(
                retosCompletados = _uiState.value.datosRetos.retosCompletados + id
            )

            repositorio.guardarDatosRetos(nuevosDatos)

            _uiState.update {
                it.copy(
                    datosRetos = nuevosDatos,
                    milisegundosHastaReset = calcularTiempoReset()
                )
            }

            onResult(true, "+${"%.2f".format(recompensa)}€ añadidos")
        }
    }
}