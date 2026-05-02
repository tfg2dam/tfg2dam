package com.simutrade.screens.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Activo
import com.simutrade.data.model.ActivoEnCartera
import com.simutrade.data.model.DatosUsuario
import com.simutrade.data.model.Rango
import com.simutrade.data.model.ResultadoOperacion
import com.simutrade.data.model.TipoTransaccion
import com.simutrade.data.model.Transaccion
import com.simutrade.data.repository.RepositorioMercado
import com.simutrade.data.repository.RepositorioUsuario
import com.simutrade.screens.rankings.RankUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UserViewModel : ViewModel() {

    private val repositorio = RepositorioUsuario()
    private val repositorioMercado = RepositorioMercado

    // ================= STATE =================

    data class UiState(
        val usuario: DatosUsuario = DatosUsuario(),
        val cartera: List<ActivoEnCartera> = emptyList(),
        val transacciones: List<Transaccion> = emptyList(),
        val rangoActual: Rango? = null,
        val cargando: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val mutexOperacion = Mutex()
    private var trabajoCarga: Job? = null

    init {
        cargarDatos()
    }

    // ================= CARGA =================

    fun cargarDatos() {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {

            _uiState.update { it.copy(cargando = true) }

            try {
                val (usuario, carteraOriginal, transacciones) = coroutineScope {
                    val usuarioDiferido = async {
                        repositorio.obtenerDatosUsuario()
                    }
                    val carteraDiferida = async {
                        repositorio.obtenerCartera()
                    }
                    val transaccionesDiferidas = async {
                        repositorio.obtenerTransacciones()
                    }
                    Triple(
                        usuarioDiferido.await(),
                        carteraDiferida.await(),
                        transaccionesDiferidas.await()
                    )
                }

                val carteraActualizada = actualizarPreciosCartera(
                    cartera = carteraOriginal,
                    usuario = usuario
                )
                val beneficio = calcularBeneficioTrading(usuario, carteraActualizada)

                _uiState.value = UiState(
                    usuario = usuario,
                    cartera = carteraActualizada,
                    transacciones = transacciones,
                    rangoActual = RankUtils.obtenerRangoPorBeneficio(beneficio),
                    cargando = false
                )

            } catch (_: Exception) {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    // ================= REFRESCAR PRECIOS =================

    private suspend fun actualizarPreciosCartera(
        cartera: List<ActivoEnCartera>,
        usuario: DatosUsuario
    ): List<ActivoEnCartera> {

        if (cartera.isEmpty()) return emptyList()

        val mercadoCompleto = coroutineScope {
            val acciones = async { repositorioMercado.obtenerTopAcciones() }
            val criptos = async { repositorioMercado.obtenerTopCriptomonedas() }
            acciones.await() + criptos.await()
        }

        val carteraActualizada = cartera.map { activoGuardado ->
            val activoActualizado = mercadoCompleto.find {
                it.id.equals(activoGuardado.idActivo, ignoreCase = true) ||
                        it.simbolo.equals(activoGuardado.simbolo, ignoreCase = true)
            }
            if (activoActualizado != null)
                activoGuardado.copy(precioActual = activoActualizado.precioActual)
            else
                activoGuardado
        }

        // Guardar solo los activos cuyo precio cambió
        carteraActualizada.zip(cartera).forEach { (nuevo, original) ->
            if (nuevo.precioActual != original.precioActual) {
                repositorio.guardarActivoEnCartera(nuevo)
            }
        }

        // ✅ Actualizar beneficio en Firestore con precios frescos
        // Descuenta saldoBonus para que el ranking no cuente el dinero de retos
        val valorCartera = carteraActualizada.sumOf { it.valorActual }
        val beneficio = calcularBeneficioTrading(usuario, carteraActualizada)
        repositorio.actualizarEstadisticasUsuario(
            valorCartera = valorCartera,
            beneficio = beneficio
        )

        return carteraActualizada
    }

    // ================= COMPRAR =================

    fun comprarActivo(
        activo: Activo,
        cantidad: Double,
        onResult: (ResultadoOperacion) -> Unit
    ) {
        viewModelScope.launch {
            mutexOperacion.withLock {

                val estado = _uiState.value

                if (cantidad <= 0 || cantidad.isNaN() || activo.precioActual <= 0) {
                    onResult(ResultadoOperacion.Error("Cantidad inválida"))
                    return@withLock
                }

                val total = cantidad * activo.precioActual

                if (total > estado.usuario.saldo) {
                    onResult(ResultadoOperacion.Error("Saldo insuficiente"))
                    return@withLock
                }

                val nuevoSaldo = estado.usuario.saldo - total
                repositorio.actualizarSaldo(nuevoSaldo)

                val existente = estado.cartera.find { it.idActivo == activo.id }

                val activoActualizado = if (existente != null) {
                    val cantidadTotal = existente.cantidad + cantidad
                    val costeTotal = (existente.precioPromedio * existente.cantidad) + total
                    existente.copy(
                        cantidad = cantidadTotal,
                        precioPromedio = costeTotal / cantidadTotal,
                        precioActual = activo.precioActual
                    )
                } else {
                    ActivoEnCartera(
                        idActivo = activo.id,
                        simbolo = activo.simbolo,
                        nombre = activo.nombre,
                        tipo = activo.tipo,
                        cantidad = cantidad,
                        precioPromedio = activo.precioActual,
                        precioActual = activo.precioActual
                    )
                }

                repositorio.guardarActivoEnCartera(activoActualizado)

                val transaccion = Transaccion(
                    id = "",
                    fecha = System.currentTimeMillis(),
                    tipo = TipoTransaccion.COMPRA,
                    idActivo = activo.id,
                    simbolo = activo.simbolo,
                    cantidad = cantidad,
                    precio = activo.precioActual,
                    total = total
                )

                repositorio.guardarTransaccion(transaccion)

                val nuevaCartera = estado.cartera
                    .filter { it.idActivo != activo.id } + activoActualizado
                val nuevasTransacciones = listOf(transaccion) + estado.transacciones
                val nuevoUsuario = estado.usuario.copy(saldo = nuevoSaldo)

                actualizarEstado(
                    usuario = nuevoUsuario,
                    cartera = nuevaCartera,
                    transacciones = nuevasTransacciones
                )

                onResult(ResultadoOperacion.Exito("Compra realizada"))
            }
        }
    }

    // ================= VENDER =================

    fun venderActivo(
        idActivo: String,
        cantidad: Double,
        precioActual: Double,
        onResult: (ResultadoOperacion) -> Unit
    ) {
        viewModelScope.launch {
            mutexOperacion.withLock {

                val estado = _uiState.value

                val activo = estado.cartera.find { it.idActivo == idActivo }
                    ?: run {
                        onResult(ResultadoOperacion.Error("Activo no encontrado"))
                        return@withLock
                    }

                if (
                    cantidad <= 0 ||
                    cantidad.isNaN() ||
                    cantidad > activo.cantidad ||
                    precioActual <= 0
                ) {
                    onResult(ResultadoOperacion.Error("Cantidad inválida"))
                    return@withLock
                }

                val total = cantidad * precioActual
                val nuevoSaldo = estado.usuario.saldo + total
                repositorio.actualizarSaldo(nuevoSaldo)

                val nuevaCartera = if (cantidad >= activo.cantidad) {
                    repositorio.eliminarActivoDeCartera(idActivo)
                    estado.cartera.filter { it.idActivo != idActivo }
                } else {
                    val activoActualizado = activo.copy(
                        cantidad = activo.cantidad - cantidad,
                        precioActual = precioActual
                    )
                    repositorio.guardarActivoEnCartera(activoActualizado)
                    estado.cartera.filter { it.idActivo != idActivo } + activoActualizado
                }

                val transaccion = Transaccion(
                    id = "",
                    fecha = System.currentTimeMillis(),
                    tipo = TipoTransaccion.VENTA,
                    idActivo = idActivo,
                    simbolo = activo.simbolo,
                    cantidad = cantidad,
                    precio = precioActual,
                    total = total
                )

                repositorio.guardarTransaccion(transaccion)

                val nuevasTransacciones = listOf(transaccion) + estado.transacciones
                val nuevoUsuario = estado.usuario.copy(saldo = nuevoSaldo)

                actualizarEstado(
                    usuario = nuevoUsuario,
                    cartera = nuevaCartera,
                    transacciones = nuevasTransacciones
                )

                onResult(ResultadoOperacion.Exito("Venta realizada"))
            }
        }
    }

    // ================= ACTUALIZAR ESTADO =================

    private suspend fun actualizarEstado(
        usuario: DatosUsuario,
        cartera: List<ActivoEnCartera>,
        transacciones: List<Transaccion>
    ) {
        val beneficio = calcularBeneficioTrading(usuario, cartera)

        _uiState.value = UiState(
            usuario = usuario,
            cartera = cartera,
            transacciones = transacciones,
            rangoActual = RankUtils.obtenerRangoPorBeneficio(beneficio),
            cargando = false
        )

        repositorio.actualizarEstadisticasUsuario(
            valorCartera = cartera.sumOf { it.valorActual },
            beneficio = beneficio
        )
    }

    // ================= CÁLCULOS =================

    private fun calcularBeneficioTrading(
        usuario: DatosUsuario,
        cartera: List<ActivoEnCartera>
    ): Double {
        val saldoTrading = usuario.saldo - usuario.saldoBonus
        val valorCartera = cartera.sumOf { it.valorActual }
        return (saldoTrading + valorCartera) - usuario.saldoInicial
    }
}