package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.KnownSolutionEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TicketCommentEntity
import com.example.data.model.TicketEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TicketEntity::class, ProjectEntity::class, KnownSolutionEntity::class, TicketCommentEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun projectDao(): ProjectDao
    abstract fun knownSolutionDao(): KnownSolutionDao
    abstract fun ticketCommentDao(): TicketCommentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "support_triage_database"
                )
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        private suspend fun seedDatabase(database: AppDatabase) {
            val projectDao = database.projectDao()
            val solutionDao = database.knownSolutionDao()
            val ticketDao = database.ticketDao()

            // Seed projects with services and keywords
            projectDao.insert(
                ProjectEntity(
                    nome = "Portal Financeiro",
                    servicos = "Emissão NFe, Boletos Bancários, Conciliação, DRE",
                    termosChave = "timeout, erro 504, sefaz, xml, lote rejeitado, certificado digital A1, chave de acesso",
                    responsaveis = "Equipe Fiscal / Dev Backend",
                    usageCount = 8
                )
            )
            projectDao.insert(
                ProjectEntity(
                    nome = "App Mobile Logística",
                    servicos = "Rastreamento GPS, Baixa de Entrega, Sincronização Offline, Canhoto Digital",
                    termosChave = "motorista, falha de gps, sincronizar, sem rede, token expirado, erro ao salvar foto",
                    responsaveis = "Time Mobile",
                    usageCount = 5
                )
            )
            projectDao.insert(
                ProjectEntity(
                    nome = "ERP Central & Estoque",
                    servicos = "Controle de Estoque, Pedido de Compra, Cadastro de Produtos, Permissões",
                    termosChave = "saldo negativo, usuário bloqueado, perfil de acesso, lentidão, trava de registro",
                    responsaveis = "Suporte N2",
                    usageCount = 12
                )
            )
            projectDao.insert(
                ProjectEntity(
                    nome = "Infraestrutura & Redes",
                    servicos = "VPN, Acesso Remoto, E-mail Corporativo, Impressoras Térmicas",
                    termosChave = "vpn desconectando, certificado ssl, outlook pedindo senha, impressora zebra",
                    responsaveis = "TI Corporativa",
                    usageCount = 4
                )
            )

            // Seed known solutions
            solutionDao.insert(
                KnownSolutionEntity(
                    sintoma = "Erro 504 Gateway Timeout ao emitir lote de NFe",
                    projeto = "Portal Financeiro",
                    solucao = "1. Consultar disponibilidade do WebService SEFAZ estadual no portal oficial.\n2. Reiniciar o serviço 'faturamento-queue' via painel interno.\n3. Se persistir, aumentar timeout do Gateway para 120s temporariamente.",
                    tags = "nfe, timeout, sefaz, faturamento",
                    sucessos = 6
                )
            )
            solutionDao.insert(
                KnownSolutionEntity(
                    sintoma = "App do motorista não sincroniza entregas offline",
                    projeto = "App Mobile Logística",
                    solucao = "1. Solicitar ao motorista forçar parada do app e limpar cache.\n2. Garantir que o SQLite local não esteja corrompido com erro de constraint.\n3. Reautenticar a sessão para renovar o JWT.",
                    tags = "motorista, sync, offline, jwt",
                    sucessos = 4
                )
            )
            solutionDao.insert(
                KnownSolutionEntity(
                    sintoma = "Usuário bloqueado por tentativas de senha no ERP",
                    projeto = "ERP Central & Estoque",
                    solucao = "Acessar 'Painel de Usuários' > Localizar login > Clicar em 'Desbloquear Acesso' e disparar e-mail de redefinição de senha segura.",
                    tags = "usuario, senha, bloqueio, acesso",
                    sucessos = 9
                )
            )

            // Seed one sample triage ticket
            val now = System.currentTimeMillis()
            ticketDao.insert(
                TicketEntity(
                    id = "TRI-20260906-0001",
                    createdAt = now - (3600 * 1000 * 4),
                    canal = "WhatsApp",
                    solicitanteNome = "Fernanda Ramos",
                    solicitanteInfo = "Financeiro / Filial Campinas",
                    projeto = "Portal Financeiro",
                    servicoModulo = "Emissão NFe",
                    tipoProblema = "Incidente",
                    prioridade = "Alta",
                    justificativaPrioridade = "Impede faturamento de cargas prontas para despacho",
                    tituloResumo = "Lote de notas fiscais travado com erro de timeout SEFAZ",
                    relatoOriginal = "Pessoal, socorro! Desde as 8h não estamos conseguindo emitir nenhuma NFe aqui em Campinas. Clica em emitir e fica rodando até dar erro 504 timeout.",
                    descricaoEstruturada = "- **Sintoma:** Bloqueio total na emissão de NF-e na Filial Campinas.\n- **Mensagem:** Erro HTTP 504 Gateway Timeout.\n- **Impacto:** 14 caminhões retidos no pátio aguardando DANFE para saída.",
                    sugestaoSolucao = "Consultar status SEFAZ SP, reiniciar fila de emissão e reprocessar lote pendente.",
                    tags = "nfe, timeout, sefaz, faturamento, filial-campinas",
                    status = "Enviado Google Chat",
                    webhookSent = true,
                    emailSent = false
                )
            )
        }
    }
}
