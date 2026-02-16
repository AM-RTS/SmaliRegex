package com.amrts.regexsmali

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SmaliConverterUiState(
    val input: String = "",
    val output: String = "",
    val advancedMode: Boolean = false,
    val includeBranches: Boolean = false,
    val excludeDebugInfo: Boolean = false,
    val isDarkTheme: Boolean? = null
) {
    fun toConversionOptions() = ConversionOptions(
        advancedWhitespaceMode = advancedMode,
        includeBranches = includeBranches,
        excludeDebugInfo = excludeDebugInfo
    )
}

class SmaliConverterViewModel : ViewModel() {

    private val converter = SmaliRegexConverter()

    private val _uiState = MutableStateFlow(SmaliConverterUiState())
    val uiState: StateFlow<SmaliConverterUiState> = _uiState.asStateFlow()

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
    }

    fun onIncludeBranchesToggled(enabled: Boolean) {
        _uiState.update { it.copy(includeBranches = enabled) }
        convert()
    }

    fun onExcludeDebugInfoToggled(enabled: Boolean) {
        _uiState.update { it.copy(excludeDebugInfo = enabled) }
        convert()
    }

    fun toggleTheme(systemIsDark: Boolean) {
        _uiState.update {
            val currentDark = it.isDarkTheme ?: systemIsDark
            it.copy(isDarkTheme = !currentDark)
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
}
