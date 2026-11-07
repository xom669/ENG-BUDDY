package com.example.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.EngBuddyDatabase
import com.example.domain.AttendanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Exact AlarmReceiver triggered by AlarmManager 10 minutes into scheduled timetable classes.
 * Overcomes Doze mode jitter by executing with exact timing and waking up the CPU.
 */
class TimetableAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
        val weightPoints = intent.getIntExtra(EXTRA_WEIGHT_POINTS, 1)
        val slotId = intent.getLongExtra(EXTRA_SLOT_ID, -1L)

        if (subjectId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = EngBuddyDatabase.getInstance(context.applicationContext)
                val subject = db.dao().getSubjectByIdOnce(subjectId)
                if (subject != null) {
                    val analysis = AttendanceCalculator.analyze(subject)
                    AttendanceNotificationHelper.showInteractiveAttendanceNotification(
                        context = context.applicationContext,
                        analysis = analysis,
                        weightPoints = weightPoints
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SUBJECT_ID = "com.example.extra.ALARM_SUBJECT_ID"
        const val EXTRA_SLOT_ID = "com.example.extra.ALARM_SLOT_ID"
        const val EXTRA_WEIGHT_POINTS = "com.example.extra.ALARM_WEIGHT_POINTS"
    }
}
