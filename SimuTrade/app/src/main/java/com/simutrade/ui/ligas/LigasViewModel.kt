package com.simutrade.ui.ligas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.simutrade.datos.modelo.Amigo
import com.simutrade.datos.modelo.EntradaRanking
import com.simutrade.datos.modelo.EstadoMiembro
import com.simutrade.datos.modelo.InvitacionLiga
import com.simutrade.datos.modelo.Liga
import com.simutrade.datos.modelo.MiembroLiga
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
        _estadoUi.update { it.copy(miUid = FirebaseAuth.getInstance().currentUser?.uid ?: "") }
        observarDatos()
    }

    // ================= OBSERVAR EN TIEMPO REAL =================

    private fun observarDatos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, error = null) }
            try {
                repositorio.observarMisLigas().collect { ligas ->
                    val ligaActualizada = _estadoUi.value.ligaSeleccionada?.let { seleccionada ->
                        ligas.find { it.id == seleccionada.id }
                    }
                    _estadoUi.update {
                        it.copy(
                            misLigas = ligas,
                            ligaSeleccionada = ligaActualizada ?: it.ligaSeleccionada,
                            cargando = false
                        )
                    }
                }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(cargando = false, error = "Error al cargar ligas") }
            }
        }

        viewModelScope.launch {
            try {
                repositorio.observarInvitaciones().collect { invitaciones ->
                    _estadoUi.update { it.copy(invitaciones = invitaciones) }
                }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(error = "Error al cargar invitaciones") }
            }
        }

        viewModelScope.launch {
            try {
                val amigos = repositorioAmigos.obtenerAmigos()
                _estadoUi.update { it.copy(misAmigos = amigos) }
            } catch (_: Exception) { }
        }
    }

    fun cargarDatos() {
        observarDatos()
    }

    // ================= CREAR =================

    fun crearLiga(nombre: String) {
        if (nombre.isBlank()) {
            _estadoUi.update { it.copy(error = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true) }
            val ligaId = repositorio.crearLiga(nombre)
            if (ligaId != null) {
                _estadoUi.update { it.copy(mensaje = "Liga creada") }
            } else {
                _estadoUi.update { it.copy(cargando = false, error = "Error al crear la liga") }
            }
        }
    }

    // ================= INVITAR =================

    // Invita a un amigo y actualiza localmente la liga al instante
    fun invitarAmigo(ligaId: String, amigoUid: String) {
        viewModelScope.launch {
            val yaEsMiembro = _estadoUi.value.ligaSeleccionada
                ?.miembros?.any { it.uid == amigoUid } == true

            if (yaEsMiembro) {
                _estadoUi.update { it.copy(mensaje = "Este usuario ya está en la liga") }
                return@launch
            }

            val exito = repositorio.invitarAmigo(ligaId, amigoUid)
            if (exito) {
                // Actualiza localmente para que desaparezca al instante de la lista
                _estadoUi.update { estado ->
                    val ligaActualizada = estado.ligaSeleccionada?.copy(
                        miembros = estado.ligaSeleccionada.miembros + MiembroLiga(
                            uid = amigoUid,
                            estado = EstadoMiembro.PENDIENTE
                        )
                    )
                    estado.copy(
                        ligaSeleccionada = ligaActualizada,
                        mensaje = "Invitación enviada"
                    )
                }
            } else {
                _estadoUi.update { it.copy(error = "Error al enviar la invitación") }
            }
        }
    }

    // ================= ACEPTAR / RECHAZAR =================

    fun aceptarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.aceptarInvitacion(ligaId)
            if (exito) {
                _estadoUi.update { it.copy(mensaje = "Te has unido a la liga") }
            } else {
                _estadoUi.update { it.copy(error = "Error al aceptar la invitación") }
            }
        }
    }

    fun rechazarInvitacion(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.rechazarInvitacion(ligaId)
            if (!exito) {
                _estadoUi.update { it.copy(error = "Error al rechazar la invitación") }
            }
        }
    }

    // ================= SALIR =================

    fun salirDeLiga(ligaId: String) {
        viewModelScope.launch {
            val exito = repositorio.salirDeLiga(ligaId)
            if (exito) {
                _estadoUi.update { estado ->
                    estado.copy(
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

    fun seleccionarLiga(liga: Liga) {
        _estadoUi.update { it.copy(ligaSeleccionada = liga) }
        cargarRankingLiga(liga.id)
    }

    fun deseleccionarLiga() {
        _estadoUi.update { it.copy(ligaSeleccionada = null, rankingLiga = emptyList()) }
    }

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

    fun limpiarMensaje() {
        _estadoUi.update { it.copy(mensaje = null, error = null) }
    }
}