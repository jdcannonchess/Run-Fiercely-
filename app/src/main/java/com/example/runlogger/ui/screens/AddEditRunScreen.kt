package com.example.runlogger.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.runlogger.data.TreadmillRun
import com.example.runlogger.viewmodel.RunViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRunScreen(
    viewModel: RunViewModel,
    runId: Int? = null,
    onNavigateBack: () -> Unit
) {
    val isEditing = runId != null && runId > 0
    val currentRun by viewModel.currentRun.collectAsState()
    
    // Form state
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var duration by remember { mutableStateOf(if (isEditing) "" else "30") }
    var selectedTargetHR by remember { mutableIntStateOf(140) } // Default to 140 bpm
    var customHRInput by remember { mutableStateOf("") }
    var isCustomHR by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf("") }
    var finalSpeed by remember { mutableStateOf("") }
    var feetClimbed by remember { mutableStateOf("") }
    var finalIncline by remember { mutableStateOf("") }
    
    // Load existing run data if editing
    LaunchedEffect(runId) {
        if (isEditing) {
            viewModel.loadRun(runId!!)
        }
    }
    
    // Populate fields when run is loaded
    LaunchedEffect(currentRun) {
        currentRun?.let { run ->
            date = run.date
            duration = run.durationMinutes.toString()
            // Check if the HR value is a preset, category, or custom
            val hr = run.targetHeartRate
            val isPresetOrCategory = TreadmillRun.PRESET_HR_VALUES.contains(hr) || 
                                      TreadmillRun.isCategory(hr)
            if (isPresetOrCategory) {
                selectedTargetHR = hr
                isCustomHR = false
            } else {
                isCustomHR = true
                customHRInput = hr.toString()
                selectedTargetHR = hr
            }
            distance = run.distanceMiles.toString()
            finalSpeed = run.finalMinuteSpeed.toString()
            feetClimbed = run.totalFeetClimbed.toString()
            finalIncline = run.finalMinuteIncline.toString()
        }
    }
    
    // Date picker
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = date }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                date = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Run" else "Today's Run") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearCurrentRun()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Micro header (only for new runs)
            if (!isEditing) {
                Text(
                    text = "Track your effort. Own your progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // Date selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(date)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    OutlinedButton(onClick = { datePickerDialog.show() }) {
                        Text("Change")
                    }
                }
            }
            
            // Duration & Target HR/Category row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(
                    modifier = Modifier.weight(1f),
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = "Duration",
                    suffix = "min",
                    icon = Icons.Default.Schedule
                )
                TargetHRSelector(
                    modifier = Modifier.weight(1f),
                    selectedValue = selectedTargetHR,
                    isCustom = isCustomHR,
                    customInput = customHRInput,
                    onValueSelected = { value ->
                        selectedTargetHR = value
                        isCustomHR = false
                    },
                    onCustomSelected = {
                        isCustomHR = true
                    },
                    onCustomInputChange = { input ->
                        customHRInput = input.filter { c -> c.isDigit() }
                        customHRInput.toIntOrNull()?.let { selectedTargetHR = it }
                    }
                )
            }
            
            // Distance & Final Speed row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(
                    modifier = Modifier.weight(1f),
                    value = distance,
                    onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Distance",
                    suffix = "mi",
                    icon = Icons.Default.DirectionsRun,
                    isDecimal = true
                )
                FormField(
                    modifier = Modifier.weight(1f),
                    value = finalSpeed,
                    onValueChange = { finalSpeed = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "End Speed",
                    suffix = "mph",
                    icon = Icons.Default.Speed,
                    isDecimal = true
                )
            }
            
            // Feet Climbed & Final Incline row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(
                    modifier = Modifier.weight(1f),
                    value = feetClimbed,
                    onValueChange = { feetClimbed = it.filter { c -> c.isDigit() } },
                    label = "Climbed",
                    suffix = "ft",
                    icon = Icons.Default.Terrain
                )
                FormField(
                    modifier = Modifier.weight(1f),
                    value = finalIncline,
                    onValueChange = { finalIncline = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "End Incline",
                    suffix = "%",
                    icon = Icons.Default.Terrain,
                    isDecimal = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button
            Button(
                onClick = {
                    // Validate and save
                    val durationVal = duration.toIntOrNull() ?: 0
                    val targetHRVal = if (isCustomHR) {
                        customHRInput.toIntOrNull() ?: selectedTargetHR
                    } else {
                        selectedTargetHR
                    }
                    val distanceVal = distance.toFloatOrNull() ?: 0f
                    val finalSpeedVal = finalSpeed.toFloatOrNull() ?: 0f
                    val feetClimbedVal = feetClimbed.toIntOrNull() ?: 0
                    val finalInclineVal = finalIncline.toFloatOrNull() ?: 0f
                    
                    viewModel.saveRun(
                        id = if (isEditing) runId else null,
                        date = date,
                        durationMinutes = durationVal,
                        targetHeartRate = targetHRVal,
                        distanceMiles = distanceVal,
                        finalMinuteSpeed = finalSpeedVal,
                        totalFeetClimbed = feetClimbedVal,
                        finalMinuteIncline = finalInclineVal
                    )
                    viewModel.clearCurrentRun()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = duration.isNotBlank() && (!isCustomHR || customHRInput.isNotBlank())
            ) {
                Text(
                    text = if (isEditing) "Update Run" else "Save Run",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun FormField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDecimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(76.dp),
        label = { Text(label) },
        suffix = { Text(suffix) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetHRSelector(
    modifier: Modifier = Modifier,
    selectedValue: Int,
    isCustom: Boolean,
    customInput: String,
    onValueSelected: (Int) -> Unit,
    onCustomSelected: () -> Unit,
    onCustomInputChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Build dropdown options: Preset HRs + Categories + Custom
    val presetOptions = TreadmillRun.PRESET_HR_VALUES.map { hr -> hr to "$hr bpm" }
    val categoryOptions = TreadmillRun.CATEGORY_OPTIONS
    
    // Determine display text
    val displayText = when {
        isCustom -> if (customInput.isBlank()) "Custom" else "$customInput bpm"
        else -> TreadmillRun.getDisplayString(selectedValue)
    }
    
    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            if (isCustom) {
                // Show text field for custom HR input
                OutlinedTextField(
                    value = customInput,
                    onValueChange = onCustomInputChange,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .height(76.dp)
                        .fillMaxWidth(),
                    label = { Text("Category") },
                    suffix = { Text("bpm") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            } else {
                // Show dropdown selector
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .height(76.dp)
                        .fillMaxWidth(),
                    label = { Text("Category") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    singleLine = true
                )
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Preset HR options
                presetOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
                
                // Divider-like spacing
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category options
                categoryOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.secondary
                            ) 
                        },
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
                
                // Divider-like spacing
                Spacer(modifier = Modifier.height(8.dp))
                
                // Custom option
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "Custom HR...",
                            color = MaterialTheme.colorScheme.tertiary
                        ) 
                    },
                    onClick = {
                        onCustomSelected()
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

