import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de datos: implementa lo que domain define. Room 3 guarda el catálogo en SQLite con el
// mismo driver empaquetado en Android e iOS, y KSP genera el código de la base por plataforma.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    compilerOptions {
        // Room genera el actual de IronProgressDatabaseConstructor en cada plataforma.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.olarte.ironprogress.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(libs.kotlinx.coroutines.core)
            // api: IronProgressDatabase y CatalogDao son tipos de Room que también verán :shared y la app.
            api(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            // Las pruebas de host corren en la JVM del computador: necesitan el SQLite nativo
            // de Linux, macOS o Windows, no el de los teléfonos.
            runtimeOnly(libs.androidx.sqlite.bundled.jvm)
            // Para leer los esquemas exportados en la prueba de migración.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
}

val roomSchemasPath: String = layout.projectDirectory.dir("schemas").asFile.absolutePath

tasks.withType<Test>().configureEach {
    // CatalogMigrationTest lee de aquí los esquemas exportados por Room.
    systemProperty("ironprogress.roomSchemas", roomSchemasPath)
}