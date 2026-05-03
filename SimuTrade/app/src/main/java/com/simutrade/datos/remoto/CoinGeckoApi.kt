package com.simutrade.datos.remoto

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val MONEDAS_POR_PAGINA = 10

// ================= DTO =================

// Moneda del top de mercado
data class MonedaCoinGeckoDto(
    val id: String,
    @SerializedName("symbol") val simbolo: String,
    @SerializedName("name") val nombre: String,
    @SerializedName("current_price") val precioActual: Double,
    @SerializedName("price_change_24h") val cambioPrecio24h: Double?,
    @SerializedName("price_change_percentage_24h") val cambioPorcentaje24h: Double?
)

// Respuesta de búsqueda
data class RespuestaBusquedaCoinGeckoDto(
    @SerializedName("coins") val monedas: List<ItemBusquedaCoinGeckoDto>
)

// Item dentro de una búsqueda
data class ItemBusquedaCoinGeckoDto(
    val id: String,
    @SerializedName("symbol") val simbolo: String,
    @SerializedName("name") val nombre: String
)

// ================= API =================

interface ApiCoinGecko {

    // Top monedas por capitalización
    @GET("coins/markets")
    suspend fun obtenerMonedasPrincipales(
        @Query("vs_currency") moneda: String = "eur",
        @Query("per_page") cantidadPorPagina: Int = MONEDAS_POR_PAGINA
    ): List<MonedaCoinGeckoDto>

    // Buscar monedas por nombre o símbolo
    @GET("search")
    suspend fun buscarMonedas(
        @Query("query") consulta: String
    ): RespuestaBusquedaCoinGeckoDto
}

// ================= CLIENTE =================

// Instancia única de Retrofit para CoinGecko
object ClienteCoinGecko {

    private const val URL_BASE = "https://api.coingecko.com/api/v3/"
    private const val TIMEOUT_SEGUNDOS = 10L

    private val clienteHttp = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .build()

    val api: ApiCoinGecko by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiCoinGecko::class.java)
    }
}