package com.chatassist.data.local.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.chatassist.data.local.ConfigRepositoryImpl
import com.chatassist.data.local.HistoryDatabase
import com.chatassist.data.local.HistoryRepositoryImpl
import com.chatassist.data.local.ModelSchemeRepositoryImpl
import com.chatassist.data.local.PricingRepositoryImpl
import com.chatassist.data.local.UsageRepositoryImpl
import com.chatassist.data.local.dao.HistoryDao
import com.chatassist.data.local.dao.PricingDao
import com.chatassist.data.local.dao.UsageDao
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.HistoryRepository
import com.chatassist.domain.repository.ModelSchemeRepository
import com.chatassist.domain.repository.PricingRepository
import com.chatassist.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore 顶层扩展：在 Context 上挂载单例 Preferences DataStore。
 *
 * 文件名 `chatassist_prefs` 存放在 `/data/data/com.chatassist/files/datastore/` 下。
 */
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chatassist_prefs"
)

/**
 * 本地数据层 Hilt 模块。
 *
 * 职责：提供 DataStore 实例，并把 ConfigRepositoryImpl 绑定到 ConfigRepository 接口。
 */
@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.appDataStore

    @Provides
    @Singleton
    fun provideHistoryDatabase(
        @ApplicationContext context: Context,
    ): HistoryDatabase = Room.databaseBuilder(
        context,
        HistoryDatabase::class.java,
        HistoryDatabase.NAME,
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideHistoryDao(db: HistoryDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideUsageDao(db: HistoryDatabase): UsageDao = db.usageDao()

    @Provides
    @Singleton
    fun providePricingDao(db: HistoryDatabase): PricingDao = db.pricingDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindModule {

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindPricingRepository(impl: PricingRepositoryImpl): PricingRepository

    @Binds
    @Singleton
    abstract fun bindModelSchemeRepository(impl: ModelSchemeRepositoryImpl): ModelSchemeRepository
}
