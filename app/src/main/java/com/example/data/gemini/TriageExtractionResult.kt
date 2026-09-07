package com.example.data.gemini

data class TriageExtractionResult(
    val tituloResumo: String,
    val solicitanteNome: String,
    val solicitanteCanal: String,
    val projeto: String,
    val servicoModulo: String,
    val tipoProblema: String,
    val prioridade: String,
    val justificativaPrioridade: String,
    val descricaoEstruturada: String,
    val sugestaoSolucao: String,
    val tags: List<String>,
    val rawJson: String = ""
)
