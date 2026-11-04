package com.olarte.ironprogress.domain.time

/** Cómo se arma el plan de cada formato de bloque (HANDOFF 11.9). Todo en segundos. */
object TimerPlans {

    /** Descanso entre series. */
    fun rest(seconds: Int): TimerPlan = TimerPlan(listOf(TimerSegment(SegmentKind.REST, seconds.ms())))

    /** EMOM: un tramo de 60 s por minuto; lo que sobra después de completar es descanso. */
    fun emom(minutes: Int, label: String? = null): TimerPlan {
        require(minutes > 0)
        return TimerPlan((1..minutes).map { TimerSegment(SegmentKind.WORK, 60_000L, round = it, label = label) })
    }

    /** AMRAP: una cuenta atrás con el total. */
    fun amrap(totalSeconds: Int): TimerPlan = TimerPlan(listOf(TimerSegment(SegmentKind.WORK, totalSeconds.ms())))

    /** Por tiempo: hacia arriba hasta terminar (Skip) o hasta el límite; sin límite, abierto. */
    fun forTime(capSeconds: Int?): TimerPlan = TimerPlan(
        listOf(
            if (capSeconds == null) {
                TimerSegment(SegmentKind.OPEN, null)
            } else {
                TimerSegment(SegmentKind.CAP, capSeconds.ms())
            },
        ),
    )

    /** Rondas: tiempo total hacia arriba, sin límite. */
    fun rounds(): TimerPlan = TimerPlan(listOf(TimerSegment(SegmentKind.OPEN, null)))

    /**
     * Intervalos a tu medida (Tabata incluido). [exerciseIds] se alternan en cada ronda. El
     * último descanso de cada serie de rondas no se cuenta: entre series va [setRestSeconds].
     */
    fun intervals(
        workSeconds: Int,
        restSeconds: Int,
        rounds: Int,
        sets: Int = 1,
        setRestSeconds: Int = 0,
        warmupSeconds: Int = 0,
        cooldownSeconds: Int = 0,
        exerciseIds: List<String> = emptyList(),
    ): TimerPlan {
        require(workSeconds > 0 && rounds > 0 && sets > 0 && restSeconds >= 0)
        val segments = mutableListOf<TimerSegment>()
        if (warmupSeconds > 0) segments += TimerSegment(SegmentKind.WARMUP, warmupSeconds.ms())
        for (set in 1..sets) {
            for (round in 1..rounds) {
                val label = exerciseIds.takeIf { it.isNotEmpty() }?.let { it[(round - 1) % it.size] }
                segments += TimerSegment(SegmentKind.WORK, workSeconds.ms(), round, set, label)
                if (round < rounds && restSeconds > 0) {
                    segments += TimerSegment(SegmentKind.REST, restSeconds.ms(), round, set)
                }
            }
            if (set < sets && setRestSeconds > 0) {
                segments += TimerSegment(SegmentKind.REST, setRestSeconds.ms(), rounds, set)
            }
        }
        if (cooldownSeconds > 0) segments += TimerSegment(SegmentKind.COOLDOWN, cooldownSeconds.ms())
        return TimerPlan(segments)
    }

    /** Una postura del flujo: por segundos o por respiraciones, y por lado si hace falta. */
    data class Pose(val id: String, val seconds: Int? = null, val breaths: Int? = null, val bothSides: Boolean = false) {
        init {
            require((seconds == null) != (breaths == null)) { "Una postura va por segundos o por respiraciones" }
        }
    }

    /** Flujo de yoga, pilates o movilidad. Cada respiración dura [breathSeconds] (8 s por defecto). */
    fun flow(poses: List<Pose>, rounds: Int = 1, breathSeconds: Int = 8): TimerPlan {
        require(poses.isNotEmpty() && rounds > 0)
        val segments = mutableListOf<TimerSegment>()
        for (round in 1..rounds) {
            for (pose in poses) {
                val seconds = pose.seconds ?: (pose.breaths!! * breathSeconds)
                repeat(if (pose.bothSides) 2 else 1) { side ->
                    val label = if (pose.bothSides) "${pose.id}:${if (side == 0) "left" else "right"}" else pose.id
                    segments += TimerSegment(SegmentKind.POSE, seconds.ms(), round, label = label)
                }
            }
        }
        return TimerPlan(segments)
    }

    /** Respiración guiada: inhalar, retener, exhalar y retener, en segundos (0 se salta), por ciclos. */
    fun breathing(inhale: Int, holdIn: Int, exhale: Int, holdOut: Int, cycles: Int): TimerPlan {
        require(inhale > 0 && exhale > 0 && cycles > 0 && holdIn >= 0 && holdOut >= 0)
        val segments = mutableListOf<TimerSegment>()
        for (cycle in 1..cycles) {
            segments += TimerSegment(SegmentKind.INHALE, inhale.ms(), cycle)
            if (holdIn > 0) segments += TimerSegment(SegmentKind.HOLD_IN, holdIn.ms(), cycle)
            segments += TimerSegment(SegmentKind.EXHALE, exhale.ms(), cycle)
            if (holdOut > 0) segments += TimerSegment(SegmentKind.HOLD_OUT, holdOut.ms(), cycle)
        }
        return TimerPlan(segments)
    }

    private fun Int.ms(): Long = this * 1_000L
}
