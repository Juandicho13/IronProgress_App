package com.olarte.ironprogress.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.SaveCustomExercise
import com.olarte.ironprogress.domain.catalog.TrackingType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseDraft(
    val spanish: String = "",
    val english: String = "",
    val equipment: Equipment = Equipment.BODYWEIGHT,
    val discipline: Discipline = Discipline.STRENGTH,
    val pattern: MovementPattern = MovementPattern.OTHER,
    val tracking: TrackingType = TrackingType.REPS,
    val muscles: List<ExerciseMuscle> = emptyList(),
    val unilateral: Boolean = false,
) {
    val valid: Boolean get() = spanish.isNotBlank() &&
        english.isNotBlank() &&
        (pattern == MovementPattern.CARDIO || muscles.any { it.role == MuscleRole.PRIMARY })
}

data class ExerciseEditorState(
    val draft: ExerciseDraft = ExerciseDraft(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val dirty: Boolean = false,
    val failed: Boolean = false,
    val confirmDiscard: Boolean = false,
)

sealed interface ExerciseEditorIntent {
    data class Change(val draft: ExerciseDraft) : ExerciseEditorIntent
    data object Save : ExerciseEditorIntent
    data object Retry : ExerciseEditorIntent
    data object Back : ExerciseEditorIntent
    data object Discard : ExerciseEditorIntent
    data object KeepEditing : ExerciseEditorIntent
}

sealed interface ExerciseEditorEffect {
    data object Close : ExerciseEditorEffect
}

class ExerciseEditorViewModel(
    private val repository: ExerciseRepository,
    private val save: SaveCustomExercise,
    private val editingId: String?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ExerciseEditorState(loading = editingId != null))
    val state = mutableState.asStateFlow()
    private val events = Channel<ExerciseEditorEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()
    private var original: Exercise? = null

    init {
        load()
    }

    private fun load() {
        if (editingId == null) return
        mutableState.update { it.copy(loading = true, failed = false) }
        viewModelScope.launch {
            try {
                val exercise = requireNotNull(repository.findExercise(ExerciseId(editingId)))
                require(exercise.isCustom)
                original = exercise
                mutableState.value = ExerciseEditorState(
                    draft = ExerciseDraft(
                        exercise.name.es,
                        exercise.name.en,
                        exercise.equipment,
                        exercise.discipline,
                        exercise.pattern,
                        exercise.trackingType,
                        exercise.muscles,
                        exercise.unilateral,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun onIntent(intent: ExerciseEditorIntent) {
        if (state.value.saving) return
        when (intent) {
            is ExerciseEditorIntent.Change -> if (!state.value.loading) {
                mutableState.update { it.copy(draft = intent.draft, dirty = true, failed = false) }
            }
            ExerciseEditorIntent.Retry -> if (editingId != null && original == null) load() else persist()
            ExerciseEditorIntent.KeepEditing -> mutableState.update { it.copy(confirmDiscard = false) }
            ExerciseEditorIntent.Back -> if (state.value.dirty) {
                mutableState.update { it.copy(confirmDiscard = true) }
            } else {
                events.trySend(ExerciseEditorEffect.Close)
            }
            ExerciseEditorIntent.Discard -> events.trySend(ExerciseEditorEffect.Close)
            ExerciseEditorIntent.Save -> persist()
        }
    }

    private fun persist() {
        val current = state.value
        if (!current.draft.valid || current.loading || (editingId != null && original == null)) return
        mutableState.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                val draft = current.draft
                val exercise = Exercise(
                    id = original?.id ?: ExerciseId(""), name = LocalizedText(draft.spanish, draft.english),
                    equipment = draft.equipment, discipline = draft.discipline, pattern = draft.pattern,
                    trackingType = draft.tracking, muscles = draft.muscles, unilateral = draft.unilateral,
                    preferredUnit = original?.preferredUnit, level = original?.level,
                    aliases = original?.aliases.orEmpty(),
                )
                save(exercise, editingId?.let(::ExerciseId))
                mutableState.update { it.copy(saving = false, dirty = false) }
                events.send(ExerciseEditorEffect.Close)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, failed = true) }
            }
        }
    }
}
