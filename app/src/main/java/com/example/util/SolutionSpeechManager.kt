package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.ProblemSolution
import com.example.data.model.SolutionStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * Represents each playable segment of a problem's solution narrative.
 */
data class SpeechSegment(
    val id: String,
    val title: String,
    val subtitle: String,
    val textToSpeak: String,
    val stepNumber: Int? = null
)

/**
 * UI State of the Text-to-Speech narration engine.
 */
data class SpeechPlaybackState(
    val isInitialized: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val currentSegmentId: String? = null,
    val currentTitle: String = "",
    val currentSubtitle: String = "",
    val currentStepNumber: Int? = null,
    val currentPlaylistIndex: Int = -1,
    val totalSegments: Int = 0,
    val speechRate: Float = 1.0f,
    val isPersianSupported: Boolean = true,
    val engineName: String? = null,
    val noticeMessage: String? = null
)

class SolutionSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null

    private val _playbackState = MutableStateFlow(SpeechPlaybackState())
    val playbackState: StateFlow<SpeechPlaybackState> = _playbackState.asStateFlow()

    private var activePlaylist: List<SpeechSegment> = emptyList()
    private var currentIndex: Int = -1

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("SolutionSpeechManager", "Failed to init TTS", e)
            _playbackState.update {
                it.copy(
                    isInitialized = false,
                    isPersianSupported = false,
                    noticeMessage = "امکان بارگذاری سرویس صوتی دستگاه وجود ندارد."
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts?.defaultEngine
            val persianLocale = Locale("fa", "IR")
            val langResult = tts?.isLanguageAvailable(persianLocale)

            val isAvailable = langResult != TextToSpeech.LANG_MISSING_DATA &&
                    langResult != TextToSpeech.LANG_NOT_SUPPORTED

            if (isAvailable) {
                tts?.language = persianLocale
            } else {
                // Try fallback to fa or default
                val faOnly = Locale("fa")
                val faResult = tts?.isLanguageAvailable(faOnly)
                if (faResult != TextToSpeech.LANG_MISSING_DATA && faResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = faOnly
                } else {
                    tts?.language = Locale.getDefault()
                }
            }

            tts?.setSpeechRate(_playbackState.value.speechRate)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post {
                        val segment = activePlaylist.getOrNull(currentIndex)
                        _playbackState.update {
                            it.copy(
                                isSpeaking = true,
                                isPaused = false,
                                currentSegmentId = segment?.id ?: utteranceId,
                                currentTitle = segment?.title ?: "در حال خواندن راه حل",
                                currentSubtitle = segment?.subtitle ?: "",
                                currentStepNumber = segment?.stepNumber,
                                currentPlaylistIndex = currentIndex,
                                totalSegments = activePlaylist.size
                            )
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        handleSegmentCompleted()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        handleSegmentError("خطا در خواندن بخش صوتی")
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post {
                        handleSegmentError("خطای شماره $errorCode در موتور صوتی")
                    }
                }
            })

            _playbackState.update {
                it.copy(
                    isInitialized = true,
                    isPersianSupported = isAvailable,
                    engineName = engine,
                    noticeMessage = if (!isAvailable) {
                        "برای تلفظ بهتر زبان فارسی، بسته صوتی فارسی Google Speech را فعال کنید."
                    } else null
                )
            }
        } else {
            _playbackState.update {
                it.copy(
                    isInitialized = false,
                    isPersianSupported = false,
                    noticeMessage = "موتور تبدیل متن به گفتار در دسترس نیست."
                )
            }
        }
    }

    /**
     * Prepares and starts playback of the complete step-by-step solution.
     */
    fun playFullSolution(solution: ProblemSolution) {
        val segments = buildSegmentsFromSolution(solution)
        if (segments.isEmpty()) return

        activePlaylist = segments
        currentIndex = 0
        speakCurrentSegment()
    }

    /**
     * Reads out a single specific step requested by the student.
     */
    fun playSingleStep(step: SolutionStep) {
        val text = buildStepSpokenText(step)
        val segment = SpeechSegment(
            id = "step_${step.stepNumber}",
            title = "مرحله ${step.stepNumber}: ${step.title}",
            subtitle = step.explanation.take(60),
            textToSpeak = text,
            stepNumber = step.stepNumber
        )

        activePlaylist = listOf(segment)
        currentIndex = 0
        speakCurrentSegment()
    }

    /**
     * Reads out just the final answer card.
     */
    fun playFinalAnswer(finalAnswer: String) {
        val text = "پاسخ نهایی مسئله: ${formatMathForSpeech(finalAnswer)}"
        val segment = SpeechSegment(
            id = "final_answer",
            title = "پاسخ نهایی مسئله",
            subtitle = finalAnswer.take(50),
            textToSpeak = text
        )

        activePlaylist = listOf(segment)
        currentIndex = 0
        speakCurrentSegment()
    }

    /**
     * Reads out Einstein's educational tip.
     */
    fun playStudentTip(tip: String) {
        val text = "نکته مفهومی و طلایی انیشتین: ${formatMathForSpeech(tip)}"
        val segment = SpeechSegment(
            id = "student_tip",
            title = "نکته طلایی انیشتین",
            subtitle = tip.take(50),
            textToSpeak = text
        )

        activePlaylist = listOf(segment)
        currentIndex = 0
        speakCurrentSegment()
    }

    /**
     * Toggle play / pause of current narration.
     */
    fun togglePlayPause() {
        val state = _playbackState.value
        if (state.isSpeaking) {
            pause()
        } else if (state.isPaused && currentIndex in activePlaylist.indices) {
            speakCurrentSegment()
        }
    }

    /**
     * Pauses the current speech narration.
     */
    fun pause() {
        tts?.stop()
        _playbackState.update {
            it.copy(isSpeaking = false, isPaused = true)
        }
    }

    /**
     * Stops speech completely and resets the playlist.
     */
    fun stop() {
        tts?.stop()
        activePlaylist = emptyList()
        currentIndex = -1
        _playbackState.update {
            it.copy(
                isSpeaking = false,
                isPaused = false,
                currentSegmentId = null,
                currentTitle = "",
                currentSubtitle = "",
                currentStepNumber = null,
                currentPlaylistIndex = -1,
                totalSegments = 0
            )
        }
    }

    /**
     * Skips forward to the next segment in the solution playlist.
     */
    fun next() {
        if (currentIndex < activePlaylist.size - 1) {
            tts?.stop()
            currentIndex++
            speakCurrentSegment()
        } else {
            stop()
        }
    }

    /**
     * Skips back to the previous segment.
     */
    fun previous() {
        if (currentIndex > 0) {
            tts?.stop()
            currentIndex--
            speakCurrentSegment()
        }
    }

    /**
     * Adjusts the playback speed (e.g. 0.75f, 1.0f, 1.25f).
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
        _playbackState.update { it.copy(speechRate = rate) }
    }

    /**
     * Opens Android System Text-to-Speech settings so user can download Persian voice.
     */
    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("SolutionSpeechManager", "Cannot open settings", e)
            }
        }
    }

    /**
     * Releases TTS resources when leaving screen.
     */
    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("SolutionSpeechManager", "Error shutting down TTS", e)
        }
        tts = null
    }

    // ========================================================================
    // INTERNAL HELPERS
    // ========================================================================

    private fun speakCurrentSegment() {
        val segment = activePlaylist.getOrNull(currentIndex) ?: run {
            stop()
            return
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, segment.id)
        }

        val speechText = segment.textToSpeak
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, segment.id)
    }

    private fun handleSegmentCompleted() {
        if (currentIndex < activePlaylist.size - 1) {
            currentIndex++
            speakCurrentSegment()
        } else {
            stop()
        }
    }

    private fun handleSegmentError(msg: String) {
        Log.w("SolutionSpeechManager", "TTS Error: $msg")
        if (currentIndex < activePlaylist.size - 1) {
            currentIndex++
            speakCurrentSegment()
        } else {
            stop()
        }
    }

    private fun buildSegmentsFromSolution(solution: ProblemSolution): List<SpeechSegment> {
        val list = mutableListOf<SpeechSegment>()

        // 1. Intro & Identified Problem
        val introText = StringBuilder()
            .append("توضیح صوتی حل مسئله. ")
            .append("موضوع: ${solution.subject}، مبحث ${solution.topic}. ")
            .append("صورت مسئله: ${formatMathForSpeech(solution.identifiedProblem)}. ")
        if (solution.givenData.isNotEmpty()) {
            introText.append("داده‌های مسئله: ")
            solution.givenData.forEach {
                introText.append("${formatMathForSpeech(it)}، ")
            }
        }
        list.add(
            SpeechSegment(
                id = "intro",
                title = "صورت مسئله و داده‌ها",
                subtitle = "${solution.subject} • ${solution.topic}",
                textToSpeak = introText.toString()
            )
        )

        // 2. Final Answer Highlight
        list.add(
            SpeechSegment(
                id = "final_answer",
                title = "پاسخ نهایی مسئله",
                subtitle = solution.finalAnswer.take(50),
                textToSpeak = "پاسخ نهایی به دست آمده: ${formatMathForSpeech(solution.finalAnswer)}"
            )
        )

        // 3. Formulas used (if any)
        if (solution.formulasUsed.isNotEmpty()) {
            val formulaText = StringBuilder("فرمول‌های به کار رفته در حل این مسئله: ")
            solution.formulasUsed.forEach {
                formulaText.append("${formatMathForSpeech(it)}، ")
            }
            list.add(
                SpeechSegment(
                    id = "formulas",
                    title = "فرمول‌ها و روابط فیزیکی و ریاضی",
                    subtitle = solution.formulasUsed.joinToString(", ").take(60),
                    textToSpeak = formulaText.toString()
                )
            )
        }

        // 4. Step by step cards
        solution.steps.forEach { step ->
            list.add(
                SpeechSegment(
                    id = "step_${step.stepNumber}",
                    title = "مرحله ${step.stepNumber}: ${step.title}",
                    subtitle = step.explanation.take(60),
                    textToSpeak = buildStepSpokenText(step),
                    stepNumber = step.stepNumber
                )
            )
        }

        // 5. Einstein Student tip (if present)
        if (solution.studentTip.isNotBlank()) {
            list.add(
                SpeechSegment(
                    id = "student_tip",
                    title = "نکته طلایی انیشتین",
                    subtitle = solution.studentTip.take(60),
                    textToSpeak = "نکته طلایی و مفهومی برای امتحان: ${formatMathForSpeech(solution.studentTip)}"
                )
            )
        }

        return list
    }

    private fun buildStepSpokenText(step: SolutionStep): String {
        val sb = StringBuilder()
        sb.append("مرحله شماره ${step.stepNumber}: ${step.title}. ")
        if (step.explanation.isNotBlank()) {
            sb.append("${formatMathForSpeech(step.explanation)}. ")
        }
        if (step.calculation.isNotBlank()) {
            sb.append("محاسبه این مرحله: ${formatMathForSpeech(step.calculation)}. ")
        }
        return sb.toString()
    }

    /**
     * Converts math symbols, formulas, and units into natural Persian phonetics
     * so that the Android TTS engine reads mathematical steps fluently.
     */
    private fun formatMathForSpeech(raw: String): String {
        var text = raw

        // Superscript exponents
        text = text.replace("²", " به توان دو ")
            .replace("^2", " به توان دو ")
            .replace("³", " به توان سه ")
            .replace("^3", " به توان سه ")
            .replace("⁴", " به توان چهار ")
            .replace("^4", " به توان چهار ")
            .replace("^n", " به توان اِن ")

        // Common math and physics symbols
        text = text.replace("√", " رادیکال ")
            .replace("π", " پی ")
            .replace("Δ", " دلتا ")
            .replace("∑", " مجموع ")
            .replace("∫", " انتگرال ")
            .replace("±", " مثبت و منفی ")
            .replace("≠", " نامساوی با ")
            .replace("≈", " تقریباً برابر با ")
            .replace("≤", " کوچکتر یا مساوی ")
            .replace("≥", " بزرگتر یا مساوی ")
            .replace("×", " ضرب در ")
            .replace("*", " ضرب در ")
            .replace("÷", " تقسیم بر ")
            .replace("=", " برابر است با ")
            .replace("°C", " درجه سلسیوس ")
            .replace("°", " درجه ")
            .replace("%", " درصد ")

        // Physics units
        text = text.replace("m/s²", " متر بر مجذور ثانیه ")
            .replace("m/s", " متر بر ثانیه ")
            .replace("km/h", " کیلومتر بر ساعت ")
            .replace("kg", " کیلوگرم ")
            .replace("cm", " سانتی‌متر ")
            .replace("mm", " میلی‌متر ")
            .replace("Pa", " پاسکال ")
            .replace("Hz", " هرتز ")

        // Add small pauses around slashes and operators
        text = text.replace("/", " تقسیم بر ")
            .replace("+", " به علاوه ")

        // Clean redundant whitespaces
        return text.replace(Regex("\\s+"), " ").trim()
    }
}
