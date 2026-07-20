/*
 * v0.15 KPI-4：Tab 导航吞吐（Navigation Latency）
 *
 * 目标：首页 → 历史 Tab 切换 < 200ms
 * 工具：MacrobenchmarkRule + FrameTimingMetric
 *
 * ChatParser 解析本身已在 v0.10 52 单测里覆盖了纯函数路径，
 * 这里作为「导航操作」的 FrameTiming 基线。
 */
package com.chatassist.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ParseBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    // v0.15：手动指定 :app 的 debug 后缀 applicationId
    private val targetPackage = "com.chatassist.debug"

    @Test
    fun historyTab_navigation() {
        val setup: MacrobenchmarkScope.() -> Unit = { pressHome() }
        val measure: MacrobenchmarkScope.() -> Unit = {
            startActivityAndWait()
            device.wait(Until.hasObject(By.res("chatassist_root_layout")), 5_000L)
            val historyTab = device.findObject(By.text("历史"))
            historyTab?.click()
            device.wait(Until.hasObject(By.text("历史记录")), 3_000L)
        }
        rule.measureRepeated(
            packageName = targetPackage,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = androidx.benchmark.macro.CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = setup,
            measureBlock = measure,
        )
    }
}
