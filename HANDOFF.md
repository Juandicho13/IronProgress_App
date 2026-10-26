# IronProgress · Manual de relevo para Gemini (jefe y hormigas)

**Estado:** 25 de octubre de 2026 · **Plan:** v3, en cuatro etapas · **Repositorio:** https://github.com/Juandicho13/IronProgress_App · **Rama de trabajo:** `feat/f0-forja-motion-haptics-ci`, commit `d3c2101`, PR #1 abierto contra `main` (`d70fad6`). **Ese commit no compila:** falta `ForjaShapes.kt` (sección 4.6, ticket F0-00).

> Este archivo reemplaza a la "Bitácora de Relevo" corta que trae `d3c2101`, que volvió a dar por hechas cosas que no están en el repo (sección 4.7). Si algo de aquí choca con el código del repositorio, manda el repositorio: se corrige el código o se corrige este archivo, pero nunca se adivina.
>
> Viene con dos documentos más: `docs/PLAN_V3.md` (el qué y el porqué del plan v3: salud, relojes, disciplinas, plataforma, cobros y legal) y `docs/PANTALLAS.md` (las 29 pantallas diseñadas, descritas para construirlas sin verlas).

## Índice

0. Cómo usar este manual
1. La app en un minuto
2. Protocolo jefe y hormigas
3. Reglas de Juan
4. Estado exacto del proyecto
5. Versiones y dependencias
6. Configuración de Gradle que funciona
7. Errores que ya pasaron y cómo no repetirlos
8. Arquitectura y convenciones de código
9. Especificación funcional
10. Modelo de datos y sincronización
11. Motores: progresión, ajuste antes y durante, tiempo y exigencia
12. Sistema de diseño Forja
13. Plan de trabajo: etapas, fases y tickets
14. Verificación: comandos, CI y dispositivos
15. Plantillas de prompts
16. Enlaces

---

## 0. Cómo usar este manual

| Documento | Qué tiene | Quién lo lee |
| --- | --- | --- |
| `HANDOFF.md` (este) | Cómo se trabaja, estado real del código, reglas, arquitectura, motores, Forja, tickets y comandos | El jefe, entero. Cada hormiga, como mínimo las secciones 2, 3, 7 y 8 y las que cite su ticket |
| `docs/PLAN_V3.md` | El plan v3 completo: etapas, salud antes y durante, iPhone y Samsung al máximo, disciplinas, plataforma, cobros, legal, los 78 requisitos funcionales y 9 no funcionales nuevos, y las decisiones | El jefe, entero, antes de escribir tickets de F1 en adelante. La hormiga, la sección que cite su ticket |
| `docs/PANTALLAS.md` | Las 29 pantallas del lienzo con estructura, medidas, estados, textos y toques | La hormiga que construye una pantalla, la sección de esa pantalla |

- **Juan:** en cada conversación nueva con el Gemini jefe, adjunta los tres archivos y pega el prompt 15.1. A cada hormiga le pegas el prompt 15.2, el ticket del jefe y la salida de los `cat` que pide el ticket, y le adjuntas `HANDOFF.md` (más `docs/PANTALLAS.md` si el ticket construye una pantalla).
- **Gemini jefe:** lees los tres antes de responder. Planeas, escribes tickets, revisas entregas y mantienes al día la sección 4 (estado y bitácora) y la 13 (plan). No escribes archivos grandes salvo que Juan lo pida.
- **Gemini hormiga:** haces un solo ticket. Antes de escribir código lees las secciones 2, 3, 7 y 8 y pides los archivos que vas a tocar.
- **Si dos documentos chocan:** manda el repositorio, luego `HANDOFF.md`, luego `docs/PLAN_V3.md` y luego `docs/PANTALLAS.md`. Lo que se corrija en uno se corrige en los demás en el mismo commit.
- Los tres viven en el repo: `HANDOFF.md` en la raíz y los otros dos en `docs/`. Cada ticket cerrado actualiza la bitácora (4.8) en el mismo commit o en el siguiente.
- La especificación, Forja, el lienzo de pantallas y la propuesta v3 viven en Claude y son privados: Gemini no puede abrirlos. Todo lo esencial está copiado en estos tres archivos.

---

## 1. La app en un minuto

IronProgress (nombre provisional; el definitivo se decide justo antes de publicar) es una app de entrenamiento para Android e iOS, hecha con Kotlin Multiplatform y Compose Multiplatform, con apps para Galaxy Watch (Wear OS) y Apple Watch. Te dice qué hacer hoy y por qué, registra una serie, una ronda o una postura en un toque, se adapta a cómo llegas y a cómo vas durante la sesión, funciona sin conexión y sincroniza con tu cuenta.

**El plan v3 en cuatro etapas** (detalle en `docs/PLAN_V3.md`, sección 14; semanas a medio tiempo, estimación gruesa):

| Etapa | Versión | Semanas | Qué trae |
| --- | --- | --- | --- |
| 1 · App personal con salud y relojes | v1 | 43 a 63 | La v2 completa (fases F0 a F6) más todas las disciplinas, intervalos a tu medida con voz, descanso a pantalla completa con Live Activity y Live Update, Salud y Health Connect desde el inicio, adaptación antes y durante la sesión, y las apps de Apple Watch y Galaxy Watch |
| 2 · Pulido de movimiento | v1.1 | 4 a 6 más | Solo animaciones y vibración al nivel de Apple, sin funciones nuevas |
| 3 · Salud avanzada y cámara | v1.5 | 15 a 23 más | Sensores avanzados, VO2 máx, cámara (reps, técnica, fotos guiadas), nutrición leída de Salud, ánimo, embarazo y plan Pro |
| 4 · Plataforma de profesionales | v2 | 28 a 41 más | Entrenadores, creadores y fisioterapeutas: cada uno pone su precio y IronProgress cobra 5 % por venta |

Hoy se trabaja en la Etapa 1, fase F0. Nada de las etapas 2 a 4 se programa antes de cerrar la anterior.

**Principios que no se negocian**

1. Registrar una serie nunca toma más de dos toques; la serie sugerida, uno. Ninguna función nueva sube el presupuesto de toques (12.5).
2. Ninguna sugerencia ni ajuste sale sin su explicación de una línea, y la persona siempre decide.
3. Los datos del usuario nunca se pierden: guardado en cada cambio y migraciones probadas.
4. Los datos de salud son del usuario: consentimiento por tipo de dato, cero anuncios y borrado total.
5. Desde los 13 años; de 13 a 17, modo joven con funciones limitadas (9.5).

**Para quién:** personas que entrenan de 2 a 6 días por semana, en cualquier disciplina (fuerza, funcional, HIIT, calistenia, yoga, pilates, movilidad, respiración, cardio y combate), y quieren progresar sin llevar la cuenta a mano. Frente a Hevy y Strong, que se centran en registrar, aquí el centro es la siguiente carga, los estancamientos y el ajuste con sueño, pulso y VFC.

**Equipos y entorno de Juan**

| Pieza | Detalle |
| --- | --- |
| Computador | MacBook Air M4, macOS 26.6.2 (Tahoe) |
| Teléfonos de prueba | Galaxy S25 Ultra (Android) y iPhone 18 Pro (iOS). El simulador de iOS es solo respaldo |
| Relojes | Galaxy Watch4 de 44 mm (Wear OS), hoy. Apple Watch Series 12, cuando llegue (hace falta para F4; el diseño usa el de 46 mm) |
| IDE | Android Studio Quail 4 (2026.1.4 Patch 1) con el plugin Kotlin Multiplatform |
| iOS | Xcode 27. Kotlin 2.4.20 está probado con Xcode 26.4; si el build de iOS falla por Xcode, se instala Xcode 26.6 al lado |
| Terminal | zsh, con `ANDROID_HOME` y `PATH` en `~/.zshrc` |

Juan es estudiante de Ingeniería de Software en UNIMINUTO. Es un proyecto personal, sin fecha de entrega. Gratis al lanzar; la plata llega en la Etapa 4.

---

## 2. Protocolo jefe y hormigas

### 2.1 Roles

| Rol | Hace | No hace |
| --- | --- | --- |
| Jefe | Lee el estado real, elige el siguiente ticket, lo escribe con la plantilla 15.3, revisa la entrega contra su criterio de terminado, actualiza la bitácora y este manual | Escribir de memoria archivos que no ha visto; marcar algo como hecho sin ver la salida de Juan |
| Hormiga | Ejecuta un ticket: pide los archivos que falten, entrega archivos completos, da comandos de verificación y de git, y el texto para la bitácora | Tocar archivos fuera del ticket, subir versiones, renombrar paquetes, "mejorar" cosas que nadie pidió |
| Juan | Pega archivos y salidas, aplica los cambios a mano, corre comandos, hace push y fusiona los PR | |

### 2.2 Flujo de un ticket

1. El jefe elige el ticket (sección 13) y lo entrega completo con la plantilla 15.3, incluidos los `cat` que la hormiga necesita y, si construye una pantalla, la sección de `docs/PANTALLAS.md` que la describe.
2. Juan abre una hormiga con el prompt 15.2, el ticket y la salida de esos `cat`, y le adjunta `HANDOFF.md` (más `docs/PANTALLAS.md` si hay pantalla).
3. La hormiga responde en este orden: plan en 3 a 6 líneas, archivos completos con su ruta, comandos para verificar, comandos de git (plantilla 15.4).
4. Juan aplica, corre la verificación y pega la salida completa. Si hay error, desde la primera línea que empieza con `e:` o desde `FAILURE`.
5. Si falla, la hormiga corrige solo lo que el error señala y vuelve a entregar completos los archivos que cambian. Nunca reescribe todo desde cero.
6. Cuando pasa, la hormiga escribe el cierre con la plantilla 15.5 y Juan se lo pega al jefe.
7. El jefe revisa, actualiza la bitácora (4.8) y pasa al siguiente ticket.

### 2.3 Reglas de oro

1. **No inventes.** Ni funciones, ni clases, ni archivos, ni versiones, ni resultados. Si no viste un archivo en esta conversación, pide su `cat`. Si no estás seguro de una API, dilo y propone cómo comprobarla (autocompletado del IDE, código fuente de la librería, la sección 5).
2. **Nada se da por hecho sin salida real.** "Compila" y "pasan las pruebas" solo se escriben después de ver la salida de Juan. Ya pasó dos veces: las bitácoras de `8d74639` y de `d3c2101` daban por hecho lo que no estaba en el repo, y la segunda dejó la rama sin compilar (4.7).
3. **Un ticket, una rama, un PR pequeño.** Rama `feat/<fase>-<tema>` o `fix/<tema>`, por ejemplo `feat/f0-navigation3`. `main` siempre debe poder publicarse.
4. **Archivos completos.** Nunca fragmentos, diffs, "el resto queda igual" ni ZIP.
5. **Rutas exactas** desde la raíz del repo y el source set correcto (tabla 8.3).
6. **Versiones fijas.** Solo las de la sección 5. Subir una versión es un ticket aparte.
7. **No cambies lo estable:** ids del catálogo, valores del enum `Muscle`, nombres de tablas y columnas ya exportadas en un esquema de Room, nombres de paquetes.
8. **Pregunta antes de suponer** cuando el ticket deja dos caminos con consecuencias distintas.
9. **Un archivo nuevo se crea completo desde el chat.** No uses "New Kotlin Class" del IDE para luego pegar encima: deja stubs vacíos con otro paquete o en otra carpeta.
10. **Un archivo no existe hasta que está en git.** Antes del commit, `git status`; después, `git show --stat HEAD`. El único "compila" que vale es el CI en verde.
11. **No se adelanta trabajo de otra etapa ni se inventan pantallas.** Lo que no está en `docs/PANTALLAS.md` se le pide a Juan con la plantilla 15.7; lo de las etapas 2 a 4 espera su turno (13.9).

### 2.4 Cómo pedirle archivos a Juan

Pide comandos listos para copiar en zsh, uno por línea, sin líneas de comentario con `#` (zsh no acepta comentarios en modo interactivo y da error) y con los globs entre comillas:

```
git status
git log --oneline -5
cat shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaTheme.kt
grep -rn "ForjaMotion" shared --include="*.kt"
find shared -name "*.kt" -path "*commonTest*"
```

---

## 3. Reglas de Juan

- Español de Colombia, con tuteo.
- **Nunca uses raya ni guion largo** (los signos que no son el guion corto del teclado) en ningún texto: respuestas, comentarios de código, mensajes de commit ni textos de la app. Usa comas, dos puntos, paréntesis o " · ".
- Identificadores en inglés y comentarios en español.
- Código: siempre el contenido completo de cada archivo, pegado en el chat con su ruta. Nada de ZIP, descargas ni parches de git. Juan aplica los cambios a mano.
- Con cada cambio van los comandos de git: `git add` con las rutas, `git commit -m "..."` y `git push`. Mensajes en estilo Conventional Commits en español: `feat(ui): ...`, `fix(data): ...`, `test(domain): ...`, `docs: ...`, `ci: ...`.
- En zsh: nada de líneas con `#` en los bloques de comandos y los globs entre comillas (`--include="*.kt"`). Sin comillas, zsh responde "no matches found".
- Firma: toda web o pantalla "Acerca de" con pie de página lleva la firma "juan-olarte-dev" con línea vertical y el monograma JO, abajo a la derecha.
- Producto: bonito, práctico y con el mínimo de toques. "Una pantalla, un trabajo."
- Textos de la app: tuteo y verbos; números con formato local ("62,5 kg", "8.420 kg"); separador " · "; sin signos de exclamación y sin emoji. Vale también para las frases de la voz, que salen escritas en el subtítulo.
- Marcas: en la app se dice "Funcional", nunca "CrossFit" (solo la pueden usar los gimnasios afiliados), y tampoco se usa "HYROX".
- Salud y menores: cero anuncios y cero venta de datos, consentimiento por tipo de dato y desde los 13 años con modo joven (9.5). Nada de lo que diga la app es consejo médico.

---

## 4. Estado exacto del proyecto

Verificado por Claude el 25 de octubre de 2026 clonando el repo y compilando en Linux con el mismo comando del CI.

### 4.1 Git

| Rama | Commit | Contenido |
| --- | --- | --- |
| `main` | `d70fad6` | Base KMP, capas, sistema Forja y pantallas prototipo |
| `feat/f0-forja-motion-haptics-ci` | `d3c2101` | `main` más ForjaMotion, ForjaHaptics, CI, Room 3 en verde, Koin, una pila de navegación propia y el módulo wearApp. PR #1 abierto contra `main`. **No compila** (4.6, fila 0) |
| `legacy` | | App vieja Android nativa. No se toca |
| `feat/capas-shared`, `feat/sistema-forja` | | Ya fusionadas en `main`. Se pueden borrar |

Commits de la rama de trabajo, del más nuevo al más viejo:

| Commit | Fecha | Mensaje |
| --- | --- | --- |
| `d3c2101` | 23 oct | feat(f0): completar Fase 0 con Koin, navegacion Forja, hapticos y modulo wearApp |
| `8da5805` | 23 oct | feat(di): conectar Room 3 con Koin en Android e iOS |
| `d43c9bc` | 23 oct | fix(data): mover RoomExerciseRepositoryTest a commonTest |
| `259ff91` | 23 oct | fix(data): dejar Room 3 en verde con pruebas de base real y del esquema v1 |
| `5b3f774` | 22 oct | fix(data): unificar paquete local de Room 3 y usar Upsert en CatalogDao |
| `8d74639` | 22 oct | feat(ui,ci): agregar ForjaMotion, ForjaHaptics multiplataforma, CI y bitacora de relevo |
| `d70fad6` | 21 oct | feat(ui): implement Forja design system, floating island and core screens |
| `4c0b5ba` | 21 oct | feat(shared): divide architecture into domain, data, health and ui layers |
| `0689216` | 24 sep | Proyecto base KMP del asistente de JetBrains |

`d3c2101` toca 9 archivos: `.github/workflows/ci.yml`, `HANDOFF.md`, `settings.gradle.kts`, `IronProgressApp.kt`, `ForjaMotion.kt`, `ForjaTheme.kt` y los tres de `wearApp/`.

### 4.2 Qué hace hoy la app al abrirla (cuando compile, tras F0-00)

- `App()` arranca Koin con `rememberPlatformModule()` y `sharedAppModule`, abre la base Room 3 `ironprogress.db` y le pasa `ExerciseRepository` a `IronProgressApp`.
- En la primera lectura, `RoomExerciseRepository` siembra los 77 ejercicios de `SeedCatalog` (versión 1).
- Se ve la isla flotante con Hoy, Plan, Progreso y el círculo de Buscar. La navegación es una pila propia: `enum class ForjaNavDestination` (TODAY, PLAN, PROGRESS, SEARCH, WORKOUT) en un `mutableStateListOf` dentro de `remember`. No es Navigation 3: en Android se pierde al girar el teléfono y si el sistema cierra la app (F0-09 la reemplaza).
- **Hoy, Plan y Progreso son prototipos con datos fijos.** Buscar abre el catálogo real (Room) con búsqueda y filtro por músculo.
- El entreno es un prototipo: series fijas de "Press banca", un descanso simulado de 85 s, el destello `emberVeil` de 900 ms al completar y las vibraciones de `ForjaTheme.haptics` (`setCompleted`, `segmentTick`, `restCountdownTick`, `restFinished`).
- `wearApp` abre una actividad con dos caras de prueba (serie actual y descanso con anillo), dibujadas con `Canvas`, `ForjaTheme` y el Material 3 del teléfono. Cuenta los segundos con `delay(1000)`, que es justo lo que el motor de tiempo prohíbe (11.9); es solo un prototipo.
- Pendiente de confirmar por Juan: que abre en el Galaxy S25 Ultra, en el iPhone 18 Pro y en el Galaxy Watch4 (F0-01).

### 4.3 Árbol de archivos del repo en `d3c2101`

```
IronProgress/
  settings.gradle.kts            TYPESAFE_PROJECT_ACCESSORS (activado, sin uso) e include de :androidApp, :wearApp, :shared, :shared:domain, :shared:data, :shared:health, :shared:ui
  build.gradle.kts               plugins con apply false (androidApplication, androidMultiplatformLibrary, composeMultiplatform, composeCompiler, kotlinMultiplatform, ksp, room3)
  gradle.properties              configuration-cache y caching activos
  gradle/libs.versions.toml      catálogo de versiones (sección 5.1)
  gradle/gradle-daemon-jvm.properties   JDK 21 del daemon
  .github/workflows/ci.yml       pruebas + APK de teléfono y reloj en ubuntu; framework iOS de simulador en macos-15
  HANDOFF.md                     la bitácora corta; este manual la reemplaza
  README.md                      todavía es la plantilla de JetBrains
  androidApp/
    build.gradle.kts             com.android.application + compose; minSdk desde el catálogo
    src/main/AndroidManifest.xml sin permisos todavía
    src/main/kotlin/com/olarte/ironprogress/MainActivity.kt   enableEdgeToEdge con barras oscuras fijas y setContent { App() }
  wearApp/
    build.gradle.kts             com.android.application + composeMultiplatform + composeCompiler; applicationId com.olarte.ironprogress; minSdk 28 y targetSdk 35 escritos a mano; depende de :shared:domain, :shared:ui y compose.material3 (el del teléfono)
    src/main/AndroidManifest.xml uses-feature watch, permisos VIBRATE y WAKE_LOCK, tema DeviceDefault; sin la meta-data com.google.android.wearable.standalone
    src/main/kotlin/com/olarte/ironprogress/wear/WearMainActivity.kt   ForjaWatchApp con las dos caras de prueba
  iosApp/
    Configuration/Config.xcconfig   TEAM_ID vacío; PRODUCT_BUNDLE_IDENTIFIER=com.olarte.ironprogress.IronProgress$(TEAM_ID)
    iosApp/iOSApp.swift, ContentView.swift   ComposeView con MainViewControllerKt.MainViewController()
    iosApp/Info.plist             CFBundleLocalizations es y en
    iosApp.xcodeproj/project.pbxproj   fase de build: ./gradlew :shared:embedAndSignAppleFrameworkForXcode; IPHONEOS_DEPLOYMENT_TARGET 18.2
  shared/                        módulo paraguas: framework iOS estático "Shared"
    build.gradle.kts
    src/commonMain/kotlin/com/olarte/ironprogress/App.kt
    src/commonMain/kotlin/com/olarte/ironprogress/di/AppModule.kt          sharedAppModule
    src/commonMain/kotlin/com/olarte/ironprogress/di/PlatformModule.kt     @Composable expect fun rememberPlatformModule(): Module
    src/androidMain/kotlin/com/olarte/ironprogress/di/PlatformModule.android.kt   usa LocalContext.current.applicationContext
    src/iosMain/kotlin/com/olarte/ironprogress/di/PlatformModule.ios.kt
    src/iosMain/kotlin/com/olarte/ironprogress/MainViewController.kt
    domain/                      Kotlin puro
      src/commonMain/kotlin/com/olarte/ironprogress/domain/catalog/Exercise.kt            ExerciseId, LocalizedText, MuscleRole, ExerciseMuscle, Exercise
      .../domain/catalog/ExerciseAttributes.kt   Equipment (10), MovementPattern (21), TrackingType (7), SetField, WeightUnit
      .../domain/catalog/Muscle.kt               13 músculos con MuscleSize y BodyView; id = name.lowercase()
      .../domain/catalog/ExerciseRepository.kt   observeExercises(), findExercise(id)
      .../domain/catalog/SearchExercises.kt      ExerciseQuery, SearchExercises, ExerciseSearch.filter, String.normalizedForSearch()
      .../domain/health/HealthRepository.kt      HealthAvailability y HealthRepository
      src/commonTest/.../catalog/ExerciseTest.kt          6 pruebas
      src/commonTest/.../catalog/ExerciseSearchTest.kt    VACÍO (0 líneas) desde 4c0b5ba
    data/                        Room 3
      build.gradle.kts           (copiado completo en 6.4)
      schemas/com.olarte.ironprogress.data.local.IronProgressDatabase/1.json   identityHash 8ea8a438950c58a8f9b7502c75fb8007
      src/commonMain/kotlin/com/olarte/ironprogress/data/local/CatalogEntities.kt      ExerciseEntity, ExerciseMuscleEntity, CatalogMetadataEntity, ExerciseWithMusclesRelation, toDomain, toEntities
      .../data/local/CatalogDao.kt              @Upsert y replaceSeedCatalog transaccional
      .../data/local/IronProgressDatabase.kt    versión 1, SCHEMA_VERSION, DATABASE_NAME, expect object IronProgressDatabaseConstructor, buildIronProgressDatabase()
      .../data/catalog/RoomExerciseRepository.kt  siembra perezosa con Mutex
      .../data/catalog/SeedCatalog.kt           77 ejercicios, VERSION = 1, internal
      .../data/catalog/SeedExerciseRepository.kt  repositorio en memoria (ya no se usa en App)
      src/androidMain/kotlin/com/olarte/ironprogress/data/local/DatabaseBuilder.android.kt   createIronProgressDatabase(context)
      src/iosMain/kotlin/com/olarte/ironprogress/data/local/DatabaseBuilder.ios.kt           createIronProgressDatabase()
      src/commonTest/.../data/catalog/SeedCatalogTest.kt            5 pruebas
      src/commonTest/.../data/catalog/RoomExerciseRepositoryTest.kt 1 prueba con DAO falso
      src/androidHostTest/kotlin/android/database/SQLException.kt   shim para las pruebas de host
      src/androidHostTest/.../data/local/CatalogDatabaseTest.kt     4 pruebas con Room en memoria
      src/androidHostTest/.../data/local/CatalogMigrationTest.kt    2 pruebas desde 1.json
    health/
      src/commonMain/kotlin/com/olarte/ironprogress/health/UnavailableHealthRepository.kt   responde NOT_SUPPORTED
    ui/
      build.gradle.kts           compose + recursos (androidResources enable = true, packageOfResClass)
      src/commonMain/kotlin/com/olarte/ironprogress/ui/IronProgressApp.kt   PROTOTIPO de 1.838 líneas: ForjaNavDestination y la pila propia, isla, Hoy, Plan, Progreso, Entreno con datos fijos
      .../ui/Labels.kt                    Muscle.labelRes() y Equipment.labelRes()
      .../ui/catalog/CatalogContract.kt   CatalogState y CatalogIntent
      .../ui/catalog/CatalogViewModel.kt  MVI con stateIn(WhileSubscribed(5_000))
      .../ui/catalog/CatalogScreen.kt     búsqueda, chips de músculo, conteo con plural, lista y estado vacío
      .../ui/components/ForjaIsland.kt    isla flotante, ForjaTab, ActiveSessionBannerState
      .../ui/components/ForjaIcons.kt     íconos dibujados a mano (no son Material Symbols)
      .../ui/theme/ForjaColors.kt         ForjaColorPalette, ForjaDarkColors, ForjaLightColors, LocalForjaColors
      .../ui/theme/ForjaTypography.kt     ForjaTypography y createForjaTypography(textFamily, figureFamily) con SansSerif
      .../ui/theme/ForjaMetrics.kt        ForjaSpacing, ForjaRadius (s, m, l, xl más shapeS, shapeM, shapeL, shapeXlTop y shapeFull), ForjaMeasures y un object ForjaMotion duplicado
      .../ui/theme/ForjaMotion.kt         ForjaMotionScheme (respeta reducir movimiento), ForjaMotionTokens, LocalForjaMotion, Modifier.forjaPressScale
      .../ui/theme/ForjaHaptics.kt        interfaz ForjaHaptics, NoOpForjaHaptics, LocalForjaHaptics, expect rememberForjaHaptics
      .../ui/theme/ForjaTheme.kt          provee colores, tipografía, formas, movimiento y vibración; parámetros darkTheme = true, reduceMotion = false y hapticsEnabled = true. Usa ForjaShapes y LocalForjaShapes
      .../ui/theme/ForjaShapes.kt         NO ESTÁ EN GIT (F0-00)
      src/androidMain/.../ui/theme/ForjaHaptics.android.kt
      src/iosMain/.../ui/theme/ForjaHaptics.ios.kt
      src/commonMain/composeResources/values/strings.xml      español
      src/commonMain/composeResources/values-es/strings.xml   español (idéntico a values)
      src/commonMain/composeResources/values-en/strings.xml   inglés
```

### 4.4 Pruebas

Son 18. Con un `ForjaShapes.kt` de prueba en el sandbox de Claude, todo lo demás compila y pasa: el APK del teléfono, el APK del reloj y las 18 pruebas.

| Módulo | Archivo | Source set | Pruebas |
| --- | --- | --- | --- |
| domain | ExerciseTest | commonTest | 6 |
| domain | ExerciseSearchTest | commonTest | 0 (archivo vacío) |
| data | SeedCatalogTest | commonTest | 5 |
| data | RoomExerciseRepositoryTest | commonTest | 1 |
| data | CatalogDatabaseTest | androidHostTest | 4 |
| data | CatalogMigrationTest | androidHostTest | 2 |

