package my.receipt.tracker

import my.receipt.tracker.components.ExpenseAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import my.receipt.tracker.components.Expense
import my.receipt.tracker.components.ExpenseExporter
import java.io.File
import java.io.FileReader

class ViewExpensesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var expensesAdapter: ExpenseAdapter
    private lateinit var expenseExporter: ExpenseExporter

    // Register the activity result launcher for editing an expense
    private val editExpenseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadExpenses() // Reload expenses after editing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        // Initialize the exporter
        expenseExporter = ExpenseExporter(this)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "View Expenses"

        val backButton = findViewById<Button>(R.id.btnBack)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Add export button
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
            editExpenseLauncher.launch(intent) // Use the new API here
        }
        recyclerView.adapter = expensesAdapter
    }
}
