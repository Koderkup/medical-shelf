package com.example.medical_shelf.data.repository

import android.content.Context
import android.net.Uri
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.BoxEntity
import com.example.medical_shelf.data.local.entity.ChecklistItemEntity
import com.example.medical_shelf.data.local.entity.IntakeLogEntity
import com.example.medical_shelf.data.local.entity.MedicineEntity
import com.example.medical_shelf.data.local.entity.ScheduleEntity
import com.example.medical_shelf.data.local.entity.UserBarcodeCacheEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val boxes: List<BoxEntity>,
    val medicines: List<MedicineEntity>,
    val checklistItems: List<ChecklistItemEntity>,
    val schedules: List<ScheduleEntity>,
    val intakeLogs: List<IntakeLogEntity>,
    val barcodeCache: List<UserBarcodeCacheEntity>
)

class BackupRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val boxes = db.boxDao().getAllBoxesList()
            val medicines = db.medicineDao().getAllMedicinesSync()
            val checklistItems = mutableListOf<ChecklistItemEntity>()
            boxes.forEach { box ->
                checklistItems.addAll(db.checklistDao().getChecklistForBoxSync(box.id))
            }
            val schedules = db.scheduleDao().getEnabledSchedulesSync()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayLogs = db.intakeLogDao().getLogsForDateSync(todayStr)

            val backup = BackupData(
                boxes = boxes,
                medicines = medicines,
                checklistItems = checklistItems,
                schedules = schedules,
                intakeLogs = todayLogs,
                barcodeCache = emptyList()
            )

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    gson.toJson(backup, writer)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup: BackupData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, BackupData::class.java)
                }
            } ?: return@withContext false

            if (backup.boxes.isNotEmpty()) {
                db.boxDao().insertBoxes(backup.boxes)
            }
            if (backup.medicines.isNotEmpty()) {
                db.medicineDao().insertMedicines(backup.medicines)
            }
            if (backup.checklistItems.isNotEmpty()) {
                db.checklistDao().insertChecklistItems(backup.checklistItems)
            }
            if (backup.schedules.isNotEmpty()) {
                db.scheduleDao().insertSchedules(backup.schedules)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}