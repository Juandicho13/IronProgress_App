package com.olarte.ironprogress.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.olarte.ironprogress.domain.settings.AppLanguage
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.ui.backup.BackupSection
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.lang_en
import com.olarte.ironprogress.ui.resources.lang_es
import com.olarte.ironprogress.ui.resources.lang_system
import com.olarte.ironprogress.ui.resources.settings_back
import com.olarte.ironprogress.ui.resources.settings_hide
import com.olarte.ironprogress.ui.resources.settings_hide_hint
import com.olarte.ironprogress.ui.resources.settings_language
import com.olarte.ironprogress.ui.resources.settings_section_app
import com.olarte.ironprogress.ui.resources.settings_section_feedback
import com.olarte.ironprogress.ui.resources.settings_section_privacy
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

/** Ajustes conectados a los vigentes de [SettingsGate]. [extra] agrega secciones de otros tickets. */
@Composable
fun SettingsRoute(onBack: () -> Unit, extra: @Composable ColumnScope.() -> Unit = { BackupSection() }) {
    SettingsScreen(LocalAppSettings.current, LocalSettingsIntents.current, onBack, extra)
}

/** Ajustes (CFG-02 y CFG-03): idioma, tema, sonido, vibración, voz y ocultar peso corporal y calorías. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        TopBar(stringResource(Res.string.settings_title), ForjaMoreIcons.ChevronLeft, stringResource(Res.string.settings_back), onBack)

        SectionLabel(stringResource(Res.string.settings_section_app))
        SettingsCard {
            Text(stringResource(Res.string.settings_language), style = ForjaTheme.typography.body, color = ForjaTheme.colors.text1)
            ChoiceChips(AppLanguage.entries, { it == settings.language }, { it.label() }) { onIntent(SettingsIntent.Language(it)) }
            Text(stringResource(Res.string.settings_theme), style = ForjaTheme.typography.body, color = ForjaTheme.colors.text1)
            ChoiceChips(ThemeMode.entries, { it == settings.theme }, { it.label() }) { onIntent(SettingsIntent.Theme(it)) }
        }

        SectionLabel(stringResource(Res.string.settings_section_feedback))
        SettingsCard {
            ToggleRow(stringResource(Res.string.settings_sound), null, settings.sound) { onIntent(SettingsIntent.Sound(it)) }
            ToggleRow(stringResource(Res.string.settings_vibration), null, settings.vibration) { onIntent(SettingsIntent.Vibration(it)) }
            ToggleRow(stringResource(Res.string.settings_voice), stringResource(Res.string.settings_voice_hint), settings.voice) {
                onIntent(SettingsIntent.Voice(it))
            }
        }

        SectionLabel(stringResource(Res.string.settings_section_privacy))
        SettingsCard {
            ToggleRow(stringResource(Res.string.settings_hide), stringResource(Res.string.settings_hide_hint), settings.hideBodyMetrics) {
                onIntent(SettingsIntent.HideBodyMetrics(it))
            }
        }
        extra()
        Spacer(Modifier.height(ForjaSpacing.esp8))
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
