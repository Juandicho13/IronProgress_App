package com.olarte.ironprogress.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.body.AddProgressPhoto
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.BodyRepository
import com.olarte.ironprogress.domain.body.BodyTrends
import com.olarte.ironprogress.domain.body.BodyWeight
import com.olarte.ironprogress.domain.body.LogBodyMeasurement
import com.olarte.ironprogress.domain.body.LogBodyWeight
import com.olarte.ironprogress.domain.body.MeasureTrend
import com.olarte.ironprogress.domain.body.ProgressPhoto
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.SettingsRepository
import com.olarte.ironprogress.ui.format.Formats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * La pestaña Cuerpo de Progreso (F2-09). [unit] sale del perfil de la bienvenida: con libras, el peso
 * va en lb y las medidas en pulgadas; la base guarda siempre kg y cm.
 */
data class BodyState(
    val loading: Boolean = true,
    val weights: List<BodyWeight> = emptyList(),
    val weightChangeKg: Double? = null,
    val measures: List<MeasureTrend> = emptyList(),
    val photos: List<ProgressPhoto> = emptyList(),
    /** "Ocultar peso corporal y calorías": los valores del peso no se ven. */
    val hidden: Boolean = false,
    /** Sin fotos en el modo joven (11.11). */
    val photosAllowed: Boolean = true,
    val unit: WeightUnit = WeightUnit.KG,
) {
    val latest: BodyWeight? get() = weights.lastOrNull()
    val isEmpty: Boolean get() = weights.isEmpty() && measures.isEmpty() && (photos.isEmpty() || !photosAllowed)
}

class BodyViewModel(
    private val body: BodyRepository,
    settings: SettingsRepository,
    private val logWeight: LogBodyWeight,
    private val logMeasure: LogBodyMeasurement,
    private val addPhoto: AddProgressPhoto,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<BodyState> = combine(
        body.observeWeights(),
        body.observeMeasurements(),
        body.observePhotos(),
        settings.settings,
    ) { weights, measures, photos, current ->
        BodyState(
            loading = false,
            weights = weights,
            weightChangeKg = BodyTrends.weightChange(weights, clock.now()),
            measures = BodyTrends.latestMeasures(measures),
            photos = photos,
            hidden = current.hideBodyMetrics,
            photosAllowed = !current.youngMode,
            unit = current.bodyUnit(),
        )
    }.catch { emit(BodyState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyState())

    /** [value] en la unidad que se ve. Responde por [onResult] si quedó guardado. */
    fun logWeight(value: Double, onResult: (Boolean) -> Unit = {}) {
        val kg = BodyUnits.toKg(value, state.value.unit)
        viewModelScope.launch { onResult(logWeight.invoke(kg)) }
    }

    fun logMeasure(kind: BodyMeasure, value: Double, customName: String?, onResult: (Boolean) -> Unit = {}) {
        val cm = BodyUnits.toCm(value, state.value.unit)
        viewModelScope.launch { onResult(logMeasure.invoke(kind, cm, customName)) }
    }

    fun deleteWeight(id: BodyEntryId) {
        viewModelScope.launch { body.deleteWeight(id) }
    }

    fun deleteMeasure(id: BodyEntryId) {
        viewModelScope.launch { body.deleteMeasurement(id) }
    }

    fun addPhoto(fileName: String) {
        viewModelScope.launch { addPhoto.invoke(fileName) }
    }

    fun deletePhoto(id: BodyEntryId) {
        viewModelScope.launch { body.deletePhoto(id) }
    }
}

/** El perfil guarda la unidad de la bienvenida; sin perfil, kg. */
private fun AppSettings.bodyUnit(): WeightUnit = when (profile?.unit) {
    com.olarte.ironprogress.domain.settings.WeightUnit.LB -> WeightUnit.LB
    else -> WeightUnit.KG
}

/** Cambios de unidad del cuerpo: lb y pulgadas para quien eligió libras. */
object BodyUnits {
    const val CM_PER_INCH = 2.54

    fun toKg(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) value / Formats.LB_PER_KG else value

    fun fromKg(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) kg * Formats.LB_PER_KG else kg

    fun toCm(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) value * CM_PER_INCH else value

    fun fromCm(cm: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) cm / CM_PER_INCH else cm
}
