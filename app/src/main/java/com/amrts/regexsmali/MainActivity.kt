package com.amrts.regexsmali

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

            RegexSmaliTheme(darkTheme = isDark) {
                SmaliConverterScreen(viewModel = vm, isDark = isDark, systemDark = systemDark)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SmaliRegex") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme(systemDark) }) {
                        Icon(
                            painter = painterResource(
                                id = if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
                            ),
                            contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InputSection(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            OptionsSection(uiState = uiState, viewModel = viewModel)

            ActionButtonsSection(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            OutputSection(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

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
    val syncedTextFieldValue = remember(uiState.input) {
        if (textFieldValue.text != uiState.input) TextFieldValue(uiState.input) else textFieldValue
    }
    textFieldValue = syncedTextFieldValue

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
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Paste", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = {
                    textFieldValue = TextFieldValue("")
                    viewModel.clear()
                },
                enabled = uiState.input.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { importLauncher.launch("*/*") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Import", style = MaterialTheme.typography.labelMedium)
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
            .height(200.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        placeholder = {
            Text(
                text = "Paste smali instructions here...",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun OptionsSection(
    uiState: SmaliConverterUiState,
    viewModel: SmaliConverterViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LabeledCheckbox(
            checked = uiState.advancedMode,
            onCheckedChange = viewModel::onAdvancedModeToggled,
            label = "Advanced whitespace mode"
        )
        LabeledCheckbox(
            checked = uiState.includeBranches,
            onCheckedChange = viewModel::onIncludeBranchesToggled,
            label = "Include branches"
        )
        LabeledCheckbox(
            checked = uiState.excludeDebugInfo,
            onCheckedChange = viewModel::onExcludeDebugInfoToggled,
            label = "Exclude debug info"
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = viewModel::convert,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("Convert")
        }

        FilledTonalButton(
            onClick = { exportLauncher.launch("regex_output.txt") },
            enabled = uiState.output.isNotEmpty(),
            shape = RoundedCornerShape(12.dp)
        ) {
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Copy Regex")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        OutlinedTextField(
            value = uiState.output,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            placeholder = {
                Text(
                    text = "Converted output will appear here...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
