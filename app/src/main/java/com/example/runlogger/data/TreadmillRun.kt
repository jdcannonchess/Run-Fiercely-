package com.example.runlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treadmill_runs")
data class TreadmillRun(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,                    // Timestamp in milliseconds
    val durationMinutes: Int,          // Duration in minutes
    val targetHeartRate: Int,          // Target HR in bpm
    val distanceMiles: Float,          // Distance in miles
    val finalMinuteSpeed: Float,       // Speed of final minute in mph
    val totalFeetClimbed: Int,         // Total elevation gained in feet
    val finalMinuteIncline: Float      // Incline % of final minute
)

