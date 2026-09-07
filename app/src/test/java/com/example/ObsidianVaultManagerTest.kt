package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.markdown.ObsidianVaultManager
import com.example.data.model.ProjectEntity
import com.example.data.model.TicketCommentEntity
import com.example.data.model.TicketEntity
import com.example.data.preferences.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ObsidianVaultManagerTest {

    private lateinit var context: Context
    private lateinit var vaultManager: ObsidianVaultManager
    private lateinit var tempVaultDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        vaultManager = ObsidianVaultManager(context)
        tempVaultDir = File(context.filesDir, "TestVault_${UUID.randomUUID()}").apply { mkdirs() }
    }

    @Test
    fun testGenerateAndParseTicketMarkdown() {
        val originalTicket = TicketEntity(
            id = "TRI-20260906-0001",
            createdAt = 1757160000000L,
            canal = "WhatsApp",
            solicitanteNome = "Carlos Andrade",
            solicitanteInfo = "Financeiro / Ramal 402",
            projeto = "ERP Corporativo",
            servicoModulo = "Emissão de NF-e",
            tipoProblema = "Incidente",
            prioridade = "Alta",
            justificativaPrioridade = "Fechamento contábil mensal impedido",
            tituloResumo = "Erro de rejeição de certificado na NF-e",
            relatoOriginal = "Cliente Carlos disse que ao emitir NF da erro de chave",
            descricaoEstruturada = "Rejeição 280: Certificado transmissor inválido ou revogado.",
            sugestaoSolucao = "Atualizar a cadeia de certificados A1 nas configurações do emissor.",
            tags = "nfe, certificado, financeiro",
            status = "Triado",
            webhookSent = true,
            emailSent = false
        )

        // 1. Generate markdown
        val mdContent = vaultManager.generateMarkdownContent(originalTicket)
        assertTrue(mdContent.contains("id: \"TRI-20260906-0001\""))
        assertTrue(mdContent.contains("project: \"ERP Corporativo\""))
        assertTrue(mdContent.contains("priority: \"Alta\""))
        assertTrue(mdContent.contains("[[ERP Corporativo]]"))

        // 2. Write to temp file and parse back
        val testFile = File(tempVaultDir, "${originalTicket.id}.md").apply {
            writeText(mdContent, Charsets.UTF_8)
        }

        val parsedTicket = vaultManager.parseTicketFromMarkdown(testFile)
        assertNotNull(parsedTicket)
        assertEquals("TRI-20260906-0001", parsedTicket!!.id)
        assertEquals("ERP Corporativo", parsedTicket.projeto)
        assertEquals("Alta", parsedTicket.prioridade)
        assertEquals("Carlos Andrade", parsedTicket.solicitanteNome)
        assertEquals("Erro de rejeição de certificado na NF-e", parsedTicket.tituloResumo)
        assertTrue(parsedTicket.webhookSent)
    }

    @Test
    fun testBackupConfigSaveAndRestore() = runBlocking {
        val settings = AppSettings(context)
        settings.googleChatWebhookUrl = "https://chat.googleapis.com/v1/spaces/SPACE123/messages?key=TEST_KEY"
        settings.customGeminiApiKey = "AIzaSy_CUSTOM_TEST_KEY_999"
        settings.supportEmail = "suporte-dev@empresa.com"
        settings.autoSendWebhook = true

        val saveResult = vaultManager.saveBackupConfig(settings, 15, tempVaultDir.absolutePath)
        assertTrue(saveResult.isSuccess)

        val backupFile = saveResult.getOrThrow()
        assertTrue(backupFile.exists())

        // Parse backup config
        val parsedConfig = vaultManager.parseBackupConfigFile(backupFile)
        assertNotNull(parsedConfig)
        assertEquals("https://chat.googleapis.com/v1/spaces/SPACE123/messages?key=TEST_KEY", parsedConfig!!.googleChatWebhook)
        assertEquals("AIzaSy_CUSTOM_TEST_KEY_999", parsedConfig.customGeminiApiKey)
        assertEquals("suporte-dev@empresa.com", parsedConfig.supportEmail)
        assertTrue(parsedConfig.autoSendWebhook)
        assertEquals(15, parsedConfig.totalTickets)
    }

    @Test
    fun testRestoreDatabaseFromVault() = runBlocking {
        val ticketsDir = File(tempVaultDir, ObsidianVaultManager.TICKETS_FOLDER).apply { mkdirs() }
        val sysDir = File(tempVaultDir, ObsidianVaultManager.SYSTEM_FOLDER).apply { mkdirs() }

        // Setup backup_config
        val settings = AppSettings(context)
        settings.googleChatWebhookUrl = "https://chat.googleapis.com/spaces/ABC"
        vaultManager.saveBackupConfig(settings, 1, tempVaultDir.absolutePath)

        // Setup ticket in Chamados/
        val ticket = TicketEntity(
            id = "TRI-20260906-0042",
            createdAt = System.currentTimeMillis(),
            canal = "E-mail",
            solicitanteNome = "Fernanda",
            solicitanteInfo = "Comercial",
            projeto = "Portal Vendas",
            servicoModulo = "Checkout",
            tipoProblema = "Incidente",
            prioridade = "Crítica",
            justificativaPrioridade = "Clientes não concluem pedido",
            tituloResumo = "Falha no gateway de pagamento",
            relatoOriginal = "Pedidos travados no carrinho",
            descricaoEstruturada = "Timeout na chamada do gateway",
            sugestaoSolucao = "Reiniciar fila de mensagens do gateway",
            tags = "gateway, vendas",
            status = "Triado"
        )
        val ticketFile = File(ticketsDir, "${ticket.id} - ${ticket.tituloResumo}.md")
        ticketFile.writeText(vaultManager.generateMarkdownContent(ticket))

        // Run full vault restore
        val restoreResult = vaultManager.restoreDatabaseFromVault(tempVaultDir.absolutePath)
        assertTrue(restoreResult.isSuccess)

        val restoreData = restoreResult.getOrThrow()
        assertEquals(1, restoreData.tickets.size)
        assertEquals("TRI-20260906-0042", restoreData.tickets[0].id)
        assertEquals("Portal Vendas", restoreData.tickets[0].projeto)
        assertEquals("Crítica", restoreData.tickets[0].prioridade)

        // Verify backup config was restored
        assertNotNull(restoreData.backupConfig)
        assertEquals("https://chat.googleapis.com/spaces/ABC", restoreData.backupConfig?.googleChatWebhook)
        assertTrue(restoreData.report.configRestored)
        assertTrue(restoreData.report.webhookRestored)
    }

    @Test
    fun testTicketWithCommentsAndClosureMarkdown() {
        val closedTicket = TicketEntity(
            id = "TRI-20260906-0099",
            createdAt = 1757160000000L,
            canal = "Google Chat",
            solicitanteNome = "Mariana Silva",
            solicitanteInfo = "RH",
            projeto = "Folha de Pagamento",
            servicoModulo = "Cálculo de Férias",
            tipoProblema = "Dúvida / Ajuste",
            prioridade = "Média",
            justificativaPrioridade = "Processamento semanal",
            tituloResumo = "Divergência de alíquota INSS",
            relatoOriginal = "Usuário informou valor divergente",
            descricaoEstruturada = "Alíquota calculada em 11% em vez de tabela progressiva",
            sugestaoSolucao = "Aplicar atualização das faixas da portaria",
            tags = "rh, inss",
            status = "Concluído",
            closedAt = 1757163600000L,
            motivoResolucao = "Tabela de faixas do INSS atualizada no banco de parâmetros.",
            resolvidoPor = "Técnico Lucas"
        )

        val comments = listOf(
            TicketCommentEntity(
                id = 1L,
                ticketId = closedTicket.id,
                createdAt = 1757161000000L,
                autorNome = "Mariana Silva",
                autorTipo = "Solicitante",
                mensagem = "Enviei a planilha com os contra-cheques em anexo."
            ),
            TicketCommentEntity(
                id = 2L,
                ticketId = closedTicket.id,
                createdAt = 1757162000000L,
                autorNome = "Técnico Lucas",
                autorTipo = "Técnico",
                mensagem = "Recebido! Identificamos que a tabela de 2026 não havia sido aplicada."
            )
        )

        // 1. Generate markdown with comments & closure
        val mdContent = vaultManager.generateMarkdownContent(closedTicket, comments)
        assertTrue(mdContent.contains("status: \"Concluído\""))
        assertTrue(mdContent.contains("closed_at:"))
        assertTrue(mdContent.contains("resolution_reason: \"Tabela de faixas do INSS atualizada no banco de parâmetros.\""))
        assertTrue(mdContent.contains("resolved_by: \"Técnico Lucas\""))
        assertTrue(mdContent.contains("Histórico de Respostas e Atualizações"))
        assertTrue(mdContent.contains("Mariana Silva (Solicitante)"))
        assertTrue(mdContent.contains("Enviei a planilha com os contra-cheques em anexo."))
        assertTrue(mdContent.contains("Resolução e Fechamento"))

        // 2. Parse back from file
        val tempFile = File(tempVaultDir, "${closedTicket.id}.md").apply {
            writeText(mdContent, Charsets.UTF_8)
        }

        val parsedTicket = vaultManager.parseTicketFromMarkdown(tempFile)
        assertNotNull(parsedTicket)
        assertEquals("TRI-20260906-0099", parsedTicket!!.id)
        assertEquals("Concluído", parsedTicket.status)
        assertEquals(closedTicket.closedAt, parsedTicket.closedAt)
        assertEquals("Tabela de faixas do INSS atualizada no banco de parâmetros.", parsedTicket.motivoResolucao)
        assertEquals("Técnico Lucas", parsedTicket.resolvidoPor)

        val parsedComments = vaultManager.parseCommentsFromMarkdown(tempFile, closedTicket.id)
        assertEquals(2, parsedComments.size)
        assertEquals("Mariana Silva", parsedComments[0].autorNome)
        assertEquals("Solicitante", parsedComments[0].autorTipo)
        assertTrue(parsedComments[0].mensagem.contains("Enviei a planilha"))
        assertEquals("Técnico Lucas", parsedComments[1].autorNome)
        assertEquals("Técnico", parsedComments[1].autorTipo)
    }
}
