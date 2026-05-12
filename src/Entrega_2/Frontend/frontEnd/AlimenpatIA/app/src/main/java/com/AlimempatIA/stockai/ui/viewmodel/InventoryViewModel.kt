package com.AlimempatIA.stockai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.AlimempatIA.stockai.data.api.RetrofitInstance
import com.AlimempatIA.stockai.data.api.dto.CameraStatusDto
import com.AlimempatIA.stockai.data.api.dto.InventoryStatsDto
import com.AlimempatIA.stockai.data.mapper.toDomain
import com.AlimempatIA.stockai.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InventoryViewModel : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cameraStatus = MutableStateFlow<CameraStatusDto?>(null)
    val cameraStatus: StateFlow<CameraStatusDto?> = _cameraStatus.asStateFlow()

    private val _stats = MutableStateFlow<InventoryStatsDto?>(null)
    val stats: StateFlow<InventoryStatsDto?> = _stats.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _availableStatuses = MutableStateFlow<List<String>>(emptyList())
    val availableStatuses: StateFlow<List<String>> = _availableStatuses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory(search: String? = null, category: String? = null, status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = RetrofitInstance.productApi.getInventory(search, category, status)

                _cameraStatus.value = response.cameraStatus
                _stats.value = response.stats
                _availableCategories.value = response.availableCategories
                _availableStatuses.value = response.availableStatuses
                _products.value = response.products.map { it.toDomain() }

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Erro ao carregar inventário"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterProducts(search: String?, category: String?, status: String?) {
        loadInventory(search, category, status)
    }

    fun getProductById(productId: String): Product? {
        return _products.value.find { it.id == productId }
    }
}