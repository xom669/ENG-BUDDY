package com.example.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.SubjectAttendanceAnalysis

object AttendanceNotificationHelper {

    const val CHANNEL_ID = "engbuddy_live_class_attendance"
    const val CHANNEL_NAME = "Live Class Attendance Prompts"
    const val NOTIFICATION_ID_BASE = 4200

    const val ACTION_MARK_ATTENDING = "com.example.action.MARK_ATTENDING"
    const val ACTION_MARK_BUNKING = "com.example.action.MARK_BUNKING"
    const val ACTION_MARK_CANCELLED = "com.example.action.MARK_CANCELLED"
    const val EXTRA_SUBJECT_ID = "extra_subject_id"
    const val EXTRA_WEIGHT_POINTS = "extra_weight_points"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Time-triggered interactive attendance prompts 10 minutes into scheduled classes."
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches the interactive OS notification with three direct action buttons:
     * [ ✅ Yes, Attending ]  [ ❌ No, Bunking ]  [ 🚫 Class Cancelled ]
     * Tapping any button updates the Room SQLite database in the background via AttendanceActionReceiver
     * without opening the app.
     */
    fun showInteractiveAttendanceNotification(
        context: Context,
        analysis: SubjectAttendanceAnalysis,
        weightPoints: Int = 1
    ): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val subject = analysis.subject
        val notifId = NOTIFICATION_ID_BASE + subject.id.toInt()

        val title = "${subject.name} (${subject.code}) is going on"
        val body = "Should I mark your attendance for this class? (Current: ${analysis.roundedPercentage.toInt()}% • Safe to bunk: ${analysis.safeToSkipClasses})"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val attendingPendingIntent = buildActionPendingIntent(
            context = context,
            action = ACTION_MARK_ATTENDING,
            subjectId = subject.id,
            weightPoints = weightPoints,
            notificationId = notifId,
            requestCodeOffset = 1
        )
        val bunkingPendingIntent = buildActionPendingIntent(
            context = context,
            action = ACTION_MARK_BUNKING,
            subjectId = subject.id,
            weightPoints = weightPoints,
            notificationId = notifId,
            requestCodeOffset = 2
        )
        val cancelledPendingIntent = buildActionPendingIntent(
            context = context,
            action = ACTION_MARK_CANCELLED,
            subjectId = subject.id,
            weightPoints = weightPoints,
            notificationId = notifId,
            requestCodeOffset = 3
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✅ Yes, Attending", attendingPendingIntent)
            .addAction(0, "❌ No, Bunking", bunkingPendingIntent)
            .addAction(0, "🚫 Class Cancelled", cancelledPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun buildActionPendingIntent(
        context: Context,
        action: String,
        subjectId: Long,
        weightPoints: Int,
        notificationId: Int,
        requestCodeOffset: Int
    ): PendingIntent {
        val intent = Intent(context, AttendanceActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_SUBJECT_ID, subjectId)
            putExtra(EXTRA_WEIGHT_POINTS, weightPoints)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId * 10 + requestCodeOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
