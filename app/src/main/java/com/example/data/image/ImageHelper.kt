package com.example.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageHelper(private val context: Context) {

    companion object {
        private const val TAG = "ImageHelper"
        private const val IMAGES_DIR = "ticket_images"
        private const val MAX_IMAGE_DIMENSION = 2048
        private const val JPEG_QUALITY = 85
    }

    /**
     * Creates a temporary file and its corresponding content URI using FileProvider
     * for camera capture.
     */
    fun createCameraImageUri(): Pair<Uri, File> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File.createTempFile(
            "IMG_${timeStamp}_",
            ".jpg",
            storageDir
        )
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Pair(uri, file)
    }

    /**
     * Imports and copies an image from a content URI (gallery, photo picker, or camera)
     * into internal app storage, optionally resizing to prevent memory issues and optimize AI processing.
     */
    fun copyAndOptimizeUri(uri: Uri, targetFileNamePrefix: String = "ticket"): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for URI: $uri")
                return null
            }

            // Decode image bounds first to inspect dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight

            while (w > MAX_IMAGE_DIMENSION || h > MAX_IMAGE_DIMENSION) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            // Reopen stream to decode actual bitmap
            val secondStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            secondStream.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                return null
            }

            // Save to internal storage directory
            val imagesDir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val targetFile = File(imagesDir, "${targetFileNamePrefix}_${timeStamp}.jpg")

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()

            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying/optimizing image URI", e)
            null
        }
    }

    /**
     * Reads image file to byte array for sending to Gemini multimodal API
     */
    fun getImageBytes(file: File): ByteArray? {
        return try {
            if (file.exists() && file.length() > 0) {
                file.readBytes()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading image bytes", e)
            null
        }
    }

    /**
     * Detects MIME type from file extension
     */
    fun getImageMimeType(file: File): String {
        return when (file.extension.lowercase(Locale.ROOT)) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }

    /**
     * Copies an image into an Obsidian vault attachments directory.
     * Returns the relative path or file reference for Obsidian Markdown embedding: `![[filename.jpg]]`.
     */
    fun copyToVaultAttachments(sourceFile: File, vaultRoot: File): String? {
        return try {
            val attachmentsDir = File(vaultRoot, "Anexos").apply { mkdirs() }
            val destFile = File(attachmentsDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            destFile.name
        } catch (e: Exception) {
            Log.e(TAG, "Error copying image to vault attachments", e)
            null
        }
    }
}
