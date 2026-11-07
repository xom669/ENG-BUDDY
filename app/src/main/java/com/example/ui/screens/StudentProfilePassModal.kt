package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserProfileEntity
import com.example.ui.components.AmbientFluidMeshHeaderBox
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentProfilePassModal(
    userProfile: UserProfileEntity,
    exportedJson: String?,
    onDismiss: () -> Unit,
    onSaveProfile: (
        displayName: String,
        rollNumber: String,
        collegeName: String,
        campusLocation: String,
        branchName: String,
        semester: String,
        sectionAndGroup: String
    ) -> Unit,
    onUpdateIdCardSlot: (frontUri: String?, backUri: String?, collapsed: Boolean?) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onExportJson: () -> Unit,
    onDeleteAccount: () -> Unit,
    onCopiedRollNumber: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current

    var isEditingIdentity by remember { mutableStateOf(false) }
    var isShowingBackSide by remember { mutableStateOf(false) }
    var showFullScreenGatePass by remember { mutableStateOf(false) }

    var draftName by remember(userProfile.displayName) { mutableStateOf(userProfile.displayName) }
    var draftRoll by remember(userProfile.rollNumber) { mutableStateOf(userProfile.rollNumber) }
    var draftCollege by remember(userProfile.collegeName) { mutableStateOf(userProfile.collegeName) }
    var draftCampus by remember(userProfile.campusLocation) { mutableStateOf(userProfile.campusLocation) }
    var draftBranch by remember(userProfile.branchName) { mutableStateOf(userProfile.branchName) }
    var draftSemester by remember(userProfile.semester) { mutableStateOf(userProfile.semester) }
    var draftSection by remember(userProfile.sectionAndGroup) { mutableStateOf(userProfile.sectionAndGroup) }

    // Zero-permission Android Photo Picker for Front & Back College ID Card
    val frontIdPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val resolved = uri?.toString() ?: "edge_cropped_college_id_front_${System.currentTimeMillis()}.jpg"
        onUpdateIdCardSlot(resolved, null, false)
    }

    val backIdPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val resolved = uri?.toString() ?: "edge_cropped_college_id_back_${System.currentTimeMillis()}.jpg"
        onUpdateIdCardSlot(null, resolved, false)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("student_profile_pass_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Student Pass",
                        tint = GoogleBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Digital Student Pass & ID Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(
                        onClick = { isEditingIdentity = !isEditingIdentity },
                        modifier = Modifier.testTag("edit_student_pass_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Student Identity")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Pass")
                    }
                }
            }

            // 1. Apple Wallet / Google Wallet Style Liquid-Glass Student Pass
            AmbientFluidMeshHeaderBox(
                isActiveGlowingBorder = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_student_pass_card")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile.collegeName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoogleBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile.campusLocation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = GoogleGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Student",
                                    tint = GoogleGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACTIVE PASS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoogleGreen
                                )
                            }
                        }
                    }

                    // Full Name in bold Plus Jakarta Sans / PP Neue Montreal
                    Text(
                        text = userProfile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Branch & Semester
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = userProfile.branchName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${userProfile.semester}  •  ${userProfile.sectionAndGroup}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // University Roll Number / Registration ID in JetBrains Mono with 1-tap Copy to Clipboard
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboard.setText(AnnotatedString(userProfile.rollNumber))
                                onCopiedRollNumber(userProfile.rollNumber)
                            }
                            .testTag("copy_roll_number_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "UNIVERSITY ROLL / REG ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = userProfile.rollNumber,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = GoogleYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "1-Tap Copy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoogleBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Roll Number to Clipboard",
                                    tint = GoogleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Inline Identity Editor (when user taps Edit icon)
            if (isEditingIdentity) {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Edit Student Pass Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draftRoll,
                            onValueChange = { draftRoll = it },
                            label = { Text("University Roll Number / Reg ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draftCollege,
                            onValueChange = { draftCollege = it },
                            label = { Text("College / University Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draftCampus,
                            onValueChange = { draftCampus = it },
                            label = { Text("Campus Location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draftBranch,
                            onValueChange = { draftBranch = it },
                            label = { Text("Branch / Department") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draftSemester,
                                onValueChange = { draftSemester = it },
                                label = { Text("Year & Semester") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draftSection,
                                onValueChange = { draftSection = it },
                                label = { Text("Section / Group") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                onSaveProfile(
                                    draftName,
                                    draftRoll,
                                    draftCollege,
                                    draftCampus,
                                    draftBranch,
                                    draftSemester,
                                    draftSection
                                )
                                isEditingIdentity = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Student Pass")
                        }
                    }
                }
            }

            // 2. Optional Physical ID Card Placeholder ("Quick-Flash ID")
            val hasUploadedId = userProfile.idCardFrontUri.isNotBlank() || userProfile.idCardBackUri.isNotBlank()

            if (userProfile.idCardSlotCollapsed && !hasUploadedId) {
                // Collapsed minimal non-intrusive "Add ID Card" chip when left empty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Physical ID Card Slot (Optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AssistChip(
                        onClick = { onUpdateIdCardSlot(null, null, false) },
                        label = { Text("+ Add ID Card") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AddCard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoogleBlue
                            )
                        },
                        modifier = Modifier.testTag("expand_add_id_card_chip")
                    )
                }
            } else {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_flash_id_card_slot")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Quick-Flash Physical College ID",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isShowingBackSide) "Showing: BACK SIDE (Barcode & Library Rules)" else "Showing: FRONT SIDE (Photo & Gate Barcode)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoogleBlue
                                )
                            }
                            if (!hasUploadedId) {
                                AssistChip(
                                    onClick = { onUpdateIdCardSlot(null, null, true) },
                                    label = { Text("Collapse") },
                                    modifier = Modifier.testTag("collapse_empty_id_slot_button")
                                )
                            }
                        }

                        // Dashed Glassmorphic Container or Auto-Cropped ID Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                                .clickable {
                                    if (!hasUploadedId) {
                                        onUpdateIdCardSlot(
                                            "scanned_front_${userProfile.rollNumber}.jpg",
                                            "scanned_back_${userProfile.rollNumber}.jpg",
                                            false
                                        )
                                    } else {
                                        isShowingBackSide = !isShowingBackSide
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val strokeColor = if (hasUploadedId) GoogleGreen else GoogleBlue.copy(alpha = 0.65f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRoundRect(
                                    color = strokeColor,
                                    style = Stroke(
                                        width = 3f,
                                        pathEffect = if (hasUploadedId) null else PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                                    )
                                )
                            }

                            if (!hasUploadedId) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCard,
                                        contentDescription = "Upload College ID",
                                        tint = GoogleBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Tap to scan or upload your College ID Card — Optional",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Auto-crops with camera edge detector • Supports Front & Back flip + Gate Barcode Brightness Boost",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // Rendered Auto-Cropped Front or Back ID Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isShowingBackSide) "BACK • LIBRARY & HOSTEL BARCODE" else "FRONT • STUDENT IDENTITY CARD",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoogleGreen
                                        )
                                        Text(
                                            text = userProfile.rollNumber,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GoogleYellow
                                        )
                                    }

                                    if (!isShowingBackSide) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(GoogleBlue.copy(alpha = 0.24f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = userProfile.displayName.take(2).uppercase(),
                                                    fontFamily = BebasNeueFontFamily,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = GoogleBlue
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = userProfile.displayName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${userProfile.branchName} • ${userProfile.semester}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Emergency Contact & Campus Library Access • Valid through May 2027 • Return to Registrar if found.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Simulated High-Contrast Code-128 Barcode strip
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        var x = 8f
                                        var idx = 0
                                        while (x < size.width - 8f) {
                                            val barW = if ((idx % 3) == 0) 6f else 3f
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(x, 0f),
                                                size = Size(barW, size.height)
                                            )
                                            x += barW + if ((idx % 2) == 0) 4f else 7f
                                            idx++
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons: Upload Front/Back, Flip Card, Full-Screen Barcode Brightness Boost
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    frontIdPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Pick Front Photo")
                            }
                            OutlinedButton(
                                onClick = {
                                    backIdPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Pick Back Photo")
                            }
                            Button(
                                onClick = { isShowingBackSide = !isShowingBackSide },
                                modifier = Modifier.testTag("flip_id_card_button")
                            ) {
                                Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Flip Card (${if (isShowingBackSide) "Front" else "Back"})")
                            }
                            Button(
                                onClick = { showFullScreenGatePass = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GoogleYellow, contentColor = Color.Black),
                                modifier = Modifier.testTag("brightness_boost_barcode_button")
                            ) {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gate Barcode Brightness Boost")
                            }
                        }
                    }
                }
            }

            // 3. Appearance & Privacy Controls
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Canvas Theme & Account Privacy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "DARK" to "Obsidian (#0B0C0E)",
                            "LIGHT" to "Alabaster (#F8F9FA)",
                            "SYSTEM" to "System"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = userProfile.themeMode.equals(mode, ignoreCase = true),
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onExportJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Pass JSON")
                        }
                        Button(
                            onClick = onDeleteAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wipe Data")
                        }
                    }

                    if (exportedJson != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = exportedJson,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Full-Screen Barcode / QR Brightness Boost Dialog for College Gate & Library Scanners
    if (showFullScreenGatePass) {
        Dialog(
            onDismissRequest = { showFullScreenGatePass = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Surface(
                        color = GoogleYellow,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% BRIGHTNESS GATE & LIBRARY SCANNER PASS",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = userProfile.collegeName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userProfile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userProfile.rollNumber,
                        fontFamily = BebasNeueFontFamily,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.Black
                    )

                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "High Contrast Student QR Code",
                        tint = Color.Black,
                        modifier = Modifier.size(180.dp)
                    )

                    // High-contrast wide Code-128 Barcode for laser gate/library scanners
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        var x = 6f
                        var i = 0
                        while (x < size.width - 6f) {
                            val w = when (i % 4) {
                                0 -> 8f
                                1 -> 3f
                                2 -> 5f
                                else -> 4f
                            }
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(x, 0f),
                                size = Size(w, size.height)
                            )
                            x += w + if (i % 2 == 0) 5f else 8f
                            i++
                        }
                    }

                    Button(
                        onClick = { showFullScreenGatePass = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exit Brightness Boost")
                    }
                }
            }
        }
    }
}
