package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * The player is a simple ball with gravity + jump impulse physics.
 * Simple, predictable physics is key for a "one more try" game —
 * the player needs to feel like death was THEIR mistake, not the game's.
 */
class Player(private val groundY: Float, private val screenWidth: Int) {

    var x: Float = screenWidth * 0.28f
    val radius: Float = screenWidth * 0.045f

    // y is the CENTER of the ball. When grounded, the ball's bottom edge sits on groundY.
    private val restingY: Float = groundY - radius
    var y: Float = restingY
    var velocityY: Float = 0f

    private val gravity = 2600f          // px/s^2
    private val jumpVelocity = -1250f    // px/s (negative = upward)

    var isOnGround = true
        private set

    // Squash/stretch for a bit of visual juice — cheap to add, makes movement feel alive
    private var squash = 1f

    private val paint = Paint().apply {
        color = Color.parseColor("#FFD23F")
        isAntiAlias = true
    }

    fun jump() {
        if (isOnGround) {
            velocityY = jumpVelocity
            isOnGround = false
            squash = 1.3f // stretch on takeoff
        }
    }

    fun update(dt: Float) {
        velocityY += gravity * dt
        y += velocityY * dt

        if (y >= restingY) {
            y = restingY
            velocityY = 0f
            if (!isOnGround) squash = 0.7f // squash on landing
            isOnGround = true
        }

        // Ease squash back to normal — quick spring-like recovery
        squash += (1f - squash) * 0.25f
    }

    fun getBounds(): RectFBounds {
        val scaledRadiusX = radius * squash
        val scaledRadiusY = radius / squash
        return RectFBounds(x - scaledRadiusX, y - scaledRadiusY, x + scaledRadiusX, y + scaledRadiusY)
    }

    fun draw(canvas: Canvas) {
        val b = getBounds()
        canvas.drawOval(b.left, b.top, b.right, b.bottom, paint)

        // Simple eye for character/personality — cheap charm
        val eyePaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        canvas.drawCircle(x + radius * 0.35f, y - radius * 0.5f, radius * 0.12f, eyePaint)
    }

    fun reset() {
        y = restingY
        velocityY = 0f
        isOnGround = true
        squash = 1f
    }
}

data class RectFBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(other: RectFBounds): Boolean {
        return left < other.right && right > other.left &&
               top < other.bottom && bottom > other.top
    }
}
