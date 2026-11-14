package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.CivilDate
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PeriodSummaryTest {
    private fun exercise(id: String, primary: Muscle, secondary: Muscle? = null) = Exercise(
        id = ExerciseId(id),
        name = LocalizedText(es = id, en = id),
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PUSH,
        muscles = listOfNotNull(
            ExerciseMuscle(primary, MuscleRole.PRIMARY),
            secondary?.let { ExerciseMuscle(it, MuscleRole.SECONDARY) },
        ),
    )

    private val bench = exercise("bench", Muscle.CHEST, Muscle.TRICEPS)
    private val squat = exercise("squat", Muscle.QUADS)
    private val catalog = listOf(bench, squat).associateBy { it.id }

    /** Las 18:00 UTC del día civil [date]; en las pruebas el día local es el de UTC. */
    private fun at(date: CivilDate): Instant = Instant.fromEpochMilliseconds(date.toEpochDay() * 86_400_000 + 18 * 3_600_000L)

    private val epochDay: (Instant) -> Long = { it.toEpochMilliseconds().floorDiv(86_400_000L) }

    private fun session(id: String, date: CivilDate, minutes: Int, work: Map<Exercise, List<Pair<Double, Int>>>): SessionDetail {
        val sid = SessionId(id)
        val start = at(date)
        val block = SessionBlock(SessionBlockId("$id-b"), sid, 0, BlockFormat.SETS)
        val exercises = work.keys.mapIndexed { i, ex -> SessionExercise(SessionExerciseId("$id-$i"), sid, block.id, ex.id, i) }
        val sets = exercises.flatMap { ex ->
            work.entries.first { it.key.id == ex.exerciseId }.value.mapIndexed { i, (kg, reps) ->
                WorkoutSet(WorkoutSetId("${ex.id.value}-$i"), ex.id, i, weightKg = kg, reps = reps, completedAt = start + i.minutes)
            }
        }
        return SessionDetail(
            WorkoutSession(sid, null, Discipline.STRENGTH, start, start + minutes.minutes, SessionStatus.FINISHED),
            listOf(block),
            exercises,
            sets,
        )
    }

    private val october = session("oct", CivilDate(2026, 10, 20), 50, mapOf(bench to listOf(60.0 to 8, 60.0 to 8)))
    private val nov1 = session("n1", CivilDate(2026, 11, 2), 60, mapOf(bench to listOf(62.5 to 8, 62.5 to 8), squat to listOf(80.0 to 5)))
    private val nov2 = session("n2", CivilDate(2026, 11, 4), 40, mapOf(bench to listOf(65.0 to 6, 62.5 to 9, 62.5 to 9)))
    private val nov3 = session("n3", CivilDate(2026, 11, 16), 30, mapOf(squat to listOf(85.0 to 5)))
    private val all = listOf(nov3, october, nov2, nov1)

    @Test
    fun monthNumbersAndComparisonWithThePreviousMonth() {
        val summary = PeriodSummaries.summarize(all, catalog, Period(PeriodKind.MONTH, 2026, 11), epochDay)

        assertEquals(3, summary.stats.sessions)
        assertEquals((60 + 40 + 30) * 60L, summary.stats.totalSeconds)
        // 62,5 × 16 + 80 × 5 + 65 × 6 + 62,5 × 18 + 85 × 5
        assertEquals(1_000.0 + 400.0 + 390.0 + 1_125.0 + 425.0, summary.stats.volumeKg)
        assertEquals(3, summary.stats.trainedDays)
        assertEquals(PeriodStats(1, 50 * 60L, 960.0, 1), summary.previous)
    }

    @Test
    fun recordsKeepTheBestPerExerciseAndKind() {
        val summary = PeriodSummaries.summarize(all, catalog, Period(PeriodKind.MONTH, 2026, 11), epochDay)

        // El press bate el peso dos veces en noviembre: queda el de 65 kg.
        val heaviest = summary.records.filter { it.exerciseId == bench.id && it.kind == RecordKind.HEAVIEST_WEIGHT }
        assertEquals(listOf(65.0), heaviest.map { it.value })
        // La sentadilla del día 2 es la primera vez (no es récord); la del 16 sí.
        assertEquals(
            listOf(85.0),
            summary.records.filter {
                it.exerciseId == squat.id && it.kind == RecordKind.HEAVIEST_WEIGHT
            }.map { it.value },
        )
        assertTrue(summary.records.none { it.sessionId == SessionId("oct") })
    }

    @Test
    fun favouriteMusclesAndStreak() {
        val summary = PeriodSummaries.summarize(all, catalog, Period(PeriodKind.MONTH, 2026, 11), epochDay)

        assertEquals(bench.id, summary.favourite?.exerciseId)
        assertEquals(5, summary.favourite?.sets)
        assertEquals(listOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.QUADS), summary.topMuscles.map { it.first })
        assertEquals(5.0, summary.topMuscles.first().second)
        // Semana del 2 (n1 y n2), la del 9 vacía y la del 16: la mejor racha es 1.
        assertEquals(1, summary.bestStreakWeeks)
    }

    @Test
    fun yearAndEmptyPeriods() {
        val year = PeriodSummaries.summarize(all, catalog, Period(PeriodKind.YEAR, 2026), epochDay)
        assertEquals(4, year.stats.sessions)
        // Octubre (semana del 19) y noviembre (semanas del 2 y del 16): racha más larga de 1.
        assertEquals(1, year.bestStreakWeeks)

        val empty = PeriodSummaries.summarize(all, catalog, Period(PeriodKind.MONTH, 2026, 9), epochDay)
        assertTrue(empty.isEmpty)
        assertNull(empty.favourite)
        assertTrue(empty.records.isEmpty())
        assertEquals(PeriodStats.EMPTY, empty.previous)
    }

    @Test
    fun periodsMoveAcrossYears() {
        assertEquals(Period(PeriodKind.MONTH, 2025, 12), Period(PeriodKind.MONTH, 2026, 1).previous())
        assertEquals(Period(PeriodKind.MONTH, 2027, 1), Period(PeriodKind.MONTH, 2026, 12).next())
        assertEquals(Period(PeriodKind.YEAR, 2025), Period(PeriodKind.YEAR, 2026).previous())
        assertEquals(CivilDate(2028, 2, 29), Period(PeriodKind.MONTH, 2028, 2).lastDay)
        assertEquals(Period(PeriodKind.MONTH, 2026, 10), PeriodSummaries.firstMonth(all, epochDay))
        // Una sesión que termina pasada la medianoche cuenta en el día siguiente.
        val late = at(CivilDate(2026, 11, 30)) + 7.hours
        assertEquals(Period(PeriodKind.MONTH, 2026, 12), Period.monthOf(epochDay(late)))
    }
}
