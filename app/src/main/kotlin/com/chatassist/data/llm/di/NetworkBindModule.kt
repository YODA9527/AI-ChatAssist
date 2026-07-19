package com.chatassist.data.llm.di

import com.chatassist.data.repository.ModelApiRepositoryImpl
import com.chatassist.domain.repository.ModelApiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 网络层仓库绑定模块（v0.3 引入）。
 *
 * 把 [ModelApiRepositoryImpl] 绑定到 [ModelApiRepository] 接口。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindModule {

    @Binds
    @Singleton
    abstract fun bindModelApiRepository(impl: ModelApiRepositoryImpl): ModelApiRepository
}
