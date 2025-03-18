package my.receipt.tracker.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExpenseExporter(private val context: Context) {

    fun exportExpensesToZip(): File? {
        try {
            // Create timestamp for unique filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Create directory structure in cache
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val imagesDir = File(exportDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            // Load expenses
            val expenses = loadExpenses()
            if (expenses.isEmpty()) {
                Toast.makeText(context, "No expenses to export", Toast.LENGTH_SHORT).show()
                return null
            }

            // Create CSV file
            val csvFile = File(exportDir, "expenses.csv")
            createCsvFile(csvFile, expenses, imagesDir)

            // Create readme file with instructions
            val readmeFile = File(exportDir, "README.txt")
            createReadmeFile(readmeFile)

            // Create ZIP file
            val zipFile = File(context.cacheDir, "expenses_export_$timestamp.zip")
            createZipFile(zipFile, exportDir)

            // Clean up temporary files
            exportDir.deleteRecursively()

            return zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun loadExpenses(): List<Expense> {
        val file = File(context.filesDir, "expenses.json")
        if (!file.exists()) return emptyList()

        return try {
            val reader = FileReader(file)
            Gson().fromJson(reader, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun createCsvFile(csvFile: File, expenses: List<Expense>, imagesDir: File) {
        FileWriter(csvFile).use { writer ->
            // Write CSV header
            writer.append("ID,Description,Location,US Amount, Aus Amount,Date,Receipt Image,Screenshot Image\n")

            // Write expense data
            expenses.forEach { expense ->
                val receiptFilename = copyImageToExportDir(expense.receiptImagePath, imagesDir, expense.description, "receipt")
                val screenshotFilename = copyImageToExportDir(expense.screenshotImagePath, imagesDir, expense.description, "screenshot")

                writer.append("${expense.id},")
                writer.append("\"${expense.description.replace("\"", "\"\"")}\",")
                writer.append("\"${expense.location.replace("\"", "\"\"")}\",")
                writer.append("${expense.usAmount},")
                writer.append("${expense.ausAumount},")
                writer.append("${expense.date},")
                writer.append("${receiptFilename ?: ""},")
                writer.append("${screenshotFilename ?: ""}\n")
            }
        }
    }

    private fun copyImageToExportDir(imagePath: String?, imagesDir: File, description: String, type: String): String? {
        if (imagePath == null || imagePath.isEmpty()) return null

        try {
            val sourceFile = File(imagePath)
            if (!sourceFile.exists()) return null

            // Create a safe filename based on description
            val safeDescription = sanitizeFilename(description)

            // Create a meaningful filename with extension
            val extension = if (imagePath.contains(".")) {
                imagePath.substring(imagePath.lastIndexOf("."))
            } else {
                ".jpg" // Default extension
            }

            // Add date to ensure uniqueness
            val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val destinationFilename = "${safeDescription}_${type}_${timestamp}${extension}"
            val destinationFile = File(imagesDir, destinationFilename)

            sourceFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return "images/$destinationFilename"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper method to sanitize filenames
    private fun sanitizeFilename(input: String): String {
        // Replace invalid filename characters with underscores
        val sanitized = input.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .replace("\\s+".toRegex(), "_") // Replace spaces with underscores

        // Limit length to avoid too long filenames
        val maxLength = 50
        return if (sanitized.length > maxLength) {
            sanitized.substring(0, maxLength)
        } else {
            sanitized
        }
    }

    private fun createReadmeFile(readmeFile: File) {
        FileWriter(readmeFile).use { writer ->
            writer.append("Expense Tracker Export\n")
            writer.append("======================\n\n")
            writer.append("This ZIP file contains:\n")
            writer.append("- expenses.csv: A CSV file containing all expense data\n")
            writer.append("- images/: A directory containing all receipt and screenshot images\n\n")
            writer.append("The CSV file contains references to the image files in the 'images' directory.\n")
            writer.append("You can open the CSV file with any spreadsheet software like Microsoft Excel or Google Sheets.\n\n")
            writer.append("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        }
    }

    private fun createZipFile(zipFile: File, sourceDir: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                val entryPath = file.toRelativeString(sourceDir)
                if (entryPath.isNotEmpty() && !file.isDirectory) {
                    val entry = ZipEntry(entryPath)
                    zos.putNextEntry(entry)

                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }

                    zos.closeEntry()
                }
            }
        }
    }

    fun shareExportedFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/zip"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(Intent.createChooser(shareIntent, "Share Expenses Export"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share export file", Toast.LENGTH_SHORT).show()
        }
    }
}