package com.smartscan.pro.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    CRISP_BW("Crisp B&W"),
    GRAYSCALE("Grayscale"),
    BRIGHTEN("Shadow Fix"),
    SHARP_DOC("Sharp Doc")
}

object ImageEnhancer {

    fun getDocumentsDir(context: Context): File {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyUriToPermanentImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2200
    ): File? {
        return runCatching {
            val bitmap = decodeScaledBitmapFromUri(context, uri, maxDimension) ?: return null
            val outFile = File(
                getDocumentsDir(context),
                "scan_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            )
            saveBitmapAsJpeg(bitmap, outFile, quality = 90)
            bitmap.recycle()
            outFile
        }.getOrNull()
    }

    fun copyUriToPermanentPdf(context: Context, uri: Uri): File? {
        return runCatching {
            val outFile = File(
                getDocumentsDir(context),
                "doc_${System.currentTimeMillis()}.pdf"
            )
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            outFile
        }.getOrNull()
    }

    private fun decodeScaledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    fun loadScaledBitmap(filePath: String, maxDimension: Int = 1400): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(filePath, decodeOpts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val largest = max(width, height)
        while ((largest / inSampleSize) > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    fun applyEnhancements(
        source: Bitmap,
        filter: ScanFilter,
        rotationDegrees: Float = 0f,
        brightness: Float = 0f, // -60f .. +60f
        contrast: Float = 1.0f  // 0.5f .. 2.0f
    ): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360f) + 360f) % 360f
        val rotated = if (normalizedRotation != 0f) {
            val matrix = Matrix().apply { postRotate(normalizedRotation) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val combinedMatrix = ColorMatrix()

        when (filter) {
            ScanFilter.ORIGINAL -> Unit
            ScanFilter.MAGIC_COLOR -> {
                val sat = ColorMatrix().apply { setSaturation(1.28f) }
                combinedMatrix.postConcat(sat)
                combinedMatrix.postConcat(contrastBrightnessMatrix(1.25f, 12f))
            }
            ScanFilter.CRISP_BW -> {
                val gray = ColorMatrix().apply { setSaturation(0f) }
                combinedMatrix.postConcat(gray)
                combinedMatrix.postConcat(contrastBrightnessMatrix(2.35f, 18f))
            }
            ScanFilter.GRAYSCALE -> {
                val gray = ColorMatrix().apply { setSaturation(0f) }
                combinedMatrix.postConcat(gray)
                combinedMatrix.postConcat(contrastBrightnessMatrix(1.18f, 6f))
            }
            ScanFilter.BRIGHTEN -> {
                combinedMatrix.postConcat(contrastBrightnessMatrix(1.12f, 32f))
            }
            ScanFilter.SHARP_DOC -> {
                val sat = ColorMatrix().apply { setSaturation(0.85f) }
                combinedMatrix.postConcat(sat)
                combinedMatrix.postConcat(contrastBrightnessMatrix(1.48f, 16f))
            }
        }

        if (contrast != 1.0f || brightness != 0f) {
            combinedMatrix.postConcat(contrastBrightnessMatrix(contrast, brightness))
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(rotated, 0f, 0f, paint)

        if (rotated !== source) {
            rotated.recycle()
        }
        return result
    }

    private fun contrastBrightnessMatrix(contrast: Float, brightness: Float): ColorMatrix {
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun saveBitmapAsJpeg(bitmap: Bitmap, file: File, quality: Int = 90): File {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file
    }

    fun calculateTotalSize(paths: List<String>, pdfPath: String? = null): Long {
        var total = 0L
        paths.forEach { path ->
            val f = File(path)
            if (f.exists()) total += f.length()
        }
        if (!pdfPath.isNullOrBlank()) {
            val pf = File(pdfPath)
            if (pf.exists()) total += pf.length()
        }
        return total
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.2f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f KB", max(1.0, kb))
        }
    }
}
