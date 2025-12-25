package com.donghwa.gameVerse.brickgame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

class Paddle(private val screenX: Int, private val screenY: Int) {
    // 패들 속성
    var x = 0f
    var width = 200f
    val height = 40f
    val bottomMargin = 600f
    val color = Color.MAGENTA

    // 감도 (속도)
    var sensitivity = 1.5f

    init {
        reset()
    }

    fun reset() {
        width = 200f
        x = (screenX / 2) - (width / 2)
    }

    // 패들의 현재 위치(RectF) 반환
    fun getRect(): RectF {
        return RectF(x, screenY - bottomMargin, x + width, screenY - bottomMargin + height)
    }

    // 패들 그리기
    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        canvas.drawRoundRect(getRect(), 10f, 10f, paint)
    }

    // 패들 이동 (드래그)
    fun move(dx: Float) {
        x += dx * sensitivity

        // 화면 밖으로 나가지 않게 제한
        if (x < 0) x = 0f
        if (x + width > screenX) x = screenX - width
    }

    // 아이템 효과로 크기 변경
    fun changeWidth(amount: Float) {
        val maxWidth = screenX / 2f
        val minWidth = 120f

        val oldWidth = width
        width = if (amount > 0) {
            min(width + amount, maxWidth)
        } else {
            max(width + amount, minWidth)
        }

        // 크기가 변할 때 중심을 유지하기 위해 위치 보정
        x -= (width - oldWidth) / 2

        // 보정 후 화면 밖 체크
        if (x < 0) x = 0f
        if (x + width > screenX) x = screenX - width
    }
}