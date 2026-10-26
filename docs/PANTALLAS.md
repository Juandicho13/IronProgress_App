# IronProgress · Guía de pantallas para Gemini

25 de octubre de 2026 · 29 pantallas diseñadas en el lienzo de Claude, descritas para construirlas sin verlas.

> Los Gemini no pueden abrir el lienzo, así que aquí está cada pantalla con su estructura, medidas, estados, textos y toques. Las medidas del lienzo son px y en la app son dp (Android) o pt (iOS): 412 × 915 es el tamaño del Galaxy S25 Ultra y en el iPhone 18 Pro el mismo diseño se adapta al ancho sin cambiar medidas. Los colores, la tipografía, los radios y los resortes son los de Forja (`HANDOFF.md`, sección 12): aquí se nombran por su token, nunca con hex sueltos en el código. Los datos que se ven (Empuje A, 62,5 kg, Pierna B) son de ejemplo.

## Índice

0. Reglas comunes de todas las pantallas
1. El entreno, de principio a fin (Hoy, Entreno, Teclado, Descanso en la barra, Terminar, Resumen)
2. Progreso, plan y ajuste del día (Progreso, Ejercicio, Plan, Ajuste del día)
3. Tema claro (Hoy, Descanso y Progreso en claro)
4. Galaxy Watch4 (descanso y listo)
5. Tiempo, intervalos y adaptación (descanso a pantalla completa, intervalos, adaptación del día y Apple Watch)
6. Ajuste durante el entreno (teléfono y Apple Watch)
7. Disciplinas y ficha de ejercicio (disciplinas, yoga, respiración, AMRAP y ficha)
8. Presupuesto de toques
9. Qué falta por diseñar

---

## 0. Reglas comunes de todas las pantallas

| Pieza | Regla |
| --- | --- |
| Lienzo del teléfono | 412 × 915 dp, fondo `background`, texto `text1`, Archivo 16/24 |
| Barra superior | Empieza a 44 dp del borde de arriba (debajo de la barra de estado), alto 48, márgenes de 12. Botones redondos de 48 con ícono de 24. El título va centrado (17/22, 600) y, si hay sesión, debajo el cronómetro de la sesión (13/16, 600, `text2`, cifras) |
| Título grande | Pantallas raíz (Hoy, Plan, Progreso): fecha o etiqueta en `caption` arriba y `title1` (30/36, 700) a 76 dp del borde, con un botón redondo de 48 en `surface3` a la derecha. Una sola por pantalla |
| Márgenes laterales | 20 dp para el contenido |
| Tarjetas | `surface1`, radio 28 (grandes) o 20 (compactas), relleno 16, separadas 12 |
| Etiqueta de sección | `caption` 12/16, 500, mayúsculas, tracking 0,04 em, color `text2` |
| Cifras | Familia Archivo SemiExpanded (ancho 112,5) con cifras tabulares (`tnum`) en todo número vivo o alineado |
| Botón principal | Alto 56, radio completo, `ember` con texto `onEmber` 17/20, 600. Uno por pantalla |
| Botón secundario | Alto 48, `surface3` o transparente con texto `text1` o `text2` |
| Barra inferior de vidrio | A 36 dp del borde de abajo, márgenes de 20, alto 64, radio completo, fondo `glass`, borde interior de 1 dp `glassBorder` y sombra `0 12 32` al 50 %. Botones de 48 dentro, con fondo `glassSelection` |
| Desenfoque bajo la barra | Capa de 150 dp pegada al borde de abajo con desenfoque de 6 y máscara que se apaga hacia arriba, para que el contenido se lea al pasar por detrás |
| Isla de navegación | Pestañas Hoy, Plan y Progreso en una píldora de vidrio de 64 de alto a 36 del borde, con el círculo de Buscar (64) aparte a la derecha. La pestaña elegida lleva fondo `glassSelection` e ícono relleno en `ember` |
| Accesorio de sesión | Píldora de vidrio de 56 encima de la isla (a 108 del borde) con el entreno en curso: lleva de vuelta al entreno en un toque |
| Íconos | Material Symbols Rounded, peso 400, 24 dp (20 en línea con texto), relleno solo en el elemento elegido. Usados: `local_fire_department`, `calendar_month`, `monitoring`, `search`, `account_circle`, `keyboard_arrow_down`, `chevron_left`, `chevron_right`, `arrow_back`, `arrow_forward`, `north_east`, `more_horiz`, `play_arrow`, `pause`, `skip_next`, `check`, `close`, `add`, `remove`, `timer`, `replay`, `edit`, `info`, `ios_share`, `trophy`, `bedtime`, `ecg_heart`, `monitor_heart`, `watch`, `volume_up`, `volume_off`, `trending_down`, `trending_up`, `fitness_center`, `sports_gymnastics`, `self_improvement`, `accessibility_new`, `spa`, `air`, `directions_run`, `sports_mma`, `touch_double` |
| Colores con significado | `ember` (brasa) es esfuerzo y acción; `ice` (hielo) es descanso, recuperación y calma; `gold` (oro) son récords y avisos de recuperación baja; `danger` solo errores. Un color nunca va solo: siempre con texto o ícono |
| Movimiento | Resortes de Forja (`ForjaTheme.motion`). Con Reducir movimiento, todo pasa a fundidos cortos. Toda animación se puede interrumpir |
| Accesibilidad | Áreas táctiles de 48 o más, `contentDescription` en cada ícono que actúa, contraste AA, texto escalable. Los cronómetros no se leen en voz alta cada segundo: se anuncian los cambios de fase |
| Textos | Tuteo y verbos, números con formato local ("62,5 kg", "8.420 kg"), separador " · ", sin raya, sin signos de exclamación y sin emoji. La regla vale también para las frases de la voz, porque salen escritas en el subtítulo |

Relojes:

| Reloj | Lienzo | Reglas |
| --- | --- | --- |
| Apple Watch Series 12 de 46 mm | 416 × 496 px (208 × 248 pt), esquinas de radio 100 px, fondo negro puro | Márgenes de 40 px; textos de 24 a 40 px; botón principal de 96 px de alto; el cronómetro es un trazo de 12 px que recorre el borde de la pantalla; la Corona ajusta y el Doble toque hace la acción principal |
| Galaxy Watch4 de 44 mm | 450 × 450 px, redondo, fondo negro puro | Anillo de 12 px a 6 px del borde (radio 213); el bisel suma o resta 15 s; botón de 240 × 96 abajo |

