package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF

class Enemy(
    private val path: List<PointF>,
    var maxHp: Int,
    val defense: Int,
    private val baseSpeed: Float, // [수정] 이름 변경 (speed -> baseSpeed)
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

    // [수정] 배속 반영
    override fun update(speedMultiplier: Int) {
        if (isDead || reachedEnd || path.isEmpty()) return

        // 배속에 따른 실제 이동 속도 계산
        val currentSpeed = baseSpeed * speedMultiplier

        // 이동 로직
        if (currentPathIndex < path.size - 1) {
            val target = path[currentPathIndex + 1]
            val dx = target.x - x
            val dy = target.y - y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (dist <= currentSpeed) {
                x = target.x
                y = target.y
                currentPathIndex++
                if (currentPathIndex >= path.size - 1) reachedEnd = true
            } else {
                x += (dx / dist) * currentSpeed
                y += (dy / dist) * currentSpeed
            }
        }

        // 도트 데미지 처리 (배속 시 더 빨리 틱이 돔)
        if (poisonDuration > 0) {
            val now = System.currentTimeMillis()
            // 기본 1초(1000ms) 간격 -> 배속 시 간격 감소
            val tickInterval = 1000L / speedMultiplier

            if (now - lastPoisonTime >= tickInterval) {
                takeDamageNoDefense(poisonDamage)
                // 지속 시간 감소 로직도 틱 단위로 처리한다고 가정
                // (정확한 시간 처리를 위해선 복잡해지므로 단순화: 틱 발생 시 1초치 감소)
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

    private fun takeDamageNoDefense(amount: Int) {
        hp -= amount
        if (hp <= 0) isDead = true
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        if (isDead) return

        paint.style = Paint.Style.FILL
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