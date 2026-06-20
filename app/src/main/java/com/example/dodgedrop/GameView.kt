package com.example.dodgedrop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

enum class GameState {
    READY,    // waiting for first tap
    PLAYING,
    GAME_OVER
}

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private val scoreManager = ScoreManager(context)

    private var screenWidth = 0
    private var screenHeight = 0
    private var groundY = 0f

    private lateinit var player: Player
    private val obstacles = mutableListOf<Obstacle>()

    private var state = GameState.READY

    private var score = 0
    private var displayScore = 0f // smoothly animates toward `score` for a satisfying tick-up feel

    // --- Difficulty curve ---
    // Starts gentle so anyone can succeed on attempt 1 (critical for hooking new players),
    // then ramps so mastery still feels rewarding over time.
    private var baseSpeed = 600f
    private var speed = baseSpeed
    private val maxSpeed = 1400f
    private var spawnTimer = 0f
    private var spawnInterval = 1.4f

    // Screen shake on death — small but punchy negative feedback that doesn't feel unfair
    private var shakeTime = 0f
    private var shakeMagnitude = 0f

    private var isNewBest = false

    // --- Paint objects (created once, reused every frame for performance) ---
    private val bgPaint = Paint().apply { color = Color.parseColor("#1A1A2E") }
    private val groundPaint = Paint().apply { color = Color.parseColor("#16162A") }
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#AAAAAA")
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val bestPaint = Paint().apply {
        color = Color.parseColor("#FFD23F")
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    private val flashPaint = Paint().apply { color = Color.WHITE }
    private var flashAlpha = 0f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        groundY = screenHeight * 0.78f
        player = Player(groundY, screenWidth)

        scorePaint.textSize = screenWidth * 0.08f
        titlePaint.textSize = screenWidth * 0.11f
        subtitlePaint.textSize = screenWidth * 0.045f
        bestPaint.textSize = screenWidth * 0.06f

        gameThread = GameThread(holder, this)
        gameThread?.running = true
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameThread?.running = false
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                // keep trying
            }
        }
    }

    fun resume() {
        if (gameThread == null || gameThread?.isAlive == false) {
            gameThread = GameThread(holder, this)
            gameThread?.running = true
            gameThread?.start()
        }
    }

    fun pause() {
        gameThread?.running = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (state) {
                GameState.READY -> startGame()
                GameState.PLAYING -> player.jump()
                GameState.GAME_OVER -> startGame()
            }
        }
        return true
    }

    private fun startGame() {
        state = GameState.PLAYING
        score = 0
        displayScore = 0f
        speed = baseSpeed
        spawnInterval = 1.4f
        spawnTimer = 0f
        obstacles.clear()
        player.reset()
        isNewBest = false
        scoreManager.incrementRuns()
    }

    fun update(dt: Float) {
        if (state != GameState.PLAYING) return

        player.update(dt)

        // Difficulty ramp: speed up gradually based on score, capped at maxSpeed
        speed = (baseSpeed + score * 14f).coerceAtMost(maxSpeed)
        spawnInterval = (1.4f - score * 0.015f).coerceAtLeast(0.65f)

        // Spawn obstacles
        spawnTimer += dt
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnObstacle()
        }

        // Update obstacles, check scoring + collision
        val iterator = obstacles.iterator()
        val playerBounds = player.getBounds()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.update(dt, speed)

            if (!obstacle.passed && obstacle.getBounds().right < player.x) {
                obstacle.passed = true
                score += 1
            }

            if (obstacle.getBounds().intersects(playerBounds)) {
                onGameOver()
                break
            }

            if (obstacle.isOffScreen()) {
                iterator.remove()
            }
        }

        // Smoothly animate displayed score toward actual score (satisfying tick-up)
        displayScore += (score - displayScore) * 0.2f

        // Decay shake
        if (shakeTime > 0f) {
            shakeTime -= dt
            shakeMagnitude *= 0.9f
        }
        if (flashAlpha > 0f) {
            flashAlpha -= dt * 3f
            if (flashAlpha < 0f) flashAlpha = 0f
        }
    }

    private fun spawnObstacle() {
        val width = screenWidth * 0.09f
        val height = screenHeight * (0.10f + Random.nextFloat() * 0.06f)
        obstacles.add(Obstacle(screenWidth.toFloat() + width, groundY, width, height))
    }

    private fun onGameOver() {
        state = GameState.GAME_OVER
        isNewBest = scoreManager.submitScore(score)
        shakeTime = 0.25f
        shakeMagnitude = 18f
        flashAlpha = 0.6f
    }

    fun draw(canvas: Canvas) {
        canvas.save()

        // Apply screen shake offset
        if (shakeTime > 0f) {
            val dx = (Random.nextFloat() - 0.5f) * shakeMagnitude
            val dy = (Random.nextFloat() - 0.5f) * shakeMagnitude
            canvas.translate(dx, dy)
        }

        // Background
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bgPaint)
        canvas.drawRect(0f, groundY, screenWidth.toFloat(), screenHeight.toFloat(), groundPaint)

        // Obstacles
        for (obstacle in obstacles) {
            obstacle.draw(canvas)
        }

        // Player
        player.draw(canvas)

        // Score (always visible during play — keeps the goal front and center)
        if (state == GameState.PLAYING) {
            canvas.drawText(displayScore.toInt().toString(), screenWidth / 2f, screenHeight * 0.12f, scorePaint)
        }

        canvas.restore() // shake shouldn't affect UI overlays below

        when (state) {
            GameState.READY -> drawReadyOverlay(canvas)
            GameState.GAME_OVER -> drawGameOverOverlay(canvas)
            GameState.PLAYING -> {}
        }

        // Death flash
        if (flashAlpha > 0f) {
            flashPaint.alpha = (flashAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), flashPaint)
        }
    }

    private fun drawReadyOverlay(canvas: Canvas) {
        val dimPaint = Paint().apply { color = Color.parseColor("#88000000") }
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), dimPaint)

        canvas.drawText("DODGE DROP", screenWidth / 2f, screenHeight * 0.35f, titlePaint)
        canvas.drawText("Tap to jump", screenWidth / 2f, screenHeight * 0.42f, subtitlePaint)

        if (scoreManager.highScore > 0) {
            canvas.drawText("Best: ${scoreManager.highScore}", screenWidth / 2f, screenHeight * 0.5f, bestPaint)
        }

        canvas.drawText("TAP TO START", screenWidth / 2f, screenHeight * 0.65f, subtitlePaint)
    }

    private fun drawGameOverOverlay(canvas: Canvas) {
        val dimPaint = Paint().apply { color = Color.parseColor("#AA000000") }
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), dimPaint)

        canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight * 0.32f, titlePaint)
        canvas.drawText("Score: $score", screenWidth / 2f, screenHeight * 0.42f, scorePaint)

        if (isNewBest) {
            canvas.drawText("\u2605 NEW BEST! \u2605", screenWidth / 2f, screenHeight * 0.5f, bestPaint)
        } else {
            canvas.drawText("Best: ${scoreManager.highScore}", screenWidth / 2f, screenHeight * 0.5f, subtitlePaint)
        }

        canvas.drawText("TAP TO RETRY", screenWidth / 2f, screenHeight * 0.65f, subtitlePaint)
    }
}
