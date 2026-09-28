package com.talha.restaurantpos.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Uploads images directly to Cloudinary via its unsigned upload API, replacing Firebase Storage.
 * No API secret ships in the app — only the cloud name + an unsigned upload preset are needed
 * (see [CloudinaryConfig]).
 *
 * Images are downscaled + JPEG-compressed before upload. Phone camera photos are often 3-8 MB;
 * on a weak/slow connection that can take minutes and looks like the app "hung". Shrinking to a
 * max 1280px edge at 80% quality brings most photos under ~300 KB, so uploads stay fast even on
 * poor networks.
 */
@Singleton
class CloudinaryUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    suspend fun upload(localUri: String, folder: String, publicId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = uriToCompressedFile(Uri.parse(localUri))
                try {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                        .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                        .addFormDataPart("folder", folder)
                        .addFormDataPart("public_id", publicId)
                        .build()

                    val request = Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            throw IOException("Cloudinary upload failed (${response.code}): $bodyString")
                        }
                        JSONObject(bodyString).getString("secure_url")
                    }
                } finally {
                    file.delete()
                }
            }
        }

    private fun uriToCompressedFile(uri: Uri): File {
        val maxEdge = 1280
        val quality = 80

        // First pass: read only bounds, so we don't fully decode a huge bitmap into memory.
        // Note: decodeStream ALWAYS returns null when inJustDecodeBounds is true — that's by
        // design (it only fills outWidth/outHeight) — so success is checked via those fields,
        // not via the return value.
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image at $uri")
        boundsStream.use { input -> BitmapFactory.decodeStream(input, null, boundsOptions) }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            throw IOException("Could not read image data at $uri")
        }

        val (width, height) = boundsOptions.outWidth to boundsOptions.outHeight
        var sampleSize = 1
        if (width > 0 && height > 0) {
            val largerEdge = max(width, height)
            while (largerEdge / sampleSize > maxEdge * 2) sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IOException("Cannot decode image at $uri")

        val bitmap = if (max(rawBitmap.width, rawBitmap.height) > maxEdge) {
            val scale = maxEdge.toFloat() / max(rawBitmap.width, rawBitmap.height)
            val scaled = Bitmap.createScaledBitmap(
                rawBitmap, (rawBitmap.width * scale).toInt(), (rawBitmap.height * scale).toInt(), true
            )
            if (scaled !== rawBitmap) rawBitmap.recycle()
            scaled
        } else rawBitmap

        val temp = File.createTempFile("upload_", ".jpg", context.cacheDir)
        FileOutputStream(temp).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        return temp
    }
}
