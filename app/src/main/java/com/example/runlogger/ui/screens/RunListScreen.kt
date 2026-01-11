package com.example.runlogger.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.runlogger.data.TreadmillRun
import com.example.runlogger.viewmodel.RunViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class to hold PB info for a run
data class RunPBInfo(
    val isPBDistance: Boolean = false,
    val isPBPace: Boolean = false,
    val isPBElevation: Boolean = false,
    val isPBDuration: Boolean = false
) {
    fun hasPB() = isPBDistance || isPBPace || isPBElevation || isPBDuration
    
    fun getBadges(): String {
        val badges = mutableListOf<String>()
        if (isPBDistance) badges.add("🏃")
        if (isPBPace) badges.add("⚡")
        if (isPBElevation) badges.add("🗻")
        if (isPBDuration) badges.add("💪")
        return badges.joinToString("")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunListScreen(
    viewModel: RunViewModel,
    onAddRunClick: () -> Unit,
    onRunClick: (Int) -> Unit,
    onSummaryClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMarathonTrainingClick: () -> Unit
) {
    val runs by viewModel.runs.collectAsState()
    val allRuns by viewModel.allRuns.collectAsState()
    val filterTargetHR by viewModel.filterTargetHR.collectAsState()
    val daysSinceBackup by viewModel.daysSinceBackup.collectAsState()
    val context = LocalContext.current
    
    // Check if backup reminder should show (never backed up OR 7+ days)
    val shouldShowBackupReminder = daysSinceBackup == null || daysSinceBackup!! >= 7
    
    // Calculate PBs per HR group
    val pbsByRunId = remember(allRuns) {
        calculatePBsPerHR(allRuns)
    }
    
    // State for delete confirmation dialog
    var runToDelete by remember { mutableStateOf<TreadmillRun?>(null) }
    
    // State for custom HR search dialog
    var showCustomHRDialog by remember { mutableStateOf(false) }
    var customHRInput by remember { mutableStateOf("") }
    
    // State for overflow menu
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    // Export CSV function
    fun exportCsv() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sortedRuns = allRuns.sortedBy { it.date }
        
        val csvContent = buildString {
            // Header row
            append("date,duration_minutes,target_hr_bpm,distance_miles,feet_climbed,final_speed_mph,final_incline_percent\n")
            
            // Data rows
            sortedRuns.forEach { run ->
                append(dateFormat.format(Date(run.date)))
                append(",")
                append(run.durationMinutes)
                append(",")
                append(run.targetHeartRate)
                append(",")
                append(String.format(Locale.US, "%.2f", run.distanceMiles))
                append(",")
                append(run.totalFeetClimbed)
                append(",")
                append(String.format(Locale.US, "%.1f", run.finalMinuteSpeed))
                append(",")
                append(String.format(Locale.US, "%.1f", run.finalMinuteIncline))
                append("\n")
            }
        }
        
        // Write to cache file
        val fileName = "run_fiercely_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvContent)
        
        // Create shareable URI via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Open share sheet
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Backup Runs"))
        
        // Record that a backup was initiated
        viewModel.recordBackup()
    }
    
    // Delete confirmation dialog
    if (runToDelete != null) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val runDate = dateFormat.format(Date(runToDelete!!.date))
        
        AlertDialog(
            onDismissRequest = { runToDelete = null },
            title = { Text("Delete Run") },
            text = { 
                Text("Are you sure you want to delete the run from $runDate? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        runToDelete?.let { viewModel.deleteRun(it) }
                        runToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { runToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Custom HR search dialog
    if (showCustomHRDialog) {
        val focusManager = LocalFocusManager.current
        
        AlertDialog(
            onDismissRequest = { 
                showCustomHRDialog = false
                customHRInput = ""
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom HR Filter") 
                }
            },
            text = { 
                OutlinedTextField(
                    value = customHRInput,
                    onValueChange = { newValue ->
                        customHRInput = newValue.filter { it.isDigit() }
                    },
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
                                viewModel.setFilterTargetHR(hr)
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
                TextButton(
                    onClick = {
                        customHRInput.toIntOrNull()?.let { hr ->
                            viewModel.setFilterTargetHR(hr)
                        }
                        showCustomHRDialog = false
                        customHRInput = ""
                    }
                ) {
                    Text("Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomHRDialog = false
                    customHRInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Fiercely")
                    }
                },
                actions = {
                    IconButton(onClick = onMarathonTrainingClick) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Marathon Training",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Calendar",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onSummaryClick) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Insights",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Overflow menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    showOverflowMenu = false
                                    exportCsv()
                                }
                            )
                        }
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
        ) {
            // Backup reminder banner (shows when 7+ days since last backup)
            if (shouldShowBackupReminder) {
                BackupReminderBanner(
                    daysSinceBackup = daysSinceBackup,
                    onBackupClick = { exportCsv() }
                )
            }
            
            // Prominent Add New Run Button
            Button(
                onClick = onAddRunClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Run",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Filter section - compact row of chips
            FilterSection(
                filterTargetHR = filterTargetHR,
                onFilterChange = { viewModel.setFilterTargetHR(it) },
                onClearFilter = { viewModel.clearFilter() },
                onCustomSearch = { showCustomHRDialog = true }
            )
            
            // Show count when filtered
            filterTargetHR?.let { hr ->
                val filterLabel = TreadmillRun.getDisplayString(hr)
                Text(
                    text = "${runs.size} run${if (runs.size != 1) "s" else ""} in $filterLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            
            if (runs.isEmpty()) {
                EmptyState(hasFilter = filterTargetHR != null)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = runs,
                        key = { it.id }
                    ) { run ->
                        SwipeableRunCard(
                            run = run,
                            pbInfo = pbsByRunId[run.id] ?: RunPBInfo(),
                            onClick = { onRunClick(run.id) },
                            onSwipeToDelete = { runToDelete = run }
                        )
                    }
                }
            }
        }
    }
}

// Calculate PBs per HR group
private fun calculatePBsPerHR(allRuns: List<TreadmillRun>): Map<Int, RunPBInfo> {
    if (allRuns.isEmpty()) return emptyMap()
    
    val result = mutableMapOf<Int, RunPBInfo>()
    
    // Group runs by target HR
    val runsByHR = allRuns.groupBy { it.targetHeartRate }
    
    // For each HR group, find the PB holders
    runsByHR.forEach { (_, runsInGroup) ->
        if (runsInGroup.isEmpty()) return@forEach
        
        // Find best in each category (with exclusion rules)
        val bestDistance = runsInGroup.maxByOrNull { it.distanceMiles }
        val bestPace = runsInGroup
            .filter { it.distanceMiles > 0 }
            .minByOrNull { it.durationMinutes.toFloat() / it.distanceMiles }
        // Exclude 0 feet climbed from elevation PB
        val bestElevation = runsInGroup
            .filter { it.totalFeetClimbed > 0 }
            .maxByOrNull { it.totalFeetClimbed }
        // Exclude 30 min duration from duration PB
        val bestDuration = runsInGroup
            .filter { it.durationMinutes != 30 }
            .maxByOrNull { it.durationMinutes }
        
        // Mark the PB holders
        runsInGroup.forEach { run ->
            val isPBDistance = run.id == bestDistance?.id
            val isPBPace = run.id == bestPace?.id
            val isPBElevation = run.id == bestElevation?.id
            val isPBDuration = run.id == bestDuration?.id
            
            if (isPBDistance || isPBPace || isPBElevation || isPBDuration) {
                result[run.id] = RunPBInfo(
                    isPBDistance = isPBDistance,
                    isPBPace = isPBPace,
                    isPBElevation = isPBElevation,
                    isPBDuration = isPBDuration
                )
            }
        }
    }
    
    return result
}

@Composable
private fun FilterSection(
    filterTargetHR: Int?,
    onFilterChange: (Int?) -> Unit,
    onClearFilter: () -> Unit,
    onCustomSearch: () -> Unit
) {
    val presetHRs = listOf(120, 130, 139, 140, 160)
    
    // First row: All + Preset HRs + Custom search
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" chip
        FilterChip(
            selected = filterTargetHR == null,
            onClick = { onClearFilter() },
            label = { Text("All", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        // Preset HR chips
        presetHRs.forEach { hr ->
            FilterChip(
                selected = filterTargetHR == hr,
                onClick = { onFilterChange(hr) },
                label = { Text("$hr", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
        
        // Custom search chip - same size as others
        val isCustomHRSelected = filterTargetHR?.let { hr ->
            !presetHRs.contains(hr) && !TreadmillRun.isCategory(hr)
        } ?: false
        FilterChip(
            selected = isCustomHRSelected,
            onClick = { onCustomSearch() },
            label = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Custom HR",
                        modifier = Modifier.size(14.dp)
                    )
                }
            },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
            )
        )
    }
    
    // Second row: Category chips (Races, Gabi, Other)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TreadmillRun.CATEGORY_OPTIONS.forEach { (value, label) ->
            FilterChip(
                selected = filterTargetHR == value,
                onClick = { onFilterChange(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRunCard(
    run: TreadmillRun,
    pbInfo: RunPBInfo,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    var shouldReset by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeToDelete()
                shouldReset = true
                false // Don't actually dismiss - we'll show dialog first
            } else {
                false
            }
        }
    )
    
    // Reset the swipe state after showing the dialog
    LaunchedEffect(shouldReset) {
        if (shouldReset) {
            dismissState.reset()
            shouldReset = false
        }
    }
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        RunCard(run = run, pbInfo = pbInfo, onClick = onClick)
    }
}

@Composable
private fun RunCard(
    run: TreadmillRun,
    pbInfo: RunPBInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(run.date))
    
    // Create share message
    fun shareRun() {
        val avgPace = run.durationMinutes / run.distanceMiles  // min per mile
        val paceMinutes = avgPace.toInt()
        val paceSeconds = ((avgPace - paceMinutes) * 60).toInt()
        val paceFormatted = "%d:%02d".format(paceMinutes, paceSeconds)
        
        val shareText = buildString {
            appendLine("🏃 Run on $formattedDate!")
            appendLine()
            appendLine("📏 Distance: ${"%.2f".format(run.distanceMiles)} miles")
            appendLine("🏎️ Avg Pace: $paceFormatted /mi")
            if (run.durationMinutes != 30) {
                appendLine("⏱️ Duration: ${run.durationMinutes} min")
            }
            if (run.totalFeetClimbed != 0) {
                appendLine("⛰️ Climbed: ${run.totalFeetClimbed} ft")
            }
            appendLine()
            append("#RunFiercely")
        }
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share your run")
        context.startActivity(shareIntent)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with date, PB badges, and share button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // PB Badges
                    if (pbInfo.hasPB()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pbInfo.getBadges(),
                            fontSize = 14.sp
                        )
                    }
                }
                IconButton(
                    onClick = { shareRun() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share run",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = "${run.durationMinutes} min",
                    isPB = pbInfo.isPBDuration
                )
                StatItem(
                    icon = Icons.Default.Favorite,
                    label = if (TreadmillRun.isCategory(run.targetHeartRate)) "Category" else "Target HR",
                    value = TreadmillRun.getDisplayString(run.targetHeartRate),
                    highlight = true
                )
                StatItem(
                    icon = Icons.Default.DirectionsRun,
                    label = "Distance",
                    value = String.format("%.2f mi", run.distanceMiles),
                    isPB = pbInfo.isPBDistance
                )
                StatItem(
                    icon = Icons.Default.Terrain,
                    label = "Climbed",
                    value = "${run.totalFeetClimbed} ft",
                    isPB = pbInfo.isPBElevation
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false,
    isPB: Boolean = false
) {
    // Determine colors - PB takes precedence for value color
    val iconColor = when {
        highlight -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val valueColor = when {
        isPB -> MaterialTheme.colorScheme.tertiary
        highlight -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = iconColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isPB) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasFilter) "No runs match this filter" else "No runs yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasFilter) "Try a different target HR" else "Tap 'New Run' to start",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BackupReminderBanner(
    daysSinceBackup: Int?,
    onBackupClick: () -> Unit
) {
    val message = when {
        daysSinceBackup == null -> "You haven't backed up your data yet"
        daysSinceBackup == 1 -> "Last backup was yesterday"
        else -> "It's been $daysSinceBackup days since your last backup"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Backup Reminder",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Button(
                onClick = onBackupClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Backup")
            }
        }
    }
}
