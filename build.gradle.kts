plugins {
    // Se declaran aquí sin aplicar para que cada plugin se cargue una sola vez
    // y no en el classloader de cada subproyecto.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover) apply false
}

// ktlint revisa el estilo de Kotlin en todos los módulos (F0-13). Reglas en .editorconfig.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            // Lo que generan KSP y los recursos de Compose no se revisa.
            exclude { it.file.path.contains("/build/") }
        }
    }
}