package com.AlimempatIA.stockai.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.AlimempatIA.stockai.data.api.RetrofitInstance
import com.AlimempatIA.stockai.data.api.dto.*
import com.AlimempatIA.stockai.domain.model.*
import com.AlimempatIA.stockai.ui.theme.*

class ReportsViewModel : ViewModel() {

    private val _members = MutableStateFlow<List<MemberRecord>>(emptyList())
    val members: StateFlow<List<MemberRecord>> = _members.asStateFlow()

    private val _summary = MutableStateFlow<ReportsSummary?>(null)
    val summary: StateFlow<ReportsSummary?> = _summary.asStateFlow()

    private val _attributeMap = MutableStateFlow<List<AttributeMapDto>>(emptyList())
    val attributeMap: StateFlow<List<AttributeMapDto>> = _attributeMap.asStateFlow()

    private val _trendCounts = MutableStateFlow<TrendCountsDto?>(null)
    val trendCounts: StateFlow<TrendCountsDto?> = _trendCounts.asStateFlow()

    private val _roles = MutableStateFlow<List<String>>(emptyList())
    val roles: StateFlow<List<String>> = _roles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {

                val response = RetrofitInstance.reportsApi.getOperatorAttributes()


                android.util.Log.d("ReportsVM", "=== API Response ===")
                android.util.Log.d("ReportsVM", "summary: ${response.summary}")
                android.util.Log.d("ReportsVM", "teamAttributeAverage: ${response.summary.teamAttributeAverage}")
                android.util.Log.d("ReportsVM", "members count: ${response.members.size}")

                _summary.value = response.summary
                _attributeMap.value = response.attributeMap
                _trendCounts.value = response.trendCounts
                _members.value = response.members.map { it.toMemberRecord() }


                val rolesResponse = RetrofitInstance.reportsApi.getRoles()
                _roles.value = rolesResponse.data ?: emptyList()

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Erro ao carregar dados"
                android.util.Log.e("ReportsVM", "Error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterMembers(role: String?, trend: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitInstance.reportsApi.getOperatorAttributes(role, trend)
                _members.value = response.members.map { it.toMemberRecord() }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun MemberDataDto.toMemberRecord(): MemberRecord {
        return MemberRecord(
            id = id,
            name = name,
            role = role,
            roleColor = try {
                Color(android.graphics.Color.parseColor(roleColor))
            } catch (e: Exception) {
                PrimaryBlue
            },
            joinDate = joinDate,
            attributes = attributes.map { attr ->
                AttributeScore(
                    name = attr.name,
                    score = attr.score,
                    previousScore = attr.previousScore,
                    color = try {
                        Color(android.graphics.Color.parseColor(attr.color))
                    } catch (e: Exception) {
                        when (attr.name.lowercase()) {
                            "precisao" -> StatusGreen
                            "agilidade" -> PrimaryBlue
                            "organizacao" -> AccentCyan
                            else -> PrimaryBlue
                        }
                    }
                )
            },
            weeklyEvolution = weeklyEvolution,
            monthlyEvolution = monthlyEvolution,
            trend = when (trend.uppercase()) {
                "UP" -> Trend.UP
                "STABLE" -> Trend.STABLE
                else -> Trend.DOWN
            },
            leaderNote = leaderNote,
            actionPlan = actionPlan
        )
    }
}