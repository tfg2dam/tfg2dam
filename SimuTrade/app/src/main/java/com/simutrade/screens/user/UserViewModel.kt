package com.simutrade.screens.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.*
import com.simutrade.data.repository.UserRepository
import com.simutrade.screens.rankings.RankUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    // ================= STATE =================

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    private val _portfolio = MutableStateFlow<List<PortfolioHolding>>(emptyList())
    val portfolio: StateFlow<List<PortfolioHolding>> = _portfolio.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _currentRank = MutableStateFlow<Rank?>(null)
    val currentRank: StateFlow<Rank?> = _currentRank.asStateFlow()

    init {
        loadData()
    }

    // ================= DATA =================

    fun loadData() {
        viewModelScope.launch {

            val user = repository.getUserData()
            val portfolioData = repository.getPortfolio()
            val transactionsData = repository.getTransactions()

            _userData.value = user
            _portfolio.value = portfolioData
            _transactions.value = transactionsData

            val profitTrading = calculateTradingProfit(
                balance = user.balance,
                initialBalance = user.initialBalance,
                bonusBalance = user.bonusBalance,
                portfolio = portfolioData
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
                onResult(OperationResult.Error("Invalid quantity"))
                return@launch
            }

            val user = _userData.value
            val total = quantity * asset.currentPrice

            if (total > user.balance) {
                onResult(OperationResult.Error("Insufficient balance"))
                return@launch
            }

            // 🔹 actualizar balance local
            val newBalance = user.balance - total
            repository.updateBalance(newBalance)
            _userData.value = user.copy(balance = newBalance)

            // 🔹 actualizar portfolio local
            val existing = _portfolio.value.find { it.assetId == asset.id }

            val updatedHolding = if (existing != null) {
                val totalQty = existing.quantity + quantity
                val totalCost = existing.averagePrice * existing.quantity + total

                existing.copy(
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

            repository.upsertPortfolio(updatedHolding)

            _portfolio.value = _portfolio.value
                .filter { it.assetId != asset.id } + updatedHolding

            // 🔹 transacción
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

            repository.addTransaction(transaction)
            _transactions.value = listOf(transaction) + _transactions.value

            // 🔹 stats
            updateStats()

            onResult(OperationResult.Success("Purchase completed", _userData.value))
        }
    }

    fun sellAsset(
        assetId: String,
        quantity: Double,
        currentPrice: Double,
        onResult: (OperationResult) -> Unit
    ) {
        viewModelScope.launch {

            val holding = _portfolio.value.find { it.assetId == assetId }
                ?: run {
                    onResult(OperationResult.Error("Asset not owned"))
                    return@launch
                }

            if (quantity <= 0 || quantity > holding.quantity) {
                onResult(OperationResult.Error("Invalid quantity"))
                return@launch
            }

            val total = quantity * currentPrice

            // 🔹 actualizar balance
            val newBalance = _userData.value.balance + total
            repository.updateBalance(newBalance)
            _userData.value = _userData.value.copy(balance = newBalance)

            // 🔹 actualizar portfolio
            if (quantity == holding.quantity) {
                repository.deletePortfolio(assetId)
                _portfolio.value = _portfolio.value.filter { it.assetId != assetId }
            } else {
                val updated = holding.copy(
                    quantity = holding.quantity - quantity,
                    currentPrice = currentPrice
                )

                repository.upsertPortfolio(updated)

                _portfolio.value = _portfolio.value
                    .filter { it.assetId != assetId } + updated
            }

            // 🔹 transacción
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

            repository.addTransaction(transaction)
            _transactions.value = listOf(transaction) + _transactions.value

            // 🔹 stats
            updateStats()

            onResult(OperationResult.Success("Sale completed", _userData.value))
        }
    }

    // ================= STATS =================

    private fun updateStats() {
        val totalTrading = getTradingTotalValue()
        val profitTrading = getTradingProfit()

        viewModelScope.launch {
            repository.updateUserStats(totalTrading, profitTrading)
        }

        _currentRank.value = RankUtils.getRankFromProfit(profitTrading)
    }

    // ================= CALCULATIONS =================

    fun getPortfolioValue(): Double =
        repository.calculatePortfolioValue(_portfolio.value)

    fun getTotalValue(): Double =
        repository.calculateTotalValue(_userData.value.balance, getPortfolioValue())

    fun getProfit(): Double =
        repository.calculateProfit(getTotalValue(), _userData.value.initialBalance)

    fun getProfitPercent(): Double =
        repository.calculateProfitPercentage(getTotalValue(), _userData.value.initialBalance)

    private fun getTradingTotalValue(): Double {
        val user = _userData.value
        val tradingBalance = user.balance - user.bonusBalance
        return tradingBalance + getPortfolioValue()
    }

    fun getTradingProfit(): Double {
        val user = _userData.value
        return repository.calculateProfit(getTradingTotalValue(), user.initialBalance)
    }

    private fun calculateTradingProfit(
        balance: Double,
        initialBalance: Double,
        bonusBalance: Double,
        portfolio: List<PortfolioHolding>
    ): Double {
        val tradingBalance = balance - bonusBalance
        val portfolioValue = portfolio.sumOf { it.quantity * it.currentPrice }
        return tradingBalance + portfolioValue - initialBalance
    }
}