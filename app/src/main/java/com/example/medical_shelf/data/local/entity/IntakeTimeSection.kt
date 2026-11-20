package com.example.medical_shelf.data.local.entity

import androidx.annotation.StringRes
import com.example.medical_shelf.R

enum class IntakeTimeSection(@param:StringRes val titleResId: Int) {
    MORNING(R.string.section_morning),
    AFTERNOON(R.string.section_afternoon),
    EVENING(R.string.section_evening),
    NIGHT(R.string.section_night);

    companion object {
        fun fromHour(hour: Int): IntakeTimeSection {
            return when (hour) {
                in 5..11 -> MORNING
                in 12..16 -> AFTERNOON
                in 17..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}