package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "known_solutions")
data class KnownSolutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sintoma: String,
    val projeto: String,
    val solucao: String,
    val tags: String = "",
    val sucessos: Int = 1
)
