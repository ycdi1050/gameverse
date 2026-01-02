package com.example.myapplication

data class Ball(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var isSuper: Boolean = false,
    var radius: Float = 20f
)