package com.visibleinfotech.e_ethanolUnloading.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UriHelper {

    private const val TAG = "UriHelper"
    private const val MAX_FILE_SIZE = 500 * 1024
    private const val COMPRESS_QUALITY_START = 90
    private const val COMPRESS_QUALITY_MIN = 40
    private const val MAX_IMAGE_DIMENSION = 2048

    fun createImageFile(context: Context): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "CANCEL_CHEQUE_${timeStamp}_"

            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

            Log.d(TAG, "Camera image created at: ${imageFile.absolutePath}")
            imageFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file: ${e.message}")
            null
        }
    }

    fun saveCameraImageToPermanentFile(context: Context, tempFile: File): Uri? {
        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, tempFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CancelCheque")
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Camera image saved permanently at URI: $uri")
                uri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving camera image: ${e.message}")
            null
        }
    }

    private fun compressImageToTargetSize(
        context: Context,
        uri: Uri,
        maxSizeKB: Int = 500
    ): File? {
        return try {
            Log.d(TAG, "=== STARTING IMAGE COMPRESSION ===")
            Log.d(TAG, "Target size: ${maxSizeKB}KB")

            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                return null
            }

            // Step 1: Decode with inSampleSize to reduce dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            Log.d(TAG, "Original dimensions: ${originalWidth}x${originalHeight}")

            // Step 2: Calculate sample size to reduce dimensions
            var sampleSize = 1
            val maxDimension = MAX_IMAGE_DIMENSION

            while ((originalWidth / sampleSize > maxDimension || originalHeight / sampleSize > maxDimension) &&
                sampleSize < 8) {
                sampleSize *= 2
            }

            Log.d(TAG, "Sample size: $sampleSize")

            // Step 3: Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Reduces memory usage
            }

            val inputStream2 = contentResolver.openInputStream(uri)
            var bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2?.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return null
            }

            Log.d(TAG, "Decoded dimensions: ${bitmap.width}x${bitmap.height}")

            // Step 4: Compress with quality
            var quality = COMPRESS_QUALITY_START
            val byteArrayOutputStream = ByteArrayOutputStream()
            var currentSizeBytes: Long

            // Try different quality levels
            do {
                byteArrayOutputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                currentSizeBytes = byteArrayOutputStream.size().toLong()
                Log.d(TAG, "Quality: $quality%, Size: ${currentSizeBytes / 1024}KB")

                if (currentSizeBytes > MAX_FILE_SIZE && quality > COMPRESS_QUALITY_MIN) {
                    quality -= 10
                } else {
                    break
                }
            } while (quality >= COMPRESS_QUALITY_MIN)

            // If still too large, try reducing dimensions further
            if (currentSizeBytes > MAX_FILE_SIZE) {
                Log.d(TAG, "Still too large after quality compression. Reducing dimensions...")
                // Reduce dimensions by 20% increments
                var scaleFactor = 0.8f
                var scaledBitmap: Bitmap? = bitmap

                while (currentSizeBytes > MAX_FILE_SIZE && scaleFactor > 0.3f && scaledBitmap != null) {
                    val newWidth = (scaledBitmap.width * scaleFactor).toInt()
                    val newHeight = (scaledBitmap.height * scaleFactor).toInt()

                    if (newWidth < 100 || newHeight < 100) break

                    val scaled = Bitmap.createScaledBitmap(scaledBitmap, newWidth, newHeight, true)
                    if (scaled != scaledBitmap) {
                        scaledBitmap.recycle()
                    }
                    scaledBitmap = scaled

                    byteArrayOutputStream.reset()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                    currentSizeBytes = byteArrayOutputStream.size().toLong()
                    Log.d(TAG, "Scaled to ${newWidth}x${newHeight}, Size: ${currentSizeBytes / 1024}KB")

                    scaleFactor -= 0.1f
                }

                if (scaledBitmap != null && scaledBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = scaledBitmap
                }
            }

            // Step 5: Save compressed image to file
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "compressed_${timeStamp}_${System.currentTimeMillis()}.jpg"
            val compressedFile = File(context.cacheDir, fileName)

            FileOutputStream(compressedFile).use { outputStream ->
                outputStream.write(byteArrayOutputStream.toByteArray())
                outputStream.flush()
            }

            // Clean up bitmap
            bitmap?.recycle()
            byteArrayOutputStream.close()

            Log.d(TAG, "✅ Compression complete!")
            Log.d(TAG, "Final file size: ${compressedFile.length() / 1024}KB")
            Log.d(TAG, "Final file path: ${compressedFile.absolutePath}")

            compressedFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            null
        }
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            Log.d(TAG, "Getting file from URI: $uri")

            // First, try to get the original file
            val contentResolver = context.contentResolver
            val fileName = getFileName(contentResolver, uri)

            Log.d(TAG, "Original filename: $fileName")

            // Check if it's a content URI or file URI
            val inputStream = when (uri.scheme) {
                "content" -> contentResolver.openInputStream(uri)
                "file" -> uri.path?.let { File(it).inputStream() }
                else -> null
            }

            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                return null
            }

            // Check file size
            val tempFile = createTempFileFromStream(context, inputStream, fileName)
            inputStream.close()

            if (tempFile == null) {
                Log.e(TAG, "Failed to create temp file")
                return null
            }

            val fileSizeKB = tempFile.length() / 1024
            Log.d(TAG, "Original file size: ${fileSizeKB}KB")

            // If file is larger than 500KB, compress it
            if (tempFile.length() > MAX_FILE_SIZE) {
                Log.d(TAG, "File size exceeds ${MAX_FILE_SIZE / 1024}KB, compressing...")

                // Get URI from temp file
                val fileUri = Uri.fromFile(tempFile)

                // Compress the image
                val compressedFile = compressImageToTargetSize(context, fileUri, 500)

                // Delete temp file if compression successful
                if (compressedFile != null && compressedFile.exists()) {
                    tempFile.delete()
                    Log.d(TAG, "Original temp file deleted")
                    return compressedFile
                } else {
                    Log.w(TAG, "Compression failed, using original file")
                    return tempFile
                }
            } else {
                Log.d(TAG, "File already under ${MAX_FILE_SIZE / 1024}KB, no compression needed")
                return tempFile
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting file from URI: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    private fun createTempFileFromStream(context: Context, inputStream: java.io.InputStream, originalFileName: String): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val uniqueFileName = "image_${timeStamp}_${System.currentTimeMillis()}.jpg"
            val tempFile = File(context.cacheDir, uniqueFileName)

            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            if (tempFile.length() == 0L) {
                Log.e(TAG, "Created file has 0 bytes!")
                return null
            }

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file: ${e.message}", e)
            null
        }
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String {
        var fileName = "image.jpg"

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                        Log.d(TAG, "Retrieved filename from cursor: $fileName")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting filename: ${e.message}")
        }

        return fileName
    }

    fun isFileValid(file: File?): Boolean {
        return file != null && file.exists() && file.isFile && file.length() > 0
    }

    fun getFileSizeKB(file: File): Long {
        return if (file.exists()) file.length() / 1024 else 0
    }

    fun compressExistingFile(context: Context, file: File, targetSizeKB: Int = 500): File? {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${file.absolutePath}")
            return null
        }

        if (file.length() <= targetSizeKB * 1024) {
            Log.d(TAG, "File already within target size: ${file.length() / 1024}KB")
            return file
        }

        val uri = Uri.fromFile(file)
        return compressImageToTargetSize(context, uri, targetSizeKB)
    }
}