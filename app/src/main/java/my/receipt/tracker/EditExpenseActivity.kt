package my.receipt.tracker

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
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
            try {
                // Load the image into the ImageView
                Glide.with(this).load(uri).into(ivReceipt)

                // Store the URI temporarily
                newReceiptPath = uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load receipt image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val screenshotImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Load the image into the ImageView
                Glide.with(this).load(uri).into(ivScreenshot)

                // Store the URI temporarily
                newScreenshotPath = uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load screenshot image", Toast.LENGTH_SHORT).show()
            }
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

    private fun saveImageToInternalStorage(uri: Uri, fileName: String): String {
        try {
            // Create a bitmap from the ImageView that has loaded the URI
            val drawable = when {
                newReceiptPath == uri.toString() -> ivReceipt.drawable
                else -> ivScreenshot.drawable
            }

            val bitmap = drawable?.toBitmap() ?: return uri.toString()

            // Save the bitmap to a file
            val file = File(filesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's any error, return the original URI as fallback
            return uri.toString()
        }
    }

    private fun saveExpense() {
        expense?.let {
            // Update expense details
            it.description = etDescription.text.toString()
            it.location = etLocation.text.toString()
            it.usAmount = etUSAmount.text.toString().toDouble()
            it.ausAumount = etAUSAmount.text.toString().toDouble()
            it.date = etDate.text.toString()

            // Save the newly selected images to internal storage if they've been changed
            if (newReceiptPath != null) {
                try {
                    val receiptFileName = "receipt_${System.currentTimeMillis()}.jpg"
                    val uri = Uri.parse(newReceiptPath)
                    it.receiptImagePath = saveImageToInternalStorage(uri, receiptFileName)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to save receipt image", Toast.LENGTH_SHORT).show()
                }
            }

            if (newScreenshotPath != null) {
                try {
                    val screenshotFileName = "screenshot_${System.currentTimeMillis()}.jpg"
                    val uri = Uri.parse(newScreenshotPath)
                    it.screenshotImagePath = saveImageToInternalStorage(uri, screenshotFileName)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to save screenshot image", Toast.LENGTH_SHORT).show()
                }
            }

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

        Toast.makeText(this, "Expense updated successfully", Toast.LENGTH_SHORT).show()
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

        Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }
}