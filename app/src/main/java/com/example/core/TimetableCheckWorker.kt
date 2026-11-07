package com.example.core

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.EngBuddyDatabase
import com.example.domain.AttendanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically checks if a class is active
 * according to the student's timetable and fires an interactive notification
 * with [Yes, Attending], [No, Bunking], and [Class Cancelled] buttons.
 */
class TimetableCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = EngBuddyDatabase.getInstance(applicationContext)
            val dao = database.dao()

            // Keep exact AlarmManager schedules fresh for all timetable slots (immune to Doze mode)
            val allSlots = dao.getTimetableSlotsOnce()
            TimetableAlarmScheduler.scheduleAllSlotsForWeek(applicationContext, allSlots)

            val calendar = Calendar.getInstance()
            // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2 .. Saturday=7
            // Convert to 1 = Monday .. 7 = Sunday to match TimetableSlotEntity
            val dayOfWeek = when (val dow = calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> 7
                else -> dow - 1
            }
            val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            // Look up any active timetable slot for today and current time
            val activeSlot = dao.getActiveSlotForTime(dayOfWeek, currentMinute)
            if (activeSlot != null) {
                // Ensure notification is triggered ~10 minutes into the slot
                val minutesIntoSlot = currentMinute - activeSlot.startTimeMinutes
                if (minutesIntoSlot >= 5) {
                    val prefs = applicationContext.getSharedPreferences("timetable_worker_prefs", Context.MODE_PRIVATE)
                    val todayKey = "notified_${calendar.get(Calendar.YEAR)}_${calendar.get(Calendar.DAY_OF_YEAR)}_${activeSlot.id}"
                    val alreadyNotified = prefs.getBoolean(todayKey, false)

                    if (!alreadyNotified) {
                        val subject = dao.getSubjectByIdOnce(activeSlot.subjectId)
                        if (subject != null) {
                            val analysis = AttendanceCalculator.analyze(subject)
                            AttendanceNotificationHelper.showInteractiveAttendanceNotification(
                                context = applicationContext,
                                analysis = analysis,
                                weightPoints = activeSlot.customWeightPoints
                            )
                            prefs.edit().putBoolean(todayKey, true).apply()
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "engbuddy_timetable_attendance_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<TimetableCheckWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}

