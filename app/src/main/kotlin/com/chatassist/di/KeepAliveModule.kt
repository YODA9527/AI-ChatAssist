package com.chatassist.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chatassist.service.KeepAliveWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 后台保活 Hilt 模块（v0.12：模块 10）。
 *
 * **职责**：
 * 1. 提供 WorkManager 自定义 [Configuration]（绑定 HiltWorkerFactory 注入 @HiltWorker）
 * 2. 由 [com.chatassist.ChatAssistApp] 实现 `Configuration.Provider` 接口注入
 *
 * **不需要 Module 绑定 KeepAliveService / KeepAliveWorker / KeepAliveManager**：
 * - KeepAliveService 是 @AndroidEntryPoint，Hilt 自动处理
 * - KeepAliveWorker 是 @HiltWorker，HiltWorkerFactory 自动处理
 * - KeepAliveManager 是 @Singleton 构造注入，自动处理
 *
 * 注：WorkManager 2.7+ 默认禁用 Manifest 初始化（避免与 Hilt 冲突），
 * 需在 manifest 加 `androidx.work.WorkManagerInitializer` 移除，
 * 或保留 Manifest 初始化但 on-demand 初始化。这里走 on-demand 路线（Configuration.Provider）。
 */
@Module
@InstallIn(SingletonComponent::class)
object KeepAliveModule {

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        @ApplicationContext context: Context,
        workerFactory: HiltWorkerFactory,
    ): Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .setWorkerFactory(workerFactory)
        .build()
}
