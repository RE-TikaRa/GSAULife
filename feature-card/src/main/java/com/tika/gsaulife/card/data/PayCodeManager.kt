package com.tika.gsaulife.card.data

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PayCodeManager {
    private data class CardKey(val openid: String, val cardId: String)

    private data class Outcome(
        val completedAt: Long,
        val result: PayCodeRepository.Result,
        val account: Account?
    )

    private val repository = PayCodeRepository()
    private val refreshMutexes = ConcurrentHashMap<CardKey, Mutex>()
    private val outcomes = ConcurrentHashMap<CardKey, Outcome>()

    suspend fun refresh(context: Context, account: Account): PayCodeRepository.Result {
        val key = CardKey(account.openid, account.cardId)
        val invokedAt = System.nanoTime()
        return refreshMutexes.computeIfAbsent(key) { Mutex() }.withLock {
            outcomes[key]?.takeIf { it.completedAt >= invokedAt }?.let { outcome ->
                outcome.account?.let { account.copyFrom(it) }
                return@withLock outcome.result
            }

            val store = AccountStore.get(context)
            val requestedAt = System.currentTimeMillis()
            val result = repository.fetch(account.openid, account.cardId)
            if (result is PayCodeRepository.Result.Ok) {
                store.list().firstOrNull { it.sameCard(account) }?.let { account.copyFrom(it) }
                account.cachedCode = result.code
                account.cachedAt = requestedAt
                if (result.name.isNotBlank()) account.name = result.name
                if (result.cardNo.isNotBlank()) account.cardNo = result.cardNo
                if (result.balance.isNotBlank()) account.balance = result.balance
                store.update(account)
            }
            outcomes[key] = Outcome(
                completedAt = System.nanoTime(),
                result = result,
                account = if (result is PayCodeRepository.Result.Ok) account.copy() else null
            )
            result
        }
    }

    suspend fun refreshCurrent(context: Context): PayCodeRepository.Result {
        val account = AccountStore.get(context).current()
            ?: return PayCodeRepository.Result.Error("无校园卡")
        return refresh(context, account)
    }

    private fun Account.copyFrom(source: Account) {
        name = source.name
        cardNo = source.cardNo
        balance = source.balance
        cachedCode = source.cachedCode
        cachedAt = source.cachedAt
        alias = source.alias
    }
}
