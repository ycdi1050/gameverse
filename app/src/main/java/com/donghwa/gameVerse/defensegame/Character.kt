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

        when (characterType) {
            DefenseCharacterType.HUMAN -> {}
            DefenseCharacterType.ROBOT -> fireRate = (fireRate * 0.9).toLong()
            DefenseCharacterType.ALIEN -> range *= 1.1f
        }

        val gradeMultiplier = weaponGrade.getDamageMultiplier()
        damage = (damage * gradeMultiplier).toInt()
    }

    fun autoAttack(enemies: List<Enemy>, currentTime: Long, state: DefenseGameState): Projectile? {
        if (recoilOffset > 0) {
            recoilOffset -= recoilRecovery
            if (recoilOffset < 0) recoilOffset = 0f
        }

        val finalFireRate = (fireRate / state.buffAtkSpeed).toLong()

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
            // [신규] 도탄 횟수 적용
            proj.ricochetCount = state.buffRicochetCount
            return proj
        } else {
            null
        }
    }

    override fun update() {}

    override fun draw(canvas: Canvas, paint: Paint) {
        drawBody(canvas, paint)
        drawWeapon(canvas, paint)

        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Lv.$level", x, y + 60f, paint)

        if (weaponGrade != WeaponGrade.NORMAL) {
            paint.color = weaponGrade.getColor()
            paint.textSize = 20f
            canvas.drawText(weaponGrade.name.first().toString(), x, y - 45f, paint)
        }
    }

    private fun drawBody(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.color = when (characterType) {
            DefenseCharacterType.HUMAN -> Color.parseColor("#FFCC80")
            DefenseCharacterType.ROBOT -> Color.parseColor("#B0BEC5")
            DefenseCharacterType.ALIEN -> Color.parseColor("#A5D6A7")
        }
        canvas.drawCircle(x, y, 35f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = weaponGrade.getColor()
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
            paint.style = Paint.Style.FILL
            paint.color = Color.DKGRAY
            canvas.save()
            canvas.translate(x + recoilX, y + recoilY)
            canvas.rotate(degrees)

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