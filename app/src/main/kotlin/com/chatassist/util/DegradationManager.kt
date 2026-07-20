package com.chatassist.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 降级策略管理器（v0.11.0：模块 12 A 项）。
 *
 * 4 维降级：网络 / OCR / 内存 / 电池。
 * 每维独立计数，连续异常触发升档（NONE → MILD → SEVERE）。
 *
 * 状态查询 API（给上层按需启用降级方案）：
 * - [pollMultiplier]：OCR 轮询间隔倍数（1.0 / 1.5 / 2.0 / 3.0）
 * - [streamEnabled]：是否启用流式输出（弱网时关）
 * - [maxContextTokens]：上下文 token 上限（内存压力时降）
 * - [shouldDropHistory]：是否丢弃旧历史（内存压力时丢）
 *
 * 设计原则：
 * - **指数级升档 + 线性级降档**（避免抖动）
 * - **状态本地 StateFlow 暴露**，UI 可观测
 * - **可重置**（用户切网络/清缓存后手动调 [reset]）
 */
@Singleton
class DegradationManager @Inject constructor() {

    enum class Level { NONE, MILD, SEVERE }
    enum class Axis { NETWORK, OCR, MEMORY, BATTERY }

    private val _level = MutableStateFlow(Level.NONE)
    val level: StateFlow<Level> = _level.asStateFlow()

    private val counts = mutableMapOf(
        Axis.NETWORK to 0,
        Axis.OCR to 0,
        Axis.MEMORY to 0,
        Axis.BATTERY to 0,
    )

    /** OCR 轮询间隔倍数（NONE=1.0 / MILD=1.5 / SEVERE=3.0）。 */
    val pollMultiplier: Float
        get() = when (_level.value) {
            Level.NONE -> 1.0f
            Level.MILD -> 1.5f
            Level.SEVERE -> 3.0f
        }

    /** 是否启用流式（NONE/MILD 启用，SEVERE 关闭以减少弱网压力）。 */
    val streamEnabled: Boolean
        get() = _level.value != Level.SEVERE

    /** 上下文 token 上限（NONE=8000 / MILD=4000 / SEVERE=2000）。 */
    val maxContextTokens: Int
        get() = when (_level.value) {
            Level.NONE -> 8000
            Level.MILD -> 4000
            Level.SEVERE -> 2000
        }

    /** 是否丢弃旧历史（仅 SEVERE 丢弃前 50%）。 */
    val shouldDropHistory: Boolean
        get() = _level.value == Level.SEVERE

    /** 上报某轴异常。连续 3 次相同轴 → 升 MILD；连续 5 次 → 升 SEVERE。 */
    @Synchronized
    fun report(axis: Axis) {
        val newCount = (counts[axis] ?: 0) + 1
        counts[axis] = newCount
        val newLevel = computeLevel()
        if (newLevel != _level.value) {
            Timber.tag(TAG).w("Degradation: ${_level.value} -> $newLevel (counts=$counts)")
            _level.value = newLevel
        }
    }

    /** 上报成功（用于降档：3 次成功降一档，最多到 NONE）。 */
    @Synchronized
    fun reportSuccess(axis: Axis? = null) {
        if (axis != null) {
            counts[axis] = (counts[axis] ?: 0).let { if (it > 0) it - 1 else 0 }
        } else {
            // 全轴降一档
            counts.replaceAll { _, v -> if (v > 0) v - 1 else 0 }
        }
        val newLevel = computeLevel()
        if (newLevel != _level.value) {
            Timber.tag(TAG).i("Degradation: ${_level.value} -> $newLevel (counts=$counts)")
            _level.value = newLevel
        }
    }

    /** 用户主动重置（清缓存、切换网络、设置页重置按钮）。 */
    @Synchronized
    fun reset() {
        counts.replaceAll { _, _ -> 0 }
        _level.value = Level.NONE
        Timber.tag(TAG).d("Degradation reset to NONE")
    }

    private fun computeLevel(): Level {
        val max = counts.values.max()
        return when {
            max >= 5 -> Level.SEVERE
            max >= 3 -> Level.MILD
            else -> Level.NONE
        }
    }

    companion object {
        private const val TAG = "DegradationManager"
    }
}
