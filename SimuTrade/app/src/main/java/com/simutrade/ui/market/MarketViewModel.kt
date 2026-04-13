package com.simutrade.ui.market

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
    val selectedPeriod: String = "7d"  // ← añadido
)

class MarketViewModel : ViewModel() {

    private val repository = MarketRepository()

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var refreshJob: Job? = null

    companion object {
        const val REFRESH_INTERVAL_MS = 30_000L
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val cryptos = repository.getTopCryptos()
                val stocks = repository.getTopStocks()
                _uiState.value = _uiState.value.copy(
                    cryptos = cryptos,
                    stocks = stocks,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar datos del mercado"
                )
            }
        }
    }

    private fun refreshPrices() {
        viewModelScope.launch {
            try {
                val cryptos = repository.getTopCryptos()
                val stocks = repository.getTopStocks()
                _uiState.value = _uiState.value.copy(
                    cryptos = cryptos,
                    stocks = stocks,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) { }
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
            val results = repository.searchAssets(query)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    // ← Actualizado con parámetro period
    fun loadPriceHistory(asset: Asset, period: String = "7d") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingHistory = true,
                selectedPeriod = period
            )
            val history = repository.getAssetHistory(asset, period)
            _uiState.value = _uiState.value.copy(
                priceHistory = history,
                isLoadingHistory = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        searchJob?.cancel()
    }
}