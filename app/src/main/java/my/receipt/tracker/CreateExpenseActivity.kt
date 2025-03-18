package my.receipt.tracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import my.receipt.tracker.components.Expense
import my.receipt.tracker.utils.CameraHelper
import my.receipt.tracker.utils.GalleryHelper

class CreateExpenseActivity : AppCompatActivity() {
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var etUSAmount: EditText
    private lateinit var etAUSAmount: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var btnCaptureReceipt: Button
    private lateinit var btnSelectScreenshot: Button
    private lateinit var btnSaveExpense: Button
    private lateinit var ivReceipt: ImageView
    private lateinit var ivScreenshot: ImageView

    private lateinit var cameraHelper: CameraHelper
    private lateinit var galleryHelperReceipt: GalleryHelper
    private lateinit var galleryHelperScreenshot: GalleryHelper

    private var selectedDate: String = ""
    private var receiptImagePath: String? = null
    private var screenshotImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_expense)

        // Initialize UI components
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        etUSAmount = findViewById(R.id.etUSAmount)
        etAUSAmount = findViewById(R.id.etAUSAmount)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnCaptureReceipt = findViewById(R.id.btnCaptureReceipt)
        btnSelectScreenshot = findViewById(R.id.btnSelectScreenshot)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        ivReceipt = findViewById(R.id.ivReceipt)
        ivScreenshot = findViewById(R.id.ivScreenshot)

        // Initialize helper classes
        cameraHelper = CameraHelper(this, takePictureContract)
        galleryHelperReceipt = GalleryHelper(selectReceiptContract)
        galleryHelperScreenshot = GalleryHelper(selectScreenshotContract)

        // Set event listeners
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnCaptureReceipt.setOnClickListener { cameraHelper.launchCamera() }
        btnSelectScreenshot.setOnClickListener { galleryHelperScreenshot.launchGallery() }
        btnSaveExpense.setOnClickListener {
            if (validateFields()) {
                saveExpense()
            }
        }
    }

    // Activity Result Contracts
    private val takePictureContract = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraHelper.displayImage(ivReceipt)
            receiptImagePath = cameraHelper.imageUri.toString()
        }
    }

    private val selectReceiptContract = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        galleryHelperReceipt.displayImage(uri, ivReceipt)
        receiptImagePath = galleryHelperReceipt.selectedImageUri.toString()
    }

    private val selectScreenshotContract = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        galleryHelperScreenshot.displayImage(uri, ivScreenshot)
        screenshotImagePath = galleryHelperScreenshot.selectedImageUri.toString()
    }

    // Show date picker
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

    // Validation
    private fun validateFields(): Boolean {
        val errorMessage = getMissingFieldMessage()
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun getMissingFieldMessage(): String? {
        return when {
            etDescription.text.toString().isBlank() -> "Description is required."
            etLocation.text.toString().isBlank() -> "Location is required."
            etUSAmount.text.toString().isBlank() -> "US Amount is required."
            etUSAmount.text.toString().toDoubleOrNull() == null -> "US Amount must be a valid number."
            etAUSAmount.text.toString().isBlank() -> "AUS Amount is required."
            etAUSAmount.text.toString().toDoubleOrNull() == null -> "AUS Amount must be a valid number."
            selectedDate.isBlank() -> "Date is required."
            receiptImagePath == null -> "Receipt image is required"
            screenshotImagePath == null -> "Bank screenshot is required"
            else -> null
        }
    }

    // Save the expense
    private fun saveExpense() {
        val description = etDescription.text.toString()
        val location = etLocation.text.toString()
        val usAmount = etUSAmount.text.toString().toDoubleOrNull() ?: return
        val ausAmount = etAUSAmount.text.toString().toDoubleOrNull() ?: return

        val newExpense = Expense(
            id = UUID.randomUUID().toString(),
            description = description,
            location = location,
            usAmount = usAmount,
            ausAumount = ausAmount,
            date = selectedDate,
            receiptImagePath = receiptImagePath,
            screenshotImagePath = screenshotImagePath
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
        FileWriter(file).use { writer ->
            Gson().toJson(expenses, writer)
        }
    }

    companion object {
        private const val EXPENSES_FILE = "expenses.json"
    }
}
