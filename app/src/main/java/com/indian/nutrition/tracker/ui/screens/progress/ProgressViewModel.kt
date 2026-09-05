package com.indian.nutrition.tracker.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indian.nutrition.tracker.data.repository.LogRepository
import com.indian.nutrition.tracker.data.repository.SettingsRepository
import com.indian.nutrition.tracker.data.repository.WaterRepository
import com.indian.nutrition.tracker.data.repository.WeightRepository
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import com.indian.nutrition.tracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Progress screen state: metric, range (persisted), logs, weight actions. */
class ProgressViewModel(container: AppContainer) : ViewModel() {

    private val logRepository: LogRepository = container.logRepository
    private val weightRepository: WeightRepository = container.weightRepository
    private val waterRepository: WaterRepository = container.waterRepository
    private val settingsRepository: SettingsRepository = container.settingsRepository

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dailyLogs: StateFlow<List<DailyLog>> = logRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weightLogs: StateFlow<List<WeightLog>> = weightRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waterLogs: StateFlow<List<WaterLog>> = waterRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _metric = MutableStateFlow(ChartMetric.WEIGHT)
    val metric: StateFlow<ChartMetric> = _metric.asStateFlow()

    private val _dateRange = MutableStateFlow(DateRange.D14)
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    /** True once the user picked a range — stops the settings default overwriting it. */
    private var userChangedRange = false

    init {
        viewModelScope.launch {
            val saved = settingsRepository.settings.first()
            if (!userChangedRange) _dateRange.value = saved.defaultChartRange
        }
    }

    fun setMetric(value: ChartMetric) { _metric.value = value }

    fun setDateRange(value: DateRange) {
        userChangedRange = true
        _dateRange.value = value
        viewModelScope.launch {
            val saved = settingsRepository.settings.first()
            settingsRepository.save(saved.copy(defaultChartRange = value))
        }
    }

    fun deleteWeight(id: String) = viewModelScope.launch { weightRepository.delete(id) }

    fun addWeight(weightKg: Double, note: String?) = viewModelScope.launch {
        weightRepository.upsert(DateUtils.today(), weightKg, note)
        // Web parity: the most recent weigh-in updates current weight.
        val saved = settingsRepository.settings.first()
        settingsRepository.save(saved.copy(currentWeightKg = weightKg))
    }
}
