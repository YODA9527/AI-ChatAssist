package com.chatassist.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.ModelScheme
import com.chatassist.domain.repository.ModelSchemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 模型配置方案列表页 ViewModel（v2.2 §13.4）。
 */
@HiltViewModel
class ModelSchemesViewModel @Inject constructor(
    private val schemeRepo: ModelSchemeRepository,
) : ViewModel() {

    val schemes: StateFlow<List<ModelScheme>> = schemeRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setActive(id: String) = viewModelScope.launch { schemeRepo.setActive(id) }

    fun delete(id: String) = viewModelScope.launch { schemeRepo.delete(id) }
}
