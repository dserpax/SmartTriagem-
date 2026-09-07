package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.TicketEntity
import com.example.ui.components.CloseTicketDialog
import com.example.ui.components.TicketKanbanView
import com.example.ui.components.TicketResponsesDialog
import com.example.ui.components.TicketTableView
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseCritical
import com.example.ui.viewmodel.TriageViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistoryViewMode(val title: String) {
    CARDS("Cards"),
    TABLE("Tabela"),
    KANBAN("Kanban")
}

@Composable
fun HistoryScreen(
    viewModel: TriageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tickets by viewModel.filteredTickets.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedProject by viewModel.projectFilter.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.priorityFilter.collectAsStateWithLifecycle()

    var viewMode by remember { mutableStateOf(HistoryViewMode.CARDS) }
    var viewingMarkdownTicket by remember { mutableStateOf<TicketEntity?>(null) }
    var ticketToDelete by remember { mutableStateOf<TicketEntity?>(null) }
    var ticketForResponses by remember { mutableStateOf<TicketEntity?>(null) }
    var ticketToClose by remember { mutableStateOf<TicketEntity?>(null) }
    var previewingImageDialogPath by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // View Mode Switcher (Cards / Tabela / Kanban)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ViewModeTab(
                    title = "Cards",
                    icon = Icons.AutoMirrored.Filled.List,
                    selected = viewMode == HistoryViewMode.CARDS,
                    onClick = { viewMode = HistoryViewMode.CARDS },
                    modifier = Modifier.weight(1f),
                    testTag = "view_mode_cards"
                )
                ViewModeTab(
                    title = "Tabela",
                    icon = Icons.Default.TableChart,
                    selected = viewMode == HistoryViewMode.TABLE,
                    onClick = { viewMode = HistoryViewMode.TABLE },
                    modifier = Modifier.weight(1f),
                    testTag = "view_mode_table"
                )
                ViewModeTab(
                    title = "Kanban",
                    icon = Icons.Default.ViewColumn,
                    selected = viewMode == HistoryViewMode.KANBAN,
                    onClick = { viewMode = HistoryViewMode.KANBAN },
                    modifier = Modifier.weight(1f),
                    testTag = "view_mode_kanban"
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_tickets_input"),
            placeholder = { Text("Buscar por título, solicitante, tags ou projeto...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedProject == null && selectedPriority == null,
                onClick = {
                    viewModel.setProjectFilter(null)
                    viewModel.setPriorityFilter(null)
                },
                label = { Text("Todos (${tickets.size})", fontSize = 12.sp) }
            )

            // Priority filter chips
            listOf("Crítica", "Alta", "Média", "Baixa").forEach { prio ->
                val isSelected = selectedPriority == prio
                val color = when (prio) {
                    "Crítica" -> RoseCritical
                    "Alta" -> AmberWarning
                    "Média" -> Color(0xFFEAB308)
                    else -> EmeraldSuccess
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.setPriorityFilter(if (isSelected) null else prio)
                    },
                    label = { Text(prio, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor = color
                    )
                )
            }

            // Project filter chips
            allProjects.forEach { proj ->
                val isSelected = selectedProject == proj.nome
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.setProjectFilter(if (isSelected) null else proj.nome)
                    },
                    label = { Text(proj.nome, fontSize = 12.sp) }
                )
            }
        }

        // View Content: Cards, Table, or Kanban
        if (tickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nenhum chamado encontrado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Faça uma nova triagem de áudio ou texto para registrar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            when (viewMode) {
                HistoryViewMode.CARDS -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tickets, key = { it.id }) { ticket ->
                            TicketCard(
                                ticket = ticket,
                                dateFormat = dateFormat,
                                onViewMarkdown = { viewingMarkdownTicket = ticket },
                                onResendChat = { viewModel.resendTicketToWebhook(ticket) },
                                onDelete = { ticketToDelete = ticket },
                                onOpenResponses = { ticketForResponses = ticket },
                                onRequestClose = { ticketToClose = ticket },
                                onImageClick = { previewingImageDialogPath = it }
                            )
                        }
                    }
                }
                HistoryViewMode.TABLE -> {
                    TicketTableView(
                        tickets = tickets,
                        dateFormat = dateFormat,
                        onViewMarkdown = { viewingMarkdownTicket = it },
                        onResendChat = { viewModel.resendTicketToWebhook(it) },
                        onDelete = { ticketToDelete = it },
                        onUpdateStatus = { ticket, newStatus -> viewModel.updateTicketStatus(ticket, newStatus) },
                        onOpenResponses = { ticketForResponses = it },
                        onRequestClose = { ticketToClose = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                HistoryViewMode.KANBAN -> {
                    TicketKanbanView(
                        tickets = tickets,
                        dateFormat = dateFormat,
                        onViewMarkdown = { viewingMarkdownTicket = it },
                        onResendChat = { viewModel.resendTicketToWebhook(it) },
                        onDelete = { ticketToDelete = it },
                        onUpdateStatus = { ticket, newStatus -> viewModel.updateTicketStatus(ticket, newStatus) },
                        onOpenResponses = { ticketForResponses = it },
                        onRequestClose = { ticketToClose = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Markdown Detail & Share Dialog
    viewingMarkdownTicket?.let { ticket ->
        val mdContent = remember(ticket) { viewModel.vaultManager.generateMarkdownContent(ticket) }
        AlertDialog(
            onDismissRequest = { viewingMarkdownTicket = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nota Obsidian Markdown (.md)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { viewingMarkdownTicket = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mdContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Markdown Ticket", mdContent)
                            clipboard.setPrimaryClip(clip)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar MD")
                    }

                    OutlinedButton(
                        onClick = {
                            val file = ticket.markdownPath?.let { File(it) }
                            if (file != null && file.exists()) {
                                context.startActivity(viewModel.vaultManager.createShareIntent(file))
                            } else {
                                // Save and share
                                val tempFile = File(context.cacheDir, "${ticket.id}.md")
                                tempFile.writeText(mdContent)
                                context.startActivity(viewModel.vaultManager.createShareIntent(tempFile))
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartilhar")
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    ticketToDelete?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToDelete = null },
            title = { Text("Excluir Chamado") },
            text = { Text("Deseja realmente remover o chamado '${ticket.id} - ${ticket.tituloResumo}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTicket(ticket)
                        ticketToDelete = null
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ticketToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Responses & Communication History Dialog
    ticketForResponses?.let { ticket ->
        TicketResponsesDialog(
            ticket = ticket,
            commentsFlow = viewModel.getCommentsForTicket(ticket.id),
            onDismiss = { ticketForResponses = null },
            onAddComment = { autorNome, autorTipo, mensagem, imageUri ->
                viewModel.addComment(ticket, autorNome, autorTipo, mensagem, imageUri)
            },
            onRequestCloseTicket = {
                val current = ticket
                ticketForResponses = null
                ticketToClose = current
            },
            onReopenTicket = {
                viewModel.reopenTicket(ticket)
                ticketForResponses = null
            }
        )
    }

    // Close Ticket Dialog (Encerramento de Ocorrência)
    ticketToClose?.let { ticket ->
        CloseTicketDialog(
            ticket = ticket,
            onDismiss = { ticketToClose = null },
            onConfirmClose = { motivo, tecnico, registrarComentario ->
                viewModel.closeTicket(ticket, motivo, tecnico, registrarComentario)
                ticketToClose = null
            }
        )
    }

    // Fullscreen Image Dialog for Ticket
    if (previewingImageDialogPath != null) {
        Dialog(onDismissRequest = { previewingImageDialogPath = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Evidência Visual Anexa",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { previewingImageDialogPath = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 450.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = File(previewingImageDialogPath!!),
                            contentDescription = "Visualização da evidência",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: TicketEntity,
    dateFormat: SimpleDateFormat,
    onViewMarkdown: () -> Unit,
    onResendChat: () -> Unit,
    onDelete: () -> Unit,
    onOpenResponses: () -> Unit,
    onRequestClose: () -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    val priorityColor = when (ticket.prioridade) {
        "Crítica" -> RoseCritical
        "Alta" -> AmberWarning
        "Média" -> Color(0xFFEAB308)
        else -> EmeraldSuccess
    }

    val isClosed = ticket.closedAt != null ||
            ticket.status.equals("Concluído", ignoreCase = true) ||
            ticket.status.equals("Resolvido", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenResponses() }
            .testTag("ticket_card_${ticket.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with ID, Priority, Status, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ticket.id,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ticket.prioridade,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (isClosed) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ticket.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isClosed) EmeraldSuccess else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(ticket.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Title
            Text(
                text = ticket.tituloResumo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Closure Info Box (if closed)
            if (isClosed && (ticket.motivoResolucao.isNotBlank() || ticket.closedAt != null)) {
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Ocorrência Concluída ${if (ticket.closedAt != null) "em " + dateFormat.format(Date(ticket.closedAt)) else ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            if (ticket.resolvidoPor.isNotBlank()) {
                                Text(
                                    text = "• ${ticket.resolvidoPor}",
                                    fontSize = 11.sp,
                                    color = EmeraldSuccess.copy(alpha = 0.9f)
                                )
                            }
                        }
                        if (ticket.motivoResolucao.isNotBlank()) {
                            Text(
                                text = "Motivo: ${ticket.motivoResolucao}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Requester and Project info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "👤 ${ticket.solicitanteNome} (${ticket.canal})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📦 [[${ticket.projeto}]]",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Solution excerpt
            if (ticket.sugestaoSolucao.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Solução: ${ticket.sugestaoSolucao.take(130)}${if (ticket.sugestaoSolucao.length > 130) "..." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Visual Attachment Preview in TicketCard
            if (!ticket.imagePath.isNullOrBlank()) {
                val file = File(ticket.imagePath)
                if (file.exists()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onImageClick(ticket.imagePath) }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Evidência visual",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "📸 Evidência Anexa (Vault)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = { onImageClick(ticket.imagePath) }) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Ampliar foto",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Footer action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Responses & Close Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenResponses,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("ticket_card_responses_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Respostas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (!isClosed) {
                        Button(
                            onClick = onRequestClose,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("ticket_card_close_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Action icons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onViewMarkdown, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Ver Markdown",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onResendChat, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Reenviar Google Chat",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeTab(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val elevation = if (selected) 2.dp else 0.dp

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = elevation,
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

