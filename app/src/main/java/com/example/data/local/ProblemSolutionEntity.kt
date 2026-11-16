package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ConceptCheckQuestion
import com.example.data.model.ProblemSolution
import com.example.data.model.Scene3DJsonCodec
import com.example.data.model.SolutionStep
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "solved_problems")
data class ProblemSolutionEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val subject: String,
    val topic: String,
    val identifiedProblem: String,
    val finalAnswer: String,
    val formulasJson: String,
    val givenDataJson: String,
    val stepsJson: String,
    val studentTip: String,
    val conceptQuestionJson: String? = null,
    val scene3dJson: String? = null,
    val imageUri: String?,
    val isFavorite: Boolean = false
) {
    fun toDomain(): ProblemSolution {
        val formulas = mutableListOf<String>()
        try {
            val arr = JSONArray(formulasJson)
            for (i in 0 until arr.length()) formulas.add(arr.getString(i))
        } catch (_: Exception) {}

        val given = mutableListOf<String>()
        try {
            val arr = JSONArray(givenDataJson)
            for (i in 0 until arr.length()) given.add(arr.getString(i))
        } catch (_: Exception) {}

        val steps = mutableListOf<SolutionStep>()
        try {
            val arr = JSONArray(stepsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                steps.add(
                    SolutionStep(
                        stepNumber = obj.optInt("stepNumber", i + 1),
                        title = obj.optString("title", ""),
                        explanation = obj.optString("explanation", ""),
                        calculation = obj.optString("calculation", ""),
                        visual3DNote = obj.optString("visual3DNote", "")
                    )
                )
            }
        } catch (_: Exception) {}

        var conceptQuestion: ConceptCheckQuestion? = null
        if (!conceptQuestionJson.isNullOrBlank()) {
            try {
                val cObj = JSONObject(conceptQuestionJson)
                val qText = cObj.optString("question", "")
                val optArr = cObj.optJSONArray("options")
                val opts = mutableListOf<String>()
                if (optArr != null) {
                    for (i in 0 until optArr.length()) opts.add(optArr.getString(i))
                }
                val cIndex = cObj.optInt("correctOptionIndex", 0)
                val expl = cObj.optString("explanation", "")
                if (qText.isNotBlank() && opts.isNotEmpty()) {
                    conceptQuestion = ConceptCheckQuestion(qText, opts, cIndex, expl)
                }
            } catch (_: Exception) {}
        }

        val scene3d = Scene3DJsonCodec.fromJsonString(scene3dJson)

        return ProblemSolution(
            id = id,
            timestamp = timestamp,
            subject = subject,
            topic = topic,
            identifiedProblem = identifiedProblem,
            finalAnswer = finalAnswer,
            formulasUsed = formulas,
            givenData = given,
            steps = steps,
            studentTip = studentTip,
            conceptQuestion = conceptQuestion,
            scene3d = scene3d,
            imageUri = imageUri,
            isFavorite = isFavorite
        )
    }

    companion object {
        fun fromDomain(domain: ProblemSolution): ProblemSolutionEntity {
            val formulasArr = JSONArray()
            domain.formulasUsed.forEach { formulasArr.put(it) }

            val givenArr = JSONArray()
            domain.givenData.forEach { givenArr.put(it) }

            val stepsArr = JSONArray()
            domain.steps.forEach { step ->
                val obj = JSONObject()
                obj.put("stepNumber", step.stepNumber)
                obj.put("title", step.title)
                obj.put("explanation", step.explanation)
                obj.put("calculation", step.calculation)
                obj.put("visual3DNote", step.visual3DNote)
                stepsArr.put(obj)
            }

            var cJson: String? = null
            domain.conceptQuestion?.let { cq ->
                val cObj = JSONObject()
                cObj.put("question", cq.question)
                val optArr = JSONArray()
                cq.options.forEach { optArr.put(it) }
                cObj.put("options", optArr)
                cObj.put("correctOptionIndex", cq.correctOptionIndex)
                cObj.put("explanation", cq.explanation)
                cJson = cObj.toString()
            }

            val sJson = domain.scene3d?.let { Scene3DJsonCodec.toJsonString(it) }

            return ProblemSolutionEntity(
                id = domain.id,
                timestamp = domain.timestamp,
                subject = domain.subject,
                topic = domain.topic,
                identifiedProblem = domain.identifiedProblem,
                finalAnswer = domain.finalAnswer,
                formulasJson = formulasArr.toString(),
                givenDataJson = givenArr.toString(),
                stepsJson = stepsArr.toString(),
                studentTip = domain.studentTip,
                conceptQuestionJson = cJson,
                scene3dJson = sJson,
                imageUri = domain.imageUri,
                isFavorite = domain.isFavorite
            )
        }
    }
}
