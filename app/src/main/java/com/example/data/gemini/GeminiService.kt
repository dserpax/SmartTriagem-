package com.example.data.gemini

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiService"
        // Recommended model for multimodal text and audio triage
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    suspend fun extractTriageFromTextOrAudio(
        textInput: String?,
        audioFile: File?,
        audioMimeType: String = "audio/mp4",
        imageBytes: ByteArray? = null,
        imageMimeType: String = "image/jpeg",
        knownProjects: List<ProjectEntity>,
        customApiKey: String? = null
    ): Result<TriageExtractionResult> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            // Provide intelligent local heuristic fallback if API key is not configured yet
            val fallback = generateLocalHeuristic(textInput ?: "Relato de áudio recebido", knownProjects)
            return@withContext Result.success(fallback)
        }

        try {
            val endpoint = "$BASE_URL$MODEL_NAME:generateContent?key=$apiKey"

            val projectCatalogPrompt = buildString {
                append("CATÁLOGO DE PROJETOS E SERVIÇOS CONHECIDOS DA EMPRESA:\n")
                if (knownProjects.isEmpty()) {
                    append("- Geral / Suporte de TI\n")
                } else {
                    knownProjects.forEach { p ->
                        append("- Projeto: \"${p.nome}\" | Serviços: ${p.servicos} | Termos-chave: ${p.termosChave}\n")
                    }
                }
            }

            val systemInstructionText = """
                Você é o Assistente Especialista de Triagem de Suporte Técnico com IA.
                Sua missão é ouvir ou ler relatos de usuários (enviados informalmente por WhatsApp, telefone, e-mail ou pessoalmente) e organizar rigorosamente uma solicitação estruturada.

                $projectCatalogPrompt

                Regras de Triagem:
                1. Identifique o Projeto correto dentre os projetos conhecidos listados acima. Se for algo novo, crie um nome coerente.
                2. Sugira a Prioridade correta: 'Baixa', 'Média', 'Alta' ou 'Crítica', justificando de acordo com o impacto operacional.
                3. Identifique o Canal de Origem provável (WhatsApp, Telefone, Presencial, Reunião, E-mail, Chat Interno).
                4. Crie um Título Resumo conciso e profissional em português.
                5. Crie uma Descrição Estruturada clara (com Sintoma, Mensagem de Erro se houver e Impacto).
                6. Sugira uma Possível Solução ou diagnóstico inicial técnico para guiar o atendente.
                7. Gere tags relevantes (sem o caractere '#' no JSON, apenas palavras).

                Retorne ESTRITAMENTE um objeto JSON válido no seguinte formato:
                {
                  "titulo_resumo": "string",
                  "solicitante_nome": "string (ou 'Não informado')",
                  "solicitante_canal": "WhatsApp | Telefone | Presencial | Reunião | E-mail | Chat Interno",
                  "projeto": "string",
                  "servico_modulo": "string",
                  "tipo_problema": "Incidente | Bug | Dúvida | Solicitação de Acesso",
                  "prioridade": "Baixa | Média | Alta | Crítica",
                  "justificativa_prioridade": "string",
                  "descricao_estruturada": "string",
                  "sugestao_solucao": "string",
                  "tags": ["tag1", "tag2"]
                }
            """.trimIndent()

            // Build Contents parts
            val partsArray = JSONArray()

            // If audio provided
            if (audioFile != null && audioFile.exists()) {
                val audioBytes = audioFile.readBytes()
                val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                val inlineDataObj = JSONObject().apply {
                    put("mimeType", audioMimeType)
                    put("data", base64Audio)
                }
                partsArray.put(JSONObject().apply {
                    put("inlineData", inlineDataObj)
                })
            }

            // If image provided (screenshot, error photo, equipment photo)
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val inlineDataObj = JSONObject().apply {
                    put("mimeType", imageMimeType)
                    put("data", base64Image)
                }
                partsArray.put(JSONObject().apply {
                    put("inlineData", inlineDataObj)
                })
            }

            // Prompt text
            val userText = when {
                !textInput.isNullOrBlank() && imageBytes != null -> {
                    "Analise o seguinte relato e examine minuciosamente a imagem/print de tela anexada para extrair todas as evidências, telas de erro, códigos e detalhes técnicos:\n\"$textInput\""
                }
                !textInput.isNullOrBlank() -> {
                    "Analise e faça a triagem estruturada do seguinte relato recebido:\n\"$textInput\""
                }
                audioFile != null && imageBytes != null -> {
                    "Por favor, ouça com atenção o áudio anexo e examine minuciosamente a imagem/print anexada para fazer a triagem técnica completa e estruturada."
                }
                audioFile != null -> {
                    "Por favor, ouça com atenção o áudio anexo e faça a triagem técnica completa e estruturada do relato do usuário."
                }
                imageBytes != null -> {
                    "Por favor, examine minuciosamente esta imagem/print de tela de suporte técnico (mensagens de erro, logs, interface do sistema ou foto de equipamento) e realize a triagem técnica completa e estruturada do chamado."
                }
                else -> {
                    "Por favor, realize a triagem técnica de suporte."
                }
            }
            partsArray.put(JSONObject().apply {
                put("text", userText)
            })

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstructionText)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API Error (${response.code}): $responseString")
                return@withContext Result.failure(Exception("Falha na API Gemini (${response.code}): $responseString"))
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textContent = parts?.optJSONObject(0)?.optString("text").orEmpty()

            if (textContent.isBlank()) {
                return@withContext Result.failure(Exception("Resposta vazia da IA"))
            }

            // Parse extracted JSON
            val cleanJson = textContent.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleanJson)

            val tagsList = mutableListOf<String>()
            val tagsArray = parsed.optJSONArray("tags")
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tagsList.add(tagsArray.getString(i).trim())
                }
            }

            val result = TriageExtractionResult(
                tituloResumo = parsed.optString("titulo_resumo", "Solicitação de Suporte"),
                solicitanteNome = parsed.optString("solicitante_nome", "Não informado"),
                solicitanteCanal = parsed.optString("solicitante_canal", "WhatsApp"),
                projeto = parsed.optString("projeto", "Geral"),
                servicoModulo = parsed.optString("servico_modulo", "Geral"),
                tipoProblema = parsed.optString("tipo_problema", "Incidente"),
                prioridade = parsed.optString("prioridade", "Média"),
                justificativaPrioridade = parsed.optString("justificativa_prioridade", "Impacto em rotina de usuário"),
                descricaoEstruturada = parsed.optString("descricao_estruturada", textInput ?: "Relato de áudio processado"),
                sugestaoSolucao = parsed.optString("sugestao_solucao", "Verificar logs do sistema e confirmar credenciais do usuário."),
                tags = tagsList,
                rawJson = cleanJson
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Gemini triage", e)
            Result.failure(e)
        }
    }

    private fun generateLocalHeuristic(text: String, projects: List<ProjectEntity>): TriageExtractionResult {
        val lower = text.lowercase()

        // Match project
        var matchedProject = projects.firstOrNull { p ->
            lower.contains(p.nome.lowercase()) || p.termosChave.split(",").any { term ->
                val t = term.trim().lowercase()
                t.isNotBlank() && lower.contains(t)
            }
        }?.nome ?: "Portal Financeiro"

        // Priority
        val priority = when {
            lower.contains("urgente") || lower.contains("parado") || lower.contains("crítico") || lower.contains("travou tudo") -> "Crítica"
            lower.contains("erro") || lower.contains("falha") || lower.contains("timeout") || lower.contains("não funciona") -> "Alta"
            lower.contains("dúvida") || lower.contains("como faz") || lower.contains("onde fica") -> "Baixa"
            else -> "Média"
        }

        val channel = when {
            lower.contains("zap") || lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("ligou") || lower.contains("telefone") -> "Telefone"
            lower.contains("email") || lower.contains("e-mail") -> "E-mail"
            lower.contains("reunião") -> "Reunião"
            else -> "WhatsApp"
        }

        val tags = mutableListOf<String>()
        if (lower.contains("nfe") || lower.contains("nota")) tags.add("nfe")
        if (lower.contains("timeout") || lower.contains("lento")) tags.add("performance")
        if (lower.contains("login") || lower.contains("senha")) tags.add("autenticacao")
        if (tags.isEmpty()) tags.add("suporte")

        return TriageExtractionResult(
            tituloResumo = if (text.length > 55) text.take(55).trim() + "..." else text.ifBlank { "Solicitação de Suporte Técnico" },
            solicitanteNome = "Usuário",
            solicitanteCanal = channel,
            projeto = matchedProject,
            servicoModulo = "Operacional",
            tipoProblema = if (priority == "Baixa") "Dúvida" else "Incidente",
            prioridade = priority,
            justificativaPrioridade = "Detectado automaticamente pelo relato do usuário",
            descricaoEstruturada = "- **Relato Inicial:** $text\n- **Status:** Aguardando validação do operador.",
            sugestaoSolucao = "1. Verificar permissões do usuário e conectividade com o serviço.\n2. Consultar base de soluções conhecidas.",
            tags = tags,
            rawJson = ""
        )
    }
}
