package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * A collectible shield orb. Floating + bobbing animation makes it read clearly as
 * "grab me" versus the asteroids' "avoid me" silhouette language.
 */
class PowerUp(var x: Float, private val baseY: Float, private val radius: Float) {

    private var bobTime = (Math.random() * Math.PI * 2).toFloat()

    private val glowPaint = Paint().apply { isAntiAlias = true }
    private val corePaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#FFD23F")
    }
    private val highlightPaint = Paint().apply { isAntiAlias = true; color = Color.WHITE; alpha = 180 }

    fun y(): Float = baseY + kotlin.math.sin(bobTime) * radius * 0.6f

    fun update(dt: Float, speed: Float) {
        x -= speed * dt
        bobTime += dt * 3f
    }

    fun isOffScreen(): Boolean = x + radius < 0

    fun getBounds(): RectFBounds {
        val r = radius * 0.8f
        val cy = y()
        return RectFBounds(x - r, cy - r, x + r, cy + r)
    }

    fun draw(canvas: Canvas) {
        val cy = y()
        glowPaint.shader = RadialGradient(
            x, cy, radius * 2.2f,
            Color.parseColor("#80FFD23F"), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, cy, radius * 2.2f, glowPaint)
        canvas.drawCircle(x, cy, radius, corePaint)

        canvas.drawCircle(x - radius * 0.3f, cy - radius * 0.3f, radius * 0.28f, highlightPaint)
    }
}
