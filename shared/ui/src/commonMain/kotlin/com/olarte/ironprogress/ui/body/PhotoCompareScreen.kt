package com.olarte.ironprogress.ui.body

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.body.ProgressPhoto
import com.olarte.ironprogress.ui.components.ForjaTopBar
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.body_compare_after
import com.olarte.ironprogress.ui.resources.body_compare_before
import com.olarte.ironprogress.ui.resources.body_compare_days
import com.olarte.ironprogress.ui.resources.body_compare_pick_after
import com.olarte.ironprogress.ui.resources.body_compare_pick_before
import com.olarte.ironprogress.ui.resources.body_compare_title
import com.olarte.ironprogress.ui.resources.editor_back
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PhotoCompareRoute(onBack: () -> Unit, model: BodyViewModel = koinViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    PhotoCompareScreen(state.photos, rememberPhotoStore {}, onBack)
}

private enum class Side { BEFORE, AFTER }

/**
 * Antes y después (F2-09): dos fotos lado a lado con su fecha y los días entre ellas. Por defecto
 * la primera y la última; se toca un lado y se elige otra foto en la tira de abajo.
 */
@Composable
fun PhotoCompareScreen(photos: List<ProgressPhoto>, store: PhotoStore?, onBack: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    var beforeId by remember(photos.firstOrNull()?.id) { mutableStateOf(photos.firstOrNull()?.id) }
    var afterId by remember(photos.lastOrNull()?.id) { mutableStateOf(photos.lastOrNull()?.id) }
    var side by remember { mutableStateOf(Side.BEFORE) }
    val before = photos.firstOrNull { it.id == beforeId }
    val after = photos.firstOrNull { it.id == afterId }

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        ForjaTopBar(stringResource(Res.string.body_compare_title), onBack, stringResource(Res.string.editor_back))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            ComparePane(stringResource(Res.string.body_compare_before), before, store, side == Side.BEFORE, Modifier.weight(1f)) {
                side =
                    Side.BEFORE
            }
            ComparePane(stringResource(Res.string.body_compare_after), after, store, side == Side.AFTER, Modifier.weight(1f)) {
                side =
                    Side.AFTER
            }
        }
        if (before != null && after != null) {
            val days = ((after.takenAt - before.takenAt).inWholeDays).let { if (it < 0) -it else it }.toInt()
            Text(
                pluralStringResource(Res.plurals.body_compare_days, days, days),
                Modifier.fillMaxWidth(),
                style = typography.body2,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(if (side == Side.BEFORE) Res.string.body_compare_pick_before else Res.string.body_compare_pick_after),
            Modifier.fillMaxWidth().padding(top = ForjaSpacing.esp5, bottom = ForjaSpacing.esp2),
            style = typography.label,
            color = colors.text2,
            textAlign = TextAlign.Center,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = ForjaSpacing.esp5),
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            items(photos, key = { it.id.value }) { photo ->
                val chosen = photo.id == if (side == Side.BEFORE) beforeId else afterId
                PhotoThumbnail(photo, store, Modifier.width(104.dp), selected = chosen) {
                    if (side == Side.BEFORE) beforeId = photo.id else afterId = photo.id
                }
            }
        }
    }
}

@Composable
private fun ComparePane(
    label: String,
    photo: ProgressPhoto?,
    store: PhotoStore?,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    Column(
        modifier.semantics { selected = active }.clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Text(label, style = typography.label, color = if (active) colors.emberInk else colors.text2)
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(ForjaRadius.shapeM).background(colors.surface3)
                .border(BorderStroke(if (active) 2.dp else 0.dp, if (active) colors.ember else colors.surface3), ForjaRadius.shapeM),
        ) {
            val bitmap = photo?.let { item -> store?.let { rememberPhoto(it, item.fileName, PHOTO_MAX_SIDE_PX) } }
            if (bitmap !=
                null
            ) {
                Image(bitmap, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            }
        }
        Text(
            photo?.let {
                formats.date(it.takenAt.toEpochMilliseconds())
            }.orEmpty(),
            style = typography.caption,
            color = colors.text2,
            maxLines = 1,
        )
    }
}