---

## 1. El entreno, de principio a fin

### 1.1 Hoy (`Main.dc.html`)

- **Para qué:** ver cómo llegas y empezar el entreno que toca en un toque.
- **Estructura, de arriba abajo (desde 76 dp):**
  1. Cabecera: `caption` con la fecha ("Miércoles 23 de septiembre") y `title1` "Hoy"; a la derecha, botón de 48 en `surface3` con `account_circle` ("Perfil y ajustes").
  2. Tarjeta **Recuperación** en `iceVeil`: ícono `watch` en hielo y etiqueta "Recuperación"; `title3` "Lista para entrenar completo"; una fila con `bedtime` "7 h 42 min de sueño" y `ecg_heart` "52 lpm en reposo" (cifras en `text1`, resto en `text2`, 14/20).
  3. Tarjeta **Esta semana**: `title3` a la izquierda y "2 de 5 · racha de 6 semanas" a la derecha (`text2`). Siete círculos de 36: día hecho en `ember` con `check`; hoy con borde de 2 en `ember` y la letra; día planeado en `surface3` con la letra; día de descanso solo con la letra en `text3`. Cada círculo dice su estado para lectores de pantalla ("Lunes: entrenaste").
  4. Tarjeta **Siguiente**: `caption` "Siguiente · Semana 3 de 6", `title2` "Empuje A", "5 ejercicios · 55 min · Gimnasio"; lista de ejercicios con separador `line` y, a la derecha, lo que cambia hoy: en `ember` con `north_east` cuando sube ("62,5 kg", "+1 rep") y en `text2` cuando se mantiene ("40 kg", "+10 kg", "25 kg"); botón principal **Empezar** con `play_arrow`.
  5. Isla de navegación con Hoy elegida.
- **Toques:** Empezar abre Entreno (1 toque). Tocar la tarjeta de recuperación abre el detalle de la adaptación (sección 5.4).
- **Variante de recuperación baja:** ver 2.4.

### 1.2 Entreno (`Entreno.dc.html`)

- **Para qué:** registrar series. Completar la serie sugerida toma 1 toque.
- **Estructura:**
  1. Barra superior: `keyboard_arrow_down` "Minimizar el entreno" (vuelve a la pestaña de donde vino y deja el accesorio de sesión), nombre de la rutina con el cronómetro de la sesión ("Empuje A", "18:42") y `more_horiz` "Opciones del entreno".
  2. Barra de progreso de ejercicios: un segmento de 4 dp por ejercicio, separados 4; los hechos en `ember`, el actual relleno en parte (por series hechas) y el resto en `surface3`.
  3. Tarjeta del ejercicio actual: `title3` con el nombre, debajo los músculos ("Pecho · Tríceps · Hombros") y un botón de opciones. La sugerencia del motor en una línea: "**+2,5 kg** porque hiciste 3 × 12 con RIR 2" (la cifra en `text1`, el resto en `text2`).
  4. Encabezado de columnas en `caption`: "#", "Anterior", "kg", "Reps". Columnas de la fila de serie: 28 / 1fr / 76 / 60 / 48 dp, huecos de 6 y relleno de 8.
  5. Filas de serie de 56 dp y radio 20. Número de serie (la de calentamiento es una "C" en hielo); "Anterior" en `text3` ("60 × 12"); campos de kg y reps de 44 de alto, radio 14, `surface3`, cifra 22/28 600; botón de completar de 48 redondo (`surface3` con `check` en `text2`; hecha: `ember` con `check` en `onEmber`). La serie actual va en `surface2` con borde de 1 dp `line`. Una serie que bate un récord lleva el `trophy` en oro.
  6. Botón de texto "Añadir serie" en `ember`.
  7. Ejercicios que siguen como filas de 64 en `surface1`, radio 28: número en un círculo de 28, nombre en `title3` y "4 series · 40 kg × 6-10" en `text2`.
  8. Barra inferior: el botón **Completar serie** (cuando no hay descanso corriendo).
- **Estados y animación:**
  - Al completar una serie, la fila destella en `emberVeil` durante 900 ms y se asienta; vibra con `CONFIRM`.
  - Aparece un aviso "Serie 2 guardada" con **Deshacer**, que se va solo a los 5 s.
  - Empieza el descanso: por defecto a pantalla completa (sección 5.1). Si la persona lo minimiza, el botón de la barra se vuelve la barra de descanso en el mismo lugar (1.4).
  - El descanso termina antes si el pulso baja del umbral R + 0,5 × (P - R) y ya pasó la mitad del tiempo; nunca después del tiempo + 60 s (`HANDOFF.md` 11.7).
- **Toques:** tocar el campo de kg o de reps abre el Teclado (1.3); completar 1; otras reps 3 (reps, número, completar).

### 1.3 Teclado numérico (`Teclado.dc.html`)

- **Para qué:** cambiar peso, reps o RIR sin el teclado del sistema.
- **Estructura:** Entreno detrás, con la serie que se edita resaltada (anillo de 2 dp en `ember` en el campo activo). Encima, una hoja de vidrio pegada al borde de abajo con radio 36 arriba:
  1. Asa de 36 × 5 en `text3`.
  2. Selector segmentado de 44 con **Peso**, **Reps** y **RIR** (pista `surface3`, opción elegida en `activeControl` con sombra suave).
  3. Valor grande (40/44, 600) con su unidad ("kg") y, a la derecha, el valor anterior en `text3`.
  4. Atajos como chips de 36 en `surface3` (área de toque de 48): con Peso, "-2,5", "+2,5" y "Sugerido 62,5"; con Reps, "-1", "+1" y "Sugerido 10"; con RIR, los valores "0", "1", "2", "3" y "4+".
  5. Teclas de 60 de alto, radio 20, `surface3`, cifras 26/600: 1 a 9, la coma, 0 y Borrar.
  6. Dos botones: **Siguiente** (pasa al campo siguiente) y **Completar serie** (principal, `ember`).
- **Vibración:** cada tecla `KEYBOARD_TAP`; cambiar de segmento `SEGMENT_TICK`.

