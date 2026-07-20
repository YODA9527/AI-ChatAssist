package com.chatassist.data.local

import com.chatassist.data.local.dao.UsageDao
import com.chatassist.data.local.entity.UsageSummary as EntitySummary
import com.chatassist.domain.model.UsageRecord
import com.chatassist.domain.model.UsageStatus
import com.chatassist.domain.model.UsageSummary
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * UsageRepositoryImpl 单测（v2.2 §13.3）：测实体 ↔ domain 映射，用假 DAO 避开 Room。
 */
class UsageRepositoryTest {

    private fun repoWithSummary(): Pair<UsageRepositoryImpl, UsageDao> {
        val dao = mockk<UsageDao>(relaxed = true)
        every { dao.observeSummary(any(), any()) } returns flowOf(
            EntitySummary(
                callCount = 10,
                promptTokens = 100,
                completionTokens = 200,
                totalTokens = 300,
                costMicro = 400,
                successCount = 8,
                failCount = 2,
            ),
        )
        return UsageRepositoryImpl(dao) to dao
    }

    @Test
    fun `observeSummary 映射字段正确`() = runTest {
        val (repo, _) = repoWithSummary()
        val s: UsageSummary = repo.observeSummary(0L, 1L).first()
        assertEquals(10, s.callCount)
        assertEquals(100, s.promptTokens)
        assertEquals(200, s.completionTokens)
        assertEquals(300, s.totalTokens)
        assertEquals(400L, s.costMicro)
        assertEquals(8, s.successCount)
        assertEquals(2, s.failCount)
        assertEquals(400.0 / 1_000_000 / 10, s.avgCostYuan)
    }

    @Test
    fun `record 映射写入实体 status 为枚举名`() = runTest {
        val (repo, dao) = repoWithSummary()
        repo.record(
            UsageRecord(
                createdAt = 123L,
                provider = "deepseek",
                model = "deepseek-chat",
                promptTokens = 10,
                completionTokens = 20,
                totalTokens = 30,
                cachedTokens = 5,
                costMicro = 123L,
                targetPackage = "com.x",
                status = UsageStatus.SUCCESS,
            ),
        )
        coVerify {
            dao.insert(
                match {
                    it.status == "SUCCESS" && it.cachedTokens == 5 &&
                        it.costMicro == 123L && it.provider == "deepseek"
                },
            )
        }
    }
}
