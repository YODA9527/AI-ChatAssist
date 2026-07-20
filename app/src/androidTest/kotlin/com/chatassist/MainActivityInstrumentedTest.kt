/*
 * v0.15 模块 13：MainActivity 仪器测试
 *
 * 目标：验证启动后根 layout 上屏、4 个 Tab 都能切到
 * 工具：HiltAndroidRule + Compose UI Test (createAndroidComposeRule)
 *
 * 注意：必须 mumu 7555 在线或真机 + USB 调试。
 * 跑法：
 *   adb -s 127.0.0.1:7555 shell am instrument -w -e class com.chatassist.MainActivityInstrumentedTest \
 *     com.chatassist.debug.test/com.chatassist.HiltTestRunner
 */
package com.chatassist

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun bottomBar_showsAllFourTabs() {
        composeTestRule.onNodeWithText("首页").assertExists()
        composeTestRule.onNodeWithText("历史").assertExists()
        composeTestRule.onNodeWithText("设置").assertExists()
        composeTestRule.onNodeWithText("关于").assertExists()
    }

    @Test
    fun switchToHistoryTab_showsHistoryContent() {
        composeTestRule.onNodeWithText("历史").performClick()
        // 切到历史页后会有「还没有任何记录」或「清空」按钮
        composeTestRule.onNodeWithText("历史记录").assertExists()
    }
}
