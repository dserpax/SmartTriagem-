package com.example.data.webhook

import android.util.Log
import com.example.data.model.TicketEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleChatWebhookService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GoogleChatWebhook"
    }

    suspend fun sendTicketCard(webhookUrl: String, ticket: TicketEntity): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            return@withContext Result.failure(IllegalArgumentException("URL de Webhook inválida ou vazia"))
        }

        try {
            val priorityIcon = when (ticket.prioridade) {
                "Crítica" -> "🔴 CRÍTICA"
                "Alta" -> "🟠 ALTA"
                "Média" -> "🟡 MÉDIA"
                else -> "🟢 BAIXA"
            }

            // Google Chat Cards V2 format
            val widgetsArray = JSONArray()

            // 1. Title & Details
            widgetsArray.put(JSONObject().apply {
                put("decoratedText", JSONObject().apply {
                    put("topLabel", "Título do Chamado")
                    put("text", "<b>${escapeHtml(ticket.tituloResumo)}</b>")
                    put("wrapText", true)
                })
            })

            // 2. Project & Module
            widgetsArray.put(JSONObject().apply {
                put("decoratedText", JSONObject().apply {
                    put("topLabel", "Projeto / Serviço")
                    put("text", "${escapeHtml(ticket.projeto)} • ${escapeHtml(ticket.servicoModulo)}")
                })
            })

            // 3. Requester & Channel
            widgetsArray.put(JSONObject().apply {
                put("decoratedText", JSONObject().apply {
                    put("topLabel", "Solicitante & Canal")
                    put("text", "${escapeHtml(ticket.solicitanteNome)} • Origem: ${escapeHtml(ticket.canal)}")
                })
            })

            // 4. Priority & Type
            widgetsArray.put(JSONObject().apply {
                put("decoratedText", JSONObject().apply {
                    put("topLabel", "Prioridade & Classificação")
                    put("text", "$priorityIcon • ${escapeHtml(ticket.tipoProblema)}")
                })
            })

            // 5. Structured Description
            widgetsArray.put(JSONObject().apply {
                put("textParagraph", JSONObject().apply {
                    put("text", "<b>📋 Detalhamento Estruturado:</b><br/>" + escapeHtml(ticket.descricaoEstruturada).replace("\n", "<br/>"))
                })
            })

            // 6. Suggested Solution
            if (ticket.sugestaoSolucao.isNotBlank()) {
                widgetsArray.put(JSONObject().apply {
                    put("textParagraph", JSONObject().apply {
                        put("text", "<b>💡 Diagnóstico / Solução Sugerida:</b><br/>" + escapeHtml(ticket.sugestaoSolucao).replace("\n", "<br/>"))
                    })
                })
            }

            // 7. Tags
            if (ticket.tags.isNotBlank()) {
                val formattedTags = ticket.tags.split(",").joinToString(" ") { 
                    val t = it.trim().removePrefix("#")
                    if (t.isNotBlank()) "#$t" else ""
                }
                widgetsArray.put(JSONObject().apply {
                    put("textParagraph", JSONObject().apply {
                        put("text", "<i>Tags: $formattedTags</i>")
                    })
                })
            }

            val cardObj = JSONObject().apply {
                put("header", JSONObject().apply {
                    put("title", "🤖 Triagem de Suporte com IA")
                    put("subtitle", "ID: ${ticket.id} • ${ticket.status}")
                    put("imageType", "CIRCLE")
                    put("imageUrl", "https://fonts.gstatic.com/s/i/short-term/release/googlesymbols/support_agent/default/48px.png")
                })
                put("sections", JSONArray().apply {
                    put(JSONObject().apply {
                        put("widgets", widgetsArray)
                    })
                })
            }

            val rootPayload = JSONObject().apply {
                put("cardsV2", JSONArray().apply {
                    put(JSONObject().apply {
                        put("cardId", "ticket_${ticket.id}")
                        put("card", cardObj)
                    })
                })
            }

            val requestBody = rootPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully sent ticket ${ticket.id} to Google Chat")
                Result.success("Enviado para o Google Chat com sucesso!")
            } else {
                Log.w(TAG, "Google Chat Webhook failed (${response.code}): $responseBody")
                Result.failure(Exception("Erro HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting to Google Chat Webhook", e)
            Result.failure(e)
        }
    }

    suspend fun testWebhook(webhookUrl: String): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            return@withContext Result.failure(IllegalArgumentException("URL inválida. Comece com https://chat.googleapis.com/..."))
        }

        try {
            val payload = JSONObject().apply {
                put("text", "🔔 <b>Teste de Conexão:</b> Assistente de Triagem de Suporte com IA conectado com sucesso ao Google Chat!")
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Conexão bem sucedida! Mensagem de teste postada no espaço.")
            } else {
                Result.failure(Exception("Erro ${response.code}: Verifique a URL do webhook."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
