package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class DefenseGameRenderer(private val state: DefenseGameState) {
    private val paint = Paint()

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (state.isLevelSelection) {
            drawLevelSelection(canvas, width, height)
            return
        }

        // [신규] 무기 선택 화면
        if (state.isWeaponSelection) {
            drawWeaponSelection(canvas, width, height)
            return
        }

        drawMap(canvas, width, height)
        drawGrid(canvas)

        if (state.isGameOver) {
            drawGameOver(canvas, width, height)
            return
        }

        if (state.isStageClear) {
            drawStageClear(canvas, width, height)
            return
        }

        drawObjects(canvas)
        drawUI(canvas, width, height)

        if (state.isPaused) {
            drawPausedPopup(canvas, width, height)
        }
    }

    private fun drawMap(canvas: Canvas, width: Int, height: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 40f
        paint.color = Color.DKGRAY
        if (state.path.isNotEmpty()) {
            for (i in 0 until state.path.size - 1) {
                canvas.drawLine(state.path[i].x, state.path[i].y, state.path[i+1].x, state.path[i+1].y, paint)
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#333333")

        for (i in 0 until state.cols) {
            for (j in 0 until state.rows) {
                val left = i * state.gridSize
                val top = j * state.gridSize
                if (!state.gridState[i][j]) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#22111111")
                    canvas.drawRect(left, top, left + state.gridSize, top + state.gridSize, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = Color.parseColor("#333333")
                } else {
                    canvas.drawRect(left, top, left + state.gridSize, top + state.gridSize, paint)
                }
            }
        }
    }

    private fun drawObjects(canvas: Canvas) {
        // [수정] turrets -> characters
        state.characters.forEach { it.draw(canvas, paint) }
        state.enemies.forEach { it.draw(canvas, paint) }
        state.projectiles.forEach { it.draw(canvas, paint) }
    }

    private fun drawUI(canvas: Canvas, width: Int, height: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Score: ${state.score}", 50f, 80f, paint)
        canvas.drawText("Lives: ${state.lives}", 50f, 130f, paint)
        canvas.drawText("Stage: ${state.stage}", 50f, 180f, paint)
        canvas.drawText("Kills: ${state.killsInCurrentStage} / ${state.requiredKills}", 50f, 230f, paint)

        paint.color = Color.YELLOW
        canvas.drawText("Points: ${state.currentPoints}", 50f, 280f, paint)

        paint.textSize = 60f
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("⚙️", width - 50f, 80f, paint)

        val btnW = 300f
        val btnH = 100f
        val btnX = width / 2f
        val btnY = height - 80f
        paint.style = Paint.Style.FILL
        paint.color = if (state.currentPoints >= state.upgradeCost) Color.parseColor("#FF6D00") else Color.DKGRAY
        canvas.drawRect(btnX - btnW/2, btnY - btnH/2, btnX + btnW/2, btnY + btnH/2, paint)
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("공격력 강화 (${state.upgradeCost}P)", btnX, btnY - 10, paint)
        canvas.drawText("배율: x${String.format("%.1f", state.globalDamageMultiplier)}", btnX, btnY + 30, paint)
    }

    // [신규] 무기 선택 화면
    private fun drawWeaponSelection(canvas: Canvas, width: Int, height: Int) {
        canvas.drawColor(Color.BLACK)
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SELECT STARTING WEAPON", width / 2f, 200f, paint)

        val weapons = WeaponType.values()
        val startY = 400f

        for ((i, weapon) in weapons.withIndex()) {
            val y = startY + i * 150f
            val btnRect = android.graphics.RectF(width / 2f - 250, y - 50, width / 2f + 250, y + 50)

            paint.style = Paint.Style.FILL
            paint.color = if (state.selectedWeapon == weapon) Color.GREEN else Color.DKGRAY
            canvas.drawRect(btnRect, paint)

            paint.color = Color.WHITE
            paint.textSize = 40f
            canvas.drawText(weapon.name, width / 2f, y + 15, paint)
        }

        // 시작 버튼
        val startBtnY = height - 200f
        paint.color = Color.CYAN
        canvas.drawRect(width / 2f - 200, startBtnY - 50, width / 2f + 200, startBtnY + 50, paint)
        paint.color = Color.BLACK
        canvas.drawText("START GAME", width / 2f, startBtnY + 15, paint)
    }

    private fun drawLevelSelection(canvas: Canvas, width: Int, height: Int) {
        canvas.drawColor(Color.BLACK)
        paint.color = Color.CYAN
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SELECT LEVEL", width / 2f, 200f, paint)

        val startY = 400f
        for (i in 1..5) {
            val y = startY + (i - 1) * 180f
            val isLocked = i > state.maxUnlockedStage
            paint.color = if (isLocked) Color.DKGRAY else Color.BLUE
            paint.style = Paint.Style.FILL
            canvas.drawRect(width / 2f - 200, y - 60, width / 2f + 200, y + 60, paint)
            paint.color = if (isLocked) Color.GRAY else Color.WHITE
            paint.textSize = 50f
            val text = if (isLocked) "LOCKED" else "STAGE $i"
            canvas.drawText(text, width / 2f, y + 20, paint)
        }
        val homeY = startY + 4 * 180f + 250f
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 150, homeY - 50, width / 2f + 150, homeY + 50, paint)
        paint.color = Color.WHITE
        paint.textSize = 40f
        canvas.drawText("HOME", width / 2f, homeY + 15, paint)
    }

    private fun drawGameOver(canvas: Canvas, width: Int, height: Int) {
        // 기존 코드 유지
        canvas.drawColor(Color.argb(200, 0, 0, 0))
        paint.color = Color.RED
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("GAME OVER", width / 2f, height / 2f - 200, paint)
        paint.color = Color.WHITE
        paint.textSize = 50f
        canvas.drawText("Score: ${state.score}", width / 2f, height / 2f - 100, paint)
        val btnY1 = height / 2f + 50
        paint.color = Color.BLUE
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 250, btnY1 - 60, width / 2f + 250, btnY1 + 60, paint)
        paint.color = Color.WHITE
        paint.textSize = 40f
        canvas.drawText("Select Level", width / 2f, btnY1 + 15, paint)
        val btnY2 = height / 2f + 200
        paint.color = Color.DKGRAY
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 250, btnY2 - 60, width / 2f + 250, btnY2 + 60, paint)
        paint.color = Color.WHITE
        canvas.drawText("Home", width / 2f, btnY2 + 15, paint)
    }

    private fun drawStageClear(canvas: Canvas, width: Int, height: Int) {
        // 기존 코드 유지
        canvas.drawColor(Color.argb(200, 0, 0, 0))
        paint.color = Color.YELLOW
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("STAGE ${state.stage} CLEARED!", width / 2f, height / 2f - 300, paint)
        paint.color = Color.WHITE
        paint.textSize = 50f
        canvas.drawText("Final Score: ${state.score}", width / 2f, height / 2f - 200, paint)
        val btnY1 = height / 2f + 50
        paint.color = Color.GREEN
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 250, btnY1 - 60, width / 2f + 250, btnY1 + 60, paint)
        paint.color = Color.BLACK
        paint.textSize = 40f
        canvas.drawText("Next Stage", width / 2f, btnY1 + 15, paint)
        val btnY2 = height / 2f + 200
        paint.color = Color.DKGRAY
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 250, btnY2 - 60, width / 2f + 250, btnY2 + 60, paint)
        paint.color = Color.WHITE
        canvas.drawText("Home", width / 2f, btnY2 + 15, paint)
    }

    private fun drawPausedPopup(canvas: Canvas, width: Int, height: Int) {
        // 기존 코드 유지
        canvas.drawColor(Color.argb(150, 0, 0, 0))
        paint.color = Color.WHITE
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("PAUSED", width / 2f, height / 2f - 250, paint)
        val btnY1 = height / 2f - 50
        paint.color = Color.GREEN
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 200, btnY1 - 60, width / 2f + 200, btnY1 + 60, paint)
        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Resume", width / 2f, btnY1 + 20, paint)
        val btnY2 = height / 2f + 100
        paint.color = Color.BLUE
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 200, btnY2 - 60, width / 2f + 200, btnY2 + 60, paint)
        paint.color = Color.WHITE
        canvas.drawText("Restart Level", width / 2f, btnY2 + 20, paint)
        val btnY3 = height / 2f + 250
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        canvas.drawRect(width / 2f - 200, btnY3 - 60, width / 2f + 200, btnY3 + 60, paint)
        paint.color = Color.WHITE
        canvas.drawText("Quit to Home", width / 2f, btnY3 + 20, paint)
    }
}