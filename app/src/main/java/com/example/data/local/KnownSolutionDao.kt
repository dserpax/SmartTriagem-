package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.KnownSolutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnownSolutionDao {
    @Query("SELECT * FROM known_solutions ORDER BY sucessos DESC")
    fun getAllSolutions(): Flow<List<KnownSolutionEntity>>

    @Query("SELECT * FROM known_solutions WHERE LOWER(projeto) = LOWER(:project) OR tags LIKE '%' || :tag || '%' LIMIT 4")
    suspend fun findSolutions(project: String, tag: String): List<KnownSolutionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(solution: KnownSolutionEntity)

    @Update
    suspend fun update(solution: KnownSolutionEntity)

    @Delete
    suspend fun delete(solution: KnownSolutionEntity)
}
