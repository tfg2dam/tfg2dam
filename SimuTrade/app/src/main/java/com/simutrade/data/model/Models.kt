package com.simutrade.data.model

import com.simutrade.screens.rankings.RankIcon

// ================= ASSETS =================

data class Asset(
    val id: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    val currentPrice: Double,
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

// ================= PORTFOLIO =================

data class PortfolioHolding(
    val assetId: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double
)

// ================= TRANSACTIONS =================

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

// ================= USER =================

data class UserData(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val balance: Double = 100.0,
    val initialBalance: Double = 100.0,
    val bonusBalance: Double = 0.0,
    val rankId: String = "bronce",
    val createdAt: Long = 0L,
    val lastLogin: Long = 0L
)

// ================= RANKING =================

data class Rank(
    val name: String,
    val minProfit: Double,
    val color: String,
    val icon: RankIcon,
    val description: String
)

data class LeaderboardEntry(
    val id: String,
    val username: String,
    val profit: Double,
    val rank: String,
    val portfolioValue: Double,
    val balance: Double = 0.0,
    val totalValue: Double = 0.0
)

// ================= RETOS =================

data class ChallengesData(
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,

    val lastTime: Long = 0L,

    val completedChallenges: List<String> = emptyList(),

    val dailyChallenges: List<String> = emptyList(),

    val currentDay: Long = 0L // CORREGIDO (antes era Int)
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val reward: Double
)

// ================= COMMON =================

sealed class OperationResult {

    data class Success(
        val message: String,
        val userData: UserData
    ) : OperationResult()

    data class Error(
        val message: String
    ) : OperationResult()
}