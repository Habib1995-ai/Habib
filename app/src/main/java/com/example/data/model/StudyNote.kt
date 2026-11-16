package com.example.data.model

import java.util.UUID

data class StudyNote(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val content: String,
    val relatedProblemId: String? = null,
    val relatedProblemTitle: String? = null,
    val subjectTag: String = "عمومی", // ریاضی، فیزیک، نکته، فرمول، عمومی
    val colorIndex: Int = 0
)
