package com.olarte.ironprogress

import androidx.compose.ui.window.ComposeUIViewController

// Swift la llama como MainViewControllerKt.MainViewController(); por eso empieza con mayúscula.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
