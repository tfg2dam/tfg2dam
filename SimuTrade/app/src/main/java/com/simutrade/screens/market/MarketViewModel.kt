package com.simutrade.screens.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Activo
import com.simutrade.data.repository.RepositorioMercado
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

class MarketViewModel : ViewModel() {

    private val repositorio = RepositorioMercado

    private val _uiState = MutableStateFlow(EstadoUiMercado())
    val uiState: StateFlow<EstadoUiMercado> = _uiState.asStateFlow()

    private var trabajoBusqueda: Job? = null
    private var trabajoRefresh: Job? = null
    private var trabajoCarga: Job? = null

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

    private fun iniciarAutoRefresh() {
        trabajoRefresh?.cancel()

        trabajoRefresh = viewModelScope.launch {
            while (isActive) {
                delay(INTERVALO_REFRESH_MS)

                val estado = _uiState.value
                if (!estado.actualizando && estado.resultadosBusqueda.isEmpty()) {
                    cargarDatosMercado(forzar = false)
                }
            }
        }
    }

    // ================= CARGAR =================

    fun cargarDatosMercado(forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()

        if (ahora < bloqueadoHasta) return

        if (
            !forzar &&
            _uiState.value.criptomonedas.isNotEmpty() &&
            (ahora - _uiState.value.ultimaActualizacion) < MINIMO_REFRESH_MS
        ) return

        trabajoCarga?.cancel()

        trabajoCarga = viewModelScope.launch {

            val hayDatosEnPantalla = _uiState.value.criptomonedas.isNotEmpty() ||
                    _uiState.value.acciones.isNotEmpty()

            _uiState.value = _uiState.value.copy(
                cargandoInicial = !hayDatosEnPantalla,
                actualizando = hayDatosEnPantalla,
                error = null
            )

            try {
                val (criptomonedas, acciones) = coroutineScope {
                    val criptomonedasDiferidas = async {
                        reintentarIO { repositorio.obtenerTopCriptomonedas() }
                    }
                    val accionesDiferidas = async {
                        reintentarIO { repositorio.obtenerTopAcciones() }
                    }
                    Pair(
                        criptomonedasDiferidas.await(),
                        accionesDiferidas.await()
                    )
                }

                _uiState.value = _uiState.value.copy(
                    criptomonedas = criptomonedas.ifEmpty {
                        _uiState.value.criptomonedas
                    },
                    acciones = acciones.ifEmpty {
                        _uiState.value.acciones
                    },
                    cargandoInicial = false,
                    actualizando = false,
                    ultimaActualizacion = System.currentTimeMillis(),
                    error = null
                )

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                val esRateLimit = e.message?.contains("429") == true ||
                        e.toString().contains("429")

                if (esRateLimit) {
                    bloqueadoHasta = System.currentTimeMillis() + MINIMO_REFRESH_MS
                }

                _uiState.value = _uiState.value.copy(
                    cargandoInicial = false,
                    actualizando = false,
                    error = if (!hayDatosEnPantalla) {
                        if (esRateLimit) "Demasiadas peticiones. Espera un momento."
                        else "Error al cargar el mercado"
                    } else null
                )
            }
        }
    }

    // ================= BÚSQUEDA =================

    fun buscar(textoBusqueda: String) {
        trabajoBusqueda?.cancel()

        if (textoBusqueda.isBlank()) {
            _uiState.value = _uiState.value.copy(
                resultadosBusqueda = emptyList(),
                buscando = false
            )
            return
        }

        trabajoBusqueda = viewModelScope.launch {
            delay(400)

            _uiState.value = _uiState.value.copy(
                buscando = true,
                error = null
            )

            try {
                val resultados = reintentarIO {
                    repositorio.buscarActivos(textoBusqueda)
                }

                // ✅ Filtra activos sin precio — no se pueden operar
                _uiState.value = _uiState.value.copy(
                    resultadosBusqueda = resultados.filter { it.precioActual > 0 },
                    buscando = false
                )

            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    buscando = false,
                    error = "Error en la búsqueda"
                )
            }
        }
    }

    // ================= REINTENTO =================

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