package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.ai.MathPhysicsAiService
import com.example.data.local.ProblemDao
import com.example.data.local.ProblemSolutionEntity
import com.example.data.local.QuizAttemptDao
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.StudyNoteDao
import com.example.data.local.StudyNoteEntity
import com.example.data.model.ProblemSolution
import com.example.data.model.SampleProblem
import com.example.data.model.StudyNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProblemRepository(
    private val problemDao: ProblemDao,
    private val studyNoteDao: StudyNoteDao,
    private val quizAttemptDao: QuizAttemptDao,
    private val aiService: MathPhysicsAiService = MathPhysicsAiService(),
    val quizRepository: QuizRepository = QuizRepository(),
    val formulaRepository: FormulaRepository = FormulaRepository()
) {
    val history: Flow<List<ProblemSolution>> = problemDao.getAllSolvedProblems().map { list ->
        list.map { it.toDomain() }
    }

    val favorites: Flow<List<ProblemSolution>> = problemDao.getFavoriteProblems().map { list ->
        list.map { it.toDomain() }
    }

    val allNotes: Flow<List<StudyNote>> = studyNoteDao.getAllNotes().map { list ->
        list.map { it.toDomain() }
    }

    val quizAttempts: Flow<List<QuizAttemptEntity>> = quizAttemptDao.getAllAttempts()

    suspend fun saveQuizAttempt(attempt: QuizAttemptEntity) {
        quizAttemptDao.insertAttempt(attempt)
    }

    fun getNotesForProblem(problemId: String): Flow<List<StudyNote>> {
        return studyNoteDao.getNotesForProblem(problemId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveNote(note: StudyNote) {
        studyNoteDao.insertNote(StudyNoteEntity.fromDomain(note))
    }

    suspend fun deleteNote(id: String) {
        studyNoteDao.deleteNoteById(id)
    }

    suspend fun solve(bitmap: Bitmap?, text: String?): Result<ProblemSolution> {
        val result = aiService.solveProblem(bitmap, text)
        result.onSuccess { solution ->
            problemDao.insertProblem(ProblemSolutionEntity.fromDomain(solution))
        }
        return result
    }

    suspend fun saveSolution(solution: ProblemSolution) {
        problemDao.insertProblem(ProblemSolutionEntity.fromDomain(solution))
    }

    suspend fun toggleFavorite(solution: ProblemSolution) {
        val newStatus = !solution.isFavorite
        problemDao.updateFavoriteStatus(solution.id, newStatus)
    }

    suspend fun deleteSolution(id: String) {
        problemDao.deleteProblemById(id)
    }

    suspend fun clearHistory() {
        problemDao.clearAll()
    }

    fun getSampleProblems(): List<SampleProblem> {
        return listOf(
            SampleProblem(
                id = "sample_1",
                title = "حرکت با شتاب ثابت و سقوط آزاد",
                subject = "فیزیک دهم و دوازدهم",
                topic = "سینماتیک و سقوط آزاد",
                problemText = "گلوله‌ای به جرم ۲ کیلوگرم از بالای ساختمانی به ارتفاع ۴۵ متری رها می‌شود. با فرض g = 10 m/s² و صرف‌نظر از اصطکاک هوا:\n۱- سرعت برخورد گلوله به زمین چقدر است؟\n۲- مدت زمان سقوط گلوله را محاسبه کنید.",
                iconName = "speed"
            ),
            SampleProblem(
                id = "sample_2",
                title = "حل معادله درجه دوم و ریشه‌ها",
                subject = "ریاضی دهم",
                topic = "معادلات درجه دوم (دلتا)",
                problemText = "معادله درجه دوم 2x² - 7x + 3 = 0 را با استفاده از روش دلتا (روش کلی) حل کرده و ریشه‌ها را بیابید.",
                iconName = "functions"
            ),
            SampleProblem(
                id = "sample_3",
                title = "قانون دوم نیوتون و نیروی اصطکاک",
                subject = "فیزیک دوازدهم",
                topic = "دینامیک و قوانین حرکت نیوتون",
                problemText = "جعبه‌ای به جرم ۶ کیلوگرم بر روی یک سطح افقی ساکن است. نیروی افقی ۳۶ نیوتون به آن وارد می‌شود. اگر ضریب اصطکاک جنبشی بین جعبه و سطح برابر با ۰.۲ باشد (g = 10 m/s²)، شتاب حرکت جعبه را به دست آورید.",
                iconName = "fitness_center"
            ),
            SampleProblem(
                id = "sample_4",
                title = "مشتق تابع و شیب خط مماس",
                subject = "حسابان و ریاضی دوازدهم",
                topic = "مشتق و هندسه منحنی",
                problemText = "برای تابع f(x) = 2x³ - 5x² + 4x - 1:\n۱- ضابطه تابع مشتق f'(x) را به دست آورید.\n۲- شیب خط مماس بر نمودار را در نقطه x = 2 محاسبه کنید.",
                iconName = "timeline"
            ),
            SampleProblem(
                id = "sample_5",
                title = "مدار الکتریکی و قانون اهم",
                subject = "فیزیک یازدهم",
                topic = "جریان الکتریکی و مدارهای DC",
                problemText = "دو مقاومت R₁ = 4 Ω و R₂ = 8 Ω به صورت متوالی به یک باتری با اختلاف پتانسیل ۳۶ ولت متصل شده‌اند. جریان کل مدار و افت ولتاژ دو سر مقاومت ۴ اهم را محاسبه فرمایید.",
                iconName = "bolt"
            ),
            SampleProblem(
                id = "sample_6",
                title = "فیزیک نجوم: مدار کپلر و سرعت فرار",
                subject = "فیزیک نجوم و اخترفیزیک",
                topic = "قوانین کپلر و گرانش کیهانی",
                problemText = "سیاره‌ای در مداری دایره‌ای به شعاع ربع واحد نجومی (r = 1.5×10¹¹ m) به دور ستاره‌ای به جرم خورشید (M = 2×10³⁰ kg) می‌چرخد. با فرض G = 6.67×10⁻¹¹ N·m²/kg²، سرعت مداری سیاره و سرعت فرار از آن فاصله را محاسبه و در فضای سه‌بعدی توضیح دهید.",
                iconName = "public"
            ),
            SampleProblem(
                id = "sample_7",
                title = "شیمی حرفه‌ای: هندسه سه‌بعدی متان و بنزن",
                subject = "شیمی آلی و ساختار مولکولی",
                topic = "هیبریداسیون sp³ و زاویه پیوندی VSEPR",
                problemText = "ساختار سه‌بعدی مولکول متان (CH₄) را بر اساس نظریه دافعه جفت الکترون‌های لایه ظرفیت (VSEPR) تحلیل کنید؛ نوع هیبریداسیون اتم کربن مرکزی، تعداد قلمروهای الکترونی، شکل فضایی و زاویه دقیق پیوندی H-C-H را شرح دهید.",
                iconName = "science"
            ),
            SampleProblem(
                id = "sample_8",
                title = "هندسه فضایی: هرم و قطر فضایی",
                subject = "هندسه فضایی",
                topic = "حجم و مساحت جانبی هرم منتظم",
                problemText = "یک هرم منتظم مربع‌القاعده دارای ضلع قاعده a = 6 cm و ارتفاع عمودی h = 4 cm است:\n۱- حجم هرم چقدر است؟\n۲- ارتفاع وجه جانبی (آپوتم) و مساحت کل هرم را با نمایش سه‌بعدی محاسبه کنید.",
                iconName = "view_in_ar"
            )
        )
    }
}
