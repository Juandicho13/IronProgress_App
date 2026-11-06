package com.olarte.ironprogress.ui.interval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Los intervalos guardados, para la sección de Plan. */
class IntervalListViewModel(repository: IntervalTemplateRepository) : ViewModel() {
    val templates: StateFlow<List<IntervalTemplate>> =
        repository.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
