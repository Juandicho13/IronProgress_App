package com.olarte.ironprogress.domain.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Crea ids nuevos. En la app son UUID v7 (ordenables por tiempo); en las pruebas, predecibles. */
fun interface IdGenerator {
    fun newId(): String
}

/** UUID v7 generado en el dispositivo (HANDOFF 8.7). */
object UuidV7IdGenerator : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun newId(): String = Uuid.generateV7().toString()
}
