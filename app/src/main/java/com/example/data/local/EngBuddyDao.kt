package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EngBuddyDao {

    // --- User Profile & Student Pass ---
    @Query("SELECT * FROM user_profile WHERE id = 'local_student_1' LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'local_student_1' LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // --- Subjects & Attendance ---
    @Query("SELECT * FROM subjects ORDER BY code ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY code ASC")
    suspend fun getSubjectsOnce(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    suspend fun getSubjectByIdOnce(subjectId: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("SELECT * FROM attendance_logs ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentAttendanceLogs(): Flow<List<AttendanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLog(log: AttendanceLogEntity)

    // --- Timetable Slots ---
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTimeMinutes ASC")
    fun observeTimetableSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTimeMinutes ASC")
    suspend fun getTimetableSlotsOnce(): List<TimetableSlotEntity>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek AND :minuteOfDay BETWEEN startTimeMinutes AND endTimeMinutes LIMIT 1")
    suspend fun getActiveSlotForTime(dayOfWeek: Int, minuteOfDay: Int): TimetableSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity)

    @Query("DELETE FROM timetable_slots WHERE id = :slotId")
    suspend fun deleteTimetableSlot(slotId: Long)

    // --- Smart Vault & 30-Day Trash Bin ---
    @Query("SELECT * FROM vault_documents WHERE status != 'TRASHED' ORDER BY createdAt DESC")
    fun observeActiveVaultDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE status = 'TRASHED' ORDER BY trashedAt DESC")
    fun observeTrashedVaultDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE status != 'TRASHED' ORDER BY createdAt DESC")
    suspend fun getActiveVaultDocumentsOnce(): List<VaultDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultDocument(doc: VaultDocumentEntity): Long

    @Update
    suspend fun updateVaultDocument(doc: VaultDocumentEntity)

    @Query("UPDATE vault_documents SET status = 'TRASHED', trashedAt = :now WHERE id = :docId")
    suspend fun softDeleteDocumentToTrash(docId: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE vault_documents SET status = 'READY', trashedAt = NULL WHERE id = :docId")
    suspend fun restoreDocumentFromTrash(docId: Long)

    @Query("DELETE FROM vault_documents WHERE status = 'TRASHED' AND trashedAt IS NOT NULL AND trashedAt < :cutoffMillis")
    suspend fun purgeExpiredTrash(cutoffMillis: Long)

    // --- Document Pages & RAG Source Pages ---
    @Query("SELECT * FROM document_pages WHERE documentId = :docId AND trashedAt IS NULL ORDER BY pageNumber ASC")
    fun observePagesForDocument(docId: Long): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE trashedAt IS NULL ORDER BY documentId ASC, pageNumber ASC")
    fun observeAllActivePages(): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE trashedAt IS NULL ORDER BY documentId ASC, pageNumber ASC")
    suspend fun getAllActivePagesOnce(): List<DocumentPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentPage(page: DocumentPageEntity): Long

    @Update
    suspend fun updateDocumentPage(page: DocumentPageEntity)

    // --- Offline Sync Queue ---
    @Query("SELECT * FROM offline_sync_queue ORDER BY createdAt DESC")
    fun observeOfflineQueue(): Flow<List<OfflineSyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOfflineTask(task: OfflineSyncQueueEntity): Long

    @Update
    suspend fun updateOfflineTask(task: OfflineSyncQueueEntity)

    @Query("UPDATE offline_sync_queue SET status = 'COMPLETED', lastError = NULL WHERE status != 'COMPLETED'")
    suspend fun markAllQueueTasksSynced()

    // --- Academic Calendar & Reminders ---
    @Query("SELECT * FROM calendar_events ORDER BY eventDateMillis ASC")
    fun observeCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteCalendarEvent(eventId: Long)

    // --- Gmail Sender Rules ---
    @Query("SELECT * FROM gmail_sender_rules ORDER BY id DESC")
    fun observeGmailSenderRules(): Flow<List<GmailSenderRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGmailSenderRule(rule: GmailSenderRuleEntity)

    @Query("UPDATE gmail_sender_rules SET ruleType = :newRuleType WHERE id = :ruleId")
    suspend fun updateGmailSenderRuleType(ruleId: Long, newRuleType: String)

    // --- Subject Overlap Preferences ---
    @Query("SELECT * FROM subject_overlap_prefs")
    fun observeOverlapPrefs(): Flow<List<SubjectOverlapPrefEntity>>

    @Query("SELECT * FROM subject_overlap_prefs WHERE LOWER(topicKeyword) = LOWER(:topic) LIMIT 1")
    suspend fun getOverlapPrefForTopic(topic: String): SubjectOverlapPrefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverlapPref(pref: SubjectOverlapPrefEntity)

    // --- Account Privacy: Full Delete ---
    @Query("DELETE FROM attendance_logs")
    suspend fun clearAttendanceLogs()

    @Query("DELETE FROM document_pages")
    suspend fun clearDocumentPages()

    @Query("DELETE FROM vault_documents")
    suspend fun clearVaultDocuments()

    @Query("DELETE FROM offline_sync_queue")
    suspend fun clearOfflineQueue()

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()
}
