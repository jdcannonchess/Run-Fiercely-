package com.example.runlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treadmill_runs")
data class TreadmillRun(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,                    // Timestamp in milliseconds
    val durationMinutes: Int,          // Duration in minutes
    val targetHeartRate: Int,          // Target HR in bpm (or negative for categories)
    val distanceMiles: Float,          // Distance in miles
    val finalMinuteSpeed: Float,       // Speed of final minute in mph
    val totalFeetClimbed: Int,         // Total elevation gained in feet
    val finalMinuteIncline: Float      // Incline % of final minute
) {
    companion object {
        // Category constants (negative values to distinguish from HR values)
        const val CATEGORY_RACES = -1
        const val CATEGORY_GABI = -2
        const val CATEGORY_OTHER = -3

        // Preset HR values
        val PRESET_HR_VALUES = listOf(120, 130, 139, 140, 160)

        /**
         * Returns the display name for a category value, or null if it's a regular HR value
         */
        fun getCategoryName(value: Int): String? = when (value) {
            CATEGORY_RACES -> "Races"
            CATEGORY_GABI -> "Gabi"
            CATEGORY_OTHER -> "Other"
            else -> null
        }

        /**
         * Returns true if the value represents a category (not a HR value)
         */
        fun isCategory(value: Int): Boolean = value < 0

        /**
         * Returns the display string for a target HR or category
         */
        fun getDisplayString(value: Int): String {
            return getCategoryName(value) ?: "$value bpm"
        }

        /**
         * All category options for selection
         */
        val CATEGORY_OPTIONS = listOf(
            CATEGORY_RACES to "Races",
            CATEGORY_GABI to "Gabi",
            CATEGORY_OTHER to "Other"
        )
    }
}

