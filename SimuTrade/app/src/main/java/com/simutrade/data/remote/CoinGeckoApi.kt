package com.simutrade.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class CoinGeckoItem(
    val id: String,
    val symbol: String,
    val name: String,
    @SerializedName("current_price") val currentPrice: Double,
    @SerializedName("price_change_24h") val priceChange24h: Double,
    @SerializedName("price_change_percentage_24h") val priceChangePercent24h: Double
)

interface CoinGeckoApi {
    @GET("coins/markets")
    suspend fun getTopCoins(
        @Query("vs_currency") currency: String = "eur",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false
    ): List<CoinGeckoItem>

    @GET("search")
    suspend fun searchCoins(
        @Query("query") query: String
    ): CoinGeckoSearchResponse
}

data class CoinGeckoSearchResponse(
    val coins: List<CoinGeckoSearchItem>
)

data class CoinGeckoSearchItem(
    val id: String,
    val symbol: String,
    val name: String
)

object CoinGeckoClient {
    val api: CoinGeckoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }
}