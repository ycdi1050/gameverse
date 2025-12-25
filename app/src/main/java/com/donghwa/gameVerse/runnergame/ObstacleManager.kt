package com.donghwa.gameVerse.runnergame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList

class ObstacleManager(private val screenX: Int, private val screenY: Int) {
    val obstacles = CopyOnWriteArrayList<RectF>()
    private val random = Random()

    private val groundY = screenY - 200f

    private var spawnTimer = 0
    private var currentSpawnInterval = 60

    fun reset() {
        obstacles.clear()
        spawnTimer = 0
        currentSpawnInterval = 60
    }

    // [수정] Score(Int) 대신 DifficultyManager를 받도록 변경
    fun update(difficultyManager: DifficultyManager) {
        val speed = difficultyManager.speed

        // 장애물 이동
        for (obs in obstacles) {
            obs.offset(-speed, 0f)
            // 화면 밖으로 나가면 삭제
            if (obs.right < 0) {
                obstacles.remove(obs)
            }
        }

        // 장애물 생성 로직
        spawnTimer++
        if (spawnTimer > currentSpawnInterval) {
            spawnObstacle()
            spawnTimer = 0

            // 다음 장애물 생성 간격을 난이도에 따라 랜덤 설정
            val range = difficultyManager.spawnIntervalRange

            // 범위 안전 처리
            val minInterval = range.first
            val maxInterval = if (range.last < range.first) range.first else range.last

            currentSpawnInterval = random.nextInt(maxInterval - minInterval + 1) + minInterval
        }
    }

    private fun spawnObstacle() {
        val height = 100f + random.nextInt(100) // 높이 랜덤
        val width = 80f

        val left = screenX.toFloat()
        val top = groundY - height
        val right = left + width
        val bottom = groundY

        obstacles.add(RectF(left, top, right, bottom))
    }

    fun checkCollision(playerRect: RectF): Boolean {
        for (obs in obstacles) {
            if (RectF.intersects(playerRect, obs)) {
                return true // 충돌!
            }
        }
        return false
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = Color.RED
        for (obs in obstacles) {
            canvas.drawRect(obs, paint)
        }
    }
}