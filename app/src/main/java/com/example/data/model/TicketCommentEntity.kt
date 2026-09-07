package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ticket_comments",
    foreignKeys = [
        ForeignKey(
            entity = TicketEntity::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ticketId"])]
)
data class TicketCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ticketId: String,
    val autorNome: String,
    val autorTipo: String = "TECNICO", // "TECNICO", "SOLICITANTE", "SISTEMA"
    val mensagem: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
