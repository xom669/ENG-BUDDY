package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CalendarEventEntity
import com.example.data.local.GmailSenderRuleEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TimetableSlotEntity
import com.example.data.local.UserProfileEntity
import com.example.domain.AttendanceZone
import com.example.domain.SubjectAttendanceAnalysis
import com.example.ui.MainDestination
import com.example.ui.ParsedCalendarDraft
import com.example.ui.components.AmbientFluidMeshHeaderBox
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.JetBrainsMonoFontFamily

private enum class HomeSecondarySheet {
    NONE,
    OMNICHANNEL_INGESTION,
    OFFLINE_QUEUE,
    ACADEMIC_CALENDAR
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    userProfile: UserProfileEntity,
    subjectAnalyses: List<SubjectAttendanceAnalysis>,
    timetableSlots: List<TimetableSlotEntity>,
    calendarEvents: List<CalendarEventEntity>,
    calendarDrafts: List<ParsedCalendarDraft>,
    gmailRules: List<GmailSenderRuleEntity>,
    offlineQueue: List<OfflineSyncQueueEntity>,
    onNavigate: (MainDestination) -> Unit,
    onQuickAskAi: (String) -> Unit,
    onLogAttendanceFromPrompt: (SubjectEntity, String, String) -> Unit,
    onFireOsAttendanceNotification: (SubjectAttendanceAnalysis) -> Unit,
    onSimulateWhatsappShare: (String, String) -> Unit,
    onTriggerClassroomSync: (Boolean) -> Unit,
    onUpdateGmailRule: (Long, String) -> Unit,
    onParseCalendarSample: (String) -> Unit,
    onUpdateCalendarDraftTitle: (Int, String) -> Unit,
    onRemoveCalendarDraft: (Int) -> Unit,
    onConfirmCalendarDrafts: () -> Unit,
    onSyncOfflineQueue: (Boolean) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var quickPrompt by remember { mutableStateOf("") }
    var expandBreakdown by remember { mutableStateOf(false) }
    var activeSecondarySheet by remember { mutableStateOf(HomeSecondarySheet.NONE) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val includedAnalyses = subjectAnalyses.filter { !it.subject.isOptionalElective }
    val totalSafeBunks = includedAnalyses.sumOf { it.safeToSkipClasses }
    val totalMustAttend = includedAnalyses.sumOf { it.mustAttendClasses }
    val aggregateAttended = includedAnalyses.sumOf { it.effectiveAttended }
    val aggregateTotal = includedAnalyses.sumOf { it.effectiveTotal }.coerceAtLeast(1)
    val aggregatePct = ((aggregateAttended.toDouble() / aggregateTotal.toDouble()) * 100.0).toInt()

    val primaryLiveAnalysis = includedAnalyses.firstOrNull() ?: subjectAnalyses.firstOrNull()
    val pendingSyncCount = offlineQueue.count { it.status != "COMPLETED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // =========================================================================
        // PRIMARY CARD #1: Ambient Fluid Mesh Hero + Progressive Disclosure Bunk Metric
        // (Follows the 70% Whitespace / Max 2 Primary Cards Rule)
        // =========================================================================
        item {
            AmbientFluidMeshHeaderBox(
                isActiveGlowingBorder = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("liquid_hero_bunk_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WEEKLY BUNK HORIZON • ${userProfile.workingDaysMode.replace('_', ' ')}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoogleBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (totalSafeBunks > 0) {
                                    "You can safely bunk $totalSafeBunks more lectures this week"
                                } else {
                                    "Attend the next $totalMustAttend classes straight to recover"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Huge Bebas Neue Aggregate Percentage
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$aggregatePct%",
                                fontFamily = BebasNeueFontFamily,
                                style = MaterialTheme.typography.displayLarge,
                                color = when {
                                    aggregatePct >= 80 -> GoogleGreen
                                    aggregatePct >= 75 -> GoogleYellow
                                    else -> GoogleRed
                                }
                            )
                            Text(
                                text = "+$totalSafeBunks BUNKS LEFT",
                                fontFamily = BebasNeueFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = GoogleGreen
                            )
                        }
                    }

                    // Progressive Disclosure Toggle Pill
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clickable { expandBreakdown = !expandBreakdown }
                            .testTag("progressive_disclosure_bunk_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expandBreakdown) {
                                    "Hide subject-by-subject breakdown"
                                } else {
                                    "Tap to expand subject-by-subject breakdown (${subjectAnalyses.size})"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = GoogleBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (expandBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progressive Disclosure Subject Breakdown
                    AnimatedVisibility(visible = expandBreakdown) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            subjectAnalyses.forEach { analysis ->
                                val tone = when (analysis.zone) {
                                    AttendanceZone.SAFE_GREEN -> GoogleGreen
                                    AttendanceZone.WARNING_YELLOW -> GoogleYellow
                                    AttendanceZone.DANGER_RED -> GoogleRed
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .clickable { onNavigate(MainDestination.ATTENDANCE) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${analysis.subject.code} • ${analysis.subject.name}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = analysis.liquidPillCopy,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tone
                                        )
                                    }
                                    Text(
                                        text = "${analysis.roundedPercentage.toInt()}%",
                                        fontFamily = BebasNeueFontFamily,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = tone
                                    )
                                }
                            }
                        }
                    }

                    // Conversational AI / Scanner Input Bar
                    OutlinedTextField(
                        value = quickPrompt,
                        onValueChange = { quickPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_quick_ai_input"),
                        placeholder = {
                            Text("Ask AI Buddy to explain a derivation or generate 5 viva Qs...")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onNavigate(MainDestination.SCANNER) },
                                    modifier = Modifier.testTag("home_inline_scanner_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Open Scanner",
                                        tint = GoogleBlue
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val q = quickPrompt.ifBlank { "Explain Carnot Cycle from my notes" }
                                        onQuickAskAi(q)
                                        onNavigate(MainDestination.AI_BUDDY)
                                    },
                                    modifier = Modifier.testTag("home_quick_ai_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Ask AI Buddy",
                                        tint = GoogleBlue
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // =========================================================================
        // PRIMARY CARD #2: Time-Triggered Interactive Attendance Prompt Card
        // (Zero-App-Open Push Notification Trigger + Direct 1-Tap Action Buttons)
        // =========================================================================
        if (primaryLiveAnalysis != null) {
            item {
                val subj = primaryLiveAnalysis.subject
                val slot = timetableSlots.find { it.subjectId == subj.id } ?: timetableSlots.firstOrNull()

                LiquidGlassCard(
                    isGlowingBorder = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_class_notification_card")
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GoogleBlue.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Live Class Prompt",
                                        tint = GoogleBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "LIVE TIMETABLE TRIGGER • ${slot?.room ?: "LH-204"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoogleYellow
                                    )
                                    Text(
                                        text = "${subj.name} (${subj.code}) is going on",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            AssistChip(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onFireOsAttendanceNotification(primaryLiveAnalysis)
                                },
                                label = { Text("Push to OS Tray") },
                                modifier = Modifier.testTag("fire_os_notification_button")
                            )
                        }

                        Text(
                            text = "Should I mark your attendance for this class? (Current: ${primaryLiveAnalysis.roundedPercentage.toInt()}% • Safe to bunk: ${primaryLiveAnalysis.safeToSkipClasses})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // The 3 Exact Interactive Notification Action Buttons from Spec 3.B
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    onLogAttendanceFromPrompt(subj, "NOTIFICATION_PROMPT", "ATTENDED")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoogleGreen.copy(alpha = 0.22f),
                                    contentColor = GoogleGreen
                                ),
                                modifier = Modifier.testTag("notif_action_yes_attending")
                            ) {
                                Text("Yes, Attending", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    onLogAttendanceFromPrompt(subj, "NOTIFICATION_PROMPT", "MISSED")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoogleRed.copy(alpha = 0.22f),
                                    contentColor = GoogleRed
                                ),
                                modifier = Modifier.testTag("notif_action_no_bunking")
                            ) {
                                Text("No, Bunking", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onLogAttendanceFromPrompt(subj, "NOTIFICATION_PROMPT", "CLASS_CANCELLED")
                                },
                                modifier = Modifier.testTag("notif_action_class_cancelled")
                            ) {
                                Text("Class Cancelled")
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SWIPEABLE SECONDARY TOOL PILLS (Opens Smooth Bottom Sheets on Demand)
        // =========================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "QUICK WORKFLOW SHEETS",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = activeSecondarySheet == HomeSecondarySheet.OMNICHANNEL_INGESTION,
                        onClick = { activeSecondarySheet = HomeSecondarySheet.OMNICHANNEL_INGESTION },
                        label = { Text("Omnichannel Sync (WhatsApp / Classroom / Gmail)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoogleBlue
                            )
                        },
                        modifier = Modifier.testTag("open_omnichannel_sheet_chip")
                    )

                    FilterChip(
                        selected = activeSecondarySheet == HomeSecondarySheet.ACADEMIC_CALENDAR,
                        onClick = { activeSecondarySheet = HomeSecondarySheet.ACADEMIC_CALENDAR },
                        label = { Text("Calendar & Exam Deadlines (${calendarEvents.size})") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoogleYellow
                            )
                        },
                        modifier = Modifier.testTag("open_calendar_sheet_chip")
                    )

                    FilterChip(
                        selected = activeSecondarySheet == HomeSecondarySheet.OFFLINE_QUEUE,
                        onClick = { activeSecondarySheet = HomeSecondarySheet.OFFLINE_QUEUE },
                        label = { Text("Offline Queue ($pendingSyncCount)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (pendingSyncCount > 0) GoogleYellow else GoogleGreen
                            )
                        },
                        modifier = Modifier.testTag("open_offline_queue_sheet_chip")
                    )
                }
            }
        }
    }

    // =========================================================================
    // SECONDARY BOTTOM SHEETS (Decluttered Progressive Disclosure)
    // =========================================================================
    if (activeSecondarySheet != HomeSecondarySheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSecondarySheet = HomeSecondarySheet.NONE },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeSecondarySheet) {
                    HomeSecondarySheet.OMNICHANNEL_INGESTION -> {
                        Text(
                            text = "Module A: Omnichannel Ingestion Engine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // 1. WhatsApp Share Target
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "1. WhatsApp & Native File Manager Share Target",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zero background chat reading. Share PDFs or images directly from WhatsApp into EngBuddy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        onSimulateWhatsappShare(
                                            "WhatsApp_Thermo_SteamTables_Derivations.pdf",
                                            "application/pdf"
                                        )
                                    },
                                    modifier = Modifier.testTag("test_whatsapp_share_button")
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Receive Shared WhatsApp PDF into Vault")
                                }
                            }
                        }

                        // 2. Google Classroom OAuth + Institution Block Fallback
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "2. Google Classroom Sync & .edu Block Fallback",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onTriggerClassroomSync(false) },
                                        modifier = Modifier.testTag("classroom_sync_button")
                                    ) {
                                        Text("Sync Coursework")
                                    }
                                    OutlinedButton(
                                        onClick = { onTriggerClassroomSync(true) },
                                        modifier = Modifier.testTag("classroom_simulate_block_button")
                                    ) {
                                        Text("Test .edu Block (403)")
                                    }
                                }
                                if (userProfile.classroomInstitutionBlocked) {
                                    Surface(
                                        color = GoogleRed.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Institution Blocked Classroom API — Forwarding Fallback Active",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = GoogleRed
                                                )
                                                Text(
                                                    text = userProfile.forwardingInboxAddress,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = GoogleBlue
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(userProfile.forwardingInboxAddress))
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy forwarding address")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Gmail Academic Filter
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "3. Gmail Allow-List & Sender Approval",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                gmailRules.forEach { rule ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rule.senderAddressOrDomain,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GoogleBlue
                                            )
                                            Text(
                                                text = "${rule.sampleSubject} (${rule.ruleType})",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (rule.ruleType != "ALLOW") {
                                                AssistChip(
                                                    onClick = { onUpdateGmailRule(rule.id, "ALLOW") },
                                                    label = { Text("Allow") }
                                                )
                                            }
                                            if (rule.ruleType != "NEVER_IMPORT") {
                                                AssistChip(
                                                    onClick = { onUpdateGmailRule(rule.id, "NEVER_IMPORT") },
                                                    label = { Text("Block") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeSecondarySheet.OFFLINE_QUEUE -> {
                        Text(
                            text = "Offline Persistent Sync Queue (SQLite)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sync on Wi-Fi Only", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = userProfile.wifiOnlySync,
                                onCheckedChange = onToggleWifiOnly,
                                modifier = Modifier.testTag("wifi_only_sync_switch")
                            )
                        }
                        offlineQueue.take(5).forEach { task ->
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            task.payloadSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = task.status,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (task.status == "COMPLETED") GoogleGreen else GoogleYellow
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onSyncOfflineQueue(false) },
                                modifier = Modifier.testTag("sync_queue_now_button")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Flush Queue Now")
                            }
                            OutlinedButton(
                                onClick = { onSyncOfflineQueue(true) },
                                modifier = Modifier.testTag("simulate_backoff_button")
                            ) {
                                Text("Simulate Offline Backoff")
                            }
                        }
                    }

                    HomeSecondarySheet.ACADEMIC_CALENDAR -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Academic Calendar & Reminders",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { onParseCalendarSample("Odd_Semester_Calendar_2026.pdf") },
                                modifier = Modifier.testTag("parse_calendar_pdf_button")
                            ) {
                                Text("Parse Syllabus PDF")
                            }
                        }

                        if (calendarDrafts.isNotEmpty()) {
                            LiquidGlassCard(
                                accentTint = GoogleYellow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Confirm & Edit Parsed Events Before Saving",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GoogleYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                    calendarDrafts.forEach { draft ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = draft.title,
                                                onValueChange = { onUpdateCalendarDraftTitle(draft.id, it) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                label = { Text("${draft.eventType} • ${draft.dateLabel}") }
                                            )
                                            IconButton(onClick = { onRemoveCalendarDraft(draft.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GoogleRed)
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = onConfirmCalendarDrafts,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoogleGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("confirm_save_calendar_button")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Confirm & Schedule Reminders")
                                    }
                                }
                            }
                        }

                        calendarEvents.forEach { ev ->
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${ev.subjectName} • ${ev.dateLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = ev.eventType,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoogleBlue
                                    )
                                }
                            }
                        }
                    }

                    HomeSecondarySheet.NONE -> Unit
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
