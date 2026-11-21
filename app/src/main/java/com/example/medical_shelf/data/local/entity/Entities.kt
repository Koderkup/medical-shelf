package com.example.medical_shelf.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "boxes")
data class BoxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val template: BoxTemplate,
    val customName: String? = null,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = BoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["boxId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["boxId"])]
)
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boxId: Long,
    val name: String,
    val comment: String? = null,
    val quantity: Int = 1,
    val dosage: String? = null,
    val expirationDate: String? = null, // e.g. "05.2026" or "2026-05-31"
    val expirationTimestamp: Long? = null, // epoch millis for range queries
    val isOpened: Boolean = false,
    val openedTimestamp: Long? = null, // epoch millis when user clicked "Opened today"
    val openedExpiryDays: Int? = null, // e.g. 14, 30, 60, 90
    val photoPath: String? = null,
    val barcode: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = BoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["boxId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["boxId"])]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boxId: Long,
    val titleResKey: String? = null, // string resource key for default template
    val customTitle: String? = null, // user created item name
    val isChecked: Boolean = false,
    val category: String? = null
)

@Entity(
    tableName = "schedules",
    indices = [Index(value = ["medicineId"])]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long = 0,
    val medicineName: String,
    val dosage: String? = null,
    val timeHour: Int,
    val timeMinute: Int,
    val section: IntakeTimeSection,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val intervalDays: Int? = 1,
    val daysOfWeek: String? = null, // e.g. "1,3,5" (1=Mon, 7=Sun)
    val courseDurationDays: Int? = null,
    val startDateTimestamp: Long = System.currentTimeMillis(),
    val isLoudAlarm: Boolean = true,
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "intake_logs",
    indices = [
        Index(value = ["scheduleId"]),
        Index(value = ["medicineId"]),
        Index(value = ["dateString"])
    ]
)
data class IntakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val medicineId: Long,
    val medicineName: String,
    val intakeTimestamp: Long = System.currentTimeMillis(),
    val dateString: String, // e.g. "2026-03-30"
    val beforeFoodTimerMinutes: Int? = null, // 30, 45, 60 or null
    val isFoodTimerCompleted: Boolean = false
)

@Entity(tableName = "user_barcode_cache")
data class UserBarcodeCacheEntity(
    @PrimaryKey val barcode: String,
    val medicineName: String,
    val savedTimestamp: Long = System.currentTimeMillis()
)