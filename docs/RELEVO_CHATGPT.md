# Relevo para ChatGPT · 4 de noviembre de 2026

Lo dejó Claude al terminar su turno de trabajo. Si eres ChatGPT (o Codex), empieza aquí. Este archivo manda sobre la sección 4 de `HANDOFF.md` y sobre los estados de los tickets de 13.3 hasta que alguien los reescriba.

## 1. Qué leer antes de tocar código

1. Este archivo, entero.
2. `HANDOFF.md`: secciones 2 (protocolo), 3 (reglas de Juan), 7 (errores que ya pasaron) y 8 (arquitectura), y la 13.3 para los tickets de F0. Después, las secciones que cite cada ticket.
3. `docs/PLAN_V3.md` antes de cualquier ticket de F1 en adelante.
4. `docs/PANTALLAS.md` solo si el ticket construye una pantalla. Una pantalla que no está ahí no se inventa: se le pide a Juan con la plantilla 15.7 del manual.

## 2. Reglas que no se negocian

- Español de Colombia con tuteo. Nunca la raya (el guion largo) en ningún texto: respuestas, comentarios, commits ni textos de la app. Sin signos de exclamación ni emoji en la app.
- Identificadores en inglés y comentarios en español.
- Si trabajas en el chat (sin acceso al repo): archivos completos con su ruta, nada de fragmentos, diffs ni ZIP. Si trabajas con acceso al repo (Codex), cambia los archivos directamente, pero muéstrale a Juan qué cambiaste.
- Con cada cambio, los comandos de git: `git add` con rutas, `git commit -m "..."` y `git push`. Commits en Conventional Commits en español: `feat(ui): ...`, `fix(data): ...`, `test(domain): ...`.
- Bloques de comandos para zsh: sin líneas que empiecen con `#` y con los globs entre comillas (`--include="*.kt"`).
- Juan hace el push y fusiona los PR desde su Mac.
- No inventes funciones, clases, versiones ni resultados. "Compila" y "pasan las pruebas" solo se escriben después de ver la salida real. Ya pasó dos veces que una bitácora dio por hecho lo que no estaba en el repo.
- Un archivo no existe hasta que está en git: `git status` antes del commit y `git show --stat HEAD` después.
- Versiones fijas: solo las del catálogo (`gradle/libs.versions.toml`) y la sección 5 del manual. Subir una versión es un ticket aparte.
- No cambies lo estable: ids del catálogo, valores del enum `Muscle`, nombres de tablas y columnas de Room ya exportadas, nombres de paquetes. Los valores nuevos de un enum van al final.

## 3. Estado exacto

| Rama | Commit | Qué tiene |
| --- | --- | --- |
| `main` | `2533c86` | Lo de `d3c2101` más `ForjaShapes.kt` (F0-00) y los documentos del 25 de octubre. Fusionado desde el PR #1 |
| `feat/f0-claude` | 25 commits sobre `main` (con este) | Los tickets de hoy (tabla de abajo) y los documentos al día. Está en el Mac de Juan; él la sube y abre el PR |

Commits de `feat/f0-claude`, en orden:

