package com.simutrade.models

data class Asset(
    val id: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    var currentPrice: Double,
    val priceChange24h: Double,
    val priceChangePercent24h: Double
)

enum class AssetType {
    STOCK,
    CRYPTO
}

data class PriceHistory(
    val time: String,
    val price: Double
)

data class PortfolioHolding(
    val assetId: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    var quantity: Double,
    var averagePrice: Double,
    var currentPrice: Double
)

data class Transaction(
    val id: String,
    val date: Long,
    val type: TransactionType,
    val assetId: String,
    val symbol: String,
    val quantity: Double,
    val price: Double,
    val total: Double
)

enum class TransactionType {
    BUY,
    SELL
}

data class UserData(
    var username: String = "Usuario",
    var balance: Double = 100.0,
    val initialBalance: Double = 100.0,
    val portfolio: MutableList<PortfolioHolding> = mutableListOf(),
    val transactions: MutableList<Transaction> = mutableListOf()
)

data class Rank(
    val name: String,
    val minProfit: Double,
    val color: String,
    val icon: String,
    val description: String
)

data class LeaderboardEntry(
    val id: String,
    val username: String,
    val profit: Double,
    val rank: String,
    val portfolioValue: Double
)

sealed class OperationResult {
    data class Success(val message: String, val userData: UserData) : OperationResult()
    data class Error(val message: String) : OperationResult()
}