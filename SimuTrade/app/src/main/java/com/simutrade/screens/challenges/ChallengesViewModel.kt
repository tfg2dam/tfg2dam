package com.simutrade.screens.challenges

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.DatosRetos
import com.simutrade.data.model.Reto
import com.simutrade.data.model.TipoActivo
import com.simutrade.data.repository.RepositorioUsuario
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class ValidacionReto(
    val completado: Boolean,
    val mensaje: String
)

class ChallengesViewModel : ViewModel() {

    private val repositorio = RepositorioUsuario()

    private val _datosRetos = MutableStateFlow(
        DatosRetos()
    )

    val datosRetos: StateFlow<DatosRetos> =
        _datosRetos.asStateFlow()

    private val _cargando = MutableStateFlow(false)

    val cargando: StateFlow<Boolean> =
        _cargando.asStateFlow()

    private val _milisegundosHastaReset =
        MutableStateFlow(0L)

    val milisegundosHastaReset: StateFlow<Long> =
        _milisegundosHastaReset.asStateFlow()

    init {
        cargarRetos()
    }

    // ================= CARGAR =================

    fun cargarRetos() {
        viewModelScope.launch {

            _cargando.value = true

            try {
                val datosGuardados =
                    repositorio.obtenerDatosRetos()

                val datosSeguros =
                    if (datosGuardados.diaActual == 0L) {

                        val inicioHoy =
                            obtenerInicioDelDia()

                        val datosIniciales =
                            datosGuardados.copy(
                                diaActual = inicioHoy,
                                retosDelDia = generarRetosAleatorios()
                            )

                        repositorio.guardarDatosRetos(
                            datosIniciales
                        )

                        datosIniciales

                    } else {
                        datosGuardados
                    }

                val datosActualizados =
                    verificarResetDiario(
                        datosSeguros
                    )

                _datosRetos.value =
                    datosActualizados

                calcularTiempoReset()

            } catch (e: Exception) {
                Log.e(
                    "ChallengesViewModel",
                    "Error cargando retos",
                    e
                )
            } finally {
                _cargando.value = false
            }
        }
    }

    // ================= RESET DIARIO =================

    private suspend fun verificarResetDiario(
        datos: DatosRetos
    ): DatosRetos {

        val inicioHoy =
            obtenerInicioDelDia()

        if (datos.diaActual != inicioHoy) {

            val ayer =
                inicioHoy - (24 * 60 * 60 * 1000)

            val nuevaRacha =
                if (
                    datos.diaActual == ayer &&
                    todosLosRetosCompletados(datos)
                ) {
                    datos.rachaActual + 1
                } else {
                    0
                }

            val datosReiniciados =
                datos.copy(
                    rachaActual = nuevaRacha,
                    rachaMaxima = maxOf(
                        datos.rachaMaxima,
                        nuevaRacha
                    ),
                    retosCompletados = emptyList(),
                    retosDelDia = generarRetosAleatorios(),
                    diaActual = inicioHoy,
                    ultimaVez = System.currentTimeMillis()
                )

            repositorio.guardarDatosRetos(
                datosReiniciados
            )

            return datosReiniciados
        }

        return datos
    }

    private fun obtenerInicioDelDia(): Long {
        return Calendar
            .getInstance(TimeZone.getDefault())
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis
    }

