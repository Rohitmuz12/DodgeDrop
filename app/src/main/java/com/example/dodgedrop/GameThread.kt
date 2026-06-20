package com.example.dodgedrop

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Dedicated thread that runs the game loop: update -> draw -> sleep, repeat.
 * Keeping this off the UI thread is what makes the game feel buttery smooth,
 * which matters a lot for "feel" and therefore for retention.
 */
class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {

    @Volatile
    var running = false

    private val targetFps = 60
    private val targetFrameTimeMs = 1000L / targetFps

    override fun run() {
        var lastTime = System.currentTimeMillis()

        while (running) {
            val startTime = System.currentTimeMillis()
            val deltaTime = (startTime - lastTime) / 1000f
            lastTime = startTime

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    gameView.update(deltaTime.coerceAtMost(0.05f)) // clamp to avoid huge jumps after a stall
                    gameView.draw(canvas)
                }
            } catch (e: Exception) {
                // Surface may not be ready yet; ignore and continue loop
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Surface might have been destroyed mid-frame
                    }
                }
            }

            val frameTime = System.currentTimeMillis() - startTime
            val sleepTime = targetFrameTimeMs - frameTime
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (e: InterruptedException) {
                    // ignore
                }
            }
        }
    }
}
