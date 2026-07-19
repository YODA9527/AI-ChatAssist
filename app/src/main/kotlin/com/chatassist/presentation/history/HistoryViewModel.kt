package com.chatassist.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.HistoryRecord
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史记录页 ViewModel（模块 09 接入 Room）。
 *
 * 数据源：HistoryRepository.observeAll()（实时响应式）。
 * 隐私模式由 ReplySuggestionUseCase 在写入侧拦截；本页仅展示策略参数与清空/清理操作。
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val configRepo: ConfigRepository,
    private val historyRepo: HistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                historyRepo.observeAll(),
                configRepo.observeAll(),
            ) { records, cfg ->
                HistoryUiState(
                    privacyMode = cfg.privacyMode,
                    maxAgeDays = cfg.historyMaxAgeDays,
                    maxCount = cfg.historyMaxCount,
                    records = records,
                    isEmpty = records.isEmpty(),
                )
            }.collect { _state.value = it }
        }
    }

    fun delete(id: Long) = viewModelScope.launch { historyRepo.deleteById(id) }

    fun clearAll() = viewModelScope.launch { historyRepo.clearAll() }

    /** 按配置保留策略清理：保留最近 maxCount 条 + 删除超过 maxAgeDays 天的。 */
    fun runCleanup() = viewModelScope.launch {
        val cfg = configRepo.snapshot()
        val olderThan = if (cfg.historyMaxAgeDays > 0) {
            System.currentTimeMillis() - cfg.historyMaxAgeDays * 86_400_000L
        } else {
            0L
        }
        historyRepo.cleanup(olderThan, cfg.historyMaxCount)
    }
}

data class HistoryUiState(
    val privacyMode: Boolean = false,
    val maxAgeDays: Int = 30,
    val maxCount: Int = 1000,
    val records: List<HistoryRecord> = emptyList(),
    val isEmpty: Boolean = true,
)
