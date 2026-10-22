package com.lukeneedham.androiddock

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsRankedTest {

    @Test
    fun ordersByRankHighestFirst() {
        val rank = mapOf("a" to 10L, "b" to 30L, "c" to 20L)
        assertEquals(listOf("b", "c", "a"), RecentApps.ranked(rank, rank, emptyMap(), emptySet()))
    }

    @Test
    fun leavesOutExcludedPackages() {
        val rank = mapOf("a" to 10L, "launcher" to 99L, "hidden" to 50L)
        assertEquals(listOf("a"), RecentApps.ranked(rank, rank, emptyMap(), setOf("launcher", "hidden")))
    }

    @Test
    fun dismissedAppStaysOutUntilUsedAgain() {
        val rank = mapOf("a" to 10L, "b" to 20L)
        val usedAt = mapOf("a" to 100L, "b" to 300L)
        val dismissed = mapOf("a" to 200L, "b" to 200L)
        assertEquals(listOf("b"), RecentApps.ranked(rank, usedAt, dismissed, emptySet()))
    }

    @Test
    fun mostUsedOrderIsIndependentOfLastUsed() {
        val foreground = mapOf("heavy" to 5_000L, "light" to 100L)
        val usedAt = mapOf("heavy" to 1L, "light" to 999L)
        assertEquals(listOf("heavy", "light"), RecentApps.ranked(foreground, usedAt, emptyMap(), emptySet()))
    }

    @Test
    fun appWithNoLastUsedTimeIsLeftOut() {
        val rank = mapOf("a" to 1L)
        assertEquals(emptyList<String>(), RecentApps.ranked(rank, emptyMap(), emptyMap(), emptySet()))
    }
}
