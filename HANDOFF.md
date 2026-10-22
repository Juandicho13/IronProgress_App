# Bitacora de Relevo para Gemini (IronProgress)

## 1. Contexto de trabajo obligatorio
- **Desarrollador:** Juan David Olarte Diaz (MacBook Air M4 con macOS 26.6.2, Galaxy S25 Ultra, Galaxy Watch4 44 mm, sin iPhone fisico).
- **Reglas de entrega:**
    1. Entregar siempre el **contenido completo de cada archivo** con su ruta exacta. Cero fragmentos sueltos, cero ZIPs y cero parches de git.
    2. Identificadores en ingles y comentarios en espanol.
    3. Hablar en espanol de Colombia con tuteo y **nunca usar la raya larga** en los textos.
    4. Incluir los comandos de `git` (`git add`, `git commit`, `git push`) al final de cada cambio.
    5. Si un archivo ya existe en el repositorio y no tienes su contenido exacto en el chat, pidelo antes de reescribirlo para no borrar cambios previos.

## 2. Estado actual del repositorio
- **Ultimo commit base:** `d70fad64`
- **Fase actual:** Fase 0 (Base tecnica).
- **Lo que ya existe en el repositorio (`d70fad64`):**
    - Division de `:shared` en `:shared:domain`, `:shared:data`, `:shared:health` y `:shared:ui`.
    - Catalogo semilla de 77 ejercicios (`SeedCatalog.kt`, `SeedExerciseRepository.kt`) y 21 pruebas en `commonTest`.
    - Pantalla de catalogo en MVI (`CatalogContract.kt`, `CatalogViewModel.kt`, `CatalogScreen.kt`) y textos ES/EN.
    - Primera parte del tema Forja en `:shared:ui`: `ForjaColors.kt`, `ForjaMetrics.kt`, `ForjaTypography.kt`, `ForjaTheme.kt`, `ForjaIcons.kt`, `ForjaIsland.kt` e `IronProgressApp.kt`.

## 3. Entregas de esta sesion (rama `feat/f0-forja-motion-haptics-ci`)
- [x] `shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaMotion.kt`
- [x] `shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaHaptics.kt`
- [x] `shared/ui/src/androidMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaHaptics.android.kt`
- [x] `shared/ui/src/iosMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaHaptics.ios.kt`
- [x] `.github/workflows/ci.yml`

## 4. Que falta para cerrar la Fase 0
1. **Conectar `ForjaMotion` y `ForjaHaptics` en `ForjaTheme.kt` y subir `minSdk` a 28:**
    - Requiere que Juan pegue en el chat `gradle/libs.versions.toml`, `shared/ui/build.gradle.kts` y `shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaTheme.kt`.
2. **Fuentes estaticas de Archivo en Compose Resources:**
    - Colocar los TTF en `shared/ui/src/commonMain/composeResources/font/` y enlazarlos en `ForjaTypography.kt`.
3. **Room 3 en `:shared:data`:**
    - Configurar `androidx.room3` con KSP y `BundledSQLiteDriver`, crear entidades del catalogo (`ExerciseEntity`, `MuscleEntity`, `ExerciseMuscleEntity`), sembrar desde `SeedCatalog`, exportar esquema y crear prueba de migracion.
4. **Koin y Navigation 3:**
    - Reemplazar la inyeccion manual de `App.kt`[cite: 2] por Koin y conectar la navegacion entre las pestanas Hoy, Plan, Progreso y Buscar.
5. **Modulo `:wearApp`:**
    - Crear el modulo base de Wear OS con target API 35 y minSdk 28.