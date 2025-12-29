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

enum class WeaponType {
    SMG, SHOTGUN, SNIPER, MISSILE, BOW
}

// [수정] characterType 필드 추가
class Character(
    var x: Float,
    var y: Float,
    var weaponType: WeaponType,
    var characterType: DefenseCharacterType
) : GameObject {
    private var angle = -Math.PI / 2
    var damage = 10
    var range = 300f
    private var fireRate = 1000L
    private var lastShotTime = 0L
    var level = 1

    private val minMissileRange = 200f
    private var recoilOffset = 0f
    private val maxRecoil = 15f
    private val recoilRecovery = 2f

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

        // 1. 무기 기본 스탯
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

        // 2. 캐릭터 타입 보너스 적용
        when (characterType) {
            DefenseCharacterType.HUMAN -> {
                // 밸런스형: 특별한 보너스 없음 (또는 비용 할인 로직 등 외부에서 처리)
            }
            DefenseCharacterType.ROBOT -> {
                // 공격 속도 10% 증가 (딜레이 감소)
                fireRate = (fireRate * 0.9).toLong()
            }
            DefenseCharacterType.ALIEN -> {
                // 사거리 10% 증가
                range *= 1.1f
            }
        }
    }

    fun autoAttack(enemies: List<Enemy>, currentTime: Long, globalDamageMultiplier: Float = 1.0f): Projectile? {
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
            recoilOffset = maxRecoil

            val color = when (weaponType) {
                WeaponType.SMG -> Color.YELLOW
                WeaponType.SHOTGUN -> Color.RED
                WeaponType.SNIPER -> Color.GREEN
                WeaponType.MISSILE -> Color.MAGENTA
                WeaponType.BOW -> Color.WHITE
            }

            val finalDamage = (damage * globalDamageMultiplier).toInt()
            Projectile(x, y, x, y, target, finalDamage, color, weaponType)
        } else {
            null
        }
    }

    override fun update() {}

    override fun draw(canvas: Canvas, paint: Paint) {
        // [수정] 캐릭터 타입에 따라 몸체 그리기
        drawBody(canvas, paint)

        // [수정] 무기 그리기 (기존 로직 + 이미지 없으면 도형)
        drawWeapon(canvas, paint)

        // 레벨 텍스트
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Lv.$level", x, y + 60f, paint)
    }

    private fun drawBody(canvas: Canvas, paint: Paint) {
        // 이미지가 있다면 ResourceManager에서 가져오겠지만, 여기선 도형으로 구분
        paint.style = Paint.Style.FILL
        paint.color = when (characterType) {
            DefenseCharacterType.HUMAN -> Color.parseColor("#FFCC80") // 살구색
            DefenseCharacterType.ROBOT -> Color.parseColor("#B0BEC5") // 회색
            DefenseCharacterType.ALIEN -> Color.parseColor("#A5D6A7") // 연두색
        }
        canvas.drawCircle(x, y, 35f, paint) // 몸통

        // 테두리
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.BLACK
        canvas.drawCircle(x, y, 35f, paint)
    }

    private fun drawWeapon(canvas: Canvas, paint: Paint) {
        val bitmap = ResourceManager.getCharacterBitmap(weaponType)
        val degrees = Math.toDegrees(angle).toFloat()
        val recoilX = -(cos(angle).toFloat() * recoilOffset)
        val recoilY = -(sin(angle).toFloat() * recoilOffset)

        if (bitmap != null) {
            val matrix = Matrix()
            matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
            matrix.postRotate(degrees)
            matrix.postTranslate(x + recoilX, y + recoilY)
            canvas.drawBitmap(bitmap, matrix, paint)
        } else {
            // 무기 이미지가 없을 때 Fallback 도형
            paint.style = Paint.Style.FILL
            paint.color = Color.DKGRAY
            // 회전된 무기(막대기) 그리기
            canvas.save()
            canvas.translate(x + recoilX, y + recoilY)
            canvas.rotate(degrees)

            // 무기 모양
            val wLen = 50f
            val wWidth = if(weaponType == WeaponType.MISSILE) 15f else 8f
            canvas.drawRect(0f, -wWidth/2, wLen, wWidth/2, paint)

            canvas.restore()
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()).toFloat()
    }
}