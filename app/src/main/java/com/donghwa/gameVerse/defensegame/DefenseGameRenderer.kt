package com.donghwa.gameVerse.defensegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF

class DefenseGameRenderer(private val state: DefenseGameState) {
    private val paint = Paint()

    fun draw(canvas: Canvas, width: Int, height: Int) {
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
            // [수정] 무기 비트맵(PNG) 가져오기 시도
            val bitmap = ResourceManager.getWeaponBitmap(drop.weaponType, drop.weaponGrade)

            if (bitmap != null) {
                // 이미지가 있을 경우: 등급 색상의 빛나는 효과 + 아이콘
                val size = 80f // 아이콘 크기

                // 배경 글로우 (등급 색상)
                paint.style = Paint.Style.FILL
                paint.color = drop.weaponGrade.getColor()
                paint.alpha = 80 // 반투명
                canvas.drawCircle(drop.x, drop.y, size / 2 + 15, paint)
                paint.alpha = 255

                // 테두리
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = drop.weaponGrade.getColor()
                canvas.drawCircle(drop.x, drop.y, size / 2 + 15, paint)
                paint.style = Paint.Style.FILL

                // 아이콘 그리기
                val destRect = RectF(
                    drop.x - size / 2,
                    drop.y - size / 2,
                    drop.x + size / 2,
                    drop.y + size / 2
                )
                // 비트맵 필터링으로 부드럽게
                paint.isFilterBitmap = true
                canvas.drawBitmap(bitmap, null, destRect, paint)
                paint.isFilterBitmap = false

            } else {
                // 이미지가 없을 경우: 기존 도형 방식 사용 (Fallback)
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
            }

            // 아이템이 사라지지 않으므로 남은 시간 표시 로직 제거됨
        }
    }

    private fun drawUI(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL
        paint.isFakeBoldText = true

        canvas.drawText("Stage: ${state.stage}", 30f, 60f, paint)
        canvas.drawText("Wave: ${state.currentWave} / ${state.maxWaves}", 30f, 110f, paint)
        canvas.drawText("Lives: ${state.lives}", 30f, 160f, paint)
        canvas.drawText("Score: ${state.score}", 30f, 210f, paint)
        canvas.drawText("Points: ${state.currentPoints}", 30f, 260f, paint)

        // --- 상단 버튼 UI (이미지 사용) ---
        // 1. 일시정지 버튼 (우측 상단 끝)
        // 버튼 영역: (w - 80) ~ w, 높이 0 ~ 80 정도 가정 (터치 영역 고려)
        val pauseBtnSize = 80
        val pauseBtnX = w - pauseBtnSize - 20
        val pauseBtnY = 20

        // 현재 상태에 따라 이미지 키 선택 (일시정지 상태면 '재생' 버튼 표시, 실행 중이면 '일시정지' 버튼 표시)
        val pauseKey = if(state.isPaused) "ui_play" else "ui_pause"
        val pauseBitmap = ResourceManager.getUIBitmap(pauseKey)

        if (pauseBitmap != null) {
            val destRect = Rect(pauseBtnX, pauseBtnY, pauseBtnX + pauseBtnSize, pauseBtnY + pauseBtnSize)
            canvas.drawBitmap(pauseBitmap, null, destRect, paint)
        } else {
            // 이미지 없을 시 텍스트 폴백
            paint.textSize = 50f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(if(state.isPaused) "▶" else "⏸", w - 30f, 60f, paint)
        }

        // 2. 배속 버튼 (일시정지 버튼 왼쪽)
        val speedBtnSize = 80
        val speedBtnX = pauseBtnX - speedBtnSize - 20 // 간격 20
        val speedBtnY = 20

        val speedKey = "ui_speed_${state.gameSpeed}"
        val speedBitmap = ResourceManager.getUIBitmap(speedKey)

        if (speedBitmap != null) {
            val destRect = Rect(speedBtnX, speedBtnY, speedBtnX + speedBtnSize, speedBtnY + speedBtnSize)
            canvas.drawBitmap(speedBitmap, null, destRect, paint)
        } else {
            // 이미지 없을 시 텍스트 폴백
            paint.textSize = 50f
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.CYAN
            canvas.drawText("x${state.gameSpeed}", (w - 150).toFloat(), 60f, paint)
        }

        // 하단 업그레이드 버튼 등은 유지
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
            paint.isFakeBoldText = false
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
            drawOptionSelectionScreen(canvas, w, h)

        } else if (state.isGameOver) {
            drawPopupBox(canvas, w, h, "GAME OVER", "Final Score: ${state.score}", "Tap to Return Home", null, Color.RED)

        } else if (state.isStageClear) {
            drawPopupBox(canvas, w, h, "STAGE CLEAR!", "Score: ${state.score}", "Tap to Continue", "Tap to Exit Game", Color.GREEN)

        } else if (state.isPaused) {
            drawPausedScreen(canvas, w, h)
        }
    }

    private fun drawPausedScreen(canvas: Canvas, w: Int, h: Int) {
        drawOverlay(canvas, w, h)

        val boxW = 700f
        val boxH = 900f
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

        // 타이틀
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("PAUSED", centerX, centerY - boxH/2 + 80, paint)

        // 옵션 목록 타이틀
        paint.textSize = 30f
        paint.color = Color.CYAN
        canvas.drawText("- 획득한 옵션 (Collected Buffs) -", centerX, centerY - boxH/2 + 140, paint)

        // 옵션 목록 그리기
        paint.textSize = 24f
        paint.color = Color.LTGRAY
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false

        val startX = centerX - boxW/2 + 50f
        var startY = centerY - boxH/2 + 190f

        if (state.collectedOptions.isEmpty()) {
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("(없음)", centerX, startY + 50, paint)
        } else {
            for ((index, option) in state.collectedOptions.withIndex()) {
                val color = option.grade.color
                paint.color = color
                val text = "${index + 1}. ${option.title}"
                canvas.drawText(text, startX, startY, paint)
                startY += 40f
                if (startY > centerY + boxH/2 - 150) break
            }
        }

        // 하단 버튼
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 40f
        paint.isFakeBoldText = true
        paint.color = Color.CYAN
        canvas.drawText("Tap to Resume", centerX, centerY + boxH/2 - 120, paint)

        paint.color = Color.parseColor("#FF5252")
        canvas.drawText("Tap to Exit Game", centerX, centerY + boxH/2 - 50, paint)
    }

    private fun drawOptionSelectionScreen(canvas: Canvas, w: Int, h: Int) {
        drawOverlay(canvas, w, h)

        paint.color = Color.CYAN
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("WAVE COMPLETE!", w/2f, 200f, paint)

        paint.color = Color.WHITE
        paint.textSize = 35f
        paint.isFakeBoldText = false
        canvas.drawText("보너스 선택 (Select Bonus)", w/2f, 280f, paint)

        val options = state.currentOptions
        val cardW = 400f
        val cardH = 250f
        val startY = 450f
        val gap = 50f

        for (i in options.indices) {
            val option = options[i]
            val cardX = w/2f
            val cardY = startY + i * (cardH + gap)

            val rect = RectF(cardX - cardW/2, cardY - cardH/2, cardX + cardW/2, cardY + cardH/2)

            paint.color = Color.parseColor("#455A64")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            paint.color = option.grade.color
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = option.grade.color
            paint.textSize = 36f
            paint.isFakeBoldText = true
            canvas.drawText(option.title, cardX, cardY - 60, paint)

            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.isFakeBoldText = false

            val descLines = option.description.split("\n")
            var lineY = cardY
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