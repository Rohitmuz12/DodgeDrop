package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.random.Random

/**
 * Multi-layer parallax space backdrop: distant slow stars, mid-speed nebula glow,
 * near fast stars, and a slowly drifting planet. Layered speeds are what sell the
 * sense of depth and motion cheaply.
 */
class StarField(private val screenWidth: Int, private val screenHeight: Int) {

    private data class Star(var x: Float, var y: Float, val radius: Float, val speed: Float, val alpha: Int)
    private data class NebulaBlob(var x: Float, val y: Float, val radius: Float, val speed: Float, val color: Int)

    private val farStars = mutableListOf<Star>()
    private val nearStars = mutableListOf<Star>()
    private val nebulae = mutableListOf<NebulaBlob>()

    private var planetX: Float
    private val planetY: Float
    private val planetRadius: Float
    private val planetSpeed = 8f

    private val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, screenHeight.toFloat(),
            Color.parseColor("#05030F"),
            Color.parseColor("#120A2E"),
            Shader.TileMode.CLAMP
        )
    }

    private val starPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val planetPaint = Paint().apply { isAntiAlias = true }
    private val planetRingPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.parseColor("#806C5CE7")
        strokeWidth = screenWidth * 0.012f
    }

    private val nebulaPaints = mutableListOf<Paint>()

    init {
        repeat(35) {
            farStars.add(
                Star(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight * 0.85f,
                    radius = Random.nextFloat() * 2.2f + 0.8f,
                    speed = 18f,
                    alpha = (120..200).random()
                )
            )
        }
        repeat(22) {
            nearStars.add(
                Star(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight * 0.85f,
                    radius = Random.nextFloat() * 2.8f + 1.6f,
                    speed = 55f,
                    alpha = (200..255).random()
                )
            )
        }

        val nebulaColors = listOf(
            Color.parseColor("#3D2B7A"),
            Color.parseColor("#1F3A6B"),
            Color.parseColor("#4A1F5C")
        )
        repeat(3) { i ->
            nebulae.add(
                NebulaBlob(
                    x = Random.nextFloat() * screenWidth,
                    y = screenHeight * (0.15f + Random.nextFloat() * 0.4f),
                    radius = screenWidth * (0.35f + Random.nextFloat() * 0.25f),
                    speed = 30f,
                    color = nebulaColors[i % nebulaColors.size]
                )
            )
            nebulaPaints.add(
                Paint().apply {
                    isAntiAlias = true
                    color = nebulaColors[i % nebulaColors.size]
                    alpha = 50
                }
            )
        }

        planetRadius = screenWidth * 0.16f
        planetY = screenHeight * 0.2f
        planetX = screenWidth * 1.3f
        planetPaint.shader = RadialGradient(
            0f, 0f, planetRadius,
            Color.parseColor("#8C7AE6"),
            Color.parseColor("#2E2057"),
            Shader.TileMode.CLAMP
        )
    }

    fun update(dt: Float, speedMultiplier: Float) {
        for (s in farStars) {
            s.x -= s.speed * speedMultiplier * dt
            if (s.x < -s.radius) s.x = screenWidth + s.radius
        }
        for (s in nearStars) {
            s.x -= s.speed * speedMultiplier * dt
            if (s.x < -s.radius) s.x = screenWidth + s.radius
        }
        for (n in nebulae) {
            n.x -= n.speed * speedMultiplier * dt
            if (n.x < -n.radius) n.x = screenWidth + n.radius
        }
        planetX -= planetSpeed * speedMultiplier * dt
        if (planetX < -planetRadius * 2) planetX = screenWidth + planetRadius * 4
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bgPaint)

        for ((i, n) in nebulae.withIndex()) {
            canvas.drawCircle(n.x, n.y, n.radius, nebulaPaints[i])
        }

        for (s in farStars) {
            starPaint.alpha = s.alpha / 2
            canvas.drawCircle(s.x, s.y, s.radius, starPaint)
        }

        // Planet with ring
        canvas.save()
        canvas.translate(planetX, planetY)
        canvas.drawCircle(0f, 0f, planetRadius, planetPaint)
        canvas.drawOval(
            -planetRadius * 1.5f, -planetRadius * 0.35f,
            planetRadius * 1.5f, planetRadius * 0.35f,
            planetRingPaint
        )
        canvas.restore()

        for (s in nearStars) {
            starPaint.alpha = s.alpha
            canvas.drawCircle(s.x, s.y, s.radius, starPaint)
        }
    }
}
