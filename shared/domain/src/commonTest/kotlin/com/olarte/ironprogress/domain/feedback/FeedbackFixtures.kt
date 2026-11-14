package com.olarte.ironprogress.domain.feedback

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
import com.olarte.ironprogress.domain.routine.SetType
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
import kotlin.time.Instant

/** Ejercicios y sesiones de prueba para el feedback y la recuperación. */
internal object FeedbackFixtures {
    val bench = exercise(
        "bench",
        Muscle.CHEST to MuscleRole.PRIMARY,
        Muscle.TRICEPS to MuscleRole.SECONDARY,
        Muscle.SHOULDERS to MuscleRole.SECONDARY,
    )
    val dips = exercise("dips", Muscle.TRICEPS to MuscleRole.PRIMARY, Muscle.CHEST to MuscleRole.SECONDARY)
    val squat = exercise("squat", Muscle.QUADS to MuscleRole.PRIMARY, Muscle.GLUTES to MuscleRole.SECONDARY)
    val curl = exercise("curl", Muscle.BICEPS to MuscleRole.PRIMARY)
    val catalog = listOf(bench, dips, squat, curl).associateBy { it.id }

    fun exercise(id: String, vararg muscles: Pair<Muscle, MuscleRole>) = Exercise(
        id = ExerciseId(id),
        name = LocalizedText(es = id, en = id),
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PUSH,
        muscles = muscles.map { (muscle, role) -> ExerciseMuscle(muscle, role) },
    )

    /** Una sesión con [plan]: por ejercicio, cuántas series hechas y cuántas pendientes. */
    fun session(
        id: String,
        at: Instant,
        plan: List<Triple<Exercise, Int, Int>>,
        finished: Boolean = true,
        weightKg: Double = 60.0,
    ): SessionDetail {
        val sid = SessionId(id)
        val block = SessionBlock(SessionBlockId("$id-b"), sid, 0, BlockFormat.SETS)
        val exercises = plan.mapIndexed { index, (exercise, _, _) ->
            SessionExercise(SessionExerciseId("$id-e$index"), sid, block.id, exercise.id, index)
        }
        val sets = plan.flatMapIndexed { index, (_, done, pending) ->
            (0 until done + pending).map { n ->
                WorkoutSet(
                    WorkoutSetId("$id-e$index-s$n"),
                    exercises[index].id,
                    n,
                    SetType.WORKING,
                    weightKg = weightKg,
                    reps = 8,
                    completedAt = if (n < done) at else null,
                )
            }
        }
        val session = if (finished) {
            WorkoutSession(sid, null, Discipline.STRENGTH, at, at, SessionStatus.FINISHED)
        } else {
            WorkoutSession(sid, null, Discipline.STRENGTH, at)
        }
        return SessionDetail(session, listOf(block), exercises, sets)
    }

    fun feedback(
        session: String,
        muscle: Muscle,
        at: Instant,
        load: Int? = 2,
        pump: Int? = null,
        soreness: Int? = null,
        jointPain: Int = 0,
        sorenessAt: Instant? = null,
    ) = SessionFeedback(
        id = FeedbackId("$session-${muscle.id}"),
        sessionId = SessionId(session),
        muscle = muscle,
        pump = pump,
        soreness = soreness,
        jointPain = jointPain,
        load = load,
        createdAt = at,
        sorenessAt = sorenessAt,
    )
}
