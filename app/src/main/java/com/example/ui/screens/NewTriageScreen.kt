package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.TicketEntity
import java.io.File
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseCritical
import com.example.ui.viewmodel.TriageViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTriageScreen(
    viewModel: TriageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State from ViewModel
    val rawText by viewModel.rawInputText.collectAsStateWithLifecycle()
    val audioFile by viewModel.audioFile.collectAsStateWithLifecycle()
    val audioMimeType by viewModel.audioMimeType.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val hasActivePreview by viewModel.hasActivePreview.collectAsStateWithLifecycle()
    val similarTickets by viewModel.similarTickets.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    // Preview fields
    val previewId by viewModel.previewId.collectAsStateWithLifecycle()
    val previewTitulo by viewModel.previewTitulo.collectAsStateWithLifecycle()
    val previewSolicitanteNome by viewModel.previewSolicitanteNome.collectAsStateWithLifecycle()
    val previewSolicitanteInfo by viewModel.previewSolicitanteInfo.collectAsStateWithLifecycle()
    val previewCanal by viewModel.previewCanal.collectAsStateWithLifecycle()
    val previewProjeto by viewModel.previewProjeto.collectAsStateWithLifecycle()
    val previewServicoModulo by viewModel.previewServicoModulo.collectAsStateWithLifecycle()
    val previewTipoProblema by viewModel.previewTipoProblema.collectAsStateWithLifecycle()
    val previewPrioridade by viewModel.previewPrioridade.collectAsStateWithLifecycle()
    val previewJustificativaPrioridade by viewModel.previewJustificativaPrioridade.collectAsStateWithLifecycle()
    val previewDescricaoEstruturada by viewModel.previewDescricaoEstruturada.collectAsStateWithLifecycle()
    val previewSugestaoSolucao by viewModel.previewSugestaoSolucao.collectAsStateWithLifecycle()
    val previewTags by viewModel.previewTags.collectAsStateWithLifecycle()

    var inputTab by remember { mutableIntStateOf(0) } // 0: Texto, 1: Áudio Voz, 2: Upload

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        }
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAudioUriSelected(uri)
        }
    }

    // Image & Camera States
    val imageFile by viewModel.imageFile.collectAsStateWithLifecycle()
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var showFullscreenImageDialog by remember { mutableStateOf<File?>(null) }

    // Launcher for taking picture with Camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraPhotoTaken(success)
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = viewModel.createCameraCaptureUri()
            cameraCaptureUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    // Photo picker for modern Android gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelectedFromGallery(uri)
        }
    }

    // Fallback file picker for image files
    val fallbackImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelectedFromGallery(uri)
        }
    }

    fun launchCamera() {
        val uri = viewModel.createCameraCaptureUri()
        cameraCaptureUri = uri
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchImagePicker() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            fallbackImagePickerLauncher.launch("image/*")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Nova Triagem de Suporte",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Receba relatos de WhatsApp, ligação ou texto e transforme em chamado estruturado com IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Como deseja informar o relato?",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = inputTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = inputTab == 0,
                        onClick = { inputTab = 0 },
                        text = { Text("Texto", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = inputTab == 1,
                        onClick = { inputTab = 1 },
                        text = { Text("Gravar Voz", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = inputTab == 2,
                        onClick = { inputTab = 2 },
                        text = { Text("Upload Áudio", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = inputTab == 3,
                        onClick = { inputTab = 3 },
                        text = { Text("Foto / Print", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (inputTab) {
                    0 -> {
                        // Text input tab
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { viewModel.onRawTextChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("raw_text_input"),
                            placeholder = {
                                Text(
                                    "Ex: O Carlos me ligou avisando que a Filial Sul não consegue emitir notas desde às 9h, dá erro 504 de timeout e os caminhões estão parados...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    1 -> {
                        // In-app voice recorder tab
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isRecording) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(RoseCritical.copy(alpha = 0.2f), CircleShape)
                                            .border(2.dp, RoseCritical, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.stopVoiceRecording() },
                                            modifier = Modifier.size(56.dp).testTag("stop_recording_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "Parar Gravação",
                                                tint = RoseCritical,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Gravando relato com microfone...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = RoseCritical
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.cancelVoiceRecording() }
                                    ) {
                                        Text("Cancelar")
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            },
                                            modifier = Modifier.size(56.dp).testTag("start_recording_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Gravar Áudio",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Toque no microfone para gravar o relato",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Audio preview if recorded
                                if (audioFile != null && !isRecording) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(EmeraldSuccess.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.toggleAudioPlayback() }) {
                                                Icon(
                                                    imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = "Ouvir",
                                                    tint = EmeraldSuccess
                                                )
                                            }
                                            Text(
                                                text = if (isPlayingAudio) "Reproduzindo áudio..." else "Áudio gravado pronto (${audioFile?.name?.take(20)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(onClick = { viewModel.clearAudio() }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Descartar",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Upload audio file tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecionar Áudio (.opus, .ogg, .mp3, .m4a)")
                            }
                            Text(
                                text = "Ideal para áudios encaminhados do WhatsApp ou reuniões.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (audioFile != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(EmeraldSuccess.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Arquivo: ${audioFile?.name} ($audioMimeType)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = { viewModel.clearAudio() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Image & Camera Input Tab
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { launchCamera() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("open_camera_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tirar Foto", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { launchImagePicker() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("open_gallery_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Galeria / Print", fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Capture fotos de telas de erro, equipamentos físicos ou importe prints do sistema. O Gemini fará leitura visual (OCR) e diagnóstico automático.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Visual Attachment Row (Available regardless of active tab)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Anexar Evidência Visual:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { launchCamera() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("quick_camera_button")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Câmera", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { launchImagePicker() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("quick_gallery_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Galeria", fontSize = 12.sp)
                        }
                    }
                }

                // Attached Image Preview Card
                if (imageFile != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "📸 Imagem / Print Anexado",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${imageFile?.name} • Evidência para análise Gemini",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { showFullscreenImageDialog = imageFile }) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Ampliar imagem",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.clearImage() }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remover anexo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showFullscreenImageDialog = imageFile }
                            ) {
                                AsyncImage(
                                    model = imageFile,
                                    contentDescription = "Visualização da foto/print de suporte",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Toque para ampliar 🔍",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action button to Analyze with Gemini
                Button(
                    onClick = { viewModel.analyzeWithGemini() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("analyze_with_ai_button"),
                    enabled = !isAnalyzing && (rawText.isNotBlank() || audioFile != null || imageFile != null),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Gemini analisando texto, áudio e imagem...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnLabel = when {
                            imageFile != null && audioFile != null -> "Classificar com IA (Multimodal: Voz + Imagem)"
                            imageFile != null -> "Classificar com IA (Multimodal com Imagem)"
                            audioFile != null -> "Classificar com IA (Multimodal de Áudio)"
                            else -> "Classificar e Triar com IA"
                        }
                        Text(btnLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Error message banner
                AnimatedVisibility(visible = analysisError != null) {
                    analysisError?.let { err ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearAnalysisError() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Similar tickets discovered
        AnimatedVisibility(visible = hasActivePreview && similarTickets.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberWarning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Problemas Parecidos no Histórico",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    similarTickets.forEach { pastTicket ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = pastTicket.id,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = pastTicket.projeto,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = pastTicket.tituloResumo,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (pastTicket.sugestaoSolucao.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Solução anterior: ${pastTicket.sugestaoSolucao.take(120)}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.applySolutionToPreview(pastTicket.sugestaoSolucao) },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aplicar Solução", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Editable Preview Card (Human-in-the-Loop)
        AnimatedVisibility(visible = hasActivePreview) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .testTag("editable_preview_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header of Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Prévia Editável da Triagem",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Revise e ajuste os dados antes de salvar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.dismissPreview() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    // Ticket ID & Priority Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ID: $previewId",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Priority Chips
                    Text(
                        text = "Prioridade do Chamado:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Baixa", "Média", "Alta", "Crítica").forEach { prio ->
                            val selected = previewPrioridade.equals(prio, ignoreCase = true)
                            val chipColor = when (prio) {
                                "Crítica" -> RoseCritical
                                "Alta" -> AmberWarning
                                "Média" -> Color(0xFFEAB308)
                                else -> EmeraldSuccess
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.updatePreviewPriority(prio) },
                                label = { Text(prio, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                    selectedLabelColor = chipColor
                                )
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = previewTitulo,
                        onValueChange = { viewModel.updatePreviewTitle(it) },
                        label = { Text("Título / Resumo da Solicitação") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Requester & Channel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = previewSolicitanteNome,
                            onValueChange = { viewModel.updatePreviewRequester(it) },
                            label = { Text("Solicitante") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Channel dropdown
                        var channelExpanded by remember { mutableStateOf(false) }
                        val channels = listOf("WhatsApp", "Telefone", "Presencial", "Reunião", "E-mail", "Chat Interno")

                        ExposedDropdownMenuBox(
                            expanded = channelExpanded,
                            onExpandedChange = { channelExpanded = !channelExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = previewCanal,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Canal Origem") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = channelExpanded,
                                onDismissRequest = { channelExpanded = false }
                            ) {
                                channels.forEach { ch ->
                                    DropdownMenuItem(
                                        text = { Text(ch) },
                                        onClick = {
                                            viewModel.updatePreviewChannel(ch)
                                            channelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Project selection
                    var projectExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = projectExpanded,
                        onExpandedChange = { projectExpanded = !projectExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = previewProjeto,
                            onValueChange = { viewModel.updatePreviewProject(it) },
                            label = { Text("Projeto / Sistema") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = projectExpanded,
                            onDismissRequest = { projectExpanded = false }
                        ) {
                            allProjects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.nome) },
                                    onClick = {
                                        viewModel.updatePreviewProject(proj.nome)
                                        projectExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Service / Module & Problem Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = previewServicoModulo,
                            onValueChange = { viewModel.updatePreviewService(it) },
                            label = { Text("Serviço / Módulo") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = previewTipoProblema,
                            onValueChange = { viewModel.updatePreviewType(it) },
                            label = { Text("Tipo de Problema") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Structured Description
                    OutlinedTextField(
                        value = previewDescricaoEstruturada,
                        onValueChange = { viewModel.updatePreviewDescription(it) },
                        label = { Text("Detalhamento Estruturado (Sintoma / Impacto)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Suggested Solution
                    OutlinedTextField(
                        value = previewSugestaoSolucao,
                        onValueChange = { viewModel.updatePreviewSolution(it) },
                        label = { Text("💡 Diagnóstico e Solução Sugerida") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Tags
                    OutlinedTextField(
                        value = previewTags,
                        onValueChange = { viewModel.updatePreviewTags(it) },
                        label = { Text("Tags (separadas por vírgula)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Visual Attachment preview in Ticket Confirmation
                    if (imageFile != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showFullscreenImageDialog = imageFile }
                                    ) {
                                        AsyncImage(
                                            model = imageFile,
                                            contentDescription = "Miniatura do anexo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "📸 Anexo Visual Vinculado",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${imageFile?.name} • Salvo no Vault em Anexos/",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(onClick = { showFullscreenImageDialog = imageFile }) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Ampliar evidência",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Confirmation Buttons
                    Text(
                        text = "Ações de Salvamento e Despacho:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save to Obsidian Vault (.md)
                        Button(
                            onClick = { viewModel.confirmAndSaveTicket(sendToChat = false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_vault_button"),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar no Vault (.md)", fontSize = 13.sp)
                        }

                        // Save & Send to Google Chat
                        Button(
                            onClick = { viewModel.confirmAndSaveTicket(sendToChat = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_and_chat_button"),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar & Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Optional Email Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${viewModel.settings.supportEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "[$previewId] $previewTitulo ($previewPrioridade)")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                    Triagem de Suporte com IA
                                    ID: $previewId
                                    Projeto: $previewProjeto ($previewServicoModulo)
                                    Solicitante: $previewSolicitanteNome ($previewCanal)
                                    Prioridade: $previewPrioridade
                                    
                                    Descrição:
                                    $previewDescricaoEstruturada
                                    
                                    Solução Sugerida:
                                    $previewSugestaoSolucao
                                    """.trimIndent()
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Enviar Triagem por E-mail"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar Resumo por E-mail (Opcional)", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Fullscreen Image Dialog
    if (showFullscreenImageDialog != null) {
        Dialog(onDismissRequest = { showFullscreenImageDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Evidência Visual",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = showFullscreenImageDialog?.name ?: "Imagem de Suporte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showFullscreenImageDialog = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 460.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        AsyncImage(
                            model = showFullscreenImageDialog,
                            contentDescription = "Visualização Ampliada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
