package com.tika.gsaulife.card.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tika.gsaulife.card.R
import com.tika.gsaulife.card.data.Account
import com.tika.gsaulife.card.databinding.CardItemAccountBinding

internal class AccountAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
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
            binding.cardItemName.text = account.displayName()
            binding.cardItemInfo.text = buildString {
                if (account.cardNo.isNotBlank()) append(account.cardNo)
                if (account.balance.isNotBlank()) {
                    if (isNotEmpty()) append("  ")
                    append(context.getString(R.string.card_balance, account.balance))
                }
            }
            binding.cardItemCurrent.visibility =
                if (item.current) View.VISIBLE else View.INVISIBLE
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(position)
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    false
                } else {
                    onLongClick(position)
                    true
                }
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
