package com.olarte.ironprogress.data.workout

import com.olarte.ironprogress.data.local.IntervalTemplateEntity
import com.olarte.ironprogress.data.local.RoomJson
import com.olarte.ironprogress.data.local.StageOneDao
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateId
import com.olarte.ironprogress.domain.interval.IntervalTemplateRepository
import com.olarte.ironprogress.domain.voice.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Intervalos guardados en Room (tabla interval_templates de F1-03). */
class RoomIntervalTemplateRepository(private val dao: StageOneDao, private val clock: Clock) : IntervalTemplateRepository {

    override fun observeTemplates(): Flow<List<IntervalTemplate>> = dao.observeIntervalTemplates().map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun findTemplate(id: IntervalTemplateId): IntervalTemplate? = dao.findIntervalTemplate(id.value)?.toDomain()

    override suspend fun saveTemplate(template: IntervalTemplate) {
        val now = clock.now().toEpochMilliseconds()
        val created = dao.findIntervalTemplate(template.id.value)?.createdAt ?: now
        dao.upsertIntervalTemplate(template.toEntity(created, now))
    }

    override suspend fun deleteTemplate(id: IntervalTemplateId) {
        dao.markIntervalTemplateDeleted(id.value, clock.now().toEpochMilliseconds())
    }
}

internal fun IntervalTemplateEntity.toDomain() = IntervalTemplate(
    id = IntervalTemplateId(id),
    name = name,
    workSeconds = workSeconds,
    restSeconds = restSeconds,
    rounds = rounds,
    exerciseIds = RoomJson.decodeFromString<List<String>>(exerciseIds).map(::ExerciseId),
    voice = RoomJson.decodeFromString<VoiceSettings>(voice),
)

internal fun IntervalTemplate.toEntity(createdAt: Long, updatedAt: Long) = IntervalTemplateEntity(
    id = id.value,
    name = name,
    workSeconds = workSeconds,
    restSeconds = restSeconds,
    rounds = rounds,
    sets = 1,
    setRestSeconds = 0,
    exerciseIds = RoomJson.encodeToString<List<String>>(exerciseIds.map { it.value }),
    voice = RoomJson.encodeToString(voice),
    intensity = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
