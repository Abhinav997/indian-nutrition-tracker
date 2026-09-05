package com.indian.nutrition.tracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indian.nutrition.tracker.data.repository.SettingsRepository
import com.indian.nutrition.tracker.data.repository.LogRepository
import com.indian.nutrition.tracker.data.repository.WaterRepository
import com.indian.nutrition.tracker.data.repository.WeightRepository
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import com.indian.nutrition.tracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Home/Today screen state: selected date, logs, water, weight, settings. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(container: AppContainer) : ViewModel() {

    private val logRepository: LogRepository = container.logRepository
    private val weightRepository: WeightRepository = container.weightRepository
    private val waterRepository: WaterRepository = container.waterRepository
    private val settingsRepository: SettingsRepository = container.settingsRepository

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dailyLogs: StateFlow<List<DailyLog>> = _selectedDate
        .flatMapLatest { logRepository.observeForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waterLogs: StateFlow<List<WaterLog>> = _selectedDate
        .flatMapLatest { waterRepository.observeForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weightLogs: StateFlow<List<WeightLog>> = weightRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDate(date: LocalDate) { _selectedDate.value = date }
    fun previousDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }
    fun nextDay() { _selectedDate.value = _selectedDate.value.plusDays(1) }
    fun jumpToToday() { _selectedDate.value = DateUtils.today() }

    fun deleteLog(id: String) = viewModelScope.launch { logRepository.delete(id) }

    fun addWater(amountMl: Int) = viewModelScope.launch {
        waterRepository.add(_selectedDate.value, amountMl, null)
    }

    fun deleteWater(id: String) = viewModelScope.launch { waterRepository.delete(id) }

    fun addWeight(weightKg: Double, note: String?) = viewModelScope.launch {
        weightRepository.upsert(_selectedDate.value, weightKg, note)
        // Web parity: the most recent weigh-in updates current weight.
        settings.value?.let { settingsRepository.save(it.copy(currentWeightKg = weightKg)) }
    }

    fun deleteWeight(id: String) = viewModelScope.launch { weightRepository.delete(id) }

    companion object {
        fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0
    }
}
