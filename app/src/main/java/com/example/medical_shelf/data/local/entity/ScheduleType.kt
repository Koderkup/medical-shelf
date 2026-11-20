package com.example.medical_shelf.data.local.entity

import androidx.annotation.StringRes
import com.example.medical_shelf.R

enum class ScheduleType(@param:StringRes val titleResId: Int) {
    DAILY(R.string.schedule_daily),
    EVERY_N_DAYS(R.string.schedule_every_n_days),
    DAYS_OF_WEEK(R.string.schedule_days_of_week)
}