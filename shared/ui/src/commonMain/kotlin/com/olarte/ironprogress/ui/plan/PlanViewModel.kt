package com.olarte.ironprogress.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.routine.EditRoutines
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanState(
    val routines: List<Routine> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val counts: Map<RoutineId, Int> = emptyMap(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val failed: Boolean = false,
)
sealed interface PlanIntent {
    data object Retry : PlanIntent
    data class FolderSave(val name: String, val folder: Folder? = null) : PlanIntent
    data class FolderDelete(val id: FolderId) : PlanIntent
    data class Delete(val id: RoutineId) : PlanIntent
    data class Duplicate(val id: RoutineId, val name: String) : PlanIntent
    data class MoveFolder(val id: FolderId, val target: Int) : PlanIntent
    data class Move(val id: RoutineId, val target: Int) : PlanIntent
}
class PlanViewModel(private val repository: RoutineRepository, private val edit: EditRoutines) : ViewModel() {
    private val mutableState = MutableStateFlow(PlanState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init {
        observe()
    }
    private fun observe() {
        mutableState.update { it.copy(loading = true, failed = false) }
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                combine(repository.observeRoutines(), repository.observeFolders()) { routines, folders -> routines to folders }
                    .catch { mutableState.update { it.copy(failed = true, loading = false) } }
                    .collect { (routines, folders) ->
                        val counts = routines.associate { it.id to (repository.findRoutine(it.id)?.exercises?.size ?: 0) }
                        mutableState.update { it.copy(routines = routines, folders = folders, counts = counts, loading = false) }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(failed = true, loading = false) }
            }
        }
    }
    fun onIntent(intent: PlanIntent) {
        if (intent == PlanIntent.Retry) {
            observe()
            return
        }
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            try {
                when (intent) {
                    PlanIntent.Retry -> Unit
                    is PlanIntent.FolderSave -> edit.folder(intent.name, intent.folder)
                    is PlanIntent.FolderDelete -> repository.deleteFolder(intent.id)
                    is PlanIntent.Delete -> repository.deleteRoutine(intent.id)
                    is PlanIntent.Duplicate -> edit.duplicate(intent.id, intent.name)
                    is PlanIntent.MoveFolder -> edit.moveFolder(intent.id, intent.target)
                    is PlanIntent.Move -> edit.move(intent.id, intent.target)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(failed = true) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
