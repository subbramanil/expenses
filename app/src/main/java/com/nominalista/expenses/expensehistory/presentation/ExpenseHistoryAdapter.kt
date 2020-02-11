package com.nominalista.expenses.expensehistory.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.nominalista.expenses.R

class ExpenseHistoryAdapter : ListAdapter<ExpenseItemModel, ExpenseHistoryItemHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseHistoryItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            EXPENSE_ITEM_TYPE -> ExpenseItemHolder(itemView)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: ExpenseHistoryItemHolder, position: Int) {
        val itemModel = getItem(position)
        when {
            (holder is ExpenseItemHolder && itemModel is ExpenseItemModel) -> holder.bind(itemModel)
        }
    }

    override fun onViewRecycled(holder: ExpenseHistoryItemHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is ExpenseItemHolder -> holder.recycle()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ExpenseItemModel -> EXPENSE_ITEM_TYPE
            else -> super.getItemViewType(position)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ExpenseItemModel>() {

        override fun areItemsTheSame(
                oldItem: ExpenseItemModel,
                newItem: ExpenseItemModel
        ): Boolean {
            return oldItem.expense.id == newItem.expense.id
        }

        override fun areContentsTheSame(
                oldItem: ExpenseItemModel,
                newItem: ExpenseItemModel
        ): Boolean {
            return oldItem.expense == newItem.expense
        }
    }

    companion object {

        private const val EXPENSE_ITEM_TYPE = R.layout.item_expense
    }
}