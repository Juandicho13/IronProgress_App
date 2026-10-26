# IronProgress v3 · El plan completo

25 de octubre de 2026 · Juan David Olarte Díaz

> Copia para los Gemini de la propuesta v3, que vive en Claude y ellos no pueden abrir. Aquí está el qué y el porqué del plan. El cómo (estado real del código, reglas, tickets y comandos) está en `HANDOFF.md`, y las pantallas diseñadas en `docs/PANTALLAS.md`. Si algo de este archivo choca con `HANDOFF.md` o con el repositorio, mandan el repositorio y luego `HANDOFF.md`. El texto le habla a Juan de tú: donde dice "tú" o "tu último mensaje", es Juan.

IronProgress sale en cuatro etapas: la app personal con salud y relojes, el pulido de movimiento, la salud avanzada con cámara y la plataforma de profesionales. La plata entra en la Etapa 4, con una comisión de 5 % por venta sobre el precio que pone cada profesional. Desde la Etapa 1 la app cubre todas las disciplinas y adapta el entreno a cómo llegas y a cómo vas durante la sesión (más descanso, menos peso, menos series o sin cardio); lo que faltaba está en la sección 7 y la F0 en curso no cambia.

## 1. La idea completa

IronProgress se vuelve tres capas que se apoyan: cada una sirve sola y la siguiente la hace más valiosa.

| Capa | Para quién | Qué hace | Etapa |
| --- | --- | --- | --- |
| 1 · App personal | Quien entrena, en cualquier disciplina | Te dice qué hacer hoy y por qué, registra una serie, una ronda o una postura en un toque, se adapta a cómo llegas y progresa sola | 1 · la v2 completa, todas las disciplinas y los dos relojes; F0 en curso |
| 2 · Salud conectada | Quien tiene Apple Watch, Galaxy Watch, iPhone o Samsung | Usa relojes, cámara y sensores para ajustar el entreno antes, durante y después, y lo lleva a la Isla Dinámica, la Now Bar y la muñeca | 1 la base (sueño, pulso, VFC y adaptación); 3 lo avanzado y la cámara |
| 3 · Plataforma de profesionales | Entrenadores, creadores y fisioterapeutas, y sus clientes | Publican y venden planes con el precio que cada uno elige, siguen a cada cliente con los datos que el cliente decide compartir y cobran; IronProgress gana 5 % por venta | 4 · nuevo |

Por qué en ese orden: la capa 1 atrae a quien entrena solo; la 2 vuelve los datos más finos para todos; la 3 trae ingresos y usuarios, porque cada entrenador llega con sus clientes. El motor de progresión es el mismo en las tres, y eso diferencia la tienda: un programa comprado no es un PDF, se ajusta solo a los números de cada persona. La Etapa 2 no suma funciones: pule el movimiento de todo lo anterior.

**Todo pasa por IronProgress y el atleta decide qué ve su profesional** (aparatos, app, nube y profesionales, de arriba abajo):

| Capa | Piezas |
| --- | --- |
| Atleta y sus aparatos | iPhone 18 Pro: Isla Dinámica, cámara y LiDAR · Galaxy S25 Ultra: Now Bar, cámara y Health Connect · Apple Watch: pulso, sueño, VFC y Doble toque · Galaxy Watch: pulso con intervalos entre latidos (IBI), composición corporal (BIA) y Energy Score |
| ↓ pulso, sueño, cámara y series | |
| App IronProgress en teléfonos y relojes | Motor de progresión: la sugerencia con su porqué en cualquier plan · Sesión en vivo: Isla Dinámica, Now Bar y reloj a la vez · Salud conectada: HealthKit, Health Connect y Samsung Health |
| ↕ funciona sin conexión y sincroniza | |
| Nube IronProgress | Cuenta y sincronización: Supabase y PowerSync con los datos del atleta · Permisos por tipo de dato y registro de accesos · Pagos: tiendas y Stripe, con la comisión de la plataforma |
| ↓ solo lo que el atleta permite · ↑ planes, mensajes y ajustes | |
| Profesionales (modo profesional y panel web) | Entrenador: clientes 1:1, video, check-ins y chat · Creador: programas y membresías · Fisioterapeuta: rehabilitación con registro vigente |

Los aparatos alimentan la app; la nube guarda, aplica los permisos y cobra; el profesional trabaja sobre lo que el atleta comparte y le devuelve planes, mensajes y ajustes por el mismo camino.

## 2. Salud antes, durante y después

Cada reloj entrega datos distintos, así que IronProgress arma su propia lectura con lo que haya y siempre explica el porqué; nunca depende de un solo aparato. El puntaje de preparación del Apple Watch Series 12 no llega a las apps, así que la preparación se calcula aquí con los datos de base.

### Qué entrega cada fuente

| Fuente | Antes de entrenar | Durante | Después |
| --- | --- | --- | --- |
| Apple Watch (HealthKit) | Sueño por fases, VFC (SDNN; RMSSD es tipo nuevo en iOS 27), pulso en reposo, temperatura de muñeca durante el sueño, frecuencia respiratoria, oxígeno, VO2 máx | Pulso en vivo con una sesión de entreno en el reloj, reflejada en el iPhone | Recuperación del pulso al minuto, esfuerzo del entreno (workoutEffortScore), el entreno guardado en Salud |
| Galaxy Watch (Health Connect y Samsung Health Data SDK) | Sueño, VFC, pulso en reposo, temperatura de piel, oxígeno, composición corporal y el Energy Score de Samsung (solo lectura; disponible desde el Watch4) | Pulso con intervalos entre latidos (VFC durante el descanso) desde nuestra app de Wear OS con el Samsung Health Sensor SDK | La sesión guardada en Health Connect |
| iPhone sin reloj | Check-in de 10 s y VFC con el dedo sobre la cámara | Pulso desde AirPods Pro 3 o una banda Bluetooth, con la sesión de entreno de HealthKit en el iPhone | Igual, sin datos de sueño |
| Samsung sin reloj | Check-in y VFC con la cámara | Pulso con una banda Bluetooth conectada a la app | La sesión guardada en Health Connect |

### Qué hace la app con eso

| Momento | Personalización | Ejemplo de explicación |
| --- | --- | --- |
| Antes | Preparación del día ampliada: a sueño, pulso en reposo, VFC y check-in se suman la temperatura nocturna (desvío frente a tu línea base), la frecuencia respiratoria y el oxígeno nocturno | "Tu temperatura de anoche sube 0,6 °C sobre tu media: hoy, versión ligera" |
| Antes | Calentamiento según el mapa de recuperación: más series de aproximación en los músculos en ámbar | "Pecho aún en ámbar: 3 series de aproximación antes del press" |
| Antes | Contexto de fase: con peso y composición corporal la app sabe si estás en volumen, definición o mantenimiento y cambia qué cuenta como progreso | "Bajaste 1,5 kg en 4 semanas: mantener tus marcas cuenta como progreso" |
| Durante | Pulso en vivo con zonas personales (calculadas con tu pulso en reposo y tu máximo estimado) y esfuerzo por serie | "Serie 3: pico de 168 lpm, recuperado en 74 s" |
| Durante | Descanso guiado por pulso en los dos relojes (ya estaba en v2) y aviso si el pulso no baja en el descanso máximo | "Tu pulso sigue alto: descansa 30 s más o baja 2,5 kg" |
| Durante | Completar la serie desde la muñeca: Doble toque en Apple Watch Series 9, Ultra 2 o posteriores; un toque en Galaxy Watch |  |
| Después | Recuperación: pulso al minuto de terminar, carga de la semana frente a las 4 anteriores y cómo dormiste esa noche | "Tu carga subió 30 % frente a tu media: mañana, piernas en versión ligera" |
| Después | VO2 máx como tendencia para quien combina cardio; no cambia el entreno del día |  |
| Después | El esfuerzo de la sesión se escribe en Salud y en Health Connect para que el resto del ecosistema lo vea |  |

### Adaptación inteligente, antes y durante el entreno

La app usa la salud para ajustar el entreno antes de empezar y, cuando los datos lo piden, también durante la sesión. Siempre dice por qué, y la persona acepta o sigue con el plan original en un toque.

Antes de empezar sigue un orden fijo: baja el peso, quita series o repeticiones y recorta o suaviza el cardio. Si hace falta, cambia un ejercicio por otro de los mismos músculos pero menos exigente (sección 6).

Durante la sesión mira el pulso en vivo, cuánto tarda en bajar entre series, las reps y el RPE, y cambia en este orden:

1. Alarga el descanso hasta 60 s, solo y con opción de deshacer.
2. Baja el peso de la serie siguiente.
3. Quita una serie o el cardio del final.
4. Cambia el ejercicio por otro de los mismos músculos pero menos exigente.

Del paso 2 en adelante llega una tarjeta que se acepta en un toque desde el teléfono, la isla o el reloj. Nunca interrumpe una serie y propone como mucho un cambio cada dos series. Si el pulso sigue cerca de tu máximo incluso descansando, propone parar.

| Señal | Cuándo | Qué cambia | Ejemplo de lo que dice la app |
| --- | --- | --- | --- |
| Preparación baja: sueño, pulso en reposo y VFC frente a tu línea base, más el check-in | Antes | 5 a 10 % menos de peso y una serie menos por ejercicio | "Dormiste 5 h y tu VFC está baja: hoy 5 % menos de peso" |
| Preparación muy baja | Antes | Quita el cardio intenso o lo cambia por cardio suave, y cambia los ejercicios más exigentes | "Hoy sin HIIT: 20 min suaves en zona 2" |
| Mucha carga reciente en otras apps | Antes | Baja el volumen de piernas o quita el cardio | "Ayer corriste 15 km: hoy piernas en versión ligera" |
| Preparación alta y semanas cumpliendo | Antes | Deja subir más rápido | "Todo en verde: puedes probar 2,5 kg más" |
| El pulso no baja durante el descanso | Durante | Alarga el descanso; si se repite, baja el peso de la serie siguiente | "Tu pulso sigue alto: 30 s más o 2,5 kg menos" |
| Reps por debajo del objetivo o RPE más alto de lo previsto | Durante | Ajusta las series que quedan | "La serie 2 salió a RPE 9,5: bajamos 2,5 kg" |
| Cansancio que se acumula: reps que caen dos series seguidas o pulso cada vez más alto | Durante | Quita la última serie o el cardio del final | "Vas más cansado que de costumbre: quitamos la serie 4 y el cardio" |
| Pulso cerca de tu máximo que no baja ni en los descansos | Durante | Alarga los descansos o baja el ritmo; si sigue, propone parar | "Tu pulso no baja ni descansando: 20 s más o para un momento" |
| Molestia marcada en una zona | Antes o durante | Cambia lo que carga esa zona por otro ejercicio de los mismos músculos | "Molestia en el hombro: press con mancuernas en vez de press militar" |

Los umbrales de los ejemplos son valores iniciales que se calibran con tus datos, como en el motor de la v2. Nada de esto es consejo médico y la app lo dice.

## 3. iPhone al máximo

El iPhone 18 Pro trae una Isla Dinámica más pequeña que muestra hasta tres Live Activities a la vez, el botón de cámara táctil y el LiDAR; con los tres se puede entrenar casi sin abrir la app.

### El cronómetro: grande dentro de la app, en la isla fuera de ella

Dentro de la app, el descanso ocupa la pantalla completa; si sales de la app o se apaga la pantalla, sigue en la Isla Dinámica, la pantalla bloqueada y el reloj, y en Android en la barra de estado.

| Dónde estás | Qué ves | Con qué |
| --- | --- | --- |
| En el entreno | Descanso a pantalla completa: anillo grande, tiempo en cifra héroe, pulso y la siguiente serie; -15 s, +15 s y Saltar abajo, donde estaba la barra | La barra del entreno crece hasta ser la pantalla y vuelve a ser barra cuando estás listo (sección 11) |
| En otra app | Anillo y tiempo en la isla; al tocarla vuelves a la pantalla completa | Live Activity |
| Con la pantalla bloqueada | La tarjeta del descanso; con la pantalla siempre activa del 18 Pro se ve atenuada sin tocar nada | La misma Live Activity, y una notificación local a la hora de fin como respaldo (ya prevista en F1-15) |
| En Android | El tiempo en la barra de estado, la pantalla bloqueada y la Now Bar | Live Update (sección 4) |
| En la muñeca | El descanso en el Smart Stack del Series 12, vibración al terminar (la notificación del iPhone llega al reloj) y Doble toque para completar la serie | La Live Activity llega sola al reloj desde la Etapa 1; la app propia del reloj, también en la Etapa 1, suma la rutina, el pulso y su propio cronómetro |

Un solo motor de tiempo, en Kotlin compartido, calcula todo desde la hora de inicio y no contando segundos: por eso la pantalla, la isla, la barra y el reloj nunca se desfasan, y el mismo motor sirve para EMOM, intervalos, posturas y respiración (sección 6). Con la sesión de entreno de HealthKit en el iPhone, Apple da por hecho que la pantalla se bloquea durante el entreno y recomienda justo esto: una Live Activity con lo esencial en la pantalla bloqueada.

### Isla Dinámica y Live Activities

