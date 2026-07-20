/*
 * v0.15 KPI-1：冷启动（Cold Start）— WARM 变体（mumu 兼容）
 *
 * 目标：app 从 launcher 拉起到 MainActivity 第一帧上屏 < 600ms
 * 工具：androidx.benchmark.macro.junit4.MacrobenchmarkRule + StartupMode.WARM
 *
 * 注：原本 StartupMode.COLD 需要 force-stop target app，mumu 上 force-stop 行为与
 * Macrobenchmark internal helper 冲突；改用 WARM（不杀进程，测 launcher → MainActivity 唤醒耗时）。
 * 真机回归时切回 StartupMode.COLD 即可。
 */
package com.chatassist.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val targetPackage = "com.chatassist.debug"

    @Test
    fun warmStart_toMainActivity() {
        val setup: MacrobenchmarkScope.() -> Unit = { pressHome() }
        val measure: MacrobenchmarkScope.() -> Unit = {
            startActivityAndWait()
            device.wait(Until.hasObject(By.res("chatassist_root_layout")), 5_000L)
        }
        rule.measureRepeated(
            packageName = targetPackage,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = androidx.benchmark.macro.CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = setup,
            measureBlock = measure,
        )
    }
}
