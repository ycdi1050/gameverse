package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Paint
import java.util.HashSet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Projectile(
    var x: Float,
    var y: Float,
    val originX: Float,
    val originY: Float,
    var target: Enemy?,
    var damage: Int,
    val color: Int,
    val weaponType: WeaponType
) : GameObject {

    var hasHit = false
    var speed = 25f
    private var angle: Double = 0.0

    var ricochetCount = 0
    val hitTargets = HashSet<Enemy>()

    init {
        updateAngle()
    }

    fun updateAngle() {
        if (target != null) {
            angle = atan2((target!!.y - y).toDouble(), (target!!.x - x).toDouble())
        }
    }

    // [수정] 배속 반영
    override fun update(speedMultiplier: Int) {
        if (hasHit) return

        if (weaponType == WeaponType.MISSILE && target != null && !target!!.isDead) {
            updateAngle()
        }

        // 배속만큼 이동 거리 증가
        val currentSpeed = speed * speedMultiplier
        x += (cos(angle) * currentSpeed).toFloat()
        y += (sin(angle) * currentSpeed).toFloat()

        if (x < -500 || x > 3000 || y < -500 || y > 3000) {
            hasHit = true
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.style = Paint.Style.FILL

        when (weaponType) {
            WeaponType.MISSILE -> {
                canvas.drawCircle(x, y, 12f, paint)
                paint.alpha = 150
                canvas.drawCircle(x - (cos(angle)*12).toFloat(), y - (sin(angle)*12).toFloat(), 8f, paint)
                paint.alpha = 255
            }
            else -> canvas.drawCircle(x, y, 8f, paint)
        }
    }
}