import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de interfaz compartida: pantallas Compose, ViewModels MVI y recursos bilingues ES/EN.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:${libs.versions.androidx.lifecycle.get()}")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.androidx.lifecycle.get()}")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.androidx.lifecycle.get()}")
        }
    }
}

compose.resources {
    packageOfResClass = "com.olarte.ironprogress.ui.resources"
}
