package com.example.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.TimetableSlotEntity
import java.util.Calendar

/**
 * Schedules exact alarms for college timetable slots using AlarmManager.
 * Schedules alarms exactly 10 minutes into the lecture/lab so the prompt appears
 * while the student is in class, immune to WorkManager periodic jitter & Doze throttling.
 */
object TimetableAlarmScheduler {

    private const val ALARM_PROMPT_OFFSET_MINUTES = 10

    fun scheduleExactSlotAlarm(context: Context, slot: TimetableSlotEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val triggerTime = computeNextTriggerTime(
            targetDayOfWeek = slot.dayOfWeek,
            startTimeMinutes = slot.startTimeMinutes + ALARM_PROMPT_OFFSET_MINUTES
        )

        val intent = Intent(context, TimetableAlarmReceiver::class.java).apply {
            putExtra(TimetableAlarmReceiver.EXTRA_SUBJECT_ID, slot.subjectId)
            putExtra(TimetableAlarmReceiver.EXTRA_SLOT_ID, slot.id)
            putExtra(TimetableAlarmReceiver.EXTRA_WEIGHT_POINTS, slot.customWeightPoints)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun scheduleAllSlotsForWeek(context: Context, slots: List<TimetableSlotEntity>) {
        slots.forEach { slot ->
            scheduleExactSlotAlarm(context, slot)
        }
    }

    fun cancelSlotAlarm(context: Context, slotId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TimetableAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Converts a 1=Mon .. 7=Sun day of week and minute of day into next epoch millis.
     */
    fun computeNextTriggerTime(targetDayOfWeek: Int, startTimeMinutes: Int): Long {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            val hour = startTimeMinutes / 60
            val minute = startTimeMinutes % 60
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to Mon=1..Sun=7
            val currentDayOfWeek = when (val dow = get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> 7
                else -> dow - 1
            }

            var daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7
            if (daysToAdd == 0 && timeInMillis <= now.timeInMillis) {
                daysToAdd = 7
            }
            add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        return calendar.timeInMillis
    }
}
