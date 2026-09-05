package com.indian.nutrition.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indian.nutrition.tracker.di.AppContainer
import androidx.compose.runtime.Composable

/** Small helper to build a ViewModel with manual DI (no Hilt). */
@Composable
inline fun <reified T : ViewModel> appViewModel(
    container: AppContainer,
    crossinline create: (AppContainer) -> T,
): T {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create(container) as VM
    }
    return viewModel(factory = factory)
}
