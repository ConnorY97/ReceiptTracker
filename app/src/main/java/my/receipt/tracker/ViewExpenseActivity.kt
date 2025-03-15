package my.receipt.tracker

import my.receipt.tracker.components.ExpenseAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import my.receipt.tracker.components.ExpenseExporter
import java.io.File
import java.io.FileReader

class ViewExpensesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var expensesAdapter: ExpenseAdapter
    private lateinit var expenseExporter: ExpenseExporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        // Initialize the exporter
        expenseExporter = ExpenseExporter(this)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "View Expenses"

        var backButton = findViewById<Button>(R.id.btnBack)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Add export button (if you don't want to use menu)
        val exportButton = findViewById<Button>(R.id.btnExport)
        exportButton.setOnClickListener {
            exportExpenses()
        }

        loadExpenses()
    }

    private fun exportExpenses() {
        Toast.makeText(this, "Preparing export...", Toast.LENGTH_SHORT).show()

        Thread {
            val exportFile = expenseExporter.exportExpensesToZip()

            runOnUiThread {
                if (exportFile != null) {
                    Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show()
                    expenseExporter.shareExportedFile(exportFile)
                } else {
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
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

        expensesAdapter = ExpenseAdapter(expenses) { selectedExpense ->
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