package com.example.medical_shelf.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medical_shelf.data.local.entity.ScheduleEntity
import com.example.medical_shelf.data.local.entity.ScheduleType
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(schedule: ScheduleEntity) {
        if (!schedule.isEnabled) return

        val triggerTime = calculateNextTriggerTime(schedule) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_INTAKE_ALARM
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, schedule.medicineId)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, schedule.medicineName)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, schedule.dosage)
            putExtra(AlarmReceiver.EXTRA_IS_LOUD, schedule.isLoudAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show Alarm Clock Intent for lockscreen/system alarm icon
        val showIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, schedule.medicineName)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, schedule.dosage)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            schedule.id.toInt() + 10000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAlarm(scheduleId: Long) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_INTAKE_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun snoozeAlarm(scheduleId: Long, medicineName: String, dosage: String?, minutes: Int = 10) {
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_INTAKE_ALARM
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + 50000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun calculateNextTriggerTime(schedule: ScheduleEntity): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.timeHour)
            set(Calendar.MINUTE, schedule.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (schedule.scheduleType) {
            ScheduleType.DAILY -> {
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            ScheduleType.EVERY_N_DAYS -> {
                val interval = schedule.intervalDays ?: 1
                while (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, interval)
                }
            }
            ScheduleType.DAYS_OF_WEEK -> {
                val daysList = schedule.daysOfWeek
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?: listOf(1, 2, 3, 4, 5, 6, 7)

                while (target.before(now) || !isCalendarDayInList(target, daysList)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return target.timeInMillis
    }

    private fun isCalendarDayInList(calendar: Calendar, daysList: List<Int>): Boolean {
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return daysList.contains(dayOfWeek)
    }
}