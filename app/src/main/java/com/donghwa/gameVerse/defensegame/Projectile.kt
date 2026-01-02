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
    var target: Enemy?, // [수정] 타겟 변경 가능하도록 var로 변경
    var damage: Int,    // [수정] 데미지 감소 적용을 위해 var로 변경
    val color: Int,
    val weaponType: WeaponType
) : GameObject {

    var hasHit = false
    var speed = 25f
    private var angle: Double = 0.0

    // [신규] 도탄 시스템 변수
    var ricochetCount = 0
    val hitTargets = HashSet<Enemy>() // 이미 맞춘 적은 다시 맞추지 않도록 기록

    init {
        updateAngle()
    }

    // 타겟이 변경되면 각도 재계산
    fun updateAngle() {
        if (target != null) {
            angle = atan2((target!!.y - y).toDouble(), (target!!.x - x).toDouble())
        }
    }

    override fun update() {
        if (hasHit) return

        if (weaponType == WeaponType.MISSILE && target != null && !target!!.isDead) {
            updateAngle()
        }

        x += (cos(angle) * speed).toFloat()
        y += (sin(angle) * speed).toFloat()

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