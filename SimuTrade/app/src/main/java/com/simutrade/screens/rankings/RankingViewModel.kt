package com.simutrade.screens.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.LeaderboardEntry
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RankingsViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val data = repository.getLeaderboard()

                // 🔥 IMPORTANTE: ordenar
                _leaderboard.value = data.sortedByDescending { it.profit }

            } catch (e: Exception) {
                _error.value = "Error cargando ranking"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadLeaderboard()
    }

    // 🔥 POSICIÓN DEL USUARIO
    fun getUserPosition(userId: String): Int {
        val index = _leaderboard.value.indexOfFirst { it.id == userId }
        return if (index != -1) index + 1 else -1
    }

    // 🔥 DIFERENCIA CON EL SIGUIENTE
    fun getGapWithNext(index: Int): Double? {
        val list = _leaderboard.value
        return if (index < list.size - 1) {
            list[index].profit - list[index + 1].profit
        } else null
    }
}