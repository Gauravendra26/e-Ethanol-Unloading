package com.visibleinfotech.e_ethanolUnloading.utils


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit


object ImageUploadUtils {

    private const val TAG = "ImageUploadUtils"

    private const val MAX_WIDTH = 1280
    private const val MAX_HEIGHT = 960
    private const val MAX_FILE_SIZE = 200 * 1024
    private const val DEFAULT_QUALITY = 95
    private const val MIN_QUALITY = 60
    private const val QUALITY_STEP = 3
    private val random = Random()

    fun optimizeImage(imageFile: File): File {
        try {
            val originalSize = imageFile.length()
            Log.d(TAG, "Original size = ${originalSize / 1024} KB")

            if (originalSize <= MAX_FILE_SIZE) {
                Log.d(TAG, "Image already under ${MAX_FILE_SIZE / 1024} KB, skipping compression")
                return imageFile
            }

            // 2) Decode bounds first to compute inSampleSize
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, boundsOptions)

            var inSampleSize = 1
            while ((boundsOptions.outWidth / inSampleSize) > MAX_WIDTH * 2 &&
                (boundsOptions.outHeight / inSampleSize) > MAX_HEIGHT * 2
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return imageFile
            }

            // 3) Resize to target bounds
            val resizedBitmap = resizeBitmap(bitmap)

            // 4) Compress with decreasing quality until we hit the target
            var quality = DEFAULT_QUALITY
            var finalSize = originalSize

            while (quality >= MIN_QUALITY) {
                FileOutputStream(imageFile).use { fos ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }

                finalSize = imageFile.length()
                Log.d(TAG, "Quality = $quality, Size = ${finalSize / 1024} KB")

                if (finalSize <= MAX_FILE_SIZE) break
                quality -= QUALITY_STEP
            }

            // 5) Second pass — scale down 80% if still above target
            if (finalSize > MAX_FILE_SIZE) {
                Log.w(TAG, "Still > ${MAX_FILE_SIZE / 1024} KB at quality $MIN_QUALITY, resizing down")
                val smallerBitmap = scaleDown(resizedBitmap, 0.8f)
                var q2 = DEFAULT_QUALITY
                while (q2 >= MIN_QUALITY) {
                    FileOutputStream(imageFile).use { fos ->
                        smallerBitmap.compress(Bitmap.CompressFormat.JPEG, q2, fos)
                    }
                    finalSize = imageFile.length()
                    Log.d(TAG, "Pass 2 — Quality = $q2, Size = ${finalSize / 1024} KB")
                    if (finalSize <= MAX_FILE_SIZE) break
                    q2 -= QUALITY_STEP
                }
                if (smallerBitmap !== resizedBitmap) smallerBitmap.recycle()
            }

            // Cleanup
            if (resizedBitmap !== bitmap) resizedBitmap.recycle()
            bitmap.recycle()

            Log.d(TAG, "Final size = ${imageFile.length() / 1024} KB")

        } catch (e: Exception) {
            Log.e(TAG, "Image optimization failed", e)
        }

        return imageFile
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = minOf(
            MAX_WIDTH.toFloat() / width,
            MAX_HEIGHT.toFloat() / height
        )

        if (ratio >= 1f) {
            return bitmap
        }

        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun scaleDown(bitmap: Bitmap, factor: Float): Bitmap {
        val w = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val h = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    fun addDateTimeStampToImage(imagePath: String): Bitmap? {
        Log.d(TAG, "=== addDateTimeStampToImage START ===")

        val originalBitmap = BitmapFactory.decodeFile(imagePath)
        if (originalBitmap == null) {
            Log.e(TAG, "Bitmap decode failed.")
            return null
        }

        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val dateTime = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.getDefault()
        ).format(Date())

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val bounds = Rect()
        textPaint.getTextBounds(dateTime, 0, dateTime.length, bounds)

        val x = 30f
        val y = bounds.height() + 60f

        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 180
        }

        canvas.drawRoundRect(
            x - 30,
            y - bounds.height() - 40,
            x + bounds.width() + 30,
            y + 30,
            20f,
            20f,
            bgPaint
        )

        canvas.drawText(dateTime, x, y, textPaint)

        try {
            FileOutputStream(imagePath).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Saving stamped image failed", e)
        }

