package com.simutrade.datos.repositorio

import com.simutrade.datos.modelo.Activo
import com.simutrade.datos.modelo.TipoActivo
import com.simutrade.datos.remoto.ClienteCoinGecko
import com.simutrade.datos.remoto.MonedaCoinGeckoDto
import com.simutrade.datos.remoto.ClienteFinnhub
import com.simutrade.datos.remoto.CotizacionFinnhubDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

object RepositorioMercado {

    private const val LIMITE_BUSQUEDA = 5
    private const val TIEMPO_CACHE_MS = 120_000L
    private const val TIMEOUT_BUSQUEDA_MS = 8_000L

    // ================= CACHÉ =================

    // Datos en memoria para evitar llamadas repetidas a la API
    private var criptomonedasEnCache: List<Activo> = emptyList()
    private var accionesEnCache: List<Activo> = emptyList()

    private var ultimaCargaCriptomonedas = 0L
    private var ultimaCargaAcciones = 0L

    // ================= ACCIONES POR DEFECTO =================

    // Lista fija de acciones que se muestran en el mercado
    private val accionesPorDefecto = listOf(
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

    // ================= TOP CRIPTOMONEDAS =================

    // Devuelve el top de criptos, usando caché si no han pasado 2 minutos
    suspend fun obtenerTopCriptomonedas(): List<Activo> {
        val ahora = System.currentTimeMillis()

        if (criptomonedasEnCache.isNotEmpty() && ahora - ultimaCargaCriptomonedas < TIEMPO_CACHE_MS) {
            return criptomonedasEnCache
        }

        return try {
            val resultado = ClienteCoinGecko.api
                .obtenerMonedasPrincipales()
                .map { it.aActivo() }

            criptomonedasEnCache = resultado
            ultimaCargaCriptomonedas = ahora
            resultado

        } catch (_: Exception) {
            criptomonedasEnCache
        }
    }

    // ================= TOP ACCIONES =================

    // Descarga las cotizaciones de las acciones por defecto en paralelo
    suspend fun obtenerTopAcciones(): List<Activo> = coroutineScope {
        val ahora = System.currentTimeMillis()

        if (accionesEnCache.isNotEmpty() && ahora - ultimaCargaAcciones < TIEMPO_CACHE_MS) {
            return@coroutineScope accionesEnCache
        }

        val diferidos = accionesPorDefecto.map { (simbolo, nombre) ->
            async {
                try {
                    val cotizacion = ClienteFinnhub.api.obtenerCotizacion(simbolo = simbolo)
                    if ((cotizacion.precioActual ?: 0.0) > 0) {
                        cotizacion.aActivo(simbolo = simbolo, nombre = nombre)
                    } else null
                } catch (_: Exception) { null }
            }
        }

        val resultado = diferidos.mapNotNull { it.await() }
        accionesEnCache = resultado
        ultimaCargaAcciones = ahora
        resultado
    }

    // ================= BÚSQUEDA =================

    // Busca activos en CoinGecko y Finnhub en paralelo
    suspend fun buscarActivos(textoBusqueda: String): List<Activo> = coroutineScope {
        val resultados = mutableListOf<Activo>()

        // Busca criptomonedas, reutiliza caché si ya existe el activo
        val criptomonedasDiferidas = async {
            try {
                withTimeout(TIMEOUT_BUSQUEDA_MS) {
                    ClienteCoinGecko.api
                        .buscarMonedas(consulta = textoBusqueda)
                        .monedas
                        .take(LIMITE_BUSQUEDA)
                        .map { item ->
                            criptomonedasEnCache.firstOrNull {
                                it.id.equals(item.id, ignoreCase = true)
                            } ?: Activo(
                                id = item.id,
                                simbolo = item.simbolo.uppercase(),
                                nombre = item.nombre,
                                tipo = TipoActivo.CRIPTO,
                                precioActual = 0.0,
                                cambioPrecio24h = 0.0,
                                cambioPorcentaje24h = 0.0
                            )
                        }
                }
            } catch (_: Exception) { emptyList() }
        }

        // Busca acciones, reutiliza caché si ya existe el activo
        val accionesDiferidas = async {
            try {
                withTimeout(TIMEOUT_BUSQUEDA_MS) {
                    val elementos = ClienteFinnhub.api
                        .buscarSimbolos(consulta = textoBusqueda)
                        .resultados
                        .take(LIMITE_BUSQUEDA)

                    elementos.map { item ->
                        async {
                            accionesEnCache.firstOrNull {
                                it.simbolo.equals(item.simbolo, ignoreCase = true)
                            } ?: run {
                                val cotizacion = try {
                                    ClienteFinnhub.api.obtenerCotizacion(simbolo = item.simbolo)
                                } catch (_: Exception) { null }

                                Activo(
                                    id = item.simbolo,
                                    simbolo = item.simbolo,
                                    nombre = item.descripcion,
                                    tipo = TipoActivo.ACCION,
                                    precioActual = cotizacion?.precioActual ?: 0.0,
                                    cambioPrecio24h = cotizacion?.cambioPrecio ?: 0.0,
                                    cambioPorcentaje24h = cotizacion?.cambioPorcentaje ?: 0.0
                                )
                            }
                        }
                    }.map { it.await() }
                }
            } catch (_: Exception) { emptyList() }
        }

        resultados.addAll(criptomonedasDiferidas.await())
        resultados.addAll(accionesDiferidas.await())
        resultados
    }

    // ================= MAPPERS =================

    // Convierte respuesta de CoinGecko a modelo interno
    private fun MonedaCoinGeckoDto.aActivo(): Activo = Activo(
        id = id,
        simbolo = simbolo.uppercase(),
        nombre = nombre,
        tipo = TipoActivo.CRIPTO,
        precioActual = precioActual,
        cambioPrecio24h = cambioPrecio24h ?: 0.0,
        cambioPorcentaje24h = cambioPorcentaje24h ?: 0.0
    )

    // Convierte respuesta de Finnhub a modelo interno
    private fun CotizacionFinnhubDto.aActivo(simbolo: String, nombre: String): Activo = Activo(
        id = simbolo,
        simbolo = simbolo,
        nombre = nombre,
        tipo = TipoActivo.ACCION,
        precioActual = precioActual ?: 0.0,
        cambioPrecio24h = cambioPrecio ?: 0.0,
        cambioPorcentaje24h = cambioPorcentaje ?: 0.0
    )
}