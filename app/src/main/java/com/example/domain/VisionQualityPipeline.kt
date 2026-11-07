package com.example.domain

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class DuplicateDecision {
    UNIQUE,
    CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH,
    POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED
}

data class PageVisionMetrics(
    val laplacianSharpness: Double,
    val isBlurry: Boolean,
    val phash: String,
    val dhash: String
)

data class DedupComparisonResult(
    val hammingDistancePhash: Int,
    val hammingDistanceDhash: Int,
    val ssimScore: Double,
    val decision: DuplicateDecision,
    val explanation: String
)

object VisionQualityPipeline {

    const val BLUR_THRESHOLD = 85.0
    const val HASH_HAMMING_THRESHOLD = 10
    const val SSIM_DUPLICATE_THRESHOLD = 0.85

    /**
     * Computes Variance of the 3x3 discrete Laplacian operator:
     * [ 0,  1,  0]
     * [ 1, -4,  1]
     * [ 0,  1,  0]
     * Run on a downsampled grayscale representation on-device for <15ms latency.
     */
    fun computeLaplacianSharpness(bitmap: Bitmap): Double {
        val width = bitmap.width.coerceAtMost(256)
        val height = bitmap.height.coerceAtMost(256)
        val scaled = if (bitmap.width != width || bitmap.height != height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = 0.299 * r + 0.587 * g + 0.114 * b
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val lap = gray[idx - width] +
                    gray[idx + width] +
                    gray[idx - 1] +
                    gray[idx + 1] -
                    4.0 * gray[idx]
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return ((variance * 10.0).roundToInt() / 10.0).coerceAtLeast(0.0)
    }

    /**
     * Computes 64-bit difference hash (dHash) from a 9x8 grayscale grid.
     */
    fun computeDHash(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        val pixels = IntArray(9 * 8)
        scaled.getPixels(pixels, 0, 9, 0, 0, 9, 8)

        val bits = StringBuilder(64)
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = luminance(pixels[y * 9 + x])
                val right = luminance(pixels[y * 9 + x + 1])
                bits.append(if (left > right) '1' else '0')
            }
        }
        return binary64ToHex16(bits.toString())
    }

    /**
     * Computes 64-bit DCT perceptual hash (pHash) on a 16x16 grayscale sample,
     * extracting the top-left 8x8 low-frequency DCT coefficients.
     */
    fun computePHash(bitmap: Bitmap): String {
        val n = 16
        val scaled = Bitmap.createScaledBitmap(bitmap, n, n, true)
        val pixels = IntArray(n * n)
        scaled.getPixels(pixels, 0, n, 0, 0, n, n)
        val matrix = Array(n) { y ->
            DoubleArray(n) { x ->
                luminance(pixels[y * n + x])
            }
        }

        val dct = Array(8) { DoubleArray(8) }
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                var sum = 0.0
                for (x in 0 until n) {
                    for (y in 0 until n) {
                        sum += matrix[y][x] *
                            cos(((2 * x + 1) * u * PI) / (2.0 * n)) *
                            cos(((2 * y + 1) * v * PI) / (2.0 * n))
                    }
                }
                dct[u][v] = sum
            }
        }

        val coeffs = DoubleArray(64)
        var k = 0
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                coeffs[k++] = dct[u][v]
            }
        }
        // Exclude DC component coeffs[0] when computing mean
        val acMean = coeffs.drop(1).average()
        val bits = buildString(64) {
            for (i in 0 until 64) {
                append(if (coeffs[i] >= acMean) '1' else '0')
            }
        }
        return binary64ToHex16(bits)
    }

    /**
     * Computes Structural Similarity Index (SSIM) in [0.0, 1.0] between two bitmaps.
     */
    fun computeSSIM(bitmapA: Bitmap, bitmapB: Bitmap): Double {
        val size = 64
        val a = Bitmap.createScaledBitmap(bitmapA, size, size, true)
        val b = Bitmap.createScaledBitmap(bitmapB, size, size, true)
        val pxA = IntArray(size * size)
        val pxB = IntArray(size * size)
        a.getPixels(pxA, 0, size, 0, 0, size, size)
        b.getPixels(pxB, 0, size, 0, 0, size, size)

        val lA = DoubleArray(pxA.size) { luminance(pxA[it]) }
        val lB = DoubleArray(pxB.size) { luminance(pxB[it]) }

        val meanA = lA.average()
        val meanB = lB.average()

        var varA = 0.0
        var varB = 0.0
        var covAB = 0.0
        for (i in lA.indices) {
            val da = lA[i] - meanA
            val db = lB[i] - meanB
            varA += da * da
            varB += db * db
            covAB += da * db
        }
        val n = lA.size.toDouble()
        varA /= n
        varB /= n
        covAB /= n

        val c1 = (0.01 * 255.0) * (0.01 * 255.0)
        val c2 = (0.03 * 255.0) * (0.03 * 255.0)

        val numerator = (2.0 * meanA * meanB + c1) * (2.0 * covAB + c2)
        val denominator = (meanA * meanA + meanB * meanB + c1) * (varA + varB + c2)
        val ssim = if (denominator == 0.0) 1.0 else (numerator / denominator).coerceIn(0.0, 1.0)
        return (ssim * 1000.0).roundToInt() / 1000.0
    }

    /**
     * Implements Edge Case #2 Default Decision:
     * - If both pHash/dHash AND SSIM agree it's a duplicate -> soft-delete lower sharpness to 30-day Trash.
     * - If pHash/dHash and SSIM disagree (e.g. two pages of dense integration matrices) -> keep both & flag "Possible Duplicate".
     */
    fun evaluateDuplicatePair(
        phashA: String,
        dhashA: String,
        phashB: String,
        dhashB: String,
        ssimScore: Double
    ): DedupComparisonResult {
        val distP = hammingDistanceHex(phashA, phashB)
        val distD = hammingDistanceHex(dhashA, dhashB)
        val hashMatches = distP <= HASH_HAMMING_THRESHOLD || distD <= HASH_HAMMING_THRESHOLD
        val ssimMatches = ssimScore >= SSIM_DUPLICATE_THRESHOLD

        return when {
            hashMatches && ssimMatches -> DedupComparisonResult(
                hammingDistancePhash = distP,
                hammingDistanceDhash = distD,
                ssimScore = ssimScore,
                decision = DuplicateDecision.CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH,
                explanation = "pHash (dist=$distP) & SSIM ($ssimScore) confirm duplicate. Lower sharpness sent to 30-day Trash."
            )
            hashMatches != ssimMatches -> DedupComparisonResult(
                hammingDistancePhash = distP,
                hammingDistanceDhash = distD,
                ssimScore = ssimScore,
                decision = DuplicateDecision.POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED,
                explanation = "pHash (dist=$distP) and SSIM ($ssimScore) disagree (similar layout/matrices). Keeping both & flagging 'Possible Duplicate'."
            )
            else -> DedupComparisonResult(
                hammingDistancePhash = distP,
                hammingDistanceDhash = distD,
                ssimScore = ssimScore,
                decision = DuplicateDecision.UNIQUE,
                explanation = "Distinct page (pHash dist=$distP, SSIM=$ssimScore)."
            )
        }
    }

    fun hammingDistanceHex(hexA: String, hexB: String): Int {
        val cleanA = hexA.padEnd(16, '0').take(16)
        val cleanB = hexB.padEnd(16, '0').take(16)
        var distance = 0
        for (i in 0 until 16) {
            val nibbleA = cleanA[i].digitToIntOrNull(16) ?: 0
            val nibbleB = cleanB[i].digitToIntOrNull(16) ?: 0
            distance += Integer.bitCount(nibbleA xor nibbleB)
        }
        return distance
    }

    private fun luminance(argb: Int): Double {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.299 * r + 0.587 * g + 0.114 * b
    }

    private fun binary64ToHex16(bits: String): String {
        val sb = StringBuilder(16)
        for (i in 0 until 64 step 4) {
            val nibble = bits.substring(i, (i + 4).coerceAtMost(bits.length)).toIntOrNull(2) ?: 0
            sb.append(nibble.toString(16).uppercase())
        }
        return sb.toString()
    }
}