        return mutableBitmap
    }


    fun generateImageName(prefix: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val randomNum = random.nextInt(99999)
        return "${prefix}_${timestamp}_${randomNum}"
    }

    fun generateImageNameWithSuffix(prefix: String, additionalSuffix: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val randomNum = random.nextInt(99999)
        return "${prefix}_${additionalSuffix}_${timestamp}_${randomNum}"
    }


    fun generateRandomImageName(prefix: String = "Image", digitCount: Int = 15): String {
        val randomDigits = (1..digitCount).joinToString("") { random.nextInt(10).toString() }
        return "${prefix}_$randomDigits"
    }

    fun generateImageNameWithFormat(prefix: String, nameFormat: String = "timestamp"): String {
        return when (nameFormat.lowercase()) {
            "timestamp" -> {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                "${prefix}_$timestamp"
            }
            "random" -> {
                val randomNum = random.nextInt(99999)
                "${prefix}_$randomNum"
            }
            "combined" -> {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val randomNum = random.nextInt(99999)
                "${prefix}_${timestamp}_$randomNum"
            }
            else -> {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                "${prefix}_$timestamp"
            }
        }
    }


    suspend fun uploadImageToApi(
        apiUrl: String,
        imageFile: File,
        siteCode: String,
        documentType: String,
        imageName: String? = null
    ): Pair<Boolean, String> {
        return try {
            if (apiUrl.isEmpty()) {
                Log.e(TAG, "Image API URL is empty")
                return Pair(false, "")
            }

            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: ${imageFile.absolutePath}")
                return Pair(false, "")
            }

            // Optimize image before upload
            val optimizedFile = optimizeImage(imageFile)

            // Generate image name if not provided
            val finalImageName = imageName ?: generateImageName(documentType)

            Log.d(TAG, "=== UPLOADING IMAGE TO API ===")
            Log.d(TAG, "API URL: $apiUrl")
            Log.d(TAG, "Image Path: ${optimizedFile.absolutePath}")
            Log.d(TAG, "Image Name: $finalImageName")
            Log.d(TAG, "Document Type: $documentType")
            Log.d(TAG, "Image Size: ${optimizedFile.length()} bytes (${optimizedFile.length() / 1024} KB)")

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "$finalImageName.jpg",
                    optimizedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("image_name", finalImageName)
                .addFormDataPart("site_code", siteCode)
                .addFormDataPart("document_type", documentType)
                .build()

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Image upload successful. Response: $responseBody")
                Pair(true, finalImageName)
            } else {
                Log.e(TAG, "Image upload failed. Response code: ${response.code}")
                Log.e(TAG, "Response message: ${response.message}")
                Pair(false, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to API", e)
            Pair(false, "")
        }
    }


    fun uploadImageToApiSync(
        apiUrl: String,
        imageFile: File,
        siteCode: String,
        documentType: String,
        imageName: String? = null
    ): Pair<Boolean, String> {
        return try {
            if (apiUrl.isEmpty()) {
                Log.e(TAG, "Image API URL is empty")
                return Pair(false, "")
            }

            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: ${imageFile.absolutePath}")
                return Pair(false, "")
            }

            val optimizedFile = optimizeImage(imageFile)
            val finalImageName = imageName ?: generateImageName(documentType)

            Log.d(TAG, "=== UPLOADING IMAGE TO API (SYNC) ===")
            Log.d(TAG, "API URL: $apiUrl")
            Log.d(TAG, "Image Name: $finalImageName")
            Log.d(TAG, "Image Size: ${optimizedFile.length() / 1024} KB")

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "$finalImageName.jpg",
                    optimizedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("image_name", finalImageName)
                .addFormDataPart("site_code", siteCode)
                .addFormDataPart("document_type", documentType)
                .build()

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d(TAG, "Image upload successful")
                Pair(true, finalImageName)
            } else {
                Log.e(TAG, "Image upload failed. Response code: ${response.code}")
                Pair(false, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to API", e)
            Pair(false, "")
        }
    }

    interface ImageUploadCallback {
        fun onSuccess(imageName: String)
        fun onFailure(error: String)
    }

    fun uploadImageToApiAsync(
        apiUrl: String,
        imageFile: File,
        siteCode: String,
        documentType: String,
        imageName: String? = null,
        callback: ImageUploadCallback
    ) {
        Thread {
            try {
                val (success, name) = uploadImageToApiSync(
                    apiUrl = apiUrl,
                    imageFile = imageFile,
                    siteCode = siteCode,
                    documentType = documentType,
                    imageName = imageName
                )

                if (success && name.isNotEmpty()) {
                    callback.onSuccess(name)
                } else {
                    callback.onFailure("Image upload failed")
                }
            } catch (e: Exception) {
                callback.onFailure("Error: ${e.message}")
            }
        }.start()
    }


    fun getOptimizedImageFile(context: android.content.Context, uri: Uri): File? {
        return try {
            val imageFile = UriHelper.getFileFromUri(context, uri)
            if (imageFile != null && imageFile.exists()) {
                optimizeImage(imageFile)
                imageFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimized image file", e)
            null
        }
    }

    fun isFileSizeValid(file: File): Boolean {
        return file.exists() && file.length() <= MAX_FILE_SIZE
    }

    fun getFileSizeKB(file: File): Long {
        return if (file.exists()) file.length() / 1024 else 0
    }


    fun getFileSizeMB(file: File): String {
        return if (file.exists()) {
            String.format("%.2f", file.length() / (1024.0 * 1024.0))
        } else {
            "0.00"
        }
    }


}