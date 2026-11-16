package com.example.tracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ImageStorage"

/**
 * Gambar dari galeri hanya bisa diakses sementara lewat URI, jadi disalin ke
 * penyimpanan internal aplikasi (diperkecil agar hemat ruang) lalu path-nya disimpan di Plan.
 */
class ImageStorage(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "plan_images").apply { mkdirs() }

    /** Mengembalikan path file hasil salinan, atau null kalau gagal (penyebab ada di Logcat, tag ImageStorage). */
    suspend fun save(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeSampledBitmap(context, uri, SAVE_MAX_SIDE)
            if (bitmap == null) {
                Log.e(TAG, "Gambar tidak bisa dibaca dari $uri")
                return@withContext null
            }
            val file = File(dir, "plan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) {
                    Log.e(TAG, "Gagal mengompres gambar ke JPEG")
                    return@withContext null
                }
            }
            file.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menyimpan gambar dari $uri", t)
            null
        }
    }

    /** Menyalin file gambar yang sudah ada untuk plan baru (agar saat satu dihapus, yang lain tetap ada). */
    suspend fun copyImageFile(oldPath: String): String? = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) return@withContext null
            val newFile = File(dir, "plan_copy_${System.currentTimeMillis()}.jpg")
            oldFile.copyTo(newFile, overwrite = true)
            newFile.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menyalin gambar dari $oldPath", t)
            null
        }
    }

    suspend fun delete(path: String?) {
        if (path == null) return
        withContext(Dispatchers.IO) { runCatching { File(path).delete() } }
    }

    private companion object {
        const val SAVE_MAX_SIDE = 1280
    }
}

private fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (maxOf(width, height) / sample > maxSide) sample *= 2
    return sample
}

/**
 * Decode dari URI dengan downsampling. Android 9+ memakai ImageDecoder
 * (orientasi EXIF otomatis, format lebih lengkap); sisanya BitmapFactory.
 */
internal fun decodeSampledBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Software agar bitmap bisa dikompres dan digambar ulang
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val sample = sampleSizeFor(info.size.width, info.size.height, maxSide)
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "ImageDecoder gagal, mencoba BitmapFactory", t)
        }
    }
    return decodeWithBitmapFactory(context, uri, maxSide)
}

private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    return try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.e(TAG, "Ukuran gambar tidak terbaca: $uri")
            return null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val degrees = exifRotationDegrees(context, uri)
        if (degrees == 0) bitmap
        else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees.toFloat()) }, true
        )
    } catch (t: Throwable) {
        Log.e(TAG, "BitmapFactory gagal membaca $uri", t)
        null
    }
}

/** Decode dari file yang sudah disimpan (sudah dalam orientasi benar). */
internal fun decodeSampledBitmap(path: String, maxSide: Int): Bitmap? {
    return try {
        if (!File(path).exists()) {
            Log.w(TAG, "File gambar tidak ditemukan: $path")
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.e(TAG, "File gambar rusak: $path")
            return null
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
        }
        BitmapFactory.decodeFile(path, options)
    } catch (t: Throwable) {
        Log.e(TAG, "Gagal membaca file gambar $path", t)
        null
    }
}

private fun exifRotationDegrees(context: Context, uri: Uri): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return 0
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)
}