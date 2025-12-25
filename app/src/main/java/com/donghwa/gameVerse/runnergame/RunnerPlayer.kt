package com.donghwa.gameVerse.runnergame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class RunnerPlayer(private val screenX: Int, private val screenY: Int) {
    val width = 100f
    val height = 100f

    var x = 150f // 왼쪽 고정 위치
    var y = 0f   // 높이 (변함)

    private var dy = 0f // 수직 속도

    // [수정] 점프 및 착지 속도 개선
    // 기존: gravity 1.5 / jump -35 -> 붕 뜨는 느낌
    // 변경: gravity 3.5 / jump -60 -> 빠르고 경쾌한 느낌
    private val gravity = 3.5f
    private val jumpStrength = -60f

    private val groundY = screenY - 200f // 바닥 높이
    private var isGrounded = false

    init {
        reset()
    }

    fun reset() {
        y = groundY - height
        dy = 0f
        isGrounded = true
    }

    fun update() {
        // 중력 적용
        dy += gravity
        y += dy

        // 바닥 충돌 체크
        if (y + height > groundY) {
            y = groundY - height
            dy = 0f
            isGrounded = true
        } else {
            isGrounded = false
        }
    }

    fun jump() {
        if (isGrounded) {
            dy = jumpStrength
            isGrounded = false
        }
    }

    fun getRect(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = Color.CYAN
        canvas.drawRect(getRect(), paint)
    }
}