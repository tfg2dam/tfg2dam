package com.simutrade.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ================= DTO =================

data class CotizacionFinnhubDto(

    @SerializedName("c")
    val precioActual: Double?,

    @SerializedName("d")
    val cambioPrecio: Double?,

    @SerializedName("dp")
    val cambioPorcentaje: Double?
)

data class RespuestaBusquedaFinnhubDto(

    @SerializedName("result")
    val resultados: List<ItemBusquedaFinnhubDto> = emptyList()
)

data class ItemBusquedaFinnhubDto(

    @SerializedName("symbol")
    val simbolo: String,

    @SerializedName("description")
    val descripcion: String
)

// ================= API =================

interface ApiFinnhub {

    @GET("quote")
    suspend fun obtenerCotizacion(
        @Query("symbol") simbolo: String
    ): CotizacionFinnhubDto

    @GET("search")
    suspend fun buscarSimbolos(
        @Query("q") consulta: String
    ): RespuestaBusquedaFinnhubDto
}

// ================= CLIENTE =================

object ClienteFinnhub {

    private const val URL_BASE = "https://finnhub.io/api/v1/"
    private const val TIMEOUT_SEGUNDOS = 10L

    // TODO: mover a local.properties antes de subir a producción
    const val API_KEY = "d7dsf8pr01qmm59ebt6gd7dsf8pr01qmm59ebt70"

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