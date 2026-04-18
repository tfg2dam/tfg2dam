package com.simutrade.screens.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.LeaderboardEntry
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RankingsViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                _leaderboard.value = repository.getLeaderboard()
            } catch (e: Exception) {
                println("Error leaderboard: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}