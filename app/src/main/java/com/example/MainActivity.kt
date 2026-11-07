package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AttendanceNotificationHelper
import com.example.data.remote.ExplainMode
import com.example.ui.EngBuddyViewModel
import com.example.ui.MainDestination
import com.example.ui.components.FloatingLiquidGlassBottomBar
import com.example.ui.screens.AiBuddyChatScreen
import com.example.ui.screens.AttendanceBunkScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.SmartVaultScreen
import com.example.ui.screens.StudentProfilePassModal
import com.example.ui.theme.EngBuddyTheme
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.liquidGlassRimBrush

class MainActivity : ComponentActivity() {

    private val viewModel: EngBuddyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AttendanceNotificationHelper.ensureChannel(this)
        com.example.core.TimetableCheckWorker.schedule(this)
        handleIncomingShareIntent(intent)

        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val themeMode = when (userProfile.themeMode.uppercase()) {
                "LIGHT" -> ThemeMode.LIGHT
                "SYSTEM" -> ThemeMode.SYSTEM
                else -> ThemeMode.DARK
            }

            EngBuddyTheme(themeMode = themeMode) {
                EngBuddyAdaptiveRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val mime = intent.type ?: "application/pdf"
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val label = uri?.lastPathSegment ?: "WhatsApp_Shared_Note_${System.currentTimeMillis() % 1000}.pdf"
                viewModel.handleIncomingShareIntent(label, mime, uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val mime = intent.type ?: "image/*"
                @Suppress("DEPRECATION")
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                viewModel.handleIncomingShareIntent("WhatsApp_Batch_Shared_Notes.pdf", mime, uris?.firstOrNull())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngBuddyAdaptiveRoot(viewModel: EngBuddyViewModel) {
    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val subjectAnalyses by viewModel.subjectAnalyses.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val activeDocs by viewModel.activeDocuments.collectAsStateWithLifecycle()
    val trashedDocs by viewModel.trashedDocuments.collectAsStateWithLifecycle()
    val allPages by viewModel.allActivePages.collectAsStateWithLifecycle()
    val offlineQueue by viewModel.offlineQueue.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val calendarDrafts by viewModel.calendarDrafts.collectAsStateWithLifecycle()
    val gmailRules by viewModel.gmailSenderRules.collectAsStateWithLifecycle()
    val overlapPrefs by viewModel.overlapPrefs.collectAsStateWithLifecycle()
    val scannedDrafts by viewModel.scannedDrafts.collectAsStateWithLifecycle()
    val batchClassification by viewModel.batchClassification.collectAsStateWithLifecycle()
    val scannerBanner by viewModel.scannerStatusBanner.collectAsStateWithLifecycle()
    val simulateTuesdayThermo by viewModel.simulateTimetableTuesdayThermo.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJsonPreview.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showStudentPassSheet by remember { mutableStateOf(false) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }
    var desktopSplitScreenExplainer by remember { mutableStateOf(false) }

    BackHandler(enabled = currentDest != MainDestination.HOME) {
        viewModel.navigateTo(MainDestination.HOME)
    }

    LaunchedEffect(statusToast) {
        statusToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusToast()
        }
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        if (isWideScreen) {
                            IconButton(
                                onClick = { isSidebarCollapsed = !isSidebarCollapsed },
                                modifier = Modifier.testTag("toggle_sidebar_button")
                            ) {
                                Icon(
                                    imageVector = if (isSidebarCollapsed) {
                                        Icons.Default.Menu
                                    } else {
                                        Icons.AutoMirrored.Filled.MenuOpen
                                    },
                                    contentDescription = "Toggle Collapsible Sidebar"
                                )
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EngBuddy",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = GoogleBlue.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "v2.0",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoogleBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (isWideScreen) {
                            IconButton(
                                onClick = { desktopSplitScreenExplainer = !desktopSplitScreenExplainer },
                                modifier = Modifier.testTag("desktop_split_screen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Splitscreen,
                                    contentDescription = "Toggle Desktop Split-Screen AI Explainer",
                                    tint = if (desktopSplitScreenExplainer) GoogleBlue else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val nextMode = when (userProfile.themeMode.uppercase()) {
                                    "DARK" -> "LIGHT"
                                    "LIGHT" -> "SYSTEM"
                                    else -> "DARK"
                                }
                                viewModel.setThemeMode(nextMode)
                            },
                            modifier = Modifier.testTag("theme_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contrast,
                                contentDescription = "Switch Theme (Obsidian / Alabaster / System)"
                            )
                        }

                        // Top-Right Sleek Liquid-Glass Avatar Pill -> Opens Student Profile & Digital ID Pass
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, liquidGlassRimBrush(isDark = isDark), RoundedCornerShape(50))
                                .clickable { showStudentPassSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("student_pass_avatar_pill")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(GoogleBlue.copy(alpha = 0.24f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = "Student Pass",
                                        tint = GoogleBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = userProfile.rollNumber,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoogleYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    FloatingLiquidGlassBottomBar(
                        currentDestination = currentDest,
                        onNavigate = viewModel::navigateTo,
                        iconForDestination = ::destinationIcon,
                        modifier = Modifier.testTag("mobile_bottom_nav_bar")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(if (isSidebarCollapsed) 80.dp else 120.dp)
                            .testTag("desktop_navigation_rail")
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MainDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDest == dest,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = destinationIcon(dest),
                                        contentDescription = dest.label
                                    )
                                },
                                label = if (!isSidebarCollapsed) {
                                    { Text(dest.label) }
                                } else {
                                    null
                                },
                                modifier = Modifier.testTag("rail_item_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentDest) {
                        MainDestination.HOME -> HomeFeedScreen(
                            userProfile = userProfile,
                            subjectAnalyses = subjectAnalyses,
                            timetableSlots = timetableSlots,
                            calendarEvents = calendarEvents,
                            calendarDrafts = calendarDrafts,
                            gmailRules = gmailRules,
                            offlineQueue = offlineQueue,
                            onNavigate = viewModel::navigateTo,
                            onQuickAskAi = { q ->
                                viewModel.runAiExplanation(q, ExplainMode.CUSTOM, null)
                            },
                            onLogAttendanceFromPrompt = viewModel::logAttendance,
                            onFireOsAttendanceNotification = viewModel::fireInteractiveAttendanceNotification,
                            onSimulateWhatsappShare = viewModel::handleIncomingShareIntent,
                            onTriggerClassroomSync = viewModel::triggerClassroomSync,
                            onUpdateGmailRule = viewModel::updateGmailSenderRule,
                            onParseCalendarSample = viewModel::parseAcademicCalendarUpload,
                            onUpdateCalendarDraftTitle = viewModel::updateCalendarDraftTitle,
                            onRemoveCalendarDraft = viewModel::removeCalendarDraft,
                            onConfirmCalendarDrafts = viewModel::confirmAndSaveParsedCalendar,
                            onSyncOfflineQueue = viewModel::syncOfflineQueue,
                            onToggleWifiOnly = viewModel::setWifiOnlySync
                        )

                        MainDestination.VAULT -> SmartVaultScreen(
                            subjects = subjects,
                            activeDocuments = activeDocs,
                            trashedDocuments = trashedDocs,
                            allPages = allPages,
                            onExplainDocumentInAiBuddy = { doc ->
                                val subj = subjects.find { it.id == doc.subjectId }
                                viewModel.runAiExplanation(
                                    query = "Explain ${doc.title}",
                                    mode = ExplainMode.ELI5,
                                    subject = subj
                                )
                                viewModel.navigateTo(MainDestination.AI_BUDDY)
                            },
                            onSoftDeleteToTrash = viewModel::softDeleteVaultDocument,
                            onRestoreFromTrash = viewModel::restoreVaultDocumentFromTrash
                        )

                        MainDestination.SCANNER -> CameraScannerScreen(
                            scannedDrafts = scannedDrafts,
                            batchClassification = batchClassification,
                            scannerStatusBanner = scannerBanner,
                            simulateTimetableTuesdayThermo = simulateTuesdayThermo,
                            overlapPrefs = overlapPrefs,
                            onToggleTimetableSimulation = viewModel::setTimetableSimulation,
                            onRunBatchScan = viewModel::runOnDeviceScannerPipeline,
                            onMovePageOrder = viewModel::moveScannedPageInOrder,
                            onResolveOverlapWithOneTap = viewModel::resolveSubjectOverlapWithOneTap,
                            onCommitPdfToVault = viewModel::commitScannedBatchToPdf,
                            onCaptureLiveBitmap = viewModel::ingestLiveCapturedBitmap
                        )

                        MainDestination.ATTENDANCE -> AttendanceBunkScreen(
                            workingDaysMode = userProfile.workingDaysMode,
                            timetableSlots = timetableSlots,
                            analyses = subjectAnalyses,
                            onSetWorkingDaysMode = viewModel::setWorkingDaysMode,
                            onLogAttendance = viewModel::logAttendance,
                            onFireInteractiveNotification = viewModel::fireInteractiveAttendanceNotification,
                            onIngestTimetableVisionSample = { subj ->
                                viewModel.ingestNewTimetableSlot(
                                    subject = subj,
                                    dayOfWeek = 1,
                                    startHour = 11,
                                    endHour = 12,
                                    room = "LH-302",
                                    isLab = false
                                )
                            },
                            onUpdateSubjectRules = viewModel::updateSubjectRules
                        )

                        MainDestination.AI_BUDDY -> AiBuddyChatScreen(
                            subjects = subjects,
                            aiResponse = aiResponse,
                            isAiLoading = isAiLoading,
                            firebaseNotice = viewModel.repository.aiRepository.requiresFirebaseNotice,
                            onRunExplain = viewModel::runAiExplanation
                        )
                    }
                }

                AnimatedVisibility(visible = isWideScreen && desktopSplitScreenExplainer && currentDest != MainDestination.AI_BUDDY) {
                    Box(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        AiBuddyChatScreen(
                            subjects = subjects,
                            aiResponse = aiResponse,
                            isAiLoading = isAiLoading,
                            firebaseNotice = viewModel.repository.aiRepository.requiresFirebaseNotice,
                            onRunExplain = viewModel::runAiExplanation
                        )
                    }
                }
            }
        }
    }

    if (showStudentPassSheet) {
        StudentProfilePassModal(
            userProfile = userProfile,
            exportedJson = exportedJson,
            onDismiss = {
                viewModel.clearExportedJsonPreview()
                showStudentPassSheet = false
            },
            onSaveProfile = viewModel::saveStudentProfile,
            onUpdateIdCardSlot = viewModel::updateDigitalIdCard,
            onSetThemeMode = viewModel::setThemeMode,
            onExportJson = viewModel::exportUserDataJson,
            onDeleteAccount = {
                viewModel.deleteUserAccountAndData()
                showStudentPassSheet = false
            },
            onCopiedRollNumber = { roll ->
                // Triggered when user 1-tap copies their roll number
            }
        )
    }
}

private fun destinationIcon(dest: MainDestination): ImageVector = when (dest) {
    MainDestination.HOME -> Icons.Default.Home
    MainDestination.VAULT -> Icons.Default.FolderSpecial
    MainDestination.SCANNER -> Icons.Default.CameraAlt
    MainDestination.ATTENDANCE -> Icons.Default.EventAvailable
    MainDestination.AI_BUDDY -> Icons.Default.AutoAwesome
}
