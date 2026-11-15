@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.olarte.ironprogress.ui.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * Selector de Fotos del iPhone (PHPicker): no pide permiso para ver toda la fototeca. Escrito sin
 * poder compilarlo en Linux: compilarlo en el Mac (relevo, sección 54) y probar añadir, ver y
 * borrar una foto antes de dar F2-09 por hecho.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberPhotoStore(onPicked: (PhotoPick) -> Unit): PhotoStore {
    val controller = LocalUIViewController.current
    val picked by rememberUpdatedState(onPicked)
    return remember(controller) {
        object : PhotoStore {
            // PHPicker guarda una referencia débil a su delegado: lo retenemos aquí.
            private var delegate: NSObject? = null

            override fun pick() {
                val configuration = PHPickerConfiguration()
                configuration.filter = PHPickerFilter.imagesFilter
                configuration.selectionLimit = 1
                val picker = PHPickerViewController(configuration = configuration)
                val pickerDelegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                        picker.dismissViewControllerAnimated(true, completion = null)
                        val result = didFinishPicking.firstOrNull() as? PHPickerResult
                        if (result == null) {
                            picked(PhotoPick.Cancelled)
                            return
                        }
                        result.itemProvider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, _ ->
                            val name = data?.let { savePrivate(it) }
                            dispatch_async(dispatch_get_main_queue()) {
                                picked(if (name != null) PhotoPick.Saved(name) else PhotoPick.Failed)
                            }
                        }
                    }
                }
                delegate = pickerDelegate
                picker.delegate = pickerDelegate
                controller.presentViewController(picker, animated = true, completion = null)
            }

            override fun delete(fileName: String) {
                NSFileManager.defaultManager.removeItemAtPath(photosDir() + "/" + fileName, error = null)
            }

            @OptIn(ExperimentalResourceApi::class)
            override suspend fun load(fileName: String, maxSidePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
                val data = NSData.dataWithContentsOfFile(photosDir() + "/" + fileName) ?: return@withContext null
                data.toByteArray().decodeToImageBitmap()
            }
        }
    }
}

/** Application Support: privado de la app, no aparece en Archivos ni en Fotos. */
private fun photosDir(): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val directory = "$base/progress_photos"
    NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
    return directory
}

/**
 * Redibuja la foto a [PHOTO_MAX_SIDE_PX] como máximo: así queda derecha (sin depender de la
 * orientación EXIF) y en JPEG, que la app sabe pintar aunque la original sea HEIC.
 */
@OptIn(ExperimentalForeignApi::class)
private fun savePrivate(data: NSData): String? {
    val image = UIImage.imageWithData(data) ?: return null
    val (width, height) = image.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = minOf(1.0, PHOTO_MAX_SIDE_PX / maxOf(width, height))
    val targetWidth = width * scale
    val targetHeight = height * scale
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight))
    val resized = renderer.imageWithActions { _ -> image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight)) }
    val jpeg = UIImageJPEGRepresentation(resized, 0.85) ?: return null
    val name = "photo-" + NSUUID().UUIDString + ".jpg"
    return if (jpeg.writeToFile(photosDir() + "/" + name, atomically = true)) name else null
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    return bytes
}
