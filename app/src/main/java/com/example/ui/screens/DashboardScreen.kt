package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnswerEmerald
import com.example.ui.theme.FormulaAmber
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.DailyActivityPoint
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.SolverViewModel
import com.example.ui.viewmodel.TopicMasteryStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SolverViewModel,
    onBackClick: () -> Unit,
    onNavigateToSolver: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "داشبورد پیشرفت تحصیلی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "گزارش عملکرد، مباحث و روند فعالیت هفتگی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Streak & Motivation Banner
            DashboardHeroCard(stats = stats)

            // 2. KPI 2x2 Summary Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiStatCard(
                    title = "مسائل حل‌شده",
                    value = "${stats.totalProblemsSolved}",
                    subtitle = "${stats.favoriteProblemsCount} نشان‌شده",
                    icon = Icons.Default.CheckCircle,
                    accentColor = AnswerEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_solved_count")
                )
                KpiStatCard(
                    title = "مباحث تسلط‌یافته",
                    value = "${stats.topicsMasteredCount}",
                    subtitle = "از ${stats.topicBreakdown.size} مبحث ثبت‌شده",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = PrimaryBlue,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_topics_mastered")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiStatCard(
                    title = "میانگین آزمون‌ها",
                    value = if (stats.totalQuizzesTaken > 0) "${stats.averageQuizScore}٪" else "—",
                    subtitle = "${stats.totalQuizzesTaken} آزمون تکمیل‌شده",
                    icon = Icons.Default.Bolt,
                    accentColor = FormulaAmber,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_quiz_average")
                )
                KpiStatCard(
                    title = "یادداشت‌های درسی",
                    value = "${stats.totalNotesCount}",
                    subtitle = "نکات و فرمول‌های من",
                    icon = Icons.Default.EditNote,
                    accentColor = SecondaryCyan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_notes_count")
                )
            }

            // 3. Weekly Activity Trends Bar Chart
            WeeklyActivityChartCard(weeklyActivity = stats.weeklyActivity)

            // 4. Subject Distribution Donut Chart
            SubjectDistributionCard(stats = stats)

            // 5. Quiz Score Trend Line Chart (when at least 2 quizzes recorded)
            if (stats.recentQuizScores.isNotEmpty()) {
                QuizScoreTrendCard(scores = stats.recentQuizScores)
            }

            // 6. Topic Mastery List
            TopicMasterySection(
                topics = stats.topicBreakdown,
                onStartSolving = onNavigateToSolver,
                onStartQuiz = onNavigateToQuiz
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DashboardHeroCard(stats: DashboardStats) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_hero_card")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.14f),
                            SecondaryCyan.copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = FormulaAmber,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "تداوم مطالعه",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (stats.activeStreakDays > 0)
                            "تداوم مطالعه: ${stats.activeStreakDays} روز پیاپی!"
                        else
                            "آماده شروع یادگیری امروز هستی؟",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            stats.totalProblemsSolved >= 10 -> "عالیه رفیق! با این سرعت خیلی زود کل مباحث رو فتح می‌کنی."
                            stats.totalProblemsSolved > 0 -> "آفرین! هر مسئله‌ای که با انیشتین حل می‌کنی، یک قدم به تسلط کامل نزدیک‌تر میشی."
                            else -> "با دوربین اولین تمرین خودت رو حل کن یا یک آزمون تستی بده تا نمودارهات فعال بشن!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityChartCard(weeklyActivity: List<DailyActivityPoint>) {
    val maxTotal = (weeklyActivity.maxOfOrNull { it.total } ?: 0).coerceAtLeast(4)
    val totalThisWeek = weeklyActivity.sumOf { it.total }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_activity_chart_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "روند فعالیت ۷ روز اخیر",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$totalThisWeek فعالیت",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "مجموع مسائل حل‌شده و آزمون‌های انجام‌شده در هر روز",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

            // Bar Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 8.dp.toPx()
                    val topPadding = 12.dp.toPx()
                    val chartHeight = height - bottomPadding - topPadding

                    // Draw 3 horizontal dashed reference grid lines
                    val gridSteps = 3
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (i in 0..gridSteps) {
                        val y = topPadding + (chartHeight / gridSteps) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    if (weeklyActivity.isNotEmpty()) {
                        val slotWidth = width / weeklyActivity.size
                        val barWidth = (slotWidth * 0.46f).coerceAtMost(28.dp.toPx())

                        weeklyActivity.forEachIndexed { index, point ->
                            val centerX = slotWidth * index + slotWidth / 2f
                            val barLeft = centerX - barWidth / 2f

                            // Background track bar
                            drawRoundRect(
                                color = emptyBarColor.copy(alpha = 0.4f),
                                topLeft = Offset(barLeft, topPadding),
                                size = Size(barWidth, chartHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            if (point.total > 0) {
                                val ratio = point.total.toFloat() / maxTotal.toFloat()
                                val filledHeight = (chartHeight * ratio).coerceAtLeast(8.dp.toPx())
                                val barTop = topPadding + chartHeight - filledHeight

                                // Solved portion + Quiz stacked visual
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(SecondaryCyan, PrimaryBlue)
                                    ),
                                    topLeft = Offset(barLeft, barTop),
                                    size = Size(barWidth, filledHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Day labels + counts below bars
            if (weeklyActivity.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weeklyActivity.forEach { point ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${point.total}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (point.total > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (point.total > 0) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = point.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectDistributionCard(stats: DashboardStats) {
    val total = stats.totalProblemsSolved
    val mathRatio = if (total > 0) stats.mathProblemsCount.toFloat() / total else 0f
    val physicsRatio = if (total > 0) stats.physicsProblemsCount.toFloat() / total else 0f
    val otherRatio = if (total > 0) stats.otherProblemsCount.toFloat() / total else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_distribution_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = SecondaryCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توزیع مسائل بر اساس درس",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart Canvas
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val strokeWidth = 18.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        if (total == 0) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        } else {
                            var startAngle = -90f
                            val slices = listOf(
                                Triple(physicsRatio, SecondaryCyan, stats.physicsProblemsCount),
                                Triple(mathRatio, PrimaryBlue, stats.mathProblemsCount),
                                Triple(otherRatio, FormulaAmber, stats.otherProblemsCount)
                            )
                            slices.forEach { (ratio, color, count) ->
                                if (count > 0) {
                                    val sweep = ratio * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مسئله",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Legend Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubjectLegendRow(
                        label = "فیزیک",
                        count = stats.physicsProblemsCount,
                        percent = (physicsRatio * 100).toInt(),
                        color = SecondaryCyan
                    )
                    SubjectLegendRow(
                        label = "ریاضی و حسابان",
                        count = stats.mathProblemsCount,
                        percent = (mathRatio * 100).toInt(),
                        color = PrimaryBlue
                    )
                    if (stats.otherProblemsCount > 0) {
                        SubjectLegendRow(
                            label = "سایر مباحث",
                            count = stats.otherProblemsCount,
                            percent = (otherRatio * 100).toInt(),
                            color = FormulaAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectLegendRow(
    label: String,
    count: Int,
    percent: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "$count ($percent٪)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun QuizScoreTrendCard(scores: List<Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_trend_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = AnswerEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "روند نمرات آزمون‌های اخیر",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val lineGridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padH = 16.dp.toPx()
                    val padV = 14.dp.toPx()
                    val usableW = (width - padH * 2).coerceAtLeast(10f)
                    val usableH = (height - padV * 2).coerceAtLeast(10f)

                    // Grid lines at 0%, 50%, 100%
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = padV + usableH * frac
                        drawLine(
                            color = lineGridColor,
                            start = Offset(padH, y),
                            end = Offset(width - padH, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val points = scores.mapIndexed { idx, score ->
                        val x = if (scores.size == 1) {
                            width / 2f
                        } else {
                            padH + (usableW * idx.toFloat() / (scores.size - 1).toFloat())
                        }
                        val y = padV + usableH * (1f - (score.coerceIn(0, 100) / 100f))
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = AnswerEmerald,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    points.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = AnswerEmerald,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                scores.forEachIndexed { idx, score ->
                    Text(
                        text = "آزمون ${idx + 1}: $score٪",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnswerEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicMasterySection(
    topics: List<TopicMasteryStat>,
    onStartSolving: () -> Unit,
    onStartQuiz: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("topic_mastery_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سطح تسلط بر مباحث درسی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topics.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "هنوز مبحثی در کارنامه ثبت نشده است",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "با حل مسائل بیشتر در موضوعات مختلف، میزان تسلط شما در هر درس اینجا نمایش داده می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onStartSolving,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حل مسئله")
                        }
                        OutlinedButton(
                            onClick = onStartQuiz,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("شروع آزمون")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topics.forEach { topicStat ->
                        TopicMasteryRow(topicStat)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicMasteryRow(stat: TopicMasteryStat) {
    val animatedProgress by animateFloatAsState(
        targetValue = stat.masteryPercent / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "topic_progress"
    )
    val levelColor = when {
        stat.masteryPercent >= 75 -> AnswerEmerald
        stat.masteryPercent >= 50 -> PrimaryBlue
        else -> FormulaAmber
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stat.subject} • ${stat.solvedCount} مسئله حل‌شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = levelColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${stat.levelLabel} (${stat.masteryPercent}٪)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = levelColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
