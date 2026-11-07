package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SubjectEntity
import com.example.data.local.TimetableSlotEntity
import com.example.domain.AttendanceZone
import com.example.domain.SubjectAttendanceAnalysis
import com.example.ui.components.AmbientFluidMeshHeaderBox
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceBunkScreen(
    workingDaysMode: String,
    timetableSlots: List<TimetableSlotEntity>,
    analyses: List<SubjectAttendanceAnalysis>,
    onSetWorkingDaysMode: (String) -> Unit,
    onLogAttendance: (SubjectEntity, sessionType: String, status: String) -> Unit,
    onFireInteractiveNotification: (SubjectAttendanceAnalysis) -> Unit,
    onIngestTimetableVisionSample: (SubjectEntity) -> Unit,
    onUpdateSubjectRules: (
        subject: SubjectEntity,
        targetPct: Double,
        labWeight: Int,
        medicalExcused: Boolean,
        roundingMode: String,
        isOptionalElective: Boolean,
        tutorialBonusPoints: Int
    ) -> Unit
) {
    var editingSubjectId by remember { mutableStateOf<Long?>(null) }
    var showTimetableIngestionCard by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_bunk_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Ambient Fluid Header: Timetable Setup + Working Days Selector
        item {
            AmbientFluidMeshHeaderBox(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMART BUNK ENGINE & TIMETABLE",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoogleBlue
                            )
                            Text(
                                text = "Zero-Effort Attendance",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { showTimetableIngestionCard = !showTimetableIngestionCard },
                            modifier = Modifier.testTag("toggle_timetable_setup_button")
                        ) {
                            Icon(Icons.Default.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showTimetableIngestionCard) "Hide Routine" else "Timetable (${timetableSlots.size})")
                        }
                    }

                    // Working Days Selector Pill: 5 Days [Mon-Fri], 6 Days [Mon-Sat], Custom
                    Text(
                        text = "College Working Days Schedule:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "5_DAYS" to "5 Days [Mon–Fri]",
                            "6_DAYS" to "6 Days [Mon–Sat]",
                            "CUSTOM" to "Custom Schedule"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = workingDaysMode == code,
                                onClick = { onSetWorkingDaysMode(code) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("working_days_chip_$code")
                            )
                        }
                    }

                    if (showTimetableIngestionCard) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Direct Timetable Upload (Gemini Vision Extraction)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Extracts Subject Name, Course Code, Start/End Time, Day of Week, and Class Type (Lecture vs Lab) to trigger interactive push notifications 10 mins into class.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            timetableSlots.forEach { slot ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${slot.courseCode} • ${slot.subjectName}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoogleBlue
                                    )
                                    Text(
                                        text = "Day ${slot.dayOfWeek} • ${slot.startTimeMinutes / 60}:00 (${if (slot.isLab) "Lab ${slot.customWeightPoints}x" else "Lec"})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            analyses.firstOrNull()?.subject?.let { firstSubj ->
                                OutlinedButton(
                                    onClick = { onIngestTimetableVisionSample(firstSubj) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("+ Scan / Extract Timetable Slot from Routine Photo")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Subject-by-Subject Liquid Glass Bunk Cards
        items(analyses, key = { it.subject.id }) { item ->
            val subj = item.subject
            val isEditingRules = editingSubjectId == subj.id
            val zoneColor = when (item.zone) {
                AttendanceZone.SAFE_GREEN -> GoogleGreen
                AttendanceZone.WARNING_YELLOW -> GoogleYellow
                AttendanceZone.DANGER_RED -> GoogleRed
            }

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_bunk_card_${subj.code}")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = GoogleBlue.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = subj.code,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoogleBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.statusBadge,
                                    fontFamily = BebasNeueFontFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = zoneColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subj.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.formulaExplanation,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Huge Bebas Neue Percentage + Rules Gear
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.roundedPercentage.toInt()}%",
                                fontFamily = BebasNeueFontFamily,
                                style = MaterialTheme.typography.displayMedium,
                                color = zoneColor
                            )
                            IconButton(
                                onClick = {
                                    editingSubjectId = if (isEditingRules) null else subj.id
                                },
                                modifier = Modifier.testTag("edit_rules_btn_${subj.code}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configure Subject Rules",
                                    tint = GoogleBlue
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (item.roundedPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = zoneColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Crystal-Clear Liquid Google Green / Red Pill (Spec Section 3.C)
                    Surface(
                        color = zoneColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.liquidPillCopy,
                                style = MaterialTheme.typography.bodyMedium,
                                color = zoneColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onFireInteractiveNotification(item) },
                                modifier = Modifier.testTag("notify_subject_${subj.code}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Trigger Interactive Push Notification",
                                    tint = zoneColor
                                )
                            }
                        }
                    }

                    // Direct Interactive Action Pills ([Yes, Attending], [No, Bunking], [Class Cancelled], [+Lab])
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { onLogAttendance(subj, "LECTURE", "ATTENDED") },
                            label = { Text("Yes, Attending") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoogleGreen
                                )
                            },
                            modifier = Modifier.testTag("attend_lecture_${subj.code}")
                        )
                        AssistChip(
                            onClick = { onLogAttendance(subj, "LECTURE", "MISSED") },
                            label = { Text("No, Bunking") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoogleRed
                                )
                            },
                            modifier = Modifier.testTag("bunk_lecture_${subj.code}")
                        )
                        AssistChip(
                            onClick = { onLogAttendance(subj, "LECTURE", "CLASS_CANCELLED") },
                            label = { Text("Cancelled (${subj.cancelledClasses})") },
                            modifier = Modifier.testTag("cancel_lecture_${subj.code}")
                        )
                        AssistChip(
                            onClick = { onLogAttendance(subj, "LAB", "ATTENDED") },
                            label = { Text("+Lab (${subj.labWeightClasses}x)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoogleBlue
                                )
                            },
                            modifier = Modifier.testTag("attend_lab_${subj.code}")
                        )
                    }

                    // Expandable Custom College Rule Engine (Threshold 60-90%, Lab Weight, Optional Elective, Tutorial Bonus)
                    if (isEditingRules) {
                        var draftTarget by remember(subj.id) { mutableDoubleStateOf(subj.targetPercentage) }
                        var draftLabWeight by remember(subj.id) { mutableIntStateOf(subj.labWeightClasses) }
                        var draftMedicalExcused by remember(subj.id) { mutableStateOf(subj.medicalLeaveExcused) }
                        var draftRounding by remember(subj.id) { mutableStateOf(subj.roundingMode) }
                        var draftOptionalElective by remember(subj.id) { mutableStateOf(subj.isOptionalElective) }
                        var draftTutorialBonus by remember(subj.id) { mutableIntStateOf(subj.tutorialBonusPoints) }

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Custom College Rules (${subj.code})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleBlue
                                )

                                Text(
                                    text = "Minimum Attendance Threshold: ${draftTarget.toInt()}% (60% – 90%)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = draftTarget.toFloat(),
                                    onValueChange = { draftTarget = it.toInt().toDouble() },
                                    valueRange = 60f..90f,
                                    steps = 5
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("3-Hour Lab Weight:", style = MaterialTheme.typography.bodySmall)
                                    listOf(1, 2, 3, 4).forEach { w ->
                                        FilterChip(
                                            selected = draftLabWeight == w,
                                            onClick = { draftLabWeight = w },
                                            label = { Text("${w}x pts") }
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Tutorial Bonus Pts:", style = MaterialTheme.typography.bodySmall)
                                    listOf(0, 1, 2, 3).forEach { bonus ->
                                        FilterChip(
                                            selected = draftTutorialBonus == bonus,
                                            onClick = { draftTutorialBonus = bonus },
                                            label = { Text("+$bonus") }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Optional Elective — Exclude from Aggregate",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(
                                        checked = draftOptionalElective,
                                        onCheckedChange = { draftOptionalElective = it }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Excuse Medical Leave",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(
                                        checked = draftMedicalExcused,
                                        onCheckedChange = { draftMedicalExcused = it }
                                    )
                                }

                                Button(
                                    onClick = {
                                        onUpdateSubjectRules(
                                            subj,
                                            draftTarget,
                                            draftLabWeight,
                                            draftMedicalExcused,
                                            draftRounding,
                                            draftOptionalElective,
                                            draftTutorialBonus
                                        )
                                        editingSubjectId = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Rules for ${subj.code}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
