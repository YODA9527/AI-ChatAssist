package com.chatassist.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.chatassist.data.local.security.SecureKeyStorage
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.ModelConfig
import com.chatassist.domain.model.ModelScheme
import com.chatassist.domain.model.SuggestedAction
import com.chatassist.domain.model.findPreset
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
 *
 * **v0.17 变更**（哥哥下旨修复"切换触发方式再返回主页闪退"）：
 * - [Json] 配置由默认 `Json{}` 升级为**强容错配置**，关键项：
 *   - `coerceInputValues = true`：旧 JSON 里出现**未知枚举值**（如 v0.16 的
 *     `triggerMode: NOTIFICATION` / `GESTURE`）→ 自动回退到枚举默认值（[TriggerMode.MANUAL]），
 *     **不再抛 [kotlinx.serialization.SerializationException]**
 *   - `ignoreUnknownKeys = true`：旧 JSON 里出现 AppConfig 已删除/未知的字段 → 忽略，不报错
 *   - `isLenient = true`：允许非标准 JSON 格式（防御性，正常写入用不上）
 *   - `encodeDefaults = true` / `explicitNulls = false`：与 [com.chatassist.data.llm.di.NetworkModule]
 *     的网络 Json 配置对齐
 * - **背景**：v0.16.1 前的版本用默认 `Json{}` 反序列化 DataStore JSON。默认配置在遇到未知枚举
 *   值时会抛异常，被外层 `runCatching` 吞掉后 cfg 静默回退到默认 [AppConfig]() → 用户感受到
 *   「切了 triggerMode 回到主页状态不对 / 像闪退」。强容错 Json 让旧值优雅降级到默认枚举值
 *   而非抛错，是更稳的方案。
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
            throw AppError.of(
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

    override suspend fun saveModelApiKey(alias: String, key: String) {
        secureKey.saveApiKey(alias, key)
    }

    override fun getModelApiKey(alias: String): String = secureKey.getApiKey(alias)

    /**
     * 老用户迁移（v2.2 §13.4）：旧版单一模型配置（apiUrl/apiKey/model）合成默认方案。
     * 真实 apiKey 从旧全局加密 key 迁移到新别名 `default:text`。
     */
    override suspend fun migrateLegacySchemes() {
        val cur = snapshot()
        if (cur.schemes.isNotEmpty()) return

        val legacyKey = secureKey.getApiKey()
        val textPreset = findPreset(cur.model.ifBlank { "deepseek-chat" })
        val textModel = ModelConfig(
            model = cur.model.ifBlank { "deepseek-chat" },
            apiUrl = cur.apiUrl.ifBlank { "https://api.deepseek.com/v1" },
            apiFormat = cur.apiFormat,
            inputPricePerMToken = textPreset?.inputPricePerMToken ?: 0.0,
            outputPricePerMToken = textPreset?.outputPricePerMToken ?: 0.0,
            cachedPricePerMToken = textPreset?.cachedPricePerMToken ?: 0.0,
            apiKeyAlias = "default:text",
        )
        if (legacyKey.isNotBlank()) secureKey.saveApiKey("default:text", legacyKey)

        val scheme = ModelScheme(
            id = "default",
            name = "默认方案",
            textModel = textModel,
            multimodalModel = ModelConfig(
                model = "local",
                apiUrl = "http://localhost:11434/v1",
                apiKeyAlias = "default:multimodal",
            ),
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        update {
            it.copy(
                schemes = listOf(scheme),
                activeSchemeId = "default",
                // 旧单模型字段已并入方案，清空避免遗留
                apiUrl = "",
                apiKey = "",
                model = "",
            )
        }
    }

    // ====================== 辅助方法 ======================

    /**
     * 强容错 Json 实例（v0.17 引入，详见类注释）。
     *
     * 关键：
     * - [coerceInputValues] 解决旧 JSON 枚举值不存在的兼容问题（核心闪退修复）
     * - 单例 [Json] 实例避免每次序列化/反序列化都重建配置对象
     */
    private val configJson: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun AppConfig.encodeToJson(): String =
        configJson.encodeToString(AppConfig.serializer(), this)

    private fun String.decodeFromJson(): AppConfig =
        configJson.decodeFromString(AppConfig.serializer(), this)

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
