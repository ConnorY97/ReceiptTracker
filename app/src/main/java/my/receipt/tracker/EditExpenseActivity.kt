package my.receipt.tracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import com.google.gson.reflect.TypeToken

class EditExpenseActivity : AppCompatActivity() {
    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)

        etDescription = findViewById(R.id.etDescription)
        etAmount = findViewById(R.id.etAmount)
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
            etAmount.setText(it.amount.toString())
            etDate.setText(it.date)

            Glide.with(this).load(it.receiptImagePath).into(ivReceipt)
            Glide.with(this).load(it.screenshotImagePath).into(ivScreenshot)
        }

        btnChangeReceipt.setOnClickListener {
            // Logic to update receipt image
            pickImage(REQUEST_RECEIPT)
        }

        btnChangeScreenshot.setOnClickListener {
            // Logic to update bank screenshot
            pickImage(REQUEST_SCREENSHOT)
        }

        btnSave.setOnClickListener {
            saveExpense()
        }

        btnDelete.setOnClickListener {
            deleteExpense()
        }
    }

    private fun deleteExpense() {
        expense?.let {
            removeExpense(it)
        }
    }

    private fun pickImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data?.toString()
            when (requestCode) {
                REQUEST_RECEIPT -> {
                    newReceiptPath = selectedImageUri
                    Glide.with(this).load(newReceiptPath).into(ivReceipt)
                }
                REQUEST_SCREENSHOT -> {
                    newScreenshotPath = selectedImageUri
                    Glide.with(this).load(newScreenshotPath).into(ivScreenshot)
                }
            }
        }
    }

    private fun saveExpense() {
        expense?.let {
            it.description = etDescription.text.toString()
            it.amount = etAmount.text.toString().toDouble()
            it.date = etDate.text.toString()
            it.receiptImagePath = newReceiptPath ?: it.receiptImagePath
            it.screenshotImagePath = newScreenshotPath ?: it.screenshotImagePath
            updateExpense(it)
        }
    }

    companion object {
        private const val REQUEST_RECEIPT = 1001
        private const val REQUEST_SCREENSHOT = 1002
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

