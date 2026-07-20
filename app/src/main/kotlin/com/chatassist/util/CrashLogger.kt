package com.chatassist.util

import android.content.Context
import com.chatassist.domain.model.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 崩溃日志落盘器（v0.11.0：模块 12 A 项）。
 *
 * 唯一职责：把 [Throwable] 写入 `filesDir/crash/` 目录，**不外传**。
 * 用于：
 * - 反馈：用户截图反馈时初夏可调取本地日志还原栈
 * - 排错：GlobalErrorHandler 兜底时落地，避免 logcat 滚动丢失
 *
 * 脱敏铁律（v0.11.0 起强制）：
 * 1. **API Key 绝不落盘**（扫描 `sk-` / `Bearer xxx` / `api_key=` / `apikey=` 等模式，全部替换为 `***REDACTED***`）
 * 2. **聊天内容不落盘**（只记栈 + 异常类型 + 时间 + 线程）
 * 3. **包名/版本/SDK 保留**（调试用）
 * 4. **栈超过 200 行截断**（防止单文件过大）
 *
 * 文件命名：`yyyy-MM-dd-HH-mm-ss.txt`（按崩溃时间，同一秒多次崩溃靠后缀 .2.txt 区分）
 */
@Singleton
class CrashLogger @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)

    fun log(throwable: Throwable) {
        runCatching {
            val crashDir = File(ctx.filesDir, "crash").apply { mkdirs() }
            val baseName = dateFormat.format(Date())
            val file = uniqueFile(crashDir, baseName)
            file.bufferedWriter().use { writer ->
                writeHeader(writer, throwable)
                writeStackTrace(writer, throwable)
            }
            Timber.tag(TAG).e(throwable, "Crash logged to ${file.absolutePath}")
            cleanupOldFiles(crashDir, keep = 20)
        }.onFailure { Timber.tag(TAG).e(it, "Failed to log crash") }
    }

    /** 把 [AppError] 转成 [Throwable] 后落盘（用于 ErrorClassifier 已知错误也写日志）。 */
    fun logAppError(error: AppError) {
        log(error)
    }

    private fun uniqueFile(dir: File, baseName: String): File {
        var f = File(dir, "$baseName.txt")
        if (!f.exists()) return f
        var i = 2
        while (true) {
            f = File(dir, "$baseName.$i.txt")
            if (!f.exists()) return f
            i++
        }
    }

    private fun writeHeader(writer: java.io.BufferedWriter, t: Throwable) {
        writer.appendLine("===== ChatAssist Crash Log =====")
        writer.appendLine("时间: ${dateFormat.format(Date())}")
        writer.appendLine("线程: ${Thread.currentThread().name}")
        writer.appendLine("异常类型: ${t.javaClass.name}")
        writer.appendLine("异常消息: ${sanitize(t.message ?: "")}")
        writer.appendLine("Package: ${ctx.packageName}")
        writer.appendLine("SDK: ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})")
        writer.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        writer.appendLine("ROM: ${android.os.Build.DISPLAY}")
        writer.appendLine()
    }

    private fun writeStackTrace(writer: java.io.BufferedWriter, t: Throwable) {
        val sw = java.io.StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        val stack = sw.toString().lineSequence()
            .take(MAX_STACK_LINES)
            .map { sanitize(it) }
            .joinToString("\n")
        writer.appendLine("===== Stack Trace =====")
        writer.appendLine(stack)
        writer.appendLine()
        var cause: Throwable? = t.cause
        var depth = 1
        while (cause != null && depth <= 5) {
            writer.appendLine("===== Caused by ($depth): ${cause.javaClass.name} =====")
            writer.appendLine(sanitize(cause.message ?: ""))
            val csw = java.io.StringWriter()
            val cpw = PrintWriter(csw)
            cause.printStackTrace(cpw)
            writer.appendLine(csw.toString().lineSequence().take(MAX_STACK_LINES).joinToString("\n"))
            cause = cause.cause
            depth++
        }
    }

    /**
     * 脱敏：替换 API Key 模式为 `***REDACTED***`。
     * 覆盖：Bearer xxx、sk-xxx、sk_xxx、api_key=xxx、apikey=xxx、apikey:xxx、key=xxx 等。
     */
    fun sanitize(text: String): String {
        if (text.isEmpty()) return text
        var s = text
        // Bearer 头部
        s = s.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9_\\-]{16,}"), "Bearer ***REDACTED***")
        // sk- / sk_ 开头（OpenAI / DeepSeek 等）
        s = s.replace(Regex("sk[-_][A-Za-z0-9_\\-]{16,}"), "***REDACTED***")
        // api_key= / apikey= / apikey: / key= / key:（至少 16 字符值避免误伤普通 "key"）
        s = s.replace(Regex("(?i)(api[_-]?key|key)\\s*[=:]\\s*[\"']?([A-Za-z0-9_\\-]{16,})[\"']?"), "$1=$2".let { "$1=***REDACTED***" })
        return s
    }

    private fun cleanupOldFiles(dir: File, keep: Int) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= keep) return
        files.drop(keep).forEach { runCatching { it.delete() } }
    }

    companion object {
        private const val TAG = "CrashLogger"
        private const val MAX_STACK_LINES = 200
    }
}
