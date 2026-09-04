package com.indian.nutrition.tracker.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Utensils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.ui.appViewModel
import com.indian.nutrition.tracker.ui.components.CustomFoodDialog
import com.indian.nutrition.tracker.ui.components.ServingSheet

/**
 * Food search screen (port of the web FoodSearchScreen):
 * Search Database / Frequently Used / Custom tabs, debounced local + OFF
 * search, source filter chips, custom-food CRUD, and tap-to-log serving.
 */
@Composable
fun FoodSearchScreen(
    container: AppContainer,
    initialMeal: MealType = MealType.LUNCH,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel = appViewModel(container) { SearchViewModel(it) }
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sourceFilter by viewModel.sourceFilter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val frequent by viewModel.frequent.collectAsStateWithLifecycle()
    val customFoods by viewModel.customFoods.collectAsStateWithLifecycle()
    val offLoading by viewModel.offLoading.collectAsStateWithLifecycle()
    val offError by viewModel.offError.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val savedMessage by viewModel.savedMessage.collectAsStateWithLifecycle()

    var servingFood by remember { mutableStateOf<Food?>(null) }
    var servingMeal by remember { mutableStateOf(initialMeal) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var editingCustom by remember { mutableStateOf<CustomFood?>(null) }
    var deletingCustom by remember { mutableStateOf<CustomFood?>(null) }

    LaunchedEffect(offline) {
        if (offline) {
            snackbarHostState.showSnackbar("Offline — showing cached & curated results")
        }
    }
    LaunchedEffect(savedMessage) {
        savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSavedMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabRow(selectedTabIndex = tab.ordinal) {
                    SearchTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { viewModel.setTab(t) },
                            text = {
                                Text(
                                    when (t) {
                                        SearchTab.SEARCH -> "Search Database"
                                        SearchTab.FREQUENT -> "Frequently Used"
                                        SearchTab.CUSTOM -> "Custom (${customFoods.size})"
                                    }
                                )
                            },
                        )
                    }
                }

                if (tab == SearchTab.SEARCH) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Text("✕")
                                }
                            }
                        },
                        label = { Text("Search Indian foods, dal, roti…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = sourceFilter == null,
                            onClick = { viewModel.setSourceFilter(null) },
                            label = { Text("All Sources") },
                        )
                        FoodSource.entries.forEach { src ->
                            FilterChip(
                                selected = sourceFilter == src,
                                onClick = { viewModel.setSourceFilter(src) },
                                label = { Text(srcLabel(src)) },
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Logging for: Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = {
                        editingCustom = null
                        showCustomDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Custom Recipe")
                    }
                }
            }
        }

        when (tab) {
            SearchTab.SEARCH -> SearchResults(
                results = results,
                loading = offLoading,
                error = offError,
                onSelect = { food -> servingFood = food },
                onCreateCustom = {
                    editingCustom = null
                    showCustomDialog = true
                },
            )

            SearchTab.FREQUENT -> FrequentResults(frequent, onSelect = { food -> servingFood = food })

            SearchTab.CUSTOM -> CustomResults(
                foods = customFoods,
                onLog = { food ->
                    servingFood = Food(
                        id = food.id,
                        name = food.name,
                        source = FoodSource.CUSTOM,
                        kcalPer100g = food.kcalPer100g,
                        proteinPer100g = food.proteinPer100g,
                        carbsPer100g = food.carbsPer100g,
                        fatPer100g = food.fatPer100g,
                        fiberPer100g = food.fiberPer100g,
                        typicalServingDescription = food.typicalServingDescription,
                        typicalServingGrams = food.typicalServingGrams,
                        category = "Custom Foods",
                    )
                },
                onEdit = { food ->
                    editingCustom = food
                    showCustomDialog = true
                },
                onDelete = { deletingCustom = it },
                onCreate = {
                    editingCustom = null
                    showCustomDialog = true
                },
            )
        }
    }

    servingFood?.let { food ->
        ServingSheet(
            food = food,
            initialMeal = servingMeal,
            onDismiss = { servingFood = null },
            onSave = { grams, quantity, meal ->
                viewModel.addServing(food, grams, quantity, meal)
                servingFood = null
            },
        )
    }

    if (showCustomDialog) {
        CustomFoodDialog(
            foodToEdit = editingCustom,
            onDismiss = { showCustomDialog = false },
            onSave = { name, kcal, protein, carbs, fat, fiber, servingDesc, servingGrams, notes ->
                viewModel.saveCustomFood(
                    name = name,
                    kcalPer100g = kcal,
                    proteinPer100g = protein,
                    carbsPer100g = carbs,
                    fatPer100g = fat,
                    fiberPer100g = fiber,
                    typicalServingDescription = servingDesc,
                    typicalServingGrams = servingGrams,
                    notes = notes,
                    editId = editingCustom?.id,
                )
                showCustomDialog = false
            },
        )
    }

    deletingCustom?.let { food ->
        AlertDialog(
            onDismissRequest = { deletingCustom = null },
            title = { Text("Delete custom food?") },
            text = { Text("\"${food.name}\" will be removed. Past logs are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomFood(food.id)
                    deletingCustom = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCustom = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SearchResults(
    results: List<Food>,
    loading: Boolean,
    error: String?,
    onSelect: (Food) -> Unit,
    onCreateCustom: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Search Results (${results.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Searching Open Food Facts & packaged database…",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (error != null && results.isEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (results.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Text("No matching foods found", fontWeight = FontWeight.Bold)
                Text(
                    "Try \"Roti\", \"Dal\", \"Paneer\", \"Rice\", or create a custom recipe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onCreateCustom) { Text("+ Create Custom Food") }
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.id }) { food -> FoodRow(food, onSelect) }
            }
        }
    }
}

@Composable
private fun FrequentResults(foods: List<Food>, onSelect: (Food) -> Unit) {
    if (foods.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Log a few foods to build your frequently-used list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(foods, key = { it.id }) { food -> FoodRow(food, onSelect) }
    }
}

@Composable
private fun CustomResults(
    foods: List<CustomFood>,
    onLog: (CustomFood) -> Unit,
    onEdit: (CustomFood) -> Unit,
    onDelete: (CustomFood) -> Unit,
    onCreate: () -> Unit,
) {
    if (foods.isEmpty()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Icon(Icons.Filled.Utensils, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("No Custom Foods Created", fontWeight = FontWeight.Bold)
            Text(
                "Save your home recipes with customized macros.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCreate) { Text("+ Create First Custom Food") }
        }
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(foods, key = { it.id }) { food ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SourceBadge(FoodSource.CUSTOM)
                            food.typicalServingDescription?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                        Text(food.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${food.kcalPer100g.toInt()} kcal · ${food.proteinPer100g}g P · " +
                                "${food.carbsPer100g}g C · ${food.fatPer100g}g F",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onEdit(food) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${food.name}")
                    }
                    IconButton(onClick = { onDelete(food) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete ${food.name}",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(onClick = { onLog(food) }) { Text("Log") }
                }
            }
        }
    }
}

@Composable
private fun FoodRow(food: Food, onSelect: (Food) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        onClick = { onSelect(food) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            if (food.imageUrl != null) {
                AsyncImage(
                    model = food.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(food.source)
                    food.brand?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Text(food.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${food.kcalPer100g.toInt()} kcal · ${food.proteinPer100g}g protein · " +
                        "${food.carbsPer100g}g C · ${food.fatPer100g}g F",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(onClick = { onSelect(food) }, label = { Text("Log +") })
        }
    }
}

@Composable
private fun SourceBadge(source: FoodSource) {
    val (bg, fg, label) = when (source) {
        FoodSource.NIN -> Triple(MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer, "NIN / IFCT")
        FoodSource.OFF -> Triple(MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer, "Open Food Facts")
        FoodSource.CUSTOM -> Triple(MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer, "Custom")
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .background(bg)
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun srcLabel(source: FoodSource): String = when (source) {
    FoodSource.NIN -> "NIN / IFCT"
    FoodSource.OFF -> "Open Food Facts"
    FoodSource.CUSTOM -> "Custom Foods"
}
