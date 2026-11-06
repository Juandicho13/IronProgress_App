package com.olarte.ironprogress.domain.discipline

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.time.TimerPlan
import com.olarte.ironprogress.domain.time.TimerPlans

/**
 * Una postura de un flujo (PANTALLAS 7.2). [figure] es el id de su dibujo propio en la interfaz;
 * [sanskrit] solo en yoga. Va por respiraciones o por segundos, como en [TimerPlans.Pose].
 */
data class FlowPose(
    val id: String,
    val name: LocalizedText,
    val figure: String,
    val sanskrit: String? = null,
    val breaths: Int? = null,
    val seconds: Int? = null,
    val bothSides: Boolean = false,
) {
    fun toTimer(): TimerPlans.Pose = TimerPlans.Pose(id, seconds = seconds, breaths = breaths, bothSides = bothSides)
}

/** Un flujo guiado de yoga, pilates o movilidad. */
data class GuidedFlow(val id: String, val discipline: Discipline, val name: LocalizedText, val poses: List<FlowPose>, val rounds: Int) {
    init {
        require(discipline in FLOW_DISCIPLINES) { "Un flujo es de yoga, pilates o movilidad" }
        require(poses.isNotEmpty() && rounds > 0)
    }

    fun toPlan(): TimerPlan = TimerPlans.flow(poses.map { it.toTimer() }, rounds, BREATH_SECONDS)

    /** La postura de un tramo: su etiqueta es el id, con ":left" o ":right" si va por lado. */
    fun poseFor(label: String?): FlowPose? = label?.substringBefore(':')?.let { id -> poses.firstOrNull { it.id == id } }

    companion object {
        /** Cada respiración del flujo dura 8 s (PANTALLAS 7.2). */
        const val BREATH_SECONDS = 8
        val FLOW_DISCIPLINES = setOf(Discipline.YOGA, Discipline.PILATES, Discipline.MOBILITY)
    }
}

/** Los flujos de ejemplo de F1-20. El catálogo grande de posturas llega en F5-09. */
object GuidedFlows {
    private fun pose(
        id: String,
        es: String,
        en: String,
        figure: String = id,
        sanskrit: String? = null,
        breaths: Int? = null,
        seconds: Int? = null,
        bothSides: Boolean = false,
    ) = FlowPose(id, LocalizedText(es, en), figure, sanskrit, breaths, seconds, bothSides)

    /** Saludo al sol A, 5 rondas: la secuencia del ejemplo de PANTALLAS 7.2. */
    val SUN_SALUTATION_A = GuidedFlow(
        id = "sun-salutation-a",
        discipline = Discipline.YOGA,
        name = LocalizedText("Saludo al sol A", "Sun Salutation A"),
        rounds = 5,
        poses = listOf(
            pose("mountain", "Montaña", "Mountain", sanskrit = "Tadasana", breaths = 1),
            pose("arms-up", "Brazos arriba", "Upward Salute", sanskrit = "Urdhva Hastasana", breaths = 1),
            pose("forward-fold", "Flexión hacia delante", "Standing Forward Bend", sanskrit = "Uttanasana", breaths = 1),
            pose("half-fold", "Media flexión", "Half Forward Bend", sanskrit = "Ardha Uttanasana", breaths = 1),
            pose("plank", "Plancha", "Plank", sanskrit = "Kumbhakasana", breaths = 1),
            pose("up-dog", "Perro boca arriba", "Upward-Facing Dog", sanskrit = "Urdhva Mukha Svanasana", breaths = 1),
            pose("down-dog", "Perro boca abajo", "Downward-Facing Dog", sanskrit = "Adho Mukha Svanasana", breaths = 5),
        ),
    )

    /** Pilates de suelo corto, por segundos. */
    val PILATES_MAT = GuidedFlow(
        id = "pilates-mat",
        discipline = Discipline.PILATES,
        name = LocalizedText("Pilates de suelo", "Pilates mat"),
        rounds = 2,
        poses = listOf(
            pose("hundred", "Los cien", "The Hundred", seconds = 50),
            pose("bridge", "Puente", "Bridge", seconds = 40),
            pose("plank", "Plancha", "Plank", seconds = 30),
            pose("child", "Postura del niño", "Child's Pose", seconds = 30),
        ),
    )

    /** Movilidad para calentar caderas, columna y hombros. */
    val MOBILITY_WARMUP = GuidedFlow(
        id = "mobility-warmup",
        discipline = Discipline.MOBILITY,
        name = LocalizedText("Movilidad para calentar", "Warm-up mobility"),
        rounds = 1,
        poses = listOf(
            pose("cat-cow", "Gato y vaca", "Cat-Cow", seconds = 45),
            pose("down-dog", "Perro boca abajo", "Downward-Facing Dog", seconds = 30),
            pose("low-lunge", "Estocada baja", "Low Lunge", seconds = 30, bothSides = true),
            pose("forward-fold", "Flexión hacia delante", "Standing Forward Bend", seconds = 30),
            pose("arms-up", "Brazos arriba", "Upward Salute", seconds = 20),
        ),
    )

    val all = listOf(SUN_SALUTATION_A, PILATES_MAT, MOBILITY_WARMUP)

    fun find(id: String): GuidedFlow? = all.firstOrNull { it.id == id }

    fun forDiscipline(discipline: Discipline): GuidedFlow? = all.firstOrNull { it.discipline == discipline }
}
