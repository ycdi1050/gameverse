package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Paint

interface GameObject {
    // [수정] 배속 처리를 위해 speedMultiplier 파라미터 추가 (기본값 1)
    fun update(speedMultiplier: Int = 1)
    fun draw(canvas: Canvas, paint: Paint)
}