| Ticket | Commit (mensaje) | Verificado |
| --- | --- | --- |
| F0-02 | `test(domain): restaurar ExerciseSearchTest con 10 pruebas (F0-02)` | Domain pasa de 6 a 16 pruebas |
| F0-03 | `fix(android): minSdk 28 y permiso VIBRATE (F0-03)` | APK del teléfono compila |
| F0-11 | `feat(wear): wearApp con Wear Compose, minSdk 30 y standalone (F0-11)` | APK del reloj compila. El reloj solo muestra "IronProgress" sobre negro: es el esqueleto |
| F0-05 | `chore(build): quitar los accessors compose.* deprecados (F0-05)` | Compilan los dos APK |
| F0-06 | `feat(ui): ForjaTheme sigue el tema y Reducir movimiento del sistema (F0-06)` | Compilan los dos APK |
| F0-12 | `feat(catalog): archivar y restaurar ejercicios (F0-12)` | 31 pruebas en verde (domain 16, data 15) |
| Docs | `docs: relevo para ChatGPT y manual al día` | Este archivo, la bitácora 4.8 y las versiones con ciclo menstrual y movilidad de los tres documentos |
| F0-04 | `feat(ui): idioma del sistema y textos de la isla en recursos (F0-04)` | APK compila. `values/` en inglés, `values-es/` en español, sin `values-en/`. Idioma con `Locale.current.language`. Pestañas, Buscar y banner en recursos; los datos de ejemplo de las pantallas prototipo se quedaron escritos en el código a propósito, porque F1 las reemplaza |
| F0-09 | `feat(nav): Navigation 3 y CatalogViewModel con Koin (F0-09)` | APK compila. `rememberNavBackStack` con `navConfiguration` (rutas en `ui/navigation/Route.kt`), `NavDisplay` con los dos decoradores, `uiModule` en `ui/di/UiModule.kt` y `koinViewModel` con `parametersOf(idioma)` |
| F0-10 | `feat(ui): vidrio con Haze 2.0.1 en la isla, Buscar y el accesorio (F0-10)` | APK compila. `NavDisplay` con `hazeSource`; `Modifier.forjaGlass()` en `components/ForjaGlass.kt` lee el `HazeState` de `LocalForjaHazeState` |
| F0-07 | `feat(ui): fuentes Archivo en la tipografía de Forja (F0-07)` | APK compila y trae los 6 TTF. Salen de la fuente variable de Google Fonts con fontTools (no hizo falta que Juan los bajara). `rememberForjaTypography()` en `ForjaTypography.kt` |
| F0-08 | `feat(ui): íconos Material Symbols Rounded en vez de los dibujados a mano (F0-08)` | APK compila. 18 vectores convertidos del paquete npm `@material-symbols/svg-400` 0.48.2; `ForjaIcons` devuelve `Painter`. Esos SVG son de tamaño óptico 48, no 24: si en el teléfono se ven gruesos a 24 dp, se cambian por los de 24 de fonts.google.com con los mismos nombres de archivo |
| F0-13 | `style: formatear con ktlintFormat (F0-13)` y `ci: ktlint en todos los módulos y framework iOS de iPhone en el CI (F0-13)` | `ktlintCheck` en verde, 31 pruebas en verde. Estilo `intellij_idea` en `.editorconfig`. Kover se declaró en el catálogo pero se conecta en F2-11 |
| F0-14 | `docs: README real, relevo y bitácora con el cierre de F0 (F0-14)` | README del proyecto, este archivo y la bitácora al día |
| F1-02 | `feat(domain): motor de tiempo para descanso, intervalos, flujo y respiración (F1-02)` | 14 pruebas nuevas, exactas al milisegundo. Código en `shared/domain/.../domain/time/`: `TimerPlan.kt` (tipos), `TimerEngine.kt` (`stateAt`, `segmentEndsAtMs`, `eventsBetween`) y `TimerPlans.kt` (un constructor por formato) |
| F1-01 | `feat(domain): rutinas, sesiones y bloques (F1-01)` | 14 pruebas nuevas (`RoutineModelsTest` 8 y `SessionUseCasesTest` 6). Paquetes `domain/routine`, `domain/session` y `domain/common` (`IdGenerator` con UUID v7). `Discipline` en `domain/catalog` |
| F1-03 | `feat(data): Room versión 2 con rutinas, sesiones, bloques y tablas de la Etapa 1 (F1-03)` | 15 tablas nuevas, `2.json` exportado, migración 1 a 2 generada por Room y `backupBeforeMigration` (RNF-03). `WorkoutDatabaseTest` 6 y `CatalogMigrationTest` sube a 3 |
| F1-04 | `feat(data): repositorios Room de rutinas y sesiones conectados en Koin (F1-04)` | `RoomRoutineRepository` y `RoomSessionRepository` en `shared/data/.../data/workout/`, con `RoomWorkoutRepositoryTest` 5 |
| F1-05 | `feat(ui): formato local de pesos, volumen, duraciones, distancias y fechas (F1-05)` | `Formats` y `rememberFormats()` en `shared/ui/.../ui/format/`, con `FormatsTest` 4. `shared:ui` ahora tiene `withHostTest` y el CI corre sus pruebas |

Claude compiló y probó todo en Linux con `./gradlew :androidApp:assembleDebug :wearApp:assembleDebug :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest`. **No pudo compilar iOS** (hace falta macOS). Lo primero que hay que mirar es el job `ios-framework` del CI cuando Juan suba la rama, por dos cambios:

- `shared/ui/src/iosMain/.../theme/ReduceMotion.ios.kt` llama a `platform.UIKit.UIAccessibilityIsReduceMotionEnabled()`. Si el nombre no resuelve en Kotlin/Native, se busca con el autocompletado de Android Studio sobre `platform.UIKit` y se corrige ese archivo.
- F0-09 y F0-10 agregan Navigation 3, kotlinx-serialization y Haze en `shared:ui`, que también se enlazan en iOS. Si el job de iOS falla en esas dependencias, pega el error completo antes de cambiar versiones.
- F1-05 agrega `PlatformFormatter.ios.kt` (`NSNumberFormatter`, `NSDateFormatter` y `NSLocale(localeIdentifier = ...)`). Si `maximumFractionDigits = n.toULong()` no compila, el tipo en Kotlin/Native es otro: se corrige con el autocompletado.
- F1-03 agrega `DatabaseFiles.ios.kt` (`NSFileManager.fileExistsAtPath`, `copyItemAtPath` y `removeItemAtPath`) y el plugin de kotlinx-serialization en `shared:domain` y `shared:data`. Si el job de iOS falla ahí, ese archivo es el primer sospechoso.
- F0-05 cambió de versión a material3. El accessor `compose.material3` resolvía la 1.9.0 aunque el catálogo dice 1.12.0-alpha03; ahora entra la 1.12.0-alpha03. En Android compila. Si algo se ve distinto en el iPhone o el framework no enlaza, ese es el sospechoso.

