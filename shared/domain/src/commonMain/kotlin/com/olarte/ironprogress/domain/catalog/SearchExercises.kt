package com.olarte.ironprogress.domain.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Lo que la persona pidió. Texto, músculo y equipo son opcionales y se combinan. */
data class ExerciseQuery(
    val text: String = "",
    val muscle: Muscle? = null,
    val equipment: Equipment? = null,
    val includeArchived: Boolean = false,
)

/** Caso de uso: los ejercicios que coinciden con la búsqueda, en vivo. */
class SearchExercises(private val repository: ExerciseRepository) {

    operator fun invoke(queries: Flow<ExerciseQuery>, languageCode: String): Flow<List<Exercise>> =
        combine(repository.observeExercises(), queries) { exercises, query ->
            ExerciseSearch.filter(exercises, query, languageCode)
        }
}

/**
 * Búsqueda sin tildes ni mayúsculas, en español y en inglés a la vez. Cada palabra escrita
 * tiene que ser el comienzo de alguna palabra del nombre: "curl bic" encuentra
 * "Curl de bíceps con barra" y "bench" encuentra "Press banca con barra".
 */
object ExerciseSearch {

    fun filter(exercises: List<Exercise>, query: ExerciseQuery, languageCode: String): List<Exercise> {
        val terms = query.text.normalizedForSearch().split(' ').filter { it.isNotEmpty() }
        return exercises
            .asSequence()
            .filter { query.includeArchived || !it.isArchived }
            .filter { query.muscle == null || query.muscle in it.primaryMuscles }
            .filter { query.equipment == null || it.equipment == query.equipment }
            .mapNotNull { exercise -> relevance(exercise, terms)?.let { exercise to it } }
            .sortedWith(
                compareBy<Pair<Exercise, Int>> { it.second }
                    .thenBy { it.first.name.resolve(languageCode).normalizedForSearch() },
            )
            .map { it.first }
            .toList()
    }

    /** null si no coincide. 0: el nombre empieza por lo escrito; 1: coincide la primera palabra; 2: el resto. */
    private fun relevance(exercise: Exercise, terms: List<String>): Int? {
        if (terms.isEmpty()) return 0
        val phrase = terms.joinToString(" ")
        return listOf(exercise.name.es, exercise.name.en)
            .map { it.normalizedForSearch() }
            .mapNotNull { name ->
                val words = name.split(' ')
                when {
                    terms.any { term -> words.none { it.startsWith(term) } } -> null
                    name.startsWith(phrase) -> 0
                    words.first().startsWith(terms.first()) -> 1
                    else -> 2
                }
            }
            .minOrNull()
    }
}

/** Minúsculas, sin tildes y con los signos convertidos en espacios: "Extensión de Tríceps" queda "extension de triceps". */
fun String.normalizedForSearch(): String {
    val builder = StringBuilder(length)
    for (char in lowercase()) {
        builder.append(
            when (char) {
                'á', 'à', 'ä', 'â', 'ã' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                'ñ' -> 'n'
                'ç' -> 'c'
                else -> if (char.isLetterOrDigit()) char else ' '
            },
        )
    }
    return builder.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
}