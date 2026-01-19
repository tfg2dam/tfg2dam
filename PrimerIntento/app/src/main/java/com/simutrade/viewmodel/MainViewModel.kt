// ============================================
// VIEW MODEL PRINCIPAL - SimuTrade
// Archivo: app/src/main/java/com/simutrade/viewmodel/MainViewModel.kt
// ============================================

package com.simutrade.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.MockData
import com.simutrade.models.*
import com.simutrade.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application)

    // Estado de la aplicación
    @OptIn(InternalSerializationApi::class)
    private val _userData = MutableStateFlow(repository.getUserData())
    @OptIn(InternalSerializationApi::class)
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    @OptIn(InternalSerializationApi::class)
    private val _assets = MutableStateFlow(MockData.mockAssets)
    @OptIn(InternalSerializationApi::class)
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    @OptIn(InternalSerializationApi::class)
    private val _selectedAsset = MutableStateFlow<Asset?>(null)
    @OptIn(InternalSerializationApi::class)
    val selectedAsset: StateFlow<Asset?> = _selectedAsset.asStateFlow()

    private val _currentPage = MutableStateFlow("dashboard")
    val currentPage: StateFlow<String> = _currentPage.asStateFlow()

    init {
        // Actualizar precios periódicamente (simulación)
        // En una app real, esto vendría de una API
    }

    // Navegación
    fun navigateTo(page: String) {
        _currentPage.value = page
    }

    @OptIn(InternalSerializationApi::class)
    fun selectAsset(asset: Asset) {
        _selectedAsset.value = asset
        navigateTo("trading")
    }

    @OptIn(InternalSerializationApi::class)
    fun clearSelectedAsset() {
        _selectedAsset.value = null
    }

    // Operaciones de trading
    @OptIn(InternalSerializationApi::class)
    fun buyAsset(asset: Asset, quantity: Double, onResult: (OperationResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.buyAsset(_userData.value, asset, quantity)
            if (result is OperationResult.Success) {
                _userData.value = result.userData
            }
            onResult(result)
        }
    }

    @OptIn(InternalSerializationApi::class)
    fun sellAsset(assetId: String, quantity: Double, currentPrice: Double, onResult: (OperationResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.sellAsset(_userData.value, assetId, quantity, currentPrice)
            if (result is OperationResult.Success) {
                _userData.value = result.userData
            }
            onResult(result)
        }
    }

    // Cálculos
    @OptIn(InternalSerializationApi::class)
    fun getPortfolioValue(): Double {
        return repository.calculatePortfolioValue(_userData.value.portfolio)
    }

    @OptIn(InternalSerializationApi::class)
    fun getTotalValue(): Double {
        val portfolioValue = getPortfolioValue()
        return repository.calculateTotalValue(_userData.value.balance, portfolioValue)
    }

    @OptIn(InternalSerializationApi::class)
    fun getProfit(): Double {
        val totalValue = getTotalValue()
        return repository.calculateProfit(totalValue, _userData.value.initialBalance)
    }

    @OptIn(InternalSerializationApi::class)
    fun getProfitPercent(): Double {
        val totalValue = getTotalValue()
        return repository.calculateProfitPercent(totalValue, _userData.value.initialBalance)
    }

    fun getCurrentRank(): Rank {
        return MockData.getRankFromProfit(getProfit())
    }

    // Educación
    @OptIn(InternalSerializationApi::class)
    fun addEducationalReward(amount: Double) {
        viewModelScope.launch {
            val updatedData = repository.addBalance(_userData.value, amount)
            _userData.value = updatedData
        }
    }

    // Resetear cuenta
    @OptIn(InternalSerializationApi::class)
    fun resetAccount() {
        viewModelScope.launch {
            repository.resetUserData()
            _userData.value = repository.getUserData()
            navigateTo("dashboard")
        }
    }

    // Actualizar precios de la cartera
    @OptIn(InternalSerializationApi::class)
    fun updatePrices() {
        viewModelScope.launch {
            val priceMap = _assets.value.associate { it.id to it.currentPrice }
            val updatedData = repository.updatePortfolioPrices(_userData.value, priceMap)
            _userData.value = updatedData
            repository.saveUserData(updatedData)
        }
    }
}
