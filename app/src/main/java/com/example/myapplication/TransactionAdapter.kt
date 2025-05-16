package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var transactionList: MutableList<Transaction>,
    private val currencyType: String, // ✅ passed from Activity
    private val updateCallback: () -> Unit,
    private val onDeleteRequest: (Int) -> Unit,
    private val onEditRequest: (Int) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.tvTitle)
        val categoryText: TextView = view.findViewById(R.id.tvCategory)
        val dateText: TextView = view.findViewById(R.id.tvDate)
        val amountText: TextView = view.findViewById(R.id.tvAmount)
        val deleteBtn: ImageView = view.findViewById(R.id.ivDelete)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        holder.titleText.text = transaction.title
        holder.categoryText.text = "Category: ${transaction.category}"
        holder.dateText.text = transaction.date

        val amountFormatted = String.format("%.2f", transaction.amount.toDoubleOrNull() ?: 0.0)
        holder.amountText.text = "$currencyType $amountFormatted"

        val context = holder.amountText.context
        val color = if (transaction.type == "Income") {
            context.getColor(R.color.green)
        } else {
            context.getColor(R.color.red)
        }
        holder.amountText.setTextColor(color)

        holder.deleteBtn.setOnClickListener {
            onDeleteRequest(position)
        }

        val editBtn: ImageView = holder.itemView.findViewById(R.id.ivEdit)
        editBtn.setOnClickListener {
            onEditRequest(position)
        }
    }

    override fun getItemCount(): Int = transactionList.size

    fun updateData(newList: List<Transaction>) {
        transactionList = newList.toMutableList()
        notifyDataSetChanged()
    }
}
