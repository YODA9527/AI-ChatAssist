package com.chatassist.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.ModelConfig
import com.chatassist.domain.model.ModelScheme
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.ModelApiRepository
import com.chatassist.domain.repository.ModelSchemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 模型方案编辑页 ViewModel（v2.2 §13.4）。
 *
 * 文本 / 多模态两个模型各自独立配置（apiUrl / 模型名 / 密钥 / 价格）。
 * 密钥走加密存储（[ConfigRepository.saveModelApiKey]），不进普通 DataStore。
 * 保存时把两个模型的价格同步进定价表（[ModelSchemeRepository.save]）。
 */
@HiltViewModel
class ModelSchemeEditViewModel @Inject constructor(
    private val schemeRepo: ModelSchemeRepository,
    private val configRepo: ConfigRepository,
    private val modelApi: ModelApiRepository,
) : ViewModel() {

    private val saveErrorHandler = CoroutineExceptionHandler { _, e ->
        Log.e("ModelSchemeEditVM", "保存失败", e)
    }

    private val _scheme = MutableStateFlow(ModelScheme.newScheme("新方案"))
    val scheme: StateFlow<ModelScheme> = _scheme.asStateFlow()

    private val _textApiKey = MutableStateFlow("")
    val textApiKey: StateFlow<String> = _textApiKey.asStateFlow()

    private val _mmApiKey = MutableStateFlow("")
    val mmApiKey: StateFlow<String> = _mmApiKey.asStateFlow()

    private val _testResult = MutableStateFlow<SchemeTestResult>(SchemeTestResult.Idle)
    val testResult: StateFlow<SchemeTestResult> = _testResult.asStateFlow()

    /** 加载待编辑方案（schemeId 为空表示新建）。 */
    fun load(schemeId: String?) {
        if (schemeId.isNullOrBlank()) return
        viewModelScope.launch {
            schemeRepo.snapshotAll().firstOrNull { it.id == schemeId }?.let { s ->
                _scheme.value = s
                _textApiKey.value = configRepo.getModelApiKey(s.textModel.apiKeyAlias)
                _mmApiKey.value = configRepo.getModelApiKey(s.multimodalModel.apiKeyAlias)
            }
        }
    }

    fun updateName(name: String) {
        _scheme.value = _scheme.value.copy(name = name)
    }

    fun updateText(config: ModelConfig) {
        _scheme.value = _scheme.value.copy(textModel = config)
    }

    fun updateMultimodal(config: ModelConfig) {
        _scheme.value = _scheme.value.copy(multimodalModel = config)
    }

    fun setTextApiKey(key: String) { _textApiKey.value = key }
    fun setMultimodalApiKey(key: String) { _mmApiKey.value = key }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch(saveErrorHandler) {
            val s = _scheme.value.copy(name = _scheme.value.name.ifBlank { "新方案" })
            configRepo.saveModelApiKey(s.textModel.apiKeyAlias, _textApiKey.value.trim())
            configRepo.saveModelApiKey(s.multimodalModel.apiKeyAlias, _mmApiKey.value.trim())
            schemeRepo.save(s)
            onSaved()
        }
    }

    fun testTextConnection() {
        runTest(_textApiKey.value) { s, key -> modelApi.testConnection(s.textModel.apiUrl, key.trim(), s.textModel.model) }
    }

    fun testMultimodalConnection() {
        runTest(_mmApiKey.value) { s, key -> modelApi.testConnection(s.multimodalModel.apiUrl, key.trim(), s.multimodalModel.model) }
    }

    private fun runTest(key: String, block: suspend (ModelScheme, String) -> kotlin.Result<Long>) {
        _testResult.value = SchemeTestResult.Testing
        viewModelScope.launch {
            val s = _scheme.value
            block(s, key)
                .onSuccess { _testResult.value = SchemeTestResult.Success(it) }
                .onFailure { e ->
                    val msg = (e as? com.chatassist.domain.model.AppError)?.userMessage ?: e.message ?: "未知错误"
                    _testResult.value = SchemeTestResult.Failed(msg)
                }
        }
    }

    fun dismissTestResult() { _testResult.value = SchemeTestResult.Idle }
}

sealed interface SchemeTestResult {
    data object Idle : SchemeTestResult
    data object Testing : SchemeTestResult
    data class Success(val latencyMs: Long) : SchemeTestResult
    data class Failed(val message: String) : SchemeTestResult
}
