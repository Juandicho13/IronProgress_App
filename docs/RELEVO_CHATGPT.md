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


## 8. F1-09 en feat/f1-argon, 4 de noviembre de 2026

Juan autorizó continuar el borrador de F1-09 en `feat/f1-argon`. La rama parte de `24933f7` y conserva F1-07 y F1-08. No se hizo push ni se tocó F1-06. Los PDF antiguos entregados como contexto no sustituyen este relevo ni el plan vigente.

- `ObserveNextRoutine` propone la siguiente rutina dentro de la carpeta de la última rutina terminada y vuelve al inicio al completar la rotación. Respeta el orden de carpetas y rutinas. Sin historial válido, propone la primera. Las sesiones libres, descartadas o de rutinas borradas no avanzan la rotación.
- Hoy usa `TodayViewModel`, Room y los componentes Forja: estado vacío con acceso al editor, lista real de ejercicios y series objetivo, duración aproximada, días entrenados y racha semanal. No muestra cifras ficticias de recuperación, metas semanales ni propuestas de progresión todavía no calculadas.
- El calendario usa el día local del teléfono, con implementaciones Android/iOS. Se refresca al volver a la pantalla. Dos sesiones en un día cuentan como un día entrenado; las sesiones futuras no cuentan. La duración es una estimación de 45 s por serie más su descanso, o el tiempo configurado en bloques temporizados; se marca con `~`.
- Empezar usa `StartSession` y pasa el identificador guardado a la ruta de Entreno. Evita iniciar desde una rutina sin series de trabajo y bloquea el doble toque mientras guarda. Continuar recupera el identificador activo incluso si la sesión es libre o se borró su rutina.
- Límite de integración: Entreno y el accesorio de la isla siguen siendo prototipos. Todavía no consumen ni editan la sesión real de Room y no hay finalización real desde esa pantalla. Esto pertenece a F1-10 y al cierre de sesión posterior. No se debe usar aún el prototipo para registrar un entrenamiento real. La rotación tras finalizar se verificó mediante los casos de uso y Room, no mediante una pantalla de finalización.
- Verificación local observada: `ktlintCheck`, `:shared:domain:testAndroidHostTest`, `:shared:data:testAndroidHostTest`, `:shared:ui:testAndroidHostTest`, `:androidApp:assembleDebug`, `:wearApp:assembleDebug`, `:shared:linkDebugFrameworkIosSimulatorArm64` y `:shared:linkDebugFrameworkIosArm64`: BUILD SUCCESSFUL. 94 pruebas, sin fallos ni errores (domain 53, data 30, ui 11). Incluye una prueba de rotación contra Room real.
- CI: no verifica este commit local mientras Juan no haga push. No se cambiaron versiones, enums, ids del catálogo ni esquemas de Room.
- Pendiente en S25 Ultra e iPhone: Hoy vacío, crear una rutina desde Hoy, reflejar edición y orden desde Plan, nombres en ES/EN, fecha y semana locales al volver desde segundo plano, textos grandes, lectores de pantalla y temas claro/oscuro. Revisar que Empezar no duplique la sesión y que Continuar vuelva a aparecer tras cerrar la app. Registrar/completar/terminar series y comprobar la rotación de extremo a extremo espera la interfaz real de sesión.


## 9. F1-10 en feat/f1-argon, 5 de noviembre de 2026

