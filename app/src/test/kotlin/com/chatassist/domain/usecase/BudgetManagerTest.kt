package com.chatassist.domain.usecase

import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.BudgetConfig
import com.chatassist.domain.model.BudgetStatus
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.UsageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * BudgetManager 单测（v2.2 §13.3.7）。
 */
class BudgetManagerTest {

    private fun manager(budget: BudgetConfig, usedMicro: Long): BudgetManager {
        val configRepo = mockk<ConfigRepository>()
        every { configRepo.observeAll() } returns flowOf(AppConfig(budget = budget))
        val usageRepo = mockk<UsageRepository>()
        io.mockk.coEvery { usageRepo.totalCost(any(), any()) } returns usedMicro
        return BudgetManager(usageRepo, configRepo)
    }

    @Test
    fun `未设预算返回 OK`() = runTest {
        assertEquals(BudgetStatus.OK, manager(BudgetConfig(monthlyBudgetYuan = 0.0), 99_000_000L).checkBudget())
    }

    @Test
    fun `预算内返回 OK`() = runTest {
        // 5 元 / 10 元 = 0.5
        assertEquals(BudgetStatus.OK, manager(BudgetConfig(monthlyBudgetYuan = 10.0), 5_000_000L).checkBudget())
    }

    @Test
    fun `达80%返回 NEAR_LIMIT`() = runTest {
        // 8.5 / 10 = 0.85
        assertEquals(BudgetStatus.NEAR_LIMIT, manager(BudgetConfig(monthlyBudgetYuan = 10.0), 8_500_000L).checkBudget())
    }

    @Test
    fun `超100%未拦截返回 EXCEEDED_WARN`() = runTest {
        // 11 / 10 = 1.1
        assertEquals(
            BudgetStatus.EXCEEDED_WARN,
            manager(BudgetConfig(monthlyBudgetYuan = 10.0, blockAtExceed = false), 11_000_000L).checkBudget(),
        )
    }

    @Test
    fun `超100%且拦截返回 EXCEEDED_BLOCK`() = runTest {
        assertEquals(
            BudgetStatus.EXCEEDED_BLOCK,
            manager(BudgetConfig(monthlyBudgetYuan = 10.0, blockAtExceed = true), 11_000_000L).checkBudget(),
        )
    }
}
