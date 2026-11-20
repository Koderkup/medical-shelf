package com.example.medical_shelf.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medical_shelf.data.local.entity.BoxEntity
import com.example.medical_shelf.data.local.entity.ChecklistItemEntity
import com.example.medical_shelf.data.local.entity.IntakeLogEntity
import com.example.medical_shelf.data.local.entity.MedicineEntity
import com.example.medical_shelf.data.local.entity.ScheduleEntity
import com.example.medical_shelf.data.local.entity.UserBarcodeCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoxDao {
    @Query("SELECT * FROM boxes ORDER BY sortOrder ASC, id ASC")
    fun getAllBoxes(): Flow<List<BoxEntity>>

    @Query("SELECT * FROM boxes ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllBoxesList(): List<BoxEntity>

    @Query("SELECT * FROM boxes WHERE id = :id")
    fun getBoxById(id: Long): Flow<BoxEntity?>

    @Query("SELECT * FROM boxes WHERE id = :id")
    suspend fun getBoxByIdSync(id: Long): BoxEntity?

    @Query("SELECT COUNT(*) FROM boxes")
    suspend fun getBoxCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: BoxEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxes(boxes: List<BoxEntity>): List<Long>

    @Update
    suspend fun updateBox(box: BoxEntity): Int

    @Delete
    suspend fun deleteBox(box: BoxEntity): Int
}

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY createdTimestamp DESC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines ORDER BY createdTimestamp DESC")
    suspend fun getAllMedicinesSync(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE boxId = :boxId ORDER BY createdTimestamp DESC")
    fun getMedicinesByBox(boxId: Long): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun getMedicineById(id: Long): Flow<MedicineEntity?>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineByIdSync(id: Long): MedicineEntity?

    @Query("SELECT COUNT(*) FROM medicines")
    fun getMedicineCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun getMedicineCountSync(): Int

    @Query("SELECT * FROM medicines WHERE name LIKE '%' || :query || '%' OR comment LIKE '%' || :query || '%' ORDER BY createdTimestamp DESC")
    fun searchMedicines(query: String): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE expirationTimestamp IS NOT NULL AND expirationTimestamp <= :thresholdTimestamp")
    fun getExpiringMedicines(thresholdTimestamp: Long): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE expirationTimestamp IS NOT NULL AND expirationTimestamp <= :thresholdTimestamp")
    suspend fun getExpiringMedicinesSync(thresholdTimestamp: Long): List<MedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<MedicineEntity>): List<Long>

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity): Int

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity): Int
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE boxId = :boxId ORDER BY id ASC")
    fun getChecklistForBox(boxId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE boxId = :boxId ORDER BY id ASC")
    suspend fun getChecklistForBoxSync(boxId: Long): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>): List<Long>

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity): Int

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItemEntity): Int
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY timeHour ASC, timeMinute ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1 ORDER BY timeHour ASC, timeMinute ASC")
    fun getEnabledSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1 ORDER BY timeHour ASC, timeMinute ASC")
    suspend fun getEnabledSchedulesSync(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Query("SELECT COUNT(*) FROM schedules WHERE isEnabled = 1")
    fun getEnabledScheduleCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>): List<Long>

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity): Int

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity): Int
}

@Dao
interface IntakeLogDao {
    @Query("SELECT * FROM intake_logs WHERE dateString = :dateString ORDER BY intakeTimestamp DESC")
    fun getLogsForDate(dateString: String): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE dateString = :dateString ORDER BY intakeTimestamp DESC")
    suspend fun getLogsForDateSync(dateString: String): List<IntakeLogEntity>

    @Query("SELECT * FROM intake_logs WHERE scheduleId = :scheduleId AND dateString = :dateString LIMIT 1")
    suspend fun getLogForScheduleAndDate(scheduleId: Long, dateString: String): IntakeLogEntity?

    @Query("SELECT * FROM intake_logs WHERE isFoodTimerCompleted = 0 AND beforeFoodTimerMinutes IS NOT NULL")
    suspend fun getPendingFoodTimers(): List<IntakeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: IntakeLogEntity): Long

    @Update
    suspend fun updateLog(log: IntakeLogEntity): Int

    @Delete
    suspend fun deleteLog(log: IntakeLogEntity): Int
}

@Dao
interface UserBarcodeCacheDao {
    @Query("SELECT medicineName FROM user_barcode_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun findNameByBarcode(barcode: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: UserBarcodeCacheEntity): Long
}