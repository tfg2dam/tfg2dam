package com.simutrade.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.datos.modelo.EntradaRanking
import com.simutrade.datos.repositorio.RepositorioUsuario
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla de ranking
data class EstadoUiRanking(
    val ranking: List<EntradaRanking> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

class RankingViewModel : ViewModel() {

    private val repositorio = RepositorioUsuario()

    private val _estadoUi = MutableStateFlow(EstadoUiRanking())
    val estadoUi: StateFlow<EstadoUiRanking> = _estadoUi.asStateFlow()

    private var trabajoCarga: Job? = null

    init { cargarRanking() }

    // ================= CARGAR =================

    // Obtiene la lista de usuarios ordenada por beneficio
    fun cargarRanking() {
        if (_estadoUi.value.cargando) return
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, error = null) }
            try {
                val datosRanking = repositorio.obtenerRanking()
                _estadoUi.update { it.copy(ranking = datosRanking, cargando = false, error = null) }
            } catch (_: Exception) {
                _estadoUi.update { it.copy(cargando = false, error = "No se pudo cargar el ranking. Inténtalo de nuevo") }
            }
        }
    }

    // ================= HELPERS =================

    // Devuelve la posición del usuario en el ranking (1 = primero), o -1 si no aparece
    fun obtenerPosicionUsuario(idUsuario: String): Int {
        return _estadoUi.value.ranking
            .indexOfFirst { it.id == idUsuario }
            .takeIf { it != -1 }
            ?.plus(1)
            ?: -1
    }
}