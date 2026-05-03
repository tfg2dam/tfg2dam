package com.simutrade.ui.amigos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.datos.modelo.Amigo
import com.simutrade.datos.modelo.SolicitudAmistad
import com.simutrade.datos.repositorio.RepositorioAmigos
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla de amigos
data class EstadoUiAmigos(
    val amigos: List<Amigo> = emptyList(),
    val solicitudes: List<SolicitudAmistad> = emptyList(),
    val resultadoBusqueda: Amigo? = null,
    val cargando: Boolean = false,
    val buscando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class AmigosViewModel : ViewModel() {

    private val repositorio = RepositorioAmigos()

    private val _estadoUi = MutableStateFlow(EstadoUiAmigos())
    val estadoUi: StateFlow<EstadoUiAmigos> = _estadoUi.asStateFlow()

    private var trabajoBusqueda: Job? = null

    init { cargarDatos() }

    // ================= CARGAR =================

    // Carga la lista de amigos y solicitudes pendientes
    fun cargarDatos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, error = null) }
            try {
                val amigos = repositorio.obtenerAmigos()
                val solicitudes = repositorio.obtenerSolicitudes()
                _estadoUi.update { it.copy(amigos = amigos, solicitudes = solicitudes, cargando = false) }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(cargando = false, error = "Error al cargar datos") }
            }
        }
    }

    // ================= BUSCAR =================

    // Busca un usuario por código con debounce de 400ms
    fun buscarPorCodigo(codigo: String) {
        trabajoBusqueda?.cancel()

        if (codigo.isBlank()) {
            _estadoUi.update { it.copy(resultadoBusqueda = null, error = null, buscando = false) }
            return
        }

        trabajoBusqueda = viewModelScope.launch {
            delay(400)
            _estadoUi.update { it.copy(buscando = true, resultadoBusqueda = null, error = null) }
            try {
                val resultado = repositorio.buscarPorCodigo(codigo)
                _estadoUi.update {
                    it.copy(
                        buscando = false,
                        resultadoBusqueda = resultado,
                        error = if (resultado == null) "No se encontró ningún usuario con ese código" else null
                    )
                }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(buscando = false, error = "Error en la búsqueda") }
            }
        }
    }

    // ================= SOLICITUDES =================

    // Envía una solicitud de amistad comprobando que no exista ya
    fun enviarSolicitud(amigoUid: String) {
        viewModelScope.launch {
            try {
                val yaEsAmigo = repositorio.esAmigo(amigoUid)
                if (yaEsAmigo) {
                    _estadoUi.update { it.copy(mensaje = "Ya sois amigos", resultadoBusqueda = null) }
                    return@launch
                }

                val solicitudPendiente = repositorio.tieneSolicitudPendiente(amigoUid)
                if (solicitudPendiente) {
                    _estadoUi.update { it.copy(mensaje = "Ya tienes una solicitud pendiente", resultadoBusqueda = null) }
                    return@launch
                }

                val exito = repositorio.enviarSolicitud(amigoUid)
                _estadoUi.update {
                    it.copy(
                        mensaje = if (exito) "Solicitud enviada" else "Error al enviar la solicitud",
                        resultadoBusqueda = null
                    )
                }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(error = "Error al enviar la solicitud") }
            }
        }
    }

    // Acepta una solicitud y recarga los datos
    fun aceptarSolicitud(solicitanteUid: String) {
        viewModelScope.launch {
            val exito = repositorio.aceptarSolicitud(solicitanteUid)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(
                        solicitudes = estado.solicitudes.filter { it.uid != solicitanteUid },
                        mensaje = "Solicitud aceptada"
                    )
                }
                cargarDatos()
            } else {
                _estadoUi.update { it.copy(error = "Error al aceptar la solicitud") }
            }
        }
    }

    // Rechaza y elimina la solicitud de la lista
    fun rechazarSolicitud(solicitanteUid: String) {
        viewModelScope.launch {
            val exito = repositorio.rechazarSolicitud(solicitanteUid)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(solicitudes = estado.solicitudes.filter { it.uid != solicitanteUid })
                }
            } else {
                _estadoUi.update { it.copy(error = "Error al rechazar la solicitud") }
            }
        }
    }

    // ================= AMIGOS =================

    // Elimina un amigo de la lista
    fun eliminarAmigo(amigoUid: String) {
        viewModelScope.launch {
            val exito = repositorio.eliminarAmigo(amigoUid)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(amigos = estado.amigos.filter { it.uid != amigoUid })
                }
            } else {
                _estadoUi.update { it.copy(error = "Error al eliminar el amigo") }
            }
        }
    }

    // ================= HELPERS =================

    // Limpia mensajes y errores de la UI
    fun limpiarMensaje() {
        _estadoUi.update { it.copy(mensaje = null, error = null) }
    }
}