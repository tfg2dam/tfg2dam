package com.simutrade.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ================= DTO =================

data class FinnhubQuoteDto(
    @SerializedName("c") val currentPrice: Double,
    @SerializedName("d") val change: Double,
    @SerializedName("dp") val changePercent: Double
)

data class FinnhubSearchResponseDto(
    val result: List<FinnhubSearchItemDto>
)

data class FinnhubSearchItemDto(
    val symbol: String,
    val description: String
)

data class FinnhubCandleDto(
    @SerializedName("c") val close: List<Double>,
    @SerializedName("t") val timestamps: List<Long>,
    @SerializedName("s") val status: String
)

// ================= API =================

interface FinnhubApi {

    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String = FinnhubClient.API_KEY
    ): FinnhubQuoteDto

    @GET("search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("token") token: String = FinnhubClient.API_KEY
    ): FinnhubSearchResponseDto

    @GET("stock/candle")
    suspend fun getStockHistory(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String = "D",
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") token: String = FinnhubClient.API_KEY
    ): FinnhubCandleDto

}

// ================= CLIENT =================

object FinnhubClient {

    private const val BASE_URL = "https://finnhub.io/api/v1/"

    // ⚠️ NOTA: en un proyecto final esto debería ir en local.properties o BuildConfig
    const val API_KEY = "d7dsf8pr01qmm59ebt6gd7dsf8pr01qmm59ebt70"

    val api: FinnhubApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApi::class.java)
    }

}
