package com.indian.nutrition.tracker.ui.screens.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indian.nutrition.tracker.data.export.BackupDto
import com.indian.nutrition.tracker.data.export.JsonBackup
import com.indian.nutrition.tracker.data.export.toDomain
import com.indian.nutrition.tracker.data.repository.CustomFoodRepository
import com.indian.nutrition.tracker.data.repository.LogRepository
import com.indian.nutrition.tracker.data.repository.SettingsRepository
import com.indian.nutrition.tracker.data.repository.WaterRepository
import com.indian.nutrition.tracker.data.repository.WeightRepository
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Calculator/settings screen state: profile, targets, data management. */
class CalculatorViewModel(container: AppContainer) : ViewModel() {

    private val settingsRepository: SettingsRepository = container.settingsRepository
    private val logRepository: LogRepository = container.logRepository
    private val weightRepository: WeightRepository = container.weightRepository
    private val waterRepository: WaterRepository = container.waterRepository
    private val customFoodRepository: CustomFoodRepository = container.customFoodRepository

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dailyLogs: StateFlow<List<DailyLog>> = logRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weightLogs: StateFlow<List<WeightLog>> = weightRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waterLogs: StateFlow<List<WaterLog>> = waterRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customFoods: StateFlow<List<CustomFood>> = customFoodRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun saveSettings(settings: UserSettings) = viewModelScope.launch {
        settingsRepository.save(settings)
        _message.value = "Saved & applied to dashboard!"
    }

    /** Wipes food/water/weight logs; keeps profile targets (web parity). */
    fun clearLogs() = viewModelScope.launch {
        logRepository.clear()
        waterRepository.clear()
        weightRepository.clear()
        _message.value = "All logged data reset to 0."
    }

    /** Builds the JSON export document from current state. */
    fun exportJson(): String {
        val s = settings.value ?: return "{}"
        return JsonBackup.export(s, dailyLogs.value, weightLogs.value, waterLogs.value, customFoods.value)
    }

    /**
     * Import a parsed backup. [replace] = wipe existing logs first (web-data
     * migration path); merge = keep ids, weight rows stay one-per-date.
     */
    fun importBackup(backup: BackupDto, replace: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (replace) {
                logRepository.clear()
                waterRepository.clear()
                weightRepository.clear()
                customFoodRepository.clear()
            }
            settingsRepository.save(backup.settings.toDomain())
            logRepository.importAll(backup.dailyLogs.map { it.toDomain() })
            waterRepository.importAll(backup.waterLogs.map { it.toDomain() })
            weightRepository.importAll(backup.weightLogs.map { it.toDomain() })
            customFoodRepository.importAll(backup.customFoods.map { it.toDomain() })
            _message.value = "Backup imported (${backup.dailyLogs.size} foods, " +
                "${backup.weightLogs.size} weights, ${backup.waterLogs.size} waters, " +
                "${backup.customFoods.size} custom recipes)"
            onDone()
        }
    }
}
