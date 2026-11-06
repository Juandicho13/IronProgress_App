package com.olarte.ironprogress.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.olarte.ironprogress.domain.settings.AppLanguage
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaListGroup
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaRadioRow
import com.olarte.ironprogress.ui.components.ForjaSectionLabel
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.components.ForjaSwitchRow
import com.olarte.ironprogress.ui.components.ForjaTopBar
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.backup_section
import com.olarte.ironprogress.ui.resources.lang_en
import com.olarte.ironprogress.ui.resources.lang_es
import com.olarte.ironprogress.ui.resources.lang_system
import com.olarte.ironprogress.ui.resources.settings_back
import com.olarte.ironprogress.ui.resources.settings_backup_hint
import com.olarte.ironprogress.ui.resources.settings_backup_row
import com.olarte.ironprogress.ui.resources.settings_hide
import com.olarte.ironprogress.ui.resources.settings_hide_hint
import com.olarte.ironprogress.ui.resources.settings_language
import com.olarte.ironprogress.ui.resources.settings_pro
import com.olarte.ironprogress.ui.resources.settings_pro_hint
import com.olarte.ironprogress.ui.resources.settings_section_app
import com.olarte.ironprogress.ui.resources.settings_section_feedback
import com.olarte.ironprogress.ui.resources.settings_section_privacy
import com.olarte.ironprogress.ui.resources.settings_section_training
import com.olarte.ironprogress.ui.resources.settings_sound
import com.olarte.ironprogress.ui.resources.settings_theme
import com.olarte.ironprogress.ui.resources.settings_title
import com.olarte.ironprogress.ui.resources.settings_vibration
import com.olarte.ironprogress.ui.resources.settings_voice
import com.olarte.ironprogress.ui.resources.settings_voice_hint
import com.olarte.ironprogress.ui.resources.theme_dark
import com.olarte.ironprogress.ui.resources.theme_light
import com.olarte.ironprogress.ui.resources.theme_system
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Ajustes conectados a los vigentes de [SettingsGate]. */
@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenBackup: () -> Unit = {}) {
    SettingsScreen(LocalAppSettings.current, LocalSettingsIntents.current, onBack, onOpenBackup)
}

private enum class SettingsSheet { LANGUAGE, THEME }

/**
 * Ajustes (CFG-02 y CFG-03) en filas agrupadas: Idioma y Tema muestran su valor y abren una hoja con
 * las opciones; sonido, vibración, voz y ocultar son interruptores; la copia de seguridad abre su
 * pantalla. Todo se guarda al tocar.
 */
@Composable
fun SettingsScreen(settings: AppSettings, onIntent: (SettingsIntent) -> Unit, onBack: () -> Unit, onOpenBackup: () -> Unit = {}) {
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    Column(Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding()) {
        ForjaTopBar(stringResource(Res.string.settings_title), onBack, stringResource(Res.string.settings_back))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            ForjaSectionLabel(stringResource(Res.string.settings_section_app))
            ForjaListGroup {
                ForjaListRow(
                    stringResource(Res.string.settings_language),
                    value = settings.language.label(),
                    onClick = { sheet = SettingsSheet.LANGUAGE },
                )
                ForjaDivider()
                ForjaListRow(
                    stringResource(Res.string.settings_theme),
                    value = settings.theme.label(),
                    onClick = { sheet = SettingsSheet.THEME },
                )
            }

            ForjaSectionLabel(stringResource(Res.string.settings_section_training))
            ForjaListGroup {
                ForjaSwitchRow(
                    stringResource(Res.string.settings_pro),
                    settings.proMode,
                    { onIntent(SettingsIntent.ProMode(it)) },
                    subtitle = stringResource(Res.string.settings_pro_hint),
                )
            }

            ForjaSectionLabel(stringResource(Res.string.settings_section_feedback))
            ForjaListGroup {
                ForjaSwitchRow(stringResource(Res.string.settings_sound), settings.sound, { onIntent(SettingsIntent.Sound(it)) })
                ForjaDivider()
                ForjaSwitchRow(stringResource(Res.string.settings_vibration), settings.vibration, {
                    onIntent(SettingsIntent.Vibration(it))
                })
                ForjaDivider()
                ForjaSwitchRow(
                    stringResource(Res.string.settings_voice),
                    settings.voice,
                    { onIntent(SettingsIntent.Voice(it)) },
                    subtitle = stringResource(Res.string.settings_voice_hint),
                )
            }

            ForjaSectionLabel(stringResource(Res.string.settings_section_privacy))
            ForjaListGroup {
                ForjaSwitchRow(
                    stringResource(Res.string.settings_hide),
                    settings.hideBodyMetrics,
                    { onIntent(SettingsIntent.HideBodyMetrics(it)) },
                    subtitle = stringResource(Res.string.settings_hide_hint),
                )
            }

            ForjaSectionLabel(stringResource(Res.string.backup_section))
            ForjaListGroup {
                ForjaListRow(
                    stringResource(Res.string.settings_backup_row),
                    subtitle = stringResource(Res.string.settings_backup_hint),
                    onClick = onOpenBackup,
                )
            }
            Spacer(Modifier.height(ForjaSpacing.esp8))
        }
    }

    when (sheet) {
        SettingsSheet.LANGUAGE -> ForjaSheet(stringResource(Res.string.settings_language), onDismiss = { sheet = null }) {
            AppLanguage.entries.forEachIndexed { index, language ->
                if (index > 0) ForjaDivider()
                ForjaRadioRow(language.label(), language == settings.language) {
                    sheet = null
                    onIntent(SettingsIntent.Language(language))
                }
            }
        }
        SettingsSheet.THEME -> ForjaSheet(stringResource(Res.string.settings_theme), onDismiss = { sheet = null }) {
            ThemeMode.entries.forEachIndexed { index, theme ->
                if (index > 0) ForjaDivider()
                ForjaRadioRow(theme.label(), theme == settings.theme) {
                    sheet = null
                    onIntent(SettingsIntent.Theme(theme))
                }
            }
        }
        null -> Unit
    }
}

@Composable
private fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> Res.string.lang_system
        AppLanguage.SPANISH -> Res.string.lang_es
        AppLanguage.ENGLISH -> Res.string.lang_en
    },
)

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> Res.string.theme_system
        ThemeMode.DARK -> Res.string.theme_dark
        ThemeMode.LIGHT -> Res.string.theme_light
    },
)
