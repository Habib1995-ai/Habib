package com.example

import com.example.data.model.ProblemSolution
import com.example.data.model.Scene3DType
import com.example.data.model.SolutionStep
import com.example.data.repository.FormulaRepository
import com.example.data.repository.QuizRepository
import com.example.util.Scene3DSynthesizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaAndDashboardTest {

    @Test
    fun `formula repository contains math, physics, astronomy, geometry, and chemistry formulas`() {
        val repo = FormulaRepository()
        val formulas = repo.getAllFormulas()

        assertTrue("Expected formulas to be non-empty", formulas.isNotEmpty())
        assertTrue("Expected physics formulas", formulas.any { it.subject == "فیزیک" })
        assertTrue("Expected math formulas", formulas.any { it.subject == "ریاضی" })
        assertTrue("Expected astrophysics formulas", formulas.any { it.subject == "فیزیک نجوم" })
        assertTrue("Expected geometry formulas", formulas.any { it.subject == "هندسه" })
        assertTrue("Expected chemistry formulas", formulas.any { it.subject == "شیمی" })
        assertTrue(
            "All formulas should have variables and usage tip",
            formulas.all { it.title.isNotBlank() && it.formula.isNotBlank() && it.variables.isNotEmpty() }
        )
    }

    @Test
    fun `quiz repository evaluates score percentage accurately`() {
        val quizRepo = QuizRepository()
        val result = quizRepo.evaluateQuiz(total = 5, correct = 4, timeSeconds = 42L)

        assertEquals(5, result.totalQuestions)
        assertEquals(4, result.correctAnswers)
        assertEquals(80, result.percentage)
        assertEquals(42L, result.timeSpentSeconds)
        assertTrue(result.feedbackMessage.isNotBlank())
    }

    @Test
    fun `scene3d synthesizer builds domain accurate 3D scenes for astronomy, chemistry, and geometry`() {
        val astroSolution = ProblemSolution(
            subject = "فیزیک نجوم",
            topic = "سیاهچاله و خمیدگی فضازمان",
            identifiedProblem = "شعاع شوارتزشیلد یک سیاهچاله با جرم خورشید چقدر است؟",
            finalAnswer = "۳ کیلومتر",
            steps = listOf(SolutionStep(1, "محاسبه شعاع", "توضیح", "R_s = 2GM/c^2"))
        )
        val astroScene = Scene3DSynthesizer.resolveScene(astroSolution)
        assertEquals(Scene3DType.ASTRONOMY_ASTROPHYSICS, astroScene.sceneType)
        assertEquals("GRAVITY_WELL", astroScene.shapeVariant)
        assertTrue(astroScene.nodes.isNotEmpty())
        assertTrue(astroScene.stepFocuses.isNotEmpty())

        val chemSolution = ProblemSolution(
            subject = "شیمی آلی",
            topic = "ساختار مولکولی متان CH4",
            identifiedProblem = "زاویه پیوندی در متان چقدر است؟",
            finalAnswer = "۱۰۹.۵ درجه"
        )
        val chemScene = Scene3DSynthesizer.resolveScene(chemSolution)
        assertEquals(Scene3DType.CHEMISTRY_MOLECULE, chemScene.sceneType)
        assertEquals("METHANE_CH4", chemScene.shapeVariant)
        assertEquals(5, chemScene.nodes.size)
        assertEquals(4, chemScene.edges.size)

        val geomSolution = ProblemSolution(
            subject = "هندسه فضایی",
            topic = "حجم هرم منتظم",
            identifiedProblem = "حجم هرم مربع‌القاعده با ضلع ۶ و ارتفاع ۴ را بیابید.",
            finalAnswer = "۴۸ سانتی‌متر مکعب"
        )
        val geomScene = Scene3DSynthesizer.resolveScene(geomSolution)
        assertEquals(Scene3DType.GEOMETRY_SOLID, geomScene.sceneType)
        assertEquals("PYRAMID", geomScene.shapeVariant)
        assertTrue(geomScene.nodes.size >= 5)
    }
}
