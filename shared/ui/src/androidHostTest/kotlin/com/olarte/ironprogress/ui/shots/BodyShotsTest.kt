package com.olarte.ironprogress.ui.shots

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.BodyMeasurement
import com.olarte.ironprogress.domain.body.BodyTrends
import com.olarte.ironprogress.domain.body.BodyWeight
import com.olarte.ironprogress.domain.body.ProgressPhoto
import com.olarte.ironprogress.ui.body.BodyState
import com.olarte.ironprogress.ui.catalog.CatalogState
import com.olarte.ironprogress.ui.progress.ProgressScreen
import com.olarte.ironprogress.ui.progress.ProgressState
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Capturas de F2-09: la pestaña Cuerpo y comparar fotos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rCO-w411dp-h891dp")
class BodyShotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dir: String? = System.getProperty("shots.dir")
    private val dark = mutableStateOf(true)
    private val screen = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val now = Instant.parse("2026-11-12T18:00:00Z")

    private fun shoot(name: String, content: @Composable () -> Unit) {
        assumeTrue(!dir.isNullOrBlank())
        if (screen.value == null) {
            compose.setContent {
                val current = screen.value ?: return@setContent
                ForjaTheme(darkTheme = dark.value, reduceMotion = true) { current() }
            }
        }
        compose.mainClock.autoAdvance = false
        listOf(true, false).forEach { isDark ->
            dark.value = isDark
            screen.value = content
            repeat(5) {
                shadowOf(Looper.getMainLooper()).idle()
                compose.mainClock.advanceTimeBy(1_000)
            }
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(dir!!).mkdirs()
            File(dir, "$name-${if (isDark) "oscuro" else "claro"}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun bodyState(hidden: Boolean): BodyState {
        val weights = listOf(84.2 to 70, 83.6 to 56, 82.9 to 42, 82.1 to 28, 81.4 to 14, 80.8 to 7, 80.5 to 1).mapIndexed {
                i,
                (kg, daysAgo),
            ->
            BodyWeight(BodyEntryId("w$i"), now - daysAgo.days, kg)
        }
        val measures = listOf(
            BodyMeasurement(BodyEntryId("m1"), now - 30.days, BodyMeasure.WAIST, 88.0),
            BodyMeasurement(BodyEntryId("m2"), now - 2.days, BodyMeasure.WAIST, 85.5),
            BodyMeasurement(BodyEntryId("m3"), now - 2.days, BodyMeasure.ARM, 37.5),
            BodyMeasurement(BodyEntryId("m4"), now - 2.days, BodyMeasure.CUSTOM, 38.0, "Pantorrilla"),
        )
        val photos = (0 until 4).map { ProgressPhoto(BodyEntryId("p$it"), now - (60 - it * 20).days, "p$it.jpg") }
        return BodyState(
            loading = false,
            weights = weights,
            weightChangeKg = BodyTrends.weightChange(weights, now),
            measures = BodyTrends.latestMeasures(measures),
            photos = photos,
            hidden = hidden,
        )
    }

    @Composable
    private fun body(state: BodyState) {
        ProgressScreen(
            ProgressState(loading = false),
            rememberLazyListState(),
            CatalogState(),
            "es",
            {},
            rememberLazyListState(),
            {},
            initialSegment = 2,
            bodyState = state,
            withPhotoPicker = false,
        )
    }

    @Test
    fun bodyWithData() = shoot("cuerpo-datos") { body(bodyState(hidden = false)) }

    @Test
    fun bodyHidden() = shoot("cuerpo-oculto") { body(bodyState(hidden = true)) }

    @Test
    fun photoCompare() = shoot("cuerpo-comparar") {
        com.olarte.ironprogress.ui.body.PhotoCompareScreen(bodyState(hidden = false).photos, store = null, onBack = {})
    }

    @Test
    fun bodyEmpty() = shoot("cuerpo-vacio") { body(BodyState(loading = false)) }
}
