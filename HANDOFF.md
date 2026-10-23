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
- **Dependencias entre modulos:** usar `project(":shared:domain")`, `project(":shared:data")`, etc., y `compose.runtime` / `compose.ui`.
- **Paquete unificado de Room 3 en `:shared:data`:** `com.olarte.ironprogress.data.local` (`CatalogEntities.kt`, `CatalogDao.kt`, `IronProgressDatabase.kt` en la misma carpeta).
- **Firma exacta de `Exercise` (`:shared:domain`):** `pattern: MovementPattern`, `unilateral: Boolean = false`, `preferredUnit: WeightUnit? = null`, `isCustom: Boolean = false`, `isArchived: Boolean = false`.

## 3. Estado de Fases
- **FASE 0 (Base tecnica): 100% COMPLETADA.**
  - [x] Division de `:shared` en `:shared:domain`, `:shared:data`, `:shared:health` y `:shared:ui`.
  - [x] Room 3 + SQLite Bundled + KSP con `RoomExerciseRepository` y 23 pruebas unitarias pasando en verde.
  - [x] Koin conectado en `:shared` (`AppModule.kt`, `PlatformModule.kt`, `.android.kt`, `.ios.kt`, `App.kt`).
  - [x] Sistema Forja completo en `:shared:ui` (`ForjaColors`, `ForjaTypography`, `ForjaMetrics`, `ForjaShapes`, `ForjaMotion`, `ForjaHaptics`, `ForjaIsland`, `IronProgressApp` con pila de navegacion, destello `brasa-velo` de 900 ms y hapticos).
  - [x] Modulo `:wearApp` (API 35, `minSdk = 28`) con las 2 caras del Galaxy Watch4.
  - [x] CI en `.github/workflows/ci.yml`.

## 4. Siguiente fase a iniciar: FASE 1 (Un entreno de principio a fin)
1. **Entidades de dominio y Room 3 para Rutinas y Sesiones:**
  - `Folder`, `Routine`, `RoutineExercise`, `WorkoutSession` (con `status = IN_PROGRESS | COMPLETED | DISCARDED`), `WorkoutSet`, `PersonalRecord`.
2. **Crear y editar ejercicios propios y rutinas con reordenamiento y superseries/circuitos.**
3. **Teclado numerico propio de Forja** (hoja de vidrio sin teclado del sistema, con botones `-2,5`, `+2,5`, `× anterior` y selector de RIR).
4. **Deteccion en vivo de los 4 tipos de record en `oro`** (peso maximo, 1RM estimado por Epley hasta 12 reps, reps al mismo peso y volumen de serie).