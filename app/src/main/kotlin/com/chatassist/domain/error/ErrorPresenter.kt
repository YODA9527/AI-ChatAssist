package com.chatassist.domain.error

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.SuggestedAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错误展示器（v0.11.0：模块 12 A 项）。
 *
 * 把 [AppError] 转成 UI 直接可用的 [ErrorUiModel]：
 * - `title`：短标题（适合 Snackbar / Dialog header）
 * - `message`：详细文案（情感化、可读）
 * - `actionLabel`：操作按钮文案（"重试" / "去设置" / "检查网络" 等）
 * - `retryable`：是否显示"重试"按钮
 *
 * 设计原则：
 * - 兜底永远存在（任何 [ErrorCode] 都有 title/message，不会显示空白）
 * - 文案情感化（"出现未预期错误，已记录到本地日志" 而非 raw exception text）
 * - 不暴露 [Throwable.cause] 给用户（保护内部细节）
 *
 * UI 集成（v0.11.0+）：`MainViewModel` / `OverlayController` 用 `present(error)` 拿到 UI model 后渲染。
 */
@Singleton
class ErrorPresenter @Inject constructor() {

    fun present(error: AppError): ErrorUiModel {
        val title = titleOf(error.code)
        val message = messageOf(error)
        val action = actionOf(error)
        val actionLabel = actionLabelOf(action)
        return ErrorUiModel(
            title = title,
            message = message,
            action = action,
            actionLabel = actionLabel,
            retryable = error.retryable,
        )
    }

    private fun titleOf(code: ErrorCode): String = when (code) {
        ErrorCode.NETWORK, ErrorCode.DNS, ErrorCode.TIMEOUT -> "网络问题"
        ErrorCode.AUTH_401 -> "API Key 无效"
        ErrorCode.AUTH_403 -> "无权限访问"
        ErrorCode.RATE_LIMIT_429 -> "请求太频繁"
        ErrorCode.SERVER_5XX -> "服务异常"
        ErrorCode.OCR_LOW_CONFIDENCE -> "识别不确定"
        ErrorCode.PARSE_LOW_CONFIDENCE -> "解析不确定"
        ErrorCode.PERMISSION_DENIED -> "需要授权"
        ErrorCode.STORAGE_FULL -> "存储已满"
        ErrorCode.CONFIG_INVALID -> "配置无效"
        ErrorCode.UNCAUGHT -> "出错了"
        ErrorCode.UNKNOWN -> "未知错误"
    }

    private fun messageOf(error: AppError): String = when (error.code) {
        ErrorCode.NETWORK -> "网络连接异常，请检查 WiFi 或移动数据后再试。"
        ErrorCode.DNS -> "无法解析服务器地址，请检查「API URL」是否正确。"
        ErrorCode.AUTH_401 -> "API Key 无效或已过期，请在「设置 → API」中重新填写。"
        ErrorCode.AUTH_403 -> "当前 API Key 无权访问该模型，请尝试切换到其他模型。"
        ErrorCode.RATE_LIMIT_429 -> "请求过于频繁，已自动等待重试…"
        ErrorCode.SERVER_5XX -> "模型服务端异常，已自动重试 ${3 - 1} 次。稍后可在「历史」中重看。"
        ErrorCode.TIMEOUT -> "模型响应超时，可在「设置」中换更快的模型，或检查网络。"
        ErrorCode.OCR_LOW_CONFIDENCE -> "屏幕识别置信度较低，请检查屏幕内容是否清晰可读。"
        ErrorCode.PARSE_LOW_CONFIDENCE -> "对话内容解析不确定，请滚动屏幕重试或手动复制。"
        ErrorCode.PERMISSION_DENIED -> "权限被拒。请在「设置 → 权限引导」中开启所需权限。"
        ErrorCode.STORAGE_FULL -> "存储空间已满，请到「历史」页清理旧记录。"
        ErrorCode.CONFIG_INVALID -> "配置无效，请到「设置」检查 API URL / Key / 模型名。"
        ErrorCode.UNCAUGHT -> "出现未预期错误，已记录到本地日志，重启后仍异常请反馈开发者。"
        ErrorCode.UNKNOWN -> "未知错误，可尝试「重试」或重启 App。"
    }

    private fun actionOf(error: AppError): SuggestedAction? = when (error.code) {
        ErrorCode.AUTH_401, ErrorCode.AUTH_403, ErrorCode.CONFIG_INVALID -> SuggestedAction.CHECK_API_KEY
        ErrorCode.NETWORK, ErrorCode.DNS -> SuggestedAction.CHECK_NETWORK
        ErrorCode.PERMISSION_DENIED -> SuggestedAction.OPEN_SETTINGS
        ErrorCode.STORAGE_FULL -> SuggestedAction.OPEN_SETTINGS
        ErrorCode.UNCAUGHT -> SuggestedAction.CONTACT_SUPPORT
        // TIMEOUT/429/5xx 等：retryable 优先给「重试」按钮，code 提示的检查网络退到次选
        else -> if (error.retryable) SuggestedAction.RETRY else error.suggestedAction
    }

    private fun actionLabelOf(action: SuggestedAction?): String? = when (action) {
        SuggestedAction.RETRY -> "重试"
        SuggestedAction.CHECK_API_KEY -> "检查 Key"
        SuggestedAction.CHECK_NETWORK -> "检查网络"
        SuggestedAction.OPEN_SETTINGS -> "去设置"
        SuggestedAction.CONTACT_SUPPORT -> "反馈问题"
        null -> null
    }
}

/**
 * UI 层可用的错误展示模型。
 */
data class ErrorUiModel(
    val title: String,
    val message: String,
    val action: SuggestedAction?,
    val actionLabel: String?,
    val retryable: Boolean,
)
