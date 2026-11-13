package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import com.example.simulated.gallery.GalleryPhoto
import com.example.simulated.gallery.GalleryStorage

object ScreenshotHelper {

    fun captureBitmap(rootWindowView: View?): Bitmap? {
        if (rootWindowView == null) return null
        return try {
            val width = rootWindowView.width.coerceAtLeast(100)
            val height = rootWindowView.height.coerceAtLeast(100)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootWindowView.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun saveBitmapWithFeedback(context: Context, bitmap: Bitmap): GalleryPhoto? {
        val saved = try {
            GalleryStorage.saveBitmap(context, bitmap, prefix = "PixelMarkup")
        } catch (_: Exception) {
            null
        }

        // Also mirror to shared MediaStore Pictures/PixelSim on API 29+ (zero permissions required)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, saved?.name ?: "PixelSim_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PixelSim")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } catch (_: Exception) {}
        }

        soundFeedback(context)
        Toast.makeText(context, "Screenshot saved to Gallery", Toast.LENGTH_SHORT).show()
        return saved
    }

    fun takeScreenshotAndSave(context: Context, rootWindowView: View?, onCaptured: () -> Unit = {}) {
        val bitmap = captureBitmap(rootWindowView)
        if (bitmap == null) {
            Toast.makeText(context, "Screenshot failed: View unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        saveBitmapWithFeedback(context, bitmap)
        onCaptured()
    }

    private fun soundFeedback(context: Context) {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }
}
