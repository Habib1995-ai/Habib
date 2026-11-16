package com.example.data.model

data class QuizQuestion(
    val id: String,
    val subject: String, // ریاضی یا فیزیک
    val topic: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val formulaUsed: String,
    val stepByStepExplanation: String
)

data class QuizResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val timeSpentSeconds: Long,
    val percentage: Int,
    val feedbackMessage: String,
    val feedbackTitle: String
)
