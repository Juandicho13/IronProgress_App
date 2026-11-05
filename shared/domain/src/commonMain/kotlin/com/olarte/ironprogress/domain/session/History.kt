package com.olarte.ironprogress.domain.session

/**
 * Edición del historial (F1-23, ANA-01): corregir o borrar series y bloques de sesiones ya
 * terminadas. Nunca toca la sesión en curso, que se edita desde Entreno.
 */
class EditPastSet(private val repository: SessionRepository) {

    /**
     * Guarda la serie corregida. Conserva la identidad, el ejercicio y la hora de completado;
     * una serie que no se había completado pasa a hecha con la hora de fin de la sesión.
     */
    suspend operator fun invoke(sessionId: SessionId, edited: WorkoutSet): WorkoutSet {
        val detail = finished(sessionId)
        val original = requireNotNull(detail.sets.firstOrNull { it.id == edited.id }) { "La serie no es de esta sesión" }
        val saved = edited.copy(
            sessionExerciseId = original.sessionExerciseId,
            sortOrder = original.sortOrder,
            completedAt = original.completedAt ?: detail.session.endedAt,
        )
        repository.saveSet(saved)
        return saved
    }

    private suspend fun finished(id: SessionId): SessionDetail {
        val detail = requireNotNull(repository.findSession(id)) { "La sesión no existe" }
        require(detail.session.status == SessionStatus.FINISHED) { "Solo se editan sesiones terminadas" }
        return detail
    }
}

/** Borra una serie o un bloque entero de una sesión terminada. */
class DeletePastEntry(private val repository: SessionRepository) {

    suspend fun set(sessionId: SessionId, id: WorkoutSetId) {
        val detail = finished(sessionId)
        require(detail.sets.any { it.id == id }) { "La serie no es de esta sesión" }
        repository.deleteSet(id)
    }

    suspend fun block(sessionId: SessionId, id: SessionBlockId) {
        val detail = finished(sessionId)
        require(detail.blocks.any { it.id == id }) { "El bloque no es de esta sesión" }
        repository.deleteBlock(detail, id)
    }

    private suspend fun finished(id: SessionId): SessionDetail {
        val detail = requireNotNull(repository.findSession(id)) { "La sesión no existe" }
        require(detail.session.status == SessionStatus.FINISHED) { "Solo se editan sesiones terminadas" }
        return detail
    }
}

/** Día civil (año, mes de 1 a 12, día) desde días contados desde 1970-01-01. */
data class CivilDate(val year: Int, val month: Int, val day: Int) {
    /** 0 es lunes y 6 es domingo. */
    val dayOfWeek: Int get() = (toEpochDay() + 3).mod(7L).toInt()

    fun toEpochDay(): Long {
        val y = if (month <= 2) year - 1 else year
        val era = y.floorDiv(400)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097L + doe - 719_468L
    }

    companion object {
        /** Algoritmo de Howard Hinnant, válido para cualquier fecha del calendario gregoriano. */
        fun fromEpochDay(epochDay: Long): CivilDate {
            val z = epochDay + 719_468
            val era = z.floorDiv(146_097L)
            val doe = z - era * 146_097
            val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
            val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
            val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
            return CivilDate(year, month, day)
        }

        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}

/**
 * Semanas del mes para el calendario, de lunes a domingo. Los huecos antes del día 1 y después del
 * último son null.
 */
fun monthGrid(year: Int, month: Int): List<List<CivilDate?>> {
    val first = CivilDate(year, month, 1)
    val days = List(first.dayOfWeek) { null } + (1..CivilDate.daysInMonth(year, month)).map { CivilDate(year, month, it) }
    return days.chunked(7).map { week -> week + List(7 - week.size) { null } }
}
