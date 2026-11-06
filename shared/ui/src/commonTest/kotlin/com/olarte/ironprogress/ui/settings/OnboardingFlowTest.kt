package com.olarte.ironprogress.ui.settings

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.settings.ExperienceLevel
import com.olarte.ironprogress.domain.settings.TrainingGoal
import com.olarte.ironprogress.domain.settings.WeightUnit
import com.olarte.ironprogress.domain.settings.WorkoutAudio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingFlowTest {

    @Test
    fun walksEveryStepAndContinueWaitsForAnAnswer() {
        var flow = OnboardingFlow()
        assertEquals(OnboardingStep.WELCOME, flow.step)
        flow = flow.next()
        assertEquals(OnboardingStep.GOAL, flow.step)

        // Sin respuesta, Continuar no avanza.
        assertFalse(flow.canContinue)
        assertEquals(OnboardingStep.GOAL, flow.next().step)

        flow = flow.answer { it.copy(goal = TrainingGoal.MUSCLE) }.next()
        assertEquals(OnboardingStep.EXPERIENCE, flow.step)
        flow = flow.answer { it.copy(experience = ExperienceLevel.INTERMEDIATE) }.next()
        assertEquals(OnboardingStep.DAYS, flow.step)
        assertTrue(flow.canContinue)
        flow = flow.changeDays(+1).changeDays(+10).next()
        assertEquals(7, flow.answers.daysPerWeek)
        assertEquals(OnboardingStep.UNITS, flow.step)
        flow = flow.answer { it.copy(unit = WeightUnit.LB) }.next()
        assertEquals(OnboardingStep.DISCIPLINES, flow.step)
        assertFalse(flow.canContinue)
        flow = flow.toggleDiscipline(Discipline.YOGA).toggleDiscipline(Discipline.STRENGTH).toggleDiscipline(Discipline.YOGA).next()
        assertEquals(setOf(Discipline.STRENGTH), flow.answers.disciplines)
        assertEquals(OnboardingStep.MODE, flow.step)
        flow = flow.answer { it.copy(proMode = true) }.next()
        assertEquals(OnboardingStep.AUDIO, flow.step)
        flow = flow.answer { it.copy(audio = WorkoutAudio.PAUSE_MUSIC) }.next()
        assertEquals(OnboardingStep.DONE, flow.step)
        assertEquals(1f, flow.progress)

        val result = flow.result(OnboardingExit.CREATE_ROUTINE)
        assertEquals(TrainingGoal.MUSCLE, result.profile.goal)
        assertEquals(ExperienceLevel.INTERMEDIATE, result.profile.experience)
        assertEquals(WeightUnit.LB, result.profile.unit)
        assertEquals(7, result.profile.daysPerWeek)
        assertTrue(result.proMode)
        assertEquals(WorkoutAudio.PAUSE_MUSIC, result.audio)
        assertEquals(OnboardingExit.CREATE_ROUTINE, result.exit)
    }

    @Test
    fun backKeepsAnswersAndStopsAtWelcome() {
        val flow = OnboardingFlow(OnboardingStep.EXPERIENCE, OnboardingAnswers(goal = TrainingGoal.HEALTH))
        val back = flow.back()
        assertEquals(OnboardingStep.GOAL, back.step)
        assertEquals(TrainingGoal.HEALTH, back.answers.goal)
        assertEquals(OnboardingStep.WELCOME, back.back().back().step)
    }

    @Test
    fun onlyModeAndAudioCanBeSkippedAndTakeTheDefaults() {
        val goal = OnboardingFlow(OnboardingStep.GOAL)
        assertEquals(goal, goal.skip())

        val skipped = OnboardingFlow(OnboardingStep.MODE).skip().skip()
        assertEquals(OnboardingStep.DONE, skipped.step)
        val result = skipped.result(OnboardingExit.TODAY)
        assertFalse(result.proMode)
        assertEquals(WorkoutAudio.KEEP_VOLUME, result.audio)
    }

    @Test
    fun progressGrowsWithEachQuestion() {
        val questions = OnboardingStep.questions
        val first = OnboardingFlow(questions.first()).progress
        val last = OnboardingFlow(questions.last()).progress
        assertTrue(first > 0f && first < last)
        assertEquals(1f, last)
        assertEquals(0f, OnboardingFlow().progress)
    }
}