Pruebas: 75 (ui 4: `FormatsTest`; domain 44: `ExerciseTest` 6, `ExerciseSearchTest` 10, `TimerEngineTest` 14, `RoutineModelsTest` 8 y `SessionUseCasesTest` 6; data 27: `SeedCatalogTest` 5, `RoomExerciseRepositoryTest` 2, `CatalogDatabaseTest` 6, `CatalogMigrationTest` 3, `WorkoutDatabaseTest` 6 y `RoomWorkoutRepositoryTest` 5).

Lo que falta confirmar en los equipos de Juan (no lo ha visto nadie):

- La app abre en el S25 Ultra, el iPhone 18 Pro y el Galaxy Watch4 (F0-01).
- Completar serie en el prototipo vibra en los dos teléfonos y suena el patrón del fin del descanso (F0-03).
- Con el teléfono en tema claro la app sale en claro, y con Reducir movimiento las animaciones son cortas (F0-06).
- Archivar un ejercicio, cerrar la app del todo y abrirla: sigue archivado. El chip "Archivados" lo muestra y "Restaurar" lo devuelve (F0-12).
- Con el teléfono en inglés la isla dice Today, Plan y Progress; en español, Hoy, Plan y Progreso (F0-04).
- Los pesos salen como 62,5 kg en español y 62.5 kg en inglés en los dos teléfonos (F1-05). Todavía no hay pantalla que use `Formats`: se ve cuando llegue F1-10.
- Navegar entre pestañas y Buscar, girar el teléfono y mandar la app a segundo plano no pierde la pestaña ni la búsqueda; el gesto de atrás cierra Buscar o el entreno y, en la raíz, sale de la app (F0-09).
- El contenido se ve desenfocado detrás de la isla y el scroll sigue fluido en el iPhone a 120 Hz (F0-10).

## 4. Lo que queda de F0 y lo que sigue

Todo el código de F0 está hecho (F0-00 y F0-02 a F0-14). Falta solo lo que hay que ver con los ojos:

1. **Juan:** subir la rama y abrir el PR:

```
git push -u origin feat/f0-claude
```

2. Mirar el CI del PR. El job de iOS es el primero que corre los cambios de hoy en macOS (sección 3).
3. Probar en los equipos la lista de la sección 3. Con eso se cierra F0-01 y F0 queda lista.
4. Fusionar el PR en `main`.

F1 tiene 27 tickets (13.4 del manual). Ya están F1-01 a F1-04 (la base de datos y el dominio de rutinas y sesiones). Lo que quedó decidido en el código y Juan tiene que mirar:

- **Valores de 10.2:** se usaron tal como están propuestos (tipos de serie, estados de sesión y lado). Si Juan cambia alguno, es un cambio de enum antes de que haya datos reales: hoy no hay sesiones guardadas en ningún teléfono. El perfil de gimnasio por defecto (barra de 20 kg y discos) no entró todavía: lo usa la calculadora de discos.
- **Rango por defecto al agregar un ejercicio:** 3 series de 8 a 12 con RIR 2 (`RoutineDetail.addExercise`). Es un valor de arranque, la persona lo cambia en el editor.
- **`WorkoutSet` no tiene `block_id`:** el bloque sale de su `session_exercises.block_id`. Así no hay dos lugares que puedan contradecirse.
- **`created_by` en `routines`:** `USER` o `PRO:<id del profesional>`.
- **Columnas JSON:** `workout_blocks.result` ya tiene su modelo (`BlockResult`, `@Serializable`, con prueba de ida y vuelta). Las de `interval_templates`, `breathing_patterns`, `exercise_demands`, `screening_answers` y `guardian_consents` se guardan como texto y su modelo llega con el ticket que las usa (F1-06, F1-18, F1-20 y F1-26).
- **Borrados lógicos:** todo lo del usuario se marca con `deleted_at` y las lecturas no lo traen.
- **Miles en español de España:** las reglas de Unicode no ponen punto de miles en números de 4 cifras en es-ES (8420, no 8.420), y iOS las sigue. En es-CO sí sale 8.420. Si Juan quiere 8.420 siempre, se decide aparte.
- **Migración 1 a 2:** la genera Room (`AutoMigration`) porque solo agrega tablas y columnas con valor por defecto. Antes de abrir, `backupBeforeMigration` copia la base a `ironprogress.db.v1.bak`.

