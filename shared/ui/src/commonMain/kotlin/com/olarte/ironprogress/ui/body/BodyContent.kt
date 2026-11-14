package com.olarte.ironprogress.ui.body

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.MeasureKey
import com.olarte.ironprogress.domain.body.ProgressPhoto
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDialog
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaNote
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaRadioRow
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.components.ForjaTextField
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.progress.ChartPoint
import com.olarte.ironprogress.ui.progress.ProgressChart
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.body_cancel
import com.olarte.ironprogress.ui.resources.body_delete_entry
import com.olarte.ironprogress.ui.resources.body_delete_entry_confirm
import com.olarte.ironprogress.ui.resources.body_empty_body
import com.olarte.ironprogress.ui.resources.body_empty_title
import com.olarte.ironprogress.ui.resources.body_hidden_note
import com.olarte.ironprogress.ui.resources.body_hidden_value
import com.olarte.ironprogress.ui.resources.body_measure_arm
import com.olarte.ironprogress.ui.resources.body_measure_change
import com.olarte.ironprogress.ui.resources.body_measure_chest
import com.olarte.ironprogress.ui.resources.body_measure_custom
import com.olarte.ironprogress.ui.resources.body_measure_custom_name
import com.olarte.ironprogress.ui.resources.body_measure_hips
import com.olarte.ironprogress.ui.resources.body_measure_sheet_title
import com.olarte.ironprogress.ui.resources.body_measure_thigh
import com.olarte.ironprogress.ui.resources.body_measure_waist
import com.olarte.ironprogress.ui.resources.body_measure_which
import com.olarte.ironprogress.ui.resources.body_measures_add
import com.olarte.ironprogress.ui.resources.body_measures_empty
import com.olarte.ironprogress.ui.resources.body_measures_title
import com.olarte.ironprogress.ui.resources.body_out_of_range
import com.olarte.ironprogress.ui.resources.body_photo_a11y
import com.olarte.ironprogress.ui.resources.body_photo_delete
import com.olarte.ironprogress.ui.resources.body_photo_delete_confirm
import com.olarte.ironprogress.ui.resources.body_photo_failed
import com.olarte.ironprogress.ui.resources.body_photos_add
import com.olarte.ironprogress.ui.resources.body_photos_compare
import com.olarte.ironprogress.ui.resources.body_photos_private
import com.olarte.ironprogress.ui.resources.body_photos_title
import com.olarte.ironprogress.ui.resources.body_recent
import com.olarte.ironprogress.ui.resources.body_save
import com.olarte.ironprogress.ui.resources.body_sheet_weight_title
import com.olarte.ironprogress.ui.resources.body_unit_cm
import com.olarte.ironprogress.ui.resources.body_unit_in
import com.olarte.ironprogress.ui.resources.body_value_label
import com.olarte.ironprogress.ui.resources.body_weight_change_30
import com.olarte.ironprogress.ui.resources.body_weight_chart_a11y
import com.olarte.ironprogress.ui.resources.body_weight_first
import com.olarte.ironprogress.ui.resources.body_weight_log
import com.olarte.ironprogress.ui.resources.body_weight_title
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** Lo que la pestaña Cuerpo puede pedir. Sin pantalla propia: la usan Progreso y las pruebas. */
class BodyActions(
    val logWeight: (Double, (Boolean) -> Unit) -> Unit = { _, _ -> },
    val logMeasure: (BodyMeasure, Double, String?, (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    val deleteWeight: (BodyEntryId) -> Unit = {},
    val deleteMeasure: (BodyEntryId) -> Unit = {},
    val addPhoto: (String) -> Unit = {},
    val deletePhoto: (ProgressPhoto) -> Unit = {},
    val comparePhotos: () -> Unit = {},
)

private sealed interface BodySheet {
    data object Weight : BodySheet

    data class Measure(val preset: MeasureKey?) : BodySheet

    data class Photo(val photo: ProgressPhoto) : BodySheet
}

/**
 * La pestaña Cuerpo de Progreso (F2-09): peso con su gráfica, medidas y fotos privadas. Con
 * "Ocultar peso corporal y calorías" el peso sale como "Oculto" y sin gráfica. Sin
 * [withPhotoPicker] (pruebas en la JVM) no hay selector de fotos y las miniaturas no cargan.
 */
@Composable
fun BodyContent(state: BodyState, actions: BodyActions, modifier: Modifier = Modifier, withPhotoPicker: Boolean = true) {
    var sheet by remember { mutableStateOf<BodySheet?>(null) }
    var photoFailed by remember { mutableStateOf(false) }
    val store = if (withPhotoPicker) {
        rememberPhotoStore { pick ->
            photoFailed = pick == PhotoPick.Failed
            if (pick is PhotoPick.Saved) actions.addPhoto(pick.fileName)
        }
    } else {
        null
    }
    val pickPhoto: () -> Unit = { store?.pick() }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
        if (state.isEmpty) {
            EmptyIntro(state, onWeight = { sheet = BodySheet.Weight }, onMeasure = { sheet = BodySheet.Measure(null) }, onPhoto = pickPhoto)
        } else {
            WeightCard(state, onLog = { sheet = BodySheet.Weight }, onDelete = actions.deleteWeight)
            MeasuresCard(state, onLog = { sheet = BodySheet.Measure(it) })
            if (state.photosAllowed) {
                PhotosCard(state, store, onAdd = pickPhoto, onCompare = actions.comparePhotos, onOpen = { sheet = BodySheet.Photo(it) })
            }
        }
        if (photoFailed) ForjaNote(stringResource(Res.string.body_photo_failed), error = true)
    }

    when (val current = sheet) {
        BodySheet.Weight -> WeightSheet(state, actions.logWeight) { sheet = null }
        is BodySheet.Measure -> MeasureSheet(state.unit, current.preset, actions.logMeasure) { sheet = null }
        is BodySheet.Photo -> PhotoSheet(current.photo, store, onDelete = {
            actions.deletePhoto(current.photo)
            sheet = null
        }) { sheet = null }
        null -> Unit
    }
}

/** Sin nada registrado: una invitación con las tres acciones. */
@Composable
private fun EmptyIntro(state: BodyState, onWeight: () -> Unit, onMeasure: () -> Unit, onPhoto: () -> Unit) {
    ForjaEmptyState(stringResource(Res.string.body_empty_title), stringResource(Res.string.body_empty_body)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            ForjaButton(stringResource(Res.string.body_weight_log), Modifier.fillMaxWidth(), onClick = onWeight)
            Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                ForjaButton(
                    stringResource(Res.string.body_measures_add),
                    Modifier.weight(1f),
                    ForjaButtonStyle.SECONDARY,
                    onClick = onMeasure,
                )
                if (state.photosAllowed) {
                    ForjaButton(
                        stringResource(Res.string.body_photos_add),
                        Modifier.weight(1f),
                        ForjaButtonStyle.SECONDARY,
                        onClick = onPhoto,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightCard(state: BodyState, onLog: () -> Unit, onDelete: (BodyEntryId) -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    var confirm by remember { mutableStateOf<BodyEntryId?>(null) }
    ForjaCard {
        Text(stringResource(Res.string.body_weight_title), style = typography.title3, color = colors.text1)
        val latest = state.latest
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
        ) {
            when {
                state.hidden -> {
                    ForjaNumber(
                        stringResource(Res.string.body_hidden_value),
                        typography.largeFigure,
                        color = colors.text2,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(Res.string.body_hidden_note),
                        style = typography.body2,
                        color = colors.text2,
                        textAlign = TextAlign.Center,
                    )
                }
                latest != null -> {
                    ForjaNumber(formats.weight(latest.weightKg, state.unit), typography.largeFigure, textAlign = TextAlign.Center)
                    val change = state.weightChangeKg
                    Text(
                        if (change == null) {
                            stringResource(Res.string.body_weight_first)
                        } else {
                            stringResource(Res.string.body_weight_change_30, formats.signedWeight(change, state.unit))
                        },
                        style = typography.body2,
                        color = colors.text2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (!state.hidden && state.weights.size >= 2) {
            val points = state.weights.map { ChartPoint(it.measuredAt.toEpochMilliseconds(), BodyUnits.fromKg(it.weightKg, state.unit)) }
            val unitLabel = if (state.unit == WeightUnit.LB) "lb" else "kg"
            ProgressChart(
                points = points,
                recordIndex = null,
                formatValue = { "${formats.number(it, 1)} $unitLabel" },
                formatTick = { formats.number(it, 0) },
                formatDate = { formats.date(it) },
                recordLabel = { it },
                tooltipDetail = { null },
                description = stringResource(
                    Res.string.body_weight_chart_a11y,
                    formats.weight(state.weights.last().weightKg, state.unit),
                    points.size,
                ),
                height = 160.dp,
            )
        }
        if (!state.hidden && state.weights.isNotEmpty()) {
            Text(stringResource(Res.string.body_recent), style = typography.label, color = colors.text2)
            Column {
                state.weights.takeLast(RECENT).reversed().forEachIndexed { index, entry ->
                    if (index > 0) ForjaDivider()
                    ForjaListRow(
                        title = formats.date(entry.measuredAt.toEpochMilliseconds()),
                        value = formats.weight(entry.weightKg, state.unit),
                        chevron = false,
                        onClick = { confirm = entry.id },
                    )
                }
            }
        }
        ForjaButton(stringResource(Res.string.body_weight_log), Modifier.fillMaxWidth(), onClick = onLog)
    }
    confirm?.let { id ->
        val entry = state.weights.firstOrNull { it.id == id }
        ForjaDialog(
            title = stringResource(Res.string.body_delete_entry),
            text = entry?.let { stringResource(Res.string.body_delete_entry_confirm, formats.date(it.measuredAt.toEpochMilliseconds())) },
            onDismiss = { confirm = null },
            confirmText = stringResource(Res.string.body_delete_entry),
            dismissText = stringResource(Res.string.body_cancel),
            destructive = true,
            onConfirm = {
                onDelete(id)
                confirm = null
            },
        )
    }
}

@Composable
private fun MeasuresCard(state: BodyState, onLog: (MeasureKey?) -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    ForjaCard {
        Text(stringResource(Res.string.body_measures_title), style = typography.title3, color = colors.text1)
        if (state.measures.isEmpty()) {
            Text(stringResource(Res.string.body_measures_empty), style = typography.body2, color = colors.text2)
        } else {
            Column {
                state.measures.forEachIndexed { index, trend ->
                    if (index > 0) ForjaDivider()
                    ForjaListRow(
                        title = trend.key.label(),
                        subtitle = trend.changeCm?.let {
                            stringResource(Res.string.body_measure_change, signedLength(formats, it, state.unit))
                        },
                        value = length(formats, trend.latest.valueCm, state.unit),
                        chevron = false,
                        onClick = { onLog(trend.key) },
                    )
                }
            }
        }
        ForjaButton(stringResource(Res.string.body_measures_add), Modifier.fillMaxWidth(), ForjaButtonStyle.SECONDARY) { onLog(null) }
    }
}

@Composable
private fun PhotosCard(state: BodyState, store: PhotoStore?, onAdd: () -> Unit, onCompare: () -> Unit, onOpen: (ProgressPhoto) -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    ForjaCard {
        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            Text(stringResource(Res.string.body_photos_title), style = typography.title3, color = colors.text1)
            Text(stringResource(Res.string.body_photos_private), style = typography.caption, color = colors.text2)
        }
        if (state.photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                state.photos.reversed().take(MAX_THUMBNAILS).chunked(PHOTO_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                        row.forEach { photo -> PhotoThumbnail(photo, store, Modifier.weight(1f)) { onOpen(photo) } }
                        repeat(PHOTO_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            ForjaButton(
                stringResource(Res.string.body_photos_add),
                Modifier.weight(1f),
                ForjaButtonStyle.SECONDARY,
                enabled = store != null,
                onClick = onAdd,
            )
            ForjaButton(
                stringResource(Res.string.body_photos_compare),
                Modifier.weight(1f),
                ForjaButtonStyle.TONAL,
                enabled = state.photos.size >= 2,
                onClick = onCompare,
            )
        }
    }
}

/** Miniatura 3:4 con la fecha abajo. Mientras carga, el fondo surface3. */
@Composable
fun PhotoThumbnail(
    photo: ProgressPhoto,
    store: PhotoStore?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val formats = rememberFormats()
    val date = formats.date(photo.takenAt.toEpochMilliseconds())
    val description = stringResource(Res.string.body_photo_a11y, date)
    val bitmap = store?.let { rememberPhoto(it, photo.fileName, THUMBNAIL_PX) }
    Box(
        modifier.aspectRatio(3f / 4f).clip(ForjaRadius.shapeS).background(colors.surface3)
            .clickable(role = Role.Image, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
        // La fecha se achica antes que cortarse en las miniaturas pequeñas.
        ForjaNumber(
            date,
            ForjaTheme.typography.caption,
            Modifier.fillMaxWidth().background(colors.modalScrim).padding(horizontal = ForjaSpacing.esp1, vertical = ForjaSpacing.esp1),
            color = if (bitmap != null) androidx.compose.ui.graphics.Color.White else colors.text2,
            textAlign = TextAlign.Center,
            minSize = 8.sp,
        )
        // El borde va encima de la foto para que se vea la elegida.
        if (selected) Box(Modifier.matchParentSize().border(2.dp, colors.ember, ForjaRadius.shapeS))
    }
}

@Composable
private fun WeightSheet(state: BodyState, onSave: (Double, (Boolean) -> Unit) -> Unit, onClose: () -> Unit) {
    val formats = rememberFormats()
    val unitLabel = if (state.unit == WeightUnit.LB) "lb" else "kg"
    val start = state.latest?.let { formats.number(BodyUnits.fromKg(it.weightKg, state.unit), 1) }.orEmpty()
    ValueSheet(
        title = stringResource(Res.string.body_sheet_weight_title),
        unitLabel = unitLabel,
        initial = start,
        onClose = onClose,
    ) { value, done -> onSave(value, done) }
}

@Composable
private fun MeasureSheet(
    unit: WeightUnit,
    preset: MeasureKey?,
    onSave: (BodyMeasure, Double, String?, (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    var kind by remember { mutableStateOf(preset?.kind ?: BodyMeasure.WAIST) }
    var name by remember { mutableStateOf(preset?.customName.orEmpty()) }
    val unitLabel = stringResource(if (unit == WeightUnit.LB) Res.string.body_unit_in else Res.string.body_unit_cm)
    ValueSheet(
        title = stringResource(Res.string.body_measure_sheet_title),
        unitLabel = unitLabel,
        initial = "",
        onClose = onClose,
        saveEnabled = kind != BodyMeasure.CUSTOM || name.isNotBlank(),
        header = {
            if (preset == null) {
                Text(
                    stringResource(Res.string.body_measure_which),
                    Modifier.padding(horizontal = ForjaSpacing.esp5),
                    style = ForjaTheme.typography.label,
                    color = ForjaTheme.colors.text2,
                )
                Column {
                    BodyMeasure.entries.forEach { option ->
                        ForjaRadioRow(MeasureKey(option, null).label(), selected = kind == option) { kind = option }
                    }
                }
            }
            if (kind == BodyMeasure.CUSTOM && preset == null) {
                ForjaTextField(
                    stringResource(Res.string.body_measure_custom_name),
                    name,
                    { name = it.take(com.olarte.ironprogress.domain.body.BodyLimits.CUSTOM_NAME_MAX) },
                    Modifier.padding(horizontal = ForjaSpacing.esp5),
                )
            }
        },
    ) { value, done -> onSave(kind, value, name.takeIf { kind == BodyMeasure.CUSTOM }, done) }
}

/**
 * Hoja para anotar un número: campo grande centrado, aviso si está fuera de rango y Cancelar y
 * Guardar del mismo ancho. Acepta coma o punto.
 */
@Composable
private fun ValueSheet(
    title: String,
    unitLabel: String,
    initial: String,
    onClose: () -> Unit,
    saveEnabled: Boolean = true,
    header: @Composable () -> Unit = {},
    onSave: (Double, (Boolean) -> Unit) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf(false) }
    val value = text.replace(',', '.').toDoubleOrNull()
    ForjaSheet(title, onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            header()
            ForjaTextField(
                stringResource(Res.string.body_value_label, unitLabel),
                text,
                {
                    text = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(6)
                    error = false
                },
                Modifier.padding(horizontal = ForjaSpacing.esp5),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error,
            )
            if (error) {
                ForjaNote(
                    stringResource(Res.string.body_out_of_range),
                    error = true,
                    modifier = Modifier.padding(horizontal = ForjaSpacing.esp5),
                )
            }
            Row(Modifier.padding(horizontal = ForjaSpacing.esp5), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                ForjaButton(stringResource(Res.string.body_cancel), Modifier.weight(1f), ForjaButtonStyle.SECONDARY, onClick = onClose)
                ForjaButton(stringResource(Res.string.body_save), Modifier.weight(1f), enabled = value != null && saveEnabled) {
                    value?.let { number ->
                        onSave(number) { saved -> if (saved) onClose() else error = true }
                    }
                }
            }
        }
    }
}

/** La foto en grande con su fecha y Borrar foto, que pide confirmación. */
@Composable
private fun PhotoSheet(photo: ProgressPhoto, store: PhotoStore?, onDelete: () -> Unit, onClose: () -> Unit) {
    val formats = rememberFormats()
    var confirm by remember { mutableStateOf(false) }
    ForjaSheet(formats.date(photo.takenAt.toEpochMilliseconds()), onClose) {
        Column(Modifier.padding(horizontal = ForjaSpacing.esp5), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            val bitmap = store?.let { rememberPhoto(it, photo.fileName, PHOTO_MAX_SIDE_PX) }
            Box(
                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(ForjaRadius.shapeM).background(ForjaTheme.colors.surface3),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(ForjaIcons.Add, contentDescription = null, tint = ForjaTheme.colors.text3, modifier = Modifier.size(32.dp))
                }
            }
            ForjaButton(stringResource(Res.string.body_photo_delete), Modifier.fillMaxWidth(), ForjaButtonStyle.DESTRUCTIVE) {
                confirm =
                    true
            }
        }
    }
    if (confirm) {
        ForjaDialog(
            title = stringResource(Res.string.body_photo_delete),
            text = stringResource(Res.string.body_photo_delete_confirm),
            onDismiss = { confirm = false },
            confirmText = stringResource(Res.string.body_photo_delete),
            dismissText = stringResource(Res.string.body_cancel),
            destructive = true,
            onConfirm = {
                store?.delete(photo.fileName)
                confirm = false
                onDelete()
            },
        )
    }
}

/** El nombre de una medida: el del tipo, o el propio si es "Otra". */
@Composable
fun MeasureKey.label(): String = customName ?: stringResource(
    when (kind) {
        BodyMeasure.WAIST -> Res.string.body_measure_waist
        BodyMeasure.CHEST -> Res.string.body_measure_chest
        BodyMeasure.ARM -> Res.string.body_measure_arm
        BodyMeasure.THIGH -> Res.string.body_measure_thigh
        BodyMeasure.HIPS -> Res.string.body_measure_hips
        BodyMeasure.CUSTOM -> Res.string.body_measure_custom
    },
)

private fun length(formats: Formats, cm: Double, unit: WeightUnit): String =
    formats.number(BodyUnits.fromCm(cm, unit), 1) + if (unit == WeightUnit.LB) " in" else " cm"

private fun signedLength(formats: Formats, cm: Double, unit: WeightUnit): String = when {
    cm > 0 -> "+" + length(formats, cm, unit)
    cm < 0 -> "−" + length(formats, abs(cm), unit)
    else -> length(formats, 0.0, unit)
}

private const val RECENT = 3
private const val PHOTO_COLUMNS = 3
private const val MAX_THUMBNAILS = 9
private const val THUMBNAIL_PX = 480
