package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Edge3D
import com.example.data.model.Node3D
import com.example.data.model.Scene3DSpec
import com.example.data.model.Scene3DType
import com.example.data.model.Step3DFocus
import com.example.ui.theme.AnswerEmerald
import com.example.ui.theme.FormulaAmber
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class ProjectedVec(
    val sx: Float,
    val sy: Float,
    val rz: Float,
    val scale: Float
)

private sealed class RenderPrimitive(val depth: Float) {
    class Face3D(
        depth: Float,
        val points: List<Offset>,
        val fillColor: Color,
        val strokeColor: Color
    ) : RenderPrimitive(depth)

    class Segment3D(
        depth: Float,
        val p1: Offset,
        val p2: Offset,
        val color: Color,
        val strokeWidth: Float,
        val isDashed: Boolean,
        val isVector: Boolean,
        val bondOrder: Int,
        val label: String,
        val isHighlighted: Boolean
    ) : RenderPrimitive(depth)

    class Sphere3D(
        depth: Float,
        val center: Offset,
        val radiusPx: Float,
        val color: Color,
        val label: String,
        val isHighlighted: Boolean
    ) : RenderPrimitive(depth)
}

@Composable
fun Interactive3DExplanationCard(
    spec: Scene3DSpec,
    selectedStepNumber: Int?,
    onSelectStepNumber: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullscreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }

    val activeFocus: Step3DFocus? = remember(spec, selectedStepNumber) {
        if (selectedStepNumber == null) null
        else spec.stepFocuses.firstOrNull { it.stepNumber == selectedStepNumber }
    }

    val accentColor = when (spec.sceneType) {
        Scene3DType.ASTRONOMY_ASTROPHYSICS -> Color(0xFF818CF8)
        Scene3DType.CHEMISTRY_MOLECULE -> AnswerEmerald
        Scene3DType.GEOMETRY_SOLID -> FormulaAmber
        Scene3DType.PHYSICS_MECHANICS -> SecondaryCyan
        Scene3DType.MATH_SURFACE_3D -> PrimaryBlue
    }

    val categoryBadge = when (spec.sceneType) {
        Scene3DType.ASTRONOMY_ASTROPHYSICS -> "نجوم و فیزیک نجوم سه‌بعدی"
        Scene3DType.CHEMISTRY_MOLECULE -> "شیمی و ساختار مولکولی سه‌بعدی"
        Scene3DType.GEOMETRY_SOLID -> "هندسه فضایی سه‌بعدی"
        Scene3DType.PHYSICS_MECHANICS -> "آزمایشگاه سه‌بعدی فیزیک"
        Scene3DType.MATH_SURFACE_3D -> "تجسم سه‌بعدی ریاضی"
    }

    if (showFullscreen) {
        FullscreenInteractive3DDialog(
            spec = spec,
            selectedStepNumber = selectedStepNumber,
            onSelectStepNumber = onSelectStepNumber,
            isPlaying = isPlaying,
            onTogglePlay = { isPlaying = !isPlaying },
            onDismiss = { showFullscreen = false }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_3d_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = spec.title.ifBlank { "نمایش تعاملی سه‌بعدی مسئله" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = categoryBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { showFullscreen = true },
                    modifier = Modifier.testTag("open_fullscreen_3d_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "نمایش تمام‌صفحه سه‌بعدی",
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive 3D Viewport
            Interactive3DViewport(
                spec = spec,
                activeFocus = activeFocus,
                isPlaying = isPlaying,
                onTogglePlay = { isPlaying = !isPlaying },
                onExpandFullscreen = { showFullscreen = true },
                viewportHeight = 290.dp
            )

            // Step Focus Selector Pills
            if (spec.stepFocuses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "همگام‌سازی سه‌بعدی با مراحل توضیح:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStepNumber == null,
                        onClick = { onSelectStepNumber(null) },
                        label = { Text("نمای کلی", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor
                        ),
                        modifier = Modifier.testTag("3d_step_chip_overview")
                    )
                    spec.stepFocuses.forEach { sf ->
                        val isSelected = selectedStepNumber == sf.stepNumber
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectStepNumber(if (isSelected) null else sf.stepNumber)
                            },
                            label = {
                                Text(
                                    text = "گام ${sf.stepNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.22f),
                                selectedLabelColor = accentColor
                            ),
                            modifier = Modifier.testTag("3d_step_chip_${sf.stepNumber}")
                        )
                    }
                }
            }

            // Spatial Conceptual Explanation Box (updates dynamically per step!)
            val activeExplanation = activeFocus?.visualExplanation?.ifBlank { spec.spatialSummary }
                ?: spec.spatialSummary
            if (activeExplanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (activeFocus != null) "🔍 توضیح سه‌بعدی گام ${activeFocus.stepNumber}:"
                            else "🧭 درک فضایی و سه‌بعدی مسئله:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (spec.formulaLabel.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = spec.formulaLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenInteractive3DDialog(
    spec: Scene3DSpec,
    selectedStepNumber: Int?,
    onSelectStepNumber: (Int?) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeFocus = remember(spec, selectedStepNumber) {
        if (selectedStepNumber == null) null
        else spec.stepFocuses.firstOrNull { it.stepNumber == selectedStepNumber }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFF070B19)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = spec.title.ifBlank { "آزمایشگاه تعاملی سه‌بعدی" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "با دست بچرخانید و با دو انگشت بزرگ‌نمایی کنید",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_fullscreen_3d_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن نمای تمام‌صفحه",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Large Canvas Area
                Box(modifier = Modifier.weight(1f)) {
                    Interactive3DViewport(
                        spec = spec,
                        activeFocus = activeFocus,
                        isPlaying = isPlaying,
                        onTogglePlay = onTogglePlay,
                        onExpandFullscreen = null,
                        viewportHeight = null
                    )
                }

                if (spec.stepFocuses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedStepNumber == null,
                            onClick = { onSelectStepNumber(null) },
                            label = { Text("نمای کلی", color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                selectedContainerColor = PrimaryBlue
                            )
                        )
                        spec.stepFocuses.forEach { sf ->
                            val isSelected = selectedStepNumber == sf.stepNumber
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectStepNumber(if (isSelected) null else sf.stepNumber) },
                                label = { Text("گام ${sf.stepNumber}", color = Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1E293B),
                                    selectedContainerColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }

                val activeExplanation = activeFocus?.visualExplanation?.ifBlank { spec.spatialSummary }
                    ?: spec.spatialSummary
                if (activeExplanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Interactive3DViewport(
    spec: Scene3DSpec,
    activeFocus: Step3DFocus?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onExpandFullscreen: (() -> Unit)?,
    viewportHeight: Dp? = 280.dp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val yawAnim = remember { Animatable(35f) }
    val pitchAnim = remember { Animatable(22f) }
    val zoomAnim = remember { Animatable(1.0f) }

    var animTimeSec by remember { mutableFloatStateOf(0f) }
    var userHasDragged by remember { mutableStateOf(false) }

    // Continuous motion clock for animated orbits, projectiles, waves, and pendulums
    LaunchedEffect(isPlaying, spec.shapeVariant) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastNanos = now
            animTimeSec += dt
        }
    }

    // Smoothly orient camera when user selects a specific solution step
    LaunchedEffect(activeFocus?.stepNumber) {
        if (activeFocus != null) {
            launch { yawAnim.animateTo(activeFocus.suggestedYawDeg, tween(450)) }
            launch { pitchAnim.animateTo(activeFocus.suggestedPitchDeg.coerceIn(-80f, 80f), tween(450)) }
        }
    }

    val bgBrush = when (spec.sceneType) {
        Scene3DType.ASTRONOMY_ASTROPHYSICS -> Brush.verticalGradient(
            listOf(Color(0xFF040714), Color(0xFF0F172A), Color(0xFF1E1B4B))
        )
        Scene3DType.CHEMISTRY_MOLECULE -> Brush.verticalGradient(
            listOf(Color(0xFF0A1428), Color(0xFF0F203C), Color(0xFF132D46))
        )
        Scene3DType.GEOMETRY_SOLID -> Brush.verticalGradient(
            listOf(Color(0xFF0D1526), Color(0xFF162238), Color(0xFF1E293B))
        )
        Scene3DType.PHYSICS_MECHANICS -> Brush.verticalGradient(
            listOf(Color(0xFF0B132B), Color(0xFF14213D), Color(0xFF1C2E4A))
        )
        Scene3DType.MATH_SURFACE_3D -> Brush.verticalGradient(
            listOf(Color(0xFF0B1120), Color(0xFF111827), Color(0xFF1E293B))
        )
    }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.BLACK)
        }
    }

    val edgeLabelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }

    val sizeModifier = if (viewportHeight != null) {
        modifier
            .fillMaxWidth()
            .height(viewportHeight)
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = sizeModifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .testTag("interactive_3d_canvas_box")
    ) {
        // 3D Touch & Gesture Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(spec.title) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        userHasDragged = true
                        scope.launch {
                            yawAnim.snapTo(yawAnim.value + pan.x * 0.42f)
                            pitchAnim.snapTo((pitchAnim.value - pan.y * 0.42f).coerceIn(-85f, 85f))
                            if (zoom != 1f) {
                                zoomAnim.snapTo((zoomAnim.value * zoom).coerceIn(0.55f, 2.6f))
                            }
                        }
                    }
                }
        ) {
            val yawRad = (yawAnim.value * PI / 180.0).toFloat()
            val pitchRad = (pitchAnim.value * PI / 180.0).toFloat()
            val baseScale = minOf(size.width, size.height) * 0.24f * zoomAnim.value
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Subtle Background Stars (Astronomy) or Coordinate Grid
            if (spec.sceneType == Scene3DType.ASTRONOMY_ASTROPHYSICS) {
                drawStarfield(yawAnim.value, pitchAnim.value)
            }

            // Compute live animated node positions
            val livePositions = computeLiveNodePositions(spec, animTimeSec)

            // Draw domain-specific 3D reference geometry (axes, orbits, surfaces, solids, ramps)
            val primitives = mutableListOf<RenderPrimitive>()
            appendDomainPrimitives(
                spec = spec,
                livePositions = livePositions,
                animTimeSec = animTimeSec,
                yawRad = yawRad,
                pitchRad = pitchRad,
                cx = cx,
                cy = cy,
                baseScale = baseScale,
                out = primitives
            )

            // Draw 3D coordinate axes for Geometry, Physics, and Math
            if (spec.sceneType == Scene3DType.GEOMETRY_SOLID ||
                spec.sceneType == Scene3DType.PHYSICS_MECHANICS ||
                spec.sceneType == Scene3DType.MATH_SURFACE_3D
            ) {
                appendAxesPrimitives(yawRad, pitchRad, cx, cy, baseScale, primitives)
            }

            // Edges / Bonds / Vectors
            val focusNodes = activeFocus?.focusNodeIndices?.toSet() ?: emptySet()
            val focusEdges = activeFocus?.focusEdgeIndices?.toSet() ?: emptySet()

            spec.edges.forEachIndexed { idx, edge ->
                if (edge.fromIndex in livePositions.indices && edge.toIndex in livePositions.indices) {
                    val pFrom = projectVec(livePositions[edge.fromIndex], yawRad, pitchRad, cx, cy, baseScale)
                    val pTo = projectVec(livePositions[edge.toIndex], yawRad, pitchRad, cx, cy, baseScale)
                    val midDepth = (pFrom.rz + pTo.rz) / 2f
                    val isHi = focusEdges.contains(idx)
                    val edgeColor = if (isHi) Color(0xFFFBBF24) else parseHexColor(edge.colorHex, Color(0xFF94A3B8))
                    primitives.add(
                        RenderPrimitive.Segment3D(
                            depth = midDepth,
                            p1 = Offset(pFrom.sx, pFrom.sy),
                            p2 = Offset(pTo.sx, pTo.sy),
                            color = edgeColor,
                            strokeWidth = if (isHi) 6.5f else 4.2f,
                            isDashed = edge.isDashed,
                            isVector = edge.isVector,
                            bondOrder = edge.bondOrder,
                            label = edge.label,
                            isHighlighted = isHi
                        )
                    )
                }
            }

            // Nodes (Atoms, Planets, Stars, Vertices, Particles)
            spec.nodes.forEachIndexed { idx, node ->
                val pos = livePositions.getOrElse(idx) { Vec3(node.x, node.y, node.z) }
                val proj = projectVec(pos, yawRad, pitchRad, cx, cy, baseScale)
                val rPx = (node.radius * baseScale * proj.scale).coerceIn(7f, 75f)
                val isHi = focusNodes.contains(idx)
                val nodeColor = parseHexColor(node.colorHex, PrimaryBlue)
                primitives.add(
                    RenderPrimitive.Sphere3D(
                        depth = proj.rz - 0.02f, // slightly in front of connecting edges at same depth
                        center = Offset(proj.sx, proj.sy),
                        radiusPx = if (isHi) rPx * 1.18f else rPx,
                        color = nodeColor,
                        label = node.label,
                        isHighlighted = isHi
                    )
                )
            }

            // Z-depth Painter's Algorithm Sort (furthest first, nearest last)
            primitives.sortByDescending { it.depth }

            // Execute draw calls in depth order
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
            primitives.forEach { prim ->
                when (prim) {
                    is RenderPrimitive.Face3D -> {
                        if (prim.points.size >= 3) {
                            val path = Path().apply {
                                moveTo(prim.points[0].x, prim.points[0].y)
                                for (i in 1 until prim.points.size) {
                                    lineTo(prim.points[i].x, prim.points[i].y)
                                }
                                close()
                            }
                            drawPath(path = path, color = prim.fillColor)
                            drawPath(
                                path = path,
                                color = prim.strokeColor,
                                style = Stroke(width = 2.0f)
                            )
                        }
                    }

                    is RenderPrimitive.Segment3D -> {
                        drawSegment3D(prim, dashEffect, edgeLabelPaint)
                    }

                    is RenderPrimitive.Sphere3D -> {
                        drawSphere3D(prim, labelPaint)
                    }
                }
            }
        }

        // Top-Start Interactive Hint Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (userHasDragged) "چرخش آزاد با دست فعال است"
                    else "با انگشت بچرخانید • با دو انگشت زوم کنید",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // Bottom Controls Bar (Play/Pause, Zoom +, Zoom -, Reset, Fullscreen)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("3d_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف حرکت سه‌بعدی" else "پخش حرکت سه‌بعدی",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            zoomAnim.animateTo((zoomAnim.value + 0.25f).coerceAtMost(2.6f), tween(200))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("3d_zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "بزرگ‌نمایی",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            zoomAnim.animateTo((zoomAnim.value - 0.25f).coerceAtLeast(0.55f), tween(200))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("3d_zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "کوچک‌نمایی",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        userHasDragged = false
                        scope.launch {
                            launch { yawAnim.animateTo(activeFocus?.suggestedYawDeg ?: 35f, tween(350)) }
                            launch { pitchAnim.animateTo(activeFocus?.suggestedPitchDeg ?: 22f, tween(350)) }
                            launch { zoomAnim.animateTo(1.0f, tween(350)) }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("3d_reset_view_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بازنشانی زاویه دوربین",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onExpandFullscreen != null) {
                    IconButton(
                        onClick = onExpandFullscreen,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("3d_expand_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "تمام‌صفحه",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun rotateVec(v: Vec3, yawRad: Float, pitchRad: Float): Vec3 {
    val cosY = cos(yawRad)
    val sinY = sin(yawRad)
    val x1 = v.x * cosY + v.z * sinY
    val z1 = -v.x * sinY + v.z * cosY

    val cosP = cos(pitchRad)
    val sinP = sin(pitchRad)
    val y2 = v.y * cosP - z1 * sinP
    val z2 = v.y * sinP + z1 * cosP
    return Vec3(x1, y2, z2)
}

private fun projectVec(
    v: Vec3,
    yawRad: Float,
    pitchRad: Float,
    cx: Float,
    cy: Float,
    baseScale: Float
): ProjectedVec {
    val r = rotateVec(v, yawRad, pitchRad)
    val cameraDist = 6.2f
    val perspective = cameraDist / (cameraDist + r.z).coerceAtLeast(1.2f)
    val sx = cx + r.x * baseScale * perspective
    val sy = cy - r.y * baseScale * perspective
    return ProjectedVec(sx, sy, r.z, perspective)
}

private fun computeLiveNodePositions(spec: Scene3DSpec, animTimeSec: Float): List<Vec3> {
    val variant = spec.shapeVariant.uppercase()
    return spec.nodes.mapIndexed { idx, node ->
        when {
            node.orbitRadius > 0.01f && abs(node.orbitSpeed) > 0.01f -> {
                val angle = animTimeSec * node.orbitSpeed + idx * 1.4f
                val ox = node.orbitRadius * cos(angle)
                val oz = node.orbitRadius * sin(angle)
                val tiltRad = (node.orbitTiltDeg * PI / 180.0).toFloat()
                val oy = oz * sin(tiltRad) + node.y
                val ozTilted = oz * cos(tiltRad)
                Vec3(ox, oy, ozTilted)
            }
            variant == "PENDULUM_3D" && idx == 1 -> {
                val theta = 0.62f * sin(animTimeSec * 2.1f)
                val len = 1.9f
                val px = len * sin(theta)
                val py = 1.55f - len * cos(theta)
                Vec3(px, py, 0f)
            }
            else -> Vec3(node.x, node.y, node.z)
        }
    }
}

private fun appendAxesPrimitives(
    yawRad: Float,
    pitchRad: Float,
    cx: Float,
    cy: Float,
    baseScale: Float,
    out: MutableList<RenderPrimitive>
) {
    val axisLen = 2.05f
    val axes = listOf(
        Triple(Vec3(-axisLen, 0f, 0f), Vec3(axisLen, 0f, 0f), "X" to Color(0x66EF4444)),
        Triple(Vec3(0f, -axisLen * 0.85f, 0f), Vec3(0f, axisLen * 0.95f, 0f), "Y" to Color(0x6610B981)),
        Triple(Vec3(0f, 0f, -axisLen), Vec3(0f, 0f, axisLen), "Z" to Color(0x6638BDF8))
    )
    axes.forEach { (start, end, labelColor) ->
        val p1 = projectVec(start, yawRad, pitchRad, cx, cy, baseScale)
        val p2 = projectVec(end, yawRad, pitchRad, cx, cy, baseScale)
        out.add(
            RenderPrimitive.Segment3D(
                depth = (p1.rz + p2.rz) / 2f + 1.5f,
                p1 = Offset(p1.sx, p1.sy),
                p2 = Offset(p2.sx, p2.sy),
                color = labelColor.second,
                strokeWidth = 2.2f,
                isDashed = true,
                isVector = true,
                bondOrder = 1,
                label = labelColor.first,
                isHighlighted = false
            )
        )
    }
}

private fun appendDomainPrimitives(
    spec: Scene3DSpec,
    livePositions: List<Vec3>,
    animTimeSec: Float,
    yawRad: Float,
    pitchRad: Float,
    cx: Float,
    cy: Float,
    baseScale: Float,
    out: MutableList<RenderPrimitive>
) {
    val variant = spec.shapeVariant.uppercase()

    // 1. Orbital rings for any orbiting node (Astronomy & Bohr Atom)
    spec.nodes.forEach { node ->
        if (node.orbitRadius > 0.05f) {
            val tiltRad = (node.orbitTiltDeg * PI / 180.0).toFloat()
            val segments = 32
            for (s in 0 until segments) {
                val a1 = (s * 2.0 * PI / segments).toFloat()
                val a2 = ((s + 1) * 2.0 * PI / segments).toFloat()
                val z1 = node.orbitRadius * sin(a1)
                val z2 = node.orbitRadius * sin(a2)
                val v1 = Vec3(node.orbitRadius * cos(a1), z1 * sin(tiltRad) + node.y, z1 * cos(tiltRad))
                val v2 = Vec3(node.orbitRadius * cos(a2), z2 * sin(tiltRad) + node.y, z2 * cos(tiltRad))
                val p1 = projectVec(v1, yawRad, pitchRad, cx, cy, baseScale)
                val p2 = projectVec(v2, yawRad, pitchRad, cx, cy, baseScale)
                out.add(
                    RenderPrimitive.Segment3D(
                        depth = (p1.rz + p2.rz) / 2f + 0.5f,
                        p1 = Offset(p1.sx, p1.sy),
                        p2 = Offset(p2.sx, p2.sy),
                        color = parseHexColor(node.colorHex, Color(0xFF38BDF8)).copy(alpha = 0.42f),
                        strokeWidth = 2.2f,
                        isDashed = true,
                        isVector = false,
                        bondOrder = 1,
                        label = "",
                        isHighlighted = false
                    )
                )
            }
        }
    }

    // 2. Gravity Well 3D warped spacetime sheet
    if (variant == "GRAVITY_WELL" || variant == "BLACK_HOLE_LENSING") {
        appendSurfaceMesh(
            yawRad, pitchRad, cx, cy, baseScale, out,
            lineColor = Color(0x55818CF8),
            yFunc = { x, z ->
                val r = sqrt(x * x + z * z)
                (-1.2f / sqrt(r * r + 0.32f)) + 0.45f
            }
        )
    }

    // 3. Math 3D Surfaces
    if (spec.sceneType == Scene3DType.MATH_SURFACE_3D) {
        val yFn: (Float, Float) -> Float = when (variant) {
            "WAVE_3D" -> { x, z -> 0.5f * sin(1.7f * x + animTimeSec * 1.6f) * cos(1.7f * z) }
            "SADDLE_3D" -> { x, z -> 0.25f * (x * x - z * z) }
            else -> { x, z -> 0.28f * (x * x + z * z) - 0.65f }
        }
        appendSurfaceMesh(yawRad, pitchRad, cx, cy, baseScale, out, Color(0x6638BDF8), yFn)
    }

    // 4. Geometry Shaded Faces / Curved Rings
    if (spec.sceneType == Scene3DType.GEOMETRY_SOLID) {
        when (variant) {
            "CUBE", "PRISM" -> if (livePositions.size >= 8) {
                val faces = listOf(
                    listOf(0, 1, 2, 3), listOf(4, 5, 6, 7),
                    listOf(0, 1, 5, 4), listOf(2, 3, 7, 6),
                    listOf(1, 2, 6, 5), listOf(0, 3, 7, 4)
                )
                addPolyFaces(faces, livePositions, yawRad, pitchRad, cx, cy, baseScale, Color(0x2838BDF8), Color(0x5593C5FD), out)
            }
            "PYRAMID" -> if (livePositions.size >= 5) {
                val faces = listOf(
                    listOf(0, 1, 2, 3),
                    listOf(0, 1, 4), listOf(1, 2, 4), listOf(2, 3, 4), listOf(3, 0, 4)
                )
                addPolyFaces(faces, livePositions, yawRad, pitchRad, cx, cy, baseScale, Color(0x2AF59E0B), Color(0x66FBBF24), out)
            }
            "TETRAHEDRON" -> if (livePositions.size >= 4) {
                val faces = listOf(
                    listOf(0, 1, 2), listOf(0, 2, 3), listOf(0, 3, 1), listOf(1, 2, 3)
                )
                addPolyFaces(faces, livePositions, yawRad, pitchRad, cx, cy, baseScale, Color(0x2A10B981), Color(0x6634D399), out)
            }
            "CONE" -> {
                appendCircleRing(Vec3(0f, -0.95f, 0f), 1.15f, yawRad, pitchRad, cx, cy, baseScale, Color(0xAA38BDF8), out)
                val apex = Vec3(0f, 1.35f, 0f)
                val pApex = projectVec(apex, yawRad, pitchRad, cx, cy, baseScale)
                for (i in 0 until 8) {
                    val a = (i * PI / 4.0).toFloat()
                    val basePt = Vec3(1.15f * cos(a), -0.95f, 1.15f * sin(a))
                    val pBase = projectVec(basePt, yawRad, pitchRad, cx, cy, baseScale)
                    out.add(
                        RenderPrimitive.Segment3D(
                            depth = (pApex.rz + pBase.rz) / 2f,
                            p1 = Offset(pBase.sx, pBase.sy),
                            p2 = Offset(pApex.sx, pApex.sy),
                            color = Color(0x5593C5FD),
                            strokeWidth = 2.2f,
                            isDashed = false,
                            isVector = false,
                            bondOrder = 1,
                            label = "",
                            isHighlighted = false
                        )
                    )
                }
            }
            "CYLINDER" -> {
                appendCircleRing(Vec3(0f, -1.0f, 0f), 1.1f, yawRad, pitchRad, cx, cy, baseScale, Color(0xAA38BDF8), out)
                appendCircleRing(Vec3(0f, 1.0f, 0f), 1.1f, yawRad, pitchRad, cx, cy, baseScale, Color(0xAA38BDF8), out)
                for (i in 0 until 8) {
                    val a = (i * PI / 4.0).toFloat()
                    val p1 = projectVec(Vec3(1.1f * cos(a), -1.0f, 1.1f * sin(a)), yawRad, pitchRad, cx, cy, baseScale)
                    val p2 = projectVec(Vec3(1.1f * cos(a), 1.0f, 1.1f * sin(a)), yawRad, pitchRad, cx, cy, baseScale)
                    out.add(
                        RenderPrimitive.Segment3D(
                            depth = (p1.rz + p2.rz) / 2f,
                            p1 = Offset(p1.sx, p1.sy),
                            p2 = Offset(p2.sx, p2.sy),
                            color = Color(0x5593C5FD),
                            strokeWidth = 2.2f,
                            isDashed = false,
                            isVector = false,
                            bondOrder = 1,
                            label = "",
                            isHighlighted = false
                        )
                    )
                }
            }
            "SPHERE" -> {
                appendCircleRing(Vec3(0f, 0f, 0f), 1.3f, yawRad, pitchRad, cx, cy, baseScale, Color(0xAA38BDF8), out)
                appendCircleRing(Vec3(0f, 0.7f, 0f), 1.09f, yawRad, pitchRad, cx, cy, baseScale, Color(0x5538BDF8), out)
                appendCircleRing(Vec3(0f, -0.7f, 0f), 1.09f, yawRad, pitchRad, cx, cy, baseScale, Color(0x5538BDF8), out)
            }
        }
    }

    // 5. Physics Specific Overlays
    if (spec.sceneType == Scene3DType.PHYSICS_MECHANICS) {
        when (variant) {
            "INCLINED_PLANE" -> {
                val rampPts = listOf(
                    Vec3(-1.8f, -1.0f, -0.8f), Vec3(1.8f, -1.0f, -0.8f), Vec3(1.8f, 0.8f, -0.8f),
                    Vec3(-1.8f, -1.0f, 0.8f), Vec3(1.8f, -1.0f, 0.8f), Vec3(1.8f, 0.8f, 0.8f)
                )
                val faces = listOf(
                    listOf(0, 1, 2), listOf(3, 4, 5), listOf(0, 2, 5, 3), listOf(0, 1, 4, 3)
                )
                addPolyFaces(faces, rampPts, yawRad, pitchRad, cx, cy, baseScale, Color(0x3364748B), Color(0x8894A3B8), out)
            }
            "PROJECTILE_3D" -> {
                val steps = 20
                for (i in 0 until steps) {
                    val t1 = (i.toFloat() / steps) * 2f - 1f
                    val t2 = ((i + 1).toFloat() / steps) * 2f - 1f
                    val p1 = projectVec(Vec3(t1 * 1.8f, 1.35f - 2.25f * t1 * t1, 0f), yawRad, pitchRad, cx, cy, baseScale)
                    val p2 = projectVec(Vec3(t2 * 1.8f, 1.35f - 2.25f * t2 * t2, 0f), yawRad, pitchRad, cx, cy, baseScale)
                    out.add(
                        RenderPrimitive.Segment3D(
                            depth = (p1.rz + p2.rz) / 2f,
                            p1 = Offset(p1.sx, p1.sy),
                            p2 = Offset(p2.sx, p2.sy),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 3.5f,
                            isDashed = false,
                            isVector = false,
                            bondOrder = 1,
                            label = "",
                            isHighlighted = false
                        )
                    )
                }
                // Animated moving ball along the parabola
                val phase = ((animTimeSec * 0.45f) % 1f) * 2f - 1f
                val ballPos = Vec3(phase * 1.8f, 1.35f - 2.25f * phase * phase, 0f)
                val pBall = projectVec(ballPos, yawRad, pitchRad, cx, cy, baseScale)
                out.add(
                    RenderPrimitive.Sphere3D(
                        depth = pBall.rz - 0.1f,
                        center = Offset(pBall.sx, pBall.sy),
                        radiusPx = 11f * pBall.scale,
                        color = Color(0xFFFBBF24),
                        label = "جرم",
                        isHighlighted = true
                    )
                )
            }
        }
    }
}

private fun addPolyFaces(
    faces: List<List<Int>>,
    pts: List<Vec3>,
    yawRad: Float,
    pitchRad: Float,
    cx: Float,
    cy: Float,
    baseScale: Float,
    fillColor: Color,
    strokeColor: Color,
    out: MutableList<RenderPrimitive>
) {
    faces.forEach { idxs ->
        val projected = idxs.map { projectVec(pts[it], yawRad, pitchRad, cx, cy, baseScale) }
        val avgDepth = projected.map { it.rz }.average().toFloat() + 0.15f
        out.add(
            RenderPrimitive.Face3D(
                depth = avgDepth,
                points = projected.map { Offset(it.sx, it.sy) },
                fillColor = fillColor,
                strokeColor = strokeColor
            )
        )
    }
}

private fun appendCircleRing(
    center: Vec3,
    radius: Float,
    yawRad: Float,
    pitchRad: Float,
    cx: Float,
    cy: Float,
    baseScale: Float,
    color: Color,
    out: MutableList<RenderPrimitive>
) {
    val segs = 28
    for (i in 0 until segs) {
        val a1 = (i * 2.0 * PI / segs).toFloat()
        val a2 = ((i + 1) * 2.0 * PI / segs).toFloat()
        val p1 = projectVec(Vec3(center.x + radius * cos(a1), center.y, center.z + radius * sin(a1)), yawRad, pitchRad, cx, cy, baseScale)
        val p2 = projectVec(Vec3(center.x + radius * cos(a2), center.y, center.z + radius * sin(a2)), yawRad, pitchRad, cx, cy, baseScale)
        out.add(
            RenderPrimitive.Segment3D(
                depth = (p1.rz + p2.rz) / 2f + 0.2f,
                p1 = Offset(p1.sx, p1.sy),
                p2 = Offset(p2.sx, p2.sy),
                color = color,
                strokeWidth = 2.4f,
                isDashed = false,
                isVector = false,
                bondOrder = 1,
                label = "",
                isHighlighted = false
            )
        )
    }
}

private fun appendSurfaceMesh(
    yawRad: Float,
    pitchRad: Float,
    cx: Float,
    cy: Float,
    baseScale: Float,
    out: MutableList<RenderPrimitive>,
    lineColor: Color,
    yFunc: (Float, Float) -> Float
) {
    val gridN = 8
    val bound = 1.65f
    val step = (bound * 2f) / gridN
    val pts = Array(gridN + 1) { ix ->
        Array(gridN + 1) { iz ->
            val x = -bound + ix * step
            val z = -bound + iz * step
            projectVec(Vec3(x, yFunc(x, z), z), yawRad, pitchRad, cx, cy, baseScale)
        }
    }
    for (ix in 0..gridN) {
        for (iz in 0..gridN) {
            val p = pts[ix][iz]
            if (ix < gridN) {
                val px = pts[ix + 1][iz]
                out.add(
                    RenderPrimitive.Segment3D(
                        depth = (p.rz + px.rz) / 2f + 0.6f,
                        p1 = Offset(p.sx, p.sy),
                        p2 = Offset(px.sx, px.sy),
                        color = lineColor,
                        strokeWidth = 1.6f,
                        isDashed = false,
                        isVector = false,
                        bondOrder = 1,
                        label = "",
                        isHighlighted = false
                    )
                )
            }
            if (iz < gridN) {
                val pz = pts[ix][iz + 1]
                out.add(
                    RenderPrimitive.Segment3D(
                        depth = (p.rz + pz.rz) / 2f + 0.6f,
                        p1 = Offset(p.sx, p.sy),
                        p2 = Offset(pz.sx, pz.sy),
                        color = lineColor,
                        strokeWidth = 1.6f,
                        isDashed = false,
                        isVector = false,
                        bondOrder = 1,
                        label = "",
                        isHighlighted = false
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawStarfield(yawDeg: Float, pitchDeg: Float) {
    val starOffsets = listOf(
        0.12f to 0.18f, 0.28f to 0.72f, 0.44f to 0.14f, 0.65f to 0.31f, 0.83f to 0.22f,
        0.19f to 0.49f, 0.77f to 0.68f, 0.91f to 0.55f, 0.52f to 0.85f, 0.36f to 0.41f,
        0.08f to 0.82f, 0.61f to 0.11f, 0.72f to 0.89f, 0.88f to 0.39f
    )
    val shiftX = ((yawDeg % 360f) / 360f) * 40f
    val shiftY = (pitchDeg / 90f) * 20f
    starOffsets.forEachIndexed { i, (rx, ry) ->
        val x = (rx * size.width + shiftX) % size.width
        val y = (ry * size.height + shiftY).coerceIn(4f, size.height - 4f)
        drawCircle(
            color = Color.White.copy(alpha = if (i % 3 == 0) 0.65f else 0.3f),
            radius = if (i % 4 == 0) 2.2f else 1.3f,
            center = Offset(if (x < 0f) x + size.width else x, y)
        )
    }
}

private fun DrawScope.drawSegment3D(
    seg: RenderPrimitive.Segment3D,
    dashEffect: PathEffect,
    edgeLabelPaint: Paint
) {
    val dx = seg.p2.x - seg.p1.x
    val dy = seg.p2.y - seg.p1.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len

    val offsets = when (seg.bondOrder) {
        2 -> listOf(-5.5f, 5.5f)
        3 -> listOf(-8f, 0f, 8f)
        else -> listOf(0f)
    }

    offsets.forEach { off ->
        val start = Offset(seg.p1.x + nx * off, seg.p1.y + ny * off)
        val end = Offset(seg.p2.x + nx * off, seg.p2.y + ny * off)
        if (seg.isHighlighted) {
            drawLine(
                color = seg.color.copy(alpha = 0.35f),
                start = start,
                end = end,
                strokeWidth = seg.strokeWidth + 6f,
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = seg.color,
            start = start,
            end = end,
            strokeWidth = seg.strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = if (seg.isDashed) dashEffect else null
        )
    }

    if (seg.isVector && len > 16f) {
        val angle = atan2(dy, dx)
        val headLen = 15f
        val spread = 0.45f
        val left = Offset(
            seg.p2.x - headLen * cos(angle - spread),
            seg.p2.y - headLen * sin(angle - spread)
        )
        val right = Offset(
            seg.p2.x - headLen * cos(angle + spread),
            seg.p2.y - headLen * sin(angle + spread)
        )
        drawLine(seg.color, seg.p2, left, strokeWidth = seg.strokeWidth, cap = StrokeCap.Round)
        drawLine(seg.color, seg.p2, right, strokeWidth = seg.strokeWidth, cap = StrokeCap.Round)
    }

    if (seg.label.isNotBlank()) {
        val mx = (seg.p1.x + seg.p2.x) / 2f + nx * 16f
        val my = (seg.p1.y + seg.p2.y) / 2f + ny * 16f
        edgeLabelPaint.color = android.graphics.Color.argb(
            235,
            (seg.color.red * 255).toInt().coerceIn(130, 255),
            (seg.color.green * 255).toInt().coerceIn(130, 255),
            (seg.color.blue * 255).toInt().coerceIn(130, 255)
        )
        drawContext.canvas.nativeCanvas.drawText(seg.label, mx, my, edgeLabelPaint)
    }
}

private fun DrawScope.drawSphere3D(
    sphere: RenderPrimitive.Sphere3D,
    labelPaint: Paint
) {
    if (sphere.isHighlighted) {
        drawCircle(
            color = Color(0xFFFBBF24).copy(alpha = 0.28f),
            radius = sphere.radiusPx * 1.48f,
            center = sphere.center
        )
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = sphere.radiusPx * 1.32f,
            center = sphere.center,
            style = Stroke(width = 3f)
        )
    }

    // 3D lit sphere shading with top-left specular highlight
    val lightCenter = Offset(
        sphere.center.x - sphere.radiusPx * 0.34f,
        sphere.center.y - sphere.radiusPx * 0.34f
    )
    val darkEdge = Color(
        red = (sphere.color.red * 0.38f).coerceIn(0f, 1f),
        green = (sphere.color.green * 0.38f).coerceIn(0f, 1f),
        blue = (sphere.color.blue * 0.45f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val highlight = Color(
        red = (sphere.color.red + 0.55f).coerceIn(0f, 1f),
        green = (sphere.color.green + 0.55f).coerceIn(0f, 1f),
        blue = (sphere.color.blue + 0.55f).coerceIn(0f, 1f),
        alpha = 1f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(highlight, sphere.color, darkEdge),
            center = lightCenter,
            radius = (sphere.radiusPx * 1.35f).coerceAtLeast(4f)
        ),
        radius = sphere.radiusPx,
        center = sphere.center
    )

    if (sphere.label.isNotBlank()) {
        labelPaint.color = if (sphere.isHighlighted) android.graphics.Color.rgb(253, 224, 71)
        else android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(
            sphere.label,
            sphere.center.x,
            sphere.center.y - sphere.radiusPx - 8f,
            labelPaint
        )
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
