package com.tika.gsaulife.card.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tika.gsaulife.card.R
import com.tika.gsaulife.card.data.Account
import com.tika.gsaulife.card.databinding.CardItemAccountBinding

internal class AccountAdapter(
    private val onClick: (Int) -> Unit,
    private val onMenuClick: (Int) -> Unit
) : ListAdapter<AccountAdapter.Item, AccountAdapter.ViewHolder>(ItemCallback) {
    data class Item(val account: Account, val current: Boolean)

    fun submit(accounts: List<Account>, current: Int) {
        submitList(accounts.mapIndexed { index, account ->
            Item(account.copy(), index == current)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: CardItemAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            val account = item.account
            val context = binding.root.context
            val name = account.displayName()
            val details = buildList {
                if (account.cardNo.isNotBlank()) add(account.cardNo)
                if (account.balance.isNotBlank()) {
                    add(context.getString(R.string.card_balance, account.balance))
                }
            }
            val info = details.joinToString("  ")
            binding.cardItemName.text = name
            binding.cardItemInfo.text = info
            binding.cardItemInfo.visibility = if (info.isEmpty()) View.GONE else View.VISIBLE
            binding.cardItemCurrent.visibility =
                if (item.current) View.VISIBLE else View.INVISIBLE
            binding.root.contentDescription = (listOf(name) + details)
                .joinToString("，")
            ViewCompat.setStateDescription(
                binding.root,
                if (item.current) context.getString(R.string.card_current_state) else null
            )
            binding.cardItemMore.contentDescription = context.getString(
                R.string.card_manage_named,
                name
            )
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(position)
            }
            binding.cardItemMore.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onMenuClick(position)
            }
        }
    }

    private object ItemCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
            oldItem.account.sameCard(newItem.account)

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
            oldItem == newItem
    }
}