- Entreno y el accesorio de sesión ya leen Room mediante `WorkoutViewModel`. Sustituyen el prototipo descrito en la sección 8. Se puede iniciar libre desde Hoy o desde rutina, añadir ejercicios y series, copiar las medidas de la serie equivalente anterior, editar peso/reps y otras medidas, RIR o RPE, tipo y nota por serie. Los valores anteriores se distinguen de una sugerencia del motor, que sigue pendiente de F2.
- Teclado numérico propio con separador local, atajos de peso de 2,5 kg, selector de campos y validación. RIR y RPE son excluyentes. Cada valor válido y cada nota se escriben en orden, sin esperar a terminar la sesión. El error conserva la cola para Reintentar. Completar evita duplicados, confirma después de guardar y ofrece Deshacer por 5 s sin borrar ediciones posteriores.
- Inicio transaccional: `createSession` guarda sesión, bloques, ejercicios y series juntos. Dos inicios simultáneos devuelven la misma sesión. Añadir un ejercicio también es transaccional. Pruebas cubren rollback, concurrencia, valores anteriores, entrada local, errores/reintento, recuperación del ViewModel y Deshacer.
- Room versión 3 agrega únicamente `workout_sets.notes`, con valor vacío por defecto. AutoMigration 2 a 3 y esquema `3.json` generado por KSP. Los esquemas 1 y 2 no cambian. La prueba abre una base v2 con sesión activa, conserva sus medidas y comprueba notas y completado al cerrar y reabrir el archivo. Se mantiene la copia previa a migrar. No se cambiaron versiones del catálogo ni ids o enums existentes.
- Verificación local: ktlintCheck, las tres suites AndroidHostTest, APK Android/Wear OS y frameworks iOS de iPhone/simulador con BUILD SUCCESSFUL. 105 pruebas sin fallos ni errores: domain 55, data 34, ui 16. Medición inicial de 20 escrituras en archivo del Mac: p95 1,016 ms y máximo 1,423 ms; no demuestra la latencia del teléfono.
- Comprobación manual en emulador Android API 36.1, instalación temporal separada: iniciar libre, añadir press banca, abrir teclado propio, guardar medidas, completar una serie y forzar cierre. Al abrir otra vez, Hoy recuperó la sesión y el accesorio mostró todas las series registradas. Se inspeccionó visualmente el teclado. La instalación temporal se retiró después; no se modificaron los datos de la app habitual.
- Pendiente en S25 Ultra e iPhone: repetir el flujo con rutinas reales, nota y RIR/RPE, copiar anterior, Deshacer antes/después de 5 s, cerrar durante una edición, español/inglés, texto grande, TalkBack/VoiceOver y tema claro/oscuro. Medir menos de 100 ms por guardado en equipos reales. La aceptación en dispositivos sigue pendiente.
- Límites por ticket: el descanso real y sus avisos son F1-11 y posteriores; los formatos temporizados conservan una acción identificada pero deshabilitada hasta sus pantallas; terminar con gesto y Resumen corresponde a F1-21. F1-10 registra series, no cierra todavía el entrenamiento desde la UI. F1-06 sigue reservado para Juan. Sin push.


## 10. F1-11 en feat/f1-argon, 5 de noviembre de 2026

- Descanso conectado a la confirmación de guardado de cada serie. Usa el descanso del ejercicio en la rutina, luego el de la rutina y, en una sesión libre, el valor existente de 120 s. Cero segundos no abre descanso.
- `RestTimer` conserva inicio y ediciones y delega en `TimerPlans.rest`, `stateAt` y `eventsBetween`. La UI consulta por cuadro, sin descontar segundos con delay. Ajustar suma o resta 15 s; Saltar y Deshacer cancelan el descanso correspondiente. Minimizar y ampliar mantienen el mismo instante de fin.
- Pantalla completa con anillo, tiempo y datos reales de la siguiente serie, entrada desde abajo con movimiento Forja, gesto hacia abajo y botón accesible para volver a las series. Barra compacta y accesorio de la isla leen el mismo estado. Al finalizar vuelve a las series y emite el patrón de fin; los últimos 3 s emiten ticks, sin repetir un evento en cada cuadro. No se inventan pulso ni recomendaciones de progresión.
- Verificación local observada: ktlintCheck, las tres suites AndroidHostTest, ambos APK debug y frameworks iOS de iPhone/simulador con BUILD SUCCESSFUL. 109 pruebas sin fallos ni errores (domain 55, data 34, ui 20). La prueba de 20 descansos compara milisegundos exactos con lecturas irregulares y ajustes; otras pruebas cubren minimizar, ampliar, finalizar, Saltar y Deshacer.
- Alcance: el descanso permanece en memoria mientras vive el ViewModel, también al cambiar de pestaña. Al volver del segundo plano recalcula el tiempo desde instantes. No se persiste todavía al matar el proceso ni se programan avisos fuera de la app: eso requiere F1-13 a F1-16. No confundir recuperación de las series guardadas de F1-10 con recuperación del descanso.
- Pendiente de aceptación en S25 Ultra e iPhone: comprobar visualmente pantalla/barra, gesto, botones con texto grande, tema claro/oscuro y Reducir movimiento; verificar vibración de 3, 2, 1 y fin, 20 descansos reales, cambio entre pestañas y vuelta del segundo plano. No se verificaron sensaciones hápticas ni fluidez en dispositivos físicos. Sin push. F1-06 sigue reservado para Juan.

