package com.simutrade.data.repository

import com.simutrade.data.model.Asset
import com.simutrade.data.model.AssetType
import com.simutrade.data.model.LeaderboardEntry
import com.simutrade.data.model.PriceHistory
import com.simutrade.data.model.Rank
import java.util.Calendar
import kotlin.math.max

object MockData {

    val mockAssets = listOf(
        Asset(
            id = "aapl",
            symbol = "AAPL",
            name = "Apple",
            type = AssetType.STOCK,
            currentPrice = 178.45,
            priceChange24h = 2.34,
            priceChangePercent24h = 1.33
        ),
        Asset(
            id = "msft",
            symbol = "MSFT",
            name = "Microsoft",
            type = AssetType.STOCK,
            currentPrice = 412.88,
            priceChange24h = -3.12,
            priceChangePercent24h = -0.75
        ),
        Asset(
            id = "amzn",
            symbol = "AMZN",
            name = "Amazon",
            type = AssetType.STOCK,
            currentPrice = 178.92,
            priceChange24h = 4.56,
            priceChangePercent24h = 2.61
        ),
        Asset(
            id = "tsla",
            symbol = "TSLA",
            name = "Tesla",
            type = AssetType.STOCK,
            currentPrice = 234.56,
            priceChange24h = -8.34,
            priceChangePercent24h = -3.43
        ),
        Asset(
            id = "nvda",
            symbol = "NVDA",
            name = "NVIDIA",
            type = AssetType.STOCK,
            currentPrice = 523.45,
            priceChange24h = 12.78,
            priceChangePercent24h = 2.50
        ),
        Asset(
            id = "googl",
            symbol = "GOOGL",
            name = "Google",
            type = AssetType.STOCK,
            currentPrice = 142.65,
            priceChange24h = 1.87,
            priceChangePercent24h = 1.33
        ),
        Asset(
            id = "btc",
            symbol = "BTC",
            name = "Bitcoin",
            type = AssetType.CRYPTO,
            currentPrice = 43567.89,
            priceChange24h = 1234.56,
            priceChangePercent24h = 2.92
        ),
        Asset(
            id = "eth",
            symbol = "ETH",
            name = "Ethereum",
            type = AssetType.CRYPTO,
            currentPrice = 2345.67,
            priceChange24h = -67.89,
            priceChangePercent24h = -2.81
        ),
        Asset(
            id = "bnb",
            symbol = "BNB",
            name = "Binance Coin",
            type = AssetType.CRYPTO,
            currentPrice = 312.45,
            priceChange24h = 8.92,
            priceChangePercent24h = 2.94
        ),
        Asset(
            id = "sol",
            symbol = "SOL",
            name = "Solana",
            type = AssetType.CRYPTO,
            currentPrice = 98.76,
            priceChange24h = 5.43,
            priceChangePercent24h = 5.82
        ),
        Asset(
            id = "ada",
            symbol = "ADA",
            name = "Cardano",
            type = AssetType.CRYPTO,
            currentPrice = 0.54,
            priceChange24h = -0.02,
            priceChangePercent24h = -3.57
        )
    )

    fun generatePriceHistory(basePrice: Double): List<PriceHistory> {
        val history = mutableListOf<PriceHistory>()
        val calendar = Calendar.getInstance()
        var price = basePrice

        for (i in 30 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val change = (Math.random() - 0.5) * basePrice * 0.05
            price = max(price + change, basePrice * 0.7)
            history.add(
                PriceHistory(
                    time = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}",
                    price = Math.round(price * 100.0) / 100.0
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, i)
        }

        return history
    }

    val ranks = listOf(
        Rank(
            "Bronce",
            Double.NEGATIVE_INFINITY,
            "#CD7F32",
            "🥉",
            "Novato en el mundo de las inversiones"
        ),
        Rank("Plata", 50.0, "#C0C0C0", "🥈", "Inversor con conocimientos básicos"),
        Rank("Oro", 150.0, "#FFD700", "🥇", "Trader experimentado"),
        Rank("Platino", 300.0, "#E5E4E2", "💎", "Experto en mercados financieros"),
        Rank("Diamante", 500.0, "#B9F2FF", "👑", "Maestro de las inversiones")
    )

    fun getRankFromProfit(profit: Double): Rank {
        return ranks.lastOrNull { profit >= it.minProfit } ?: ranks.first()
    }

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