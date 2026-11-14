package com.olarte.ironprogress.ui.body

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
actual fun rememberPhotoStore(onPicked: (PhotoPick) -> Unit): PhotoStore {
    val context = LocalContext.current.applicationContext
    val picked by rememberUpdatedState(onPicked)
    val scope = rememberCoroutineScope()
    // Photo Picker del sistema: no hace falta pedir permiso de fotos.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            picked(PhotoPick.Cancelled)
        } else {
            scope.launch {
                val name = withContext(Dispatchers.IO) { copyPrivate(context, uri) }
                picked(if (name != null) PhotoPick.Saved(name) else PhotoPick.Failed)
            }
        }
    }
    return remember(context) {
        object : PhotoStore {
            override fun pick() {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            override fun delete(fileName: String) {
                runCatching { File(photosDir(context), fileName).delete() }
            }

            override suspend fun load(fileName: String, maxSidePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
                val file = File(photosDir(context), fileName)
                if (!file.exists()) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSidePx) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        }
    }
}

/** Carpeta privada de la app: no la ve la galería ni otras apps. */
private fun photosDir(context: Context): File = File(context.filesDir, "progress_photos").apply { mkdirs() }

/**
 * Copia la foto reducida a [PHOTO_MAX_SIDE_PX] y en JPEG. ImageDecoder (Android 9+, el mínimo de la
 * app) respeta la orientación de la cámara.
 */
private fun copyPrivate(context: Context, uri: Uri): String? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        val longest = maxOf(width, height)
        if (longest > PHOTO_MAX_SIDE_PX) {
            val scale = PHOTO_MAX_SIDE_PX.toFloat() / longest
            decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val name = "photo-${UUID.randomUUID()}.jpg"
    File(photosDir(context), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    bitmap.recycle()
    name
}.getOrNull()

private const val JPEG_QUALITY = 85
