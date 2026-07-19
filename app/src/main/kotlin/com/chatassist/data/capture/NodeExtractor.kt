package com.chatassist.data.capture

import android.content.res.Resources
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.parser.ChatParser

/**
 * 节点树抽象，便于纯函数遍历 + 单测（不依赖 Android 框架状态）。
 */
interface ChatNode {
    val text: CharSequence?
    val bounds: Rect?
    val childCount: Int
    fun childAt(index: Int): ChatNode?
}

/**
 * [AccessibilityNodeInfo] 适配成 [ChatNode]。
 */
class AccessibilityChatNode(private val node: AccessibilityNodeInfo) : ChatNode {
    override val text: CharSequence? get() = node.text
    override val bounds: Rect?
        get() {
            val r = Rect()
            node.getBoundsInScreen(r)
            return r.takeIf { it.width() > 0 && it.height() > 0 }
        }
    override val childCount: Int get() = node.childCount
    override fun childAt(index: Int): ChatNode? =
        node.getChild(index)?.let { AccessibilityChatNode(it) }
}

/**
 * 纯函数：遍历节点树，抽取可见聊天文本并解析成结构化 [ChatMessage]。
 *
 * v0.6 接入 [ChatParser]：在遍历时按节点 bounds 算出水平中心，连同屏幕宽度交给解析器，
 * 一并补全 sender / isSelf / timestamp（系统消息单独标记）。
 * 不持有任何 Android 框架状态，便于 JUnit 单测（用 FakeNode 替代真节点；FakeNode 无 bounds，
 * 解析器退化成「未知位置」，仅靠文本内容判定系统/时间行）。
 *
 * @param minLen/maxLen 过滤掉太短（如单个标点）或异常长的文本，减少噪声。
 */
fun extractMessages(
    root: ChatNode,
    packageName: String,
    chatParser: ChatParser = ChatParser(),
    getScreenWidth: () -> Int = { Resources.getSystem().displayMetrics.widthPixels },
    minLen: Int = 1,
    maxLen: Int = 2000,
): List<ChatMessage> {
    val out = mutableListOf<ChatMessage>()
    val seen = mutableSetOf<String>()

    fun visit(node: ChatNode?) {
        node ?: return
        if (node.childCount == 0) {
            val text = node.text?.toString().orEmpty().trim()
            if (text.length in minLen..maxLen && text !in seen) {
                seen += text
                val b = node.bounds
                val screenWidth = if (b != null) getScreenWidth() else 0
                val centerX = b?.centerX()?.toInt() ?: -1
                out += chatParser.parse(text, centerX, screenWidth, packageName, CaptureMode.TEXT_ONLY, out.size)
            }
        } else {
            for (i in 0 until node.childCount) visit(node.childAt(i))
        }
    }

    visit(root)
    return out
}
