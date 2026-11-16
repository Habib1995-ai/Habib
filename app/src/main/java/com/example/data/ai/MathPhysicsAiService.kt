package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ConceptCheckQuestion
import com.example.data.model.ProblemSolution
import com.example.data.model.Scene3DJsonCodec
import com.example.data.model.SolutionStep
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MathPhysicsAiService"

class MathPhysicsAiService {

    // Core schema (shared and used directly by fast fallback model)
    private val coreSolutionProperties = mapOf(
        "subject" to Schema.string(description = "رشته: ریاضی، فیزیک، فیزیک نجوم، هندسه یا شیمی"),
        "topic" to Schema.string(description = "مبحث دقیق مسئله"),
        "identifiedProblem" to Schema.string(description = "متن کامل و دقیق مسئله شناسایی‌شده به فارسی"),
        "finalAnswer" to Schema.string(description = "پاسخ نهایی دقیق همراه با یکا به فارسی"),
        "formulasUsed" to Schema.array(
            items = Schema.string(),
            description = "فرمول‌های اصلی استفاده‌شده"
        ),
        "givenData" to Schema.array(
            items = Schema.string(),
            description = "داده‌ها و مفروضات مسئله"
        ),
        "steps" to Schema.array(
            items = Schema.obj(
                properties = mapOf(
                    "stepNumber" to Schema.integer(description = "شماره گام"),
                    "title" to Schema.string(description = "عنوان گام"),
                    "explanation" to Schema.string(description = "توضیح مفهومی روان و کوتاه"),
                    "calculation" to Schema.string(description = "جایگذاری و محاسبات مرحله"),
                    "visual3DNote" to Schema.string(description = "نکته کوتاه سه‌بعدی برای این مرحله")
                ),
                optionalProperties = listOf("visual3DNote")
            ),
            description = "۳ تا ۵ گام روشن و منظم حل مسئله"
        ),
        "studentTip" to Schema.string(description = "نکته طلایی آموزشی برای امتحان"),
        "conceptQuestion" to Schema.obj(
            properties = mapOf(
                "question" to Schema.string(description = "سوال مفهومی چهارگزینه‌ای مرتبط"),
                "options" to Schema.array(items = Schema.string(), description = "۴ گزینه کوتاه"),
                "correctOptionIndex" to Schema.integer(description = "اندیس گزینه صحیح از ۰ تا ۳"),
                "explanation" to Schema.string(description = "توضیح کوتاه دلیل درستی گزینه")
            )
        )
    )

