package com.example.runlogger.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.runlogger.data.TreadmillRun
import com.example.runlogger.viewmodel.DateRangePreset
import com.example.runlogger.viewmodel.RunViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PBCategory {
    LONGEST_RUN,
    BEST_PACE,
    MOST_ELEVATION,
    LONGEST_DURATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: RunViewModel,
    onNavigateBack: () -> Unit
) {
    val allRuns by viewModel.allRuns.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    // Date range state from ViewModel (persisted)
    val selectedPreset by viewModel.summaryDatePreset.collectAsState()
    val customStartDate by viewModel.summaryCustomStartDate.collectAsState()
    val customEndDate by viewModel.summaryCustomEndDate.collectAsState()
    
    // HR filter state (single select)
    val presetHRs = listOf(120, 130, 139, 140, 160)
    var selectedHR by remember { mutableStateOf<Int?>(null) }
    var customHR by remember { mutableStateOf<Int?>(null) }
    
    // Custom HR dialog state
    var showCustomHRDialog by remember { mutableStateOf(false) }
    var customHRInput by remember { mutableStateOf("") }
    
    // PB history sheet state
    var selectedPBCategory by remember { mutableStateOf<PBCategory?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Calculate date ranges for presets
    fun getPresetDateRange(preset: DateRangePreset): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        val endOfToday = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        return when (preset) {
            DateRangePreset.ALL_TIME -> Pair(null, null)
            DateRangePreset.THIS_WEEK -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, endOfToday)
            }
            DateRangePreset.THIS_MONTH -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, endOfToday)
            }
            DateRangePreset.THIS_YEAR -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, endOfToday)
            }
            DateRangePreset.CUSTOM -> Pair(customStartDate, customEndDate)
        }
    }
    
    // Get current date range
    val (startDate, endDate) = getPresetDateRange(selectedPreset)
    
    // Determine active HR filter
    val activeHRFilter = customHR ?: selectedHR
    
    // Filter runs by date range AND HR
    val filteredRuns = allRuns.filter { run ->
        val afterStart = startDate == null || run.date >= startDate
        val beforeEnd = endDate == null || run.date <= endDate
        val matchesHR = activeHRFilter == null || run.targetHeartRate == activeHRFilter
        afterStart && beforeEnd && matchesHR
    }
    
    // Calculate totals from filtered runs
    val totalRuns = filteredRuns.size
    val totalDurationMinutes = filteredRuns.sumOf { it.durationMinutes }
    val totalMiles = filteredRuns.sumOf { it.distanceMiles.toDouble() }.toFloat()
    val totalFeetClimbed = filteredRuns.sumOf { it.totalFeetClimbed }
    
    // Calculate averages
    val avgSpeedMph = if (totalDurationMinutes > 0) {
        totalMiles * 60f / totalDurationMinutes
    } else 0f
    
    val avgPaceMinPerMile = if (totalMiles > 0) {
        totalDurationMinutes / totalMiles
    } else 0f
    
    // Format total duration as hours and minutes
    val totalHours = totalDurationMinutes / 60
    val remainingMinutes = totalDurationMinutes % 60
    val formattedDuration = if (totalHours > 0) "${totalHours}h ${remainingMinutes}m" else "${remainingMinutes}m"
    
    // Format pace as MM:SS
    val paceMinutes = avgPaceMinPerMile.toInt()
    val paceSeconds = ((avgPaceMinPerMile - paceMinutes) * 60).toInt()
    val formattedPace = String.format("%d:%02d", paceMinutes, paceSeconds)
    
    // Calculate personal bests
    val bestSingleRunMiles = if (filteredRuns.isNotEmpty()) filteredRuns.maxOf { it.distanceMiles } else 0f
    val bestSingleRunDuration = if (filteredRuns.isNotEmpty()) filteredRuns.maxOf { it.durationMinutes } else 0
    val mostElevationSingleRun = if (filteredRuns.isNotEmpty()) filteredRuns.maxOf { it.totalFeetClimbed } else 0
    val bestPace = if (filteredRuns.isNotEmpty()) {
        filteredRuns.minOf { run -> 
            if (run.distanceMiles > 0) run.durationMinutes / run.distanceMiles else Float.MAX_VALUE 
        }
    } else 0f
    
    // Format best pace
    val bestPaceMinutes = bestPace.toInt()
    val bestPaceSeconds = ((bestPace - bestPaceMinutes) * 60).toInt()
    val formattedBestPace = if (bestPace > 0 && bestPace < Float.MAX_VALUE) {
        String.format("%d:%02d", bestPaceMinutes, bestPaceSeconds)
    } else "—"
    
    // Date pickers
    val startCalendar = Calendar.getInstance().apply {
        customStartDate?.let { timeInMillis = it }
    }
    val endCalendar = Calendar.getInstance().apply {
        customEndDate?.let { timeInMillis = it }
    }
    
    val startDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                viewModel.setSummaryCustomStartDate(cal.timeInMillis)
                viewModel.setSummaryDatePreset(DateRangePreset.CUSTOM)
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    val endDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                viewModel.setSummaryCustomEndDate(cal.timeInMillis)
                viewModel.setSummaryDatePreset(DateRangePreset.CUSTOM)
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    // Custom HR dialog
    if (showCustomHRDialog) {
        val focusManager = LocalFocusManager.current
        
        AlertDialog(
            onDismissRequest = { 
                showCustomHRDialog = false
                customHRInput = ""
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom HR Filter") 
                }
            },
            text = { 
                OutlinedTextField(
                    value = customHRInput,
                    onValueChange = { newValue -> customHRInput = newValue.filter { it.isDigit() } },
                    label = { Text("Target HR") },
                    suffix = { Text("bpm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            customHRInput.toIntOrNull()?.let { hr ->
                                customHR = hr
                                selectedHR = null
                            }
                            showCustomHRDialog = false
                            customHRInput = ""
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    customHRInput.toIntOrNull()?.let { hr ->
                        customHR = hr
                        selectedHR = null
                    }
                    showCustomHRDialog = false
                    customHRInput = ""
                }) { Text("Filter") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomHRDialog = false
                    customHRInput = ""
                }) { Text("Cancel") }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insights")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Date Range Section - 6 equal width buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedPreset == DateRangePreset.ALL_TIME,
                    onClick = { viewModel.setSummaryDatePreset(DateRangePreset.ALL_TIME) },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedPreset == DateRangePreset.THIS_WEEK,
                    onClick = { viewModel.setSummaryDatePreset(DateRangePreset.THIS_WEEK) },
                    label = { Text("Week", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedPreset == DateRangePreset.THIS_MONTH,
                    onClick = { viewModel.setSummaryDatePreset(DateRangePreset.THIS_MONTH) },
                    label = { Text("Month", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedPreset == DateRangePreset.THIS_YEAR,
                    onClick = { viewModel.setSummaryDatePreset(DateRangePreset.THIS_YEAR) },
                    label = { Text("Year", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedPreset == DateRangePreset.CUSTOM,
                    onClick = { viewModel.setSummaryDatePreset(DateRangePreset.CUSTOM) },
                    label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            
            // Custom date pickers row
            if (selectedPreset == DateRangePreset.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { startDatePicker.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = customStartDate?.let { dateFormat.format(Date(it)) } ?: "Start",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    OutlinedButton(
                        onClick = { endDatePicker.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = customEndDate?.let { dateFormat.format(Date(it)) } ?: "End",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // HR Filter Section - Row 1: All + 5 presets + search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All HR
                FilterChip(
                    selected = activeHRFilter == null,
                    onClick = {
                        selectedHR = null
                        customHR = null
                    },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
                
                // Preset HRs
                presetHRs.forEach { hr ->
                    FilterChip(
                        selected = selectedHR == hr && customHR == null,
                        onClick = {
                            selectedHR = hr
                            customHR = null
                        },
                        label = { Text("$hr", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
                
                // Custom HR search button
                FilterChip(
                    selected = customHR != null && !TreadmillRun.isCategory(customHR ?: 0),
                    onClick = { showCustomHRDialog = true },
                    label = { 
                        if (customHR != null && !TreadmillRun.isCategory(customHR ?: 0)) {
                            Text("$customHR", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
            
            // HR Filter Section - Row 2: Category chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TreadmillRun.CATEGORY_OPTIONS.forEach { (value, label) ->
                    FilterChip(
                        selected = selectedHR == value && customHR == null,
                        onClick = {
                            selectedHR = value
                            customHR = null
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                }
            }
            
            // Stats Header - Compact
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsRun,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$totalRuns",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "runs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Totals Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = formattedDuration
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DirectionsRun,
                    label = "Distance",
                    value = String.format("%.1f mi", totalMiles)
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Terrain,
                    label = "Climbed",
                    value = "${totalFeetClimbed} ft"
                )
            }
            
            // Averages Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Avg Speed",
                    value = String.format("%.1f mph", avgSpeedMph)
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    label = "Avg Pace",
                    value = "$formattedPace /mi"
                )
            }
            
            // Personal Bests section
            if (totalRuns > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Personal Bests",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(tap for history)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PersonalBestCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🏃",
                        title = "Longest",
                        value = String.format("%.2f mi", bestSingleRunMiles),
                        onClick = { selectedPBCategory = PBCategory.LONGEST_RUN }
                    )
                    PersonalBestCard(
                        modifier = Modifier.weight(1f),
                        emoji = "⚡",
                        title = "Best Pace",
                        value = "$formattedBestPace",
                        onClick = { selectedPBCategory = PBCategory.BEST_PACE }
                    )
                    PersonalBestCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🗻",
                        title = "Elevation",
                        value = "$mostElevationSingleRun ft",
                        onClick = { selectedPBCategory = PBCategory.MOST_ELEVATION }
                    )
                    PersonalBestCard(
                        modifier = Modifier.weight(1f),
                        emoji = "💪",
                        title = "Duration",
                        value = "${bestSingleRunDuration}m",
                        onClick = { selectedPBCategory = PBCategory.LONGEST_DURATION }
                    )
                }
            }
            
            // Empty state message if no runs
            if (totalRuns == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No runs match your filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
        
        // PB History Bottom Sheet
        if (selectedPBCategory != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedPBCategory = null },
                sheetState = sheetState
            ) {
                PBHistorySheet(
                    category = selectedPBCategory!!,
                    runs = filteredRuns,
                    onDismiss = { selectedPBCategory = null }
                )
            }
        }
    }
}

@Composable
private fun PBHistorySheet(
    category: PBCategory,
    runs: List<TreadmillRun>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    val (title, emoji, sortedRuns) = when (category) {
        PBCategory.LONGEST_RUN -> Triple("Longest Runs", "🏃", runs.sortedByDescending { it.distanceMiles })
        PBCategory.BEST_PACE -> Triple("Best Pace Runs", "⚡", runs.filter { it.distanceMiles > 0 }.sortedBy { it.durationMinutes.toFloat() / it.distanceMiles })
        PBCategory.MOST_ELEVATION -> Triple("Most Elevation Runs", "🗻", runs.sortedByDescending { it.totalFeetClimbed })
        PBCategory.LONGEST_DURATION -> Triple("Longest Duration Runs", "💪", runs.sortedByDescending { it.durationMinutes })
    }
    
    val runsByDate = runs.sortedBy { it.date }
    val pbDates = mutableSetOf<Long>()
    var currentBest: Float = when (category) {
        PBCategory.LONGEST_RUN -> 0f
        PBCategory.BEST_PACE -> Float.MAX_VALUE
        PBCategory.MOST_ELEVATION -> 0f
        PBCategory.LONGEST_DURATION -> 0f
    }
    
    runsByDate.forEach { run ->
        val value = when (category) {
            PBCategory.LONGEST_RUN -> run.distanceMiles
            PBCategory.BEST_PACE -> if (run.distanceMiles > 0) run.durationMinutes.toFloat() / run.distanceMiles else Float.MAX_VALUE
            PBCategory.MOST_ELEVATION -> run.totalFeetClimbed.toFloat()
            PBCategory.LONGEST_DURATION -> run.durationMinutes.toFloat()
        }
        val isPB = when (category) {
            PBCategory.BEST_PACE -> value < currentBest
            else -> value > currentBest
        }
        if (isPB) {
            currentBest = value
            pbDates.add(run.date)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Top 10 • ⭐ = Personal Best when set", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(sortedRuns.take(10)) { index, run ->
                val isPBRun = pbDates.contains(run.date)
                val formattedValue = when (category) {
                    PBCategory.LONGEST_RUN -> String.format("%.2f mi", run.distanceMiles)
                    PBCategory.BEST_PACE -> {
                        val pace = run.durationMinutes.toFloat() / run.distanceMiles
                        String.format("%d:%02d /mi", pace.toInt(), ((pace - pace.toInt()) * 60).toInt())
                    }
                    PBCategory.MOST_ELEVATION -> "${run.totalFeetClimbed} ft"
                    PBCategory.LONGEST_DURATION -> "${run.durationMinutes} min"
                }
                
                PBHistoryItem(index + 1, dateFormat.format(Date(run.date)), formattedValue, isPBRun, run.targetHeartRate)
                
                if (index < sortedRuns.take(10).size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PBHistoryItem(rank: Int, date: String, value: String, isPB: Boolean, targetHR: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#$rank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp))
        
        if (isPB) Icon(Icons.Default.Star, "PB", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        else Spacer(modifier = Modifier.size(18.dp))
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(date, style = MaterialTheme.typography.bodyMedium)
            Text(TreadmillRun.getDisplayString(targetHR), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CompactStatCard(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PersonalBestCard(modifier: Modifier = Modifier, emoji: String, title: String, value: String, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 18.sp)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
