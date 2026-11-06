package com.olarte.ironprogress.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme

/*
 * Piezas de Forja para las pantallas de formulario y lista (PANTALLAS 0): un botón principal por
 * pantalla, secundarios en surface3, tonales en emberVeil, de texto en emberInk; barra superior con
 * botones redondos de 48 y título centrado; tarjetas surface1; filas con chevron; diálogos y hojas
 * con los colores de Forja y no los de Material.
 */

enum class ForjaButtonStyle { PRIMARY, SECONDARY, TONAL, TEXT, DESTRUCTIVE }

@Composable
fun ForjaButton(
    text: String,
    modifier: Modifier = Modifier,
    style: ForjaButtonStyle = ForjaButtonStyle.PRIMARY,
    enabled: Boolean = true,
    icon: Painter? = null,
    onClick: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.97f else 1f,
        ForjaTheme.motion.spatialFast(),
        label = "ForjaButtonScale",
    )
    val (background, content) = when {
        !enabled && style == ForjaButtonStyle.TEXT -> Color.Transparent to colors.text3
        !enabled -> colors.surface3 to colors.text3
        style == ForjaButtonStyle.PRIMARY -> (if (pressed) colors.emberPressed else colors.ember) to colors.onEmber
        style == ForjaButtonStyle.SECONDARY -> colors.surface3 to colors.text1
        style == ForjaButtonStyle.TONAL -> colors.emberVeil to colors.emberInk
        style == ForjaButtonStyle.DESTRUCTIVE -> colors.danger to colors.onDanger
        else -> Color.Transparent to colors.emberInk
    }
    val height = if (style == ForjaButtonStyle.PRIMARY) ForjaMeasures.primaryButtonHeight else ForjaMeasures.secondaryButtonHeight
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = height)
            .widthIn(min = ForjaMeasures.minTouch)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (style == ForjaButtonStyle.TEXT) ForjaSpacing.esp3 else ForjaSpacing.esp5),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
                Spacer(Modifier.width(ForjaSpacing.esp2))
            }
            Text(
                text,
                style = if (style ==
                    ForjaButtonStyle.PRIMARY
                ) {
                    ForjaTheme.typography.primaryButton
                } else {
                    ForjaTheme.typography.secondaryButton
                },
                color = content,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Botón redondo de 48 con ícono de 24 (barra superior, cerrar, subir y bajar). */
@Composable
fun ForjaIconButton(
    icon: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    tint: Color = ForjaTheme.colors.text1,
    rotation: Float = 0f,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(ForjaMeasures.minTouch)
            .clip(CircleShape)
            .background(if (filled) ForjaTheme.colors.surface3 else Color.Transparent)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(ForjaMeasures.iconSize).rotate(rotation))
    }
}

