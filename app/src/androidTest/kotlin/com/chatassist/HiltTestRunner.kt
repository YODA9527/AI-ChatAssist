/*
 * v0.15 模块 13 测试基建：Hilt 仪器测试 Runner
 *
 * v0.16 修复：ChatAssistApp 实现了 Configuration.Provider（提供 HiltWorkerFactory 给 WorkManager），
 * 但 HiltTestApplication 不实现它。WorkManagerInitializer 在 main manifest 已被 disable。
 *
 * 解决思路（保持精简）：
 * - 不在 HiltTestRunner 里手动 init WorkManager（androidTest 阶段没 Context application，初始化会 NPE）
 * - KeepAliveManager.start() 内部已 runCatching 包裹 WorkManager.getInstance()，会吞掉
 *   IllegalStateException，不影响 androidTest 渲染
 * - KeepAliveService 不依赖 WorkManager，单独启动 OK
 */
package com.chatassist

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * HiltTestRunner —— 用 HiltTestApplication 替换 default Application，
 * 让 @HiltAndroidTest 拿到一个隔离的 DI 容器。
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
