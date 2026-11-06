package com.olarte.ironprogress.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseSearchTest {

    // Lista propia de prueba: no usa SeedCatalog, que vive en data.
    private val benchPress = exercise(
        id = "barbell-bench-press",
        es = "Press banca con barra",
        en = "Barbell Bench Press",
        equipment = Equipment.BARBELL,
        primary = Muscle.CHEST,
        secondary = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
    )
    private val latPulldown = exercise(
        id = "lat-pulldown",
        es = "Jalón al pecho",
        en = "Lat Pulldown",
        equipment = Equipment.CABLE,
        primary = Muscle.LATS,
        secondary = listOf(Muscle.BICEPS),
    )
    private val bicepsCurl = exercise(
        id = "barbell-curl",
        es = "Curl de bíceps con barra",
        en = "Barbell Curl",
        equipment = Equipment.BARBELL,
        primary = Muscle.BICEPS,
    )
    private val tricepsExtension = exercise(
        id = "overhead-triceps-extension",
        es = "Extensión de tríceps sobre la cabeza",
        en = "Overhead Triceps Extension",
        equipment = Equipment.DUMBBELL,
        primary = Muscle.TRICEPS,
    )
    private val archivedSquat = exercise(
        id = "smith-squat",
        es = "Sentadilla en máquina Smith",
        en = "Smith Machine Squat",
        equipment = Equipment.SMITH_MACHINE,
        primary = Muscle.QUADS,
        archived = true,
    )

    private val all = listOf(benchPress, latPulldown, bicepsCurl, tricepsExtension, archivedSquat)

    private fun search(
        text: String = "",
        muscle: Muscle? = null,
        equipment: Equipment? = null,
        includeArchived: Boolean = false,
        exercises: List<Exercise> = all,
        languageCode: String = "es",
    ): List<String> = ExerciseSearch
        .filter(exercises, ExerciseQuery(text, muscle, equipment, includeArchived), languageCode)
        .map { it.id.value }

    @Test
    fun emptyTextReturnsAllNotArchivedSortedByRequestedLanguage() {
        assertEquals(
            listOf("barbell-curl", "overhead-triceps-extension", "lat-pulldown", "barbell-bench-press"),
            search(languageCode = "es"),
        )
        assertEquals(
            listOf("barbell-bench-press", "barbell-curl", "lat-pulldown", "overhead-triceps-extension"),
            search(languageCode = "en"),
        )
    }

    @Test
    fun ignoresAccentsAndCase() {
        assertEquals(listOf("lat-pulldown"), search("jalon"))
        assertEquals(listOf("lat-pulldown"), search("JALÓN AL"))
    }

    @Test
    fun findsEnglishNamesWhileTheScreenIsInSpanish() {
        assertEquals(listOf("barbell-bench-press"), search("bench", languageCode = "es"))
    }

    @Test
    fun everyTermMustStartSomeWordOfTheName() {
        assertEquals(listOf("barbell-curl"), search("curl bic"))
        assertEquals(listOf("barbell-curl"), search("bic curl"))
    }

    @Test
    fun aTermThatStartsNoWordFindsNothing() {
        assertEquals(emptyList(), search("ceps"))
    }

    @Test
    fun ordersByRelevanceThenAlphabetically() {
        val startsWithPhrase = exercise("a", "Press banca inclinado", "Zz one", Equipment.BARBELL, Muscle.CHEST)
        val firstWordMatches = exercise("b", "Press inclinado en banca", "Zz two", Equipment.BARBELL, Muscle.CHEST)
        val anywhere = exercise("c", "Banca y press de pecho", "Zz three", Equipment.BARBELL, Muscle.CHEST)

        assertEquals(
            listOf("a", "b", "c"),
            search("press banca", exercises = listOf(anywhere, firstWordMatches, startsWithPhrase)),
        )
    }

    @Test
    fun muscleFilterLooksOnlyAtPrimaryMuscles() {
        val result = search(muscle = Muscle.TRICEPS)

        assertEquals(listOf("overhead-triceps-extension"), result)
        assertTrue("barbell-bench-press" !in result)
    }

    @Test
    fun equipmentFilterKeepsOnlyThatEquipment() {
        assertEquals(listOf("barbell-curl", "barbell-bench-press"), search(equipment = Equipment.BARBELL))
    }

    @Test
    fun archivedAreHiddenUnlessAsked() {
        assertTrue("smith-squat" !in search("sentadilla"))
        assertEquals(listOf("smith-squat"), search("sentadilla", includeArchived = true))
    }

    @Test
    fun normalizedForSearchRemovesAccentsCaseAndSigns() {
        assertEquals("extension de triceps", "Extensión de Tríceps".normalizedForSearch())
        assertEquals("press banca barra", "press-banca  (barra)".normalizedForSearch())
    }

    private fun exercise(
        id: String,
        es: String,
        en: String,
        equipment: Equipment,
        primary: Muscle,
        secondary: List<Muscle> = emptyList(),
        archived: Boolean = false,
    ) = Exercise(
        id = ExerciseId(id),
        name = LocalizedText(es = es, en = en),
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = equipment,
        pattern = MovementPattern.OTHER,
        muscles = listOf(ExerciseMuscle(primary, MuscleRole.PRIMARY)) +
            secondary.map { ExerciseMuscle(it, MuscleRole.SECONDARY) },
        isArchived = archived,
    )
}
