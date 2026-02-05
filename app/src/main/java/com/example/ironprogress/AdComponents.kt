package com.example.ironprogress

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Componente de Banner Publicitario (Standard Banner).
 *
 * Renderiza un anuncio de 320x50dp utilizando la interoperabilidad de [AndroidView].
 * Diseñado para ser colocado en la parte inferior de la pantalla o dentro de listas
 * sin obstruir la navegación principal.
 */
@Composable
fun AdmobBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)

                // NOTA: Se utiliza el ID de Prueba oficial de Google (Test ID).
                // Esto previene tráfico inválido y suspensiones de cuenta durante la fase de desarrollo.
                // TODO: Reemplazar con el ID de producción real antes del lanzamiento (Release).
                adUnitId = "ca-app-pub-3940256099942544/6300978111"

                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/**
 * Componente de Rectángulo Mediano (MREC - Medium Rectangle).
 *
 * Renderiza un anuncio de 300x250dp. Este formato ofrece mayor visibilidad y CPM (Coste por Mil).
 * Ideal para pantallas de "Descanso", "Finalización de Rutina" o contenido estático
 * donde el usuario tiene pausas naturales.
 */
@Composable
fun AdmobRectangle() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)

                // NOTA: ID de Prueba oficial para formato MREC.
                // TODO: Configurar ID de producción específico para este placement.
                adUnitId = "ca-app-pub-3940256099942544/6300978111"

                loadAd(AdRequest.Builder().build())
            }
        }
    )
}