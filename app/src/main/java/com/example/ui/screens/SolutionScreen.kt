package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeUp
import com.example.data.model.ConceptCheckQuestion
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProblemSolution
import com.example.data.model.SolutionStep
import com.example.data.model.StudyNote
import com.example.ui.components.AudioNarrationBar
import com.example.ui.components.Interactive3DExplanationCard
import com.example.ui.theme.AnswerEmerald
import com.example.ui.theme.AnswerEmeraldContainer
import com.example.ui.theme.FormulaAmber
import com.example.ui.theme.FormulaAmberContainer
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.SolverViewModel
import com.example.util.PdfExporter
import com.example.util.Scene3DSynthesizer
import com.example.util.SolutionSpeechManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionScreen(
    solution: ProblemSolution,
    viewModel: SolverViewModel,
    onBackClick: () -> Unit,
    onSolveAnother: () -> Unit,
    onToggleFavorite: (ProblemSolution) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    var isExportingPdf by remember { mutableStateOf(false) }
    var exportedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }

    var selected3DStepNumber by remember(solution.id) { mutableStateOf<Int?>(null) }
    val scene3dSpec = remember(solution) { Scene3DSynthesizer.resolveScene(solution) }

    val defaultFileName = remember(solution) {
        val cleanTopic = solution.topic.replace(Regex("[^a-zA-Z0-9آ-ی]+"), "_").trim('_').take(25)
        if (cleanTopic.isBlank()) "Einstein_Solution.pdf" else "Einstein_Solution_${cleanTopic}.pdf"
    }

    val allNotes by viewModel.allNotes.collectAsState()
    val relatedNotes = remember(allNotes, solution.id) {
        allNotes.filter { it.relatedProblemId == solution.id }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isExportingPdf = true
                val success = PdfExporter.exportSolutionToUri(
                    context = context,
                    solution = solution,
                    notes = relatedNotes,
                    uri = uri
                )
                isExportingPdf = false
                if (success) {
                    exportedPdfUri = uri
                    showPdfSuccessDialog = true
                } else {
                    Toast.makeText(context, "خطا در ذخیره فایل PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val speechManager = remember { SolutionSpeechManager(context) }
    val speechState by speechManager.playbackState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            speechManager.release()
        }
    }

    // Handle Android hardware back press cleanly
    BackHandler {
        speechManager.stop()
        onBackClick()
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت یادداشت برای این مسئله", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("عنوان یادداشت") },
                        placeholder = { Text("مثلاً: نکته امتحانی یا فرمول سرعت") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("متن یادداشت شما") },
                        placeholder = { Text("توضیحات، نکات مهم یا اشکالاتی که باید دوباره مرور کنید...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteContent.isNotBlank() || noteTitle.isNotBlank()) {
                            viewModel.saveNote(
                                title = noteTitle.ifBlank { "یادداشت مسئله ${solution.topic}" },
                                content = noteContent,
                                relatedProblemId = solution.id,
                                relatedProblemTitle = "${solution.subject}: ${solution.topic}",
                                subjectTag = if (solution.subject.contains("فیزیک")) "فیزیک" else "ریاضی"
                            )
                            noteTitle = ""
                            noteContent = ""
                            showAddNoteDialog = false
                            Toast.makeText(context, "یادداشت ذخیره شد", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("ذخیره یادداشت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showPdfSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AnswerEmerald,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "فایل PDF با موفقیت ذخیره شد",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "برگه کامل تحلیل و حل گام‌به‌گام این مسئله، فرمول‌ها و نکات طلایی در حافظه دستگاه شما ذخیره شد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        exportedPdfUri?.let { PdfExporter.openPdfUri(context, it) }
                        showPdfSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاهده فایل PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfSuccessDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = solution.subject.ifBlank { "پاسخ مسئله" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = solution.topic.ifBlank { "تحلیل و حل گام‌به‌گام" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            speechManager.stop()
                            onBackClick()
                        },
                        modifier = Modifier.testTag("solution_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (speechState.isSpeaking) {
                                speechManager.pause()
                            } else {
                                speechManager.playFullSolution(solution)
                            }
                        },
                        modifier = Modifier.testTag("solution_audio_header_button")
                    ) {
                        Icon(
                            imageVector = if (speechState.isSpeaking) Icons.Default.Pause else Icons.Default.VolumeUp,
                            contentDescription = "روایت صوتی راه حل",
                            tint = if (speechState.isSpeaking) AnswerEmerald else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            createDocumentLauncher.launch(defaultFileName)
                        },
                        enabled = !isExportingPdf,
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "دریافت فایل PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            shareSolutionViaSocial(context, solution)
                        },
                        modifier = Modifier.testTag("share_solution_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "اشتراک‌گذاری در پیام‌رسان‌ها",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            copySolutionToClipboard(context, solution)
                        },
                        modifier = Modifier.testTag("copy_solution_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی راه حل"
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(solution) },
                        modifier = Modifier.testTag("favorite_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (solution.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "نشان کردن",
                            tint = if (solution.isFavorite) FormulaAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            speechManager.stop()
                            onSolveAnother()
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(50.dp)
                            .testTag("solve_another_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مسئله جدید",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedButton(
                        onClick = { createDocumentLauncher.launch(defaultFileName) },
                        enabled = !isExportingPdf,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(50.dp)
                            .testTag("export_pdf_bottom_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دریافت PDF", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { shareSolutionViaSocial(context, solution) },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("share_bottom_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "اشتراک‌گذاری", modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============================================================
            // 0. AUDIO NARRATION BAR (روایت صوتی پله‌پله راه حل)
            // ============================================================
            AudioNarrationBar(
                state = speechState,
                onPlayAll = { speechManager.playFullSolution(solution) },
                onTogglePlayPause = { speechManager.togglePlayPause() },
                onStop = { speechManager.stop() },
                onNext = { speechManager.next() },
                onPrevious = { speechManager.previous() },
                onSetRate = { speechManager.setSpeechRate(it) },
                onOpenSettings = { speechManager.openTtsSettings() }
            )

            // ============================================================
            // 0.1 PDF WORKSHEET DOWNLOAD BANNER (برگه چاپی و پی‌دی‌اف)
            // ============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_download_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "برگه چاپی و PDF این مسئله",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isExportingPdf) "در حال پردازش و ساخت فایل PDF..."
                            else "ذخیره برگه استاندارد A4 با فرمول‌ها، محاسبات و نکات در حافظه گوشی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { createDocumentLauncher.launch(defaultFileName) },
                        enabled = !isExportingPdf,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pdf_banner_button")
                    ) {
                        Text("ذخیره PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ============================================================
            // 1. FINAL ANSWER CARD (SHOWN FIRST AS REQUESTED BY USER)
            // ============================================================
            val isFinalAnswerSpeaking = speechState.currentSegmentId == "final_answer" && speechState.isSpeaking
            FinalAnswerHeroCard(
                finalAnswer = solution.finalAnswer,
                isSpeaking = isFinalAnswerSpeaking,
                onPlayAnswer = {
                    if (isFinalAnswerSpeaking) {
                        speechManager.stop()
                    } else {
                        speechManager.playFinalAnswer(solution.finalAnswer)
                    }
                }
            )

            // ============================================================
            // 1.5 INTERACTIVE 3D HANDS-ON EXPLANATION LAB (مدل سه‌بعدی تعاملی)
            // ============================================================
            Interactive3DExplanationCard(
                spec = scene3dSpec,
                selectedStepNumber = selected3DStepNumber,
                onSelectStepNumber = { selected3DStepNumber = it }
            )

            // ============================================================
            // 2. IDENTIFIED PROBLEM & GIVEN DATA
            // ============================================================
            IdentifiedProblemCard(
                identifiedProblem = solution.identifiedProblem,
                subject = solution.subject,
                topic = solution.topic,
                givenData = solution.givenData
            )

            // ============================================================
            // 3. FORMULAS USED CARD
            // ============================================================
            if (solution.formulasUsed.isNotEmpty()) {
                FormulasUsedCard(formulas = solution.formulasUsed)
            }

            // ============================================================
            // 4. STEP-BY-STEP SOLUTION CARDS (پله پله راه حل فرمول)
            // ============================================================
            Text(
                text = "📝 مراحل گام به گام و پله‌پله حل مسئله:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (solution.steps.isNotEmpty()) {
                solution.steps.forEach { step ->
                    val isStepSpeaking = speechState.currentStepNumber == step.stepNumber && speechState.isSpeaking
                    val isStep3DFocused = selected3DStepNumber == step.stepNumber
                    StepCard(
                        step = step,
                        isSpeaking = isStepSpeaking,
                        is3DFocused = isStep3DFocused,
                        onPlayStep = {
                            if (isStepSpeaking) {
                                speechManager.stop()
                            } else {
                                speechManager.playSingleStep(step)
                            }
                        },
                        onShowStepIn3D = {
                            selected3DStepNumber = step.stepNumber
                            coroutineScope.launch {
                                scrollState.animateScrollTo(420)
                            }
                        }
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "راه حل تفصیلی در بخش پاسخ نهایی درج شده است.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ============================================================
            // 5. STUDENT GOLD TIP (نکته طلایی و آموزشی برای دانش‌آموز)
            // ============================================================
            if (solution.studentTip.isNotBlank()) {
                val isTipSpeaking = speechState.currentSegmentId == "student_tip" && speechState.isSpeaking
                StudentTipCard(
                    tip = solution.studentTip,
                    isSpeaking = isTipSpeaking,
                    onPlayTip = {
                        if (isTipSpeaking) {
                            speechManager.stop()
                        } else {
                            speechManager.playStudentTip(solution.studentTip)
                        }
                    }
                )
            }

            // ============================================================
            // 5.1 CONCEPT CHECK QUESTION (سنجش میزان یادگیری دانش‌آموز پس از راه حل)
            // ============================================================
            ConceptCheckCard(
                conceptQuestion = solution.conceptQuestion,
                subject = solution.subject,
                topic = solution.topic
            )

            // ============================================================
            // 6. NOTES SECTION FOR THIS SPECIFIC PROBLEM
            // ============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("problem_notes_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "یادداشت‌های من برای این مسئله",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.testTag("add_note_to_problem_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAdd,
                                contentDescription = "افزودن یادداشت",
                                tint = PrimaryBlue
                            )
                        }
                    }

                    if (relatedNotes.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "هنوز یادداشتی برای این سوال ثبت نکرده‌اید. با دکمه بالا می‌توانید نکات مهم، فرمول‌های طلایی یا خطاهای خود را یادداشت کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        relatedNotes.forEach { note ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (note.content.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = note.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف یادداشت",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * The prominent Hero Answer Card displaying the final correct answer first
 */
@Composable
private fun FinalAnswerHeroCard(
    finalAnswer: String,
    isSpeaking: Boolean = false,
    onPlayAnswer: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("final_answer_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AnswerEmeraldContainer
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, AnswerEmerald)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
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
                        color = AnswerEmerald,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "پاسخ نهایی و قطعی مسئله",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AnswerEmerald
                        )
                        Text(
                            text = "نتیجه محاسبات صحیح",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF065F46)
                        )
                    }
                }

                IconButton(
                    onClick = onPlayAnswer,
                    modifier = Modifier.testTag("final_answer_audio_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = if (isSpeaking) "توقف خواندن پاسخ" else "شنیدن صوتی پاسخ نهایی",
                        tint = AnswerEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = finalAnswer,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 32.sp
                    ),
                    color = Color(0xFF064E3B),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun IdentifiedProblemCard(
    identifiedProblem: String,
    subject: String,
    topic: String,
    givenData: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("identified_problem_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "صورت مسئله شناسایی‌شده",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$subject • $topic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = identifiedProblem,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (givenData.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "داده‌ها و مفروضات اولیه:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                givenData.forEach { dataItem ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dataItem,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulasUsedCard(formulas: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("formulas_used_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FormulaAmberContainer.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            FormulaAmber.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = FormulaAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فرمول‌ها و روابط فیزیکی / ریاضی به کار رفته",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FormulaAmber
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            formulas.forEach { formula ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = formula,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    step: SolutionStep,
    isSpeaking: Boolean = false,
    is3DFocused: Boolean = false,
    onPlayStep: () -> Unit = {},
    onShowStepIn3D: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solution_step_${step.stepNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSpeaking -> PrimaryBlue.copy(alpha = 0.06f)
                is3DFocused -> SecondaryCyan.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSpeaking || is3DFocused) 2.dp else 1.dp,
            when {
                isSpeaking -> PrimaryBlue
                is3DFocused -> SecondaryCyan
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        color = if (isSpeaking || is3DFocused) SecondaryCyan else PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${step.stepNumber}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step.title.ifBlank { "مرحله شماره ${step.stepNumber}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpeaking) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "در حال خواندن...",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onShowStepIn3D,
                        modifier = Modifier.testTag("step_${step.stepNumber}_3d_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "نمایش سه‌بعدی این مرحله",
                            tint = if (is3DFocused) SecondaryCyan else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onPlayStep,
                        modifier = Modifier.testTag("step_${step.stepNumber}_audio_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "توقف خواندن این مرحله" else "شنیدن صوتی این مرحله",
                            tint = if (isSpeaking) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (step.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (step.calculation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = step.calculation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (step.visual3DNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SecondaryCyan.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step.visual3DNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentTipCard(
    tip: String,
    isSpeaking: Boolean = false,
    onPlayTip: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_tip_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeaking) SecondaryCyan.copy(alpha = 0.18f) else SecondaryCyan.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSpeaking) 2.dp else 1.dp,
            if (isSpeaking) SecondaryCyan else SecondaryCyan.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SecondaryCyan,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "نکته طلایی و مفهومی برای امتحان:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCyan
                    )
                }

                IconButton(
                    onClick = onPlayTip,
                    modifier = Modifier.testTag("tip_audio_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = if (isSpeaking) "توقف خواندن نکته" else "شنیدن صوتی نکته طلایی",
                        tint = SecondaryCyan
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun buildSolutionShareText(solution: ProblemSolution): String {
    val textBuilder = StringBuilder()
    textBuilder.append("✨ پاسخ مسئله: ").append(solution.subject).append(" - ").append(solution.topic).append("\n\n")
    textBuilder.append("❓ صورت مسئله:\n").append(solution.identifiedProblem).append("\n\n")
    textBuilder.append("🎯 پاسخ نهایی و قطعی:\n").append(solution.finalAnswer).append("\n\n")

    if (solution.formulasUsed.isNotEmpty()) {
        textBuilder.append("📐 فرمول‌های اصلی:\n")
        solution.formulasUsed.forEach { textBuilder.append("- ").append(it).append("\n") }
        textBuilder.append("\n")
    }

    if (solution.steps.isNotEmpty()) {
        textBuilder.append("📝 مراحل گام به گام حل:\n")
        solution.steps.forEach { step ->
            textBuilder.append("گام ").append(step.stepNumber).append(": ").append(step.title).append("\n")
            if (step.explanation.isNotBlank()) textBuilder.append(step.explanation).append("\n")
            if (step.calculation.isNotBlank()) textBuilder.append("محاسبات: ").append(step.calculation).append("\n")
            textBuilder.append("\n")
        }
    }

    if (solution.studentTip.isNotBlank()) {
        textBuilder.append("💡 نکته آموزشی:\n").append(solution.studentTip).append("\n\n")
    }

    textBuilder.append("📲 حل شده با اپلیکیشن اندروید «حل‌المسائل هوشمند ریاضی و فیزیک»")
    return textBuilder.toString()
}

/**
 * Share via Android standard Intent (Telegram, WhatsApp, SMS, Eitaa, Shad, etc.)
 */
fun shareSolutionViaSocial(context: Context, solution: ProblemSolution) {
    val shareText = buildSolutionShareText(solution)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "حل مسئله ${solution.subject}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری پاسخ مسئله با:")
    context.startActivity(shareIntent)
}

private fun copySolutionToClipboard(context: Context, solution: ProblemSolution) {
    val shareText = buildSolutionShareText(solution)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Math Solution", shareText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "راه حل کامل در کلیپ‌بورد کپی شد!", Toast.LENGTH_SHORT).show()
}

/**
 * Interactive Concept Check Card to evaluate student understanding after viewing the solution
 */
@Composable
private fun ConceptCheckCard(
    conceptQuestion: ConceptCheckQuestion?,
    subject: String,
    topic: String
) {
    val question = remember(conceptQuestion, subject, topic) {
        conceptQuestion ?: generateFallbackConceptQuestion(subject, topic)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    val isCorrect = selectedIndex == question.correctOptionIndex

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("concept_check_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryBlue.copy(alpha = 0.07f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            PrimaryBlue.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "سنجش یادگیری انیشتین 🎯",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "سوال مفهومی برای محک زدن یادگیری شما",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasAnswered) {
                    TextButton(
                        onClick = {
                            selectedIndex = null
                            hasAnswered = false
                        },
                        modifier = Modifier.testTag("concept_retry_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تلاش مجدد", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Options
            question.options.forEachIndexed { index, optionText ->
                val isSelected = selectedIndex == index
                val isCorrectOption = index == question.correctOptionIndex

                val backgroundColor = when {
                    !hasAnswered && isSelected -> PrimaryBlue.copy(alpha = 0.15f)
                    hasAnswered && isCorrectOption -> AnswerEmerald.copy(alpha = 0.18f)
                    hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    hasAnswered && isCorrectOption -> AnswerEmerald
                    hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                    !hasAnswered && isSelected -> PrimaryBlue
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                }

                Surface(
                    onClick = {
                        if (!hasAnswered) {
                            selectedIndex = index
                            hasAnswered = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = backgroundColor,
                    border = androidx.compose.foundation.BorderStroke(
                        if (hasAnswered && (isCorrectOption || isSelected)) 2.dp else 1.dp,
                        borderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("concept_option_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                hasAnswered && isCorrectOption -> AnswerEmerald
                                hasAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                                isSelected -> PrimaryBlue
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (hasAnswered && isCorrectOption) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else if (hasAnswered && isSelected && !isCorrect) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected || (hasAnswered && isCorrectOption)) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Feedback and Explanation when answered
            AnimatedVisibility(visible = hasAnswered) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCorrect) AnswerEmeraldContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isCorrect) AnswerEmerald else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCorrect) "آفرین! مفهوم مسئله را کاملاً درست یاد گرفتی 🌟" else "پاسخ صحیح گزینه ${question.correctOptionIndex + 1} است.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    if (question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 تحلیل مفهومی انیشتین:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateFallbackConceptQuestion(subject: String, topic: String): ConceptCheckQuestion {
    return if (subject.contains("فیزیک") || topic.contains("حرکت") || topic.contains("نیرو")) {
        ConceptCheckQuestion(
            question = "در این مسئله، اگر متغیر اصلی (مانند جرم یا فاصله) دو برابر می‌شد، کمیت نهایی بر اساس فرمول‌های استفاده شده چه تغییری می‌کرد؟",
            options = listOf("دو برابر می‌شد", "چهار برابر می‌شد", "نصف می‌شد", "تغییری نمی‌کرد"),
            correctOptionIndex = 0,
            explanation = "با توجه به روابط خطی موجود در صورت مسئله، متغیرها با یکدیگر رابطه مستقیم دارند."
        )
    } else {
        ConceptCheckQuestion(
            question = "در حل این مسئله، کدام مفهوم یا رابطه جبری بیشترین نقش را در ساده‌سازی مراحل ایفا کرد؟",
            options = listOf("فاکتورگیری و ساده‌سازی روابط", "ضرب متقابل طرفین", "جایگذاری مستقیم در معادله", "تعیین علامت و ریشه‌ها"),
            correctOptionIndex = 2,
            explanation = "با جایگذاری صحیح مقادیر در رابطه اصلی، مسئله به ساده‌ترین فرم ممکن تبدیل و محاسبه گردید."
        )
    }
}

