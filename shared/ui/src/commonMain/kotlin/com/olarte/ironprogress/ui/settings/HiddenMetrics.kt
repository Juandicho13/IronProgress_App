package com.olarte.ironprogress.ui.settings

import com.olarte.ironprogress.domain.session.WorkoutSet

/**
 * Con "Ocultar peso corporal y calorías" (CFG-03) ninguna caloría sale en pantalla ni en avisos.
 * La serie guarda el dato igual: solo se oculta al mostrarlo. El peso corporal aún no se muestra
 * en ninguna pantalla; cuando llegue (F2-09) tiene que pasar por aquí.
 */
fun WorkoutSet.forDisplay(hideBodyMetrics: Boolean): WorkoutSet = if (hideBodyMetrics) copy(calories = null) else this
