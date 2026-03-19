package com.amrts.regexsmali

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amrts.regexsmali.ui.theme.CodeTextStyle
import com.amrts.regexsmali.ui.theme.RegexSmaliTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencesRepository = UserPreferencesRepository(applicationContext)
        setContent {
            val vm: SmaliConverterViewModel = viewModel(
                factory = SmaliConverterViewModel.Factory(preferencesRepository)
            )
            val uiState by vm.uiState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = uiState.isDarkTheme ?: systemDark

            if (uiState.isLoaded) {
                RegexSmaliTheme(darkTheme = isDark) {
                    SmaliConverterScreen(viewModel = vm, isDark = isDark, systemDark = systemDark)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmaliConverterScreen(
    viewModel: SmaliConverterViewModel,
    isDark: Boolean,
    systemDark: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("SmaliRegex") },
                colors = TopAppBarDefaults.topAppBarColors(),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme(systemDark) }) {
                        Icon(
                            painter = painterResource(
                                id = if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
                            ),
                            contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InputSection(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            HorizontalDivider()

            OptionsSection(uiState = uiState, viewModel = viewModel)

            ActionButtonsSection(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            AnimatedVisibility(
                visible = uiState.output.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()
                    OutputSection(
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InputSection(
    uiState: SmaliConverterUiState,
    viewModel: SmaliConverterViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    if (textFieldValue.text != uiState.input) {
        textFieldValue = TextFieldValue(
            text = uiState.input,
            selection = TextRange(uiState.input.length)
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)
                    ?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                textFieldValue = TextFieldValue(content)
                viewModel.onFileImported(content)
                scope.launch { snackbarHostState.showSnackbar("File imported") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Failed to import file") }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Smali Input",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val clip = clipboardManager.getText()?.text ?: ""
                    if (clip.isNotEmpty()) {
                        textFieldValue = TextFieldValue(clip)
                        viewModel.onPasteDetected(clip)
                    }
                }
            ) {
                Text("Paste")
            }

            OutlinedButton(
                onClick = {
                    textFieldValue = TextFieldValue("")
                    viewModel.clear()
                },
                enabled = uiState.input.isNotEmpty()
            ) {
                Text("Clear")
            }

            OutlinedButton(
                onClick = { importLauncher.launch("*/*") }
            ) {
                Text("Import")
            }
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val oldText = textFieldValue.text
            val newText = newValue.text
            textFieldValue = newValue
            viewModel.onInputChanged(newText)

            if (newText.length - oldText.length > 1) {
                viewModel.convert()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        textStyle = CodeTextStyle,
        placeholder = {
            Text(
                text = "Paste smali instructions here...",
                style = CodeTextStyle
            )
        },
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun OptionsSection(
    uiState: SmaliConverterUiState,
    viewModel: SmaliConverterViewModel
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        ListItem(
            headlineContent = { Text("Advanced whitespace") },
            supportingContent = { Text("Collapse spaces into \\s{N} patterns") },
            trailingContent = {
                Switch(
                    checked = uiState.advancedMode,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onAdvancedModeToggled(it)
                    }
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ListItem(
            headlineContent = { Text("Include branches") },
            supportingContent = { Text("Convert branch labels to regex") },
            trailingContent = {
                Switch(
                    checked = uiState.includeBranches,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onIncludeBranchesToggled(it)
                    }
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ListItem(
            headlineContent = { Text("Exclude debug info") },
            supportingContent = { Text("Filter .line, .local, .prologue directives") },
            trailingContent = {
                Switch(
                    checked = uiState.excludeDebugInfo,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onExcludeDebugInfoToggled(it)
                    }
                )
            }
        )
    }
}

@Composable
private fun ActionButtonsSection(
    uiState: SmaliConverterUiState,
    viewModel: SmaliConverterViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                    writer.write(uiState.output)
                }
                scope.launch { snackbarHostState.showSnackbar("Exported successfully") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Failed to export") }
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.convert()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Convert")
        }

        FilledTonalButton(
            onClick = { exportLauncher.launch("regex_output.txt") },
            enabled = uiState.output.isNotEmpty(),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Export")
        }
    }
}

@Composable
private fun OutputSection(
    uiState: SmaliConverterUiState,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Regex Output",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = {
                if (uiState.output.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(uiState.output))
                    scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                }
            },
            enabled = uiState.output.isNotEmpty(),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Copy Regex")
        }
    }

    OutlinedTextField(
        value = uiState.output,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        textStyle = CodeTextStyle,
        placeholder = {
            Text(
                text = "Converted output will appear here...",
                style = CodeTextStyle
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    )
}

