package com.simutrade.ui.ligas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.simutrade.datos.modelo.Amigo
import com.simutrade.datos.modelo.EntradaRanking
import com.simutrade.datos.modelo.InvitacionLiga
import com.simutrade.datos.modelo.Liga
import com.simutrade.datos.repositorio.RepositorioAmigos
import com.simutrade.datos.repositorio.RepositorioLigas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla de ligas
data class EstadoUiLigas(
    val misLigas: List<Liga> = emptyList(),
    val invitaciones: List<InvitacionLiga> = emptyList(),
    val ligaSeleccionada: Liga? = null,
    val rankingLiga: List<EntradaRanking> = emptyList(),
    val misAmigos: List<Amigo> = emptyList(),
    val miUid: String = "",
    val cargando: Boolean = false,
    val cargandoRanking: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class LigasViewModel : ViewModel() {

    private val repositorio = RepositorioLigas()
    private val repositorioAmigos = RepositorioAmigos()

    private val _estadoUi = MutableStateFlow(EstadoUiLigas())
    val estadoUi: StateFlow<EstadoUiLigas> = _estadoUi.asStateFlow()

    init {
        // Guarda el UID del usuario actual al iniciar
        _estadoUi.update { it.copy(miUid = FirebaseAuth.getInstance().currentUser?.uid ?: "") }
        cargarDatos()
    }

    // ================= CARGAR =================

    // Carga ligas, invitaciones y amigos en paralelo
    fun cargarDatos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, error = null) }
            try {
                val ligas = repositorio.obtenerMisLigas()
                val invitaciones = repositorio.obtenerInvitaciones()
                val amigos = repositorioAmigos.obtenerAmigos()
                _estadoUi.update {
                    it.copy(misLigas = ligas, invitaciones = invitaciones, misAmigos = amigos, cargando = false)
                }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(cargando = false, error = "Error al cargar ligas") }
            }
        }
    }

    // ================= CREAR =================

    // Crea una nueva liga y recarga los datos
    fun crearLiga(nombre: String) {
        if (nombre.isBlank()) {
            _estadoUi.update { it.copy(error = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true) }
            val ligaId = repositorio.crearLiga(nombre)
            if (ligaId != null) {
                cargarDatos()
                _estadoUi.update { it.copy(mensaje = "Liga creada") }
            } else {
                _estadoUi.update { it.copy(cargando = false, error = "Error al crear la liga") }
            }
        }
    }

    // ================= INVITAR =================

    // Invita a un amigo comprobando que no sea ya miembro
    fun invitarAmigo(ligaId: String, amigoUid: String) {
        viewModelScope.launch {
            val yaEsMiembro = _estadoUi.value.ligaSeleccionada
                ?.miembros?.any { it.uid == amigoUid } == true

            if (yaEsMiembro) {
                _estadoUi.update { it.copy(mensaje = "Este usuario ya está en la liga") }
                return@launch
            }

            val exito = repositorio.invitarAmigo(ligaId, amigoUid)
            _estadoUi.update {
                it.copy(
                    mensaje = if (exito) "Invitación enviada" else null,
                    error = if (!exito) "Error al enviar la invitación" else null
                )
            }
        }
    }

    // ================= ACEPTAR / RECHAZAR =================

    // Acepta la invitación y recarga los datos
    fun aceptarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.aceptarInvitacion(ligaId)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(
                        invitaciones = estado.invitaciones.filter { it.ligaId != ligaId },
                        mensaje = "Te has unido a la liga"
                    )
                }
                cargarDatos()
            } else {
                _estadoUi.update { it.copy(error = "Error al aceptar la invitación") }
            }
        }
    }

    // Rechaza y elimina la invitación de la lista
    fun rechazarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.rechazarInvitacion(ligaId)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(invitaciones = estado.invitaciones.filter { it.ligaId != ligaId })
                }
            } else {
                _estadoUi.update { it.copy(error = "Error al rechazar la invitación") }
            }
        }
    }

    // ================= SALIR =================

    // Sale de la liga y la elimina de la lista
    fun salirDeLiga(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.salirDeLiga(ligaId)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(
                        misLigas = estado.misLigas.filter { it.id != ligaId },
                        ligaSeleccionada = null,
                        rankingLiga = emptyList(),
                        mensaje = "Has salido de la liga"
                    )
                }
            } else {
                _estadoUi.update { it.copy(error = "Error al salir de la liga") }
            }
        }
    }

    // ================= SELECCIÓN =================

    // Selecciona una liga y carga su ranking
    fun seleccionarLiga(liga: Liga) {
        _estadoUi.update { it.copy(ligaSeleccionada = liga) }
        cargarRankingLiga(liga.id)
    }

    // Deselecciona la liga actual
    fun deseleccionarLiga() {
        _estadoUi.update { it.copy(ligaSeleccionada = null, rankingLiga = emptyList()) }
    }

    // Carga el ranking de una liga específica
    private fun cargarRankingLiga(ligaId: String) {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargandoRanking = true) }
            try {
                val ranking = repositorio.obtenerRankingLiga(ligaId)
                _estadoUi.update { it.copy(rankingLiga = ranking, cargandoRanking = false) }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(cargandoRanking = false, error = "Error al cargar el ranking de la liga") }
            }
        }
    }

    // ================= HELPERS =================

    // Limpia mensajes y errores de la UI
    fun limpiarMensaje() {
        _estadoUi.update { it.copy(mensaje = null, error = null) }
    }
}