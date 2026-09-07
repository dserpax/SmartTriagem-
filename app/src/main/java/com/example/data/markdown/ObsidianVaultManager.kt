package com.example.data.markdown

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.KnownSolutionEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TicketCommentEntity
import com.example.data.model.TicketEntity
import com.example.data.preferences.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultBackupConfig(
    val googleChatWebhook: String = "",
    val customGeminiApiKey: String = "",
    val supportEmail: String = "",
    val autoSendWebhook: Boolean = false,
    val autoExportMarkdown: Boolean = true,
    val backupDate: String = "",
    val totalTickets: Int = 0
)

data class VaultRestoreReport(
    val success: Boolean,
    val vaultPath: String,
    val ticketsRestored: Int,
    val projectsRestored: Int,
    val solutionsRestored: Int,
    val configRestored: Boolean,
    val webhookRestored: Boolean,
    val message: String,
    val details: List<String> = emptyList()
)

data class VaultRestoreData(
    val tickets: List<TicketEntity>,
    val projects: List<ProjectEntity>,
    val solutions: List<KnownSolutionEntity>,
    val backupConfig: VaultBackupConfig?,
    val report: VaultRestoreReport,
    val comments: List<TicketCommentEntity> = emptyList()
)

class ObsidianVaultManager(private val context: Context) {

