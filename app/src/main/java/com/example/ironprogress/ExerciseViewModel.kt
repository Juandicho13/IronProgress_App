package com.example.ironprogress

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserProfile(
    val name: String = "",
    val currentWeight: String = "",
    val height: String = "",
    val isKg: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

class ExerciseViewModel(private val dao: IronDao, private val context: Context) : ViewModel() {

    // --- 1. DEFINICIÓN DE VARIABLES (ESTO DEBE IR PRIMERO) ---

    // Perfil
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    // Carpetas (Folders)
    private val _currentFolder = MutableStateFlow<String?>(null)
    val currentFolder = _currentFolder.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders = _folders.asStateFlow()

    // Temporizador
    private val _currentRestTime = MutableStateFlow(90)
    val currentRestTime = _currentRestTime.asStateFlow()
    private var timerJob: Job? = null

    // Entrenamiento en Vivo
    private val _activeRoutineExercises = MutableStateFlow<List<RoutineExerciseEntity>?>(null)
    val activeRoutineExercises = _activeRoutineExercises.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex = _currentExerciseIndex.asStateFlow()

    private val _currentSetNumber = MutableStateFlow(1)
    val currentSetNumber = _currentSetNumber.asStateFlow()

    // --- 2. BLOQUE INIT (AHORA ES SEGURO EJECUTARLO) ---
    // Al estar aquí abajo, las variables de arriba YA existen.
    init {
        loadUserProfile()
        loadLastFolder()
        loadFolders()
    }

    // --- LÓGICA DE PERFIL Y CARPETAS ---
    private fun loadUserProfile() {
        val prefs = context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
        _userProfile.value = UserProfile(
            name = prefs.getString("USER_NAME", "") ?: "",
            currentWeight = prefs.getString("USER_WEIGHT", "") ?: "",
            height = prefs.getString("USER_HEIGHT", "") ?: "",
            isKg = prefs.getBoolean("USER_IS_KG", true),
            soundEnabled = prefs.getBoolean("USER_SOUND", true),
            vibrationEnabled = prefs.getBoolean("USER_VIBRATION", true)
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("USER_NAME", profile.name)
                putString("USER_WEIGHT", profile.currentWeight)
                putString("USER_HEIGHT", profile.height)
                putBoolean("USER_IS_KG", profile.isKg)
                putBoolean("USER_SOUND", profile.soundEnabled)
                putBoolean("USER_VIBRATION", profile.vibrationEnabled)
                apply()
            }
            _userProfile.value = profile
        }
    }

    private fun loadFolders() {
        val prefs = context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet("FOLDERS_LIST", null)

        if (savedSet == null) {
            val defaults = listOf("GIMNASIO", "CASA")
            _folders.value = defaults
            saveFoldersList(defaults)
        } else {
            _folders.value = savedSet.toList().sorted()
        }
    }

    fun createFolder(name: String) {
        val currentList = _folders.value.toMutableList()
        if (!currentList.contains(name.uppercase())) {
            currentList.add(name.uppercase())
            _folders.value = currentList.sorted()
            saveFoldersList(currentList)
        }
    }

    fun deleteFolder(folderName: String) {
        val currentList = _folders.value.toMutableList()
        if (currentList.contains(folderName)) {
            currentList.remove(folderName)
            _folders.value = currentList.sorted()
            saveFoldersList(currentList)
        }
    }

    private fun saveFoldersList(list: List<String>) {
        val prefs = context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("FOLDERS_LIST", list.toSet()).apply()
    }

    fun setFolder(folderName: String?) {
        _currentFolder.value = folderName
        context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
            .edit().putString("LAST_FOLDER", folderName).apply()
    }

    private fun loadLastFolder() {
        val prefs = context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
        // ESTA LÍNEA CAUSABA EL CRASH ANTES
        _currentFolder.value = prefs.getString("LAST_FOLDER", null)
    }

    // --- TEMPORIZADOR ---
    fun startRestTimer(seconds: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME, seconds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = seconds
            while (timeLeft > 0) {
                _currentRestTime.value = timeLeft
                delay(1000)
                timeLeft--
            }
            _currentRestTime.value = 0
        }
    }

    fun stopRestTimer() {
        val intent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP }
        context.startService(intent)
        timerJob?.cancel()
    }

    // --- HISTORIAL Y DAO ---
    val groupedExercises: StateFlow<Map<String, List<ExerciseEntity>>> = dao.getAllExercises()
        .map { list ->
            list.groupBy { exercise ->
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(exercise.date))
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val uniqueExerciseNames: Flow<List<String>> = dao.getUniqueExerciseNames()

    fun getExerciseHistory(name: String): Flow<List<ExerciseEntity>> = dao.getHistoryByName(name)

    fun getTodaysHistory(name: String): Flow<List<ExerciseEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        return dao.getHistoryByDateRange(name, start, end)
    }

    fun addExercise(name: String, muscle: String, sets: Int, reps: String, weight: Double) {
        viewModelScope.launch {
            dao.insertExercise(
                ExerciseEntity(
                    name = name,
                    muscleGroup = muscle,
                    targetSets = sets,
                    targetReps = reps,
                    currentWeight = weight
                )
            )
        }
    }

    fun updateExercise(exercise: ExerciseEntity) { viewModelScope.launch { dao.updateExercise(exercise) } }
    fun deleteExercise(id: Int) { viewModelScope.launch { dao.deleteExercise(id) } }

    // --- GESTIÓN DE RUTINAS ---
    val allRoutines: Flow<List<RoutineEntity>> = dao.getAllRoutines()

    fun addRoutineComplex(name: String, restTime: Int, folder: String, exercises: List<RoutineExerciseEntity>) {
        viewModelScope.launch {
            val routineId = dao.insertRoutine(RoutineEntity(name = name, restTimeSeconds = restTime, folder = folder)).toInt()
            exercises.forEach { exercise -> dao.insertRoutineExercise(exercise.copy(routineId = routineId)) }
        }
    }

    fun updateRoutineComplex(routineId: Int, name: String, restTime: Int, exercises: List<RoutineExerciseEntity>) {
        viewModelScope.launch {
            val oldRoutine = dao.getRoutineById(routineId)
            val currentFolder = oldRoutine?.folder ?: "General"

            dao.updateRoutine(RoutineEntity(id = routineId, name = name, restTimeSeconds = restTime, folder = currentFolder))
            dao.deleteRoutineExercises(routineId)
            exercises.forEach { exercise -> dao.insertRoutineExercise(exercise.copy(id = 0, routineId = routineId)) }
        }
    }

    suspend fun getExercisesForRoutine(routineId: Int): List<RoutineExerciseEntity> {
        return dao.getRoutineExercises(routineId)
    }

    fun deleteRoutine(id: Int) { viewModelScope.launch { dao.deleteRoutine(id) } }

    // --- WORKOUT EN VIVO ---
    fun startLiveWorkout(routineId: Int) {
        viewModelScope.launch {
            val routine = dao.getRoutineById(routineId) ?: return@launch
            val exercises = dao.getRoutineExercises(routineId)
            _currentRestTime.value = routine.restTimeSeconds
            _activeRoutineExercises.value = exercises
            _currentExerciseIndex.value = 0
            _currentSetNumber.value = 1
            saveLastRoutineId(routineId)
        }
    }

    fun logSetData(currentEx: RoutineExerciseEntity, actualReps: String, actualWeight: Double) {
        viewModelScope.launch {
            val logEntry = ExerciseEntity(
                name = currentEx.name,
                muscleGroup = "Rutina en Vivo",
                targetSets = currentSetNumber.value,
                targetReps = actualReps,
                currentWeight = actualWeight,
                date = System.currentTimeMillis()
            )
            dao.insertExercise(logEntry)

            dao.updateRoutineExercisePreview(currentEx.id, actualWeight, actualReps)

            _activeRoutineExercises.value?.let { list ->
                val updatedList = list.map {
                    if (it.id == currentEx.id) it.copy(weight = actualWeight, reps = actualReps) else it
                }
                _activeRoutineExercises.value = updatedList
            }
        }
    }

    fun advanceState() {
        val exercises = _activeRoutineExercises.value ?: return

        if (exercises.isEmpty()) return
        val currentIndex = _currentExerciseIndex.value
        if (currentIndex >= exercises.size) return
        val currentEx = exercises[currentIndex]

        if (_currentSetNumber.value < currentEx.sets) {
            _currentSetNumber.value += 1
        } else {
            _currentExerciseIndex.value += 1
            _currentSetNumber.value = 1
        }
    }

    private fun saveLastRoutineId(routineId: Int) {
        context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE)
            .edit().putInt("LAST_ROUTINE_ID", routineId).apply()
    }

    fun getNextRoutineId(routines: List<RoutineEntity>): Int {
        if (routines.isEmpty()) return -1
        val lastId = context.getSharedPreferences("IronPrefs", Context.MODE_PRIVATE).getInt("LAST_ROUTINE_ID", -1)
        val index = routines.indexOfFirst { it.id == lastId }
        if (index == -1) return routines.first().id
        return routines[(index + 1) % routines.size].id
    }

    // --- MATEMÁTICAS ---
    fun calculate1RM(weight: Double, reps: Int): Int {
        if (weight <= 0 || reps <= 0) return 0
        if (reps == 1) return weight.toInt()
        return (weight * (1 + reps / 30.0)).toInt()
    }
}

class ExerciseViewModelFactory(private val dao: IronDao, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseViewModel(dao, context) as T
    }
}