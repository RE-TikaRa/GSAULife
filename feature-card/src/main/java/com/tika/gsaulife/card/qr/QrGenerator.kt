package com.tika.gsaulife.card.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal object QrGenerator {
    const val SIZE_WIDGET = 288
    const val SIZE_CARD = 600
    const val SIZE_FULLSCREEN = 700

    fun encode(
        content: String,
        size: Int,
        foreground: Int = Color.BLACK,
        background: Int = Color.TRANSPARENT
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            val offset = y * matrix.width
            for (x in 0 until matrix.width) {
                pixels[offset + x] = if (matrix[x, y]) foreground else background
            }
        }
        return createBitmap(
            matrix.width,
            matrix.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        }
    }
}
