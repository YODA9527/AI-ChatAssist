/*
 * v0.15 KPI-3：悬浮窗显示耗时（Overlay Show Latency）
 *
 * 目标：用户在首页点「显示悬浮窗」→ 悬浮窗浮出 < 300ms
 */
package com.chatassist.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
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
class OverlayShowBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    // v0.15：手动指定 :app 的 debug 后缀 applicationId
    private val targetPackage = "com.chatassist.debug"

    @Test
    fun overlayShow_tapToAppear() {
        val setup: MacrobenchmarkScope.() -> Unit = { pressHome() }
        val measure: MacrobenchmarkScope.() -> Unit = {
            startActivityAndWait()
            device.wait(Until.hasObject(By.res("chatassist_root_layout")), 5_000L)
            val showBtn = device.wait(Until.findObject(By.text("显示悬浮窗")), 3_000L)
            if (showBtn != null) {
                showBtn.click()
            }
            device.wait(Until.hasObject(By.text("隐藏悬浮窗")), 3_000L)
        }
        rule.measureRepeated(
            packageName = targetPackage,
            metrics = listOf(),
            compilationMode = androidx.benchmark.macro.CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = setup,
            measureBlock = measure,
        )
    }
}
