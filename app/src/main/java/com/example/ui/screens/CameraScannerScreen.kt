package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.SubjectEntity
import com.example.data.local.SubjectOverlapPrefEntity
import com.example.data.remote.BatchClassificationResult
import com.example.data.remote.ScannedPageDraft
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCoral
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.geminiAuroraBrush

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraScannerScreen(
    scannedDrafts: List<ScannedPageDraft>,
    batchClassification: BatchClassificationResult?,
    scannerStatusBanner: String?,
    simulateTimetableTuesdayThermo: Boolean,
    overlapPrefs: List<SubjectOverlapPrefEntity>,
    onToggleTimetableSimulation: (Boolean) -> Unit,
    onRunBatchScan: (includeUnnumberedMessyPages: Boolean, includeFourierOverlap: Boolean) -> Unit,
    onMovePageOrder: (fromIndex: Int, toIndex: Int) -> Unit,
    onResolveOverlapWithOneTap: (SubjectEntity, Boolean) -> Unit,
    onCommitPdfToVault: (String) -> Unit,
    onCaptureLiveBitmap: ((android.graphics.Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onCaptureLiveBitmap?.invoke(bitmap)
        }
    }

    var messyUnnumberedMode by remember { mutableStateOf(true) }
    var fourierOverlapMode by remember { mutableStateOf(false) }
    var rememberOverlapChoice by remember { mutableStateOf(true) }
    var customPdfTitle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("camera_scanner_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Intelligent Camera & Note Pipeline",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "On-device Laplacian blur + pHash/dHash + SSIM dedup • Timetable-aware filing • PDF Stitcher",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Live Edge-Detection Viewfinder & Perspective Crop Preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0B0C0E))
                            .border(1.dp, GeminiBlue.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Draw auto-edge detection polygon & notebook preview lines on Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val poly = Path().apply {
                                moveTo(w * 0.14f, h * 0.16f)
                                lineTo(w * 0.86f, h * 0.12f)
                                lineTo(w * 0.90f, h * 0.86f)
                                lineTo(w * 0.10f, h * 0.84f)
                                close()
                            }
                            drawPath(
                                path = poly,
                                color = Color(0xFF4B90FF),
                                style = Stroke(width = 4f)
                            )
                            // Corner handles
                            val corners = listOf(
                                Offset(w * 0.14f, h * 0.16f),
                                Offset(w * 0.86f, h * 0.12f),
                                Offset(w * 0.90f, h * 0.86f),
                                Offset(w * 0.10f, h * 0.84f)
                            )
                            corners.forEach { pt ->
                                drawCircle(color = Color(0xFFFF5546), radius = 9f, center = pt)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Auto Edge Detection",
                                tint = GeminiBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Auto-Edge Detection & Perspective Quadrilateral Locked",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = if (hasCameraPermission) {
                                    "Camera Permission Granted • On-Device Vision Kernels Ready"
                                } else {
                                    "Optional Hardware Camera Permission Available"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA8C7FA)
                            )
                        }
                    }

                    // Pipeline Configuration Toggles (Edge Cases #1, #2, #3, #8)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = messyUnnumberedMode,
                            onClick = { messyUnnumberedMode = !messyUnnumberedMode },
                            label = { Text("Messy / Unnumbered Pages (Drag-Drop Preview)") },
                            modifier = Modifier.testTag("toggle_messy_pages_chip")
                        )
                        FilterChip(
                            selected = fourierOverlapMode,
                            onClick = { fourierOverlapMode = !fourierOverlapMode },
                            label = { Text("Subject Overlap: Fourier (Math vs Signals)") },
                            modifier = Modifier.testTag("toggle_fourier_overlap_chip")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = GeminiPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Timetable-Aware Filing Prior (Tue 10:15 AM)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Defaults to Thermodynamics unless page content overrides",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = simulateTimetableTuesdayThermo,
                            onCheckedChange = onToggleTimetableSimulation,
                            modifier = Modifier.testTag("timetable_prior_switch")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    takePictureLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_camera_real_page_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snap Live Note")
                        }

                        OutlinedButton(
                            onClick = {
                                onRunBatchScan(messyUnnumberedMode, fourierOverlapMode)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_batch_execute_button")
                        ) {
                            Text("Simulate 4-Page Batch")
                        }
                    }
                }
            }
        }

        // 2. Status Banner & Confidence Indicator
        if (scannerStatusBanner != null) {
            item {
                Surface(
                    color = if (batchClassification?.needsManualOrderReview == true) {
                        StatusWarningAmber.copy(alpha = 0.16f)
                    } else {
                        StatusSafeGreen.copy(alpha = 0.16f)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (batchClassification?.needsManualOrderReview == true) {
                                Icons.Default.WarningAmber
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = if (batchClassification?.needsManualOrderReview == true) {
                                StatusWarningAmber
                            } else {
                                StatusSafeGreen
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = scannerStatusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Edge Case #8: Subject Overlap 1-Tap Disambiguation Card (when tiedSubject != null)
        val classification = batchClassification
        if (classification != null && classification.tiedSubject != null && classification.suggestedSubject != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GeminiCoral, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Subject Overlap Detected: ${classification.overlapTopicKeyword}",
                            style = MaterialTheme.typography.titleMedium,
                            color = GeminiCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Both '${classification.suggestedSubject.name}' and '${classification.tiedSubject.name}' cover this topic. Pick once with one tap:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberOverlapChoice,
                                onCheckedChange = { rememberOverlapChoice = it }
                            )
                            Text(
                                text = "Remember my choice for '${classification.overlapTopicKeyword}'",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onResolveOverlapWithOneTap(
                                        classification.suggestedSubject,
                                        rememberOverlapChoice
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(classification.suggestedSubject.code)
                            }
                            OutlinedButton(
                                onClick = {
                                    onResolveOverlapWithOneTap(
                                        classification.tiedSubject,
                                        rememberOverlapChoice
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(classification.tiedSubject.code)
                            }
                        }
                    }
                }
            }
        }

        // 4. On-Device Dedup & Blur Filter Results (Showing Auto-Trash Duplicate + Possible Duplicate Flag)
        if (scannedDrafts.isNotEmpty() && classification != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "1. On-Device Vision Filter (Laplacian + pHash/dHash + SSIM)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        scannedDrafts.forEach { draft ->
                            Surface(
                                color = if (draft.isSentToTrash) {
                                    StatusDangerRed.copy(alpha = 0.12f)
                                } else if (draft.isPossibleDuplicate) {
                                    StatusWarningAmber.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = draft.rawTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = when {
                                                draft.isSentToTrash -> "30-DAY TRASH"
                                                draft.isPossibleDuplicate -> "POSSIBLE DUP"
                                                else -> "SHARP KEPT"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                draft.isSentToTrash -> StatusDangerRed
                                                draft.isPossibleDuplicate -> StatusWarningAmber
                                                else -> StatusSafeGreen
                                            }
                                        )
                                    }
                                    Text(
                                        text = "Laplacian=${draft.laplacianSharpness} | pHash=${draft.phash} | SSIM=${draft.ssimWithPrevious}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = draft.duplicateDecisionLabel,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Edge Case #1: Reorder Preview (with Confidence Indicators) & Final PDF Stitcher
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Page Ordering Preview & Timetable Classification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Suggested Subject: ${classification.suggestedSubject?.code ?: "ME-301"} (${classification.suggestedSubject?.name ?: "Thermodynamics"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GeminiBlue,
                            fontWeight = FontWeight.Medium
                        )
                        if (classification.timetablePriorUsed != null) {
                            Text(
                                text = "Prior Info: ${classification.timetablePriorUsed}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GeminiPurple
                            )
                        }

                        classification.orderedPages.forEachIndexed { index, page ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
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
                                            text = "Final Page #${index + 1}: ${page.detectedHeading}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = page.ocrText,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { onMovePageOrder(index, index - 1) },
                                            enabled = index > 0,
                                            modifier = Modifier.testTag("move_page_up_$index")
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                        }
                                        IconButton(
                                            onClick = { onMovePageOrder(index, index + 1) },
                                            enabled = index < classification.orderedPages.lastIndex,
                                            modifier = Modifier.testTag("move_page_down_$index")
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customPdfTitle,
                            onValueChange = { customPdfTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Stitched PDF Filename (optional)") },
                            placeholder = {
                                Text("${classification.suggestedSubject?.code ?: "ME-301"} — ${classification.detectedChapterTitle}")
                            },
                            singleLine = true
                        )

                        Button(
                            onClick = { onCommitPdfToVault(customPdfTitle) },
                            colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("commit_stitched_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (classification.needsManualOrderReview) {
                                    "Confirm Page Order & Stitch Clean PDF (${classification.orderedPages.size}p)"
                                } else {
                                    "Auto-Stitch & Save Clean PDF to Smart Vault"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
