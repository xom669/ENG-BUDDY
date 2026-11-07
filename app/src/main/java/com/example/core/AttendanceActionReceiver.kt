package com.example.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AttendanceLogEntity
import com.example.data.local.EngBuddyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles background notification button taps ([Yes, Attending], [No, Bunking], [Class Cancelled])
 * directly inside the Android OS notification tray without opening the app.
 */
class AttendanceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val subjectId = intent.getLongExtra(AttendanceNotificationHelper.EXTRA_SUBJECT_ID, -1L)
        if (subjectId <= 0L) return

        val weight = intent.getIntExtra(AttendanceNotificationHelper.EXTRA_WEIGHT_POINTS, 1).coerceAtLeast(1)
        val notificationId = intent.getIntExtra(
            AttendanceNotificationHelper.EXTRA_NOTIFICATION_ID,
            AttendanceNotificationHelper.NOTIFICATION_ID_BASE
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = EngBuddyDatabase.getInstance(context).dao()
                val subject = dao.getSubjectByIdOnce(subjectId)
                if (subject != null) {
                    when (action) {
                        AttendanceNotificationHelper.ACTION_MARK_ATTENDING -> {
                            dao.updateSubject(
                                subject.copy(
                                    attendedClasses = subject.attendedClasses + weight,
                                    totalClasses = subject.totalClasses + weight
                                )
                            )
                            dao.insertAttendanceLog(
                                AttendanceLogEntity(
                                    subjectId = subject.id,
                                    sessionType = "NOTIFICATION_PROMPT",
                                    status = "ATTENDED",
                                    classesCounted = weight,
                                    note = "Marked [Yes, Attending] from Push Notification"
                                )
                            )
                        }

                        AttendanceNotificationHelper.ACTION_MARK_BUNKING -> {
                            dao.updateSubject(
                                subject.copy(
                                    totalClasses = subject.totalClasses + weight
                                )
                            )
                            dao.insertAttendanceLog(
                                AttendanceLogEntity(
                                    subjectId = subject.id,
                                    sessionType = "NOTIFICATION_PROMPT",
                                    status = "MISSED",
                                    classesCounted = weight,
                                    note = "Marked [No, Bunking] from Push Notification"
                                )
                            )
                        }

                        AttendanceNotificationHelper.ACTION_MARK_CANCELLED -> {
                            dao.updateSubject(
                                subject.copy(
                                    cancelledClasses = subject.cancelledClasses + 1
                                )
                            )
                            dao.insertAttendanceLog(
                                AttendanceLogEntity(
                                    subjectId = subject.id,
                                    sessionType = "NOTIFICATION_PROMPT",
                                    status = "CLASS_CANCELLED",
                                    classesCounted = 0,
                                    note = "Marked [Class Cancelled] — ignored in attendance denominator"
                                )
                            )
                        }
                    }
                }
                NotificationManagerCompat.from(context).cancel(notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
