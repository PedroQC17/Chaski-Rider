package com.example.chaskirider.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

internal fun riderMapMarker(context: Context): BitmapDescriptor {
    val size = (44 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(size / 80f, size / 80f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE
    canvas.drawCircle(40f, 40f, 39f, paint)
    paint.color = Color.rgb(37, 99, 235)
    canvas.drawCircle(40f, 40f, 35f, paint)
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3.5f
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeJoin = Paint.Join.ROUND
    canvas.drawCircle(22f, 54f, 9f, paint)
    canvas.drawCircle(58f, 54f, 9f, paint)
    val bike = Path().apply {
        moveTo(22f, 54f); lineTo(31f, 38f); lineTo(43f, 54f); lineTo(22f, 54f)
        moveTo(43f, 54f); lineTo(51f, 35f); lineTo(58f, 54f)
        moveTo(47f, 31f); lineTo(51f, 31f); lineTo(51f, 35f)
        moveTo(27f, 38f); lineTo(37f, 38f)
        moveTo(34f, 26f); lineTo(29f, 35f); lineTo(39f, 41f); lineTo(35f, 50f)
        moveTo(34f, 26f); lineTo(43f, 33f); lineTo(49f, 33f)
    }
    canvas.drawPath(bike, paint)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(30f, 34f, 47f, 42f, 4f, 4f, paint)
    canvas.drawRoundRect(33f, 44f, 44f, 53f, 2f, 2f, paint)
    canvas.drawCircle(36f, 19f, 6f, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
