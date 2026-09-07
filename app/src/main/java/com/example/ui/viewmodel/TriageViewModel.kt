package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioRecordHelper
import com.example.data.gemini.GeminiService
import com.example.data.local.AppDatabase
import com.example.data.markdown.ObsidianVaultManager
import com.example.data.markdown.VaultRestoreReport
import com.example.data.model.KnownSolutionEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TicketCommentEntity
import com.example.data.model.TicketEntity
import com.example.data.preferences.AppSettings
import com.example.data.webhook.GoogleChatWebhookService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TriageViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val ticketDao = db.ticketDao()
    private val projectDao = db.projectDao()
    private val solutionDao = db.knownSolutionDao()
    private val commentDao = db.ticketCommentDao()

    private val geminiService = GeminiService(application)
    private val webhookService = GoogleChatWebhookService()
    val vaultManager = ObsidianVaultManager(application)
    val audioHelper = AudioRecordHelper(application)
    val imageHelper = com.example.data.image.ImageHelper(application)
    val settings = AppSettings(application)

    // Data streams
    val allProjects: StateFlow<List<ProjectEntity>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSolutions: StateFlow<List<KnownSolutionEntity>> = solutionDao.getAllSolutions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _projectFilter = MutableStateFlow<String?>(null)
    val projectFilter: StateFlow<String?> = _projectFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<String?>(null)
    val priorityFilter: StateFlow<String?> = _priorityFilter.asStateFlow()

    val filteredTickets: StateFlow<List<TicketEntity>> = combine(
        ticketDao.getAllTickets(),
        _searchQuery,
        _projectFilter,
        _priorityFilter
    ) { tickets, query, proj, prio ->
        tickets.filter { ticket ->
            val matchesQuery = query.isBlank() ||
                    ticket.tituloResumo.contains(query, ignoreCase = true) ||
                    ticket.solicitanteNome.contains(query, ignoreCase = true) ||
                    ticket.projeto.contains(query, ignoreCase = true) ||
                    ticket.tags.contains(query, ignoreCase = true) ||
                    ticket.relatoOriginal.contains(query, ignoreCase = true)

            val matchesProject = proj == null || ticket.projeto.equals(proj, ignoreCase = true)
            val matchesPriority = prio == null || ticket.prioridade.equals(prio, ignoreCase = true)

            matchesQuery && matchesProject && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Input States
    private val _rawInputText = MutableStateFlow("")
    val rawInputText: StateFlow<String> = _rawInputText.asStateFlow()

    private val _audioFile = MutableStateFlow<File?>(null)
    val audioFile: StateFlow<File?> = _audioFile.asStateFlow()

    private val _audioMimeType = MutableStateFlow("audio/mp4")
    val audioMimeType: StateFlow<String> = _audioMimeType.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    // Image Input States (Camera & Gallery Import)
    private val _imageFile = MutableStateFlow<File?>(null)
    val imageFile: StateFlow<File?> = _imageFile.asStateFlow()

    private val _cameraTempUri = MutableStateFlow<Uri?>(null)
    val cameraTempUri: StateFlow<Uri?> = _cameraTempUri.asStateFlow()

    private var cameraTempFile: File? = null

    // Processing & Analysis
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Editable Preview States
    private val _hasActivePreview = MutableStateFlow(false)
    val hasActivePreview: StateFlow<Boolean> = _hasActivePreview.asStateFlow()

    private val _previewId = MutableStateFlow("")
    val previewId: StateFlow<String> = _previewId.asStateFlow()

    private val _previewTitulo = MutableStateFlow("")
    val previewTitulo: StateFlow<String> = _previewTitulo.asStateFlow()

    private val _previewSolicitanteNome = MutableStateFlow("")
    val previewSolicitanteNome: StateFlow<String> = _previewSolicitanteNome.asStateFlow()

    private val _previewSolicitanteInfo = MutableStateFlow("")
    val previewSolicitanteInfo: StateFlow<String> = _previewSolicitanteInfo.asStateFlow()

    private val _previewCanal = MutableStateFlow("WhatsApp")
    val previewCanal: StateFlow<String> = _previewCanal.asStateFlow()

    private val _previewProjeto = MutableStateFlow("")
    val previewProjeto: StateFlow<String> = _previewProjeto.asStateFlow()

    private val _previewServicoModulo = MutableStateFlow("")
    val previewServicoModulo: StateFlow<String> = _previewServicoModulo.asStateFlow()

    private val _previewTipoProblema = MutableStateFlow("Incidente")
    val previewTipoProblema: StateFlow<String> = _previewTipoProblema.asStateFlow()

    private val _previewPrioridade = MutableStateFlow("Média")
    val previewPrioridade: StateFlow<String> = _previewPrioridade.asStateFlow()

    private val _previewJustificativaPrioridade = MutableStateFlow("")
    val previewJustificativaPrioridade: StateFlow<String> = _previewJustificativaPrioridade.asStateFlow()

    private val _previewDescricaoEstruturada = MutableStateFlow("")
    val previewDescricaoEstruturada: StateFlow<String> = _previewDescricaoEstruturada.asStateFlow()

    private val _previewSugestaoSolucao = MutableStateFlow("")
    val previewSugestaoSolucao: StateFlow<String> = _previewSugestaoSolucao.asStateFlow()

    private val _previewTags = MutableStateFlow("")
    val previewTags: StateFlow<String> = _previewTags.asStateFlow()

    // Similar tickets discovered
    private val _similarTickets = MutableStateFlow<List<TicketEntity>>(emptyList())
    val similarTickets: StateFlow<List<TicketEntity>> = _similarTickets.asStateFlow()

    // Transient action feedback
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Vault as Database & Restoration states
    private val _isRestoringVault = MutableStateFlow(false)
    val isRestoringVault: StateFlow<Boolean> = _isRestoringVault.asStateFlow()

    private val _isSyncingVault = MutableStateFlow(false)
    val isSyncingVault: StateFlow<Boolean> = _isSyncingVault.asStateFlow()

    private val _restoreReport = MutableStateFlow<VaultRestoreReport?>(null)
    val restoreReport: StateFlow<VaultRestoreReport?> = _restoreReport.asStateFlow()

    // UI helper methods
    fun onRawTextChanged(text: String) {
        _rawInputText.value = text
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setProjectFilter(project: String?) {
        _projectFilter.value = project
    }

    fun setPriorityFilter(priority: String?) {
        _priorityFilter.value = priority
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearAnalysisError() {
        _analysisError.value = null
    }

    // Audio recording controls
    fun startVoiceRecording() {
        val result = audioHelper.startRecording()
        if (result.isSuccess) {
            _audioFile.value = result.getOrNull()
            _audioMimeType.value = "audio/mp4"
            _isRecording.value = true
        } else {
            _analysisError.value = "Erro ao iniciar microfone: ${result.exceptionOrNull()?.message}"
        }
    }

    fun stopVoiceRecording() {
        val file = audioHelper.stopRecording()
        _isRecording.value = false
        if (file != null && file.exists()) {
            _audioFile.value = file
            _actionMessage.value = "Áudio gravado com sucesso! Pronto para triagem com IA."
        }
    }

    fun cancelVoiceRecording() {
        audioHelper.cancelRecording()
        _isRecording.value = false
        _audioFile.value = null
    }

    fun onAudioUriSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val copied = audioHelper.copyUriToTempFile(uri)
            if (copied != null) {
                _audioFile.value = copied.first
                _audioMimeType.value = copied.second
                _actionMessage.value = "Áudio carregado (${copied.second}). Pronto para análise!"
            } else {
                _analysisError.value = "Não foi possível carregar o arquivo de áudio selecionado."
            }
        }
    }

    fun toggleAudioPlayback() {
        val file = _audioFile.value ?: return
        if (_isPlayingAudio.value) {
            audioHelper.stopPlaying()
            _isPlayingAudio.value = false
        } else {
            audioHelper.playAudio(file) {
                _isPlayingAudio.value = false
            }
            _isPlayingAudio.value = true
        }
    }

    fun clearAudio() {
        audioHelper.stopPlaying()
        _isPlayingAudio.value = false
        _audioFile.value?.delete()
        _audioFile.value = null
    }

    // Camera and Gallery Image Controls
    fun createCameraCaptureUri(): Uri {
        val (uri, file) = imageHelper.createCameraImageUri()
        cameraTempFile = file
        _cameraTempUri.value = uri
        return uri
    }

    fun onCameraPhotoTaken(success: Boolean) {
        if (success) {
            val temp = cameraTempFile
            if (temp != null && temp.exists() && temp.length() > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    val optimized = imageHelper.copyAndOptimizeUri(Uri.fromFile(temp), "camera")
                    if (optimized != null) {
                        _imageFile.value = optimized
                        _actionMessage.value = "Foto da câmera capturada com sucesso! Pronto para triagem com IA."
                    } else {
                        _analysisError.value = "Erro ao processar a foto capturada."
                    }
                }
            }
        }
    }

    fun onImageSelectedFromGallery(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val optimized = imageHelper.copyAndOptimizeUri(uri, "imported_image")
            if (optimized != null) {
                _imageFile.value = optimized
                _actionMessage.value = "Imagem/Print importado com sucesso! Pronto para triagem com IA."
            } else {
                _analysisError.value = "Não foi possível carregar o arquivo de imagem selecionado."
            }
        }
    }

    fun clearImage() {
        _imageFile.value?.delete()
        _imageFile.value = null
        cameraTempFile?.delete()
        cameraTempFile = null
        _cameraTempUri.value = null
    }

    // AI Analysis
    fun analyzeWithGemini() {
        val text = _rawInputText.value
        val audio = _audioFile.value
        val image = _imageFile.value
        if (text.isBlank() && audio == null && image == null) {
            _analysisError.value = "Por favor, digite um relato, grave/anexe um áudio ou anexe uma foto/print antes de analisar."
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null

            val projectsList = projectDao.getAllProjectsList()
            val imageBytes = image?.let { imageHelper.getImageBytes(it) }
            val imageMime = image?.let { imageHelper.getImageMimeType(it) } ?: "image/jpeg"

            val result = geminiService.extractTriageFromTextOrAudio(
                textInput = text.ifBlank { null },
                audioFile = audio,
                audioMimeType = _audioMimeType.value,
                imageBytes = imageBytes,
                imageMimeType = imageMime,
                knownProjects = projectsList,
                customApiKey = settings.customGeminiApiKey.ifBlank { null }
            )

            _isAnalyzing.value = false
            result.onSuccess { extraction ->
                // Generate unique Ticket ID
                val dateId = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
                _previewId.value = "TRI-$dateId"
                _previewTitulo.value = extraction.tituloResumo
                _previewSolicitanteNome.value = extraction.solicitanteNome
                _previewSolicitanteInfo.value = ""
                _previewCanal.value = extraction.solicitanteCanal
                _previewProjeto.value = extraction.projeto
                _previewServicoModulo.value = extraction.servicoModulo
                _previewTipoProblema.value = extraction.tipoProblema
                _previewPrioridade.value = extraction.prioridade
                _previewJustificativaPrioridade.value = extraction.justificativaPrioridade
                _previewDescricaoEstruturada.value = extraction.descricaoEstruturada
                _previewSugestaoSolucao.value = extraction.sugestaoSolucao
                _previewTags.value = extraction.tags.joinToString(", ")
                _hasActivePreview.value = true

                // Search for similar historical tickets and known solutions
                searchSimilarHistory(extraction.projeto, extraction.tags.firstOrNull() ?: extraction.tituloResumo)

                _actionMessage.value = "Triagem concluída com sucesso! Revise os campos antes de confirmar."
            }.onFailure { err ->
                _analysisError.value = "Falha ao processar com IA: ${err.message}"
            }
        }
    }

    private fun searchSimilarHistory(project: String, keyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val similar = ticketDao.findSimilarTickets(project, keyword.take(15))
            _similarTickets.value = similar
        }
    }

    // Editable Preview Field Updates
    fun updatePreviewTitle(v: String) { _previewTitulo.value = v }
    fun updatePreviewRequester(v: String) { _previewSolicitanteNome.value = v }
    fun updatePreviewRequesterInfo(v: String) { _previewSolicitanteInfo.value = v }
    fun updatePreviewChannel(v: String) { _previewCanal.value = v }
    fun updatePreviewProject(v: String) {
        _previewProjeto.value = v
        searchSimilarHistory(v, _previewTags.value.split(",").firstOrNull() ?: "")
    }
    fun updatePreviewService(v: String) { _previewServicoModulo.value = v }
    fun updatePreviewType(v: String) { _previewTipoProblema.value = v }
    fun updatePreviewPriority(v: String) { _previewPrioridade.value = v }
    fun updatePreviewPriorityReason(v: String) { _previewJustificativaPrioridade.value = v }
    fun updatePreviewDescription(v: String) { _previewDescricaoEstruturada.value = v }
    fun updatePreviewSolution(v: String) { _previewSugestaoSolucao.value = v }
    fun updatePreviewTags(v: String) { _previewTags.value = v }

    fun applySolutionToPreview(sol: String) {
        _previewSugestaoSolucao.value = sol
        _actionMessage.value = "Solução anterior aplicada à prévia!"
    }

    fun dismissPreview() {
        _hasActivePreview.value = false
    }

    // Confirm and Save Ticket
    fun confirmAndSaveTicket(sendToChat: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            val now = System.currentTimeMillis()
            val originalText = buildString {
                if (_rawInputText.value.isNotBlank()) {
                    append(_rawInputText.value)
                }
                if (_audioFile.value != null) {
                    if (isNotEmpty()) append("\n\n")
                    append("[Relato por gravação de áudio multimodal]")
                }
            }

            var finalImagePath: String? = null
            val img = _imageFile.value
            if (img != null && img.exists()) {
                val vaultRoot = vaultManager.getVaultRootDirectory(settings.customVaultPath.ifBlank { null })
                imageHelper.copyToVaultAttachments(img, vaultRoot)
                finalImagePath = img.absolutePath
            }

            var ticket = TicketEntity(
                id = _previewId.value.ifBlank { "TRI-${System.currentTimeMillis()}" },
                createdAt = now,
                canal = _previewCanal.value,
                solicitanteNome = _previewSolicitanteNome.value.ifBlank { "Usuário" },
                solicitanteInfo = _previewSolicitanteInfo.value,
                projeto = _previewProjeto.value.ifBlank { "Geral" },
                servicoModulo = _previewServicoModulo.value.ifBlank { "Suporte" },
                tipoProblema = _previewTipoProblema.value,
                prioridade = _previewPrioridade.value,
                justificativaPrioridade = _previewJustificativaPrioridade.value,
                tituloResumo = _previewTitulo.value.ifBlank { "Chamado sem título" },
                relatoOriginal = originalText,
                descricaoEstruturada = _previewDescricaoEstruturada.value,
                sugestaoSolucao = _previewSugestaoSolucao.value,
                tags = _previewTags.value,
                status = "Triado",
                markdownPath = null,
                imagePath = finalImagePath,
                webhookSent = false,
                emailSent = false
            )

            // Save to Obsidian Vault (.md)
            val vaultResult = vaultManager.saveTicketToVault(ticket, settings.customVaultPath.ifBlank { null })
            if (vaultResult.isSuccess) {
                ticket = ticket.copy(markdownPath = vaultResult.getOrNull()?.absolutePath)
            }

            // Send to Google Chat Webhook if requested or configured
            var webhookSuccess = false
            val webhookUrl = settings.googleChatWebhookUrl
            if (sendToChat && webhookUrl.isNotBlank()) {
                val postResult = webhookService.sendTicketCard(webhookUrl, ticket)
                if (postResult.isSuccess) {
                    webhookSuccess = true
                    ticket = ticket.copy(webhookSent = true, status = "Enviado Google Chat")
                }
            }

            // Insert into Room DB
            ticketDao.insert(ticket)

            // Dynamic learning: reinforce project usage count and add unknown project/service
            val projName = ticket.projeto
            val existingProj = projectDao.getProjectByName(projName)
            if (existingProj != null) {
                projectDao.incrementUsage(projName)
            } else if (projName.isNotBlank() && projName != "Geral") {
                projectDao.insert(
                    ProjectEntity(
                        nome = projName,
                        servicos = ticket.servicoModulo,
                        termosChave = ticket.tags,
                        responsaveis = "Equipe $projName",
                        usageCount = 1
                    )
                )
            }

            // Save known solution if it was provided
            if (ticket.sugestaoSolucao.isNotBlank() && ticket.sugestaoSolucao.length > 15) {
                solutionDao.insert(
                    KnownSolutionEntity(
                        sintoma = ticket.tituloResumo,
                        projeto = ticket.projeto,
                        solucao = ticket.sugestaoSolucao,
                        tags = ticket.tags,
                        sucessos = 1
                    )
                )
            }

            // Mirror project and backup configuration to Obsidian Vault as primary database
            val updatedProj = projectDao.getProjectByName(projName)
            if (updatedProj != null) {
                vaultManager.saveProjectToVault(updatedProj, settings.customVaultPath.ifBlank { null })
            }
            vaultManager.saveBackupConfig(settings, filteredTickets.value.size + 1, settings.customVaultPath.ifBlank { null })

            // Reset input and preview
            _hasActivePreview.value = false
            _rawInputText.value = ""
            clearAudio()
            clearImage()
            _isSaving.value = false

            val msg = buildString {
                append("Chamado ${ticket.id} salvo com sucesso no Obsidian Vault!")
                if (sendToChat) {
                    if (webhookSuccess) append(" E despachado para o Google Chat!")
                    else append(" (Aviso: webhook do Google Chat não configurado ou com falha).")
                }
            }
            _actionMessage.value = msg
        }
    }

    // Vault as Primary Database: Selection, Restoration and Backup
    fun selectVaultAndRestore(vaultPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRestoringVault.value = true
            try {
                if (vaultPath.isNotBlank()) {
                    settings.customVaultPath = vaultPath.trim()
                }
                val restoreResult = vaultManager.restoreDatabaseFromVault(settings.customVaultPath.ifBlank { null })
                if (restoreResult.isSuccess) {
                    val data = restoreResult.getOrThrow()

                    // 1. Restore settings & keys from backup_config.md
                    data.backupConfig?.let { cfg ->
                        if (cfg.googleChatWebhook.isNotBlank()) {
                            settings.googleChatWebhookUrl = cfg.googleChatWebhook
                        }
                        if (cfg.customGeminiApiKey.isNotBlank()) {
                            settings.customGeminiApiKey = cfg.customGeminiApiKey
                        }
                        if (cfg.supportEmail.isNotBlank()) {
                            settings.supportEmail = cfg.supportEmail
                        }
                        settings.autoSendWebhook = cfg.autoSendWebhook
                        settings.autoExportMarkdown = cfg.autoExportMarkdown
                    }

                    // 2. Restore tickets into Room Database (mirroring the .md notes)
                    data.tickets.forEach { ticket ->
                        ticketDao.insert(ticket)
                    }

                    // 3. Restore projects into Room Database
                    data.projects.forEach { proj ->
                        val existing = projectDao.getProjectByName(proj.nome)
                        if (existing == null) {
                            projectDao.insert(proj)
                        } else {
                            projectDao.update(existing.copy(
                                servicos = if (existing.servicos.isBlank()) proj.servicos else existing.servicos,
                                termosChave = if (existing.termosChave.isBlank()) proj.termosChave else existing.termosChave,
                                usageCount = maxOf(existing.usageCount, proj.usageCount)
                            ))
                        }
                    }

                    // 4. Restore solutions
                    data.solutions.forEach { sol ->
                        solutionDao.insert(sol)
                    }

                    // 5. Restore comments/responses into Room Database
                    if (data.comments.isNotEmpty()) {
                        commentDao.insertAll(data.comments)
                    }

                    _restoreReport.value = data.report
                    _actionMessage.value = data.report.message
                } else {
                    val error = restoreResult.exceptionOrNull()?.message ?: "Erro desconhecido ao ler o vault."
                    _actionMessage.value = "Erro ao restaurar vault: $error"
                }
            } catch (e: Exception) {
                _actionMessage.value = "Falha na restauração do vault: ${e.message}"
            } finally {
                _isRestoringVault.value = false
            }
        }
    }

    fun restoreFromCurrentVault() {
        selectVaultAndRestore(settings.customVaultPath)
    }

    fun syncFullDatabaseToVault() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingVault.value = true
            try {
                val tickets = ticketDao.getAllTickets().first()
                val projects = projectDao.getAllProjectsList()
                val solutions = solutionDao.getAllSolutions().first()

                val result = vaultManager.syncFullDatabaseToVault(
                    tickets = tickets,
                    projects = projects,
                    solutions = solutions,
                    settings = settings,
                    customVaultPath = settings.customVaultPath.ifBlank { null }
                )
                if (result.isSuccess) {
                    val report = result.getOrThrow()
                    _restoreReport.value = report
                    _actionMessage.value = "Backup completo e espelho em .MD gravado no Vault com sucesso!"
                } else {
                    _actionMessage.value = "Erro ao sincronizar vault: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _actionMessage.value = "Erro ao sincronizar vault: ${e.message}"
            } finally {
                _isSyncingVault.value = false
            }
        }
    }

    fun clearRestoreReport() {
        _restoreReport.value = null
    }

    // Quick actions on existing tickets
    fun resendTicketToWebhook(ticket: TicketEntity) {
        val webhookUrl = settings.googleChatWebhookUrl
        if (webhookUrl.isBlank()) {
            _actionMessage.value = "Configure a URL do Webhook do Google Chat nas Configurações."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = webhookService.sendTicketCard(webhookUrl, ticket)
            if (result.isSuccess) {
                val updated = ticket.copy(webhookSent = true, status = "Enviado Google Chat")
                ticketDao.update(updated)
                _actionMessage.value = "Chamado ${ticket.id} reenviado ao Google Chat com sucesso!"
            } else {
                _actionMessage.value = "Falha ao enviar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun exportAllToVault() {
        viewModelScope.launch(Dispatchers.IO) {
            val tickets = filteredTickets.value
            val result = vaultManager.exportAllTickets(tickets, settings.customVaultPath.ifBlank { null })
            if (result.isSuccess) {
                _actionMessage.value = "${result.getOrNull()} notas exportadas para o Obsidian Vault com sucesso!"
            } else {
                _actionMessage.value = "Erro ao exportar vault: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteTicket(ticket: TicketEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ticketDao.delete(ticket)
            _actionMessage.value = "Chamado ${ticket.id} removido."
        }
    }

    fun updateTicketStatus(ticket: TicketEntity, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = ticket.copy(status = newStatus)
            ticketDao.update(updated)
            // Synchronize updated status with Obsidian Markdown Vault note with comments
            val comments = commentDao.getCommentsForTicketList(ticket.id)
            vaultManager.saveTicketToVault(updated, settings.customVaultPath.ifBlank { null }, comments)
            _actionMessage.value = "Status do chamado ${ticket.id} atualizado para '$newStatus'!"
        }
    }

    // --- Response & Communication History System ---

    fun getCommentsForTicket(ticketId: String): Flow<List<TicketCommentEntity>> {
        return commentDao.getCommentsForTicket(ticketId)
    }

    fun addComment(
        ticket: TicketEntity,
        autorNome: String,
        autorTipo: String,
        mensagem: String,
        imageUri: Uri? = null
    ) {
        if (mensagem.isBlank() && imageUri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            var commentImagePath: String? = null
            if (imageUri != null) {
                val optimized = imageHelper.copyAndOptimizeUri(imageUri, "comment_${ticket.id}")
                if (optimized != null) {
                    val vaultRoot = vaultManager.getVaultRootDirectory(settings.customVaultPath.ifBlank { null })
                    imageHelper.copyToVaultAttachments(optimized, vaultRoot)
                    commentImagePath = optimized.absolutePath
                }
            }

            val newComment = TicketCommentEntity(
                ticketId = ticket.id,
                autorNome = autorNome.trim().ifBlank { if (autorTipo == "TECNICO") "Técnico Suporte" else ticket.solicitanteNome.ifBlank { "Solicitante" } },
                autorTipo = autorTipo,
                mensagem = mensagem.trim().ifBlank { "[Imagem anexa enviada]" },
                imagePath = commentImagePath,
                createdAt = System.currentTimeMillis()
            )
            commentDao.insert(newComment)

            // Sincroniza imediatamente o arquivo .md com o histórico completo de respostas
            val allComments = commentDao.getCommentsForTicketList(ticket.id)
            vaultManager.saveTicketToVault(ticket, settings.customVaultPath.ifBlank { null }, allComments)
            _actionMessage.value = "Resposta registrada e sincronizada com o Obsidian Vault!"
        }
    }

    // --- Ticket Closure (Fechamento de Ocorrências) ---

    fun closeTicket(
        ticket: TicketEntity,
        motivoResolucao: String,
        resolvidoPor: String,
        adicionarHistorico: Boolean = true
    ) {
        if (motivoResolucao.isBlank()) {
            _actionMessage.value = "O motivo da resolução é obrigatório para encerrar o chamado."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val technicianName = resolvidoPor.trim().ifBlank { "Técnico Responsável" }
            val cleanReason = motivoResolucao.trim()

            val closedTicket = ticket.copy(
                status = "Concluído",
                closedAt = now,
                motivoResolucao = cleanReason,
                resolvidoPor = technicianName
            )
            ticketDao.update(closedTicket)

            if (adicionarHistorico) {
                val closureComment = TicketCommentEntity(
                    ticketId = ticket.id,
                    autorNome = technicianName,
                    autorTipo = "TECNICO",
                    mensagem = "🏁 Chamado encerrado como CONCLUÍDO.\nMotivo da Resolução: $cleanReason",
                    createdAt = now
                )
                commentDao.insert(closureComment)
            }

            // Grava o arquivo .md atualizado no Obsidian Vault com cabeçalho de resolução e histórico
            val allComments = commentDao.getCommentsForTicketList(ticket.id)
            vaultManager.saveTicketToVault(closedTicket, settings.customVaultPath.ifBlank { null }, allComments)
            _actionMessage.value = "Chamado ${ticket.id} concluído com sucesso e sincronizado no Obsidian Vault!"
        }
    }

    fun reopenTicket(ticket: TicketEntity, motivoReabertura: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val reopenedTicket = ticket.copy(
                status = "Em Atendimento",
                closedAt = null
            )
            ticketDao.update(reopenedTicket)

            val commentMsg = if (motivoReabertura.isNotBlank()) {
                "🔄 Chamado reaberto para atendimento.\nMotivo: ${motivoReabertura.trim()}"
            } else {
                "🔄 Chamado reaberto pelo técnico para continuidade do suporte."
            }

            val reopenComment = TicketCommentEntity(
                ticketId = ticket.id,
                autorNome = "Técnico Suporte",
                autorTipo = "TECNICO",
                mensagem = commentMsg,
                createdAt = now
            )
            commentDao.insert(reopenComment)

            val allComments = commentDao.getCommentsForTicketList(ticket.id)
            vaultManager.saveTicketToVault(reopenedTicket, settings.customVaultPath.ifBlank { null }, allComments)
            _actionMessage.value = "Chamado ${ticket.id} reaberto com sucesso!"
        }
    }

    fun addOrUpdateProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (project.id == 0) {
                projectDao.insert(project)
            } else {
                projectDao.update(project)
            }
            vaultManager.saveProjectToVault(project, settings.customVaultPath.ifBlank { null })
            _actionMessage.value = "Projeto '${project.nome}' atualizado e sincronizado com o Vault!"
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            projectDao.delete(project)
            _actionMessage.value = "Projeto removido."
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioHelper.cleanup()
    }
}
