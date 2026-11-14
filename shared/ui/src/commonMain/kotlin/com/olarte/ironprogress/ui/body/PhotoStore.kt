package com.olarte.ironprogress.ui.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Fotos de progreso privadas (F2-09). [pick] abre el selector de fotos del sistema (en Android el
 * Photo Picker, en iPhone el de Fotos), copia la elegida reducida a 1600 px dentro del
 * almacenamiento privado de la app y responde con el nombre del archivo. Nunca escribe en la
 * galería y la app no pide permiso para leer todas las fotos.
 */
interface PhotoStore {
    fun pick()

    fun delete(fileName: String)

    /** La foto lista para pintar, reducida a [maxSidePx] por su lado mayor; null si no existe. */
    suspend fun load(fileName: String, maxSidePx: Int): ImageBitmap?
}

/** Lo que responde el selector: la foto guardada, cancelado o que no se pudo copiar. */
sealed interface PhotoPick {
    data class Saved(val fileName: String) : PhotoPick

    data object Cancelled : PhotoPick

    data object Failed : PhotoPick
}

/** Lado mayor de la copia privada. */
const val PHOTO_MAX_SIDE_PX = 1600

@Composable
expect fun rememberPhotoStore(onPicked: (PhotoPick) -> Unit): PhotoStore

/** Carga una foto fuera del hilo principal. Mientras carga (o si no existe) es null. */
@Composable
fun rememberPhoto(store: PhotoStore, fileName: String, maxSidePx: Int): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, store, fileName, maxSidePx) {
        value = runCatching { store.load(fileName, maxSidePx) }.getOrNull()
    }
    return bitmap
}
