package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Projectile(
    var x: Float,
    var y: Float,
    val originX: Float, // 발사 원점 X
    val originY: Float, // 발사 원점 Y
    private val target: Enemy,
    val baseDamage: Int,
    val color: Int,
    val weaponType: WeaponType // 무기 타입
) : GameObject {

    private val speed = if (weaponType == WeaponType.SNIPER) 50f else 30f
    val radius = if (weaponType == WeaponType.MISSILE) 15f else 10f
    var hasHit = false

    override fun update() {
        if (hasHit) return

        if (target.isDead) {
            hasHit = true
            return
        }

        val dx = target.x - x
        val dy = target.y - y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist <= speed + target.radius) {
            hasHit = true
            return
        } else {
            val angle = atan2(dy.toDouble(), dx.toDouble())
            x += (cos(angle) * speed).toFloat()
            y += (sin(angle) * speed).toFloat()
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        if (hasHit) return
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(x, y, radius, paint)
    }
}