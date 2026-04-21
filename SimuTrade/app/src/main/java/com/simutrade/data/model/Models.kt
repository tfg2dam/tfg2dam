package com.simutrade.data.model

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
    val idUsuario: String = "",
    val nombreUsuario: String = "",
    val email: String = "",
    val saldo: Double = 100.0,
    val saldoInicial: Double = 100.0,
    val saldoBonus: Double = 0.0,
    val idRango: String = "bronce",
    val creadoEn: Long = 0L,
    val ultimoLogin: Long = 0L
)

// ================= RANKING =================

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

// ================= RETOS =================

data class RetosData(
    val rachaActual: Int = 0,
    val rachaMaxima: Int = 0,

    val ultimaVez: Long = 0L,

    val retosCompletados: List<String> = emptyList(),

    val retosDelDia: List<String> = emptyList(),

    val diaActual: Long = 0L // 🔥 CORREGIDO (antes era Int)
)

data class Reto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val emoji: String,
    val recompensa: Double
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