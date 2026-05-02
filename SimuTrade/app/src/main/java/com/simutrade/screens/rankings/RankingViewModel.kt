package com.simutrade.screens.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.EntradaRanking
import com.simutrade.data.repository.RepositorioUsuario
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoUiRankings(
    val ranking: List<EntradaRanking> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

class RankingsViewModel : ViewModel() {

    private val repositorio = RepositorioUsuario()

    private val _uiState = MutableStateFlow(EstadoUiRankings())
    val uiState: StateFlow<EstadoUiRankings> = _uiState.asStateFlow()

    private var trabajoCarga: Job? = null

    init {
        cargarRanking()
    }

    // ================= CARGAR =================

    fun cargarRanking() {
        if (_uiState.value.cargando) return

        trabajoCarga?.cancel()

        trabajoCarga = viewModelScope.launch {

            _uiState.update {
                it.copy(cargando = true, error = null)
            }

            try {
                val datosRanking = repositorio.obtenerRanking()

                _uiState.update {
                    it.copy(
                        ranking = datosRanking,
                        cargando = false,
                        error = null
                    )
                }

            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = "No se pudo cargar el ranking. Inténtalo de nuevo"
                    )
                }
            }
        }
    }

    // ================= HELPERS =================

    fun obtenerPosicionUsuario(idUsuario: String): Int {
        return _uiState.value.ranking
            .indexOfFirst { it.id == idUsuario }
            .takeIf { it != -1 }
            ?.plus(1)
            ?: -1
    }
}