## 11. F1-13 en feat/f1-claude, 5 de noviembre de 2026

Lo hizo Claude en paralelo con F1-10 (ChatGPT), en una rama aparte que sale del commit de F1-09.

- Koin arranca antes de la interfaz: `IronProgressKoin.start(platformModule)` en `shared/.../di/IronProgressKoin.kt`, que no arranca un segundo Koin si ya hay uno.
- Android: `IronProgressApplication` (registrada en el manifiesto) llama a `startIronProgress(context)`. El `Context` pasa a mano, sin koin-android.
- iOS: `iOSApp.swift` llama a `IosStartupKt.startIronProgress()` en su `init`, y `MainViewController` lo vuelve a llamar por si acaso (no hace nada la segunda vez).
- `App()` ya no crea Koin: solo dibuja `IronProgressApp()`. En Koin 4.1.0 `KoinContext` está deprecado porque Compose usa el Koin de `startKoin` (verificado en el jar).
- `rememberPlatformModule` desapareció. Si F1-10 lo usa en alguna parte, se cambia por lo de arriba.
- `shared` tiene `withHostTest` e `IronProgressKoinTest` (2): arranca Koin sin Compose, lee los 77 ejercicios y crea una rutina. El CI corre `:shared:testAndroidHostTest`.
- Verificado en Linux: 96 pruebas, los dos APK y ktlintCheck. Falta compilar iOS en el Mac (el cambio de Swift es una línea) y abrir la app en los equipos.

## 12. F1-06 en feat/f1-claude, 5 de noviembre de 2026

- Los 77 ejercicios tienen exigencia (`ExerciseDemand`: general, técnica, columna, impacto por articulación y MET), porcentaje por músculo (`ExerciseMuscle.workShare`, suman 100), escalera (`ExerciseProgression`) y fuente (`ExerciseSource`: "IronProgress", "Propio").
- Los valores están en `shared/data/.../catalog/SeedCatalogFacts.kt`. Son un **borrador** que Juan revisa en la página "Revisión F1-06"; cambiar un valor es editar ese archivo. El ticket se cierra cuando él lo apruebe.
- MET solo con código claro del Compendio 2024 (02054, 02022, 02024, 02058, 02071). Cinta, bicicleta y paseo del granjero quedan sin MET.
- `SeedCatalog.VERSION = 2`: al abrir, la app vuelve a sembrar los 77 con sus datos y conserva lo archivado. `ExerciseRepository.findFacts` trae todo (con valor por defecto `null` para no romper los repositorios de prueba).
- Ficha de ejercicio (PANTALLAS 7.5) en `ui/catalog/ExerciseDetailScreen.kt`, ruta `Route.ExerciseDetail`. Se abre tocando una tarjeta del catálogo. Quedan fuera por ahora la figura (llega con las figuras de F1-20), "Historial" (F1-23) y "Añadir a un entreno".
- Verificado en Linux: 114 pruebas, los dos APK y ktlintCheck. Falta verla en los teléfonos.

## 13b. F1-14 en feat/f1-claude, 5 de noviembre de 2026

