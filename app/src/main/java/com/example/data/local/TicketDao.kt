package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: String): TicketEntity?

    @Query("""
        SELECT * FROM tickets 
        WHERE tituloResumo LIKE '%' || :query || '%' 
           OR solicitanteNome LIKE '%' || :query || '%'
           OR projeto LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
           OR relatoOriginal LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTickets(query: String): Flow<List<TicketEntity>>

    @Query("""
        SELECT * FROM tickets 
        WHERE (projeto = :project AND projeto != '') 
           OR tags LIKE '%' || :keyword || '%' 
           OR tituloResumo LIKE '%' || :keyword || '%'
        ORDER BY createdAt DESC LIMIT 5
    """)
    suspend fun findSimilarTickets(project: String, keyword: String): List<TicketEntity>

    @Query("SELECT COUNT(*) FROM tickets")
    fun getTicketsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: TicketEntity)

    @Update
    suspend fun update(ticket: TicketEntity)

    @Delete
    suspend fun delete(ticket: TicketEntity)
}
