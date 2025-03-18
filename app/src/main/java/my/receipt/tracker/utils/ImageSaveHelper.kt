package my.receipt.tracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream

class ImageSaveHelper(private val context: Context) {

    fun saveImageToInternalStorage(uri: Uri, fileName: String, imageView: ImageView): String {
        return try {
            val bitmap = imageView.drawable?.toBitmap() ?: return uri.toString()

            // Save the bitmap to internal storage
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()  // Fallback to original URI in case of error
        }
    }
}