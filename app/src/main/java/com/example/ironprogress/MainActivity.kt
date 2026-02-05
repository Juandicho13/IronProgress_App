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

/**
 * Actividad Principal (Entry Point).
 *
 * Configura la infraestructura base de la aplicación:
 * 1. Inicialización de la Base de Datos (Singleton).
 * 2. Inyección de Dependencias manual para el [ExerciseViewModel].
 * 3. Definición del Grafo de Navegación (NavHost).
 * 4. Aplicación del Tema Global (Dark Gym Theme).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IronProgressTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- Inyección de Dependencias (Service Locator Pattern) ---
                    val context = LocalContext.current
                    val database = IronDatabase.getDatabase(context)
                    val dao = database.ironDao()

                    // Instancia única del ViewModel compartida por toda la actividad
                    val viewModel: ExerciseViewModel = viewModel(
                        factory = ExerciseViewModelFactory(dao, context)
                    )

                    // Controlador de navegación
                    val navController = rememberNavController()

                    // --- Grafo de Navegación ---
                    NavHost(navController = navController, startDestination = "dashboard") {

                        // region Menú Principal
                        composable("dashboard") {
                            HomeScreen(viewModel = viewModel, navController = navController)
                        }
                        // endregion

                        // region Gestión de Entrenamiento
                        // Modo Manual (CRUD simple)
                        composable("home_workout") {
                            ExerciseScreen(viewModel = viewModel, navController = navController)
                        }

                        // Gestión de Planes y Rutinas
                        composable("routines") {
                            RoutineScreen(viewModel = viewModel, navController = navController)
                        }

                        // Modo Entrenamiento en Vivo (Live Workout)
                        composable("live_workout") {
                            LiveWorkoutScreen(viewModel = viewModel, navController = navController)
                        }

                        // Temporizador de Descanso (Pantalla de transición entre sets)
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
                                    // Al terminar el tiempo, avanzamos el estado lógico de la rutina
                                    viewModel.advanceState()
                                    navController.popBackStack()
                                }
                            )
                        }
                        // endregion

                        // region Analíticas y Datos
                        // Menú de Selección de Gráficas
                        composable("progress") {
                            ProgressScreen(viewModel = viewModel, navController = navController)
                        }

                        // Detalle de Gráfica (Canvas)
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
                        // endregion

                        // region Configuración y Herramientas
                        // Perfil de Usuario y Ajustes
                        composable("profile") {
                            ProfileScreen(navController = navController, viewModel = viewModel)
                        }

                        // Herramienta Externa: Calculadora de 1RM
                        composable("calculator") {
                            OneRMCalculatorScreen(navController = navController)
                        }
                        // endregion
                    }
                }
            }
        }
    }
}