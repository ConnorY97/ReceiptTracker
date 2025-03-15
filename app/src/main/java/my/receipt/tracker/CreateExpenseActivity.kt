package my.receipt.tracker

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
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
    private var tempCameraImageUri: Uri? = null
    private val CAMERA_PERMISSION_CODE = 100

    private fun captureReceipt() {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            // Permission is already granted, proceed with capturing image
            launchCamera()
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = File(filesDir, "Camera")
            storageDir.mkdirs()
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                imageFile
            )

            tempCameraImageUri = uri
            takePictureContract.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // Register activity result contracts
    // For camera - use TakePicture contract which works with Uri
    private val takePictureContract = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraImageUri?.let { uri ->
                // Load uri into ImageView
                ivReceipt.setImageURI(uri)
                ivReceipt.visibility = ImageView.VISIBLE

                // Save the URI path
                receiptImagePath = uri.toString()
            }
        }
    }

    // For gallery selection
    private val selectImageContract = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Set image URI directly
                ivScreenshot.setImageURI(it)
                ivScreenshot.visibility = ImageView.VISIBLE

                // Save the URI for later use
                screenshotImagePath = it.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

//    private fun captureReceipt() {
//        try {
//            // Create a file to save the image
//            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            val imageFileName = "JPEG_${timeStamp}_"
//            val storageDir = File(filesDir, "Camera")
//            storageDir.mkdirs()
//            val imageFile = File.createTempFile(
//                imageFileName,
//                ".jpg",
//                storageDir
//            )
//
//            // Get a URI for the file using FileProvider
//            val uri = FileProvider.getUriForFile(
//                this,
//                "${applicationContext.packageName}.fileprovider",
//                imageFile
//            )
//
//            // Store the URI for later use
//            tempCameraImageUri = uri
//
//            // Launch camera with the URI - now we know it's non-null
//            takePictureContract.launch(uri)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun selectScreenshot() {
        selectImageContract.launch("image/*")
    }

    private fun saveImageToInternalStorage(uri: Uri, fileName: String): String {
        try {
            // Create a bitmap from the ImageView that has loaded the URI
            val drawable = when {
                uri.toString() == tempCameraImageUri.toString() -> ivReceipt.drawable
                else -> ivScreenshot.drawable
            }

            val bitmap = drawable?.toBitmap()
            if (bitmap == null) {
                return uri.toString()
            }

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
        val description = etDescription.text.toString()
        val amount = etAmount.text.toString().toDoubleOrNull()

        if (description.isBlank() || amount == null || selectedDate.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (receiptImagePath == null) {
            Toast.makeText(this, "Please capture a receipt image", Toast.LENGTH_SHORT).show()
            return
        }

        // Save the bitmap images to internal storage for permanent storage
        var finalReceiptPath = receiptImagePath
        tempCameraImageUri?.let { uri ->
            val receiptFileName = "receipt_${System.currentTimeMillis()}.jpg"
            finalReceiptPath = saveImageToInternalStorage(uri, receiptFileName)
        }

        var finalScreenshotPath = screenshotImagePath
        if (screenshotImagePath != null) {
            try {
                val screenshotFileName = "screenshot_${System.currentTimeMillis()}.jpg"
                val uri = Uri.parse(screenshotImagePath)
                if (uri != null) {
                    finalScreenshotPath = saveImageToInternalStorage(uri, screenshotFileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep the original path if there's any error
            }
        }

        val newExpense = Expense(
            id = UUID.randomUUID().toString(),
            description = description,
            amount = amount,
            date = selectedDate,
            receiptImagePath = finalReceiptPath,
            screenshotImagePath = finalScreenshotPath
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

// Expense Data Model
data class Expense(
    var description: String,
    var amount: Double,
    var date: String,
    var receiptImagePath: String?,
    var screenshotImagePath: String?,
    val id: String = UUID.randomUUID().toString()
)