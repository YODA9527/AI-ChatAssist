package com.chatassist.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.AppError
import com.chatassist.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 设置页 ViewModel。
 *
 * 职责：
 * - 暴露当前配置
 * - 处理 API Key 保存（走加密通道）
 * - 测试连接
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepo: ConfigRepository,
) : ViewModel() {

    /**
     * 异常兜底：配置读写（DataStore / 加密存储）若意外抛错，
     * 记录日志而不是让未捕获异常直接崩 App。
     */
    private val saveErrorHandler = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "配置保存失败")
    }

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _testResult = MutableStateFlow<TestResult>(TestResult.Idle)
    val testResult: StateFlow<TestResult> = _testResult.asStateFlow()

    init {
        viewModelScope.launch {
            configRepo.observeAll().collect { cfg ->
                _state.value = SettingsUiState.Loaded(cfg)
            }
        }
    }

    /**
     * 更新 API Key（立即加密保存，不进入普通 DataStore）。
     */
    fun updateApiKey(key: String) {
        viewModelScope.launch(saveErrorHandler) {
            configRepo.update { it.copy(apiKey = key) }
        }
    }

    /**
     * 更新普通配置项。
     */
    fun update(transform: (AppConfig) -> AppConfig) {
        viewModelScope.launch(saveErrorHandler) {
            configRepo.update(transform)
        }
    }

    /**
     * 测试 API 连接。
     */
    fun testConnection() {
        _testResult.value = TestResult.Testing
        viewModelScope.launch {
            configRepo.testConnection()
                .onSuccess { ms ->
                    _testResult.value = TestResult.Success(ms)
                }
                .onFailure { e ->
                    val msg = when (e) {
                        is AppError -> e.userMessage
                        else -> e.message ?: "未知错误"
                    }
                    _testResult.value = TestResult.Failed(msg)
                }
        }
    }

    fun dismissTestResult() {
        _testResult.value = TestResult.Idle
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Loaded(val config: AppConfig) : SettingsUiState
}

sealed interface TestResult {
    data object Idle : TestResult
    data object Testing : TestResult
    data class Success(val latencyMs: Long) : TestResult
    data class Failed(val message: String) : TestResult
}
