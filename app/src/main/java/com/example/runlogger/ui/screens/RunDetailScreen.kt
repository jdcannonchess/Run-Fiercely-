package com.example.runlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runlogger.data.TreadmillRun
import com.example.runlogger.viewmodel.RunViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    viewModel: RunViewModel,
    runId: Int,
    onNavigateBack: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    val currentRun by viewModel.currentRun.collectAsState()
    val allRuns by viewModel.allRuns.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Calculate PBs for this run
    val pbInfo = remember(currentRun, allRuns) {
        currentRun?.let { run ->
            calculatePBForRun(run, allRuns)
        } ?: RunPBInfo()
    }
    
    LaunchedEffect(runId) {
        viewModel.loadRun(runId)
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Run") },
            text = { Text("Are you sure you want to delete this run? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRunById(runId)
                        viewModel.clearCurrentRun()
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearCurrentRun()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(runId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        currentRun?.let { run ->
            val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(run.date))
            
            // Calculate derived stats
            val avgSpeedMph = if (run.durationMinutes > 0) {
                run.distanceMiles * 60f / run.durationMinutes
            } else 0f
            
            val avgPaceMinPerMile = if (run.distanceMiles > 0) {
                run.durationMinutes / run.distanceMiles
            } else 0f
            
            // Format pace as MM:SS
            val paceMinutes = avgPaceMinPerMile.toInt()
            val paceSeconds = ((avgPaceMinPerMile - paceMinutes) * 60).toInt()
            val formattedPace = String.format("%d:%02d", paceMinutes, paceSeconds)
            
            // Calculate average incline
            val avgInclinePercent = if (run.distanceMiles > 0) {
                (run.totalFeetClimbed / (run.distanceMiles * 5280f)) * 100f
            } else 0f
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact Date Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Row 1: Target HR, Duration, Distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = "${run.targetHeartRate}",
                        unit = "bpm",
                        label = "Target HR",
                        isHighlight = true
                    )
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = "${run.durationMinutes}",
                        unit = "min",
                        label = "Duration",
                        isPB = pbInfo.isPBDuration,
                        pbEmoji = "💪"
                    )
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = String.format("%.2f", run.distanceMiles),
                        unit = "mi",
                        label = "Distance",
                        isPB = pbInfo.isPBDistance,
                        pbEmoji = "🏃"
                    )
                }
                
                // Row 2: Avg Pace, Avg Speed, Final Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = formattedPace,
                        unit = "/mi",
                        label = "Avg Pace",
                        isPB = pbInfo.isPBPace,
                        pbEmoji = "⚡"
                    )
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = String.format("%.1f", avgSpeedMph),
                        unit = "mph",
                        label = "Avg Speed"
                    )
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = String.format("%.1f", run.finalMinuteSpeed),
                        unit = "mph",
                        label = "Final Speed"
                    )
                }
                
                // Row 3: Climbed, Avg Incline, Final Incline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = "${run.totalFeetClimbed}",
                        unit = "ft",
                        label = "Climbed",
                        isPB = pbInfo.isPBElevation,
                        pbEmoji = "🗻"
                    )
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = String.format("%.1f", avgInclinePercent),
                        unit = "%",
                        label = "Avg Incline"
                    )
                    CompactStatTile(
                        modifier = Modifier.weight(1f),
                        value = String.format("%.1f", run.finalMinuteIncline),
                        unit = "%",
                        label = "Final Incline"
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Edit button at bottom
                Button(
                    onClick = { onEditClick(runId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Run", style = MaterialTheme.typography.titleMedium)
                }
            }
        } ?: run {
            // Loading state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Calculate PB info for a single run
private fun calculatePBForRun(run: TreadmillRun, allRuns: List<TreadmillRun>): RunPBInfo {
    val runsInGroup = allRuns.filter { it.targetHeartRate == run.targetHeartRate }
    if (runsInGroup.isEmpty()) return RunPBInfo()
    
    val bestDistance = runsInGroup.maxByOrNull { it.distanceMiles }
    val bestPace = runsInGroup
        .filter { it.distanceMiles > 0 }
        .minByOrNull { it.durationMinutes.toFloat() / it.distanceMiles }
    val bestElevation = runsInGroup
        .filter { it.totalFeetClimbed > 0 }
        .maxByOrNull { it.totalFeetClimbed }
    val bestDuration = runsInGroup
        .filter { it.durationMinutes != 30 }
        .maxByOrNull { it.durationMinutes }
    
    return RunPBInfo(
        isPBDistance = run.id == bestDistance?.id,
        isPBPace = run.id == bestPace?.id,
        isPBElevation = run.id == bestElevation?.id,
        isPBDuration = run.id == bestDuration?.id
    )
}

@Composable
private fun CompactStatTile(
    modifier: Modifier = Modifier,
    value: String,
    unit: String,
    label: String,
    isHighlight: Boolean = false,
    isPB: Boolean = false,
    pbEmoji: String = ""
) {
    val containerColor = when {
        isHighlight -> MaterialTheme.colorScheme.secondaryContainer
        isPB -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val valueColor = when {
        isHighlight -> MaterialTheme.colorScheme.onSecondaryContainer
        isPB -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val labelColor = when {
        isHighlight -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // PB Badge in top-right corner
            if (isPB && pbEmoji.isNotEmpty()) {
                Text(
                    text = pbEmoji,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (isPB) FontWeight.ExtraBold else FontWeight.Bold,
                        color = valueColor
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = labelColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )
            }
        }
    }
}
