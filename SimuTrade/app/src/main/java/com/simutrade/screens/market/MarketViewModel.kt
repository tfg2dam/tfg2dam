package com.simutrade.screens.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Asset
import com.simutrade.data.repository.MarketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarketUiState(
    val cryptos: List<Asset> = emptyList(),
    val stocks: List<Asset> = emptyList(),
    val searchResults: List<Asset> = emptyList(),
    val isLoading: Boolean = false,
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

    companion object {
        const val REFRESH_INTERVAL_MS = 120_000L
        const val MIN_REFRESH_MS = 60_000L
    }

    init {
        loadMarketData()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                if (_uiState.value.searchResults.isEmpty()) {
                    refreshPrices()
                }
            }
        }
    }

    fun loadMarketData() {
        val ahora = System.currentTimeMillis()

        if (ahora - lastLoadTime < MIN_REFRESH_MS && _uiState.value.cryptos.isNotEmpty()) {
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val cryptos = repository.getTopCryptos()
                val stocks = repository.getTopStocks()

                lastLoadTime = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    cryptos = cryptos,
                    stocks = stocks,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    val esRateLimit = e.message?.contains("429") == true ||
                            e.toString().contains("429")

                    if (esRateLimit) lastLoadTime = System.currentTimeMillis()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (_uiState.value.cryptos.isEmpty()) {
                            if (esRateLimit) "Demasiadas peticiones. Espera un momento."
                            else "Error al cargar datos del mercado"
                        } else null
                    )
                }
            }
        }
    }

    private fun refreshPrices() {
        viewModelScope.launch {
            try {
                val cryptos = repository.getTopCryptos()
                val stocks = repository.getTopStocks()

                lastLoadTime = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    cryptos = cryptos,
                    stocks = stocks,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                // silencioso, mantenemos datos anteriores
            }
        }
    }

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
            delay(500)
            _uiState.value = _uiState.value.copy(isSearching = true)

            try {
                val results = repository.searchAssets(query)
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

    fun loadPriceHistory(asset: Asset, period: String = "7d") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingHistory = true,
                selectedPeriod = period
            )

            try {
                val history = repository.getAssetHistory(asset, period)
                _uiState.value = _uiState.value.copy(
                    priceHistory = history,
                    isLoadingHistory = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = "Error cargando gráfico"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        refreshJob?.cancel()
        searchJob?.cancel()
    }
}