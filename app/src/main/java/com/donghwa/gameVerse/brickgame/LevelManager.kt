package com.donghwa.gameVerse.brickgame

import android.graphics.Color
import android.graphics.RectF
import java.util.ArrayList
import java.util.Random
import kotlin.math.max
import kotlin.math.min

// [통합] 벽돌 데이터 클래스
data class Brick(
    var rect: RectF,
    var isVisible: Boolean,
    var color: Int,
    var hp: Int = 1
) {
    companion object {
        const val COLS = 6
        const val HEIGHT = 100f
        const val PADDING = 15f
    }
}

// 레벨 및 벽돌 배치 관리 클래스
class LevelManager(private val screenX: Int, private val screenY: Int) {

    private val baseBrickCols = Brick.COLS
    private val random = Random()

    fun createBricks(level: Int): ArrayList<Brick> {
        val bricks = ArrayList<Brick>()

        val currentCols = baseBrickCols + (level - 1)
        val brickWidth = (screenX / currentCols).toFloat()

        val currentHeight = max(40f, Brick.HEIGHT - ((level - 1) * 5f))

        val padding = Brick.PADDING
        val offsetTop = 300f

        val currentRows = min(4 + level, 10)

        val colors = arrayOf(
            Color.parseColor("#FF5252"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#E040FB"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#536DFE"),
            Color.parseColor("#448AFF"),
            Color.parseColor("#00E5FF")
        )

        for (row in 0 until currentRows) {
            for (col in 0 until currentCols) {

                var shouldSkip = false

                when (level) {
                    1 -> {
                        val centerStart = currentCols / 2 - 1
                        val centerEnd = currentCols / 2
                        if (col in centerStart..centerEnd) shouldSkip = true
                    }
                    2 -> {
                        if ((row + col) % 2 == 0) shouldSkip = true
                    }
                    3 -> {
                        if (random.nextFloat() < 0.3f) shouldSkip = true
                    }
                    4 -> {
                        if ((row + col) % 3 == 0) shouldSkip = true
                    }
                }

                if (shouldSkip) continue

                val rect = RectF(
                    (col * brickWidth) + padding,
                    (row * currentHeight) + offsetTop + padding,
                    ((col + 1) * brickWidth) - padding,
                    ((row + 1) * currentHeight) + offsetTop - padding
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
        return bricks
    }
}