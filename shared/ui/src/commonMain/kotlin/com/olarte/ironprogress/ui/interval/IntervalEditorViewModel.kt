package com.olarte.ironprogress.ui.interval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.interval.IntervalPreset
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateId
import com.olarte.ironprogress.domain.interval.IntervalTemplateRepository
import com.olarte.ironprogress.domain.voice.VoiceSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IntervalEditorState(
    val template: IntervalTemplate,
    val exercises: List<Exercise> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
) {
    val selected: List<Exercise> get() = template.exerciseIds.mapNotNull { id -> exercises.firstOrNull { it.id == id } }
}

sealed interface IntervalEditorIntent {
    data class Rename(val name: String) : IntervalEditorIntent
    data class ChoosePreset(val preset: IntervalPreset) : IntervalEditorIntent
    data class Work(val delta: Int) : IntervalEditorIntent
    data class Rest(val delta: Int) : IntervalEditorIntent
    data class Rounds(val delta: Int) : IntervalEditorIntent
    data class ToggleExercise(val id: ExerciseId) : IntervalEditorIntent
    data class Voice(val settings: VoiceSettings) : IntervalEditorIntent
    data object Save : IntervalEditorIntent
    data object Start : IntervalEditorIntent
}

sealed interface IntervalEditorEffect {
    data object Close : IntervalEditorEffect
    data class Run(val id: String) : IntervalEditorEffect
}

/** Crear y editar intervalos (PANTALLAS 5.2). Empezar guarda primero y abre la pantalla de 5.3. */
class IntervalEditorViewModel(
    private val templates: IntervalTemplateRepository,
    private val catalog: ExerciseRepository,
    ids: IdGenerator,
    private val editingId: String?,
    defaultName: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        IntervalEditorState(
            template = IntervalTemplate(
                id = IntervalTemplateId(editingId ?: ids.newId()),
                name = defaultName,
                workSeconds = IntervalPreset.FIFTEEN_FORTY_FIVE.workSeconds,
                restSeconds = IntervalPreset.FIFTEEN_FORTY_FIVE.restSeconds,
                rounds = IntervalPreset.FIFTEEN_FORTY_FIVE.rounds,
            ),
            loading = editingId != null,
        ),
    )
    val state = mutableState.asStateFlow()
    private val events = Channel<IntervalEditorEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val exercises = catalog.observeExercises().first().filter { !it.isArchived }
            val saved = editingId?.let { templates.findTemplate(IntervalTemplateId(it)) }
            mutableState.update { it.copy(exercises = exercises, template = saved ?: it.template, loading = false) }
        }
    }

    fun onIntent(intent: IntervalEditorIntent) {
        when (intent) {
            is IntervalEditorIntent.Rename -> edit { if (intent.name.isBlank()) it else it.copy(name = intent.name.take(MAX_NAME)) }
            is IntervalEditorIntent.ChoosePreset -> edit { it.withPreset(intent.preset) }
            is IntervalEditorIntent.Work -> edit {
                it.copy(workSeconds = (it.workSeconds + intent.delta).coerceIn(IntervalTemplate.WORK_RANGE))
            }
            is IntervalEditorIntent.Rest -> edit {
                it.copy(restSeconds = (it.restSeconds + intent.delta).coerceIn(IntervalTemplate.REST_RANGE))
            }
            is IntervalEditorIntent.Rounds -> edit {
                it.copy(rounds = (it.rounds + intent.delta).coerceIn(IntervalTemplate.ROUNDS_RANGE))
            }
            is IntervalEditorIntent.ToggleExercise -> edit {
                val ids = it.exerciseIds
                when {
                    intent.id in ids -> it.copy(exerciseIds = ids - intent.id)
                    ids.size < MAX_EXERCISES -> it.copy(exerciseIds = ids + intent.id)
                    else -> it
                }
            }
            is IntervalEditorIntent.Voice -> edit { it.copy(voice = intent.settings) }
            IntervalEditorIntent.Save -> save { events.trySend(IntervalEditorEffect.Close) }
            IntervalEditorIntent.Start -> save { events.trySend(IntervalEditorEffect.Run(it.id.value)) }
        }
    }

    private fun edit(change: (IntervalTemplate) -> IntervalTemplate) {
        mutableState.update { it.copy(template = change(it.template), failed = false) }
    }

    private fun save(then: (IntervalTemplate) -> Unit) {
        if (state.value.saving) return
        val template = state.value.template
        mutableState.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                templates.saveTemplate(template)
                mutableState.update { it.copy(saving = false) }
                then(template)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    private companion object {
        const val MAX_EXERCISES = 4
        const val MAX_NAME = 40
    }
}