### 1.4 Descanso en la barra (`Descanso.dc.html`)

- **Para qué:** ver tus series mientras descansas, cuando minimizas el descanso a pantalla completa.
- **Estructura:**
  1. Barra superior y progreso como en Entreno ("Empuje A", "24:10", ejercicio 2 de 5).
  2. Ejercicio terminado arriba como fila compacta con `check` en `ember`: "Press banca con barra" y "4 de 4 · 62,5 kg × 11, 10, 10, 9".
  3. La pregunta del RIR de la última serie: "¿Cuántas te quedaban en la última?" con un segmentado 0, 1, 2, 3, 4+. Se puede ignorar.
  4. El ejercicio siguiente abierto ("Press militar", "Hombros · Tríceps") con su sugerencia ("**+1 rep** por serie con el mismo peso") y sus filas.
  5. Barra inferior de descanso: "-15", el anillo de 40 en hielo con el tiempo ("1:48") y el pulso ("138"), "+15" y **Saltar** (`skip_next`).
- **Toques:** ajustar el descanso 1; saltar 1. Al terminar el descanso vibra con el patrón 0, 180, 90, 180 ms y la barra vuelve a ser Completar serie.

### 1.5 Terminar (`Terminar.dc.html`)

- **Para qué:** cerrar el último ejercicio y terminar con un gesto que no se hace por error.
- **Estructura:** los ejercicios hechos como filas compactas con `check` en `ember` y su resumen ("4 series · 62,5 kg × 11, 10, 10, 9"); el último ejercicio abierto con sus series completas y la sugerencia ("**+1 rep** por serie con el mismo peso"); abajo **Deslizar para terminar**: pista de 64 en `surface3` con sombra, el texto centrado en `text2`, el pulgar de 56 en `ember` a 4 dp del borde izquierdo y la estela en `emberVeil` detrás del pulgar.
- **Gesto:** al pasar el umbral vibra con `GESTURE_THRESHOLD_ACTIVATE` y abre el Resumen.

### 1.6 Resumen (`Resumen.dc.html`)

- **Para qué:** ver lo que lograste y dejar el feedback que ajusta la semana siguiente.
- **Estructura:**
  1. Botón redondo de compartir (`ios_share`) arriba a la derecha.
  2. `caption` "Empuje A · Miércoles 23 sep" y `title1` "Entreno terminado" (a 104 dp).
  3. Tarjeta de **Récords** en `goldVeil`: etiqueta en oro con `trophy`, la cifra "2" (40/44, 600, cifras proporcionales) y "Press banca 62,5 × 11 · Fondos +12,5 × 8".
  4. Dos métricas en dos columnas: **Duración** "58:12" con "4 min menos que la media" y **Volumen** "8.420 kg" con "+6 % vs. la semana pasada" en `ember` con `north_east`.
  5. Tarjeta "¿Cuánto trabajo sintió cada músculo?" con "Ajusta las series de la próxima semana." y, por músculo trabajado (Pecho, Tríceps, Hombros), un segmentado **Poco / Justo / Mucho**. Debajo, "Molestias" con chips de 36: Ninguna (elegida, en `emberVeil` con texto `ember`), Hombro, Codo y Muñeca.
  6. Botón principal **Guardar**.
- **Toques:** dar el feedback 1 (todo viene en "Justo"); guardar 1.

---

## 2. Progreso, plan y ajuste del día

### 2.1 Progreso (`Progreso.dc.html`)

- **Estructura:** `title1` "Progreso" con botón de historial; segmentado **Ejercicios / Músculos / Cuerpo** (Músculos elegido).
  1. Tarjeta **Recuperación muscular**: "Espalda y bíceps siguen cargados desde ayer"; mapa del cuerpo de frente y de espalda, cada músculo pintado de `heat0` (fresco) a `heat4` (cargado), con la leyenda "Fresco" a "Cargado" en 5 bloques de 22 × 10. El mapa tiene una descripción para lectores de pantalla con el estado de cada grupo.
  2. Tarjeta **Series por músculo**, "Últimos 7 días · 17 al 23 sep": una fila por músculo (columnas 86 / 1fr / 52) con la barra de series en `ember`, la banda del rango objetivo (10 a 20) en `surface3` detrás y el número a la derecha; si está por debajo, la palabra "bajo" en `text3`. Eje 0, 10 y 20. Pie: "Rango objetivo: 10 a 20 series".
- **Reglas:** las series cuentan los últimos 7 días, no la semana de calendario; el primario suma 1 y el secundario 0,5.

### 2.2 Ejercicio, historial (`Ejercicio.dc.html`)

- **Estructura:** barra superior con `arrow_back`, el nombre ("Press banca con barra") y opciones.
  1. "1RM estimado" en `caption`, "85 kg" (40/44) y "+2 kg vs. hace 4 semanas" en `ember` con `north_east`.
  2. Tarjeta con segmentado **3 m / 6 m / 1 a / Todo** y la gráfica de 340 × 170: línea de 2 dp en `ember` con área al 10 %, rejilla en `line`, ejes en `text3` (80 y 85; jul, ago, sep) y el punto del récord en oro con la etiqueta "Récord 85".
  3. Tarjeta **Récords** con `trophy` en oro: "1RM estimado · Hoy · 85 kg", "Mayor peso · 26 ago · 65 kg × 6", "Más reps con 60 kg · 9 sep · 12".
  4. Tarjeta **Últimas sesiones**: fecha a la izquierda y las series a la derecha ("62,5 × 11, 10, 10, 9").
- **Nota:** la ficha con músculos y exigencia es otra pantalla (7.5); desde la ficha se llega aquí con "Historial".

### 2.3 Plan con un entreno en curso (`Plan.dc.html`)

- **Estructura:** `title1` "Plan" con botón `add` "Crear rutina".
  1. Tarjeta del **programa**: "Programa", `title2` "Hipertrofia 6 semanas", "Semana 3 de 6 · descarga en la semana 6"; barra de 6 segmentos de 6 dp (hechas en `ember`, la actual a medias, la de descarga en hielo) y la semana en 7 círculos como en Hoy.
  2. Tarjeta **Rutinas** ("Gimnasio"): Pierna A, Tirón A, Empuje A, Pierna B y Tirón B, cada una con "6 ejercicios · 60 min" y su día a la derecha; la rutina en curso lleva el chip **En curso** en `emberVeil`.
  3. Fila **Biblioteca de programas**: "PPL, torso y pierna, cuerpo completo, 5 × 5".
  4. Accesorio de sesión sobre la isla: "Press banca · Serie 3 de 4" y "Descanso 1:12 · 128" en hielo, con un botón de 44 para saltar el descanso. Tocarlo vuelve al entreno.
