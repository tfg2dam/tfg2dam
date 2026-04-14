package com.simutrade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.MockData
import com.simutrade.data.model.*
import com.simutrade.data.repository.MarketRepository
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = UserRepository()
    private val marketRepository = MarketRepository()

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    private val _cartera = MutableStateFlow<List<PortfolioHolding>>(emptyList())
    val cartera: StateFlow<List<PortfolioHolding>> = _cartera.asStateFlow()

    private val _transacciones = MutableStateFlow<List<Transaction>>(emptyList())
    val transacciones: StateFlow<List<Transaction>> = _transacciones.asStateFlow()

    private val _assets = MutableStateFlow(MockData.mockAssets)
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _selectedAsset = MutableStateFlow<Asset?>(null)
    val selectedAsset: StateFlow<Asset?> = _selectedAsset.asStateFlow()

    private val _currentPage = MutableStateFlow("dashboard")
    val currentPage: StateFlow<String> = _currentPage.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _isLoadingLeaderboard = MutableStateFlow(false)
    val isLoadingLeaderboard: StateFlow<Boolean> = _isLoadingLeaderboard.asStateFlow()

    private val _retosData = MutableStateFlow(RetosData())
    val retosData: StateFlow<RetosData> = _retosData.asStateFlow()

    private val _isLoadingRetos = MutableStateFlow(false)
    val isLoadingRetos: StateFlow<Boolean> = _isLoadingRetos.asStateFlow()

    init {
        cargarDatos()
        cargarAssets()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _userData.value = repository.getUserData()
            _cartera.value = repository.getCartera()
            _transacciones.value = repository.getTransacciones()
        }
    }

    fun cargarAssets() {
        viewModelScope.launch {
            try {
                val cryptos = marketRepository.getTopCryptos()
                val stocks = marketRepository.getTopStocks()
                _assets.value = cryptos + stocks
            } catch (e: Exception) {
                // silencioso, los mockAssets siguen como fallback
            }
        }
    }

    fun cargarLeaderboard() {
        viewModelScope.launch {
            _isLoadingLeaderboard.value = true
            try {
                _leaderboard.value = repository.getLeaderboard()
            } catch (e: Exception) {
                // silencioso
            } finally {
                _isLoadingLeaderboard.value = false
            }
        }
    }

    fun cargarRetos() {
        viewModelScope.launch {
            _isLoadingRetos.value = true
            try {
                _retosData.value = repository.getRetosData()
            } catch (e: Exception) {
                // silencioso
            } finally {
                _isLoadingRetos.value = false
            }
        }
    }

    fun completarReto(retoId: String, recompensa: Double) {
        viewModelScope.launch {
            val retosData = _retosData.value
            if (retoId in retosData.retosCompletados) return@launch

            val hoy = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            val ayer = hoy - 24 * 60 * 60 * 1000

            val nuevaRacha = when {
                retosData.ultimaVez >= hoy -> retosData.rachaActual
                retosData.ultimaVez >= ayer -> retosData.rachaActual + 1
                else -> 1
            }

            val nuevosRetosCompletados = retosData.retosCompletados + retoId
            val nuevaRetosData = retosData.copy(
                rachaActual = nuevaRacha,
                rachaMaxima = maxOf(nuevaRacha, retosData.rachaMaxima),
                ultimaVez = System.currentTimeMillis(),
                retosCompletados = nuevosRetosCompletados
            )

            repository.saveRetosData(nuevaRetosData)
            _retosData.value = nuevaRetosData

            val bonusRacha = when {
                nuevaRacha >= 30 -> 15.0
                nuevaRacha >= 7 -> 5.0
                else -> 2.0
            }
            val totalRecompensa = recompensa + bonusRacha
            val nuevoSaldo = _userData.value.saldo + totalRecompensa
            repository.updateSaldo(nuevoSaldo)
            _userData.value = _userData.value.copy(saldo = nuevoSaldo)
        }
    }

    fun getRetosDelDia(): List<Reto> {
        val dia = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return listOf(
            Reto("reto_${dia}_1", "Primera operación", "Realiza una compra o venta hoy", "📈", 2.0),
            Reto("reto_${dia}_2", "Diversifica", "Ten al menos 2 activos en cartera", "🎯", 3.0),
            Reto("reto_${dia}_3", "Inversor activo", "Opera con un activo de cada tipo", "⚡", 5.0)
        )
    }

    fun navigateTo(page: String) { _currentPage.value = page }

    fun selectAsset(asset: Asset) {
        _selectedAsset.value = asset
        navigateTo("trading")
    }

    fun getCurrentRank() = MockData.getRankFromProfit(
        repository.calcularBeneficio(
            repository.calcularValorTotal(
                _userData.value.saldo,
                repository.calcularValorCartera(_cartera.value)
            ),
            _userData.value.saldoInicial
        )
    )

    fun comprarActivo(asset: Asset, quantity: Double, onResult: (OperationResult) -> Unit) {
        viewModelScope.launch {
            val total = quantity * asset.currentPrice
            val userData = _userData.value

            if (total > userData.saldo) {
                onResult(OperationResult.Error("Saldo insuficiente"))
                return@launch
            }

            val nuevoSaldo = userData.saldo - total
            repository.updateSaldo(nuevoSaldo)
            _userData.value = userData.copy(saldo = nuevoSaldo)

            val existente = _cartera.value.find { it.assetId == asset.id }
            val holding = if (existente != null) {
                val totalQty = existente.quantity + quantity
                val totalCost = existente.averagePrice * existente.quantity + total
                existente.copy(
                    quantity = totalQty,
                    averagePrice = totalCost / totalQty,
                    currentPrice = asset.currentPrice
                )
            } else {
                PortfolioHolding(
                    asset.id, asset.symbol, asset.name, asset.type,
                    quantity, asset.currentPrice, asset.currentPrice
                )
            }
            repository.upsertCartera(holding)

            val transaction = Transaction(
                id = System.currentTimeMillis().toString(),
                date = System.currentTimeMillis(),
                type = TransactionType.BUY,
                assetId = asset.id,
                symbol = asset.symbol,
                quantity = quantity,
                price = asset.currentPrice,
                total = total
            )
            repository.addTransaccion(transaction)
            cargarDatos()
            onResult(OperationResult.Success("Compra realizada con éxito", _userData.value))
        }
    }

    fun venderActivo(assetId: String, quantity: Double, currentPrice: Double, onResult: (OperationResult) -> Unit) {
        viewModelScope.launch {
            val holding = _cartera.value.find { it.assetId == assetId }
                ?: run { onResult(OperationResult.Error("No tienes este activo")); return@launch }

            if (quantity > holding.quantity) {
                onResult(OperationResult.Error("Cantidad insuficiente"))
                return@launch
            }

            val total = quantity * currentPrice
            val nuevoSaldo = _userData.value.saldo + total
            repository.updateSaldo(nuevoSaldo)
            _userData.value = _userData.value.copy(saldo = nuevoSaldo)

            if (quantity == holding.quantity) repository.deleteCartera(assetId)
            else repository.upsertCartera(
                holding.copy(quantity = holding.quantity - quantity, currentPrice = currentPrice)
            )

            val transaction = Transaction(
                id = System.currentTimeMillis().toString(),
                date = System.currentTimeMillis(),
                type = TransactionType.SELL,
                assetId = assetId,
                symbol = holding.symbol,
                quantity = quantity,
                price = currentPrice,
                total = total
            )
            repository.addTransaccion(transaction)
            cargarDatos()
            onResult(OperationResult.Success("Venta realizada con éxito", _userData.value))
        }
    }

    fun navigateToTradingFromCartera(holding: PortfolioHolding) {
        val assetFromMarket = _assets.value.find {
            it.id == holding.assetId || it.symbol == holding.symbol
        }

        if (assetFromMarket != null) {
            selectAsset(assetFromMarket)
        } else {
            val tempAsset = Asset(
                id = holding.assetId,
                symbol = holding.symbol,
                name = holding.name,
                type = holding.type,
                currentPrice = holding.currentPrice,
                priceChange24h = 0.0,
                priceChangePercent24h = 0.0
            )
            selectAsset(tempAsset)
        }
    }

    fun getPortfolioValue() = repository.calcularValorCartera(_cartera.value)
    fun getTotalValue() = repository.calcularValorTotal(_userData.value.saldo, getPortfolioValue())
    fun getProfit() = repository.calcularBeneficio(getTotalValue(), _userData.value.saldoInicial)
    fun getProfitPercent() = repository.calcularBeneficioPct(getTotalValue(), _userData.value.saldoInicial)

    fun buyAsset(asset: Asset, quantity: Double, onResult: (OperationResult) -> Unit) {
        comprarActivo(asset, quantity, onResult)
    }

    fun sellAsset(assetId: String, quantity: Double, currentPrice: Double, onResult: (OperationResult) -> Unit) {
        venderActivo(assetId, quantity, currentPrice, onResult)
    }

    fun clearSelectedAsset() {
        _selectedAsset.value = null
    }
}