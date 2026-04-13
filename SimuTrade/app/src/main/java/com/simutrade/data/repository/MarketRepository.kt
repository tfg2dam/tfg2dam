package com.simutrade.data.repository

import com.simutrade.data.model.Asset
import com.simutrade.data.model.AssetType
import com.simutrade.data.remote.CoinGeckoClient
import com.simutrade.data.remote.FinnhubClient

class MarketRepository {

    // Acciones fijas que queremos mostrar
    private val defaultStocks = listOf(
        "AAPL" to "Apple",
        "MSFT" to "Microsoft",
        "GOOGL" to "Alphabet",
        "AMZN" to "Amazon",
        "TSLA" to "Tesla",
        "META" to "Meta",
        "NVDA" to "NVIDIA",
        "NFLX" to "Netflix",
        "BRK.B" to "Berkshire",
        "JPM" to "JPMorgan"
    )

    suspend fun getTopCryptos(): List<Asset> {
        return try {
            CoinGeckoClient.api.getTopCoins().map { coin ->
                Asset(
                    id = coin.id,
                    symbol = coin.symbol.uppercase(),
                    name = coin.name,
                    type = AssetType.CRYPTO,
                    currentPrice = coin.currentPrice,
                    priceChange24h = coin.priceChange24h,
                    priceChangePercent24h = coin.priceChangePercent24h
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopStocks(): List<Asset> {
        return defaultStocks.mapNotNull { (symbol, name) ->
            try {
                val quote = FinnhubClient.api.getQuote(symbol)
                if (quote.currentPrice > 0) {
                    Asset(
                        id = symbol,
                        symbol = symbol,
                        name = name,
                        type = AssetType.STOCK,
                        currentPrice = quote.currentPrice,
                        priceChange24h = quote.change,
                        priceChangePercent24h = quote.changePercent
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun searchAssets(query: String): List<Asset> {
        val results = mutableListOf<Asset>()

        // Buscar criptos
        try {
            val cryptoResults = CoinGeckoClient.api.searchCoins(query).coins.take(5)
            cryptoResults.forEach { coin ->
                results.add(
                    Asset(
                        id = coin.id,
                        symbol = coin.symbol.uppercase(),
                        name = coin.name,
                        type = AssetType.CRYPTO,
                        currentPrice = 0.0,
                        priceChange24h = 0.0,
                        priceChangePercent24h = 0.0
                    )
                )
            }
        } catch (e: Exception) { }

        // Buscar acciones
        try {
            val stockResults = FinnhubClient.api.searchSymbol(query).result.take(5)
            stockResults.forEach { item ->
                val quote = try { FinnhubClient.api.getQuote(item.symbol) } catch (e: Exception) { null }
                results.add(
                    Asset(
                        id = item.symbol,
                        symbol = item.symbol,
                        name = item.description,
                        type = AssetType.STOCK,
                        currentPrice = quote?.currentPrice ?: 0.0,
                        priceChange24h = quote?.change ?: 0.0,
                        priceChangePercent24h = quote?.changePercent ?: 0.0
                    )
                )
            }
        } catch (e: Exception) { }

        return results
    }

    suspend fun getAssetHistory(asset: Asset, period: String = "7d"): List<Pair<Long, Double>> {
        return try {
            if (asset.type == AssetType.CRYPTO) {
                val (days, interval) = when (period) {
                    "1h"  -> 1 to "minute"
                    "1d"  -> 1 to "hourly"
                    "7d"  -> 7 to "daily"
                    "30d" -> 30 to "daily"
                    "1A"  -> 365 to "daily"
                    else  -> 7 to "daily"
                }
                val history = CoinGeckoClient.api.getCoinHistory(
                    coinId = asset.id,
                    days = days,
                    interval = interval
                )
                // Para 1h cogemos solo las últimas 60 entradas
                val prices = if (period == "1h") history.prices.takeLast(60) else history.prices
                prices.map { it[0].toLong() to it[1] }
            } else {
                generateSimulatedHistory(asset.currentPrice, asset.priceChangePercent24h, period)
            }
        } catch (e: Exception) {
            generateSimulatedHistory(asset.currentPrice, asset.priceChangePercent24h, period)
        }
    }

    private fun generateSimulatedHistory(
        currentPrice: Double,
        changePercent24h: Double,
        period: String = "7d"
    ): List<Pair<Long, Double>> {
        val now = System.currentTimeMillis()
        val (points, intervalMs) = when (period) {
            "1h"  -> 60 to (60 * 1000L)           // 60 puntos cada minuto
            "1d"  -> 24 to (60 * 60 * 1000L)       // 24 puntos cada hora
            "7d"  -> 8  to (24 * 60 * 60 * 1000L)  // 8 puntos cada día
            "30d" -> 30 to (24 * 60 * 60 * 1000L)  // 30 puntos cada día
            "1A"  -> 52 to (7 * 24 * 60 * 60 * 1000L) // 52 puntos cada semana
            else  -> 8  to (24 * 60 * 60 * 1000L)
        }

        val multiplier = when (period) {
            "1h"  -> 0.1
            "1d"  -> 1.0
            "7d"  -> 7.0
            "30d" -> 30.0
            "1A"  -> 365.0
            else  -> 7.0
        }

        val priceStart = currentPrice / (1 + (changePercent24h / 100) * multiplier / 30)

        return (0 until points).map { i ->
            val timestamp = now - (points - 1 - i) * intervalMs
            val progress = i.toDouble() / (points - 1)
            val basePrice = priceStart + (currentPrice - priceStart) * progress
            val variation = basePrice * 0.008 * (Math.random() - 0.5)
            timestamp to (basePrice + variation)
        }
    }
}