- **Toques:** volver al entreno desde otra pestaña 1; tocar la pestaña actual sube la lista hasta arriba.

### 2.4 Ajuste del día (`AjusteDia.dc.html`)

- **Para qué:** proponer la versión ligera cuando llegas con poca recuperación, sin quitarte la decisión.
- **Estructura:** Hoy, con la tarjeta de recuperación cambiada a **Recuperación baja** ("Hoy te conviene la versión ligera"): "Dormiste 5 h 10 min (tu media es 7 h 20) y tu pulso en reposo está 7 lpm arriba. Una serie menos por ejercicio y 7,5 % menos de carga." con los botones **Aplicar** (48, `surface3`) y **Entrenar normal** (texto). Encima, la hoja **Hoy, versión ligera**:
  1. "Tu reloj y tus últimos entrenos muestran menos recuperación de lo normal."
  2. Las razones: "Dormiste 5 h 10 min · tu media es 7 h 20", "Pulso en reposo de 61 lpm · 7 más que tu media", "Tercer día seguido de entreno".
  3. Caja **Qué cambia**: "Una serie menos por ejercicio · 7,5 % menos de carga · mismos ejercicios".
  4. **Aplicar** (principal), **Entrenar normal** y "Esto no es consejo médico."
- **Toques:** aplicar el ajuste 1. Para ver o elegir cada cambio, la pantalla de detalle (5.4).

---

## 3. Tema claro (`HoyClaro.dc.html`, `DescansoClaro.dc.html`, `ProgresoClaro.dc.html`)

