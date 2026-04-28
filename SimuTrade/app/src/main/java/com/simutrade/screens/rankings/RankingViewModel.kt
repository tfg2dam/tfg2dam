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

    private val _uiState = MutableStateFlow(
        EstadoUiRankings()
    )

    val uiState: StateFlow<EstadoUiRankings> =
        _uiState.asStateFlow()

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
                it.copy(
                    cargando = true,
                    error = null
                )
            }

            try {
                val datosRanking =
                    repositorio.obtenerRanking()

                _uiState.update {
                    it.copy(
                        ranking = datosRanking
                            .sortedByDescending { entrada ->
                                entrada.beneficio
                            },
                        cargando = false,
                        error = null
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = e.message
                            ?: "Error cargando ranking"
                    )
                }
            }
        }
    }

    // ================= RECARGAR =================

    fun recargar() {
        cargarRanking()
    }

    // ================= HELPERS =================

    fun obtenerPosicionUsuario(
        idUsuario: String
    ): Int {

        val indice =
            _uiState.value.ranking.indexOfFirst {
                it.id == idUsuario
            }

        return if (indice != -1) {
            indice + 1
        } else {
            -1
        }
    }

    // ================= LIMPIEZA =================

    override fun onCleared() {
        super.onCleared()
        trabajoCarga?.cancel()
    }
}