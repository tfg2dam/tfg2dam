package com.simutrade.screens.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Reto
import com.simutrade.data.model.RetosData
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class RetoValidacion(
    val completado: Boolean,
    val mensaje: String
)

class ChallengesViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _retosData = MutableStateFlow(RetosData())
    val retosData: StateFlow<RetosData> = _retosData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Milisegundos que faltan para el reset de mañana
    private val _millisHastaReset = MutableStateFlow(0L)
    val millisHastaReset: StateFlow<Long> = _millisHastaReset.asStateFlow()

    fun cargarRetos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val datos = repository.getRetosData()
                val datosActualizados = comprobarResetDiario(datos)
                _retosData.value = datosActualizados
                calcularTiempoHastaReset()
            } catch (e: Exception) {
                println("Error retos: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Comprueba si hay que hacer reset de retos por ser un día nuevo
    private suspend fun comprobarResetDiario(datos: RetosData): RetosData {
        val hoy = getDiaActual()

        // Es un dia nuevo respecto a cuando se guardaron los retos
        if (datos.diaActual != hoy && datos.diaActual != 0) {
            val ayer = hoy - 1

            // Calcular si se rompe la racha
            // Si el último día activo fue ayer, la racha se mantiene
            // Si fue hace más de un día, se rompe
            val nuevaRacha = if (datos.diaActual == ayer) datos.rachaActual else 0
            val nuevaMax = maxOf(datos.rachaMaxima, nuevaRacha)

            val datosReseteados = datos.copy(
                rachaActual = nuevaRacha,
                rachaMaxima = nuevaMax,
                retosCompletados = emptyList(), // reset de retos completados
                diaActual = hoy
            )

            repository.saveRetosData(datosReseteados)
            return datosReseteados
        }

        // Primer uso — inicializar diaActual
        if (datos.diaActual == 0) {
            val inicializado = datos.copy(diaActual = hoy)
            repository.saveRetosData(inicializado)
            return inicializado
        }

        return datos
    }

    private fun calcularTiempoHastaReset() {
        val ahora = Calendar.getInstance()
        val manana = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _millisHastaReset.value = manana.timeInMillis - ahora.timeInMillis
    }

    private fun getDiaActual(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    }

    fun getRetosDelDia(): List<Reto> {
        val dia = getDiaActual()
        return listOf(
            Reto("reto_${dia}_1", "Primera operacion", "Realiza una compra o venta hoy", "1", 2.0),
            Reto("reto_${dia}_2", "Diversifica", "Ten al menos 2 activos en cartera", "2", 3.0),
            Reto("reto_${dia}_3", "Inversor activo", "Opera con un activo de cada tipo (accion y cripto)", "3", 5.0)
        )
    }

    fun todoRetosCompletados(): Boolean {
        val retosDelDia = getRetosDelDia()
        return retosDelDia.all { it.id in _retosData.value.retosCompletados }
    }

    suspend fun validarReto(retoId: String): RetoValidacion {
        val dia = getDiaActual()
        val cartera = repository.getCartera()
        val transacciones = repository.getTransacciones()

        val hoyInicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val transaccionesHoy = transacciones.filter { it.date >= hoyInicio }

        return when (retoId) {
            "reto_${dia}_1" -> {
                if (transaccionesHoy.isNotEmpty())
                    RetoValidacion(true, "Has realizado ${transaccionesHoy.size} operacion(es) hoy")
                else
                    RetoValidacion(false, "No has realizado ninguna compra o venta hoy. Ve al Mercado y opera.")
            }
            "reto_${dia}_2" -> {
                if (cartera.size >= 2)
                    RetoValidacion(true, "Tienes ${cartera.size} activos diferentes en cartera")
                else
                    RetoValidacion(false, "Solo tienes ${cartera.size} activo(s). Necesitas al menos 2 activos diferentes.")
            }
            "reto_${dia}_3" -> {
                val tieneAccion = cartera.any { it.type.name == "STOCK" }
                val tieneCripto = cartera.any { it.type.name == "CRYPTO" }
                when {
                    tieneAccion && tieneCripto ->
                        RetoValidacion(true, "Tienes acciones y criptomonedas en cartera")
                    !tieneAccion && !tieneCripto ->
                        RetoValidacion(false, "No tienes ningun activo. Compra al menos una accion y una cripto.")
                    !tieneAccion ->
                        RetoValidacion(false, "Te falta una accion. Tienes criptos pero ninguna accion.")
                    else ->
                        RetoValidacion(false, "Te falta una cripto. Tienes acciones pero ninguna accion.")
                }
            }
            else -> RetoValidacion(false, "Reto desconocido")
        }
    }

    fun completarReto(retoId: String, recompensa: Double, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val validacion = validarReto(retoId)

            if (!validacion.completado) {
                onResult(false, validacion.mensaje)
                return@launch
            }

            if (retoId in _retosData.value.retosCompletados) {
                onResult(false, "Ya completaste este reto hoy")
                return@launch
            }

            try {
                val userData = repository.getUserData()
                val nuevoSaldo = userData.saldo + recompensa
                repository.updateSaldo(nuevoSaldo)

                val nuevosCompletados = _retosData.value.retosCompletados + retoId
                val nuevosRetos = _retosData.value.copy(retosCompletados = nuevosCompletados)
                repository.saveRetosData(nuevosRetos)
                _retosData.value = nuevosRetos

                // Si completo todos los retos del dia, actualizar racha
                if (todoRetosCompletados()) {
                    actualizarRachaAlCompletarTodos()
                }

                calcularTiempoHastaReset()
                onResult(true, "Reto completado. +${"%.2f".format(recompensa)} EUR añadidos a tu saldo")

            } catch (e: Exception) {
                onResult(false, "Error al completar el reto: ${e.message}")
            }
        }
    }

    private suspend fun actualizarRachaAlCompletarTodos() {
        val retosData = _retosData.value
        val hoy = getDiaActual()

        val nuevaRacha = retosData.rachaActual + 1
        val nuevaMax = maxOf(nuevaRacha, retosData.rachaMaxima)

        val updated = retosData.copy(
            rachaActual = nuevaRacha,
            rachaMaxima = nuevaMax,
            diaActual = hoy
        )
        _retosData.value = updated
        repository.saveRetosData(updated)
    }
}