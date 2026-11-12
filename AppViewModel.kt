package com.apz.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apz.manager.data.AppRepository
import com.apz.manager.domain.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AppsUiState(
    val loading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val error: String? = null
)

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(loading = true, error = null)
                _state.value = _state.value.copy(apps = repository.installedApps(), loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun setQuery(value: String) { _state.value = _state.value.copy(query = value) }
}
