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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application)

    private val _userData = MutableStateFlow(repository.getUserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    private val _assets = MutableStateFlow(MockData.mockAssets)
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _selectedAsset = MutableStateFlow<Asset?>(null)
    val selectedAsset: StateFlow<Asset?> = _selectedAsset.asStateFlow()

    private val _currentPage = MutableStateFlow("dashboard")
    val currentPage: StateFlow<String> = _currentPage.asStateFlow()

    fun navigateTo(page: String) {
        _currentPage.value = page
    }

    fun selectAsset(asset: Asset) {
        _selectedAsset.value = asset
        navigateTo("trading")
    }

    fun clearSelectedAsset() {
        _selectedAsset.value = null
    }

    fun buyAsset(asset: Asset, quantity: Double, onResult: (OperationResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.buyAsset(_userData.value, asset, quantity)
            if (result is OperationResult.Success) {
                _userData.value = result.userData
            }
            onResult(result)
        }
    }

    fun sellAsset(assetId: String, quantity: Double, currentPrice: Double, onResult: (OperationResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.sellAsset(_userData.value, assetId, quantity, currentPrice)
            if (result is OperationResult.Success) {
                _userData.value = result.userData
            }
            onResult(result)
        }
    }

    fun getPortfolioValue(): Double {
        return repository.calculatePortfolioValue(_userData.value.portfolio)
    }

    fun getTotalValue(): Double {
        return repository.calculateTotalValue(_userData.value.balance, getPortfolioValue())
    }

    fun getProfit(): Double {
        return repository.calculateProfit(getTotalValue(), _userData.value.initialBalance)
    }

    fun getProfitPercent(): Double {
        return repository.calculateProfitPercent(getTotalValue(), _userData.value.initialBalance)
    }

    fun getCurrentRank(): Rank {
        return MockData.getRankFromProfit(getProfit())
    }

    fun addEducationalReward(amount: Double) {
        viewModelScope.launch {
            val updatedData = repository.addBalance(_userData.value, amount)
            _userData.value = updatedData
        }
    }

    fun resetAccount() {
        viewModelScope.launch {
            repository.resetUserData()
            _userData.value = repository.getUserData()
            navigateTo("dashboard")
        }
    }

    fun updatePrices() {
        viewModelScope.launch {
            val priceMap = _assets.value.associate { it.id to it.currentPrice }
            val updatedData = repository.updatePortfolioPrices(_userData.value, priceMap)
            _userData.value = updatedData
            repository.saveUserData(updatedData)
        }
    }
}