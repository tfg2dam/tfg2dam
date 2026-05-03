package com.simutrade.ui.main

import androidx.lifecycle.ViewModel
import com.simutrade.datos.modelo.Activo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Pantallas disponibles en la navegación principal
sealed class Pantalla {
    data object Inicio : Pantalla()
    data object Mercado : Pantalla()
    data object Trading : Pantalla()
    data object Ranking : Pantalla()
    data object Retos : Pantalla()
    data object Amigos : Pantalla()
    data object Ligas : Pantalla()
}

// Estado de la pantalla principal
data class EstadoUiPrincipal(
    val pantallaActual: Pantalla = Pantalla.Inicio,
    val activoSeleccionado: Activo? = null
)

class MainViewModel : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiPrincipal())
    val estadoUi: StateFlow<EstadoUiPrincipal> = _estadoUi.asStateFlow()

    // ================= NAVEGACIÓN =================

    // Navega a la pantalla indicada, limpiando el activo si no es Trading
    fun navegarA(pantalla: Pantalla) {
        val estadoActual = _estadoUi.value
        if (estadoActual.pantallaActual == pantalla) return
        _estadoUi.value = estadoActual.copy(
            pantallaActual = pantalla,
            activoSeleccionado = estadoActual.activoSeleccionado.takeIf {
                pantalla == Pantalla.Trading
            }
        )
    }

    // ================= ACTIVO =================

    // Selecciona un activo y navega automáticamente a Trading
    fun seleccionarActivo(activo: Activo) {
        _estadoUi.value = _estadoUi.value.copy(
            activoSeleccionado = activo,
            pantallaActual = Pantalla.Trading
        )
    }
}