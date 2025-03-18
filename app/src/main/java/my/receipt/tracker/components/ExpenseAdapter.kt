package my.receipt.tracker.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import my.receipt.tracker.R

class ExpenseAdapter(private val expenses: List<Expense>, private val onExpenseClick: (Expense) -> Unit) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        var tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.tvDescription.text = expense.description
        holder.tvLocation.text = expense.location
        holder.tvDate.text = expense.date
        holder.itemView.setOnClickListener {
            onExpenseClick(expense)
        }
    }

    override fun getItemCount() = expenses.size
}