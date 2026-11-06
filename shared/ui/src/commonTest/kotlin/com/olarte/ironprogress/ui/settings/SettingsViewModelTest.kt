package com.olarte.ironprogress.ui.settings

import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.settings.AppLanguage
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.CompleteOnboarding
import com.olarte.ironprogress.domain.settings.SettingsRepository
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.domain.settings.TrainingProfile
import com.olarte.ironprogress.domain.settings.WorkoutAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private class MemorySettings : SettingsRepository {
        val flow = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = flow

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            flow.update(transform)
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun changesApplyAtOnceAndOnboardingFinishesOnce() = runTest(dispatcher) {
        val repo = MemorySettings()
        val model = SettingsViewModel(repo, CompleteOnboarding(repo))
        runCurrent()
        assertFalse(model.state.value.loading)
        assertFalse(model.state.value.settings.onboardingDone)

        model.onIntent(SettingsIntent.Language(AppLanguage.ENGLISH))
        model.onIntent(SettingsIntent.Theme(ThemeMode.DARK))
        model.onIntent(SettingsIntent.HideBodyMetrics(true))
        model.onIntent(SettingsIntent.Voice(false))
        model.onIntent(SettingsIntent.FinishOnboarding(TrainingProfile(daysPerWeek = 5)))
        runCurrent()

        val s = model.state.value.settings
        assertEquals(AppLanguage.ENGLISH, s.language)
        assertEquals(ThemeMode.DARK, s.theme)
        assertTrue(s.hideBodyMetrics)
        assertFalse(s.voice)
        assertTrue(s.onboardingDone)
        assertEquals(5, s.profile?.daysPerWeek)
    }

    @Test
    fun onboardingSavesProModeAndAudioAndTheAudioRowChangesIt() = runTest(dispatcher) {
        val repo = MemorySettings()
        val model = SettingsViewModel(repo, CompleteOnboarding(repo))
        runCurrent()

        model.onIntent(SettingsIntent.FinishOnboarding(TrainingProfile(), proMode = true, audio = WorkoutAudio.NO_VOICE))
        runCurrent()
        assertTrue(model.state.value.settings.proMode)
        assertEquals(WorkoutAudio.NO_VOICE, model.state.value.settings.workoutAudio)
        assertFalse(model.state.value.settings.voice)

        model.onIntent(SettingsIntent.Audio(WorkoutAudio.DUCK))
        runCurrent()
        assertEquals(WorkoutAudio.DUCK, model.state.value.settings.workoutAudio)
        assertTrue(model.state.value.settings.voice)
    }

    @Test
    fun hidingRemovesCaloriesOnlyFromWhatIsShown() {
        val set = WorkoutSet(
            id = WorkoutSetId("w"),
            sessionExerciseId = SessionExerciseId("e"),
            sortOrder = 0,
            calories = 20,
            durationSeconds = 60,
        )
        assertNull(set.forDisplay(true).calories)
        assertEquals(60, set.forDisplay(true).durationSeconds)
        assertEquals(20, set.forDisplay(false).calories)
    }
}
