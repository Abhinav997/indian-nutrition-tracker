package com.indian.nutrition.tracker.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indian.nutrition.tracker.data.repository.CustomFoodRepository
import com.indian.nutrition.tracker.data.repository.FoodRepository
import com.indian.nutrition.tracker.data.repository.LogRepository
import com.indian.nutrition.tracker.data.repository.OffCacheRepository
import com.indian.nutrition.tracker.data.repository.OffSearchRepository
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.FoodLookup
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.OffCacheProduct
import com.indian.nutrition.tracker.domain.model.ServingUnit
import com.indian.nutrition.tracker.util.DateUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import java.time.LocalDate

enum class SearchTab { SEARCH, FREQUENT, CUSTOM }

/** Search screen state: tabs, debounced OFF lookup, local master + custom CRUD. */
@OptIn(FlowPreview::class)
class SearchViewModel(
    container: AppContainer,
    private val loggingDate: LocalDate = DateUtils.today(),
) : ViewModel() {

    private val foodRepository: FoodRepository = container.foodRepository
    private val customFoodRepository: CustomFoodRepository = container.customFoodRepository
    private val logRepository: LogRepository = container.logRepository
    private val offCacheRepository: OffCacheRepository = container.offCacheRepository
    private val offSearchRepository: OffSearchRepository = container.offSearchRepository

    private val _tab = MutableStateFlow(SearchTab.SEARCH)
    val tab: StateFlow<SearchTab> = _tab.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null = All Sources. */
    private val _sourceFilter = MutableStateFlow<FoodSource?>(null)
    val sourceFilter: StateFlow<FoodSource?> = _sourceFilter.asStateFlow()

    private val _offLoading = MutableStateFlow(false)
    val offLoading: StateFlow<Boolean> = _offLoading.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _offError = MutableStateFlow<String?>(null)
    val offError: StateFlow<String?> = _offError.asStateFlow()

    private val _offFoods = MutableStateFlow<List<Food>>(emptyList())
    val offFoods: StateFlow<List<Food>> = _offFoods.asStateFlow()

    private val _savedMessage = MutableStateFlow<String?>(null)
    val savedMessage: StateFlow<String?> = _savedMessage.asStateFlow()

    val customFoods: StateFlow<List<CustomFood>> = customFoodRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val offCache: StateFlow<List<OffCacheProduct>> = offCacheRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Unified master list (custom > NIN > packaged > OFF cache), name-deduped. */
    val master: StateFlow<List<Food>> = combine(customFoods, offCache) { custom, cache ->
        foodRepository.masterList(custom, cache)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Frequently-logged foods (padded with NIN staples), re-ranked live. */
    val frequent: StateFlow<List<Food>> = combine(master, logRepository.observeAll()) { m, logs ->
        foodRepository.frequentlyUsed(m, logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Search-tab rows: local master (source-filtered) merged with OFF results. */
    val results: StateFlow<List<Food>> = combine(master, _query, _sourceFilter, _offFoods) { m, q, src, off ->
        val local = FoodLookup.search(m, q, src)
        mergeByName(local, off)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _query
                .map { it.trim() }
                .debounce(450)
                .distinctUntilChanged()
                .collect { q ->
                    if (_tab.value == SearchTab.SEARCH && q.length >= 3) runOffSearch(q)
                    else resetOff()
                }
        }
    }

    fun setTab(value: SearchTab) {
        _tab.value = value
        if (value != SearchTab.SEARCH) resetOff()
    }

    fun setQuery(value: String) { _query.value = value }

    fun setSourceFilter(value: FoodSource?) { _sourceFilter.value = value }

    fun consumeSavedMessage() { _savedMessage.value = null }

    private suspend fun runOffSearch(q: String) {
        _offLoading.value = true
        _offError.value = null
        try {
            val result = offSearchRepository.search(q)
            _offFoods.value = result.foods
            _offline.value = result.offline
            if (result.fromCache && result.foods.isEmpty()) {
                _offError.value = "Could not connect to Open Food Facts API"
            }
        } catch (_: Exception) {
            _offFoods.value = emptyList()
            _offline.value = true
            _offError.value = "Could not connect to Open Food Facts API"
        } finally {
            _offLoading.value = false
        }
    }

    private fun resetOff() {
        _offFoods.value = emptyList()
        _offLoading.value = false
        _offline.value = false
        _offError.value = null
    }

    /** Web parity merge (id-based) plus a name-dedupe fix (no duplicate rows). */
    private fun mergeByName(local: List<Food>, off: List<Food>): List<Food> {
        val seenIds = HashSet<String>()
        val seenNames = HashSet<String>()
        val out = mutableListOf<Food>()
        (local + off).forEach { food ->
            val name = food.name.lowercase().trim()
            if (seenIds.add(food.id) && seenNames.add(name)) out.add(food)
        }
        return out
    }

    fun addServing(food: Food, servingGrams: Int, quantity: Double, mealType: MealType) {
        val grams = max(1, (servingGrams * quantity).roundToInt())
        viewModelScope.launch {
            logRepository.add(
                date = loggingDate,
                foodId = food.id,
                foodName = food.name,
                source = food.source.name,
                servingGrams = grams,
                calories = (food.kcalPer100g * grams / 100).roundToInt(),
                protein = round1(food.proteinPer100g * grams / 100),
                carbs = round1(food.carbsPer100g * grams / 100),
                fat = round1(food.fatPer100g * grams / 100),
                mealType = mealType,
            )
            _savedMessage.value = "Added to ${mealType.displayName}"
        }
    }

    fun saveCustomFood(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        fiberPer100g: Double?,
        typicalServingDescription: String?,
        typicalServingGrams: Int?,
        notes: String?,
        servingUnit: ServingUnit,
        editId: String?,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val food = CustomFood(
                id = editId ?: "",
                name = cleanName,
                kcalPer100g = kcalPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                fiberPer100g = fiberPer100g,
                typicalServingDescription = typicalServingDescription?.trim()
                    ?.takeIf { it.isNotEmpty() },
                typicalServingGrams = typicalServingGrams?.coerceIn(1, 100_000),
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = now,
                servingUnit = servingUnit,
            )
            if (editId == null) customFoodRepository.add(food) else customFoodRepository.update(food)
        }
    }

    fun deleteCustomFood(id: String) = viewModelScope.launch { customFoodRepository.delete(id) }

    companion object {
        fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0
    }
}
