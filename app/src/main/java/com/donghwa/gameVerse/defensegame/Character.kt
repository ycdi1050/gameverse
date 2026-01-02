package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import java.util.Random
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Character(
    var x: Float,
    var y: Float,
    var weaponType: WeaponType,
    var characterType: DefenseCharacterType,
    var weaponGrade: WeaponGrade
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

    private val random = Random()

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
                fireRate = (350L - (bonus * 20L)).coerceAtLeast(100L)
            }
            WeaponType.SHOTGUN -> {
                damage = 15 + (bonus * 5)
                range = 250f
                fireRate = 1800L
            }
            WeaponType.SNIPER -> {
                damage = 40 + (bonus * 15)
                range = 800f
                fireRate = 3500L
            }
            WeaponType.MISSILE -> {
                damage = 10 + (bonus * 5)
                range = 600f
                fireRate = 2500L
            }
            WeaponType.BOW -> {
                damage = 8 + (bonus * 3)
                range = 400f
                fireRate = 1400L
            }
        }

        when (characterType) {
            DefenseCharacterType.POTATO -> {}
            DefenseCharacterType.ROBOT -> fireRate = (fireRate * 0.9).toLong()
            DefenseCharacterType.ALIEN -> range *= 1.1f
        }

        val gradeMultiplier = weaponGrade.getDamageMultiplier()
        damage = (damage * gradeMultiplier).toInt()
    }

    fun autoAttack(enemies: List<Enemy>, currentTime: Long, state: DefenseGameState, speedMultiplier: Int): Projectile? {
        if (recoilOffset > 0) {
            recoilOffset -= (recoilRecovery * speedMultiplier)
            if (recoilOffset < 0) recoilOffset = 0f
        }

        val finalFireRate = (fireRate / state.buffAtkSpeed / speedMultiplier).toLong()

        if (currentTime - lastShotTime < finalFireRate) return null

        val finalRange = range * state.buffRange

        val target = enemies.filter { enemy ->
            val d = dist(x, y, enemy.x, enemy.y)
            !enemy.isDead && !enemy.reachedEnd && d <= finalRange &&
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

            var finalDamage = (damage * state.globalDamageMultiplier * state.buffAtkDamage).toInt()

            if (state.isDoubleShot) {
                finalDamage = (finalDamage * 0.8f).toInt()
            }

            if (state.buffCritChance > 0 && random.nextFloat() < state.buffCritChance) {
                finalDamage = (finalDamage * state.buffCritDamage).toInt()
            }

            val proj = Projectile(x, y, x, y, target, finalDamage, color, weaponType)
            proj.ricochetCount = state.buffRicochetCount
            return proj
        } else {
            null
        }
    }

    override fun update(speedMultiplier: Int) {}

    override fun draw(canvas: Canvas, paint: Paint) {
        // [수정] Paint 객체 초기화 (이전 그리기 속성 제거)
        // 비트맵 그릴 때 필터링(부드럽게) 적용
        paint.isFilterBitmap = true
        paint.isAntiAlias = true

        if (!drawCharacterWithWeapon(canvas, paint)) {
            drawBody(canvas, paint)
            drawWeapon(canvas, paint)
        }

        // [수정] 텍스트 그리기 전 Paint 속성 확실하게 재설정
        paint.reset()
        paint.isAntiAlias = true
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        // 그림자 효과 제거 (혹시 설정되어 있었다면)
        paint.clearShadowLayer()
        paint.style = Paint.Style.FILL

        canvas.drawText("Lv.$level", x, y + 95f, paint)

        if (weaponGrade != WeaponGrade.NORMAL) {
            paint.color = weaponGrade.getColor()
            paint.textSize = 24f
            // 그림자 제거 확인
            paint.clearShadowLayer()
            canvas.drawText(weaponGrade.name.first().toString(), x, y - 85f, paint)
        }
    }

    private fun drawCharacterWithWeapon(canvas: Canvas, paint: Paint): Boolean {
        val combinedBitmap = ResourceManager.getCombinedBitmap(characterType, weaponType)

        if (combinedBitmap != null) {
            val matrix = Matrix()
            matrix.postTranslate(-combinedBitmap.width / 2f, -combinedBitmap.height / 2f)

            val targetSize = 160f
            val scaleX = targetSize / combinedBitmap.width
            val scaleY = targetSize / combinedBitmap.height
            matrix.postScale(scaleX, scaleY)

            val degrees = Math.toDegrees(angle).toFloat()

            matrix.reset()
            matrix.preTranslate(-combinedBitmap.width / 2f, -combinedBitmap.height / 2f)
            matrix.postScale(scaleX, scaleY)
            matrix.postRotate(degrees)

            val recoilX = -(cos(angle).toFloat() * recoilOffset)
            val recoilY = -(sin(angle).toFloat() * recoilOffset)

            matrix.postTranslate(x + recoilX, y + recoilY)

            canvas.drawBitmap(combinedBitmap, matrix, paint)

            return true
        }
        return false
    }

    private fun drawBody(canvas: Canvas, paint: Paint) {
        val bitmap = ResourceManager.getUnitBitmap(characterType)

        if (bitmap != null) {
            val matrix = Matrix()

            val targetSize = 160f
            val scaleX = targetSize / bitmap.width
            val scaleY = targetSize / bitmap.height

            matrix.reset()
            matrix.preTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
            matrix.postScale(scaleX, scaleY)
            matrix.postTranslate(x, y)

            canvas.drawBitmap(bitmap, matrix, paint)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = when (characterType) {
                DefenseCharacterType.POTATO -> Color.parseColor("#FFCC80")
                DefenseCharacterType.ROBOT -> Color.parseColor("#B0BEC5")
                DefenseCharacterType.ALIEN -> Color.parseColor("#A5D6A7")
            }
            canvas.drawCircle(x, y, 70f, paint)
        }
    }

    private fun drawWeapon(canvas: Canvas, paint: Paint) {
        val bitmap = ResourceManager.getWeaponBitmap(weaponType, weaponGrade)

        val degrees = Math.toDegrees(angle).toFloat()
        val recoilX = -(cos(angle).toFloat() * recoilOffset)
        val recoilY = -(sin(angle).toFloat() * recoilOffset)

        if (bitmap != null) {
            val matrix = Matrix()

            var scaleFactor = 1.3f
            var forwardOffset = 50f
            var sideOffset = 20f

            when (weaponType) {
                WeaponType.SMG -> {
                    scaleFactor = 1.0f
                    forwardOffset = 55f
                }
                WeaponType.SHOTGUN -> {
                    scaleFactor = 1.4f
                    forwardOffset = 65f
                }
                WeaponType.SNIPER -> {
                    scaleFactor = 1.6f
                    forwardOffset = 80f
                }
                WeaponType.MISSILE -> {
                    scaleFactor = 1.4f
                    forwardOffset = 45f
                    sideOffset = 30f
                }
                WeaponType.BOW -> {
                    scaleFactor = 1.4f
                    forwardOffset = 60f
                }
            }

            matrix.preTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
            val baseScale = 100f / bitmap.width
            val finalScale = baseScale * scaleFactor
            matrix.postScale(finalScale, finalScale)
            matrix.postTranslate(forwardOffset, sideOffset)
            matrix.postRotate(degrees)
            matrix.postTranslate(x + recoilX, y + recoilY)

            canvas.drawBitmap(bitmap, matrix, paint)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.DKGRAY
            canvas.save()
            canvas.translate(x + recoilX, y + recoilY)
            canvas.rotate(degrees)

            val wLen = 90f
            val wWidth = if(weaponType == WeaponType.MISSILE) 30f else 15f
            canvas.drawRect(0f, -wWidth/2, wLen, wWidth/2, paint)

            canvas.restore()
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()).toFloat()
    }
}