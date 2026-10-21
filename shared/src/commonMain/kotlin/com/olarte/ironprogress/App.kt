package com.olarte.ironprogress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.olarte.ironprogress.data.catalog.SeedExerciseRepository
import com.olarte.ironprogress.ui.IronProgressApp

@Composable
fun App() {
    val repository = remember { SeedExerciseRepository() }
    IronProgressApp(exerciseRepository = repository)
}
