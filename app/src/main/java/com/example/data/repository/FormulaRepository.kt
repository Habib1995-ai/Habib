package com.example.data.repository

import com.example.data.model.FormulaItem
import com.example.data.model.FormulaVariable

class FormulaRepository {

    fun getAllFormulas(): List<FormulaItem> = listOf(
        // ==================== فیزیک: حرکت‌شناسی (سینماتیک) ====================
        FormulaItem(
            id = "phys_kin_1",
            title = "معادله مکان - زمان در حرکت با شتاب ثابت",
            subject = "فیزیک",
            category = "سینماتیک",
            gradeLevel = "دوازدهم",
            formula = "Δx = ½ a·t² + v₀·t",
            variables = listOf(
                FormulaVariable("Δx", "جابه‌جایی بر حسب متر (m)"),
                FormulaVariable("a", "شتاب ثابت بر حسب متر بر مجذور ثانیه (m/s²)"),
                FormulaVariable("t", "مدت زمان حرکت بر حسب ثانیه (s)"),
                FormulaVariable("v₀", "سرعت اولیه بر حسب متر بر ثانیه (m/s)")
            ),
            usageTip = "وقتی سرعت نهایی در صورت مسئله داده نشده و مجهول هم نیست، بهترین انتخاب همین فرمول است."
        ),
        FormulaItem(
            id = "phys_kin_2",
            title = "معادله مستقل از زمان (سرعت - جابه‌جایی)",
            subject = "فیزیک",
            category = "سینماتیک",
            gradeLevel = "دوازدهم",
            formula = "v² - v₀² = 2a·Δx",
            variables = listOf(
                FormulaVariable("v", "سرعت نهایی (m/s)"),
                FormulaVariable("v₀", "سرعت اولیه (m/s)"),
                FormulaVariable("a", "شتاب حرکت (m/s²)"),
                FormulaVariable("Δx", "جابه‌جایی (m)")
            ),
            usageTip = "هر زمان حرفی از «زمان (t)» در مسئله نبود، مستقیماً سراغ معادله مستقل از زمان بروید."
        ),
        FormulaItem(
            id = "phys_kin_3",
            title = "سرعت در حرکت با شتاب ثابت",
            subject = "فیزیک",
            category = "سینماتیک",
            gradeLevel = "دوازدهم",
            formula = "v = a·t + v₀",
            variables = listOf(
                FormulaVariable("v", "سرعت در لحظه t (m/s)"),
                FormulaVariable("v₀", "سرعت اولیه (m/s)"),
                FormulaVariable("a", "شتاب (m/s²)"),
                FormulaVariable("t", "زمان (s)")
            ),
            usageTip = "در سقوط آزاد از حالت سکون، v₀ = 0 و شتاب برابر g ≈ 10 m/s² در نظر گرفته می‌شود."
        ),

        // ==================== فیزیک: دینامیک ====================
        FormulaItem(
            id = "phys_dyn_1",
            title = "قانون دوم نیوتون",
            subject = "فیزیک",
            category = "دینامیک",
            gradeLevel = "دوازدهم",
            formula = "F_net = m · a",
            variables = listOf(
                FormulaVariable("F_net", "برآیند نیروهای وارد بر جسم بر حسب نیوتون (N)"),
                FormulaVariable("m", "جرم جسم بر حسب کیلوگرم (kg)"),
                FormulaVariable("a", "شتاب حرکت جسم (m/s²)")
            ),
            usageTip = "همیشه ابتدا جهت حرکت را مثبت در نظر بگیرید و نیروهای هم‌جهت را منهای نیروهای خلاف جهت کنید."
        ),
        FormulaItem(
            id = "phys_dyn_2",
            title = "نیروی اصطکاک جنبشی",
            subject = "فیزیک",
            category = "دینامیک",
            gradeLevel = "دوازدهم",
            formula = "f_k = μ_k · N",
            variables = listOf(
                FormulaVariable("f_k", "نیروی اصطکاک جنبشی (N)"),
                FormulaVariable("μ_k", "ضریب اصطکاک جنبشی (بدون یکا)"),
                FormulaVariable("N", "نیروی عمودی تکیه‌گاه (N)")
            ),
            usageTip = "در سطح افقی بدون نیروی عمودی اضافی، نیروی تکیه‌گاه برابر وزن (N = m·g) است."
        ),
        FormulaItem(
            id = "phys_dyn_3",
            title = "قانون گرانش عمومی نیوتون",
            subject = "فیزیک",
            category = "دینامیک",
            gradeLevel = "دوازدهم",
            formula = "F = G · (m₁ · m₂) / r²",
            variables = listOf(
                FormulaVariable("F", "نیروی جاذبه گرانشی بین دو جرم (N)"),
                FormulaVariable("G", "ثابت جهانی گرانش (6.67×10⁻¹¹ N·m²/kg²)"),
                FormulaVariable("m₁, m₂", "جرم دو جسم (kg)"),
                FormulaVariable("r", "فاصله بین مراکز دو جسم (m)")
            ),
            usageTip = "اگر فاصله دو جسم ۲ برابر شود، نیروی گرانش ۱/۴ برابر می‌گردد."
        ),

        // ==================== فیزیک: کار، انرژی و توان ====================
        FormulaItem(
            id = "phys_work_1",
            title = "کار نیروی ثابت",
            subject = "فیزیک",
            category = "کار و انرژی",
            gradeLevel = "دهم",
            formula = "W = F · d · cos(θ)",
            variables = listOf(
                FormulaVariable("W", "کار انجام‌شده بر حسب ژول (J)"),
                FormulaVariable("F", "بزرگی نیرو (N)"),
                FormulaVariable("d", "بزرگی جابه‌جایی (m)"),
                FormulaVariable("θ", "زاویه بین بردار نیرو و جابه‌جایی")
            ),
            usageTip = "نیروی عمود بر جابه‌جایی (θ = 90°) هیچ کاری انجام نمی‌دهد (cos 90° = 0)."
        ),
        FormulaItem(
            id = "phys_work_2",
            title = "قضیه کار و انرژی جنبشی",
            subject = "فیزیک",
            category = "کار و انرژی",
            gradeLevel = "دهم",
            formula = "W_total = ΔK = ½ m·v₂² - ½ m·v₁²",
            variables = listOf(
                FormulaVariable("W_total", "کار کل (برآیند کار تمام نیروها) بر حسب ژول (J)"),
                FormulaVariable("ΔK", "تغییرات انرژی جنبشی (J)"),
                FormulaVariable("m", "جرم جسم (kg)"),
                FormulaVariable("v₁, v₂", "سرعت اولیه و نهایی (m/s)")
            ),
            usageTip = "بسیار کاربردی برای مسائل بدون زمان که در آن‌ها سرعت و ارتفاع یا مسافت تغییر می‌کند."
        ),
        FormulaItem(
            id = "phys_work_3",
            title = "انرژی پتانسیل گرانشی",
            subject = "فیزیک",
            category = "کار و انرژی",
            gradeLevel = "دهم",
            formula = "U = m · g · h",
            variables = listOf(
                FormulaVariable("U", "انرژی پتانسیل گرانشی (J)"),
                FormulaVariable("m", "جرم جسم (kg)"),
                FormulaVariable("g", "شتاب گرانش (≈ 10 m/s²)"),
                FormulaVariable("h", "ارتفاع از سطح مبنا (m)")
            ),
            usageTip = "در نبود اصطکاک و نیروهای اتلافی، مجموع انرژی مکانیکی (K + U) ثابت می‌ماند."
        ),

        // ==================== فیزیک: الکتریسیته و مدارهای جریان مستقیم ====================
        FormulaItem(
            id = "phys_elec_1",
            title = "قانون کولن (نیروی الکتریکی)",
            subject = "فیزیک",
            category = "الکتریسیته",
            gradeLevel = "یازدهم",
            formula = "F = k · |q₁ · q₂| / r²",
            variables = listOf(
                FormulaVariable("F", "نیروی الکتریکی بین دو بار نقطه‌ای (N)"),
                FormulaVariable("k", "ثابت کولن (9×10⁹ N·m²/C²)"),
                FormulaVariable("q₁, q₂", "مقدار بارهای الکتریکی بر حسب کولن (C)"),
                FormulaVariable("r", "فاصله بین دو بار بر حسب متر (m)")
            ),
            usageTip = "در جایگذاری قانون کولن، علامت منفی بار را وارد فرمول نکنید؛ علامت فقط جهت جاذبه یا دافعه را مشخص می‌کند."
        ),
        FormulaItem(
            id = "phys_elec_2",
            title = "قانون اهم",
            subject = "فیزیک",
            category = "مدارهای الکتریکی",
            gradeLevel = "یازدهم",
            formula = "V = R · I",
            variables = listOf(
                FormulaVariable("V", "اختلاف پتانسیل دو سر مقاومت بر حسب ولت (V)"),
                FormulaVariable("R", "مقاومت الکتریکی بر حسب اهم (Ω)"),
                FormulaVariable("I", "شدت جریان الکتریکی بر حسب آمپر (A)")
            ),
            usageTip = "در بستن متوالی، جریان همه مقاومت‌ها برابر و در بستن موازی، ولتاژ همه شاخه‌ها برابر است."
        ),
        FormulaItem(
            id = "phys_elec_3",
            title = "توان الکتریکی مصرفی",
            subject = "فیزیک",
            category = "مدارهای الکتریکی",
            gradeLevel = "یازدهم",
            formula = "P = V·I = R·I² = V² / R",
            variables = listOf(
                FormulaVariable("P", "توان الکتریکی بر حسب وات (W)"),
                FormulaVariable("V", "اختلاف پتانسیل (V)"),
                FormulaVariable("I", "شدت جریان (A)"),
                FormulaVariable("R", "مقاومت (Ω)")
            ),
            usageTip = "برای مقایسه توان لامپ‌های موازی از V²/R و برای مدارهای متوالی از R·I² استفاده کنید."
        ),

        // ==================== فیزیک: موج و فشار ====================
        FormulaItem(
            id = "phys_press_1",
            title = "فشار در عمق مایع ساکن",
            subject = "فیزیک",
            category = "فشار و شاره",
            gradeLevel = "دهم",
            formula = "P = P₀ + ρ · g · h",
            variables = listOf(
                FormulaVariable("P", "فشار کل در عمق h بر حسب پاسکال (Pa)"),
                FormulaVariable("P₀", "فشار هوای بالای سطح مایع (≈ 10⁵ Pa)"),
                FormulaVariable("ρ", "چگالی مایع (kg/m³)"),
                FormulaVariable("h", "عمق از سطح آزاد مایع (m)")
            ),
            usageTip = "فشار مایع فقط به عمق، چگالی و شتاب گرانش بستگی دارد و به شکل ظرف ربطی ندارد."
        ),
        FormulaItem(
            id = "phys_wave_1",
            title = "تندی انتشار موج",
            subject = "فیزیک",
            category = "موج و نوسان",
            gradeLevel = "دوازدهم",
            formula = "v = λ · f = λ / T",
            variables = listOf(
                FormulaVariable("v", "تندی انتشار موج (m/s)"),
                FormulaVariable("λ", "طول موج بر حسب متر (m)"),
                FormulaVariable("f", "بسامد یا فرکانس بر حسب هرتز (Hz)"),
                FormulaVariable("T", "دوره تناوب بر حسب ثانیه (s)")
            ),
            usageTip = "هنگام عبور موج از یک محیط به محیط دیگر، بسامد (f) ثابت می‌ماند اما تندی و طول موج تغییر می‌کنند."
        ),

        // ==================== ریاضی: جبر و معادلات ====================
        FormulaItem(
            id = "math_alg_1",
            title = "فرمول کلی و دلتای معادله درجه دوم",
            subject = "ریاضی",
            category = "جبر و معادلات",
            gradeLevel = "دهم",
            formula = "Δ = b² - 4ac  |  x = (-b ± √Δ) / 2a",
            variables = listOf(
                FormulaVariable("a, b, c", "ضرایب معادله استاندارد ax² + bx + c = 0"),
                FormulaVariable("Δ", "ممیز معادله (اگر Δ>0 دو ریشه، Δ=0 یک ریشه مضاعف، Δ<0 بدون ریشه حقیقی)")
            ),
            usageTip = "اگر a + b + c = 0 باشد یکی از ریشه‌ها ۱ و ریشه دیگر c/a است؛ سریع‌ترین راه برای تست!"
        ),
        FormulaItem(
            id = "math_alg_2",
            title = "مجموع و حاصل‌ضرب ریشه‌های معادله درجه دوم",
            subject = "ریاضی",
            category = "جبر و معادلات",
            gradeLevel = "دهم",
            formula = "S = x₁ + x₂ = -b / a  |  P = x₁ · x₂ = c / a",
            variables = listOf(
                FormulaVariable("S", "مجموع دو ریشه معادله درجه دوم"),
                FormulaVariable("P", "حاصل‌ضرب دو ریشه"),
                FormulaVariable("a, b, c", "ضرایب معادله ax² + bx + c = 0")
            ),
            usageTip = "برای به دست آوردن x₁² + x₂² بدون یافتن ریشه‌ها از اتحاد S² - 2P استفاده کنید."
        ),
        FormulaItem(
            id = "math_alg_3",
            title = "اتحادهای مهم جبری (مربع و مزدوج)",
            subject = "ریاضی",
            category = "جبر و اتحادها",
            gradeLevel = "نهم و دهم",
            formula = "(a ± b)² = a² ± 2ab + b²  |  a² - b² = (a - b)(a + b)",
            variables = listOf(
                FormulaVariable("a, b", "عبارت‌های جبری اول و دوم")
            ),
            usageTip = "در ساده‌سازی کسرهای گویا، ابتدا همیشه دنبال اتحاد مزدوج و فاکتورگیری بگردید."
        ),
        FormulaItem(
            id = "math_alg_4",
            title = "قوانین پایه لگاریتم",
            subject = "ریاضی",
            category = "لگاریتم و نما",
            gradeLevel = "یازدهم",
            formula = "log(A·B) = log A + log B  |  log(Aⁿ) = n·log A",
            variables = listOf(
                FormulaVariable("A, B", "عبارت‌های مثبت جلوی لگاریتم (A > 0, B > 0)"),
                FormulaVariable("n", "توان عدد یا متغیر")
            ),
            usageTip = "همیشه شرط دامنه لگاریتم را چک کنید: پایه باید مثبت و مخالف ۱، و جلوی لگاریتم اکیداً مثبت باشد."
        ),

        // ==================== ریاضی: مثلثات ====================
        FormulaItem(
            id = "math_trig_1",
            title = "رابطه فیثاغورسی مثلثات",
            subject = "ریاضی",
            category = "مثلثات",
            gradeLevel = "دهم و یازدهم",
            formula = "sin²(θ) + cos²(θ) = 1",
            variables = listOf(
                FormulaVariable("θ", "زاویه بر حسب رادیان یا درجه"),
                FormulaVariable("sin(θ), cos(θ)", "سینوس و کسینوس زاویه θ")
            ),
            usageTip = "اگر sin و ربع مثلثاتی زاویه معلوم باشد، با این رابطه بلافاصله cos به دست می‌آید."
        ),
        FormulaItem(
            id = "math_trig_2",
            title = "قضیه کسینوس‌ها در مثلث",
            subject = "ریاضی",
            category = "مثلثات و هندسه",
            gradeLevel = "دهم",
            formula = "a² = b² + c² - 2bc·cos(A)",
            variables = listOf(
                FormulaVariable("a, b, c", "طول اضلاع مثلث"),
                FormulaVariable("A", "زاویه روبه‌رو به ضلع a")
            ),
            usageTip = "تعمیم قضیه فیثاغورس برای هر مثلث دلخواه! وقتی دو ضلع و زاویه بین آن‌ها را دارید بسیار عالی است."
        ),
        FormulaItem(
            id = "math_trig_3",
            title = "نسبت‌های دو برابر کمان",
            subject = "ریاضی",
            category = "مثلثات",
            gradeLevel = "یازدهم و دوازدهم",
            formula = "sin(2x) = 2sin(x)cos(x)  |  cos(2x) = cos²(x) - sin²(x)",
            variables = listOf(
                FormulaVariable("x", "کمان یا زاویه اولیه")
            ),
            usageTip = "در حل معادلات مثلثاتی و انتگرال‌گیری از توابع مثلثاتی بسیار پرکاربرد است."
        ),

        // ==================== ریاضی: حسابان (مشتق، حد، دنباله) ====================
        FormulaItem(
            id = "math_calc_1",
            title = "قواعد پایه مشتق‌گیری",
            subject = "ریاضی",
            category = "حسابان و مشتق",
            gradeLevel = "دوازدهم",
            formula = "(xⁿ)' = n·xⁿ⁻¹  |  (u·v)' = u'v + uv'  |  (u/v)' = (u'v - uv') / v²",
            variables = listOf(
                FormulaVariable("xⁿ", "تابع توانی"),
                FormulaVariable("u, v", "توابع مشتق‌پذیر بر حسب x")
            ),
            usageTip = "مشتق تابع در نقطه x₀ برابر شیب خط مماس بر نمودار در آن نقطه است."
        ),
        FormulaItem(
            id = "math_calc_2",
            title = "جمله عمومی و مجموع دنباله حسابی",
            subject = "ریاضی",
            category = "دنباله‌ها",
            gradeLevel = "دهم و دوازدهم",
            formula = "aₙ = a₁ + (n - 1)d  |  Sₙ = (n / 2)·(a₁ + aₙ)",
            variables = listOf(
                FormulaVariable("a₁", "جمله اول دنباله"),
                FormulaVariable("d", "قدر نسبت دنباله حسابی (تفاوت دو جمله متوالی)"),
                FormulaVariable("n", "شماره جمله"),
                FormulaVariable("Sₙ", "مجموع n جمله اول")
            ),
            usageTip = "در هر دنباله حسابی، جمله وسط برابر میانگین حسابی دو جمله مجاورش است."
        ),
        FormulaItem(
            id = "math_calc_3",
            title = "فاصله دو نقطه و شیب خط در صفحه",
            subject = "ریاضی",
            category = "هندسه تحلیلی",
            gradeLevel = "دهم و یازدهم",
            formula = "d = √((x₂ - x₁)² + (y₂ - y₁)²)  |  m = (y₂ - y₁) / (x₂ - x₁)",
            variables = listOf(
                FormulaVariable("d", "فاصله اقلیدسی بین دو نقطه A و B"),
                FormulaVariable("m", "شیب خط گذرنده از دو نقطه"),
                FormulaVariable("(x₁,y₁), (x₂,y₂)", "مختصات دو نقطه در دستگاه مختصات")
            ),
            usageTip = "دو خط غیرعمودی موازی‌اند اگر m₁ = m₂ و عمودند اگر m₁ · m₂ = -1."
        ),

        // ==================== فیزیک نجوم و اخترفیزیک ====================
        FormulaItem(
            id = "astro_1",
            title = "قانون سوم کپلر (دوره تناوب مداری)",
            subject = "فیزیک نجوم",
            category = "مکانیک سماوی",
            gradeLevel = "المپیاد و دانشگاهی",
            formula = "T² = (4π² / G·M) · a³",
            variables = listOf(
                FormulaVariable("T", "دوره تناوب گردش سیاره به دور ستاره (s)"),
                FormulaVariable("a", "نیم‌قطر اطول مدار بیضوی (m)"),
                FormulaVariable("M", "جرم ستاره مرکزی (kg)"),
                FormulaVariable("G", "ثابت جهانی گرانش (6.67×10⁻¹¹)")
            ),
            usageTip = "اگر واحد a بر حسب واحد نجومی (AU) و T بر حسب سال زمینی باشد، برای منظومه شمسی T² = a³ برقرار است."
        ),
        FormulaItem(
            id = "astro_2",
            title = "سرعت فرار از میدان گرانشی و شعاع شوارتزشیلد",
            subject = "فیزیک نجوم",
            category = "گرانش و سیاهچاله",
            gradeLevel = "المپیاد و فیزیک نوین",
            formula = "v_esc = √(2GM / R)  |  R_s = 2GM / c²",
            variables = listOf(
                FormulaVariable("v_esc", "حداقل سرعت فرار از گرانش جرم آسمانی (m/s)"),
                FormulaVariable("R_s", "شعاع افق رویداد سیاهچاله (شعاع شوارتزشیلد)"),
                FormulaVariable("c", "سرعت نور در خلاء (3×10⁸ m/s)")
            ),
            usageTip = "سرعت فرار دقیقاً √2 برابر سرعت مداری دایره‌ای در همان فاصله است و به جرم فضاپیما وابسته نیست!"
        ),

        // ==================== هندسه فضایی ====================
        FormulaItem(
            id = "geom_1",
            title = "حجم و مساحت کره و مخروط",
            subject = "هندسه",
            category = "احجام فضایی",
            gradeLevel = "دهم تا دوازدهم",
            formula = "V_sphere = (4/3)πR³  |  V_cone = (1/3)πr²h",
            variables = listOf(
                FormulaVariable("R, r", "شعاع کره یا شعاع قاعده مخروط"),
                FormulaVariable("h", "ارتفاع عمودی مخروط از مرکز قاعده تا رأس")
            ),
            usageTip = "حجم مخروط دقیقاً یک‌سوم حجم استوانه هم‌قاعده و هم‌ارتفاع با خودش است."
        ),
        FormulaItem(
            id = "geom_2",
            title = "حجم هرم و قطر فضایی مکعب",
            subject = "هندسه",
            category = "احجام فضایی",
            gradeLevel = "یازدهم و دوازدهم",
            formula = "V_pyramid = (1/3)·S_base·h  |  d_cube = a√3",
            variables = listOf(
                FormulaVariable("S_base", "مساحت قاعده هرم"),
                FormulaVariable("h", "ارتفاع عمودی هرم"),
                FormulaVariable("a", "طول ضلع مکعب")
            ),
            usageTip = "قطر فضایی مکعب از دو بار استفاده متوالی قضیه فیثاغورس در دو بعد و سه بعد به دست می‌آید."
        ),

        // ==================== شیمی حرفه‌ای ====================
        FormulaItem(
            id = "chem_1",
            title = "معادله حالت گازهای ایده‌آل",
            subject = "شیمی",
            category = "استوکیومتری و گازها",
            gradeLevel = "دهم و یازدهم",
            formula = "P · V = n · R · T",
            variables = listOf(
                FormulaVariable("P", "فشار گاز بر حسب اتمسفر (atm) یا پاسکال"),
                FormulaVariable("V", "حجم گاز بر حسب لیتر (L) یا مترمکعب"),
                FormulaVariable("n", "تعداد مول گاز (mol)"),
                FormulaVariable("R", "ثابت جهانی گازها (0.082 L·atm/mol·K)"),
                FormulaVariable("T", "دمای مطلق بر حسب کلوین (K = °C + 273)")
            ),
            usageTip = "همیشه پیش از محاسبه، دما را به کلوین تبدیل کنید تا پاسخ اشتباه نشود."
        ),
        FormulaItem(
            id = "chem_2",
            title = "انرژی آزاد گیبس و خودبه‌خودی بودن واکنش",
            subject = "شیمی",
            category = "ترمودینامیک و سینتیک",
            gradeLevel = "دوازدهم",
            formula = "ΔG = ΔH - T · ΔS  |  pH = -log[H⁺]",
            variables = listOf(
                FormulaVariable("ΔG", "تغییرات انرژی آزاد گیبس (اگر منفی باشد واکنش خودبه‌خودی است)"),
                FormulaVariable("ΔH", "تغییرات آنتالپی واکنش (kJ/mol)"),
                FormulaVariable("ΔS", "تغییرات آنتروپی یا بی‌نظمی (J/mol·K)")
            ),
            usageTip = "دقت کنید یکای ΔH معمولاً کیلوژول و یکای ΔS ژول است؛ پیش از کم کردن یکاها را یکسان کنید!"
        )
    )

    fun getSubjects(): List<String> = listOf("همه", "فیزیک", "ریاضی", "فیزیک نجوم", "هندسه", "شیمی")
}
