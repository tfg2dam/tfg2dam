package com.simutrade.screens.main

import androidx.lifecycle.ViewModel
import com.simutrade.data.model.Asset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ================= SCREENS =================

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Market : Screen("market")
    object Trading : Screen("trading")
    object Rankings : Screen("rankings")
    object Challenges : Screen("challenges")
}

// ================= VIEWMODEL =================

class MainViewModel : ViewModel() {

    private val _currentPage = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentPage: StateFlow<Screen> = _currentPage.asStateFlow()

    private val _selectedAsset = MutableStateFlow<Asset?>(null)
    val selectedAsset: StateFlow<Asset?> = _selectedAsset.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentPage.value = screen
    }

    fun selectAsset(asset: Asset) {
        _selectedAsset.value = asset
        navigateTo(Screen.Trading)
    }

    fun clearSelectedAsset() {
        _selectedAsset.value = null
    }
}