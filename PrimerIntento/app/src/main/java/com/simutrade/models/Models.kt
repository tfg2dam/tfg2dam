// ============================================
// MODELOS DE DATOS - SimuTrade
// Archivo: app/src/main/java/com/simutrade/models/Models.kt
// ============================================

package com.simutrade.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi // Modelo de activo (acción o criptomoneda)
@Serializable
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

@InternalSerializationApi // Historial de precios para gráficos
@Serializable
data class PriceHistory(
    val time: String,
    val price: Double
)

@InternalSerializationApi // Posición en la cartera del usuario
@Serializable
data class PortfolioHolding(
    val assetId: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    var quantity: Double,
    var averagePrice: Double,
    var currentPrice: Double
)

@InternalSerializationApi // Transacción (compra/venta)
@Serializable
data class Transaction(
    val id: String,
    val date: Long, // timestamp
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

@InternalSerializationApi // Datos del usuario
@Serializable
data class UserData(
    var username: String = "Usuario",
    var balance: Double = 100.0,
    val initialBalance: Double = 100.0,
    val portfolio: MutableList<PortfolioHolding> = mutableListOf(),
    val transactions: MutableList<Transaction> = mutableListOf()
)

// Sistema de rangos
data class Rank(
    val name: String,
    val minProfit: Double,
    val color: String,
    val icon: String,
    val description: String
)

@InternalSerializationApi // Entrada de la tabla de clasificación
@Serializable
data class LeaderboardEntry(
    val id: String,
    val username: String,
    val profit: Double,
    val rank: String,
    val portfolioValue: Double
)

// Resultado de operación
sealed class OperationResult {
    data class Success @OptIn(InternalSerializationApi::class) constructor(val message: String, val userData: UserData) : OperationResult()
    data class Error(val message: String) : OperationResult()
}
