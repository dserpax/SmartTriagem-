package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val servicos: String = "", // Comma-separated modules/services
    val termosChave: String = "", // Terms, jargon, acronyms
    val responsaveis: String = "",
    val webhookUrl: String = "", // Custom webhook for this project (optional)
    val usageCount: Int = 0
)
