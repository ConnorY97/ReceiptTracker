package my.receipt.tracker.utils

import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher

class GalleryHelper(
    private val selectImageLauncher: ActivityResultLauncher<String>
) {
    var selectedImageUri: Uri? = null

    fun launchGallery() {
        selectImageLauncher.launch("image/*")
    }

    fun displayImage(uri: Uri?, imageView: ImageView) {
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
            imageView.visibility = ImageView.VISIBLE
        }
    }
}
