package com.olarte.ironprogress.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.settings.ExperienceLevel
import com.olarte.ironprogress.domain.settings.TrainingGoal
import com.olarte.ironprogress.domain.settings.TrainingProfile
import com.olarte.ironprogress.domain.settings.WeightUnit
import com.olarte.ironprogress.domain.settings.WorkoutAudio
import com.olarte.ironprogress.ui.catalog.editorLabel
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.interval.StepButton
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.exp_advanced
import com.olarte.ironprogress.ui.resources.exp_advanced_hint
import com.olarte.ironprogress.ui.resources.exp_beginner
import com.olarte.ironprogress.ui.resources.exp_beginner_hint
import com.olarte.ironprogress.ui.resources.exp_intermediate
import com.olarte.ironprogress.ui.resources.exp_intermediate_hint
import com.olarte.ironprogress.ui.resources.goal_endurance
import com.olarte.ironprogress.ui.resources.goal_endurance_hint
import com.olarte.ironprogress.ui.resources.goal_health
import com.olarte.ironprogress.ui.resources.goal_health_hint
import com.olarte.ironprogress.ui.resources.goal_muscle
import com.olarte.ironprogress.ui.resources.goal_muscle_hint
import com.olarte.ironprogress.ui.resources.goal_strength
import com.olarte.ironprogress.ui.resources.goal_strength_hint
import com.olarte.ironprogress.ui.resources.goal_weight
import com.olarte.ironprogress.ui.resources.goal_weight_hint
import com.olarte.ironprogress.ui.resources.onb2_audio_sub
import com.olarte.ironprogress.ui.resources.onb2_audio_title
import com.olarte.ironprogress.ui.resources.onb2_back
import com.olarte.ironprogress.ui.resources.onb2_continue
import com.olarte.ironprogress.ui.resources.onb2_create_routine
import com.olarte.ironprogress.ui.resources.onb2_days_caption
import com.olarte.ironprogress.ui.resources.onb2_days_per_week
import com.olarte.ironprogress.ui.resources.onb2_days_sub
import com.olarte.ironprogress.ui.resources.onb2_days_title
import com.olarte.ironprogress.ui.resources.onb2_disc_sub
import com.olarte.ironprogress.ui.resources.onb2_disc_title
import com.olarte.ironprogress.ui.resources.onb2_done_demo
import com.olarte.ironprogress.ui.resources.onb2_done_sub
import com.olarte.ironprogress.ui.resources.onb2_done_title
import com.olarte.ironprogress.ui.resources.onb2_exp_sub
import com.olarte.ironprogress.ui.resources.onb2_exp_title
import com.olarte.ironprogress.ui.resources.onb2_go_today
import com.olarte.ironprogress.ui.resources.onb2_goal_sub
import com.olarte.ironprogress.ui.resources.onb2_goal_title
import com.olarte.ironprogress.ui.resources.onb2_mode_basic
import com.olarte.ironprogress.ui.resources.onb2_mode_basic_hint
import com.olarte.ironprogress.ui.resources.onb2_mode_pro
import com.olarte.ironprogress.ui.resources.onb2_mode_pro_hint
import com.olarte.ironprogress.ui.resources.onb2_mode_sub
import com.olarte.ironprogress.ui.resources.onb2_mode_title
import com.olarte.ironprogress.ui.resources.onb2_recommended
import com.olarte.ironprogress.ui.resources.onb2_skip
import com.olarte.ironprogress.ui.resources.onb2_summary_audio
import com.olarte.ironprogress.ui.resources.onb2_summary_exp
import com.olarte.ironprogress.ui.resources.onb2_summary_goal
import com.olarte.ironprogress.ui.resources.onb2_summary_mode
import com.olarte.ironprogress.ui.resources.onb2_summary_unit
import com.olarte.ironprogress.ui.resources.onb2_units_sub
import com.olarte.ironprogress.ui.resources.onb2_units_title
import com.olarte.ironprogress.ui.resources.onb2_welcome_sub
import com.olarte.ironprogress.ui.resources.onb2_welcome_title
import com.olarte.ironprogress.ui.resources.onb_start
import com.olarte.ironprogress.ui.resources.unit_kg
import com.olarte.ironprogress.ui.resources.unit_kg_hint
import com.olarte.ironprogress.ui.resources.unit_lb
import com.olarte.ironprogress.ui.resources.unit_lb_hint
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
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

/**
 * Bienvenida (CFG-01) paso a paso: portada, una pregunta por pantalla con barra de progreso, atrás
 * y Omitir donde es opcional, tarjetas grandes para elegir y Continuar abajo, y al final un resumen
 * con "Crear mi primera rutina" o "Ir a Hoy". Sale solo la primera vez.
 */
