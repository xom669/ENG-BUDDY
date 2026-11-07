package com.example.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.local.AttendanceLogEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.local.EngBuddyDao
import com.example.data.local.GmailSenderRuleEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.SubjectOverlapPrefEntity
import com.example.data.local.TimetableSlotEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VaultDocumentEntity
import com.example.data.remote.AiExplainerResponse
import com.example.data.remote.AiRepository
import com.example.data.remote.BatchClassificationResult
import com.example.data.remote.ExplainMode
import com.example.data.remote.ScannedPageDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.math.min
import kotlin.math.pow

class EngBuddyRepository(
    private val context: Context,
    private val dao: EngBuddyDao,
    val aiRepository: AiRepository
) {
    val userProfile: Flow<UserProfileEntity?> = dao.observeUserProfile()
    val subjects: Flow<List<SubjectEntity>> = dao.observeSubjects()
    val timetableSlots: Flow<List<TimetableSlotEntity>> = dao.observeTimetableSlots()
    val activeDocuments: Flow<List<VaultDocumentEntity>> = dao.observeActiveVaultDocuments()
    val trashedDocuments: Flow<List<VaultDocumentEntity>> = dao.observeTrashedVaultDocuments()
    val allActivePages: Flow<List<DocumentPageEntity>> = dao.observeAllActivePages()
    val recentAttendanceLogs: Flow<List<AttendanceLogEntity>> = dao.observeRecentAttendanceLogs()
    val offlineQueue: Flow<List<OfflineSyncQueueEntity>> = dao.observeOfflineQueue()
    val calendarEvents: Flow<List<CalendarEventEntity>> = dao.observeCalendarEvents()
    val gmailSenderRules: Flow<List<GmailSenderRuleEntity>> = dao.observeGmailSenderRules()
    val overlapPrefs: Flow<List<SubjectOverlapPrefEntity>> = dao.observeOverlapPrefs()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.getUserProfileOnce() == null) {
            dao.upsertUserProfile(UserProfileEntity())
        }
        val existingSubjects = dao.getSubjectsOnce()
        if (existingSubjects.isEmpty()) {
            val s1 = dao.insertSubject(
                SubjectEntity(
                    code = "ME-204",
                    name = "Thermodynamics & Heat Transfer",
                    professorName = "Prof. R. K. Verma",
                    targetPercentage = 75.0,
                    labWeightClasses = 2,
                    medicalLeaveExcused = true,
                    isOptionalElective = false,
                    tutorialBonusPoints = 0,
                    roundingMode = "NEAREST",
                    attendedClasses = 25,
                    totalClasses = 32,
                    medicalExcusedClasses = 0,
                    colorHex = 0xFF4285F4
                )
            )
            val s2 = dao.insertSubject(
                SubjectEntity(
                    code = "EC-304",
                    name = "Signals & Systems",
                    professorName = "Dr. Ananya Nair",
                    targetPercentage = 75.0,
                    labWeightClasses = 2,
                    medicalLeaveExcused = true,
                    isOptionalElective = false,
                    tutorialBonusPoints = 0,
                    roundingMode = "FLOOR",
                    attendedClasses = 18,
                    totalClasses = 27,
                    medicalExcusedClasses = 0,
                    colorHex = 0xFFEA4335
                )
            )
            val s3 = dao.insertSubject(
                SubjectEntity(
                    code = "MA-302",
                    name = "Engineering Mathematics III",
                    professorName = "Prof. S. Kulkarni",
                    targetPercentage = 75.0,
                    labWeightClasses = 1,
                    medicalLeaveExcused = false,
                    isOptionalElective = false,
                    tutorialBonusPoints = 1,
                    roundingMode = "NEAREST",
                    attendedClasses = 24,
                    totalClasses = 29,
                    medicalExcusedClasses = 0,
                    colorHex = 0xFFFBBC04
                )
            )
            val s4 = dao.insertSubject(
                SubjectEntity(
                    code = "CS-305",
                    name = "Operating Systems & Kernels",
                    professorName = "Prof. Vikram Desai",
                    targetPercentage = 75.0,
                    labWeightClasses = 2,
                    medicalLeaveExcused = true,
                    isOptionalElective = false,
                    tutorialBonusPoints = 0,
                    roundingMode = "CEIL",
                    attendedClasses = 29,
                    totalClasses = 34,
                    medicalExcusedClasses = 1,
                    colorHex = 0xFF34A853
                )
            )

            // Seed Weekly Timetable Slots
            dao.insertTimetableSlot(
                TimetableSlotEntity(
                    subjectId = s1,
                    courseCode = "ME-204",
                    subjectName = "Thermodynamics & Heat Transfer",
                    dayOfWeek = 2, // Tuesday
                    startTimeMinutes = 10 * 60,
                    endTimeMinutes = 11 * 60,
                    room = "LH-204",
                    isLab = false,
                    customWeightPoints = 1
                )
            )
            dao.insertTimetableSlot(
                TimetableSlotEntity(
                    subjectId = s2,
                    courseCode = "EC-304",
                    subjectName = "Signals & Systems",
                    dayOfWeek = 3, // Wednesday
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 11 * 60,
                    room = "DSP Lab-2",
                    isLab = true,
                    customWeightPoints = 2
                )
            )
            dao.insertTimetableSlot(
                TimetableSlotEntity(
                    subjectId = s3,
                    courseCode = "MA-302",
                    subjectName = "Engineering Mathematics III",
                    dayOfWeek = 4, // Thursday
                    startTimeMinutes = 11 * 60,
                    endTimeMinutes = 12 * 60,
                    room = "LH-101",
                    isLab = false,
                    customWeightPoints = 1
                )
            )
            dao.insertTimetableSlot(
                TimetableSlotEntity(
                    subjectId = s4,
                    courseCode = "CS-305",
                    subjectName = "Operating Systems & Kernels",
                    dayOfWeek = 5, // Friday
                    startTimeMinutes = 14 * 60,
                    endTimeMinutes = 17 * 60,
                    room = "Systems Lab-1",
                    isLab = true,
                    customWeightPoints = 2
                )
            )

            // Seed Smart Vault Documents + Pages
            val docThermo = dao.insertVaultDocument(
                VaultDocumentEntity(
                    subjectId = s1,
                    subjectName = "Thermodynamics & Heat Transfer",
                    chapterTitle = "Ch 4: Entropy & Carnot Cycle",
                    title = "Second Law, Clausius Inequality & T-S Diagrams",
                    sourceType = "CAMERA",
                    pageCount = 2,
                    orderingConfidence = 0.96,
                    status = "READY",
                    summaryText = "Derivation of Carnot efficiency η = 1 - T_L/T_H, Clausius inequality ∮ δQ/T ≤ 0, and irreversible entropy generation."
                )
            )
            dao.insertDocumentPage(
                DocumentPageEntity(
                    documentId = docThermo,
                    pageNumber = 1,
                    detectedHeading = "4.1 Carnot Cycle & Thermal Efficiency",
                    ocrText = "For any reversible heat engine operating between reservoirs T_H and T_L, thermal efficiency η_max = 1 - (T_L / T_H). Isothermal expansion 1->2 absorbs Q_H at T_H; adiabatic expansion 2->3 drops temperature to T_L.",
                    laplacianSharpness = 184.5,
                    phash = "A4F19C3B82D011E4",
                    dhash = "9C31A8F042E1B305",
                    ssimScore = 0.21,
                    duplicateFlag = "NONE",
                    confidence = 0.97
                )
            )
            dao.insertDocumentPage(
                DocumentPageEntity(
                    documentId = docThermo,
                    pageNumber = 2,
                    detectedHeading = "4.2 Clausius Inequality & Entropy Generation",
                    ocrText = "Cyclic integral δQ/T <= 0. Defining state property Entropy dS = (δQ/T)_rev. For an isolated system ΔS_total = ΔS_sys + ΔS_surr >= 0 (Entropy Principle).",
                    laplacianSharpness = 168.2,
                    phash = "C8124B7F19A360D2",
                    dhash = "7B20D4198E3A5012",
                    ssimScore = 0.34,
                    duplicateFlag = "NONE",
                    confidence = 0.95
                )
            )

            val docSignals = dao.insertVaultDocument(
                VaultDocumentEntity(
                    subjectId = s2,
                    subjectName = "Signals & Systems",
                    chapterTitle = "Ch 3: Continuous-Time Fourier Transform",
                    title = "CTFT Properties, Convolution & Parseval's Theorem",
                    sourceType = "WHATSAPP_SHARE",
                    pageCount = 2,
                    orderingConfidence = 0.92,
                    status = "READY",
                    summaryText = "Fourier Transform pair X(jω) = ∫ x(t)e^(-jωt)dt, duality, time-shifting, and convolution in time equals multiplication in frequency."
                )
            )
            dao.insertDocumentPage(
                DocumentPageEntity(
                    documentId = docSignals,
                    pageNumber = 1,
                    detectedHeading = "3.1 Fourier Transform Definition & Convergence",
                    ocrText = "Dirichlet conditions for existence of X(jω): signal x(t) is absolutely integrable ∫|x(t)|dt < ∞, has finite maxima/minima and finite discontinuities in any finite interval.",
                    laplacianSharpness = 192.0,
                    phash = "F1083E7A4D92B61C",
                    dhash = "E2194F8B5C03A72D",
                    ssimScore = 0.18,
                    duplicateFlag = "NONE",
                    confidence = 0.93
                )
            )
            dao.insertDocumentPage(
                DocumentPageEntity(
                    documentId = docSignals,
                    pageNumber = 2,
                    detectedHeading = "3.2 Convolution Property & LTI Frequency Response",
                    ocrText = "For an LTI system with impulse response h(t), y(t) = x(t) * h(t) transforms to Y(jω) = X(jω) · H(jω). Parseval's relation: ∫|x(t)|^2 dt = (1/2π) ∫|X(jω)|^2 dω.",
                    laplacianSharpness = 176.4,
                    phash = "3B94D2816F0AE572",
                    dhash = "2A83C1705E9FD461",
                    ssimScore = 0.29,
                    duplicateFlag = "NONE",
                    confidence = 0.91
                )
            )

            // Seed one soft-deleted duplicate in 30-day Trash Bin
            dao.insertVaultDocument(
                VaultDocumentEntity(
                    subjectId = s1,
                    subjectName = "Thermodynamics & Heat Transfer",
                    chapterTitle = "Ch 4: Entropy & Carnot Cycle",
                    title = "[Duplicate Page] Carnot Cycle Blurry Shot #1",
                    sourceType = "CAMERA",
                    pageCount = 1,
                    orderingConfidence = 0.85,
                    status = "TRASHED",
                    summaryText = "Auto-discarded duplicate (Laplacian sharpness 51.2 vs 184.5; pHash dist=2, SSIM=0.92). Soft-deleted to 30-day Trash.",
                    trashedAt = System.currentTimeMillis() - 3_600_000L
                )
            )

            // Seed Academic Calendar Events
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    title = "Mid-Semester Exam: Thermodynamics (ME-204)",
                    subjectName = "Thermodynamics & Heat Transfer",
                    eventType = "EXAM",
                    eventDateMillis = now + 3 * dayMs,
                    dateLabel = "In 3 Days • 09:30 AM (Main Block)",
                    isConfirmed = true,
                    reminderMinutesBefore = 1440
                )
            )
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    title = "DSP Lab Record Submission #4 (FFT Filter)",
                    subjectName = "Signals & Systems",
                    eventType = "LAB_SUBMISSION",
                    eventDateMillis = now + 5 * dayMs,
                    dateLabel = "In 5 Days • 04:00 PM (DSP Lab-2)",
                    isConfirmed = true,
                    reminderMinutesBefore = 720
                )
            )

            // Seed Gmail Sender Rules
            dao.upsertGmailSenderRule(
                GmailSenderRuleEntity(
                    senderAddressOrDomain = "@cse.iitb.ac.in",
                    sampleSubject = "[CS-305] Lab 4 Pintos Thread Scheduler Rubric PDF",
                    ruleType = "ALLOW",
                    keywordMatched = "Lab / Rubric / PDF"
                )
            )
            dao.upsertGmailSenderRule(
                GmailSenderRuleEntity(
                    senderAddressOrDomain = "ta.signals@ee.college.edu",
                    sampleSubject = "Practice Problem Set 3: Fourier & Z-Transform Solutions",
                    ruleType = "PENDING_APPROVAL",
                    keywordMatched = "Problem Set / Solutions"
                )
            )
            dao.upsertGmailSenderRule(
                GmailSenderRuleEntity(
                    senderAddressOrDomain = "coupons@fooddelivery-promos.com",
                    sampleSubject = "Flat 60% OFF Late Night Campus Cravings!",
                    ruleType = "NEVER_IMPORT",
                    keywordMatched = "Blocked Commercial Noise"
                )
            )

            // Seed one Offline Queue item
            dao.enqueueOfflineTask(
                OfflineSyncQueueEntity(
                    operationType = "UPLOAD_SCAN_BATCH",
                    title = "Thermodynamics Ch-4 Stitched PDF (2 pages)",
                    payloadSummary = "Processed on-device (Laplacian + pHash/SSIM). Waiting for Wi-Fi sync to pgvector.",
                    retryCount = 0,
                    requiresWifi = true,
                    status = "PENDING"
                )
            )
        }
    }

    suspend fun updateThemeMode(mode: String) {
        val current = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.upsertUserProfile(current.copy(themeMode = mode))
    }

    suspend fun updateWorkingDaysMode(mode: String) {
        val current = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.upsertUserProfile(current.copy(workingDaysMode = mode))
    }

    suspend fun updateStudentProfilePass(
        displayName: String,
        rollNumber: String,
        collegeName: String,
        campusLocation: String,
        branchName: String,
        semester: String,
        sectionAndGroup: String
    ) {
        val current = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.upsertUserProfile(
            current.copy(
                displayName = displayName,
                rollNumber = rollNumber,
                collegeName = collegeName,
                campusLocation = campusLocation,
                branchName = branchName,
                semester = semester,
                sectionAndGroup = sectionAndGroup
            )
        )
    }

    suspend fun updateIdCardSlot(
        frontUri: String? = null,
        backUri: String? = null,
        collapsed: Boolean? = null
    ) {
        val current = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.upsertUserProfile(
            current.copy(
                idCardFrontUri = frontUri ?: current.idCardFrontUri,
                idCardBackUri = backUri ?: current.idCardBackUri,
                idCardSlotCollapsed = collapsed ?: current.idCardSlotCollapsed
            )
        )
    }

    suspend fun toggleWifiOnlySync(enabled: Boolean) {
        val current = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.upsertUserProfile(current.copy(wifiOnlySync = enabled))
    }

    suspend fun recordAttendance(
        subject: SubjectEntity,
        sessionType: String,
        status: String,
        note: String = ""
    ) {
        val weight = if (sessionType == "LAB") subject.labWeightClasses else 1
        val updated = when (status) {
            "ATTENDED" -> subject.copy(
                attendedClasses = subject.attendedClasses + weight,
                totalClasses = subject.totalClasses + weight
            )
            "MISSED" -> subject.copy(
                totalClasses = subject.totalClasses + weight
            )
            "MEDICAL_EXCUSED" -> subject.copy(
                totalClasses = subject.totalClasses + weight,
                medicalExcusedClasses = subject.medicalExcusedClasses + weight
            )
            "CLASS_CANCELLED" -> subject.copy(
                cancelledClasses = subject.cancelledClasses + 1
            )
            else -> subject
        }
        dao.updateSubject(updated)
        dao.insertAttendanceLog(
            AttendanceLogEntity(
                subjectId = subject.id,
                sessionType = sessionType,
                status = status,
                classesCounted = if (status == "CLASS_CANCELLED") 0 else weight,
                note = note
            )
        )
    }

    suspend fun updateSubjectRules(
        subject: SubjectEntity,
        targetPct: Double,
        labWeight: Int,
        medicalExcused: Boolean,
        roundingMode: String,
        isOptionalElective: Boolean = subject.isOptionalElective,
        tutorialBonusPoints: Int = subject.tutorialBonusPoints
    ) {
        dao.updateSubject(
            subject.copy(
                targetPercentage = targetPct.coerceIn(60.0, 90.0),
                labWeightClasses = labWeight.coerceIn(1, 4),
                medicalLeaveExcused = medicalExcused,
                roundingMode = roundingMode,
                isOptionalElective = isOptionalElective,
                tutorialBonusPoints = tutorialBonusPoints.coerceIn(0, 10)
            )
        )
    }

    suspend fun ingestTimetableSlot(
        subject: SubjectEntity,
        dayOfWeek: Int,
        startHour: Int,
        endHour: Int,
        room: String,
        isLab: Boolean
    ) {
        dao.insertTimetableSlot(
            TimetableSlotEntity(
                subjectId = subject.id,
                courseCode = subject.code,
                subjectName = subject.name,
                dayOfWeek = dayOfWeek.coerceIn(1, 7),
                startTimeMinutes = startHour * 60,
                endTimeMinutes = endHour * 60,
                room = room,
                isLab = isLab,
                customWeightPoints = if (isLab) subject.labWeightClasses else 1
            )
        )
    }

    suspend fun classifyBatchWithTimetablePrior(
        pages: List<ScannedPageDraft>,
        overrideDayOfWeek: Int? = null,
        overrideMinuteOfDay: Int? = null
    ): BatchClassificationResult {
        val subjectsList = dao.getSubjectsOnce()
        val cal = Calendar.getInstance()
        val rawDow = cal.get(Calendar.DAY_OF_WEEK)
        val isoDow = overrideDayOfWeek ?: if (rawDow == Calendar.SUNDAY) 7 else rawDow - 1
        val minuteOfDay = overrideMinuteOfDay ?: (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))

        val activeSlot = dao.getActiveSlotForTime(isoDow, minuteOfDay)
        val overlapPref = dao.getOverlapPrefForTopic("Fourier / Laplace Transforms")

        return aiRepository.classifyAndOrderBatch(
            pages = pages,
            subjects = subjectsList,
            activeTimetableSlot = activeSlot,
            rememberedOverlapSubjectId = overlapPref?.chosenSubjectId
        )
    }

    suspend fun rememberSubjectOverlapChoice(topicKeyword: String, subject: SubjectEntity) {
        dao.upsertOverlapPref(
            SubjectOverlapPrefEntity(
                topicKeyword = topicKeyword,
                chosenSubjectId = subject.id,
                chosenSubjectName = subject.name
            )
        )
    }

    suspend fun stitchAndSaveScannedBatch(
        title: String,
        chapterTitle: String,
        subject: SubjectEntity,
        orderedActivePages: List<ScannedPageDraft>,
        trashedDuplicates: List<ScannedPageDraft>,
        orderingConfidence: Double,
        sourceType: String = "CAMERA"
    ): Long = withContext(Dispatchers.IO) {
        val pdfFile = generateStitchedPdfFile(title, subject.name, chapterTitle, orderedActivePages)

        val docId = dao.insertVaultDocument(
            VaultDocumentEntity(
                subjectId = subject.id,
                subjectName = subject.name,
                chapterTitle = chapterTitle,
                title = title,
                sourceType = sourceType,
                pageCount = orderedActivePages.size,
                orderingConfidence = orderingConfidence,
                status = "READY",
                summaryText = orderedActivePages.joinToString(" • ") {
                    "P${it.detectedPageNumber ?: it.tempId}: ${it.detectedHeading}"
                },
                pdfPath = pdfFile.absolutePath
            )
        )

        orderedActivePages.forEachIndexed { index, draft ->
            dao.insertDocumentPage(
                DocumentPageEntity(
                    documentId = docId,
                    pageNumber = index + 1,
                    detectedHeading = draft.detectedHeading,
                    ocrText = draft.ocrText,
                    laplacianSharpness = draft.laplacianSharpness,
                    phash = draft.phash,
                    dhash = draft.dhash,
                    ssimScore = draft.ssimWithPrevious,
                    duplicateFlag = if (draft.isPossibleDuplicate) "POSSIBLE_DUPLICATE" else "NONE",
                    confidence = orderingConfidence
                )
            )
        }

        trashedDuplicates.forEach { dup ->
            dao.insertVaultDocument(
                VaultDocumentEntity(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    chapterTitle = chapterTitle,
                    title = "[Auto-Trash Duplicate] ${dup.rawTitle}",
                    sourceType = sourceType,
                    pageCount = 1,
                    orderingConfidence = orderingConfidence,
                    status = "TRASHED",
                    summaryText = "${dup.duplicateDecisionLabel} (Sharpness: ${dup.laplacianSharpness})",
                    trashedAt = System.currentTimeMillis()
                )
            )
        }

        val profile = dao.getUserProfileOnce()
        dao.enqueueOfflineTask(
            OfflineSyncQueueEntity(
                operationType = "UPLOAD_SCAN_BATCH",
                title = "$title (${orderedActivePages.size}p PDF)",
                payloadSummary = "Stitched to ${pdfFile.name} • Queueing pgvector embedding sync",
                requiresWifi = profile?.wifiOnlySync ?: true,
                status = "PENDING"
            )
        )

        docId
    }

    private fun generateStitchedPdfFile(
        title: String,
        subjectName: String,
        chapterTitle: String,
        pages: List<ScannedPageDraft>
    ): File {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            color = Color.rgb(66, 133, 244)
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(24, 26, 28)
            textSize = 11f
        }
        val metaPaint = Paint().apply {
            color = Color.rgb(95, 99, 104)
            textSize = 9f
        }

        pages.forEachIndexed { idx, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = pdfPage.canvas
            canvas.drawColor(Color.WHITE)

            canvas.drawText("$subjectName — $chapterTitle", 40f, 48f, metaPaint)
            canvas.drawText("Page ${idx + 1}: ${page.detectedHeading}", 40f, 76f, titlePaint)
            canvas.drawText(
                "Sharpness(Laplacian)=${page.laplacianSharpness} | pHash=${page.phash} | dHash=${page.dhash}",
                40f,
                98f,
                metaPaint
            )

            val words = page.ocrText.split(" ")
            var currentLine = StringBuilder()
            var y = 135f
            for (word in words) {
                if (currentLine.length + word.length + 1 > 78) {
                    canvas.drawText(currentLine.toString(), 40f, y, bodyPaint)
                    y += 18f
                    currentLine = StringBuilder(word)
                } else {
                    if (currentLine.isNotEmpty()) currentLine.append(" ")
                    currentLine.append(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), 40f, y, bodyPaint)
            }

            canvas.drawText("Generated by EngBuddy v2.0 Liquid-Glass Vault • $title", 40f, 805f, metaPaint)
            pdfDocument.finishPage(pdfPage)
        }

        val outDir = File(context.filesDir, "stitched_pdfs").apply { mkdirs() }
        val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
        val outFile = File(outDir, "${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outFile
    }

    suspend fun softDeleteDocument(docId: Long) {
        dao.softDeleteDocumentToTrash(docId)
    }

    suspend fun restoreFromTrash(docId: Long) {
        dao.restoreDocumentFromTrash(docId)
    }

    suspend fun runOfflineQueueSync(simulateNetworkFailure: Boolean = false) {
        if (simulateNetworkFailure) {
            val currentProfile = dao.getUserProfileOnce()
            val backoffSeconds = min(900L, (15.0 * 2.0.pow(1.0)).toLong())
            dao.enqueueOfflineTask(
                OfflineSyncQueueEntity(
                    operationType = "RETRY_BACKOFF",
                    title = "Exponential Backoff Scheduled (${backoffSeconds}s)",
                    payloadSummary = "Wi-Fi / Server unreachable (Wi-Fi Only = ${currentProfile?.wifiOnlySync ?: true}). Next retry in ${backoffSeconds}s.",
                    retryCount = 1,
                    nextRetryAt = System.currentTimeMillis() + backoffSeconds * 1000L,
                    status = "FAILED_BACKOFF",
                    lastError = "Network unavailable — queued safely on disk"
                )
            )
        } else {
            dao.markAllQueueTasksSynced()
        }
    }

    suspend fun explainNotes(
        query: String,
        mode: ExplainMode,
        selectedSubject: SubjectEntity?
    ): AiExplainerResponse {
        val pages = dao.getAllActivePagesOnce()
        val profile = dao.getUserProfileOnce()
        if (profile != null) {
            dao.upsertUserProfile(profile.copy(dailyAiCallsUsed = profile.dailyAiCallsUsed + 1))
        }
        return aiRepository.explainNotesWithRag(
            query = query,
            mode = mode,
            selectedSubject = selectedSubject,
            availablePages = pages
        )
    }

    suspend fun simulateClassroomSync(simulateInstitutionBlocked: Boolean) {
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        if (simulateInstitutionBlocked) {
            dao.upsertUserProfile(
                profile.copy(
                    classroomConnected = false,
                    classroomInstitutionBlocked = true
                )
            )
        } else {
            dao.upsertUserProfile(
                profile.copy(
                    classroomConnected = true,
                    classroomInstitutionBlocked = false
                )
            )
            val subjectsList = dao.getSubjectsOnce()
            val osSubj = subjectsList.find { it.code.startsWith("CS") } ?: subjectsList.firstOrNull()
            val docId = dao.insertVaultDocument(
                VaultDocumentEntity(
                    subjectId = osSubj?.id,
                    subjectName = osSubj?.name ?: "Operating Systems & Kernels",
                    chapterTitle = "Classroom Coursework Sync",
                    title = "[Classroom] Lab 5 Virtual Memory Page Replacement Handout.pdf",
                    sourceType = "CLASSROOM",
                    pageCount = 1,
                    orderingConfidence = 1.0,
                    status = "READY",
                    summaryText = "Synced from Google Classroom API (coursework.me.readonly): Belady's anomaly, LRU vs Clock page replacement."
                )
            )
            dao.insertDocumentPage(
                DocumentPageEntity(
                    documentId = docId,
                    pageNumber = 1,
                    detectedHeading = "Virtual Memory & Page Fault Servicing",
                    ocrText = "Effective Access Time (EAT) = (1 - p) * memory_access + p * page_fault_overhead. LRU uses stack/counters and does not suffer from Belady's anomaly.",
                    laplacianSharpness = 210.0,
                    phash = "91AE40C273F58B12",
                    dhash = "80BD3FB162E47A01",
                    ssimScore = 0.12,
                    duplicateFlag = "NONE",
                    confidence = 1.0
                )
            )
        }
    }

    suspend fun updateGmailRule(ruleId: Long, newRuleType: String) {
        dao.updateGmailSenderRuleType(ruleId, newRuleType)
    }

    suspend fun ingestSharedFile(
        fileName: String,
        mimeType: String,
        sourceChannel: String = "WHATSAPP_SHARE"
    ) {
        val subjectsList = dao.getSubjectsOnce()
        val targetSubject = subjectsList.firstOrNull()
        val docId = dao.insertVaultDocument(
            VaultDocumentEntity(
                subjectId = targetSubject?.id,
                subjectName = targetSubject?.name ?: "General Engineering",
                chapterTitle = "Shared via $sourceChannel",
                title = fileName,
                sourceType = sourceChannel,
                pageCount = 1,
                orderingConfidence = 0.95,
                status = "READY",
                summaryText = "Ingested via native OS Share Target ($mimeType). Indexed for offline search & RAG."
            )
        )
        dao.insertDocumentPage(
            DocumentPageEntity(
                documentId = docId,
                pageNumber = 1,
                detectedHeading = fileName.substringBeforeLast("."),
                ocrText = "Shared engineering document ($fileName) imported via $sourceChannel. Contains formulas, numerical derivations, and lab reference tables.",
                laplacianSharpness = 190.0,
                phash = "55AA33CC99FF1100",
                dhash = "449922BB88EE00FF",
                ssimScore = 0.15,
                duplicateFlag = "NONE",
                confidence = 0.95
            )
        )
    }

    suspend fun saveConfirmedCalendarEvents(events: List<CalendarEventEntity>) {
        events.forEach { ev ->
            dao.insertCalendarEvent(ev.copy(isConfirmed = true))
        }
    }

    suspend fun exportAccountDataJson(): String = withContext(Dispatchers.IO) {
        val profile = dao.getUserProfileOnce()
        val subjectsList = dao.getSubjectsOnce()
        val docsList = dao.getActiveVaultDocumentsOnce()

        val root = JSONObject().apply {
            put("exportedAtIso", java.time.Instant.now().toString())
            put("studentPass", JSONObject().apply {
                put("email", profile?.email ?: "")
                put("displayName", profile?.displayName ?: "")
                put("rollNumber", profile?.rollNumber ?: "23CS10042")
                put("collegeName", profile?.collegeName ?: "")
                put("branchName", profile?.branchName ?: "")
                put("semester", profile?.semester ?: "")
                put("sectionAndGroup", profile?.sectionAndGroup ?: "")
                put("workingDaysMode", profile?.workingDaysMode ?: "5_DAYS")
            })
            put("subjects", JSONArray().apply {
                subjectsList.forEach { s ->
                    put(JSONObject().apply {
                        put("code", s.code)
                        put("name", s.name)
                        put("attendedClasses", s.attendedClasses)
                        put("totalClasses", s.totalClasses)
                        put("cancelledClasses", s.cancelledClasses)
                        put("targetPercentage", s.targetPercentage)
                    })
                }
            })
            put("vaultDocumentsCount", docsList.size)
        }
        root.toString(2)
    }

    suspend fun deleteAccountData() = withContext(Dispatchers.IO) {
        dao.clearAttendanceLogs()
        dao.clearDocumentPages()
        dao.clearVaultDocuments()
        dao.clearOfflineQueue()
        dao.clearCalendarEvents()
        val current = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.upsertUserProfile(current.copy(isSignedIn = false, classroomConnected = false, gmailConnected = false))
    }
}
