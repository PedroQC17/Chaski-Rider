package com.example.chaskirider.feature.orders.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.chaskirider.feature.orders.domain.EvidenceAnalyzer
import com.example.chaskirider.feature.orders.domain.EvidenceIssue
import com.example.chaskirider.feature.orders.domain.EvidenceQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class AndroidEvidenceAnalyzer(private val context: Context) : EvidenceAnalyzer {
    override suspend fun analyze(uri: String): EvidenceQuality = withContext(Dispatchers.IO) {
        try {
            val parsed = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext EvidenceQuality(listOf(EvidenceIssue.UNREADABLE))
            }

            val sample = (min(bounds.outWidth, bounds.outHeight) / 640).coerceAtLeast(1)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext EvidenceQuality(listOf(EvidenceIssue.UNREADABLE))

            try {
                val (luminance, variance) = bitmap.luminanceStats()
                EvidenceQuality.fromMetrics(
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    meanLuminance = luminance,
                    laplacianVariance = variance
                )
            } finally {
                bitmap.recycle()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            EvidenceQuality(listOf(EvidenceIssue.UNREADABLE))
        }
    }
}

private fun Bitmap.luminanceStats(): Pair<Double, Double> {
    val w = width
    val h = height
    if (w < 3 || h < 3) return 0.0 to 0.0

    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val gray = DoubleArray(pixels.size)
    var sum = 0.0
    for (index in pixels.indices) {
        val color = pixels[index]
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val value = red * 0.299 + green * 0.587 + blue * 0.114
        gray[index] = value
        sum += value
    }
    val mean = sum / gray.size

    var laplacianSum = 0.0
    var laplacianSquareSum = 0.0
    var count = 0
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val index = y * w + x
            val laplacian = 4 * gray[index] - gray[index - 1] - gray[index + 1] -
                gray[index - w] - gray[index + w]
            laplacianSum += laplacian
            laplacianSquareSum += laplacian * laplacian
            count++
        }
    }
    if (count == 0) return mean to 0.0
    val laplacianMean = laplacianSum / count
    val variance = (laplacianSquareSum / count) - (laplacianMean * laplacianMean)
    return mean to variance.coerceAtLeast(0.0)
}
