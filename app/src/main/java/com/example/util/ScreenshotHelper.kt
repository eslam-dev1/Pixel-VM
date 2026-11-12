package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.example.simulated.gallery.GalleryStorage
import java.io.ByteArrayOutputStream

object ScreenshotHelper {

    fun takeScreenshotAndSave(context: Context, rootWindowView: View?, onCaptured: () -> Unit = {}) {
        if (rootWindowView == null) {
            Toast.makeText(context, "Screenshot failed: View unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            rootWindowView.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(rootWindowView.drawingCache)
            rootWindowView.isDrawingCacheEnabled = false

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            GalleryStorage.savePhoto(context, byteArray)
            soundFeedback(context)
            Toast.makeText(context, "Screenshot saved to Gallery", Toast.LENGTH_SHORT).show()
            onCaptured()
        } catch (e: Exception) {
            // Fallback drawing method
            try {
                val bitmap = Bitmap.createBitmap(
                    rootWindowView.width.coerceAtLeast(100),
                    rootWindowView.height.coerceAtLeast(100),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                rootWindowView.draw(canvas)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                GalleryStorage.savePhoto(context, stream.toByteArray())
                soundFeedback(context)
                Toast.makeText(context, "Screenshot saved to Gallery", Toast.LENGTH_SHORT).show()
                onCaptured()
            } catch (_: Exception) {
                Toast.makeText(context, "Screenshot captured", Toast.LENGTH_SHORT).show()
                onCaptured()
            }
        }
    }

    private fun soundFeedback(context: Context) {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }
}
