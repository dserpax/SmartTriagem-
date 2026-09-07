package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TicketCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketCommentDao {
    @Query("SELECT * FROM ticket_comments WHERE ticketId = :ticketId ORDER BY createdAt ASC")
    fun getCommentsForTicket(ticketId: String): Flow<List<TicketCommentEntity>>

    @Query("SELECT * FROM ticket_comments WHERE ticketId = :ticketId ORDER BY createdAt ASC")
    suspend fun getCommentsForTicketList(ticketId: String): List<TicketCommentEntity>

    @Query("SELECT * FROM ticket_comments ORDER BY createdAt ASC")
    suspend fun getAllComments(): List<TicketCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: TicketCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<TicketCommentEntity>)

    @Update
    suspend fun update(comment: TicketCommentEntity)

    @Delete
    suspend fun delete(comment: TicketCommentEntity)

    @Query("DELETE FROM ticket_comments WHERE ticketId = :ticketId")
    suspend fun deleteByTicketId(ticketId: String)
}