    companion object {
        const val DEFAULT_VAULT_FOLDER = "ObsidianVault"
        const val TICKETS_FOLDER = "Chamados"
        const val PROJECTS_FOLDER = "Projetos"
        const val KNOWLEDGE_FOLDER = "Base de Conhecimento"
        const val SYSTEM_FOLDER = "_Sistema"
        const val BACKUP_CONFIG_FILE = "backup_config.md"
        const val MOC_FILE = "_Indice_Geral_MOC.md"
        const val README_FILE = "README_OBSIDIAN.md"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Directory resolution
    fun getVaultRootDirectory(customPath: String? = null): File {
        val root = if (!customPath.isNullOrBlank()) {
            val file = File(customPath)
            // If the user specified path ending in 'Chamados', use parent as vault root
            if (file.name == TICKETS_FOLDER && file.parentFile != null) {
                file.parentFile!!
            } else {
                file
            }
        } else {
            context.getExternalFilesDir(DEFAULT_VAULT_FOLDER) ?: File(context.filesDir, DEFAULT_VAULT_FOLDER)
        }
        if (!root.exists()) {
            root.mkdirs()
        }
        ensureVaultStructure(root)
        return root
    }

    fun getTicketsDirectory(customPath: String? = null): File {
        val root = getVaultRootDirectory(customPath)
        val dir = File(root, TICKETS_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getProjectsDirectory(customPath: String? = null): File {
        val root = getVaultRootDirectory(customPath)
        val dir = File(root, PROJECTS_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getKnowledgeDirectory(customPath: String? = null): File {
        val root = getVaultRootDirectory(customPath)
        val dir = File(root, KNOWLEDGE_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSystemDirectory(customPath: String? = null): File {
        val root = getVaultRootDirectory(customPath)
        val dir = File(root, SYSTEM_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // Backwards-compatible method that returns the tickets directory for existing callers
    fun getVaultDirectory(customPath: String? = null): File {
        return getTicketsDirectory(customPath)
    }

    private fun ensureVaultStructure(vaultRoot: File) {
        File(vaultRoot, TICKETS_FOLDER).mkdirs()
        File(vaultRoot, PROJECTS_FOLDER).mkdirs()
        File(vaultRoot, KNOWLEDGE_FOLDER).mkdirs()
        File(vaultRoot, SYSTEM_FOLDER).mkdirs()
        ensureReadmeObsidian(vaultRoot)
    }

    // --- Markdown Generation ---

    fun generateMarkdownContent(ticket: TicketEntity, comments: List<TicketCommentEntity> = emptyList()): String {
        val dateStr = dateFormat.format(Date(ticket.createdAt))
        val dateIso = dateOnlyFormat.format(Date(ticket.createdAt))
        val priorityEmoji = when (ticket.prioridade) {
            "Crítica" -> "🔴"
            "Alta" -> "🟠"
            "Média" -> "🟡"
            else -> "🟢"
        }

        val tagList = ticket.tags.split(",")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }

        val yamlTags = if (tagList.isNotEmpty()) {
            tagList.joinToString("\n") { "  - $it" }
        } else {
            "  - suporte\n  - triagem"
        }

        val inlineTags = tagList.joinToString(" ") { "#$it" }
        val projectLink = if (ticket.projeto.isNotBlank()) "[[${ticket.projeto}]]" else "[[Geral]]"

        val isClosed = ticket.closedAt != null ||
                ticket.status.equals("Concluído", ignoreCase = true) ||
                ticket.status.equals("Fechado", ignoreCase = true) ||
                ticket.status.equals("Resolvido", ignoreCase = true)

        return buildString {
            appendLine("---")
            appendLine("type: \"chamado\"")
            appendLine("id: \"${ticket.id}\"")
            appendLine("title: \"${escapeYaml(ticket.tituloResumo)}\"")
            appendLine("date: \"$dateStr\"")
            appendLine("date_iso: \"$dateIso\"")
            appendLine("project: \"${escapeYaml(ticket.projeto)}\"")
            appendLine("service: \"${escapeYaml(ticket.servicoModulo)}\"")
            appendLine("priority: \"${ticket.prioridade}\"")
            appendLine("type_issue: \"${ticket.tipoProblema}\"")
            appendLine("requester: \"${escapeYaml(ticket.solicitanteNome)}\"")
            appendLine("requester_info: \"${escapeYaml(ticket.solicitanteInfo)}\"")
            appendLine("channel: \"${ticket.canal}\"")
            appendLine("status: \"${ticket.status}\"")
            if (isClosed) {
                val closeDateStr = ticket.closedAt?.let { dateFormat.format(Date(it)) } ?: dateStr
                appendLine("closed_at: \"$closeDateStr\"")
                ticket.closedAt?.let { appendLine("closed_at_ms: $it") }
                if (ticket.motivoResolucao.isNotBlank()) {
                    appendLine("resolution_reason: \"${escapeYaml(ticket.motivoResolucao)}\"")
                }
                if (ticket.resolvidoPor.isNotBlank()) {
                    appendLine("resolved_by: \"${escapeYaml(ticket.resolvidoPor)}\"")
                }
            }
            if (ticket.imagePath != null && ticket.imagePath.isNotBlank()) {
                appendLine("image_attachment: \"${escapeYaml(ticket.imagePath)}\"")
            }
            appendLine("webhook_sent: ${ticket.webhookSent}")
            appendLine("email_sent: ${ticket.emailSent}")
            appendLine("created_at_ms: ${ticket.createdAt}")
            appendLine("tags:")
            appendLine(yamlTags)
            appendLine("---")
            appendLine()
            appendLine("# ${ticket.id} - ${ticket.tituloResumo}")
            appendLine()
            appendLine("> **Status:** `${ticket.status}` | **Prioridade:** $priorityEmoji **${ticket.prioridade}** | **Data:** $dateStr")
            appendLine()

            if (ticket.imagePath != null && ticket.imagePath.isNotBlank()) {
                val imgFileName = File(ticket.imagePath).name
                appendLine("## 📸 Evidência Visual / Anexo")
                appendLine("![[$imgFileName]]")
                appendLine()
            }

            appendLine("## 📌 Metadados da Solicitação")
            appendLine("- **Solicitante:** ${ticket.solicitanteNome.ifBlank { "Não informado" }}")
            if (ticket.solicitanteInfo.isNotBlank()) {
                appendLine("- **Origem / Info:** ${ticket.solicitanteInfo}")
            }
            appendLine("- **Canal de Entrada:** ${ticket.canal}")
            appendLine("- **Projeto:** $projectLink")
            appendLine("- **Serviço / Módulo:** ${ticket.servicoModulo.ifBlank { "Geral" }}")
            appendLine("- **Tipo de Problema:** ${ticket.tipoProblema}")
            if (ticket.justificativaPrioridade.isNotBlank()) {
                appendLine("- **Justificativa da Prioridade:** ${ticket.justificativaPrioridade}")
            }
            appendLine()
            appendLine("## 📋 Descrição Estruturada")
            appendLine(ticket.descricaoEstruturada.ifBlank { "_Sem detalhamento estruturado._" })
            appendLine()
            appendLine("## 💡 Diagnóstico e Solução Sugerida")
            appendLine(ticket.sugestaoSolucao.ifBlank { "_Sem sugestão cadastrada._" })
            appendLine()

            if (isClosed) {
                val closeDateStr = ticket.closedAt?.let { dateFormat.format(Date(it)) } ?: dateStr
                appendLine("## 🏁 Resolução e Fechamento")
                appendLine("- **Status Final:** ${ticket.status}")
                appendLine("- **Data de Fechamento:** $closeDateStr")
                if (ticket.resolvidoPor.isNotBlank()) {
                    appendLine("- **Encerrado por:** ${ticket.resolvidoPor}")
                }
                appendLine("- **Motivo da Resolução:** ${ticket.motivoResolucao.ifBlank { "Ocorrência resolvida com sucesso." }}")
                appendLine()
            }

            // Histórico de Respostas e Atualizações
            appendLine("## 💬 Histórico de Respostas e Atualizações")
            if (comments.isNotEmpty()) {
                comments.forEach { c ->
                    val cDate = dateFormat.format(Date(c.createdAt))
                    appendLine("- **[$cDate] ${c.autorNome} (${c.autorTipo}):**")
                    c.mensagem.lines().forEach { line ->
                        appendLine("  $line")
                    }
                    if (!c.imagePath.isNullOrBlank()) {
                        appendLine("  ![[${File(c.imagePath).name}]]")
                    }
                }
            } else {
                appendLine("_Nenhum comentário ou atualização registrado ainda._")
            }
            appendLine()

            if (ticket.relatoOriginal.isNotBlank()) {
                appendLine("## 💬 Relato Original Bruto")
                appendLine("```text")
                appendLine(ticket.relatoOriginal.trim())
                appendLine("```")
                appendLine()
            }
            appendLine("## 🏷️ Tags e Conexões")
            appendLine(if (inlineTags.isNotBlank()) inlineTags else "#suporte #triagem")
            appendLine()
            appendLine("---")
            appendLine("_Gerado e sincronizado pelo Assistente de Triagem de Suporte com IA_")
        }
    }

    fun generateProjectMarkdown(project: ProjectEntity): String {
        return buildString {
            appendLine("---")
            appendLine("type: \"projeto\"")
            appendLine("name: \"${escapeYaml(project.nome)}\"")
            appendLine("services: \"${escapeYaml(project.servicos)}\"")
            appendLine("keywords: \"${escapeYaml(project.termosChave)}\"")
            appendLine("responsaveis: \"${escapeYaml(project.responsaveis)}\"")
            appendLine("webhook_url: \"${escapeYaml(project.webhookUrl)}\"")
            appendLine("usage_count: ${project.usageCount}")
            appendLine("tags:")
            appendLine("  - projeto")
            appendLine("  - suporte")
            appendLine("---")
            appendLine()
            appendLine("# 📂 Projeto: ${project.nome}")
            appendLine()
            appendLine("> Registro da base de conhecimento e catálogo de suporte.")
            appendLine()
            appendLine("## ℹ️ Informações Gerais")
            appendLine("- **Módulos / Serviços:** ${project.servicos.ifBlank { "Geral" }}")
            appendLine("- **Termos-Chave e Jargões:** ${project.termosChave.ifBlank { "Nenhum termo cadastrado" }}")
            appendLine("- **Responsáveis:** ${project.responsaveis.ifBlank { "Não informado" }}")
            appendLine("- **Total de Chamados Triados:** ${project.usageCount}")
            if (project.webhookUrl.isNotBlank()) {
                appendLine("- **Webhook Dedicado:** `${project.webhookUrl}`")
            }
            appendLine()
            appendLine("## 📑 Chamados Vinculados a este Projeto (Dataview)")
            appendLine("```dataview")
            appendLine("TABLE file.link as \"Chamado\", priority as \"Prioridade\", status as \"Status\", date as \"Data\", requester as \"Solicitante\"")
            appendLine("FROM \"$TICKETS_FOLDER\"")
            appendLine("WHERE project = \"${project.nome}\"")
            appendLine("SORT date desc")
            appendLine("```")
            appendLine()
            appendLine("---")
            appendLine("_Nota sincronizada com o Assistente de Triagem_")
        }
    }

    fun generateSolutionMarkdown(solution: KnownSolutionEntity): String {
        return buildString {
            appendLine("---")
            appendLine("type: \"solucao_conhecida\"")
            appendLine("symptom: \"${escapeYaml(solution.sintoma)}\"")
            appendLine("project: \"${escapeYaml(solution.projeto)}\"")
            appendLine("success_count: ${solution.sucessos}")
            appendLine("tags:")
            appendLine("  - solucao")
            appendLine("  - base_conhecimento")
            appendLine("---")
            appendLine()
            appendLine("# 💡 Solução: ${solution.sintoma}")
            appendLine()
            appendLine("- **Projeto Vinculado:** [[Projetos/${solution.projeto}|${solution.projeto}]]")
            appendLine("- **Aplicações com Sucesso:** ${solution.sucessos}")
            if (solution.tags.isNotBlank()) {
                appendLine("- **Tags:** ${solution.tags}")
            }
            appendLine()
            appendLine("## 🛠️ Procedimento / Resolução")
            appendLine(solution.solucao)
            appendLine()
            appendLine("---")
            appendLine("_Solução da base de conhecimento do suporte_")
        }
    }

    fun generateBackupConfigMarkdown(settings: AppSettings, totalTickets: Int): String {
        val now = dateFormat.format(Date())
        return buildString {
            appendLine("---")
            appendLine("type: \"backup_config\"")
            appendLine("app: \"Assistente de Triagem de Suporte com IA\"")
            appendLine("version: \"1.0\"")
            appendLine("backup_date: \"$now\"")
            appendLine("google_chat_webhook: \"${escapeYaml(settings.googleChatWebhookUrl)}\"")
            appendLine("custom_gemini_api_key: \"${escapeYaml(settings.customGeminiApiKey)}\"")
            appendLine("support_email: \"${escapeYaml(settings.supportEmail)}\"")
            appendLine("auto_send_webhook: ${settings.autoSendWebhook}")
            appendLine("auto_export_markdown: ${settings.autoExportMarkdown}")
            appendLine("total_tickets: $totalTickets")
            appendLine("tags:")
            appendLine("  - sistema")
            appendLine("  - backup")
            appendLine("  - configuracoes")
            appendLine("---")
            appendLine()
            appendLine("# 🔐 Backup de Configurações e Chaves do Sistema")
            appendLine()
            appendLine("> **AVISO IMPORTANTE:** Este arquivo mantém o espelho das chaves de integração e preferências do Assistente de Triagem.")
            appendLine("> Ao selecionar ou apontar este Vault no aplicativo, **todas as chaves e o banco de dados são restaurados automaticamente!**")
            appendLine()
            appendLine("## 📋 Configurações Espelhadas")
            appendLine("- **Google Chat Webhook:** ${if (settings.googleChatWebhookUrl.isNotBlank()) "Configurado (`${settings.googleChatWebhookUrl.take(35)}...`)" else "_Não configurado_"}")
            appendLine("- **E-mail de Suporte:** `${settings.supportEmail}`")
            appendLine("- **Chave Personalizada Gemini:** ${if (settings.customGeminiApiKey.isNotBlank()) "Chave cadastrada no backup" else "_Usando padrão da plataforma_"}")
            appendLine("- **Despacho Automático Chat:** `${settings.autoSendWebhook}`")
            appendLine("- **Data do Último Backup:** `$now`")
            appendLine("- **Total de Chamados Sincronizados:** `$totalTickets`")
            appendLine()
            appendLine("---")
            appendLine("_Arquivo gerado automaticamente pelo aplicativo de Triagem. Compatível com Obsidian Vault._")
        }
    }

    private fun generateReadmeObsidianMarkdown(): String {
        return buildString {
            appendLine("# 🗄️ Obsidian Vault - Banco de Dados de Triagem de Suporte")
            appendLine()
            appendLine("Este diretório é um **Vault completo e autossuficiente do Obsidian** e funciona como o **banco de dados primário** do aplicativo **Assistente de Triagem de Suporte com IA**.")
            appendLine()
            appendLine("## 📁 Estrutura de Pastas e Organização")
            appendLine()
            appendLine("- **`📁 Chamados/`**: Contém uma nota `.md` para cada chamado triado. Cada nota inclui **YAML Frontmatter** completo (`id`, `date`, `priority`, `project`, `requester`, `status`, `tags`) e links bidirecionais wiki-style.")
            appendLine("- **`📁 Projetos/`**: Notas de cada sistema/projeto com catálogo de módulos, termos e query Dataview para ver todas as demandas vinculadas.")
            appendLine("- **`📁 Base de Conhecimento/`**: Procedimentos e soluções catalogadas pela IA e pelo operador.")
            appendLine("- **`📁 _Sistema/`**:")
            appendLine("  - **`backup_config.md`**: Backup seguro de chaves (Webhook do Google Chat, e-mail de suporte, preferências). Permite restauração instantânea em qualquer aparelho.")
            appendLine("  - **`_Indice_Geral_MOC.md`**: Map of Content (MOC) geral para navegação no Obsidian com tabelas e suporte a Dataview.")
            appendLine()
            appendLine("## 🚀 Como usar no Obsidian")
            appendLine("1. Abra o Obsidian no Desktop ou Android.")
            appendLine("2. Clique em **'Abrir pasta existente como cofre (Open folder as vault)'** e selecione esta pasta raiz.")
            appendLine("3. Use a **Graph View** para visualizar a teia de relações entre chamados, projetos e solicitantes.")
            appendLine("4. (Opcional) Instale o plugin comunitário **Dataview** para tabelas dinâmicas automáticas!")
            appendLine()
            appendLine("## 🔄 Restauração Automática")
            appendLine("Ao abrir o aplicativo em um novo aparelho e selecionar este Vault, o aplicativo lê todos os arquivos `.md`, restaura os chamados, recria os projetos e restabelece suas chaves de webhook e configurações.")
        }
    }

    private fun ensureReadmeObsidian(vaultRoot: File) {
        try {
            val sysDir = File(vaultRoot, SYSTEM_FOLDER)
            val readmeFile = File(sysDir, README_FILE)
            if (!readmeFile.exists()) {
                readmeFile.writeText(generateReadmeObsidianMarkdown(), Charsets.UTF_8)
            }
        } catch (ignored: Exception) {}
    }

    // --- File Saving Methods ---

    suspend fun saveTicketToVault(
        ticket: TicketEntity,
        customVaultPath: String? = null,
        comments: List<TicketCommentEntity> = emptyList()
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            val ticketsDir = File(root, TICKETS_FOLDER).apply { mkdirs() }
            val fileName = sanitizeFileName("${ticket.id} - ${ticket.tituloResumo.take(40)}") + ".md"
            val file = File(ticketsDir, fileName)

            val markdownContent = generateMarkdownContent(ticket, comments)
            file.writeText(markdownContent, Charsets.UTF_8)

            // Update MOC
            updateVaultIndex(root)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBackupConfig(settings: AppSettings, totalTickets: Int, customVaultPath: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            val sysDir = File(root, SYSTEM_FOLDER).apply { mkdirs() }
            val file = File(sysDir, BACKUP_CONFIG_FILE)

            val content = generateBackupConfigMarkdown(settings, totalTickets)
            file.writeText(content, Charsets.UTF_8)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProjectToVault(project: ProjectEntity, customVaultPath: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            val projDir = File(root, PROJECTS_FOLDER).apply { mkdirs() }
            val fileName = sanitizeFileName(project.nome) + ".md"
            val file = File(projDir, fileName)

            val content = generateProjectMarkdown(project)
            file.writeText(content, Charsets.UTF_8)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSolutionToVault(solution: KnownSolutionEntity, customVaultPath: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            val knDir = File(root, KNOWLEDGE_FOLDER).apply { mkdirs() }
            val fileName = sanitizeFileName("Solucao - ${solution.sintoma.take(35)}") + ".md"
            val file = File(knDir, fileName)

            val content = generateSolutionMarkdown(solution)
            file.writeText(content, Charsets.UTF_8)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportAllTickets(tickets: List<TicketEntity>, customVaultPath: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            val ticketsDir = File(root, TICKETS_FOLDER).apply { mkdirs() }
            var count = 0
            tickets.forEach { ticket ->
                val fileName = sanitizeFileName("${ticket.id} - ${ticket.tituloResumo.take(40)}") + ".md"
                val file = File(ticketsDir, fileName)
                file.writeText(generateMarkdownContent(ticket), Charsets.UTF_8)
                count++
            }
            updateVaultIndex(root)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Full export & sync of all entities and settings to vault
    suspend fun syncFullDatabaseToVault(
        tickets: List<TicketEntity>,
        projects: List<ProjectEntity>,
        solutions: List<KnownSolutionEntity>,
        settings: AppSettings,
        customVaultPath: String? = null
    ): Result<VaultRestoreReport> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            ensureVaultStructure(root)

            // 1. Export tickets
            val ticketsDir = File(root, TICKETS_FOLDER).apply { mkdirs() }
            tickets.forEach { ticket ->
                val fileName = sanitizeFileName("${ticket.id} - ${ticket.tituloResumo.take(40)}") + ".md"
                File(ticketsDir, fileName).writeText(generateMarkdownContent(ticket), Charsets.UTF_8)
            }

            // 2. Export projects
            val projDir = File(root, PROJECTS_FOLDER).apply { mkdirs() }
            projects.forEach { proj ->
                val fileName = sanitizeFileName(proj.nome) + ".md"
                File(projDir, fileName).writeText(generateProjectMarkdown(proj), Charsets.UTF_8)
            }

            // 3. Export solutions
            val solDir = File(root, KNOWLEDGE_FOLDER).apply { mkdirs() }
            solutions.forEach { sol ->
                val fileName = sanitizeFileName("Solucao - ${sol.sintoma.take(35)}") + ".md"
                File(solDir, fileName).writeText(generateSolutionMarkdown(sol), Charsets.UTF_8)
            }

            // 4. Save backup config
            saveBackupConfig(settings, tickets.size, customVaultPath)

            // 5. Update MOC
            updateVaultIndex(root)

            val report = VaultRestoreReport(
                success = true,
                vaultPath = root.absolutePath,
                ticketsRestored = tickets.size,
                projectsRestored = projects.size,
                solutionsRestored = solutions.size,
                configRestored = true,
                webhookRestored = settings.googleChatWebhookUrl.isNotBlank(),
                message = "Vault sincronizado com sucesso: ${tickets.size} chamados, ${projects.size} projetos e chaves de backup gravadas em _Sistema/backup_config.md."
            )
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateVaultIndex(vaultRoot: File) {
        try {
            val ticketsDir = File(vaultRoot, TICKETS_FOLDER)
            val indexFile = File(vaultRoot, MOC_FILE)
            val sysIndexFile = File(File(vaultRoot, SYSTEM_FOLDER), MOC_FILE)

            val files = (ticketsDir.listFiles { _, name -> name.endsWith(".md") && !name.startsWith("_") } ?: emptyArray())
                .sortedByDescending { it.lastModified() }

            val content = buildString {
                appendLine("---")
                appendLine("type: \"moc\"")
                appendLine("title: \"Índice Geral de Chamados de Suporte (MOC)\"")
                appendLine("date: \"${dateFormat.format(Date())}\"")
                appendLine("total_tickets: ${files.size}")
                appendLine("tags:")
                appendLine("  - moc")
                appendLine("  - indice")
                appendLine("  - suporte")
                appendLine("---")
                appendLine()
                appendLine("# 🗂️ Map of Content (MOC) - Central de Suporte")
                appendLine()
                appendLine("> Total de chamados no Vault: **${files.size}** | Atualizado em: ${dateFormat.format(Date())}")
                appendLine()
                appendLine("## 🚀 Navegação Rápida no Obsidian")
                appendLine("- 📂 [[$PROJECTS_FOLDER|Ver Catálogo de Projetos]]")
                appendLine("- 💡 [[$KNOWLEDGE_FOLDER|Base de Conhecimento e Soluções]]")
                appendLine("- 🔐 [[$SYSTEM_FOLDER/$BACKUP_CONFIG_FILE|Backup de Chaves e Configurações]]")
                appendLine("- 📖 [[$SYSTEM_FOLDER/$README_FILE|Instruções de Organização do Vault]]")
                appendLine()
                appendLine("## 📊 Tabela Dataview (Plugins Comunitários)")
                appendLine("```dataview")
                appendLine("TABLE file.link as \"Chamado\", project as \"Projeto\", priority as \"Prioridade\", status as \"Status\", date as \"Data\"")
                appendLine("FROM \"$TICKETS_FOLDER\"")
                appendLine("SORT date desc")
                appendLine("```")
                appendLine()
                appendLine("## 📑 Lista de Chamados (Markdown Nativo)")
                if (files.isEmpty()) {
                    appendLine("_Nenhum chamado salvo ainda no vault._")
                } else {
                    files.forEach { f ->
                        val noteName = f.nameWithoutExtension
                        appendLine("- [[$TICKETS_FOLDER/$noteName|$noteName]]")
                    }
                }
            }
            indexFile.writeText(content, Charsets.UTF_8)
            sysIndexFile.writeText(content, Charsets.UTF_8)
        } catch (ignored: Exception) {
            // Best effort
        }
    }

    // --- RESTORATION AND PARSING METHODS ---

    /**
     * Scans the vault directory, extracts:
     * 1. Backup configuration & keys from _Sistema/backup_config.md
     * 2. All ticket notes from Chamados/ (and any root notes starting with TRI-)
     * 3. Projects from Projetos/
     * 4. Solutions from Base de Conhecimento/
     */
    suspend fun restoreDatabaseFromVault(customVaultPath: String? = null): Result<VaultRestoreData> = withContext(Dispatchers.IO) {
        try {
            val root = getVaultRootDirectory(customVaultPath)
            val details = mutableListOf<String>()

            // 1. Restore Backup Config
            val backupConfig = findAndParseBackupConfig(root)
            if (backupConfig != null) {
                details.add("Arquivo de backup de configurações localizado em _Sistema/$BACKUP_CONFIG_FILE.")
            } else {
                details.add("Aviso: arquivo _Sistema/$BACKUP_CONFIG_FILE não encontrado. Configurações mantidas.")
            }

            // 2. Restore Tickets and Comments
            val tickets = mutableListOf<TicketEntity>()
            val comments = mutableListOf<TicketCommentEntity>()
            val ticketsDir = File(root, TICKETS_FOLDER)
            val filesToScan = mutableListOf<File>()

            if (ticketsDir.exists()) {
                ticketsDir.listFiles { _, name -> name.endsWith(".md") && !name.startsWith("_") }?.let {
                    filesToScan.addAll(it)
                }
            }

            // Also check root folder in case user stored notes there or has TRI- files
            root.listFiles { _, name -> name.endsWith(".md") && name.startsWith("TRI-") }?.let {
                filesToScan.addAll(it)
            }

            filesToScan.forEach { file ->
                val ticket = parseTicketFromMarkdown(file)
                if (ticket != null) {
                    tickets.add(ticket)
                    val ticketComments = parseCommentsFromMarkdown(file, ticket.id)
                    comments.addAll(ticketComments)
                }
            }
            details.add("${tickets.size} notas de chamados recuperadas com metadados YAML.")
            if (comments.isNotEmpty()) {
                details.add("${comments.size} respostas e atualizações de histórico recuperadas.")
            }

            // 3. Restore Projects
            val projects = mutableListOf<ProjectEntity>()
            val projDir = File(root, PROJECTS_FOLDER)
            if (projDir.exists()) {
                projDir.listFiles { _, name -> name.endsWith(".md") && !name.startsWith("_") }?.forEach { file ->
                    val proj = parseProjectFromMarkdown(file)
                    if (proj != null) {
                        projects.add(proj)
                    }
                }
            }

            // If projects were not found as separate notes, extract distinct projects from tickets
            val knownProjNames = projects.map { it.nome.lowercase() }.toSet()
            tickets.map { it.projeto }.filter { it.isNotBlank() && it.lowercase() !in knownProjNames }.distinct().forEach { projName ->
                projects.add(
                    ProjectEntity(
                        nome = projName,
                        servicos = tickets.firstOrNull { it.projeto == projName }?.servicoModulo ?: "Geral",
                        termosChave = tickets.filter { it.projeto == projName }.flatMap { it.tags.split(",") }.map { it.trim() }.distinct().take(5).joinToString(", "),
                        responsaveis = "Equipe $projName",
                        usageCount = tickets.count { it.projeto == projName }
                    )
                )
            }
            details.add("${projects.size} projetos restaurados na base de conhecimento.")

            // 4. Restore Solutions
            val solutions = mutableListOf<KnownSolutionEntity>()
            val knDir = File(root, KNOWLEDGE_FOLDER)
            if (knDir.exists()) {
                knDir.listFiles { _, name -> name.endsWith(".md") && !name.startsWith("_") }?.forEach { file ->
                    val sol = parseSolutionFromMarkdown(file)
                    if (sol != null) {
                        solutions.add(sol)
                    }
                }
            }

            // Extract solutions from tickets that have suggestions if not in separate notes
            tickets.filter { it.sugestaoSolucao.isNotBlank() && it.sugestaoSolucao.length > 20 }.take(15).forEach { t ->
                if (solutions.none { it.sintoma == t.tituloResumo }) {
                    solutions.add(
                        KnownSolutionEntity(
                            sintoma = t.tituloResumo,
                            projeto = t.projeto,
                            solucao = t.sugestaoSolucao,
                            tags = t.tags,
                            sucessos = 1
                        )
                    )
                }
            }
            details.add("${solutions.size} soluções conhecidas restauradas.")

            val report = VaultRestoreReport(
                success = true,
                vaultPath = root.absolutePath,
                ticketsRestored = tickets.size,
                projectsRestored = projects.size,
                solutionsRestored = solutions.size,
                configRestored = backupConfig != null,
                webhookRestored = !backupConfig?.googleChatWebhook.isNullOrBlank(),
                message = "Vault restaurado com sucesso! ${tickets.size} chamados, ${projects.size} projetos e chaves recuperadas.",
                details = details
            )

            Result.success(VaultRestoreData(tickets, projects, solutions, backupConfig, report, comments))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findAndParseBackupConfig(vaultRoot: File): VaultBackupConfig? {
        val candidates = listOf(
            File(File(vaultRoot, SYSTEM_FOLDER), BACKUP_CONFIG_FILE),
            File(vaultRoot, BACKUP_CONFIG_FILE),
            File(File(vaultRoot, SYSTEM_FOLDER), "config.md"),
            File(vaultRoot, "_config.md")
        )
        for (file in candidates) {
            if (file.exists() && file.isFile) {
                val parsed = parseBackupConfigFile(file)
                if (parsed != null) return parsed
            }
        }
        return null
    }

    fun parseBackupConfigFile(file: File): VaultBackupConfig? {
        try {
            val text = file.readText(Charsets.UTF_8)
            val yamlProps = extractYamlProperties(text)
            if (yamlProps.isEmpty() && !text.contains("google_chat_webhook")) return null

            val webhook = yamlProps["google_chat_webhook"] ?: extractPropertyRegex(text, "google_chat_webhook")
            val geminiKey = yamlProps["custom_gemini_api_key"] ?: extractPropertyRegex(text, "custom_gemini_api_key")
            val email = yamlProps["support_email"] ?: extractPropertyRegex(text, "support_email")
            val autoWebhook = (yamlProps["auto_send_webhook"] ?: "false").toBoolean()
            val autoExport = (yamlProps["auto_export_markdown"] ?: "true").toBoolean()
            val date = yamlProps["backup_date"] ?: ""
            val totalTickets = (yamlProps["total_tickets"] ?: "0").toIntOrNull() ?: 0

            return VaultBackupConfig(
                googleChatWebhook = webhook.trim(),
                customGeminiApiKey = geminiKey.trim(),
                supportEmail = email.trim().ifBlank { "suporte@empresa.com" },
                autoSendWebhook = autoWebhook,
                autoExportMarkdown = autoExport,
                backupDate = date,
                totalTickets = totalTickets
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parseTicketFromMarkdown(file: File): TicketEntity? {
        try {
            val text = file.readText(Charsets.UTF_8)
            val yamlProps = extractYamlProperties(text)

            // Extract ID: from YAML, or from filename (e.g. TRI-20260906-0001), or from title
            var id = yamlProps["id"] ?: ""
            if (id.isBlank()) {
                val match = Regex("(TRI-\\d{4,8}-\\d+)").find(file.name)
                    ?: Regex("(TRI-\\d{4,8}-\\d+)").find(text)
                if (match != null) {
                    id = match.value
                }
            }

            // If still no ID and doesn't look like a support ticket, ignore
            if (id.isBlank() && !text.contains("suporte", ignoreCase = true) && !text.contains("triagem", ignoreCase = true)) {
                return null
            }
            if (id.isBlank()) {
                id = "TRI-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(file.lastModified()))}-${(1000..9999).random()}"
            }

            // Extract title
            val title = yamlProps["title"]
                ?: extractFirstHeader(text)
                ?: file.nameWithoutExtension.removePrefix(id).trim(' ', '-', '_')

            // Extract dates
            val createdAtMs = yamlProps["created_at_ms"]?.toLongOrNull()
                ?: parseDateStringToMs(yamlProps["date"] ?: yamlProps["date_iso"])
                ?: file.lastModified()

            // Extract fields
            val canal = yamlProps["channel"]
                ?: extractSectionContent(text, "- **Canal de Entrada:**")
                ?: "WhatsApp"

            val solicitanteNome = yamlProps["requester"]
                ?: extractSectionContent(text, "- **Solicitante:**")
                ?: ""

            val solicitanteInfo = yamlProps["requester_info"]
                ?: extractSectionContent(text, "- **Origem / Info:**")
                ?: ""

            val projetoRaw = yamlProps["project"]
                ?: extractWikiLink(text, "- **Projeto:**")
                ?: ""
            val projeto = cleanWikiLink(projetoRaw).ifBlank { "Geral" }

            val servicoModulo = yamlProps["service"]
                ?: extractSectionContent(text, "- **Serviço / Módulo:**")
                ?: "Geral"

            val tipoProblema = yamlProps["type_issue"] ?: yamlProps["type"]
                ?: extractSectionContent(text, "- **Tipo de Problema:**")
                ?: "Incidente"

            val prioridadeRaw = yamlProps["priority"]
                ?: extractPriorityFromBody(text)
                ?: "Média"
            val prioridade = normalizePriority(prioridadeRaw)

            val justificativaPrioridade = yamlProps["priority_reason"]
                ?: extractSectionContent(text, "- **Justificativa da Prioridade:**")
                ?: ""

            val status = yamlProps["status"] ?: "Triado"
            val webhookSent = (yamlProps["webhook_sent"] ?: "false").toBoolean()
            val emailSent = (yamlProps["email_sent"] ?: "false").toBoolean()

            // Closure fields
            val closedAtMs = yamlProps["closed_at_ms"]?.toLongOrNull()
                ?: parseDateStringToMs(yamlProps["closed_at"])
                ?: parseDateStringToMs(extractSectionContent(text, "- **Data de Fechamento:**"))

            val motivoResolucao = yamlProps["resolution_reason"]
                ?: extractSectionContent(text, "- **Motivo da Resolução:**")
                ?: ""

            val resolvidoPor = yamlProps["resolved_by"]
                ?: extractSectionContent(text, "- **Encerrado por:**")
                ?: ""

            // Extract tags
            val tagsFromYaml = extractYamlTags(text)
            val inlineTags = extractHashtags(text)
            val combinedTags = (tagsFromYaml + inlineTags).distinct().joinToString(", ")

            // Extract body sections
            val descricaoEstruturada = extractMarkdownSection(text, "## 📋 Descrição Estruturada")
                ?: extractMarkdownSection(text, "## Descrição")
                ?: ""

            val sugestaoSolucao = extractMarkdownSection(text, "## 💡 Diagnóstico e Solução Sugerida")
                ?: extractMarkdownSection(text, "## Diagnóstico")
                ?: ""

            val relatoOriginal = extractCodeBlock(text, "## 💬 Relato Original Bruto")
                ?: extractMarkdownSection(text, "## Relato Original")
                ?: ""

            val imagePath = yamlProps["image_attachment"]
                ?: extractWikiLink(text, "## 📸 Evidência Visual")
                ?: extractWikiLink(text, "![[")

            return TicketEntity(
                id = id,
                createdAt = createdAtMs,
                canal = canal.trim(),
                solicitanteNome = solicitanteNome.trim(),
                solicitanteInfo = solicitanteInfo.trim(),
                projeto = projeto.trim(),
                servicoModulo = servicoModulo.trim(),
                tipoProblema = tipoProblema.trim(),
                prioridade = prioridade,
                justificativaPrioridade = justificativaPrioridade.trim(),
                tituloResumo = title.trim(),
                relatoOriginal = relatoOriginal.trim(),
                descricaoEstruturada = descricaoEstruturada.trim(),
                sugestaoSolucao = sugestaoSolucao.trim(),
                tags = combinedTags,
                status = status.trim(),
                closedAt = closedAtMs,
                motivoResolucao = motivoResolucao.trim(),
                resolvidoPor = resolvidoPor.trim(),
                markdownPath = file.absolutePath,
                imagePath = imagePath?.trim(),
                webhookSent = webhookSent,
                emailSent = emailSent
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parseCommentsFromMarkdown(file: File, ticketId: String): List<TicketCommentEntity> {
        try {
            val text = file.readText(Charsets.UTF_8)
            val section = extractMarkdownSection(text, "## 💬 Histórico de Respostas e Atualizações")
                ?: extractMarkdownSection(text, "## Histórico de Respostas")
                ?: return emptyList()

            val comments = mutableListOf<TicketCommentEntity>()
            val regex = Regex("""-\s*\*\*\[(.*?)\]\s*(.*?)\s*\((.*?)\):\*\*\s*([\s\S]*?)(?=(?:-\s*\*\*\[|\Z))""")
            val matches = regex.findAll(section)

            for (match in matches) {
                val dateStr = match.groupValues[1].trim()
                val autorNome = match.groupValues[2].trim()
                val autorTipo = match.groupValues[3].trim()
                val rawMsg = match.groupValues[4]
                val mensagem = rawMsg.lines()
                    .map { it.trim().removePrefix("  ") }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .trim()

                if (mensagem.isNotBlank() && !mensagem.startsWith("_Nenhum comentário")) {
                    val createdAt = parseDateStringToMs(dateStr) ?: file.lastModified()
                    comments.add(
                        TicketCommentEntity(
                            ticketId = ticketId,
                            autorNome = autorNome.ifBlank { "Técnico Suporte" },
                            autorTipo = autorTipo.ifBlank { "TECNICO" },
                            mensagem = mensagem,
                            createdAt = createdAt
                        )
                    )
                }
            }
            return comments
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun parseProjectFromMarkdown(file: File): ProjectEntity? {
        try {
            val text = file.readText(Charsets.UTF_8)
            val yamlProps = extractYamlProperties(text)

            val name = yamlProps["name"]
                ?: file.nameWithoutExtension.trim()
            if (name.isBlank() || name.startsWith("_")) return null

            val services = yamlProps["services"]
                ?: extractSectionContent(text, "- **Módulos / Serviços:**")
                ?: ""

            val keywords = yamlProps["keywords"]
                ?: extractSectionContent(text, "- **Termos-Chave e Jargões:**")
                ?: ""

            val responsaveis = yamlProps["responsaveis"]
                ?: extractSectionContent(text, "- **Responsáveis:**")
                ?: ""

            val webhookUrl = yamlProps["webhook_url"] ?: ""
            val usageCount = (yamlProps["usage_count"] ?: "0").toIntOrNull() ?: 0

            return ProjectEntity(
                nome = name,
                servicos = services,
                termosChave = keywords,
                responsaveis = responsaveis,
                webhookUrl = webhookUrl,
                usageCount = usageCount
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parseSolutionFromMarkdown(file: File): KnownSolutionEntity? {
        try {
            val text = file.readText(Charsets.UTF_8)
            val yamlProps = extractYamlProperties(text)

            val symptom = yamlProps["symptom"]
                ?: file.nameWithoutExtension.removePrefix("Solucao - ").trim()
            if (symptom.isBlank() || symptom.startsWith("_")) return null

            val projectRaw = yamlProps["project"]
                ?: extractWikiLink(text, "- **Projeto Vinculado:**")
                ?: "Geral"
            val project = cleanWikiLink(projectRaw)

            val solucao = extractMarkdownSection(text, "## 🛠️ Procedimento / Resolução")
                ?: extractMarkdownSection(text, "## Resolução")
                ?: ""

            val successCount = (yamlProps["success_count"] ?: "1").toIntOrNull() ?: 1
            val tags = extractYamlTags(text).joinToString(", ")

            return KnownSolutionEntity(
                sintoma = symptom,
                projeto = project,
                solucao = solucao,
                tags = tags,
                sucessos = successCount
            )
        } catch (e: Exception) {
            return null
        }
    }

    // --- Helper Parsers ---

    private fun extractYamlProperties(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = text.lines()
        if (lines.isEmpty() || lines.first().trim() != "---") return result

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line == "---") break
            if (line.startsWith("#") || line.isBlank()) continue

            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                var value = line.substring(colonIndex + 1).trim()
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length - 1)
                }
                result[key] = value
            }
        }
        return result
    }

    private fun extractYamlTags(text: String): List<String> {
        val tags = mutableListOf<String>()
        val lines = text.lines()
        var insideTags = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "---" && insideTags) break
            if (trimmed == "tags:") {
                insideTags = true
                continue
            }
            if (insideTags) {
                if (trimmed.startsWith("-")) {
                    val tag = trimmed.removePrefix("-").trim().removePrefix("#")
                    if (tag.isNotBlank()) tags.add(tag)
                } else if (!trimmed.startsWith("#") && trimmed.isNotBlank() && !trimmed.startsWith("-")) {
                    // Next property started
                    insideTags = false
                }
            }
        }
        return tags
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#([a-zA-Z0-9_\\-]+)")
        return regex.findAll(text).map { it.groupValues[1] }.filter { it.isNotBlank() }.toList()
    }

    private fun extractPropertyRegex(text: String, propName: String): String {
        val match = Regex("""$propName\s*:\s*"?([^"\n\r]+)"?""").find(text)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractFirstHeader(text: String): String? {
        val match = Regex("""^#\s+(.+)$""", RegexOption.MULTILINE).find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractSectionContent(text: String, prefix: String): String? {
        val index = text.indexOf(prefix)
        if (index < 0) return null
        val after = text.substring(index + prefix.length)
        val endOfLine = after.indexOf('\n')
        val line = if (endOfLine >= 0) after.substring(0, endOfLine) else after
        return line.trim()
    }

    private fun extractMarkdownSection(text: String, sectionHeader: String): String? {
        val index = text.indexOf(sectionHeader)
        if (index < 0) return null
        val after = text.substring(index + sectionHeader.length).trimStart('\r', '\n')
        val nextHeader = Regex("""\n##+\s+""").find(after)
        val content = if (nextHeader != null) {
            after.substring(0, nextHeader.range.first)
        } else {
            val endSep = after.indexOf("\n---")
            if (endSep >= 0) after.substring(0, endSep) else after
        }
        return content.trim()
    }

    private fun extractCodeBlock(text: String, sectionHeader: String): String? {
        val section = extractMarkdownSection(text, sectionHeader) ?: return null
        val startBlock = section.indexOf("```")
        if (startBlock < 0) return section.trim()
        val afterStart = section.substring(startBlock + 3)
        val startCode = afterStart.indexOf('\n')
        val codeWithoutHeader = if (startCode >= 0) afterStart.substring(startCode + 1) else afterStart
        val endBlock = codeWithoutHeader.indexOf("```")
        return if (endBlock >= 0) codeWithoutHeader.substring(0, endBlock).trim() else codeWithoutHeader.trim()
    }

    private fun extractWikiLink(text: String, prefix: String): String? {
        val content = extractSectionContent(text, prefix) ?: return null
        val match = Regex("""\[\[(.*?)\]\]""").find(content)
        return match?.groupValues?.get(1) ?: content
    }

    private fun cleanWikiLink(link: String): String {
        var clean = link.removePrefix("[[").removeSuffix("]]")
        if (clean.contains('|')) {
            clean = clean.substringAfter('|')
        }
        if (clean.contains('/')) {
            clean = clean.substringAfterLast('/')
        }
        return clean.trim()
    }

    private fun extractPriorityFromBody(text: String): String? {
        return when {
            text.contains("Crítica", ignoreCase = true) || text.contains("Critica", ignoreCase = true) || text.contains("🔴") -> "Crítica"
            text.contains("Alta", ignoreCase = true) || text.contains("🟠") -> "Alta"
            text.contains("Baixa", ignoreCase = true) || text.contains("🟢") -> "Baixa"
            text.contains("Média", ignoreCase = true) || text.contains("Media", ignoreCase = true) || text.contains("🟡") -> "Média"
            else -> null
        }
    }

    private fun normalizePriority(value: String): String {
        val lower = value.lowercase()
        return when {
            lower.contains("crít") || lower.contains("crit") -> "Crítica"
            lower.contains("alt") -> "Alta"
            lower.contains("baix") -> "Baixa"
            else -> "Média"
        }
    }

    private fun parseDateStringToMs(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            dateFormat.parse(dateStr)?.time
                ?: dateOnlyFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun createShareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun escapeYaml(text: String): String {
        return text.replace("\"", "\\\"").replace("\n", " ")
    }
}
