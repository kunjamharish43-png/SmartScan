package com.smartscan.pro.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

enum class PdfPageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
    FIT_IMAGE("Auto Fit", 0, 0)
}

enum class PdfQuality(val label: String, val maxImageDim: Int) {
    HIGH("High HD", 2000),
    MEDIUM("Balanced", 1300),
    COMPACT("Small Size", 800)
}

data class PdfBuildOptions(
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val quality: PdfQuality = PdfQuality.MEDIUM,
    val watermarkText: String = "",
    val includePageNumbers: Boolean = true,
    val attachSignature: Boolean = false,
    val signatureOnAllPages: Boolean = false
)

object PdfStudioEngine {

    fun getSavedSignatureFile(context: Context): File {
        return File(ImageEnhancer.getDocumentsDir(context), "user_signature.png")
    }

    fun hasSavedSignature(context: Context): Boolean {
        return getSavedSignatureFile(context).exists()
    }

    fun saveNormalizedSignature(
        context: Context,
        strokes: List<List<Pair<Float, Float>>>,
        inkColorInt: Int
    ): File? {
        if (strokes.isEmpty()) return null
        return runCatching {
            val outW = 640
            val outH = 280
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColorInt
                style = Paint.Style.STROKE
                strokeWidth = 7f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            strokes.forEach { stroke ->
                if (stroke.size >= 2) {
                    val path = Path()
                    path.moveTo(stroke[0].first * outW, stroke[0].second * outH)
                    for (i in 1 until stroke.size) {
                        path.lineTo(stroke[i].first * outW, stroke[i].second * outH)
                    }
                    canvas.drawPath(path, paint)
                }
            }

            val outFile = getSavedSignatureFile(context)
            FileOutputStream(outFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
            outFile
        }.getOrNull()
    }

    fun createPdfFromImages(
        context: Context,
        imagePaths: List<String>,
        baseFileName: String,
        options: PdfBuildOptions
    ): File? {
        val validPaths = imagePaths.filter { File(it).exists() }
        if (validPaths.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val sigBitmap: Bitmap? = if (options.attachSignature && hasSavedSignature(context)) {
            BitmapFactory.decodeFile(getSavedSignatureFile(context).absolutePath)
        } else {
            null
        }

        return try {
            val totalPages = validPaths.size
            validPaths.forEachIndexed { index, path ->
                val bmp = ImageEnhancer.loadScaledBitmap(path, options.quality.maxImageDim)
                    ?: return@forEachIndexed

                val pageWidth: Int
                val pageHeight: Int
                val margin: Int

                if (options.pageSize == PdfPageSize.FIT_IMAGE) {
                    val scale = min(842f / bmp.height.toFloat(), 595f / bmp.width.toFloat()).coerceAtMost(1f)
                    pageWidth = (bmp.width * scale).toInt().coerceAtLeast(200)
                    pageHeight = (bmp.height * scale).toInt().coerceAtLeast(200)
                    margin = 0
                } else {
                    pageWidth = options.pageSize.widthPt
                    pageHeight = options.pageSize.heightPt
                    margin = 24
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val availW = (pageWidth - margin * 2).toFloat()
                val availH = (pageHeight - margin * 2 - if (options.includePageNumbers) 18 else 0).toFloat()
                val scale = min(availW / bmp.width.toFloat(), availH / bmp.height.toFloat())
                val drawW = (bmp.width * scale).toInt()
                val drawH = (bmp.height * scale).toInt()
                val left = ((pageWidth - drawW) / 2)
                val top = ((pageHeight - drawH) / 2)

                val dstRect = Rect(left, top, left + drawW, top + drawH)
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bmp, null, dstRect, imgPaint)
                bmp.recycle()

                // Custom Diagonal Watermark
                if (options.watermarkText.isNotBlank()) {
                    drawWatermark(canvas, pageWidth, pageHeight, options.watermarkText.trim())
                }

                // Digital Signature Stamp
                val shouldDrawSig = sigBitmap != null &&
                    (options.signatureOnAllPages || index == totalPages - 1)
                if (shouldDrawSig && sigBitmap != null) {
                    val sigW = (pageWidth * 0.28f).toInt().coerceAtLeast(90)
                    val sigH = (sigW * (sigBitmap.height.toFloat() / sigBitmap.width.toFloat())).toInt()
                    val sigLeft = pageWidth - sigW - 28
                    val sigTop = pageHeight - sigH - 36
                    canvas.drawBitmap(sigBitmap, null, Rect(sigLeft, sigTop, sigLeft + sigW, sigTop + sigH), imgPaint)
                }

                // Page Footer Number
                if (options.includePageNumbers) {
                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.DKGRAY
                        textSize = 10f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(
                        "Page ${index + 1} of$totalPages",
                        pageWidth / 2f,
                        pageHeight - 10f,
                        numPaint
                    )
                }

                pdfDocument.finishPage(page)
            }

            val safeName = baseFileName
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(40)
                .ifBlank { "SmartScan_Doc" }
            val outFile = File(
                ImageEnhancer.getDocumentsDir(context),
                "${safeName}_${System.currentTimeMillis()}.pdf"
            )
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            outFile
        } catch (e: Exception) {
            null
        } finally {
            sigBitmap?.recycle()
            pdfDocument.close()
        }
    }

    private fun drawWatermark(canvas: Canvas, width: Int, height: Int, text: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(65, 90, 90, 110)
            textSize = (width * 0.085f).coerceIn(26f, 54f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.rotate(-35f)
        canvas.drawText(text, 0f, 0f, paint)
        canvas.restore()
    }

    fun renderExternalPdfToImages(
        context: Context,
        pdfUri: Uri,
        maxPages: Int = 35
    ): List<String> {
        val savedPaths = mutableListOf<String>()
        runCatching {
            context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = min(renderer.pageCount, maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = 2.0f
                            val bmpW = (page.width * scale).toInt().coerceAtMost(1800)
                            val bmpH = (page.height * scale).toInt().coerceAtMost(2400)
                            val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val outFile = File(
                                ImageEnhancer.getDocumentsDir(context),
                                "pdf_page_${System.currentTimeMillis()}_$i.jpg"
                            )
                            ImageEnhancer.saveBitmapAsJpeg(bmp, outFile, quality = 88)
                            bmp.recycle()
                            savedPaths.add(outFile.absolutePath)
                        }
                    }
                }
            }
        }
        return savedPaths
    }

    fun copyFileToUri(context: Context, sourceFile: File, destinationUri: Uri): Boolean {
        if (!sourceFile.exists()) return false
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } != null
        }.getOrDefault(false)
    }

    fun sharePdfFile(context: Context, pdfFile: File, title: String) {
        if (!pdfFile.exists()) return
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
    }
}
