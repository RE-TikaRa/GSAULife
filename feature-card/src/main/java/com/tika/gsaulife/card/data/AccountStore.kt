package com.tika.gsaulife.card.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

class AccountStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()

    fun list(): MutableList<Account> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return mutableListOf()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return mutableListOf()
        return (0 until array.length())
            .mapNotNull { array.optJSONObject(it)?.let(Account::fromJson) }
            .toMutableList()
    }

    fun save(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach { array.put(it.toJson()) }
        prefs.edit { putString(KEY_ACCOUNTS, array.toString()) }
    }

    fun currentIndex(): Int = clampIndex(prefs.getInt(KEY_CURRENT, 0), list().size)

    fun setCurrentIndex(index: Int) {
        prefs.edit { putInt(KEY_CURRENT, index) }
    }

    fun current(): Account? {
        val accounts = list()
        return accounts.getOrNull(clampIndex(prefs.getInt(KEY_CURRENT, 0), accounts.size))
    }

    fun switchToNext(): Account? = synchronized(lock) {
        val accounts = list()
        if (accounts.isEmpty()) return@synchronized null
        val next = nextIndex(currentIndex(), accounts.size)
        setCurrentIndex(next)
        accounts[next]
    }

    fun add(account: Account) = synchronized(lock) {
        val accounts = list()
        val existing = accounts.indexOfFirst { it.sameCard(account) }
        if (existing >= 0) accounts[existing] = account else accounts.add(account)
        save(accounts)
    }

    fun removeAt(index: Int) = synchronized(lock) {
        val accounts = list()
        if (index !in accounts.indices) return@synchronized
        accounts.removeAt(index)
        save(accounts)
        setCurrentIndex(indexAfterRemoval(prefs.getInt(KEY_CURRENT, 0), index, accounts.size))
    }

    fun replaceAt(index: Int, original: Account, account: Account): Boolean = synchronized(lock) {
        val accounts = list()
        if (!replaceIn(accounts, index, original, account)) return@synchronized false
        save(accounts)
        true
    }

    fun update(account: Account) = synchronized(lock) {
        val accounts = list()
        val index = accounts.indexOfFirst { it.sameCard(account) }
        if (index >= 0) {
            accounts[index] = account
            save(accounts)
        }
    }

    fun clearCachedCodes() = synchronized(lock) {
        val accounts = list()
        accounts.forEach {
            it.cachedCode = ""
            it.cachedAt = 0L
        }
        save(accounts)
    }

    companion object {
        private const val PREFS = "gsaulife_card"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_CURRENT = "current_index"

        fun clampIndex(stored: Int, size: Int): Int =
            if (size == 0) -1 else stored.coerceIn(0, size - 1)

        fun nextIndex(current: Int, size: Int): Int =
            if (size == 0) -1 else (current + 1) % size

        fun indexAfterRemoval(current: Int, removed: Int, sizeAfter: Int): Int = when {
            sizeAfter == 0 -> 0
            removed < current -> current - 1
            else -> current.coerceAtMost(sizeAfter - 1)
        }

        internal fun replaceIn(
            accounts: MutableList<Account>,
            index: Int,
            original: Account,
            account: Account
        ): Boolean {
            if (index !in accounts.indices) return false
            if (!accounts[index].sameCard(original)) return false
            val duplicate = accounts.withIndex().any { (otherIndex, existing) ->
                otherIndex != index && existing.sameCard(account)
            }
            if (duplicate) return false
            accounts[index] = account
            return true
        }

        @Volatile
        private var instance: AccountStore? = null

        fun get(context: Context): AccountStore =
            instance ?: synchronized(this) {
                instance ?: AccountStore(context.applicationContext).also { instance = it }
            }
    }
}