| Idea | Cómo se ve | Cómo funciona |
| --- | --- | --- |
| Descanso en la isla | Compacta: anillo hielo y "1:12" a la izquierda, pulso a la derecha. Mínima: solo el anillo | Live Activity que arranca al completar la serie; el contador corre solo en pantalla, sin gastar actualizaciones |
| Completar la serie sin abrir la app | Expandida: "Press banca · Serie 3 de 4 · 62,5 kg × 10" y el botón Completar serie | Botón con App Intent (`LiveActivityIntent`): guarda en la base local y arranca el descanso |
| Ajustar el descanso | -15 s, +15 s y Saltar en la vista expandida | Los mismos App Intents |
| Listo por pulso | El anillo pasa de hielo a brasa y dice "Listo" | Dato de la sesión del Apple Watch reflejada en el iPhone |
| Récord | Alerta breve en oro: "Nuevo récord · 100 kg × 5" | Alerta de la Live Activity |
| Pantalla bloqueada y StandBy | Tarjeta con ejercicio, serie, descanso y pulso; en StandBy se ve al 200 % con el teléfono cargando sobre el banco | La misma Live Activity |
| En la muñeca | Vista pequeña en el Smart Stack del Apple Watch; Doble toque completa la serie | `supplementalActivityFamilies([.small])`; el Doble toque funciona en Live Activities que llegan del iPhone |
| En horizontal | Grabando la técnica con el teléfono acostado, el descanso sigue visible | iOS 27 muestra las vistas compacta y mínima en horizontal (`isDynamicIslandLimitedInWidth`) |
| Tu entrenador en vivo | "Tu entrenador está viendo · Baja 2,5 kg" | Actualización por push desde el servidor (capa 3) |
| Entreno agendado | "Empuje A · 6:00 p. m. · 45 min" antes de empezar | Inicio programado o por push, útil para entrenos que asigna un entrenador |

Como la isla del 18 Pro comparte espacio entre tres actividades, la vista compacta tiene que leerse con un solo número y un anillo.

### Botón de cámara (Camera Control)

- Abre la cámara de IronProgress desde el botón, incluso con el teléfono bloqueado, con una extensión `LockedCameraCapture`.
- Deslizar sobre el botón cambia de modo con un `AVCaptureIndexPicker`: Técnica, Foto de progreso y Barra. El zoom usa el control del sistema (`AVCaptureSystemZoomSlider`).
- Presionar graba la serie en curso y el video queda adjunto a esa serie.
- La apertura variable del 18 Pro tiene API para desarrolladores: cerrarla deja enfocados el cuerpo entero y la barra en los videos de técnica.

### LiDAR

- Mide la distancia al cuerpo para que cada foto de progreso se tome a la misma distancia y altura, y así se puedan comparar.
- Vision detecta 17 articulaciones en 3D (en metros) y acepta profundidad del LiDAR como entrada: da escala real para medir recorrido, ángulos y velocidad de la barra.
- ARKit sigue el cuerpo con la cámara trasera (`ARBodyTrackingConfiguration`) para pruebas de movilidad guiadas.

### App de Apple Watch (Etapa 1)

- Funciona sin el teléfono cerca: la rutina del día viaja al reloj y las series vuelven al teléfono al reconectar.
- Lleva la rutina: ejercicio, serie, peso y reps objetivo y lo que sigue. Doble toque (Series 9, Ultra 2 y posteriores; el Series 12 de Juan lo tiene) completa la serie con `handGestureShortcut(.primaryAction)` y la Corona Digital ajusta peso o reps.
- Un cronómetro lindo: anillo que se llena en hielo y pasa a brasa, cifras que ruedan al cambiar, vibración en los últimos 3 s y una versión atenuada para la pantalla siempre activa.
- Intervalos, EMOM y Tabata en la muñeca, con vibraciones distintas para trabajo y descanso.
- Sesión de entreno con pulso en vivo, reflejada en el iPhone.
- Widgets y complicaciones en el Smart Stack ("Hoy: Empuje A") para empezar con un toque.
- Botón de Acción del Ultra para empezar el entreno sugerido: por verificar.

### Extras de iOS

- Siri y Atajos con App Intents: "Completa la serie", "Empieza mi entreno".
- Modelo de lenguaje en el teléfono (Foundation Models, iOS 26, funciona sin internet y en español) para resumir la semana o redactar el mensaje al entrenador. Nunca decide cargas: eso sigue siendo del motor de reglas.

## 4. Samsung y Android al máximo

En Samsung, el equivalente de la Isla Dinámica es la Now Bar con las Live Updates de Android, y el Galaxy Watch entrega más datos crudos que cualquier reloj gracias a los SDK de Samsung; el sensor láser del S25 Ultra, en cambio, no está disponible para las apps.

| Pieza | Qué permite | Requisito |
| --- | --- | --- |
| Live Updates y Now Bar | El descanso, la serie y el pulso en la barra de estado, la pantalla bloqueada y la Now Bar de One UI 8 | Android 16 (One UI 8 ya las muestra en la Now Bar) |
| Plantilla Metric Style | Hasta tres datos a la vez (descanso, pulso y serie) y hasta tres botones en la vista expandida (Completar, +15 s, Saltar); pensada para salud y deporte | Android 17; Samsung la mostrará en la Now Bar de One UI 9. Hasta que llegue al S25 Ultra, notificación en vivo clásica |
| App de Galaxy Watch (Wear OS) | Desde la Etapa 1, la rutina del día, el cronómetro y el pulso de Health Services; en la Etapa 3, con el Samsung Health Sensor SDK: pulso con intervalos entre latidos, temperatura de piel, oxígeno, composición corporal (BIA) medida en el reloj, ECG, EDA y acelerómetro, según el modelo | Galaxy Watch4 o posterior. Probar con el modo de desarrollador; para publicar hay que pedir el Samsung Partner Program y registrar el paquete y la firma de la app |
| Samsung Health Data SDK (teléfono) | Leer Energy Score, composición corporal, temperatura de piel, sueño, pulso y oxígeno de Samsung Health | Leer en pruebas con el modo de desarrollador; escribir o publicar exige asociación con Samsung |
| Health Connect | El estándar de Android: VO2 máx, VFC (RMSSD), temperatura de piel, sueño, pulso en reposo, peso y composición | Ya previsto en la v2 (F4) |
| Cámara | Pose del cuerpo con ML Kit (33 puntos, profundidad aproximada, en beta) o MediaPipe; mismas funciones de la sección 5 | CameraX |
| Profundidad | La Depth API de ARCore calcula profundidad con el movimiento de la cámara y usa un sensor ToF solo si el teléfono lo tiene | Teléfonos compatibles con ARCore |

Sobre el sensor láser: en el S25 Ultra sirve para el enfoque automático y Android no lo entrega a las apps como mapa de profundidad (no hay una API pública documentada). Por eso, en Samsung, la escala para medir se toma de la profundidad de ARCore o de una referencia conocida en la imagen, como el diámetro de un disco olímpico.

En el Galaxy Watch, desde la Etapa 1: la rutina del día, el mismo cronómetro con el anillo siguiendo el borde redondo, el bisel para ajustar peso o descanso, la actividad en curso en la esfera y un Tile con el entreno de hoy.

## 5. Lo que analiza la cámara

La cámara cuenta repeticiones y mide recorrido y ritmo con buena precisión; la velocidad de la barra sirve como tendencia, pero todavía no alcanza para decidir cargas por sí sola.

| Análisis | Qué mide | Precisión esperada | Plataforma |
| --- | --- | --- | --- |
| Repeticiones y ritmo | Reps, duración de la bajada y la subida, pausas | Alta: una app de cámara detectó más del 95 % de las repeticiones frente a captura de movimiento (PeerJ, 2024) | Ambas |
| Técnica por ángulos | Profundidad de la sentadilla, ángulos de rodilla y cadera, inclinación del tronco, simetría izquierda y derecha | Media; depende del ángulo de la cámara y la luz, así que se muestra como guía, no como juicio | Ambas; mejor con profundidad |
| Velocidad de la barra | Velocidad media de subida y cuánto cae dentro de la serie (fatiga) | Variable: en un estudio de 2024, una de tres apps fue tan válida como un transductor (PLOS ONE), y otro estudio encontró que una app sobreestima la velocidad (PeerJ) | Primero iPhone con LiDAR, en beta y con validación propia |
| Movilidad | Grados de flexión y extensión de hombro, cadera y rodilla en una prueba guiada | Media; orientativa para el profesional | Ambas |
| Fotos de progreso | Misma distancia, altura y pose, con la foto anterior superpuesta | Alta, porque es una guía y no una medición | Ambas; el LiDAR mide la distancia en iPhone |
| Pulso y VFC con el dedo | Pulso en reposo y VFC en la mañana para quien no tiene reloj | Validada frente a ECG en reposo si la persona no se mueve (HRV4Training, IJSPP) | Ambas, con cámara y flash |
| Postura estática | Inclinación de hombros y pelvis en fotos de frente y de lado | Baja a media; orientativa | Ambas, para fisioterapia |

Reglas:

1. El video se analiza en el teléfono y solo sube si la persona lo comparte con su entrenador.
2. Cada plataforma entrega los puntos del cuerpo (Vision en iPhone, ML Kit o MediaPipe en Android) y el análisis (repeticiones, ángulos, ritmo, velocidad) es Kotlin compartido en `shared:domain`, con pruebas, así que da lo mismo en los dos sistemas.
3. Cada medición muestra su método y su precisión, como exige Apple (guía 1.4.1). La cámara nunca mide oxígeno, presión, glucosa ni temperatura: Apple lo prohíbe.
4. La app nunca diagnostica: muestra ángulos y repeticiones y deja el juicio a la persona o a su profesional.
5. La velocidad de la barra no mueve el motor de progresión hasta que se valide contra un transductor con los entrenos de Juan.

## 6. Todas las disciplinas

IronProgress cubre todas las disciplinas con un solo modelo: cada ejercicio dice qué se anota, cada bloque dice su formato y un solo cronómetro mueve todo. Por disciplina cambian el catálogo, la pantalla y el tipo con que se guarda en Salud y en Health Connect.

### Qué necesita cada una

| Disciplina | Qué se anota | Formatos | Se guarda en Salud / Health Connect como | Etapa |
| --- | --- | --- | --- | --- |
| Fuerza, hipertrofia y powerlifting | Peso y reps, RIR o RPE y tipo de serie; se suman rest-pause, myo-reps y cluster | Series, superseries y circuitos | `traditionalStrengthTraining` / `STRENGTH_TRAINING` | 1 |
| Halterofilia | Peso y reps por complejo (arranque, cargada y envión) y % de 1RM | Series y complejos | `functionalStrengthTraining` / `WEIGHTLIFTING` | 1 |
| Calistenia | Reps, segundos de sostén, lastre o asistencia; habilidades por nivel (muscle-up, parada de manos) | Series y escaleras de progresión | `functionalStrengthTraining` / `CALISTHENICS` | 1 |
| Funcional (estilo CrossFit e HYROX) | Reps, carga, calorías y distancia; resultado por tiempo, por rondas más reps o por carga | EMOM, AMRAP, por tiempo con límite, rondas y chipper | `crossTraining` / `HIGH_INTENSITY_INTERVAL_TRAINING` | 1 |
| HIIT, circuitos y cuerda | Trabajo y descanso por ronda, reps | Tabata e intervalos | `highIntensityIntervalTraining` o `jumpRope` / `HIGH_INTENSITY_INTERVAL_TRAINING` | 1 |
| Deportes de combate | Rounds de trabajo y descanso | Intervalos por rounds | `boxing`, `kickboxing` o `martialArts` / `BOXING` o `MARTIAL_ARTS` | 1 |
| Yoga | Cada postura por segundos o por respiraciones, y por lado | Flujo | `yoga` / `YOGA` | 1 |
| Pilates, en suelo y en reformer | Reps, segundos y resorte del reformer | Series y flujo | `pilates` / `PILATES` | 1 |
| Movilidad, estiramientos y calentamiento | Segundos o respiraciones por lado | Flujo | `flexibility` o `cooldown` / `STRETCHING` | 1 |
| Respiración | Fases de inhalar, retener y exhalar en segundos; ciclos o minutos; pulso antes y después | Patrón de respiración | `mindfulSession` / `MindfulnessSessionRecord` de tipo `BREATHING` | 1 |
| Escalada en tabla de suspensión | Segundos, carga y tamaño del borde | Intervalos | `climbing` / `ROCK_CLIMBING` | 1 |
| Adaptado (silla de ruedas o sentado) | Lo de su disciplina, con ejercicios marcados para silla o sentado | Todos | `wheelchairWalkPace` o `wheelchairRunPace` en cardio / `WHEELCHAIR` | 1 |
| Cardio: correr, bici, remo, natación y elíptica | Distancia, tiempo, ritmo, calorías, vatios y zona de pulso | Continuo o por intervalos | Su tipo propio en los dos (`running` / `RUNNING` y así) | 1 registro e importación; 3 planes al reloj |
| Meditación | Minutos, y ánimo antes y después | Guiada o libre | `mindfulSession` / `MindfulnessSessionRecord` de tipo `MEDITATION` | 3 |
| Rehabilitación y prevención | Reps, segundos, isometría, dolor de 0 a 10 y movilidad | Series y flujo | `preparationAndRecovery` / el de su disciplina | 1 molestias; 4 con fisioterapeuta |
| Deportes de equipo, raqueta y baile | Solo la sesión: duración, pulso y esfuerzo | Sesión | Su tipo propio cuando existe; el pádel no tiene | 1, leídos de Salud y Health Connect |

