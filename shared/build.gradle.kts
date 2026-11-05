import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.olarte.ironprogress.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:data"))
            implementation(project(":shared:health"))
            api(project(":shared:ui"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            runtimeOnly(libs.androidx.sqlite.bundled.jvm)
        }
    }
}
