package com.example.runlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runlogger.data.TreadmillRun
import com.example.runlogger.viewmodel.RunViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayRunData(
    val date: Calendar,
    val runs: List<TreadmillRun>,
    val totalDistance: Float,
    val totalDuration: Int,
    val totalElevation: Int,
    val avgPace: Float,
    val avgIncline: Float
)

data class MonthlyStats(
    val totalRuns: Int,
    val totalMiles: Float,
    val totalFeetClimbed: Int,
    val totalMinutes: Int,
    val bestSingleRunMiles: Float,
    val bestSingleRunDuration: Int,
    val bestPace: Float,
    val mostElevationSingleRun: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: RunViewModel,
    onNavigateBack: () -> Unit
) {
    val allRuns by viewModel.allRuns.collectAsState()
    
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayData by remember { mutableStateOf<DayRunData?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Group runs by date
    val runsByDate = remember(allRuns) {
        val calendar = Calendar.getInstance()
        allRuns.groupBy { run ->
            calendar.timeInMillis = run.date
            Triple(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
    
    // Calculate streak with 2-day gap tolerance and track rest days
    val (currentStreak, restDays) = remember(allRuns, runsByDate) {
        calculateStreakWithGaps(runsByDate)
    }
    
    // Get runs for displayed month
    val monthRuns = remember(allRuns, displayedMonth) {
        val calendar = Calendar.getInstance()
        val year = displayedMonth.get(Calendar.YEAR)
        val month = displayedMonth.get(Calendar.MONTH)
        
        allRuns.filter { run ->
            calendar.timeInMillis = run.date
            calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month
        }
    }
    
    // Calculate monthly stats
    val monthlyStats = remember(monthRuns) {
        if (monthRuns.isEmpty()) null
        else {
            val totalMiles = monthRuns.sumOf { it.distanceMiles.toDouble() }.toFloat()
            MonthlyStats(
                totalRuns = monthRuns.size,
                totalMiles = totalMiles,
                totalFeetClimbed = monthRuns.sumOf { it.totalFeetClimbed },
                totalMinutes = monthRuns.sumOf { it.durationMinutes },
                bestSingleRunMiles = monthRuns.maxOf { it.distanceMiles },
                bestSingleRunDuration = monthRuns.maxOf { it.durationMinutes },
                bestPace = monthRuns.minOf { run -> 
                    if (run.distanceMiles > 0) run.durationMinutes / run.distanceMiles else Float.MAX_VALUE 
                },
                mostElevationSingleRun = monthRuns.maxOf { it.totalFeetClimbed }
            )
        }
    }
    
    fun getDayData(year: Int, month: Int, day: Int): DayRunData? {
        val runs = runsByDate[Triple(year, month, day)] ?: return null
        if (runs.isEmpty()) return null
        
        val totalDistance = runs.sumOf { it.distanceMiles.toDouble() }.toFloat()
        val totalDuration = runs.sumOf { it.durationMinutes }
        val totalElevation = runs.sumOf { it.totalFeetClimbed }
        val avgPace = if (totalDistance > 0) totalDuration / totalDistance else 0f
        val avgIncline = if (totalDistance > 0) {
            (totalElevation / (totalDistance * 5280f)) * 100f
        } else 0f
        
        return DayRunData(
            date = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) },
            runs = runs,
            totalDistance = totalDistance,
            totalDuration = totalDuration,
            totalElevation = totalElevation,
            avgPace = avgPace,
            avgIncline = avgIncline
        )
    }
    
    // Build calendar days with rest day info
    val calendarDays = remember(displayedMonth, runsByDate, restDays) {
        val days = mutableListOf<CalendarDay>()
        val calendar = displayedMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        repeat(firstDayOfWeek) {
            days.add(CalendarDay(0, false, false, null, false))
        }
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        for (day in 1..daysInMonth) {
            val dayData = getDayData(year, month, day)
            val isRestDay = restDays.contains(Triple(year, month, day))
            days.add(CalendarDay(day, true, dayData != null, dayData, isRestDay))
        }
        
        val remainingDays = (7 - (days.size % 7)) % 7
        repeat(remainingDays) {
            days.add(CalendarDay(0, false, false, null, false))
        }
        
        days
    }
    
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearTitle = monthYearFormat.format(displayedMonth.time)
    val monthNameShort = SimpleDateFormat("MMMM", Locale.getDefault()).format(displayedMonth.time)
    
    val calendarRows = (calendarDays.size + 6) / 7
    val calendarHeight = (calendarRows * 48).dp
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Streaks")
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Streak card
            if (currentStreak > 0) {
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
                        Text(text = "🔥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$currentStreak day streak!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    displayedMonth = (displayedMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                }
                Text(
                    text = monthYearTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    displayedMonth = (displayedMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Day headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(calendarHeight),
                userScrollEnabled = false
            ) {
                items(calendarDays) { calendarDay ->
                    CalendarDayCell(
                        day = calendarDay,
                        onClick = { calendarDay.dayData?.let { selectedDayData = it } }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Monthly Stats
            if (monthlyStats != null) {
                Text(
                    text = "$monthNameShort Totals",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonthStatCard(Modifier.weight(1f), Icons.Default.DirectionsRun, "${monthlyStats.totalRuns}", "Runs")
                    MonthStatCard(Modifier.weight(1f), Icons.Default.DirectionsRun, String.format("%.1f", monthlyStats.totalMiles), "Miles")
                    MonthStatCard(Modifier.weight(1f), Icons.Default.Terrain, "${monthlyStats.totalFeetClimbed}", "Ft")
                    MonthStatCard(Modifier.weight(1f), Icons.Default.Timer, "${monthlyStats.totalMinutes}", "Min")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$monthNameShort Personal Bests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                val bestPaceMinutes = monthlyStats.bestPace.toInt()
                val bestPaceSeconds = ((monthlyStats.bestPace - bestPaceMinutes) * 60).toInt()
                val formattedBestPace = String.format("%d:%02d", bestPaceMinutes, bestPaceSeconds)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonalBestCard(Modifier.weight(1f), "🏃", "Longest", String.format("%.2f mi", monthlyStats.bestSingleRunMiles))
                    PersonalBestCard(Modifier.weight(1f), "⚡", "Best Pace", "$formattedBestPace /mi")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonalBestCard(Modifier.weight(1f), "🗻", "Elevation", "${monthlyStats.mostElevationSingleRun} ft")
                    PersonalBestCard(Modifier.weight(1f), "💪", "Duration", "${monthlyStats.bestSingleRunDuration} min")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏃", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "No runs logged in $monthNameShort yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        if (selectedDayData != null) {
            ModalBottomSheet(onDismissRequest = { selectedDayData = null }, sheetState = sheetState) {
                DayDetailSheet(selectedDayData!!) { selectedDayData = null }
            }
        }
    }
}

// Calculate streak allowing up to 2 day gaps, returns (streakLength, setOfRestDays)
private fun calculateStreakWithGaps(
    runsByDate: Map<Triple<Int, Int, Int>, List<TreadmillRun>>
): Pair<Int, Set<Triple<Int, Int, Int>>> {
    if (runsByDate.isEmpty()) return Pair(0, emptySet())
    
    val restDays = mutableSetOf<Triple<Int, Int, Int>>()
    var streak = 0
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    
    val todayKey = Triple(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    
    // Start from today or yesterday if no run today
    var startFound = false
    var daysBack = 0
    
    // Look for the most recent run (today or within last 2 days)
    while (daysBack <= 2 && !startFound) {
        calendar.timeInMillis = today.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -daysBack)
        val key = Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        if (runsByDate.containsKey(key)) {
            startFound = true
            // Mark days between today and this run as rest days (if any)
            for (i in 0 until daysBack) {
                val restCal = Calendar.getInstance()
                restCal.timeInMillis = today.timeInMillis
                restCal.add(Calendar.DAY_OF_MONTH, -i)
                val restKey = Triple(restCal.get(Calendar.YEAR), restCal.get(Calendar.MONTH), restCal.get(Calendar.DAY_OF_MONTH))
                if (!runsByDate.containsKey(restKey)) {
                    restDays.add(restKey)
                }
            }
        }
        daysBack++
    }
    
    if (!startFound) return Pair(0, emptySet())
    
    // Now count the streak going backwards, allowing up to 2 day gaps
    streak = 1
    var consecutiveGapDays = 0
    
    while (true) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val key = Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        
        if (runsByDate.containsKey(key)) {
            streak++
            consecutiveGapDays = 0
        } else {
            consecutiveGapDays++
            if (consecutiveGapDays > 2) {
                // Streak broken - more than 2 consecutive rest days
                break
            }
            // Check if there's a run within the next 2 days (looking backwards)
            var futureRunFound = false
            for (i in 1..2) {
                val futureCal = calendar.clone() as Calendar
                futureCal.add(Calendar.DAY_OF_MONTH, -i)
                val futureKey = Triple(futureCal.get(Calendar.YEAR), futureCal.get(Calendar.MONTH), futureCal.get(Calendar.DAY_OF_MONTH))
                if (runsByDate.containsKey(futureKey)) {
                    futureRunFound = true
                    break
                }
            }
            if (futureRunFound) {
                restDays.add(key)
            } else {
                break
            }
        }
    }
    
    return Pair(streak, restDays)
}

data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val hasRun: Boolean,
    val dayData: DayRunData?,
    val isRestDay: Boolean
)

@Composable
private fun CalendarDayCell(day: CalendarDay, onClick: () -> Unit) {
    val today = Calendar.getInstance()
    val isToday = day.isCurrentMonth && 
        day.dayOfMonth == today.get(Calendar.DAY_OF_MONTH) &&
        day.dayData?.date?.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
        day.dayData?.date?.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    
    val backgroundColor = when {
        day.hasRun -> MaterialTheme.colorScheme.primaryContainer
        day.isRestDay -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = day.hasRun, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day.isCurrentMonth && day.dayOfMonth > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(2.dp)) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (day.hasRun) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        day.hasRun -> MaterialTheme.colorScheme.onPrimaryContainer
                        day.isRestDay -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                when {
                    day.hasRun && day.dayData != null -> {
                        Icon(
                            Icons.Default.DirectionsRun,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            String.format("%.1f", day.dayData.totalDistance),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    day.isRestDay -> {
                        Text("🧘", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthStatCard(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PersonalBestCard(modifier: Modifier, emoji: String, title: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayDetailSheet(dayData: DayRunData, onDismiss: () -> Unit) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(dayData.date.time)
    val paceMinutes = dayData.avgPace.toInt()
    val paceSeconds = ((dayData.avgPace - paceMinutes) * 60).toInt()
    val formattedPace = String.format("%d:%02d", paceMinutes, paceSeconds)
    
    Column(Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsRun, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
        }
        Spacer(Modifier.height(16.dp))
        Text("${dayData.runs.size} run${if (dayData.runs.size > 1) "s" else ""} logged", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DayStatCard(Modifier.weight(1f), Icons.Default.DirectionsRun, "Distance", String.format("%.2f mi", dayData.totalDistance))
            DayStatCard(Modifier.weight(1f), Icons.Default.Timer, "Avg Pace", "$formattedPace /mi")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DayStatCard(Modifier.weight(1f), Icons.Default.Terrain, "Elevation", "${dayData.totalElevation} ft")
            DayStatCard(Modifier.weight(1f), Icons.Default.Terrain, "Avg Incline", String.format("%.2f%%", dayData.avgIncline))
        }
    }
}

@Composable
private fun DayStatCard(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
