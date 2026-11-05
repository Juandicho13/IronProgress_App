package com.olarte.ironprogress.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.settings.ExperienceLevel
import com.olarte.ironprogress.domain.settings.TrainingGoal
import com.olarte.ironprogress.domain.settings.TrainingProfile
import com.olarte.ironprogress.domain.settings.WeightUnit
import com.olarte.ironprogress.ui.catalog.editorLabel
import com.olarte.ironprogress.ui.interval.StepButton
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.exp_advanced
import com.olarte.ironprogress.ui.resources.exp_beginner
import com.olarte.ironprogress.ui.resources.exp_intermediate
import com.olarte.ironprogress.ui.resources.goal_endurance
import com.olarte.ironprogress.ui.resources.goal_health
import com.olarte.ironprogress.ui.resources.goal_muscle
import com.olarte.ironprogress.ui.resources.goal_strength
import com.olarte.ironprogress.ui.resources.goal_weight
import com.olarte.ironprogress.ui.resources.onb_days
import com.olarte.ironprogress.ui.resources.onb_days_value
import com.olarte.ironprogress.ui.resources.onb_disciplines
import com.olarte.ironprogress.ui.resources.onb_disciplines_hint
import com.olarte.ironprogress.ui.resources.onb_experience
import com.olarte.ironprogress.ui.resources.onb_goal
import com.olarte.ironprogress.ui.resources.onb_start
import com.olarte.ironprogress.ui.resources.onb_subtitle
import com.olarte.ironprogress.ui.resources.onb_title
import com.olarte.ironprogress.ui.resources.onb_units
import com.olarte.ironprogress.ui.resources.unit_kg
import com.olarte.ironprogress.ui.resources.unit_lb
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Disciplinas que se ofrecen en la bienvenida, en el orden de la pantalla de disciplinas (7.1). */
internal val OnboardingDisciplines = listOf(
    Discipline.STRENGTH,
    Discipline.FUNCTIONAL,
    Discipline.HIIT,
    Discipline.CALISTHENICS,
    Discipline.YOGA,
    Discipline.PILATES,
    Discipline.MOBILITY,
    Discipline.BREATHING,
    Discipline.CARDIO,
    Discipline.COMBAT,
)

/** Bienvenida (CFG-01): una sola página con las cinco preguntas y Empezar. Sale solo la primera vez. */
@Composable
fun OnboardingScreen(onFinish: (TrainingProfile) -> Unit) {
    val colors = ForjaTheme.colors
    var goal by rememberSaveable { mutableStateOf(TrainingGoal.STRENGTH) }
    var experience by rememberSaveable { mutableStateOf(ExperienceLevel.BEGINNER) }
    var unit by rememberSaveable { mutableStateOf(WeightUnit.KG) }
    var days by rememberSaveable { mutableStateOf(3) }
    var disciplines by rememberSaveable { mutableStateOf(listOf(Discipline.STRENGTH)) }

    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp6),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        Text(
            stringResource(Res.string.onb_title),
            Modifier.semantics { heading() },
            style = ForjaTheme.typography.title1,
            color = colors.text1,
        )
        Text(stringResource(Res.string.onb_subtitle), style = ForjaTheme.typography.body, color = colors.text2)

        SettingsCard {
            SectionLabel(stringResource(Res.string.onb_goal))
            ChoiceChips(TrainingGoal.entries, { it == goal }, { it.label() }) { goal = it }
        }
        SettingsCard {
            SectionLabel(stringResource(Res.string.onb_experience))
            ChoiceChips(ExperienceLevel.entries, { it == experience }, { it.label() }) { experience = it }
        }
        SettingsCard {
            SectionLabel(stringResource(Res.string.onb_units))
            ChoiceChips(WeightUnit.entries, { it == unit }, { it.label() }) { unit = it }
        }
        SettingsCard {
            SectionLabel(stringResource(Res.string.onb_days))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
                StepButton("−", days > TrainingProfile.DAYS_RANGE.first) { days-- }
                Text(stringResource(Res.string.onb_days_value, days), style = ForjaTheme.typography.largeFigure, color = colors.text1)
                StepButton("+", days < TrainingProfile.DAYS_RANGE.last) { days++ }
            }
        }
        SettingsCard {
            SectionLabel(stringResource(Res.string.onb_disciplines))
            Text(stringResource(Res.string.onb_disciplines_hint), style = ForjaTheme.typography.caption, color = colors.text2)
            ChoiceChips(OnboardingDisciplines, { it in disciplines }, { it.editorLabel() }) { picked ->
                disciplines = if (picked in disciplines) disciplines - picked else disciplines + picked
            }
        }
        Spacer(Modifier.height(ForjaSpacing.esp2))
        PrimaryButton(stringResource(Res.string.onb_start), enabled = disciplines.isNotEmpty()) {
            onFinish(TrainingProfile(goal, experience, unit, days, disciplines.toSet()))
        }
    }
}

@Composable
internal fun TrainingGoal.label(): String = stringResource(
    when (this) {
        TrainingGoal.STRENGTH -> Res.string.goal_strength
        TrainingGoal.MUSCLE -> Res.string.goal_muscle
        TrainingGoal.ENDURANCE -> Res.string.goal_endurance
        TrainingGoal.HEALTH -> Res.string.goal_health
        TrainingGoal.WEIGHT -> Res.string.goal_weight
    },
)

@Composable
internal fun ExperienceLevel.label(): String = stringResource(
    when (this) {
        ExperienceLevel.BEGINNER -> Res.string.exp_beginner
        ExperienceLevel.INTERMEDIATE -> Res.string.exp_intermediate
        ExperienceLevel.ADVANCED -> Res.string.exp_advanced
    },
)

@Composable
internal fun WeightUnit.label(): String = stringResource(if (this == WeightUnit.KG) Res.string.unit_kg else Res.string.unit_lb)
