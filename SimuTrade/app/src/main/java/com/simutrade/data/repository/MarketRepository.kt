package com.simutrade.data.repository

import com.simutrade.data.model.Asset
import com.simutrade.data.model.AssetType
import com.simutrade.data.remote.CoinGeckoClient
import com.simutrade.data.remote.FinnhubClient

class MarketRepository {

    companion object {
        private const val SEARCH_LIMIT = 5
    }

    // ================= DEFAULT STOCKS =================

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

    // ================= GET TOP =================

    suspend fun getTopCryptos(): List<Asset> {
        return try {
            CoinGeckoClient.api.getTopCoins().map { it.toAsset() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTopStocks(): List<Asset> {
        return defaultStocks.mapNotNull { (symbol, name) ->
            try {
                val quote = FinnhubClient.api.getQuote(symbol)
                if (quote.currentPrice > 0) {
                    quote.toAsset(symbol, name)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // ================= SEARCH =================

    suspend fun searchAssets(query: String): List<Asset> {
        val results = mutableListOf<Asset>()

        // CRYPTO
        try {
            val cryptoResults = CoinGeckoClient.api
                .searchCoins(query)
                .coins
                .take(SEARCH_LIMIT)

            cryptoResults.forEach {
                results.add(
                    Asset(
                        id = it.id,
                        symbol = it.symbol.uppercase(),
                        name = it.name,
                        type = AssetType.CRYPTO,
                        currentPrice = 0.0,
                        priceChange24h = 0.0,
                        priceChangePercent24h = 0.0
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // STOCKS
        try {
            val stockResults = FinnhubClient.api
                .searchSymbols(query)
                .result
                .take(SEARCH_LIMIT)

            stockResults.forEach { item ->
                val quote = try {
                    FinnhubClient.api.getQuote(item.symbol)
                } catch (e: Exception) {
                    null
                }

                results.add(
                    Asset(
                        id = item.symbol,
                        symbol = item.symbol,
                        name = item.description,
                        type = AssetType.STOCK,
                        currentPrice = quote?.currentPrice ?: 0.0,
                        priceChange24h = quote?.priceChange ?: 0.0,
                        priceChangePercent24h = quote?.priceChangePercentage ?: 0.0
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    // ================= HISTORY =================

    suspend fun getAssetHistory(
        asset: Asset,
        period: String = "7d"
    ): List<Pair<Long, Double>> {
        return try {

            // CRYPTO (CoinGecko)
            if (asset.type == AssetType.CRYPTO) {

                val (days, interval) = when (period) {
                    "1h"  -> 1 to "minute"
                    "1d"  -> 1 to "hourly"
                    "7d"  -> 7 to "daily"
                    "30d" -> 30 to "daily"
                    "1A"  -> 365 to "daily"
                    else  -> 7 to "daily"
                }

                val history = CoinGeckoClient.api.getCoinMarketChart(
                    coinId = asset.id,
                    days = days,
                    interval = interval
                )

                val prices = if (period == "1h") {
                    history.prices.takeLast(60)
                } else {
                    history.prices
                }

                prices.map { it[0].toLong() to it[1] }
            }

            // STOCKS (Finnhub)
            else {

                val now = System.currentTimeMillis() / 1000

                val (from, resolution) = when (period) {
                    "1h"  -> now - 3600 to "1"
                    "1d"  -> now - 86400 to "5"
                    "7d"  -> now - 604800 to "15"
                    "30d" -> now - 2592000 to "D"
                    "1A"  -> now - 31536000 to "W"
                    else  -> now - 604800 to "60"
                }

                val response = FinnhubClient.api.getStockCandles(
                    symbol = asset.symbol,
                    resolution = resolution,
                    from = from,
                    to = now
                )

                if (response.status == "ok") {
                    response.timestamps.zip(response.closePrices)
                } else {
                    emptyList()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            generateSimulatedHistory(asset.currentPrice, period)
        }
    }

    // ================= MAPPERS =================

    private fun com.simutrade.data.remote.CoinGeckoItemDto.toAsset(): Asset {
        return Asset(
            id = id,
            symbol = symbol.uppercase(),
            name = name,
            type = AssetType.CRYPTO,
            currentPrice = currentPrice,
            priceChange24h = priceChange24h,
            priceChangePercent24h = priceChangePercentage24h
        )
    }

    private fun com.simutrade.data.remote.FinnhubQuoteDto.toAsset(
        symbol: String,
        name: String
    ): Asset {
        return Asset(
            id = symbol,
            symbol = symbol,
            name = name,
            type = AssetType.STOCK,
            currentPrice = currentPrice,
            priceChange24h = priceChange,
            priceChangePercent24h = priceChangePercentage
        )
    }

    // ================= FALLBACK =================

    private fun generateSimulatedHistory(
        currentPrice: Double,
        period: String
    ): List<Pair<Long, Double>> {

        val now = System.currentTimeMillis()

        val (points, intervalMs) = when (period) {
            "1h"  -> 60 to 60_000L
            "1d"  -> 24 to 3_600_000L
            "7d"  -> 8 to 86_400_000L
            "30d" -> 30 to 86_400_000L
            "1A"  -> 52 to 604_800_000L
            else  -> 8 to 86_400_000L
        }

        return (0 until points).map { i ->
            val timestamp = now - (points - 1 - i) * intervalMs
            val progress = i.toDouble() / (points - 1)
            val price = currentPrice * (0.95 + 0.1 * progress)
            timestamp to price
        }
    }
}