    private fun calcularTiempoReset() {
        val ahora =
            Calendar.getInstance(
                TimeZone.getDefault()
            )

        val manana =
            (ahora.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        _milisegundosHastaReset.value =
            manana.timeInMillis - ahora.timeInMillis
    }

    // ================= GENERADOR =================

    private fun generarRetosAleatorios(): List<String> {
        return listOf(
            "operacion",
            "diversificar",
            "trader",
            "multimercado",
            "beneficio"
        ).shuffled().take(3)
    }

    // ================= RETOS DEL DÍA =================

    fun obtenerRetosDelDia(): List<Reto> {

        val datos =
            _datosRetos.value

        return datos.retosDelDia.mapIndexed { index, tipo ->

            val id =
                "reto_${tipo}_${datos.diaActual}_${index + 1}"

            when (tipo) {

                "operacion" -> Reto(
                    id = id,
                    titulo = "Haz tu primera operación",
                    descripcion = "Compra o vende cualquier activo hoy",
                    recompensa = 1.0
                )

                "diversificar" -> Reto(
                    id = id,
                    titulo = "Diversifica tu cartera",
                    descripcion = "Ten al menos 2 inversiones diferentes",
                    recompensa = 1.5
                )

                "multimercado" -> Reto(
                    id = id,
                    titulo = "Invierte en varios mercados",
                    descripcion = "Invierte en acciones y criptomonedas",
                    recompensa = 2.0
                )

                "trader" -> Reto(
                    id = id,
                    titulo = "Actividad alta",
                    descripcion = "Realiza 3 operaciones hoy",
                    recompensa = 2.5
                )

                "beneficio" -> Reto(
                    id = id,
                    titulo = "Consigue beneficios",
                    descripcion = "Haz que tu cartera esté en positivo",
                    recompensa = 3.0
                )

                else -> Reto(
                    id = "error",
                    titulo = "Error",
                    descripcion = "No se pudo cargar el reto",
                    recompensa = 0.0
                )
            }
        }
    }

    private fun todosLosRetosCompletados(
        datos: DatosRetos
    ): Boolean {

        val idsRetos =
            datos.retosDelDia.mapIndexed { index, tipo ->
                "reto_${tipo}_${datos.diaActual}_${index + 1}"
            }

        return idsRetos.all {
            it in datos.retosCompletados
        }
    }

    // ================= VALIDACIÓN =================

    suspend fun validarReto(
        id: String
    ): ValidacionReto = coroutineScope {

        val tipo =
            obtenerTipoDesdeId(id)

        val carteraDeferred =
            async {
                repositorio.obtenerCartera()
            }

        val transaccionesDeferred =
            async {
                repositorio.obtenerTransacciones()
            }

        val usuarioDeferred =
            async {
                repositorio.obtenerDatosUsuario()
            }

        val cartera =
            carteraDeferred.await()

        val transacciones =
            transaccionesDeferred.await()

        val usuario =
            usuarioDeferred.await()

        val inicioHoy =
            obtenerInicioDelDia()

        val transaccionesHoy =
            transacciones.filter {
                it.fecha >= inicioHoy
            }

        when (tipo) {

            "operacion" -> {
                if (transaccionesHoy.isNotEmpty()) {
                    ValidacionReto(
                        true,
                        "Ya has hecho una operación hoy"
                    )
                } else {
                    ValidacionReto(
                        false,
                        "Haz una compra o una venta"
                    )
                }
            }

            "trader" -> {
                val restantes =
                    3 - transaccionesHoy.size

                if (transaccionesHoy.size >= 3) {
                    ValidacionReto(
                        true,
                        "Has completado las 3 operaciones"
                    )
                } else {
                    ValidacionReto(
                        false,
                        "Te faltan $restantes operaciones"
                    )
                }
            }

            "diversificar" -> {
                val activosDistintos =
                    cartera.map {
                        it.idActivo
                    }.distinct().size

                if (activosDistintos >= 2) {
                    ValidacionReto(
                        true,
                        "Ya tienes varias inversiones"
                    )
                } else {
                    ValidacionReto(
                        false,
                        "Te falta ${2 - activosDistintos} inversión más"
                    )
                }
            }

            "multimercado" -> {
                val tieneAcciones =
                    cartera.any {
                        it.tipo == TipoActivo.ACCION
                    }

                val tieneCripto =
                    cartera.any {
                        it.tipo == TipoActivo.CRIPTO
                    }

                when {
                    tieneAcciones && tieneCripto ->
                        ValidacionReto(
                            true,
                            "Ya inviertes en ambos mercados"
                        )

                    !tieneAcciones && !tieneCripto ->
                        ValidacionReto(
                            false,
                            "Empieza invirtiendo en cualquier activo"
                        )

                    !tieneAcciones ->
                        ValidacionReto(
                            false,
                            "Te falta invertir en acciones"
                        )

                    else ->
                        ValidacionReto(
                            false,
                            "Te falta invertir en criptomonedas"
                        )
                }
            }

            "beneficio" -> {
                val valorCartera =
                    repositorio.calcularValorCartera(
                        cartera
                    )

                val valorTotal =
                    repositorio.calcularValorTotal(
                        usuario.saldo,
                        valorCartera
                    )

                val beneficio =
                    repositorio.calcularBeneficio(
                        valorTotal,
                        usuario.saldoInicial
                    )

                if (beneficio > 0) {
                    ValidacionReto(
                        true,
                        "Vas ganando €${"%.2f".format(beneficio)}"
                    )
                } else {
                    ValidacionReto(
                        false,
                        "Tu cartera aún no está en positivo"
                    )
                }
            }

            else -> {
                ValidacionReto(
                    false,
                    "Reto desconocido"
                )
            }
        }
    }

    private fun obtenerTipoDesdeId(
        id: String
    ): String {
        return when {
            id.contains("operacion") -> "operacion"
            id.contains("diversificar") -> "diversificar"
            id.contains("trader") -> "trader"
            id.contains("multimercado") -> "multimercado"
            id.contains("beneficio") -> "beneficio"
            else -> ""
        }
    }

    // ================= COMPLETAR =================

    fun completarReto(
        id: String,
        recompensa: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {

            val validacion =
                validarReto(id)

            if (!validacion.completado) {
                onResult(
                    false,
                    validacion.mensaje
                )
                return@launch
            }

            val datosActuales =
                _datosRetos.value

            if (id in datosActuales.retosCompletados) {
                onResult(
                    false,
                    "Ya has completado este reto"
                )
                return@launch
            }

            repositorio.actualizarSaldoBonus(
                recompensa
            )

            val nuevosDatos =
                datosActuales.copy(
                    retosCompletados =
                        datosActuales.retosCompletados + id
                )

            repositorio.guardarDatosRetos(
                nuevosDatos
            )

            _datosRetos.value =
                nuevosDatos

            calcularTiempoReset()

            onResult(
                true,
                "+${"%.2f".format(recompensa)}€ añadidos"
            )
        }
    }
}