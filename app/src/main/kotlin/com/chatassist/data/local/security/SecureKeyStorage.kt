package com.chatassist.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key 加密存储。
 *
 * 使用 EncryptedSharedPreferences + Android Keystore。
 * - AES256_GCM 加密内容
 * - AES256_SIV 加密 key 名称
 * 即使 root 也无法轻易读取。
 *
 * v2.2 §13.4：从「全局单 key」升级为「按别名多 key」——每个方案的每个模型
 * （文本 / 多模态）各自持有一个 [apiKeyAlias]，真实 key 据此寻址。
 * 旧版全局 key（[KEY_API]）保留作迁移源：[getApiKey]/[saveApiKey] 单参版本仍可读写它。
 */
@Singleton
class SecureKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // ====================== 按别名（多模型）======================

    fun saveApiKey(alias: String, key: String) {
        prefs.edit().putString(PREFIX + alias, key).apply()
    }

    fun getApiKey(alias: String): String =
        prefs.getString(PREFIX + alias, "").orEmpty()

    fun clearApiKey(alias: String) {
        prefs.edit().remove(PREFIX + alias).apply()
    }

    // ====================== 旧版全局 key（迁移源/兼容）======================

    fun saveApiKey(key: String) = saveApiKey(KEY_API, key)

    fun getApiKey(): String = getApiKey(KEY_API)

    fun clearApiKey() = clearApiKey(KEY_API)

    companion object {
        private const val FILE_NAME = "chatassist_secure"
        private const val KEY_API = "api_key"
        private const val PREFIX = "model_key_"
    }
}
