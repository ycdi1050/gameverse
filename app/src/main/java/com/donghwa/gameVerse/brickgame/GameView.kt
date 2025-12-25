package com.donghwa.gameVerse.brickgame

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.donghwa.gameVerse.ItemManager

class GameView(
    context: Context,
    private val onExit: () -> Unit,
    private val onGameOver: (Int) -> Unit
) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    private var isReady = false

    private val drawLock = Any()

    private var screenX = 0
    private var screenY = 0

    private lateinit var paddle: Paddle
    private lateinit var levelManager: LevelManager
    private lateinit var ballManager: BallManager
    private lateinit var itemManager: ItemManager
    private lateinit var uiManager: UIManager

    private var previousX = 0f

    private var brickSpeed = 0.2f
    private var bricks = ArrayList<Brick>()

    private var level = 1
    private var score = 0
    private var lives = 3
    private var isGameOver = false
    private var isScoreSaved = false

    override fun run() {
        while (isPlaying) {
            if (isReady) {
                synchronized(drawLock) {
                    if (!isGameOver && ::uiManager.isInitialized && !uiManager.isSettingsOpen) {
                        update()
                    }
                }
            }
            draw()
            control()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w
        screenY = h

        paddle = Paddle(w, h)
        levelManager = LevelManager(w, h)
        ballManager = BallManager(w, h)
        itemManager = ItemManager(w, h)

        uiManager = UIManager(
            screenX = w,
            screenY = h,
            onResume = {},
            onRestart = { resetGame() }, // 다시하기 버튼 클릭 시
            onExit = { onExit() },       // 홈으로/나가기 버튼 클릭 시
            onSensitivityChanged = { newSensitivity ->
                if (::paddle.isInitialized) {
                    paddle.sensitivity = newSensitivity
                }
            }
        )

        uiManager.setKnobPosition(paddle.sensitivity)

        resetGame()

        isReady = true
    }

    private fun resetGame() {
        synchronized(drawLock) {
            if (screenX == 0 || screenY == 0) return

            paddle.reset()
            level = 1
            bricks = levelManager.createBricks(level)
            itemManager.clear()

            score = 0
            lives = 3
            ballManager.ballSpeed = 28f
            brickSpeed = 0.2f
            isGameOver = false
            isScoreSaved = false

            if (::uiManager.isInitialized) {
                uiManager.isSettingsOpen = false
            }

            ballManager.spawnInitialBall(paddle)
        }
    }

    private fun update() {
        if (!isReady) return

        ballManager.update()
        itemManager.update()

        val deathLineY = screenY - paddle.bottomMargin
        var brickHitBottom = false

        for (brick in bricks) {
            if (brick.isVisible) {
                brick.rect.offset(0f, brickSpeed)
                if (brick.rect.bottom >= deathLineY) {
                    brickHitBottom = true
                }
            }
        }

        if (brickHitBottom) {
            handleLifeLost(resetBricks = true)
            return
        }

        if (ballManager.removeDeadBalls()) {
            handleLifeLost(resetBricks = false)
        }

        val paddleRect = paddle.getRect()

        for (ball in ballManager.balls) {
            val ballRect = RectF(ball.x - ball.radius, ball.y - ball.radius, ball.x + ball.radius, ball.y + ball.radius)

            if (RectF.intersects(paddleRect, ballRect)) {
                if (ball.dy > 0) {
                    val hitPoint = ball.x - (paddle.x + paddle.width / 2)
                    ball.dx = hitPoint / (paddle.width / 2) * ballManager.ballSpeed
                    ball.dy = -ball.dy
                }
            }

            for (brick in bricks) {
                if (brick.isVisible) {
                    if (RectF.intersects(brick.rect, ballRect)) {
                        if (brick.hp > 1) {
                            brick.hp--
                            ball.dy = -ball.dy
                            brick.color = Color.parseColor("#FF5252")
                        } else {
                            brick.isVisible = false
                            score += 10

                            if (!ball.isSuper) {
                                ball.dy = -ball.dy
                            }

                            itemManager.spawnItem(brick.rect.centerX(), brick.rect.centerY(), level)
                        }

                        if (bricks.none { it.isVisible }) {
                            level++
                            ballManager.ballSpeed += 3f
                            brickSpeed += 0.05f
                            bricks = levelManager.createBricks(level)
                            ballManager.resetBallsForNextLevel(paddle)
                        }
                        break
                    }
                }
            }
        }

        itemManager.checkCollision(paddleRect) { type ->
            when (type) {
                0 -> ballManager.addNewBall(paddle)
                1 -> paddle.changeWidth(50f)
                2 -> paddle.changeWidth(-50f)
                3 -> {
                    lives--
                    if (lives <= 0) {
                        isGameOver = true
                        if (!isScoreSaved) {
                            isScoreSaved = true
                            onGameOver(score)
                        }
                    }
                }
                4 -> { for (ball in ballManager.balls) ball.isSuper = true }
                5 -> { for (ball in ballManager.balls) ball.radius = 40f }
                6 -> lives++
            }
        }
    }

    private fun handleLifeLost(resetBricks: Boolean = false) {
        lives--
        if (lives <= 0) {
            isGameOver = true
            if (!isScoreSaved) {
                isScoreSaved = true
                onGameOver(score)
            }
        } else {
            ballManager.spawnInitialBall(paddle)
            paddle.reset()

            if (resetBricks) {
                itemManager.clear()
                bricks = levelManager.createBricks(level)
            }
        }
    }

    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas() ?: return

            try {
                synchronized(drawLock) {
                    canvas.drawColor(Color.parseColor("#121212"))

                    if (!isReady) {
                        paint.color = Color.WHITE
                        paint.textSize = 50f
                        paint.textAlign = Paint.Align.CENTER
                        val cx = if (screenX > 0) (screenX / 2).toFloat() else 500f
                        val cy = if (screenY > 0) (screenY / 2).toFloat() else 800f
                        canvas.drawText("Loading...", cx, cy, paint)
                        return
                    }

                    paint.color = Color.RED
                    paint.strokeWidth = 3f
                    paint.pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
                    val lineY = screenY - paddle.bottomMargin
                    canvas.drawLine(0f, lineY, screenX.toFloat(), lineY, paint)
                    paint.pathEffect = null
                    paint.style = Paint.Style.FILL

                    paddle.draw(canvas, paint)
                    ballManager.draw(canvas, paint)
                    itemManager.draw(canvas, paint)

                    for (brick in bricks) {
                        if (brick.isVisible) {
                            paint.color = brick.color
                            canvas.drawRoundRect(brick.rect, 10f, 10f, paint)
                        }
                    }

                    if (::uiManager.isInitialized) {
                        uiManager.draw(canvas, paint, score, level, lives, isGameOver)
                    }
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun control() {
        try {
            Thread.sleep(17)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isReady) return true

        val x = event.x

        // [수정] UI 매니저에게 터치 위임 (람다 제거)
        if (::uiManager.isInitialized && uiManager.handleTouch(event, isGameOver)) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = x
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isGameOver) {
                    synchronized(drawLock) {
                        val dx = x - previousX
                        paddle.move(dx)
                    }
                    previousX = x
                }
            }
        }
        return true
    }

    fun pause() {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        if (isPlaying) return
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }
}