package com.AlimempatIA.stockai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.AlimempatIA.stockai.data.api.RetrofitInstance
import com.AlimempatIA.stockai.data.api.dto.DashboardStatsData

class DashboardViewModel : ViewModel() {

    var dashboardData by mutableStateOf<DashboardStatsData?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                val response = RetrofitInstance.dashboardApi.getDashboardStats()

                android.util.Log.d("DashboardVM", "=== Dashboard API Response ===")
                android.util.Log.d("DashboardVM", "success: ${response.success}")
                android.util.Log.d("DashboardVM", "recognized: ${response.data?.recognized}")
                android.util.Log.d("DashboardVM", "notRecognized: ${response.data?.notRecognized}")
                android.util.Log.d("DashboardVM", "totalTeam: ${response.data?.totalTeam}")

                if (response.success && response.data != null) {
                    dashboardData = response.data
                } else {
                    error = response.message ?: "Erro ao carregar dados"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Erro de conexão"
                android.util.Log.e("DashboardVM", "Error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }
}