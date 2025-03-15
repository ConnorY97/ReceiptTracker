package my.receipt.tracker.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.receipt.tracker.Expense
import my.receipt.tracker.R

class ExpenseAdapter(private val expenses: List<Expense>, private val onExpenseClick: (Expense) -> Unit) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        //val ivReceipt: ImageView = itemView.findViewById(R.id.ivReceipt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.tvDescription.text = expense.description
        holder.tvAmount.text = "\$${expense.amount}"
        holder.tvDate.text = expense.date

        holder.itemView.setOnClickListener {
            onExpenseClick(expense)
        }
    }

    override fun getItemCount() = expenses.size
}