package com.simutrade.screens.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.*
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class RetoValidacion(
    val completado: Boolean,
    val mensaje: String
)

class ChallengesViewModel : ViewModel() {

    private val repository = UserRepository()

    // ================= STATE =================

    private val _retosData = MutableStateFlow(RetosData())
    val retosData: StateFlow<RetosData> = _retosData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _millisHastaReset = MutableStateFlow(0L)
    val millisHastaReset: StateFlow<Long> = _millisHastaReset.asStateFlow()

    // ================= INIT =================

    fun cargarRetos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val datos = repository.getRetosData()
                val actualizados = comprobarResetDiario(datos)
                _retosData.value = actualizados
                calcularTiempoHastaReset()
            } catch (e: Exception) {
                println("Error retos: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ================= RESET DIARIO =================

    private suspend fun comprobarResetDiario(datos: RetosData): RetosData {
        val hoy = getDiaActual()

        // PRIMER USO
        if (datos.diaActual == 0) {
            val inicial = datos.copy(
                diaActual = hoy,
                retosDelDia = generarRetosRandom()
            )
            repository.saveRetosData(inicial)
            return inicial
        }

        // NUEVO DÍA
        if (datos.diaActual != hoy) {

            val ayer = hoy - 1

            val nuevaRacha =
                if (datos.diaActual == ayer) datos.rachaActual else 0

            val nuevaMax = maxOf(datos.rachaMaxima, nuevaRacha)

            val reseteado = datos.copy(
                rachaActual = nuevaRacha,
                rachaMaxima = nuevaMax,
                retosCompletados = emptyList(),
                retosDelDia = generarRetosRandom(),
                diaActual = hoy
            )

            repository.saveRetosData(reseteado)
            return reseteado
        }

        return datos
    }

    // ================= TIEMPO =================

    private fun calcularTiempoHastaReset() {
        val ahora = Calendar.getInstance(TimeZone.getDefault())

        val manana = (ahora.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        _millisHastaReset.value = manana.timeInMillis - ahora.timeInMillis
    }

    private fun getDiaActual(): Int =
        Calendar.getInstance(TimeZone.getDefault()).get(Calendar.DAY_OF_YEAR)

    // ================= RANDOM =================

    private fun generarRetosRandom(): List<String> {
        val pool = listOf(
            "operacion",
            "diversifica",
            "trader",
            "multimercado",
            "beneficio"
        )

        return pool.shuffled().take(3)
    }

    // ================= RETOS =================

    fun getRetosDelDia(): List<Reto> {
        val dia = getDiaActual()

        return _retosData.value.retosDelDia.mapIndexed { index, tipo ->

            val numero = index + 1
            val id = "reto_${tipo}_${dia}_$numero" // 🔥 FIX CLAVE

            when (tipo) {

                "operacion" -> Reto(id, "Primera operación", "Haz una compra o venta hoy", "📈", 2.0)

                "diversifica" -> Reto(id, "Diversifica", "Ten al menos 2 activos distintos", "📊", 3.0)

                "trader" -> Reto(id, "Trader activo", "Realiza 3 operaciones hoy", "🔥", 5.0)

                "multimercado" -> Reto(id, "Multi mercado", "Ten acciones y criptos", "🌍", 4.0)

                "beneficio" -> Reto(id, "En beneficio", "Consigue beneficio positivo", "💰", 6.0)

                else -> Reto("error", "Error", "Error", "❌", 0.0)
            }
        }
    }

    fun todoRetosCompletados(): Boolean {
        val retos = getRetosDelDia()
        return retos.all { it.id in _retosData.value.retosCompletados }
    }

    // ================= VALIDACIONES =================

    suspend fun validarReto(retoId: String): RetoValidacion {

        val tipo = obtenerTipoDesdeId(retoId)

        val cartera = repository.getCartera()
        val transacciones = repository.getTransacciones()
        val user = repository.getUserData()

        val hoyInicio = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val transaccionesHoy = transacciones.filter { it.date >= hoyInicio }

        return when (tipo) {

            "operacion" -> {
                if (transaccionesHoy.isNotEmpty())
                    RetoValidacion(true, "Has hecho ${transaccionesHoy.size} operación(es)")
                else
                    RetoValidacion(false, "Haz al menos una operación hoy")
            }

            "trader" -> {
                if (transaccionesHoy.size >= 3)
                    RetoValidacion(true, "Has hecho ${transaccionesHoy.size} operaciones")
                else
                    RetoValidacion(false, "Necesitas 3 operaciones")
            }

            "diversifica" -> {
                val distintos = cartera.map { it.assetId }.distinct().size
                if (distintos >= 2)
                    RetoValidacion(true, "Tienes $distintos activos distintos")
                else
                    RetoValidacion(false, "Necesitas al menos 2 activos")
            }

            "multimercado" -> {
                val stock = cartera.any { it.type == AssetType.STOCK }
                val crypto = cartera.any { it.type == AssetType.CRYPTO }

                when {
                    stock && crypto -> RetoValidacion(true, "Tienes ambos mercados")
                    !stock && !crypto -> RetoValidacion(false, "No tienes activos")
                    !stock -> RetoValidacion(false, "Te falta una acción")
                    else -> RetoValidacion(false, "Te falta una cripto")
                }
            }

            "beneficio" -> {
                val portfolio = repository.calcularValorCartera(cartera)
                val total = repository.calcularValorTotal(user.saldo, portfolio)
                val profit = repository.calcularBeneficio(total, user.saldoInicial)

                if (profit > 0)
                    RetoValidacion(true, "Vas ganando €${"%.2f".format(profit)}")
                else
                    RetoValidacion(false, "Aún no estás en beneficio")
            }

            else -> RetoValidacion(false, "Reto desconocido")
        }
    }

    private fun obtenerTipoDesdeId(retoId: String): String {
        return when {
            retoId.contains("operacion") -> "operacion"
            retoId.contains("diversifica") -> "diversifica"
            retoId.contains("trader") -> "trader"
            retoId.contains("multimercado") -> "multimercado"
            retoId.contains("beneficio") -> "beneficio"
            else -> ""
        }
    }

    // ================= COMPLETAR =================

    fun completarReto(
        retoId: String,
        recompensa: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {

            val validacion = validarReto(retoId)

            if (!validacion.completado) {
                onResult(false, validacion.mensaje)
                return@launch
            }

            if (retoId in _retosData.value.retosCompletados) {
                onResult(false, "Ya completado")
                return@launch
            }

            val user = repository.getUserData()
            val nuevoSaldo = user.saldo + recompensa
            repository.updateSaldo(nuevoSaldo)

            val nuevos = _retosData.value.retosCompletados + retoId
            val updated = _retosData.value.copy(retosCompletados = nuevos)

            repository.saveRetosData(updated)
            _retosData.value = updated

            if (todoRetosCompletados()) {
                actualizarRacha()
            }

            calcularTiempoHastaReset()

            onResult(true, "+${"%.2f".format(recompensa)}€")
        }
    }

    private suspend fun actualizarRacha() {
        val actual = _retosData.value
        val nueva = actual.rachaActual + 1
        val max = maxOf(nueva, actual.rachaMaxima)

        val updated = actual.copy(
            rachaActual = nueva,
            rachaMaxima = max
        )

        _retosData.value = updated
        repository.saveRetosData(updated)
    }
}