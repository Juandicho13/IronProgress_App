package com.olarte.ironprogress

import androidx.compose.ui.window.ComposeUIViewController

// Swift la llama como MainViewControllerKt.MainViewController(); por eso empieza con mayúscula.
// startIronProgress() ya corrió en el init de la App; llamarlo aquí otra vez no hace nada y
// cubre el caso de que alguien cree la vista sin pasar por ahí.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController(
    configure = { startIronProgress() },
) { App() }
