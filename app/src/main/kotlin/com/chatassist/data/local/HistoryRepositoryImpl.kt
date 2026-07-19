package com.chatassist.data.local

import com.chatassist.data.local.dao.HistoryDao
import com.chatassist.data.local.entity.HistoryEntity
import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.HistoryRecord
import com.chatassist.domain.model.HistoryRecordInput
import com.chatassist.domain.model.TokenUsage
import com.chatassist.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史记录仓库实现（模块 09）。
 *
 * 消息列表 / Prompt 以 JSON 落库（kotlinx.serialization），读出时失败兜底为空列表，
 * 不影响主链路。计费 cost 透传（v2.2 计费模块接手前恒为 0.0）。
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao,
) : HistoryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun record(input: HistoryRecordInput): Long {
        val entity = HistoryEntity(
            capturedAt = System.currentTimeMillis(),
            targetPackage = input.targetPackage,
            messagesJson = json.encodeToString(ListSerializer(ChatMessage.serializer()), input.messages),
            promptJson = input.prompt?.let { json.encodeToString(BuiltPrompt.serializer(), it) }.orEmpty(),
            responseText = input.responseText,
            promptTokens = input.usage.promptTokens,
            completionTokens = input.usage.completionTokens,
            totalTokens = input.usage.totalTokens,
            cost = input.cost,
        )
        return dao.insert(entity)
    }

    override fun observeAll(): Flow<List<HistoryRecord>> =
        dao.observeAll().map { list -> list.map(::toDomain) }

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun cleanup(olderThan: Long, keepLast: Int) {
        if (keepLast > 0) dao.deleteExceptLatest(keepLast)
        if (olderThan > 0) dao.deleteOlderThan(keepLast, olderThan)
    }

    private fun toDomain(e: HistoryEntity): HistoryRecord = HistoryRecord(
        id = e.id,
        capturedAt = e.capturedAt,
        targetPackage = e.targetPackage,
        messages = runCatching { json.decodeFromString<List<ChatMessage>>(e.messagesJson) }
            .getOrElse { emptyList() },
        responseText = e.responseText,
        usage = TokenUsage.of(e.promptTokens, e.completionTokens, e.totalTokens),
        cost = e.cost,
    )
}
