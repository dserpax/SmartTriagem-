package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TicketEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseCritical
import java.text.SimpleDateFormat
import java.util.Date

enum class KanbanColumnType(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val canonicalStatus: String
) {
    TRIADOS(
        title = "Triados / Novos",
        icon = Icons.Default.Inbox,
        accentColor = Color(0xFF2563EB),
        canonicalStatus = "Triado"
    ),
    EM_ATENDIMENTO(
        title = "Em Atendimento",
        icon = Icons.Default.Build,
        accentColor = Color(0xFFD97706),
        canonicalStatus = "Em Atendimento"
    ),
    AGUARDANDO(
        title = "Aguardando",
        icon = Icons.Default.HourglassEmpty,
        accentColor = Color(0xFF7C3AED),
        canonicalStatus = "Aguardando Solicitante"
    ),
    RESOLVIDO(
        title = "Resolvido",
        icon = Icons.Default.CheckCircle,
        accentColor = Color(0xFF059669),
        canonicalStatus = "Resolvido"
    );

    fun matches(status: String): Boolean {
        return when (this) {
            TRIADOS -> status.equals("Triado", ignoreCase = true) ||
                    status.equals("Novo", ignoreCase = true) ||
                    status.contains("Google Chat", ignoreCase = true) ||
                    status.isBlank()
            EM_ATENDIMENTO -> status.contains("Atendimento", ignoreCase = true) ||
                    status.contains("Análise", ignoreCase = true) ||
                    status.contains("Andamento", ignoreCase = true)
            AGUARDANDO -> status.contains("Aguardando", ignoreCase = true) ||
                    status.contains("Bloqueado", ignoreCase = true) ||
                    status.contains("Pendente", ignoreCase = true)
            RESOLVIDO -> status.contains("Resolvido", ignoreCase = true) ||
                    status.contains("Concluído", ignoreCase = true) ||
                    status.contains("Fechado", ignoreCase = true)
        }
    }
}

@Composable
fun TicketKanbanView(
    tickets: List<TicketEntity>,
    dateFormat: SimpleDateFormat,
    onViewMarkdown: (TicketEntity) -> Unit,
    onResendChat: (TicketEntity) -> Unit,
    onDelete: (TicketEntity) -> Unit,
    onUpdateStatus: (TicketEntity, String) -> Unit,
    onOpenResponses: (TicketEntity) -> Unit = {},
    onRequestClose: (TicketEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Group tickets by Kanban columns
    val columns = remember(tickets) {
        KanbanColumnType.entries.map { col ->
            val matchingTickets = tickets.filter { ticket ->
                // Check if matches this column, or if doesn't match any other and this is TRIADOS
                if (col.matches(ticket.status)) {
                    true
                } else if (col == KanbanColumnType.TRIADOS) {
                    KanbanColumnType.entries.none { other -> other != KanbanColumnType.TRIADOS && other.matches(ticket.status) }
                } else {
                    false
                }
            }
            col to matchingTickets
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        columns.forEachIndexed { index, (colType, colTickets) ->
            KanbanColumn(
                columnType = colType,
                tickets = colTickets,
                dateFormat = dateFormat,
                columnIndex = index,
                totalColumns = columns.size,
                onMovePrevious = { ticket ->
                    val prevCol = columns.getOrNull(index - 1)?.first
                    if (prevCol != null) {
                        onUpdateStatus(ticket, prevCol.canonicalStatus)
                    }
                },
                onMoveNext = { ticket ->
                    val nextCol = columns.getOrNull(index + 1)?.first
                    if (nextCol != null) {
                        if (nextCol == KanbanColumnType.RESOLVIDO && ticket.closedAt == null) {
                            onRequestClose(ticket)
                        } else {
                            onUpdateStatus(ticket, nextCol.canonicalStatus)
                        }
                    }
                },
                onViewMarkdown = onViewMarkdown,
                onResendChat = onResendChat,
                onDelete = onDelete,
                onUpdateStatus = onUpdateStatus,
                onOpenResponses = onOpenResponses,
                onRequestClose = onRequestClose
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    columnType: KanbanColumnType,
    tickets: List<TicketEntity>,
    dateFormat: SimpleDateFormat,
    columnIndex: Int,
    totalColumns: Int,
    onMovePrevious: (TicketEntity) -> Unit,
    onMoveNext: (TicketEntity) -> Unit,
    onViewMarkdown: (TicketEntity) -> Unit,
    onResendChat: (TicketEntity) -> Unit,
    onDelete: (TicketEntity) -> Unit,
    onUpdateStatus: (TicketEntity, String) -> Unit,
    onOpenResponses: (TicketEntity) -> Unit,
    onRequestClose: (TicketEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Column Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(columnType.accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = columnType.icon,
                                contentDescription = null,
                                tint = columnType.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = columnType.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Count Badge
                    Surface(
                        color = columnType.accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${tickets.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = columnType.accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Cards in this column
            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = columnType.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Nenhum chamado nesta etapa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tickets, key = { it.id }) { ticket ->
                        KanbanCard(
                            ticket = ticket,
                            dateFormat = dateFormat,
                            canMovePrevious = columnIndex > 0,
                            canMoveNext = columnIndex < totalColumns - 1,
                            onMovePrevious = { onMovePrevious(ticket) },
                            onMoveNext = { onMoveNext(ticket) },
                            onViewMarkdown = { onViewMarkdown(ticket) },
                            onResendChat = { onResendChat(ticket) },
                            onDelete = { onDelete(ticket) },
                            onUpdateStatus = { newStatus -> onUpdateStatus(ticket, newStatus) },
                            onOpenResponses = { onOpenResponses(ticket) },
                            onRequestClose = { onRequestClose(ticket) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanCard(
    ticket: TicketEntity,
    dateFormat: SimpleDateFormat,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onMovePrevious: () -> Unit,
    onMoveNext: () -> Unit,
    onViewMarkdown: () -> Unit,
    onResendChat: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onOpenResponses: () -> Unit,
    onRequestClose: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val priorityColor = when (ticket.prioridade) {
        "Crítica" -> RoseCritical
        "Alta" -> AmberWarning
        "Média" -> Color(0xFFEAB308)
        else -> EmeraldSuccess
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenResponses() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: ID + Priority + More options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = ticket.id,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = ticket.prioridade,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opções",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Respostas & Histórico", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onOpenResponses()
                            }
                        )
                        if (ticket.closedAt == null && !ticket.status.equals("Concluído", ignoreCase = true)) {
                            DropdownMenuItem(
                                text = { Text("Encerrar Ocorrência", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldSuccess) },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onRequestClose()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Ver Nota .MD", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onViewMarkdown()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reenviar Google Chat", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onResendChat()
                            }
                        )
                        HorizontalDivider()
                        Text(
                            text = "Mover para etapa:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        KanbanColumnType.entries.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.title, fontSize = 11.sp) },
                                onClick = {
                                    menuExpanded = false
                                    if (col == KanbanColumnType.RESOLVIDO && ticket.closedAt == null) {
                                        onRequestClose()
                                    } else {
                                        onUpdateStatus(col.canonicalStatus)
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Excluir chamado", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Title
            Text(
                text = ticket.tituloResumo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Requester and Project
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${ticket.solicitanteNome}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "[[${ticket.projeto}]]",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick navigation between Kanban columns
            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move Back button
                if (canMovePrevious) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onMovePrevious() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Recuar etapa",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Voltar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Date label
                Text(
                    text = dateFormat.format(Date(ticket.createdAt)),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // Move Next button
                if (canMoveNext) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onMoveNext() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Avançar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Avançar etapa",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}