Las mismas tres pantallas en claro, con los tokens de la columna clara de Forja: fondo `background` claro (#f3f4f6), tarjetas `surface1` blancas con sombra suave, `surface3` (#e8eaee) en campos y pistas, y textos de acento con `emberInk` y `iceInk`, que se oscurecen para pasar 4,5:1. El vidrio de la isla y de la barra pasa a blanco al 84 %. Nada cambia de lugar ni de tamaño.

---

## 4. Galaxy Watch4

### 4.1 Reloj descanso (`RelojDescanso.dc.html`)

450 × 450 redondo, fondo negro. Anillo de radio 213 y 12 de grosor: pista en `surface3` y progreso en hielo que se vacía. En el centro: "Descanso" (28, 600, `text2`), el tiempo "1:12" (96/100, 700, cifras), el pulso "128" con corazón en hielo (36) y "Siguiente · serie 3 de 4" (26, `text2`). Abajo, botón **+15 s** de 240 × 96 en `surface3`.

### 4.2 Reloj listo (`RelojListo.dc.html`)

"Listo" en `ember` (28), la serie que sigue "62,5 × 10" (56, 700), "kg · serie 3 de 4" (26, `text2`) y el pulso "100" en hielo (36). Abajo, **Completar** de 240 × 96 en `ember`. Vibra al estar listo; completar la serie desde el reloj es 1 toque; girar el bisel suma o resta 15 s.

---

## 5. Tiempo, intervalos y adaptación

### 5.1 Descanso a pantalla completa (`DescansoCompleto.dc.html`)

- **Para qué:** el descanso por defecto después de completar una serie: grande, claro y con lo que sigue.
- **Estructura:**
  1. Barra superior como en Entreno (minimizar, "Empuje A" con "24:10", opciones) y la barra de progreso de ejercicios.
  2. A 128 dp: "Descanso · Press banca con barra" en `caption`.
  3. Anillo de 300 × 300 centrado (a 156 dp): radio 138, grosor 12, pista `surface3`, progreso en hielo que se vacía desde arriba en sentido horario. Dentro: el tiempo "1:12" en cifra héroe (72/76, 700), "de 2:00" (15/20, `text2`) y el pulso con corazón en hielo ("128", 22/28).
  4. Debajo del anillo: "Si tu pulso baja de 112 antes del final, te aviso para seguir." (14/20, `text2`, centrado).
  5. Tarjeta **Siguiente** (a 528 dp): "Siguiente" y "Serie 3 de 4", el nombre del ejercicio y dos cajas en `surface2` (radio 20) con "62,5 kg" y "10 reps" (40/44); debajo, `north_east` en `ember` y "Mismo peso; si llegas a 10, la próxima **+2,5 kg**".
  6. "Desliza hacia abajo para volver a tus series" en `caption` `text3` con `keyboard_arrow_down`.
  7. Barra inferior de vidrio: **-15**, **+15** y **Saltar descanso**.
- **Estado listo:** al llegar a 0 (o antes por pulso) el anillo pasa a `ember` lleno, el centro dice "Listo" en `ember` con "62,5 × 10" y "kg · serie 3 de 4", y la barra muestra **Completar serie 3** (principal).
- **Fuera de la app:** al salir o apagar la pantalla, el mismo descanso sigue en la Live Activity (iPhone), la Live Update (Android) y los relojes, todos leyendo el mismo motor de tiempo (RF-VIV-08, RNF-17).
- **Movimiento:** el anillo avanza con una transición de 400 ms; el cambio de hielo a brasa es un fundido de 300 ms; deslizar hacia abajo encoge la pantalla hasta la barra de descanso (1.4) siguiendo al dedo.

### 5.2 Intervalos a tu medida (`IntervalosCrear.dc.html`)

- **Para qué:** armar intervalos con los tiempos que quieras, como "15 s a tope y 45 s de descanso".
- **Estructura:**
  1. Barra superior: **Cancelar** (texto), "Nuevo intervalo" y **Guardar** (texto en `ember`).
  2. Nombre grande "Ráfagas de 15" (28/34, 700) con un botón `edit` para cambiarlo.
  3. Chips de base, con desplazamiento horizontal (40 de alto): **A tu medida**, **Tabata 20/10**, **15/45**, **30/30** y **40/20**. El chip elegido va en `emberVeil` con borde y texto `ember`. Si los valores no coinciden con ninguna base, se elige solo "A tu medida".
  4. Tarjeta **Cada ronda** con el total de una ronda a la derecha ("1:00") y una barra de proporción (10 dp, dos tramos con 3 dp de hueco: trabajo en `ember` y descanso en hielo, cada uno tan ancho como su tiempo). Tres filas de 60 dp:
     - **Trabajo** · "Máximas reps" · paso a paso de -/+ (botones de 48 en `surface3`) con "0:15" en medio (22/28, cifras). De 5 s en 5 s, entre 5 s y 5 min.
     - **Descanso** · "Quieto o caminando" · "0:45". De 5 s en 5 s, desde 0.
     - **Rondas** (ícono `replay`) · "9:15 en total" · "10". De 1 en 1, entre 1 y 50.
     Los botones de menos se atenúan al 40 % en el mínimo.
  5. Fila de **ejercicios** (64, radio 20): `fitness_center`, "Burpees · Kettlebell swing", "Se alternan en cada ronda" y `chevron_right`.
  6. Sección **Voz** con el botón de texto **Probar voz** (`volume_up`, `ember`). Tarjeta con tres interruptores (filas de 56; toda la fila es el interruptor):
     - **Cuenta atrás** · "Los últimos 5 s de cada fase".
     - **Dice qué sigue** · "«Descansa», «Burpees en 3, 2, 1»".
     - **Baja tu música** · "Solo mientras la voz habla".
  7. Botón principal **Empezar · 9:15** con `play_arrow`.
- **Cálculo:** total = rondas × trabajo + (rondas - 1) × descanso (el último descanso no cuenta).
- **Probar voz:** muestra 2,6 s una píldora de vidrio con lo que se oye: "«Burpees en 3, 2, 1… ya»"; sin "Dice qué sigue", "«Cinco, cuatro, tres, dos, uno»"; sin voz, "Solo sonará un pitido al cambiar de fase".

### 5.3 Intervalos con voz (`IntervalosVoz.dc.html`)

- **Para qué:** correr los intervalos sin mirar la pantalla: la voz cuenta, el color dice la fase y los números se ven de lejos.
- **Estructura:**
  1. Barra superior: minimizar, "Ráfagas de 15" con "4:10 de 9:15" (tiempo de la sesión) y el botón de la voz (`volume_up` o `volume_off`, "Silenciar la voz").
  2. Segmentos de rondas (10, de 4 dp): hechas en `ember`, la actual en `text1`, el resto en `surface3`; debajo "Ronda 5 de 10".
  3. Fase en mayúsculas con tracking 0,14 em, en el color de la fase: "TRABAJO · MÁXIMAS REPS" en `ember`, "DESCANSO" en hielo; debajo el ejercicio (28/34, 700): "Burpees" o, en descanso, "Respira".
  4. Anillo de 300 (radio 140, grosor 14) en `ember` (trabajo) o hielo (descanso), con transición lineal de 950 ms por segundo. Detrás, un brillo radial de 600 dp del color de la fase que cambia con un fundido de 700 ms.
  5. En el centro: el tiempo "0:15 de 0:15" (72/76) y, **en los últimos 5 s del trabajo, el número solo en 156 dp, peso 800**, con un latido (empieza en escala 1,18 y opacidad 0,55 y se asienta en 420 ms) en cada segundo.
  6. Subtítulo de la voz: píldora de vidrio de 40 con `volume_up` y la frase que se está diciendo, centrada debajo del anillo. Solo se muestra mientras hay frase.
  7. Debajo, según la fase:
     - **Trabajo:** dos cajas: "La última vez · 11 · burpees · supéralos" y "Pulso · 168 · Zona 5 · máxima".
     - **Descanso:** tarjeta "¿Cuántos burpees hiciste?" con cinco chips de 48 (de 2 menos a 2 más que la última vez; el de la última vez con borde `ember`, el elegido relleno en `ember`) y "Si no eliges, guardo 11, como la última vez." Debajo, "Siguiente: **Kettlebell swing · 16 kg**".
     - **Fin:** anillo en oro lleno, "Listo" en oro con "10 rondas · 9:15" y dos cajas con el total de cada ejercicio ("Burpees · 57 · en 5 rondas").
  8. Barra inferior: pausa (`pause` o `play_arrow`), **Saltar al descanso** o **Saltar descanso** (`skip_next`) y `more_horiz`. Al terminar, **Ver resumen**.
- **Frases de la voz (las mismas en el subtítulo):**

| Momento | Frase |
| --- | --- |
| Empieza el trabajo | «Ya. Todas las que puedas» |
| Mitad del trabajo | «Mitad. No pares» |
| Últimos 5 s de trabajo | «Cinco», «Cuatro», «Tres», «Dos», «Uno» |
| Empieza el descanso | «Descansa» |
| Quedan 30 s de descanso | «Quedan treinta segundos» |
| Quedan 5 s de descanso | «Siguiente: kettlebell swing» |
| Últimos 3 s de descanso | «Tres», «Dos», «Uno» |
| Fin | «Terminaste. Buen trabajo» |
| En pausa | "En pausa" (solo texto) |

- **Audio:** la voz baja la música mientras habla y la devuelve al terminar (iPhone: `AVAudioSession` con `duckOthers`; Android: foco de audio con atenuación). Voz grabada para las cuentas y las frases fijas; la del sistema para los nombres de ejercicios. Con el teléfono bloqueado sigue en la Live Activity y en los relojes.

### 5.4 Adaptación del día: detalle (`AdaptacionDia.dc.html`)

- **Para qué:** ver por qué la app propone una versión ligera y elegir cambio por cambio.
- **Estructura:**
  1. Barra superior: `chevron_left` (vuelve a Hoy), "Ajuste de hoy" e `info` ("Cómo se decide").
  2. Píldora **Recuperación baja** en `surface2` con `trending_down` y texto en oro.
  3. Título "Pierna B, versión ligera" (28/34, 700; si se quitan todos los cambios, "Pierna B, completa") y "Hoy tu cuerpo viene con menos recuperación de lo normal. Tú decides qué cambiar."
  4. **Lo que ve tu reloj** con la fuente a la derecha ("Apple Watch · 7:02") y cuatro cajas de 148 dp con desplazamiento horizontal: Sueño "5 h 40" (tu media: 7 h 20), VFC "38 ms" (22 % bajo tu media), Pulso en reposo "60 lpm" (6 más que tu media), Ayer "Empuje A" (exigencia 8 de 10).
  5. **Qué cambia** con "Orden: peso, series, cardio y al final ejercicios." y una tarjeta con cuatro interruptores de fila completa:
     - **Menos peso** · "-7,5 %: sentadilla 77,5 kg · rumano 65 kg" (encendido).
     - **Una serie menos** · "Sentadilla, rumano y prensa: de 4 a 3" (encendido).
     - **Sin cardio al final** · "Quitas los 12 min de bici en zona 2" (encendido).
     - **Cambiar las zancadas** · "Por prensa a una pierna, cuida la rodilla" (apagado).
  6. Caja con **Duración** "39 min" (antes 60) y **Exigencia** "5 de 10" (antes 8), que se recalculan al tocar cada interruptor.
  7. "Esto no es consejo médico. Si algo duele, para."
  8. Botón principal con el número de cambios ("Usar estos 3 cambios", "Usar este cambio" o, sin cambios, "Entrenar normal") y debajo **Mantener el plan**.
- **Reglas:** el peso se redondea a los discos del perfil (85 kg × 0,925 = 78,6 → 77,5). El cambio de ejercicio busca uno de los mismos músculos con menos impacto en la articulación (`ExerciseDemand`). Cada decisión se guarda en `AdaptationLog` con `momento = antes`.

### 5.5 Apple Watch · hoy (`AppleWatchHoy.dc.html`)

416 × 496, fondo negro. Cabecera a 30 px: "Hoy" en `ember` y la hora "7:45". Título "Pierna B" (44/50, 700) y, con `trending_down` en oro, "Versión ligera · 39 min" (24). Lista de los primeros ejercicios con su peso a la derecha (24/36): "Sentadilla 77,5 kg", "Peso muerto rumano 65 kg", "Prensa 140 kg" y "y 2 más" en `text3`. Botón **Empezar** de 96 px en `ember` con `play_arrow`. La rutina funciona sin el iPhone cerca.

### 5.6 Apple Watch · serie (`AppleWatchSerie.dc.html`)

Cabecera "Serie 3 de 4" en `ember` y la hora. "Sentadilla" (40/46, 700) y, con corazón en `ember`, "142 · 12:40" (pulso y tiempo de sesión). Dos filas de 76 px, radio 28, `surface1`: "77,5 kg" y "8 reps" (56/76, 700). La fila que ajusta la Corona lleva un borde de 3 px en `ember`; tocar la otra fila le pasa la Corona. Debajo, "Gira la corona: reps" (22, `text3`). Botón **Completar** de 96 px con `touch_double`: el Doble toque también completa.

### 5.7 Apple Watch · descanso (`AppleWatchDescanso.dc.html`)

El cronómetro es el borde de la pantalla: un trazo de 12 px que sigue el rectángulo redondeado (inset de 6, radio 94; largo total 1.614,6 px), en hielo sobre una pista `surface3`, que empieza arriba al centro y se vacía en sentido horario. Cabecera "Descanso" en hielo y la hora. Tiempo "1:12" (120/124, 700), pulso "128" en hielo (36) y, debajo, "Siguiente" y "Serie 3 · 77,5 × 8". Dos botones de 84 px: **+15 s** y **Saltar**. La Corona suma o resta 15 s.

---

## 6. Ajuste durante el entreno

### 6.1 Ajuste durante el entreno, teléfono (`AdaptacionDurante.dc.html`)

- **Para qué:** cuando los datos lo piden en plena sesión, la app primero alarga el descanso sola y luego propone bajar el peso de la serie siguiente; la persona decide en un toque.
- **Estructura:** es el descanso a pantalla completa de 5.1 con estos cambios:
  1. Barra superior "Pierna B" con "18:40"; "Descanso · Sentadilla".
  2. Anillo de 260 (radio 122, grosor 12) en hielo con "0:48", "de 2:30", el pulso "152" y "listo bajo 120".
  3. Píldora debajo del anillo en `surface2` con `timer` en hielo: "Sumé 30 s: tu pulso no baja" y el botón **Deshacer** en `ember`. Deshacer quita los 30 s (el total vuelve a 2:00) y la píldora se va.
  4. Tarjeta **Propuesta para la serie 3** (entra subiendo 16 dp con fundido en 420 ms), con `trending_down` en oro: "Tu pulso sigue en 152 tras 2:00 de descanso y la serie 2 salió con 6 de 8 reps."; la carga nueva "72,5 kg × 8" (40/44) y la anterior tachada "77,5 kg" en `text3`; botones **Aplicar** (`ember`) y **Seguir igual** (`surface3`), de 48 y mitad de ancho cada uno.
  5. "No es consejo médico. Si te sientes mal, para."
  6. Barra inferior de descanso: -15, +15, Saltar descanso; en listo, Completar serie 3.
- **Estados:**
  - **Aplicado:** la tarjeta se vuelve una fila con `check` en `ember` sobre `emberVeil`: "Serie 3: 72,5 kg × 8" y "Solo por hoy; tu plan no cambia.", con **Deshacer**.
  - **Seguir igual:** aviso "Sigues con 77,5 kg" y la tarjeta normal de la serie siguiente (77,5 kg y 8 reps) con "Sigues con el plan · ver la propuesta".
  - **Listo:** "Listo" en `ember` y la carga que quedó ("72,5 × 8" o "77,5 × 8").
- **Reglas del ajuste en vivo** (`HANDOFF.md` 11.8): orden descanso, peso, series o cardio y por último el ejercicio; el descanso se ajusta solo (hasta 60 s) y con Deshacer; lo demás pregunta; nunca interrumpe una serie; como mucho un cambio cada dos series; si el pulso sigue cerca del máximo incluso descansando, propone parar; se guarda en `AdaptationLog` con `momento = durante`.

### 6.2 Apple Watch · ajuste en vivo (`AppleWatchAjuste.dc.html`)

El borde de la pantalla sigue siendo el descanso (hielo, 0:48 en la cabecera, la hora a la derecha). Título "¿Bajas a 72,5 kg?" (34/40, 700) y "Tu pulso sigue en 152 y la serie 2 salió con 6 reps." (24, `text2`). Botón **Sí, bajar** de 96 px en `ember` con `touch_double` (el Doble toque dice que sí) y **Seguir con 77,5** de 84 px en `surface3`. Al responder: círculo de 96 con `check` y "Serie 3: 72,5 kg · Solo por hoy" o "Sigues con 77,5 · Si te cuesta, avísame"; tocarlo deja cambiar la respuesta. La misma propuesta sale en la Live Activity y se acepta desde cualquiera de los tres.

---

## 7. Disciplinas y ficha de ejercicio

### 7.1 Disciplinas (`Disciplinas.dc.html`)

- **Para qué:** empezar un entreno de cualquier disciplina desde Plan, sin una cuarta pestaña.
- **Estructura:** barra superior con `chevron_left` (vuelve a Plan) y "Nuevo entreno"; título "¿Qué vas a entrenar?" (28/34) y "Todas usan el mismo cronómetro y se guardan en Salud."; cuadrícula de 2 columnas con 10 baldosas de 96 dp (radio 20, `surface1`, relleno 14; ícono en un círculo de 36 arriba y nombre con detalle abajo):

| Baldosa | Detalle | Ícono | Color | Abre |
| --- | --- | --- | --- | --- |
| Fuerza | Series con peso | `fitness_center` | brasa | Entreno |
| Funcional | EMOM, AMRAP, por tiempo | `local_fire_department` | brasa | Bloque funcional (7.4) |
| HIIT e intervalos | Tus tiempos, con voz | `timer` | brasa | Intervalos a tu medida (5.2) |
| Calistenia | Peso corporal | `sports_gymnastics` | brasa | Entreno |
| Yoga | Flujos por postura | `self_improvement` | hielo | Flujo (7.2) |
| Pilates | Suelo y reformer | `accessibility_new` | hielo | Flujo |
| Movilidad | Estirar y calentar | `spa` | hielo | Flujo |
| Respiración | Patrones guiados | `air` | hielo | Respiración (7.3) |
| Cardio | Correr, bici o remo | `directions_run` | brasa | Intervalos o sesión continua |
| Combate | Rounds con voz | `sports_mma` | brasa | Intervalos por rounds |

  El círculo del ícono va en `emberVeil` con ícono `ember` (lo intenso) o en `iceVeil` con ícono hielo (lo calmado). Debajo, la fila **Ver todas** con "Escalada, adaptado, deportes y más" y `chevron_right`. Al presionar, la baldosa baja a escala 0,97.
- **Marca:** se dice "Funcional", nunca "CrossFit": esa marca solo la pueden usar los gimnasios afiliados.

### 7.2 Flujo de yoga (`YogaFlujo.dc.html`)

- **Estructura:**
  1. Barra superior: minimizar, "Saludo al sol A" con "Ronda 2 de 5" y el botón de la voz.
  2. Segmentos de posturas de la ronda (7): hechas en hielo, la actual en `text1`.
  3. Tarjeta de 372 × 240 (radio 28) con un brillo radial `iceVeil` y la **figura de la postura**: dibujo propio de Forja con trazos de 16 dp en hielo, puntas y uniones redondeadas, cabeza de radio 14 y una línea de suelo en `surface3`. Al cambiar de postura, la figura nueva entra con un fundido de 450 ms.
  4. Nombre de la postura (28/34, 700, centrado) y su nombre en sánscrito en cursiva `text2` ("Perro boca abajo", "Adho Mukha Svanasana").
  5. Guía de respiración: un círculo de 64 con borde de 2 en hielo y un relleno al 28 % que crece al inhalar y se encoge al exhalar (ciclo de 8 s, curva suave), junto a la palabra **Inhala** o **Exhala** (32/40, hielo) que cambia a mitad del ciclo. Con pausa, la animación se congela.
  6. Puntos de respiraciones (12 dp): hechas en hielo, la actual con anillo, y "Respiración 3 de 5".
  7. Caja **Siguiente** con la postura que viene ("Montaña · ronda 3" o "Brazos arriba · 1 respiración").
  8. "La voz nombra cada postura y cuenta las respiraciones; el reloj vibra al cambiar."
  9. Barra inferior: pausa, **Siguiente postura** y `more_horiz`.
- **Secuencia del ejemplo:** Montaña (Tadasana), Brazos arriba (Urdhva Hastasana), Flexión hacia delante (Uttanasana), Media flexión (Ardha Uttanasana), Plancha (Kumbhakasana), Perro boca arriba (Urdhva Mukha Svanasana), Perro boca abajo (Adho Mukha Svanasana, 5 respiraciones). Cada postura dura sus respiraciones, de 8 s cada una; al terminar la última pasa sola a la siguiente.
- **Figuras:** dibujarlas como vectores propios (no imágenes con licencia ajena). Las coordenadas de las 7 están en el diseño; la regla es una sola grosor de trazo, perfil mirando a la izquierda y el suelo siempre a la misma altura.

### 7.3 Respiración guiada (`Respiracion.dc.html`)

- **Estructura:**
  1. Barra superior: minimizar, "Respiración guiada" con "Quedan 4:27" y el botón de la voz. Brillo radial de hielo al fondo.
  2. Chips de patrón: **Cuadrada 4-4-4-4**, **Relajante 4-7-8** y **Coherente 5-5** (elegido en `iceVeil` con borde hielo).
  3. Figura de 300 × 300:
     - **Cuadrada:** un cuadrado de 240 con esquinas de radio 28 y trazo de 8 en `surface3`. El lado de la fase actual se pinta en hielo (arriba inhala, derecha retén, abajo exhala, izquierda retén) y un punto blanco de 22 con halo de hielo recorre el borde a velocidad constante (un lado cada 4 s).
     - **Relajante y coherente:** un círculo de 240 que crece hasta escala 1 al inhalar, se queda al retener y baja a 0,55 al exhalar, con la duración exacta de cada fase.
     En el centro: la fase ("Inhala", "Retén", "Exhala", 30/36, hielo) y los segundos que quedan de la fase (72/76).
  4. Dos cajas: **Ciclo** "3" y **Pulso** "71" con "empezaste en 78".
  5. Aviso con `info` en oro: "Sentado o acostado; nunca en el agua, conduciendo ni de pie. Si te mareas, vuelve a respirar normal."
  6. "Al terminar se guarda en Salud y en Health Connect como sesión de respiración."
  7. Barra inferior: pausa, **Terminar** (al terminar, "Guardado en Salud") y `more_horiz`.
- **Guardado:** HealthKit `mindfulSession`; Health Connect `MindfulnessSessionRecord` de tipo respiración (si el teléfono lo soporta). Patrones con retención larga o hiperventilación muestran el aviso antes de empezar.

### 7.4 Funcional · AMRAP (`WodAmrap.dc.html`)

- **Estructura:**
  1. Barra superior: minimizar, "AMRAP 12 min" con "Funcional" y opciones. Brillo radial de brasa arriba.
  2. Tiempo que queda "7:42" (96/100, 700, cifras; en `ember` en los últimos 10 s) y "de 12:00".
  3. Barra de avance de 8 dp en `ember` sobre `surface3`, con transición lineal de 1 s.
  4. Marcador **Rondas + reps** "3 + 10" (56/64) y, a la derecha, "Tu marca **8 + 12**" y "A este ritmo **8 rondas**" (en `ember`). El ritmo es (rondas + reps hechas / reps por ronda) × 720 / segundos transcurridos.
  5. Tarjeta **Ronda 4** con "Toca cada uno al terminarlo" y una fila de 60 por movimiento: círculo de 28 (hecho: `ember` con `check`; el que sigue: anillo `ember`; pendiente: anillo `activeControl`), las reps (22/28) y el movimiento con su carga ("10 · Wall balls · Balón de 9 kg", "15 · Kettlebell swing · 16 kg", "20 · Saltos dobles · Cuerda"). El movimiento en juego va sobre `surface2`. Al marcar el último, la ronda se suma sola.
  6. Pulso "168 · zona 5" con corazón en `ember`.
  7. "La voz avisa cada minuto y los últimos 10 s; el reloj suma rondas con un toque."
  8. Barra inferior: pausa, **+ 1 ronda** (principal, `ember`) y `more_horiz`. Al acabar el tiempo: "Tiempo" y **Guardar 8 + 12**.
- **Formatos hermanos que usan esta misma pantalla:** EMOM (cuenta atrás de cada minuto, lo que sobra es descanso), por tiempo con límite (cuenta hacia arriba) y rondas (tiempo total). Cambia el contador y el resultado (`HANDOFF.md` 11.9).

### 7.5 Ficha de ejercicio (`FichaEjercicio.dc.html`)

- **Para qué:** lo que el motor sabe de un ejercicio: músculos, exigencia y alternativas.
- **Estructura:**
  1. Barra superior: `chevron_left`, "Ejercicio" y **Historial** (texto en `ember`, abre 2.2).
  2. "Press banca con barra" (26/32, 700) y "Fuerza · barra y banco · nivel intermedio".
  3. Tarjeta de 150 dp con brillo `emberVeil` y la figura (mismo estilo que el yoga, en `ember`): banco en `activeControl`, persona acostada y la barra vista de lado como un disco gris con centro claro.
  4. **Músculos** ("parte del trabajo"): filas de 28 (columnas 140 / 1fr / 44) con el nombre, una barra de 10 dp en `ember` sobre `surface3` y el porcentaje: Pectoral mayor 55 %, Tríceps 25 %, Deltoides anterior 20 %.
  5. **Exigencia** (columnas 150 / 1fr / 56) con medidores de segmentos de 8 dp: General 4 de 5, Técnica 3 de 5, Carga en la columna 1 de 3.
  6. Impacto por articulación como chips de 32 con un punto: Hombro **alto** (punto oro), Codo **medio**, Muñeca **medio** (punto `text2`), Lumbar **bajo** (punto `activeControl`).
  7. **Si hoy necesitas otra cosa:** dos filas de 56: `trending_down` en hielo, "Menos exigente · Press con mancuernas · 3 de 5"; `trending_up` en `ember`, "Más exigente · Press banca con pausa · 5 de 5".
  8. "Fuente: free-exercise-db, dominio público. Valores revisados a mano." (12/16, `text3`).
  9. Botón principal **Añadir a un entreno** (`add`).
- **Datos:** salen de `ExerciseMuscle` (porcentaje), `ExerciseDemand`, `ExerciseProgression` y `ExerciseSource` (`HANDOFF.md` sección 10).

---

## 8. Presupuesto de toques

| Acción | Toques |
| --- | --- |
| Empezar el entreno sugerido | 1 |
| Registrar la serie tal como se sugirió | 1 |
| Pasar a la siguiente serie | 0 (avanza sola al terminar el descanso) |
| Registrar otras reps | 3 (reps, número y Completar serie) |
| Ajustar el descanso ±15 s o saltarlo | 1 |
| Marcar el RIR | 0 a 1 (se puede ignorar) |
| Deshacer una serie | 1, en el aviso |
| Volver al entreno desde otra pestaña | 1, en el accesorio sobre la isla |
| Aplicar el ajuste del día | 1, en la tarjeta de Hoy |
| Aceptar un ajuste durante el entreno | 1 (en el reloj, Doble toque) |
| Ver el historial de un ejercicio | 2 |
| Sustituir un ejercicio | 2 |
| Dar el feedback de la sesión | 1 |
| Terminar | 1 gesto de deslizar |
| Anotar las reps de una ronda de intervalos | 1, en el descanso (o ninguno: guarda las de la última vez) |
| Sumar una ronda en un AMRAP | 1 |

Cambiar de pestaña y hacer scroll nunca vibran.

## 9. Qué falta por diseñar

Siguen sin diseño en el lienzo (Etapa 1): el PAR-Q+ al crear la cuenta, el modo joven y el permiso de los padres, molestias y lesiones, preparación en detalle, el Galaxy Watch con la rutina y los intervalos, intervalos en el Apple Watch, la Live Activity en sus cuatro vistas, la Live Update y la Now Bar. Hasta que estén, una hormiga que llegue a ellas pide el diseño al jefe y el jefe se lo pide a Juan; no se inventa una pantalla nueva.
