package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.AnswerEmerald
import com.example.ui.theme.AnswerEmeraldLight
import com.example.ui.theme.FormulaAmberLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private const val TAG = "CameraScanScreen"

private enum class FocusState {
    IDLE,
    FOCUSING,
    LOCKED,
    FALLBACK
}

@Composable
fun CameraScanScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraInitialized by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var showEmulatorTip by remember { mutableStateOf(true) }

    // Tap-to-focus visual state
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var focusState by remember { mutableStateOf(FocusState.IDLE) }
    var focusTriggerId by remember { mutableIntStateOf(0) }

    val focusScale = remember { Animatable(1.45f) }
    val focusAlpha = remember { Animatable(0f) }

    BackHandler {
        onClose()
    }

    // Rebind CameraX whenever previewViewRef or cameraSelector changes
    LaunchedEffect(previewViewRef, cameraSelector) {
        val pv = previewViewRef ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = pv.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            imageCapture = capture

            try {
                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
                camera = boundCamera
                isTorchOn = false
                hasCameraInitialized = true
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Animate & auto-dismiss the tap-to-focus indicator
    LaunchedEffect(focusTriggerId) {
        if (focusTriggerId > 0 && focusTapOffset != null) {
            focusAlpha.snapTo(1f)
            focusScale.snapTo(1.45f)
            focusScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            delay(1500L)
            focusAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350)
            )
            focusTapOffset = null
            focusState = FocusState.IDLE
        }
    }

    // Photo picker fallback right from camera screen
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = uriToBitmap(context, uri)
            if (bitmap != null) {
                onImageCaptured(bitmap)
            }
        }
    }

    // Animated scan line
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_viewfinder_container")
    ) {
        // Camera PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { pv ->
                    previewViewRef = pv
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Target Box + Tap-to-Focus Reticle Overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera, previewViewRef) {
                    detectTapGestures { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        focusTapOffset = offset
                        focusState = FocusState.FOCUSING
                        focusTriggerId++

                        val pv = previewViewRef
                        val cam = camera
                        if (pv != null && cam != null) {
                            try {
                                val factory = pv.meteringPointFactory
                                val meteringPoint = factory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(
                                    meteringPoint,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()

                                val future = cam.cameraControl.startFocusAndMetering(action)
                                future.addListener({
                                    try {
                                        val result = future.get()
                                        focusState = if (result.isFocusSuccessful) {
                                            FocusState.LOCKED
                                        } else {
                                            FocusState.FALLBACK
                                        }
                                    } catch (e: Exception) {
                                        focusState = FocusState.FALLBACK
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            } catch (e: Exception) {
                                Log.w(TAG, "Tap-to-focus not supported on this camera", e)
                                focusState = FocusState.FALLBACK
                            }
                        } else {
                            focusState = FocusState.FALLBACK
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val frameWidth = canvasWidth * 0.85f
            val frameHeight = canvasHeight * 0.52f
            val frameLeft = (canvasWidth - frameWidth) / 2f
            val frameTop = (canvasHeight - frameHeight) / 2f - 40.dp.toPx()

            // Dim darkened outside area
            drawRect(
                color = Color.Black.copy(alpha = 0.52f),
                size = size
            )

            // Transparent cut-out viewfinder
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(20.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Viewfinder Border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Scanning laser line
            val lineY = frameTop + (frameHeight * scanProgress)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        SecondaryCyan,
                        Color.White,
                        SecondaryCyan,
                        Color.Transparent
                    )
                ),
                start = Offset(frameLeft + 10.dp.toPx(), lineY),
                end = Offset(frameLeft + frameWidth - 10.dp.toPx(), lineY),
                strokeWidth = 3.dp.toPx()
            )

            // Tap-to-Focus Visual Reticle Indicator
            val tapCenter = focusTapOffset
            val alpha = focusAlpha.value
            if (tapCenter != null && alpha > 0.01f) {
                val baseColor = when (focusState) {
                    FocusState.LOCKED -> AnswerEmeraldLight
                    FocusState.FOCUSING -> FormulaAmberLight
                    else -> Color.White
                }.copy(alpha = alpha)

                val reticleRadius = 32.dp.toPx() * focusScale.value
                val innerRadius = 5.dp.toPx()

                // Soft glow halo
                drawCircle(
                    color = baseColor.copy(alpha = alpha * 0.16f),
                    radius = reticleRadius + 6.dp.toPx(),
                    center = tapCenter
                )

                // Outer focus ring
                drawCircle(
                    color = baseColor,
                    radius = reticleRadius,
                    center = tapCenter,
                    style = Stroke(width = 2.4.dp.toPx())
                )

                // Cardinal crosshair ticks around the ring
                val tickLength = 8.dp.toPx()
                // Top tick
                drawLine(
                    color = baseColor,
                    start = Offset(tapCenter.x, tapCenter.y - reticleRadius),
                    end = Offset(tapCenter.x, tapCenter.y - reticleRadius + tickLength),
                    strokeWidth = 2.2.dp.toPx()
                )
                // Bottom tick
                drawLine(
                    color = baseColor,
                    start = Offset(tapCenter.x, tapCenter.y + reticleRadius),
                    end = Offset(tapCenter.x, tapCenter.y + reticleRadius - tickLength),
                    strokeWidth = 2.2.dp.toPx()
                )
                // Left tick
                drawLine(
                    color = baseColor,
                    start = Offset(tapCenter.x - reticleRadius, tapCenter.y),
                    end = Offset(tapCenter.x - reticleRadius + tickLength, tapCenter.y),
                    strokeWidth = 2.2.dp.toPx()
                )
                // Right tick
                drawLine(
                    color = baseColor,
                    start = Offset(tapCenter.x + reticleRadius, tapCenter.y),
                    end = Offset(tapCenter.x + reticleRadius - tickLength, tapCenter.y),
                    strokeWidth = 2.2.dp.toPx()
                )

                // Center focus dot
                drawCircle(
                    color = baseColor,
                    radius = innerRadius,
                    center = tapCenter
                )
            }
        }

        // Top Control Bar (Close, Torch, Camera Switch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(48.dp)
                    .testTag("camera_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "بستن دوربین",
                    tint = Color.White
                )
            }

            // Focus status badge pill when tap-to-focus is active
            if (focusTapOffset != null && focusState != FocusState.IDLE) {
                val badgeColor = when (focusState) {
                    FocusState.LOCKED -> AnswerEmerald
                    FocusState.FOCUSING -> Color(0xFFD97706)
                    else -> Color(0xFF334155)
                }
                val badgeText = when (focusState) {
                    FocusState.LOCKED -> "فوکوس تثبیت شد ✓"
                    FocusState.FOCUSING -> "در حال تنظیم فوکوس..."
                    else -> "نوردهی تنظیم شد"
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor.copy(alpha = 0.9f),
                    modifier = Modifier.testTag("camera_focus_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Torch / Flash button
                IconButton(
                    onClick = {
                        val newTorch = !isTorchOn
                        camera?.cameraControl?.enableTorch(newTorch)
                        isTorchOn = newTorch
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(48.dp)
                        .testTag("camera_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "فلاش",
                        tint = if (isTorchOn) Color(0xFFFDE047) else Color.White
                    )
                }

                // Switch Camera button
                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(48.dp)
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "تغییر دوربین",
                        tint = Color.White
                    )
                }
            }
        }

        // Notice explaining real camera vs emulator
        if (showEmulatorTip) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 74.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصویر شطرنجی فقط در شبیه‌ساز کامپیوتر است؛ با نصب برنامه روی گوشی شما، لنز دوربین واقعی فعال می‌شود.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showEmulatorTip = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پیام",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Bottom Capture Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "برای وضوح بیشتر روی متن مسئله لمس کنید و سپس دکمه عکس را بزنید",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Shortcut Button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(48.dp)
                        .testTag("camera_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "گالری",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Main Big Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else Color.White)
                        .clickable(enabled = !isCapturing && imageCapture != null) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true

                            val executor = ContextCompat.getMainExecutor(context)
                            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    val bitmap = imageProxyToBitmap(imageProxy)
                                    imageProxy.close()
                                    isCapturing = false
                                    if (bitmap != null) {
                                        onImageCaptured(bitmap)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                                    isCapturing = false
                                }
                            })
                        }
                        .testTag("shutter_capture_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Spacer to balance gallery button
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val planeProxy = image.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Rotate bitmap according to sensor orientation
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode ImageProxy to Bitmap", e)
        null
    }
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}
