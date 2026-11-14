import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de dominio: modelos, casos de uso y reglas del entreno. Kotlin puro: no conoce
// Android, iOS ni la base de datos.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kover)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.olarte.ironprogress.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Cobertura de los motores (F2-11, RNF-08): progresión, tiempo, voz y récords o series por
// músculo. El CI falla si bajan del 90 % de líneas. La adaptación (F4) entra aquí cuando exista.
kover {
    reports {
        filters {
            includes {
                packages(
                    "com.olarte.ironprogress.domain.progression",
                    "com.olarte.ironprogress.domain.time",
                    "com.olarte.ironprogress.domain.voice",
                    "com.olarte.ironprogress.domain.progress",
                )
            }
        }
        verify {
            rule("Motores al 90 %") {
                minBound(90)
            }
        }
    }
}
