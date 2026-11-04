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
| `feat/f0-claude` | 7 commits sobre `main` | Los tickets de hoy (tabla de abajo) y los documentos al día. Está en el Mac de Juan; él la sube y abre el PR |

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

Claude compiló y probó todo en Linux con `./gradlew :androidApp:assembleDebug :wearApp:assembleDebug :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest`. **No pudo compilar iOS** (hace falta macOS). Lo primero que hay que mirar es el job `ios-framework` del CI cuando Juan suba la rama, por dos cambios:

- `shared/ui/src/iosMain/.../theme/ReduceMotion.ios.kt` llama a `platform.UIKit.UIAccessibilityIsReduceMotionEnabled()`. Si el nombre no resuelve en Kotlin/Native, se busca con el autocompletado de Android Studio sobre `platform.UIKit` y se corrige ese archivo.
- F0-05 cambió de versión a material3. El accessor `compose.material3` resolvía la 1.9.0 aunque el catálogo dice 1.12.0-alpha03; ahora entra la 1.12.0-alpha03. En Android compila. Si algo se ve distinto en el iPhone o el framework no enlaza, ese es el sospechoso.

Pruebas: 31 (domain: `ExerciseTest` 6 y `ExerciseSearchTest` 10; data: `SeedCatalogTest` 5, `RoomExerciseRepositoryTest` 2, `CatalogDatabaseTest` 6 y `CatalogMigrationTest` 2).

Lo que falta confirmar en los equipos de Juan (no lo ha visto nadie):

- La app abre en el S25 Ultra, el iPhone 18 Pro y el Galaxy Watch4 (F0-01).
- Completar serie en el prototipo vibra en los dos teléfonos y suena el patrón del fin del descanso (F0-03).
- Con el teléfono en tema claro la app sale en claro, y con Reducir movimiento las animaciones son cortas (F0-06).
- Archivar un ejercicio, cerrar la app del todo y abrirla: sigue archivado. El chip "Archivados" lo muestra y "Restaurar" lo devuelve (F0-12).

## 4. Lo que queda de F0, en orden

1. **Juan:** subir la rama, abrir el PR, mirar el CI (sobre todo iOS) y probar en los equipos lo de la lista de arriba. Comandos:

```
git push -u origin feat/f0-claude
```

2. **F0-04 · Idioma del sistema y textos en recursos.** Es el siguiente ticket de código. Rama nueva desde `feat/f0-claude` (o desde `main` si ya se fusionó):
   - `shared/ui/src/commonMain/composeResources/values/strings.xml` pasa a inglés (hoy es una copia del español): copiar ahí el contenido de `values-en/strings.xml` y borrar `values-en` con `git rm -r`.
   - `values-es/strings.xml` queda en español.
   - En `IronProgressApp.kt` cambiar `val languageCode = "es"` por el idioma del sistema con `androidx.compose.ui.text.intl.Locale.current.language`. La llave del ViewModel ya incluye el idioma.
   - Pasar a los dos `strings.xml` los textos visibles que están escritos en el código de `IronProgressApp.kt` y `ForjaIsland.kt`: pestañas (Hoy, Plan, Progreso), Buscar, el banner de sesión y los botones del prototipo. Juan puede listar los candidatos con `grep -n "text = \"" shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/IronProgressApp.kt`.
   - No arregles la lógica del prototipo de entreno (bug 18 de 4.6): F1-10 lo reemplaza entero.
   - Terminado cuando: con el teléfono en inglés todo sale en inglés, en español todo en español y en francés todo en inglés, en Android y en iPhone.
3. **F0-09 · Navigation 3 y ViewModels con Koin.** Ticket completo en 13.3 del manual, con el código verificado. Reemplaza la pila propia (`ForjaNavDestination` en `remember`). Ojo: hoy `CatalogViewModel` recibe `SearchExercises`, `SetExerciseArchived` y el idioma; el `uiModule` de Koin tiene que pasar los tres.
4. **F0-10 · Vidrio con Haze 2.0.1** en la isla, después de F0-09. API en 12.6 del manual (no uses la de Haze 1.x).
5. **F0-07 · Fuentes Archivo** y **F0-08 · Íconos Material Symbols Rounded.** Necesitan que Juan descargue los archivos (pasos en 13.3); sin ellos no se pueden hacer.
6. **F0-13 · Calidad en el CI** (ktlint, Kover y `:shared:linkDebugFrameworkIosArm64`).
7. **F0-14 · Cierre de F0:** README real, bitácora y estado.

Ya hechos y no hay que repetirlos: F0-00, F0-02, F0-03, F0-05, F0-06, F0-11 y F0-12. F0-01 queda hecho cuando Juan confirme los equipos.

Después de F0 sigue F1 con 27 tickets (13.4 del manual), empezando por F1-01 (modelos de dominio de rutinas, sesión y bloques) y F1-02 (motor de tiempo). Los valores propuestos de 10.2 y los nombres de 10.5 se confirman con Juan antes de F1-01.

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
