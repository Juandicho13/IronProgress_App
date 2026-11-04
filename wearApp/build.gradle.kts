import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// App del Galaxy Watch (Wear OS). App Android normal, como androidApp: sin el plugin de
// Compose Multiplatform y con los componentes de Wear Compose.
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":shared:domain"))
    // Solo para los colores y la tipografía de Forja; los componentes son de Wear Compose.
    implementation(project(":shared:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
}

android {
    namespace = "com.olarte.ironprogress.wear"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Igual al del teléfono: el Wearable Data Layer solo une apps con el mismo id y la misma firma.
        applicationId = "com.olarte.ironprogress"
        minSdk = libs.versions.wear.minSdk.get().toInt()
        targetSdk = libs.versions.wear.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
