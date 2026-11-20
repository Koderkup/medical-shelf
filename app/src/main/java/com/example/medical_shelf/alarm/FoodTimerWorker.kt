package com.example.medical_shelf.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medical_shelf.R

class FoodTimerWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medicineName = inputData.getString("medicine_name") ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = context.getString(R.string.notification_food_ready_title)
        val body = context.getString(R.string.notification_food_ready_body)

        val builder = NotificationCompat.Builder(context, AlarmReceiver.CHANNEL_FOOD_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(if (medicineName.isNotBlank()) "$medicineName: $body" else body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())

        return Result.success()
    }
}