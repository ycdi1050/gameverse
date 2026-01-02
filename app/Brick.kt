package com.example.myapplication

import android.graphics.RectF

data class Brick(
    var rect: RectF,
    var isVisible: Boolean,
    var color: Int,
    var hp: Int = 1
)