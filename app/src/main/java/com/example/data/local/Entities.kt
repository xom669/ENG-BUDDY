package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "local_student_1",
    val email: String = "aarav.sharma@iitb.ac.in",
    val displayName: String = "Aarav Sharma",
    val rollNumber: String = "23CS10042",
    val collegeName: String = "Indian Institute of Technology",
    val campusLocation: String = "Powai Campus, Mumbai",
    val branchName: String = "B.Tech Computer Science & Engineering",
    val semester: String = "3rd Year • Sem 5",
    val sectionAndGroup: String = "Section B • Group 2",
    val workingDaysMode: String = "5_DAYS", // 5_DAYS (Mon-Fri), 6_DAYS (Mon-Sat), CUSTOM
    val defaultTargetPercentage: Double = 75.0,
    val idCardFrontUri: String = "",
    val idCardBackUri: String = "",
    val idCardSlotCollapsed: Boolean = false,
    val isSignedIn: Boolean = true,
    val classroomConnected: Boolean = false,
    val classroomInstitutionBlocked: Boolean = false,
    val forwardingInboxAddress: String = "vault+23cs10042@engbuddy.io",
    val gmailConnected: Boolean = false,
    val whatsappWebhookEnabled: Boolean = false,
    val wifiOnlySync: Boolean = true,
    val themeMode: String = "DARK", // SYSTEM, DARK, LIGHT
    val encryptedByoGeminiKey: String = "",
    val tier: String = "Free (Student)",
    val dailyAiCallsUsed: Int = 4,
    val dailyAiQuotaLimit: Int = 50,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["code"], unique = true)]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val professorName: String,
    val targetPercentage: Double = 75.0,
    val labWeightClasses: Int = 2, // e.g., 3-hour Lab counts as 2 attendance points
    val medicalLeaveExcused: Boolean = true,
    val isOptionalElective: Boolean = false, // Optional Elective — exclude from aggregate
    val tutorialBonusPoints: Int = 0, // Professor gives +1 bonus attendance for tutorials
    val roundingMode: String = "NEAREST", // FLOOR, NEAREST, CEIL
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0,
    val medicalExcusedClasses: Int = 0,
    val cancelledClasses: Int = 0,
    val colorHex: Long = 0xFF4285F4
)

@Entity(
    tableName = "timetable_slots",
    indices = [Index(value = ["dayOfWeek", "startTimeMinutes"]), Index(value = ["subjectId"])],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val courseCode: String = "ME-204",
    val subjectName: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startTimeMinutes: Int, // e.g., 10*60 = 600 (10:00 AM)
    val endTimeMinutes: Int,
    val room: String,
    val isLab: Boolean = false,
    val customWeightPoints: Int = 1
)

@Entity(
    tableName = "vault_documents",
    indices = [Index(value = ["subjectId"]), Index(value = ["status"]), Index(value = ["trashedAt"])],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class VaultDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?,
    val subjectName: String,
    val chapterTitle: String,
    val title: String,
    val sourceType: String, // CAMERA, WHATSAPP_SHARE, CLASSROOM, GMAIL, MANUAL
    val pageCount: Int,
    val orderingConfidence: Double, // 0.0 .. 1.0
    val status: String, // READY, NEEDS_ORDER_REVIEW, QUEUED_OFFLINE, TRASHED
    val summaryText: String,
    val pdfPath: String = "",
    val trashedAt: Long? = null, // 30-day soft-delete timestamp
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_pages",
    indices = [Index(value = ["documentId"]), Index(value = ["phash"])],
    foreignKeys = [
        ForeignKey(
            entity = VaultDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageNumber: Int,
    val detectedHeading: String,
    val ocrText: String,
    val laplacianSharpness: Double, // > 120 sharp, < 70 blurry
    val phash: String,
    val dhash: String,
    val ssimScore: Double,
    val duplicateFlag: String, // NONE, CONFIRMED_DUPLICATE, POSSIBLE_DUPLICATE
    val confidence: Double,
    val trashedAt: Long? = null
)

@Entity(
    tableName = "attendance_logs",
    indices = [Index(value = ["subjectId", "timestamp"])],
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val sessionType: String, // LECTURE, LAB, NOTIFICATION_PROMPT
    val status: String, // ATTENDED, MISSED, MEDICAL_EXCUSED, CLASS_CANCELLED
    val classesCounted: Int,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "offline_sync_queue",
    indices = [Index(value = ["status", "nextRetryAt"])]
)
data class OfflineSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String, // UPLOAD_SCAN_BATCH, SYNC_ATTENDANCE, PARSE_CALENDAR
    val title: String,
    val payloadSummary: String,
    val retryCount: Int = 0,
    val nextRetryAt: Long = System.currentTimeMillis(),
    val requiresWifi: Boolean = true,
    val status: String = "PENDING", // PENDING, SYNCING, COMPLETED, FAILED_BACKOFF
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "calendar_events",
    indices = [Index(value = ["eventDateMillis"]), Index(value = ["isConfirmed"])]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectName: String,
    val eventType: String, // EXAM, LAB_SUBMISSION, ASSIGNMENT, HOLIDAY
    val eventDateMillis: Long,
    val dateLabel: String,
    val isConfirmed: Boolean = true,
    val reminderMinutesBefore: Int = 1440 // 24 hours default
)

@Entity(tableName = "gmail_sender_rules")
data class GmailSenderRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderAddressOrDomain: String,
    val sampleSubject: String,
    val ruleType: String, // ALLOW, NEVER_IMPORT, PENDING_APPROVAL
    val keywordMatched: String = "Assignment / Lab / Exam"
)

@Entity(
    tableName = "subject_overlap_prefs",
    indices = [Index(value = ["topicKeyword"], unique = true)]
)
data class SubjectOverlapPrefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicKeyword: String,
    val chosenSubjectId: Long,
    val chosenSubjectName: String
)
