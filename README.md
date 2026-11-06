# IronProgress

App de entrenamiento para Android e iOS, con apps para Galaxy Watch (Wear OS) y, más adelante, Apple Watch. Te dice qué hacer hoy y por qué, registra una serie en un toque, se adapta a cómo llegas y funciona sin conexión. Nombre provisional: el definitivo se decide antes de publicar.

Hecha con Kotlin Multiplatform y Compose Multiplatform, con el sistema de diseño Forja.

## Documentos

| Archivo | Qué tiene |
| --- | --- |
| `HANDOFF.md` | Manual del proyecto: reglas, estado, arquitectura, motores, Forja, tickets y comandos |
| `docs/PLAN_V3.md` | El plan v3 completo, por etapas |
| `docs/PANTALLAS.md` | Las pantallas diseñadas, descritas para construirlas |
| `docs/RELEVO_CHATGPT.md` | El estado más reciente y el siguiente paso |

## Módulos

| Módulo | Qué es |
| --- | --- |
| `androidApp` | App del teléfono Android |
| `iosApp` | Contenedor de Xcode para el iPhone |
| `wearApp` | App del Galaxy Watch (Wear Compose) |
| `shared` | Paraguas: inyección con Koin y framework iOS `Shared` |
| `shared:domain` | Kotlin puro: modelos, casos de uso y motores |
| `shared:data` | Room 3, repositorios y catálogo semilla |
| `shared:health` | Health Connect y HealthKit (por venir) |
| `shared:ui` | Pantallas Compose, Forja, navegación y textos en español e inglés |

## Requisitos

- JDK 21, Android Studio con el plugin Kotlin Multiplatform y Xcode para iOS.
- `local.properties` con `sdk.dir` apuntando al SDK de Android.

## Correr

```
./gradlew :androidApp:installDebug
./gradlew :wearApp:installDebug
```

iPhone: abrir `iosApp/iosApp.xcodeproj` en Xcode, elegir el Team en Signing & Capabilities y darle Run. Pasos completos de los tres equipos en la sección 14 de `HANDOFF.md`.

## Probar

```
./gradlew ktlintCheck
./gradlew :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest :shared:ui:testAndroidHostTest :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug :wearApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

El CI de GitHub corre lo mismo en cada push a `main`, `feat/**` y `fix/**`, y en cada PR.

## Licencias de terceros

- Fuente Archivo: SIL Open Font License 1.1 (`licenses/Archivo-OFL.txt`).
- Íconos Material Symbols: Apache 2.0 (`licenses/MaterialSymbols-LICENSE.txt`).
