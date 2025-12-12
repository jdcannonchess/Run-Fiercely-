package com.example.runlogger.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.runlogger.data.AppDatabase
import com.example.runlogger.data.TreadmillRun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DateRangePreset {
    ALL_TIME,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    CUSTOM
}

class RunViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dao = AppDatabase.getDatabase(application).treadmillRunDao()
    private val prefs = application.getSharedPreferences("run_logger_prefs", Context.MODE_PRIVATE)
    
    // Filter by specific target HR (null means show all)
    private val _filterTargetHR = MutableStateFlow<Int?>(null)
    val filterTargetHR: StateFlow<Int?> = _filterTargetHR.asStateFlow()
    
    // Get distinct target HRs for suggestions
    val distinctTargetHRs: StateFlow<List<Int>> = dao.getDistinctTargetHeartRates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // All runs (unfiltered) - used for summary stats
    val allRuns: StateFlow<List<TreadmillRun>> = dao.getAllRuns()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Latest 139 HR run - for marathon training pre-population
    val latest139HRRun: StateFlow<TreadmillRun?> = dao.getLatest139HRRun()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Filtered runs based on selected heart rate
    val runs: StateFlow<List<TreadmillRun>> = _filterTargetHR
        .flatMapLatest { targetHR ->
            if (targetHR == null) {
                dao.getAllRuns()
            } else {
                dao.getRunsByExactHeartRate(targetHR)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Current run being viewed/edited
    private val _currentRun = MutableStateFlow<TreadmillRun?>(null)
    val currentRun: StateFlow<TreadmillRun?> = _currentRun.asStateFlow()
    
    // Summary date range selection (persisted)
    private val _summaryDatePreset = MutableStateFlow(loadDatePreset())
    val summaryDatePreset: StateFlow<DateRangePreset> = _summaryDatePreset.asStateFlow()
    
    private val _summaryCustomStartDate = MutableStateFlow(loadCustomStartDate())
    val summaryCustomStartDate: StateFlow<Long?> = _summaryCustomStartDate.asStateFlow()
    
    private val _summaryCustomEndDate = MutableStateFlow(loadCustomEndDate())
    val summaryCustomEndDate: StateFlow<Long?> = _summaryCustomEndDate.asStateFlow()
    
    // Load saved preferences
    private fun loadDatePreset(): DateRangePreset {
        val presetName = prefs.getString("date_preset", DateRangePreset.ALL_TIME.name)
        return try {
            DateRangePreset.valueOf(presetName ?: DateRangePreset.ALL_TIME.name)
        } catch (e: Exception) {
            DateRangePreset.ALL_TIME
        }
    }
    
    private fun loadCustomStartDate(): Long? {
        val value = prefs.getLong("custom_start_date", -1L)
        return if (value == -1L) null else value
    }
    
    private fun loadCustomEndDate(): Long? {
        val value = prefs.getLong("custom_end_date", -1L)
        return if (value == -1L) null else value
    }
    
    // Save date range selection
    fun setSummaryDatePreset(preset: DateRangePreset) {
        _summaryDatePreset.value = preset
        prefs.edit().putString("date_preset", preset.name).apply()
        
        // Clear custom dates if not using custom preset
        if (preset != DateRangePreset.CUSTOM) {
            _summaryCustomStartDate.value = null
            _summaryCustomEndDate.value = null
            prefs.edit()
                .remove("custom_start_date")
                .remove("custom_end_date")
                .apply()
        }
    }
    
    fun setSummaryCustomStartDate(date: Long?) {
        _summaryCustomStartDate.value = date
        if (date != null) {
            prefs.edit().putLong("custom_start_date", date).apply()
        } else {
            prefs.edit().remove("custom_start_date").apply()
        }
    }
    
    fun setSummaryCustomEndDate(date: Long?) {
        _summaryCustomEndDate.value = date
        if (date != null) {
            prefs.edit().putLong("custom_end_date", date).apply()
        } else {
            prefs.edit().remove("custom_end_date").apply()
        }
    }
    
    fun setFilterTargetHR(targetHR: Int?) {
        _filterTargetHR.value = targetHR
    }
    
    fun clearFilter() {
        _filterTargetHR.value = null
    }
    
    fun loadRun(id: Int) {
        viewModelScope.launch {
            dao.getRunById(id).collect { run ->
                _currentRun.value = run
            }
        }
    }
    
    fun clearCurrentRun() {
        _currentRun.value = null
    }
    
    fun saveRun(
        id: Int? = null,
        date: Long,
        durationMinutes: Int,
        targetHeartRate: Int,
        distanceMiles: Float,
        finalMinuteSpeed: Float,
        totalFeetClimbed: Int,
        finalMinuteIncline: Float
    ) {
        viewModelScope.launch {
            val run = TreadmillRun(
                id = id ?: 0,
                date = date,
                durationMinutes = durationMinutes,
                targetHeartRate = targetHeartRate,
                distanceMiles = distanceMiles,
                finalMinuteSpeed = finalMinuteSpeed,
                totalFeetClimbed = totalFeetClimbed,
                finalMinuteIncline = finalMinuteIncline
            )
            if (id != null && id > 0) {
                dao.updateRun(run)
            } else {
                dao.insertRun(run)
            }
        }
    }
    
    fun deleteRun(run: TreadmillRun) {
        viewModelScope.launch {
            dao.deleteRun(run)
        }
    }
    
    fun deleteRunById(id: Int) {
        viewModelScope.launch {
            dao.deleteRunById(id)
        }
    }
}