- Lo escribió Argon (Gemini) en `build/claude-wt`; no pudo correr Gradle en su entorno. Claude lo compiló, corrigió un smart cast en `RestNotificationHelper.kt` y el estilo, y verificó 119 pruebas, los dos APK y ktlintCheck.
- Piezas: `androidApp/.../session/RestForegroundService.kt` (servicio `health`), `androidApp/.../notifications/RestNotificationHelper.kt` (estados 1 y 2 de PANTALLAS 5.8), `shared/ui/.../rest/RestController.kt` y `RestNotificationBridge` (primer o segundo plano y permiso), manifiesto con `FOREGROUND_SERVICE_HEALTH` y `HIGH_SAMPLING_RATE_SENSORS`.
- Falta probar en el S25 Ultra: que la notificación salga en la Now Bar, los botones +15 s, Saltar y Completar serie con la app cerrada, y el sonido al terminar.
## 13. F1-17 en feat/f1-voz, 5 de noviembre de 2026
Rama nueva que sale de `feat/f1-claude` (después del diseño de 5.8), para no cruzarse con F1-14, que Argon hace en `feat/f1-claude`.
- `shared/domain/.../voice/VoiceScript.kt`: `VoiceSettings` (los tres interruptores de 5.2) y `TimerPlan.voiceLinesBetween`, que convierte los eventos del motor en las frases de PANTALLAS 5.3. En el descanso, a los 5 s dice lo que sigue y cuenta solo 3, 2, 1. Una consulta tardía (app dormida) solo dice la fase actual. 6 pruebas.
- `shared/ui/.../voice/`: `VoiceSpeaker` con `rememberVoiceSpeaker()`; `AndroidVoiceSpeaker` (TextToSpeech, foco con `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, lo puede usar el servicio sin pantalla) y `AppleVoiceSpeaker` (AVSpeechSynthesizer, sesión Playback con `duckOthers` y desactivación con `NotifyOthersOnDeactivation`). `VoiceLine.text()` da la frase en el idioma del teléfono, la misma del subtítulo.
- Todo con la voz del sistema: la voz grabada sigue pendiente (13.10).
- **iOS no se compiló aquí.** Si `VoiceSpeaker.ios.kt` falla, lo primero son los imports de `platform.AVFAudio` (`setActive`) y `platform.Foundation` (`languageCode`): se corrigen con el autocompletado.
- Todavía nadie la usa: la conecta F1-18 (intervalos).
## 14. F1-18 en feat/f1-voz, 5 de noviembre de 2026
- Dominio en `domain/interval/`: `IntervalTemplate` (límites de los botones de 5.2, total sin el último descanso, ejercicios que se alternan, `toPlan()`), `IntervalPreset` (Tabata 20/10, 15/45, 30/30, 40/20), `IntervalTemplateRepository` y `SaveIntervalResult`, que guarda los intervalos terminados como sesión `HIIT` con un bloque `INTERVALS` (`BlockResult.RepsPerRound`) y una serie por ronda.
- `RoomIntervalTemplateRepository` usa la tabla `interval_templates` de F1-03 (sin migración nueva). `VoiceSettings` ahora es `@Serializable` y se guarda en la columna `voice`.
- Pantallas en `ui/interval/`: crear y editar (5.2), correr con voz (5.3) con anillo, segmentos de rondas, número grande en los últimos 5 s, subtítulo de la voz de 2,6 s, chips de reps en el descanso (la última vez ±2; si no eliges, guarda la ronda anterior del mismo ejercicio o la última vez) y pantalla de fin.
- Se entra desde Plan, en la sección "Intervalos a tu medida", hasta que exista la pantalla de disciplinas (F1-20).
- Quedan fuera por ahora: "Probar voz", la caja de pulso (F4) y los íconos de Material Symbols que faltan (pausa, saltar y volumen salen como texto).
- La voz sigue mientras la app esté viva. Con la pantalla bloqueada mucho rato, Android puede pausar la app: eso lo cubre el servicio de F1-14. Hay que probarlo con música en los dos teléfonos.
- Verificado en Linux: 132 pruebas (2 de ViewModel, 2 de Room y 4 de dominio nuevas), los dos APK y ktlintCheck. iOS sin compilar aquí.

