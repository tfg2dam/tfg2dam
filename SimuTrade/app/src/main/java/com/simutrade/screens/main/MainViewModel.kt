package com.simutrade.screens.main

import androidx.lifecycle.ViewModel
import com.simutrade.data.model.Asset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _currentPage = MutableStateFlow("dashboard")
    val currentPage: StateFlow<String> = _currentPage.asStateFlow()

    private val _selectedAsset = MutableStateFlow<Asset?>(null)
    val selectedAsset: StateFlow<Asset?> = _selectedAsset.asStateFlow()

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
}