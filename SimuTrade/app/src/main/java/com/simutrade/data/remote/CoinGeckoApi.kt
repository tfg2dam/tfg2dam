package com.simutrade.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ================= DTO (RESPUESTAS API) =================

data class MonedaCoinGeckoDto(
    val id: String,

    @SerializedName("symbol")
    val simbolo: String,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("current_price")
    val precioActual: Double,

    @SerializedName("price_change_24h")
    val cambioPrecio24h: Double?,

    @SerializedName("price_change_percentage_24h")
    val cambioPorcentaje24h: Double?
)

data class RespuestaBusquedaCoinGeckoDto(

    @SerializedName("coins")
    val monedas: List<ItemBusquedaCoinGeckoDto>
)

data class ItemBusquedaCoinGeckoDto(
    val id: String,

    @SerializedName("symbol")
    val simbolo: String,

    @SerializedName("name")
    val nombre: String
)

// ================= API =================

interface ApiCoinGecko {

    @GET("coins/markets")
    suspend fun obtenerMonedasPrincipales(

        @Query("vs_currency")
        moneda: String = "eur",

        @Query("order")
        orden: String = "market_cap_desc",

        @Query("per_page")
        cantidadPorPagina: Int = 10,

        @Query("page")
        pagina: Int = 1,

        @Query("sparkline")
        incluirSparkline: Boolean = false

    ): List<MonedaCoinGeckoDto>

    @GET("search")
    suspend fun buscarMonedas(

        @Query("query")
        consulta: String

    ): RespuestaBusquedaCoinGeckoDto
}

// ================= CLIENTE =================

object ClienteCoinGecko {

    private const val URL_BASE =
        "https://api.coingecko.com/api/v3/"

    val api: ApiCoinGecko by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiCoinGecko::class.java)
    }
}