@Composable
fun OnboardingScreen(initial: OnboardingFlow = OnboardingFlow(), onFinish: (OnboardingResult) -> Unit) {
    var flow by remember { mutableStateOf(initial) }
    val colors = ForjaTheme.colors
    val reduceMotion = ForjaTheme.motion.reduceMotion
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        AnimatedContent(
            targetState = flow.step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                if (reduceMotion) {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                } else {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(280)) { it / 6 * dir } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(220)) { -it / 6 * dir } + fadeOut(tween(200)))
                }
            },
            label = "onboarding",
        ) { step ->
            // Cada paso dibuja con el estado vigente; el paso animado es el de la transición.
            val shown = flow.copy(step = step)
            when (step) {
                OnboardingStep.WELCOME -> Welcome { flow = flow.next() }
                OnboardingStep.DONE -> Done(shown, onBack = { flow = flow.back() }) { onFinish(flow.result(it)) }
                else -> Question(shown, { flow = flow.back() }, { flow = flow.skip() }, { flow = flow.next() }) {
                    StepBody(shown) { flow = it }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onStart: () -> Unit) {
    val colors = ForjaTheme.colors
    Column(Modifier.fillMaxSize().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3)) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(112.dp).clip(CircleShape).background(colors.emberVeil), contentAlignment = Alignment.Center) {
                Icon(ForjaIcons.FireFilled, contentDescription = null, tint = colors.ember, modifier = Modifier.size(64.dp))
            }
            Text(
                stringResource(Res.string.onb2_welcome_title),
                Modifier.semantics { heading() },
                style = ForjaTheme.typography.title1.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = colors.text1,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.onb2_welcome_sub),
                Modifier.widthIn(max = 340.dp),
                style = ForjaTheme.typography.body,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        }
        ForjaButton(stringResource(Res.string.onb_start), Modifier.fillMaxWidth(), ForjaButtonStyle.PRIMARY, onClick = onStart)
    }
}

/** Marco de una pregunta: atrás, barra fina, Omitir, título, cuerpo con scroll y Continuar fijo abajo. */
@Composable
private fun Question(flow: OnboardingFlow, onBack: () -> Unit, onSkip: () -> Unit, onContinue: () -> Unit, body: @Composable () -> Unit) {
    val colors = ForjaTheme.colors
    val (title, subtitle) = flow.step.texts()
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            ForjaIconButton(ForjaMoreIcons.ChevronLeft, stringResource(Res.string.onb2_back), onClick = onBack)
            ProgressLine(flow.progress, Modifier.weight(1f))
            Box(Modifier.widthIn(min = 72.dp), contentAlignment = Alignment.CenterEnd) {
                if (flow.step.optional) {
                    Text(
                        stringResource(Res.string.onb2_skip),
                        Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onSkip)
                            .padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
                        style = ForjaTheme.typography.body,
                        color = colors.text2,
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            Spacer(Modifier.height(ForjaSpacing.esp4))
            Text(
                title,
                Modifier.semantics { heading() },
                style = ForjaTheme.typography.title1.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = colors.text1,
            )
            Text(subtitle, style = ForjaTheme.typography.body, color = colors.text2)
            Spacer(Modifier.height(ForjaSpacing.esp2))
            body()
            Spacer(Modifier.height(ForjaSpacing.esp4))
        }
        ForjaButton(
            stringResource(Res.string.onb2_continue),
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            ForjaButtonStyle.PRIMARY,
            enabled = flow.canContinue,
            onClick = onContinue,
        )
    }
}

@Composable
private fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    val colors = ForjaTheme.colors
    Box(modifier.height(4.dp).clip(CircleShape).background(colors.surface3)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(colors.ember))
    }
}

@Composable
private fun OnboardingStep.texts(): Pair<String, String> = when (this) {
    OnboardingStep.GOAL -> stringResource(Res.string.onb2_goal_title) to stringResource(Res.string.onb2_goal_sub)
    OnboardingStep.EXPERIENCE -> stringResource(Res.string.onb2_exp_title) to stringResource(Res.string.onb2_exp_sub)
    OnboardingStep.DAYS -> stringResource(Res.string.onb2_days_title) to stringResource(Res.string.onb2_days_sub)
    OnboardingStep.UNITS -> stringResource(Res.string.onb2_units_title) to stringResource(Res.string.onb2_units_sub)
    OnboardingStep.DISCIPLINES -> stringResource(Res.string.onb2_disc_title) to stringResource(Res.string.onb2_disc_sub)
    OnboardingStep.MODE -> stringResource(Res.string.onb2_mode_title) to stringResource(Res.string.onb2_mode_sub)
    OnboardingStep.AUDIO -> stringResource(Res.string.onb2_audio_title) to stringResource(Res.string.onb2_audio_sub)
    else -> "" to ""
}

