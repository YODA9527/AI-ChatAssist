package com.chatassist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.ThemeMode
import com.chatassist.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 根级 ViewModel。
 *
 * 仅负责把主题相关配置（themeMode / dynamicColor）从 DataStore 暴露给
 * [ChatAssistRoot]，让 [ChatAssistTheme] 能真正应用用户设置的主题，
 * 而不是永远停留在默认的 SYSTEM 模式。
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val configRepo: ConfigRepository,
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(true)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    init {
        viewModelScope.launch {
            configRepo.observeAll().collect { cfg ->
                _themeMode.value = cfg.themeMode
                _dynamicColor.value = cfg.dynamicColor
            }
        }
    }
}
