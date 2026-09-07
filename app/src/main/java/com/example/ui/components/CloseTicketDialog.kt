package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TicketEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloseTicketDialog(
    ticket: TicketEntity,
    onDismiss: () -> Unit,
    onConfirmClose: (motivoResolucao: String, resolvidoPor: String, registrarComentario: Boolean) -> Unit
) {
    var motivoResolucao by remember { mutableStateOf(ticket.sugestaoSolucao.ifBlank { "" }) }
    var resolvidoPor by remember { mutableStateOf(ticket.resolvidoPor.ifBlank { "Técnico Suporte" }) }
    var registrarComentario by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }

    val dataHoraFormatada = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Encerramento do Chamado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${ticket.id} - ${ticket.tituloResumo}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Solicitante: ${ticket.solicitanteNome.ifBlank { "Não informado" }} • Projeto: ${ticket.projeto}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Data/Hora de Encerramento: $dataHoraFormatada",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Responsável pelo encerramento
                OutlinedTextField(
                    value = resolvidoPor,
                    onValueChange = { resolvidoPor = it },
                    label = { Text("Técnico / Responsável pelo Encerramento *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_ticket_technician_input")
                )

                // Motivo da Resolução (Obrigatório)
                OutlinedTextField(
                    value = motivoResolucao,
                    onValueChange = {
                        motivoResolucao = it
                        if (it.isNotBlank()) showError = false
                    },
                    label = { Text("Motivo da Resolução / Procedimento Adotado *") },
                    placeholder = { Text("Descreva detalhadamente como o problema foi resolvido ou a justificativa do fechamento...") },
                    minLines = 3,
                    maxLines = 6,
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                text = "O motivo da resolução é obrigatório para encerrar o chamado.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Este motivo será registrado no arquivo Markdown do Obsidian Vault e no banco de dados.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_ticket_reason_input")
                )

                // Checkbox para registrar no histórico de respostas
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = registrarComentario,
                        onCheckedChange = { registrarComentario = it },
                        modifier = Modifier.testTag("close_ticket_comment_checkbox")
                    )
                    Text(
                        text = "Registrar encerramento no histórico de comunicação do chamado",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (motivoResolucao.isBlank()) {
                        showError = true
                    } else {
                        onConfirmClose(motivoResolucao, resolvidoPor, registrarComentario)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("confirm_close_ticket_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Concluir Chamado")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_close_ticket_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
