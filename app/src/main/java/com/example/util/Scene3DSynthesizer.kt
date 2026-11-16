package com.example.util

import com.example.data.model.Edge3D
import com.example.data.model.Node3D
import com.example.data.model.ProblemSolution
import com.example.data.model.Scene3DSpec
import com.example.data.model.Scene3DType
import com.example.data.model.Step3DFocus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Scene3DSynthesizer {

    /**
     * Resolves a complete, render-ready Scene3DSpec for any ProblemSolution.
     * Uses AI-generated scene3d when present, hydrating missing geometry from shapeVariant,
     * or synthesizes a domain-matched 3D scene from the problem's subject/topic/text.
     */
    fun resolveScene(solution: ProblemSolution): Scene3DSpec {
        val aiScene = solution.scene3d
        if (aiScene != null) {
            if (aiScene.nodes.isNotEmpty()) {
                return if (aiScene.stepFocuses.isEmpty() && solution.steps.isNotEmpty()) {
                    aiScene.copy(stepFocuses = buildFallbackStepFocuses(solution, aiScene.nodes.size, aiScene.edges.size))
                } else {
                    aiScene
                }
            }
            val template = buildTemplateByVariant(
                variant = aiScene.shapeVariant,
                sceneType = aiScene.sceneType,
                title = aiScene.title,
                summary = aiScene.spatialSummary,
                formula = aiScene.formulaLabel.ifBlank { solution.formulasUsed.firstOrNull() ?: "" }
            )
            return aiScene.copy(
                nodes = template.nodes,
                edges = template.edges,
                stepFocuses = aiScene.stepFocuses.ifEmpty {
                    buildFallbackStepFocuses(solution, template.nodes.size, template.edges.size)
                }
            )
        }

        return inferSceneFromProblem(solution)
    }

    private fun inferSceneFromProblem(solution: ProblemSolution): Scene3DSpec {
        val text = "${solution.subject} ${solution.topic} ${solution.identifiedProblem}".lowercase()
        val formula = solution.formulasUsed.firstOrNull().orEmpty()

        val spec = when {
            // Astrophysics / Astronomy
            containsAny(text, "نجوم", "اختر", "کپلر", "سیاره", "مدار", "خورشید", "ماه", "ستاره", "کهکشان", "گرانش", "سرعت فرار", "سیاهچاله", "orbit", "kepler", "planet", "gravity", "black hole", "astro") -> {
                when {
                    containsAny(text, "سیاهچاله", "نسبیت", "خمیدگی", "فضازمان", "شوارتزشیلد", "gravity well", "black hole") ->
                        buildTemplateByVariant("GRAVITY_WELL", Scene3DType.ASTRONOMY_ASTROPHYSICS, "خمیدگی فضازمان و چاه گرانشی", "نمایش سه‌بعدی خمیدگی شبکه فضازمان در اطراف جرم سنگین و مسیر نور/ذره", formula)
                    containsAny(text, "دوتایی", "binary", "دو ستاره") ->
                        buildTemplateByVariant("BINARY_ORBIT", Scene3DType.ASTRONOMY_ASTROPHYSICS, "مدار سیستم ستاره‌ای دوتایی", "چرخش دو جرم آسمانی به دور مرکز جرم مشترک در فضای سه‌بعدی", formula)
                    else ->
                        buildTemplateByVariant("ELLIPTICAL_ORBIT", Scene3DType.ASTRONOMY_ASTROPHYSICS, "مدار بیضوی کپلر و سرعت مداری", "سیاره در مدار بیضوی به دور ستاره مرکزی با بردارهای سرعت مماسی و گرانش مرکزی", formula)
                }
            }

            // Chemistry
            containsAny(text, "شیمی", "مولکول", "پیوند", "اتم", "اوربیتال", "هیبرید", "متان", "آب", "آمونیاک", "بنزن", "دی‌اکسید", "کربن", "الکترون", "vsepr", "ch4", "h2o", "nh3", "co2", "c6h6", "chem") -> {
                when {
                    containsAny(text, "بنزن", "حلقوی", "آروماتیک", "c6h6", "benzene") ->
                        buildTemplateByVariant("BENZENE_RING", Scene3DType.CHEMISTRY_MOLECULE, "ساختار سه‌بعدی حلقه بنزن (C₆H₆)", "حلقه مسطح شش‌ضلعی با پیوندهای یک‌درمیان دوگانه و زاویه ۱۲۰ درجه", formula)
                    containsAny(text, "متان", "چهاروجهی", "ch4", "sp3", "tetrahedral") ->
                        buildTemplateByVariant("METHANE_CH4", Scene3DType.CHEMISTRY_MOLECULE, "ساختار چهاروجهی متان (CH₄)", "کربن مرکزی با هیبریداسیون sp³ و ۴ هیدروژن با زاویه پیوندی ۱۰۹.۵ درجه در فضا", formula)
                    containsAny(text, "آمونیاک", "هرمی", "nh3", "ammonia") ->
                        buildTemplateByVariant("AMMONIA_NH3", Scene3DType.CHEMISTRY_MOLECULE, "هندسه هرمی مثلثی آمونیاک (NH₃)", "نیتروژن مرکزی همراه با جفت الکترون ناپیوندی در راس و زاویه پیوندی ۱۰۷ درجه", formula)
                    containsAny(text, "دی‌اکسید", "خطی", "co2") ->
                        buildTemplateByVariant("CO2_LINEAR", Scene3DType.CHEMISTRY_MOLECULE, "ساختار خطی دی‌اکسید کربن (CO₂)", "کربن مرکزی متصل با دو پیوند دوگانه به اکسیژن‌ها با زاویه پیوندی ۱۸۰ درجه", formula)
                    containsAny(text, "بور", "تراز انرژی", "اتم", "الکترون", "پروتون", "bohr", "atom") ->
                        buildTemplateByVariant("ATOM_BOHR", Scene3DType.CHEMISTRY_MOLECULE, "مدل اتمی و ترازهای انرژی", "هسته مرکزی و چرخش سه‌بعدی الکترون‌ها در مدارهای کوانتیده", formula)
                    else ->
                        buildTemplateByVariant("WATER_H2O", Scene3DType.CHEMISTRY_MOLECULE, "ساختار سه‌بعدی مولکول آب (H₂O)", "اکسیژن مرکزی و دو اتم هیدروژن با زاویه پیوندی ۱۰۴.۵ درجه به دلیل دافعه جفت‌های ناپیوندی", formula)
                }
            }

            // Geometry
            containsAny(text, "هندسه", "حجم", "مساحت جانبی", "هرم", "مخروط", "استوانه", "کره", "مکعب", "منشور", "چهاروجهی", "فضایی", "قطر", "بردار", "cube", "pyramid", "cone", "cylinder", "sphere", "geometry") -> {
                when {
                    containsAny(text, "هرم", "pyramid") ->
                        buildTemplateByVariant("PYRAMID", Scene3DType.GEOMETRY_SOLID, "هرم مربع‌القاعده سه‌بعدی", "نمایش قاعده مربعی، یال‌های جانبی و خط‌چین ارتفاع عمودی h در فضای سه‌بعدی", formula)
                    containsAny(text, "مخروط", "cone") ->
                        buildTemplateByVariant("CONE", Scene3DType.GEOMETRY_SOLID, "مخروط قائم سه‌بعدی", "نمایش شعاع قاعده r، ارتفاع عمودی h و مولد مخروط l", formula)
                    containsAny(text, "استوانه", "cylinder") ->
                        buildTemplateByVariant("CYLINDER", Scene3DType.GEOMETRY_SOLID, "استوانه قائم سه‌بعدی", "نمایش دو قاعده دایره‌ای موازی با شعاع r و ارتفاع h", formula)
                    containsAny(text, "کره", "sphere") ->
                        buildTemplateByVariant("SPHERE", Scene3DType.GEOMETRY_SOLID, "کره و مقاطع فضایی سه‌بعدی", "نمایش مرکز کره، شعاع R و دایره‌های عظیمه در فضا", formula)
                    containsAny(text, "چهاروجهی", "tetrahedron") ->
                        buildTemplateByVariant("TETRAHEDRON", Scene3DType.GEOMETRY_SOLID, "چهاروجهی منتظم سه‌بعدی", "چهار وجه مثلث متساوی‌الاضلاع در فضای سه‌بعدی", formula)
                    containsAny(text, "بردار", "ضرب داخلی", "ضرب خارجی", "vector") ->
                        buildTemplateByVariant("VECTORS_3D", Scene3DType.GEOMETRY_SOLID, "بردارها و زاویه در فضای سه‌بعدی", "نمایش بردارهای فضایی بر روی محورهای X, Y, Z", formula)
                    else ->
                        buildTemplateByVariant("CUBE", Scene3DType.GEOMETRY_SOLID, "مکعب و قطر فضایی در هندسه سه‌بعدی", "نمایش وجوه مکعب، اضلاع a و قطر فضایی قابل چرخش با دست", formula)
                }
            }

            // Physics Mechanics / Fields
            containsAny(text, "فیزیک", "نیوتون", "اصطکاک", "سطح شیبدار", "پرتاب", "سقوط", "مغناطیس", "الکتریک", "مدار", "موج", "نوسان", "پاندول", "آونگ", "شتاب", "نیرو") -> {
                when {
                    containsAny(text, "شیبدار", "اصطکاک", "نیوتون", "جعبه", "incline") ->
                        buildTemplateByVariant("INCLINED_PLANE", Scene3DType.PHYSICS_MECHANICS, "تحلیل سه‌بعدی نیروها روی سطح شیبدار", "تجزیه بردار وزن mg، نیروی عمودی N و نیروی اصطکاک f_k در سه بعد", formula)
                    containsAny(text, "مغناطیس", "الکتریک", "کولن", "مدار", "اهم", "لورنتس", "بار") ->
                        buildTemplateByVariant("MAGNETIC_FIELD", Scene3DType.PHYSICS_MECHANICS, "میدان سه‌بعدی و نیروی الکترومغناطیسی", "نمایش خطوط میدان B، بردار سرعت v و نیروی عمود بر صفحه حرکت", formula)
                    containsAny(text, "آونگ", "پاندول", "نوسان", "موج") ->
                        buildTemplateByVariant("PENDULUM_3D", Scene3DType.PHYSICS_MECHANICS, "حرکت نوسانی و آونگ سه‌بعدی", "تبدیل انرژی پتانسیل و جنبشی همراه با بردار نیروی بازگرداننده", formula)
                    else ->
                        buildTemplateByVariant("PROJECTILE_3D", Scene3DType.PHYSICS_MECHANICS, "مسیر سهموی حرکت و بردارهای سرعت", "نمایش سه‌بعدی مسیر حرکت، ارتفاع اوج و بردارهای سرعت و شتاب گرانش g", formula)
                }
            }

            // Math 3D Surfaces
            else -> {
                when {
                    containsAny(text, "مثلثات", "sin", "cos", "موج") ->
                        buildTemplateByVariant("WAVE_3D", Scene3DType.MATH_SURFACE_3D, "رویه سه‌بعدی تابع مثلثاتی", "نمودار سه‌بعدی نوسان تابع بر حسب محورهای X, Y, Z", formula)
                    containsAny(text, "مشتق", "درجه دوم", "سهمی", "مماس", "x²", "x^2", "parabola") ->
                        buildTemplateByVariant("PARABOLOID_3D", Scene3DType.MATH_SURFACE_3D, "سهمی‌گون و شیب مماس در فضای سه‌بعدی", "نمایش سه‌بعدی انحنای تابع و نقطه اکسترمم / ریشه‌ها", formula)
                    else ->
                        buildTemplateByVariant("SADDLE_3D", Scene3DType.MATH_SURFACE_3D, "نمودار سه‌بعدی تابع جبری", "کاوش سه‌بعدی رویه معادله روی دستگاه مختصات فضایی", formula)
                }
            }
        }

        return spec.copy(
            stepFocuses = buildFallbackStepFocuses(solution, spec.nodes.size, spec.edges.size)
        )
    }

    fun buildTemplateByVariant(
        variant: String,
        sceneType: Scene3DType,
        title: String,
        summary: String,
        formula: String = ""
    ): Scene3DSpec {
        val v = variant.uppercase().trim()
        return when (v) {
            // ---------------- ASTRONOMY & ASTROPHYSICS ----------------
            "SOLAR_SYSTEM", "ELLIPTICAL_ORBIT" -> {
                val nodes = listOf(
                    Node3D(0, "ستاره مرکزی (خورشید)", 0f, 0f, 0f, 0.42f, "#F59E0B"),
                    Node3D(1, "سیاره / قمر (m)", 1.65f, 0f, 0f, 0.20f, "#38BDF8", orbitRadius = 1.65f, orbitSpeed = 0.9f, orbitTiltDeg = 12f),
                    Node3D(2, "اوج / حضیض", -1.9f, 0f, 0.35f, 0.10f, "#A855F7"),
                    Node3D(3, "جرم بیرونی", 2.4f, 0.25f, 0f, 0.14f, "#10B981", orbitRadius = 2.4f, orbitSpeed = 0.55f, orbitTiltDeg = -18f)
                )
                val edges = listOf(
                    Edge3D(1, 0, "F گرانش", isVector = true, isDashed = false, colorHex = "#F43F5E"),
                    Edge3D(0, 2, "فاصله r", isVector = false, isDashed = true, colorHex = "#94A3B8")
                )
                Scene3DSpec(Scene3DType.ASTRONOMY_ASTROPHYSICS, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "BINARY_ORBIT" -> {
                val nodes = listOf(
                    Node3D(0, "مرکز جرم (CM)", 0f, 0f, 0f, 0.11f, "#E2E8F0"),
                    Node3D(1, "ستاره M₁", -1.1f, 0f, 0f, 0.36f, "#F59E0B", orbitRadius = 1.1f, orbitSpeed = 1.0f, orbitTiltDeg = 10f),
                    Node3D(2, "ستاره M₂", 1.55f, 0f, 0f, 0.25f, "#38BDF8", orbitRadius = 1.55f, orbitSpeed = 1.0f, orbitTiltDeg = 10f)
                )
                val edges = listOf(
                    Edge3D(1, 2, "فاصله جدایی a", isVector = false, isDashed = true, colorHex = "#94A3B8"),
                    Edge3D(1, 0, "F₁", isVector = true, colorHex = "#F43F5E"),
                    Edge3D(2, 0, "F₂", isVector = true, colorHex = "#F43F5E")
                )
                Scene3DSpec(Scene3DType.ASTRONOMY_ASTROPHYSICS, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "GRAVITY_WELL", "BLACK_HOLE_LENSING" -> {
                val nodes = listOf(
                    Node3D(0, "سیاهچاله / جرم مرکزی", 0f, -0.85f, 0f, 0.36f, "#1E1B4B"),
                    Node3D(1, "افق رویداد (Rs)", 0.75f, -0.35f, 0f, 0.11f, "#F97316", orbitRadius = 0.8f, orbitSpeed = 1.6f, orbitTiltDeg = 8f),
                    Node3D(2, "ذره آزمایشی / فوتون", 1.85f, 0.25f, 0f, 0.15f, "#38BDF8", orbitRadius = 1.85f, orbitSpeed = 0.85f, orbitTiltDeg = 18f)
                )
                val edges = listOf(
                    Edge3D(2, 0, "کشش فضازمان", isVector = true, colorHex = "#F43F5E"),
                    Edge3D(1, 0, "شعاع شوارتزشیلد", isVector = false, isDashed = true, colorHex = "#FBBF24")
                )
                Scene3DSpec(Scene3DType.ASTRONOMY_ASTROPHYSICS, "GRAVITY_WELL", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            // ---------------- PROFESSIONAL CHEMISTRY ----------------
            "WATER_H2O" -> {
                val rad = (104.5 / 2.0) * PI / 180.0
                val bond = 1.45f
                val hx = (bond * sin(rad)).toFloat()
                val hy = (-bond * cos(rad)).toFloat()
                val nodes = listOf(
                    Node3D(0, "O (اکسیژن)", 0f, 0.2f, 0f, 0.34f, "#EF4444"),
                    Node3D(1, "H₁", -hx, hy + 0.2f, 0f, 0.20f, "#F8FAFC"),
                    Node3D(2, "H₂", hx, hy + 0.2f, 0f, 0.20f, "#F8FAFC"),
                    Node3D(3, "جفت ناپیوندی", -0.55f, 1.05f, 0.55f, 0.13f, "#A855F7"),
                    Node3D(4, "جفت ناپیوندی", 0.55f, 1.05f, -0.55f, 0.13f, "#A855F7")
                )
                val edges = listOf(
                    Edge3D(0, 1, "پیوند کووالانسی", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 2, "۱۰۴.۵°", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 3, "", isDashed = true, colorHex = "#C084FC"),
                    Edge3D(0, 4, "", isDashed = true, colorHex = "#C084FC")
                )
                Scene3DSpec(Scene3DType.CHEMISTRY_MOLECULE, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "METHANE_CH4" -> {
                val d = 1.35f
                val nodes = listOf(
                    Node3D(0, "C (sp³)", 0f, 0f, 0f, 0.33f, "#334155"),
                    Node3D(1, "H₁", 0f, d, 0f, 0.20f, "#F8FAFC"),
                    Node3D(2, "H₂", d * 0.94f, -d * 0.33f, 0f, 0.20f, "#F8FAFC"),
                    Node3D(3, "H₃", -d * 0.47f, -d * 0.33f, d * 0.81f, 0.20f, "#F8FAFC"),
                    Node3D(4, "H₄", -d * 0.47f, -d * 0.33f, -d * 0.81f, 0.20f, "#F8FAFC")
                )
                val edges = listOf(
                    Edge3D(0, 1, "۱۰۹.۵°", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 2, "C-H", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 3, "C-H", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 4, "C-H", bondOrder = 1, colorHex = "#CBD5E1")
                )
                Scene3DSpec(Scene3DType.CHEMISTRY_MOLECULE, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "AMMONIA_NH3" -> {
                val d = 1.35f
                val nodes = listOf(
                    Node3D(0, "N (sp³)", 0f, 0.35f, 0f, 0.32f, "#3B82F6"),
                    Node3D(1, "H₁", d * 0.94f, -0.45f, 0f, 0.20f, "#F8FAFC"),
                    Node3D(2, "H₂", -d * 0.47f, -0.45f, d * 0.81f, 0.20f, "#F8FAFC"),
                    Node3D(3, "H₃", -d * 0.47f, -0.45f, -d * 0.81f, 0.20f, "#F8FAFC"),
                    Node3D(4, "جفت ناپیوندی", 0f, 1.35f, 0f, 0.16f, "#A855F7")
                )
                val edges = listOf(
                    Edge3D(0, 1, "۱۰۷°", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 2, "N-H", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 3, "N-H", bondOrder = 1, colorHex = "#CBD5E1"),
                    Edge3D(0, 4, "ناپیوندی", isDashed = true, colorHex = "#C084FC")
                )
                Scene3DSpec(Scene3DType.CHEMISTRY_MOLECULE, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "CO2_LINEAR" -> {
                val nodes = listOf(
                    Node3D(0, "C (sp)", 0f, 0f, 0f, 0.32f, "#334155"),
                    Node3D(1, "O₁", -1.65f, 0f, 0f, 0.30f, "#EF4444"),
                    Node3D(2, "O₂", 1.65f, 0f, 0f, 0.30f, "#EF4444")
                )
                val edges = listOf(
                    Edge3D(0, 1, "۱۸۰° (پیوند دوگانه)", bondOrder = 2, colorHex = "#F59E0B"),
                    Edge3D(0, 2, "C=O", bondOrder = 2, colorHex = "#F59E0B")
                )
                Scene3DSpec(Scene3DType.CHEMISTRY_MOLECULE, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "BENZENE_RING" -> {
                val rC = 1.25f
                val rH = 2.05f
                val nodes = mutableListOf<Node3D>()
                for (i in 0 until 6) {
                    val a = i * (PI / 3.0)
                    nodes.add(Node3D(i, "C${i + 1}", (rC * cos(a)).toFloat(), 0f, (rC * sin(a)).toFloat(), 0.25f, "#334155"))
                }
                for (i in 0 until 6) {
                    val a = i * (PI / 3.0)
                    nodes.add(Node3D(6 + i, "H", (rH * cos(a)).toFloat(), 0f, (rH * sin(a)).toFloat(), 0.15f, "#F8FAFC"))
                }
                val edges = mutableListOf<Edge3D>()
                for (i in 0 until 6) {
                    val next = (i + 1) % 6
                    val order = if (i % 2 == 0) 2 else 1
                    edges.add(Edge3D(i, next, if (i == 0) "۱۲۰°" else "", bondOrder = order, colorHex = if (order == 2) "#38BDF8" else "#CBD5E1"))
                    edges.add(Edge3D(i, 6 + i, "", bondOrder = 1, colorHex = "#94A3B8"))
                }
                Scene3DSpec(Scene3DType.CHEMISTRY_MOLECULE, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "ATOM_BOHR", "ORBITAL_HYBRID" -> {
                val nodes = listOf(
                    Node3D(0, "هسته (+Z)", 0f, 0f, 0f, 0.34f, "#EF4444"),
                    Node3D(1, "e⁻ (n=1)", 0.95f, 0f, 0f, 0.14f, "#38BDF8", orbitRadius = 0.95f, orbitSpeed = 1.8f, orbitTiltDeg = 20f),
                    Node3D(2, "e⁻ (n=2)", -1.65f, 0f, 0f, 0.15f, "#22D3EE", orbitRadius = 1.65f, orbitSpeed = -1.1f, orbitTiltDeg = -35f),
                    Node3D(3, "e⁻ (n=2)", 0f, 0f, 1.65f, 0.15f, "#22D3EE", orbitRadius = 1.65f, orbitSpeed = 1.25f, orbitTiltDeg = 55f)
                )
                val edges = listOf(
                    Edge3D(0, 1, "r₁", isDashed = true, colorHex = "#64748B"),
                    Edge3D(0, 2, "جهش کوانتومی", isVector = true, isDashed = true, colorHex = "#FBBF24")
                )
                Scene3DSpec(Scene3DType.CHEMISTRY_MOLECULE, "ATOM_BOHR", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            // ---------------- 3D GEOMETRY SOLIDS ----------------
            "CUBE", "PRISM" -> {
                val s = 1.0f
                val nodes = listOf(
                    Node3D(0, "A", -s, -s, -s, 0.12f, "#38BDF8"),
                    Node3D(1, "B", s, -s, -s, 0.12f, "#38BDF8"),
                    Node3D(2, "C", s, -s, s, 0.12f, "#38BDF8"),
                    Node3D(3, "D", -s, -s, s, 0.12f, "#38BDF8"),
                    Node3D(4, "A'", -s, s, -s, 0.12f, "#6366F1"),
                    Node3D(5, "B'", s, s, -s, 0.12f, "#6366F1"),
                    Node3D(6, "C'", s, s, s, 0.12f, "#6366F1"),
                    Node3D(7, "D'", -s, s, s, 0.12f, "#6366F1")
                )
                val edges = listOf(
                    Edge3D(0, 1, "a"), Edge3D(1, 2, ""), Edge3D(2, 3, ""), Edge3D(3, 0, ""),
                    Edge3D(4, 5, ""), Edge3D(5, 6, ""), Edge3D(6, 7, ""), Edge3D(7, 4, ""),
                    Edge3D(0, 4, "h"), Edge3D(1, 5, ""), Edge3D(2, 6, ""), Edge3D(3, 7, ""),
                    Edge3D(0, 6, "قطر فضایی d", isDashed = true, colorHex = "#F59E0B")
                )
                Scene3DSpec(Scene3DType.GEOMETRY_SOLID, "CUBE", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "PYRAMID" -> {
                val s = 1.15f
                val h = 1.35f
                val nodes = listOf(
                    Node3D(0, "A", -s, -0.8f, -s, 0.12f, "#38BDF8"),
                    Node3D(1, "B", s, -0.8f, -s, 0.12f, "#38BDF8"),
                    Node3D(2, "C", s, -0.8f, s, 0.12f, "#38BDF8"),
                    Node3D(3, "D", -s, -0.8f, s, 0.12f, "#38BDF8"),
                    Node3D(4, "S (رأس)", 0f, h, 0f, 0.16f, "#F59E0B"),
                    Node3D(5, "O (مرکز)", 0f, -0.8f, 0f, 0.10f, "#10B981")
                )
                val edges = listOf(
                    Edge3D(0, 1, "a"), Edge3D(1, 2, ""), Edge3D(2, 3, ""), Edge3D(3, 0, ""),
                    Edge3D(0, 4, "یال جانبی"), Edge3D(1, 4, ""), Edge3D(2, 4, ""), Edge3D(3, 4, ""),
                    Edge3D(4, 5, "ارتفاع h", isDashed = true, colorHex = "#10B981")
                )
                Scene3DSpec(Scene3DType.GEOMETRY_SOLID, "PYRAMID", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "TETRAHEDRON" -> {
                val d = 1.3f
                val nodes = listOf(
                    Node3D(0, "A (رأس)", 0f, d, 0f, 0.15f, "#F59E0B"),
                    Node3D(1, "B", d * 0.94f, -d * 0.45f, 0f, 0.13f, "#38BDF8"),
                    Node3D(2, "C", -d * 0.47f, -d * 0.45f, d * 0.81f, 0.13f, "#38BDF8"),
                    Node3D(3, "D", -d * 0.47f, -d * 0.45f, -d * 0.81f, 0.13f, "#38BDF8"),
                    Node3D(4, "O", 0f, -d * 0.45f, 0f, 0.09f, "#10B981")
                )
                val edges = listOf(
                    Edge3D(0, 1, "a"), Edge3D(0, 2, ""), Edge3D(0, 3, ""),
                    Edge3D(1, 2, ""), Edge3D(2, 3, ""), Edge3D(3, 1, ""),
                    Edge3D(0, 4, "h", isDashed = true, colorHex = "#10B981")
                )
                Scene3DSpec(Scene3DType.GEOMETRY_SOLID, "TETRAHEDRON", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "CONE", "CYLINDER", "SPHERE" -> {
                val nodes = when (v) {
                    "CONE" -> listOf(
                        Node3D(0, "O (مرکز قاعده)", 0f, -0.95f, 0f, 0.12f, "#10B981"),
                        Node3D(1, "S (رأس)", 0f, 1.35f, 0f, 0.16f, "#F59E0B"),
                        Node3D(2, "R", 1.15f, -0.95f, 0f, 0.11f, "#38BDF8")
                    )
                    "CYLINDER" -> listOf(
                        Node3D(0, "مرکز پایین", 0f, -1.0f, 0f, 0.11f, "#10B981"),
                        Node3D(1, "مرکز بالا", 0f, 1.0f, 0f, 0.11f, "#10B981"),
                        Node3D(2, "R", 1.1f, -1.0f, 0f, 0.11f, "#38BDF8")
                    )
                    else -> listOf(
                        Node3D(0, "مرکز O", 0f, 0f, 0f, 0.14f, "#F59E0B"),
                        Node3D(1, "شعاع R", 1.3f, 0f, 0f, 0.13f, "#38BDF8"),
                        Node3D(2, "قطب شمال", 0f, 1.3f, 0f, 0.11f, "#10B981")
                    )
                }
                val edges = when (v) {
                    "CONE" -> listOf(
                        Edge3D(0, 1, "h (ارتفاع)", isDashed = true, colorHex = "#10B981"),
                        Edge3D(0, 2, "r (شعاع)", isVector = true, colorHex = "#38BDF8"),
                        Edge3D(2, 1, "l (مولد)", colorHex = "#F59E0B")
                    )
                    "CYLINDER" -> listOf(
                        Edge3D(0, 1, "h (ارتفاع)", isDashed = true, colorHex = "#10B981"),
                        Edge3D(0, 2, "r (شعاع)", isVector = true, colorHex = "#38BDF8")
                    )
                    else -> listOf(
                        Edge3D(0, 1, "R", isVector = true, colorHex = "#38BDF8"),
                        Edge3D(0, 2, "R", isDashed = true, colorHex = "#10B981")
                    )
                }
                Scene3DSpec(Scene3DType.GEOMETRY_SOLID, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "VECTORS_3D" -> {
                val nodes = listOf(
                    Node3D(0, "مبدأ O", 0f, 0f, 0f, 0.14f, "#E2E8F0"),
                    Node3D(1, "بردار A", 1.5f, 1.1f, 0.6f, 0.17f, "#38BDF8"),
                    Node3D(2, "بردار B", -0.6f, 1.3f, 1.4f, 0.17f, "#F59E0B"),
                    Node3D(3, "برآیند A+B", 0.9f, 2.0f, 1.8f, 0.19f, "#10B981")
                )
                val edges = listOf(
                    Edge3D(0, 1, "A", isVector = true, colorHex = "#38BDF8"),
                    Edge3D(0, 2, "B", isVector = true, colorHex = "#F59E0B"),
                    Edge3D(0, 3, "R = A+B", isVector = true, colorHex = "#10B981"),
                    Edge3D(1, 3, "", isDashed = true, colorHex = "#64748B"),
                    Edge3D(2, 3, "", isDashed = true, colorHex = "#64748B")
                )
                Scene3DSpec(Scene3DType.GEOMETRY_SOLID, "VECTORS_3D", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            // ---------------- PHYSICS MECHANICS & FIELDS ----------------
            "INCLINED_PLANE" -> {
                val nodes = listOf(
                    Node3D(0, "مرکز جرم m", 0f, 0.25f, 0f, 0.28f, "#38BDF8"),
                    Node3D(1, "وزن mg", 0f, -1.25f, 0f, 0.11f, "#F43F5E"),
                    Node3D(2, "نیروی عمودی N", -0.65f, 1.25f, 0f, 0.11f, "#10B981"),
                    Node3D(3, "نیروی محرک / F", 1.25f, -0.35f, 0f, 0.11f, "#F59E0B"),
                    Node3D(4, "اصطکاک f_k", -1.1f, 0.75f, 0f, 0.11f, "#A855F7")
                )
                val edges = listOf(
                    Edge3D(0, 1, "mg", isVector = true, colorHex = "#F43F5E"),
                    Edge3D(0, 2, "N", isVector = true, colorHex = "#10B981"),
                    Edge3D(0, 3, "F / mg·sinθ", isVector = true, colorHex = "#F59E0B"),
                    Edge3D(0, 4, "f_k", isVector = true, colorHex = "#A855F7")
                )
                Scene3DSpec(Scene3DType.PHYSICS_MECHANICS, "INCLINED_PLANE", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "PROJECTILE_3D" -> {
                val nodes = listOf(
                    Node3D(0, "نقطه پرتاب (t=0)", -1.8f, -0.9f, 0f, 0.16f, "#38BDF8"),
                    Node3D(1, "نقطه اوج (H_max)", 0f, 1.35f, 0f, 0.22f, "#F59E0B"),
                    Node3D(2, "نقطه فرود (R)", 1.8f, -0.9f, 0f, 0.16f, "#10B981"),
                    Node3D(3, "v₀", -1.15f, 0.25f, 0f, 0.09f, "#38BDF8"),
                    Node3D(4, "شتاب g", 0f, 0.35f, 0f, 0.09f, "#F43F5E")
                )
                val edges = listOf(
                    Edge3D(0, 3, "v₀", isVector = true, colorHex = "#38BDF8"),
                    Edge3D(1, 4, "g", isVector = true, colorHex = "#F43F5E"),
                    Edge3D(0, 2, "برد افقی R", isDashed = true, colorHex = "#94A3B8")
                )
                Scene3DSpec(Scene3DType.PHYSICS_MECHANICS, "PROJECTILE_3D", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "PENDULUM_3D" -> {
                val nodes = listOf(
                    Node3D(0, "نقطه آویز", 0f, 1.55f, 0f, 0.13f, "#E2E8F0"),
                    Node3D(1, "گوی m", 1.15f, -0.35f, 0f, 0.28f, "#38BDF8"),
                    Node3D(2, "وزن mg", 1.15f, -1.45f, 0f, 0.09f, "#F43F5E"),
                    Node3D(3, "نقطه تعادل", 0f, -0.65f, 0f, 0.10f, "#10B981")
                )
                val edges = listOf(
                    Edge3D(0, 1, "طول L", colorHex = "#CBD5E1"),
                    Edge3D(1, 2, "mg", isVector = true, colorHex = "#F43F5E"),
                    Edge3D(0, 3, "خط تعادل", isDashed = true, colorHex = "#64748B")
                )
                Scene3DSpec(Scene3DType.PHYSICS_MECHANICS, "PENDULUM_3D", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            "MAGNETIC_FIELD" -> {
                val nodes = listOf(
                    Node3D(0, "بار +q", 0f, 0f, 0f, 0.26f, "#F59E0B"),
                    Node3D(1, "سرعت v", 1.5f, 0f, 0f, 0.11f, "#38BDF8"),
                    Node3D(2, "میدان B", 0f, 0f, 1.5f, 0.11f, "#A855F7"),
                    Node3D(3, "نیروی لورنتس F", 0f, 1.5f, 0f, 0.13f, "#10B981")
                )
                val edges = listOf(
                    Edge3D(0, 1, "v", isVector = true, colorHex = "#38BDF8"),
                    Edge3D(0, 2, "B", isVector = true, colorHex = "#A855F7"),
                    Edge3D(0, 3, "F = q(v×B)", isVector = true, colorHex = "#10B981")
                )
                Scene3DSpec(Scene3DType.PHYSICS_MECHANICS, "MAGNETIC_FIELD", title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            // ---------------- MATH 3D SURFACES ----------------
            "PARABOLOID_3D", "WAVE_3D", "SADDLE_3D" -> {
                val nodes = listOf(
                    Node3D(0, "نقطه اکسترمم / ریشه", 0f, -0.25f, 0f, 0.18f, "#F59E0B"),
                    Node3D(1, "نقطه مماس P", 0.9f, 0.65f, 0.5f, 0.15f, "#10B981"),
                    Node3D(2, "بردار شیب", 1.65f, 1.35f, 0.5f, 0.10f, "#38BDF8")
                )
                val edges = listOf(
                    Edge3D(1, 2, "شیب خط مماس", isVector = true, colorHex = "#38BDF8"),
                    Edge3D(0, 1, "", isDashed = true, colorHex = "#94A3B8")
                )
                Scene3DSpec(Scene3DType.MATH_SURFACE_3D, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }

            else -> {
                val nodes = listOf(
                    Node3D(0, "مرکز مسئله", 0f, 0f, 0f, 0.25f, "#38BDF8"),
                    Node3D(1, "متغیر X", 1.4f, 0.4f, 0f, 0.18f, "#F59E0B"),
                    Node3D(2, "پاسخ Y", 0f, 1.3f, 0.8f, 0.18f, "#10B981")
                )
                val edges = listOf(
                    Edge3D(0, 1, "رابطه", isVector = true, colorHex = "#38BDF8"),
                    Edge3D(0, 2, "نتیجه", isVector = true, colorHex = "#10B981")
                )
                Scene3DSpec(sceneType, v, title, summary, formulaLabel = formula, nodes = nodes, edges = edges)
            }
        }
    }

    private fun buildFallbackStepFocuses(
        solution: ProblemSolution,
        nodeCount: Int,
        edgeCount: Int
    ): List<Step3DFocus> {
        if (solution.steps.isEmpty()) return emptyList()
        return solution.steps.mapIndexed { index, step ->
            val targetNode = if (nodeCount > 0) listOf(index % nodeCount) else emptyList()
            val targetEdge = if (edgeCount > 0) listOf(index % edgeCount) else emptyList()
            val yaw = 25f + (index * 38f) % 140f
            val pitch = 18f + ((index % 3) * 10f)
            val explanation = step.visual3DNote.ifBlank {
                step.explanation.ifBlank { step.title }
            }
            Step3DFocus(
                stepNumber = step.stepNumber,
                visualExplanation = explanation,
                focusNodeIndices = targetNode,
                focusEdgeIndices = targetEdge,
                suggestedYawDeg = yaw,
                suggestedPitchDeg = pitch
            )
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