@Composable
private fun StepBody(flow: OnboardingFlow, onChange: (OnboardingFlow) -> Unit) {
    val answers = flow.answers
    when (flow.step) {
        OnboardingStep.GOAL -> TrainingGoal.entries.forEach { goal ->
            OptionCard(goal.label(), goal.hint(), goal.icon(), answers.goal == goal) { onChange(flow.answer { it.copy(goal = goal) }) }
        }
        OnboardingStep.EXPERIENCE -> ExperienceLevel.entries.forEach { level ->
            OptionCard(level.label(), level.hint(), ForjaMoreIcons.FitnessCenter, answers.experience == level) {
                onChange(flow.answer { it.copy(experience = level) })
            }
        }
        OnboardingStep.DAYS -> DaysPicker(answers.daysPerWeek) { onChange(flow.changeDays(it)) }
        OnboardingStep.UNITS -> WeightUnit.entries.forEach { unit ->
            val hint = stringResource(if (unit == WeightUnit.KG) Res.string.unit_kg_hint else Res.string.unit_lb_hint)
            OptionCard(unit.label(), hint, ForjaMoreIcons.FitnessCenter, answers.unit == unit) {
                onChange(flow.answer { it.copy(unit = unit) })
            }
        }
        OnboardingStep.DISCIPLINES -> OnboardingDisciplines.forEach { discipline ->
            OptionCard(discipline.editorLabel(), null, discipline.onboardingIcon(), discipline in answers.disciplines) {
                onChange(flow.toggleDiscipline(discipline))
            }
        }
        OnboardingStep.MODE -> {
            OptionCard(
                stringResource(Res.string.onb2_mode_basic),
                stringResource(Res.string.onb2_mode_basic_hint),
                ForjaIcons.Check,
                answers.proMode == false,
                badge = stringResource(Res.string.onb2_recommended),
            ) { onChange(flow.answer { it.copy(proMode = false) }) }
            OptionCard(
                stringResource(Res.string.onb2_mode_pro),
                stringResource(Res.string.onb2_mode_pro_hint),
                ForjaIcons.MonitoringOutline,
                answers.proMode == true,
            ) { onChange(flow.answer { it.copy(proMode = true) }) }
        }
        OnboardingStep.AUDIO -> WorkoutAudio.entries.forEach { audio ->
            val icon = if (audio == WorkoutAudio.NO_VOICE) ForjaMoreIcons.VolumeOff else ForjaMoreIcons.VolumeUp
            OptionCard(audio.label(), audio.hint(), icon, answers.audio == audio) { onChange(flow.answer { it.copy(audio = audio) }) }
        }
        else -> Unit
    }
}

/** Tarjeta grande para elegir: ícono, título y una línea que explica; elegida, con borde y fondo ember. */
@Composable
private fun OptionCard(title: String, description: String?, icon: Painter, selected: Boolean, badge: String? = null, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(ForjaRadius.shapeL)
            .background(if (selected) colors.emberVeil else colors.surface1)
            .border(2.dp, if (selected) colors.ember else colors.surface1, ForjaRadius.shapeL)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(ForjaSpacing.esp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (selected) colors.ember else colors.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) colors.onEmber else colors.text1, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                Text(title, style = ForjaTheme.typography.title3, color = colors.text1)
                badge?.let {
                    Text(
                        it,
                        Modifier.clip(CircleShape).background(colors.goldVeil).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = ForjaTheme.typography.caption,
                        color = colors.goldInk,
                    )
                }
            }
            description?.let { Text(it, style = ForjaTheme.typography.body2, color = colors.text2) }
        }
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .then(if (selected) Modifier.background(colors.ember) else Modifier.border(2.dp, colors.controlLine, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(ForjaIcons.Check, contentDescription = null, tint = colors.onEmber, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DaysPicker(days: Int, onChange: (Int) -> Unit) {
    val colors = ForjaTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = ForjaSpacing.esp6), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp5),
        ) {
            StepButton("−", days > TrainingProfile.DAYS_RANGE.first) { onChange(-1) }
            ForjaNumber(
                "$days",
                ForjaTheme.typography.heroFigure.copy(fontSize = 96.sp, lineHeight = 100.sp),
                Modifier.widthIn(min = 96.dp),
                textAlign = TextAlign.Center,
            )
            StepButton("+", days < TrainingProfile.DAYS_RANGE.last) { onChange(1) }
        }
        Text(stringResource(Res.string.onb2_days_caption), style = ForjaTheme.typography.body, color = colors.text2)
    }
}

