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
}