/** Barra superior de las pantallas que no son pestañas: atrás a la izquierda, título centrado, acciones a la derecha. */
@Composable
fun ForjaTopBar(
    title: String,
    onBack: (() -> Unit)?,
    backDescription: String,
    modifier: Modifier = Modifier,
    backIcon: Painter = ForjaMoreIcons.ChevronLeft,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = ForjaMeasures.minTouch + ForjaSpacing.esp2).padding(horizontal = ForjaSpacing.esp3),
    ) {
        if (onBack != null) {
            ForjaIconButton(backIcon, backDescription, Modifier.align(Alignment.CenterStart), onClick = onBack)
        }
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = ForjaMeasures.minTouch * 2 + ForjaSpacing.esp2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                Modifier.semantics { heading() },
                // Dos líneas antes de cortar: un nombre largo de rutina se lee entero.
                style = if (title.length > TITLE_LONG) longTitleStyle() else ForjaTheme.typography.title3,
                color = ForjaTheme.colors.text1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(subtitle, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2, maxLines = 1)
            }
        }
        Row(
            Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** Título grande de pestaña (Hoy, Plan, Progreso) con una acción redonda a la derecha. */
@Composable
fun ForjaLargeTitle(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, trailing: @Composable () -> Unit = {}) {
    // Cabecera común de Hoy, Plan, Progreso y Ejercicios: la línea de arriba (fecha o vacía) y la
    // acción de la derecha siempre ocupan su lugar, así el título queda a la misma altura en todas.
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            Text(
                eyebrow.orEmpty(),
                style = ForjaTheme.typography.caption,
                color = ForjaTheme.colors.text2,
                maxLines = 1,
                modifier = if (eyebrow == null) Modifier.clearAndSetSemantics {} else Modifier,
            )
            Text(
                title,
                Modifier.semantics { heading() },
                style = ForjaTheme.typography.title1,
                color = ForjaTheme.colors.text1,
                maxLines = 1,
            )
        }
        Box(Modifier.heightIn(min = ForjaMeasures.minTouch), contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

/** Espacio entre la barra de estado y la cabecera de las pestañas: el mismo en las cuatro. */
val ForjaHeaderTop = ForjaSpacing.esp3

/**
 * Pantalla completa de Forja: fondo opaco, barra superior y una lista con márgenes de 20 y
 * separaciones de 12. El fondo opaco evita que se vea la pantalla de abajo en las transiciones.
 */
@Composable
fun ForjaScreen(
    title: String,
    onBack: (() -> Unit)?,
    backDescription: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding().imePadding().clearFocusOnTap().then(modifier),
    ) {
        ForjaTopBar(title, onBack, backDescription, subtitle = subtitle, actions = actions)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(
                start = ForjaSpacing.esp5,
                end = ForjaSpacing.esp5,
                top = ForjaSpacing.esp3,
                bottom = ForjaSpacing.esp8,
            ),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            content = content,
        )
        if (bottomBar != null) {
            Column(
                Modifier.fillMaxWidth().background(ForjaTheme.colors.background).navigationBarsPadding()
                    .padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                content = bottomBar,
            )
        }
    }
}

/** Tarjeta surface1 de radio 28 y relleno 16 (PANTALLAS 0). */
@Composable
fun ForjaCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(ForjaSpacing.esp4),
    spacing: androidx.compose.ui.unit.Dp = ForjaSpacing.esp3,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(ForjaTheme.colors.surface1).padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Etiqueta de sección: caption en mayúsculas y text2. */
@Composable
fun ForjaSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier.padding(start = ForjaSpacing.esp1, top = ForjaSpacing.esp2),
        style = ForjaTheme.typography.caption,
        color = ForjaTheme.colors.text2,
    )
}

/** Grupo de filas en una tarjeta, separadas por líneas finas. */
@Composable
fun ForjaListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(ForjaTheme.colors.surface1), content = content)
}

@Composable
fun ForjaDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = ForjaSpacing.esp4).height(1.dp).background(ForjaTheme.colors.line))
}

/**
 * Fila de lista de 56 o más: ícono opcional, título y subtítulo a la izquierda, valor y chevron a la
 * derecha. Si tiene onClick es navegación y lleva chevron.
 */
@Composable
fun ForjaListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    leading: Painter? = null,
    leadingTint: Color = ForjaTheme.colors.emberInk,
    titleColor: Color = ForjaTheme.colors.text1,
    chevron: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = ForjaTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ForjaMeasures.setRowHeight)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = ForjaSpacing.esp4, vertical = ForjaSpacing.esp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        if (leading != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(colors.emberVeil), contentAlignment = Alignment.Center) {
                Icon(leading, contentDescription = null, tint = leadingTint, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ForjaTheme.typography.body, color = titleColor)
            if (subtitle != null) Text(subtitle, style = ForjaTheme.typography.body2, color = colors.text2)
        }
        if (value != null) Text(value, style = ForjaTheme.typography.body2, color = colors.text2, textAlign = TextAlign.End)
        trailing?.invoke()
        if (onClick != null && chevron) {
            Icon(
                ForjaIcons.ChevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(ForjaMeasures.inlineIconSize),
            )
        }
    }
}

/** Fila con interruptor; toda la fila lo cambia. */
@Composable
fun ForjaSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    ForjaListRow(
        title = title,
        subtitle = subtitle,
        chevron = false,
        onClick = { onChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ForjaTheme.colors.onEmber,
                    checkedTrackColor = ForjaTheme.colors.ember,
                    checkedBorderColor = ForjaTheme.colors.ember,
                    uncheckedThumbColor = ForjaTheme.colors.controlLine,
                    uncheckedTrackColor = ForjaTheme.colors.surface3,
                    uncheckedBorderColor = ForjaTheme.colors.controlLine,
                ),
            )
        },
    )
}

