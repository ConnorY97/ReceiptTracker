package my.receipt.tracker.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraHelper(
    private val context: Context,
    private val takePictureLauncher: ActivityResultLauncher<Uri>
) {
    var imageUri: Uri? = null

    fun launchCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = File(context.filesDir, "Camera")
        storageDir.mkdirs()

        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        takePictureLauncher.launch(imageUri!!)
    }

    fun displayImage(imageView: ImageView) {
        imageUri?.let { uri ->
            imageView.setImageURI(uri)
            imageView.visibility = ImageView.VISIBLE
        }
    }
}
