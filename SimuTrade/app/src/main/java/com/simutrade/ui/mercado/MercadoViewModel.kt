package com.simutrade.ui.mercado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.datos.modelo.Activo
import com.simutrade.datos.repositorio.RepositorioMercado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// Estado de la pantalla de mercado
data class EstadoUiMercado(
    val criptomonedas: List<Activo> = emptyList(),
    val acciones: List<Activo> = emptyList(),
    val resultadosBusqueda: List<Activo> = emptyList(),
    val cargandoInicial: Boolean = true,
    val actualizando: Boolean = false,
    val buscando: Boolean = false,
    val error: String? = null,
    val ultimaActualizacion: Long = 0L
)

class MercadoViewModel : ViewModel() {

    private val repositorio = RepositorioMercado

    private val _estadoUi = MutableStateFlow(EstadoUiMercado())
    val estadoUi: StateFlow<EstadoUiMercado> = _estadoUi.asStateFlow()

    private var trabajoBusqueda: Job? = null
    private var trabajoRefresh: Job? = null
    private var trabajoCarga: Job? = null

    // Timestamp hasta el que no se permite hacer más peticiones (rate limit)
    private var bloqueadoHasta = 0L

    companion object {
        private const val INTERVALO_REFRESH_MS = 120_000L
        const val MINIMO_REFRESH_MS = 60_000L
        private const val TIMEOUT_MS = 10_000L
    }

    init {
        cargarDatosMercado(forzar = true)
        iniciarAutoRefresh()
    }

    // ================= AUTO REFRESH =================

    // Refresca el mercado cada 2 minutos si no hay búsqueda activa
    private fun iniciarAutoRefresh() {
        trabajoRefresh?.cancel()
        trabajoRefresh = viewModelScope.launch {
            while (isActive) {
                delay(INTERVALO_REFRESH_MS)
                val estado = _estadoUi.value
                if (!estado.actualizando && estado.resultadosBusqueda.isEmpty()) {
                    cargarDatosMercado(forzar = false)
                }
            }
        }
    }

    // ================= CARGAR =================

    // Carga criptomonedas y acciones en paralelo respetando el rate limit
    fun cargarDatosMercado(forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()

        if (ahora < bloqueadoHasta) return
        if (!forzar &&
            _estadoUi.value.criptomonedas.isNotEmpty() &&
            (ahora - _estadoUi.value.ultimaActualizacion) < MINIMO_REFRESH_MS
        ) return

        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            val hayDatos = _estadoUi.value.criptomonedas.isNotEmpty() ||
                    _estadoUi.value.acciones.isNotEmpty()

            _estadoUi.value = _estadoUi.value.copy(
                cargandoInicial = !hayDatos,
                actualizando = hayDatos,
                error = null
            )

            try {
                val (criptomonedas, acciones) = coroutineScope {
                    val criptomonedasDiferidas = async { reintentarIO { repositorio.obtenerTopCriptomonedas() } }
                    val accionesDiferidas      = async { reintentarIO { repositorio.obtenerTopAcciones() } }
                    Pair(criptomonedasDiferidas.await(), accionesDiferidas.await())
                }

                _estadoUi.value = _estadoUi.value.copy(
                    criptomonedas = criptomonedas.ifEmpty { _estadoUi.value.criptomonedas },
                    acciones = acciones.ifEmpty { _estadoUi.value.acciones },
                    cargandoInicial = false,
                    actualizando = false,
                    ultimaActualizacion = System.currentTimeMillis(),
                    error = null
                )

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                // Detecta rate limit 429 y bloquea peticiones durante 1 minuto
                val esRateLimit = e.message?.contains("429") == true || e.toString().contains("429")
                if (esRateLimit) bloqueadoHasta = System.currentTimeMillis() + MINIMO_REFRESH_MS

                _estadoUi.value = _estadoUi.value.copy(
                    cargandoInicial = false,
                    actualizando = false,
                    error = if (!hayDatos) {
                        if (esRateLimit) "Demasiadas peticiones. Espera un momento." else "Error al cargar el mercado"
                    } else null
                )
            }
        }
    }

    // ================= BÚSQUEDA =================

    // Busca activos con debounce de 400ms y filtra los que no tienen precio
    fun buscar(textoBusqueda: String) {
        trabajoBusqueda?.cancel()

        if (textoBusqueda.isBlank()) {
            _estadoUi.value = _estadoUi.value.copy(resultadosBusqueda = emptyList(), buscando = false)
            return
        }

        trabajoBusqueda = viewModelScope.launch {
            delay(400)
            _estadoUi.value = _estadoUi.value.copy(buscando = true, error = null)
            try {
                val resultados = reintentarIO { repositorio.buscarActivos(textoBusqueda) }
                // Filtra activos sin precio — no se pueden operar
                _estadoUi.value = _estadoUi.value.copy(
                    resultadosBusqueda = resultados.filter { it.precioActual > 0 },
                    buscando = false
                )
            } catch (_: Exception) {
                _estadoUi.value = _estadoUi.value.copy(buscando = false, error = "Error en la búsqueda")
            }
        }
    }

    // ================= REINTENTO =================

    // Reintenta una operación de red con backoff exponencial
    private suspend fun <T> reintentarIO(
        intentos: Int = 2,
        retrasoInicial: Long = 500L,
        bloque: suspend () -> T
    ): T {
        var retrasoActual = retrasoInicial
        repeat(intentos - 1) {
            try {
                return withTimeout(TIMEOUT_MS) { bloque() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                delay(retrasoActual)
                retrasoActual *= 2
            }
        }
        return withTimeout(TIMEOUT_MS) { bloque() }
    }
}