/** Fila de opción única con la marca de check en brasa. */
@Composable
fun ForjaRadioRow(title: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    ForjaListRow(
        title = title,
        subtitle = subtitle,
        chevron = false,
        onClick = onClick,
        trailing = {
            Box(Modifier.size(ForjaMeasures.iconSize), contentAlignment = Alignment.Center) {
                if (selected) Icon(ForjaIcons.Check, contentDescription = null, tint = ForjaTheme.colors.emberInk)
            }
        },
    )
}

/** Campo de texto de Forja: borde controlLine, brasa al enfocar, texto text1 en los dos temas. */
@Composable
fun ForjaTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
) {
    val colors = ForjaTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = ForjaTheme.typography.body2) },
        textStyle = ForjaTheme.typography.body,
        shape = ForjaRadius.shapeS,
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = keyboardOptions.withDefaultImeAction(singleLine),
        keyboardActions = dismissingKeyboardActions(),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text1,
            unfocusedTextColor = colors.text1,
            disabledTextColor = colors.text3,
            focusedContainerColor = colors.background,
            unfocusedContainerColor = colors.background,
            disabledContainerColor = colors.background,
            cursorColor = colors.ember,
            focusedBorderColor = colors.ember,
            unfocusedBorderColor = colors.line,
            disabledBorderColor = colors.line,
            focusedLabelColor = colors.emberInk,
            unfocusedLabelColor = colors.text2,
            errorBorderColor = colors.danger,
            errorLabelColor = colors.danger,
        ),
    )
}

/**
 * Diálogo de Forja: tarjeta surface1, título, texto y los botones en una fila del mismo ancho.
 * La acción que borra o descarta va en danger.
 */
@Composable
fun ForjaDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    text: String? = null,
    dismissText: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(ForjaTheme.colors.surface1).clearFocusOnTap()
                .padding(ForjaSpacing.esp5),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            Text(title, Modifier.semantics { heading() }, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1)
            if (text != null) Text(text, style = ForjaTheme.typography.body2, color = ForjaTheme.colors.text2)
            content?.invoke(this)
            Spacer(Modifier.height(ForjaSpacing.esp1))
            Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                if (dismissText != null) {
                    ForjaButton(dismissText, Modifier.weight(1f), ForjaButtonStyle.SECONDARY, onClick = onDismiss)
                }
                ForjaButton(
                    confirmText,
                    Modifier.weight(1f),
                    if (destructive) ForjaButtonStyle.DESTRUCTIVE else ForjaButtonStyle.TONAL,
                    enabled = confirmEnabled,
                    onClick = onConfirm,
                )
            }
        }
    }
}

/** Hoja inferior de Forja: surface1, asa en controlLine y título. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForjaSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = ForjaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface1,
        contentColor = colors.text1,
        scrimColor = colors.modalScrim,
        shape = ForjaRadius.shapeXlTop,
        dragHandle = {
            Box(
                Modifier.padding(vertical = ForjaSpacing.esp3).size(width = 36.dp, height = 5.dp).clip(CircleShape)
                    .background(colors.controlLine),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().clearFocusOnTap().navigationBarsPadding().imePadding().padding(bottom = ForjaSpacing.esp4)) {
            Text(
                title,
                Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5).padding(bottom = ForjaSpacing.esp3).semantics { heading() },
                style = ForjaTheme.typography.title3,
                color = colors.text1,
            )
            content()
        }
    }
}

/** Estado vacío centrado: título, texto y, si hay, una acción. */
@Composable
fun ForjaEmptyState(title: String, body: String? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    ForjaCard(modifier, padding = PaddingValues(ForjaSpacing.esp6)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            Text(title, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1, textAlign = TextAlign.Center)
            if (body != null) Text(body, style = ForjaTheme.typography.body2, color = ForjaTheme.colors.text2, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(ForjaSpacing.esp2))
                action()
            }
        }
    }
}

/** Texto de estado (cargando, aviso) en text2, o danger si es un error. */
@Composable
fun ForjaNote(text: String, error: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = ForjaTheme.typography.body2,
        color = if (error) ForjaTheme.colors.danger else ForjaTheme.colors.text2,
    )
}

private const val TITLE_LONG = 18

@Composable
private fun longTitleStyle() = ForjaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
