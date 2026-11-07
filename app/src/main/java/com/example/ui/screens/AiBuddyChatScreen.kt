package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.local.SubjectEntity
import com.example.data.remote.AiExplainerResponse
import com.example.data.remote.ExplainMode
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.geminiAuroraBrush

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiBuddyChatScreen(
    subjects: List<SubjectEntity>,
    aiResponse: AiExplainerResponse?,
    isAiLoading: Boolean,
    firebaseNotice: String,
    onRunExplain: (query: String, mode: ExplainMode, subject: SubjectEntity?) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ExplainMode.ELI5) }
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var customQuery by remember {
        mutableStateOf("Explain Carnot Cycle efficiency & Clausius inequality from my scanned notes")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_buddy_chat_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "AI Notes Explainer & Exam Crammer",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Grounded strictly in your scanned Vault notes via RAG • Every answer cites the source page",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mandatory In-App Notice when Firebase Setup was Declined
        item {
            Surface(
                color = StatusWarningAmber.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firebase_required_notice_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Firebase AI Notice",
                        tint = StatusWarningAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = firebaseNotice,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Mode Selector Pills: ELI5, 5 Viva Questions, 1-Page Cheat Sheet, Custom
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
                        text = "1. Select Exam Crammer Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExplainMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = {
                                    selectedMode = mode
                                    onRunExplain(customQuery, mode, selectedSubject)
                                },
                                label = { Text(mode.label) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = GeminiBlue
                                    )
                                },
                                modifier = Modifier.testTag("explain_mode_chip_${mode.name}")
                            )
                        }
                    }

                    Text(
                        text = "2. Filter by Subject Context (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubject == null,
                            onClick = {
                                selectedSubject = null
                                onRunExplain(customQuery, selectedMode, null)
                            },
                            label = { Text("All Vault Notes") }
                        )
                        subjects.forEach { s ->
                            FilterChip(
                                selected = selectedSubject?.id == s.id,
                                onClick = {
                                    selectedSubject = s
                                    onRunExplain(customQuery, selectedMode, s)
                                },
                                label = { Text(s.code) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customQuery,
                        onValueChange = { customQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_buddy_query_input"),
                        label = { Text("Highlight topic, derivation, or circuit diagram") },
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onRunExplain(customQuery, selectedMode, selectedSubject)
                                },
                                modifier = Modifier.testTag("ai_buddy_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Generate Explanation",
                                    tint = GeminiBlue
                                )
                            }
                        }
                    )
                }
            }
        }

        if (isAiLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GeminiBlue)
                }
            }
        }

        // Grounded RAG Answer + Verified Page Citations
        if (aiResponse != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, geminiAuroraBrush(), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                        .testTag("ai_buddy_answer_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = GeminiBlue.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = aiResponse.mode.badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GeminiBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = if (aiResponse.cachedHit) {
                                    "CACHE HIT (0 Tokens) • ${aiResponse.modelLabel}"
                                } else {
                                    aiResponse.modelLabel
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (aiResponse.cachedHit) StatusSafeGreen else GeminiPurple
                            )
                        }

                        Text(
                            text = aiResponse.markdownAnswer,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Grounded Page Citations Section
                        if (aiResponse.citations.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = "Citations",
                                        tint = GeminiPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Verified Citations from Your Handwritten Notes (${aiResponse.citations.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GeminiPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                aiResponse.citations.forEach { cite ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Doc #${cite.documentId} • Page ${cite.pageNumber} — ${cite.heading}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GeminiBlue
                                            )
                                            Text(
                                                text = "\"${cite.snippet}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
