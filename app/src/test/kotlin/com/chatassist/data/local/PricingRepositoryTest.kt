package com.chatassist.data.local

import com.chatassist.data.local.dao.PricingDao
import com.chatassist.data.local.entity.PricingEntity
import com.chatassist.domain.model.Pricing
import com.chatassist.domain.model.TokenUsage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * PricingRepositoryImpl 单测（v2.2 §13.3.3）：测计价与映射，用假 DAO 避开 Room。
 */
class PricingRepositoryTest {

    private fun repo(): Pair<PricingRepositoryImpl, PricingDao> {
        val dao = mockk<PricingDao>(relaxed = true)
        coEvery { dao.get("deepseek::deepseek-chat") } returns PricingEntity(
            key = "deepseek::deepseek-chat",
            provider = "deepseek",
            model = "deepseek-chat",
            inputPricePerMToken = 1.0,
            outputPricePerMToken = 2.0,
            cachedPricePerMToken = 0.1,
            updatedAt = 0,
            isBuiltin = true,
        )
        return PricingRepositoryImpl(dao) to dao
    }

    @Test
    fun `calculateCost 按定价键匹配计价`() = runTest {
        val (pricingRepo, _) = repo()
        val cost = pricingRepo.calculateCost(
            TokenUsage.of(1_000_000, 1_000_000, 2_000_000),
            "deepseek-chat",
        )
        // 1.0 + 2.0 = 3.0 元 → 3_000_000 微元
        assertEquals(3_000_000L, cost)
    }

    @Test
    fun `calculateCost 无定价返回 0`() = runTest {
        val (pricingRepo, dao) = repo()
        coEvery { dao.get(any()) } returns null
        coEvery { dao.getByModel(any()) } returns null
        assertEquals(0L, pricingRepo.calculateCost(TokenUsage.of(1_000_000, 1_000_000, 2_000_000), "unknown-model"))
    }

    @Test
    fun `observeAll 映射为 domain`() = runTest {
        val (pricingRepo, dao) = repo()
        every { dao.observeAll() } returns flowOf(
            listOf(
                PricingEntity("deepseek::deepseek-chat", "deepseek", "deepseek-chat", 1.0, 2.0, 0.1, updatedAt = 0, isBuiltin = true),
            ),
        )
        val list: List<Pricing> = pricingRepo.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("deepseek-chat", list[0].model)
        assertEquals(1.0, list[0].inputPricePerMToken)
    }
}
