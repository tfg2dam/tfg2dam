package com.simutrade.screens.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.Reto
import com.simutrade.data.model.RetosData
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ChallengesViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _retosData = MutableStateFlow(RetosData())
    val retosData: StateFlow<RetosData> = _retosData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun cargarRetos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _retosData.value = repository.getRetosData()
            } catch (e: Exception) {
                println("Error retos: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRetosDelDia(): List<Reto> {
        val dia = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        return listOf(
            Reto("reto_${dia}_1", "Primera operación", "Realiza una compra o venta hoy", "📈", 2.0),
            Reto("reto_${dia}_2", "Diversifica", "Ten al menos 2 activos en cartera", "🎯", 3.0),
            Reto("reto_${dia}_3", "Inversor activo", "Opera con un activo de cada tipo", "⚡", 5.0)
        )
    }
}