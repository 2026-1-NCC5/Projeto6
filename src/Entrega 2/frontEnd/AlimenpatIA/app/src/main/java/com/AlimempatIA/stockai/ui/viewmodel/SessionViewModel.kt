// ui/viewmodel/SessionViewModel.kt
package com.AlimempatIA.stockai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.AlimempatIA.stockai.data.api.RetrofitInstance
import com.AlimempatIA.stockai.data.api.dto.*

class SessionViewModel : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionUiState?>(null)
    val sessionState: StateFlow<SessionUiState?> = _sessionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSessionId: String? = null

    fun startSession(qrCode: String, userId: String, userName: String, sessionType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val request = SessionRequest(
                qrCode = qrCode,
                userId = userId,
                userName = userName,
                sessionType = sessionType
            )

            try {
                val response = RetrofitInstance.sessionApi.startSession(request)
                if (response.success) {
                    val sessionData = response.data
                    if (sessionData != null) {
                        currentSessionId = sessionData.sessionId
                        _sessionState.value = SessionUiState.Active(sessionData)
                        saveSessionId(sessionData.sessionId)
                    }
                } else {
                    _error.value = response.message ?: "Erro ao iniciar sessão"
                    _sessionState.value = SessionUiState.Error(response.message ?: "Erro ao iniciar sessão")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro de conexão"
                _sessionState.value = SessionUiState.Error(e.message ?: "Erro de conexão")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun endSession(productsScanned: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            val sessionId = getCurrentSessionId()
            if (sessionId == null) {
                _error.value = "Nenhuma sessão ativa"
                _sessionState.value = SessionUiState.Error("Nenhuma sessão ativa")
                _isLoading.value = false
                return@launch
            }

            val request = SessionEndRequest(
                sessionId = sessionId,
                productsScanned = productsScanned
            )

            try {
                val response = RetrofitInstance.sessionApi.endSession(request)
                if (response.success) {
                    val completedData = response.data
                    if (completedData != null) {
                        _sessionState.value = SessionUiState.Completed(completedData)
                        clearSessionId()
                    }
                } else {
                    _error.value = response.message ?: "Erro ao finalizar sessão"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro de conexão"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getActiveSession(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.sessionApi.getActiveSession(userId)
                if (response.success && response.data != null) {
                    currentSessionId = response.data.sessionId
                    _sessionState.value = SessionUiState.Active(response.data)
                }
            } catch (e: Exception) {

            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun saveSessionId(sessionId: String) {

        currentSessionId = sessionId
    }

    private fun getCurrentSessionId(): String? {
        return currentSessionId
    }

    private fun clearSessionId() {
        currentSessionId = null
    }
}

sealed class SessionUiState {
    data class Active(val data: SessionData) : SessionUiState()
    data class Completed(val data: CompletedSessionData) : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}