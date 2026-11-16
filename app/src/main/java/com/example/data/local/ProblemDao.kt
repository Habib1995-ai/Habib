package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {
    @Query("SELECT * FROM solved_problems ORDER BY timestamp DESC")
    fun getAllSolvedProblems(): Flow<List<ProblemSolutionEntity>>

    @Query("SELECT * FROM solved_problems WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteProblems(): Flow<List<ProblemSolutionEntity>>

    @Query("SELECT * FROM solved_problems WHERE id = :id LIMIT 1")
    suspend fun getProblemById(id: String): ProblemSolutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(entity: ProblemSolutionEntity)

    @Update
    suspend fun updateProblem(entity: ProblemSolutionEntity)

    @Delete
    suspend fun deleteProblem(entity: ProblemSolutionEntity)

    @Query("DELETE FROM solved_problems WHERE id = :id")
    suspend fun deleteProblemById(id: String)

    @Query("UPDATE solved_problems SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("DELETE FROM solved_problems")
    suspend fun clearAll()
}
