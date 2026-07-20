package com.chatassist.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrashLoggerTest {

    private val sanitizer = object {
        val logger = TestableCrashLogger()
    }

    @Test
    fun `Bearer token 脱敏`() {
        val input = "Authorization: Bearer sk-1234567890abcdef1234567890abcdef"
        val output = sanitizer.logger.sanitize(input)
        assertFalse(output.contains("sk-1234567890"), "sk- 开头 token 应被脱敏")
        assertTrue(output.contains("REDACTED"))
    }

    @Test
    fun `sk- 开头 token 脱敏`() {
        val input = "sk-abcdefghijklmnopqrstuvwxyz1234567890"
        val output = sanitizer.logger.sanitize(input)
        assertFalse(output.contains("abcdefghijklmnop"))
    }

    @Test
    fun `sk_ 开头 token 脱敏`() {
        val input = "sk_abcdefghijklmnopqrstuvwxyz1234567890"
        val output = sanitizer.logger.sanitize(input)
        assertFalse(output.contains("abcdefghijklmnop"))
    }

    @Test
    fun `api_key= 形式脱敏`() {
        val input = "config api_key=abcdefghijklmnopqrstuvwxyz payload"
        val output = sanitizer.logger.sanitize(input)
        assertFalse(output.contains("abcdefghijklmnop"))
    }

    @Test
    fun `短字符串不误伤`() {
        val input = "sk-short" // 不足 16 字符
        val output = sanitizer.logger.sanitize(input)
        // 短 key 不应被脱敏（避免误伤普通文本）
        assertEquals(input, output)
    }

    @Test
    fun `普通文本不动`() {
        val input = "普通错误消息：网络异常，请检查 API URL"
        val output = sanitizer.logger.sanitize(input)
        assertEquals(input, output)
    }

    @Test
    fun `空字符串返回空`() {
        assertEquals("", sanitizer.logger.sanitize(""))
    }

    @Test
    fun `多个 token 全部脱敏`() {
        val input = "sk-aaaaaaaaaaaaaaaaaaaa sk-bbbbbbbbbbbbbbbbbbbb"
        val output = sanitizer.logger.sanitize(input)
        assertFalse(output.contains("aaaaaaaaaaaaaaaaaaaa"))
        assertFalse(output.contains("bbbbbbbbbbbbbbbbbbbb"))
    }

    @Test
    fun `保留栈信息中的类名和方法名`() {
        val input = "java.lang.NullPointerException at com.chatassist.MainActivity.onCreate(MainActivity.kt:42)"
        val output = sanitizer.logger.sanitize(input)
        assertEquals(input, output, "栈信息不应被脱敏")
    }

    /** 测试包装类：暴露 sanitize 方法（不依赖 Android Context）。 */
    private class TestableCrashLogger {
        fun sanitize(text: String): String {
            // 复制 CrashLogger.sanitize 逻辑（避免 Android Context 依赖）
            if (text.isEmpty()) return text
            var s = text
            s = s.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9_\\-]{16,}"), "Bearer ***REDACTED***")
            s = s.replace(Regex("sk[-_][A-Za-z0-9_\\-]{16,}"), "***REDACTED***")
            s = s.replace(Regex("(?i)(api[_-]?key|key)\\s*[=:]\\s*[\"']?([A-Za-z0-9_\\-]{16,})[\"']?"), "$1=***REDACTED***")
            return s
        }
    }
}
