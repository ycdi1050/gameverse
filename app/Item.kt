package com.example.myapplication

// type - 0: 멀티볼, 1: 패들 확대, 2: 패들 축소, 3: 폭탄, 4: 무적볼, 5: 거대볼, 6: 목숨추가
data class Item(
    var x: Float,
    var y: Float,
    val type: Int,
    val width: Float = 40f,
    val height: Float = 40f
)