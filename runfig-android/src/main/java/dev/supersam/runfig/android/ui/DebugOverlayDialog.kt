package dev.supersam.runfig.android.ui

import android.R
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import dev.supersam.runfig.android.RunfigCache
import dev.supersam.runfig.android.api.DebugAction
import dev.supersam.runfig.android.api.DebugInfoProvider
import dev.supersam.runfig.android.api.DebugOverlayRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DebugOverlayDialog : DialogFragment() {

    companion object {
        const val TAG = "DebugOverlayDialog"
    }

    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    DebugOverlayScreen(
                        providers = DebugOverlayRegistry.infoProviders,
                        actions = DebugOverlayRegistry.actions,
                        onDismiss = { dismissAllowingStateLoss() },
                        dialogScope = dialogScope
                    )
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.window?.setBackgroundDrawableResource(R.color.transparent)
        }
        return dialog
    }

    override fun getTheme(): Int {
        return androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }
}

@Composable
fun PreferencesEditor() {
    var prefsMap by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var editValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSavedIndicator by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Define preference types for filtering
    val prefTypes = listOf("All", "Boolean", "Int", "Long", "Float", "String")
    var selectedTypeIndex by remember { mutableStateOf(0) }

    val lavenderColor = Color(0xFFE6E0F8)
    val lightGrayColor = Color(0xFFF2F2F2)

    LaunchedEffect(Unit) {
        prefsMap = RunfigCache.getAll() ?: emptyMap()
        editValues = prefsMap.mapValues { it.value.toString() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        color = lightGrayColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Preferences Editor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search preferences") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            )

            // Type filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(lavenderColor.copy(alpha = 0.5f))
            ) {
                prefTypes.forEachIndexed { index, type ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTypeIndex = index }
                            .background(
                                if (selectedTypeIndex == index) lavenderColor else Color.Transparent,
                                RoundedCornerShape(32.dp)
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (selectedTypeIndex == index && index == 0) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(45f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTypeIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // Filter preferences based on search and type
            val filteredPrefs = prefsMap.filter { (key, value) ->
                val matchesSearch = key.contains(searchQuery, ignoreCase = true)
                val matchesType = selectedTypeIndex == 0 || when (value) {
                    is Boolean -> selectedTypeIndex == 1
                    is Int -> selectedTypeIndex == 2
                    is Long -> selectedTypeIndex == 3
                    is Float -> selectedTypeIndex == 4
                    is String -> selectedTypeIndex == 5
                    else -> false
                }
                matchesSearch && matchesType
            }

            if (filteredPrefs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "No results",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(bottom = 8.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "No matching preferences found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredPrefs.forEach { (key, value) ->
                        PreferenceItem(
                            key = key,
                            value = value,
                            editValue = editValues[key] ?: "",
                            onValueChanged = { newValue ->
                                editValues = editValues.toMutableMap().apply { put(key, newValue) }
                            },
                            onSave = {
                                try {
                                    // Save with correct type
                                    val newValue = editValues[key] ?: return@PreferenceItem
                                    when (value) {
                                        is Boolean -> RunfigCache.putBoolean(key, newValue.toBoolean())
                                        is Int -> RunfigCache.putInt(key, newValue.toInt())
                                        is Long -> RunfigCache.putLong(key, newValue.toLong())
                                        is Float -> RunfigCache.putFloat(key, newValue.toFloat())
                                        is String -> RunfigCache.putString(key, newValue)
                                    }

                                    // Refresh data
                                    prefsMap = RunfigCache.getAll() ?: emptyMap()
                                } catch (e: Exception) {
                                    Log.e("PreferencesEditor", "Failed to save preference: $key", e)
                                }
                            },
                            showSaved = showSavedIndicator == key
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceItem(
    key: String,
    value: Any?,
    editValue: String,
    onValueChanged: (String) -> Unit,
    onSave: () -> Unit,
    showSaved: Boolean
) {
    val originalType = when (value) {
        is Boolean -> "Boolean"
        is Int -> "Int"
        is Long -> "Long"
        is Float -> "Float"
        is String -> "String"
        else -> "Unknown"
    }

    var expanded by remember { mutableStateOf(false) }
    val isModified = editValue != value.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with key and expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (value !is Boolean) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = originalType,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // For boolean values, show switch directly in header
                if (value is Boolean) {
                    val boolValue = remember(editValue) {
                        try { editValue.toBoolean() } catch (e: Exception) { false }
                    }

                    Switch(
                        checked = boolValue,
                        onCheckedChange = {
                            onValueChanged(it.toString())
                            // Auto-save for booleans
                            onSave()
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.DarkGray
                    )
                }
            }

            // Expanded content for non-boolean types
            AnimatedVisibility(
                visible = expanded && value !is Boolean,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Input field based on type
                    when (value) {
                        is Int, is Long -> {
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = onValueChanged,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        is Float -> {
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = onValueChanged,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                        is String -> {
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = onValueChanged,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Show save button if value is modified
                    if (isModified) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    // Reset to original value
                                    onValueChanged(value.toString())
                                }
                            ) {
                                Text("Reset")
                            }

                            Button(
                                onClick = onSave,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugOverlayScreen(
    providers: List<DebugInfoProvider>,
    actions: List<DebugAction>,
    onDismiss: () -> Unit,
    dialogScope: CoroutineScope
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Preferences", "Info", "Actions")

    val lavenderColor = Color(0xFFE6E0F8)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Debug Tools") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = lavenderColor,
                    titleContentColor = Color.DarkGray
                ),
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(lavenderColor.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, tab ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (selectedTab == index) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = Color.DarkGray,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .rotate(45f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = tab,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> PreferencesEditor()
                    1 -> InfoSection(providers, context)
                    2 -> ActionsSection(actions, dialogScope, snackbarHostState)
                }
            }
        }
    }
}

@Composable
fun InfoSection(providers: List<DebugInfoProvider>, context: Context) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(providers) { provider ->
            InfoCard(provider, context)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun InfoCard(provider: DebugInfoProvider, context: Context) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    text = provider.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            // Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        provider.Content(context = context)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionsSection(
    actions: List<DebugAction>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            DebugActionButton(action, scope, snackbarHostState)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun DebugActionButton(
    action: DebugAction,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Determine if this is a potentially destructive action
    val isDestructive = action.title.contains("clear", ignoreCase = true) ||
            action.title.contains("delete", ignoreCase = true) ||
            action.title.contains("reset", ignoreCase = true) ||
            action.title.contains("remove", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isDestructive)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
        else
            CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Optional description
                if (action.description?.isNotBlank() == true) {
                    Text(
                        text = action.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (isDestructive) {
                        showConfirmation = true
                    } else {
                        executeAction(action, context, scope, coroutineScope, snackbarHostState) {
                            isLoading = it
                        }
                    }
                },
                enabled = !isLoading,
                colors = if (isDestructive)
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                else
                    ButtonDefaults.buttonColors()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Execute")
                }
            }
        }
    }

    // Confirmation dialog for destructive actions
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Action") },
            text = {
                Text("Are you sure you want to execute '${action.title}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        executeAction(action, context, scope, coroutineScope, snackbarHostState) {
                            isLoading = it
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun executeAction(
    action: DebugAction,
    context: Context,
    scope: CoroutineScope,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    scope.launch {
        var message: String = ""
        try {
            action.onAction(context)
            message = "${action.title} executed successfully"
        } catch (e: Exception) {
            Log.e("DebugAction", "Action '${action.title}' failed", e)
            message = "Error: ${e.message?.take(100) ?: "Unknown error"}"
        } finally {
            setLoading(false)

            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }
}