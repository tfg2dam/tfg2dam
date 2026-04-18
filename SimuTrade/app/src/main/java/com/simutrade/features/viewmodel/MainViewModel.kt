package com.simutrade.features.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.mock.MockData
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

    // ================= STATE =================

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    private val _cartera = MutableStateFlow<List<PortfolioHolding>>(emptyList())
    val cartera: StateFlow<List<PortfolioHolding>> = _cartera.asStateFlow()

    private val _transacciones = MutableStateFlow<List<Transaction>>(emptyList())
    val transacciones: StateFlow<List<Transaction>> = _transacciones.asStateFlow()

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
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


    // ================= INIT =================

    init {
        cargarDatos()
        cargarAssets()
    }


    // ================= DATA =================

    fun cargarDatos() {
        viewModelScope.launch {
            _userData.value = repository.getUserData()
            _cartera.value = repository.getCartera()
            _transacciones.value = repository.getTransacciones()

            verificarRetosAutomaticos()
        }
    }

    fun cargarAssets() {
        viewModelScope.launch {
            try {
                val cryptos = marketRepository.getTopCryptos()
                val stocks = marketRepository.getTopStocks()
                _assets.value = cryptos + stocks
            } catch (e: Exception) {
                println("Error cargando assets: ${e.message}")
            }
        }
    }

    fun cargarLeaderboard() {
        viewModelScope.launch {
            _isLoadingLeaderboard.value = true
            try {
                _leaderboard.value = repository.getLeaderboard()
            } catch (e: Exception) {
                println("Error leaderboard: ${e.message}")
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
                println("Error retos: ${e.message}")
            } finally {
                _isLoadingRetos.value = false
            }
        }
    }


    // ================= NAVIGATION =================

    fun navigateTo(page: String) {
        _currentPage.value = page
    }

    fun selectAsset(asset: Asset) {
        _selectedAsset.value = asset
        navigateTo("trading")
    }

    fun clearSelectedAsset() {
        _selectedAsset.value = null
    }

    fun navigateToTradingFromCartera(holding: PortfolioHolding) {
        val assetFromMarket = _assets.value.find {
            it.id == holding.assetId || it.symbol == holding.symbol
        }

        selectAsset(
            assetFromMarket ?: Asset(
                id = holding.assetId,
                symbol = holding.symbol,
                name = holding.name,
                type = holding.type,
                currentPrice = holding.currentPrice,
                priceChange24h = 0.0,
                priceChangePercent24h = 0.0
            )
        )
    }


    // ================= TRADING =================

    fun buyAsset(
        asset: Asset,
        quantity: Double,
        onResult: (OperationResult) -> Unit
    ) {
        viewModelScope.launch {

            if (quantity <= 0 || quantity.isNaN()) {
                onResult(OperationResult.Error("Cantidad inválida"))
                return@launch
            }

            val total = quantity * asset.currentPrice
            val currentUser = _userData.value

            if (total > currentUser.saldo) {
                onResult(OperationResult.Error("Saldo insuficiente"))
                return@launch
            }

            val nuevoSaldo = currentUser.saldo - total
            repository.updateSaldo(nuevoSaldo)
            _userData.value = currentUser.copy(saldo = nuevoSaldo)

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
                    asset.id,
                    asset.symbol,
                    asset.name,
                    asset.type,
                    quantity,
                    asset.currentPrice,
                    asset.currentPrice
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
            verificarRetosAutomaticos()

            val totalValue = getTotalValue()
            val profit = getProfit()
            repository.updateUserStats(totalValue, profit)

            onResult(OperationResult.Success("Compra realizada con éxito", _userData.value))
        }
    }

    fun sellAsset(
        assetId: String,
        quantity: Double,
        currentPrice: Double,
        onResult: (OperationResult) -> Unit
    ) {
        viewModelScope.launch {

            val holding = _cartera.value.find { it.assetId == assetId }
                ?: run {
                    onResult(OperationResult.Error("No tienes este activo"))
                    return@launch
                }

            if (quantity <= 0 || quantity > holding.quantity) {
                onResult(OperationResult.Error("Cantidad inválida"))
                return@launch
            }

            val total = quantity * currentPrice
            val nuevoSaldo = _userData.value.saldo + total

            repository.updateSaldo(nuevoSaldo)
            _userData.value = _userData.value.copy(saldo = nuevoSaldo)

            if (quantity == holding.quantity) {
                repository.deleteCartera(assetId)
            } else {
                repository.upsertCartera(
                    holding.copy(
                        quantity = holding.quantity - quantity,
                        currentPrice = currentPrice
                    )
                )
            }

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
            verificarRetosAutomaticos()

            val totalValue = getTotalValue()
            val profit = getProfit()
            repository.updateUserStats(totalValue, profit)

            onResult(OperationResult.Success("Venta realizada con éxito", _userData.value))
        }
    }


    // ================= RETOS =================

    fun getRetosDelDia(): List<Reto> {
        val dia = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

        return listOf(
            Reto("reto_${dia}_1", "Primera operación", "Realiza una compra o venta hoy", "📈", 2.0),
            Reto("reto_${dia}_2", "Diversifica", "Ten al menos 2 activos en cartera", "🎯", 3.0),
            Reto("reto_${dia}_3", "Inversor activo", "Opera con un activo de cada tipo", "⚡", 5.0)
        )
    }

    fun verificarRetosAutomaticos() {
        val inicioDia = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val transaccionesHoy = _transacciones.value.filter { it.date >= inicioDia }
        val retos = getRetosDelDia()
        val completados = _retosData.value.retosCompletados

        retos.forEach { reto ->
            if (reto.id in completados) return@forEach

            val completado = when {
                reto.id.endsWith("_1") -> transaccionesHoy.isNotEmpty()
                reto.id.endsWith("_2") -> _cartera.value.map { it.assetId }.distinct().size >= 2
                reto.id.endsWith("_3") -> {
                    val tipos = _cartera.value.map { it.type }.toSet()
                    tipos.contains(AssetType.STOCK) && tipos.contains(AssetType.CRYPTO)
                }
                else -> false
            }

            if (completado) {
                completarReto(reto.id, reto.recompensa)
            }
        }
    }

    fun completarReto(retoId: String, recompensa: Double) {
        viewModelScope.launch {

            val nuevos = _retosData.value.retosCompletados + retoId

            val nuevaData = _retosData.value.copy(
                retosCompletados = nuevos,
                ultimaVez = System.currentTimeMillis()
            )

            repository.saveRetosData(nuevaData)
            _retosData.value = nuevaData

            val nuevoSaldo = _userData.value.saldo + recompensa
            repository.updateSaldo(nuevoSaldo)
            _userData.value = _userData.value.copy(saldo = nuevoSaldo)
        }
    }


    // ================= CALCULATIONS =================

    fun getPortfolioValue() =
        repository.calcularValorCartera(_cartera.value)

    fun getTotalValue() =
        repository.calcularValorTotal(_userData.value.saldo, getPortfolioValue())

    fun getProfit() =
        repository.calcularBeneficio(getTotalValue(), _userData.value.saldoInicial)

    fun getProfitPercent() =
        repository.calcularBeneficioPct(getTotalValue(), _userData.value.saldoInicial)

    fun getCurrentRank() =
        MockData.getRankFromProfit(getProfit())
}