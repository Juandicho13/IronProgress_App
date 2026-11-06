package com.olarte.ironprogress.ui.backup

import com.olarte.ironprogress.domain.backup.BackupRepository
import com.olarte.ironprogress.domain.backup.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private class Fake : BackupRepository {
        val restored = mutableListOf<String>()

        override suspend fun createBackup(): String = "{copia}"

        override suspend fun restore(json: String): RestoreResult {
            restored += json
            return if (json == "{copia}") RestoreResult.Restored(3) else RestoreResult.NotABackup
        }

        override suspend fun exportSetsCsv(languageCode: String): String = "a,b\n"
    }

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-05T15:00:00Z")
    }
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun savingAsksForAFileWithTheDateInItsName() = runTest(dispatcher) {
        val model = BackupViewModel(Fake(), clock)
        model.onIntent(BackupIntent.CreateBackup)
        runCurrent()
        assertEquals(SaveRequest("ironprogress-2026-11-05.json", "application/json", "{copia}"), model.saveRequests.first())

        model.onIntent(BackupIntent.Saved(true))
        assertEquals(BackupMessage.SAVED, model.state.value.message)
        model.onIntent(BackupIntent.Saved(null))
        assertNull(model.state.value.message)
        assertFalse(model.state.value.busy)
    }

    @Test
    fun restoringWaitsForConfirmation() = runTest(dispatcher) {
        val fake = Fake()
        val model = BackupViewModel(fake, clock)
        model.onIntent(BackupIntent.Opened("{copia}"))
        assertEquals("{copia}", model.state.value.pendingRestore)
        assertEquals(emptyList(), fake.restored)

        model.onIntent(BackupIntent.ConfirmRestore)
        runCurrent()
        assertEquals(listOf("{copia}"), fake.restored)
        assertEquals(BackupMessage.RESTORED, model.state.value.message)

        model.onIntent(BackupIntent.Opened("basura"))
        model.onIntent(BackupIntent.ConfirmRestore)
        runCurrent()
        assertEquals(BackupMessage.NOT_A_BACKUP, model.state.value.message)
    }
}
