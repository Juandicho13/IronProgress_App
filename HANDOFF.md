# Bitacora de Relevo para Gemini (IronProgress)

## 1. Contexto de trabajo obligatorio
- **Desarrollador:** Juan David Olarte Diaz (MacBook Air M4 con macOS 26.6.2, Galaxy S25 Ultra, Galaxy Watch4 44 mm, sin iPhone fisico).
- **Equipo de Geminis:**
    - **Gemini Jefe:** dirige la arquitectura y entrega cada bloque de archivos completos.
    - **Gemini Hormiguita 1 / 2:** apoya en ajustes puntuales contra archivos locales y toma el relevo cuando se agota el cupo.
- **Reglas de entrega:**
    1. Entregar siempre el **contenido completo de cada archivo** con su ruta exacta. Cero fragmentos sueltos, cero ZIPs y cero parches de git.
    2. Identificadores en ingles y comentarios en espanol.
    3. Hablar en espanol de Colombia con tuteo y **nunca usar la raya larga** en los textos.
    4. Incluir los comandos de `git` (`git add`, `git commit`, `git push`) al final de cada cambio.

## 2. Firmas y convenciones reales del repositorio
- **Plugin KMP Android en submodulos:** `alias(libs.plugins.androidMultiplatformLibrary)`.
- **Paquete unificado de Room 3 en `:shared:data`:** `com.olarte.ironprogress.data.local` (`CatalogEntities.kt`, `CatalogDao.kt`, `IronProgressDatabase.kt` viven en la misma carpeta `local/` para evitar errores de resolucion en KSP 2).
- **Firma exacta de `Exercise` (`:shared:domain`):** `pattern: MovementPattern`, `unilateral: Boolean = false`, `preferredUnit: WeightUnit? = null`, `isCustom: Boolean = false`, `isArchived: Boolean = false`.

## 3. Entregas completadas en Fase 0
- [x] `ForjaMotion.kt`, `ForjaHaptics.kt` (`commonMain`, `androidMain`, `iosMain`) y `ForjaShapes.kt`.
- [x] `.github/workflows/ci.yml` y `AndroidManifest.xml` con permiso `VIBRATE` y `minSdk = 28`.
- [x] Room 3 en `:shared:data` compilando y pasando las 23 pruebas unitarias (`CatalogMigrationTest` y `RoomExerciseRepositoryTest`).
- [x] Koin en `:shared` (`AppModule.kt`, `PlatformModule.kt`, `PlatformModule.android.kt`, `PlatformModule.ios.kt`) conectado en `App.kt` inyectando `RoomExerciseRepository`.

## 4. Ultimo bloque pendiente para cerrar la Fase 0
1. **Conectar `ForjaShapes`, `ForjaMotion` y `ForjaHaptics` en `ForjaTheme.kt` y Navigation 3 en `IronProgressApp.kt`** (requiere revisar el contenido actual de `ForjaTheme.kt` e `IronProgressApp.kt`).
2. **Crear el modulo `:wearApp`** (Wear OS con `minSdk = 28` y `targetSdk = 35`).