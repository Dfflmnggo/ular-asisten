package com.dff.ular

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class SnakeView(c: Context) : View(c) {
    private val n = 25
    private val xs = FloatArray(n)
    private val ys = FloatArray(n)
    private var angle = 0f
    var hue = 120f
    private var ready = false
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15f * d }
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 20, 20, 20) }
    private var msg = listOf<String>()
    private var until = 0L
    private val rnd = java.util.Random()

    fun say(t: String) {
        msg = t.split("\n")
        until = System.currentTimeMillis() + 6000
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (!ready) {
            for (i in 0 until n) { xs[i] = w / 2; ys[i] = h / 2 }
            ready = true
        }
        angle += (rnd.nextFloat() - 0.5f) * 0.3f
        xs[0] += cos(angle) * 2f * d
        ys[0] += sin(angle) * 2f * d
        if (xs[0] < 0 || xs[0] > w || ys[0] < 0 || ys[0] > h) {
            angle = atan2(h / 2 - ys[0], w / 2 - xs[0])
        }
        for (i in 1 until n) {
            val a = atan2(ys[i - 1] - ys[i], xs[i - 1] - xs[i])
            xs[i] = xs[i - 1] - cos(a) * 5f * d
            ys[i] = ys[i - 1] - sin(a) * 5f * d
        }
        for (i in n - 1 downTo 0) {
            paint.color = Color.HSVToColor(floatArrayOf((hue + i * 3) % 360f, 0.7f, 0.8f))
            cv.drawCircle(xs[i], ys[i], (7f - i * 0.15f) * d, paint)
        }
        paint.color = Color.WHITE
        for (s in intArrayOf(-1, 1)) {
            cv.drawCircle(
                xs[0] + cos(angle + s * 0.8f) * 4f * d,
                ys[0] + sin(angle + s * 0.8f) * 4f * d,
                1.8f * d, paint
            )
        }
        if (System.currentTimeMillis() < until) {
            var y = 100f * d
            for (line in msg.take(8)) {
                val tw = tp.measureText(line)
                cv.drawRoundRect(RectF(16f * d, y - 18f * d, 32f * d + tw, y + 6f * d), 8f * d, 8f * d, bg)
                cv.drawText(line, 24f * d, y, tp)
                y += 26f * d
            }
        }
        postInvalidateOnAnimation()
    }
}
