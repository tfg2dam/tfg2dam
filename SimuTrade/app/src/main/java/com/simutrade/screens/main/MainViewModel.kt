package com.simutrade.screens.main

import androidx.lifecycle.ViewModel
import com.simutrade.data.model.Activo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ================= PANTALLAS =================

sealed class Pantalla {

    object Inicio : Pantalla()
    object Mercado : Pantalla()
    object Trading : Pantalla()
    object Rankings : Pantalla()
    object Retos : Pantalla()
    object Amigos : Pantalla()
    object Ligas : Pantalla()
}

// ================= ESTADO =================

data class EstadoUiPrincipal(
    val pantallaActual: Pantalla = Pantalla.Inicio,
    val activoSeleccionado: Activo? = null
)

// ================= VIEWMODEL =================

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EstadoUiPrincipal())
    val uiState: StateFlow<EstadoUiPrincipal> = _uiState.asStateFlow()

    // ================= NAVEGACIÓN =================

    fun navegarA(pantalla: Pantalla) {
        val estadoActual = _uiState.value

        if (estadoActual.pantallaActual == pantalla) return

        _uiState.value = estadoActual.copy(
            pantallaActual = pantalla,
            activoSeleccionado =
                if (pantalla == Pantalla.Trading) estadoActual.activoSeleccionado
                else null
        )
    }

    // ================= ACTIVO =================

    fun seleccionarActivo(activo: Activo) {
        _uiState.value = _uiState.value.copy(
            activoSeleccionado = activo,
            pantallaActual = Pantalla.Trading
        )
    }

    fun limpiarActivoSeleccionado() {
        _uiState.value = _uiState.value.copy(activoSeleccionado = null)
    }
}