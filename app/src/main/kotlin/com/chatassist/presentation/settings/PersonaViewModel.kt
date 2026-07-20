package com.chatassist.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.PersonaProfile
import com.chatassist.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 双向人设 ViewModel（v0.18，v2.2 §13.1）。
 *
 * 职责：
 * - 暴露人设方案列表 + 当前锁定 id
 * - 新建/保存/删除方案
 * - 设为当前（activePersonaId）、设为默认（isDefault）
 *
 * 所有写操作经 [ConfigRepository.update]（DataStore），异常由 [saveErrorHandler] 兜底记录，
 * 不裸崩 App。
 */
@HiltViewModel
class PersonaViewModel @Inject constructor(
    private val configRepo: ConfigRepository,
) : ViewModel() {

    private val saveErrorHandler = CoroutineExceptionHandler { _, e ->
        Log.e("PersonaViewModel", "人设操作失败", e)
    }

    private val _state = MutableStateFlow<PersonaUiState>(PersonaUiState.Loading)
    val state: StateFlow<PersonaUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configRepo.observeAll().collect { cfg ->
                _state.value = PersonaUiState.Loaded(
                    profiles = cfg.personas,
                    activePersonaId = cfg.activePersonaId,
                )
            }
        }
    }

    /** 设为当前生效方案（手动锁定，优先级高于自动匹配）。 */
    fun setActive(id: String) {
        viewModelScope.launch(saveErrorHandler) {
            configRepo.update { cfg ->
                cfg.copy(
                    activePersonaId = id,
                    personas = cfg.personas.map { p ->
                        if (p.id == id) p.copy(lastUsedAt = System.currentTimeMillis()) else p
                    },
                )
            }
        }
    }

    /** 保存方案（新增或更新，按 id 是否存在区分）。 */
    fun save(profile: PersonaProfile) {
        viewModelScope.launch(saveErrorHandler) {
            configRepo.update { cfg ->
                val exists = cfg.personas.any { it.id == profile.id }
                val next = if (exists) {
                    cfg.personas.map { if (it.id == profile.id) profile else it }
                } else {
                    cfg.personas + profile
                }
                // 首个方案自动设为默认，省去用户手动操作
                val withDefault = if (next.size == 1 && !next.any { it.isDefault }) {
                    next.map { it.copy(isDefault = it.id == profile.id) }
                } else {
                    next
                }
                cfg.copy(personas = withDefault)
            }
        }
    }

    /** 删除方案；若删掉当前锁定项则清空锁定，若删掉默认项则把第一个设为默认。 */
    fun delete(id: String) {
        viewModelScope.launch(saveErrorHandler) {
            configRepo.update { cfg ->
                val remaining = cfg.personas.filter { it.id != id }
                val activePersonaId = if (cfg.activePersonaId == id) "" else cfg.activePersonaId
                val withDefault = if (remaining.isNotEmpty() && !remaining.any { it.isDefault }) {
                    remaining.mapIndexed { i, p -> if (i == 0) p.copy(isDefault = true) else p }
                } else {
                    remaining
                }
                cfg.copy(personas = withDefault, activePersonaId = activePersonaId)
            }
        }
    }

    /** 设为默认方案。 */
    fun setDefault(id: String) {
        viewModelScope.launch(saveErrorHandler) {
            configRepo.update { cfg ->
                cfg.copy(personas = cfg.personas.map { it.copy(isDefault = it.id == id) })
            }
        }
    }
}

sealed interface PersonaUiState {
    data object Loading : PersonaUiState
    data class Loaded(
        val profiles: List<PersonaProfile>,
        val activePersonaId: String,
    ) : PersonaUiState
}
