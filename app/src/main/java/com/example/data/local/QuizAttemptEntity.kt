package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val subjectFilter: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Int,
    val timeSpentSeconds: Long
)
