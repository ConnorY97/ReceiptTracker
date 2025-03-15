package my.receipt.tracker

import ExpenseAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader

class ViewExpensesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var expensesAdapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        var backButton = findViewById<Button>(R.id.btnBack)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        loadExpenses()
    }

    private fun loadExpenses() {
        val file = File(filesDir, "expenses.json")
        if (!file.exists()) return

        val expenses: List<Expense> = try {
            val reader = FileReader(file)
            Gson().fromJson(reader, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        expensesAdapter = ExpenseAdapter(expenses.map { expense ->
            Expense(expense.id, expense.description, expense.amount, expense.date, "", "")
        }) { selectedExpense ->
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("expense", Gson().toJson(selectedExpense))
            startActivityForResult(intent, REQUEST_EDIT_EXPENSE)
        }
        recyclerView.adapter = expensesAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_EXPENSE && resultCode == Activity.RESULT_OK) {
            loadExpenses()
        }
    }

    companion object {
        private const val REQUEST_EDIT_EXPENSE = 1
    }
}
