package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FormulaReferenceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SolutionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SolverViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Persian is an RTL language - provide RTL layout direction for student-friendly UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainAppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: SolverViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeSolution by viewModel.activeSolution.collectAsState()

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
                onNavigateToQuiz = { viewModel.navigateTo(AppScreen.QUIZ) },
                onNavigateToNotes = { viewModel.navigateTo(AppScreen.NOTES) },
                onNavigateToCameraScan = { viewModel.navigateTo(AppScreen.CAMERA_SCAN) },
                onNavigateToFormulas = { viewModel.navigateTo(AppScreen.FORMULA_REFERENCE) },
                onNavigateToDashboard = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        }
        AppScreen.CAMERA_SCAN -> {
            CameraScanScreen(
                onImageCaptured = { bitmap ->
                    viewModel.onImageSelected(bitmap)
                    viewModel.navigateTo(AppScreen.HOME)
                },
                onClose = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.SOLUTION -> {
            activeSolution?.let { solution ->
                SolutionScreen(
                    solution = solution,
                    viewModel = viewModel,
                    onBackClick = { viewModel.navigateTo(AppScreen.HOME) },
                    onSolveAnother = {
                        viewModel.clearInput()
                        viewModel.navigateTo(AppScreen.HOME)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            } ?: run {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
                    onNavigateToQuiz = { viewModel.navigateTo(AppScreen.QUIZ) },
                    onNavigateToNotes = { viewModel.navigateTo(AppScreen.NOTES) },
                    onNavigateToCameraScan = { viewModel.navigateTo(AppScreen.CAMERA_SCAN) },
                    onNavigateToFormulas = { viewModel.navigateTo(AppScreen.FORMULA_REFERENCE) },
                    onNavigateToDashboard = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                )
            }
        }
        AppScreen.HISTORY -> {
            HistoryScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.QUIZ -> {
            QuizScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.NOTES -> {
            NotesScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.FORMULA_REFERENCE -> {
            FormulaReferenceScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.DASHBOARD -> {
            DashboardScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.navigateTo(AppScreen.HOME) },
                onNavigateToSolver = { viewModel.navigateTo(AppScreen.HOME) },
                onNavigateToQuiz = { viewModel.navigateTo(AppScreen.QUIZ) }
            )
        }
    }
}
