package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.WeightLog
import com.indian.nutrition.tracker.util.UnitConverters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightSheet(
    unitSystem: UnitSystem,
    currentWeightKg: Double,
    weightLog: WeightLog? = null,
    onDismiss: () -> Unit,
    onSave: (weightKg: Double, note: String?) -> Unit,
) {
    val startingWeightKg = weightLog?.weightKg ?: currentWeightKg
    var unit by rememberSaveable(weightLog?.id) { mutableStateOf(unitSystem) }
    var input by rememberSaveable(weightLog?.id) {
        mutableStateOf(
            if (unitSystem == UnitSystem.LB) UnitConverters.kgToLb(startingWeightKg).toString()
            else startingWeightKg.toString()
        )
    }
    var note by rememberSaveable(weightLog?.id) { mutableStateOf(weightLog?.note.orEmpty()) }

    val weightKg = if (unit == UnitSystem.LB) UnitConverters.lbToKg(input.toDoubleOrNull() ?: 0.0)
    else input.toDoubleOrNull() ?: 0.0
    val valid = weightKg in 20.0..350.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = Modifier.testTag("log-weight-modal-dialog"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (weightLog == null) "Log Body Weight" else "Edit Body Weight",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close-log-weight-btn"),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = unit == UnitSystem.KG, onClick = { unit = UnitSystem.KG }, label = { Text("kg") })
                FilterChip(selected = unit == UnitSystem.LB, onClick = { unit = UnitSystem.LB }, label = { Text("lb") })
            }

            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    input = v.filter { c -> c.isDigit() || c == '.' }.take(6)
                },
                label = { Text("Weight (${if (unit == UnitSystem.KG) "kg" else "lb"})") },
                singleLine = true,
                isError = !valid,
                supportingText = { if (!valid) Text("Enter a weight between 20 and 350 kg.") },
                modifier = Modifier.fillMaxWidth().testTag("weight-log-val-input"),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("weight-log-note-input"),
            )

            Button(
                onClick = { onSave(weightKg, note.trim().ifEmpty { null }) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().testTag("confirm-log-weight-btn"),
            ) { Text("Save weight") }
        }
    }
}
