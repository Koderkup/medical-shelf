package com.example.medical_shelf.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medical_shelf.R
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class ExpiryCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(context)
        val warningDays = prefs.expiryWarningDays.first()

        val db = AppDatabase.getDatabase(context)
        val thresholdTime = System.currentTimeMillis() + (warningDays.toLong() * 24 * 60 * 60 * 1000)

        val expiringMeds = db.medicineDao().getExpiringMedicinesSync(thresholdTime)

        if (expiringMeds.isNotEmpty()) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val title = context.getString(R.string.status_expiring_soon)
            val body = context.getString(
                R.string.box_total_count,
                expiringMeds.size
            )

            val builder = NotificationCompat.Builder(context, AlarmReceiver.CHANNEL_EXPIRY_NOTIF)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(9999, builder.build())
        }

        return Result.success()
    }
}