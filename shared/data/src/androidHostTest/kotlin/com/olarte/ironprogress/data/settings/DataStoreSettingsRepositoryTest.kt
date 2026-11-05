package com.olarte.ironprogress.data.settings

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.settings.AppLanguage
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.CompleteOnboarding
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.domain.settings.TrainingProfile
import com.olarte.ironprogress.domain.settings.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreSettingsRepositoryTest {

    private fun tempPath(): String {
        val dir = File(System.getProperty("java.io.tmpdir"), "ip-settings-${System.nanoTime()}")
        dir.mkdirs()
        return File(dir, DataStoreSettingsRepository.FILE_NAME).absolutePath
    }

    @Test
    fun startsWithDefaultsAndOnboardingPending() = runTest {
        val repo = DataStoreSettingsRepository.create(tempPath())
        val settings = repo.settings.first()
        assertEquals(AppSettings(), settings)
        assertFalse(settings.onboardingDone)
    }

    @Test
    fun keepsChangesAndOnboardingOnlyOnce() = runTest {
        val repo = DataStoreSettingsRepository.create(tempPath())
        repo.update { it.copy(language = AppLanguage.ENGLISH, theme = ThemeMode.LIGHT, hideBodyMetrics = true) }
        CompleteOnboarding(repo)(TrainingProfile(unit = WeightUnit.LB, daysPerWeek = 4, disciplines = setOf(Discipline.YOGA)))

        val saved = repo.settings.first()
        assertEquals(AppLanguage.ENGLISH, saved.language)
        assertEquals(ThemeMode.LIGHT, saved.theme)
        assertTrue(saved.hideBodyMetrics)
        assertTrue(saved.onboardingDone)
        assertEquals(4, saved.profile?.daysPerWeek)
        assertEquals(setOf(Discipline.YOGA), saved.profile?.disciplines)
    }
}
