package com.simutrade.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ================= DTO (RESPUESTAS API) =================

data class CoinGeckoItemDto(
    val id: String,
    val symbol: String,
    val name: String,
    @SerializedName("current_price") val currentPrice: Double,
    @SerializedName("price_change_24h") val priceChange24h: Double,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double
)

data class CoinGeckoSearchResponseDto(
    val coins: List<CoinGeckoSearchItemDto>
)

data class CoinGeckoSearchItemDto(
    val id: String,
    val symbol: String,
    val name: String
)

data class CoinGeckoMarketChartDto(
    val prices: List<List<Double>>
)

// ================= API =================

interface CoinGeckoApi {

    @GET("coins/markets")
    suspend fun getTopCoins(
        @Query("vs_currency") currency: String = "eur",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false
    ): List<CoinGeckoItemDto>

    @GET("search")
    suspend fun searchCoins(
        @Query("query") query: String
    ): CoinGeckoSearchResponseDto

    @GET("coins/{id}/market_chart")
    suspend fun getCoinMarketChart(
        @Path("id") coinId: String,
        @Query("vs_currency") currency: String = "eur",
        @Query("days") days: Int = 7,
        @Query("interval") interval: String = "daily"
    ): CoinGeckoMarketChartDto

}

// ================= CLIENT =================

object CoinGeckoClient {

    private const val BASE_URL = "https://api.coingecko.com/api/v3/"

    val api: CoinGeckoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }
}