Comando: `./gradlew :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest`. Las pruebas de `commonTest` corren en la JVM con esa tarea; las de `androidHostTest` necesitan el SQLite nativo del computador.

### 4.5 Real frente a prototipo

| Pieza | Estado |
| --- | --- |
| Capas domain, data, health, ui | Real |
| Catálogo de 77 ejercicios en Room 3, búsqueda ES/EN y filtro | Real |
| Koin con base por plataforma | Real (falta verlo en los teléfonos) |
| CI en GitHub Actions | Real: pruebas, APK de teléfono y reloj, framework iOS de simulador. En `d3c2101` el paso de los APK tiene que fallar por `ForjaShapes` |
| Tokens de color, medidas y tipografía Forja | Real, pero sin la fuente Archivo |
| ForjaTheme con formas, movimiento y vibración | Escrito; falta `ForjaShapes.kt` y no lee el tema ni el ajuste de reducir movimiento del sistema |
| Vibración | Conectada en el prototipo de entreno; en el teléfono falta el permiso `VIBRATE` |
| Isla flotante | Real, sin vidrio |
| Hoy, Plan, Progreso, Entreno | Prototipos con datos fijos dentro de IronProgressApp.kt |
| Navegación | Pila propia en `remember`, sin Navigation 3 |
| Idioma | Fijo en "es" |
| wearApp | Prototipo de dos caras con el Material 3 del teléfono |

### 4.6 Deuda y bugs conocidos

| # | Problema | Dónde | Ticket |
| --- | --- | --- | --- |
| 0 | **`ForjaShapes.kt` no está en git.** `:shared:ui:compileAndroidMain` falla con `Unresolved reference 'ForjaShapes'` en `ForjaTheme.kt:23` y nada compila: ni el APK del teléfono ni el del reloj ni el framework de iOS | `shared/ui/src/commonMain/.../ui/theme/` | F0-00 |
| 1 | `minSdk` 24; la especificación pide 28 (mínimo de Health Connect) | `gradle/libs.versions.toml` | F0-03 |
| 2 | Falta `VIBRATE` en el manifest del teléfono. `restFinished()` usa `Vibrator.vibrate`; sin el permiso lanza `SecurityException`, que `runCatching` atrapa y cae a `performHapticFeedback`. No hay crash, pero no sale el patrón 0, 180, 90, 180 | `androidApp/src/main/AndroidManifest.xml` | F0-03 |
| 3 | `ExerciseSearchTest.kt` vacío desde que se creó | `shared/domain/src/commonTest/...` | F0-02 |
| 4 | `values/` y `values-es/` iguales en español. Un teléfono en francés ve la interfaz en español, pero `LocalizedText.resolve` le da los nombres en inglés | `composeResources` | F0-04 |
| 5 | `languageCode = "es"` fijo y textos escritos en el código ("Hoy", "Plan", "Progreso", "Buscar ejercicios", "Serie x de 4") | `IronProgressApp.kt` | F0-04 |
| 6 | `ForjaTheme(darkTheme = true)` fijo en IronProgressApp y en WearMainActivity; `reduceMotion` no lee el ajuste del sistema | `ForjaTheme.kt`, `IronProgressApp.kt` | F0-06 |
| 7 | Dos sistemas de movimiento: `object ForjaMotion` (en ForjaMetrics.kt) y `ForjaMotionScheme` con `ForjaMotionTokens` | `ForjaMetrics.kt`, `ForjaMotion.kt` | F0-06 |
| 8 | Accessors `compose.*` deprecados desde CMP 1.10 (en `shared`, `shared:ui` y `wearApp`) y coordenadas de lifecycle escritas a mano | `build.gradle.kts` de esos módulos | F0-05 |
| 9 | Tipografía con `FontFamily.SansSerif` | `ForjaTypography.kt` | F0-07 |
| 10 | Íconos dibujados a mano | `ForjaIcons.kt` | F0-08 |
| 11 | Pila de navegación propia en `remember` (se pierde al girar y al cierre del sistema) y `CatalogViewModel` creado con `viewModel { }` a mano aunque `koin-compose-viewmodel` ya está en el catálogo | `IronProgressApp.kt` | F0-09 |
| 12 | Sin vidrio en la isla | `ForjaIsland.kt` | F0-10 |
| 13 | wearApp con `compose.material3` (el del teléfono) en vez de Wear Compose, `minSdk` 28 (Health Services pide 30), sin la meta-data `standalone` y contando segundos con `delay` | `wearApp/` | F0-11 |
| 14 | Re-siembra: `replaceSeedCatalog` hace upsert de los ejercicios semilla. Si el usuario archiva uno, subir `SeedCatalog.VERSION` le quitaría el archivado | `CatalogDao.kt`, `RoomExerciseRepository.kt` | F0-12 |
| 15 | Sin ktlint, sin cobertura, y el CI no enlaza `iosArm64` (el del iPhone) | `ci.yml` | F0-13 |
| 16 | `MainActivity` fija barras del sistema oscuras | `MainActivity.kt` | F0-06 |
| 17 | README de plantilla; `TEAM_ID` vacío en `Config.xcconfig` | | F0-14 |
| 18 | Prototipo de entreno: "Completar serie" deja pasar a una serie 5 (`if (currentSetNumber <= 4)` suma hasta 5), el aviso de Deshacer nunca se oculta solo a los 5 s, y tocar otra vez una pestaña sube las tres listas a la vez | `IronProgressApp.kt` | No se arreglan: F1-10 reemplaza ese código. No copiar esa lógica |
| 19 | `IPHONEOS_DEPLOYMENT_TARGET` 18.2 en el proyecto; la especificación propone iOS 17 | `project.pbxproj` | Decisión abierta 13.10 |
| 20 | `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` quedó activado sin usarse. La convención sigue siendo `project(":shared:domain")` (6.1) | `settings.gradle.kts` | Sin ticket; no mezclar los dos estilos |
| 21 | `ForjaTheme.kt` trae en un comentario un `[cite: 1]` que quedó al copiar texto de otro chat | `ForjaTheme.kt` | F0-06 (se borra) |

### 4.7 Correcciones a la bitácora corta de `d3c2101`

| Lo que dice esa bitácora | Lo que hay de verdad en `d3c2101` |
| --- | --- |
| "FASE 0: 100 % COMPLETADA" | Ninguno de los tickets de F0 (13.3, del F0-00 al F0-14) está cerrado: F0-06, F0-09 y F0-11 van a medias y los demás no han empezado. Además, el commit no compila |
| Sistema Forja completo, con `ForjaShapes` | `ForjaShapes.kt` no está en git; `ForjaTheme` lo usa y el build falla. La bitácora del 23 de octubre ya lo había marcado como inexistente |
| 23 pruebas unitarias en verde | 18; `ExerciseSearchTest` sigue vacío |
| `:wearApp` con API 35 y `minSdk = 28` | Así está, pero Health Services pide `minSdk` 30 (verificado en su AAR) y la UI usa el Material 3 del teléfono, no Wear Compose |
| Pila de navegación | Es propia, en `remember`; no es Navigation 3 ni sobrevive a girar el teléfono |
| Juan sin iPhone físico | Tiene un iPhone 18 Pro y prueba iOS en el teléfono |
| "Siguiente fase: FASE 1" con su lista | Primero se cierra F0 (13.3), empezando por F0-00. F1 cambió con el plan v3 (13.4) |

**Lección, por segunda vez:** la bitácora solo registra lo que se ve en `git log`, `git ls-files`, `git status` y la salida de Gradle o del CI. Un archivo que existe en el computador de Juan pero no en git no existe para nadie más.

### 4.8 Bitácora de tickets

| Fecha | Ticket | Rama y commit | Resultado verificado |
| --- | --- | --- | --- |
| 2026-10-21 | Capas y sistema Forja | `main`, `d70fad6` | Fusionado en main |
| 2026-10-22 | ForjaMotion, ForjaHaptics, CI | rama de trabajo, `8d74639` | En la rama |
| 2026-10-23 | Room 3 en verde | rama de trabajo, `259ff91` y `d43c9bc` | 18 pruebas en verde; APK debug compila |
| 2026-10-23 | Koin con Room 3 | rama de trabajo, `8da5805` | Falta confirmar en los teléfonos (F0-01) |
| 2026-10-23 | Navegación propia, vibración en el prototipo y wearApp | rama de trabajo, `d3c2101` | No compila: falta `ForjaShapes.kt`. Con un archivo de prueba compilan los dos APK y pasan las 18 pruebas (Claude, 25 oct) |

---

## 5. Versiones y dependencias

### 5.1 En uso (`gradle/libs.versions.toml` en `d3c2101`, sin cambios desde `8da5805`)

| Pieza | Versión | Nota |
| --- | --- | --- |
| Gradle | 9.5.1 | wrapper |
| JDK del daemon | 21 (Azul) | `gradle-daemon-jvm.properties`; el CI usa Zulu 21 |
| AGP | 9.1.1 | app en su propio módulo `androidApp`; Kotlin integrado en AGP 9 (sin plugin kotlin-android) |
| Kotlin | 2.4.20 | también el plugin de Compose Compiler |
| Compose Multiplatform | 1.12.1 | existe 1.12.2; no se sube sin ticket |
| compose material3 (JetBrains) | 1.12.0-alpha03 | |
| lifecycle (JetBrains) | 2.11.0 | viewmodel, viewmodel-compose, runtime-compose |
| activity | 1.13.0 | |
| core | 1.19.0 | |
| kotlinx-coroutines | 1.11.0 | |
| kotlinx-serialization | 1.12.0 | hoy solo `-json` en pruebas de data |
| KSP | 2.3.6 | formato KSP2, sin prefijo de Kotlin |
| Room 3 | 3.0.3 | paquete `androidx.room3`, plugin `androidx.room3` |
| SQLite (androidx) | 2.7.1 | Room 3.0.3 trae 2.7.1 y Gradle alinea todo `androidx.sqlite` a esa versión |
| Koin | 4.1.0 | `koin-core`, `koin-compose`, `koin-compose-viewmodel` (este último aún sin usar). Existe 4.2.2; no se sube sin ticket |
| compileSdk / targetSdk | 37 / 37 | Play exige al menos 36 en teléfono. wearApp escribe a mano `targetSdk = 35` (F0-11 lo pasa al catálogo) |
| minSdk | 24 | debe pasar a 28 (F0-03). wearApp escribe a mano 28 y Health Services pide 30 (F0-11) |
| JVM target | 11 | |

### 5.2 Para los tickets de F0 (verificadas en Maven Central y Google Maven el 23 de octubre de 2026)

| Pieza | Coordenadas | Versión | Ticket |
| --- | --- | --- | --- |
| Navigation 3 UI (CMP) | `org.jetbrains.androidx.navigation3:navigation3-ui` | 1.1.2 | F0-09 |
| ViewModel por entrada de Nav 3 | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3` | 2.11.0 (la de lifecycle) | F0-09 |
| Serialización (núcleo) | `org.jetbrains.kotlinx:kotlinx-serialization-core` | 1.12.0 | F0-09 |
| Plugin de serialización | id `org.jetbrains.kotlin.plugin.serialization` | la de Kotlin (2.4.20) | F0-09 |
| Haze | `dev.chrisbanes.haze:haze` y `dev.chrisbanes.haze:haze-blur` | 2.0.1 (compilada contra Compose 1.12.1) | F0-10 |
| Wear Compose | `androidx.wear.compose:compose-material3` y `androidx.wear.compose:compose-foundation` | 1.7.1 (minSdk 25) | F0-11 |
| ktlint para Gradle | id `org.jlleitschuh.gradle.ktlint` | 14.2.0 | F0-13 |
| Kover (cobertura) | id `org.jetbrains.kotlinx.kover` | 0.9.9 | F0-13 |

Líneas exactas para agregar al catálogo cuando llegue cada ticket (no antes):

```toml
[versions]
navigation3 = "1.1.2"
haze = "2.0.1"
wear-compose = "1.7.1"
wear-minSdk = "30"
wear-targetSdk = "35"
ktlint-gradle = "14.2.0"
kover = "0.9.9"

