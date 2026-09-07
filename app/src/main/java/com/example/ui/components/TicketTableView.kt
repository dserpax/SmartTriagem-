package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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

@Composable
fun TicketTableView(
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
    val horizontalScrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal scrollable container for Table
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(modifier = Modifier.width(1120.dp)) {
                    // Table Header
                    TableHeaderRow()

                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    // Table Rows
                    if (tickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum chamado corresponde aos filtros atuais.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(tickets, key = { _, ticket -> ticket.id }) { index, ticket ->
                                TableDataRow(
                                    ticket = ticket,
                                    isEven = index % 2 == 0,
                                    dateFormat = dateFormat,
                                    onViewMarkdown = { onViewMarkdown(ticket) },
                                    onResendChat = { onResendChat(ticket) },
                                    onDelete = { onDelete(ticket) },
                                    onUpdateStatus = { newStatus -> onUpdateStatus(ticket, newStatus) },
                                    onOpenResponses = { onOpenResponses(ticket) },
                                    onRequestClose = { onRequestClose(ticket) }
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Table Footer Summary
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total de registros: ${tickets.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Deslize horizontalmente para ver todas as colunas →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell(text = "ID Chamado", width = 135.dp)
            HeaderCell(text = "Prioridade", width = 100.dp)
            HeaderCell(text = "Status", width = 135.dp)
            HeaderCell(text = "Projeto", width = 130.dp)
            HeaderCell(text = "Título / Sintoma", width = 230.dp)
            HeaderCell(text = "Solicitante", width = 140.dp)
            HeaderCell(text = "Canal", width = 85.dp)
            HeaderCell(text = "Data", width = 105.dp)
            HeaderCell(text = "Ações", width = 160.dp)
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableDataRow(
    ticket: TicketEntity,
    isEven: Boolean,
    dateFormat: SimpleDateFormat,
    onViewMarkdown: () -> Unit,
    onResendChat: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onOpenResponses: () -> Unit,
    onRequestClose: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    val priorityColor = when (ticket.prioridade) {
        "Crítica" -> RoseCritical
        "Alta" -> AmberWarning
        "Média" -> Color(0xFFEAB308)
        else -> EmeraldSuccess
    }

    val rowBg = if (isEven) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. ID Chamado
        Box(modifier = Modifier.width(135.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = ticket.id,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // 2. Prioridade
        Box(modifier = Modifier.width(100.dp)) {
            Surface(
                color = priorityColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = ticket.prioridade,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 3. Status (com Dropdown para alteração rápida)
        Box(modifier = Modifier.width(135.dp)) {
            val statusColor = when (ticket.status) {
                "Resolvido", "Concluído" -> EmeraldSuccess
                "Em Atendimento", "Em Análise" -> AmberWarning
                "Aguardando", "Aguardando Solicitante" -> Color(0xFF8B5CF6)
                else -> MaterialTheme.colorScheme.primary
            }

            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { statusMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ticket.status.ifBlank { "Triado" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Alterar status",
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = { statusMenuExpanded = false }
            ) {
                listOf("Triado", "Em Atendimento", "Aguardando Solicitante", "Concluído").forEach { statusOption ->
                    DropdownMenuItem(
                        text = { Text(statusOption, fontSize = 12.sp) },
                        onClick = {
                            statusMenuExpanded = false
                            if (statusOption == "Concluído" || statusOption == "Resolvido") {
                                onRequestClose()
                            } else {
                                onUpdateStatus(statusOption)
                            }
                        }
                    )
                }
            }
        }

        // 4. Projeto
        Box(modifier = Modifier.width(130.dp)) {
            Text(
                text = "[[${ticket.projeto}]]",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 5. Título / Sintoma
        Box(modifier = Modifier.width(230.dp)) {
            Text(
                text = ticket.tituloResumo,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 6. Solicitante
        Box(modifier = Modifier.width(140.dp)) {
            Column {
                Text(
                    text = ticket.solicitanteNome,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ticket.solicitanteInfo.isNotBlank()) {
                    Text(
                        text = ticket.solicitanteInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 7. Canal
        Box(modifier = Modifier.width(85.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = ticket.canal,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 8. Data
        Box(modifier = Modifier.width(105.dp)) {
            Text(
                text = dateFormat.format(Date(ticket.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 9. Ações
        Row(
            modifier = Modifier.width(160.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenResponses, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Respostas / Comunicação",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (ticket.closedAt == null && !ticket.status.equals("Concluído", ignoreCase = true)) {
                IconButton(onClick = onRequestClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Encerrar Chamado",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(onClick = onViewMarkdown, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Ver Markdown",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onResendChat, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Reenviar Chat",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
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
