package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey
    val id: String, // e.g. TRI-20260906-0001
    val createdAt: Long = System.currentTimeMillis(),
    val canal: String = "WhatsApp", // WhatsApp, Telefone, Presencial, Reunião, E-mail, Chat Interno
    val solicitanteNome: String = "",
    val solicitanteInfo: String = "",
    val projeto: String = "",
    val servicoModulo: String = "",
    val tipoProblema: String = "Incidente", // Incidente, Bug, Dúvida, Solicitação de Acesso
    val prioridade: String = "Média", // Baixa, Média, Alta, Crítica
    val justificativaPrioridade: String = "",
    val tituloResumo: String = "",
    val relatoOriginal: String = "",
    val descricaoEstruturada: String = "",
    val sugestaoSolucao: String = "",
    val tags: String = "", // Comma-separated or #tags
    val status: String = "Triado", // Triado, Enviado Google Chat, Concluído, Em Atendimento, Aguardando Solicitante
    val closedAt: Long? = null,
    val motivoResolucao: String = "",
    val resolvidoPor: String = "",
    val markdownPath: String? = null,
    val imagePath: String? = null,
    val webhookSent: Boolean = false,
    val emailSent: Boolean = false
)
