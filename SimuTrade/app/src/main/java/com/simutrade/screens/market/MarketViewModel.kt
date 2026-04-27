package com.simutrade.screens.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Asset
import com.simutrade.data.repository.MarketRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MarketUiState(
    val cryptos: List<Asset> = emptyList(),
    val stocks: List<Asset> = emptyList(),
    val searchResults: List<Asset> = emptyList(),

    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false, // 🔥 CLAVE UX

    val isSearching: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0L,

    val priceHistory: List<Pair<Long, Double>> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val selectedPeriod: String = "7d"
)

class MarketViewModel : ViewModel() {

    private val repository = MarketRepository()

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var refreshJob: Job? = null
    private var loadJob: Job? = null

    private var lastLoadTime = 0L

    // 🔥 CACHE REAL (importantísimo)
    private var cachedCryptos: List<Asset> = emptyList()
    private var cachedStocks: List<Asset> = emptyList()

    companion object {
        const val REFRESH_INTERVAL_MS = 60_000L // 🔥 1 minuto
        const val MIN_REFRESH_MS = 5_000L
        const val TIMEOUT_MS = 10_000L
    }

    init {
        loadMarketData(force = true)
        startAutoRefresh()
    }

    // ================= AUTO REFRESH =================

    private fun startAutoRefresh() {
        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)

                // 🔥 SIEMPRE refresca (como pedías)
                loadMarketData(force = true)
            }
        }
    }

    // ================= LOAD =================

    fun loadMarketData(force: Boolean = false) {

        val now = System.currentTimeMillis()

        // 🔥 anti spam SOLO si no es manual
        if (!force &&
            now - lastLoadTime < MIN_REFRESH_MS &&
            cachedCryptos.isNotEmpty()
        ) return

        loadJob?.cancel()

        loadJob = viewModelScope.launch {

            val hasCache = cachedCryptos.isNotEmpty() || cachedStocks.isNotEmpty()

            _uiState.value = _uiState.value.copy(
                isInitialLoading = !hasCache, // solo primera vez
                isRefreshing = hasCache,      // 🔥 loader en lista
                error = null
            )

            try {
                val (cryptos, stocks) = coroutineScope {

                    val cryptosDeferred = async {
                        retryIO { repository.getTopCryptos() }
                    }

                    val stocksDeferred = async {
                        retryIO { repository.getTopStocks() }
                    }

                    cryptosDeferred.await() to stocksDeferred.await()
                }

                // 🔥 SOLO actualizar si hay datos válidos
                if (cryptos.isNotEmpty()) cachedCryptos = cryptos
                if (stocks.isNotEmpty()) cachedStocks = stocks

                lastLoadTime = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    cryptos = cachedCryptos,
                    stocks = cachedStocks,
                    isInitialLoading = false,
                    isRefreshing = false,
                    lastUpdated = System.currentTimeMillis(),
                    error = null
                )

            } catch (e: Exception) {

                if (e !is CancellationException) {

                    val isRateLimit =
                        e.message?.contains("429") == true ||
                                e.toString().contains("429")

                    // 🔥 NUNCA perder datos
                    _uiState.value = _uiState.value.copy(
                        cryptos = cachedCryptos,
                        stocks = cachedStocks,
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = if (!hasCache) {
                            if (isRateLimit) {
                                "Demasiadas peticiones. Espera unos segundos."
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
    }

    // ================= SEARCH =================

    fun search(query: String) {

        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        searchJob = viewModelScope.launch {

            delay(400)

            _uiState.value = _uiState.value.copy(isSearching = true)

            try {
                val results = retryIO {
                    repository.searchAssets(query)
                }

                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    isSearching = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "Error en la búsqueda"
                )
            }
        }
    }

    // ================= HISTÓRICO =================

    fun loadPriceHistory(asset: Asset, period: String = "7d") {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoadingHistory = true,
                selectedPeriod = period,
                error = null
            )

            try {
                val history = retryIO {
                    repository.getAssetHistory(asset, period)
                }

                _uiState.value = _uiState.value.copy(
                    priceHistory = history,
                    isLoadingHistory = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = "Error cargando datos"
                )
            }
        }
    }

    // ================= RETRY =================

    private suspend fun <T> retryIO(
        times: Int = 2,
        initialDelay: Long = 500,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay

        repeat(times - 1) {
            try {
                return withTimeout(TIMEOUT_MS) { block() }
            } catch (_: Exception) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }

        return withTimeout(TIMEOUT_MS) { block() }
    }

    // ================= CLEAN =================

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        refreshJob?.cancel()
        searchJob?.cancel()
    }
}