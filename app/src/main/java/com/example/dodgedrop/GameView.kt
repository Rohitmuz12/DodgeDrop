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
    READY,
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
    private lateinit var starField: StarField
    private val particles = ParticleSystem()
    private val obstacles = mutableListOf<Obstacle>()
    private val powerUps = mutableListOf<PowerUp>()

    private var state = GameState.READY

    private var score = 0
    private var displayScore = 0f

    // --- Combo system ---
    // Consecutive clears build a multiplier; hitting a shield (not a clean clear) resets it.
    private var combo = 0
    private var comboFlashTime = 0f

    // --- Difficulty curve ---
    private val baseSpeed = 600f
    private var speed = baseSpeed
    private val maxSpeed = 1500f
    private var spawnTimer = 0f
    private var spawnInterval = 1.4f

    private var powerUpTimer = 0f
    private var powerUpInterval = 8f

    private var shakeTime = 0f
    private var shakeMagnitude = 0f
    private var flashAlpha = 0f
    private var isNewBest = false

    // --- Paint objects ---
    private val groundPaint = Paint().apply {
        color = Color.parseColor("#0D0820")
    }
    private val groundLinePaint = Paint().apply {
        color = Color.parseColor("#6C5CE7")
        alpha = 120
        strokeWidth = 3f
    }
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    private val comboPaint = Paint().apply {
        color = Color.parseColor("#FFD23F")
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#7FE7FF")
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#AAAAEE")
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
    private val readyDimPaint = Paint().apply { color = Color.parseColor("#AA05030F") }
    private val gameOverDimPaint = Paint().apply { color = Color.parseColor("#CC05030F") }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        groundY = screenHeight * 0.78f
        player = Player(groundY, screenWidth)
        starField = StarField(screenWidth, screenHeight)

        scorePaint.textSize = screenWidth * 0.09f
        comboPaint.textSize = screenWidth * 0.05f
        titlePaint.textSize = screenWidth * 0.105f
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
            } catch (e: InterruptedException) { }
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
        combo = 0
        speed = baseSpeed
        spawnInterval = 1.4f
        spawnTimer = 0f
        powerUpTimer = 0f
        obstacles.clear()
        powerUps.clear()
        particles.clear()
        player.reset()
        isNewBest = false
        scoreManager.incrementRuns()
    }

    fun update(dt: Float) {
        val speedMultiplier = if (state == GameState.PLAYING) speed / baseSpeed else 0.4f
        starField.update(dt, speedMultiplier)
        particles.update(dt)

        if (comboFlashTime > 0f) comboFlashTime -= dt
        if (shakeTime > 0f) {
            shakeTime -= dt
            shakeMagnitude *= 0.9f
        }
        if (flashAlpha > 0f) {
            flashAlpha -= dt * 3f
            if (flashAlpha < 0f) flashAlpha = 0f
        }

        if (state != GameState.PLAYING) return

        player.update(dt)

        // Thruster trail while airborne
        if (!player.isOnGround) {
            particles.emitTrail(player.x - player.radius, player.y, Color.parseColor("#7FE7FF"))
        }

        speed = (baseSpeed + score * 16f).coerceAtMost(maxSpeed)
        spawnInterval = (1.4f - score * 0.018f).coerceAtLeast(0.6f)

        spawnTimer += dt
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnObstacle()
        }

        powerUpTimer += dt
        if (powerUpTimer >= powerUpInterval) {
            powerUpTimer = 0f
            spawnPowerUp()
        }

        val playerBounds = player.getBounds()

        // Obstacles: move, score, collide
        val obstacleIterator = obstacles.iterator()
        while (obstacleIterator.hasNext()) {
            val obstacle = obstacleIterator.next()
            obstacle.update(dt, speed)

            if (!obstacle.passed && obstacle.getBounds().right < player.x) {
                obstacle.passed = true
                score += 1
                combo += 1
                comboFlashTime = 0.4f
                particles.emitBurst(player.x, player.y, Color.parseColor("#7FE7FF"), 8)
            }

            if (obstacle.getBounds().intersects(playerBounds)) {
                if (player.hasShield) {
                    player.hasShield = false
                    obstacleIterator.remove()
                    particles.emitBurst(obstacle.centerX(), obstacle.centerY(), Color.parseColor("#FFD23F"), 20)
                    combo = 0
                } else {
                    onGameOver()
                    break
                }
            } else if (obstacle.isOffScreen()) {
                obstacleIterator.remove()
            }
        }

        // Power-ups: move, collect
        val powerUpIterator = powerUps.iterator()
        while (powerUpIterator.hasNext()) {
            val powerUp = powerUpIterator.next()
            powerUp.update(dt, speed)

            if (powerUp.getBounds().intersects(playerBounds)) {
                player.hasShield = true
                particles.emitBurst(powerUp.x, powerUp.y(), Color.parseColor("#FFD23F"), 18)
                powerUpIterator.remove()
            } else if (powerUp.isOffScreen()) {
                powerUpIterator.remove()
            }
        }

        displayScore += (score - displayScore) * 0.2f
    }

    private fun spawnObstacle() {
        val size = screenWidth * (0.13f + Random.nextFloat() * 0.07f)
        obstacles.add(Obstacle(screenWidth.toFloat() + size, groundY, size))
    }

    private fun spawnPowerUp() {
        val radius = screenWidth * 0.035f
        val floatY = groundY - screenHeight * (0.18f + Random.nextFloat() * 0.12f)
        powerUps.add(PowerUp(screenWidth.toFloat() + radius, floatY, radius))
    }

    private fun onGameOver() {
        state = GameState.GAME_OVER
        isNewBest = scoreManager.submitScore(score)
        shakeTime = 0.3f
        shakeMagnitude = 22f
        flashAlpha = 0.7f
        particles.emitExplosion(player.x, player.y)
        combo = 0
    }

    fun render(canvas: Canvas) {
        canvas.save()

        if (shakeTime > 0f) {
            val dx = (Random.nextFloat() - 0.5f) * shakeMagnitude
            val dy = (Random.nextFloat() - 0.5f) * shakeMagnitude
            canvas.translate(dx, dy)
        }

        starField.draw(canvas)

        // Ground
        canvas.drawRect(0f, groundY, screenWidth.toFloat(), screenHeight.toFloat(), groundPaint)
        canvas.drawLine(0f, groundY, screenWidth.toFloat(), groundY, groundLinePaint)

        for (powerUp in powerUps) powerUp.draw(canvas)
        for (obstacle in obstacles) obstacle.draw(canvas)

        particles.draw(canvas)

        if (state == GameState.PLAYING || state == GameState.GAME_OVER) {
            player.draw(canvas)
        }

        if (state == GameState.PLAYING) {
            canvas.drawText(displayScore.toInt().toString(), screenWidth / 2f, screenHeight * 0.12f, scorePaint)
            if (combo >= 3) {
                val comboScale = if (comboFlashTime > 0f) 1.15f else 1f
                canvas.save()
                canvas.scale(comboScale, comboScale, screenWidth / 2f, screenHeight * 0.18f)
                canvas.drawText("${combo}x COMBO", screenWidth / 2f, screenHeight * 0.18f, comboPaint)
                canvas.restore()
            }
        }

        canvas.restore()

        when (state) {
            GameState.READY -> drawReadyOverlay(canvas)
            GameState.GAME_OVER -> drawGameOverOverlay(canvas)
            GameState.PLAYING -> {}
        }

        if (flashAlpha > 0f) {
            flashPaint.alpha = (flashAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), flashPaint)
        }
    }

    private fun drawReadyOverlay(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), readyDimPaint)

        canvas.drawText("DODGE DROP", screenWidth / 2f, screenHeight * 0.35f, titlePaint)
        canvas.drawText("Tap to launch", screenWidth / 2f, screenHeight * 0.42f, subtitlePaint)

        if (scoreManager.highScore > 0) {
            canvas.drawText("Best: ${scoreManager.highScore}", screenWidth / 2f, screenHeight * 0.5f, bestPaint)
        }

        canvas.drawText("TAP TO START", screenWidth / 2f, screenHeight * 0.65f, subtitlePaint)
    }

    private fun drawGameOverOverlay(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), gameOverDimPaint)

        canvas.drawText("SHIP LOST", screenWidth / 2f, screenHeight * 0.32f, titlePaint)
        canvas.drawText("Score: $score", screenWidth / 2f, screenHeight * 0.42f, scorePaint)

        if (isNewBest) {
            canvas.drawText("\u2605 NEW BEST! \u2605", screenWidth / 2f, screenHeight * 0.5f, bestPaint)
        } else {
            canvas.drawText("Best: ${scoreManager.highScore}", screenWidth / 2f, screenHeight * 0.5f, subtitlePaint)
        }

        canvas.drawText("TAP TO RETRY", screenWidth / 2f, screenHeight * 0.65f, subtitlePaint)
    }
}
