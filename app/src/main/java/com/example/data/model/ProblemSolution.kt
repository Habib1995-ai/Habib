package com.example.data.model

import java.util.UUID

enum class Scene3DType {
    ASTRONOMY_ASTROPHYSICS,
    GEOMETRY_SOLID,
    CHEMISTRY_MOLECULE,
    PHYSICS_MECHANICS,
    MATH_SURFACE_3D
}

data class Node3D(
    val id: Int = 0,
    val label: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val radius: Float = 0.25f,
    val colorHex: String = "#3B82F6",
    val orbitRadius: Float = 0f,
    val orbitSpeed: Float = 0f,
    val orbitTiltDeg: Float = 0f
)

data class Edge3D(
    val fromIndex: Int = 0,
    val toIndex: Int = 0,
    val label: String = "",
    val isVector: Boolean = false,
    val isDashed: Boolean = false,
    val bondOrder: Int = 1,
    val colorHex: String = "#94A3B8"
)

data class Step3DFocus(
    val stepNumber: Int = 1,
    val visualExplanation: String = "",
    val focusNodeIndices: List<Int> = emptyList(),
    val focusEdgeIndices: List<Int> = emptyList(),
    val suggestedYawDeg: Float = 35f,
    val suggestedPitchDeg: Float = 22f
)

data class Scene3DSpec(
    val sceneType: Scene3DType,
    val shapeVariant: String,
    val title: String,
    val spatialSummary: String = "",
    val interactionHint: String = "با یک انگشت بچرخانید و با دو انگشت بزرگ‌نمایی کنید",
    val formulaLabel: String = "",
    val nodes: List<Node3D> = emptyList(),
    val edges: List<Edge3D> = emptyList(),
    val stepFocuses: List<Step3DFocus> = emptyList()
)

data class SolutionStep(
    val stepNumber: Int,
    val title: String,
    val explanation: String,
    val calculation: String,
    val visual3DNote: String = ""
)

data class ConceptCheckQuestion(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class ProblemSolution(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val subject: String,
    val topic: String,
    val identifiedProblem: String,
    val finalAnswer: String,
    val formulasUsed: List<String> = emptyList(),
    val givenData: List<String> = emptyList(),
    val steps: List<SolutionStep> = emptyList(),
    val studentTip: String = "",
    val conceptQuestion: ConceptCheckQuestion? = null,
    val scene3d: Scene3DSpec? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false
)

data class SampleProblem(
    val id: String,
    val title: String,
    val subject: String,
    val topic: String,
    val problemText: String,
    val iconName: String
)
