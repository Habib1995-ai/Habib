package com.example.data.repository

import com.example.data.model.QuizQuestion
import com.example.data.model.QuizResult

class QuizRepository {

    private val allQuestions = listOf(
        QuizQuestion(
            id = "q1",
            subject = "فیزیک",
            topic = "سقوط آزاد",
            questionText = "گلوله‌ای از ارتفاع ۲۰ متری سطح زمین رها می‌شود. سرعت آن هنگام برخورد به زمین چند متر بر ثانیه است؟ (g = 10 m/s² و اصطکاک هوا صفر)",
            options = listOf("۱۰ m/s", "۲۰ m/s", "۱۵ m/s", "۲۵ m/s"),
            correctIndex = 1,
            formulaUsed = "v² - v₀² = 2gΔh  یا  v = √(2gh)",
            stepByStepExplanation = "۱- چون گلوله رها شده، سرعت اولیه v₀ = 0 است.\n۲- با جایگذاری در رابطه مستقل از زمان: v² = 2 × 10 × 20 = 400\n۳- جذر ۴۰۰ برابر با ۲۰ متر بر ثانیه است."
        ),
        QuizQuestion(
            id = "q2",
            subject = "ریاضی",
            topic = "معادله درجه دوم",
            questionText = "مجموع ریشه‌های معادله درجه دوم x² - 5x + 6 = 0 کدام است؟",
            options = listOf("۶", "۵", "-۵", "-۶"),
            correctIndex = 1,
            formulaUsed = "S = x₁ + x₂ = -b / a",
            stepByStepExplanation = "۱- ضرایب معادله عبارتند از: a = 1, b = -5, c = 6\n۲- طبق رابطه مجموع ریشه‌ها: S = -(-5) / 1 = 5\n۳- (ریشه‌ها ۲ و ۳ هستند که حاصل‌جمع آن‌ها ۵ می‌شود)."
        ),
        QuizQuestion(
            id = "q3",
            subject = "فیزیک",
            topic = "قانون دوم نیوتون و اصطکاک",
            questionText = "به جسمی به جرم ۴ کیلوگرم نیروی افقی ۲۰ نیوتون وارد می‌شود. اگر نیروی اصطکاک جنبشی ۸ نیوتون باشد، شتاب جسم چند m/s² است؟",
            options = listOf("۵ m/s²", "۲ m/s²", "۳ m/s²", "۴ m/s²"),
            correctIndex = 2,
            formulaUsed = "F_net = m × a  ⇒  F - f_k = m × a",
            stepByStepExplanation = "۱- نیروی خالص وارد بر جسم برابر است با: F_net = 20 - 8 = 12 N\n۲- طبق قانون دوم نیوتون: a = F_net / m = 12 / 4 = 3 m/s²."
        ),
        QuizQuestion(
            id = "q4",
            subject = "ریاضی",
            topic = "مشتق تابع",
            questionText = "مشتق تابع f(x) = 3x² - 4x + 7 در نقطه x = 2 برابر است با:",
            options = listOf("۸", "۱۲", "۱۰", "۶"),
            correctIndex = 0,
            formulaUsed = "f'(x) = d/dx(axⁿ) = n·a·xⁿ⁻¹",
            stepByStepExplanation = "۱- مشتق تابع عبارت است از: f'(x) = 6x - 4\n۲- مقدار مشتق در x = 2: f'(2) = 6(2) - 4 = 12 - 4 = 8."
        ),
        QuizQuestion(
            id = "q5",
            subject = "فیزیک",
            topic = "مدار الکتریکی و قانون اهم",
            questionText = "مقاومت الکتریکی یک لامپ که با ولتاژ ۲۲۰ ولت کار می‌کند و جریان ۲ آمپر از آن می‌گذرد، چند اهم است؟",
            options = listOf("۴۴۰ Ω", "۱۱۰ Ω", "۲۲۰ Ω", "۵۵ Ω"),
            correctIndex = 1,
            formulaUsed = "V = I × R  ⇒  R = V / I",
            stepByStepExplanation = "۱- طبق قانون اهم رابطه بین ولتاژ، جریان و مقاومت R = V / I است.\n۲- جایگذاری اعداد: R = 220 / 2 = 110 اهم."
        ),
        QuizQuestion(
            id = "q6",
            subject = "ریاضی",
            topic = "مثلثات",
            questionText = "مقدار عبارت sin²(30°) + cos²(30°) کدام است؟",
            options = listOf("۰", "۰.۵", "۱", "√۳/۲"),
            correctIndex = 2,
            formulaUsed = "sin²(θ) + cos²(θ) = 1 (اتحاد بنیادین مثلثات)",
            stepByStepExplanation = "۱- برای هر زاویه دلخواه θ همواره sin²(θ) + cos²(θ) = 1 است.\n۲- به طور مشخص sin(30°) = 1/2 (مربع آن 1/4) و cos(30°) = √3/2 (مربع آن 3/4) است و 1/4 + 3/4 = 1."
        ),
        QuizQuestion(
            id = "q7",
            subject = "فیزیک",
            topic = "انرژی جنبشی",
            questionText = "اگر سرعت اتومبیلی ۲ برابر شود، انرژی جنبشی آن چند برابر خواهد شد؟",
            options = listOf("۲ برابر", "۴ برابر", "۸ برابر", "تغییری نمی‌کند"),
            correctIndex = 1,
            formulaUsed = "K = 1/2 · m · v²",
            stepByStepExplanation = "۱- انرژی جنبشی متناسب با مجذور (مربع) سرعت است: K ∝ v²\n۲- بنابراین با دو برابر شدن سرعت، انرژی جنبشی (2)² = 4 برابر می‌شود."
        ),
        QuizQuestion(
            id = "q8",
            subject = "ریاضی",
            topic = "لگاریتم",
            questionText = "حاصل عبارت log₂(32) برابر با کدام گزینه است؟",
            options = listOf("۴", "۵", "۶", "۱۶"),
            correctIndex = 1,
            formulaUsed = "log_a(aᵇ) = b",
            stepByStepExplanation = "۱- عدد ۳۲ را به صورت توان ۲ می‌نویسیم: 32 = 2⁵\n۲- بنابراین log₂(2⁵) = 5 است."
        ),
        QuizQuestion(
            id = "q9",
            subject = "فیزیک",
            topic = "فشار و ویژگی ماده",
            questionText = "فشار ناشی از ستون آب به عمق ۱۰ متر چقدر است؟ (چگالی آب ۱۰۰۰ kg/m³ و g = 10 m/s²)",
            options = listOf("10,000 Pa", "100,000 Pa", "1,000 Pa", "50,000 Pa"),
            correctIndex = 1,
            formulaUsed = "P = ρ × g × h",
            stepByStepExplanation = "۱- فشار مایعات از رابطه P = ρgh به دست می‌آید.\n۲- P = 1000 × 10 × 10 = 100,000 پاسکال (معادل ۱ اتمسفر)."
        ),
        QuizQuestion(
            id = "q10",
            subject = "ریاضی",
            topic = "دنباله حسابی",
            questionText = "در یک دنباله حسابی با جمله اول a₁ = 3 و قدرنسبت d = 4، جمله دهم کدام است؟",
            options = listOf("۳۶", "۳۹", "۴۳", "۴۰"),
            correctIndex = 1,
            formulaUsed = "a_n = a₁ + (n - 1) × d",
            stepByStepExplanation = "۱- فرمول جمله عمومی دنباله حسابی: a_n = a₁ + (n - 1)d\n۲- a₁₀ = 3 + (10 - 1) × 4 = 3 + (9 × 4) = 3 + 36 = 39."
        )
    )

