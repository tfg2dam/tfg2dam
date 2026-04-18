package com.simutrade.screens.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.mock.MockData
import com.simutrade.data.model.Asset
import com.simutrade.data.model.OperationResult
import com.simutrade.data.model.PortfolioHolding
import com.simutrade.data.model.Transaction
import com.simutrade.data.model.TransactionType
import com.simutrade.data.model.UserData
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

    init {
        cargarDatos()
    }

    // ================= DATA =================

    fun cargarDatos() {
        viewModelScope.launch {
            _userData.value = repository.getUserData()
            _cartera.value = repository.getCartera()
            _transacciones.value = repository.getTransacciones()
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

            // 💰 actualizar saldo
            val nuevoSaldo = currentUser.saldo - total
            repository.updateSaldo(nuevoSaldo)
            _userData.value = currentUser.copy(saldo = nuevoSaldo)

            // 📦 actualizar cartera
            val existente = _cartera.value.find { it.assetId == asset.id }

            val holding = if (existente != null) {
                val totalQty = existente.quantity + quantity
                val totalCost =
                    existente.averagePrice * existente.quantity + total

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

            // 🧾 transacción
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

            // 🔄 recargar datos
            cargarDatos()

            // 📊 actualizar stats
            val totalValue = getTotalValue()
            val profit = getProfit()
            repository.updateUserStats(totalValue, profit)

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

            // 💰 saldo
            val nuevoSaldo = _userData.value.saldo + total
            repository.updateSaldo(nuevoSaldo)
            _userData.value = _userData.value.copy(saldo = nuevoSaldo)

            // 📦 cartera
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

            // 🧾 transacción
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

            // 🔄 recargar
            cargarDatos()

            // 📊 stats
            val totalValue = getTotalValue()
            val profit = getProfit()
            repository.updateUserStats(totalValue, profit)

            onResult(OperationResult.Success("Venta realizada", _userData.value))
        }
    }

    // ================= CÁLCULOS =================

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