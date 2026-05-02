package com.simutrade.screens.ligas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.simutrade.data.model.Amigo
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.InvitacionLiga
import com.simutrade.data.model.Liga
import com.simutrade.data.repository.AmigosRepository
import com.simutrade.data.repository.LigasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LigasUiState(
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

    private val repositorio = LigasRepository()
    private val repositorioAmigos = AmigosRepository()

    private val _uiState = MutableStateFlow(LigasUiState())
    val uiState: StateFlow<LigasUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(miUid = FirebaseAuth.getInstance().currentUser?.uid ?: "")
        }
        cargarDatos()
    }

    // ================= CARGAR =================

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val ligas = repositorio.obtenerMisLigas()
                val invitaciones = repositorio.obtenerInvitaciones()
                val amigos = repositorioAmigos.obtenerAmigos()
                _uiState.update {
                    it.copy(
                        misLigas = ligas,
                        invitaciones = invitaciones,
                        misAmigos = amigos,
                        cargando = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = "Error al cargar ligas")
                }
            }
        }
    }

    // ================= CREAR =================

    fun crearLiga(nombre: String) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val ligaId = repositorio.crearLiga(nombre)
            if (ligaId != null) {
                cargarDatos()
                _uiState.update { it.copy(mensaje = "Liga creada") }
            } else {
                _uiState.update {
                    it.copy(cargando = false, error = "Error al crear la liga")
                }
            }
        }
    }

    // ================= INVITAR =================

    fun invitarAmigo(ligaId: String, amigoUid: String) {
        viewModelScope.launch {
            // ✅ Comprobar si ya es miembro antes de invitar
            val yaEsMiembro = _uiState.value.ligaSeleccionada
                ?.miembros?.any { it.uid == amigoUid } == true

            if (yaEsMiembro) {
                _uiState.update { it.copy(mensaje = "Este usuario ya está en la liga") }
                return@launch
            }

            val exito = repositorio.invitarAmigo(ligaId, amigoUid)
            _uiState.update {
                it.copy(
                    mensaje = if (exito) "Invitación enviada" else null,
                    error = if (!exito) "Error al enviar la invitación" else null
                )
            }
        }
    }

    // ================= ACEPTAR / RECHAZAR =================

    fun aceptarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.aceptarInvitacion(ligaId)
            if (exito) {
                _uiState.update { estado ->
                    estado.copy(
                        invitaciones = estado.invitaciones.filter { it.ligaId != ligaId },
                        mensaje = "Te has unido a la liga"
                    )
                }
                cargarDatos()
            } else {
                _uiState.update { it.copy(error = "Error al aceptar la invitación") }
            }
        }
    }

    fun rechazarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.rechazarInvitacion(ligaId)
            if (exito) {
                _uiState.update { estado ->
                    estado.copy(
                        invitaciones = estado.invitaciones.filter { it.ligaId != ligaId }
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Error al rechazar la invitación") }
            }
        }
    }

    // ================= SALIR =================

    fun salirDeLiga(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.salirDeLiga(ligaId)
            if (exito) {
                _uiState.update { estado ->
                    estado.copy(
                        misLigas = estado.misLigas.filter { it.id != ligaId },
                        ligaSeleccionada = null,
                        rankingLiga = emptyList(),
                        mensaje = "Has salido de la liga"
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Error al salir de la liga") }
            }
        }
    }

    // ================= SELECCIÓN =================

    fun seleccionarLiga(liga: Liga) {
        _uiState.update { it.copy(ligaSeleccionada = liga) }
        cargarRankingLiga(liga.id)
    }

    fun deseleccionarLiga() {
        _uiState.update {
            it.copy(ligaSeleccionada = null, rankingLiga = emptyList())
        }
    }

    private fun cargarRankingLiga(ligaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoRanking = true) }
            try {
                val ranking = repositorio.obtenerRankingLiga(ligaId)
                _uiState.update {
                    it.copy(rankingLiga = ranking, cargandoRanking = false)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        cargandoRanking = false,
                        error = "Error al cargar el ranking de la liga"
                    )
                }
            }
        }
    }

    // ================= HELPERS =================

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null, error = null) }
    }
}