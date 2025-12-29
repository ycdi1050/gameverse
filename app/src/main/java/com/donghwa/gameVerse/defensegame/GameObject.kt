package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Paint

interface GameObject {
    fun update()
    fun draw(canvas: Canvas, paint: Paint)
}