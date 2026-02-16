package com.amrts.regexsmali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmaliConverterUiState(
    val input: String = "",
    val output: String = "",
    val advancedMode: Boolean = false,
    val includeBranches: Boolean = false,
    val excludeDebugInfo: Boolean = false,
    val isDarkTheme: Boolean? = null,
    val isLoaded: Boolean = false
) {
    fun toConversionOptions() = ConversionOptions(
        advancedWhitespaceMode = advancedMode,
        includeBranches = includeBranches,
        excludeDebugInfo = excludeDebugInfo
    )
}

class SmaliConverterViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val converter = SmaliRegexConverter()

    private val _uiState = MutableStateFlow(SmaliConverterUiState())
    val uiState: StateFlow<SmaliConverterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = preferencesRepository.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    advancedMode = saved.advancedMode,
                    includeBranches = saved.includeBranches,
                    excludeDebugInfo = saved.excludeDebugInfo,
                    isDarkTheme = saved.isDarkTheme,
                    isLoaded = true
                )
            }
        }
    }

    fun onInputChanged(newInput: String) {
        _uiState.update { it.copy(input = newInput) }
    }

    fun onPasteDetected(pastedText: String) {
        _uiState.update { it.copy(input = pastedText) }
        convert()
    }

    fun onAdvancedModeToggled(enabled: Boolean) {
        _uiState.update { it.copy(advancedMode = enabled) }
        convert()
        viewModelScope.launch { preferencesRepository.updateAdvancedMode(enabled) }
    }

    fun onIncludeBranchesToggled(enabled: Boolean) {
        _uiState.update { it.copy(includeBranches = enabled) }
        convert()
        viewModelScope.launch { preferencesRepository.updateIncludeBranches(enabled) }
    }

    fun onExcludeDebugInfoToggled(enabled: Boolean) {
        _uiState.update { it.copy(excludeDebugInfo = enabled) }
        convert()
        viewModelScope.launch { preferencesRepository.updateExcludeDebugInfo(enabled) }
    }

    fun toggleTheme(systemIsDark: Boolean) {
        _uiState.update {
            val currentDark = it.isDarkTheme ?: systemIsDark
            it.copy(isDarkTheme = !currentDark)
        }
        viewModelScope.launch {
            _uiState.value.isDarkTheme?.let { preferencesRepository.updateIsDarkTheme(it) }
        }
    }

    fun convert() {
        val state = _uiState.value
        if (state.input.isBlank()) {
            _uiState.update { it.copy(output = "") }
            return
        }
        val result = converter.convert(state.input, state.toConversionOptions())
        _uiState.update { it.copy(output = result) }
    }

    fun clear() {
        _uiState.update { it.copy(input = "", output = "") }
    }

    fun onFileImported(content: String) {
        _uiState.update { it.copy(input = content) }
        convert()
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SmaliConverterViewModel(preferencesRepository) as T
        }
    }
}
