package com.simutrade.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

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
    val resultados: List<ItemBusquedaFinnhubDto>
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

        @Query("symbol")
        simbolo: String,

        @Query("token")
        token: String = ClienteFinnhub.API_KEY

    ): CotizacionFinnhubDto

    @GET("search")
    suspend fun buscarSimbolos(

        @Query("q")
        consulta: String,

        @Query("token")
        token: String = ClienteFinnhub.API_KEY

    ): RespuestaBusquedaFinnhubDto
}

// ================= CLIENTE =================

object ClienteFinnhub {

    private const val URL_BASE =
        "https://finnhub.io/api/v1/"

    // NOTA:
    // esto debería ir en local.properties o BuildConfig
    const val API_KEY =
        "d7dsf8pr01qmm59ebt6gd7dsf8pr01qmm59ebt70"

    val api: ApiFinnhub by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiFinnhub::class.java)
    }
}