package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.markdown.ObsidianVaultManager
import com.example.data.model.TicketEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Triagem de Suporte", appName)
    }

    @Test
    fun `test markdown generation with frontmatter for obsidian`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultManager = ObsidianVaultManager(context)

        val ticket = TicketEntity(
            id = "TRI-TEST-001",
            createdAt = 1757160000000L,
            canal = "WhatsApp",
            solicitanteNome = "João Silva",
            solicitanteInfo = "Financeiro",
            projeto = "Portal Financeiro",
            servicoModulo = "Emissão NFe",
            tipoProblema = "Incidente",
            prioridade = "Alta",
            justificativaPrioridade = "Faturamento parado",
            tituloResumo = "Erro 504 ao emitir notas",
            relatoOriginal = "Cliente avisou no WhatsApp que nada emite",
            descricaoEstruturada = "- Sintoma: 504 Timeout",
            sugestaoSolucao = "Reiniciar serviço da fila",
            tags = "nfe, timeout, sefaz"
        )

        val md = vaultManager.generateMarkdownContent(ticket)

        assertTrue("Should contain YAML frontmatter start", md.startsWith("---"))
        assertTrue("Should contain ticket ID in YAML", md.contains("id: \"TRI-TEST-001\""))
        assertTrue("Should contain Obsidian internal wiki-link for project", md.contains("[[Portal Financeiro]]"))
        assertTrue("Should contain tags", md.contains("#nfe"))
    }
}
