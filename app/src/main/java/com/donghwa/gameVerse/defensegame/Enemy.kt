package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF

class Enemy(
    private val path: List<PointF>,
    var maxHp: Int,
    val defense: Int,
    private val speed: Float,
    private val stage: Int
) : GameObject {

    var hp = maxHp
    var currentPathIndex = 0
    var x = -100f
    var y = -100f
    val radius = 30f + (stage * 2)
    var isDead = false
    var reachedEnd = false

    // 도트 데미지 관련
    private var poisonDuration = 0
    private var poisonDamage = 0
    private var lastPoisonTime = 0L

    init {
        if (path.isNotEmpty()) {
            x = path[0].x
            y = path[0].y
        }
    }

    override fun update() {
        if (isDead || reachedEnd || path.isEmpty()) return

        // 이동 로직
        if (currentPathIndex < path.size - 1) {
            val target = path[currentPathIndex + 1]
            val dx = target.x - x
            val dy = target.y - y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (dist <= speed) {
                x = target.x
                y = target.y
                currentPathIndex++
                if (currentPathIndex >= path.size - 1) reachedEnd = true
            } else {
                x += (dx / dist) * speed
                y += (dy / dist) * speed
            }
        }

        // 도트 데미지 처리
        if (poisonDuration > 0) {
            val now = System.currentTimeMillis()
            if (now - lastPoisonTime >= 1000L) { // 1초마다 피해
                takeDamageNoDefense(poisonDamage)
                poisonDuration--
                lastPoisonTime = now
            }
        }
    }

    // 도트 적용
    fun applyPoison(damage: Int, durationSeconds: Int) {
        poisonDamage = damage
        poisonDuration = durationSeconds
        lastPoisonTime = System.currentTimeMillis()
    }

    fun takeDamage(rawDamage: Int): Boolean {
        val actualDamage = (rawDamage - defense).coerceAtLeast(1)
        hp -= actualDamage
        if (hp <= 0) {
            isDead = true
            return true
        }
        return false
    }

    // 방어력 무시 데미지 (도트 등)
    private fun takeDamageNoDefense(amount: Int) {
        hp -= amount
        if (hp <= 0) isDead = true
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        if (isDead) return

        paint.style = Paint.Style.FILL
        // 중독 상태면 녹색 섞임
        if (poisonDuration > 0) {
            paint.color = Color.GREEN
        } else {
            paint.color = when ((stage - 1) % 3) {
                0 -> Color.RED
                1 -> Color.MAGENTA
                else -> Color.parseColor("#FF5722")
            }
        }
        canvas.drawCircle(x, y, radius, paint)

        // 체력바
        val hpRatio = hp.toFloat() / maxHp
        paint.color = Color.RED
        canvas.drawRect(x - 30, y - radius - 20, x + 30, y - radius - 10, paint)
        paint.color = Color.GREEN
        canvas.drawRect(x - 30, y - radius - 20, x - 30 + (60 * hpRatio), y - radius - 10, paint)
    }
}