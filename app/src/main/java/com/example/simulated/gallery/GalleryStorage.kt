package com.example.simulated.gallery

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GalleryPhoto(
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val file: File? = null
)

object GalleryStorage {

    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, "simulated_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAllPhotos(context: Context): List<GalleryPhoto> {
        val dir = getPhotosDirectory(context)
        val files = dir.listFiles { f ->
            f.isFile && (f.name.endsWith(".jpg", ignoreCase = true) || f.name.endsWith(".png", ignoreCase = true))
        } ?: emptyArray()

        return files.sortedByDescending { it.lastModified() }.map { file ->
            GalleryPhoto(
                uri = Uri.fromFile(file),
                name = file.name,
                dateAdded = file.lastModified(),
                file = file
            )
        }
    }

    fun savePhoto(context: Context, bytes: ByteArray): GalleryPhoto {
        val dir = getPhotosDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }

        return GalleryPhoto(
            uri = Uri.fromFile(file),
            name = fileName,
            dateAdded = file.lastModified(),
            file = file
        )
    }

    fun saveImportedPhoto(context: Context, sourceUri: Uri): GalleryPhoto? {
        return try {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return null
            savePhoto(context, bytes)
        } catch (_: Exception) {
            null
        }
    }

    fun deletePhoto(photo: GalleryPhoto): Boolean {
        return photo.file?.delete() ?: false
    }
}
