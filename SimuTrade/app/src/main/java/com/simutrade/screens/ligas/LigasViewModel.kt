package com.simutrade.screens.ligas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.model.InvitacionLiga
import com.simutrade.data.model.Liga
import com.simutrade.data.repository.AmigosRepository
import com.simutrade.data.repository.LigasRepository
import com.simutrade.data.model.Amigo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LigasUiState(
    val misLigas: List<Liga> = emptyList(),
    val invitaciones: List<InvitacionLiga> = emptyList(),
    val ligaSeleccionada: Liga? = null,
    val rankingLiga: List<EntradaRanking> = emptyList(),
    val misAmigos: List<Amigo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingRanking: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class LigasViewModel : ViewModel() {

    private val repository = LigasRepository()
    private val amigosRepository = AmigosRepository()

    private val _uiState = MutableStateFlow(LigasUiState())
    val uiState: StateFlow<LigasUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ligas = repository.getMisLigas()
                val invitaciones = repository.getInvitaciones()
                val amigos = amigosRepository.getAmigos()

                _uiState.value = _uiState.value.copy(
                    misLigas = ligas,
                    invitaciones = invitaciones,
                    misAmigos = amigos,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar ligas"
                )
            }
        }
    }

    fun crearLiga(nombre: String) {
        if (nombre.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "El nombre no puede estar vacio")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val ligaId = repository.crearLiga(nombre)
            if (ligaId != null) {
                _uiState.value = _uiState.value.copy(mensaje = "Liga creada")
                cargarDatos()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al crear la liga"
                )
            }
        }
    }

    fun invitarAmigo(ligaId: String, amigoUid: String) {
        viewModelScope.launch {
            val exito = repository.invitarAmigo(ligaId, amigoUid)
            _uiState.value = _uiState.value.copy(
                mensaje = if (exito) "Invitacion enviada" else "Error al invitar"
            )
        }
    }

    fun aceptarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repository.aceptarInvitacion(ligaId)
            if (exito) {
                _uiState.value = _uiState.value.copy(mensaje = "Te has unido a la liga")
                cargarDatos()
            } else {
                _uiState.value = _uiState.value.copy(error = "Error al aceptar invitacion")
            }
        }
    }

    fun rechazarInvitacion(ligaId: String) {
        viewModelScope.launch {
            repository.rechazarInvitacion(ligaId)
            cargarDatos()
        }
    }

    fun salirDeLiga(ligaId: String) {
        viewModelScope.launch {
            val exito = repository.salirDeLiga(ligaId)
            if (exito) {
                _uiState.value = _uiState.value.copy(
                    mensaje = "Has salido de la liga",
                    ligaSeleccionada = null
                )
                cargarDatos()
            } else {
                _uiState.value = _uiState.value.copy(error = "Error al salir de la liga")
            }
        }
    }

    fun seleccionarLiga(liga: Liga) {
        _uiState.value = _uiState.value.copy(ligaSeleccionada = liga)
        cargarRankingLiga(liga.id)
    }

    fun deseleccionarLiga() {
        _uiState.value = _uiState.value.copy(
            ligaSeleccionada = null,
            rankingLiga = emptyList()
        )
    }

    private fun cargarRankingLiga(ligaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRanking = true)
            val ranking = repository.getRankingLiga(ligaId)
            _uiState.value = _uiState.value.copy(
                rankingLiga = ranking,
                isLoadingRanking = false
            )
        }
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(mensaje = null, error = null)
    }
}