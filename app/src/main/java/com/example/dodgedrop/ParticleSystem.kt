package com.example.dodgedrop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val radius: Float,
    val color: Int
)

/**
 * Pooled-ish particle system (simple ArrayList, fine at this scale) for visual juice:
 * thruster trail behind the player, celebratory bursts on clearing an obstacle,
 * and a satisfying explosion on death.
 */
class ParticleSystem {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint().apply { isAntiAlias = true }

    fun emitTrail(x: Float, y: Float, color: Int) {
        repeat(2) {
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 8f,
                    y = y + (Random.nextFloat() - 0.5f) * 8f,
                    vx = -120f - Random.nextFloat() * 80f,
                    vy = (Random.nextFloat() - 0.5f) * 60f,
                    life = 0.4f,
                    maxLife = 0.4f,
                    radius = Random.nextFloat() * 5f + 2f,
                    color = color
                )
            )
        }
    }

    fun emitBurst(x: Float, y: Float, color: Int, count: Int = 14) {
        repeat(count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 150f + Random.nextFloat() * 200f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    life = 0.5f + Random.nextFloat() * 0.3f,
                    maxLife = 0.8f,
                    radius = Random.nextFloat() * 6f + 3f,
                    color = color
                )
            )
        }
    }

    fun emitExplosion(x: Float, y: Float) {
        val colors = intArrayOf(
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#FFD23F"),
            Color.parseColor("#FF9F43"),
            Color.WHITE
        )
        repeat(40) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 200f + Random.nextFloat() * 450f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    life = 0.6f + Random.nextFloat() * 0.5f,
                    maxLife = 1.1f,
                    radius = Random.nextFloat() * 8f + 3f,
                    color = colors[Random.nextInt(colors.size)]
                )
            )
        }
    }

    fun update(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vx *= 0.94f
            p.vy *= 0.94f
            p.life -= dt
            if (p.life <= 0f) iterator.remove()
        }
    }

    fun draw(canvas: Canvas) {
        for (p in particles) {
            val lifeRatio = (p.life / p.maxLife).coerceIn(0f, 1f)
            paint.color = p.color
            paint.alpha = (lifeRatio * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.radius * lifeRatio, paint)
        }
    }

    fun clear() = particles.clear()
}
