package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * The player: a glowing energy orb. Gravity/jump physics are unchanged from the
 * original design (predictable physics matters more for "fair" deaths than visuals
 * do), but rendering is now a layered glow + pulsing core instead of a flat circle.
 */
class Player(private val groundY: Float, private val screenWidth: Int) {

    var x: Float = screenWidth * 0.28f
    val radius: Float = screenWidth * 0.045f

    private val restingY: Float = groundY - radius
    var y: Float = restingY
    var velocityY: Float = 0f

    private val gravity = 2600f
    private val jumpVelocity = -1250f

    var isOnGround = true
        private set

    private var squash = 1f
    private var pulseTime = 0f

    var hasShield = false
    private var shieldPulse = 0f

    private val coreColor = Color.parseColor("#7FE7FF")
    private val glowColorOuter = Color.parseColor("#406CE7FF") // semi-transparent cyan
    private val shieldColor = Color.parseColor("#FFD23F")

    private val glowPaint = Paint().apply { isAntiAlias = true }
    private val corePaint = Paint().apply { isAntiAlias = true; color = coreColor }
    private val shieldPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = shieldColor
    }
    private val highlightPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        alpha = 160
    }

    fun jump() {
        if (isOnGround) {
            velocityY = jumpVelocity
            isOnGround = false
            squash = 1.3f
        }
    }

    fun update(dt: Float) {
        velocityY += gravity * dt
        y += velocityY * dt

        if (y >= restingY) {
            y = restingY
            velocityY = 0f
            if (!isOnGround) squash = 0.7f
            isOnGround = true
        }

        squash += (1f - squash) * 0.25f
        pulseTime += dt * 4f
        shieldPulse += dt * 6f
    }

    fun getBounds(): RectFBounds {
        val scaledRadiusX = radius * squash
        val scaledRadiusY = radius / squash
        return RectFBounds(x - scaledRadiusX, y - scaledRadiusY, x + scaledRadiusX, y + scaledRadiusY)
    }

    fun draw(canvas: Canvas) {
        val pulse = 1f + 0.08f * kotlin.math.sin(pulseTime)
        val glowRadius = radius * 2.2f * pulse

        glowPaint.shader = RadialGradient(
            x, y, glowRadius,
            glowColorOuter, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, glowRadius, glowPaint)

        // Core (squash/stretch applied)
        val b = getBounds()
        canvas.drawOval(b.left, b.top, b.right, b.bottom, corePaint)

        // Bright inner highlight for a glassy look
        canvas.drawCircle(x - radius * 0.25f, y - radius * 0.3f, radius * 0.3f, highlightPaint)

        if (hasShield) {
            val shieldRadius = radius * 1.7f + kotlin.math.sin(shieldPulse) * 4f
            shieldPaint.strokeWidth = radius * 0.18f
            shieldPaint.alpha = 200
            canvas.drawCircle(x, y, shieldRadius, shieldPaint)
        }
    }

    fun reset() {
        y = restingY
        velocityY = 0f
        isOnGround = true
        squash = 1f
        hasShield = false
    }
}

data class RectFBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(other: RectFBounds): Boolean {
        return left < other.right && right > other.left &&
               top < other.bottom && bottom > other.top
    }
}
