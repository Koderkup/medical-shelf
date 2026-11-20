package com.example.medical_shelf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medical_shelf.data.local.dao.BoxDao
import com.example.medical_shelf.data.local.dao.ChecklistDao
import com.example.medical_shelf.data.local.dao.IntakeLogDao
import com.example.medical_shelf.data.local.dao.MedicineDao
import com.example.medical_shelf.data.local.dao.ScheduleDao
import com.example.medical_shelf.data.local.dao.UserBarcodeCacheDao
import com.example.medical_shelf.data.local.entity.BoxEntity
import com.example.medical_shelf.data.local.entity.BoxTemplate
import com.example.medical_shelf.data.local.entity.ChecklistItemEntity
import com.example.medical_shelf.data.local.entity.IntakeLogEntity
import com.example.medical_shelf.data.local.entity.MedicineEntity
import com.example.medical_shelf.data.local.entity.ScheduleEntity
import com.example.medical_shelf.data.local.entity.UserBarcodeCacheEntity
import java.util.concurrent.Executors

@Database(
    entities = [
        BoxEntity::class,
        MedicineEntity::class,
        ChecklistItemEntity::class,
        ScheduleEntity::class,
        IntakeLogEntity::class,
        UserBarcodeCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun boxDao(): BoxDao
    abstract fun medicineDao(): MedicineDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun intakeLogDao(): IntakeLogDao
    abstract fun userBarcodeCacheDao(): UserBarcodeCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_shelf.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    Executors.newSingleThreadExecutor().execute {
                        prepopulateDatabase(database)
                    }
                }
            }

            private fun prepopulateDatabase(db: AppDatabase) {
                // Prepopulate 4 default boxes
                val homeBox = BoxEntity(id = 1, template = BoxTemplate.HOME, isSystem = true, sortOrder = 0)
                val kidsBox = BoxEntity(id = 2, template = BoxTemplate.KIDS, isSystem = true, sortOrder = 1)
                val travelBox = BoxEntity(id = 3, template = BoxTemplate.TRAVEL, isSystem = true, sortOrder = 2)
                val carBox = BoxEntity(id = 4, template = BoxTemplate.CAR, isSystem = true, sortOrder = 3)

                val boxDao = db.boxDao()
                val checklistDao = db.checklistDao()

                // Insert system boxes
                kotlinx.coroutines.runBlocking {
                    boxDao.insertBoxes(listOf(homeBox, kidsBox, travelBox, carBox))

                    // Prepopulate Home Box Checklist
                    val homeChecklist = listOf(
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_antipyretic"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_antiseptic"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_sorbents"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_allergy"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_thermometer"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_burns"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_patch"),
                        ChecklistItemEntity(boxId = 1, titleResKey = "checklist_scissors")
                    )

                    // Prepopulate Kids Box Checklist
                    val kidsChecklist = listOf(
                        ChecklistItemEntity(boxId = 2, titleResKey = "checklist_antipyretic"),
                        ChecklistItemEntity(boxId = 2, titleResKey = "checklist_antiseptic"),
                        ChecklistItemEntity(boxId = 2, titleResKey = "checklist_sorbents"),
                        ChecklistItemEntity(boxId = 2, titleResKey = "checklist_allergy"),
                        ChecklistItemEntity(boxId = 2, titleResKey = "checklist_thermometer"),
                        ChecklistItemEntity(boxId = 2, titleResKey = "checklist_patch")
                    )

                    // Prepopulate Travel Box Checklist
                    val travelChecklist = listOf(
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_antipyretic"),
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_motion_sickness"),
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_sorbents"),
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_antiseptic"),
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_allergy"),
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_patch"),
                        ChecklistItemEntity(boxId = 3, titleResKey = "checklist_individual")
                    )

                    // Prepopulate Car Box Checklist
                    val carChecklist = listOf(
                        ChecklistItemEntity(boxId = 4, titleResKey = "checklist_antiseptic"),
                        ChecklistItemEntity(boxId = 4, titleResKey = "checklist_burns"),
                        ChecklistItemEntity(boxId = 4, titleResKey = "checklist_patch"),
                        ChecklistItemEntity(boxId = 4, titleResKey = "checklist_scissors"),
                        ChecklistItemEntity(boxId = 4, titleResKey = "checklist_antipyretic")
                    )

                    checklistDao.insertChecklistItems(homeChecklist + kidsChecklist + travelChecklist + carChecklist)
                }
            }
        }
    }
}