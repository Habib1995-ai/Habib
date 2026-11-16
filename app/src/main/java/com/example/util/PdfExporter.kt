package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.data.model.ProblemSolution
import com.example.data.model.StudyNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to generate and export high-quality, printable PDF worksheets
 * of math and physics solutions with full Persian RTL formatting.
 */
object PdfExporter {

    private const val TAG = "PdfExporter"

    // Standard A4 dimensions in points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val MARGIN_HORIZONTAL = 36f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 46f
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)

    /**
     * Exports the given problem solution and any attached student study notes
     * to a user-selected Uri destination as a PDF file.
     */
    suspend fun exportSolutionToUri(
        context: Context,
        solution: ProblemSolution,
        notes: List<StudyNote> = emptyList(),
        uri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        var outputStream: OutputStream? = null
        val pdfDocument = PdfDocument()

        try {
            outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                Log.e(TAG, "Cannot open output stream for Uri: $uri")
                return@withContext false
            }

            val pageHelper = PdfPageHelper(pdfDocument)

            // 1. First Page Header
            pageHelper.drawHeader(
                appName = "انیشتین همراته",
                subtitle = "برگه تحلیل و راه حل گام‌به‌گام مسئله",
                subject = solution.subject,
                topic = solution.topic
            )

            // 2. Prominent Final Answer Hero Card
            pageHelper.drawFinalAnswerCard(solution.finalAnswer)

            // 3. Problem Statement & Given Data
            pageHelper.drawProblemStatementCard(
                problem = solution.identifiedProblem,
                givenData = solution.givenData
            )

            // 4. Formulas Used (if present)
            if (solution.formulasUsed.isNotEmpty()) {
                pageHelper.drawFormulasCard(solution.formulasUsed)
            }

            // 5. Step-by-Step Explanations & Computations
            if (solution.steps.isNotEmpty()) {
                pageHelper.drawSectionHeading("مراحل گام‌به‌گام و پله‌پله حل مسئله:")
                solution.steps.forEach { step ->
                    pageHelper.drawStepCard(
                        stepNumber = step.stepNumber,
                        title = step.title,
                        explanation = step.explanation,
                        calculation = step.calculation
                    )
                }
            }

            // 6. Einstein Golden Student Tip (if present)
            if (solution.studentTip.isNotBlank()) {
                pageHelper.drawStudentTipCard(solution.studentTip)
            }

            // 7. Conceptual Question / Learning Assessment (if present)
            solution.conceptQuestion?.let { question ->
                pageHelper.drawConceptCheckCard(
                    question = question.question,
                    options = question.options,
                    correctIndex = question.correctOptionIndex,
                    explanation = question.explanation
                )
            }

            // 8. Attached Student Study Notes (if any)
            if (notes.isNotEmpty()) {
                pageHelper.drawNotesSection(notes)
            }

            // Finalize all pages and write out
            pageHelper.finishAllPages()
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            false
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) { }
            try {
                pdfDocument.close()
            } catch (_: Exception) { }
        }
    }

    /**
     * Launches an external viewer intent for the exported PDF file.
     */
    fun openPdfUri(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "مشاهده فایل PDF"))
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open PDF viewer", e)
        }
    }

    /**
     * Helper to manage multi-page allocation, coordinates, borders, and typography.
     */
    private class PdfPageHelper(private val document: PdfDocument) {
        private var currentPageNumber = 1
        private var currentY = MARGIN_TOP

        private var currentPage: PdfDocument.Page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
        )
        private var currentCanvas: Canvas = currentPage.canvas

        private val primaryBlue = Color.rgb(37, 99, 235)       // #2563EB
        private val secondaryCyan = Color.rgb(6, 182, 212)     // #06B6D4
        private val emeraldGreen = Color.rgb(5, 150, 105)      // #059669
        private val emeraldBg = Color.rgb(236, 253, 245)       // #ECFDF5
        private val amberColor = Color.rgb(217, 119, 6)        // #D97706
        private val amberBg = Color.rgb(254, 243, 199)         // #FEF3C7
        private val cardBg = Color.rgb(248, 250, 252)          // #F8FAFC
        private val borderColor = Color.rgb(226, 232, 240)     // #E2E8F0
        private val textPrimary = Color.rgb(15, 23, 42)        // #0F172A
        private val textSecondary = Color.rgb(71, 85, 105)     // #475569
        private val darkHeaderBg = Color.rgb(30, 41, 59)       // #1E293B

        private val titlePaint = TextPaint().apply {
            color = textPrimary
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        private val bodyPaint = TextPaint().apply {
            color = textSecondary
            textSize = 10.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        private val monoPaint = TextPaint().apply {
            color = primaryBlue
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        private val shapePaint = Paint().apply {
            isAntiAlias = true
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                drawPageFooter()
                document.finishPage(currentPage)
                currentPageNumber++
                currentPage = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                )
                currentCanvas = currentPage.canvas
                currentY = MARGIN_TOP

                // Draw mini continuation header
                drawContinuationHeader()
            }
        }

        fun drawHeader(appName: String, subtitle: String, subject: String, topic: String) {
            val headerHeight = 76f
            ensureSpace(headerHeight + 20f)

            // Header Background Box
            shapePaint.color = darkHeaderBg
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + headerHeight)
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            // Accent stripe
            shapePaint.color = secondaryCyan
            val stripeRect = RectF(MARGIN_HORIZONTAL, currentY + headerHeight - 4f, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + headerHeight)
            currentCanvas.drawRect(stripeRect, shapePaint)

            // App Name & Subtitle
            val headerTextPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText(appName, MARGIN_HORIZONTAL + 16f, currentY + 14f, USABLE_WIDTH - 32f, headerTextPaint)

            val subTextPaint = TextPaint().apply {
                color = Color.rgb(203, 213, 225)
                textSize = 9.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            drawStaticText(subtitle, MARGIN_HORIZONTAL + 16f, currentY + 36f, USABLE_WIDTH - 150f, subTextPaint)

            // Subject / Topic tag & Date on the left (RTL visually)
            val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            val metaPaint = TextPaint().apply {
                color = Color.rgb(186, 230, 253)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val metaText = "$subject • $topic\nتاریخ صدور: $currentDate"
            drawStaticText(metaText, MARGIN_HORIZONTAL + USABLE_WIDTH - 200f, currentY + 16f, 185f, metaPaint, Layout.Alignment.ALIGN_OPPOSITE)

            currentY += headerHeight + 14f
        }

        private fun drawContinuationHeader() {
            val miniHeaderHeight = 22f
            shapePaint.color = darkHeaderBg
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + miniHeaderHeight)
            currentCanvas.drawRoundRect(rect, 6f, 6f, shapePaint)

            val textPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            drawStaticText("انیشتین همراته — ادامه راه حل مسئله", MARGIN_HORIZONTAL + 10f, currentY + 4f, USABLE_WIDTH - 20f, textPaint)
            currentY += miniHeaderHeight + 12f
        }

        fun drawFinalAnswerCard(finalAnswer: String) {
            val answerLayout = createLayout(finalAnswer, USABLE_WIDTH - 32f, TextPaint().apply {
                color = Color.rgb(6, 78, 59)
                textSize = 13.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            })

            val cardHeight = 34f + answerLayout.height + 24f
            ensureSpace(cardHeight + 12f)

            // Card Background
            shapePaint.color = emeraldBg
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + cardHeight)
            currentCanvas.drawRoundRect(rect, 12f, 12f, shapePaint)

            // Border
            shapePaint.color = emeraldGreen
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = 1.5f
            currentCanvas.drawRoundRect(rect, 12f, 12f, shapePaint)

            // Title
            val headerPaint = TextPaint().apply {
                color = emeraldGreen
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText("🎯 پاسخ نهایی و قطعی مسئله:", MARGIN_HORIZONTAL + 16f, currentY + 12f, USABLE_WIDTH - 32f, headerPaint)

            // Answer Content Box
            val answerBoxRect = RectF(MARGIN_HORIZONTAL + 12f, currentY + 34f, MARGIN_HORIZONTAL + USABLE_WIDTH - 12f, currentY + 34f + answerLayout.height + 14f)
            shapePaint.color = Color.WHITE
            shapePaint.style = Paint.Style.FILL
            currentCanvas.drawRoundRect(answerBoxRect, 8f, 8f, shapePaint)

            currentCanvas.save()
            currentCanvas.translate(MARGIN_HORIZONTAL + 20f, currentY + 40f)
            answerLayout.draw(currentCanvas)
            currentCanvas.restore()

            currentY += cardHeight + 12f
        }

        fun drawProblemStatementCard(problem: String, givenData: List<String>) {
            val problemLayout = createLayout(problem, USABLE_WIDTH - 28f, bodyPaint)
            var contentHeight = 30f + problemLayout.height + 12f

            val dataLayouts = givenData.map {
                createLayout("• $it", USABLE_WIDTH - 36f, bodyPaint)
            }
            if (dataLayouts.isNotEmpty()) {
                contentHeight += 18f
                dataLayouts.forEach { contentHeight += it.height + 4f }
            }

            ensureSpace(contentHeight + 12f)

            // Draw card background
            drawStandardCardBox(contentHeight)

            // Title
            val headerPaint = TextPaint().apply {
                color = primaryBlue
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText("❓ صورت مسئله شناسایی‌شده:", MARGIN_HORIZONTAL + 14f, currentY + 12f, USABLE_WIDTH - 28f, headerPaint)

            var innerY = currentY + 30f
            currentCanvas.save()
            currentCanvas.translate(MARGIN_HORIZONTAL + 14f, innerY)
            problemLayout.draw(currentCanvas)
            currentCanvas.restore()
            innerY += problemLayout.height + 8f

            if (dataLayouts.isNotEmpty()) {
                val dataHeaderPaint = TextPaint().apply {
                    color = primaryBlue
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                drawStaticText("داده‌ها و مفروضات اولیه:", MARGIN_HORIZONTAL + 14f, innerY, USABLE_WIDTH - 28f, dataHeaderPaint)
                innerY += 16f

                dataLayouts.forEach { dLayout ->
                    currentCanvas.save()
                    currentCanvas.translate(MARGIN_HORIZONTAL + 20f, innerY)
                    dLayout.draw(currentCanvas)
                    currentCanvas.restore()
                    innerY += dLayout.height + 4f
                }
            }

            currentY += contentHeight + 12f
        }

        fun drawFormulasCard(formulas: List<String>) {
            var totalHeight = 34f
            val formulaLayouts = formulas.map {
                val layout = createLayout(it, USABLE_WIDTH - 36f, monoPaint)
                totalHeight += layout.height + 14f
                layout
            }

            ensureSpace(totalHeight + 12f)

            shapePaint.color = amberBg
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + totalHeight)
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            shapePaint.color = amberColor
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = 1f
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            val headerPaint = TextPaint().apply {
                color = amberColor
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText("📐 فرمول‌ها و روابط فیزیکی / ریاضی به کار رفته:", MARGIN_HORIZONTAL + 14f, currentY + 10f, USABLE_WIDTH - 28f, headerPaint)

            var innerY = currentY + 28f
            formulaLayouts.forEach { fLayout ->
                val fBoxRect = RectF(MARGIN_HORIZONTAL + 12f, innerY, MARGIN_HORIZONTAL + USABLE_WIDTH - 12f, innerY + fLayout.height + 8f)
                shapePaint.color = Color.WHITE
                shapePaint.style = Paint.Style.FILL
                currentCanvas.drawRoundRect(fBoxRect, 6f, 6f, shapePaint)

                currentCanvas.save()
                currentCanvas.translate(MARGIN_HORIZONTAL + 18f, innerY + 4f)
                fLayout.draw(currentCanvas)
                currentCanvas.restore()

                innerY += fLayout.height + 14f
            }

            currentY += totalHeight + 12f
        }

        fun drawSectionHeading(text: String) {
            ensureSpace(30f)
            val headingPaint = TextPaint().apply {
                color = primaryBlue
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText(text, MARGIN_HORIZONTAL, currentY + 4f, USABLE_WIDTH, headingPaint)
            currentY += 24f
        }

        fun drawStepCard(stepNumber: Int, title: String, explanation: String, calculation: String) {
            val expLayout = if (explanation.isNotBlank()) createLayout(explanation, USABLE_WIDTH - 28f, bodyPaint) else null
            val calcLayout = if (calculation.isNotBlank()) createLayout(calculation, USABLE_WIDTH - 40f, monoPaint) else null

            var cardHeight = 32f
            expLayout?.let { cardHeight += it.height + 8f }
            calcLayout?.let { cardHeight += it.height + 16f }
            cardHeight += 10f

            ensureSpace(cardHeight + 10f)

            drawStandardCardBox(cardHeight)

            // Step Badge & Title
            shapePaint.color = primaryBlue
            shapePaint.style = Paint.Style.FILL
            val badgeRect = RectF(MARGIN_HORIZONTAL + 12f, currentY + 8f, MARGIN_HORIZONTAL + 36f, currentY + 30f)
            currentCanvas.drawRoundRect(badgeRect, 6f, 6f, shapePaint)

            val badgeTextPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText("$stepNumber", MARGIN_HORIZONTAL + 16f, currentY + 11f, 16f, badgeTextPaint, Layout.Alignment.ALIGN_CENTER)

            val stepTitlePaint = TextPaint().apply {
                color = textPrimary
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText(title.ifBlank { "مرحله شماره $stepNumber" }, MARGIN_HORIZONTAL + 42f, currentY + 11f, USABLE_WIDTH - 60f, stepTitlePaint)

            var innerY = currentY + 34f
            expLayout?.let {
                currentCanvas.save()
                currentCanvas.translate(MARGIN_HORIZONTAL + 14f, innerY)
                it.draw(currentCanvas)
                currentCanvas.restore()
                innerY += it.height + 8f
            }

            calcLayout?.let {
                val calcBoxRect = RectF(MARGIN_HORIZONTAL + 14f, innerY, MARGIN_HORIZONTAL + USABLE_WIDTH - 14f, innerY + it.height + 10f)
                shapePaint.color = Color.rgb(241, 245, 249)
                shapePaint.style = Paint.Style.FILL
                currentCanvas.drawRoundRect(calcBoxRect, 6f, 6f, shapePaint)

                currentCanvas.save()
                currentCanvas.translate(MARGIN_HORIZONTAL + 22f, innerY + 5f)
                it.draw(currentCanvas)
                currentCanvas.restore()
            }

            currentY += cardHeight + 10f
        }

        fun drawStudentTipCard(tip: String) {
            val tipLayout = createLayout(tip, USABLE_WIDTH - 28f, bodyPaint)
            val cardHeight = 30f + tipLayout.height + 16f
            ensureSpace(cardHeight + 12f)

            shapePaint.color = Color.rgb(240, 249, 255)
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + cardHeight)
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            shapePaint.color = secondaryCyan
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = 1f
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            val headerPaint = TextPaint().apply {
                color = secondaryCyan
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText("💡 نکته طلایی و مفهومی انیشتین برای امتحان:", MARGIN_HORIZONTAL + 14f, currentY + 10f, USABLE_WIDTH - 28f, headerPaint)

            currentCanvas.save()
            currentCanvas.translate(MARGIN_HORIZONTAL + 14f, currentY + 28f)
            tipLayout.draw(currentCanvas)
            currentCanvas.restore()

            currentY += cardHeight + 12f
        }

        fun drawConceptCheckCard(question: String, options: List<String>, correctIndex: Int, explanation: String) {
            val qLayout = createLayout("پرسش: $question", USABLE_WIDTH - 28f, TextPaint().apply {
                color = Color.rgb(109, 40, 217)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            })

            val optLayouts = options.mapIndexed { idx, opt ->
                val prefix = if (idx == correctIndex) "✅ گزینه ${idx + 1}: " else "⭕ گزینه ${idx + 1}: "
                createLayout(prefix + opt, USABLE_WIDTH - 36f, bodyPaint)
            }

            val expLayout = if (explanation.isNotBlank()) {
                createLayout("تحلیل پاسخ: $explanation", USABLE_WIDTH - 28f, bodyPaint)
            } else null

            var totalHeight = 32f + qLayout.height + 10f
            optLayouts.forEach { totalHeight += it.height + 5f }
            expLayout?.let { totalHeight += it.height + 12f }
            totalHeight += 10f

            ensureSpace(totalHeight + 12f)

            shapePaint.color = Color.rgb(250, 245, 255)
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + totalHeight)
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            shapePaint.color = Color.rgb(147, 51, 234)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = 1f
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            val headerPaint = TextPaint().apply {
                color = Color.rgb(126, 34, 206)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawStaticText("🧠 چالش و پرسش مفهومی برای سنجش یادگیری:", MARGIN_HORIZONTAL + 14f, currentY + 10f, USABLE_WIDTH - 28f, headerPaint)

            var innerY = currentY + 28f
            currentCanvas.save()
            currentCanvas.translate(MARGIN_HORIZONTAL + 14f, innerY)
            qLayout.draw(currentCanvas)
            currentCanvas.restore()
            innerY += qLayout.height + 8f

            optLayouts.forEach { oLayout ->
                currentCanvas.save()
                currentCanvas.translate(MARGIN_HORIZONTAL + 18f, innerY)
                oLayout.draw(currentCanvas)
                currentCanvas.restore()
                innerY += oLayout.height + 5f
            }

            expLayout?.let {
                innerY += 4f
                currentCanvas.save()
                currentCanvas.translate(MARGIN_HORIZONTAL + 14f, innerY)
                it.draw(currentCanvas)
                currentCanvas.restore()
            }

            currentY += totalHeight + 12f
        }

        fun drawNotesSection(notes: List<StudyNote>) {
            drawSectionHeading("یادداشت‌های اختصاصی دانش‌آموز:")

            notes.forEach { note ->
                val titleLayout = createLayout(note.title, USABLE_WIDTH - 28f, TextPaint().apply {
                    color = textPrimary
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                })
                val contentLayout = if (note.content.isNotBlank()) createLayout(note.content, USABLE_WIDTH - 28f, bodyPaint) else null

                var noteHeight = 16f + titleLayout.height
                contentLayout?.let { noteHeight += it.height + 6f }
                noteHeight += 10f

                ensureSpace(noteHeight + 8f)
                drawStandardCardBox(noteHeight)

                var innerY = currentY + 8f
                currentCanvas.save()
                currentCanvas.translate(MARGIN_HORIZONTAL + 14f, innerY)
                titleLayout.draw(currentCanvas)
                currentCanvas.restore()
                innerY += titleLayout.height + 4f

                contentLayout?.let {
                    currentCanvas.save()
                    currentCanvas.translate(MARGIN_HORIZONTAL + 14f, innerY)
                    it.draw(currentCanvas)
                    currentCanvas.restore()
                }

                currentY += noteHeight + 8f
            }
        }

        private fun drawStandardCardBox(height: Float) {
            shapePaint.color = cardBg
            shapePaint.style = Paint.Style.FILL
            val rect = RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + USABLE_WIDTH, currentY + height)
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)

            shapePaint.color = borderColor
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = 1f
            currentCanvas.drawRoundRect(rect, 10f, 10f, shapePaint)
        }

        private fun drawPageFooter() {
            val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 16f

            // Separator line
            shapePaint.color = borderColor
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = 0.8f
            currentCanvas.drawLine(MARGIN_HORIZONTAL, footerY - 8f, MARGIN_HORIZONTAL + USABLE_WIDTH, footerY - 8f, shapePaint)

            val footerPaint = TextPaint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            drawStaticText(
                "اپلیکیشن «انیشتین همراته» • سامانه حل هوشمند مسائل ریاضی و فیزیک",
                MARGIN_HORIZONTAL,
                footerY,
                USABLE_WIDTH - 80f,
                footerPaint
            )

            drawStaticText(
                "صفحه $currentPageNumber",
                MARGIN_HORIZONTAL + USABLE_WIDTH - 70f,
                footerY,
                70f,
                footerPaint,
                Layout.Alignment.ALIGN_OPPOSITE
            )
        }

        fun finishAllPages() {
            drawPageFooter()
            document.finishPage(currentPage)
        }

        private fun createLayout(text: String, width: Float, paint: TextPaint, alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout {
            val targetWidth = width.toInt().coerceAtLeast(10)
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, targetWidth)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(2f, 1.15f)
                .build()
        }

        private fun drawStaticText(
            text: String,
            x: Float,
            y: Float,
            width: Float,
            paint: TextPaint,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        ) {
            val layout = createLayout(text, width, paint, alignment)
            currentCanvas.save()
            currentCanvas.translate(x, y)
            layout.draw(currentCanvas)
            currentCanvas.restore()
        }
    }
}