    fun getRandomQuiz(subjectFilter: String? = null, count: Int = 5): List<QuizQuestion> {
        val pool = if (subjectFilter.isNullOrBlank() || subjectFilter == "همه") {
            allQuestions
        } else {
            allQuestions.filter { it.subject == subjectFilter }
        }
        return pool.shuffled().take(count)
    }

    fun evaluateQuiz(total: Int, correct: Int, timeSeconds: Long): QuizResult {
        val percentage = if (total > 0) ((correct.toDouble() / total.toDouble()) * 100).toInt() else 0

        val (title, msg) = when {
            percentage == 100 -> Pair(
                "شاهکار! نمره کامل 🌟",
                "عالی بود! تمام مفاهیم و فرمول‌ها را به درستی درک کرده‌اید و به همه سوالات پاسخ صحیح دادید."
            )
            percentage >= 80 -> Pair(
                "بسیار عالی و مسلط 👏",
                "عملکرد بسیار خوبی داشتید! با مرور راه‌حل تشریحی سوالاتی که اشتباه زدید، می‌توانید به نمره ۱۰۰ برسید."
            )
            percentage >= 60 -> Pair(
                "خوب، اما جای پیشرفت داری! 📚",
                "بیش از نیمی از سوالات را درست حل کردید. حتماً بخش یادداشت‌ها و فرمول‌های کلیدی را مرور کنید."
            )
            else -> Pair(
                "نیاز به تمرین بیشتر و مرور فرمول‌ها 💪",
                "اصلاً نگران نباش! مراحل حل هر سوال را با دقت بررسی کن و فرمول‌ها را در بخش یادداشت‌ها یادداشت کن و دوباره امتحان بده."
            )
        }

        return QuizResult(
            totalQuestions = total,
            correctAnswers = correct,
            timeSpentSeconds = timeSeconds,
            percentage = percentage,
            feedbackMessage = msg,
            feedbackTitle = title
        )
    }
}
