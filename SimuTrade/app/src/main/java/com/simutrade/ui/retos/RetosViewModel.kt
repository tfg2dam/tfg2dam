package com.simutrade.ui.retos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.datos.modelo.DatosRetos
import com.simutrade.datos.modelo.Reto
import com.simutrade.datos.modelo.TipoActivo
import com.simutrade.datos.repositorio.RepositorioUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

// Resultado de validar si un reto está completado
data class ValidacionReto(
    val completado: Boolean,
    val mensaje: String
)

class RetosViewModel : ViewModel() {

    private val repositorio = RepositorioUsuario()

    // ================= ESTADO =================

    data class EstadoUi(
        val datosRetos: DatosRetos = DatosRetos(),
        val retosDelDia: List<Reto> = emptyList(),
        val cargando: Boolean = false,
        val milisegundosHastaReset: Long = 0L
    )

    private val _estadoUi = MutableStateFlow(EstadoUi())
    val estadoUi: StateFlow<EstadoUi> = _estadoUi.asStateFlow()

    init { cargarRetos() }

    // ================= CARGAR =================

    // Carga los retos del día, inicializando si es la primera vez
    fun cargarRetos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true) }
            try {
                val datosGuardados = repositorio.obtenerDatosRetos()

                // Si no hay día guardado, inicializa los datos
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

                _estadoUi.value = EstadoUi(
                    datosRetos = datosActualizados,
                    retosDelDia = retos,
                    cargando = false,
                    milisegundosHastaReset = calcularTiempoReset()
                )
            } catch (e: Exception) {
                Log.e("RetosViewModel", "Error cargando retos", e)
                _estadoUi.update { it.copy(cargando = false) }
            }
        }
    }

    // ================= RESET DIARIO =================

    // Reinicia los retos si ha pasado el día, actualizando la racha
    private suspend fun verificarResetDiario(datos: DatosRetos): DatosRetos {
        val inicioHoy = obtenerInicioDelDia()
        if (datos.diaActual == inicioHoy) return datos

        // Calcula el inicio de ayer usando Calendar para evitar problemas con DST
        val ayer = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = inicioHoy
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        // Incrementa la racha solo si ayer se completaron todos los retos
        val nuevaRacha = if (datos.diaActual == ayer && todosLosRetosCompletados(datos)) {
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

    // Devuelve el timestamp del inicio del día actual
    private fun obtenerInicioDelDia(): Long {
        return Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Calcula los milisegundos que faltan para el reset de mañana
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

    // Selecciona 3 retos aleatorios del pool disponible
    private fun generarRetosAleatorios(): List<String> {
        return listOf("operacion", "diversificar", "trader", "multimercado", "beneficio")
            .shuffled().take(3)
    }

    // ================= CONSTRUIR RETOS =================

    // Convierte los IDs de retos en objetos Reto con título y recompensa
    private fun construirRetos(datos: DatosRetos): List<Reto> {
        return datos.retosDelDia.mapIndexed { index, tipo ->
            val id = "reto_${tipo}_${datos.diaActual}_${index + 1}"
            when (tipo) {
                "operacion" -> Reto(id = id, titulo = "Haz tu primera operación",
                    descripcion = "Compra o vende cualquier activo hoy", recompensa = 1.0)
                "diversificar" -> Reto(id = id, titulo = "Diversifica tu cartera",
                    descripcion = "Ten al menos 2 inversiones diferentes", recompensa = 1.5)
                "multimercado" -> Reto(id = id, titulo = "Invierte en varios mercados",
                    descripcion = "Invierte en acciones y criptomonedas", recompensa = 2.0)
                "trader" -> Reto(id = id, titulo = "Actividad alta",
                    descripcion = "Realiza 3 operaciones hoy", recompensa = 2.5)
                "beneficio" -> Reto(id = id, titulo = "Consigue beneficios",
                    descripcion = "Haz que tu cartera esté en positivo", recompensa = 3.0)
                else -> Reto(id = "error", titulo = "Error",
                    descripcion = "No se pudo cargar el reto", recompensa = 0.0)
            }
        }
    }

    // Comprueba si todos los retos del día están completados
    private fun todosLosRetosCompletados(datos: DatosRetos): Boolean {
        val idsRetos = datos.retosDelDia.mapIndexed { index, tipo ->
            "reto_${tipo}_${datos.diaActual}_${index + 1}"
        }
        return idsRetos.all { it in datos.retosCompletados }
    }

    // ================= VALIDACIÓN =================

    // Valida si el usuario cumple las condiciones del reto
    private suspend fun validarReto(id: String): ValidacionReto {
        val tipo = obtenerTipoDesdeId(id)
        val inicioHoy = obtenerInicioDelDia()

        return when (tipo) {
            "operacion" -> {
                val hoy = repositorio.obtenerTransacciones().filter { it.fecha >= inicioHoy }
                if (hoy.isNotEmpty()) ValidacionReto(true, "Ya has hecho una operación hoy")
                else ValidacionReto(false, "Haz una compra o una venta")
            }
            "trader" -> {
                val hoy = repositorio.obtenerTransacciones().filter { it.fecha >= inicioHoy }
                val restantes = 3 - hoy.size
                if (hoy.size >= 3) ValidacionReto(true, "Has completado las 3 operaciones")
                else ValidacionReto(false, "Te faltan $restantes operaciones")
            }
            "diversificar" -> {
                val cartera = repositorio.obtenerCartera()
                val distintos = cartera.map { it.idActivo }.distinct().size
                if (distintos >= 2) ValidacionReto(true, "Ya tienes varias inversiones")
                else ValidacionReto(false, "Te falta ${2 - distintos} inversión más")
            }
            "multimercado" -> {
                val cartera = repositorio.obtenerCartera()
                val tieneAcciones = cartera.any { it.tipo == TipoActivo.ACCION }
                val tieneCripto = cartera.any { it.tipo == TipoActivo.CRIPTO }
                when {
                    tieneAcciones && tieneCripto -> ValidacionReto(true, "Ya inviertes en ambos mercados")
                    !tieneAcciones && !tieneCripto -> ValidacionReto(false, "Empieza invirtiendo en cualquier activo")
                    !tieneAcciones -> ValidacionReto(false, "Te falta invertir en acciones")
                    else -> ValidacionReto(false, "Te falta invertir en criptomonedas")
                }
            }
            "beneficio" -> {
                val cartera = repositorio.obtenerCartera()
                val usuario = repositorio.obtenerDatosUsuario()
                val beneficio = (usuario.saldo - usuario.saldoBonus + cartera.sumOf { it.valorActual }) - usuario.saldoInicial
                if (beneficio > 0) ValidacionReto(true, "Vas ganando €${"%.2f".format(beneficio)}")
                else ValidacionReto(false, "Tu cartera aún no está en positivo")
            }
            else -> ValidacionReto(false, "Reto desconocido")
        }
    }

    // Extrae el tipo de reto desde su ID
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

    // Valida, otorga la recompensa y marca el reto como completado
    fun completarReto(id: String, recompensa: Double, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (id in _estadoUi.value.datosRetos.retosCompletados) {
                onResult(false, "Ya has completado este reto")
                return@launch
            }

            val validacion = validarReto(id)
            if (!validacion.completado) {
                onResult(false, validacion.mensaje)
                return@launch
            }

            repositorio.actualizarSaldoBonus(recompensa)

            val nuevosDatos = _estadoUi.value.datosRetos.copy(
                retosCompletados = _estadoUi.value.datosRetos.retosCompletados + id
            )

            // Comprobar si con este reto se completan todos
            val idsRetos = nuevosDatos.retosDelDia.mapIndexed { index, tipo ->
                "reto_${tipo}_${nuevosDatos.diaActual}_${index + 1}"
            }
            val todosCompletados = idsRetos.all { it in nuevosDatos.retosCompletados }

            // Si se completan todos, subir la racha inmediatamente
            val datosFinales = if (todosCompletados) {
                val nuevaRacha = nuevosDatos.rachaActual + 1
                nuevosDatos.copy(
                    rachaActual = nuevaRacha,
                    rachaMaxima = maxOf(nuevaRacha, nuevosDatos.rachaMaxima)
                )
            } else {
                nuevosDatos
            }

            repositorio.guardarDatosRetos(datosFinales)

            _estadoUi.update {
                it.copy(
                    datosRetos = datosFinales,
                    milisegundosHastaReset = calcularTiempoReset()
                )
            }

            onResult(true, "+${"%.2f".format(recompensa)}€ añadidos")
        }
    }
}