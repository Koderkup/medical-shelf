package com.example.medical_shelf.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.medical_shelf.R
import com.example.medical_shelf.data.local.AppDatabase
import com.example.medical_shelf.data.local.entity.IntakeLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INTAKE_ALARM = "com.example.medical_shelf.ACTION_INTAKE_ALARM"
        const val ACTION_TAKE_MEDICINE = "com.example.medical_shelf.ACTION_TAKE_MEDICINE"
        const val ACTION_SNOOZE_ALARM = "com.example.medical_shelf.ACTION_SNOOZE_ALARM"

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_IS_LOUD = "extra_is_loud"
        const val EXTRA_BEFORE_FOOD_MINS = "extra_before_food_mins"

        const val CHANNEL_INTAKE_ALARM = "channel_intake_alarm"
        const val CHANNEL_FOOD_TIMER = "channel_food_timer"
        const val CHANNEL_EXPIRY_NOTIF = "channel_expiry_notif"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val db = AppDatabase.getDatabase(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Reschedule all active alarms on boot
                CoroutineScope(Dispatchers.IO).launch {
                    val schedules = db.scheduleDao().getEnabledSchedulesSync()
                    val scheduler = AlarmScheduler(context)
                    schedules.forEach { schedule ->
                        scheduler.scheduleAlarm(schedule)
                    }
                }
            }

            ACTION_INTAKE_ALARM -> {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1L)
                val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Лекарство"
                val dosage = intent.getStringExtra(EXTRA_DOSAGE)

                showIntakeNotification(context, scheduleId, medicineId, medicineName, dosage)
            }

            ACTION_TAKE_MEDICINE -> {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1L)
                val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: ""
                val beforeFoodMins = intent.getIntExtra(EXTRA_BEFORE_FOOD_MINS, 0)

                cancelNotification(context, scheduleId.toInt())

                CoroutineScope(Dispatchers.IO).launch {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val log = IntakeLogEntity(
                        scheduleId = scheduleId,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dateString = todayStr,
                        beforeFoodTimerMinutes = if (beforeFoodMins > 0) beforeFoodMins else null
                    )
                    db.intakeLogDao().insertLog(log)

                    // Decrement medicine quantity
                    if (medicineId > 0) {
                        val medicine = db.medicineDao().getMedicineByIdSync(medicineId)
                        if (medicine != null && medicine.quantity > 0) {
                            db.medicineDao().updateMedicine(medicine.copy(quantity = medicine.quantity - 1))
                        }
                    }

                    // Schedule before-food timer if requested
                    if (beforeFoodMins > 0) {
                        val workRequest = OneTimeWorkRequestBuilder<FoodTimerWorker>()
                            .setInitialDelay(beforeFoodMins.toLong(), TimeUnit.MINUTES)
                            .setInputData(workDataOf("medicine_name" to medicineName))
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                    }

                    // Reschedule next alarm iteration
                    if (scheduleId > 0) {
                        val schedule = db.scheduleDao().getScheduleById(scheduleId)
                        if (schedule != null && schedule.isEnabled) {
                            AlarmScheduler(context).scheduleAlarm(schedule)
                        }
                    }
                }
            }

            ACTION_SNOOZE_ALARM -> {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: ""
                val dosage = intent.getStringExtra(EXTRA_DOSAGE)

                cancelNotification(context, scheduleId.toInt())

                if (scheduleId > 0) {
                    AlarmScheduler(context).snoozeAlarm(scheduleId, medicineName, dosage, 10)
                }
            }
        }
    }

    private fun showIntakeNotification(
        context: Context,
        scheduleId: Long,
        medicineId: Long,
        medicineName: String,
        dosage: String?
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannels(context, notificationManager)

        // Fullscreen Intent
        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt() + 20000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Took it
        val takeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TAKE_MEDICINE
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + 30000,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + 40000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (!dosage.isNullOrBlank()) {
            context.getString(R.string.notification_alarm_body, dosage)
        } else {
            medicineName
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_INTAKE_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_alarm_title, medicineName))
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVibrate(longArrayOf(0, 500, 500, 500, 500, 1000))
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.btn_took_it),
                takePendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.btn_snooze_10),
                snoozePendingIntent
            )

        notificationManager.notify(scheduleId.toInt(), builder.build())
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    private fun createNotificationChannels(
        context: Context,
        notificationManager: NotificationManager
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val intakeChannel = NotificationChannel(
                CHANNEL_INTAKE_ALARM,
                context.getString(R.string.nav_intake),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medicine alarm notifications"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500, 500, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val foodChannel = NotificationChannel(
                CHANNEL_FOOD_TIMER,
                context.getString(R.string.timer_before_food, ""),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timer after medicine intake notifications"
                enableVibration(true)
            }

            val expiryChannel = NotificationChannel(
                CHANNEL_EXPIRY_NOTIF,
                context.getString(R.string.status_expiring_soon),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Medicine expiration warnings"
            }

            notificationManager.createNotificationChannel(intakeChannel)
            notificationManager.createNotificationChannel(foodChannel)
            notificationManager.createNotificationChannel(expiryChannel)
        }
    }
}