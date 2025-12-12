package com.example.runlogger.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runlogger.viewmodel.RunViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow

data class WeeklyProgression(
    val weekNumber: Int,
    val date: Date,
    val targetDistance: Float,
    val estimatedDuration: Int,
    val growthFromStart: Float,
    val isFinal: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarathonTrainingScreen(
    viewModel: RunViewModel,
    onNavigateBack: () -> Unit
) {
    val latest139Run by viewModel.latest139HRRun.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Input state - will be pre-populated from latest 139 HR run
    var durationInput by remember { mutableStateOf("") }
    var distanceInput by remember { mutableStateOf("") }
    
    // Goal settings - configurable
    var goalDate by remember { mutableLongStateOf(getDefaultGoalDate()) }
    var goalDistanceInput by remember { mutableStateOf("22") }
    
    // Calculated results
    var progressionList by remember { mutableStateOf<List<WeeklyProgression>>(emptyList()) }
    var pace by remember { mutableFloatStateOf(0f) }
    var goalDuration by remember { mutableIntStateOf(0) }
    var weeksRemaining by remember { mutableIntStateOf(0) }
    var weeklyGrowth by remember { mutableFloatStateOf(0f) }
    var hasCalculated by remember { mutableStateOf(false) }
    
    // Pre-populate from latest 139 HR run when it loads
    LaunchedEffect(latest139Run) {
        latest139Run?.let { run ->
            if (durationInput.isEmpty() && distanceInput.isEmpty()) {
                durationInput = run.durationMinutes.toString()
                distanceInput = String.format(Locale.US, "%.2f", run.distanceMiles)
            }
        }
    }
    
    // Goal date picker
    val goalCalendar = Calendar.getInstance().apply { timeInMillis = goalDate }
    val goalDatePicker = remember(goalDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 12, 0, 0)
                goalDate = cal.timeInMillis
            },
            goalCalendar.get(Calendar.YEAR),
            goalCalendar.get(Calendar.MONTH),
            goalCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    fun calculateProgression() {
        val duration = durationInput.toIntOrNull() ?: return
        val distance = distanceInput.toFloatOrNull() ?: return
        val goalDistanceMiles = goalDistanceInput.toFloatOrNull() ?: return
        
        if (duration <= 0 || distance <= 0 || goalDistanceMiles <= 0) return
        
        // Calculate pace (minutes per mile)
        pace = duration.toFloat() / distance
        
        // Calculate goal duration
        goalDuration = (pace * goalDistanceMiles).toInt()
        
        // Get all Saturdays between today and goal date
        val saturdays = getSaturdays(System.currentTimeMillis(), goalDate)
        weeksRemaining = saturdays.size
        
        if (weeksRemaining < 1) return
        
        // Calculate weekly growth rate (exponential)
        // finalDistance = startDistance * (1 + rate)^weeks
        // rate = (finalDistance / startDistance)^(1/weeks) - 1
        weeklyGrowth = ((goalDistanceMiles / distance).toDouble().pow(1.0 / weeksRemaining) - 1).toFloat()
        
        // Generate progression list
        val progression = mutableListOf<WeeklyProgression>()
        var currentDistance = distance
        
        for (i in saturdays.indices) {
            val isLast = i == saturdays.lastIndex
            val weekDistance = if (isLast) goalDistanceMiles else currentDistance * (1 + weeklyGrowth)
            val weekDuration = (pace * weekDistance).toInt()
            val growthFromStart = if (i == 0) 0f else ((weekDistance / distance) - 1) * 100
            
            progression.add(
                WeeklyProgression(
                    weekNumber = i + 1,
                    date = saturdays[i],
                    targetDistance = weekDistance,
                    estimatedDuration = weekDuration,
                    growthFromStart = growthFromStart,
                    isFinal = isLast
                )
            )
            
            currentDistance = weekDistance
        }
        
        progressionList = progression
        hasCalculated = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marathon Training")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Last 139 HR Run",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (latest139Run != null) {
                            Text(
                                text = "Pre-filled from ${dateFormat.format(Date(latest139Run!!.date))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = durationInput,
                                onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Duration") },
                                suffix = { Text("min") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = distanceInput,
                                onValueChange = { distanceInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Distance") },
                                suffix = { Text("mi") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Goal Settings Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Goal Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { goalDatePicker.show() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateFormat.format(Date(goalDate)))
                            }
                            
                            OutlinedTextField(
                                value = goalDistanceInput,
                                onValueChange = { goalDistanceInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Goal") },
                                suffix = { Text("mi") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Calculate Button
            item {
                Button(
                    onClick = { calculateProgression() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calculate Progression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Results Section (only shown after calculation)
            if (hasCalculated && progressionList.isNotEmpty()) {
                // Stats Overview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Training Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem(
                                    icon = Icons.Default.Speed,
                                    value = formatPace(pace),
                                    label = "Pace"
                                )
                                StatItem(
                                    icon = Icons.Default.Schedule,
                                    value = formatDuration(goalDuration),
                                    label = "${goalDistanceInput.toFloatOrNull()?.toInt() ?: 22}mi Time"
                                )
                                StatItem(
                                    icon = Icons.Default.CalendarMonth,
                                    value = weeksRemaining.toString(),
                                    label = "Saturdays"
                                )
                                StatItem(
                                    icon = Icons.Default.TrendingUp,
                                    value = String.format("%.1f%%", weeklyGrowth * 100),
                                    label = "Weekly"
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Progress bar
                            val startDistance = distanceInput.toFloatOrNull() ?: 0f
                            val goalDistanceVal = goalDistanceInput.toFloatOrNull() ?: 22f
                            val progressPercent = (startDistance / goalDistanceVal).coerceIn(0f, 1f)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Starting: ${String.format("%.1f", startDistance)} mi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Goal: ${String.format("%.0f", goalDistanceVal)} mi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            LinearProgressIndicator(
                                progress = { progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
                
                // Progression Table Header
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Weekly Progression",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Table Header Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "WK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = "DATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = "DISTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = "EST. TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = "GROWTH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                }
                
                // Progression Rows
                itemsIndexed(progressionList) { index, week ->
                    ProgressionRow(
                        week = week,
                        dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault()),
                        isLast = index == progressionList.lastIndex
                    )
                    
                    if (index < progressionList.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Empty state when no 139 HR runs exist
            if (latest139Run == null && !hasCalculated) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No 139 HR runs found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Enter your run data manually above",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProgressionRow(
    week: WeeklyProgression,
    dateFormat: SimpleDateFormat,
    isLast: Boolean
) {
    val backgroundColor = if (week.isFinal) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val weekLabel = if (week.isFinal) "🏁" else week.weekNumber.toString()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                if (isLast) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = weekLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (week.isFinal) FontWeight.Bold else FontWeight.Normal,
            color = if (week.isFinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = dateFormat.format(week.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = String.format("%.1f mi", week.targetDistance),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (week.isFinal) FontWeight.Bold else FontWeight.SemiBold,
            color = if (week.isFinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = formatDuration(week.estimatedDuration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = if (week.growthFromStart > 0) "+${week.growthFromStart.toInt()}%" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (week.growthFromStart > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )
    }
}

// Helper function to get default goal date (March 21, 2026)
private fun getDefaultGoalDate(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(2026, Calendar.MARCH, 21, 12, 0, 0)
    return calendar.timeInMillis
}

// Helper function to get all Saturdays between two dates
private fun getSaturdays(startMillis: Long, endMillis: Long): List<Date> {
    val saturdays = mutableListOf<Date>()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startMillis
    
    // Move to next Saturday if not already Saturday
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
    if (daysUntilSaturday > 0) {
        calendar.add(Calendar.DAY_OF_MONTH, daysUntilSaturday)
    }
    
    // Collect all Saturdays up to and including end date
    while (calendar.timeInMillis <= endMillis) {
        saturdays.add(Date(calendar.timeInMillis))
        calendar.add(Calendar.DAY_OF_MONTH, 7)
    }
    
    return saturdays
}

// Helper function to format pace as MM:SS
private fun formatPace(minsPerMile: Float): String {
    val mins = minsPerMile.toInt()
    val secs = ((minsPerMile - mins) * 60).toInt()
    return String.format("%d:%02d", mins, secs)
}

// Helper function to format duration
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}


