package org.andrew.enzona.ui.transfer

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Dibuja [content] como código QR (negro sobre blanco, también en tema oscuro: así lo lee cualquier lector).
 * Devuelve null si no cabe en un QR; quien lo pide simplemente no lo muestra.
 */
fun qrImage(content: String, sizePx: Int): ImageBitmap? {
    if (content.isEmpty() || sizePx <= 0) return null
    val matrix = try {
        QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, sizePx, sizePx,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1),
        )
    } catch (_: WriterException) {
        return null
    } catch (_: IllegalArgumentException) {
        return null
    }
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix.get(index % matrix.width, index / matrix.width)) BLACK else WHITE
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
