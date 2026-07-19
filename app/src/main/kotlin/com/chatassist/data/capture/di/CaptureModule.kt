package com.chatassist.data.capture.di

import com.chatassist.data.capture.CaptureStrategyManager
import com.chatassist.domain.capture.CaptureStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 捕获层 Hilt 绑定模块。
 *
 * 把 [CaptureStrategyManager] 绑定到 domain 接口 [CaptureStrategy]，
 * 使 ViewModel/UseCase 只依赖 domain 契约（满足 Clean 分层铁律）。
 * 各 Channel / PermissionManager / ScreenCaptureManager 走 @Inject 构造器，无需在此声明。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureBindModule {

    @Binds
    @Singleton
    abstract fun bindCaptureStrategy(impl: CaptureStrategyManager): CaptureStrategy
}
