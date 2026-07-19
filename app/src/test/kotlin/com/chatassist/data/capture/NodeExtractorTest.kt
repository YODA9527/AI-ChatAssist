package com.chatassist.data.capture

import com.chatassist.domain.model.CaptureMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NodeExtractorTest {

    private class FakeNode(
        override val text: CharSequence? = null,
        override val bounds: android.graphics.Rect? = null,
        private val children: List<ChatNode> = emptyList(),
    ) : ChatNode {
        override val childCount: Int get() = children.size
        override fun childAt(index: Int): ChatNode? = children.getOrNull(index)
    }

    @Test
    fun `extracts leaf text nodes and skips blanks`() {
        val root = FakeNode(
            children = listOf(
                FakeNode(text = "你好呀"),
                FakeNode(text = "在吗"),
                FakeNode(text = "   "),
                FakeNode(text = ""),
                FakeNode(children = listOf(FakeNode(text = "嵌套消息"))),
            ),
        )
        val msgs = extractMessages(root, "com.tencent.mm")
        assertEquals(3, msgs.size)
        assertTrue(msgs.any { it.content == "嵌套消息" })
        assertTrue(msgs.all { it.source == CaptureMode.TEXT_ONLY })
    }

    @Test
    fun `dedups identical text`() {
        val root = FakeNode(
            children = listOf(
                FakeNode(text = "重复"),
                FakeNode(text = "重复"),
            ),
        )
        val msgs = extractMessages(root, "com.tencent.mm")
        assertEquals(1, msgs.size)
    }
}
