package my.receipt.tracker

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.util.*

class CreateExpenseActivity : AppCompatActivity() {
    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var btnCaptureReceipt: Button
    private lateinit var btnSelectScreenshot: Button
    private lateinit var btnSaveExpense: Button
    private lateinit var ivReceipt: ImageView
    private lateinit var ivScreenshot: ImageView

    private var selectedDate: String = ""
    private var receiptImagePath: String? = null
    private var screenshotImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_expense)

        etDescription = findViewById(R.id.etDescription)
        etAmount = findViewById(R.id.etAmount)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnCaptureReceipt = findViewById(R.id.btnCaptureReceipt)
        btnSelectScreenshot = findViewById(R.id.btnSelectScreenshot)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        ivReceipt = findViewById(R.id.ivReceipt)
        ivScreenshot = findViewById(R.id.ivScreenshot)

        btnSelectDate.setOnClickListener { showDatePicker() }
        btnCaptureReceipt.setOnClickListener { captureReceipt() }
        btnSelectScreenshot.setOnClickListener { selectScreenshot() }
        btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            btnSelectDate.text = selectedDate
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun captureReceipt() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun selectScreenshot() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        receiptImagePath = saveImageToInternalStorage(it, "receipt_${System.currentTimeMillis()}.jpg")
                        ivReceipt.setImageBitmap(it)
                        ivReceipt.visibility = ImageView.VISIBLE
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri: Uri? = data?.data
                    selectedImageUri?.let {
                        ivScreenshot.setImageURI(it)
                        ivScreenshot.visibility = ImageView.VISIBLE
                        screenshotImagePath = it.toString()
                    }
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, fileName: String): String {
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return file.absolutePath
    }

    private fun saveExpense() {
        val description = etDescription.text.toString()
        val amount = etAmount.text.toString().toDoubleOrNull()

        if (description.isBlank() || amount == null || selectedDate.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val newExpense = Expense(
            id = UUID.randomUUID().toString(), // Generate a unique ID
            description = description,
            amount = amount,
            date = selectedDate,
            receiptImagePath = receiptImagePath,
            screenshotImagePath = screenshotImagePath // Make sure this variable is set
        )

        val expenses = loadExpenses().toMutableList()
        expenses.add(newExpense)
        saveExpensesToFile(expenses)

        Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show()
        finish()
    }



    private fun loadExpenses(): List<Expense> {
        val file = File(filesDir, EXPENSES_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val reader = FileReader(file)
            Gson().fromJson(reader, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveExpensesToFile(expenses: List<Expense>) {
        val file = File(filesDir, EXPENSES_FILE)
        val writer = FileWriter(file)
        Gson().toJson(expenses, writer)
        writer.flush()
        writer.close()
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private const val EXPENSES_FILE = "expenses.json"
    }
}

// Expense Data Model
data class Expense(
    var description: String,
    var amount: Double,
    var date: String,
    var receiptImagePath: String?,
    var screenshotImagePath: String?,
    val id: String = UUID.randomUUID().toString()
)
