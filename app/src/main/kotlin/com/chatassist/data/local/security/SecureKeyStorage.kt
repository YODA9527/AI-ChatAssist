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

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API, key).apply()
    }

    fun getApiKey(): String =
        prefs.getString(KEY_API, "").orEmpty()

    fun clearApiKey() {
        prefs.edit().remove(KEY_API).apply()
    }

    companion object {
        private const val FILE_NAME = "chatassist_secure"
        private const val KEY_API = "api_key"
    }
}
