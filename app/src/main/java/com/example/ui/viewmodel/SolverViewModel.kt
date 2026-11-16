package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.QuizAttemptEntity
import com.example.data.model.FormulaItem
import com.example.data.model.ProblemSolution
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizResult
import com.example.data.model.SampleProblem
import com.example.data.model.StudyNote
import com.example.data.repository.ProblemRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "SolverViewModel"

sealed interface SolverUiState {
    data object Idle : SolverUiState
    data class Loading(val message: String = "در حال پردازش و استخراج مسئله...") : SolverUiState
    data class Success(val solution: ProblemSolution) : SolverUiState
    data class Error(val message: String) : SolverUiState
}

enum class AppScreen {
    HOME,
    SOLUTION,
    HISTORY,
    QUIZ,
    NOTES,
    CAMERA_SCAN,
    FORMULA_REFERENCE,
    DASHBOARD
}

data class DailyActivityPoint(
    val dayLabel: String,
    val solvedCount: Int,
    val quizCount: Int
) {
    val total: Int get() = solvedCount + quizCount
}

data class TopicMasteryStat(
    val topic: String,
    val subject: String,
    val solvedCount: Int,
    val masteryPercent: Int,
    val levelLabel: String
)

data class DashboardStats(
    val totalProblemsSolved: Int = 0,
    val mathProblemsCount: Int = 0,
    val physicsProblemsCount: Int = 0,
    val otherProblemsCount: Int = 0,
    val favoriteProblemsCount: Int = 0,
    val totalNotesCount: Int = 0,
    val totalQuizzesTaken: Int = 0,
    val averageQuizScore: Int = 0,
    val topicsMasteredCount: Int = 0,
    val activeStreakDays: Int = 0,
    val topicBreakdown: List<TopicMasteryStat> = emptyList(),
    val weeklyActivity: List<DailyActivityPoint> = emptyList(),
    val recentQuizScores: List<Int> = emptyList()
)

class SolverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProblemRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = ProblemRepository(
            problemDao = database.problemDao(),
            studyNoteDao = database.studyNoteDao(),
            quizAttemptDao = database.quizAttemptDao()
        )
    }

    private val _uiState = MutableStateFlow<SolverUiState>(SolverUiState.Idle)
    val uiState: StateFlow<SolverUiState> = _uiState.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _problemInputText = MutableStateFlow("")
    val problemInputText: StateFlow<String> = _problemInputText.asStateFlow()

    private val _showGuideDialog = MutableStateFlow(false)
    val showGuideDialog: StateFlow<Boolean> = _showGuideDialog.asStateFlow()

    private val _activeSolution = MutableStateFlow<ProblemSolution?>(null)
    val activeSolution: StateFlow<ProblemSolution?> = _activeSolution.asStateFlow()

    // History and samples
    val history = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sampleProblems: List<SampleProblem> = repository.getSampleProblems()

    val formulaReferences: List<FormulaItem> = repository.formulaRepository.getAllFormulas()

    // Notes
    val allNotes = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Quiz Attempts & Dashboard Analytics
    val quizAttempts = repository.quizAttempts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dashboardStats: StateFlow<DashboardStats> = combine(
        history,
        quizAttempts,
        allNotes
    ) { solvedList, attempts, notes ->
        computeDashboardStats(solvedList, attempts, notes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    private var activeQuizSubjectFilter: String = "همه"

    // ==========================================
    // Quiz State
    // ==========================================
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    private val _quizTimerSeconds = MutableStateFlow(0L)
    val quizTimerSeconds: StateFlow<Long> = _quizTimerSeconds.asStateFlow()

    private val _isQuizActive = MutableStateFlow(false)
    val isQuizActive: StateFlow<Boolean> = _isQuizActive.asStateFlow()

    private val _quizResult = MutableStateFlow<QuizResult?>(null)
    val quizResult: StateFlow<QuizResult?> = _quizResult.asStateFlow()

    private var timerJob: Job? = null

    fun onImageSelected(bitmap: Bitmap?) {
        _selectedBitmap.value = bitmap
    }

    fun onTextChanged(text: String) {
        _problemInputText.value = text
    }

    fun appendFormulaSymbol(symbol: String) {
        _problemInputText.value += symbol
    }

    fun clearInput() {
        _selectedBitmap.value = null
        _problemInputText.value = ""
        _uiState.value = SolverUiState.Idle
    }

    fun setGuideDialogVisible(visible: Boolean) {
        _showGuideDialog.value = visible
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun loadSample(sample: SampleProblem) {
        _problemInputText.value = sample.problemText
        _selectedBitmap.value = null
    }

    fun useFormulaInSolver(formulaText: String) {
        _problemInputText.value = "با استفاده از فرمول $formulaText یک مسئله نمونه حل کن یا این رابطه را بررسی کن:\n"
        _selectedBitmap.value = null
        _currentScreen.value = AppScreen.HOME
    }

    fun viewSavedSolution(solution: ProblemSolution) {
        _activeSolution.value = solution
        _uiState.value = SolverUiState.Success(solution)
        _currentScreen.value = AppScreen.SOLUTION
    }

    fun solve() {
        if (_uiState.value is SolverUiState.Loading) return

        val bitmap = _selectedBitmap.value
        val text = _problemInputText.value.trim()

        if (bitmap == null && text.isBlank()) {
            _uiState.value = SolverUiState.Error("لطفاً با دوربین عکسی از مسئله بگیرید یا متن مسئله را تایپ فرمایید.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SolverUiState.Loading("هوش مصنوعی در حال شناسایی و حل فرمول‌های مسئله است...")
            try {
                val result = repository.solve(bitmap, text.ifBlank { null })
                result.fold(
                    onSuccess = { solution ->
                        _activeSolution.value = solution
                        _uiState.value = SolverUiState.Success(solution)
                        _currentScreen.value = AppScreen.SOLUTION
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Solve error", error)
                        _uiState.value = SolverUiState.Error(
                            error.localizedMessage ?: "متأسفانه در حل مسئله خطایی رخ داد. لطفاً تصویر واضح‌تری بگیرید."
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during solving", e)
                _uiState.value = SolverUiState.Error(
                    e.localizedMessage ?: "خطای غیرمنتظره‌ای رخ داد."
                )
            }
        }
    }

    fun toggleFavorite(solution: ProblemSolution) {
        viewModelScope.launch {
            repository.toggleFavorite(solution)
            val updated = solution.copy(isFavorite = !solution.isFavorite)
            if (_activeSolution.value?.id == solution.id) {
                _activeSolution.value = updated
            }
        }
    }

    fun deleteSolution(id: String) {
        viewModelScope.launch {
            repository.deleteSolution(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // ==========================================
    // Notes Operations
    // ==========================================
    fun saveNote(
        title: String,
        content: String,
        relatedProblemId: String? = null,
        relatedProblemTitle: String? = null,
        subjectTag: String = "عمومی",
        colorIndex: Int = 0
    ) {
        viewModelScope.launch {
            val note = StudyNote(
                title = title.ifBlank { "یادداشت جدید" },
                content = content,
                relatedProblemId = relatedProblemId,
                relatedProblemTitle = relatedProblemTitle,
                subjectTag = subjectTag,
                colorIndex = colorIndex
            )
            repository.saveNote(note)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // ==========================================
    // Quiz Operations
    // ==========================================
    fun startQuiz(subjectFilter: String? = "همه", count: Int = 5) {
        timerJob?.cancel()
        activeQuizSubjectFilter = subjectFilter ?: "همه"
        val questions = repository.quizRepository.getRandomQuiz(subjectFilter, count)
        _quizQuestions.value = questions
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _quizTimerSeconds.value = 0L
        _quizResult.value = null
        _isQuizActive.value = true

        timerJob = viewModelScope.launch {
            while (_isQuizActive.value) {
                delay(1000L)
                _quizTimerSeconds.value++
            }
        }
    }

    fun selectQuizOption(questionIndex: Int, optionIndex: Int) {
        val currentMap = _selectedAnswers.value.toMutableMap()
        currentMap[questionIndex] = optionIndex
        _selectedAnswers.value = currentMap
    }

    fun nextQuizQuestion() {
        if (_currentQuestionIndex.value < _quizQuestions.value.size - 1) {
            _currentQuestionIndex.value++
        }
    }

    fun previousQuizQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
        }
    }

    fun submitQuiz() {
        _isQuizActive.value = false
        timerJob?.cancel()

        val questions = _quizQuestions.value
        val answers = _selectedAnswers.value
        var correctCount = 0

        questions.forEachIndexed { index, q ->
            if (answers[index] == q.correctIndex) {
                correctCount++
            }
        }

        val timeSpent = _quizTimerSeconds.value
        val result = repository.quizRepository.evaluateQuiz(
            total = questions.size,
            correct = correctCount,
            timeSeconds = timeSpent
        )
        _quizResult.value = result

        // Persist quiz attempt to Room for the Progress Dashboard
        if (questions.isNotEmpty()) {
            viewModelScope.launch {
                repository.saveQuizAttempt(
                    QuizAttemptEntity(
                        subjectFilter = activeQuizSubjectFilter,
                        totalQuestions = questions.size,
                        correctAnswers = correctCount,
                        percentage = result.percentage,
                        timeSpentSeconds = timeSpent
                    )
                )
            }
        }
    }

    fun resetQuiz() {
        timerJob?.cancel()
        _isQuizActive.value = false
        _quizQuestions.value = emptyList()
        _selectedAnswers.value = emptyMap()
        _quizResult.value = null
        _quizTimerSeconds.value = 0L
    }

    private fun computeDashboardStats(
        solvedList: List<ProblemSolution>,
        attempts: List<QuizAttemptEntity>,
        notes: List<StudyNote>
    ): DashboardStats {
        val mathCount = solvedList.count {
            it.subject.contains("ریاضی") || it.subject.contains("حسابان") ||
                it.subject.contains("هندسه") || it.topic.contains("معادله") ||
                it.topic.contains("هرم") || it.topic.contains("حجم")
        }
        val physicsCount = solvedList.count {
            it.subject.contains("فیزیک") || it.subject.contains("نجوم") ||
                it.subject.contains("اختر") || it.topic.contains("حرکت") ||
                it.topic.contains("نیوتون") || it.topic.contains("مدار") ||
                it.topic.contains("کپلر")
        }
        val otherCount = (solvedList.size - mathCount - physicsCount).coerceAtLeast(0)

        val avgScore = if (attempts.isNotEmpty()) {
            attempts.map { it.percentage }.average().toInt()
        } else 0

        // Group by topic to compute topic mastery
        val topicGroups = solvedList.groupBy {
            it.topic.ifBlank { it.subject.ifBlank { "مسائل علمی" } }.trim()
        }
        val topicStats = topicGroups.map { (topic, items) ->
            val count = items.size
            val masteryPercent = (count * 25).coerceIn(25, 100)
            val level = when {
                masteryPercent >= 75 -> "مسلط"
                masteryPercent >= 50 -> "در حال پیشرفت"
                else -> "شروع یادگیری"
            }
            TopicMasteryStat(
                topic = topic,
                subject = items.firstOrNull()?.subject ?: "ریاضی و فیزیک",
                solvedCount = count,
                masteryPercent = masteryPercent,
                levelLabel = level
            )
        }.sortedByDescending { it.solvedCount }

        val masteredTopics = topicStats.count { it.masteryPercent >= 50 }

        // Build last 7 days activity window
        val calendar = Calendar.getInstance()
        val dayPoints = mutableListOf<DailyActivityPoint>()
        var streak = 0
        var streakBroken = false

        for (daysAgo in 6 downTo 0) {
            val dayStart = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayEnd = (dayStart.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val startMillis = dayStart.timeInMillis
            val endMillis = dayEnd.timeInMillis

            val solvedToday = solvedList.count { it.timestamp in startMillis until endMillis }
            val quizToday = attempts.count { it.timestamp in startMillis until endMillis }

            val persianDayLabel = when (daysAgo) {
                0 -> "امروز"
                1 -> "دیروز"
                else -> persianWeekdayShort(dayStart.get(Calendar.DAY_OF_WEEK))
            }

            dayPoints.add(
                DailyActivityPoint(
                    dayLabel = persianDayLabel,
                    solvedCount = solvedToday,
                    quizCount = quizToday
                )
            )
        }

        // Calculate active streak ending today or yesterday
        for (i in dayPoints.indices.reversed()) {
            if (dayPoints[i].total > 0) {
                if (!streakBroken) streak++
            } else if (i < dayPoints.lastIndex) {
                streakBroken = true
            }
        }

        return DashboardStats(
            totalProblemsSolved = solvedList.size,
            mathProblemsCount = mathCount,
            physicsProblemsCount = physicsCount,
            otherProblemsCount = otherCount,
            favoriteProblemsCount = solvedList.count { it.isFavorite },
            totalNotesCount = notes.size,
            totalQuizzesTaken = attempts.size,
            averageQuizScore = avgScore,
            topicsMasteredCount = masteredTopics,
            activeStreakDays = streak,
            topicBreakdown = topicStats,
            weeklyActivity = dayPoints,
            recentQuizScores = attempts.take(7).reversed().map { it.percentage }
        )
    }

    private fun persianWeekdayShort(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "۱شنبه"
            Calendar.MONDAY -> "۲شنبه"
            Calendar.TUESDAY -> "۳شنبه"
            Calendar.WEDNESDAY -> "۴شنبه"
            Calendar.THURSDAY -> "۵شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> "-"
        }
    }
}
