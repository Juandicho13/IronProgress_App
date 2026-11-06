package com.olarte.ironprogress.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

/**
 * Selector de Archivos del iPhone. Escrito sin poder compilarlo en Linux: compilarlo en el Mac
 * (relevo, sección 21) y probar guardar y abrir una copia antes de dar F1-24 por hecho.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberBackupFiles(onSaved: (Boolean?) -> Unit, onOpened: (String?) -> Unit): BackupFiles {
    val controller = LocalUIViewController.current
    val saved by rememberUpdatedState(onSaved)
    val opened by rememberUpdatedState(onOpened)

    return remember(controller) {
        object : BackupFiles {
            // El selector guarda una referencia débil a su delegado: lo retenemos aquí.
            private var delegate: NSObject? = null

            override fun save(suggestedName: String, mimeType: String, content: String) {
                val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + suggestedName)
                val written = NSString.create(
                    string = content,
                ).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
                if (!written) return saved(false)
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                val pickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) = saved(true)

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = saved(null)
                }
                delegate = pickerDelegate
                picker.delegate = pickerDelegate
                controller.presentViewController(picker, animated = true, completion = null)
            }

            override fun open() {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON), asCopy = true)
                val pickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                        opened(url?.let { NSString.stringWithContentsOfURL(it, encoding = NSUTF8StringEncoding, error = null) })
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = opened(null)
                }
                delegate = pickerDelegate
                picker.delegate = pickerDelegate
                controller.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
