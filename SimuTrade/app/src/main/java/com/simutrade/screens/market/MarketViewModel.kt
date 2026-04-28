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

    private val repositorio = RepositorioMercado()

    private val _uiState = MutableStateFlow(
        EstadoUiMercado()
    )

    val uiState: StateFlow<EstadoUiMercado> =
        _uiState.asStateFlow()

    private var trabajoBusqueda: Job? = null
    private var trabajoRefresh: Job? = null
    private var trabajoCarga: Job? = null

    private var ultimaCarga = 0L
    private var bloqueadoHasta = 0L

    private var criptomonedasCache: List<Activo> = emptyList()
    private var accionesCache: List<Activo> = emptyList()

    companion object {
        private const val INTERVALO_REFRESH_MS = 120_000L

        // público para usar desde MarketScreen
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

                if (
                    !estado.actualizando &&
                    estado.resultadosBusqueda.isEmpty()
                ) {
                    cargarDatosMercado(forzar = false)
                }
            }
        }
    }

    // ================= CARGAR =================

    fun cargarDatosMercado(
        forzar: Boolean = false
    ) {
        val ahora = System.currentTimeMillis()

        // bloqueo temporal por rate limit
        if (ahora < bloqueadoHasta) {
            return
        }

        // cooldown normal
        if (
            !forzar &&
            ahora - ultimaCarga < MINIMO_REFRESH_MS &&
            criptomonedasCache.isNotEmpty()
        ) {
            return
        }

        trabajoCarga?.cancel()

        trabajoCarga = viewModelScope.launch {

            val hayCache =
                criptomonedasCache.isNotEmpty() ||
                        accionesCache.isNotEmpty()

            if (hayCache) {
                _uiState.value = _uiState.value.copy(
                    criptomonedas = criptomonedasCache,
                    acciones = accionesCache,
                    cargandoInicial = false,
                    actualizando = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    cargandoInicial = true,
                    actualizando = false,
                    error = null
                )
            }

            try {
                val (criptomonedas, acciones) = coroutineScope {

                    val criptomonedasDiferidas = async {
                        reintentarIO {
                            repositorio.obtenerTopCriptomonedas()
                        }
                    }

                    val accionesDiferidas = async {
                        reintentarIO {
                            repositorio.obtenerTopAcciones()
                        }
                    }

                    Pair(
                        criptomonedasDiferidas.await(),
                        accionesDiferidas.await()
                    )
                }

                if (criptomonedas.isNotEmpty()) {
                    criptomonedasCache = criptomonedas
                }

                if (acciones.isNotEmpty()) {
                    accionesCache = acciones
                }

                ultimaCarga = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    criptomonedas = criptomonedasCache,
                    acciones = accionesCache,
                    cargandoInicial = false,
                    actualizando = false,
                    ultimaActualizacion = System.currentTimeMillis(),
                    error = null
                )

            } catch (e: Exception) {

                if (e is CancellationException) {
                    return@launch
                }

                val esRateLimit =
                    e.message?.contains("429") == true ||
                            e.toString().contains("429")

                if (esRateLimit) {
                    bloqueadoHasta =
                        System.currentTimeMillis() + MINIMO_REFRESH_MS
                }

                _uiState.value = _uiState.value.copy(
                    criptomonedas = criptomonedasCache,
                    acciones = accionesCache,
                    cargandoInicial = false,
                    actualizando = false,
                    error = if (!hayCache) {
                        if (esRateLimit) {
                            "Demasiadas peticiones. Espera un momento."
                        } else {
                            "Error al cargar el mercado"
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    // ================= BÚSQUEDA =================

    fun buscar(
        textoBusqueda: String
    ) {
        trabajoBusqueda?.cancel()

        if (textoBusqueda.isBlank()) {
            _uiState.value = _uiState.value.copy(
                resultadosBusqueda = emptyList(),
                buscando = false
            )
            return
        }

        trabajoBusqueda = viewModelScope.launch {

            val consultaActual = textoBusqueda

            delay(400)

            _uiState.value = _uiState.value.copy(
                buscando = true,
                error = null
            )

            try {
                val resultados = reintentarIO {
                    repositorio.buscarActivos(consultaActual)
                }

                _uiState.value = _uiState.value.copy(
                    resultadosBusqueda = resultados,
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
                return withTimeout(TIMEOUT_MS) {
                    bloque()
                }
            } catch (_: Exception) {
                delay(retrasoActual)
                retrasoActual *= 2
            }
        }

        return withTimeout(TIMEOUT_MS) {
            bloque()
        }
    }

    // ================= LIMPIEZA =================

    override fun onCleared() {
        super.onCleared()

        trabajoCarga?.cancel()
        trabajoRefresh?.cancel()
        trabajoBusqueda?.cancel()
    }
}