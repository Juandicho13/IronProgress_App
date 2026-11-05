import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de interfaz compartida: pantallas Compose, ViewModels MVI y recursos bilingues ES/EN.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.olarte.ironprogress.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.haze.core)
            implementation(libs.haze.blur)
        }
        androidMain.dependencies {
            // Selector de archivos del sistema (SAF) para la copia de seguridad (F1-24).
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.compose.uiTest)
            implementation(libs.androidx.compose.uiTestJunit4)
            implementation(libs.androidx.compose.uiTestManifest)
        }
    }
}

compose.resources {
    packageOfResClass = "com.olarte.ironprogress.ui.resources"
}

// Robolectric baja su Android de prueba en tiempo de ejecución. Gradle lo resuelve antes, como
// cualquier dependencia, y las pruebas corren sin red (pruebas de interfaz de F1-27).
val robolectricAndroid: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    robolectricAndroid(libs.robolectric.androidAll)
}

val robolectricJars = layout.buildDirectory.dir("robolectric-jars")
val copyRobolectricAndroid by tasks.registering(Sync::class) {
    from(robolectricAndroid)
    into(robolectricJars)
}

tasks.withType<Test>().configureEach {
    dependsOn(copyRobolectricAndroid)
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricJars.get().asFile.absolutePath)
}
