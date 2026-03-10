package com.simutrade.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.simutrade.models.*

class UserRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("simutrade_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val KEY_USER_DATA = "user_data"
        private const val INITIAL_BALANCE = 100.0
    }

    fun getUserData(): UserData {
        val jsonString = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (jsonString != null) {
            try {
                gson.fromJson(jsonString, UserData::class.java)
            } catch (e: Exception) {
                getDefaultUserData()
            }
        } else {
            getDefaultUserData()
        }
    }

    fun saveUserData(userData: UserData) {
        val jsonString = gson.toJson(userData)
        sharedPreferences.edit().putString(KEY_USER_DATA, jsonString).apply()
    }

    fun resetUserData() {
        saveUserData(getDefaultUserData())
    }

    private fun getDefaultUserData(): UserData {
        return UserData(
            username = "Usuario",
            balance = INITIAL_BALANCE,
            initialBalance = INITIAL_BALANCE,
            portfolio = mutableListOf(),
            transactions = mutableListOf()
        )
    }

    fun calculatePortfolioValue(portfolio: List<PortfolioHolding>): Double {
        return portfolio.sumOf { it.quantity * it.currentPrice }
    }

    fun calculateTotalValue(balance: Double, portfolioValue: Double): Double {
        return balance + portfolioValue
    }

    fun calculateProfit(totalValue: Double, initialBalance: Double): Double {
        return totalValue - initialBalance
    }

    fun calculateProfitPercent(totalValue: Double, initialBalance: Double): Double {
        return ((totalValue - initialBalance) / initialBalance) * 100
    }

    fun buyAsset(userData: UserData, asset: Asset, quantity: Double): OperationResult {
        val total = quantity * asset.currentPrice

        if (total > userData.balance) {
            return OperationResult.Error("Saldo insuficiente")
        }

        userData.balance -= total

        val existingHolding = userData.portfolio.find { it.assetId == asset.id }

        if (existingHolding != null) {
            val totalQuantity = existingHolding.quantity + quantity
            val totalCost = existingHolding.averagePrice * existingHolding.quantity + total
            existingHolding.quantity = totalQuantity
            existingHolding.averagePrice = totalCost / totalQuantity
            existingHolding.currentPrice = asset.currentPrice
        } else {
            userData.portfolio.add(
                PortfolioHolding(
                    assetId = asset.id,
                    symbol = asset.symbol,
                    name = asset.name,
                    type = asset.type,
                    quantity = quantity,
                    averagePrice = asset.currentPrice,
                    currentPrice = asset.currentPrice
                )
            )
        }

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
        userData.transactions.add(0, transaction)

        saveUserData(userData)
        return OperationResult.Success("Compra realizada con éxito", userData)
    }

    fun sellAsset(userData: UserData, assetId: String, quantity: Double, currentPrice: Double): OperationResult {
        val holding = userData.portfolio.find { it.assetId == assetId }
            ?: return OperationResult.Error("No tienes este activo en tu cartera")

        if (quantity > holding.quantity) {
            return OperationResult.Error("Cantidad insuficiente")
        }

        val total = quantity * currentPrice

        userData.balance += total

        if (quantity == holding.quantity) {
            userData.portfolio.remove(holding)
        } else {
            holding.quantity -= quantity
            holding.currentPrice = currentPrice
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
        userData.transactions.add(0, transaction)

        saveUserData(userData)
        return OperationResult.Success("Venta realizada con éxito", userData)
    }

    fun updatePortfolioPrices(userData: UserData, priceMap: Map<String, Double>): UserData {
        userData.portfolio.forEach { holding ->
            priceMap[holding.assetId]?.let { newPrice ->
                holding.currentPrice = newPrice
            }
        }
        return userData
    }

    fun addBalance(userData: UserData, amount: Double): UserData {
        userData.balance += amount
        saveUserData(userData)
        return userData
    }
}