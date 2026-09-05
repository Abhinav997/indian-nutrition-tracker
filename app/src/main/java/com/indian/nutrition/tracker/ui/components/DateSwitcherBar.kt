package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.util.DateUtils
import java.time.LocalDate

/**
 * Prev / next / today date switcher with friendly labels
 * ("Today", "Yesterday", "Tomorrow", else "Thu, Sep 4").
 */
@Composable
fun DateSwitcherBar(
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = DateUtils.today()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.testTag("prev-date-button")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Text(
                text = DateUtils.dayLabel(selectedDate, today),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(onClick = onNext, modifier = Modifier.testTag("next-date-button")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }
    }

    if (selectedDate != today) {
        androidx.compose.material3.TextButton(
            onClick = onToday,
            modifier = Modifier.padding(top = 2.dp).testTag("jump-to-today-btn"),
        ) {
            Text(text = "Jump to Today", style = MaterialTheme.typography.labelMedium)
        }
    }
}
