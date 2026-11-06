package com.olarte.ironprogress.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.CivilDate
import com.olarte.ironprogress.domain.session.DeletePastEntry
import com.olarte.ironprogress.domain.session.EditPastSet
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.ui.today.localEpochDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Una sesión terminada en la lista del historial. */
data class HistoryRow(
    val session: WorkoutSession,
    val routineName: String?,
    val day: CivilDate,
    val volumeKg: Double,
    val workingSets: Int,
) {
    val durationSeconds: Long
        get() = session.endedAt?.let { (it - session.startedAt).inWholeSeconds.coerceAtLeast(0) } ?: 0
}

data class HistoryState(
    val loading: Boolean = true,
    val rows: List<HistoryRow> = emptyList(),
    val calendar: Boolean = true,
    val year: Int = 1970,
    val month: Int = 1,
    val selectedDay: CivilDate? = null,
    val failed: Boolean = false,
) {
    val trainedDays: Set<CivilDate> get() = rows.map { it.day }.toSet()

    /** En el calendario, las sesiones del día elegido; en la lista, todas. */
    val visibleRows: List<HistoryRow>
        get() = if (calendar) rows.filter { it.day == selectedDay } else rows
}

sealed interface HistoryIntent {
    data class ShowCalendar(val calendar: Boolean) : HistoryIntent
    data class MoveMonth(val delta: Int) : HistoryIntent
    data class SelectDay(val day: CivilDate) : HistoryIntent
    data object Retry : HistoryIntent
}

/** Historial por calendario y por lista (F1-23). Se actualiza solo al terminar o editar una sesión. */
class HistoryViewModel(
    private val sessions: SessionRepository,
    private val routines: RoutineRepository,
    clock: Clock,
    private val dayOf: (Long) -> Long = ::localEpochDay,
) : ViewModel() {
    private val today = CivilDate.fromEpochDay(dayOf(clock.now().toEpochMilliseconds()))
    private val mutableState = MutableStateFlow(HistoryState(year = today.year, month = today.month, selectedDay = today))
    val state = mutableState.asStateFlow()

    init {
        observe()
    }

    private var observation: Job? = null

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            sessions.observeFinishedSessions()
                .catch { mutableState.update { it.copy(loading = false, failed = true) } }
                .collect { finished ->
                    try {
                        val names = mutableMapOf<String, String?>()
                        val rows = finished.sortedByDescending { it.startedAt }.mapNotNull { session ->
                            val detail = sessions.findSession(session.id) ?: return@mapNotNull null
                            val name = session.routineId?.let { id ->
                                names.getOrPut(id.value) { routines.findRoutine(id)?.routine?.name }
                            }
                            HistoryRow(
                                session,
                                name,
                                CivilDate.fromEpochDay(dayOf(session.startedAt.toEpochMilliseconds())),
                                detail.volumeKg,
                                detail.workingSetsDone,
                            )
                        }
                        mutableState.update { it.copy(loading = false, rows = rows, failed = false) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutableState.update { it.copy(loading = false, failed = true) }
                    }
                }
        }
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.ShowCalendar -> mutableState.update { it.copy(calendar = intent.calendar) }
            is HistoryIntent.MoveMonth -> mutableState.update {
                val index = it.year * 12 + (it.month - 1) + intent.delta
                it.copy(year = index.floorDiv(12), month = index.mod(12) + 1)
            }
            is HistoryIntent.SelectDay -> mutableState.update { it.copy(selectedDay = intent.day) }
            HistoryIntent.Retry -> {
                mutableState.update { it.copy(loading = true, failed = false) }
                observe()
            }
        }
    }
}

/** Serie abierta para corregir, con lo escrito tal cual. */
data class SetDraft(val set: WorkoutSet, val weight: String, val reps: String, val invalid: Boolean = false)

data class HistorySessionState(
    val loading: Boolean = true,
    val detail: SessionDetail? = null,
    val routineName: String? = null,
    val exercises: Map<ExerciseId, Exercise> = emptyMap(),
    val draft: SetDraft? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
) {
    fun exerciseName(id: ExerciseId, languageCode: String): String = exercises[id]?.name?.resolve(languageCode) ?: id.value
}

sealed interface HistorySessionIntent {
    data class Open(val id: WorkoutSetId) : HistorySessionIntent
    data class Weight(val text: String) : HistorySessionIntent
    data class Reps(val text: String) : HistorySessionIntent
    data object Save : HistorySessionIntent
    data object Cancel : HistorySessionIntent
    data object DeleteSet : HistorySessionIntent
    data class DeleteBlock(val id: SessionBlockId) : HistorySessionIntent
    data object Retry : HistorySessionIntent
}

/** Una sesión pasada con sus series y bloques, para corregir o borrar (F1-23). */
class HistorySessionViewModel(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val editPastSet: EditPastSet,
    private val deletePast: DeletePastEntry,
    sessionId: String,
) : ViewModel() {
    private val id = SessionId(sessionId)
    private val mutableState = MutableStateFlow(HistorySessionState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val detail = sessions.findSession(id)
                val name = detail?.session?.routineId?.let { routines.findRoutine(it)?.routine?.name }
                val catalog = exercises.observeExercises().first().associateBy { it.id }
                mutableState.update {
                    it.copy(loading = false, detail = detail, routineName = name, exercises = catalog, failed = detail == null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    private fun write(action: suspend () -> Unit) {
        if (state.value.saving) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                action()
                val detail = sessions.findSession(id)
                mutableState.update { it.copy(saving = false, detail = detail, draft = null, failed = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    fun onIntent(intent: HistorySessionIntent) {
        when (intent) {
            is HistorySessionIntent.Open -> {
                val set = state.value.detail?.sets?.firstOrNull { it.id == intent.id } ?: return
                mutableState.update { it.copy(draft = SetDraft(set, set.weightKg?.let(::plain).orEmpty(), set.reps?.toString().orEmpty())) }
            }
            is HistorySessionIntent.Weight -> mutableState.update { it.copy(draft = it.draft?.copy(weight = intent.text, invalid = false)) }
            is HistorySessionIntent.Reps -> mutableState.update { it.copy(draft = it.draft?.copy(reps = intent.text, invalid = false)) }
            HistorySessionIntent.Cancel -> mutableState.update { it.copy(draft = null) }
            HistorySessionIntent.Save -> {
                val draft = state.value.draft ?: return
                val weight = draft.weight.trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()
                val reps = draft.reps.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                val weightOk = draft.weight.isBlank() || (weight != null && weight >= 0 && weight.isFinite())
                val repsOk = draft.reps.isBlank() || (reps != null && reps >= 0)
                if (!weightOk || !repsOk) {
                    mutableState.update { it.copy(draft = draft.copy(invalid = true)) }
                    return
                }
                write { editPastSet(id, draft.set.copy(weightKg = weight, reps = reps)) }
            }
            HistorySessionIntent.DeleteSet -> {
                val draft = state.value.draft ?: return
                write { deletePast.set(id, draft.set.id) }
            }
            is HistorySessionIntent.DeleteBlock -> write { deletePast.block(id, intent.id) }
            HistorySessionIntent.Retry -> {
                mutableState.update { it.copy(loading = true, failed = false) }
                load()
            }
        }
    }

    /** "62.5" o "60": lo que se escribe en el campo, sin ceros de más. */
    private fun plain(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()
}
