package com.example.runlogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TreadmillRunDao {
    
    @Query("SELECT * FROM treadmill_runs ORDER BY date DESC")
    fun getAllRuns(): Flow<List<TreadmillRun>>
    
    @Query("SELECT * FROM treadmill_runs WHERE id = :id")
    fun getRunById(id: Int): Flow<TreadmillRun?>
    
    @Query("SELECT * FROM treadmill_runs WHERE targetHeartRate = :targetHR ORDER BY date DESC")
    fun getRunsByExactHeartRate(targetHR: Int): Flow<List<TreadmillRun>>
    
    @Query("SELECT * FROM treadmill_runs WHERE targetHeartRate = 139 ORDER BY date DESC LIMIT 1")
    fun getLatest139HRRun(): Flow<TreadmillRun?>
    
    @Query("SELECT DISTINCT targetHeartRate FROM treadmill_runs ORDER BY targetHeartRate ASC")
    fun getDistinctTargetHeartRates(): Flow<List<Int>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: TreadmillRun)
    
    @Update
    suspend fun updateRun(run: TreadmillRun)
    
    @Delete
    suspend fun deleteRun(run: TreadmillRun)
    
    @Query("DELETE FROM treadmill_runs WHERE id = :id")
    suspend fun deleteRunById(id: Int)
}
