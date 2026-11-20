package com.example.medical_shelf.data.local

import androidx.room.TypeConverter
import com.example.medical_shelf.data.local.entity.BoxTemplate
import com.example.medical_shelf.data.local.entity.IntakeTimeSection
import com.example.medical_shelf.data.local.entity.ScheduleType

class Converters {
    @TypeConverter
    fun fromBoxTemplate(value: BoxTemplate): String = value.name

    @TypeConverter
    fun toBoxTemplate(value: String): BoxTemplate = try {
        BoxTemplate.valueOf(value)
    } catch (e: Exception) {
        BoxTemplate.CUSTOM
    }

    @TypeConverter
    fun fromIntakeTimeSection(value: IntakeTimeSection): String = value.name

    @TypeConverter
    fun toIntakeTimeSection(value: String): IntakeTimeSection = try {
        IntakeTimeSection.valueOf(value)
    } catch (e: Exception) {
        IntakeTimeSection.MORNING
    }

    @TypeConverter
    fun fromScheduleType(value: ScheduleType): String = value.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = try {
        ScheduleType.valueOf(value)
    } catch (e: Exception) {
        ScheduleType.DAILY
    }
}