package com.simutrade.data.repository

import com.simutrade.data.model.*
import java.util.Calendar
import kotlin.math.max
import kotlin.random.Random

// ================= MOCK DATA =================
// Datos mock utilizados para desarrollo y como fallback en caso de fallo de APIs/Firebase

object MockData {

    val mockAssets = listOf(
        Asset("aapl", "AAPL", "Apple", AssetType.STOCK, 178.45, 2.34, 1.33),
        Asset("msft", "MSFT", "Microsoft", AssetType.STOCK, 412.88, -3.12, -0.75),
        Asset("amzn", "AMZN", "Amazon", AssetType.STOCK, 178.92, 4.56, 2.61),
        Asset("tsla", "TSLA", "Tesla", AssetType.STOCK, 234.56, -8.34, -3.43),
        Asset("nvda", "NVDA", "NVIDIA", AssetType.STOCK, 523.45, 12.78, 2.50),
        Asset("googl", "GOOGL", "Google", AssetType.STOCK, 142.65, 1.87, 1.33),

        Asset("btc", "BTC", "Bitcoin", AssetType.CRYPTO, 43567.89, 1234.56, 2.92),
        Asset("eth", "ETH", "Ethereum", AssetType.CRYPTO, 2345.67, -67.89, -2.81),
        Asset("bnb", "BNB", "Binance Coin", AssetType.CRYPTO, 312.45, 8.92, 2.94),
        Asset("sol", "SOL", "Solana", AssetType.CRYPTO, 98.76, 5.43, 5.82),
        Asset("ada", "ADA", "Cardano", AssetType.CRYPTO, 0.54, -0.02, -3.57)
    )

    // ================= PRICE HISTORY =================

    fun generatePriceHistory(basePrice: Double): List<PriceHistory> {
        val history = mutableListOf<PriceHistory>()
        val calendar = Calendar.getInstance()
        var price = basePrice

        for (i in 30 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            val change = (Random.nextDouble() - 0.5) * basePrice * 0.05
            price = max(price + change, basePrice * 0.7)

            history.add(
                PriceHistory(
                    time = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}",
                    price = (price * 100).toInt() / 100.0
                )
            )

            calendar.add(Calendar.DAY_OF_YEAR, i)
        }

        return history
    }

    // ================= RANKS =================

    val ranks = listOf(
        Rank("Bronce", Double.NEGATIVE_INFINITY, "#CD7F32", "🥉", "Novato en el mundo de las inversiones"),
        Rank("Plata", 50.0, "#C0C0C0", "🥈", "Inversor con conocimientos básicos"),
        Rank("Oro", 150.0, "#FFD700", "🥇", "Trader experimentado"),
        Rank("Platino", 300.0, "#E5E4E2", "💎", "Experto en mercados financieros"),
        Rank("Diamante", 500.0, "#B9F2FF", "👑", "Maestro de las inversiones")
    )

    fun getRankFromProfit(profit: Double): Rank {
        return ranks.lastOrNull { profit >= it.minProfit } ?: ranks.first()
    }

    // ================= LEADERBOARD =================

    val mockLeaderboard = listOf(
        LeaderboardEntry("1", "InversionMaster", 892.45, "Diamante", 992.45),
        LeaderboardEntry("2", "TradingPro", 678.32, "Diamante", 778.32),
        LeaderboardEntry("3", "CryptoKing", 543.21, "Diamante", 643.21),
        LeaderboardEntry("4", "BolsaExperto", 432.10, "Platino", 532.10),
        LeaderboardEntry("5", "WallStreet99", 387.65, "Platino", 487.65),
        LeaderboardEntry("6", "DayTrader", 234.56, "Oro", 334.56),
        LeaderboardEntry("7", "StockWizard", 198.43, "Oro", 298.43),
        LeaderboardEntry("8", "CryptoNinja", 145.32, "Plata", 245.32)
    )
}