[libraries]
androidx-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
androidx-lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "androidx-lifecycle" }
androidx-lifecycle-viewmodelNavigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "androidx-lifecycle" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinx-serialization" }
haze-core = { module = "dev.chrisbanes.haze:haze", version.ref = "haze" }
haze-blur = { module = "dev.chrisbanes.haze:haze-blur", version.ref = "haze" }
wear-compose-material3 = { module = "androidx.wear.compose:compose-material3", version.ref = "wear-compose" }
wear-compose-foundation = { module = "androidx.wear.compose:compose-foundation", version.ref = "wear-compose" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint-gradle" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```

### 5.3 Para fases futuras (comprobar de nuevo al empezar la fase)

| Pieza | Coordenadas | Última vista el 23 oct 2026 | Fase |
| --- | --- | --- | --- |
| kotlinx-datetime | `org.jetbrains.kotlinx:kotlinx-datetime` | 0.8.0 | F1 |
| Turbine (pruebas de Flow) | `app.cash.turbine:turbine` | 1.2.1 | F1 |
| DataStore Preferences | `androidx.datastore:datastore-preferences-core` | 1.3.0 | F1 |
| supabase-kt (BOM) | `io.github.jan-tennert.supabase:bom` | 3.8.0 | F3 |
| PowerSync | `com.powersync:core` | 1.15.2 | F3 |
| Ktor client | `io.ktor:ktor-client-core` | 3.6.0 | F3 |
| Health Connect | `androidx.health.connect:connect-client` | 1.1.0 (minSdk 26) | F4 |
| Health Services (reloj) | `androidx.health:health-services-client` | 1.1.0 (minSdk 30) | F4 |
| Wearable Data Layer | `com.google.android.gms:play-services-wearable` | comprobar | F4 |
| Wear Tiles y ProtoLayout | `androidx.wear.tiles:tiles` y `androidx.wear.protolayout:protolayout` | comprobar | F4 |
| Actividad en curso del reloj | `androidx.wear:wear-ongoing` | comprobar | F4 |
| Samsung Health Sensor SDK y Data SDK | se descargan de Samsung Developers (no están en Maven) | comprobar | Etapa 3 |
| HealthKit, ActivityKit, WidgetKit, App Intents, WatchConnectivity, AVFoundation | vienen con Xcode | del sistema | F1 y F4 |
| detekt | `dev.detekt` 2.0 | solo alpha (2.0.0-alpha.6); detekt 1.23 no soporta Kotlin 2.4 | cuando salga estable |

---

## 6. Configuración de Gradle que funciona

### 6.1 Reglas

- Las librerías KMP usan `alias(libs.plugins.androidMultiplatformLibrary)` (id `com.android.kotlin.multiplatform.library`). Nunca `com.android.library` ni nombres inventados como `androidKotlinMultiplatformLibrary`.
- El bloque Android de una librería KMP va dentro de `kotlin { android { ... } }` con `namespace`, `compileSdk`, `minSdk` y `compilerOptions { jvmTarget }`.
- Las pruebas de JVM se activan con `withHostTest {}` dentro de `android { }`. La tarea es `testAndroidHostTest` y el source set de pruebas solo Android es `androidHostTest`.
- Dependencias entre módulos con `project(":shared:domain")`. Nunca `projects.shared.domain`: aunque `d3c2101` activó `TYPESAFE_PROJECT_ACCESSORS` en `settings.gradle.kts`, nadie los usa y no se mezclan estilos (4.6, fila 20).
- Los recursos de Compose viven en `shared:ui`, que necesita `androidResources { enable = true }` y `compose.resources { packageOfResClass = "com.olarte.ironprogress.ui.resources" }`.
- `:shared` es el paraguas: produce el framework iOS estático `Shared` y lleva el plugin de Compose para empaquetar en iOS los recursos de `ui`.
- La app Android (`androidApp`) usa `com.android.application` y el plugin de Compose Compiler, con `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }` porque AGP 9 trae Kotlin integrado.
- wearApp es una app Android como androidApp: `com.android.application` y Compose Compiler, sin el plugin de Compose Multiplatform y con Wear Compose (F0-11).
- `configuration-cache` está activo: dentro de acciones de tareas no se usa `project`. Para pasar rutas a las pruebas se calcula un `String` antes (como `roomSchemasPath` en 6.4).

### 6.2 `settings.gradle.kts` (actual, `d3c2101`)

```kotlin
rootProject.name = "IronProgress"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":wearApp")
include(":shared")
include(":shared:domain")
include(":shared:data")
include(":shared:health")
include(":shared:ui")
```

### 6.3 Plantilla de módulo KMP sin Compose (`shared/domain/build.gradle.kts`, actual)

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de dominio: modelos, casos de uso y reglas del entreno. Kotlin puro: no conoce
// Android, iOS ni la base de datos.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.olarte.ironprogress.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

### 6.4 Módulo con Room 3 (`shared/data/build.gradle.kts`, actual y en verde)

```kotlin
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
```

### 6.5 Módulo paraguas (`shared/build.gradle.kts`, actual)

```kotlin
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
            implementation(project(":shared:ui"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
    }
}
```

### 6.6 Inyección con Koin (como está en `d3c2101`, sin cambios desde `8da5805`)

```kotlin
// shared/src/commonMain/kotlin/com/olarte/ironprogress/App.kt
@Composable
fun App() {
    val platformModule = rememberPlatformModule()
    KoinApplication(
        application = {
            modules(platformModule, sharedAppModule)
        },
    ) {
        val exerciseRepository: ExerciseRepository = koinInject()
        IronProgressApp(exerciseRepository = exerciseRepository)
    }
}

// shared/src/commonMain/kotlin/com/olarte/ironprogress/di/AppModule.kt
val sharedAppModule = module {
    single<CatalogDao> { get<IronProgressDatabase>().catalogDao() }
    single<ExerciseRepository> { RoomExerciseRepository(get()) }
    single<HealthRepository> { UnavailableHealthRepository() }
    factory { SearchExercises(get()) }
}

// shared/src/androidMain/kotlin/com/olarte/ironprogress/di/PlatformModule.android.kt
@Composable
actual fun rememberPlatformModule(): Module {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        module { single { createIronProgressDatabase(appContext) } }
    }
}

// shared/src/iosMain/kotlin/com/olarte/ironprogress/di/PlatformModule.ios.kt
@Composable
actual fun rememberPlatformModule(): Module =
    remember { module { single { createIronProgressDatabase() } } }
```

Este arranque vive dentro de Compose. F1-13 lo saca a un `startKoin` que corre antes de la UI, porque el servicio del descanso en Android y los botones de la Live Activity en iPhone necesitan Koin y la base sin pantalla.

### 6.7 `wearApp/build.gradle.kts` (actual, `d3c2101`; F0-11 lo reemplaza)

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.olarte.ironprogress.wear"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.olarte.ironprogress"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":shared:domain"))
    implementation(project(":shared:ui"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(libs.androidx.activity.compose)
}
```

Lo que está mal: el plugin `composeMultiplatform` y los accessors `compose.*` (deprecados), `compose.material3` del teléfono en vez de Wear Compose, y `minSdk` y `targetSdk` escritos a mano. La plantilla correcta está en F0-11 (13.3).

---

## 7. Errores que ya pasaron y cómo no repetirlos

| # | Síntoma | Causa | Cómo se hace bien |
| --- | --- | --- | --- |
| 1 | `Unresolved reference: configureForjaDatabase` o `getDatabaseBuilder` | Gemini inventó nombres de funciones | Las funciones reales son `createIronProgressDatabase(context)` en Android y `createIronProgressDatabase()` en iOS. Antes de llamar algo, búscalo con `grep -rn` |
| 2 | `Unresolved reference: androidContext` | `androidContext()` es de `koin-android`, que el proyecto no usa | En el módulo de plataforma se toma `LocalContext.current.applicationContext` (6.6); desde F1-13, el `Context` de `Application` se le pasa a mano al módulo |
| 3 | Imports `androidx.room.*` no resuelven | Room 3 cambió de paquete | Todo es `androidx.room3.*` (Entity, Dao, Upsert, Room, RoomDatabase, ConstructedBy). El plugin es `androidx.room3` y la extensión es `room3 { schemaDirectory(...) }` |
| 4 | KSP: "Cannot have empty 'parentColumns' in @Relation" | En Room 3 `@Relation` usa arreglos | `@Relation(parentColumns = ["id"], entityColumns = ["exercise_id"])` |
| 5 | Error de versión de KSP | Se usó el formato viejo `2.x.y-1.0.z` | KSP2 usa su propia versión: `2.3.6` |
| 6 | Plugin Android no encontrado o mezcla de plugins | `com.android.library` o un alias inventado | `alias(libs.plugins.androidMultiplatformLibrary)` |
| 7 | `projects.shared.domain` no existe o se mezcla con `project(...)` | Los typesafe project accessors no se usan (`d3c2101` los activó en settings, pero nadie los llama) | `project(":shared:domain")` en todos lados |
| 8 | Pruebas en `commonMain` rompen la compilación (24 o 66 errores) | Se pegó el archivo en la pestaña de un archivo que estaba en `commonMain`, o el IDE lo recreó | Las pruebas comunes van en `src/commonTest`, las que usan Room real o `java.io` en `src/androidHostTest`. Cierra las pestañas de archivos borrados antes de pegar. Para mover: `git mv -f origen destino` |
| 9 | `UnsatisfiedLinkError: no sqliteJni` en pruebas | Las pruebas de host corren en la JVM del Mac y no tienen el SQLite nativo | `runtimeOnly(libs.androidx.sqlite.bundled.jvm)` en `androidHostTest` (ya está) |
| 10 | `@Upsert` falla en pruebas de host con una excepción sin mensaje | El `android.jar` de pruebas trae un `android.database.SQLException` vacío y Room necesita su mensaje | El shim `shared/data/src/androidHostTest/kotlin/android/database/SQLException.kt` (ya está). No lo borres |
| 11 | Conflicto de versiones de `androidx.sqlite` | Se pidió 2.6.0 | Room 3.0.3 trae 2.7.1 y Gradle alinea todo; el catálogo usa 2.7.1 |
| 12 | Archivos duplicados en paquetes distintos (`CatalogDao` en `local.entity.dao`, stubs vacíos en `:shared`) | "New Kotlin Class" del IDE y pegados en el lugar equivocado | Todo Room vive en `com.olarte.ironprogress.data.local`. Antes de crear un archivo, `find shared -name "Nombre*.kt"` |
| 13 | `zsh: no matches found` | Glob sin comillas | `--include="*.kt"` |
| 14 | `zsh: command not found: #` | Se pegaron comentarios en la terminal | Bloques de comandos sin líneas `#` |
| 15 | La bitácora decía cosas que no estaban en el repo (pasó dos veces) | Se escribió lo planeado como si estuviera hecho | Solo se registra lo verificado con `git log`, `git ls-files` y la salida de Gradle o del CI (4.7) |
| 16 | El test de repositorio no compilaba | Imports del paquete viejo y `insertExerciseMuscles` en vez de `upsertExerciseMuscles` | Copia los nombres del DAO real (`CatalogDao.kt`) |
| 17 | `Dispatchers.IO` no resuelve en iOS | En Kotlin/Native hace falta el import de extensión | `import kotlinx.coroutines.IO` (ya está en IronProgressDatabase.kt) |
| 18 | Riesgo: API de Haze 1.x con Haze 2 | Haze 2.0 renombró todo | Con 2.0.1: `hazeSource`, `hazeBlur(input = HazeInput.Sources(state), style = HazeBlurStyle { ... })`, `HazeColorEffect.tint(...)`. Ya no existen `hazeEffect(state, style)`, `HazeStyle` ni `HazeTint` (12.6) |
| 19 | Riesgo: crash en iOS al guardar la pila de navegación | `rememberNavBackStack(Route.A)` sin configuración usa reflexión, que no existe en iOS | Siempre `rememberNavBackStack(configuration, ...)` con un `SavedStateConfiguration` que registre las rutas (F0-09) |
| 20 | Riesgo: DSL de ViewModel de Koin deprecado | `org.koin.compose.viewmodel.dsl.viewModel` está marcado `@Deprecated` en 4.1.0 | Usa `import org.koin.core.module.dsl.viewModel` y en pantalla `org.koin.compose.viewmodel.koinViewModel` |
| 21 | Riesgo: wearApp no recibe mensajes del teléfono | El Wearable Data Layer exige el mismo `applicationId` y la misma firma en teléfono y reloj | wearApp usa `applicationId = "com.olarte.ironprogress"` |
| 22 | Riesgo: wearApp no instala o no compila con Health Services | Health Services pide API 30 y `d3c2101` dejó 28 | `minSdk = 30` y `targetSdk = 35` desde el catálogo (F0-11) |
| 23 | Riesgo: borrar datos al cambiar el esquema | Room sin migración destruye o falla | Nunca `fallbackToDestructiveMigration`. Cada cambio de entidad sube la versión, agrega migración y prueba (8.6) |
| 24 | Riesgo: `@Serializable` sin efecto | Falta el plugin de serialización en el módulo | `alias(libs.plugins.kotlinSerialization)` en el módulo que declara las clases serializables |
| 25 | Riesgo: ktlint marca todas las funciones `@Composable` | La regla de nombres de funciones exige minúscula | `.editorconfig` con `ktlint_function_naming_ignore_when_annotated_with = Composable` (F0-13) |
| 26 | `Unresolved reference 'ForjaShapes'` en `ForjaTheme.kt:23` y nada compila | El archivo se creó en el Mac pero no entró al commit (`d3c2101`) | `git status` antes del commit y `git show --stat HEAD` después; los archivos nuevos van en `git add` con su ruta; no se fusiona sin el CI en verde (F0-00) |
| 27 | Riesgo: el reloj con componentes del teléfono | wearApp usó `compose.material3` del teléfono | En el reloj, Wear Compose Material 3 (`androidx.wear.compose:compose-material3`); de `shared:ui` solo los colores y la tipografía (F0-11) |
| 28 | Riesgo: cronómetros que se desfasan o se congelan | Un bucle con `delay(1000)` acumula error y se detiene con la pantalla apagada | Todo sale del motor de tiempo, calculado desde instantes (11.9) |
| 29 | Riesgo: la Live Activity no avanza o gasta batería | Actualizarla cada segundo; iOS limita las actualizaciones | El contador lo dibuja el sistema con `Text(timerInterval:)`; solo se actualiza al cambiar de fase (RNF-15) |
| 30 | Riesgo: los botones de la isla o el servicio del descanso fallan sin pantalla | Koin y la base arrancan dentro de `App()`, en Compose | `startKoin` antes de la UI (F1-13): para un `LiveActivityIntent` el sistema abre la app sin mostrarla |
| 31 | Riesgo: la voz pausa la música | Sesión de audio sin `duckOthers` o foco de audio sin atenuación | iPhone: `AVAudioSession` con `duckOthers`; Android: `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`; se devuelve el foco al terminar cada frase (F1-17) |
| 32 | Riesgo: un valor nuevo de enum rompe lo guardado | Insertarlo en medio, renombrar o borrar | Los valores nuevos van al final y se guardan como texto (10.5) |
| 33 | Riesgo: "CrossFit" o "HYROX" en un texto de la app | Son marcas registradas | "Funcional" y "carrera fitness" (3) |
| 34 | Riesgo: la Live Update no sale en la Now Bar | Notificación coloreada, con vistas propias, sin título o que no está en curso | Las condiciones de F1-14; `NotificationManager.canPostPromotedNotifications()` dice si se puede |
| 35 | Restos de otros chats en el código (`[cite: 1]` en `ForjaTheme.kt`) | Se pegó texto de Gemini con sus citas | Revisar el diff antes del commit; `grep -rn "\[cite" shared wearApp` no debe encontrar nada |

---

## 8. Arquitectura y convenciones de código

### 8.1 Módulos

```
androidApp  -->  shared (paraguas: DI y framework iOS "Shared")
iosApp      -->  shared (más la extensión de widgets con la Live Activity, en Swift, desde F1-16)
wearApp     -->  shared:domain y shared:ui (de ui, solo colores y tipografía de Forja)
watchApp    -->  Swift; habla con iosApp por WatchConnectivity (si usa shared:domain se decide en F4-10)
shared      -->  shared:ui      (Compose, MVI, Forja, navegación, textos ES/EN, voz)
shared      -->  shared:data    (Room 3, repositorios, preferencias, sync, importadores, catálogo)
shared      -->  shared:health  (Health Connect / HealthKit)
shared      -->  shared:domain  (Kotlin puro: catálogo, rutinas, sesión y los motores)

shared:ui      -->  shared:domain
shared:data    -->  shared:domain
shared:health  -->  shared:domain
shared:domain no depende de ningún otro módulo del proyecto.
```

| Módulo | Responsabilidad | Tecnología |
| --- | --- | --- |
| shared:domain | Modelos, casos de uso y los motores: progresión, adaptación antes y durante, tiempo, exigencia, volumen y 1RM | Kotlin puro, coroutines, kotlinx-datetime |
| shared:data | Base local, repositorios, preferencias, sincronización, importadores CSV y catálogo semilla | Room 3 con BundledSQLiteDriver, DataStore, PowerSync, supabase-kt, kotlinx-serialization |
| shared:health | Interfaz de salud con implementación por plataforma | Health Connect y HealthKit |
| shared:ui | Pantallas, ViewModels MVI, Forja, navegación, textos y voz (`expect`/`actual`) | Compose Multiplatform, Navigation 3, Koin, recursos de Compose |
| androidApp | Actividad, servicio de la sesión activa, Live Update, notificaciones, permisos, receptor del reloj | Android SDK, Wearable Data Layer |
| iosApp | Contenedor SwiftUI, notificaciones locales, permisos de HealthKit; extensión de widgets con la Live Activity y los App Intents | Xcode, Swift, ActivityKit, WidgetKit, App Intents |
| wearApp | Rutina del día, serie, cronómetro, intervalos, pulso en vivo y vibración cuando estás recuperado; Tile y actividad en curso | Compose for Wear OS, Health Services, Wearable Data Layer |
| watchApp (nuevo, F4) | Lo mismo en el Apple Watch, aunque el iPhone no esté cerca; la Live Activity llega sola al Smart Stack | SwiftUI, HealthKit (`HKWorkoutSession`), WatchConnectivity |

Decisiones de arquitectura:

- **MVI por pantalla:** estado inmutable en `StateFlow`, intenciones como `sealed interface` y efectos de una sola vez por `Channel`.
- **Sesión activa:** cada cambio se escribe en Room al instante; si el sistema cierra la app, la pantalla se reconstruye desde la base.
- **Tiempo:** un solo motor en `shared:domain` (11.9). En Android, servicio en primer plano de tipo `health` con Live Update; en iOS, Live Activity con el contador del sistema y una notificación local a la hora de fin como respaldo.
- **Estado de la sesión hacia Swift:** una interfaz Kotlin en `shared` que Swift implementa y Koin inyecta; así la Live Activity no duplica la lógica.
- **Arranque:** Koin y la base arrancan antes de la UI (F1-13), porque el servicio del descanso y los botones de la isla corren sin pantalla.
- **Relojes:** el Galaxy Watch lee el pulso con Health Services y lo manda por `MessageClient`; el Apple Watch usa `HKWorkoutSession`, reflejada en el iPhone. El teléfono decide cuándo estás recuperado y qué ajustar; los dos relojes corren la sesión aunque el teléfono no esté cerca y sincronizan al reconectar.
- **Adaptación:** reglas con explicación, sin IA, antes y durante la sesión (11.6 y 11.8).
- **Nube:** Supabase (cuentas, Postgres con Row Level Security y almacenamiento) más PowerSync.
- **Capa Pro futura:** cada función se activa por un permiso de uso (entitlement), sin reescribir pantallas.

### 8.2 Paquetes

| Módulo | Paquete raíz | Subpaquetes |
| --- | --- | --- |
| shared | `com.olarte.ironprogress` | `di` |
| shared:domain | `com.olarte.ironprogress.domain` | `catalog`, `health`, y por venir `routine`, `session`, `time`, `progression`, `adaptation`, `analytics`, `readiness`, `screening`, `settings` |
| shared:data | `com.olarte.ironprogress.data` | `local` (todo Room: entidades, DAOs, base), `catalog`, y por venir `routine`, `session`, `settings`, `sync`, `importer` |
| shared:health | `com.olarte.ironprogress.health` | |
| shared:ui | `com.olarte.ironprogress.ui` | `theme`, `components`, `navigation`, `di`, `voice`, y una carpeta por pantalla: `catalog`, `today`, `plan`, `progress`, `workout`, `rest`, `intervals`, `functional`, `flow`, `breathing`, `disciplines`, `adaptation`, `history`, `settings`, `onboarding`, `screening` |
| Recursos | `com.olarte.ironprogress.ui.resources` (clase `Res`) | |
| androidApp | `com.olarte.ironprogress` | `session` (servicio), `notifications` (incluida la Live Update) |
| wearApp | `com.olarte.ironprogress.wear` | |
| iosApp y watchApp | Swift, sin paquete; carpetas por función (`LiveActivity`, `Intents`, `Health`) | |

### 8.3 Dónde va cada archivo

| Qué | Dónde |
| --- | --- |
| Código común | `src/commonMain/kotlin/...` |
| Código solo Android | `src/androidMain/kotlin/...` |
| Código solo iOS | `src/iosMain/kotlin/...` |
| Archivos `expect` / `actual` | `X.kt` en commonMain; `X.android.kt` en androidMain; `X.ios.kt` en iosMain |
| Pruebas puras (dominio, ViewModels, repositorios con fakes) | `src/commonTest/kotlin/...` |
| Pruebas con Room real, `java.io` o el esquema JSON | `src/androidHostTest/kotlin/...` (solo `shared:data`) |
| Textos | `shared/ui/src/commonMain/composeResources/values/strings.xml` (inglés, por defecto) y `values-es/strings.xml` (español) |
| Fuentes | `shared/ui/src/commonMain/composeResources/font/` |
| Íconos e imágenes | `shared/ui/src/commonMain/composeResources/drawable/` |
| Esquemas de Room | `shared/data/schemas/` (los genera KSP; se versionan en git; nunca se editan a mano) |
| Voz grabada | `shared/ui/src/commonMain/composeResources/files/voice/es/` y `.../en/` (se lee con `Res.readBytes`) |
| Código Swift del iPhone | `iosApp/iosApp/` |
| Live Activity y widgets | Un target nuevo de Xcode dentro de `iosApp/` (lo crea F1-16) |
| App del Apple Watch | Un target nuevo de Xcode (lo crea F4-11) |
| App del Galaxy Watch | `wearApp/src/main/kotlin/com/olarte/ironprogress/wear/` |

### 8.4 Plantilla MVI

```kotlin
// ui/<pantalla>/<Pantalla>Contract.kt
data class TodayState(
    val isLoading: Boolean = true,
    val routineName: String? = null,
)

sealed interface TodayIntent {
    data object StartWorkout : TodayIntent
}

sealed interface TodayEffect {
    data class OpenWorkout(val sessionId: String) : TodayEffect
}

// ui/<pantalla>/<Pantalla>ViewModel.kt
class TodayViewModel(
    private val observeNextRoutine: ObserveNextRoutine,
) : ViewModel() {

    private val _effects = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = _effects.receiveAsFlow()

    val state: StateFlow<TodayState> = observeNextRoutine()
        .map { routine -> TodayState(isLoading = false, routineName = routine?.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    fun onIntent(intent: TodayIntent) { /* ... */ }
}

// ui/<pantalla>/<Pantalla>Screen.kt
@Composable
fun TodayRoute(onOpenWorkout: (String) -> Unit) {
    val viewModel = koinViewModel<TodayViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TodayEffect.OpenWorkout -> onOpenWorkout(effect.sessionId)
            }
        }
    }
    TodayScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun TodayScreen(state: TodayState, onIntent: (TodayIntent) -> Unit) { /* sin lógica, sin Koin */ }
```

Reglas: el `Screen` no conoce Koin ni ViewModels (así se puede previsualizar y probar); el `Route` conecta. Los casos de uso viven en domain y se inyectan.

### 8.5 Koin

- Módulos: `rememberPlatformModule()` (base por plataforma), `sharedAppModule` (repositorios y casos de uso, en `:shared`) y, desde F0-09, `uiModule` (ViewModels, en `shared:ui/di/UiModule.kt`).
- ViewModels: `viewModel { CatalogViewModel(get(), ...) }` con `import org.koin.core.module.dsl.viewModel`. Con parámetros: `viewModel { (languageCode: String) -> CatalogViewModel(get(), languageCode) }` y en pantalla `koinViewModel<CatalogViewModel>(key = "catalog-$languageCode") { parametersOf(languageCode) }`.
- Repositorios: `single`. Casos de uso: `factory`.
- Nada de `koin-android` ni `androidContext()`.
- Desde F1-13, Koin arranca con `startKoin` antes de la UI (6.6) y el módulo de plataforma de Android recibe el `Context` como parámetro.

### 8.6 Room 3

- Todo lo de Room va en `com.olarte.ironprogress.data.local`: entidades, DAOs, base, conversiones `toDomain()` y `toEntities()` (internal).
- Tablas en `snake_case` y plural (`exercises`, `exercise_muscles`); columnas en `snake_case` con `@ColumnInfo(name = ...)`.
- Enums se guardan como texto estable (`Muscle.id`, `name` del enum). No se renombran valores de enum guardados.
- Pesos en kg (`REAL`), distancias en metros, tiempos en segundos, instantes en milisegundos desde época (`INTEGER`).
- **Cambiar el esquema:** sube `version` en `@Database` y `SCHEMA_VERSION` a la vez, agrega la migración (`@AutoMigration(from = N, to = N + 1)` cuando Room puede inferirla, o una `Migration` escrita), compila para que KSP exporte `N+1.json`, versiona ese JSON y amplía `CatalogMigrationTest` (crear la base desde `N.json`, insertar filas, abrir con el código actual y verificar que siguen). Nunca `fallbackToDestructiveMigration`, nunca editar un JSON exportado.
- Consultas fuera del hilo principal: el builder ya fija `Dispatchers.IO`. DAOs con `suspend` o `Flow`.
- La especificación pide una copia local de la base antes de migrar (RNF-03): se implementa en F1-03.
- Las listas que viven en una columna (respuestas del PAR-Q+, señales y cambios de la adaptación, fases de respiración) van como JSON de texto con un `@Serializable` del dominio y su prueba de ida y vuelta (10.3).

### 8.7 Tiempo, ids y unidades

- Ids de lo que crea el usuario: UUID v7 generados en el dispositivo con `kotlin.uuid.Uuid.generateV7()` (desde Kotlin 2.4; requiere `@OptIn(ExperimentalUuidApi::class)`), guardados como texto. El catálogo semilla usa slugs fijos en inglés (`barbell-bench-press`).
- Instantes: `kotlin.time.Instant` y `kotlin.time.Clock` (en kotlinx-datetime 0.7 y posteriores ya no existen `kotlinx.datetime.Instant` ni `kotlinx.datetime.Clock`). Si el compilador pide `@OptIn(ExperimentalTime::class)`, se agrega. Fechas de calendario con `kotlinx.datetime.LocalDate`.
- Para probar, el tiempo se inyecta (`Clock`), nunca se lee directo en el dominio.
- Todo se guarda en kg; la unidad preferida (por ejercicio o general) solo cambia cómo se muestra. 1 lb = 0,45359237 kg.
- Motor de tiempo: los instantes van en milisegundos desde época, tomados del `Clock` inyectado, para que el teléfono, la Live Activity y los relojes hablen del mismo instante; las duraciones del plan, en segundos (11.9).

### 8.8 Textos e idioma

- Cero textos visibles en el código: todo va a `strings.xml` con su traducción. `values/` es inglés (por defecto) y `values-es/` español.
- El idioma de los datos (nombres de ejercicios) sale de `androidx.compose.ui.text.intl.Locale.current.language` y se pasa a `LocalizedText.resolve`.
- Números, pesos y fechas con formato local. Kotlin común no trae formateador: se hace un `expect`/`actual` en `shared:ui` (Android con `java.text.NumberFormat`, iOS con `NSNumberFormatter`). Nunca `toString()` para mostrar un peso.
- iOS: `Info.plist` ya trae `CFBundleLocalizations` con `es` y `en`; sin eso iOS le reporta inglés a la app.
- Las frases de la voz también van en `strings.xml` (español e inglés) con las reglas de 3; la voz grabada usa los mismos ids como nombre de archivo.

### 8.9 UI con Forja

- Colores solo desde `ForjaTheme.colors`; tipografía desde `ForjaTheme.typography`; medidas desde `ForjaSpacing`, `ForjaRadius` y `ForjaMeasures`; resortes desde el esquema de movimiento (tras F0-06, `ForjaTheme.motion`); vibración desde `ForjaTheme.haptics`. Nada de `Color(0x...)` ni `16.dp` sueltos en pantallas.
- Áreas táctiles de 48 dp o más, contraste AA, `contentDescription` en íconos que actúan, texto escalable.
- Cada pantalla tiene un solo título grande.
- Relojes: fondo negro puro. En Wear OS, Wear Compose Material 3 con los colores de Forja; en el Apple Watch y en la Live Activity, SwiftUI con `ForjaTokens.swift` (12.10).
- Una pantalla que no está en `docs/PANTALLAS.md` no se construye (2.3, regla 11).

### 8.10 Pruebas

- Dominio: `commonTest`, cobertura del 90 % o más en los motores de progresión, adaptación y tiempo (RNF-08), con historiales de ejemplo y un `Clock` falso que avanza a mano.
- ViewModels: `commonTest` con Turbine (desde F1) y repositorios falsos.
- Room: `androidHostTest` con `Room.inMemoryDatabaseBuilder<IronProgressDatabase>(factory = { IronProgressDatabaseConstructor.initialize() })` y el driver empaquetado.
- Una prueba de migración por cada versión del esquema.
- Nombres de prueba en inglés descriptivo (`seedsAll77ExercisesOnFirstObservation`), comentarios en español.

### 8.11 Git

- Ramas `feat/f1-...`, `fix/...`, `test/...`, `ci/...`, `docs/...`. Un PR por ticket hacia `main`; se fusiona cuando el CI está en verde y Juan lo probó en el teléfono si toca UI.
- Commits pequeños en Conventional Commits en español.
- El PR #1 se fusiona cuando el CI esté en verde (F0-00 y F0-01).

---

## 9. Especificación funcional

### 9.1 Alcance de la v1 (Etapa 1 del plan v3)

La v1 es la Etapa 1: la base obligatoria y las 21 funciones de la v2, más lo que suma el plan v3. Lo que la v2 dejaba para después y ahora entra en la v1: la notificación en vivo con botones, la app completa del Galaxy Watch, la Live Activity y el Apple Watch.

Base obligatoria (v2):

- Catálogo de ejercicios en español e inglés, más ejercicios personalizados.
- Rutinas en carpetas y siguiente rutina automática.
- Registro rápido: valores de la sesión anterior, copiar serie, teclado numérico propio y botones de ±2,5.
- Sesión flexible (reordenar, saltar, añadir series y notas) que sobrevive si el sistema cierra la app.
- Temporizador de descanso que funciona con la pantalla apagada.
- kg y lb por ejercicio, guardado siempre en kg.
- Historial con calendario, récords automáticos y gráficas de 1RM estimado y volumen.
- Peso corporal, copia de seguridad, exportación, onboarding, tema claro y oscuro, accesibilidad.
- Cuenta y sincronización entre dispositivos.

| Área (v2) | Funciones en la v1 |
| --- | --- |
| Reloj y datos | Ajuste diario del entreno; descanso guiado por pulso; lectura de Health Connect y Apple Salud; app para Galaxy Watch |
| Progresión | Sugerencia de peso y reps; detección de estancamiento; feedback después de entrenar |
| Registro | Tipos de serie y superseries; ejercicios más allá de peso por reps; calculadoras de discos y calentamiento |
| Análisis | Series semanales por músculo; mapa corporal de recuperación; medidas y fotos de progreso; resúmenes mensual y anual |
| Rutinas | Programas por semanas; perfiles de gimnasio; biblioteca de programas |
| Extras | Sustitutos sugeridos; importar desde Hevy o Strong |
| Motivación | Tarjeta para compartir; logros y rachas |

Lo que suma el v3 dentro de la v1:

- Todas las disciplinas con un solo modelo de bloques con formato: series, superseries y circuitos, EMOM, AMRAP, por tiempo con límite, intervalos, rondas, flujo y respiración (`docs/PLAN_V3.md`, sección 6).
- Un solo motor de tiempo que mueve el descanso a pantalla completa, los intervalos con voz, las posturas y la respiración, y lo lleva a la Live Activity, la Live Update y los relojes (11.9).
- Intervalos a tu medida, con una voz que cuenta, avisa y baja la música mientras habla.
- Salud y Health Connect desde el inicio: leer sueño, pulso en reposo, VFC y entrenos de otras apps, y escribir cada sesión con el tipo de su disciplina.
- Adaptación antes de empezar y durante la sesión (11.6 y 11.8).
- Apps de Apple Watch Series 12 y Galaxy Watch con la rutina, la serie, el cronómetro animado, los intervalos y el pulso.
- Catálogo de unos 1.000 ejercicios y posturas con su exigencia, sus músculos con porcentaje, su fuente y su licencia (11.10).
- PAR-Q+ al crear la cuenta, molestias y lesiones, y el modo joven de 13 a 17 años (9.5).

| Después de la v1 | Cuándo |
| --- | --- |
| Pulido de movimiento: transiciones, resortes y vibración al nivel de Apple | Etapa 2 (v1.1) |
| Sensores avanzados (temperatura, respiración, oxígeno, VO2 máx), cámara (reps, técnica, velocidad de la barra en beta, fotos guiadas), nutrición leída de Salud, ánimo, embarazo y plan Pro | Etapa 3 (v1.5) |
| Plataforma: entrenadores, creadores y fisioterapeutas, tienda con el precio de cada profesional y 5 % de comisión | Etapa 4 (v2) |
| Reglas de progresión por ejercicio (lineal, por RIR, por % de 1RM) y generador de rutina | Sin fecha; el jefe lo propone cuando toque |
| Asistente con IA y red social | Sin fecha |

### 9.2 Requisitos funcionales de la v2 (51)

Los 51 requisitos de la v2 siguen vigentes; los nuevos del v3 están en 9.4 y, cuando amplían uno de estos, manda el del v3.

| ID | Requisito | Fase |
| --- | --- | --- |
| RF-CAT-01 | Catálogo inicial en español e inglés con músculos primarios y secundarios, equipo, patrón de movimiento y tipo de registro | F1 (hecho en F0) |
| RF-CAT-02 | Crear, editar y archivar ejercicios personalizados | F1 |
| RF-CAT-03 | Buscar y filtrar por nombre, músculo y equipo | F1 (búsqueda y músculo hechos; falta el filtro de equipo en la UI) |
| RF-CAT-04 | Fusionar dos ejercicios conservando todo el historial | F5 |
| RF-CAT-05 | Sugerir sustitutos por patrón de movimiento, músculos y equipo del perfil de gimnasio | F5 |
| RF-RUT-01 | Crear, editar, duplicar y borrar rutinas con ejercicios ordenables, series objetivo, rango de reps, RIR objetivo, descanso y notas | F1 |
| RF-RUT-02 | Organizar rutinas en carpetas, con orden por arrastre | F1 |
| RF-RUT-03 | Proponer la siguiente rutina según la rotación de la carpeta o del programa | F1 |
| RF-RUT-04 | Programas por semanas (mesociclos) con semanas de descarga planificadas | F5 |
| RF-RUT-05 | Biblioteca de programas listos (PPL, torso/pierna, full body, 5x5) que se copian y se editan | F5 |
| RF-RUT-06 | Perfiles de gimnasio con equipo, barra y discos disponibles | F5 |
| RF-SES-01 | Iniciar una sesión libre o desde una rutina o programa | F1 |
| RF-SES-02 | Registrar cada serie con peso, reps, RIR o RPE opcional, tipo de serie y nota | F1 |
| RF-SES-03 | Mostrar la serie equivalente de la sesión anterior y copiarla con un toque | F1 |
| RF-SES-04 | Reordenar, saltar o sustituir ejercicios y añadir o quitar series durante la sesión | F1 |
| RF-SES-05 | Superseries y circuitos: ejercicios agrupados que se alternan (lo amplía RF-DIS-02) | F1 |
| RF-SES-06 | Tipos de registro: peso por reps, peso corporal, lastrado, asistido, tiempo, distancia, tiempo más distancia y unilateral izquierda/derecha (los amplía RF-DIS-03) | F1 |
| RF-SES-07 | Guardar la sesión activa en cada cambio y recuperarla tras un cierre o reinicio | F1 |
| RF-SES-08 | Resumen al terminar: duración, volumen, récords y comparación con la sesión anterior | F1 |
| RF-SES-09 | Calculadora de discos por lado y series de calentamiento sugeridas | F1 |
| RF-SES-10 | Unidad kg o lb por ejercicio, con guardado siempre en kg | F1 |
| RF-TMR-01 | Iniciar el descanso al completar una serie, con duración por ejercicio o por rutina (lo amplían RF-DIS-04 y RF-VIV-08) | F1 |
| RF-TMR-02 | Avisar con sonido y vibración según los ajustes, con la pantalla apagada o la app en segundo plano | F1 |
| RF-TMR-03 | Sumar o restar 15 s, pausar y saltar el descanso | F1 |
| RF-TMR-04 | Descanso guiado por pulso: avisar en el teléfono y en la muñeca cuando el pulso se recupera, entre un mínimo y un máximo | F4 |
| RF-PRG-01 | Sugerir peso y reps para cada serie de trabajo, con una explicación de una línea | F2 |
| RF-PRG-02 | Detectar estancamiento por ejercicio y proponer descarga, cambio de rango o sustituto | F2 |
| RF-PRG-03 | Feedback después de entrenar (bombeo, agujetas, molestias y carga) que ajusta las series de la semana siguiente | F2 |
| RF-PRG-04 | Ajuste diario con sueño, pulso en reposo, VFC si existe y fatiga: proponer una versión reducida de la sesión que el usuario acepta o ignora (lo amplía RF-ADA-01) | F4 |
| RF-PRG-05 | Check-in manual de 10 s (sueño, energía, agujetas) cuando no hay datos del reloj | F4 |
| RF-ANA-01 | Historial por calendario y por lista, con edición y borrado de series pasadas | F1 |
| RF-ANA-02 | Récords automáticos (1RM estimado, mayor peso, más reps a un peso, mayor volumen) con aviso en vivo | F2 |
| RF-ANA-03 | Gráficas por ejercicio: 1RM estimado, mejor serie y volumen en el tiempo | F2 |
| RF-ANA-04 | Series efectivas por músculo y semana, con semáforo frente a un rango objetivo | F2 |
| RF-ANA-05 | Mapa corporal con músculos trabajados y estado de recuperación | F2 |
| RF-ANA-06 | Peso corporal y medidas (manual o desde Health Connect) y fotos de progreso privadas | F2 |
| RF-ANA-07 | Resúmenes mensual y anual | F2 |
| RF-SAL-01 | Leer de Health Connect sueño, frecuencia cardiaca, VFC si existe, peso y composición corporal, con permisos granulares | F4 |
| RF-SAL-02 | Escribir cada sesión terminada en Health Connect como ejercicio (lo amplía RF-SAL-10) | F4 |
| RF-SAL-03 | App mínima para Wear OS (Galaxy Watch4 o superior) que mide el pulso durante la sesión y lo envía al teléfono (la amplía RF-SAL-11) | F4 |
| RF-SAL-04 | En iOS, leer Apple Salud (sueño y frecuencia cardiaca) con la misma lógica de ajuste (lo amplía RF-SAL-10) | F4 |
| RF-CTA-01 | Registro e inicio de sesión con correo y Google; en iOS también con Apple | F3 |
| RF-CTA-02 | Todo funciona sin conexión y la sincronización corre sola al volver internet | F3 |
| RF-CTA-03 | Conflictos resueltos por registro con la escritura más reciente, sin perder series | F3 |
| RF-CTA-04 | Borrar la cuenta y sus datos desde la app y desde una página web | F3 |
| RF-DAT-01 | Copia de seguridad restaurable y exportación a CSV y JSON | F1 |
| RF-DAT-02 | Importar el historial desde el CSV de Hevy o de Strong, emparejando ejercicios | F5 |
| RF-MOT-01 | Tarjeta del entreno como imagen para compartir | F5 |
| RF-MOT-02 | Logros y rachas semanales | F5 |
| RF-CFG-01 | Onboarding: objetivo, experiencia, unidades y días por semana (se suman el PAR-Q+ y la edad: RF-SEG-01 y RF-CFG-04) | F1 |
| RF-CFG-02 | Idioma (español o inglés), tema, sonido y vibración | F1 |

### 9.3 Requisitos no funcionales de la v2 (10)

| ID | Requisito | Cómo se verifica |
| --- | --- | --- |
| RNF-01 Rendimiento | Guardar una serie responde en menos de 100 ms (actualización optimista); ninguna operación de disco corre en el hilo principal | Trazas del profiler en el S25 Ultra y en un Android de gama media |
| RNF-02 Offline | Registro, historial y análisis funcionan sin internet; la sincronización es eventual | Varios días de cambios en modo avión y luego sincronización |
| RNF-03 Integridad | Migraciones versionadas con esquema exportado, nunca destructivas, y copia local antes de migrar | Prueba automática de migración desde cada versión publicada |
| RNF-04 Seguridad | Tokens de sesión, Row Level Security en todas las tablas del servidor, TLS y claves fuera del repositorio | Pruebas de políticas con dos usuarios distintos |
| RNF-05 Privacidad | Datos de salud tratados como datos sensibles (Ley 1581 de 2012); consentimiento por tipo de dato; sin anuncios ni venta de datos; fotos solo en el teléfono salvo que el usuario active su respaldo | Revisión contra la política de privacidad y las declaraciones de Google Play y App Store |
| RNF-06 Accesibilidad | Texto escalable, contraste AA, TalkBack y VoiceOver, áreas táctiles de 48 dp o más | Accessibility Scanner y revisión manual de flujos clave |
| RNF-07 Idiomas | Todos los textos en recursos en español e inglés; números, fechas y unidades según el idioma del sistema | Recorrido completo en ambos idiomas y lint de textos sin traducir |
| RNF-08 Calidad | Cobertura de pruebas de 90 % o más en el motor de progresión (desde el v3, también en los de adaptación y tiempo); CI en cada push; ktlint y detekt sin errores | Reporte de cobertura en el CI |
| RNF-09 Batería | El reloj mide pulso solo durante una sesión activa; sin sensores en segundo plano fuera de ella | Consumo medido en una sesión de 60 min |
| RNF-10 Compatibilidad | Android 9 (API 28) o superior, mínimo de Health Connect; Wear OS en Galaxy Watch4 o superior; iOS 17 o superior (propuesta; decisión abierta 13.10); target API 36 en teléfono y 35 en Wear OS | Matriz de dispositivos en la prueba cerrada |

### 9.4 Requisitos nuevos del plan v3 en la Etapa 1 (32)

El texto completo de cada uno está en `docs/PLAN_V3.md`, sección 13. Cuando amplían o reemplazan uno de la v2 (por ejemplo, RF-SAL-11 reemplaza la "app mínima" de RF-SAL-03), manda el del v3. Los de las etapas 2 a 4 no se programan todavía.

| ID | Requisito (resumen) | Fase |
| --- | --- | --- |
| RF-DIS-01 | Disciplina por ejercicio y por sesión, que define qué se anota y con qué tipo se guarda en Salud y Health Connect | F1 |
| RF-DIS-02 | Bloques con formato: series, superseries, circuitos, EMOM, AMRAP, por tiempo con límite, intervalos, rondas, flujo y respiración | F1 |
| RF-DIS-03 | Tipos de registro nuevos (calorías, distancia sola, altura, segundos o respiraciones por lado, vatios) y series rest-pause, myo-reps y cluster | F1 |
| RF-DIS-04 | Un solo motor de tiempo para descanso, intervalos, posturas y respiración, que manda la pantalla completa, la Live Activity, la Live Update y los relojes | F1 |
| RF-DIS-08 | Guía por voz y vibración en flujos, intervalos y respiración, también con la pantalla apagada | F1 |
| RF-DIS-09 | Respiración guiada con patrones (cuadrada, 4-7-8, coherente y otros) y aviso de seguridad antes de los que tienen retención | F1 |
| RF-DIS-12 | Intervalos a tu medida: trabajo, descanso, rondas y series, voz que cuenta y avisa, máximas reps por ronda y plantillas | F1 |
| RF-VIV-01 | Live Activity del entreno en iPhone: compacta, mínima, expandida, pantalla bloqueada, StandBy y horizontal | F1 |
| RF-VIV-02 | Botones en la Live Activity: completar serie, -15 s, +15 s y saltar | F1 |
| RF-VIV-04 | Live Update en Android (barra de estado, pantalla bloqueada y Now Bar) y Metric Style donde exista | F1 |
| RF-VIV-08 | Descanso a pantalla completa dentro de la app, que pasa a la Live Activity o a la Live Update al salir o al apagar la pantalla | F1 |
| RF-SEG-01 | PAR-Q+ al crear la cuenta, con la recomendación de consultar si alguna respuesta es sí | F1 |
| RF-CFG-03 | Ocultar en toda la app el peso corporal y las calorías | F1 |
| RF-CFG-04 | Edad mínima de 13 años; de 13 a 17, modo joven con permiso de los padres donde la ley lo pida (9.5) | F1 |
| RF-DIS-13 | Exigencia por ejercicio y porcentaje de trabajo por músculo (los 77 de hoy en F1; el resto del catálogo en F5) | F1 y F5 |
| RF-DIS-06 | Progresiones y regresiones por ejercicio, que el motor usa cuando no hay peso que subir | F2 |
| RF-DIS-07 | Resultado de cada formato y récords por formato (el resultado se guarda desde F1) | F2 |
| RF-PRG-09 | Calentamiento según el estado de recuperación de los músculos del día | F2 |
| RF-VIV-05 | Alertas en vivo de récord (F2) y de "listo" por pulso (F4) | F2 y F4 |
| RF-ADA-01 | Adaptación antes de empezar con sueño, pulso en reposo, VFC, check-in, molestias y carga de otras apps | F4 |
| RF-ADA-02 | Adaptación durante la sesión cuando los datos lo piden (11.8) | F4 |
| RF-ADA-03 | Registro de cada adaptación (señales, cambio y si se aceptó) | F4 |
| RF-SEG-02 | Molestias y lesiones: zona y dolor de 0 a 10; el motor evita o cambia lo que carga esa zona | F4 |
| RF-SAL-07 | App de Apple Watch: rutina sin el teléfono, Doble toque, Corona, cronómetro animado, intervalos y pulso reflejado en el iPhone | F4 |
| RF-SAL-10 | Escribir cada sesión en Salud y Health Connect con el tipo de su disciplina y el esfuerzo | F4 |
| RF-SAL-11 | App de Galaxy Watch: rutina, cronómetro con el anillo en el borde, bisel, intervalos y pulso | F4 |
| RF-INT-01 | Leer entrenos y sueño de otras apps y relojes (Garmin, Strava, Oura, Apple Fitness) desde Salud y Health Connect | F4 |
| RF-PRG-07 | Zonas de pulso personales y esfuerzo por serie (pico y segundos de recuperación) | F4 |
| RF-VIV-03 | La Live Activity en el Smart Stack del Apple Watch, con Doble toque | F4 |
| RF-DIS-05 | Catálogo de unos 1.000 ejercicios y posturas al lanzar, en español e inglés (y sánscrito en yoga) | F5 |
| RF-DIS-11 | Ejercicios marcados para silla de ruedas o sentado, y sesiones con sus tipos en Salud y Health Connect | F5 |
| RF-DIS-14 | Catálogo que crece hacia más de 2.000 entradas con variantes de agarre, postura y equipo | F5 |

No funcionales nuevos de la Etapa 1: RNF-15 Batería en vivo (F1 y F4), RNF-16 Licencias limpias (F5), RNF-17 Tiempo exacto (F1) y RNF-19 Protección de menores (F1). RNF-18 Movimiento fluido es de la Etapa 2.

### 9.5 Edad mínima y modo joven (decidido el 25 de octubre)

Desde los 13 años; por debajo no entra nadie por ahora. De 13 a 17, modo joven (RF-CFG-04 y RNF-19; detalle legal en `docs/PLAN_V3.md`, sección 10):

- Sin pruebas de 1RM ni series al fallo; progresión más lenta y técnica primero.
- Sin peso corporal ni calorías a la vista por defecto, sin fotos de progreso y sin registro de comida.
- Sin tienda ni compras, y sin mensajes con profesionales ni con otros usuarios: ningún adulto le escribe a un menor dentro de la app.
- Intervalos y retos con intensidad limitada, sin el modo intenso.
- Adaptación más prudente: alarga antes los descansos y nunca sube la carga dentro de la misma sesión.

| Dónde | Qué pide la ley | Qué hace la app |
| --- | --- | --- |
| EE. UU. | COPPA: permiso verificable de los padres antes de recoger datos de menores de 13 | No admite menores de 13 |
| España | Desde los 14 años la persona consiente sola; por debajo, sus padres | A los 13, con permiso de los padres |
| Colombia | Los datos de menores se tratan con autorización previa del padre o tutor | Pide la autorización de 13 a 17 |
| iPhone | Declared Age Range (iOS 26) da el rango de edad que declaró la familia, sin fecha de nacimiento | Activa el modo joven sin pedir la fecha exacta |

`GuardianConsent` (10.4) guarda quién autorizó, en qué país, con qué alcance, cuándo se otorgó y si se revocó. Antes de F6 se prueba con cuentas de 13, 15 y 17 años en cada país.

### 9.6 Pantallas diseñadas

Son 29 en el lienzo de Claude: 15 de la v2 (el entreno de punta a punta, progreso, plan, ajuste del día, tema claro y dos caras del Galaxy Watch4) y 14 del v3 (descanso a pantalla completa, intervalos, adaptación antes y durante, cuatro del Apple Watch, disciplinas, yoga, respiración, AMRAP y la ficha de ejercicio). Todas están descritas en `docs/PANTALLAS.md` con estructura, medidas, estados, textos y toques.

Siguen sin diseño: el PAR-Q+, el modo joven y el permiso de los padres, molestias y lesiones, preparación en detalle, el Galaxy Watch con la rutina y los intervalos, intervalos en el Apple Watch, la Live Activity en sus cuatro vistas, la Live Update y la Now Bar. Una hormiga que llegue a una pantalla sin diseño se detiene y el jefe se la pide a Juan; nunca se inventa una pantalla.

---

## 10. Modelo de datos y sincronización

Todo en kg, metros y segundos, con ids UUID v7 creados en el dispositivo. Los nombres de campo van en inglés en el código; entre paréntesis, el nombre de la especificación. Son las 20 entidades de la v2 (10.1) más 11 del v3 para la Etapa 1 (10.3 y 10.4); las de las etapas 3 y 4 están en `docs/PLAN_V3.md`, sección 12, y no se crean todavía.

### 10.1 Entidades de la v2 (20)

| Entidad | Campos clave | Relación | Estado |
| --- | --- | --- | --- |
| Exercise | id, nameEs, nameEn, trackingType, equipment, pattern, unilateral, preferredUnit, isCustom, isArchived | N:M con Muscle | Hecha (`exercises`); el v3 le suma campos (10.5) |
| Muscle | id, nombre ES/EN, zona del mapa | Catálogo fijo | Hecha como enum `Muscle` con textos en recursos (no es tabla) |
| ExerciseMuscle | exerciseId, muscleId, role (primario 1,0; secundario 0,5) | Une Exercise y Muscle | Hecha (`exercise_muscles`, con `sort_order`); el v3 le suma el porcentaje (10.5) |
| Folder | id, name, sortOrder | 1:N con Routine | F1 |
| Routine | id, folderId, name, notes, restSeconds, sortOrder | 1:N con RoutineExercise | F1 |
| RoutineExercise | id, routineId, exerciseId, sortOrder, supersetGroup, restSeconds, unit | 1:N con SetTemplate | F1 |
| SetTemplate | id, routineExerciseId, sortOrder, setType, repsMin, repsMax, targetRir | Plantilla de cada serie | F1 |
| Program | id, name, weeks, origin (biblioteca o propio) | 1:N con ProgramWeek | F5 |
| ProgramWeek | id, programId, number, isDeload, volumeFactor | 1:N con ProgramDay | F5 |
| ProgramDay | id, programWeekId, routineId, sortOrder | Apunta a una Routine | F5 |
| GymProfile | id, name, equipment, barKg, platesKg | Lo usan la sesión y la calculadora | F5 (F1 usa valores por defecto) |
| WorkoutSession | id, routineId, programDayId, gymProfileId, startedAt, endedAt, status, notes, adjustmentApplied | 1:N con SessionExercise | F1 |
| SessionExercise | id, sessionId, exerciseId, sortOrder, supersetGroup, notes | Lo que de verdad se hizo, con sustituciones | F1 |
| WorkoutSet | id, sessionExerciseId, sortOrder, setType, weightKg, reps, durationSeconds, distanceMeters, side, rir, rpe, completedAt, suggestion, endHeartRate, recoverySeconds | Unidad atómica del registro | F1 (campos de pulso desde F4) |
| SessionFeedback | id, sessionId, muscleId, pump, soreness, jointPain, load | Uno por músculo trabajado | F2 |
| DailyReadiness | date, sleepMinutes, restingHeartRate, hrvMs, checkIn, score, source | Uno por día | F4 |
| BodyMeasurement | id, date, type, value, source | Peso, grasa, cintura, brazo y otras | F2 (el peso corporal puede adelantarse a F1) |
| ProgressPhoto | id, date, localPath, cloudPath, pose | Privada por defecto | F2 |
| PersonalRecord | id, exerciseId, type, value, setId, date | Derivada y guardada para consultas rápidas | F2 |
| Achievement | id, code, unlockedAt | Logros y rachas | F5 |

### 10.2 Reglas de sincronización (diseñar las tablas de F1 con esto desde ya)

- Cada tabla del usuario lleva `user_id` (nulo hasta F3), `created_at`, `updated_at` y `deleted_at`. Los borrados son lógicos para que viajen entre dispositivos.
- Ids UUID v7 generados en el dispositivo, ordenables por tiempo.
- El catálogo base viaja dentro de la app con número de versión; solo se sincronizan los ejercicios personalizados. La exigencia, la fuente y las escaleras del catálogo base (10.3) viajan con él.
- Conflictos: gana la escritura más reciente de cada registro. Dos series creadas en dos teléfonos (o en el teléfono y el reloj) son dos filas, así que ninguna se pierde.
- El pulso crudo se queda en el teléfono; se sincroniza el resumen por serie (pulso al terminar y segundos de recuperación).
- Las fotos se guardan en el teléfono; su respaldo en la nube es opcional y privado.
- La unidad preferida solo cambia cómo se muestra el dato, nunca cómo se guarda.
- Nuevo del v3: cada rutina guarda quién la creó (`createdBy`: el usuario o, desde la Etapa 4, un profesional) y de qué programa publicado viene (`sourceProgramVersionId`, nulo hasta la Etapa 4).
- Nuevo del v3: los entrenos y el sueño que vienen de otras apps no se copian a la base; se leen de Salud y Health Connect cuando el motor los necesita (F4-01). Si la línea base de 14 días necesita guardarlos, el jefe propone la tabla en ese ticket.

**Valores propuestos que la especificación no fija (el jefe los confirma con Juan antes de F1-01):** tipos de serie `WARMUP`, `WORKING`, `DROP` y `FAILURE` de la v2, más `REST_PAUSE`, `MYO_REPS` y `CLUSTER` del v3 (todos menos `WARMUP` cuentan como series de trabajo); estados de sesión `IN_PROGRESS`, `FINISHED` y `DISCARDED`; lado `LEFT` o `RIGHT` para unilaterales; perfil de gimnasio por defecto con barra de 20 kg y discos de 25, 20, 15, 10, 5, 2,5 y 1,25 kg.

### 10.3 Entidades nuevas de la Etapa 1 (v3)

| Entidad | Campos clave (en el código) | Para qué | Tabla en | Se usa en |
| --- | --- | --- | --- | --- |
| WorkoutBlock | id, routineId o sessionId, sortOrder, format, rounds, workSeconds, restSeconds, timeCapSeconds, result | Un entreno es una lista de bloques; cada bloque dice cómo corre el reloj y cómo se puntúa | F1-03 | F1 |
| IntervalTemplate | id, name, workSeconds, restSeconds, rounds, sets, exerciseIds (en el orden en que se alternan), voice (cuenta atrás, dice qué sigue, baja la música), intensity | Intervalos a tu medida guardados y compartibles (`docs/PANTALLAS.md` 5.2) | F1-03 | F1-18 |
| BreathingPattern | id, name, phases (inhalar, retener, exhalar, retener, en segundos), cycles o minutes, riskLevel | Patrones de respiración; `riskLevel` decide si sale el aviso de seguridad | F1-03 | F1-20 |
| ExerciseProgression | exerciseId, easierId, harderId | La escalera de cada ejercicio (11.10) | F1-03 (se llena en F1-06) | F2-12 y la adaptación |
| ExerciseSource | exerciseId, source, license, author | De dónde salió cada ejercicio y con qué licencia (RNF-16) | F1-03 (se llena en F1-06) | Ficha de ejercicio y F5-09 |
| ExerciseDemand | exerciseId, overall (1 a 5), technique (1 a 5), spine (0 a 3), jointImpact (por articulación: bajo, medio o alto), met | Qué tan exigente es cada ejercicio (11.10) | F1-03 (se llena en F1-06) | Adaptación y ficha |
| ScreeningAnswer | id, userId, date, answers (las del PAR-Q+), result | Tamizaje al crear la cuenta (RF-SEG-01) | F1-03 | F1-26 |
| GuardianConsent | ver 10.4 | Permiso de los padres en el modo joven | F1-03 | F1-26 |
| AdaptationLog | id, sessionId, moment (`BEFORE` o `DURING`; "momento" en el diseño), signals, changes (cada cambio con su `accepted`), createdAt | Registro de cada adaptación para calibrar el motor (RF-ADA-03) | Migración de F4-05 | F4-05 y F4-14 |
| PainEntry | id, userId, sessionExerciseId, zone, pain (0 a 10), createdAt | Molestia marcada en un ejercicio (RF-SEG-02) | Migración de F4-06 | F4-06 |
| Injury | id, userId, zone, from, to, note | Lesión con fechas, que el motor respeta mientras dure | Migración de F4-06 | F4-06 |

Tablas en `snake_case` y plural, como las de hoy: `workout_blocks`, `interval_templates`, `breathing_patterns`, `exercise_progressions`, `exercise_sources`, `exercise_demands`, `screening_answers`, `guardian_consents`, `adaptation_logs`, `pain_entries`, `injuries`. Las listas (`answers`, `signals`, `changes`, `phases`, `jointImpact`, `result`) se guardan como JSON en una columna de texto, con un `@Serializable` en el dominio y su prueba de ida y vuelta.

`WorkoutBlock.result` depende del formato: tiempo en segundos (por tiempo y rondas), rondas más reps (AMRAP), rondas completas (EMOM), reps por ronda (intervalos) y completado con sensación (flujo y respiración). Desde F1 se guarda aunque los récords por formato lleguen en F2-13.

Las zonas de `PainEntry` e `Injury` son las mismas 6 articulaciones de `ExerciseDemand.jointImpact` (rodilla, cadera, hombro, codo, muñeca y lumbar) más `OTHER`, que se anota pero no cambia nada solo.

### 10.4 Menores, tamizaje y ajustes del perfil

- `GuardianConsent`: id, minorUserId, guardian (nombre y contacto del padre, madre o tutor), country, scope (qué autorizó), grantedAt, revokedAt. Se pide de 13 a 17 en Colombia y a los 13 en España; en EE. UU. no entra nadie menor de 13 (9.5).
- La edad no se guarda como fecha: se guarda el rango (`ageRange`: 13, de 14 a 15, de 16 a 17 o 18 o más). En iPhone sale de Declared Age Range sin pedir la fecha; donde no hay ese dato se pregunta solo el año de nacimiento y se guarda el rango.
- `youthMode` se deriva del rango y no lo puede apagar la persona menor de 18.
- `hideBodyNumbers` (RF-CFG-03) oculta el peso corporal y las calorías en toda la app; en modo joven arranca encendido.
- Hasta F3 estos ajustes viven en DataStore con los demás (F1-25); en F3 pasan a la cuenta. El consentimiento y el tamizaje se sincronizan con Row Level Security como cualquier tabla del usuario.

### 10.5 Lo que el v3 cambia en lo que ya existe

Las listas de la v2 crecen por el final y nada de lo guardado cambia (regla 7 de 2.3). Los valores de enum se guardan como texto (8.6), así que agregar al final no rompe nada; renombrar o borrar sí.

| Pieza | Hoy en el repo | Qué suma el v3 | Cuándo |
| --- | --- | --- | --- |
| `TrackingType` | 7 valores: `WEIGHT_REPS`, `REPS`, `WEIGHTED_BODYWEIGHT`, `ASSISTED_BODYWEIGHT`, `DURATION`, `DISTANCE_DURATION`, `WEIGHT_DISTANCE` | Calorías, distancia sola, altura con reps (salto al cajón), respiraciones y vatios con tiempo. "Por lado" no es un tipo nuevo: es `Exercise.unilateral = true` con su `side` en cada serie. Nombres propuestos: `CALORIES`, `DISTANCE`, `HEIGHT_REPS`, `BREATHS`, `WATTS_DURATION` | F1-01 |
| `SetField` | `WEIGHT`, `REPS`, `DURATION`, `DISTANCE` | `CALORIES`, `HEIGHT`, `BREATHS`, `WATTS` | F1-01 |
| `Equipment` | 10 valores | Unos 25: barra hexagonal, balón medicinal, saco, trineo, cajón, cuerda, anillas, barra de dominadas, paralelas, correas de suspensión, rodillo, esterilla, bloque y cinta de yoga, reformer, remo, bici, cinta de correr, ergómetro de esquí, air bike, saco de boxeo y tabla de suspensión, entre otros. `OTHER` se queda donde está | F1-01 los que usen los 77 y las pantallas de F1; el resto en F5-09 |
| `MovementPattern` | 21 valores | `ROTATION`, `ANTI_ROTATION`, `JUMP`, `OLYMPIC_LIFT`, `LOCOMOTION`, `INVERSION`, `POSE`, `BREATHING` | F1-01 |
| `Discipline` (nuevo) | No existe | `STRENGTH`, `WEIGHTLIFTING`, `CALISTHENICS`, `FUNCTIONAL`, `HIIT`, `COMBAT`, `YOGA`, `PILATES`, `MOBILITY`, `BREATHING`, `CLIMBING`, `CARDIO`, `REHAB`, `SPORT`; `MEDITATION` se suma en la Etapa 3. Cada uno con su tipo de HealthKit y de Health Connect según la tabla de `docs/PLAN_V3.md`, sección 6 | F1-01 |
| `BlockFormat` (nuevo) | No existe | `SETS` (series, superseries y circuitos), `EMOM`, `AMRAP`, `FOR_TIME`, `INTERVALS`, `ROUNDS`, `FLOW`, `BREATHING`. El cardio por intervalos usa `INTERVALS` con tramos por distancia, tiempo o zona | F1-01 |
| `Exercise` | Los campos de 10.1 | `discipline`, `level` (principiante, intermedio, avanzado), `aliases` (el nombre en sánscrito en yoga); la exigencia, la fuente y la escalera van en sus tablas (10.3). En F5-10, `adaptedFor` (silla de ruedas o sentado) | F1-01 y F1-03 |
| `ExerciseMuscle` | `role` | `workShare`: porcentaje del trabajo de ese músculo; los de un ejercicio suman 100. `role` se queda: lo usan las reglas de la v2 cuando falta el porcentaje | F1-03; se llena en F1-06 |
| `WorkoutSet` | Los campos de 10.1 | `blockId`, `round` (ronda dentro del bloque), `calories`, `heightCm`, `breaths`, `watts` | F1-01 y F1-03 |
| `WorkoutSession` | Los campos de 10.1 | `discipline` (la principal de la sesión, para el tipo de Salud y Health Connect) | F1-01 y F1-03 |
| `Routine` | Los campos de 10.1 | `createdBy` y `sourceProgramVersionId` (10.2) | F1-01 y F1-03 |
| `DailyReadiness` | Los campos de 10.1 | En la Etapa 3: desvío de temperatura, frecuencia respiratoria, oxígeno y Energy Score de Samsung | Etapa 3 |

`WorkoutSet` ya trae `endHeartRate` y `recoverySeconds` desde la v2: son el esfuerzo por serie que pide RF-PRG-07 (pico y segundos de recuperación) y se llenan en F4.

---

## 11. Motores: progresión, ajuste antes y durante, tiempo y exigencia

Todas las sugerencias y ajustes salen de reglas explicables y probadas, no de una IA. Cada uno muestra su porqué en una línea y la persona siempre decide. Los umbrales son valores iniciales que se calibran con los entrenos de Juan. Todo esto vive en `shared:domain` como funciones puras con pruebas: progresión en `progression`, ajuste en `adaptation`, tiempo en `time`. Nada de esto es consejo médico y la app lo dice.

### 11.1 1RM estimado

```
1RM_est = w × (1 + (r + RIR) / 30)
```

Fórmula de Epley con las reps en reserva sumadas: w es el peso y r las reps. Sin RIR registrado se toma 0. Solo se calcula si r + RIR es 12 o menos.

### 11.2 Sugerencia de peso y reps (doble progresión)

| Lo que pasó la última vez | Sugerencia de hoy |
| --- | --- |
| Todas las series de trabajo llegaron al tope del rango con el RIR objetivo o más | Subir un incremento y volver al mínimo del rango |
| Llegaron al tope, pero al fallo (RIR 0) | Mismo peso; repetir antes de subir |
| Alguna serie quedó dentro del rango | Mismo peso; una rep más en las series que no llegaron al tope |
| Dos sesiones seguidas por debajo del mínimo | Bajar entre 5 % y 10 % y volver al mínimo del rango |
| Ejercicio sin historial | Una serie de prueba con RIR 2 y proponer desde ahí |
| Día con ajuste activo (11.6) | No subir peso; aplicar la reducción del ajuste |
| No hay peso que subir (peso corporal, yoga, pilates) | Subir por la escalera del ejercicio (11.10) |

Incrementos por defecto: 2,5 kg (5 lb) en barra, el siguiente par de mancuernas y el siguiente escalón en máquinas, redondeando a los discos del perfil de gimnasio. Ejemplo de explicación: "Hiciste 3 × 12 con 60 kg y RIR 2: hoy 62,5 kg entre 8 y 12 reps". En modo joven cambian algunas reglas (11.11).

### 11.3 Estancamiento

Hay estancamiento cuando el mejor 1RM estimado de un ejercicio no mejora más de 1 % en sus últimas 4 sesiones (sin contar descargas), o cuando cae dos veces seguidas por debajo del mínimo. La app propone, en este orden: revisar la recuperación si hubo días bajos, una semana de descarga (40 % menos series), cambiar el rango de reps o un sustituto.

### 11.4 Feedback después de entrenar

Al terminar se califica cada músculo trabajado: bombeo (0 a 3), molestias articulares (0 a 2) y carga percibida (0 a 3). Las agujetas (0 a 3) se preguntan antes de volver a entrenar ese músculo.

- Agujetas ya curadas, bombeo bajo y carga fácil: una serie más para ese músculo la semana siguiente, hasta el tope de su rango.
- Agujetas presentes al volver a entrenarlo o carga muy dura: mantener o quitar una serie.
- Molestias de 2: sugerir un sustituto del ejercicio que las causó.

Rango por defecto: 10 a 20 series efectivas por músculo y semana, configurable. Solo cuentan series de trabajo; el músculo primario suma 1 y el secundario 0,5 (`Exercise.setWeightFor`), en los últimos 7 días. Cuando el ejercicio tiene porcentajes por músculo (11.10), se usa la regla de 11.10.

### 11.5 Mapa de recuperación

```
F_m(t) = Σ_i s_i · 0,5^((t - t_i) / h_m)
```

La fatiga de cada músculo suma sus series efectivas (s = 1 primario, 0,5 secundario, o el valor de 11.10 cuando hay porcentaje) y baja a la mitad cada h horas: 24 h en músculos pequeños y 48 h en grandes (`MuscleSize.recoveryHalfLifeHours`). Verde por debajo de 3, ámbar entre 3 y 6, rojo por encima de 6. Colores del mapa: `heat0` a `heat4`.

### 11.6 Ajuste antes de empezar (línea base de 14 días)

Es el "ajuste diario" de la v2, ampliado por el v3 (RF-PRG-04 y RF-ADA-01). Se calcula al abrir Hoy y cuando llegan datos nuevos del reloj.

| Señal | Cuenta como día bajo si... |
| --- | --- |
| Sueño | Menos de 6 h o menos del 80 % de tu mediana |
| Pulso en reposo | 5 lpm o más sobre tu mediana |
| VFC (si Salud o Health Connect la traen) | Por debajo de tu media menos una desviación estándar |
| Check-in | Energía 1 de 5 o agujetas fuertes en los músculos de hoy |
| Mapa de recuperación | Músculos de la sesión de hoy en rojo |
| Carga de otras apps (nuevo) | Un entreno de otra app en las últimas 24 h con más de 60 min o de intensidad alta que cargó los músculos de hoy (una carrera larga antes de piernas) |
| Molestia o lesión (nuevo) | Una zona con dolor de 4 o más en los últimos 3 días, o una lesión activa: no cuenta como señal, pero obliga a cambiar lo que carga esa zona |

Qué cambia, siempre en este orden: **1** peso, **2** series o reps, **3** cardio, **4** ejercicio.

| Nivel | Cuándo | Qué propone |
| --- | --- | --- |
| Normal | Ninguna señal | El plan tal cual |
| Atento | Una señal | Mismos pesos y 1 más al RIR objetivo |
| Bajo | Dos señales | 5 a 10 % menos de peso y una serie menos por ejercicio |
| Muy bajo | Tres señales o más | Lo de "Bajo", más quitar el cardio intenso o pasarlo a zona 2 y cambiar los ejercicios de exigencia 5 por otros de los mismos músculos y menos exigentes |
| Alto | Sueño y VFC sobre tu media, sin molestias y 3 semanas cumpliendo el plan | Permite el siguiente incremento una sesión antes ("Todo en verde: puedes probar 2,5 kg más"). Nunca en modo joven |

- El peso se redondea a los discos del perfil (85 kg × 0,925 = 78,6, se muestra 77,5).
- La pantalla es la hoja "Hoy, versión ligera" y su detalle (`docs/PANTALLAS.md` 2.4 y 5.4): un interruptor por cambio, la duración y la exigencia de la sesión recalculadas al tocar cada uno (11.10).
- La persona acepta todo, una parte o nada. Cada decisión se guarda en `AdaptationLog` con `moment = BEFORE`, las señales que pesaron y cada cambio con su `accepted`.
- La tarjeta de Hoy muestra solo las dos señales que más pesaron; el detalle muestra todas con su línea base.
- Si no hay pulso en reposo, se calcula como el promedio más bajo de 5 min durante el sueño. Sin reloj, quedan el check-in, el mapa y las otras apps.

### 11.7 Descanso guiado por pulso

```
umbral = R + 0,5 × (P - R)
```

R es el pulso en reposo y P el pico al terminar la serie. Cuando el pulso baja del umbral, el reloj vibra y el teléfono avisa. Nunca antes del 50 % del descanso programado ni después de ese descanso más 60 s. Sin reloj o sin señal vuelve el temporizador normal. Vale igual para el Galaxy Watch y el Apple Watch, y el "Listo" sale también en la Live Activity y en la Live Update (F4-08).

### 11.8 Ajuste durante la sesión (nuevo del v3)

Cuando los datos lo piden en plena sesión, el motor ajusta lo que queda (RF-ADA-02). Mira el pulso en vivo, cuánto tarda en bajar entre series (11.7), las reps hechas frente al objetivo y el RPE o RIR marcado.

Cambia en este orden:

1. **Alarga el descanso**, solo y con Deshacer: 30 s cada vez y como mucho 60 s por descanso ("Sumé 30 s: tu pulso no baja").
2. **Baja el peso de la serie siguiente.**
3. **Quita una serie** o el cardio del final.
4. **Cambia el ejercicio** por otro de los mismos músculos y menos exigente (11.10).

Del paso 2 en adelante llega una tarjeta que se acepta en un toque desde el teléfono, la isla (botones Aplicar y Seguir igual en la Live Activity) o el reloj (el Doble toque dice que sí). La primera respuesta manda y las otras dos se actualizan.

| Señal (valores iniciales) | Qué hace | Lo que dice la app |
| --- | --- | --- |
| El descanso llegó a su fin y el pulso sigue sobre el umbral de 11.7 | Paso 1 | "Sumé 30 s: tu pulso no baja" |
| Pasa otra vez en el descanso siguiente del mismo ejercicio | Paso 2 | "Tu pulso sigue alto: 30 s más o 2,5 kg menos" |
| Una serie salió con 2 reps o más por debajo del mínimo, o con RPE 1,5 o más sobre el previsto | Paso 2 en la serie siguiente | "La serie 2 salió a RPE 9,5: bajamos 2,5 kg" |
| Reps que caen dos series seguidas en el mismo ejercicio, o el pico de pulso sube en tres series seguidas | Paso 3 | "Vas más cansado que de costumbre: quitamos la serie 4 y el cardio" |
| Molestia marcada durante la sesión, o el paso 2 ya se aplicó y la serie vuelve a fallar | Paso 4 | "Molestia en el hombro: press con mancuernas en vez de press militar" |
| A mitad de dos descansos seguidos el pulso sigue sobre el 90 % de tu máximo estimado | Propone parar | "Tu pulso no baja ni descansando: 20 s más o para un momento" |

- **Cuánto bajar:** la carga que el 1RM estimado de la serie que falló (11.1) da para el mínimo del rango con el RIR objetivo, redondeada a los discos del perfil y nunca más de 10 % por debajo de la anterior. Con 77,5 kg × 6 reps al fallo y un rango de 8 con RIR 1, sale 72,5 kg (`docs/PANTALLAS.md` 6.1).
- **Nunca interrumpe una serie:** todo llega en el descanso.
- **Como mucho un cambio cada dos series**, contando el descanso alargado.
- **Solo por hoy:** el plan no cambia. La sesión siguiente parte de lo que de verdad se hizo (11.2).
- Sin pulso (sin reloj ni banda) solo mira reps y RPE.
- Se guarda en `AdaptationLog` con `moment = DURING`, la serie afectada, las señales y si se aceptó.
- En el reloj, la tarjeta es la pantalla "¿Bajas a 72,5 kg?" (`docs/PANTALLAS.md` 6.2).

### 11.9 Motor de tiempo (nuevo del v3)

Un solo motor, en `shared:domain/time`, mueve todo lo que corre con reloj: el descanso entre series, el EMOM, el AMRAP, por tiempo, los intervalos, las rondas, las posturas y la respiración (RF-DIS-04 y RNF-17).

**Regla de oro: el estado se calcula desde instantes, nunca contando segundos.** El estado en cualquier momento es una función pura del plan del bloque, el instante de inicio, las pausas, los ajustes (más o menos 15 s, saltar) y el instante actual. Por eso la pantalla, la isla, la barra de estado y los relojes nunca se desfasan, y un `delay(1000)` en un bucle (como el prototipo del reloj, 4.2) queda prohibido.

Forma propuesta (F1-02 la fija con pruebas):

```kotlin
// Propuesta; el ticket F1-02 decide los nombres finales.
data class TimerPlan(val segments: List<TimerSegment>)

data class TimerSegment(
    val kind: SegmentKind,          // WORK, REST, POSE, INHALE, HOLD, EXHALE, CAP...
    val durationMs: Long?,          // null: abierto, cuenta hacia arriba (por tiempo, rondas)
    val round: Int,
    val label: String? = null,      // id del ejercicio o de la postura
)

data class TimerState(
    val segmentIndex: Int,
    val kind: SegmentKind,
    val elapsedInSegmentMs: Long,
    val remainingInSegmentMs: Long?,
    val totalElapsedMs: Long,
    val round: Int,
    val isPaused: Boolean,
    val isFinished: Boolean,
)

fun TimerPlan.stateAt(nowMs: Long, startedAtMs: Long, edits: List<TimerEdit>): TimerState

// Eventos entre dos instantes: cambios de fase, "quedan 5", los tics de los últimos 3 s.
fun TimerPlan.eventsBetween(fromMs: Long, toMs: Long, startedAtMs: Long, edits: List<TimerEdit>): List<TimerEvent>
```

| Formato | Segmentos |
| --- | --- |
| Series (descanso) | Un `REST` al completar cada serie, con la duración del ejercicio o la rutina; más o menos 15 s y saltar son `TimerEdit` |
| EMOM | N segmentos de 60 s; lo que sobra después de completar es descanso |
| AMRAP | Un segmento con el total, en cuenta atrás |
| Por tiempo con límite | Abierto hacia arriba, con el límite como `CAP` |
| Intervalos | Calentamiento, luego trabajo y descanso por ronda y por serie de rondas, y vuelta a la calma. El último descanso no cuenta en el total |
| Rondas | Abierto hacia arriba |
| Flujo | Un `POSE` por postura y por lado: respiraciones × 8 s (por defecto) o segundos |
| Respiración | Las fases del patrón por ciclo, por ciclos o por minutos |

A dónde va el estado:

- **Pantalla:** se redibuja con el reloj de cuadros de Compose leyendo `stateAt(ahora)`; el estado nunca se guarda segundo a segundo.
- **Live Activity (iPhone):** se le manda el instante de fin y el contador lo dibuja el sistema con `Text(timerInterval:)` y `ProgressView(timerInterval:)`; la actividad solo se actualiza al cambiar de fase (RNF-15).
- **Live Update (Android):** notificación en curso con `setWhen(fin)`, `setUsesChronometer(true)` y `setChronometerCountDown(true)`; se actualiza solo al cambiar de fase.
- **Relojes:** reciben el plan y el instante de inicio y calculan lo mismo. En el Galaxy Watch con el mismo Kotlin; en el Apple Watch, si el dominio no compila para watchOS (F4-10), se porta `stateAt` a Swift con las mismas pruebas.
- **Voz y vibración:** salen de `eventsBetween`; las frases están en `docs/PANTALLAS.md` 5.3.
- **Con la app cerrada:** Android sigue con el servicio en primer plano; en iPhone la Live Activity cuenta sola y una notificación local a la hora de fin es el respaldo (F1-15).

Pruebas: un `Clock` falso que avanza a mano; 20 descansos con pausas y ajustes, un EMOM de 10 min, un AMRAP de 12, un Tabata 20/10 de 8 rondas, un flujo con lados y una respiración cuadrada, todos exactos al milisegundo.

### 11.10 Exigencia, músculos con porcentaje y escaleras (nuevo del v3)

Cada ejercicio guarda cuánto exige y qué músculos trabaja (`ExerciseDemand` y `ExerciseMuscle.workShare`, 10.3 y 10.5). La adaptación los usa para saber qué quitar o cambiar primero, y la ficha de ejercicio los muestra (`docs/PANTALLAS.md` 7.5).

| Dato | Escala | Para qué |
| --- | --- | --- |
| Músculos con porcentaje | Suman 100 por ejercicio (press banca: pectoral mayor 55, tríceps 25, deltoides anterior 20) | Series por músculo y mapa de recuperación |
| Exigencia general | 1 a 5 (peso muerto 5, curl 1) | La adaptación quita o cambia primero lo más exigente |
| Dificultad técnica | 1 a 5 | Variantes más sencillas para principiantes o días cansados |
| Carga en la columna | 0 a 3 | Días con molestias lumbares |
| Impacto por articulación | Bajo, medio o alto en rodilla, cadera, hombro, codo, muñeca y lumbar | Cambiar ejercicios cuando hay molestias |
| Gasto en cardio | MET del Compendio de actividades físicas 2024 | Calorías y carga de cardio y circuitos |

**Valores iniciales para los 77 de hoy (F1-06):** salen de reglas por patrón y equipo y Juan los revisa a mano antes de cerrar el ticket. Punto de partida propuesto para la exigencia general:

| Exigencia | Patrones |
| --- | --- |
| 5 | Bisagra y sentadilla con barra, levantamientos olímpicos |
| 4 | Sentadilla y bisagra con mancuernas o máquina, zancada con barra, press horizontal y vertical con barra, dominadas |
| 3 | Remos, jalones, zancadas, extensión de cadera, paseos con carga |
| 2 | Aperturas, extensión y curl de rodilla, abducción, aislamientos de hombro, encogimientos, core |
| 1 | Curl y extensión de codo, pantorrilla |

La carga en la columna va de 3 (bisagra con barra, sentadilla con barra en la espalda) a 0 (máquinas con respaldo y poleas sentado). El MET solo se llena con el código de actividad del Compendio; si no hay uno claro, queda vacío: no se inventa.

**Series efectivas con porcentaje (propuesta para confirmar con Juan):** s = porcentaje del músculo ÷ porcentaje del músculo principal de ese ejercicio. El principal suma 1, como en la v2, y en el press banca el tríceps suma 25 ÷ 55 = 0,45. Así los rangos de 10 a 20 series siguen valiendo.

**Exigencia de la sesión (la "8 de 10" de `docs/PANTALLAS.md` 5.4, propuesta):** carga = Σ series de trabajo × exigencia general × RPE previsto ÷ 10, y se expresa de 1 a 10 frente a tus sesiones de las últimas 4 semanas (10 es la más exigente). Sin historial se compara con la sesión tal como está en el plan.

**Escaleras:** `ExerciseProgression` dice cuál es más fácil y cuál más difícil (flexión con rodillas, flexión, flexión declinada). Cuando no hay peso que subir: si todas las series llegan al tope del rango con el RIR objetivo en 2 sesiones seguidas, propone la variante más difícil en el mínimo del rango; si quedan 2 sesiones seguidas por debajo del mínimo, la más fácil (F2-12).

**Cambiar un ejercicio** (paso 4 de 11.6 y 11.8, molestias y sustitutos de F5-04): mismos músculos principales, mismo patrón si se puede, menor exigencia general o menor impacto en la articulación que molesta, y el equipo que haya en el perfil de gimnasio.

### 11.11 Modo joven en los motores (13 a 17 años)

- Sin series al fallo: el tipo `FAILURE` no se ofrece y el RIR objetivo es 2 o más. La app no pide pruebas de 1RM (tampoco lo hace para adultos); el 1RM estimado no se muestra.
- Progresión más lenta: sube peso solo después de dos sesiones seguidas en el tope del rango, con el incremento más pequeño que permita el equipo.
- La preparación "Alta" nunca acelera la subida, y durante la sesión el motor nunca sube la carga.
- Ajuste más prudente: el paso 1 de 11.8 salta con la primera señal y el umbral para proponer parar baja al 85 % del máximo estimado.
- Intervalos y retos sin el modo intenso; el límite exacto de intensidad lo propone el jefe con Juan antes de F1-26.

---

## 12. Sistema de diseño Forja

Concepto: "Entrenar calienta. Descansar templa." Brasa (naranja) es esfuerzo y acción, Hielo (azul) es recuperación y Oro marca los récords. El tema oscuro es el principal. Los valores ya están en `ForjaColors.kt` y coinciden con la especificación; no los cambies.

### 12.1 Colores (nombre en la especificación y en el código)

| Especificación | Código | Oscuro | Claro |
| --- | --- | --- | --- |
| fondo | `background` | #0a0b0d | #f3f4f6 |
| superficie-1 | `surface1` | #15171b | #ffffff |
| superficie-2 | `surface2` | #1d2025 | #ffffff |
| superficie-3 | `surface3` | #262a30 | #e8eaee |
| control-activo | `activeControl` | #3a3f47 | #ffffff |
| linea | `line` | #ffffff14 | #0e101414 |
| linea-control | `controlLine` | #6c727b | #7f8792 |
| texto-1 | `text1` | #f3f4f6 | #0e1014 |
| texto-2 | `text2` | #a5abb4 | #545b66 |
| texto-3 | `text3` | #838a94 | #646b76 |
| brasa | `ember` | #ff6b2c | #ff6b2c |
| brasa-presionada | `emberPressed` | #f2581a | #ec5a1d |
| sobre-brasa | `onEmber` | #140a05 | #140a05 |
| brasa-tinta | `emberInk` | #ff6b2c | #b83d0b |
| brasa-velo | `emberVeil` | #3a241e | #ffeae1 |
| hielo | `ice` | #6cc4f2 | #2a86c2 |
| hielo-tinta | `iceInk` | #6cc4f2 | #166aa0 |
| hielo-velo | `iceVeil` | #23333d | #e6f2f9 |
| oro | `gold` | #f5c451 | #f5c451 |
| sobre-oro | `onGold` | #2a1f00 | #2a1f00 |
| oro-tinta | `goldInk` | #f5c451 | #7e5a00 |
| oro-velo | `goldVeil` | #393324 | #fdf2d9 |
| peligro | `danger` | #ff5a6e | #c8243e |
| sobre-peligro | `onDanger` | #1a0508 | #ffffff |
| calor-0 a calor-4 | `heat0` a `heat4` | #2a2f37, #834228, #b24d22, #de5b1f, #ff8047 | #e3e6ea, #f29b70, #e97a3f, #d05e1c, #9e420e |
| vidrio | `glass` | #1a1c21db | #ffffffd6 |
| vidrio-borde | `glassBorder` | #ffffff1f | #0e101414 |
| vidrio-seleccion | `glassSelection` | #ffffff1f | #0e101412 |
| velo-modal | `modalScrim` | #000000a3 | #0e101466 |

Los colores de 8 dígitos están en CSS `#RRGGBBAA`; en Compose van como `0xAARRGGBB`. El contraste AA ya está verificado: no bajes la opacidad del vidrio (86 % en oscuro y 84 % en claro).

### 12.2 Tipografía

Archivo (licencia OFL 1.1), variable de peso 100 a 900 y ancho 62 a 125. "Archivo Cifras" es la instancia de ancho 112,5 (SemiExpanded) para números: cifras tabulares (`tnum`) en números vivos o alineados y proporcionales (`pnum`) en números héroe estáticos. En Compose se usan TTF estáticos: Archivo Regular, Medium, SemiBold y Bold, más Archivo SemiExpanded SemiBold y Bold.

| Estilo (código) | Tamaño / interlínea (sp) | Peso | Tracking |
| --- | --- | --- | --- |
| `title1` | 30/36 | 700 | -0,01 em |
| `title2` | 20/26 | 600 | |
| `title3` | 17/22 | 600 | |
| `body` | 16/24 | 400 | |
| `body2` | 14/20 | 400 | |
| `label` | 13/16 | 500 | 0,01 em |
| `caption` | 12/16 | 500 | 0,04 em |
| `tab` | 12/14 | 500 | |
| `heroFigure` | 72/76 | 700 | -0,02 em |
| `largeFigure` | 40/44 | 600 | -0,01 em |
| `figure` | 22/28 | 600 | |
| `smallFigure` | 15/20 | 600 | |

### 12.3 Medidas (dp, ya en `ForjaMetrics.kt`)

- Espacios: 2, 4, 8, 12, 16, 20, 24, 28, 32 y 48.
- Radios: 14, 20, 28, 36 y completo.
- Toque mínimo 48; botón principal 56; botón secundario 48; fila de serie 56; tecla 60.
- Isla de 64 de alto con margen de 12; accesorio de 56.
- Ícono de 24 (20 en línea); anillo de descanso de 40 en la barra (a pantalla completa, 300 con grosor 12: `docs/PANTALLAS.md` 5.1); desenfoque del vidrio de 24.
- Columnas de la fila de serie: 28 / 1fr / 76 / 60 / 48, con huecos de 6 dp y relleno de 8 dp.

### 12.4 Movimiento (resortes M3 Expressive, `dampingRatio / stiffness`)

| Resorte | Valor | Uso |
| --- | --- | --- |
| espacial-rápido (`spatialFast`) | 0,6 / 800 | Presionar botones, chips e indicador de pestaña |
| espacial (`spatial`) | 0,8 / 380 | Transformaciones de la isla, hojas y tarjetas |
| espacial-lento (`spatialSlow`) | 0,8 / 200 | Transiciones de pantalla completa y entrada del resumen |
| efecto-rápido (`effectFast`) | 1,0 / 3800 | Color, relleno de íconos y opacidad |
| efecto (`effect`) | 1,0 / 1600 | |
| efecto-lento (`effectSlow`) | 1,0 / 800 | |

Hay que respetar el ajuste de reducir movimiento: `ForjaMotionScheme(reduceMotion = true)` cambia todo por un `tween` de 150 ms. Escala al presionar: 0,97 en botones y 0,95 en chips (`Modifier.forjaPressScale`).

### 12.5 Vibración (ya en `ForjaHaptics`)

| Momento | Android | iOS |
| --- | --- | --- |
| Completar una serie | `CONFIRM` | impacto medio |
| Selector segmentado o ajustar arrastrando | `SEGMENT_TICK` | selección |
| Tecla del teclado propio | `KEYBOARD_TAP` | impacto suave |
| Últimos 3 s del descanso (un tic por segundo) | `CLOCK_TICK` | selección |
| Fin del descanso | patrón 0, 180, 90, 180 ms | notificación de éxito |
| Récord personal | `CONFIRM` dos veces con 120 ms de pausa | éxito |
| Pasar el umbral de Deslizar para terminar | `GESTURE_THRESHOLD_ACTIVATE` | impacto rígido |
| Error o acción rechazada | `REJECT` | notificación de error |

Cambiar de pestaña y hacer scroll nunca vibran.

Presupuesto de toques (tabla completa en `docs/PANTALLAS.md`, sección 8): empezar el entreno sugerido 1; registrar la serie sugerida 1; pasar a la siguiente serie 0; registrar otras reps 3; ajustar el descanso 1; saltar el descanso 1; marcar el RIR 0 o 1; deshacer una serie 1; volver al entreno 1; aplicar el ajuste del día 1; aceptar un ajuste durante el entreno 1 (en el reloj, Doble toque); sustituir un ejercicio 2; ver el historial de un ejercicio 2; dar el feedback de la sesión 1; anotar las reps de una ronda de intervalos 1 (o 0: guarda las de la última vez); sumar una ronda en un AMRAP 1; terminar, 1 gesto de deslizar. Ninguna función nueva sube estos números.

### 12.6 Vidrio y la isla

- El vidrio va solo en la capa de navegación (isla y accesorio), siguiendo la guía Liquid Glass de Apple.
- La isla es una píldora flotante inspirada en la barra de la Galería de One UI 8.5, con tres pestañas: Hoy (`local_fire_department`), Plan (`calendar_month`) y Progreso (`monitoring`). Al lado va un círculo aparte para Buscar (`search`).
- Al hacer scroll hacia abajo la isla se minimiza, como en iOS 26; al subir, vuelve.
- Encima va el accesorio con el entreno en curso, que te devuelve a él en un toque.
- Con Haze 2.0.1 (verificado en su código fuente):

```kotlin
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

val hazeState = rememberHazeState()
Box(Modifier.fillMaxSize()) {
    // El contenido que pasa por detrás de la isla
    NavDisplay(/* ... */ modifier = Modifier.hazeSource(hazeState))

    ForjaIsland(
        /* ... */
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .clip(ForjaRadius.shapeFull)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    backgroundColor(ForjaTheme.colors.background)
                    colorEffects(listOf(HazeColorEffect.tint(ForjaTheme.colors.glass)))
                    blurRadius(ForjaMeasures.glassBlur)
                    noiseFactor(0.03f)
                },
            ),
    )
}
```

- Formas: `androidx.graphics:graphics-shapes` (RoundedPolygon con suavizado de 0,6) solo si una pantalla lo necesita; hoy no hace falta.
- Íconos: Material Symbols Rounded, peso 400, con relleno solo en el ítem seleccionado.

### 12.7 Componentes previstos

Isla, AccesorioSesion, BarraEntreno, FilaSerie, Teclado, Botón, Chip, Deslizador, Segmentado, Tarjeta, Preparación, VolumenMúsculo, MapaCalor, Métrica, Hoja y Aviso. En código: `ForjaIsland`, `ForjaSessionAccessory`, `ForjaWorkoutBar`, `ForjaSetRow`, `ForjaKeypad`, `ForjaButton`, `ForjaChip`, `ForjaSlideToFinish`, `ForjaSegmentedControl`, `ForjaCard`, `ForjaReadinessCard`, `ForjaMuscleVolume`, `ForjaHeatMap`, `ForjaMetric`, `ForjaSheet` y `ForjaNotice`, en `ui/components/`. Los nuevos del v3 están en 12.11.

### 12.8 Principios y voz

Una pantalla, un trabajo. Lo simple es más difícil. El diseño es cómo funciona. Decir que no. La parte de atrás de la cerca. De punta a punta. Pruébalo sudado.

Voz: tuteo y verbos; números con formato local ("62,5 kg", "8.420 kg"); separador " · "; sin raya, sin signos de exclamación y sin emoji. Vale para las frases de la voz y sus subtítulos. Se dice "Funcional", nunca "CrossFit".

### 12.9 Lo que el v3 cambia en Forja

Se mantiene todo lo anterior: las dos temperaturas (brasa para el esfuerzo, hielo para la recuperación, oro solo para récords y peligro solo para borrar), la isla de tres pestañas con el círculo de Buscar, el vidrio solo en la navegación, el presupuesto de toques, la tarjeta de preparación sin puntaje de 0 a 100 y "nunca dos ejes" en las gráficas. Lo nuevo entra por Buscar, Plan, Hoy y el perfil, nunca por una cuarta pestaña.

| Pieza | Antes | Ahora |
| --- | --- | --- |
| Descanso | La barra del entreno se volvía el descanso con un anillo de 40 dp | Al completar la serie, la barra crece hasta ser la pantalla: anillo grande, tiempo en cifra héroe, pulso y la siguiente serie; -15 s, +15 s y Saltar quedan abajo, en la franja de la barra. Deslizar hacia abajo la devuelve a barra (la barra con anillo de 40 sigue existiendo para ese estado) y al estar listo vuelve sola con la siguiente serie cargada (`docs/PANTALLAS.md` 5.1) |
| Bloques | Solo series y superseries | Cabecera del bloque con su formato y su reloj (EMOM 4 de 10, AMRAP 7:12, Tabata 3 de 8); la barra cambia su texto según el formato: Completar ronda, Siguiente postura |
| Completar serie fuera del entreno | El accesorio de sesión no completa series | Se completa solo donde se ven los valores que se guardan: sí en la Live Activity expandida, en la Live Update expandida y en los relojes; no en el accesorio ni en la vista compacta de la isla |
| Buscar | Ejercicios, rutinas e historial | Suma un filtro por disciplina (y en la Etapa 4, programas y profesionales) |
| Tarjeta de preparación | Sueño y pulso en reposo (o VFC) | Sigue con dos datos, pero muestra los dos que más pesaron hoy; el detalle lista todas las señales con su línea base |
| Relojes | Dos caras del Galaxy Watch4 | Galaxy Watch y Apple Watch Series 12 (pantalla rectangular, Doble toque), más la Live Activity pequeña del Smart Stack |

### 12.10 Isla, Live Activity, Live Update y relojes

- **Fondo negro siempre** en la isla, la Live Activity y los relojes. Brasa y hielo van en sus tonos del tema oscuro (`ember` #ff6b2c, `ice` #6cc4f2) y las cifras en Archivo Cifras tabulares.
- **La vista compacta se lee con un solo número y un anillo:** en el iPhone 18 Pro la isla comparte espacio con hasta tres actividades. Compacta: anillo hielo y "1:12" a la izquierda, pulso a la derecha. Mínima: solo el anillo. Expandida: "Press banca · Serie 3 de 4 · 62,5 kg × 10" con Completar serie, -15 s, +15 s y Saltar.
- **Pantalla bloqueada y StandBy:** tarjeta con ejercicio, serie, descanso y pulso. En StandBy se ve al 200 %, con el teléfono cargando sobre el banco.
- **Listo:** el anillo pasa de hielo a brasa y dice "Listo". Récord: alerta breve en oro ("Nuevo récord · 100 kg × 5").
- **Live Update (Android):** el sistema la dibuja con su propio estilo: no puede ir coloreada (`setColorized`) ni llevar vistas propias. Forja entra por el texto corto del chip ("1:12"), el ícono, el color de acento (`setColor` en brasa) y la barra de progreso de `ProgressStyle` si se usa.
- **Apple Watch:** 416 × 496 px (208 × 248 pt), el cronómetro es un trazo de 12 px que recorre el borde de la pantalla, botón principal de 96 px, la Corona ajusta y el Doble toque hace la acción principal.
- **Galaxy Watch4:** 450 × 450 px redondo, anillo de 12 px a 6 px del borde, el bisel suma o resta 15 s, botón de 240 × 96 abajo.
- **Swift:** la Live Activity y la app del Apple Watch no ven `ForjaTheme`. Llevan un `ForjaTokens.swift` con los valores de 12.1 y 12.2 copiados tal cual (F1-16); si cambia un token, cambia en los dos lados en el mismo commit.
- Medidas completas de cada vista del reloj en `docs/PANTALLAS.md`, secciones 0, 4, 5 y 6. La Live Activity en sus cuatro vistas, la Live Update y la Now Bar siguen sin diseño (9.6).

### 12.11 Componentes nuevos (previstos, no existen todavía)

| Componente | En código (propuesto) | Dónde se ve en `docs/PANTALLAS.md` |
| --- | --- | --- |
| Descanso a pantalla completa | `ForjaRestScreen` | 5.1 y 6.1 |
| Reloj de bloque | `ForjaBlockClock` | 7.4: cabecera de EMOM, AMRAP y Tabata |
| Anillo de fase | `ForjaPhaseRing` | 5.3: trabajo en `ember`, descanso en hielo y fin en oro |
| Subtítulo de la voz | `ForjaVoiceCaption` | 5.3: píldora de vidrio con la frase que se dice |
| Guía de respiración | `ForjaBreathingGuide` | 7.2 y 7.3: círculo que crece al inhalar, se queda al retener y se encoge al exhalar |
| Figura de postura | `ForjaPoseFigure` | 7.2 y 7.5: dibujo vectorial propio, trazo de 16 dp, perfil a la izquierda |
| Baldosa de disciplina | `ForjaDisciplineTile` | 7.1: cuadrícula de 2 columnas |
| Tarjeta de ajuste | `ForjaAdaptationCard` | 6.1: propuesta del ajuste en vivo con Aplicar y Seguir igual |
| Medidor de exigencia | `ForjaDemandMeter` | 7.5: segmentos de 8 dp |
| Barra de músculo | `ForjaMuscleShare` | 7.5: porcentaje por músculo |
| Paso a paso | `ForjaStepper` | 5.2: menos y más con botones de 48 |

Los nombres los fija el ticket que crea cada uno; todos van en `ui/components/` y usan solo tokens de Forja.

### 12.12 Zonas de pulso, íconos y movimiento de las pantallas nuevas

- **Zonas de pulso:** escala de 5 pasos derivada de brasa, siempre con el número de zona al lado ("Zona 5 · máxima"). Los valores no están definidos: antes de F4-09 el jefe le pide a Juan el diseño y se validan contraste y daltonismo. El set de estado para el panel profesional (bien, atención, crítico) es de la Etapa 4.
- **Íconos que usan las 29 pantallas** (Material Symbols Rounded, peso 400): `local_fire_department`, `calendar_month`, `monitoring`, `search`, `account_circle`, `keyboard_arrow_down`, `chevron_left`, `chevron_right`, `arrow_back`, `arrow_forward`, `north_east`, `more_horiz`, `play_arrow`, `pause`, `skip_next`, `check`, `close`, `add`, `remove`, `timer`, `replay`, `edit`, `info`, `ios_share`, `trophy`, `bedtime`, `ecg_heart`, `monitor_heart`, `watch`, `volume_up`, `volume_off`, `trending_down`, `trending_up`, `fitness_center`, `sports_gymnastics`, `self_improvement`, `accessibility_new`, `spa`, `air`, `directions_run`, `sports_mma`, `touch_double`. F0-08 baja el mínimo; cada ticket de F1 en adelante baja los que use, con el mismo procedimiento.
- **Movimiento de las pantallas nuevas** (valores del diseño): el anillo del descanso avanza con una transición de 400 ms; el paso de hielo a brasa es un fundido de 300 ms; el anillo de los intervalos avanza lineal 950 ms por segundo y el brillo de fondo cambia en 700 ms; el número grande de los últimos 5 s late (de escala 1,18 y opacidad 0,55 a 1 en 420 ms); las tarjetas nuevas entran subiendo 16 dp con un fundido de 420 ms. Lo que se mueve en el espacio sigue usando los resortes de 12.4, y con Reducir movimiento todo pasa a fundidos cortos.
- **Vibraciones nuevas (propuestas; todavía no están en `ForjaHaptics`):**

| Momento | Android | iOS |
| --- | --- | --- |
| Empieza el trabajo en intervalos, EMOM o ronda | patrón 0, 250 ms | impacto rígido |
| Empieza el descanso en intervalos | patrón 0, 80, 60, 80 ms | impacto suave dos veces |
| Cambio de postura o de fase de respiración | `CLOCK_TICK` | selección |
| Llega una propuesta del ajuste en vivo | Ninguna en el teléfono (llega en el descanso, que ya vibró) | Ninguna; en el reloj, el aviso normal |

---

## 13. Plan de trabajo: etapas, fases y tickets

### 13.1 Etapas y fases

El plan v3 tiene cuatro etapas (sección 1 y `docs/PLAN_V3.md`, sección 14). Solo la Etapa 1 tiene fases y tickets; las otras tres se detallan cuando se cierre la anterior (13.9).

| Fase de la Etapa 1 | Qué entrega | Lista cuando | Semanas |
| --- | --- | --- | --- |
| F0 Base técnica | KMP con androidApp, iosApp y wearApp; CI en verde; sistema de diseño; Room 3 con migraciones probadas; Koin; Navigation 3; textos ES/EN | El CI está en verde y una pantalla guarda y lee datos en los dos teléfonos | 1 a 2 |
| F1 Núcleo sin conexión, disciplinas y tiempo | Lo de la v2 (catálogo y ejercicios propios, carpetas y rutinas, sesión con registro rápido, superseries, tipos de registro, temporizador, calculadoras, historial, copia, exportación y onboarding) más bloques con formato y disciplinas, el motor de tiempo, el descanso a pantalla completa, la Live Activity y la Live Update, voz, intervalos a tu medida, funcional, flujo y respiración, PAR-Q+, modo joven y ocultar números | Juan usa la app 2 semanas en entrenos reales (fuerza y al menos unos intervalos y un flujo) sin perder un dato, y el descanso avisa a su hora con el teléfono bloqueado | 12 a 17 |
| F2 Progresión y análisis | Lo de la v2 (sugerencias, récords, gráficas, estancamiento, feedback, series por músculo, mapa, medidas, fotos y resúmenes) más escaleras de ejercicios, resultado y récords por formato, calentamiento según recuperación y récord en vivo en la isla | El motor tiene 90 % de cobertura y sus sugerencias cuadran con el historial de Juan | 6 a 8 |
| F3 Cuenta y sincronización | Supabase, PowerSync, inicio de sesión con correo, Google y Apple, y borrado de cuenta | El mismo historial aparece en dos dispositivos tras editar ambos en modo avión | 3 a 4 |
| F4 Salud, adaptación y relojes | Leer y escribir Salud y Health Connect, check-in, adaptación antes y durante la sesión, molestias, zonas de pulso, descanso por pulso, la app completa del Galaxy Watch y la del Apple Watch | 2 semanas entrenando con los dos relojes y ajustes que se entienden | 11 a 16 |
| F5 Programas, catálogo y extras | Lo de la v2 (programas por semanas, biblioteca, perfiles de gimnasio, sustitutos, fusionar, importar Hevy y Strong, tarjeta para compartir, logros y rachas) más el catálogo de unos 1.000 ejercicios y posturas y los ejercicios adaptados | Un CSV real de Hevy o Strong entra sin errores y el catálogo pasa la revisión de licencias | 6 a 10 |
| F6 Lanzamiento | Pulido, accesibilidad, privacidad (salud y menores), declaraciones de las tiendas, apps de reloj publicadas, prueba cerrada, TestFlight, fichas y nombre definitivo | Aprobada en Google Play y en App Store, con los dos relojes | 4 a 6 |

Total de la Etapa 1: 43 a 63 semanas a medio tiempo (estimación gruesa). Desde F1 la app va al S25 Ultra y al iPhone 18 Pro como beta interna; la beta abierta con gente del gimnasio desde la mitad de la Etapa 1 es una decisión abierta (13.10).

### 13.2 Cómo leer un ticket

- Los de F0 están completos abajo. Los de F1 a F6 vienen en tablas de resumen: el jefe escribe cada uno completo con la plantilla 15.3 cuando toca, con los `cat` que necesita la hormiga.
- Cada ticket trae **rama**, **archivos** (los que se crean o cambian), **pasos**, **terminado cuando** y **verificación**.
- Se hacen en orden, salvo que se diga lo contrario.
- **Bloqueado por diseño:** si el ticket construye una pantalla que no está en `docs/PANTALLAS.md`, el jefe no lo escribe: le pide el diseño a Juan con la plantilla 15.7.
- La columna "v2" dice qué ticket de la versión anterior del manual reemplaza, para no perder el hilo.

### 13.3 Tickets para cerrar F0

Estado el 25 de octubre en `d3c2101`: ninguno cerrado; F0-06, F0-09 y F0-11 van a medias. Orden: **F0-00 y F0-01 primero**; después cualquier orden, salvo F0-09 antes de F0-10 (el vidrio va sobre `NavDisplay`) y F0-11 antes de F0-05 (así F0-05 ya no encuentra accessors en wearApp).

**F0-00 · Subir `ForjaShapes.kt` y dejar el PR #1 en verde (Juan, sin hormiga)**
- Por qué: `ForjaTheme.kt` usa `ForjaShapes` y `LocalForjaShapes`, pero el archivo nunca entró a git; nada compila (4.6, fila 0).
- Rama: la misma del PR, `feat/f0-forja-motion-haptics-ci`.
- Pasos:
  1. `git status`. Si `shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaShapes.kt` sale como archivo sin seguimiento, es el de Juan: revisarlo contra el de abajo y subirlo. Si no existe, crearlo completo desde el chat con este contenido (probado por Claude el 25 de octubre: con él compilan los dos APK y pasan las 18 pruebas).
  2. `./gradlew :androidApp:assembleDebug :wearApp:assembleDebug`.
  3. Commit y push; esperar el CI del PR #1 en verde (los dos jobs).

```kotlin
package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

/**
 * Formas de Forja que el tema entrega a toda la app.
 * Usan los radios de [ForjaRadius]: 14, 20, 28, 36 y completo.
 */
@Immutable
data class ForjaShapes(
    // Campos, teclas y chips pequeños (radio 14).
    val small: Shape = ForjaRadius.shapeS,
    // Tarjetas compactas, filas de serie y baldosas (radio 20).
    val medium: Shape = ForjaRadius.shapeM,
    // Tarjetas grandes (radio 28).
    val large: Shape = ForjaRadius.shapeL,
    // Hojas inferiores: solo las esquinas de arriba (radio 36).
    val sheet: Shape = ForjaRadius.shapeXlTop,
    // Botones, píldoras, la isla y los círculos.
    val full: Shape = ForjaRadius.shapeFull,
)

// Se lee con ForjaTheme.shapes; ForjaTheme la provee con LocalForjaShapes.
val LocalForjaShapes = staticCompositionLocalOf { ForjaShapes() }
```

- Git:

```
git status
git add shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/theme/ForjaShapes.kt
git commit -m "fix(ui): agregar ForjaShapes que ForjaTheme ya usaba"
git push
git show --stat HEAD
```

- Terminado cuando: `git show --stat HEAD` lista el archivo y los dos jobs del CI del PR #1 están en verde.

**F0-01 · Fusionar la base actual (Juan, sin hormiga)**
- Estado en `d3c2101`: pendiente; espera a F0-00.
- Pasos: revisar que el CI del PR #1 esté en verde en GitHub; correr la app desde Android Studio en el S25 Ultra, desde Xcode en el iPhone 18 Pro y el reloj con `./gradlew :wearApp:installDebug` (pasos en 14.3, 14.4 y 14.5); abrir Buscar y comprobar que salen 77 ejercicios; fusionar el PR #1 en `main` y borrar las ramas `feat/capas-shared` y `feat/sistema-forja`.
- Terminado cuando: `main` tiene el commit de F0-00 y la app abre en los dos teléfonos (con el catálogo) y en el Galaxy Watch4.
- Si falla en el iPhone: pegar al jefe el error de Xcode completo. Es probable que sea de firma (14.4).

**F0-02 · Restaurar ExerciseSearchTest**
- Estado en `d3c2101`: sin empezar; el archivo sigue vacío.
- Rama: `test/f0-exercise-search`.
- Archivos: `shared/domain/src/commonTest/kotlin/com/olarte/ironprogress/domain/catalog/ExerciseSearchTest.kt` (nuevo contenido). Leer antes `SearchExercises.kt`, `Exercise.kt`, `ExerciseAttributes.kt` y `Muscle.kt`.
- Pasos: crear una lista pequeña de ejercicios de prueba dentro del archivo (no usar `SeedCatalog`, que es de data) y escribir estas 10 pruebas:
  1. Texto vacío devuelve todos los no archivados, en orden alfabético del idioma pedido.
  2. Ignora tildes y mayúsculas: "jalon" encuentra "Jalón al pecho".
  3. Busca en inglés aunque la pantalla esté en español: "bench" encuentra "Press banca con barra".
  4. Cada palabra debe ser el comienzo de alguna palabra del nombre: "curl bic" y "bic curl" encuentran "Curl de bíceps con barra".
  5. Una palabra que no es comienzo de ninguna no encuentra nada: "ceps" no encuentra "bíceps".
  6. Orden por relevancia: primero los que empiezan por la frase, luego los que coinciden en la primera palabra, luego el resto.
  7. El filtro de músculo mira solo los primarios: con TRICEPS, un press banca (tríceps secundario) no sale.
  8. El filtro de equipo deja solo ese equipo.
  9. Los archivados se ocultan salvo con `includeArchived = true`.
  10. `normalizedForSearch()`: "Extensión de Tríceps" queda "extension de triceps" y "press-banca  (barra)" queda "press banca barra".
- Terminado cuando: `./gradlew :shared:domain:testAndroidHostTest` pasa con 16 pruebas en domain.

**F0-03 · minSdk 28 y permiso de vibración**
- Estado en `d3c2101`: sin empezar (`minSdk` 24 y sin `VIBRATE` en el teléfono). El `minSdk` del reloj va aparte, en F0-11.
- Rama: `fix/f0-minsdk-vibrate`.
- Archivos: `gradle/libs.versions.toml` (`android-minSdk = "28"`), `androidApp/src/main/AndroidManifest.xml` (agregar `<uses-permission android:name="android.permission.VIBRATE" />`).
- Todos los módulos leen `libs.versions.android.minSdk`, así que basta ese cambio.
- Terminado cuando: las pruebas pasan, `:androidApp:assembleDebug` compila y la app instala en el S25 Ultra.

**F0-04 · Idioma del sistema y textos en recursos**
- Estado en `d3c2101`: sin empezar.
- Rama: `feat/f0-locale`.
- Archivos: `composeResources/values/strings.xml` (pasa a inglés, con el contenido de `values-en`), `composeResources/values-es/strings.xml` (español), borrar `composeResources/values-en/strings.xml` con `git rm`, `IronProgressApp.kt`, `ForjaIsland.kt` si recibe textos.
- Pasos: agregar a los dos `strings.xml` todos los textos visibles que hoy están en el código (pestañas, buscar, banner de sesión, botones del prototipo); en `IronProgressApp` tomar el idioma con `Locale.current.language` (`androidx.compose.ui.text.intl.Locale`) en vez de `"es"`; mantener la llave del ViewModel con el idioma.
- Terminado cuando: con el teléfono en inglés todo sale en inglés, en español todo en español y en francés todo en inglés, en Android y en iPhone.

**F0-05 · Quitar los accessors `compose.*` deprecados**
- Estado en `d3c2101`: sin empezar. Si F0-11 ya pasó, wearApp no tiene accessors.
- Rama: `chore/f0-compose-deps`.
- Archivos: `shared/build.gradle.kts`, `shared/ui/build.gradle.kts`, `gradle/libs.versions.toml` (agregar `androidx-lifecycle-viewmodel` de 5.2).
- Pasos: Juan corre `./gradlew :shared:ui:compileKotlinIosSimulatorArm64 --warning-mode all` y pega las líneas de deprecación; la hormiga reemplaza cada accessor por la coordenada que sugiere el aviso, usando las entradas que ya existen en el catálogo (`libs.compose.runtime`, `libs.compose.foundation`, `libs.compose.material3`, `libs.compose.ui`, `libs.compose.components.resources`) y cambia las tres coordenadas de lifecycle escritas a mano por `libs.androidx.lifecycle.viewmodel`, `libs.androidx.lifecycle.viewmodelCompose` y `libs.androidx.lifecycle.runtimeCompose`.
- Terminado cuando: no quedan avisos de esos accessors, compilan Android y el framework iOS, y la app se ve igual.

**F0-06 · ForjaTheme completo (tema del sistema, movimiento y vibración)**
- Estado en `d3c2101`: a medias. `ForjaTheme` ya provee formas, movimiento y vibración y existen `ForjaTheme.motion` y `ForjaTheme.haptics`; falta leer el tema y el reducir movimiento del sistema, borrar el `object ForjaMotion` duplicado, el paso 5 y el `[cite: 1]` del comentario (4.6, filas 6, 7, 16 y 21).
- Rama: `feat/f0-forja-theme`.
- Archivos: `ForjaTheme.kt`, `ForjaMetrics.kt` (quitar `object ForjaMotion`), `ForjaMotion.kt`, nuevo `ReduceMotion.kt` con `@Composable expect fun rememberReduceMotionEnabled(): Boolean` más `ReduceMotion.android.kt` y `ReduceMotion.ios.kt`, `CatalogScreen.kt`, `ForjaIsland.kt`, `IronProgressApp.kt` (cambiar `ForjaMotion.x()` por el esquema del tema), `MainActivity.kt`.
- Pasos:
  1. `ForjaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` provee `LocalForjaColors`, `LocalForjaTypography`, `LocalForjaShapes`, `LocalForjaMotion provides ForjaMotionScheme(reduceMotion = rememberReduceMotionEnabled())` y `LocalForjaHaptics provides rememberForjaHaptics()`.
  2. Agregar `ForjaTheme.motion` y `ForjaTheme.haptics` al objeto `ForjaTheme`.
  3. Reducir movimiento: en Android, `Settings.Global.ANIMATOR_DURATION_SCALE` igual a 0; en iOS, la función de UIKit para "Reducir movimiento" (confirmar el nombre con autocompletado en `platform.UIKit`).
  4. Reemplazar todos los usos de `ForjaMotion.` (`grep -rn "ForjaMotion\." shared --include="*.kt"`) por `ForjaTheme.motion.` y borrar el objeto viejo.
  5. `MainActivity`: `enableEdgeToEdge()` sin estilos fijos, para que las barras sigan el tema.
  6. Conectar al menos `haptics.setCompleted()` en "Completar serie" del prototipo para probar.
- Terminado cuando: con el teléfono en claro la app sale en claro y en oscuro sale en oscuro; con reducir movimiento activado las animaciones son cortas; completar serie vibra en los dos teléfonos.
- Decisión de Juan: si prefiere oscuro siempre hasta F1, el valor por defecto queda `true` y el ajuste llega con F1-25.

**F0-07 · Fuentes Archivo**
- Estado en `d3c2101`: sin empezar.
- Rama: `feat/f0-archivo-font`.
- Archivos: seis TTF en `shared/ui/src/commonMain/composeResources/font/` con nombres en minúscula y guion bajo: `archivo_regular.ttf`, `archivo_medium.ttf`, `archivo_semibold.ttf`, `archivo_bold.ttf`, `archivo_semiexpanded_semibold.ttf`, `archivo_semiexpanded_bold.ttf`; la licencia en `licenses/Archivo-OFL.txt` (fuera de `composeResources`); `ForjaTypography.kt`; `ForjaTheme.kt`.
- Pasos: Juan descarga Archivo desde Google Fonts y toma los archivos de la carpeta `static` del zip; la hormiga arma dos `FontFamily` con `org.jetbrains.compose.resources.Font(Res.font.archivo_regular, FontWeight.Normal)` y similares (son `@Composable`), y `ForjaTheme` llama `createForjaTypography(textFamily, figureFamily)` con ellas. Las cifras usan la familia SemiExpanded con `fontFeatureSettings = "tnum"` (ya está) y la cifra héroe con `"pnum"`.
- Terminado cuando: los textos y números se ven en Archivo en los dos teléfonos.

**F0-08 · Íconos Material Symbols Rounded**
- Estado en `d3c2101`: sin empezar. Los íconos que piden las pantallas nuevas están en 12.12.
- Rama: `feat/f0-material-symbols`.
- Archivos: XML en `shared/ui/src/commonMain/composeResources/drawable/` (por ejemplo `ic_local_fire_department.xml` e `ic_local_fire_department_filled.xml`), `ForjaIcons.kt` (pasa a devolver recursos o se borra), `ForjaIsland.kt` y las pantallas que usen íconos.
- Pasos: Juan descarga de fonts.google.com/icons cada ícono en estilo Rounded, peso 400, grado 0, tamaño óptico 24, con relleno apagado y encendido, formato Android. Íconos mínimos: `local_fire_department`, `calendar_month`, `monitoring`, `search`, `arrow_back`, `chevron_right`, `north_east`, `play_arrow`, `check`, `close`, `add`, `remove`, `timer`, `more_vert`. Si el XML trae `android:tint` con `?attr/...`, se borra esa línea; si trae `android:fillColor="@android:color/white"`, se cambia por `#FFFFFFFF`. Se usan con `painterResource(Res.drawable.ic_search)` y el `tint` de `Icon`.
- Terminado cuando: la isla muestra los íconos reales, relleno solo en la pestaña elegida, en Android y en iPhone.

**F0-09 · Navigation 3 y ViewModels con Koin**
- Estado en `d3c2101`: a medias, por otro camino: hay una pila propia (`ForjaNavDestination` en `remember`) que este ticket reemplaza entera, y `CatalogViewModel` se sigue creando a mano.
- Rama: `feat/f0-navigation3`.
- Archivos: `gradle/libs.versions.toml` (5.2), `shared/ui/build.gradle.kts` (plugin `alias(libs.plugins.kotlinSerialization)`; dependencias `libs.androidx.navigation3.ui`, `libs.androidx.lifecycle.viewmodelNavigation3`, `libs.kotlinx.serialization.core`, `libs.koin.compose.viewmodel`), nuevo `shared/ui/src/commonMain/kotlin/com/olarte/ironprogress/ui/navigation/Route.kt`, nuevo `.../ui/navigation/IronProgressNavHost.kt`, nuevo `.../ui/di/UiModule.kt`, `IronProgressApp.kt` (deja de crear el ViewModel a mano), `shared/src/commonMain/.../App.kt` (agrega `uiModule`), `CatalogScreen.kt` (separar `CatalogRoute` y `CatalogScreen`).
- Patrón (API verificada en el código fuente de navigation3-runtime 1.1.7, navigation3-ui 1.1.2 y lifecycle-viewmodel-navigation3 2.11.0):

```kotlin
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Today : Route
    @Serializable data object Plan : Route
    @Serializable data object Progress : Route
    @Serializable data object Catalog : Route
    @Serializable data class ExerciseDetail(val exerciseId: String) : Route
}

// Sin esta configuración la pila usa reflexión y falla en iOS.
private val navConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Today::class, Route.Today.serializer())
            subclass(Route.Plan::class, Route.Plan.serializer())
            subclass(Route.Progress::class, Route.Progress.serializer())
            subclass(Route.Catalog::class, Route.Catalog.serializer())
            subclass(Route.ExerciseDetail::class, Route.ExerciseDetail.serializer())
        }
    }
}

@Composable
fun IronProgressNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(navConfiguration, Route.Today)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.Today> { /* TodayRoute(...) */ }
            entry<Route.Plan> { /* PlanRoute(...) */ }
            entry<Route.Progress> { /* ProgressRoute(...) */ }
            entry<Route.Catalog> { CatalogRoute() }
            entry<Route.ExerciseDetail> { key -> /* ExerciseDetailRoute(key.exerciseId) */ }
        },
    )
}
```

- Isla: tocar una pestaña reemplaza la raíz de la pila (`backStack.clear()` y `backStack.add(tab)`); tocar la pestaña actual sube al inicio solo su lista; Buscar agrega `Route.Catalog` encima. El estado de la isla (seleccionada, minimizada) se deriva de la ruta superior.
- Koin: `val uiModule = module { viewModel { (languageCode: String) -> CatalogViewModel(get(), languageCode) } }` y en `CatalogRoute`: `koinViewModel<CatalogViewModel>(key = "catalog-$languageCode") { parametersOf(languageCode) }`.
- Terminado cuando: las cuatro rutas navegan, el botón o gesto de atrás vuelve en Android y en iPhone, girar el teléfono o mandar la app a segundo plano no pierde la ruta ni el texto de búsqueda, y no queda ningún `viewModel { }` manual.

**F0-10 · Vidrio con Haze en la isla**
- Estado en `d3c2101`: sin empezar. Va después de F0-09.
- Rama: `feat/f0-glass`.
- Archivos: `gradle/libs.versions.toml` (5.2), `shared/ui/build.gradle.kts` (`libs.haze.core`, `libs.haze.blur`), `IronProgressApp.kt` o `IronProgressNavHost.kt` (`hazeSource`), `ForjaIsland.kt` (`hazeBlur`), con el código de 12.6.
- Terminado cuando: el contenido se ve desenfocado detrás de la isla en los dos teléfonos y el scroll sigue fluido en el iPhone a 120 Hz.

**F0-11 · Módulo wearApp con Wear Compose**
- Estado en `d3c2101`: a medias. El módulo existe, está en `settings.gradle.kts` y en el CI, y abre dos caras de prueba; pero usa el Material 3 del teléfono, el plugin de Compose Multiplatform, `minSdk` 28 y `targetSdk` 35 escritos a mano, no tiene la meta-data `standalone` y cuenta segundos con `delay` (4.6, fila 13).
- Rama: `feat/f0-wearapp`.
- Archivos: `gradle/libs.versions.toml` (las líneas de Wear de 5.2), `wearApp/build.gradle.kts` (completo, con la plantilla de abajo), `wearApp/src/main/AndroidManifest.xml`, `wearApp/src/main/kotlin/com/olarte/ironprogress/wear/WearMainActivity.kt` (se queda ese nombre), nuevo `wearApp/src/main/res/values/strings.xml` con `app_name`.
- Plantilla de `wearApp/build.gradle.kts` (como androidApp, sin el plugin de Compose Multiplatform):

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
```

- Manifest: se quedan `uses-feature` de reloj, `VIBRATE`, `WAKE_LOCK`, el tema `@android:style/Theme.DeviceDefault` y la actividad con `MAIN` y `LAUNCHER`; dentro de `<application>` se agrega `<meta-data android:name="com.google.android.wearable.standalone" android:value="false" />`. Es `false` porque el reloj necesita la app del teléfono instalada para recibir rutinas; durante la sesión funciona aunque el teléfono no esté cerca (F4-13).
- Pantalla: `androidx.wear.compose.material3.MaterialTheme` con los colores de `ForjaDarkColors` y un `Text` "IronProgress" centrado sobre negro. Las dos caras de prueba de `d3c2101` se borran: la cara del descanso vuelve en F4-13 movida por el motor de tiempo (11.9). Nada de pulso todavía (F4-07). Nada de `androidx.compose.material3` en el código del reloj.
- Terminado cuando: `./gradlew :wearApp:assembleDebug` compila en el CI, `grep -rn "androidx.compose.material3" wearApp` no encuentra nada y la app abre en el Galaxy Watch4 (pasos en 14.5).

**F0-12 · Criterio de salida de F0: guardar y leer en ambos (propuesta)**
- Estado en `d3c2101`: sin empezar.
- Rama: `feat/f0-archive-exercise`.
- Idea: archivar y restaurar un ejercicio desde el catálogo. Escribe en Room, usa la columna `is_archived` que ya existe (sin migración) y sirve después para RF-CAT-02.
- Archivos: `ExerciseRepository.kt` (`suspend fun setArchived(id: ExerciseId, archived: Boolean)`), `CatalogDao.kt` (`@Query("UPDATE exercises SET is_archived = :archived WHERE id = :exerciseId")`), `RoomExerciseRepository.kt`, `SeedExerciseRepository.kt`, `RoomExerciseRepositoryTest.kt` (el DAO falso implementa lo nuevo), `CatalogDatabaseTest.kt`, `CatalogContract.kt`, `CatalogViewModel.kt`, `CatalogScreen.kt`, `strings.xml` de los dos idiomas.
- Corrige además el bug 14 de 4.6: al re-sembrar, conservar el `is_archived` de las filas que ya existen.
- Pruebas nuevas: archivar persiste y el ejercicio desaparece de la búsqueda; restaurar lo devuelve; re-sembrar con una versión mayor conserva el archivado.
- Terminado cuando: Juan archiva un ejercicio, cierra la app del todo, la abre y sigue archivado, en el S25 Ultra y en el iPhone 18 Pro.

**F0-13 · Calidad en el CI**
- Estado en `d3c2101`: sin empezar. El CI ya compila el APK del reloj.
- Rama: `ci/f0-quality`.
- Archivos: `gradle/libs.versions.toml` (ktlint y kover), `build.gradle.kts` raíz (aplicar ktlint a todos los subproyectos), `.editorconfig` nuevo (con `ktlint_function_naming_ignore_when_annotated_with = Composable`), `shared/domain/build.gradle.kts` (kover), `.github/workflows/ci.yml`.
- Pasos: primero `./gradlew ktlintFormat` en un commit propio; luego el CI agrega `ktlintCheck`, el reporte de Kover de domain y `:shared:linkDebugFrameworkIosArm64` en el job de macOS. detekt queda para cuando detekt 2.0 sea estable.
- Terminado cuando: el CI corre todo en verde.

**F0-14 · Cierre de F0**
- Estado en `d3c2101`: sin empezar.
- Rama: `docs/f0-close`.
- Archivos: `README.md` (qué es la app, módulos, cómo correr en Android, iPhone y reloj, cómo probar), `iosApp/Configuration/Config.xcconfig` (`TEAM_ID` de Juan, si él quiere versionarlo), `HANDOFF.md` (bitácora y estado) y `docs/` si algo cambió.
- Terminado cuando: F0 cumple su criterio de salida y la bitácora lo muestra con commits.

### 13.4 Tickets de F1 (núcleo sin conexión, disciplinas y tiempo)

El orden importa: el motor de tiempo (F1-02) y la base (F1-03) van antes que cualquier pantalla que los use, y el arranque sin pantalla (F1-13) antes del descanso con la app cerrada.

| Ticket | v2 | Qué | Requisitos | Terminado cuando |
| --- | --- | --- | --- | --- |
| F1-01 | F1-01 | Modelos de dominio de rutinas, sesión y bloques: Folder, Routine (con `createdBy`), RoutineExercise, SetTemplate, WorkoutSession (con `discipline`), SessionExercise, WorkoutSet (con `blockId`, `round` y los campos nuevos), WorkoutBlock, `BlockFormat`, `Discipline`, `SetType`, `SessionStatus` y `Side`, más los valores nuevos de `TrackingType`, `SetField`, `Equipment` y `MovementPattern` al final de cada lista (10.5); interfaces de repositorio y casos de uso básicos, con ids UUID v7 y `Clock` inyectado | RUT-01, SES-01, SES-02, DIS-01, DIS-02, DIS-03 | Pruebas de dominio en verde (rangos válidos, orden, superseries, bloques) y las de `ExerciseTest` siguen pasando |
| F1-02 | nuevo | Motor de tiempo en `domain/time` (11.9): plan por formato, `stateAt`, `eventsBetween`, pausas, más o menos 15 s y saltar | DIS-04, RNF-17 | Las pruebas de 11.9 en verde, exactas al milisegundo |
| F1-03 | F1-02 | Room versión 2: tablas de la v2 con columnas de sincronización (10.2) y las nuevas de F1 (10.3: bloques, plantillas de intervalos, patrones de respiración, exigencia, fuente, escaleras, tamizaje y consentimiento); columnas nuevas en `exercises` y `exercise_muscles` (10.5); DAOs; migración 1 a 2; `2.json` versionado; prueba de migración desde `1.json`; copia de la base antes de migrar | RNF-03 | Prueba de migración en verde; la base de Juan con ejercicios archivados sobrevive |
| F1-04 | F1-03 | Repositorios Room de rutinas, sesión y bloques, conectados en Koin | | Pruebas de DAO en `androidHostTest` |
| F1-05 | parte de F1-12 | Formato local de números, pesos, duraciones y fechas con `expect`/`actual` en `shared:ui` (Android con `java.text.NumberFormat`, iOS con `NSNumberFormatter`) | RNF-07 | "62,5 kg" y "8.420 kg" en español, "62.5 kg" y "8,420 kg" en inglés, en los dos teléfonos |
| F1-06 | nuevo | Exigencia, músculos con porcentaje, fuente y escaleras de los 77 ejercicios (11.10), con `SeedCatalog.VERSION = 2` y los mismos ids; ficha de ejercicio (`docs/PANTALLAS.md` 7.5) | DIS-13 (los 77), DIS-01 | Los 77 tienen exigencia y porcentajes que suman 100, Juan los revisó y la ficha se ve como en el diseño |
| F1-07 | F1-04 | Ejercicios propios: crear, editar y archivar, con id UUID v7 y `is_custom = true`; filtros de equipo y de disciplina en el catálogo | CAT-02, CAT-03 | Un ejercicio propio aparece en la búsqueda en los dos idiomas |
| F1-08 | F1-05 | Carpetas y rutinas: lista, editor de rutina por bloques (en los de series: ejercicios ordenables, series objetivo, rango de reps, RIR objetivo, descanso y notas), duplicar, borrar, ordenar por arrastre. Los otros formatos se arman con su pantalla propia (F1-18 a F1-20) | RUT-01, RUT-02, DIS-02 | Juan arma sus rutinas reales de fuerza |
| F1-09 | F1-06 | Siguiente rutina por rotación y pantalla Hoy real (`docs/PANTALLAS.md` 1.1; deja de ser prototipo) | RUT-03 | Hoy muestra la rutina que toca |
| F1-10 | F1-07 | Sesión activa y pantalla Entreno (`docs/PANTALLAS.md` 1.2 y 1.3): iniciar libre o desde rutina; fila de serie con la serie anterior y copiar en un toque; completar en un toque; teclado numérico propio; botones de ±2,5; RIR o RPE, tipo de serie y nota; la barra cambia su texto según el formato del bloque; guardado en Room en cada cambio; recuperar la sesión tras cerrar la app; aviso de Deshacer que se va solo a los 5 s | SES-01, SES-02, SES-03, SES-07, RNF-01 | Matar la app en mitad de una sesión y volver sin perder nada; guardar una serie en menos de 100 ms |
| F1-11 | nuevo | Descanso a pantalla completa y su barra (`docs/PANTALLAS.md` 5.1 y 1.4), leyendo el motor de tiempo: crece desde la barra al completar, deslizar abajo lo devuelve a la barra, al estar listo vuelve solo con la siguiente serie; más o menos 15 s y saltar; vibración de los últimos 3 s y del fin | VIV-08, TMR-01, TMR-03 | Se ve como en el diseño y el tiempo coincide con el del motor después de 20 descansos |
| F1-12 | F1-08 | Reordenar, saltar y sustituir ejercicios, añadir y quitar series, superseries y circuitos; los 8 tipos de registro de la v2 y los nuevos (calorías, distancia sola, altura, respiraciones, vatios; rest-pause, myo-reps y cluster) | SES-04, SES-05, SES-06, DIS-03 | Una sesión real con superserie, un ejercicio por tiempo y uno por calorías |
| F1-13 | nuevo | Arranque sin pantalla: Koin y la base arrancan antes de la UI (`startKoin` en `Application.onCreate` con el `Context` pasado a mano, sin `koin-android`, y al iniciar la app en iOS) y `App()` deja de crear Koin dentro de Compose. Lo necesitan el servicio del descanso y los botones de la Live Activity, que corren sin pantalla. Confirmar con el autocompletado cómo se usa Koin 4.1.0 desde Compose cuando ya arrancó | Base de F1-14 y F1-16 | La app abre igual y una prueba arranca Koin y lee el catálogo sin Compose |
| F1-14 | F1-09 | Descanso con la app cerrada en Android: servicio en primer plano tipo `health` y Live Update. Notificación en curso con el instante de fin y el cronómetro del sistema en cuenta atrás (`setWhen`, `setUsesChronometer`, `setChronometerCountDown`); en Android 16, pedir la promoción a Live Update para la barra de estado, la pantalla bloqueada y la Now Bar (`setRequestPromotedOngoing(true)`, el permiso `POST_PROMOTED_NOTIFICATIONS` en el manifest y `setShortCriticalText` para el chip de la barra de estado; tiene que ser en curso, con título, sin vistas propias y sin color de fondo, y si el sistema no la promueve queda como notificación en curso normal); en Android 17, `MetricStyle` donde exista. Acciones Completar serie, +15 s y Saltar donde el sistema las muestre; sonido y vibración al terminar. Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`, `POST_NOTIFICATIONS` (permiso en tiempo de ejecución) y el requisito del tipo health (`HIGH_SAMPLING_RATE_SENSORS` o un permiso como `ACTIVITY_RECOGNITION`: decisión abierta 13.10). Bloqueado por diseño en lo visual: los textos del chip y la vista expandida de la Live Update y de la Now Bar se piden con 15.7 | TMR-01, TMR-02, TMR-03, VIV-04 | Descanso que avisa a su hora con el S25 Ultra bloqueado y se ve en la Now Bar |
| F1-15 | F1-10 | Descanso con la app cerrada en iPhone, respaldo: notificación local a la hora de fin con `UNUserNotificationCenter` desde `iosMain`, permiso de notificaciones, reprogramar al sumar o restar 15 s y cancelar al saltar | TMR-01, TMR-02, TMR-03 | Descanso que avisa a su hora con el iPhone bloqueado |
| F1-16 | nuevo | Live Activity del entreno: extensión de widgets en Swift con ActivityKit (compacta, mínima, expandida, pantalla bloqueada y StandBy; horizontal en iOS 27); contador con `Text(timerInterval:)`; botones con App Intents (`LiveActivityIntent`): Completar serie, -15 s, +15 s y Saltar; `ForjaTokens.swift` (12.10). El estado llega desde Kotlin por una interfaz en `shared` que Swift implementa y Koin inyecta. Bloqueado por diseño: las cuatro vistas no están en `docs/PANTALLAS.md` (12.10 trae las reglas) | VIV-01, VIV-02, RNF-15 | Con el iPhone bloqueado la isla cuenta, los botones guardan en la base y la serie aparece al abrir la app; en una sesión de 60 min no se actualiza cada segundo |
| F1-17 | nuevo | Voz y audio con `expect`/`actual` en `shared:ui`: `AVSpeechSynthesizer` y `AVAudioSession` con `duckOthers` en iPhone; `TextToSpeech` y foco de audio con atenuación (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) en Android. Voz grabada para las cuentas y las frases fijas en `composeResources/files/voice/es/` y `.../en/`, y la del sistema para los nombres de ejercicios; mientras no exista la voz grabada (13.10), todo con la del sistema. Los eventos salen del motor de tiempo | DIS-08 | Con música sonando, la voz cuenta, la música baja y vuelve, en los dos teléfonos y con la pantalla apagada |
| F1-18 | nuevo | Intervalos a tu medida: crear y correr con voz (`docs/PANTALLAS.md` 5.2 y 5.3); plantillas `IntervalTemplate`; reps por ronda en el descanso con un toque; resultado del bloque | DIS-12, DIS-02 | "15 s a tope y 45 s de descanso, 10 rondas" corre con la pantalla bloqueada y guarda las reps de cada ronda |
| F1-19 | nuevo | Bloque funcional: EMOM, AMRAP, por tiempo con límite y rondas en la misma pantalla (`docs/PANTALLAS.md` 7.4); marcador de rondas más reps, marca anterior y ritmo; resultado guardado | DIS-02, DIS-07 (guardar) | Un AMRAP de 12 min y un EMOM de 10 con el resultado correcto en el historial |
| F1-20 | nuevo | Disciplinas, flujo y respiración: pantalla de disciplinas, flujo de yoga, pilates y movilidad con figuras vectoriales propias, y respiración guiada con sus patrones y el aviso de seguridad (`docs/PANTALLAS.md` 7.1, 7.2 y 7.3). Pocas posturas y patrones de ejemplo; el catálogo grande llega en F5-09 | DIS-01, DIS-08, DIS-09 | Saludo al sol A de 5 rondas y 5 min de respiración cuadrada con voz y vibración, con la pantalla bloqueada |
| F1-21 | F1-11 | Terminar con Deslizar para terminar y Resumen (`docs/PANTALLAS.md` 1.5 y 1.6): duración, volumen, récords simples, comparación con la sesión anterior y el resultado de cada bloque | SES-08 | Resumen con números correctos contra una sesión de prueba |
| F1-22 | F1-12 | Calculadora de discos por lado y calentamiento sugerido; kg o lb por ejercicio | SES-09, SES-10 | 62,5 kg muestra "137,8 lb" con la unidad cambiada y se guarda en kg |
| F1-23 | F1-13 | Historial por calendario y por lista, con edición y borrado de series y bloques pasados | ANA-01 | Editar una serie de ayer y verla corregida |
| F1-24 | F1-14 | Copia de seguridad restaurable (archivo de la base o JSON propio) y exportación a CSV y JSON con el selector de archivos de cada sistema | DAT-01 | Restaurar en otro teléfono una copia hecha en el primero |
| F1-25 | F1-15 | Onboarding (objetivo, experiencia, unidades, días por semana, disciplinas que haces) y Ajustes (idioma, tema, sonido, vibración, voz, ocultar peso corporal y calorías) con DataStore | CFG-01, CFG-02, CFG-03 | Cambiar idioma y tema desde Ajustes sin reiniciar; con "ocultar" encendido no se ve ningún peso corporal ni caloría |
| F1-26 | nuevo | PAR-Q+ al crear la cuenta y edad con modo joven: rango de edad (Declared Age Range en iPhone; año de nacimiento donde no exista), permiso de los padres donde la ley lo pida (`GuardianConsent`, 10.4) y los límites de 9.5 y 11.11 en toda la app. Bloqueado por diseño: PAR-Q+, modo joven y permiso de los padres no están en `docs/PANTALLAS.md`. Antes, confirmar con los autores del PAR-Q+ que se puede usar en una app comercial (13.10) | SEG-01, CFG-04, RNF-19 | Pruebas con cuentas de 13, 15 y 17 años en Colombia, España y EE. UU. |
| F1-27 | F1-16 | Accesibilidad base (TalkBack, VoiceOver, texto grande; los cronómetros anuncian los cambios de fase, no cada segundo), pruebas de UI de registrar una serie y recuperar la sesión, ViewModels con Turbine; beta interna de 2 semanas | RNF-06 | Criterio de salida de F1 (13.1) |

### 13.5 Tickets de F2 (progresión y análisis)

| Ticket | Qué | Requisitos |
| --- | --- | --- |
| F2-01 | Motor en `domain/progression`: 1RM estimado, doble progresión, incrementos y redondeo a discos, con explicación de una línea en recursos, y las reglas del modo joven (11.11) | PRG-01 |
| F2-02 | Mostrar la sugerencia en la fila de serie y registrarla en `WorkoutSet.suggestion` | PRG-01 |
| F2-03 | Récords automáticos (1RM estimado, mayor peso, más reps a un peso, mayor volumen) con aviso en vivo y vibración de récord | ANA-02 |
| F2-04 | Detalle de ejercicio con gráficas de 1RM estimado, mejor serie y volumen (`docs/PANTALLAS.md` 2.2; Canvas propio con Forja) | ANA-03 |
| F2-05 | Estancamiento y propuestas en orden (11.3) | PRG-02 |
| F2-06 | Feedback después de entrenar y agujetas antes de volver a entrenar el músculo; ajuste de series de la semana siguiente (11.4) | PRG-03 |
| F2-07 | Series efectivas por músculo (últimos 7 días, con los porcentajes de 11.10) y semáforo frente al rango | ANA-04 |
| F2-08 | Mapa corporal de recuperación en Progreso, con siluetas frente y espalda (`docs/PANTALLAS.md` 2.1) | ANA-05 |
| F2-09 | Peso corporal, medidas y fotos privadas; respetan "ocultar" y el modo joven (sin fotos) | ANA-06 |
| F2-10 | Resúmenes mensual y anual | ANA-07 |
| F2-11 | Cobertura de 90 % o más con Kover en el CI, en los motores de progresión, adaptación y tiempo | RNF-08 |
| F2-12 | Escaleras de progresión cuando no hay peso que subir (11.10) | DIS-06 |
| F2-13 | Resultado y récords por formato: mejor tiempo, más rondas más reps, más reps por ronda | DIS-07 |
| F2-14 | Calentamiento según la recuperación de los músculos del día: más series de aproximación en los ámbar ("Pecho aún en ámbar: 3 series de aproximación antes del press") | PRG-09 |
| F2-15 | Alerta de récord en la Live Activity y en la Live Update | VIV-05 (récord) |

### 13.6 Tickets de F3 (cuenta y sincronización)

Sin cambios de fondo con el v3; las tablas nuevas de 10.3 se sincronizan como las demás, y el consentimiento y el tamizaje van con Row Level Security.

| Ticket | Qué | Requisitos |
| --- | --- | --- |
| F3-00 | Exploración de 2 días: cómo se integra PowerSync con Room 3 en KMP, límites gratuitos de Supabase y PowerSync. Sin código de producción. Plan B: sincronización propia sobre Supabase | Riesgo |
| F3-01 | Proyecto Supabase: tablas espejo, Row Level Security en todas, pruebas de políticas con dos usuarios. Claves fuera del repo (`local.properties` y un xcconfig no versionado) | RNF-04 |
| F3-02 | Inicio de sesión con correo, Google y Apple (Apple también en Android si se ofrece Google en iOS, por la guía 4.8) | CTA-01 |
| F3-03 | Sincronización automática al volver internet | CTA-02 |
| F3-04 | Conflictos por escritura más reciente sin perder series (también las que llegan de los relojes) | CTA-03 |
| F3-05 | Borrar la cuenta y sus datos desde la app y desde una página web (con la firma de Juan en el pie) | CTA-04 |

### 13.7 Tickets de F4 (salud, adaptación y relojes)

F4 volvió a la Etapa 1 y creció. Los tickets del Apple Watch (F4-10 a F4-12) van cuando llegue el Series 12; se puede avanzar con el simulador de watchOS, pero se cierran en el reloj.

| Ticket | v2 | Qué | Requisitos |
| --- | --- | --- | --- |
| F4-00 | F4-00 | Exploración: con el Watch4 y el S25 Ultra, qué llega a Health Connect (sueño, pulso en reposo, VFC); con el iPhone, qué llega a Salud. En la Etapa 1 el Galaxy Watch usa Health Services; el Samsung Health Sensor SDK queda para la Etapa 3 | Riesgo |
| F4-01 | F4-01 | Health Connect: lectura con permisos por tipo de dato y `HealthAvailability` real, incluidos los entrenos y el sueño de otras apps (Garmin, Strava, Oura) | SAL-01, INT-01 |
| F4-02 | F4-02 | Health Connect: escribir cada sesión con el tipo de su disciplina (`docs/PLAN_V3.md`, sección 6) | SAL-02, SAL-10 |
| F4-03 | F4-07 | HealthKit: leer sueño, pulso, pulso en reposo y VFC (y los entrenos de Apple Fitness y otras apps) y escribir cada sesión con su tipo y el esfuerzo | SAL-04, SAL-10, INT-01 |
| F4-04 | F4-03 | Check-in manual de 10 s | PRG-05 |
| F4-05 | F4-04 | Adaptación antes de empezar (11.6): hoja "Hoy, versión ligera" y detalle (`docs/PANTALLAS.md` 2.4 y 5.4); migración con `adaptation_logs`. La preparación en detalle (todas las señales con su línea base) está bloqueada por diseño | PRG-04, ADA-01, ADA-03 |
| F4-06 | nuevo | Molestias y lesiones: zona y dolor de 0 a 10 por ejercicio, lesiones con fechas; el motor evita o cambia lo que carga esa zona. Bloqueado por diseño | SEG-02 |
| F4-07 | F4-05 | wearApp: pulso con Health Services solo durante la sesión y envío por `MessageClient`; permisos del reloj según su versión de Wear OS | SAL-03, RNF-09 |
| F4-08 | F4-06 | Descanso guiado por pulso en el teléfono y en el reloj (11.7), con "Listo" en la Live Activity y en la Live Update | TMR-04, VIV-05 (listo) |
| F4-09 | nuevo | Zonas de pulso personales (con el pulso en reposo y el máximo estimado) y esfuerzo por serie (pico y segundos de recuperación). Bloqueado por diseño: la escala de zonas (12.12) | PRG-07 |
| F4-10 | nuevo | Exploración de 2 días: ¿compila `shared:domain` para los targets de watchOS que pide Xcode para el Series 12 y se puede usar desde SwiftUI? Si no, la app del reloj es Swift puro y recibe los datos por WatchConnectivity. Sin código de producción | Riesgo |
| F4-11 | nuevo | watchApp: rutina del día sin el iPhone cerca, serie con la Corona y el Doble toque, y descanso en el borde (`docs/PANTALLAS.md` 5.5, 5.6 y 5.7); sesión con `HKWorkoutSession`; las series vuelven al iPhone al reconectar | SAL-07 |
| F4-12 | nuevo | watchApp: intervalos, EMOM y Tabata en la muñeca con vibraciones distintas para trabajo y descanso; pulso en vivo reflejado en el iPhone; la Live Activity en el Smart Stack (`supplementalActivityFamilies([.small])`) con Doble toque; widget "Hoy: Empuje A". Bloqueado por diseño: intervalos en el reloj | SAL-07, VIV-03 |
| F4-13 | nuevo | wearApp completo: rutina del día, serie, cronómetro con el anillo siguiendo el borde (`docs/PANTALLAS.md` 4.1 y 4.2), bisel para el peso o el descanso, intervalos, actividad en curso en la esfera y un Tile con el entreno de hoy. Bloqueado por diseño: rutina e intervalos en el Galaxy Watch | SAL-11 |
| F4-14 | nuevo | Ajuste durante la sesión (11.8) en el teléfono, la isla y los dos relojes (`docs/PANTALLAS.md` 6.1 y 6.2) | ADA-02, ADA-03 |

### 13.8 Tickets de F5 y F6

| Ticket | Qué | Requisitos |
| --- | --- | --- |
| F5-01 | Programas por semanas con descargas | RUT-04 |
| F5-02 | Biblioteca: PPL, torso/pierna, full body, 5x5 | RUT-05 |
| F5-03 | Perfiles de gimnasio con equipo, barra y discos | RUT-06 |
| F5-04 | Sustitutos por patrón, músculos y equipo del perfil (11.10) | CAT-05 |
| F5-05 | Fusionar ejercicios conservando el historial | CAT-04 |
| F5-06 | Importar CSV de Hevy y Strong emparejando ejercicios, probado con CSV reales anonimizados | DAT-02 |
| F5-07 | Tarjeta del entreno como imagen para compartir | MOT-01 |
| F5-08 | Logros y rachas semanales | MOT-02 |
| F5-09 | Catálogo de unos 1.000 ejercicios y posturas: free-exercise-db (dominio público) traducido y revisado, más yoga, pilates, movilidad y respiración propios; los 77 de hoy conservan sus ids; exigencia, porcentajes, fuente y licencia de cada uno; imágenes propias con el estilo de Forja | DIS-05, DIS-13, RNF-16 |
| F5-10 | Ejercicios adaptados (silla de ruedas o sentado) y sesiones con los tipos de silla de ruedas de Salud y Health Connect | DIS-11 |
| F5-11 | Variantes de agarre, postura y equipo para crecer hacia más de 2.000 entradas | DIS-14 |
| F6-01 | Pulido y revisión de accesibilidad con Accessibility Scanner y VoiceOver | RNF-06 |
| F6-02 | Privacidad: política (Ley 1581 de 2012, RGPD y LOPDGDD, COPPA), formulario de apps de salud y declaración de Health Connect en Play, etiquetas de privacidad y uso de HealthKit en App Store, y revisión del modo joven | RNF-05, RNF-19 |
| F6-03 | App Bundle firmado con Play App Signing y R8; archivo de Xcode a TestFlight | |
| F6-04 | Prueba cerrada en Play: 12 testers durante 14 días seguidos (cuentas personales creadas desde el 13 de noviembre de 2023) | |
| F6-05 | Fichas de las tiendas (Isla Dinámica, relojes, disciplinas y adaptación) y nombre definitivo, justo antes de publicar; el identificador `com.olarte.ironprogress` no cambia aunque cambie el nombre | |
| F6-06 | Publicar las apps de reloj: Wear OS en Play como formato de reloj de la misma app y la del Apple Watch dentro del envío de iOS | SAL-07, SAL-11 |

Requisitos de tiendas a tener presentes: Google Play cuesta USD 25 una vez; target API 36 en teléfono y 35 en Wear OS desde el 31 de agosto de 2026; borrado de cuenta en la app y en una web. App Store cuesta USD 99 al año; compilar con el SDK de iOS 26 desde el 28 de abril de 2026; con inicio de sesión de Google hay que ofrecer uno equivalente como Iniciar sesión con Apple (guía 4.8); borrado de cuenta dentro de la app (guía 5.1.1(v)). Si App Review pide publicar como empresa por los datos de salud (guía 5.1.1(ix)), ver `docs/PLAN_V3.md`, sección 10.

### 13.9 Etapas 2 a 4 (sin tickets todavía)

El jefe no escribe tickets de una etapa hasta que la anterior cumpla su "lista cuando". Requisitos completos en `docs/PLAN_V3.md`, sección 13.

| Etapa | Qué trae | Requisitos | Lista cuando |
| --- | --- | --- | --- |
| 2 · Pulido de movimiento (v1.1, 4 a 6 semanas) | Transiciones que siguen al dedo, resortes que conservan la velocidad del gesto, cifras que ruedan, vibración sincronizada, todo a 120 Hz y con versión para Reducir movimiento, en los 10 flujos clave; catálogo de movimiento en Forja | RF-PUL-01, RF-PUL-02, RNF-18 | 5 ms de tirones o menos por segundo de animación en iPhone |
| 3 · Salud avanzada y cámara (v1.5, 15 a 23 semanas) | Temperatura, respiración, oxígeno, VO2 máx y recuperación del pulso; Samsung Health Sensor SDK y Data SDK; pulso con AirPods o banda; cámara (reps, técnica, velocidad de la barra en beta, fotos guiadas, pulso con el dedo); planes de cardio al reloj; nutrición leída de Salud; ánimo; embarazo y posparto; plan Pro | RF-SAL-05, 06, 08, 09; RF-PRG-06, 08; RF-ANA-08, 09; RF-CAM-01 a 06; RF-DIS-10; RF-SEG-03; RF-NUT-01; RF-ANI-01; RF-FIS-03; RNF-12, RNF-13 | 95 % de reps contadas en 3 ejercicios básicos |
| 4 · Plataforma de profesionales (v2, 28 a 41 semanas) | Entrenadores, creadores y fisioterapeutas; panel web; check-ins, video y mensajes; tienda donde cada profesional pone su precio y IronProgress cobra 5 %; venta en la app y en la web | RF-VIV-06, 07; RF-PRO; RF-COA; RF-PAG; RF-MKT; RF-FIS-01, 02, 04, 05; RNF-11, RNF-14 | 3 entrenadores reales y 10 ventas pagadas |

Antes de la tienda de la Etapa 4: empresa en EE. UU., términos y cómo pagar a los profesionales. La Etapa 4 no depende de la 3 y puede ir antes si hace falta plata.

### 13.10 Decisiones

Del plan v3 (detalle en `docs/PLAN_V3.md`, sección 15): 12 decididas y 8 abiertas; ninguna abierta frena F1.

| # | Decisión | Estado | Respuesta o recomendación |
| --- | --- | --- | --- |
| 1 | Orden de las etapas | Decidida | 1 app personal con salud y relojes, 2 pulido, 3 salud avanzada y cámara, 4 plataforma |
| 2 | Cronómetro | Decidida | Pantalla completa en la app; fuera de ella, Isla Dinámica, pantalla bloqueada, relojes y barra de estado |
| 3 | Relojes | Decidida | Apple Watch Series 12 y Galaxy Watch con la rutina, un cronómetro animado y el pulso, en la Etapa 1 |
| 4 | Salud desde el inicio | Decidida | Leer y escribir Salud y Health Connect en la Etapa 1, para adaptar antes y durante la sesión |
| 5 | Disciplinas e intervalos | Decidida | Todas, con intervalos a tu medida y voz, y unos 1.000 ejercicios con su exigencia |
| 6 | Pulido de movimiento | Decidida | Etapa 2 propia, publicada como actualización |
| 7 | Modelo de cobro | Decidida | Cada profesional pone su precio; la tienda cobra lo suyo e IronProgress 5 % |
| 8 | Empresa | Decidida | En EE. UU., más adelante; mientras tanto, como persona |
| 9 | Fisioterapeutas | Decidida | Como un entrenador: responden por su servicio; entran cuando exista la empresa |
| 10 | Menores | Decidida | Lo más jóvenes posible, con funciones limitadas |
| 11 | Nombre | Decidida | Al final, justo antes de publicar la Etapa 1; `com.olarte.ironprogress` no cambia |
| 12 | Edad exacta | Decidida | 13 años, con modo joven hasta los 17 |
| 13 | Tipo de empresa y estado | Abierta | LLC o C corp en Delaware, con contador en EE. UU. y otro donde viva Juan (antes de cobrar) |
| 14 | Residencia fiscal al cobrar | Abierta | Colombia o España (antes de cobrar) |
| 15 | Revisión de la comisión | Abierta | Empezar con 5 % y revisarla al año |
| 16 | Beta abierta | Abierta | Sí, con gente del gimnasio en TestFlight y en la prueba cerrada de Play, desde la mitad de la Etapa 1 |
| 17 | Panel web profesional | Abierta | Exploración de una semana al empezar la Etapa 4 |
| 18 | Velocidad de la barra | Abierta | Beta solo en iPhone con LiDAR, sin tocar el motor hasta validarla (Etapa 3) |
| 19 | Ciclo menstrual como contexto | Abierta | Opcional, solo para ver tendencias (Etapa 3) |
| 20 | Adelantar la plataforma | Abierta | Se puede: la Etapa 4 no depende de la 3 |

Decisiones técnicas para el jefe (se confirman con Juan en el ticket que las necesita):

- [x] Health Services o Samsung Health Sensor SDK en el reloj: Health Services en la Etapa 1 y el Sensor SDK en la Etapa 3.
- [x] Nombre definitivo: al final (decisión 11). Ya descartados: Súbele, Levanto, Palante, UnaMás, Vanzia, Supren, Vanza y Levanza, entre otros.
- [ ] Versión mínima de iOS: la especificación propone 17 y el proyecto dice 18.2; Declared Age Range pide iOS 26 (antes de iOS 26 se pregunta el año de nacimiento).
- [ ] Versión mínima de watchOS de la app del reloj (F4-10).
- [ ] Rango de series semanales por músculo según la experiencia de Juan.
- [ ] Valores propuestos de 10.2 y nombres de 10.5 (antes de F1-01).
- [ ] Series efectivas con porcentaje y exigencia de la sesión (11.10, antes de F2-07 y F4-05).
- [ ] Tema por defecto antes de F1-25 (sistema u oscuro).
- [ ] Requisito del servicio tipo health en F1-14 (`HIGH_SAMPLING_RATE_SENSORS` o `ACTIVITY_RECOGNITION`).
- [ ] Criterio de salida de F0 con F0-12 (archivar ejercicio) u otra pantalla que Juan prefiera.
- [ ] Voz grabada: quién la graba y en qué idiomas; mientras tanto, la del sistema.
- [ ] PAR-Q+: confirmar con sus autores las condiciones para una app comercial (antes de F1-26).
- [ ] Límite de intensidad de los intervalos en modo joven (11.11, antes de F1-26).

### 13.11 Riesgos

| Riesgo | Impacto | Plan |
| --- | --- | --- |
| Alcance grande para una sola persona: la Etapa 1 pasó de 26 a 35 semanas a 43 a 63 | Alto | Beta interna en cada fase; beta abierta desde la mitad; F5 puede salir como actualización sin tocar lo demás |
| Datos de salud sensibles (Ley 1581 de 2012, RGPD, políticas de Health Connect y de HealthKit) | Alto | Consentimiento por tipo de dato, política de privacidad, borrado total y cero anuncios |
| Menores de edad (COPPA, LOPDGDD, Ley 1581) | Alto | Modo joven con límites, permiso de los padres guardado, pruebas con cuentas de 13, 15 y 17 años y asesoría legal antes de publicar; este manual ordena los temas, no es asesoría legal |
| Kotlin/Native en watchOS sin probar en este proyecto | Medio | Exploración F4-10; plan B, Swift puro con WatchConnectivity |
| La Live Activity se queda sin actualizaciones o gasta batería | Medio | Contador del sistema y actualizar solo al cambiar de fase (RNF-15); medir en 60 min |
| La voz con la pantalla bloqueada en iPhone | Medio | Probar en F1-17 con el iPhone 18 Pro; si el sistema suspende la app, usar el modo de audio en segundo plano o la sesión de entreno de HealthKit en el iPhone |
| Live Update y Metric Style dependen de la versión de One UI | Medio | Notificación en curso clásica como respaldo; probar con la versión que tenga el S25 Ultra |
| Licencias del catálogo y de las imágenes | Medio | Solo dominio público o propio; cada ejercicio guarda fuente y licencia (RNF-16); nada de ExerciseDB ni de wger sin revisar la licencia de cada ejercicio |
| Samsung Health comparte con Health Connect actividad, ejercicio, pulso y sueño, pero no confirma VFC ni pulso en reposo | Medio | El ajuste funciona con sueño, pulso durante el sueño y check-in; se verifica con el Watch4 en F4-00 |
| El reloj sincroniza con Samsung Health a su ritmo, no en tiempo real | Medio | El pulso en vivo sale de la app propia de Wear OS |
| Límites y costos de Supabase y PowerSync | Medio | Revisarlos en F3-00; plan B con sincronización propia |
| Que las sugerencias se tomen como consejo médico | Medio | Aviso claro, valores conservadores, PAR-Q+ y la decisión siempre de la persona |
| Traducir el catálogo (873 de free-exercise-db más yoga y respiración) | Medio | Por partes en F5-09, empezando por los 200 más usados |
| El Apple Watch Series 12 todavía no llega | Bajo | F4-10 a F4-12 van al final de F4 |

---

## 14. Verificación: comandos, CI y dispositivos

### 14.1 Comandos (copiar tal cual en zsh)

```
git status
git log --oneline -5
./gradlew :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest
./gradlew :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:linkDebugFrameworkIosArm64
./gradlew :androidApp:installDebug
```

El reloj: `./gradlew :wearApp:assembleDebug` (ya está en el CI desde `d3c2101`) y `./gradlew :wearApp:installDebug` (14.5).

Contar pruebas después de correrlas:

```
grep -ho 'testsuite name="[^"]*" tests="[0-9]*"' shared/domain/build/test-results/testAndroidHostTest/*.xml shared/data/build/test-results/testAndroidHostTest/*.xml
```

Si Gradle se porta raro tras cambiar plugins: `./gradlew --stop` y volver a correr. Si KSP no regenera: agregar `--rerun-tasks` a la tarea de pruebas.

### 14.2 CI

`.github/workflows/ci.yml` corre en cada push a `main`, `feat/**` y `fix/**` y en cada PR a `main`:

- Job `android-and-tests` (ubuntu, JDK Zulu 21): `:shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest` y luego `:androidApp:assembleDebug :wearApp:assembleDebug`. En `d3c2101` este paso tiene que fallar por `ForjaShapes` (F0-00).
- Job `ios-framework` (macos-15): `:shared:linkDebugFrameworkIosSimulatorArm64`.
- F0-13 agrega ktlint, Kover y `:shared:linkDebugFrameworkIosArm64`. Desde F1-16 conviene compilar también el proyecto de Xcode (con la extensión de widgets) en el job de macOS; el jefe lo agrega en ese ticket.
- Ojo: las ramas `test/**`, `chore/**`, `ci/**` y `docs/**` solo corren el CI al abrir el PR. Si se quiere en cada push, agregarlas en `on.push.branches`.

### 14.3 Galaxy S25 Ultra

1. Ajustes, Acerca del teléfono, Información de software: tocar 7 veces "Número de compilación" para activar las opciones de desarrollador.
2. Opciones de desarrollador: activar Depuración USB.
3. Conectar por cable, aceptar la huella del Mac y correr la configuración `androidApp` desde Android Studio o `./gradlew :androidApp:installDebug`.
4. La Live Update y la Now Bar piden Android 16 (One UI 8). La versión está en Ajustes, Acerca del teléfono, Información de software.

### 14.4 iPhone 18 Pro

1. Conectar el iPhone por cable, confiar en el Mac y activar el Modo de desarrollador en Ajustes, Privacidad y seguridad (el iPhone se reinicia).
2. En Xcode, abrir `iosApp/iosApp.xcodeproj`, target iosApp, pestaña Signing & Capabilities, y elegir el Team con el Apple ID de Juan. El bundle id sale de `Config.xcconfig` (`com.olarte.ironprogress.IronProgress$(TEAM_ID)`); poner ahí el `TEAM_ID` evita choques.
3. Elegir el iPhone como destino y darle Run. Con un dispositivo real Gradle compila `iosArm64`; con el simulador, `iosSimulatorArm64`.
4. La primera vez, en el iPhone: Ajustes, General, VPN y gestión de dispositivos, y confiar en el certificado de desarrollador.
5. Con la cuenta gratuita de Apple la firma vence a los 7 días y hay que volver a darle Run. Con el Apple Developer Program (USD 99 al año) dura un año y habilita TestFlight.
6. Solo se prueba bien en el teléfono: la vibración de Forja, el vidrio a 120 Hz, las notificaciones del descanso con la pantalla apagada, la Live Activity con el teléfono bloqueado y en StandBy, la voz con música y, en F4, Apple Salud.

### 14.5 Galaxy Watch4

1. En el reloj: Ajustes, Acerca del reloj, Información de software, tocar varias veces "Versión de software" hasta que se activen las opciones de desarrollador.
2. Opciones de desarrollador: activar Depuración ADB y Depuración inalámbrica (o por Wi-Fi), con el reloj y el Mac en la misma red.
3. En la terminal del Mac: `adb pair IP:PUERTO` con el código que muestra el reloj y luego `adb connect IP:PUERTO`.
4. `./gradlew :wearApp:installDebug` o la configuración `wearApp` en Android Studio.
5. Health Services pide `minSdk` 30: el Watch4 con Wear OS 3 o posterior cumple.

### 14.6 Apple Watch Series 12 (cuando llegue)

1. Emparejar el reloj con el iPhone 18 Pro desde la app Watch, con la misma cuenta de Apple.
2. Conectar el iPhone al Mac por cable. El reloj aparece en Xcode, en Window, Devices and Simulators, a través del iPhone; la primera vez Xcode tarda en prepararlo.
3. En el reloj: Ajustes, Privacidad y seguridad, Modo de desarrollador (aparece después de conectarlo a Xcode), activarlo y dejar que se reinicie.
4. En Xcode, elegir el esquema de la app del reloj (lo crea F4-11) y el Apple Watch como destino, y darle Run.
5. Con la cuenta gratuita la firma vence a los 7 días, igual que en el iPhone (14.4).
6. Solo se prueba bien en el reloj: el Doble toque, la Corona, la pantalla siempre activa, el pulso de `HKWorkoutSession` y la Live Activity en el Smart Stack.

### 14.7 Pruebas del v3 que solo valen en los dispositivos

| Prueba | Cómo | Requisito |
| --- | --- | --- |
| Tiempo exacto | 20 descansos con la app cerrada y el teléfono bloqueado; cada aviso llega a su hora y la isla, la barra de estado y el reloj marcan lo mismo | RNF-17 |
| Batería | Sesión de 60 min con la Live Activity o la Live Update y el reloj; anotar el consumo antes y después | RNF-15, RNF-09 |
| Voz con música | Intervalos con música sonando y la pantalla apagada: la música baja mientras habla la voz y vuelve | RF-DIS-08 |
| Botones fuera de la app | Completar serie, -15 s, +15 s y Saltar desde la isla y desde la Now Bar, con el teléfono bloqueado; la serie queda en la base | RF-VIV-02, RF-VIV-04 |
| Relojes sin teléfono | Una sesión entera con el teléfono en otra habitación; al volver, las series aparecen en el teléfono | RF-SAL-07, RF-SAL-11 |
| Modo joven | Cuentas de 13, 15 y 17 años en Colombia, España y EE. UU.: sin fotos, mensajes, tienda ni peso visible por defecto, y el permiso guardado | RNF-19 |

El simulador de iOS muestra la Isla Dinámica y las Live Activities, pero el bloqueo real, StandBy, la pantalla siempre activa, el Doble toque y el reloj solo se prueban en los equipos de Juan.

### 14.8 Qué pegarle al jefe después de cada ticket

- La salida de `git log --oneline -3`.
- El resultado de la verificación del ticket (las últimas 30 líneas si pasó; desde la primera línea con `e:` o `FAILURE` si falló).
- Una frase de lo que se vio en cada teléfono, si el ticket toca la UI.

---

## 15. Plantillas de prompts

### 15.1 Arranque del Gemini jefe

```
Eres el Gemini jefe del proyecto IronProgress. Te adjunto tres archivos: HANDOFF.md (el manual: cómo se trabaja, estado real, reglas, arquitectura, motores, Forja, tickets y comandos), docs/PLAN_V3.md (el plan v3 completo) y docs/PANTALLAS.md (las 29 pantallas diseñadas). Lee HANDOFF.md entero antes de responder, y PLAN_V3.md antes de escribir cualquier ticket de F1 en adelante.

Tu trabajo:
1. Confirmar en 5 líneas el estado actual (sección 4) y cuál es el siguiente ticket (sección 13).
2. Escribir ese ticket completo con la plantilla 15.3, incluidos los comandos cat que la hormiga necesita y, si construye una pantalla, la sección de PANTALLAS.md.
3. Cuando te pegue el cierre de una hormiga, revisarlo contra el criterio de terminado y darme la fila nueva de la bitácora 4.8.
4. Si un ticket necesita una pantalla que no está en PANTALLAS.md, no la inventes: dame el pedido con la plantilla 15.7.

Reglas: secciones 2.3 y 3. Si algo choca, manda el repo, luego HANDOFF.md, luego PLAN_V3.md y luego PANTALLAS.md, y me dices qué corregir. No escribas código largo salvo que te lo pida.
```

### 15.2 Arranque de una hormiga

```
Eres una Gemini hormiga del proyecto IronProgress. Haces un solo ticket. Te adjunto HANDOFF.md (y docs/PANTALLAS.md si el ticket construye una pantalla). Antes de escribir código lee las secciones 2, 3, 7 y 8 del manual y las que cite el ticket.

Ticket:
<pegar aquí el ticket del jefe>

Archivos actuales:
<pegar aquí la salida de los cat>

Responde con el formato 15.4. Si te falta un archivo, pídeme el cat exacto en vez de suponer su contenido. No toques nada fuera del ticket y no inventes pantallas, funciones ni versiones.
```

### 15.3 Plantilla de ticket (la escribe el jefe)

```
Ticket <ID> · <título>
Rama: <feat/...>
Objetivo: <una frase>
Requisitos: <RF/RNF o bug de 4.6>
Etapa y fase: <Etapa 1 · F1>
Pantalla: <docs/PANTALLAS.md, sección X, o sin pantalla>
Motor: <sección 11.x, si aplica>
Archivos que se crean o cambian (rutas completas):
- ...
Comandos para que Juan pegue el estado actual:
cat ...
Pasos:
1. ...
Fuera de alcance: <lo que no se toca>
Terminado cuando:
- ...
Verificación:
./gradlew ...
```

### 15.4 Formato de entrega de la hormiga

```
Plan:
- (3 a 6 líneas)

Archivo 1: <ruta completa>
<contenido completo>

Archivo 2: <ruta completa>
<contenido completo>

Archivos que se borran:
git rm <ruta>

Verificación:
<comandos, uno por línea, sin #>

Git:
git checkout -b <rama>
git add <rutas>
git commit -m "<tipo(alcance): mensaje en español>"
git push -u origin <rama>
```

### 15.5 Cierre de ticket (la hormiga se lo da a Juan para el jefe)

```
Cierre <ID>
Rama y commit: <rama>, <hash corto que Juan ve en git log>
Archivos: <lista>
Verificación: <comando> · <resultado real pegado por Juan>
Pruebas: <número antes> a <número después>
Pendientes o riesgos: <si hay>
Fila para la bitácora 4.8: | <fecha> | <ID> <título> | <rama>, <commit> | <resultado verificado> |
```

### 15.6 Reporte de error (Juan a la hormiga)

```
Falló <comando>. Salida desde el primer error:
<pegar>
Corrige solo lo que este error señala y entrégame completos los archivos que cambian.
```

### 15.7 Pedir un diseño que falta (el jefe se lo da a Juan)

```
Falta el diseño de <pantalla> para el ticket <ID>.
Qué tiene que mostrar: <datos y acciones>
Estados: <vacío, normal, error y los propios de la pantalla>
Toques: <cuántos para la acción principal, según 12.5>
De dónde se abre y a dónde lleva: <rutas>
Pídeselo a Claude en el lienzo de pantallas y pega aquí su descripción con el mismo formato de docs/PANTALLAS.md.
```

---

## 16. Enlaces

Proyecto:

- Repositorio: https://github.com/Juandicho13/IronProgress_App
- En Claude, privados de Juan (Gemini no puede abrirlos; lo esencial está en estos tres archivos): propuesta v3 (https://claude.ai/artifact/35EJctnRJemNo8ZeCCQ3XQ), especificación v2 (https://claude.ai/artifact/YcS9GUQoBRGUMHoCs21H4B), sistema de diseño Forja (https://claude.ai/artifact/3kyPa8bD9nUTFoLzqr6tBM) y lienzo de pantallas (https://claude.ai/artifact/SWyD8Z831afkpdujtotr3t).

Base técnica (v2):

- Room 3, notas de versión: https://developer.android.com/jetpack/androidx/releases/room3
- Navigation 3 en Compose Multiplatform: https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html
- Novedades de Compose Multiplatform 1.10 (accessors deprecados, Navigation 3): https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html
- Migrar a Haze 2.0: https://chrisbanes.github.io/haze/latest/migrating-2.0/
- `Uuid.generateV7()` en la librería estándar de Kotlin: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/-companion/generate-v7.html
- Tipos de servicio en primer plano (Android 14): https://developer.android.com/about/versions/14/changes/fgs-types-required
- Compatibilidad de Kotlin Multiplatform con Xcode: https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- Correr la app en un dispositivo real (Apple): https://developer.apple.com/documentation/xcode/running-your-app-on-simulated-or-physical-devices
- Activar el Modo de desarrollador en el iPhone (Apple): https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device
- PowerSync para Kotlin Multiplatform: https://docs.powersync.com/client-sdk-references/kotlin-multiplatform
- Disponibilidad de Health Connect: https://developer.android.com/health-and-fitness/health-connect/availability
- Samsung Health y Health Connect, preguntas frecuentes: https://developer.samsung.com/health/health-connect-faq.html
- Pulso del Galaxy Watch al teléfono (Samsung): https://developer.samsung.com/health/blog/en/transfer-heart-rate-from-galaxy-watch-to-a-phone
- free-exercise-db: https://github.com/yuhonas/free-exercise-db
- Material Symbols: https://fonts.google.com/icons
- Archivo en Google Fonts: https://fonts.google.com/specimen/Archivo

Nuevos del v3 (más fuentes en `docs/PLAN_V3.md`, sección 16):

- Crear una notificación Live Update (Android): https://developer.android.com/develop/ui/views/notifications/live-update
- Mostrar datos en vivo con Live Activities (ActivityKit): https://developer.apple.com/documentation/activitykit/displaying-live-data-with-live-activities
- `LiveActivityIntent` (App Intents, iOS 17 o posterior): https://developer.apple.com/documentation/appintents/liveactivityintent
- Live Activities essentials, WWDC26: https://developer.apple.com/videos/play/wwdc2026/223/
- Doble toque en el Apple Watch: https://developer.apple.com/documentation/watchos-apps/enabling-double-tap
- Entrenos con HealthKit en iPhone, WWDC25: https://developer.apple.com/videos/play/wwdc2025/322/
- Tipos de dato de HealthKit: https://developer.apple.com/documentation/healthkit/hkquantitytypeidentifier
- Compose para Wear OS: https://developer.android.com/training/wearables/compose
- Wear Compose Material 3, versiones: https://developer.android.com/jetpack/androidx/releases/wear-compose-m3
- Tipos de ejercicio de Health Connect: https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/ExerciseSessionRecord
- Política de Health Connect: https://support.google.com/googleplay/android-developer/answer/12991134?hl=en
- Samsung Health Sensor SDK (Etapa 3): https://developer.samsung.com/health/sensor/overview.html
- Declared Age Range: https://ppc.land/declared-age-range/
- Guías de revisión de App Store: https://developer.apple.com/app-store/review/guidelines/
- PAR-Q+ (Warburton y otros, 2011): https://hfjc.library.ubc.ca/index.php/HFJC/article/view/103
- Compendio de actividades físicas 2024 (MET): https://doaj.org/article/91ffa80dcdef401c9db9fc7db2a08e42
- Marca CrossFit y afiliados (por qué se dice "Funcional"): https://www.crossfit.com/faq/affiliates
