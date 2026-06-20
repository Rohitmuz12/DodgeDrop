package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * A single obstacle (spike block) moving right-to-left toward the player.
 */
class Obstacle(
    var x: Float,
    private val groundY: Float,
    private val width: Float,
    private val height: Float
) {
    private val paint = Paint().apply {
        color = Color.parseColor("#FF4757")
        isAntiAlias = true
    }

    var passed = false // tracks whether this obstacle has been scored yet

    fun update(dt: Float, speed: Float) {
        x -= speed * dt
    }

    fun isOffScreen(): Boolean = x + width < 0

    fun getBounds(): RectFBounds {
        // Slightly inset hitbox so near-misses feel fair (forgiving collision = less rage-quit)
        val inset = width * 0.12f
        return RectFBounds(
            x + inset,
            groundY - height,
            x + width - inset,
            groundY
        )
    }

    fun draw(canvas: Canvas) {
        // Draw as a simple triangle spike for visual variety vs the round player
        val path = android.graphics.Path()
        path.moveTo(x, groundY)
        path.lineTo(x + width / 2f, groundY - height)
        path.lineTo(x + width, groundY)
        path.close()
        canvas.drawPath(path, paint)
    }
}
