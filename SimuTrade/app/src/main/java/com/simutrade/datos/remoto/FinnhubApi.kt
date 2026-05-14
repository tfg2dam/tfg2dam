package com.simutrade.datos.remoto

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.simutrade.BuildConfig

// ================= DTO =================

// Precio actual de una acción
data class CotizacionFinnhubDto(
    @SerializedName("c") val precioActual: Double?,
    @SerializedName("d") val cambioPrecio: Double?,
    @SerializedName("dp") val cambioPorcentaje: Double?
)

// Respuesta de búsqueda de símbolos
data class RespuestaBusquedaFinnhubDto(
    @SerializedName("result") val resultados: List<ItemBusquedaFinnhubDto> = emptyList()
)

// Item dentro de una búsqueda
data class ItemBusquedaFinnhubDto(
    @SerializedName("symbol") val simbolo: String,
    @SerializedName("description") val descripcion: String
)

// ================= API =================

interface ApiFinnhub {

    // Cotización en tiempo real de una acción
    @GET("quote")
    suspend fun obtenerCotizacion(
        @Query("symbol") simbolo: String
    ): CotizacionFinnhubDto

    // Buscar acciones por nombre o símbolo
    @GET("search")
    suspend fun buscarSimbolos(
        @Query("q") consulta: String
    ): RespuestaBusquedaFinnhubDto
}

// ================= CLIENTE =================

// Instancia única de Retrofit para Finnhub
object ClienteFinnhub {

    private const val URL_BASE = "https://finnhub.io/api/v1/"
    private const val TIMEOUT_SEGUNDOS = 10L

    // API key de Finnhub — no compartir públicamente
    const val API_KEY = BuildConfig.FINNHUB_API_KEY

    // Añade la API key en cada petición automáticamente
    private val clienteHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Finnhub-Token", API_KEY)
                .build()
            chain.proceed(request)
        }
        .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .build()

    val api: ApiFinnhub by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiFinnhub::class.java)
    }
}