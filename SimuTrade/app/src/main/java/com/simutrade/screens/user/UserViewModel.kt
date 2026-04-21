package com.simutrade.screens.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.screens.rankings.RankUtils
import com.simutrade.data.model.*
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    // ================= STATE =================

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    private val _cartera = MutableStateFlow<List<PortfolioHolding>>(emptyList())
    val cartera: StateFlow<List<PortfolioHolding>> = _cartera.asStateFlow()

    private val _transacciones = MutableStateFlow<List<Transaction>>(emptyList())
    val transacciones: StateFlow<List<Transaction>> = _transacciones.asStateFlow()

    private val _currentRank = MutableStateFlow<Rank?>(null)
    val currentRank: StateFlow<Rank?> = _currentRank.asStateFlow()

    init {
        cargarDatos()
    }

    // ================= DATA =================

    fun cargarDatos() {
        viewModelScope.launch {

            val user = repository.getUserData()
            val carteraData = repository.getCartera()
            val transaccionesData = repository.getTransacciones()

            _userData.value = user
            _cartera.value = carteraData
            _transacciones.value = transaccionesData

            // El rango usa SOLO el beneficio de trading (sin bonus de retos)
            val profitTrading = calcularBeneficioTrading(
                saldo = user.saldo,
                saldoInicial = user.saldoInicial,
                saldoBonus = user.saldoBonus,
                cartera = carteraData
            )

            _currentRank.value = RankUtils.getRankFromProfit(profitTrading)
        }
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
                    assetId = asset.id,
                    symbol = asset.symbol,
                    name = asset.name,
                    type = asset.type,
                    quantity = quantity,
                    averagePrice = asset.currentPrice,
                    currentPrice = asset.currentPrice
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

            // updateUserStats usa SOLO el beneficio de trading
            val profitTrading = getProfitTrading()
            val totalTrading = getTotalValueTrading()
            repository.updateUserStats(totalTrading, profitTrading)

            onResult(OperationResult.Success("Compra realizada", _userData.value))
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

            // updateUserStats usa SOLO el beneficio de trading
            val profitTrading = getProfitTrading()
            val totalTrading = getTotalValueTrading()
            repository.updateUserStats(totalTrading, profitTrading)

            onResult(OperationResult.Success("Venta realizada", _userData.value))
        }
    }

    // ================= CÁLCULOS =================

    fun getPortfolioValue() =
        repository.calcularValorCartera(_cartera.value)

    // Valor total incluyendo bonus de retos (para mostrar en UI)
    fun getTotalValue() =
        repository.calcularValorTotal(_userData.value.saldo, getPortfolioValue())

    // Beneficio incluyendo bonus de retos (para mostrar en UI)
    fun getProfit() =
        repository.calcularBeneficio(getTotalValue(), _userData.value.saldoInicial)

    fun getProfitPercent() =
        repository.calcularBeneficioPct(getTotalValue(), _userData.value.saldoInicial)

    // Valor total SOLO de trading (sin contar bonus de retos)
    private fun getTotalValueTrading(): Double {
        val user = _userData.value
        val saldoTrading = user.saldo - user.saldoBonus
        return saldoTrading + getPortfolioValue()
    }

    fun getProfitTrading(): Double {
        val user = _userData.value
        return repository.calcularBeneficio(getTotalValueTrading(), user.saldoInicial)
    }

    // Cálculo interno usado en cargarDatos
    private fun calcularBeneficioTrading(
        saldo: Double,
        saldoInicial: Double,
        saldoBonus: Double,
        cartera: List<PortfolioHolding>
    ): Double {
        val saldoTrading = saldo - saldoBonus
        val valorCartera = cartera.sumOf { it.quantity * it.currentPrice }
        val total = saldoTrading + valorCartera
        return total - saldoInicial
    }
}