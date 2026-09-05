package com.indian.nutrition.tracker.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Slate700,
    onSecondary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
)

private val DarkColors = darkColorScheme(
    primary = Teal300,
    onPrimary = Teal900,
    primaryContainer = Teal800,
    onPrimaryContainer = Teal50,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate500,
)

/**
 * Reads battery-saver changes while the app is visible. Android does not
 * expose power-saver state through Compose, so the broadcast is bridged into
 * state here instead of only checking it once at startup.
 */
@Composable
private fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var powerSaveMode by remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }

    DisposableEffect(context, powerManager) {
        if (powerManager == null) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                        powerSaveMode = powerManager.isPowerSaveMode
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED,
            )
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    return powerSaveMode
}

/**
 * App theme follows the user's system light/dark preference and also switches
 * to dark surfaces whenever Android Battery Saver is active. On Android 12+
 * it uses the system dynamic palette; older devices use the app's teal/slate
 * palette as a compatible fallback.
 */
@Composable
fun NutritionTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val powerSaveMode = rememberPowerSaveMode()
    val useDarkTheme = darkTheme || powerSaveMode
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDarkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
