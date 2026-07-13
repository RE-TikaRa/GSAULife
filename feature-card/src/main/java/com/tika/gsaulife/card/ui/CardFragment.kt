package com.tika.gsaulife.card.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tika.gsaulife.card.R
import com.tika.gsaulife.card.data.Account
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.data.LinkParser
import com.tika.gsaulife.card.data.PayCodeManager
import com.tika.gsaulife.card.data.PayCodePolicy
import com.tika.gsaulife.card.data.PayCodeRepository
import com.tika.gsaulife.card.databinding.CardFragmentBinding
import com.tika.gsaulife.card.qr.QrGenerator
import com.tika.gsaulife.card.widget.PayWidgetProvider
import com.tika.gsaulife.card.work.RefreshController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CardFragment : Fragment() {
    private var _binding: CardFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var store: AccountStore
    private lateinit var adapter: AccountAdapter
    private var refreshJob: Job? = null
    private var expiryJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = AccountStore.get(requireContext())
        adapter = AccountAdapter(
            onClick = ::selectAccount,
            onLongClick = ::showAccountMenu
        )
        binding.cardList.layoutManager = LinearLayoutManager(requireContext())
        binding.cardList.adapter = adapter
        binding.cardBtnAdd.setOnClickListener { showAddDialog() }
        binding.cardAddEmpty.setOnClickListener { showAddDialog() }
        binding.cardBtnRefresh.setOnClickListener { refreshNow() }
        binding.cardQr.setOnClickListener {
            if (store.current() != null) {
                startActivity(Intent(requireContext(), PayActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        RefreshController.enterPaymentScreen(requireContext())
        renderCurrent()
        adapter.submit(store.list(), store.currentIndex())
        startRefreshLoop()
    }

    override fun onPause() {
        refreshJob?.cancel()
        expiryJob?.cancel()
        RefreshController.leavePaymentScreen(requireContext())
        super.onPause()
    }

    override fun onDestroyView() {
        refreshJob?.cancel()
        expiryJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun renderCurrent() {
        val binding = _binding ?: return
        clearHintAction()
        val account = store.current()
        val hasAccounts = store.list().isNotEmpty()
        binding.cardQrContainer.visibility = if (account == null) View.GONE else View.VISIBLE
        binding.cardAddEmpty.visibility = if (account == null) View.VISIBLE else View.GONE
        binding.cardListTitle.visibility = if (hasAccounts) View.VISIBLE else View.GONE
        binding.cardList.visibility = if (hasAccounts) View.VISIBLE else View.GONE
        binding.cardBtnRefresh.isEnabled = account != null
        if (account == null) {
            expiryJob?.cancel()
            binding.cardName.setText(R.string.card_no_card)
            binding.cardBalance.visibility = View.GONE
            binding.cardQr.setImageDrawable(null)
            binding.cardHint.setText(R.string.card_add_hint)
            return
        }

        binding.cardName.text = account.displayName()
        showBalance(account.balance)
        if (account.hasFreshCode()) {
            binding.cardQr.setImageBitmap(QrGenerator.encode(account.cachedCode, QrGenerator.SIZE_CARD))
            binding.cardHint.setText(R.string.card_open_fullscreen)
            scheduleExpiry(account)
        } else {
            expiryJob?.cancel()
            binding.cardQr.setImageDrawable(null)
            binding.cardHint.setText(R.string.card_loading)
        }
    }

    private fun showBalance(balance: String) {
        val binding = _binding ?: return
        binding.cardBalance.visibility = if (balance.isBlank()) View.GONE else View.VISIBLE
        if (balance.isNotBlank()) {
            binding.cardBalance.text = getString(R.string.card_balance, balance)
        }
    }

    private fun startRefreshLoop() {
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                store.current()?.let { refreshAccount(it) }
                delay(PayCodePolicy.REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun refreshNow() {
        val account = store.current() ?: return
        _binding?.cardHint?.setText(R.string.card_loading)
        viewLifecycleOwner.lifecycleScope.launch { refreshAccount(account) }
    }

    private suspend fun refreshAccount(account: Account) {
        val result = PayCodeManager.refresh(requireContext(), account)
        val binding = _binding ?: return
        if (!account.sameCard(store.current())) return
        when (result) {
            is PayCodeRepository.Result.Ok -> {
                clearHintAction()
                binding.cardName.text = account.displayName()
                showBalance(account.balance)
                binding.cardQr.setImageBitmap(QrGenerator.encode(result.code, QrGenerator.SIZE_CARD))
                binding.cardHint.setText(R.string.card_open_fullscreen)
                scheduleExpiry(account)
                adapter.submit(store.list(), store.currentIndex())
            }
            PayCodeRepository.Result.Invalid -> {
                if (!account.hasFreshCode()) binding.cardQr.setImageDrawable(null)
                showInvalidHint(account)
            }
            is PayCodeRepository.Result.Error -> {
                if (!account.hasFreshCode()) binding.cardQr.setImageDrawable(null)
                binding.cardHint.text = getString(R.string.card_fetch_failed, result.message)
            }
        }
        PayWidgetProvider.refreshAll(requireContext())
    }

    private fun scheduleExpiry(account: Account) {
        expiryJob?.cancel()
        val remaining = account.cachedAt + PayCodePolicy.VALIDITY_MS - System.currentTimeMillis()
        expiryJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(remaining.coerceAtLeast(0L))
            val current = store.current() ?: return@launch
            if (!account.sameCard(current)) return@launch
            if (current.hasFreshCode()) {
                renderCurrent()
            } else {
                _binding?.cardQr?.setImageDrawable(null)
                _binding?.cardHint?.setText(R.string.card_loading)
            }
        }
    }

    private fun clearHintAction() {
        val binding = _binding ?: return
        binding.cardHint.setOnClickListener(null)
        binding.cardHint.isClickable = false
        binding.cardHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.card_text_hint))
    }

    private fun showInvalidHint(account: Account) {
        val binding = _binding ?: return
        binding.cardHint.setText(R.string.card_invalid)
        binding.cardHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.card_primary))
        binding.cardHint.isClickable = true
        binding.cardHint.setOnClickListener { showRebindDialog(account) }
    }

    private fun selectAccount(index: Int) {
        store.setCurrentIndex(index)
        renderCurrent()
        adapter.submit(store.list(), store.currentIndex())
        binding.root.smoothScrollTo(0, 0)
        startRefreshLoop()
        PayWidgetProvider.refreshAll(requireContext())
    }

    private fun showAccountMenu(index: Int) {
        val account = store.list().getOrNull(index) ?: return
        CardDialogs.menu(
            requireContext(),
            account.displayName(),
            arrayOf(
                getString(R.string.card_rename),
                getString(R.string.card_rebind),
                getString(R.string.card_delete)
            )
        ) { selected ->
            when (selected) {
                0 -> showRenameDialog(index)
                1 -> showRebindDialog(account)
                2 -> confirmDelete(index)
            }
        }
    }

    private fun showAddDialog() {
        CardDialogs.input(
            context = requireContext(),
            title = getString(R.string.card_add_title),
            message = getString(R.string.card_add_message),
            hint = getString(R.string.card_link_hint),
            positiveText = getString(R.string.card_add),
            onPositive = ::addFromLink
        )
    }

    private fun addFromLink(link: String) {
        val parsed = LinkParser.parse(link)
        if (parsed == null) {
            notice(R.string.card_link_missing)
            return
        }
        val account = Account(parsed.openid, parsed.cardId)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = PayCodeManager.refresh(requireContext(), account)) {
                is PayCodeRepository.Result.Ok -> {
                    store.add(account)
                    val index = store.list().indexOfFirst { it.sameCard(account) }
                    if (index >= 0) store.setCurrentIndex(index)
                    renderCurrent()
                    adapter.submit(store.list(), store.currentIndex())
                    PayWidgetProvider.refreshAll(requireContext())
                    CardDialogs.notice(binding.root, getString(R.string.card_added, account.displayName()))
                }
                PayCodeRepository.Result.Invalid -> notice(R.string.card_link_invalid)
                is PayCodeRepository.Result.Error ->
                    CardDialogs.notice(binding.root, getString(R.string.card_verify_failed, result.message))
            }
        }
    }

    private fun showRebindDialog(account: Account) {
        val index = store.list().indexOfFirst { it.sameCard(account) }
        if (index < 0) return
        CardDialogs.input(
            context = requireContext(),
            title = getString(R.string.card_rebind_title, account.displayName()),
            message = getString(R.string.card_rebind_message),
            hint = getString(R.string.card_link_hint),
            positiveText = getString(R.string.card_rebind),
            onPositive = { rebind(index, account, it) }
        )
    }

    private fun rebind(index: Int, original: Account, link: String) {
        val parsed = LinkParser.parse(link)
        if (parsed == null) {
            notice(R.string.card_link_missing)
            return
        }
        val account = Account(parsed.openid, parsed.cardId, alias = original.alias)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = PayCodeManager.refresh(requireContext(), account)) {
                is PayCodeRepository.Result.Ok -> {
                    if (!store.replaceAt(index, original, account)) {
                        notice(R.string.card_already_bound)
                        return@launch
                    }
                    store.setCurrentIndex(index)
                    renderCurrent()
                    adapter.submit(store.list(), store.currentIndex())
                    PayWidgetProvider.refreshAll(requireContext())
                    CardDialogs.notice(binding.root, getString(R.string.card_rebound, account.displayName()))
                }
                PayCodeRepository.Result.Invalid -> notice(R.string.card_link_invalid)
                is PayCodeRepository.Result.Error ->
                    CardDialogs.notice(binding.root, getString(R.string.card_verify_failed, result.message))
            }
        }
    }

    private fun showRenameDialog(index: Int) {
        val account = store.list().getOrNull(index) ?: return
        CardDialogs.input(
            context = requireContext(),
            title = getString(R.string.card_rename_title),
            message = getString(R.string.card_rename_message),
            hint = getString(R.string.card_alias_hint),
            positiveText = getString(R.string.card_save),
            initial = account.alias,
            onPositive = {
                store.update(account.copy(alias = it.trim()))
                renderCurrent()
                adapter.submit(store.list(), store.currentIndex())
                PayWidgetProvider.refreshAll(requireContext())
            }
        )
    }

    private fun confirmDelete(index: Int) {
        val account = store.list().getOrNull(index) ?: return
        CardDialogs.confirm(
            context = requireContext(),
            title = getString(R.string.card_delete_title),
            message = getString(R.string.card_delete_message, account.displayName()),
            positiveText = getString(R.string.card_delete),
            onPositive = {
                store.removeAt(index)
                renderCurrent()
                adapter.submit(store.list(), store.currentIndex())
                startRefreshLoop()
                PayWidgetProvider.refreshAll(requireContext())
            }
        )
    }

    private fun notice(message: Int) {
        _binding?.let { CardDialogs.notice(it.root, getString(message)) }
    }
}
