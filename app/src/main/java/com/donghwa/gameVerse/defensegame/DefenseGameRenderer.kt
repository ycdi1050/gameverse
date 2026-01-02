package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

class DefenseGameRenderer(private val state: DefenseGameState) {
    private val paint = Paint()

    fun draw(canvas: Canvas, width: Int, height: Int) {
        // 배경
        paint.color = Color.parseColor("#263238")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        drawGrid(canvas, width, height)
        drawPath(canvas)

        for (char in state.characters) char.draw(canvas, paint)
        for (enemy in state.enemies) enemy.draw(canvas, paint)
        for (proj in state.projectiles) proj.draw(canvas, paint)

        drawDrops(canvas)
        drawUI(canvas, width, height)

        state.draggingCharacter?.let { it.draw(canvas, paint) }

        state.selectedTile?.let {
            val gs = state.gridSize
            val col = (it.x / gs).toInt()
            val row = (it.y / gs).toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = Color.CYAN
            canvas.drawRect(col * gs, row * gs, (col + 1) * gs, (row + 1) * gs, paint)
        }

        drawScreens(canvas, width, height)
    }

    private fun drawGrid(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.DKGRAY
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        val gs = state.gridSize

        for (i in 0..state.cols) canvas.drawLine(i * gs, 0f, i * gs, h.toFloat(), paint)
        for (j in 0..state.rows) canvas.drawLine(0f, j * gs, w.toFloat(), j * gs, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#40E57373")
        state.gridState?.let { grid ->
            for (i in 0 until state.cols) {
                for (j in 0 until state.rows) {
                    if (!grid[i][j]) canvas.drawRect(i * gs, j * gs, (i + 1) * gs, (j + 1) * gs, paint)
                }
            }
        }
    }

    private fun drawPath(canvas: Canvas) {
        if (state.path.isEmpty()) return
        paint.color = Color.parseColor("#546E7A")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 40f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val path = Path()
        path.moveTo(state.path[0].x, state.path[0].y)
        for (i in 1 until state.path.size) path.lineTo(state.path[i].x, state.path[i].y)
        canvas.drawPath(path, paint)
    }

    private fun drawDrops(canvas: Canvas) {
        for (drop in state.drops) {
            val size = 40f
            paint.style = Paint.Style.FILL
            paint.color = drop.weaponGrade.getColor()
            paint.alpha = 100
            canvas.drawCircle(drop.x, drop.y, size + 10, paint)

            paint.alpha = 255
            paint.color = Color.parseColor("#FFD700")
            canvas.drawRect(drop.x - size/2, drop.y - size/2, drop.x + size/2, drop.y + size/2, paint)

            paint.color = Color.RED
            canvas.drawRect(drop.x - 5, drop.y - size/2, drop.x + 5, drop.y + size/2, paint)
            canvas.drawRect(drop.x - size/2, drop.y - 5, drop.x + size/2, drop.y + 5, paint)

            paint.color = Color.BLACK
            paint.textSize = 25f
            paint.textAlign = Paint.Align.CENTER
            val initial = drop.weaponType.name.first().toString()
            canvas.drawText(initial, drop.x, drop.y - size/2 - 10, paint)

            val timeLeft = (drop.lifeTime - (System.currentTimeMillis() - drop.creationTime)) / 1000f
            if (timeLeft < 2.0f && !drop.isCollecting) {
                paint.color = Color.WHITE
                paint.textSize = 20f
                canvas.drawText(String.format("%.1f", timeLeft), drop.x, drop.y + size + 10, paint)
            }
        }
    }

    private fun drawUI(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL

        // [신규] 웨이브 정보 표시
        canvas.drawText("Stage: ${state.stage}", 30f, 60f, paint)
        canvas.drawText("Wave: ${state.currentWave} / ${state.maxWaves}", 30f, 110f, paint)
        canvas.drawText("Lives: ${state.lives}", 30f, 160f, paint)
        canvas.drawText("Score: ${state.score}", 30f, 210f, paint)
        canvas.drawText("Points: ${state.currentPoints}", 30f, 260f, paint)

        paint.textSize = 50f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(if(state.isPaused) "▶" else "⏸", w - 30f, 60f, paint)

        if (!state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear) {
            paint.color = if (state.currentPoints >= state.upgradeCost) Color.parseColor("#76FF03") else Color.GRAY
            paint.style = Paint.Style.FILL
            val btnW = 300f
            val btnH = 100f
            val btnX = w / 2f
            val btnY = h - 80f
            canvas.drawRect(btnX - btnW/2, btnY - btnH/2, btnX + btnW/2, btnY + btnH/2, paint)

            paint.color = Color.BLACK
            paint.textSize = 30f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Upgrade DMG", btnX, btnY - 10, paint)
            paint.textSize = 24f
            canvas.drawText("Cost: ${state.upgradeCost}", btnX, btnY + 30, paint)
        }
    }

    private fun drawScreens(canvas: Canvas, w: Int, h: Int) {
        if (state.isLevelSelection) {
            drawOverlay(canvas, w, h)
            paint.color = Color.WHITE
            paint.textSize = 60f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Select Stage", w/2f, 200f, paint)
            val startY = 400f
            for (i in 1..5) {
                val btnY = startY + (i - 1) * 180f
                paint.color = if(i <= state.maxUnlockedStage) Color.CYAN else Color.GRAY
                canvas.drawRect(w/2f - 200, btnY - 60, w/2f + 200, btnY + 60, paint)
                paint.color = Color.BLACK
                paint.textSize = 40f
                canvas.drawText("Stage $i", w/2f, btnY + 15, paint)
                if(i > state.maxUnlockedStage) canvas.drawText("🔒", w/2f + 150, btnY + 15, paint)
            }
            val lastBtnY = startY + 4 * 180f
            val homeY = lastBtnY + 250f
            paint.color = Color.RED
            canvas.drawRect(w/2f - 150, homeY - 50, w/2f + 150, homeY + 50, paint)
            paint.color = Color.WHITE
            canvas.drawText("Exit Game", w/2f, homeY + 15, paint)

        } else if (state.isWeaponSelection) {
            drawOverlay(canvas, w, h)
            paint.color = Color.WHITE
            paint.textSize = 60f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Select Weapon", w/2f, 200f, paint)
            val weapons = WeaponType.values()
            val startY = 400f
            for ((i, weapon) in weapons.withIndex()) {
                val btnY = startY + i * 150f
                paint.color = Color.YELLOW
                canvas.drawRect(w/2f - 250, btnY - 50, w/2f + 250, btnY + 50, paint)
                paint.color = Color.BLACK
                paint.textSize = 40f
                canvas.drawText(weapon.name, w/2f, btnY + 15, paint)
            }

        } else if (state.isOptionSelection) {
            // [신규] 옵션 선택 화면 그리기
            drawOptionSelectionScreen(canvas, w, h)

        } else if (state.isGameOver) {
            drawPopupBox(canvas, w, h, "GAME OVER", "Final Score: ${state.score}", "Tap to Return Home", null, Color.RED)

        } else if (state.isStageClear) {
            drawPopupBox(canvas, w, h, "STAGE CLEAR!", "Score: ${state.score}", "Tap to Continue", "Tap to Exit Game", Color.GREEN)

        } else if (state.isPaused) {
            drawPopupBox(canvas, w, h, "PAUSED", "Game Paused", "Tap to Resume", "Tap to Exit Game", Color.WHITE)
        }
    }

    // [신규] 옵션 선택 UI 그리기
    private fun drawOptionSelectionScreen(canvas: Canvas, w: Int, h: Int) {
        drawOverlay(canvas, w, h)

        paint.color = Color.CYAN
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("WAVE COMPLETE!", w/2f, 250f, paint)

        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.isFakeBoldText = false
        canvas.drawText("보너스 선택 (Select Bonus)", w/2f, 350f, paint)

        // 옵션 카드 2개 그리기
        val options = state.currentOptions
        val cardW = 400f
        val cardH = 300f
        val startY = 500f

        for (i in options.indices) {
            val option = options[i]
            val cardX = w/2f
            val cardY = startY + i * (cardH + 50f)

            val rect = RectF(cardX - cardW/2, cardY - cardH/2, cardX + cardW/2, cardY + cardH/2)

            paint.color = Color.parseColor("#455A64")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.YELLOW
            paint.textSize = 40f
            paint.isFakeBoldText = true
            canvas.drawText(option.title, cardX, cardY - 50, paint)

            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.isFakeBoldText = false

            // 설명 텍스트 줄바꿈 처리 (간단하게)
            val descLines = option.description.split("\n")
            var lineY = cardY + 20f
            for(line in descLines) {
                canvas.drawText(line, cardX, lineY, paint)
                lineY += 35f
            }
        }
    }

    private fun drawPopupBox(canvas: Canvas, w: Int, h: Int, title: String, msg1: String, btn1: String, btn2: String? = null, titleColor: Int) {
        drawOverlay(canvas, w, h)
        val boxW = 600f
        val boxH = 450f
        val centerX = w / 2f
        val centerY = h / 2f
        val rect = RectF(centerX - boxW/2, centerY - boxH/2, centerX + boxW/2, centerY + boxH/2)
        paint.color = Color.parseColor("#37474F")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 30f, 30f, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawRoundRect(rect, 30f, 30f, paint)
        paint.style = Paint.Style.FILL
        paint.color = titleColor
        paint.textSize = 70f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText(title, centerX, centerY - 100, paint)
        paint.color = Color.WHITE
        paint.textSize = 45f
        paint.isFakeBoldText = false
        canvas.drawText(msg1, centerX, centerY, paint)
        paint.color = Color.CYAN
        paint.textSize = 40f
        canvas.drawText(btn1, centerX, centerY + 100, paint)
        if (btn2 != null) {
            paint.color = Color.parseColor("#FF5252")
            canvas.drawText(btn2, centerX, centerY + 180, paint)
        }
    }

    private fun drawOverlay(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.BLACK
        paint.alpha = 180
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.alpha = 255
    }
}