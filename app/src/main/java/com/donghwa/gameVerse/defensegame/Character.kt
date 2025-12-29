package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// WeaponType enum 정의 유지
enum class WeaponType {
    SMG, SHOTGUN, SNIPER, MISSILE, BOW
}

class Character(var x: Float, var y: Float, var weaponType: WeaponType) : GameObject {
    private var angle = -Math.PI / 2 // 라디안 각도
    var damage = 10
    var range = 300f
    private var fireRate = 1000L
    private var lastShotTime = 0L
    var level = 1

    private val minMissileRange = 200f

    // 공격 애니메이션 관련
    private var recoilOffset = 0f // 반동 거리
    private val maxRecoil = 15f   // 최대 반동
    private val recoilRecovery = 2f // 복구 속도

    init {
        updateStats()
    }

    fun setPosition(newX: Float, newY: Float) {
        x = newX
        y = newY
    }

    fun upgrade() {
        level++
        updateStats()
    }

    private fun updateStats() {
        val bonus = level - 1
        when (weaponType) {
            WeaponType.SMG -> {
                damage = 3 + (bonus * 2)
                range = 350f
                fireRate = (150L - (bonus * 10L)).coerceAtLeast(50L)
            }
            WeaponType.SHOTGUN -> {
                damage = 15 + (bonus * 5)
                range = 250f
                fireRate = 1000L
            }
            WeaponType.SNIPER -> {
                damage = 40 + (bonus * 15)
                range = 800f
                fireRate = 2000L
            }
            WeaponType.MISSILE -> {
                damage = 10 + (bonus * 5)
                range = 600f
                fireRate = 1500L
            }
            WeaponType.BOW -> {
                damage = 8 + (bonus * 3)
                range = 400f
                fireRate = 800L
            }
        }
    }

    fun autoAttack(enemies: List<Enemy>, currentTime: Long, globalDamageMultiplier: Float = 1.0f): Projectile? {
        // 반동 회복
        if (recoilOffset > 0) {
            recoilOffset -= recoilRecovery
            if (recoilOffset < 0) recoilOffset = 0f
        }

        if (currentTime - lastShotTime < fireRate) return null

        val target = enemies.filter { enemy ->
            val d = dist(x, y, enemy.x, enemy.y)
            !enemy.isDead && !enemy.reachedEnd && d <= range &&
                    (weaponType != WeaponType.MISSILE || d >= minMissileRange)
        }.sortedByDescending { it.currentPathIndex }.firstOrNull()

        return if (target != null) {
            angle = atan2((target.y - y).toDouble(), (target.x - x).toDouble())
            lastShotTime = currentTime

            // 발사 시 반동 적용
            recoilOffset = maxRecoil

            val color = when (weaponType) {
                WeaponType.SMG -> Color.YELLOW
                WeaponType.SHOTGUN -> Color.RED
                WeaponType.SNIPER -> Color.GREEN
                WeaponType.MISSILE -> Color.MAGENTA
                WeaponType.BOW -> Color.WHITE
            }

            val finalDamage = (damage * globalDamageMultiplier).toInt()

            // 총구 위치 보정 (반동 적용된 위치에서 발사되는 것처럼 보이게 할 수도 있음)
            Projectile(x, y, x, y, target, finalDamage, color, weaponType)
        } else {
            null
        }
    }

    override fun update() {}

    override fun draw(canvas: Canvas, paint: Paint) {
        val bitmap = ResourceManager.getCharacterBitmap(weaponType)

        if (bitmap != null) {
            // 이미지 회전 및 그리기
            val matrix = Matrix()
            // 비트맵 중심을 기준으로 회전 (angle은 라디안이므로 도(degree)로 변환)
            // 기본 이미지가 오른쪽(0도)을 보고 있다고 가정
            val degrees = Math.toDegrees(angle).toFloat()

            // 반동 효과: 현재 각도의 반대 방향으로 살짝 이동
            val recoilX = -(cos(angle).toFloat() * recoilOffset)
            val recoilY = -(sin(angle).toFloat() * recoilOffset)

            matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f) // 중심점 이동
            matrix.postRotate(degrees) // 회전
            matrix.postTranslate(x + recoilX, y + recoilY) // 원래 위치 + 반동 적용

            canvas.drawBitmap(bitmap, matrix, paint)
        } else {
            // 비트맵 로드 실패 시 기존 도형 그리기 (Fallback)
            drawFallbackShape(canvas, paint)
        }

        // 레벨 텍스트
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Lv.$level", x, y + 60f, paint)
    }

    private fun drawFallbackShape(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.color = Color.GRAY
        canvas.drawCircle(x, y, 30f, paint)
        canvas.drawLine(x, y, x + cos(angle).toFloat() * 50, y + sin(angle).toFloat() * 50, paint)
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()).toFloat()
    }
}