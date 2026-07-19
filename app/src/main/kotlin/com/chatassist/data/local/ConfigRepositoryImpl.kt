package com.chatassist.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.chatassist.data.local.security.SecureKeyStorage
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.SuggestedAction
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.ModelApiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置仓库实现。
 *
 * 存储策略：
 * - 普通配置 → DataStore<Preferences>（明文，速度快）
 * - API Key → SecureKeyStorage（EncryptedSharedPreferences + Android Keystore）
 *
 * 这样设计的原因：DataStore 不能加密，但 API Key 必须加密。
 * 其他配置项（URL / 模型名 / 人设等）就算泄露也无所谓。
 *
 * **v0.3 变更**：
 * - 删除独立的 `ConnectionTester`，统一通过 [ModelApiRepository.testConnection] 测试连接
 * - 保持 [testConnection] 接口签名不变（向后兼容 [SettingsViewModel]）
 */
@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureKey: SecureKeyStorage,
    private val modelApi: ModelApiRepository,
) : ConfigRepository {

    override fun observeAll(): Flow<AppConfig> =
        dataStore.data.map { prefs ->
            val json = prefs[KEY_CONFIG_JSON] ?: return@map AppConfig()
            runCatching { json.decodeFromJson() }
                .getOrElse { AppConfig() }
                .withApiKey(secureKey.getApiKey())
        }

    override suspend fun snapshot(): AppConfig = observeAll().first()

    override suspend fun update(transform: (AppConfig) -> AppConfig) {
        val current = snapshot()
        val updated = transform(current)
        // API Key 走单独加密通道
        val withoutKey = updated.copy(apiKey = "")
        if (updated.apiKey != current.apiKey) {
            secureKey.saveApiKey(updated.apiKey)
        }
        dataStore.edit { prefs ->
            prefs[KEY_CONFIG_JSON] = withoutKey.encodeToJson()
        }
    }

    override suspend fun testConnection(): Result<Long> = runCatching {
        val cfg = snapshot()
        if (cfg.apiKey.isBlank()) {
            throw AppError(
                code = ErrorCode.AUTH_401,
                userMessage = "请先填写 API Key",
                technicalDetail = "API Key is blank",
                retryable = false,
                suggestedAction = SuggestedAction.CHECK_API_KEY,
            )
        }
        // 委托给 ModelApiRepository，统一网络出口（maxAttempts=1 保持轻量快速反馈）
        modelApi.testConnection(maxAttempts = 1).getOrThrow()
    }

    // ====================== 辅助方法 ======================

    private fun AppConfig.encodeToJson(): String =
        Json.encodeToString(AppConfig.serializer(), this)

    private fun String.decodeFromJson(): AppConfig =
        Json.decodeFromString(AppConfig.serializer(), this)

    private fun AppConfig.withApiKey(key: String) =
        if (key.isEmpty()) this else copy(apiKey = key)

    companion object {
        private val KEY_CONFIG_JSON = stringPreferencesKey("config_json_v1")
    }
}

/**
 * Preferences 占位导入（实际由 LocalModule 提供 DataStore<Preferences>）。
 * 这里用 typealias 避免循环依赖。
 */
typealias Preferences = androidx.datastore.preferences.core.Preferences
private fun stringPreferencesKey(name: String) =
    androidx.datastore.preferences.core.stringPreferencesKey(name)
