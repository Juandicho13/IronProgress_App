package com.example.ironprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ironprogress.ui.theme.IronProgressTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IronProgressTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val database = IronDatabase.getDatabase(context)
                    val dao = database.ironDao()

                    // ViewModel compartido por toda la app
                    val viewModel: ExerciseViewModel = viewModel(
                        factory = ExerciseViewModelFactory(dao, context)
                    )

                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "dashboard") {

                        // 1. INICIO
                        composable("dashboard") {
                            HomeScreen(viewModel = viewModel, navController = navController)
                        }

                        // 2. LISTA MANUAL
                        composable("home_workout") {
                            ExerciseScreen(viewModel = viewModel, navController = navController)
                        }

                        // 3. GESTIÓN DE RUTINAS
                        composable("routines") {
                            RoutineScreen(viewModel = viewModel, navController = navController)
                        }

                        // 4. TEMPORIZADOR
                        composable(
                            route = "timer/{duration}/{nextExercise}",
                            arguments = listOf(
                                navArgument("duration") { type = NavType.IntType },
                                navArgument("nextExercise") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val duration = backStackEntry.arguments?.getInt("duration") ?: 60
                            val nextExercise = backStackEntry.arguments?.getString("nextExercise") ?: ""

                            TimerScreen(
                                durationSeconds = duration,
                                nextExerciseName = nextExercise,
                                onFinish = {
                                    viewModel.advanceState()
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 5. DETALLE DE GRÁFICA
                        composable(
                            route = "graph/{name}",
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            GraphScreen(
                                exerciseName = name,
                                viewModel = viewModel,
                                navController = navController
                            )
                        }

                        // 6. ENTRENAMIENTO EN VIVO
                        composable("live_workout") {
                            LiveWorkoutScreen(viewModel = viewModel, navController = navController)
                        }

                        // 7. MENÚ DE PROGRESO
                        composable("progress") {
                            ProgressScreen(viewModel = viewModel, navController = navController)
                        }

                        // 8. 🔥 PERFIL (NUEVO)
                        composable("profile") {
                            // Importante: Pasamos el viewModel para guardar/leer preferencias
                            ProfileScreen(navController = navController, viewModel = viewModel)
                        }

                        // 9. CALCULADORA 1RM (NUEVO)
                        composable("calculator") {
                            OneRMCalculatorScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}