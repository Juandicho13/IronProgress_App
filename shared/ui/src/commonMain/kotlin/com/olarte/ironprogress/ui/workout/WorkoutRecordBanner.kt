package com.olarte.ironprogress.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.progress.RecordBanner

/**
 * La franja del récord en vivo (F2-03) con los datos del entreno. Va debajo de la barra de arriba
 * de Entreno y del descanso: abre su espacio en vez de taparlos.
 */
@Composable
fun WorkoutRecordBanner(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit, modifier: Modifier = Modifier) {
    val record = state.record
    val exercise = record?.let { state.catalog[it.exerciseId] }
    RecordBanner(
        hit = record,
        exerciseName = exercise?.name?.resolve(appLanguage()) ?: record?.exerciseId?.value.orEmpty(),
        unit = exercise?.preferredUnit ?: WeightUnit.KG,
        modifier = modifier,
        onDismiss = { onIntent(WorkoutIntent.DismissRecord) },
    )
}
