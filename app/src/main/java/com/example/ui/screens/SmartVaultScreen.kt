package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentPageEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.VaultDocumentEntity
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDangerRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartVaultScreen(
    subjects: List<SubjectEntity>,
    activeDocuments: List<VaultDocumentEntity>,
    trashedDocuments: List<VaultDocumentEntity>,
    allPages: List<DocumentPageEntity>,
    onExplainDocumentInAiBuddy: (VaultDocumentEntity) -> Unit,
    onSoftDeleteToTrash: (Long) -> Unit,
    onRestoreFromTrash: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf<Long?>(null) }
    var showTrashBin by remember { mutableStateOf(false) }
    var expandedDocId by remember { mutableStateOf<Long?>(activeDocuments.firstOrNull()?.id) }

    val filteredDocs = remember(activeDocuments, searchQuery, selectedSubjectFilter) {
        activeDocuments.filter { doc ->
            val matchesSubject = selectedSubjectFilter == null || doc.subjectId == selectedSubjectFilter
            val matchesSearch = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.chapterTitle.contains(searchQuery, ignoreCase = true) ||
                doc.summaryText.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_vault_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Vault & Stitched PDFs",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${activeDocuments.size} organized PDFs • ${trashedDocuments.size} in 30-day Trash Bin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilterChip(
                    selected = showTrashBin,
                    onClick = { showTrashBin = !showTrashBin },
                    label = { Text("30-Day Trash (${trashedDocuments.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Trash Bin",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("vault_toggle_trash_chip")
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_input"),
                placeholder = { Text("Search handwritten OCR, subjects, chapters, or formulas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Subject Folder Filter Chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSubjectFilter == null,
                    onClick = { selectedSubjectFilter = null },
                    label = { Text("All Subjects") }
                )
                subjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubjectFilter == subject.id,
                        onClick = {
                            selectedSubjectFilter = if (selectedSubjectFilter == subject.id) null else subject.id
                        },
                        label = { Text(subject.code) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GeminiBlue
                            )
                        }
                    )
                }
            }
        }

        // 30-Day Soft-Delete Trash Bin View (Edge Case #2: One-Tap Restore)
        if (showTrashBin) {
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
                            text = "30-Day Soft-Delete Trash Bin",
                            style = MaterialTheme.typography.titleMedium,
                            color = StatusWarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lower-sharpness duplicate scans and deleted notes are never hard-deleted immediately. Tap Restore to recover any item.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (trashedDocuments.isEmpty()) {
                            Text(
                                text = "Trash Bin is empty.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            trashedDocuments.forEach { trashed ->
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
                                                text = trashed.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${trashed.subjectName} • ${trashed.summaryText}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { onRestoreFromTrash(trashed.id) },
                                            modifier = Modifier.testTag("restore_trash_btn_${trashed.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.RestoreFromTrash,
                                                contentDescription = "Restore",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Stitched PDF Documents
        items(filteredDocs, key = { it.id }) { doc ->
            val docPages = allPages.filter { it.documentId == doc.id }
            val isExpanded = expandedDocId == doc.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDocId = if (isExpanded) null else doc.id }
                    .testTag("vault_doc_card_${doc.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GeminiBlue.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Stitched PDF",
                                    tint = GeminiBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${doc.subjectName} • ${doc.chapterTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GeminiBlue
                                )
                                Text(
                                    text = "Source: ${doc.sourceType} • ${doc.pageCount} pages • Order Confidence: ${(doc.orderingConfidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onSoftDeleteToTrash(doc.id) },
                            modifier = Modifier.testTag("soft_delete_doc_${doc.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Move to 30-day Trash",
                                tint = StatusDangerRed
                            )
                        }
                    }

                    Text(
                        text = doc.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isExpanded && docPages.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Indexed RAG Pages & Vision Hashes (${docPages.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = GeminiPurple
                            )
                            docPages.forEach { p ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Page ${p.pageNumber}: ${p.detectedHeading}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (p.duplicateFlag == "POSSIBLE_DUPLICATE") {
                                            Text(
                                                text = "POSSIBLE DUPLICATE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StatusWarningAmber
                                            )
                                        }
                                    }
                                    Text(
                                        text = p.ocrText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Laplacian=${p.laplacianSharpness} | pHash=${p.phash} | SSIM=${p.ssimScore}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { onExplainDocumentInAiBuddy(doc) },
                            label = { Text("Explain / Generate Viva Qs in AI Buddy") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GeminiBlue
                                )
                            },
                            modifier = Modifier.testTag("explain_doc_btn_${doc.id}")
                        )
                        AssistChip(
                            onClick = { expandedDocId = if (isExpanded) null else doc.id },
                            label = { Text(if (isExpanded) "Hide Pages" else "Inspect ${doc.pageCount} Pages") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
