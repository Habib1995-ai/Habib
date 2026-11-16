package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.StudyNote

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val title: String,
    val content: String,
    val relatedProblemId: String?,
    val relatedProblemTitle: String?,
    val subjectTag: String,
    val colorIndex: Int
) {
    fun toDomain(): StudyNote {
        return StudyNote(
            id = id,
            timestamp = timestamp,
            title = title,
            content = content,
            relatedProblemId = relatedProblemId,
            relatedProblemTitle = relatedProblemTitle,
            subjectTag = subjectTag,
            colorIndex = colorIndex
        )
    }

    companion object {
        fun fromDomain(domain: StudyNote): StudyNoteEntity {
            return StudyNoteEntity(
                id = domain.id,
                timestamp = domain.timestamp,
                title = domain.title,
                content = domain.content,
                relatedProblemId = domain.relatedProblemId,
                relatedProblemTitle = domain.relatedProblemTitle,
                subjectTag = domain.subjectTag,
                colorIndex = domain.colorIndex
            )
        }
    }
}
