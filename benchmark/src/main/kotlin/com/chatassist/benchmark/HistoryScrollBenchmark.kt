/*
 * v0.15 KPI-2：历史页滚动帧率（Frame Timing）
 *
 * 目标：HistoryScreen 的 LazyColumn 滚动时 P95 帧 < 16ms（60 FPS）
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
class HistoryScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    // v0.15：手动指定 :app 的 debug 后缀 applicationId
    private val targetPackage = "com.chatassist.debug"

    @Test
    fun historyScroll_frameTiming() {
        val setup: MacrobenchmarkScope.() -> Unit = { pressHome() }
        val measure: MacrobenchmarkScope.() -> Unit = {
            startActivityAndWait()
            device.wait(Until.hasObject(By.res("chatassist_root_layout")), 5_000L)
            val historyTab = device.findObject(By.text("历史"))
            historyTab?.click()
            device.wait(Until.hasObject(By.text("历史记录")), 1_000L)

            val displayHeight = device.displayHeight
            val displayWidth = device.displayWidth
            val midX = displayWidth / 2
            val startY = (displayHeight * 0.75f).toInt()
            val endY = (displayHeight * 0.25f).toInt()
            repeat(5) {
                device.swipe(midX, startY, midX, endY, 20)
            }
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
