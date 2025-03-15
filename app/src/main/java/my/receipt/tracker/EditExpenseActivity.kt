package my.receipt.tracker

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import com.google.gson.reflect.TypeToken

class EditExpenseActivity : AppCompatActivity() {
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var etUSAmount: EditText
    private lateinit var etAUSAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var ivReceipt: ImageView
    private lateinit var ivScreenshot: ImageView
    private lateinit var btnChangeReceipt: Button
    private lateinit var btnChangeScreenshot: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var expense: Expense? = null
    private var newReceiptPath: String? = null
    private var newScreenshotPath: String? = null

    // Define the activity result launchers
    private val receiptImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newReceiptPath = it.toString()
            Glide.with(this).load(newReceiptPath).into(ivReceipt)
        }
    }

    private val screenshotImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newScreenshotPath = it.toString()
            Glide.with(this).load(newScreenshotPath).into(ivScreenshot)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)

        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        etUSAmount = findViewById(R.id.etUSAmount)
        etAUSAmount = findViewById(R.id.etAUSAmount)
        etDate = findViewById(R.id.etDate)
        ivReceipt = findViewById(R.id.ivReceipt)
        ivScreenshot = findViewById(R.id.ivScreenshot)
        btnChangeReceipt = findViewById(R.id.btnChangeReceipt)
        btnChangeScreenshot = findViewById(R.id.btnChangeScreenshot)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        val expenseJson = intent.getStringExtra("expense")
        expense = Gson().fromJson(expenseJson, Expense::class.java)

        expense?.let {
            etDescription.setText(it.description)
            etLocation.setText(it.location)
            etUSAmount.setText(String.format(it.usAmount.toString()))
            etAUSAmount.setText(String.format(it.ausAumount.toString()))
            etDate.setText(it.date)

            Glide.with(this).load(it.receiptImagePath).into(ivReceipt)
            Glide.with(this).load(it.screenshotImagePath).into(ivScreenshot)
        }

        btnChangeReceipt.setOnClickListener {
            // Launch image picker for receipt
            receiptImageLauncher.launch("image/*")
        }

        btnChangeScreenshot.setOnClickListener {
            // Launch image picker for screenshot
            screenshotImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveExpense()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Yes") { _, _ ->
                deleteExpense()
            }
            .setNegativeButton("No", null) // Dismisses the dialog
            .show()
    }

    private fun deleteExpense() {
        expense?.let {
            removeExpense(it)
        }
    }

    private fun saveExpense() {
        expense?.let {
            it.description = etDescription.text.toString()
            it.location = etLocation.text.toString()
            it.usAmount = etUSAmount.text.toString().toDouble()
            it.ausAumount = etAUSAmount.text.toString().toDouble()
            it.date = etDate.text.toString()
            it.receiptImagePath = newReceiptPath ?: it.receiptImagePath
            it.screenshotImagePath = newScreenshotPath ?: it.screenshotImagePath
            updateExpense(it)
        }
    }

    private fun updateExpense(updatedExpense: Expense) {
        val file = File(filesDir, "expenses.json")
        if (!file.exists()) return

        val expenses: MutableList<Expense> = try {
            val reader = FileReader(file)
            Gson().fromJson(reader, object : TypeToken<MutableList<Expense>>() {}.type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        val index = expenses.indexOfFirst { it.id == updatedExpense.id }
        if (index != -1) {
            expenses[index] = updatedExpense
        }

        FileWriter(file).use { writer ->
            Gson().toJson(expenses, writer)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun removeExpense(expenseToRemove: Expense) {
        val file = File(filesDir, "expenses.json")
        if (!file.exists()) return

        val expenses: MutableList<Expense> = try {
            val reader = FileReader(file)
            Gson().fromJson(reader, object : TypeToken<MutableList<Expense>>() {}.type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        expenses.removeAll { it.id == expenseToRemove.id }

        FileWriter(file).use { writer ->
            Gson().toJson(expenses, writer)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }
}