Las marcas: "CrossFit" solo lo pueden usar los gimnasios afiliados, que pagan USD 1.000 al entrar y USD 4.500 al año ([CrossFit](https://www.crossfit.com/faq/affiliates)), y HYROX también es una marca. Dentro de la app se dice "funcional" y "carrera fitness", y cada coach nombra sus entrenos como quiera.

### Un solo modelo: bloques con formato

Un entreno es una lista de bloques, y cada bloque tiene un formato que dice cómo corre el cronómetro y cómo se puntúa. Las series y superseries de la v2 son el primer formato; los demás se suman.

| Formato | Cómo corre el cronómetro | Resultado | Ejemplo |
| --- | --- | --- | --- |
| Series, con superseries y circuitos (ya en la v2) | Descanso entre series | Peso por reps y récords | Press banca 4 × 8 |
| EMOM | Cuenta atrás de cada minuto; lo que sobra es descanso | Rondas completas | 10 min: 5 dominadas y 10 sentadillas |
| AMRAP | Cuenta atrás del total | Rondas más reps | 12 min de remo, burpees y kettlebell |
| Por tiempo, con límite | Hacia arriba hasta terminar o hasta el límite | Tiempo, o reps al llegar al límite | 21-15-9 de thrusters y dominadas |
| Intervalos a tu medida, Tabata incluido | Trabajo y descanso que elige cada quien, con voz que cuenta y avisa | Máximas reps por ronda | 15 s a tope y 15 s de descanso, 10 rondas |
| Rondas | Total hacia arriba | Tiempo total | 5 rondas de 400 m y 15 swings |
| Flujo | Segundos o respiraciones por postura y por lado, con voz y vibración | Completado y sensación | Saludo al sol, 5 rondas |
| Respiración | Marcapasos de inhalar, retener y exhalar, con animación y vibración | Minutos, y pulso antes y después | Cuadrada 4-4-4-4, 5 min |
| Cardio por intervalos | Tramos por distancia, tiempo o zona de pulso | Distancia, ritmo y zona | 6 × 400 m a ritmo de 5 km |

### Intervalos a tu medida, con voz

Cada persona arma sus intervalos con los tiempos que quiera, y una voz los lleva para que no tenga que mirar la pantalla.

- Tiempos libres: trabajo y descanso desde 5 s, rondas, series de rondas, calentamiento y vuelta a la calma; se guardan como plantillas y se comparten.
- La voz cuenta y avisa: "3, 2, 1, ya", "quedan 5" y "descanso" (sin signos de exclamación, porque también salen en texto); en modo intenso anima en cada ronda, y también se puede usar solo vibración.
- Máximas repeticiones: al terminar cada ronda se anotan con un toque, o con la Corona o el bisel del reloj sin parar.
- La voz baja tu música un momento en vez de pausarla (`duckOthers` en iPhone y foco de audio con atenuación en Android).
- Voz propia grabada para cuentas y frases, que suena mejor que la del sistema; la del sistema queda para los nombres de ejercicios.
- El mismo motor de tiempo lo lleva a la pantalla completa, la isla, la barra de estado y los dos relojes.

Para que el modelo aguante, las listas de la v2 crecen por el final y lo que ya existe no cambia (regla 7 del manual):

- `TrackingType` suma calorías, distancia sola, altura (salto al cajón), segundos o respiraciones por lado y vatios.
- Los tipos de serie suman rest-pause, myo-reps y cluster a los cuatro propuestos (calentamiento, trabajo, drop y fallo).
- `Equipment` suma unos 25: barra hexagonal, balón medicinal, saco, trineo, cajón, cuerda, anillas, barra de dominadas, paralelas, correas de suspensión, rodillo, esterilla, bloque y cinta de yoga, reformer, remo, bici, cinta de correr, ergómetro de esquí, air bike, saco de boxeo y tabla de suspensión, entre otros.
- `MovementPattern` suma rotación, antirrotación, salto, levantamiento olímpico, locomoción, inversión, postura y respiración.
- `Exercise` suma disciplina, nivel, alias (el nombre en sánscrito en yoga), progresiones y la fuente con su licencia.

### El catálogo: cómo llegar a "todos los ejercicios"

"Todos" no cabe en una lista cerrada, así que el catálogo crece en tres capas: sale con unos 1.000 ejercicios y posturas y llega a más de 2.000 entradas con variantes de agarre, postura y equipo.

| Capa | De dónde sale | Etapa |
| --- | --- | --- |
| Base | [free-exercise-db](https://apis.io/providers/free-exercise-db/) (873 ejercicios en dominio público) traducida al español y revisada, más posturas de yoga y pilates, movilidad y patrones de respiración escritos por nosotros; los 77 de hoy conservan sus ids | 1 |
| Propios | Cada persona crea los suyos (ya en la v2) | 1 |
| De profesionales | Entrenadores y creadores suben ejercicios con su video; pasan al catálogo de todos solo después de una revisión | 4 |

- Imágenes y videos propios, con el estilo de Forja: todo se ve igual y nada depende de licencias ajenas. [ExerciseDB, hoy AscendAPI](https://exercisedb.dev/), trae 1.500 ejercicios con GIF, pero no publica una licencia clara para uso comercial, y [wger](https://cdn.jsdelivr.net/gh/wger-project/wger@master/README.md) pone una licencia Creative Commons distinta en cada ejercicio.
- Cada ejercicio guarda de dónde salió y con qué licencia.
- Progresiones: cada ejercicio sabe cuál es más fácil y cuál más difícil (flexión con rodillas, flexión, flexión declinada), y el motor sube por esa escalera cuando no hay peso que subir, en calistenia, yoga y pilates.

### Exigencia y músculos de cada ejercicio

Cada ejercicio guarda cuánto exige y qué músculos trabaja, para que la adaptación sepa qué quitar o cambiar primero.

| Dato | Escala | Para qué sirve |
| --- | --- | --- |
| Músculos con porcentaje | La parte del trabajo de cada músculo (en el press banca, el pecho lleva la mayor parte y tríceps y deltoides frontal el resto) | Series por músculo exactas y el mapa de recuperación |
| Exigencia general | 1 a 5 (peso muerto 5, curl 1) | La adaptación quita o cambia primero lo más exigente |
| Dificultad técnica | 1 a 5 | Variantes más sencillas para principiantes o días cansados |
| Carga en la columna | 0 a 3 | Días con molestias lumbares |
| Impacto por articulación | Bajo, medio o alto en rodilla, cadera, hombro, codo, muñeca y lumbar | Cambiar ejercicios cuando hay molestias |
| Gasto en cardio | MET del [Compendio de actividades físicas 2024](https://doaj.org/article/91ffa80dcdef401c9db9fc7db2a08e42) | Calorías y carga de cardio y circuitos |

Los valores salen de reglas por patrón y equipo, se revisan a mano en los 200 ejercicios más usados y se calibran con el RPE y el pulso de cada persona. Los músculos ganan detalle (cabezas del deltoides, porciones del pecho, glúteo mayor y medio) con una tabla de partes, sin cambiar el enum `Muscle` de hoy.

### Seguridad

- Antes de empezar, el PAR-Q+, un cuestionario de aptitud reconocido internacionalmente ([Warburton y otros, 2011](https://hfjc.library.ubc.ca/index.php/HFJC/article/view/103)): si alguna respuesta es sí, la app recomienda consultar a un médico o a un profesional calificado antes de subir la intensidad. Hay que confirmar con sus autores las condiciones para usarlo en una app comercial.
- Respiración: los patrones con retención larga o con hiperventilación muestran un aviso antes de empezar: nunca en el agua, conduciendo ni de pie, porque pueden causar un desmayo sin aviso ([Chan, Singapore General Hospital](https://www.activesgcircle.gov.sg/read/breath-holding-with-hyperventilation-an-invisible-danger)). La app no trae tablas de apnea.
- Embarazo y posparto (Etapa 3): un modo que oculta los ejercicios marcados y pide el visto bueno de un profesional.
- Nada de esto es consejo médico, y la app lo dice.

## 7. Lo que faltaba

Sí faltaban cosas, y casi todas caben en la etapa que ya les toca; las que cambian el modelo de datos de la Etapa 1 (disciplinas y tamizaje) entran desde F1, aunque su pantalla llegue después.

| Tema | Qué falta | Propuesta | Etapa |
| --- | --- | --- | --- |
| Todas las disciplinas | Formatos, tipos de registro y catálogo para más que fuerza | Modelo de bloques y motor de tiempo desde F1 (sección 6) | 1 |
| Intervalos a tu medida | Tiempos que elige cada quien y una voz que cuenta | Constructor de intervalos con voz y máximas reps por ronda (sección 6) | 1 |
| Adaptación con salud | Bajar peso, series o cardio según cómo llegas | Lectura de Salud y Health Connect desde la Etapa 1 (sección 2) | 1 |
| Tamizaje de salud | Saber si alguien debe consultar antes de entrenar fuerte | PAR-Q+ al crear la cuenta y antes de contratar a un profesional | 1 |
| Salud y Health Connect | Que cada entreno cuente en los anillos de Apple y en Health Connect | Leer y escribir desde la Etapa 1 | 1 |
| Molestias y lesiones | Marcar una zona y su dolor de 0 a 10 | El motor propone evitar o cambiar lo que carga esa zona y la persona decide | 1 |
| Otros relojes y apps | Garmin, Strava, Oura y Apple Fitness | Leer sus entrenos y su sueño desde Salud y Health Connect para la carga de la semana, sin integraciones propias al principio | 1 |
| Ocultar números | Hay quien prefiere no ver su peso ni sus calorías | Un ajuste que los oculta en toda la app | 1 |
| Menores | Quién puede usar la app y con qué límites | Desde los 13 años, con modo joven hasta los 17 (sección 10) | 1 |
| Pulido de movimiento | Animaciones al nivel de Apple | Una etapa propia, publicada como actualización (sección 11) | 2 |
| Nutrición básica | Calorías, proteína y agua | Leer lo que registran otras apps en Salud y Health Connect y anotar rápido proteína y agua; sin base de alimentos propia | 3 |
| Estado de ánimo | Cómo te sientes antes y después | En iPhone se guarda en Salud (State of Mind, iOS 18 o posterior) | 3 |
| Embarazo y posparto | Ejercicios que no convienen en esa etapa | Un modo que los oculta y pide el visto bueno de un profesional | 3 |
| Planes de cardio en el reloj | Intervalos de carrera, bici o natación | [WorkoutKit](https://developer.apple.com/tutorials/data/documentation/workoutkit.md) los manda al app Entreno del Apple Watch con el nombre y el icono de IronProgress; en Android, ejercicio planificado de Health Connect | 3 |
| Venta por la web | Lo que el cliente compra en nuestra web, sin comisión de la tienda | Se puede usar en la app si también se vende dentro de ella (guía 3.1.3(b)); dentro de la app no se puede decir que en la web es más barato, salvo en la UE con sus comisiones (sección 9) | 4 |
| Contenido de usuarios | Programas, videos y mensajes de profesionales | Filtro, reportes con respuesta rápida, bloqueo de usuarios y un contacto publicado (guía 1.2); en la UE, además, un canal para denunciar contenido ilegal (sección 10) | 4 |
| Música en los videos | Canciones con derechos de autor | Prohibido subirlas: cada quien escucha su música con su app | 4 |
| Titulación y seguro | Quién responde por el servicio | Cada profesional declara su titulación, su registro y su seguro de responsabilidad civil, y responde por ellos (sección 10) | 4 |
| Disputas | "Pagué y mi entrenador no responde" | Tiempo máximo de respuesta, reembolso según la tienda y salida del profesional que lo repita | 4 |
| Nutricionistas | Planes de alimentación personalizados | Son profesionales de la salud en Colombia y en España: entran como los fisioterapeutas, con registro verificado y cuando exista la empresa | 4 |
| Retos | El "reto de 30 días" de un creador | Un programa de pago único; sin premios en dinero, que traen reglas de sorteos | 4 |

Lo que dejamos fuera, por ahora:

- Reservas de clases y gestión de gimnasios: es otro producto.
- Base de alimentos y dietas propias.
- GPS con mapa propio: Apple, Strava y Garmin ya lo hacen, y lo leemos de Salud.
- Clases en vivo para grupos: Apple exige compra integrada de uno a muchos, y el video en vivo es caro.
- Menores de edad en la plataforma.

## 8. La plataforma: entrenadores, creadores y fisioterapeutas

La plataforma pone a los profesionales dentro de la misma app: el cliente entrena en IronProgress y su profesional ve lo que el cliente decide compartir, ajusta el plan y le escribe. No es otra app ni un chat aparte.

### Roles

| Rol | Qué hace | Cómo entra | Verificación |
| --- | --- | --- | --- |
| Atleta | Entrena, compra programas, contrata a un profesional y decide qué datos comparte | Cuenta normal | Ninguna |
| Creador (influencer) | Publica programas, gratis o con el precio que elige, y una membresía con su comunidad | Solicita perfil de creador | Identidad y datos de cobro |
| Entrenador | Sigue clientes 1:1: plan, revisión de técnica en video, check-in semanal, mensajes y alertas | Perfil profesional; invita a sus clientes con un enlace o QR | Identidad y cobro; titulación y seguro declarados; en Colombia, su inscripción en el Registro de Entrenadores Deportivos (Ley 2210 de 2022) |
| Fisioterapeuta | Lo mismo que un entrenador, con planes de rehabilitación, dolor por ejercicio y pruebas de movilidad | Llega por su cuenta con sus pacientes y el mismo perfil profesional; se abre cuando exista la empresa (sección 10) | Registro profesional obligatorio: ReTHUS en Colombia, colegiación en España |
| Equipo IronProgress | Aprueba perfiles, modera, resuelve reclamos | Panel interno |  |

### Productos

| Producto | Qué recibe el atleta | Detalle que lo diferencia |
| --- | --- | --- |
| Programa | Semanas de rutinas con videos de técnica del creador | El motor lo adapta a tus números desde la primera sesión; tiene versiones y quien compró recibe las correcciones |
| Membresía de creador | Todos sus programas, publicaciones y preguntas | Precio mensual |
| Coaching 1:1 | Plan hecho para ti, revisión de técnica, check-in semanal, chat y ajustes | Opción de sesión en vivo: el entrenador ve serie, pulso y video mientras entrenas |
| Plan de fisioterapia | Ejercicios de rehabilitación, registro de dolor y pruebas de movilidad | El fisioterapeuta lo arma con las mismas herramientas que un entrenador y decide el alta; su consentimiento informado y su historia clínica los lleva él, fuera de IronProgress |
| Reto | Un programa corto con fecha de inicio y comunidad | Pago único, sin premios en dinero |

Cada profesional pone el precio de cada producto (sección 9).

### Flujos clave

A. Entrenador con sus propios clientes (funciona sin tienda abierta):

1. Crea su perfil y comparte un enlace de invitación.
2. El cliente instala, acepta y elige qué comparte (por defecto, solo sus entrenos).
3. El entrenador asigna un programa o arma uno desde su biblioteca.
4. El cliente entrena con la app de siempre; el entrenador ve las sesiones y recibe alertas (sesión saltada, dolor, preparación baja).
5. Cada semana llega el check-in y el entrenador ajusta.

B. Comprar un programa:

1. Buscar, filtrar por objetivo, nivel, idioma y precio.
2. Perfil del creador y vista previa con la semana 1.
3. Compra con la compra integrada de cada tienda.
4. El programa se agenda en Plan y el motor ajusta los pesos desde la primera sesión de prueba.

C. Fisioterapia, igual que A:

1. El fisioterapeuta crea su perfil, IronProgress verifica su registro y él invita a sus pacientes con su enlace.
2. El paciente hace los ejercicios, marca el dolor de 0 a 10 y repite las pruebas de movilidad.
3. El fisioterapeuta ajusta el plan y da el alta; el paciente sigue con un programa normal y las restricciones que él marcó.

### Dónde vive en la app

- Buscar (el círculo de la isla) suma Programas y Profesionales a los resultados.
- Plan muestra el programa comprado o el plan del entrenador, con su nombre.
- Hoy suma la tarjeta "Tu entrenador" con el último mensaje o el check-in pendiente.
- El perfil (avatar en Hoy) guarda compras, profesionales y permisos de datos.
- Modo profesional: la misma app con otra isla (Clientes · Bandeja · Biblioteca) y un panel web para trabajar con muchos clientes desde el computador.

### Qué ve el profesional de cada cliente

Adherencia, sesiones y series, volumen por músculo, récords, check-ins, videos compartidos y alertas. De salud ve el nivel de preparación calculado por IronProgress (alta, media, baja) y no los datos crudos de pulso, VFC o sueño, salvo que el cliente dé ese permiso aparte.

### Confianza y calidad

- Reseñas solo de compradores verificados, y la ficha dice cómo se verifican.
- Reglas para profesionales: nada de esteroides ni otras sustancias, dietas extremas ni promesas médicas; reportes y moderación (guía 1.2 de Apple).
- Cada profesional responde por su titulación, su seguro y su servicio; IronProgress es la herramienta, y así lo dicen los términos para profesionales.
- Reembolsos según la tienda en programas y según política propia en coaching, con un tiempo máximo de respuesta del profesional.
- Solo mayores de edad compran o contratan profesionales.

## 9. Cobros y modelo de negocio

Las reglas de Apple y Google deciden la mitad del modelo: un programa o una membresía es contenido digital y se cobra con la compra integrada de cada tienda, mientras que el entrenamiento personal 1:1 en tiempo real puede cobrarse por fuera.

### Qué método de pago exige cada producto

| Producto | iPhone | Android | Ojo |
| --- | --- | --- | --- |
| Programa (pago único) | Compra integrada (guía 3.1.1) | Facturación de Google Play | La tienda cobra su comisión antes del reparto |
| Membresía de creador | Suscripción integrada | Google Play la exige para suscripciones de fitness | Igual |
| Coaching 1:1 en tiempo real | Puede usar otro medio de pago: la guía 3.1.3(d) nombra el "fitness training" entre dos personas; uno a pocos o uno a muchos debe usar compra integrada | Exento si es un servicio en línea entre dos personas que no se puede repetir, como un entrenador personal | El coaching sin sesión en vivo (revisar videos, ajustar el plan) es zona gris: confirmarlo con Apple y Google antes de lanzar |
| Fisioterapia | Otro medio de pago (consulta médica en 3.1.3(d)) | Google indica que los servicios clínicos regulados no deben usar su facturación | Exige cuenta de desarrollador de empresa (sección 10) |
| Plan Pro del atleta | Suscripción integrada | Google Play | Ya previsto en la v2 como freemium sin anuncios |
| Herramientas para profesionales | Si se desbloquean en la app de iPhone, Apple puede exigir compra integrada; la excepción de empresas (3.1.3(c)) aplica a ventas a organizaciones, no a un entrenador suelto | Igual con Google Play | Decidido: sin cuota; IronProgress vive de la comisión por venta |

En la UE, desde el 1 de octubre de 2026 Apple cobra 26 % con compra integrada (15 % para pequeños desarrolladores), 20 % con pago alternativo dentro de la app (10 %) y 15 % si la compra se completa en la web (10 %). Google permite facturación alternativa en el Espacio Económico Europeo.

### Modelo decidido: cada profesional pone su precio

Cada profesional elige el precio de lo que vende; de cada venta cobra primero la tienda y después IronProgress su comisión de 5 %, sin cuota mensual. El atleta usa la app gratis y el plan Pro sigue como opción.

Ejemplo: un coaching de 29,99 € al mes vendido en España con compra integrada y la comisión decidida de 5 %.

| Paso | Cuenta | Queda |
| --- | --- | --- |
| Precio que paga el cliente | Incluye 21 % de IVA | 29,99 € |
| IVA | La tienda lo cobra y lo paga al fisco | 24,79 € |
| Comisión de Apple | 15 % del precio sin IVA (pequeños desarrolladores) | 21,07 € |
| Comisión de IronProgress | 5 % de lo que paga la tienda: 1,05 € | 20,01 € para el profesional |

El profesional recibe el 81 % de lo que queda sin IVA, cerca del 67 % del precio; en Colombia, con 19 % de IVA, le queda un 68 %. La app le muestra este desglose mientras elige su precio. El 5 % atrae sobre todo a los profesionales; lo que más baja el precio para el cliente es la venta por la web, que no paga el 15 % de la tienda (sección 7).

Con 5 % la plataforma necesita volumen. Lo que hay que vender al mes, sin impuestos y con 15 % de la tienda, para que IronProgress gane USD 1.000:

| Comisión de IronProgress | Ventas al mes |
| --- | --- |
| 5 % | USD 23.529 |
| 10 % | USD 11.765 |
| 15 % | USD 7.843 |

Propuesta: arrancar con 5 % como precio de lanzamiento, escrito así en los términos, y revisarlo al año con los números reales; de referencia, [TrainHeroic](https://www.trainheroic.com/grow/) deja el 70 % al coach en su marketplace. Ayudan un pago mínimo por profesional (por ejemplo, USD 50) para no gastar la comisión en transferencias y el plan Pro como segunda fuente de ingresos.

Cómo se cobra el precio de cada profesional:

- iPhone, al empezar: suscripciones y compras por nivel de precio creadas en App Store Connect (Apple tiene 900 puntos de precio), y el servidor sabe a qué profesional corresponde cada compra. La hoja de pago muestra el nombre del nivel y no el del profesional; confirmarlo con App Review.
- iPhone, cuando el catálogo crezca: pedir a Apple la [Advanced Commerce API](https://developer.apple.com/in-app-purchase/advanced-commerce-api), hecha para catálogos con muchos creadores. El catálogo vive en nuestro servidor, la hoja de pago muestra el nombre y el precio del plan de cada profesional y la comisión no cambia; Apple recomienda usar sus 900 puntos de precio y no permite ofertas de suscripción ni compartir en familia ([precios](https://developer.apple.com/tutorials/data/documentation/advancedcommerceapi/prices.md), [SKU](https://developer.apple.com/documentation/advancedcommerceapi/creating-your-purchases.md)).
- Android: la [API de Google Play](https://developers.google.cn/android-publisher/api-ref/rest/v3/monetization?hl=en) crea y cambia suscripciones y sus planes desde el servidor, así que cada profesional puede tener la suya.
- Fuera de la tienda: el coaching 1:1 en tiempo real (3.1.3(d)) y lo que se compre en la web sin pasar por la app (sección 7) pagan solo la pasarela y la comisión de IronProgress.

### Cómo llega la plata a cada profesional

- Apple paga dentro de los 45 días siguientes al cierre de cada mes fiscal ([Apple](https://www.developer.apple.com/help/app-store-connect/getting-paid/overview-of-receiving-payments)); el profesional cobra cuando la tienda nos paga, con su reporte de ventas, y los reembolsos se descuentan del pago siguiente.
- Mientras no exista la empresa, se cobra a nombre de una persona con su registro tributario (el RUT en Colombia) y se paga a cada profesional contra su cuenta de cobro o su factura; un contador lo revisa antes de la primera venta.
- Con la empresa en EE. UU., las tiendas y Stripe pagan en dólares a su cuenta allá (Stripe no opera con empresas colombianas). A profesionales en EE. UU. y Europa se les paga con [Stripe Connect](https://docs.stripe.com/connect/cross-border-payouts-to-recipients), que asume el cumplimiento; a cuentas en Colombia, con [Global Payouts](https://docs.stripe.com/changelog/dahlia/2026-03-25/cross-border-payouts-new-countries) desde marzo de 2026, donde ese cumplimiento queda a cargo de la empresa y puede pedir licencia de transmisor de dinero, así que se revisa con el abogado.
- DAC7: si hay vendedores en la UE, la plataforma reporta lo que cobra cada uno; confirmar con el asesor cómo aplica a una empresa de fuera de la UE.
- Impuestos (IVA, retenciones) y facturación de cada profesional: con un contador antes de abrir cobros.

## 10. Legal, privacidad y confianza

La plataforma mueve datos de salud entre personas, así que todo lo que ve un profesional sale de un permiso explícito, por tipo de dato y por profesional, que se puede retirar en un toque y deja registro de quién vio qué.

### Permisos de datos

| Tipo de dato | Por defecto | Nota |
| --- | --- | --- |
| Entrenos, series y récords | Compartido al aceptar al profesional | Es el servicio que se contrata |
| Nivel de preparación (alta, media, baja) | Compartido | Es un dato derivado que calcula IronProgress |
| Pulso, VFC, sueño y temperatura crudos | No compartido; permiso aparte | Health Connect prohíbe pasar datos de salud a terceros sin consentimiento explícito, y Apple solo permite usarlos para mejorar la gestión de la salud y con permiso (guía 5.1.3) |
| Fotos de progreso y videos | Solo los que la persona envía | Se pueden borrar |
| Dolor y movilidad | Solo con el profesional que la persona elija | Dato de salud |

Al terminar la relación con el profesional, el acceso se corta solo.

### Reglas y normas que aplican

| Tema | Qué exige | Qué hace IronProgress |
| --- | --- | --- |
| Datos de salud | Ley 1581 de 2012 en Colombia y RGPD en la UE: son datos sensibles o de categoría especial y necesitan consentimiento explícito | Consentimiento por tipo de dato, registro de accesos, borrado total y una evaluación de impacto antes de abrir la plataforma |
| Empresa | Apple pide que las apps con servicios de salud las publique una empresa y no una persona (guía 5.1.1(ix)) | Se arranca como persona y la empresa, en EE. UU., se crea antes de abrir a fisioterapeutas y nutricionistas (abajo). Apple deja convertir la cuenta de persona en cuenta de empresa sin perder las apps, según [desarrolladores que lo hicieron](https://developer.apple.com/forums/thread/756190) en una o dos semanas |
| Datos públicos del vendedor | En la UE, quien se declara comerciante ante Apple muestra en la ficha de la app una dirección, un teléfono y un correo; una persona puede poner un apartado postal ([Apple](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-compliance-information)) | Al empezar a cobrar en la UE, declararse comerciante con un apartado postal y un teléfono de trabajo, o con la dirección de la empresa cuando exista |
| Titulación de entrenadores | En Colombia, la [Ley 2210 de 2022](https://dmsjuridica.com/buscador_20179478954/legislacion/leyes/2022/11/01/ley-2210-de-2022/?print=pdf) exige estar en el Registro de Entrenadores Deportivos para ejercer como entrenador deportivo; en España, [comunidades como Cataluña (2008) y Madrid (2016)](https://fitgeneration.es/se-puede-ser-entrenador-personal-sin-titulacion/) regulan las profesiones del deporte | Cada profesional declara su titulación y su registro y responde por ellos; la insignia de verificado solo aparece con un registro oficial comprobado |
| Fisioterapia en Colombia | La Resolución 1644 de 2026 (31 de julio, reemplazó a la 2654 de 2019) separa la telesalud de orientación y educación, sin habilitación, de la telemedicina (teleconsulta, telemonitoreo y otras), que exige habilitación en el REPS, consentimiento informado e historia clínica; la IA solo puede apoyar el juicio clínico | La app es la herramienta del profesional: no presta el servicio de salud ni habilita a nadie; el fisioterapeuta lleva su consentimiento informado y su historia clínica, y la app exporta lo que el paciente comparte. Solo entran fisioterapeutas con ReTHUS vigente (consulta pública y gratuita) |
| Fisioterapia en España | Profesión sanitaria colegiada | Verificar el número de colegiado con su colegio |
| Producto sanitario | En la UE, un software es producto sanitario según su finalidad declarada: si analiza datos para apoyar diagnósticos o tratamientos, suele serlo y la Regla 11 lo pone en clase IIa o superior. Una app de bienestar sin afirmaciones médicas no lo es | Movilidad, postura y dolor se presentan como herramientas para el criterio del profesional, sin diagnósticos ni promesas médicas. Revisar textos de la app y de las tiendas con un abogado regulatorio en cada país |
| Mediciones | Apple exige declarar método y precisión de cualquier medición de salud (guía 1.4.1) | Cada medición de cámara o sensor muestra cómo se obtuvo |
| Contenido de usuarios | Apple pide filtro, reportes con respuesta rápida, bloqueo de usuarios y un contacto publicado (guía 1.2); en la UE, la [Ley de Servicios Digitales](https://www.cliffordchance.com/insights/resources/blogs/talking-tech/en/articles/2022/12/taking-a-closer-look-at-the-digital-services-act.html) pide a todo servicio que aloje contenido, sin importar su tamaño, un canal para denunciar lo ilegal | Moderación desde la Etapa 4, con las reglas para profesionales de la sección 8 |
| Pagos a menores | En la UE, Apple pide control parental para pagos alternativos de menores de 18 | La tienda y los profesionales son solo para mayores de edad |

### Empresa en Estados Unidos: qué resuelve y qué no

Se puede, y es común entre fundadores de otros países: la plata de las tiendas y de Stripe llega a una cuenta allá. Pero la empresa suma las leyes de EE. UU. a las de cada país donde haya usuarios; no las reemplaza.

| Tema | Con empresa en EE. UU. |
| --- | --- |
| Crearla | [Stripe Atlas](https://www.rho.co/blog/stripe-atlas-review) forma una LLC o una C corp de Delaware por USD 500 con el número fiscal (EIN); sin número de seguro social, el EIN tarda de 10 a 35 días hábiles |
| Apple y Google | Cuenta de desarrollador de empresa (piden número D-U-N-S); cumple la guía 5.1.1(ix) para abrir a fisioterapeutas y nutricionistas, y en la UE la ficha muestra la dirección de la empresa y no la tuya |
| Cobros | Las tiendas pagan en dólares; Stripe funciona para la web y para pagar a profesionales (sección 9) |
| Obligaciones allá | Una LLC con un solo dueño extranjero presenta cada año el [Formulario 5472](https://www.sdocpa.com/form-5472-foreign-owned-llc-guide/); no presentarlo cuesta USD 25.000 por formulario |
| Leyes de EE. UU. por tener usuarios allá | La ley de Washington [My Health My Data](https://www.stoel.com/insights/publications/faq-washington-states-my-health-my-data-act) pide consentimiento aparte para recoger y para compartir datos de salud, alcanza a empresas de fuera del estado y deja demandar a los usuarios; COPPA protege a los menores de 13 |
| Lo que no cambia en la UE | El RGPD aplica a quien ofrece servicios a personas en la UE y pide un [representante en la UE](https://captaincompliance.com/education/gdpr-article-27-eu-representative-requirements-enforcement-and-compliance-steps-for-non-eu-companies/), porque tratar datos de salud no es algo ocasional; las ventas por web pagan el IVA del país de cada cliente por la [ventanilla OSS para empresas de fuera de la UE](https://helpdesk.hellotax.com/hc/en-150/articles/49817186344209-Non-Union-OSS-for-Non-EU-Digital-Service-Providers) |
| Lo que no cambia en Colombia | La Ley 1581 sigue aplicando, y los servicios digitales desde el exterior pagan [IVA de 19 %](https://trykintsugi.com/es-419/sales-tax-guides/latam/colombia): la empresa se registra ante la DIAN o el impuesto se retiene en el pago |
| Desde dónde se maneja | Si las decisiones se toman en Colombia, la DIAN puede tratar la empresa como colombiana por todo el año ([sede efectiva de administración](https://accounter.co/normatividad/una-sociedad-extranjera-sede-efectiva-de-administracion-es-considerada-una-sociedad-nacional-por-todo-el-ano-gravable-sus-ingresos-estarian-sometidos-a-impuesto-concepto-42-de-2024)); en España pasa lo mismo si desde allí se dirige y controla ([sede de dirección efectiva](https://sede.agenciatributaria.gob.es/Sede/ayuda/manuales-videos-folletos/manuales-practicos/manual-sociedades-2024/capitulo-01-cuestiones-generales/quienes-son-contribuyentes-impuesto-sobre-sociedades/residencia-domicilio-fiscal.html)) |
| Tus impuestos | Los pagas donde vives, Colombia hoy o España si te mudas; en España no está resuelto del todo cómo tributa una LLC de EE. UU. ([Garrigues](https://www.garrigues.com/es_ES/noticia/regimen-fiscal-de-una-llc-limited-liability-company)), así que hace falta asesor fiscal desde el principio |

### Menores: desde los 13 años, con modo joven

Decidido: desde los 13 años, con un modo joven hasta los 17 y permiso de los padres donde la ley lo pide.

| Dónde | Qué pide la ley | Qué hace IronProgress |
| --- | --- | --- |
| EE. UU. | [COPPA](https://www.finnegan.com/en/insights/articles/coppas-amended-rule-is-now-in-full-effect-what-operators-need-to-know.html): permiso verificable de los padres antes de recoger datos de menores de 13; las reglas nuevas rigen por completo desde el 22 de abril de 2026 | No admite menores de 13 |
| España | Desde los 14 años la persona consiente sola; por debajo, sus padres ([LOPDGDD, artículo 7](https://www.ineaf.es/tribuna/rgpd-tratamiento-de-datos-de-menores/)) | A los 13 años, con permiso de los padres |
| Colombia | Los datos de menores solo se tratan si respetan su interés superior, con autorización previa, expresa e informada de su representante y oyendo al menor ([SIC](https://ambitojuridico.com/sites/default/files/BancoMedios/Archivos/cpto-217036-20.pdf)) | Todo menor de 18 entra con la autorización de un padre o tutor |
| iPhone | [Declared Age Range](https://ppc.land/declared-age-range/) (iOS 26) da el rango de edad que declaró la familia, sin fecha de nacimiento | Activa el modo joven sin pedir la fecha exacta |

Modo joven, de 13 a 17 años:

- Sin pruebas de 1RM ni series al fallo; progresión más lenta y técnica primero.
- Sin peso corporal ni calorías a la vista por defecto, sin fotos de progreso y sin registro de comida.
- Sin tienda ni compras, y sin mensajes con profesionales ni con otros usuarios: ningún adulto le escribe a un menor dentro de la app.
- Intervalos y retos con intensidad limitada, sin el modo intenso.
- Adaptación más prudente: alarga antes los descansos y nunca sube la carga dentro de la misma sesión.

Por debajo de 13 no entra nadie por ahora: con cuentas manejadas por los padres, en EE. UU. haría falta el permiso verificable de COPPA.

Antes de cobrar: asesoría legal y tributaria en EE. UU. y en el país donde vivas. Este documento ordena los temas; no es asesoría legal.

## 11. Revisión del diseño

Forja aguanta el crecimiento si se respeta su regla de oro: tres pestañas y una sola acción principal por pantalla. Lo nuevo entra por Buscar, Plan, Hoy, el perfil y un modo profesional aparte, nunca por una cuarta pestaña. Revisé el sistema Forja (sección 12 de `HANDOFF.md`) y las 15 pantallas que había en el lienzo contra el nuevo funcionamiento; hoy son 29 y están descritas en `docs/PANTALLAS.md`.

### Lo que se mantiene

- Las dos temperaturas: brasa para el esfuerzo, hielo para la recuperación, oro solo para récords y peligro solo para borrar.
- La isla de tres pestañas con el círculo de Buscar, y el vidrio solo en la navegación.
- El presupuesto de toques: ninguna función nueva puede subirlo.
- La tarjeta de preparación sin puntaje de 0 a 100: dice qué hacer.
- "Nunca dos ejes" en las gráficas.

### Lo que cambia

| Pieza | Hoy en Forja | Cambio propuesto | Por qué |
| --- | --- | --- | --- |
| Descanso | La barra del entreno se transforma en el descanso en el mismo lugar, con un anillo de 40 dp | Al completar la serie, la barra crece hasta ser la pantalla: anillo grande, tiempo en cifra héroe, pulso y la siguiente serie; -15 s, +15 s y Saltar quedan abajo, en la franja de la barra. Deslizar hacia abajo la devuelve a barra para ver la lista, y al estar listo vuelve sola con la siguiente serie cargada | Lo decidió Juan: dentro de la app el descanso se ve grande, y el pulgar sigue en el mismo sitio |
| Bloques con formato | Solo series y superseries | Cabecera del bloque con su formato y su reloj (EMOM 4 de 10, AMRAP 7:12, Tabata 3 de 8); la barra cambia su texto según el formato: Completar ronda, Siguiente postura | Todas las disciplinas usan la misma barra y la misma pantalla |
| Completar serie fuera del entreno | El accesorio de sesión no completa series: "se completa viendo los valores" | La regla pasa a ser "se completa solo donde se ven los valores que se guardan": sí en la Live Activity expandida, en la Live Update y en el reloj; no en el accesorio ni en la vista compacta | La isla y el reloj muestran peso y reps antes del botón |
| Buscar | Ejercicios, rutinas e historial | Suma Programas y Profesionales, agrupados, y un filtro por disciplina | Es la puerta de la tienda sin una cuarta pestaña |
| Tarjeta de preparación | Dos datos fijos: sueño y pulso en reposo (o VFC) | Sigue con dos, pero muestra los dos que más pesaron hoy; el detalle lista todas las señales con su línea base | Con temperatura, respiración y oxígeno ya no caben todas |
| Color | Brasa, hielo, oro, peligro y calor del mapa | Escala de zonas de pulso de 5 pasos derivada de brasa, siempre con el número de zona. Un set de estado (bien, atención, crítico) solo para el panel profesional, que no se parezca al oro. Validar contraste y daltonismo antes de adoptarlos | El atleta mantiene dos temperaturas; el profesional necesita semáforos |
| Vidrio en Compose | La guía dice Haze 1.7 con `hazeEffect` y `HazeStyle` | Actualizar a Haze 2.0.1 (`hazeBlur`, `HazeBlurStyle`, `HazeColorEffect`) | La API cambió y el código usará la nueva |
| Iconos | Unos 30 iconos | Agregar `videocam`, `photo_camera`, `straighten` (movilidad), `speed` (velocidad), `groups` (clientes), `chat`, `verified`, `storefront` y `self_improvement` (yoga y respiración) | Funciones nuevas |
| Reloj | 2 caras del Galaxy Watch4 | Agregar el Apple Watch Series 12 (pantalla rectangular, Doble toque) y la Live Activity pequeña del Smart Stack | Llega el Apple Watch |

### Componentes nuevos

- **Descanso a pantalla completa:** anillo grande en hielo que pasa a brasa al estar listo, tiempo en cifra héroe, pulso y la siguiente serie; los controles van en la franja de la barra, con los mismos botones de 48 dp.
- **Reloj de bloque:** cabecera con el formato y su reloj (EMOM 4 de 10, AMRAP 7:12, Tabata 3 de 8 · Trabajo), movida por el mismo motor que el descanso.
- **Guía de respiración:** un círculo que crece al inhalar, se queda quieto al retener y se encoge al exhalar, con vibración en cada cambio, para seguirla con los ojos cerrados.
- **Postura:** ilustración propia, nombre en español y en sánscrito, lado y cuenta de respiraciones.
- **Actividad en vivo:** la sesión en la Isla Dinámica (compacta, mínima, expandida), la pantalla bloqueada, StandBy, el Smart Stack y la Live Update de Android. El fondo de la isla siempre es negro: brasa y hielo van en sus tonos del tema oscuro, cifras con Archivo Cifras tabulares.
- **Cámara:** pantalla completa oscura; esqueleto en hielo, trayectoria de la barra en brasa, contador de reps en cifra héroe; controles sobre vidrio y nada encima del cuerpo.
- **Tarjeta de profesional:** foto, nombre, verificado, especialidad y precio; sin estrellas sueltas sin número de reseñas.
- **Permiso de datos:** una fila por tipo de dato con interruptor y una línea que dice quién lo verá.
- **Mensaje del entrenador:** burbuja con su avatar; las correcciones al plan llegan como cambios aceptables con un toque.
- **Tabla densa** para el panel profesional: clientes con adherencia, preparación, dolor y último check-in.

### Etapa 2: pulido de movimiento al nivel de Apple

Una etapa sin funciones nuevas, solo movimiento, que se publica como actualización propia para que cada usuario note la mejora sin aprender nada nuevo.

- Reglas: toda animación se puede interrumpir y sigue al dedo; los resortes conservan la velocidad del gesto y no hay curvas lineales; lo que se toca crece hasta su detalle y vuelve al mismo sitio; la vibración va sincronizada con cada cambio; todo corre a 120 Hz en el iPhone 18 Pro; y con Reducir movimiento todo pasa a fundidos.
- Qué se pule: la navegación entre pantallas, completar serie, el descanso a pantalla completa y su paso a la isla, las cifras que ruedan, las gráficas que entran, las listas al reordenar, las hojas inferiores, las celebraciones de récord, los relojes y la Live Activity.
- Cómo se mide: en iPhone, 5 ms o menos de tirones por cada segundo de animación, con las métricas de animación de XCTest ([WWDC20](https://wwdcnotes.com/notes/wwdc20/10077)); en Android, JankStats y Macrobenchmark en el S25 Ultra y en un teléfono de gama media; y un video antes y después de cada flujo.
- Qué deja: un catálogo de movimiento en Forja, con cada transición, su nombre y su resorte, que usan todas las pantallas que vengan después.
- Cómo se nota: notas de versión "Más fluida", y la calificación y la duración de las sesiones comparadas antes y después.

### Pantallas nuevas para diseñar

| Grupo | Pantallas |
| --- | --- |
| Disciplinas (Etapa 1) | Descanso a pantalla completa, Constructor de intervalos, Intervalos con voz en curso, Bloque funcional (EMOM, AMRAP y por tiempo), Flujo de yoga, Respiración guiada, Catálogo por disciplina con exigencia, PAR-Q+ al empezar |
| Salud y adaptación (Etapa 1) | Adaptación del día (qué cambió y por qué), Ajuste durante el entreno (teléfono y reloj), Molestias y lesiones, Preparación en detalle, Entreno con pulso y zonas |
| Menores (Etapa 1) | Modo joven, Permiso de los padres |
| Relojes (Etapa 1) | Apple Watch Series 12: rutina del día, serie, cronómetro, intervalos y listo; Galaxy Watch: lo mismo en pantalla redonda; Smart Stack y Tile |
| Isla y barra (Etapa 1) | Live Activity en sus cuatro vistas, Live Update y Now Bar |
| Atleta (Etapas 3 y 4) | Cámara de técnica, Resultado por repetición, Foto de progreso guiada, Recuperación después del entreno, Nutrición del día, Explorar (dentro de Buscar), Perfil de profesional, Programa (detalle y compra), Mi entrenador, Check-in semanal, Permisos de datos |
| Profesional (Etapa 4) | Clientes, Ficha del cliente, Revisión de video con dibujo y nota de voz, Editor de programa, Panel web |

Son unas 50 pantallas y vistas, contando el ajuste durante el entreno en el teléfono y en el reloj. Ya hay 14 en el canvas: descanso a pantalla completa, dos de intervalos, adaptación antes y durante, cuatro del Apple Watch, disciplinas, yoga, respiración, AMRAP y la ficha de ejercicio. Siguen el PAR-Q+, el modo joven con el permiso de los padres, las molestias, el Galaxy Watch y la isla.

## 12. Arquitectura y datos

La base KMP aguanta todo esto: lo nuevo son piezas nativas pequeñas alrededor (Swift para la isla, el reloj y la cámara de Apple; Kotlin para Wear OS y Android) que hablan con el mismo dominio compartido.

### Piezas nuevas

| Pieza | Módulo | Tecnología | Qué comparte con KMP |
| --- | --- | --- | --- |
| Motor de tiempo | `shared:domain` | Kotlin puro: un reloj por sesión con estados (trabajo, descanso, postura, fase de respiración) calculados desde la hora de inicio | Todo: la pantalla completa, la Live Activity, la Live Update y los relojes leen el mismo estado |
| Motor de adaptación | `shared:domain` | Kotlin puro: reglas con explicación, sin aprendizaje automático al principio; corre antes y durante la sesión | Todo |
| Voz y audio | `shared:ui` con `expect`/`actual` | `AVSpeechSynthesizer` y `AVAudioSession` con `duckOthers` en iPhone; `TextToSpeech` y foco de audio con atenuación en Android; voz grabada en los recursos | Lo que se dice sale del motor de tiempo |
| Catálogo ampliado | `shared:data` | Semilla versionada con la fuente, la licencia y la exigencia de cada ejercicio | Todo |
| Live Activity y widgets | `iosApp` más una extensión de widgets | Swift, ActivityKit, WidgetKit, App Intents | El estado de la sesión, por una interfaz Kotlin que Swift implementa y Koin inyecta |
| App de Apple Watch | `watchApp` (nuevo) | SwiftUI, HealthKit (`HKWorkoutSession`), WatchConnectivity; la rutina funciona sin el teléfono; pruebas en el Series 12 | El dominio puede compilarse para watchOS; confirmarlo en una exploración corta |
| Planes de cardio al reloj | `iosApp` | Swift, WorkoutKit | El plan sale del dominio |
| Cámara en iPhone | `iosApp` | AVFoundation, `AVCaptureControl`, LockedCameraCapture, Vision, ARKit | Los puntos del cuerpo pasan a Kotlin; el análisis es común |
| HealthKit | `shared:health` (iosMain) | HealthKit | La interfaz `HealthRepository` |
| Wear OS | `wearApp` (ya planeado en F0) | Compose for Wear OS, Health Services, Data Layer; rutina y cronómetro desde la Etapa 1 y Samsung Health Sensor SDK en la Etapa 3 | `shared:domain` |
| Live Updates | `androidApp` | Notificaciones de Android 16 y Metric Style de Android 17 | El estado de la sesión |
| Health Connect y Samsung Health Data SDK | `shared:health` (androidMain) |  | La interfaz `HealthRepository` |
| Cámara en Android | `shared:ui` (androidMain) | CameraX con ML Kit o MediaPipe | El análisis es común |
| Análisis de movimiento | `shared:domain` | Kotlin puro: repeticiones, ángulos, ritmo, velocidad | Todo |
| Backend | Supabase | Cuentas, Postgres con Row Level Security, almacenamiento, tiempo real para el chat, funciones para pagos y push | Modelos con kotlinx-serialization |
| Push | Servidor | APNs (incluye push de Live Activities) y FCM |  |
| Pagos | App y servidor | StoreKit 2 y Google Play Billing con verificación en el servidor; Advanced Commerce API cuando crezca el catálogo; Stripe cuando exista la empresa en EE. UU. |  |
| Panel web profesional | `webApp` (nuevo) | Compose Multiplatform para web o una web aparte: decidir con una exploración | Dominio y modelos |
| Video | Almacenamiento de Supabase al inicio; un servicio de video si crece |  |  |

### Entidades nuevas (se suman a las 20 de la v2)

| Entidad | Campos clave | Etapa |
| --- | --- | --- |
| WorkoutBlock | id, routineId o sessionId, orden, formato (series, EMOM, AMRAP, por tiempo, intervalos, rondas, flujo, respiración), rondas, trabajoSeg, descansoSeg, límiteSeg, resultado | 1 |
| IntervalTemplate | id, nombre, trabajoSeg, descansoSeg, rondas, series, voz, intensidad | 1 |
| BreathingPattern | id, nombre, fases (inhalar, retener, exhalar, retener) en segundos, ciclos o minutos, nivelDeRiesgo | 1 |
| ExerciseProgression | exerciseId, másFácilId, másDifícilId | 1 |
| ExerciseSource | exerciseId, fuente, licencia, autor | 1 |
| ExerciseDemand | exerciseId, general (1 a 5), técnica (1 a 5), columna (0 a 3), impactoPorArticulación, met | 1 |
| ScreeningAnswer | id, userId, fecha, respuestas del PAR-Q+, resultado | 1 |
| GuardianConsent | id, userIdDelMenor, tutor, país, alcance, otorgadoEn, revocadoEn | 1 |
| AdaptationLog | id, sessionId, momento (antes o durante), señales, cambiosPropuestos, aceptado | 1 |
| PainEntry | id, userId, sessionExerciseId, zona, dolor (0 a 10) | 1 |
| Injury | id, userId, zona, desde, hasta, nota | 1 |
| MediaAsset | id, dueño, tipo, ruta, duración, compartidoCon | 3 |
| SetAnalysis | id, setId, reps, recorrido, ritmo, velocidadMedia, pérdidaVelocidad, fuente | 3 |
| MobilityTest | id, userId, articulación, lado, grados, fecha, método | 3 |
| MoodEntry | id, userId, sessionId, momento (antes o después), valor | 3 |
| NutritionDay | fecha, calorías, proteínaG, aguaMl, fuente | 3 |
| ProfessionalProfile | id, userId, tipo (creador, entrenador, fisio), bio, especialidades, idiomas, verificación, registroProfesional, titulación, seguro | 4 |
| Offering | id, profesionalId, tipo (programa, membresía, coaching, fisio, reto), precio que fija el profesional, moneda, productoTienda o sku | 4 |
| ProgramVersion | id, programId, versión, publicadoEn | 4 |
| Purchase | id, userId, offeringId, canal (App Store, Google Play o web), estado, vence | 4 |
| CoachingRelationship | id, profesionalId, clienteId, estado, inicio, fin | 4 |
| DataGrant | id, relaciónId, tipoDato, otorgadoEn, revocadoEn | 4 |
| Assignment | id, relaciónId, programId o routineId, desde, hasta | 4 |
| CheckIn | id, relaciónId, fecha, respuestas, pesoKg, fotos | 4 |
| Message | id, relaciónId, autor, texto, adjunto, enviadoEn | 4 |
| VideoAnnotation | id, mediaId, autor, segundo, dibujo, nota | 4 |
| Review | id, purchaseId, estrellas, texto | 4 |
| Payout | id, profesionalId, periodo, bruto, comisiónTienda, comisiónIronProgress, reembolsos, neto | 4 |
| AccessLog | id, quién, qué, cuándo | 4 |
| Report | id, autor, objeto, motivo, estado | 4 |

DailyReadiness (v2) suma temperaturaDesvío, respiración, oxígeno y energyScoreSamsung; WorkoutSet ya trae pulsoFin y recuperaciónSeg.

Exercise (v2) suma disciplina, nivel, alias, progresiones, fuente y exigencia; ExerciseMuscle (v2) suma el porcentaje de trabajo de cada músculo; WorkoutSet suma calorías, altura, respiraciones, vatios y el bloque al que pertenece. Los valores nuevos de los enums van al final de cada lista (sección 6).

### Lo que conviene hacer desde F1 para no rehacer después

- Cada rutina y programa guarda quién lo creó (`createdBy`: el usuario o un profesional) y si viene de un programa publicado.
- Las tablas del usuario ya llevan `user_id`, `created_at`, `updated_at` y `deleted_at` (regla de la v2).
- El temporizador de F1 nace como motor de tiempo: pantalla completa en la app, Live Activity en iOS y Live Update en Android, con los mismos estados para todos los formatos, intervalos con voz incluidos.
- `WorkoutBlock`, `IntervalTemplate` y los tipos de registro nuevos entran al esquema de Room en F1 (migración 1 a 2), aunque algunas pantallas lleguen después.
- El catálogo ampliado conserva los ids de los 77 ejercicios de hoy y guarda la fuente, la licencia y la exigencia de cada uno.
- La lectura de salud, la adaptación y su registro (`AdaptationLog`) llegan con F4, que vuelve a la Etapa 1.

## 13. Requisitos nuevos

Son 78 requisitos funcionales y 9 no funcionales que se suman a los 51 y 10 de la v2, ordenados por etapa. La columna Etapa usa las cuatro etapas de la sección 14: 1 (app personal con salud y relojes), 2 (pulido de movimiento), 3 (salud avanzada y cámara) y 4 (plataforma de profesionales).

| ID | Requisito | Etapa |
| --- | --- | --- |
| RF-DIS-01 | Disciplina por ejercicio y por sesión (sección 6), que define qué se anota y con qué tipo se guarda en Salud y Health Connect | 1 |
| RF-DIS-02 | Bloques con formato: series, superseries y circuitos (ya en la v2), EMOM, AMRAP, por tiempo con límite, intervalos, rondas, flujo y patrón de respiración | 1 |
| RF-DIS-03 | Tipos de registro nuevos: calorías, distancia sola, altura, segundos o respiraciones por lado y vatios; tipos de serie rest-pause, myo-reps y cluster | 1 |
| RF-DIS-04 | Un solo motor de tiempo para descanso, intervalos, posturas y respiración, que manda la pantalla completa, la Live Activity, la Live Update y los relojes | 1 |
| RF-DIS-05 | Catálogo de unos 1.000 ejercicios y posturas al lanzar, con nombres en español e inglés (y sánscrito en yoga), músculos, equipo, patrón, disciplina, nivel, fuente y licencia | 1 |
| RF-DIS-06 | Progresiones y regresiones por ejercicio, que el motor usa cuando no hay peso que subir | 1 |
| RF-DIS-07 | Resultado de cada formato (tiempo, rondas más reps o carga) y récords por formato | 1 |
| RF-DIS-08 | Guía por voz y vibración en flujos, intervalos y respiración, también con la pantalla apagada | 1 |
| RF-DIS-09 | Respiración guiada con patrones (cuadrada, 4-7-8, coherente a unas 6 por minuto, suspiro fisiológico, alterna) y aviso de seguridad antes de los que tienen retención o hiperventilación | 1 |
| RF-DIS-11 | Ejercicios marcados para silla de ruedas o sentado, y sesiones con los tipos de silla de ruedas de Salud y Health Connect | 1 |
| RF-DIS-12 | Intervalos a tu medida: trabajo, descanso, rondas y series que elige cada persona, voz que cuenta y avisa, máximas reps por ronda y plantillas para guardar y compartir | 1 |
| RF-DIS-13 | Exigencia por ejercicio (general, técnica, columna, articulaciones y MET) y porcentaje de trabajo por músculo | 1 |
| RF-DIS-14 | Catálogo que crece a más de 2.000 entradas con variantes de agarre, postura y equipo | 1 |
| RF-ADA-01 | Adaptación del día con sueño, pulso en reposo, VFC, check-in, molestias y carga de otras apps: baja peso, series, reps o cardio y cambia ejercicios exigentes, siempre con explicación y la decisión de la persona | 1 |
| RF-ADA-02 | Adaptación durante la sesión cuando los datos lo piden (pulso en vivo, recuperación entre series, reps y RPE): alarga el descanso solo, propone bajar peso, quitar una serie o el cardio o cambiar el ejercicio, y propone parar si el pulso no baja ni descansando; se acepta desde el teléfono, la isla o el reloj | 1 |
| RF-ADA-03 | Registro de cada adaptación (señales, cambio y si se aceptó) para calibrar el motor | 1 |
| RF-SEG-01 | PAR-Q+ al crear la cuenta y antes de contratar a un profesional, con la recomendación de consultar si alguna respuesta es sí | 1 |
| RF-SEG-02 | Molestias y lesiones: zona y dolor de 0 a 10; el motor propone evitar o cambiar lo que carga esa zona | 1 |
| RF-CFG-03 | Ocultar en toda la app el peso corporal y las calorías | 1 |
| RF-CFG-04 | Edad mínima de 13 años; de 13 a 17, modo joven con funciones limitadas y permiso de los padres donde la ley lo pida, con Declared Age Range en iPhone | 1 |
| RF-SAL-07 | App de Apple Watch: rutina del día sin el teléfono cerca, completar serie con Doble toque, Corona para ajustar, cronómetro animado, intervalos y pulso en vivo reflejado en el iPhone | 1 |
| RF-SAL-10 | Escribir cada sesión en Salud y en Health Connect con el tipo de su disciplina, y el esfuerzo de la sesión | 1 |
| RF-SAL-11 | App de Galaxy Watch con lo mismo: rutina, cronómetro con el anillo en el borde, bisel, intervalos y pulso | 1 |
| RF-INT-01 | Leer entrenos y sueño de otras apps y relojes (Garmin, Strava, Oura, Apple Fitness) desde Salud y Health Connect para la carga de la semana | 1 |
| RF-PRG-07 | Zonas de pulso personales y esfuerzo por serie (pico y segundos de recuperación) | 1 |
| RF-PRG-09 | Calentamiento según el estado de recuperación de los músculos del día | 1 |
| RF-VIV-01 | Live Activity del entreno en iPhone: compacta, mínima, expandida, pantalla bloqueada, StandBy y horizontal | 1 |
| RF-VIV-02 | Botones en la Live Activity: completar serie, -15 s, +15 s y saltar | 1 |
| RF-VIV-03 | La Live Activity en el Smart Stack del Apple Watch, con Doble toque | 1 |
| RF-VIV-04 | Live Update en Android (barra de estado, pantalla bloqueada y Now Bar) y plantilla Metric Style donde exista | 1 |
| RF-VIV-05 | Alertas en vivo de récord y de "listo" por pulso | 1 |
| RF-VIV-08 | Descanso a pantalla completa dentro de la app, que pasa a la Live Activity o a la Live Update al salir de la app o al apagar la pantalla | 1 |
| RF-PUL-01 | Transiciones compartidas, gestos interrumpibles, cifras que ruedan y vibración sincronizada en los 10 flujos clave, con versión para Reducir movimiento | 2 |
| RF-PUL-02 | Catálogo de movimiento en Forja, con cada transición, su nombre y su resorte | 2 |
| RF-SAL-05 | Leer de HealthKit lo avanzado: temperatura de muñeca, frecuencia respiratoria, oxígeno, VO2 máx y recuperación del pulso al minuto | 3 |
| RF-SAL-06 | Leer de Samsung Health el Energy Score, la composición corporal, la temperatura de piel y el sueño | 3 |
| RF-SAL-08 | Galaxy Watch: pulso con intervalos entre latidos durante el descanso y composición corporal bajo demanda con el Samsung Health Sensor SDK | 3 |
| RF-SAL-09 | Pulso desde AirPods Pro 3 o una banda Bluetooth cuando no hay reloj | 3 |
| RF-PRG-06 | Preparación ampliada con temperatura nocturna, frecuencia respiratoria y oxígeno, siempre con su explicación | 3 |
| RF-PRG-08 | Contexto de fase (volumen, definición, mantenimiento) según peso y composición, que cambia qué cuenta como progreso | 3 |
| RF-ANA-08 | Recuperación después de la sesión: pulso al minuto, carga de la semana frente a las 4 anteriores y sueño de esa noche | 3 |
| RF-ANA-09 | Tendencia de VO2 máx para quien combina cardio | 3 |
| RF-CAM-01 | Abrir la cámara de IronProgress con el botón de cámara del iPhone, incluso bloqueado, con modos al deslizar | 3 |
| RF-CAM-02 | Contar repeticiones y medir ritmo con la cámara, en el teléfono, y adjuntarlo a la serie | 3 |
| RF-CAM-03 | Guía de técnica por ejercicio (ángulos clave y simetría) mostrada como sugerencia | 3 |
| RF-CAM-04 | Velocidad media de la barra y pérdida de velocidad por serie, en beta | 3 |
| RF-CAM-05 | Fotos de progreso guiadas, a la misma distancia y en la misma pose, con la anterior superpuesta | 3 |
| RF-CAM-06 | Pulso y VFC con el dedo sobre la cámara para el check-in sin reloj | 3 |
| RF-DIS-10 | Planes de cardio por intervalos enviados al app Entreno del Apple Watch con WorkoutKit y a Health Connect como ejercicio planificado | 3 |
| RF-SEG-03 | Modo embarazo y posparto que oculta los ejercicios marcados y pide el visto bueno de un profesional | 3 |
| RF-NUT-01 | Leer de Salud y Health Connect las calorías, la proteína y el agua que registran otras apps, y anotar rápido proteína y agua | 3 |
| RF-ANI-01 | Estado de ánimo antes y después de entrenar; en iPhone se guarda en Salud | 3 |
| RF-FIS-03 | Pruebas de movilidad guiadas con cámara y su evolución, para cualquier atleta | 3 |
| RF-VIV-06 | Mensajes del entrenador durante la sesión por push en la Live Activity | 4 |
| RF-VIV-07 | Entreno agendado visible antes de empezar | 4 |
| RF-PRO-01 | Perfil profesional (creador, entrenador, fisioterapeuta) con verificación de identidad y de cobro, titulación y seguro declarados, e insignia solo con registro oficial comprobado | 4 |
| RF-PRO-02 | Modo profesional en la app y panel web | 4 |
| RF-COA-01 | Invitar clientes por enlace o QR | 4 |
| RF-COA-02 | Permisos de datos por tipo y por profesional, revocables, con registro de accesos | 4 |
| RF-COA-03 | Asignar programas y ajustar el plan del cliente | 4 |
| RF-COA-04 | Revisión de técnica en video con dibujo y nota de voz | 4 |
| RF-COA-05 | Check-in semanal configurable (peso, fotos, preguntas) | 4 |
| RF-COA-06 | Mensajes entre profesional y cliente | 4 |
| RF-COA-07 | Alertas al profesional: sesiones saltadas, dolor, preparación baja | 4 |
| RF-COA-08 | Sesión en vivo: el entrenador ve serie, pulso y video del cliente | 4 |
| RF-PAG-01 | Cobro del coaching 1:1 fuera de la tienda donde las reglas lo permitan, y pagos mensuales a profesionales | 4 |
| RF-PAG-02 | Cada profesional pone su precio entre los puntos de precio de la tienda y ve en vivo cuánto recibe después de la tienda y de la comisión de 5 % | 4 |
| RF-PAG-03 | Venta por la web con acceso en la app (guía 3.1.3(b)), sin decir dentro de la app que en la web es más barato | 4 |
| RF-MKT-01 | Publicar programas con versiones y semana de muestra | 4 |
| RF-MKT-02 | Comprar programas y membresías con la compra integrada de cada tienda | 4 |
| RF-MKT-03 | El motor adapta el programa comprado a los números de cada persona | 4 |
| RF-MKT-04 | Buscar y filtrar programas y profesionales por objetivo, disciplina, nivel, idioma y precio | 4 |
| RF-MKT-05 | Reseñas solo de compradores verificados | 4 |
| RF-MKT-06 | Moderación con filtro, reportes, bloqueo de usuarios y contacto publicado (guía 1.2), canal de denuncia en la UE y reembolsos | 4 |
| RF-FIS-01 | Verificar el registro profesional (ReTHUS o colegio) | 4 |
| RF-FIS-02 | Planes de rehabilitación con dolor de 0 a 10 por ejercicio, con las herramientas del entrenador | 4 |
| RF-FIS-04 | El fisioterapeuta lleva su consentimiento informado y su historia clínica fuera de IronProgress; la app exporta en PDF lo que el paciente comparte | 4 |
| RF-FIS-05 | Alta a entrenamiento normal con los ejercicios a evitar, que el motor respeta | 4 |

| ID | Requisito no funcional | Cómo se verifica |
| --- | --- | --- |
| RNF-11 Privacidad de la plataforma | El profesional solo ve lo que el permiso permite; todo acceso queda registrado; evaluación de impacto antes de la Etapa 4 | Pruebas de Row Level Security con cliente, profesional y un tercero |
| RNF-12 Cámara en el dispositivo | El video se analiza en el teléfono y solo sube si la persona lo comparte | Revisión de tráfico de red durante un análisis |
| RNF-13 Precisión declarada | Cada medición de cámara o sensor muestra su método y precisión | Revisión de textos antes de cada envío a las tiendas |
| RNF-14 Empresa y cobros | Registro tributario y contador antes de la primera venta; empresa en EE. UU. con cuenta de desarrollador de empresa antes de abrir a fisioterapeutas y nutricionistas; Formulario 5472 cada año si es una LLC de dueño extranjero | Documentos tributarios, de la empresa y de Apple y Google |
| RNF-15 Batería en vivo | La Live Activity y los relojes solo se actualizan cuando cambia algo (el contador corre solo) | Consumo medido en una sesión de 60 min |
| RNF-16 Licencias limpias | Cada ejercicio, imagen y video del catálogo guarda su fuente y su licencia; nada sin licencia comercial clara | Revisión del archivo de fuentes antes de cada versión |
| RNF-17 Tiempo exacto | La pantalla, la isla, la barra de estado y los relojes muestran el mismo tiempo, y el descanso avisa a su hora aunque la app esté cerrada | Prueba de 20 descansos con la app cerrada y el teléfono bloqueado |
| RNF-18 Movimiento fluido | 5 ms o menos de tirones por segundo de animación en iPhone y sin cuadros perdidos visibles en Android en los 10 flujos clave; todo interrumpible y con versión para Reducir movimiento | Métricas de animación de XCTest, JankStats y Macrobenchmark, y video antes y después |
| RNF-19 Protección de menores | En modo joven no hay fotos de progreso, mensajes, tienda ni peso visible por defecto, y el permiso de los padres queda guardado | Pruebas con cuentas de 13, 15 y 17 años en cada país |

## 14. Plan por etapas

**La comisión llega en la Etapa 4, sobre una app pulida que ya funciona sola** (F0 en curso; semanas a medio tiempo, estimación gruesa):

| Etapa | Versión | Semanas | Qué trae | Lista cuando |
| --- | --- | --- | --- | --- |
| 1 · App personal con salud y relojes | v1 | 43 a 63 | La v2 completa, disciplinas, relojes y adaptación: fuerza, funcional, yoga, respiración e intervalos con voz a tu medida; se adapta a tu sueño, pulso y VFC (menos peso, series o cardio); Apple Watch y Galaxy Watch con la rutina y un cronómetro animado | 2 semanas de entrenos con los 2 relojes |
| 2 · Pulido de movimiento | v1.1 | 4 a 6 más | Solo animaciones, sin funciones nuevas: transiciones que siguen al dedo, resortes y vibración sincronizada; descanso, isla y relojes a 120 Hz; catálogo de movimiento en Forja. Se publica como actualización propia | 5 ms de tirones o menos por segundo en iPhone |
| 3 · Salud avanzada y cámara | v1.5 | 15 a 23 más | Sensores de Samsung, VO2 máx y la cámara: temperatura, respiración, oxígeno y recuperación del pulso; repeticiones, técnica y fotos guiadas; barra en beta; nutrición leída de Salud, ánimo, embarazo y plan Pro | 95 % de reps contadas en 3 ejercicios básicos |
| Antes de la tienda | | | Empresa en EE. UU., términos y cómo pagar a los profesionales | |
| 4 · Plataforma de profesionales | v2 | 28 a 41 más | Aquí entra la comisión de 5 % por venta: entrenadores con sus clientes, panel web, check-ins y video; tienda donde cada profesional pone su precio, con venta en la app y en la web; fisioterapeutas y nutricionistas con las mismas herramientas | 3 entrenadores reales y 10 ventas pagadas |

Hasta la Etapa 4 son unas 90 a 133 semanas a medio tiempo: la Etapa 1 creció porque ahora trae la salud, los dos relojes y todas las disciplinas. Para tener opinión de usuarios antes, propongo una beta abierta con gente del gimnasio desde la mitad de la Etapa 1. La plataforma no depende de la salud avanzada: si hace falta plata antes, la Etapa 4 puede ir antes que la 3.

Qué cambia en las fases de la v2:

- **F1:** suma el modelo de bloques y disciplinas, el motor de tiempo con el descanso a pantalla completa, los intervalos a tu medida con voz, la Live Activity y la Live Update, y el PAR-Q+ con el modo joven en el onboarding; las rutinas guardan quién las creó.
- **F2:** el motor también sube por escaleras de ejercicios (calistenia, yoga) y guarda el resultado de cada formato.
- **F4:** se queda en la Etapa 1 y crece: lectura y escritura de Salud y Health Connect, adaptación inteligente con molestias y las apps de Apple Watch y Galaxy Watch con la rutina y el cronómetro.
- **F5:** suma el catálogo de más de 1.000 ejercicios con su exigencia, sus músculos con porcentaje, su fuente y su licencia.
- **F6:** las fichas de las tiendas anuncian la Isla Dinámica, los relojes, las disciplinas y la adaptación; el nombre definitivo se decide al final, justo antes de publicar.
- **F3:** sin cambios.

## 15. Decisiones: las que tomaste y las que faltan

Con tu último mensaje quedaron 12 decididas y 8 abiertas; ninguna de las abiertas frena la F1.

| # | Decisión | Estado | Respuesta o recomendación | Cuándo |
| --- | --- | --- | --- | --- |
| 1 | Orden de las etapas | Decidida | 1 app personal con salud y relojes, 2 pulido de movimiento, 3 salud avanzada y cámara, 4 plataforma (sección 14) | 25 oct |
| 2 | Cronómetro | Decidida | Pantalla completa en la app; al salir o apagar la pantalla, Isla Dinámica, pantalla bloqueada, relojes y barra de estado (sección 3) | 24 oct |
| 3 | Relojes | Decidida | Apple Watch Series 12 y Galaxy Watch con la rutina, un cronómetro animado y el pulso desde la Etapa 1 | 25 oct |
| 4 | Salud desde el inicio | Decidida | Leer y escribir Salud y Health Connect en la Etapa 1 para adaptar el entreno antes y durante la sesión (sección 2) | 25 oct |
| 5 | Disciplinas e intervalos | Decidida | Todas, con intervalos a tu medida y voz, y más de 1.000 ejercicios con su exigencia (sección 6) | 25 oct |
| 6 | Pulido de movimiento | Decidida | Etapa 2 propia, publicada como actualización (sección 11) | 25 oct |
| 7 | Modelo de cobro | Decidida | Cada profesional pone su precio; la tienda cobra lo suyo y IronProgress 5 % (sección 9) | 25 oct |
| 8 | Empresa | Decidida | En EE. UU., más adelante; mientras tanto, como personas (sección 10) | 25 oct |
| 9 | Fisioterapeutas | Decidida | Igual que un entrenador: llegan por su cuenta y responden por su servicio; entran cuando exista la empresa | 24 oct |
| 10 | Menores | Decidida | Lo más jóvenes posible, con funciones limitadas (sección 10) | 25 oct |
| 11 | Nombre | Decidida | Al final, justo antes de publicar la Etapa 1; el identificador del paquete (com.olarte.ironprogress) no cambia aunque cambie el nombre | 25 oct |
| 12 | Edad exacta | Decidida | 13 años, con modo joven hasta los 17; por debajo no entra nadie por ahora | 25 oct |
| 13 | Tipo de empresa y estado | Abierta | LLC o C corp en Delaware (Stripe Atlas, USD 500), con un contador en EE. UU. y otro donde vivas | Antes de cobrar |
| 14 | Residencia fiscal al cobrar | Abierta | Colombia o España cambian cómo tributa lo que gane la empresa y dónde se considera dirigida | Antes de cobrar |
| 15 | Revisión de la comisión | Abierta | Arrancar con 5 % como lanzamiento y revisarla al año con los números (sección 9) | Al año de abrir la tienda |
| 16 | Beta abierta | Abierta | Sí, con gente del gimnasio en TestFlight y en la prueba cerrada de Play, desde la mitad de la Etapa 1 | Mitad de la Etapa 1 |
| 17 | Panel web profesional: Compose Multiplatform para web o una web aparte | Abierta | Exploración de una semana al empezar la Etapa 4 y decidir con código en la mano | Etapa 4 |
| 18 | Velocidad de la barra | Abierta | Beta solo en iPhone con LiDAR y sin tocar el motor hasta validarla con un transductor | Etapa 3 |
| 19 | Ciclo menstrual como dato de contexto (lo dan HealthKit y Health Connect) | Abierta | Opcional y solo para observar tendencias, sin cambiar cargas solo por eso | Etapa 3 |
| 20 | Adelantar la plataforma | Abierta | Se puede: la Etapa 4 no depende de la 3 | Al cerrar la Etapa 2 |

## 16. Fuentes

Páginas consultadas entre el 23 y el 25 de octubre de 2026. Base del proyecto: la especificación v2, Forja y las pantallas, que viven en Claude; lo esencial de las tres está en `HANDOFF.md` y en `docs/PANTALLAS.md`.

Apple:

- [Live Activities essentials, WWDC26](https://developer.apple.com/videos/play/wwdc2026/223/)
- [Enhancing your app experience with the Camera Control](https://developer.apple.com/tutorials/data/documentation/avfoundation/enhancing-your-app-experience-with-the-camera-control.md)
- [Apple debuts iPhone 18 Pro and iPhone 18 Pro Max](https://www.apple.com/newsroom/2026/09/apple-debuts-iphone-18-pro-and-iphone-18-pro-max/)
- [iPhone 18 Pro review, AppleInsider](https://appleinsider.com/articles/26/10/01/iphone-18-pro-review-a-massive-camera-update-is-just-the-beginning)
- [Apple Watch Series 12 y Ultra 4, Apple Newsroom](https://www.apple.com/newsroom/2026/09/get-ready-to-experience-iphone-18-pro-the-new-apple-watch-lineup-and-airpods-5/)
- [Apple Watch Series 12, iOS 27 y RMSSD en HealthKit, Momentum](https://www.themomentum.ai/blog/apple-watch-series-12-ios-27-rmssd-healthkit)
- [HKQuantityTypeIdentifier](https://developer.apple.com/documentation/healthkit/hkquantitytypeidentifier)
- [Tipos de entreno de HealthKit (HKWorkoutActivityType), revisados en el paquete @kingstinct/react-native-healthkit 16.2.0](https://www.npmjs.com/package/@kingstinct/react-native-healthkit)
- [Track workouts with HealthKit on iOS and iPadOS, WWDC25](https://developer.apple.com/videos/play/wwdc2025/322/)
- [WorkoutKit](https://developer.apple.com/tutorials/data/documentation/workoutkit.md)
- [Pulso con AirPods Pro 3 durante entrenos](https://support.apple.com/guide/airpods/dev1b40fb47d)
- [iOS 26.4 adds more Sleep and Vitals data, 9to5Mac](https://9to5mac.com/2026/02/17/ios-26-4-adds-more-sleep-and-vitals-data-to-apple-health/)
- [Enabling the double-tap gesture on Apple Watch](https://developer.apple.com/documentation/watchos-apps/enabling-double-tap)
- [ARBodyTrackingConfiguration](https://developer.apple.com/documentation/arkit/arbodytrackingconfiguration)
- [3D body pose en Vision, WWDC23 Notes](https://wwdcnotes.com/notes/wwdc23/111241)
- [Foundation Models framework, Cult of Mac](https://www.cultofmac.com/news/apple-foundation-models-framework)
- [Eliminate animation hitches with XCTest, WWDC20 Notes](https://wwdcnotes.com/notes/wwdc20/10077)
- [Declared Age Range, PPC Land](https://ppc.land/declared-age-range/)
- [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [Cambios para apps en la Unión Europea, agosto de 2026](https://apple.com/newsroom/2026/08/apple-announces-changes-for-apps-in-the-european-union)
- [Advanced Commerce API](https://developer.apple.com/in-app-purchase/advanced-commerce-api) y [lo que hay que saber, RevenueCat](https://www.revenuecat.com/blog/growth/what-you-need-to-know-apples-advanced-commerce-api)
- [Creating SKUs for your Apple In-App Purchases](https://developer.apple.com/documentation/advancedcommerceapi/creating-your-purchases.md) y [Specifying prices for Advanced Commerce SKUs](https://developer.apple.com/tutorials/data/documentation/advancedcommerceapi/prices.md)
- [Overview of receiving payments, App Store Connect](https://www.developer.apple.com/help/app-store-connect/getting-paid/overview-of-receiving-payments)
- [EU Digital Services Act compliance information, App Store Connect](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-compliance-information)
- [Individual to Company or Transfer Apps?, Apple Developer Forums](https://developer.apple.com/forums/thread/756190)

Samsung, Android y Google:

- [Samsung Health Sensor SDK](https://developer.samsung.com/health/sensor/overview.html) y [sus preguntas frecuentes](https://developer.samsung.com/health/sensor/faq.html)
- [Samsung Health Data SDK, guía de migración](https://developer.samsung.com/health/data/migration-guide/overview.html)
- [Energy Score en Galaxy Watch, Tom's Guide](https://www.tomsguide.com/wellness/smartwatches/samsung-energy-score-heres-all-the-galaxy-watches-getting-this-ai-feature)
- [One UI 8 y Live Updates en la Now Bar, Android Authority](https://www.androidauthority.com/one-ui-8-live-updates-support-3573794/)
- [Plantilla Metric Style de Android 17, Android Authority](https://androidauthority.com/android-17-live-updates-metric-style-template-3669117)
- [Now Bar en One UI 9, SamMobile](https://www.sammobile.com/news/now-bar-one-ui-9-support-three-more-types-third-party-apps/)
- [Depth API de ARCore](https://developers.google.com/ar/develop/unity-arf/depth/developer-guide)
- [ML Kit Pose Detection](https://developers.google.cn/ml-kit/vision/pose-detection?hl=en)
- [Política de Health Connect](https://support.google.com/googleplay/android-developer/answer/12991134?hl=en)
- [Atención plena en Health Connect](https://developer.android.com/health-and-fitness/health-connect/features/mindfulness?hl=id)
- [Tipos de ejercicio de Health Connect (ExerciseSessionRecord), revisados en el código de connect-client 1.2.0-alpha06](https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/ExerciseSessionRecord)
- [Política de pagos de Google Play](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en)
- [API de Google Play para suscripciones y precios](https://developers.google.cn/android-publisher/api-ref/rest/v3/monetization?hl=en)

Evidencia de cámara:

- [Renner, Mitter y Baca 2024, PLOS ONE: validez de apps de velocidad de barra](https://ucrisportal.univie.ac.at/en/publications/concurrent-validity-of-novel-smartphone-based-apps-monitoring-bar/)
- [Trowell y otros 2024, PeerJ: validación de Metric VBT](https://peerj.com/articles/17789)
- [Validación de VFC con la cámara del teléfono, HRV4Training](https://www.hrv4training.com/blog2/validation-paper-accepted-for-publication-in-the-international-journal-of-sports-physiology-and-performance-ppg-vs-polar-h7-vs-electrocardiogram)

Disciplinas, catálogo y seguridad:

- [free-exercise-db, APIs.io](https://apis.io/providers/free-exercise-db/)
- [wger, licencias](https://cdn.jsdelivr.net/gh/wger-project/wger@master/README.md)
- [AscendAPI, antes ExerciseDB](https://exercisedb.dev/)
- [Compendio de actividades físicas para adultos 2024, Journal of Sport and Health Science](https://doaj.org/article/91ffa80dcdef401c9db9fc7db2a08e42)
- [Afiliación y marca CrossFit](https://www.crossfit.com/faq/affiliates)
- [PAR-Q+ y ePARmed-X+, Warburton y otros, 2011](https://hfjc.library.ubc.ca/index.php/HFJC/article/view/103)
- [Hiperventilar antes de una apnea, Chan, Singapore General Hospital](https://www.activesgcircle.gov.sg/read/breath-holding-with-hyperventilation-an-invisible-danger)

Legal y mercado:

- [Resolución 1644 de 2026, Centro de Estudios Regulatorios](https://www.cerlatam.com/normatividad/minsalud-resolucion-1644-de-2026-31-jul-26/) y [lectura para fisioterapeutas, Physitrack](https://www.physitrack.com/nl/insights/resolucion-1644-2026-telemedicina-fisioterapeutas-colombia)
- [Consulta del ReTHUS, RCN Radio](https://newsroom.rcnradio.com/colombia/re-thus-asi-puedes-verificar-si-un-medico-o-profesional-de-salud-esta-autorizado-para-ejercer-en-colombia)
- [Ley 2210 de 2022, entrenador deportivo, DMS Jurídica](https://dmsjuridica.com/buscador_20179478954/legislacion/leyes/2022/11/01/ley-2210-de-2022/?print=pdf)
- [Profesiones del deporte en España, FitGeneration](https://fitgeneration.es/se-puede-ser-entrenador-personal-sin-titulacion/)
- [MDCG 2019-11 explicado, OpenRegulatory](https://openregulatory.com/mdcg/mdcg-2019-11)
- [Ley de Servicios Digitales, Clifford Chance](https://www.cliffordchance.com/insights/resources/blogs/talking-tech/en/articles/2022/12/taking-a-closer-look-at-the-digital-services-act.html)
- [DAC7 para operadores de plataformas en España, RSM](https://www.rsm.global/spain/es/insights/nuevas-obligaciones-fiscales-para-operadores-de-market-place)
- [Stripe en Colombia 2026, BTO Digital](https://btodigital.com/stripe-en-colombia/)
- [Stripe Atlas, Rho](https://www.rho.co/blog/stripe-atlas-review)
- [Pagos de Stripe Connect entre países](https://docs.stripe.com/connect/cross-border-payouts-to-recipients) y [Global Payouts a Colombia, marzo de 2026](https://docs.stripe.com/changelog/dahlia/2026-03-25/cross-border-payouts-new-countries)
- [Formulario 5472 para LLC de dueño extranjero, SDO CPA](https://www.sdocpa.com/form-5472-foreign-owned-llc-guide/)
- [Representante del RGPD para empresas de fuera de la UE, Captain Compliance](https://captaincompliance.com/education/gdpr-article-27-eu-representative-requirements-enforcement-and-compliance-steps-for-non-eu-companies/)
- [Ventanilla OSS para empresas de fuera de la UE, Hellotax](https://helpdesk.hellotax.com/hc/en-150/articles/49817186344209-Non-Union-OSS-for-Non-EU-Digital-Service-Providers)
- [IVA de servicios digitales en Colombia, Kintsugi](https://trykintsugi.com/es-419/sales-tax-guides/latam/colombia)
- [Sede efectiva de administración, Concepto DIAN 42 de 2024, Accounter](https://accounter.co/normatividad/una-sociedad-extranjera-sede-efectiva-de-administracion-es-considerada-una-sociedad-nacional-por-todo-el-ano-gravable-sus-ingresos-estarian-sometidos-a-impuesto-concepto-42-de-2024)
- [Residencia de sociedades en España, Agencia Tributaria](https://sede.agenciatributaria.gob.es/Sede/ayuda/manuales-videos-folletos/manuales-practicos/manual-sociedades-2024/capitulo-01-cuestiones-generales/quienes-son-contribuyentes-impuesto-sobre-sociedades/residencia-domicilio-fiscal.html)
- [Régimen fiscal de una LLC en España, Garrigues](https://www.garrigues.com/es_ES/noticia/regimen-fiscal-de-una-llc-limited-liability-company)
- [My Health My Data Act, Stoel Rives](https://www.stoel.com/insights/publications/faq-washington-states-my-health-my-data-act)
- [COPPA reformada, Finnegan](https://www.finnegan.com/en/insights/articles/coppas-amended-rule-is-now-in-full-effect-what-operators-need-to-know.html)
- [Datos de menores en España (LOPDGDD), INEAF](https://www.ineaf.es/tribuna/rgpd-tratamiento-de-datos-de-menores/)
- [Datos de menores en Colombia, concepto de la SIC](https://ambitojuridico.com/sites/default/files/BancoMedios/Archivos/cpto-217036-20.pdf)
- [TrainHeroic para coaches](https://www.trainheroic.com/grow/)