@Composable
private fun Done(flow: OnboardingFlow, onBack: () -> Unit, onExit: (OnboardingExit) -> Unit) {
    val colors = ForjaTheme.colors
    val answers = flow.answers
    val profile = answers.toProfile()
    val lines = listOf(
        stringResource(Res.string.onb2_summary_goal, profile.goal.label()),
        stringResource(Res.string.onb2_summary_exp, profile.experience.label()),
        pluralStringResource(Res.plurals.onb2_days_per_week, profile.daysPerWeek, profile.daysPerWeek),
        stringResource(Res.string.onb2_summary_unit, profile.unit.label()),
        stringResource(
            Res.string.onb2_summary_mode,
            stringResource(if (answers.proMode == true) Res.string.onb2_mode_pro else Res.string.onb2_mode_basic),
        ),
        stringResource(Res.string.onb2_summary_audio, (answers.audio ?: WorkoutAudio.KEEP_VOLUME).label()),
    )
    Column(Modifier.fillMaxSize().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3)) {
        Row(Modifier.fillMaxWidth()) {
            ForjaIconButton(ForjaMoreIcons.ChevronLeft, stringResource(Res.string.onb2_back), onClick = onBack)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(colors.emberVeil), contentAlignment = Alignment.Center) {
                Icon(ForjaIcons.Check, contentDescription = null, tint = colors.ember, modifier = Modifier.size(48.dp))
            }
            Text(
                stringResource(Res.string.onb2_done_title),
                Modifier.semantics { heading() },
                style = ForjaTheme.typography.title1.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = colors.text1,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(Res.string.onb2_done_sub), style = ForjaTheme.typography.body, color = colors.text2)
            Column(
                Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
            ) {
                lines.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
                        Icon(ForjaIcons.Check, contentDescription = null, tint = colors.emberInk, modifier = Modifier.size(20.dp))
                        Text(line, style = ForjaTheme.typography.body, color = colors.text1)
                    }
                }
            }
            Text(
                stringResource(Res.string.onb2_done_demo),
                style = ForjaTheme.typography.caption,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            ForjaButton(stringResource(Res.string.onb2_create_routine), Modifier.fillMaxWidth(), ForjaButtonStyle.PRIMARY) {
                onExit(OnboardingExit.CREATE_ROUTINE)
            }
            ForjaButton(stringResource(Res.string.onb2_go_today), Modifier.fillMaxWidth(), ForjaButtonStyle.SECONDARY) {
                onExit(OnboardingExit.TODAY)
            }
        }
    }
}

@Composable
private fun TrainingGoal.hint(): String = stringResource(
    when (this) {
        TrainingGoal.STRENGTH -> Res.string.goal_strength_hint
        TrainingGoal.MUSCLE -> Res.string.goal_muscle_hint
        TrainingGoal.ENDURANCE -> Res.string.goal_endurance_hint
        TrainingGoal.HEALTH -> Res.string.goal_health_hint
        TrainingGoal.WEIGHT -> Res.string.goal_weight_hint
    },
)

@Composable
private fun TrainingGoal.icon(): Painter = when (this) {
    TrainingGoal.STRENGTH -> ForjaMoreIcons.FitnessCenter
    TrainingGoal.MUSCLE -> ForjaMoreIcons.SportsGymnastics
    TrainingGoal.ENDURANCE -> ForjaMoreIcons.DirectionsRun
    TrainingGoal.HEALTH -> ForjaMoreIcons.Spa
    TrainingGoal.WEIGHT -> ForjaIcons.FireOutline
}

@Composable
private fun ExperienceLevel.hint(): String = stringResource(
    when (this) {
        ExperienceLevel.BEGINNER -> Res.string.exp_beginner_hint
        ExperienceLevel.INTERMEDIATE -> Res.string.exp_intermediate_hint
        ExperienceLevel.ADVANCED -> Res.string.exp_advanced_hint
    },
)

@Composable
private fun Discipline.onboardingIcon(): Painter = when (this) {
    Discipline.STRENGTH -> ForjaMoreIcons.FitnessCenter
    Discipline.FUNCTIONAL -> ForjaIcons.FireOutline
    Discipline.HIIT -> ForjaIcons.Timer
    Discipline.CALISTHENICS -> ForjaMoreIcons.SportsGymnastics
    Discipline.YOGA -> ForjaMoreIcons.SelfImprovement
    Discipline.PILATES -> ForjaMoreIcons.AccessibilityNew
    Discipline.MOBILITY -> ForjaMoreIcons.Spa
    Discipline.BREATHING -> ForjaMoreIcons.Air
    Discipline.CARDIO -> ForjaMoreIcons.DirectionsRun
    else -> ForjaMoreIcons.SportsMma
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
