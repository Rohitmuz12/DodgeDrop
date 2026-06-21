package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.random.Random

/**
 * An asteroid obstacle drifting right-to-left. Drawn as an irregular jagged polygon
 * (generated once per instance) with slow rotation, which reads much more "space"
 * than a flat triangle and costs almost nothing extra at runtime.
 */
class Obstacle(
    var x: Float,
    private val groundY: Float,
    private val size: Float
) {
    var passed = false

    private var rotation = Random.nextFloat() * 360f
    private val rotationSpeed = (Random.nextFloat() - 0.5f) * 70f // deg/sec, some spin one way, some the other

    // Precompute a jagged silhouette: alternating radii around a circle
    private val points: List<Pair<Float, Float>>
    private val baseY: Float = groundY - size * 0.5f // visual center sits half-buried for a "rising from ground" feel

    private val rockPaint = Paint().apply {
        color = Color.parseColor("#8C7C8C")
        isAntiAlias = true
    }
    private val rockShadowPaint = Paint().apply {
        color = Color.parseColor("#4A3F52")
        isAntiAlias = true
    }
    private val rimGlowPaint = Paint().apply {
        color = Color.parseColor("#806C5CE7")
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = size * 0.04f
    }

    init {
        val sides = 9
        val pts = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until sides) {
            val angle = (i.toFloat() / sides) * 2f * Math.PI.toFloat()
            val r = size * 0.5f * (0.78f + Random.nextFloat() * 0.28f)
            pts.add(Pair(kotlin.math.cos(angle) * r, kotlin.math.sin(angle) * r))
        }
        points = pts
    }

    fun update(dt: Float, speed: Float) {
        x -= speed * dt
        rotation += rotationSpeed * dt
    }

    fun isOffScreen(): Boolean = x + size < 0

    /** Collision uses a slightly inset circular hitbox — simpler and fairer than the jagged visual shape. */
    fun getBounds(): RectFBounds {
        val hitRadius = size * 0.38f
        return RectFBounds(
            x - hitRadius,
            baseY - hitRadius,
            x + hitRadius,
            baseY + hitRadius
        )
    }

    fun centerX(): Float = x
    fun centerY(): Float = baseY

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(x, baseY)
        canvas.rotate(rotation)

        val path = Path()
        points.forEachIndexed { i, (px, py) ->
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        // Soft shadow offset for depth
        canvas.save()
        canvas.translate(size * 0.05f, size * 0.06f)
        canvas.drawPath(path, rockShadowPaint)
        canvas.restore()

        canvas.drawPath(path, rockPaint)
        canvas.drawPath(path, rimGlowPaint)

        canvas.restore()
    }
}
