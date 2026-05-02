package com.simutrade.screens.amigos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.SolicitudAmistad
import com.simutrade.data.repository.AmigosRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AmigosUiState(
    val amigos: List<Amigo> = emptyList(),
    val solicitudes: List<SolicitudAmistad> = emptyList(),
    val resultadoBusqueda: Amigo? = null,
    val cargando: Boolean = false,
    val buscando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class AmigosViewModel : ViewModel() {

    private val repositorio = AmigosRepository()

    private val _uiState = MutableStateFlow(AmigosUiState())
    val uiState: StateFlow<AmigosUiState> = _uiState.asStateFlow()

    private var trabajoBusqueda: Job? = null

    init {
        cargarDatos()
    }

    // ================= CARGAR =================

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val amigos = repositorio.obtenerAmigos()
                val solicitudes = repositorio.obtenerSolicitudes()
                _uiState.update {
                    it.copy(
                        amigos = amigos,
                        solicitudes = solicitudes,
                        cargando = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = "Error al cargar datos")
                }
            }
        }
    }

    // ================= BUSCAR =================

    fun buscarPorCodigo(codigo: String) {
        trabajoBusqueda?.cancel()

        if (codigo.isBlank()) {
            _uiState.update { it.copy(resultadoBusqueda = null, error = null, buscando = false) }
            return
        }

        trabajoBusqueda = viewModelScope.launch {
            // ✅ Debounce — espera 400ms antes de buscar
            delay(400)

            _uiState.update { it.copy(buscando = true, resultadoBusqueda = null, error = null) }

            try {
                val resultado = repositorio.buscarPorCodigo(codigo)
                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadoBusqueda = resultado,
                        error = if (resultado == null)
                            "No se encontró ningún usuario con ese código"
                        else null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(buscando = false, error = "Error en la búsqueda")
                }
            }
        }
    }

    // ================= SOLICITUDES =================

    fun enviarSolicitud(amigoUid: String) {
        viewModelScope.launch {
            try {
                // ✅ Comprobaciones previas protegidas con try/catch
                val yaEsAmigo = repositorio.esAmigo(amigoUid)
                if (yaEsAmigo) {
                    _uiState.update {
                        it.copy(mensaje = "Ya sois amigos", resultadoBusqueda = null)
                    }
                    return@launch
                }

                val solicitudPendiente = repositorio.tieneSolicitudPendiente(amigoUid)
                if (solicitudPendiente) {
                    _uiState.update {
                        it.copy(
                            mensaje = "Ya tienes una solicitud pendiente",
                            resultadoBusqueda = null
                        )
                    }
                    return@launch
                }

                val exito = repositorio.enviarSolicitud(amigoUid)
                _uiState.update {
                    it.copy(
                        mensaje = if (exito) "Solicitud enviada" else "Error al enviar la solicitud",
                        resultadoBusqueda = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(error = "Error al enviar la solicitud")
                }
            }
        }
    }

    fun aceptarSolicitud(solicitanteUid: String) {
        viewModelScope.launch {
            val exito = repositorio.aceptarSolicitud(solicitanteUid)
            if (exito) {
                _uiState.update { estado ->
                    estado.copy(
                        solicitudes = estado.solicitudes.filter { it.uid != solicitanteUid },
                        mensaje = "Solicitud aceptada"
                    )
                }
                cargarDatos()
            } else {
                _uiState.update { it.copy(error = "Error al aceptar la solicitud") }
            }
        }
    }

    fun rechazarSolicitud(solicitanteUid: String) {
        viewModelScope.launch {
            val exito = repositorio.rechazarSolicitud(solicitanteUid)
            if (exito) {
                _uiState.update { estado ->
                    estado.copy(
                        solicitudes = estado.solicitudes.filter { it.uid != solicitanteUid }
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Error al rechazar la solicitud") }
            }
        }
    }

    // ================= AMIGOS =================

    fun eliminarAmigo(amigoUid: String) {
        viewModelScope.launch {
            val exito = repositorio.eliminarAmigo(amigoUid)
            if (exito) {
                _uiState.update { estado ->
                    estado.copy(
                        amigos = estado.amigos.filter { it.uid != amigoUid }
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Error al eliminar el amigo") }
            }
        }
    }

    // ================= HELPERS =================

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null, error = null) }
    }
}