Siguen, en este orden:

- **F1-06**, exigencia, porcentajes, fuente y escaleras de los 77 ejercicios. Necesita que Juan revise los valores: no se inventan.
- **F1-07**, ejercicios propios, y **F1-08**, el editor de rutinas. Ya tienen los casos de uso y repositorios que necesitan.
- Las pantallas que usan el motor de tiempo (F1-11, F1-18, F1-19 y F1-20) leen `stateAt(ahora)` en cada cuadro y la voz sale de `eventsBetween`. Nunca un `delay(1000)` para contar.

## 5. Cómo se trabajó hoy, por si ayuda

- Un commit por ticket, con el número del ticket en el mensaje y el cuerpo diciendo qué cambió y qué se verificó.
- Antes de cada commit se corrió la compilación de los dos APK y las pruebas de domain y data.
- Dónde quedaron las piezas nuevas:
  - `shared/domain/.../catalog/SetExerciseArchived.kt`: caso de uso.
  - `CatalogDao.setArchived` y `CatalogDao.findArchivedIds`: el segundo lo usa `replaceSeedCatalog` para no perder el archivado al re-sembrar.
  - `shared/ui/.../theme/ReduceMotion.kt`, con su `actual` en Android y en iOS.
  - `wearApp/src/main/res/values/strings.xml`.
- El `object ForjaMotion` duplicado ya no existe: todo usa `ForjaTheme.motion`. En las funciones que lo necesitan se lee una vez al inicio (`val motion = ForjaTheme.motion`), porque dentro de lambdas que no son `@Composable` no se puede leer el tema.

## 6. Fila de bitácora que va cuando Juan confirme los equipos

```
| <fecha> | F0-01 · Base fusionada y probada | main, <commit> | Abre en S25 Ultra, iPhone 18 Pro y Galaxy Watch4; Buscar muestra 77 ejercicios |
```

## 7. Continuación de Codex, 4 de noviembre de 2026

- PR #2 de feat/f0-claude: ejecución 41872646175. Android terminó en success; iOS fue cancelado sin pasos ejecutados. No hay fallo de código de iOS observado en CI.
- Base 8ed4f60: los frameworks de simulador e iPhone terminaron localmente con BUILD SUCCESSFUL en macOS. Esto no sustituye el CI ni abrir la app en el teléfono.
- F1-07 implementado: SaveCustomExercise, escritura transaccional de ejercicio y músculos, protección de semilla, UUID v7, formulario MVI y filtros de equipo/disciplina. Diseños autorizados por Juan en PANTALLAS sección 9. No se tocó F1-06.
- Verificación F1-07: comando completo pedido por Juan con BUILD SUCCESSFUL; pruebas nuevas de persistencia y ViewModel. Falta probar crear, editar, archivar/restaurar y buscar en ES/EN tras cerrar la app en ambos teléfonos, además del descarte al volver.
- F1-07 quedó en `feab7e6`.
- F1-08 implementado en el commit `feat(ui): editar carpetas y rutinas por bloques (F1-08)`: Plan usa Room, crea y renombra carpetas, duplica y borra rutinas, agrupa por carpeta y guarda el orden de carpetas y rutinas en transacciones. El editor mantiene un borrador, permite bloques de series, ejercicios, rango de reps por serie, RIR, descanso y notas. Usa los helpers de dominio existentes para agregar, quitar y mover ejercicios. Otros formatos se conservan y esperan sus editores.
- Verificación final local: `ktlintCheck`, las tres suites AndroidHostTest, ambos APK debug y ambos frameworks iOS terminaron con BUILD SUCCESSFUL. Pruebas actuales: 81 (domain 44, data 29, ui 8), sin fallos ni errores. No se modificaron versiones del catálogo, enums ni esquemas de Room.
- CI del PR #2 sobre `8ed4f60`: Android success e iOS cancelled sin pasos. Los commits nuevos solo están en el Mac; ese CI no los verifica todavía.
- Falta probar en S25 Ultra e iPhone: crear y editar ejercicio propio en ES/EN, filtrar equipo/disciplina, archivar y restaurar después de cerrar; crear carpetas y rutinas reales, agregar bloques y ejercicios, editar reps/RIR/descanso/notas, mover ejercicios y ordenar carpetas/rutinas por arrastre y botones, duplicar y borrar sin afectar originales. Confirmar persistencia tras cerrar la app y descarte al volver, teclado, texto grande, tema claro/oscuro y lectores de pantalla. La interfaz aún no tiene verificación visual en dispositivos.
- No se hizo push. F1-06 sigue pendiente de Juan. F1-07 y F1-08 quedan pendientes de aceptación visual en teléfonos.
