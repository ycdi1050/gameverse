package com.donghwa.gameVerse.brickgame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

// [통합] 공 데이터 클래스
data class Ball(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var isSuper: Boolean = false,
    var radius: Float = 20f
)

// 공 관리 클래스
class BallManager(private val screenX: Int, private val screenY: Int) {
    val balls = CopyOnWriteArrayList<Ball>()
    var ballSpeed = 28f
    private val random = Random()

    fun spawnInitialBall(paddle: Paddle) {
        balls.clear()
        addNewBall(paddle)
    }

    fun addNewBall(paddle: Paddle) {
        val startX = (screenX / 2).toFloat()
        val startY = (screenY - (paddle.bottomMargin + 100)).toFloat()
        val startDX = if (random.nextBoolean()) ballSpeed else -ballSpeed
        val startDY = -ballSpeed

        balls.add(Ball(startX, startY, startDX, startDY, false, 20f))
    }

    fun resetBallsForNextLevel(paddle: Paddle) {
        if (balls.isEmpty()) {
            addNewBall(paddle)
        } else {
            val startX = (screenX / 2).toFloat()
            val startY = (screenY - (paddle.bottomMargin + 100)).toFloat()

            for (ball in balls) {
                ball.x = startX
                ball.y = startY
                val direction = if (random.nextBoolean()) 1 else -1
                ball.dx = (direction * ballSpeed) + (random.nextFloat() * 6 - 3)
                ball.dy = -ballSpeed
            }
        }
    }

    fun update() {
        for (ball in balls) {
            ball.x += ball.dx
            ball.y += ball.dy

            // 벽 충돌 (좌우)
            if (ball.x - ball.radius <= 0) {
                ball.dx = abs(ball.dx)
                ball.x = ball.radius
            } else if (ball.x + ball.radius >= screenX) {
                ball.dx = -abs(ball.dx)
                ball.x = screenX - ball.radius
            }

            // 천장 충돌
            if (ball.y - ball.radius <= 0) {
                ball.dy = abs(ball.dy)
                ball.y = ball.radius
            }

            // 수직 이동 방지
            if (abs(ball.dx) < 2f) {
                ball.dx = if (ball.dx < 0) -2f else 2f
            }
        }
    }

    fun removeDeadBalls(): Boolean {
        var allDead = false
        for (ball in balls) {
            if (ball.y - ball.radius >= screenY) {
                balls.remove(ball)
            }
        }
        if (balls.isEmpty()) allDead = true
        return allDead
    }

    fun draw(canvas: Canvas, paint: Paint) {
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
    }
}