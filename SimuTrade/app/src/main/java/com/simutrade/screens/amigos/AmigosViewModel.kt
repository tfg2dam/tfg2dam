package com.simutrade.screens.amigos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.SolicitudAmistad
import com.simutrade.data.repository.AmigosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AmigosUiState(
    val amigos: List<Amigo> = emptyList(),
    val solicitudes: List<SolicitudAmistad> = emptyList(),
    val resultadoBusqueda: Amigo? = null,
    val isLoading: Boolean = false,
    val isBuscando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class AmigosViewModel : ViewModel() {

    private val repository = AmigosRepository()

    private val _uiState = MutableStateFlow(AmigosUiState())
    val uiState: StateFlow<AmigosUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val amigos = repository.getAmigos()
                val solicitudes = repository.getSolicitudes()
                _uiState.value = _uiState.value.copy(
                    amigos = amigos,
                    solicitudes = solicitudes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar datos"
                )
            }
        }
    }

    fun buscarPorCodigo(codigo: String) {
        if (codigo.isBlank()) {
            _uiState.value = _uiState.value.copy(resultadoBusqueda = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBuscando = true, resultadoBusqueda = null)
            val resultado = repository.buscarPorCodigo(codigo)
            _uiState.value = _uiState.value.copy(
                isBuscando = false,
                resultadoBusqueda = resultado,
                error = if (resultado == null) "No se encontro ningun usuario con ese codigo" else null
            )
        }
    }

    fun enviarSolicitud(amigoUid: String) {
        viewModelScope.launch {
            val exito = repository.enviarSolicitud(amigoUid)
            _uiState.value = _uiState.value.copy(
                mensaje = if (exito) "Solicitud enviada" else "Error al enviar solicitud",
                resultadoBusqueda = null
            )
        }
    }

    fun aceptarSolicitud(solicitanteUid: String) {
        viewModelScope.launch {
            val exito = repository.aceptarSolicitud(solicitanteUid)
            if (exito) {
                cargarDatos()
                _uiState.value = _uiState.value.copy(mensaje = "Solicitud aceptada")
            } else {
                _uiState.value = _uiState.value.copy(error = "Error al aceptar solicitud")
            }
        }
    }

    fun rechazarSolicitud(solicitanteUid: String) {
        viewModelScope.launch {
            repository.rechazarSolicitud(solicitanteUid)
            cargarDatos()
        }
    }

    fun eliminarAmigo(amigoUid: String) {
        viewModelScope.launch {
            repository.eliminarAmigo(amigoUid)
            cargarDatos()
        }
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(mensaje = null, error = null)
    }
}