    // Lightweight 3D metadata schema — keeps constrained decoding fast while supporting custom molecules/graphs
    private val scene3dSchema = Schema.obj(
        properties = mapOf(
            "sceneType" to Schema.string(description = "ASTRONOMY_ASTROPHYSICS, GEOMETRY_SOLID, CHEMISTRY_MOLECULE, PHYSICS_MECHANICS, یا MATH_SURFACE_3D"),
            "shapeVariant" to Schema.string(description = "قالب آماده: ELLIPTICAL_ORBIT, BINARY_ORBIT, GRAVITY_WELL, WATER_H2O, METHANE_CH4, AMMONIA_NH3, CO2_LINEAR, BENZENE_RING, ATOM_BOHR, CUSTOM_MOLECULE, CUBE, PYRAMID, CONE, CYLINDER, SPHERE, TETRAHEDRON, VECTORS_3D, INCLINED_PLANE, PROJECTILE_3D, PENDULUM_3D, MAGNETIC_FIELD, PARABOLOID_3D, WAVE_3D, SADDLE_3D, CUSTOM_GRAPH"),
            "title" to Schema.string(description = "عنوان کوتاه مدل سه‌بعدی به فارسی"),
            "spatialSummary" to Schema.string(description = "توضیح کوتاه درک فضایی و سه‌بعدی مسئله"),
            "formulaLabel" to Schema.string(description = "فرمول کلیدی مدل سه‌بعدی"),
            "nodes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "label" to Schema.string(description = "نام کوتاه اتم، جرم یا رأس"),
                        "x" to Schema.double(description = "مختصات X بین -2.0 تا 2.0"),
                        "y" to Schema.double(description = "مختصات Y بین -2.0 تا 2.0"),
                        "z" to Schema.double(description = "مختصات Z بین -2.0 تا 2.0"),
                        "colorHex" to Schema.string(description = "کد رنگ هگز مثل #38BDF8")
                    ),
                    optionalProperties = listOf("colorHex")
                ),
                description = "فقط برای CUSTOM_MOLECULE یا CUSTOM_GRAPH پر شود؛ برای قالب‌های آماده خالی بگذارید"
            ),
            "edges" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "fromIndex" to Schema.integer(),
                        "toIndex" to Schema.integer(),
                        "label" to Schema.string(),
                        "isVector" to Schema.boolean(),
                        "bondOrder" to Schema.integer()
                    ),
                    optionalProperties = listOf("label", "isVector", "bondOrder")
                ),
                description = "فقط در صورت پر کردن nodes مقداردهی شود"
            )
        ),
        optionalProperties = listOf("formulaLabel", "nodes", "edges")
    )

    private val fullSolutionSchema = Schema.obj(
        properties = coreSolutionProperties + ("scene3d" to scene3dSchema),
        optionalProperties = listOf("conceptQuestion", "scene3d")
    )

    private val fastFallbackSchema = Schema.obj(
        properties = coreSolutionProperties,
        optionalProperties = listOf("conceptQuestion")
    )

    private val fullGenConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = fullSolutionSchema
    }

    private val fastGenConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = fastFallbackSchema
    }

    // Lazy initialization as mandated by firebase-ai-logic-android
    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = fullGenConfig
        )
    }

    // Ultra-fast rescue model if primary model hits timeout under heavy load or slow network
    private val fastFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = fastGenConfig
        )
    }

    suspend fun solveProblem(
        bitmap: Bitmap?,
        userNotesOrText: String?
    ): Result<ProblemSolution> = withContext(Dispatchers.IO) {
        val scaledBitmap = bitmap?.let { scaleBitmapIfNeeded(it, 1024) }
        val primaryPrompt = buildPrompt(userNotesOrText, bitmap == null, include3dInstructions = true)

        try {
            val jsonString = streamResponse(
                targetModel = primaryModel,
                prompt = primaryPrompt,
                bitmap = scaledBitmap,
                timeoutMs = 55_000L
            )
            Log.d(TAG, "Primary AI streaming response received successfully.")
            Result.success(parseJsonSolution(jsonString))
        } catch (timeoutEx: TimeoutCancellationException) {
            Log.w(TAG, "Primary AI request timed out after 55s; attempting fast fallback model.", timeoutEx)
            try {
                val fastPrompt = buildPrompt(userNotesOrText, bitmap == null, include3dInstructions = false)
                val fallbackJson = streamResponse(
                    targetModel = fastFallbackModel,
                    prompt = fastPrompt,
                    bitmap = scaledBitmap,
                    timeoutMs = 30_000L
                )
                Log.d(TAG, "Fast fallback AI response succeeded.")
                Result.success(parseJsonSolution(fallbackJson))
            } catch (fallbackTimeout: TimeoutCancellationException) {
                Log.e(TAG, "AI solving request timed out (primary and fallback)", fallbackTimeout)
                Result.failure(
                    Exception("زمان پردازش مسئله به پایان رسید. لطفاً اتصال اینترنت را بررسی و مجدداً تلاش کنید.")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fast fallback AI request failed", e)
                Result.failure(mapFriendlyException(e))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to solve problem via Firebase AI Logic", e)
            Result.failure(mapFriendlyException(e))
        }
    }

    private suspend fun streamResponse(
        targetModel: com.google.firebase.ai.GenerativeModel,
        prompt: String,
        bitmap: Bitmap?,
        timeoutMs: Long
    ): String {
        val accumulated = StringBuilder()
        withTimeout(timeoutMs) {
            val flow = if (bitmap != null) {
                targetModel.generateContentStream(
                    content {
                        text(prompt)
                        image(bitmap)
                    }
                )
            } else {
                targetModel.generateContentStream(prompt)
            }
            flow.collect { chunk ->
                accumulated.append(chunk.text.orEmpty())
            }
        }
        val text = accumulated.toString()
        if (text.isBlank()) {
            throw IllegalStateException("پاسخی از مدل هوش مصنوعی دریافت نشد.")
        }
        return text
    }

    private fun buildPrompt(
        userNotesOrText: String?,
        noBitmapProvided: Boolean,
        include3dInstructions: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append(
            """
            شما آلبرت انیشتین و استاد مسلط ریاضی، فیزیک، فیزیک نجوم، هندسه فضایی و شیمی حرفه‌ای هستید.
            وظیفه شما:
            ۱- اگر تصویر ارسال شده، صورت مسئله یا شکل/ساختار شیمیایی را با دقت استخراج کن.
            ۲- پاسخ نهایی (finalAnswer) را کاملاً دقیق و همراه با یکا بنویس.
            ۳- حل را در ۳ تا ۵ گام روشن، روان و خلاصه (steps) همراه با محاسبات و یک visual3DNote کوتاه برای هر گام ارائه بده.
            ۴- فرمول‌ها (formulasUsed)، داده‌ها (givenData)، یک نکته طلایی (studentTip) و یک سوال مفهومی (conceptQuestion) بنویس.
            """.trimIndent()
        )
        if (include3dInstructions) {
            sb.append(
                """

                ۵- برای scene3d مناسب‌ترین sceneType و shapeVariant را انتخاب کن. اگر یکی از قالب‌های آماده مناسب است، nodes و edges را خالی بگذار تا سریع‌تر پاسخ بدهی؛ فقط برای مولکول‌ها یا نمودارهای خاص (CUSTOM_MOLECULE / CUSTOM_GRAPH) مختصات کوتاه بده.
                """.trimIndent()
            )
        }
        if (!userNotesOrText.isNullOrBlank()) {
            sb.append("\n\nمتن یا توضیحات کاربر:\n").append(userNotesOrText)
        } else if (noBitmapProvided) {
            sb.append("\nلطفاً یک مسئله نمونه علمی با توضیح گام‌به‌گام حل کن.")
        }
        return sb.toString()
    }

    private fun mapFriendlyException(e: Exception): Exception {
        val friendlyMsg = when {
            e.message?.contains("403") == true || e.message?.contains("AppCheck") == true ->
                "خطای دسترسی سرویس هوش مصنوعی (App Check). لطفاً چند لحظه دیگر دوباره تلاش کنید."
            e.message?.contains("quota") == true || e.message?.contains("Resource has been exhausted") == true ->
                "سهمیه درخواست‌های هوش مصنوعی پر شده است. لطفاً کمی صبر کنید."
            else -> e.localizedMessage ?: "خطایی در پردازش و حل مسئله رخ داد."
        }
        return Exception(friendlyMsg, e)
    }

    private fun parseJsonSolution(jsonString: String): ProblemSolution {
        val root = JSONObject(jsonString)

        val subject = root.optString("subject", "ریاضی و فیزیک")
        val topic = root.optString("topic", "مسئله علمی")
        val identifiedProblem = root.optString("identifiedProblem", "مسئله شناسایی‌شده")
        val finalAnswer = root.optString("finalAnswer", "پاسخ در مراحل محاسبه شده است")
        val studentTip = root.optString("studentTip", "")

        val formulasList = mutableListOf<String>()
        val formulasArray = root.optJSONArray("formulasUsed")
        if (formulasArray != null) {
            for (i in 0 until formulasArray.length()) {
                val f = formulasArray.optString(i)
                if (f.isNotBlank()) formulasList.add(f)
            }
        }

        val givenList = mutableListOf<String>()
        val givenArray = root.optJSONArray("givenData")
        if (givenArray != null) {
            for (i in 0 until givenArray.length()) {
                val g = givenArray.optString(i)
                if (g.isNotBlank()) givenList.add(g)
            }
        }

        val stepsList = mutableListOf<SolutionStep>()
        val stepsArray = root.optJSONArray("steps")
        if (stepsArray != null) {
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.optJSONObject(i) ?: continue
                stepsList.add(
                    SolutionStep(
                        stepNumber = stepObj.optInt("stepNumber", i + 1),
                        title = stepObj.optString("title", "گام شماره ${i + 1}"),
                        explanation = stepObj.optString("explanation", ""),
                        calculation = stepObj.optString("calculation", ""),
                        visual3DNote = stepObj.optString("visual3DNote", "")
                    )
                )
            }
        }

        var conceptQuestion: ConceptCheckQuestion? = null
        val cObj = root.optJSONObject("conceptQuestion")
        if (cObj != null) {
            val qText = cObj.optString("question", "")
            val optArr = cObj.optJSONArray("options")
            val opts = mutableListOf<String>()
            if (optArr != null) {
                for (i in 0 until optArr.length()) {
                    val opt = optArr.optString(i)
                    if (opt.isNotBlank()) opts.add(opt)
                }
            }
            val cIndex = cObj.optInt("correctOptionIndex", 0)
            val expl = cObj.optString("explanation", "")
            if (qText.isNotBlank() && opts.isNotEmpty()) {
                conceptQuestion = ConceptCheckQuestion(qText, opts, cIndex, expl)
            }
        }

        val scene3d = Scene3DJsonCodec.fromJsonObject(root.optJSONObject("scene3d"))

        return ProblemSolution(
            subject = subject,
            topic = topic,
            identifiedProblem = identifiedProblem,
            finalAnswer = finalAnswer,
            formulasUsed = formulasList,
            givenData = givenList,
            steps = stepsList,
            studentTip = studentTip,
            conceptQuestion = conceptQuestion,
            scene3d = scene3d
        )
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
    }
}
