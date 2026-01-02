package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

class GameView(context: Context) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()
    private val random = Random()

    private val drawLock = Any()

    private var screenX = 0
    private var screenY = 0

    // 터치 드래그 처리를 위한 변수
    private var previousX = 0f

    // 패들 드래그 속도(감도) 변수 (기본 1.5배)
    private var paddleSensitivity = 1.5f

    // 설정 메뉴 관련 변수
    private var isSettingsOpen = false
    private val gearRect = RectF()

    // 슬라이더 UI 관련 변수
    private val sliderRect = RectF()
    private var knobX = 0f
    private val knobRadius = 30f
    private var isDraggingSlider = false

    private var paddleX = 0f
    private var paddleWidth = 200f
    private val paddleHeight = 40f
    private val paddleBottomMargin = 600f
    private val paddleColor = Color.MAGENTA

    private val balls = CopyOnWriteArrayList<Ball>()

    private var ballSpeed = 28f

    private val items = CopyOnWriteArrayList<Item>()

    private val bricks = ArrayList<Brick>()

    private val brickCols = 10

    private var level = 1
    private var score = 0
    private var lives = 3
    private var isGameOver = false

    override fun run() {
        while (isPlaying) {
            synchronized(drawLock) {
                if (!isGameOver && !isSettingsOpen) {
                    update()
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

        // 1. 톱니바퀴 버튼 위치 (우측 상단)
        val gearSize = 80f
        val margin = 50f
        gearRect.set(w - gearSize - margin, margin, w - margin, margin + gearSize)

        // 2. 슬라이더 위치 (화면 중앙 팝업 형태)
        val sliderWidth = w * 0.7f
        val sliderHeight = 10f
        val sliderLeft = (w - sliderWidth) / 2
        val sliderTop = h / 2f
        val sliderBottom = sliderTop + sliderHeight

        sliderRect.set(sliderLeft, sliderTop, sliderLeft + sliderWidth, sliderBottom)

        // 감도에 따른 노브 위치 초기화
        updateKnobPositionFromSensitivity()

        resetGame()
    }

    private fun updateKnobPositionFromSensitivity() {
        val sensitivityRange = 3.0f
        val normalized = (paddleSensitivity - 0.5f) / sensitivityRange
        knobX = sliderRect.left + (normalized * sliderRect.width())
    }

    private fun resetGame() {
        synchronized(drawLock) {
            if (screenX == 0 || screenY == 0) return

            paddleWidth = 200f
            paddleX = (screenX / 2) - (paddleWidth / 2)

            level = 1
            createBricks()

            items.clear()

            score = 0
            lives = 3
            ballSpeed = 28f
            isGameOver = false
            isSettingsOpen = false

            spawnInitialBall()
        }
    }

    private fun spawnInitialBall() {
        balls.clear()
        addNewBall()
    }

    private fun resetBallsForNextLevel() {
        if (balls.isEmpty()) {
            addNewBall()
        } else {
            val startX = (screenX / 2).toFloat()
            val startY = (screenY - (paddleBottomMargin + 100)).toFloat()

            for (ball in balls) {
                ball.x = startX
                ball.y = startY
                val direction = if (random.nextBoolean()) 1 else -1
                ball.dx = (direction * ballSpeed) + (random.nextFloat() * 6 - 3)
                ball.dy = -ballSpeed
            }
        }
    }

    private fun addNewBall() {
        val startX = (screenX / 2).toFloat()
        val startY = (screenY - (paddleBottomMargin + 100)).toFloat()
        val startDX = if (random.nextBoolean()) ballSpeed else -ballSpeed
        val startDY = -ballSpeed
        balls.add(Ball(startX, startY, startDX, startDY, false, 20f))
    }

    private fun createBricks() {
        bricks.clear()
        val brickWidth = (screenX / brickCols).toFloat()
        val brickHeight = 50f
        val padding = 10f
        val offsetTop = 300f

        val currentRows = min(3 + level, 8)

        val colors = arrayOf(
            Color.parseColor("#FF5252"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#E040FB"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#536DFE"),
            Color.parseColor("#448AFF")
        )

        for (row in 0 until currentRows) {
            for (col in 0 until brickCols) {
                if (level == 1) {
                    if (col == 2 || col == 7) continue
                } else if (level == 2) {
                    if (col == 4 || col == 5) continue
                }

                val rect = RectF(
                    (col * brickWidth) + padding,
                    (row * brickHeight) + offsetTop + padding,
                    ((col + 1) * brickWidth) - padding,
                    ((row + 1) * brickHeight) + offsetTop - padding
                )

                var hp = 1
                var color = colors[row % colors.size]

                if (level >= 3 && random.nextFloat() < 0.2f) {
                    hp = 2
                    color = Color.LTGRAY
                }

                bricks.add(Brick(rect, true, color, hp))
            }
        }
    }

    private fun update() {
        if (screenX == 0) return

        if (paddleX < 0) paddleX = 0f
        if (paddleX + paddleWidth > screenX) paddleX = screenX - paddleWidth

        val paddleRect = RectF(paddleX, screenY - paddleBottomMargin, paddleX + paddleWidth, screenY - paddleBottomMargin + paddleHeight)

        for (ball in balls) {
            ball.x += ball.dx
            ball.y += ball.dy

            if (ball.x - ball.radius <= 0 || ball.x + ball.radius >= screenX) ball.dx = -ball.dx
            if (ball.y - ball.radius <= 0) ball.dy = -ball.dy

            if (ball.y - ball.radius >= screenY) {
                balls.remove(ball)
                continue
            }

            val ballRect = RectF(ball.x - ball.radius, ball.y - ball.radius, ball.x + ball.radius, ball.y + ball.radius)

            if (RectF.intersects(paddleRect, ballRect)) {
                if (ball.dy > 0) {
                    val hitPoint = ball.x - (paddleX + paddleWidth / 2)
                    ball.dx = hitPoint / (paddleWidth / 2) * ballSpeed
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

                            if (random.nextFloat() < 0.4f) {
                                var type = random.nextInt(3)
                                val rand = random.nextFloat()
                                if (level >= 2 && rand < 0.3f) type = 3
                                else if (rand < 0.35f) type = 4
                                else if (rand < 0.45f) type = 5
                                else if (rand < 0.50f) type = 6
                                items.add(Item(brick.rect.centerX(), brick.rect.centerY(), type))
                            }
                        }

                        if (bricks.none { it.isVisible }) {
                            level++
                            ballSpeed += 3f
                            createBricks()
                            resetBallsForNextLevel()
                        }
                        break
                    }
                }
            }
        }

        for (item in items) {
            item.y += 12f
            if (item.y > screenY) {
                items.remove(item)
                continue
            }

            val itemRect = RectF(item.x - item.width/2, item.y - item.height/2, item.x + item.width/2, item.y + item.height/2)
            if (RectF.intersects(paddleRect, itemRect)) {
                when (item.type) {
                    0 -> addNewBall()
                    1 -> {
                        val maxWidth = screenX / 2f
                        paddleWidth = min(paddleWidth + 50f, maxWidth)
                        paddleX -= 25f
                    }
                    2 -> {
                        paddleWidth = max(paddleWidth - 50f, 120f)
                        paddleX += 25f
                    }
                    3 -> {
                        lives--
                        if (lives <= 0) isGameOver = true
                    }
                    4 -> { for (ball in balls) ball.isSuper = true }
                    5 -> { for (ball in balls) ball.radius = 40f }
                    6 -> lives++
                }
                items.remove(item)
            }
        }

        if (balls.isEmpty()) {
            lives--
            if (lives <= 0) isGameOver = true
            else {
                paddleWidth = 200f
                spawnInitialBall()
            }
        }
    }

    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas() ?: return

            try {
                synchronized(drawLock) {
                    canvas.drawColor(Color.parseColor("#121212"))

                    if (screenX == 0 || screenY == 0) {
                        paint.color = Color.WHITE
                        paint.textSize = 50f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText("Loading...", (canvas.width / 2).toFloat(), (canvas.height / 2).toFloat(), paint)
                        return
                    }

                    // UI 그리기
                    paint.color = Color.WHITE
                    paint.textSize = 60f
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText("Score: $score", 50f, 100f, paint)

                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("Lv: $level", (screenX / 2).toFloat(), 100f, paint)

                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("Lives: $lives", (screenX - 160).toFloat(), 100f, paint)

                    // 톱니바퀴 아이콘 그리기
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = 60f
                    paint.color = Color.LTGRAY
                    canvas.drawText("⚙️", gearRect.centerX(), gearRect.centerY() + 20f, paint)

                    // 설정창(팝업) 그리기
                    if (isSettingsOpen) {
                        canvas.drawColor(Color.argb(150, 0, 0, 0))

                        paint.color = Color.DKGRAY
                        val popupRect = RectF(sliderRect.left - 50f, sliderRect.top - 150f, sliderRect.right + 50f, sliderRect.bottom + 100f)
                        canvas.drawRoundRect(popupRect, 30f, 30f, paint)

                        paint.color = Color.WHITE
                        paint.textSize = 50f
                        canvas.drawText("패들 감도 설정", popupRect.centerX(), popupRect.top + 60f, paint)

                        paint.textSize = 40f
                        canvas.drawText("느림", sliderRect.left, sliderRect.bottom + 60f, paint)
                        canvas.drawText("빠름", sliderRect.right, sliderRect.bottom + 60f, paint)

                        paint.color = Color.CYAN
                        canvas.drawText(String.format("x%.1f", paddleSensitivity), knobX, sliderRect.top - 30f, paint)

                        paint.color = Color.GRAY
                        canvas.drawRoundRect(sliderRect, 10f, 10f, paint)

                        paint.color = Color.WHITE
                        canvas.drawCircle(knobX, sliderRect.centerY(), knobRadius, paint)

                        paint.color = Color.LTGRAY
                        paint.textSize = 35f
                        canvas.drawText("(화면 빈 곳을 터치하면 닫기)", popupRect.centerX(), popupRect.bottom - 30f, paint)

                    } else if (isGameOver) {
                        paint.textSize = 100f
                        paint.textAlign = Paint.Align.CENTER
                        paint.color = Color.RED
                        canvas.drawText("GAME OVER", (screenX / 2).toFloat(), (screenY / 2).toFloat(), paint)
                        paint.textSize = 50f
                        paint.color = Color.WHITE
                        canvas.drawText("Tap to Restart", (screenX / 2).toFloat(), (screenY / 2 + 100).toFloat(), paint)
                    } else {
                        // 게임 화면 그리기
                        paint.color = paddleColor
                        val paddleRect = RectF(paddleX, screenY - paddleBottomMargin, paddleX + paddleWidth, screenY - paddleBottomMargin + paddleHeight)
                        canvas.drawRoundRect(paddleRect, 10f, 10f, paint)

                        for (ball in balls) {
                            paint.color = if (ball.isSuper) Color.parseColor("#FF5722") else Color.YELLOW
                            canvas.drawCircle(ball.x, ball.y, ball.radius, paint)

                            if (ball.isSuper) {
                                paint.style = Paint.Style.STROKE
                                paint.strokeWidth = 5f
                                paint.color = Color.RED
                                canvas.drawCircle(ball.x, ball.y, ball.radius + 5, paint)
                                paint.style = Paint.Style.FILL
                            }
                        }

                        for (item in items) {
                            paint.color = when (item.type) {
                                0 -> Color.GREEN
                                1 -> Color.CYAN
                                2 -> Color.RED
                                3 -> Color.DKGRAY
                                4 -> Color.parseColor("#FFC107")
                                5 -> Color.parseColor("#9C27B0")
                                6 -> Color.parseColor("#E91E63")
                                else -> Color.WHITE
                            }

                            if (item.type == 6) {
                                paint.textSize = 50f
                                paint.textAlign = Paint.Align.CENTER
                                val baseline = item.y - (paint.descent() + paint.ascent()) / 2
                                canvas.drawText("♥", item.x, baseline, paint)
                            } else {
                                val itemRect = RectF(item.x - item.width/2, item.y - item.height/2, item.x + item.width/2, item.y + item.height/2)
                                canvas.drawRoundRect(itemRect, 5f, 5f, paint)

                                if (item.type >= 3) {
                                    paint.style = Paint.Style.STROKE
                                    paint.strokeWidth = 3f
                                    paint.color = if(item.type==3) Color.RED else Color.WHITE
                                    canvas.drawRoundRect(itemRect, 5f, 5f, paint)
                                    paint.style = Paint.Style.FILL
                                }
                            }
                        }

                        for (brick in bricks) {
                            if (brick.isVisible) {
                                paint.color = brick.color
                                canvas.drawRoundRect(brick.rect, 10f, 10f, paint)
                            }
                        }
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
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isSettingsOpen) {
                    val touchPadding = 50f
                    if (x >= sliderRect.left - touchPadding && x <= sliderRect.right + touchPadding &&
                        y >= sliderRect.top - touchPadding && y <= sliderRect.bottom + touchPadding) {
                        isDraggingSlider = true
                        updateKnobAndSensitivity(x)
                    } else {
                        isSettingsOpen = false
                    }
                } else {
                    if (x >= gearRect.left && x <= gearRect.right &&
                        y >= gearRect.top && y <= gearRect.bottom) {
                        isSettingsOpen = true
                    }
                    else if (isGameOver) {
                        resetGame()
                    }
                    else {
                        previousX = x
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isSettingsOpen) {
                    if (isDraggingSlider) {
                        updateKnobAndSensitivity(x)
                    }
                } else if (!isGameOver) {
                    synchronized(drawLock) {
                        val dx = (x - previousX) * paddleSensitivity
                        paddleX += dx

                        if (paddleX < 0) paddleX = 0f
                        if (paddleX + paddleWidth > screenX) paddleX = screenX - paddleWidth
                    }
                    previousX = x
                }
            }
            MotionEvent.ACTION_UP -> {
                isDraggingSlider = false
            }
        }
        return true
    }

    private fun updateKnobAndSensitivity(touchX: Float) {
        knobX = max(sliderRect.left, min(touchX, sliderRect.right))
        val percentage = (knobX - sliderRect.left) / sliderRect.width()
        paddleSensitivity = 0.5f + (percentage * 3.0f)
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
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }
}