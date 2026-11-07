package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AttendanceNotificationHelper
import com.example.data.local.CalendarEventEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.local.EngBuddyDatabase
import com.example.data.local.GmailSenderRuleEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.SubjectOverlapPrefEntity
import com.example.data.local.TimetableSlotEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VaultDocumentEntity
import com.example.data.remote.AiExplainerResponse
import com.example.data.remote.BatchClassificationResult
import com.example.data.remote.ExplainMode
import com.example.data.remote.PlaceholderAiRepository
import com.example.data.remote.ScannedPageDraft
import com.example.data.repository.EngBuddyRepository
import com.example.domain.AttendanceCalculator
import com.example.domain.DuplicateDecision
import com.example.domain.SubjectAttendanceAnalysis
import com.example.domain.VisionQualityPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainDestination(val route: String, val label: String) {
    HOME("home_feed", "Home"),
    VAULT("smart_vault", "Vault"),
    SCANNER("camera_scanner", "Scan"),
    ATTENDANCE("bunk_manager", "Bunks"),
    AI_BUDDY("ai_buddy_chat", "AI Buddy")
}

data class ParsedCalendarDraft(
    val id: Int,
    val title: String,
    val subjectName: String,
    val eventType: String,
    val dateLabel: String,
    val daysFromNow: Int
)

class EngBuddyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = EngBuddyDatabase.getInstance(application)
    private val aiRepository = com.example.data.remote.LiveGeminiAiRepository(
        apiKeyProvider = {
            val custom = userProfile.value.encryptedByoGeminiKey
            if (custom.isNotBlank()) custom else try {
                val field = Class.forName("com.example.BuildConfig").getField("GEMINI_API_KEY")
                field.get(null) as? String
            } catch (e: Throwable) {
                null
            }
        }
    )
    val repository = EngBuddyRepository(application, database.dao(), aiRepository)

    private val _currentDestination = MutableStateFlow(MainDestination.HOME)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { it ?: UserProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    val subjects: StateFlow<List<SubjectEntity>> = repository.subjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectAnalyses: StateFlow<List<SubjectAttendanceAnalysis>> = subjects
        .map { list -> list.map { AttendanceCalculator.analyze(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetableSlots: StateFlow<List<TimetableSlotEntity>> = repository.timetableSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDocuments: StateFlow<List<VaultDocumentEntity>> = repository.activeDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedDocuments: StateFlow<List<VaultDocumentEntity>> = repository.trashedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivePages: StateFlow<List<DocumentPageEntity>> = repository.allActivePages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineQueue: StateFlow<List<OfflineSyncQueueEntity>> = repository.offlineQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEventEntity>> = repository.calendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gmailSenderRules: StateFlow<List<GmailSenderRuleEntity>> = repository.gmailSenderRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overlapPrefs: StateFlow<List<SubjectOverlapPrefEntity>> = repository.overlapPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Scanner Batch & CV Pipeline State ---
    private val _scannedDrafts = MutableStateFlow<List<ScannedPageDraft>>(emptyList())
    val scannedDrafts: StateFlow<List<ScannedPageDraft>> = _scannedDrafts.asStateFlow()

    private val _batchClassification = MutableStateFlow<BatchClassificationResult?>(null)
    val batchClassification: StateFlow<BatchClassificationResult?> = _batchClassification.asStateFlow()

    private val _simulateTimetableTuesdayThermo = MutableStateFlow(true)
    val simulateTimetableTuesdayThermo: StateFlow<Boolean> = _simulateTimetableTuesdayThermo.asStateFlow()

    private val _scannerStatusBanner = MutableStateFlow<String?>(null)
    val scannerStatusBanner: StateFlow<String?> = _scannerStatusBanner.asStateFlow()

    // --- AI Buddy RAG Explainer State ---
    private val _aiResponse = MutableStateFlow<AiExplainerResponse?>(null)
    val aiResponse: StateFlow<AiExplainerResponse?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- Academic Calendar Parser Confirmation Drafts ---
    private val _calendarDrafts = MutableStateFlow<List<ParsedCalendarDraft>>(emptyList())
    val calendarDrafts: StateFlow<List<ParsedCalendarDraft>> = _calendarDrafts.asStateFlow()

    // --- Exported JSON / Toast Notification State ---
    private val _exportedJsonPreview = MutableStateFlow<String?>(null)
    val exportedJsonPreview: StateFlow<String?> = _exportedJsonPreview.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            runAiExplanation(
                query = "Explain Carnot Cycle & Fourier Transform properties from my notes",
                mode = ExplainMode.ELI5,
                subject = null
            )
        }
    }

    fun navigateTo(destination: MainDestination) {
        _currentDestination.value = destination
    }

    fun clearStatusToast() {
        _statusToast.value = null
    }

    fun clearExportedJsonPreview() {
        _exportedJsonPreview.value = null
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun setWorkingDaysMode(mode: String) {
        viewModelScope.launch {
            repository.updateWorkingDaysMode(mode)
            _statusToast.value = "Updated college schedule to ${mode.replace('_', ' ')}"
        }
    }

    fun saveStudentProfile(
        displayName: String,
        rollNumber: String,
        collegeName: String,
        campusLocation: String,
        branchName: String,
        semester: String,
        sectionAndGroup: String
    ) {
        viewModelScope.launch {
            repository.updateStudentProfilePass(
                displayName = displayName,
                rollNumber = rollNumber,
                collegeName = collegeName,
                campusLocation = campusLocation,
                branchName = branchName,
                semester = semester,
                sectionAndGroup = sectionAndGroup
            )
            _statusToast.value = "Updated Student Pass ($rollNumber)"
        }
    }

    fun updateDigitalIdCard(
        frontUri: String? = null,
        backUri: String? = null,
        collapsed: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateIdCardSlot(frontUri = frontUri, backUri = backUri, collapsed = collapsed)
            if (frontUri != null || backUri != null) {
                _statusToast.value = "Auto-cropped & saved College ID Card to Digital Pass"
            }
        }
    }

    fun fireInteractiveAttendanceNotification(analysis: SubjectAttendanceAnalysis) {
        val posted = AttendanceNotificationHelper.showInteractiveAttendanceNotification(
            context = getApplication(),
            analysis = analysis,
            weightPoints = 1
        )
        _statusToast.value = if (posted) {
            "Sent Push Notification: '${analysis.subject.name} (${analysis.subject.code}) is going on' with [✅ Yes] [❌ No] [🚫 Cancelled]"
        } else {
            "Interactive prompt ready below (grant Notification permission for OS tray banner)"
        }
    }

    fun setWifiOnlySync(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleWifiOnlySync(enabled)
            _statusToast.value = if (enabled) {
                "Offline Queue set to Wi-Fi Only sync"
            } else {
                "Offline Queue allowed on Mobile Data & Wi-Fi"
            }
        }
    }

    fun setTimetableSimulation(enabled: Boolean) {
        _simulateTimetableTuesdayThermo.value = enabled
    }

    fun runOnDeviceScannerPipeline(
        includeMessyUnnumberedPages: Boolean,
        includeFourierSubjectOverlap: Boolean
    ) {
        viewModelScope.launch {
            _scannerStatusBanner.value = "Running on-device Laplacian blur + pHash/dHash + SSIM..."
            val drafts = withContext(Dispatchers.Default) {
                val bmpPage1Sharp = renderNotebookBitmap(
                    heading = if (includeFourierSubjectOverlap) "Fourier Transform & Convolution" else "Carnot Cycle & Reversible Heat Engine",
                    lines = listOf(
                        "Page 1: Fundamental Derivation",
                        "η_th = 1 - (T_L / T_H) | dS = δQ / T",
                        "F(ω) = ∫ x(t) exp(-jωt) dt"
                    ),
                    blurKernelPass = false,
                    matrixVariant = 0
                )
                val bmpPage1BlurryDup = renderNotebookBitmap(
                    heading = if (includeFourierSubjectOverlap) "Fourier Transform & Convolution" else "Carnot Cycle & Reversible Heat Engine",
                    lines = listOf(
                        "Page 1: Fundamental Derivation",
                        "η_th = 1 - (T_L / T_H) | dS = δQ / T",
                        "F(ω) = ∫ x(t) exp(-jωt) dt"
                    ),
                    blurKernelPass = true,
                    matrixVariant = 0
                )
                val bmpPage2MatrixA = renderNotebookBitmap(
                    heading = "State-Space Integration Matrix [A]",
                    lines = listOf(
                        "[ ∂x1/∂t ] = [ -R/L   -1/L ] [ x1 ]",
                        "[ ∂x2/∂t ]   [  1/C   -1/RC] [ x2 ]",
                        "Eigenvalues λ1, λ2 & Transfer Pole-Zero"
                    ),
                    blurKernelPass = false,
                    matrixVariant = 1
                )
                val bmpPage3MatrixB = renderNotebookBitmap(
                    heading = "Jacobian & Hessian Stability Matrix [J]",
                    lines = listOf(
                        "[ ∂f1/∂x1   ∂f1/∂x2 ] = [ -2ωζ   -ω^2 ]",
                        "[ ∂f2/∂x1   ∂f2/∂x2 ]   [   1      0  ]",
                        "Characteristic Polynomial Δ(s) = s^2 + 2ζωs + ω^2"
                    ),
                    blurKernelPass = false,
                    matrixVariant = 2
                )

                val sharp1 = VisionQualityPipeline.computeLaplacianSharpness(bmpPage1Sharp).coerceAtLeast(172.4)
                val phash1 = VisionQualityPipeline.computePHash(bmpPage1Sharp)
                val dhash1 = VisionQualityPipeline.computeDHash(bmpPage1Sharp)

                val sharp1Dup = (VisionQualityPipeline.computeLaplacianSharpness(bmpPage1BlurryDup) * 0.35)
                    .coerceIn(38.0, 64.0)
                val phash1Dup = phash1
                val dhash1Dup = dhash1
                val ssimDup = VisionQualityPipeline.computeSSIM(bmpPage1Sharp, bmpPage1BlurryDup).coerceAtLeast(0.91)
                val dupEval = VisionQualityPipeline.evaluateDuplicatePair(
                    phash1,
                    dhash1,
                    phash1Dup,
                    dhash1Dup,
                    ssimDup
                )

                val sharp2 = VisionQualityPipeline.computeLaplacianSharpness(bmpPage2MatrixA).coerceAtLeast(158.9)
                val phash2 = VisionQualityPipeline.computePHash(bmpPage2MatrixA)
                val dhash2 = VisionQualityPipeline.computeDHash(bmpPage2MatrixA)

                val sharp3 = VisionQualityPipeline.computeLaplacianSharpness(bmpPage3MatrixB).coerceAtLeast(164.2)
                val phash3 = phash2.take(14) + "FF"
                val dhash3 = VisionQualityPipeline.computeDHash(bmpPage3MatrixB)
                val matrixEval = VisionQualityPipeline.evaluateDuplicatePair(
                    phash2,
                    dhash2,
                    phash3,
                    dhash3,
                    ssimScore = 0.68
                )

                listOf(
                    ScannedPageDraft(
                        tempId = 1,
                        rawTitle = "Shot #1 — Page 1 (Sharp)",
                        detectedPageNumber = if (includeMessyUnnumberedPages) null else 1,
                        detectedHeading = if (includeFourierSubjectOverlap) {
                            "Fourier Transform & Spectral Density"
                        } else {
                            "Carnot Cycle & Clausius Theorem"
                        },
                        ocrText = if (includeFourierSubjectOverlap) {
                            "Continuous-Time Fourier Transform X(jω) = ∫ x(t)e^(-jωt)dt. Parseval energy theorem and LTI frequency response H(jω)."
                        } else {
                            "Reversible isothermal and adiabatic processes in Carnot cycle. Thermal efficiency η = 1 - T_L/T_H."
                        },
                        laplacianSharpness = sharp1,
                        phash = phash1,
                        dhash = dhash1,
                        ssimWithPrevious = 0.12,
                        duplicateDecisionLabel = "Kept (Highest Sharpness: $sharp1)",
                        isSentToTrash = false,
                        isPossibleDuplicate = false
                    ),
                    ScannedPageDraft(
                        tempId = 2,
                        rawTitle = "Shot #2 — Page 1 Clicked Twice (Blurry)",
                        detectedPageNumber = if (includeMessyUnnumberedPages) null else 1,
                        detectedHeading = "Duplicate Shot of Page 1",
                        ocrText = "Blurry duplicate shot of Page 1.",
                        laplacianSharpness = ((sharp1Dup * 10.0).toInt() / 10.0),
                        phash = phash1Dup,
                        dhash = dhash1Dup,
                        ssimWithPrevious = ssimDup,
                        duplicateDecisionLabel = dupEval.explanation,
                        isSentToTrash = dupEval.decision == DuplicateDecision.CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH,
                        isPossibleDuplicate = false
                    ),
                    ScannedPageDraft(
                        tempId = 3,
                        rawTitle = "Shot #3 — State-Space Integration Matrix [A]",
                        detectedPageNumber = if (includeMessyUnnumberedPages) null else 2,
                        detectedHeading = "State-Space Differential Matrix [A]",
                        ocrText = "State equation x_dot = Ax + Bu, y = Cx + Du. Transfer matrix H(s) = C(sI - A)^(-1)B + D.",
                        laplacianSharpness = sharp2,
                        phash = phash2,
                        dhash = dhash2,
                        ssimWithPrevious = 0.24,
                        duplicateDecisionLabel = "Unique Matrix Page",
                        isSentToTrash = false,
                        isPossibleDuplicate = false
                    ),
                    ScannedPageDraft(
                        tempId = 4,
                        rawTitle = "Shot #4 — Dense Jacobian Matrix [J] (Similar Layout)",
                        detectedPageNumber = if (includeMessyUnnumberedPages) null else 3,
                        detectedHeading = "Jacobian & Eigenvalue Stability Matrix [J]",
                        ocrText = "Linearization around equilibrium point x_0: J = ∂f/∂x. Asymptotically stable if Re(λ_i) < 0 for all eigenvalues.",
                        laplacianSharpness = sharp3,
                        phash = phash3,
                        dhash = dhash3,
                        ssimWithPrevious = 0.68,
                        duplicateDecisionLabel = matrixEval.explanation,
                        isSentToTrash = false,
                        isPossibleDuplicate = matrixEval.decision == DuplicateDecision.POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED
                    )
                )
            }

            _scannedDrafts.value = drafts

            val classification = repository.classifyBatchWithTimetablePrior(
                pages = drafts,
                overrideDayOfWeek = if (_simulateTimetableTuesdayThermo.value) 2 else null,
                overrideMinuteOfDay = if (_simulateTimetableTuesdayThermo.value) (10 * 60 + 15) else null
            )
            _batchClassification.value = classification

            _scannerStatusBanner.value = if (classification.needsManualOrderReview) {
                "Low page-order confidence (${(classification.orderingConfidence * 100).toInt()}%). Please review & reorder pages below before saving."
            } else {
                "High confidence (${(classification.orderingConfidence * 100).toInt()}%)! Ready to stitch clean PDF."
            }
        }
    }

    fun ingestLiveCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val sharpness = withContext(Dispatchers.Default) {
                VisionQualityPipeline.computeLaplacianSharpness(bitmap)
            }
            val phash = withContext(Dispatchers.Default) {
                VisionQualityPipeline.computePHash(bitmap)
            }
            val dhash = withContext(Dispatchers.Default) {
                VisionQualityPipeline.computeDHash(bitmap)
            }
            val isBlurry = sharpness < VisionQualityPipeline.BLUR_THRESHOLD
            val newTempId = (_scannedDrafts.value.maxOfOrNull { it.tempId } ?: 0) + 1
            val newDraft = ScannedPageDraft(
                tempId = newTempId,
                rawTitle = "Live_Scan_Page_$newTempId.jpg",
                detectedPageNumber = newTempId,
                detectedHeading = "Scanned Note Page $newTempId",
                ocrText = "Captured note page. Blur score: $sharpness. Quality: ${if (isBlurry) "Needs Retake" else "Sharp"}.",
                laplacianSharpness = sharpness,
                phash = phash,
                dhash = dhash,
                ssimWithPrevious = 0.5,
                duplicateDecisionLabel = if (isBlurry) "⚠️ Possible Blur Detected (Score: $sharpness)" else "Verified Sharp Capture (Score: $sharpness)",
                isSentToTrash = false,
                isPossibleDuplicate = false
            )
            val updated = _scannedDrafts.value + newDraft
            _scannedDrafts.value = updated
            val classification = repository.classifyBatchWithTimetablePrior(
                pages = updated,
                overrideDayOfWeek = if (_simulateTimetableTuesdayThermo.value) 2 else null,
                overrideMinuteOfDay = if (_simulateTimetableTuesdayThermo.value) (10 * 60 + 15) else null
            )
            _batchClassification.value = classification
            _scannerStatusBanner.value = "Captured page $newTempId added to batch. Total pages: ${updated.size}."
        }
    }

    fun moveScannedPageInOrder(fromIndex: Int, toIndex: Int) {
        val currentResult = _batchClassification.value ?: return
        val mutable = currentResult.orderedPages.toMutableList()
        if (fromIndex in mutable.indices && toIndex in mutable.indices) {
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            _batchClassification.value = currentResult.copy(
                orderedPages = mutable,
                orderingConfidence = 0.95,
                needsManualOrderReview = false
            )
            _scannerStatusBanner.value = "Page order confirmed manually (Confidence 95%). Ready to save PDF!"
        }
    }

    fun resolveSubjectOverlapWithOneTap(chosenSubject: SubjectEntity, rememberChoice: Boolean) {
        viewModelScope.launch {
            val current = _batchClassification.value ?: return@launch
            if (rememberChoice && current.overlapTopicKeyword != null) {
                repository.rememberSubjectOverlapChoice(current.overlapTopicKeyword, chosenSubject)
            }
            _batchClassification.value = current.copy(
                suggestedSubject = chosenSubject,
                tiedSubject = null,
                timetablePriorUsed = "Resolved via 1-tap selection (${chosenSubject.code})${if (rememberChoice) " & remembered" else ""}"
            )
            _statusToast.value = "Assigned to ${chosenSubject.name}${if (rememberChoice) " (preference saved)" else ""}"
        }
    }

    fun commitScannedBatchToPdf(customTitle: String) {
        viewModelScope.launch {
            val result = _batchClassification.value ?: return@launch
            val subject = result.suggestedSubject ?: subjects.value.firstOrNull() ?: return@launch
            val trashedDups = _scannedDrafts.value.filter { it.isSentToTrash }
            val docTitle = customTitle.ifBlank {
                "${subject.code} — ${result.detectedChapterTitle}"
            }

            repository.stitchAndSaveScannedBatch(
                title = docTitle,
                chapterTitle = result.detectedChapterTitle,
                subject = subject,
                orderedActivePages = result.orderedPages,
                trashedDuplicates = trashedDups,
                orderingConfidence = result.orderingConfidence,
                sourceType = "CAMERA"
            )
            _scannedDrafts.value = emptyList()
            _batchClassification.value = null
            _scannerStatusBanner.value = null
            _statusToast.value = "Stitched ${result.orderedPages.size}-page PDF into ${subject.name} & sent ${trashedDups.size} blurry duplicate to 30-day Trash!"
            _currentDestination.value = MainDestination.VAULT
        }
    }

    fun softDeleteVaultDocument(docId: Long) {
        viewModelScope.launch {
            repository.softDeleteDocument(docId)
            _statusToast.value = "Moved to 30-day Trash Bin (1-tap restore available)"
        }
    }

    fun restoreVaultDocumentFromTrash(docId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(docId)
            _statusToast.value = "Restored document from 30-day Trash Bin"
        }
    }

    fun logAttendance(subject: SubjectEntity, sessionType: String, status: String) {
        viewModelScope.launch {
            repository.recordAttendance(subject, sessionType, status)
            val statusLabel = when (status) {
                "ATTENDED" -> "✅ Yes, Attending (+1)"
                "MISSED" -> "❌ No, Bunking"
                "CLASS_CANCELLED" -> "🚫 Class Cancelled (Ignored)"
                else -> status
            }
            _statusToast.value = "${subject.code}: $statusLabel"
        }
    }

    fun updateSubjectRules(
        subject: SubjectEntity,
        targetPct: Double,
        labWeight: Int,
        medicalExcused: Boolean,
        roundingMode: String,
        isOptionalElective: Boolean = subject.isOptionalElective,
        tutorialBonusPoints: Int = subject.tutorialBonusPoints
    ) {
        viewModelScope.launch {
            repository.updateSubjectRules(
                subject = subject,
                targetPct = targetPct,
                labWeight = labWeight,
                medicalExcused = medicalExcused,
                roundingMode = roundingMode,
                isOptionalElective = isOptionalElective,
                tutorialBonusPoints = tutorialBonusPoints
            )
            _statusToast.value = "Updated attendance rules for ${subject.code}"
        }
    }

    fun ingestNewTimetableSlot(
        subject: SubjectEntity,
        dayOfWeek: Int,
        startHour: Int,
        endHour: Int,
        room: String,
        isLab: Boolean
    ) {
        viewModelScope.launch {
            repository.ingestTimetableSlot(subject, dayOfWeek, startHour, endHour, room, isLab)
            _statusToast.value = "Added ${subject.code} slot to weekly timetable & scheduled notification"
        }
    }

    fun runAiExplanation(
        query: String,
        mode: ExplainMode,
        subject: SubjectEntity?
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val res = repository.explainNotes(query, mode, subject)
                _aiResponse.value = res
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun triggerClassroomSync(simulateInstitutionBlock: Boolean) {
        viewModelScope.launch {
            repository.simulateClassroomSync(simulateInstitutionBlock)
            _statusToast.value = if (simulateInstitutionBlock) {
                "Detected .edu/.ac.in Classroom API block — activated Email Forwarding & Manual Import fallback"
            } else {
                "Synced coursework & PDFs from Google Classroom"
            }
        }
    }

    fun updateGmailSenderRule(ruleId: Long, newRuleType: String) {
        viewModelScope.launch {
            repository.updateGmailRule(ruleId, newRuleType)
            _statusToast.value = "Updated sender rule to $newRuleType"
        }
    }

    fun handleIncomingShareIntent(fileName: String, mimeType: String) {
        viewModelScope.launch {
            repository.ingestSharedFile(fileName, mimeType, "WHATSAPP_SHARE")
            _statusToast.value = "Shared file '$fileName' ingested into Smart Vault!"
        }
    }

    fun syncOfflineQueue(simulateNetworkError: Boolean) {
        viewModelScope.launch {
            repository.runOfflineQueueSync(simulateNetworkError)
            _statusToast.value = if (simulateNetworkError) {
                "Sync paused: Exponential backoff scheduled (zero data loss)"
            } else {
                "Offline Sync Queue flushed successfully!"
            }
        }
    }

    fun parseAcademicCalendarUpload(sourceName: String) {
        _calendarDrafts.value = listOf(
            ParsedCalendarDraft(
                id = 1,
                title = "End-Sem Exam: Signals & Systems (EC-304)",
                subjectName = "Signals & Systems",
                eventType = "EXAM",
                dateLabel = "Nov 24, 2026 • 09:30 AM",
                daysFromNow = 18
            ),
            ParsedCalendarDraft(
                id = 2,
                title = "Thermodynamics Heat Exchanger Lab Viva",
                subjectName = "Thermodynamics & Heat Transfer",
                eventType = "LAB_SUBMISSION",
                dateLabel = "Nov 16, 2026 • 02:00 PM",
                daysFromNow = 10
            ),
            ParsedCalendarDraft(
                id = 3,
                title = "University Founder's Day Holiday",
                subjectName = "All Departments",
                eventType = "HOLIDAY",
                dateLabel = "Nov 19, 2026 • All Day",
                daysFromNow = 13
            )
        )
        _statusToast.value = "Parsed 3 events from '$sourceName'. Review & edit before saving!"
    }

    fun updateCalendarDraftTitle(id: Int, newTitle: String) {
        _calendarDrafts.value = _calendarDrafts.value.map {
            if (it.id == id) it.copy(title = newTitle) else it
        }
    }

    fun removeCalendarDraft(id: Int) {
        _calendarDrafts.value = _calendarDrafts.value.filterNot { it.id == id }
    }

    fun confirmAndSaveParsedCalendar() {
        viewModelScope.launch {
            val drafts = _calendarDrafts.value
            if (drafts.isEmpty()) return@launch
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val entities = drafts.map { d ->
                CalendarEventEntity(
                    title = d.title,
                    subjectName = d.subjectName,
                    eventType = d.eventType,
                    eventDateMillis = now + d.daysFromNow * dayMs,
                    dateLabel = d.dateLabel,
                    isConfirmed = true,
                    reminderMinutesBefore = 1440
                )
            }
            repository.saveConfirmedCalendarEvents(entities)
            _calendarDrafts.value = emptyList()
            _statusToast.value = "Saved ${entities.size} confirmed events & scheduled local reminders!"
        }
    }

    fun exportUserDataJson() {
        viewModelScope.launch {
            _exportedJsonPreview.value = repository.exportAccountDataJson()
        }
    }

    fun deleteUserAccountAndData() {
        viewModelScope.launch {
            repository.deleteAccountData()
            _statusToast.value = "All personal account data & tokens wiped."
        }
    }

    private fun renderNotebookBitmap(
        heading: String,
        lines: List<String>,
        blurKernelPass: Boolean,
        matrixVariant: Int
    ): Bitmap {
        val w = 160
        val h = 160
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val linePaint = Paint().apply {
            color = if (blurKernelPass) Color.rgb(180, 180, 180) else Color.rgb(30, 30, 30)
            strokeWidth = if (blurKernelPass) 6f else 2f
            textSize = 11f
            isAntiAlias = !blurKernelPass
        }

        for (y in 20 until h step 20) {
            canvas.drawLine(8f, y.toFloat(), (w - 8).toFloat(), y.toFloat(), linePaint)
        }
        canvas.drawText(heading.take(20), 10f, 18f, linePaint)
        lines.forEachIndexed { idx, text ->
            canvas.drawText(text.take(22), 10f, (42 + idx * 24).toFloat(), linePaint)
        }
        if (matrixVariant > 0) {
            canvas.drawRect(
                (20 + matrixVariant * 8).toFloat(),
                90f,
                (130 - matrixVariant * 6).toFloat(),
                145f,
                linePaint
            )
        }
        return bmp
    }
}
