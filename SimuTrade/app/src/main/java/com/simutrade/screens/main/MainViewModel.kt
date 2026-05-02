package com.simutrade.screens.main

import androidx.lifecycle.ViewModel
import com.simutrade.data.model.Activo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ================= PANTALLAS =================

sealed class Pantalla {
    data object Inicio : Pantalla()
    data object Mercado : Pantalla()
    data object Trading : Pantalla()
    data object Rankings : Pantalla()
    data object Retos : Pantalla()
    data object Amigos : Pantalla()
    data object Ligas : Pantalla()
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
            activoSeleccionado = estadoActual.activoSeleccionado.takeIf {
                pantalla == Pantalla.Trading
            }
        )
    }

    // ================= ACTIVO =================

    fun seleccionarActivo(activo: Activo) {
        _uiState.value = _uiState.value.copy(
            activoSeleccionado = activo,
            pantallaActual = Pantalla.Trading
        )
    }
}