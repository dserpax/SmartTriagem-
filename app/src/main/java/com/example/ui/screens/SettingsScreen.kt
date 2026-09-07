package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.webhook.GoogleChatWebhookService
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.TriageViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: TriageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings

    val isRestoringVault by viewModel.isRestoringVault.collectAsState()
    val isSyncingVault by viewModel.isSyncingVault.collectAsState()
    val restoreReport by viewModel.restoreReport.collectAsState()

    var webhookUrl by remember { mutableStateOf(settings.googleChatWebhookUrl) }
    var customGeminiKey by remember { mutableStateOf(settings.customGeminiApiKey) }
    var supportEmail by remember { mutableStateOf(settings.supportEmail) }
    var vaultPathInput by remember { mutableStateOf(settings.customVaultPath) }

    var isTestingWebhook by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }

    var showObsidianStructure by remember { mutableStateOf(false) }

    // When restore completes, update input fields to reflect restored keys
    LaunchedEffect(restoreReport) {
        if (restoreReport != null) {
            webhookUrl = settings.googleChatWebhookUrl
            customGeminiKey = settings.customGeminiApiKey
            supportEmail = settings.supportEmail
            vaultPathInput = settings.customVaultPath
        }
    }

    val currentVaultDir = remember(vaultPathInput, isRestoringVault, isSyncingVault) {
        viewModel.vaultManager.getVaultRootDirectory(settings.customVaultPath.ifBlank { null })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // SECTION 1: OBSIDIAN VAULT COMO BANCO DE DADOS (Destaque Principal)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Obsidian Vault como Banco de Dados",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Notas em .MD como fonte de verdade primária e sincronizada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Banco Ativo",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Current Vault Path Display
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📁 Localização Raiz do Vault:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentVaultDir.absolutePath,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Vault Selection / Path Input
                OutlinedTextField(
                    value = vaultPathInput,
                    onValueChange = {
                        vaultPathInput = it
                        settings.customVaultPath = it.trim()
                    },
                    label = { Text("Caminho do Vault (opcional / pasta personalizada)") },
                    placeholder = { Text(currentVaultDir.absolutePath) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_path_input"),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Caminho do Vault", currentVaultDir.absolutePath)
                                clipboard.setPrimaryClip(clip)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar caminho", modifier = Modifier.size(20.dp))
                        }
                    }
                )

                // Actions: Restore & Full Sync
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botão 1: Restaurar Banco e Chaves
                    Button(
                        onClick = { viewModel.selectVaultAndRestore(vaultPathInput) },
                        enabled = !isRestoringVault && !isSyncingVault,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_vault_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isRestoringVault) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restaurando banco e chaves do Vault...", fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restaurar Chaves e Banco do Vault", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Botão 2: Gravar / Sincronizar Backup Completo
                    OutlinedButton(
                        onClick = { viewModel.syncFullDatabaseToVault() },
                        enabled = !isRestoringVault && !isSyncingVault,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_to_vault_button")
                    ) {
                        if (isSyncingVault) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gravando notas e chaves no Vault...", fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar Backup Completo no Vault Agora", fontSize = 13.sp)
                        }
                    }
                }

                // Restore Report Card if present
                restoreReport?.let { report ->
                    Surface(
                        color = if (report.success) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (report.success) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (report.success) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (report.success) "Sincronização / Restauração Concluída!" else "Aviso de Restauração",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (report.success) EmeraldSuccess else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "• Chamados recuperados: ${report.ticketsRestored}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Projetos aprendidos: ${report.projectsRestored}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Chaves de backup (Webhook / Configs): ${if (report.configRestored) "Restauradas com sucesso ✅" else "Mantidas atuais"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (report.webhookRestored) {
                                Text(
                                    text = "• Webhook Google Chat restaurado e pronto para envio!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }

                // Expandable Section: Obsidian Database Architecture
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showObsidianStructure = !showObsidianStructure }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estrutura e Organização do Banco em .MD",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Icon(
                                imageVector = if (showObsidianStructure) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showObsidianStructure) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "O Vault segue o padrão formal do Obsidian para atuar como banco de dados NoSQL baseado em Markdown:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "📂 Chamados/ → Uma nota individual para cada solicitação com YAML Frontmatter (`id`, `date`, `priority`, `project`, `status`, `tags`) e Dataview.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "📂 Projetos/ → Notas dedicadas com histórico e consultas Dataview para ver no Obsidian todos os chamados de cada projeto.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "📂 Base de Conhecimento/ → Soluções e diagnósticos catalogados.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "📂 _Sistema/backup_config.md → Backup criptografado/estruturado de chaves (Webhook Google Chat, Gemini, preferências) que permite restauração instantânea.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "📂 _Sistema/_Indice_Geral_MOC.md → Map of Content (MOC) para navegação no Obsidian por Graph View ou tabelas.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: INTEGRAÇÃO GOOGLE CHAT (WEBHOOK)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Integração Google Chat (Webhook)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Despacha chamados triados em Card interativo para o canal da equipe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = {
                        webhookUrl = it
                        settings.googleChatWebhookUrl = it.trim()
                    },
                    label = { Text("URL do Webhook do Google Chat") },
                    placeholder = { Text("https://chat.googleapis.com/v1/spaces/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webhook_url_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isTestingWebhook = true
                                testResultMsg = null
                                val service = GoogleChatWebhookService()
                                val res = service.testWebhook(webhookUrl)
                                isTestingWebhook = false
                                if (res.isSuccess) {
                                    testSuccess = true
                                    testResultMsg = "Conexão OK! Card de teste enviado para o canal."
                                } else {
                                    testSuccess = false
                                    testResultMsg = "Erro: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        enabled = !isTestingWebhook && webhookUrl.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("test_webhook_button")
                    ) {
                        if (isTestingWebhook) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testando...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testar Webhook", fontSize = 12.sp)
                        }
                    }

                    if (webhookUrl.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvo no backup", fontSize = 11.sp, color = EmeraldSuccess)
                        }
                    }
                }

                testResultMsg?.let { msg ->
                    Surface(
                        color = if (testSuccess == true) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testSuccess == true) EmeraldSuccess else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // SECTION 3: GEMINI API KEY CONFIGURATION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Chave da API do Gemini (IA)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val defaultKeyConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                        Text(
                            text = if (defaultKeyConfigured) "Chave ativa injetada pelo Secrets panel." else "Usando chave padrão do ambiente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (defaultKeyConfigured) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = customGeminiKey,
                    onValueChange = {
                        customGeminiKey = it
                        settings.customGeminiApiKey = it.trim()
                    },
                    label = { Text("Chave Personalizada (Backup no Vault)") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // SECTION 4: E-MAIL DE SUPORTE
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "E-mail de Suporte da Equipe",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedTextField(
                    value = supportEmail,
                    onValueChange = {
                        supportEmail = it
                        settings.supportEmail = it.trim()
                    },
                    label = { Text("Destinatário Padrão de Chamados") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
