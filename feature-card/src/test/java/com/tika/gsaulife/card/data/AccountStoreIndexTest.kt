package com.tika.gsaulife.card.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountStoreIndexTest {
    @Test
    fun `clampIndex 空列表返回 -1`() {
        assertEquals(-1, AccountStore.clampIndex(0, 0))
        assertEquals(-1, AccountStore.clampIndex(3, 0))
    }

    @Test
    fun `clampIndex 越界夹到合法范围`() {
        assertEquals(2, AccountStore.clampIndex(5, 3))
        assertEquals(0, AccountStore.clampIndex(-1, 3))
        assertEquals(1, AccountStore.clampIndex(1, 3))
    }

    @Test
    fun `nextIndex 环形前进`() {
        assertEquals(1, AccountStore.nextIndex(0, 3))
        assertEquals(2, AccountStore.nextIndex(1, 3))
        assertEquals(0, AccountStore.nextIndex(2, 3))
    }

    @Test
    fun `nextIndex 单卡停在原地`() {
        assertEquals(0, AccountStore.nextIndex(0, 1))
    }

    @Test
    fun `nextIndex 空列表返回 -1`() {
        assertEquals(-1, AccountStore.nextIndex(0, 0))
    }

    @Test
    fun `删当前卡之前的卡 选中索引前移`() {
        assertEquals(1, AccountStore.indexAfterRemoval(current = 2, removed = 0, sizeAfter = 3))
    }

    @Test
    fun `删当前卡之后的卡 选中索引不动`() {
        assertEquals(1, AccountStore.indexAfterRemoval(current = 1, removed = 3, sizeAfter = 3))
    }

    @Test
    fun `删当前选中的卡 索引夹到新末尾`() {
        assertEquals(1, AccountStore.indexAfterRemoval(current = 2, removed = 2, sizeAfter = 2))
    }

    @Test
    fun `删到空列表 索引归零`() {
        assertEquals(0, AccountStore.indexAfterRemoval(current = 0, removed = 0, sizeAfter = 0))
    }

    @Test
    fun `替换拒绝另一位置已存在的校园卡身份`() {
        val first = Account("first", "9", alias = "第一张")
        val second = Account("second", "9", alias = "第二张")
        val accounts = mutableListOf(first, second)

        assertFalse(AccountStore.replaceIn(accounts, 1, second, Account("first", "9")))
        assertEquals(second, accounts[1])
    }

    @Test
    fun `替换接受当前位置原身份`() {
        val accounts = mutableListOf(Account("first", "9", alias = "旧备注"))
        val replacement = Account("first", "9", alias = "新备注")

        assertTrue(AccountStore.replaceIn(accounts, 0, accounts[0], replacement))
        assertEquals(replacement, accounts[0])
    }

    @Test
    fun `重新绑定期间原卡被删除 不覆盖后续卡片`() {
        val original = Account("first", "9")
        val following = Account("second", "9")
        val accounts = mutableListOf(original, following)
        accounts.removeAt(0)

        assertFalse(AccountStore.replaceIn(accounts, 0, original, Account("third", "9")))
        assertEquals(listOf(following), accounts)
    }
}
