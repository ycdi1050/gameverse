package com.donghwa.gameVerse.brickgame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.max
import kotlin.math.min

class UIManager(
    private val screenX: Int,
    private val screenY: Int,
    private val onResume: () -> Unit,
    private val onRestart: () -> Unit,
    private val onExit: () -> Unit,
    var onSensitivityChanged: (Float) -> Unit
) {
    var isSettingsOpen = false
    private val gearRect = RectF()
    private val sliderRect = RectF()

    // 설정 메뉴 버튼
    private val resumeBtnRect = RectF()
    private val restartBtnRect = RectF()
    private val exitBtnRect = RectF()

    // [신규] 게임 오버 화면 버튼
    private val goRestartBtnRect = RectF()
    private val goExitBtnRect = RectF()

    private var knobX = 0f
    private val knobRadius = 30f
    private var isDraggingSlider = false

    init {
        val gearSize = 80f
        val margin = 50f
        gearRect.set(screenX - gearSize - margin, margin, screenX - margin, margin + gearSize)

        val centerX = screenX / 2f
        val centerY = screenY / 2f

        // 슬라이더
        val sliderWidth = screenX * 0.7f
        val sliderHeight = 10f
        val sliderLeft = (screenX - sliderWidth) / 2
        val sliderTop = centerY - 150f
        val sliderBottom = sliderTop + sliderHeight
        sliderRect.set(sliderLeft, sliderTop, sliderLeft + sliderWidth, sliderBottom)

        // 설정 메뉴 버튼들
        val btnWidth = 400f
        val btnHeight = 90f
        val btnGap = 40f
        val btnLeft = centerX - (btnWidth / 2)

        val resumeTop = sliderBottom + 100f
        resumeBtnRect.set(btnLeft, resumeTop, btnLeft + btnWidth, resumeTop + btnHeight)

        val restartTop = resumeBtnRect.bottom + btnGap
        restartBtnRect.set(btnLeft, restartTop, btnLeft + btnWidth, restartTop + btnHeight)

        val exitTop = restartBtnRect.bottom + btnGap
        exitBtnRect.set(btnLeft, exitTop, btnLeft + btnWidth, exitTop + btnHeight)

        // [신규] 게임 오버 버튼들 배치 (화면 중앙 하단)
        val goRestartTop = centerY + 50f
        goRestartBtnRect.set(btnLeft, goRestartTop, btnLeft + btnWidth, goRestartTop + btnHeight)

        val goExitTop = goRestartBtnRect.bottom + btnGap
        goExitBtnRect.set(btnLeft, goExitTop, btnLeft + btnWidth, goExitTop + btnHeight)
    }

    fun setKnobPosition(sensitivity: Float) {
        val sensitivityRange = 3.0f
        val normalized = (sensitivity - 0.5f) / sensitivityRange
        knobX = sliderRect.left + (normalized * sliderRect.width())
    }

    // [수정] 게임 오버 시 버튼 클릭 처리 추가
    fun handleTouch(event: MotionEvent, isGameOver: Boolean): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isSettingsOpen) {
                    val touchPadding = 30f
                    if (x >= sliderRect.left - touchPadding && x <= sliderRect.right + touchPadding &&
                        y >= sliderRect.top - touchPadding && y <= sliderRect.bottom + touchPadding) {
                        isDraggingSlider = true
                        updateKnobAndSensitivity(x)
                    }
                    else if (resumeBtnRect.contains(x, y)) {
                        isSettingsOpen = false
                        onResume()
                    }
                    else if (restartBtnRect.contains(x, y)) {
                        isSettingsOpen = false
                        onRestart()
                    }
                    else if (exitBtnRect.contains(x, y)) {
                        isSettingsOpen = false
                        onExit()
                    }
                    else {
                        isSettingsOpen = false
                        onResume()
                    }
                    return true
                } else if (isGameOver) {
                    // [신규] 게임 오버 버튼 처리
                    if (goRestartBtnRect.contains(x, y)) {
                        onRestart()
                    } else if (goExitBtnRect.contains(x, y)) {
                        onExit()
                    }
                    return true
                } else {
                    if (gearRect.contains(x, y)) {
                        isSettingsOpen = true
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isSettingsOpen && isDraggingSlider) {
                    updateKnobAndSensitivity(x)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                isDraggingSlider = false
            }
        }
        return isSettingsOpen || isGameOver
    }

    private fun updateKnobAndSensitivity(touchX: Float) {
        knobX = max(sliderRect.left, min(touchX, sliderRect.right))
        val percentage = (knobX - sliderRect.left) / sliderRect.width()
        val newSensitivity = 0.5f + (percentage * 3.0f)
        onSensitivityChanged(newSensitivity)
    }

    fun draw(canvas: Canvas, paint: Paint, score: Int, level: Int, lives: Int, isGameOver: Boolean) {
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Score: $score", 50f, 100f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Lv: $level", (screenX / 2).toFloat(), 100f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Lives: $lives", (screenX - 160).toFloat(), 100f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 60f
        paint.color = Color.LTGRAY
        canvas.drawText("⚙️", gearRect.centerX(), gearRect.centerY() + 20f, paint)

        if (isSettingsOpen) {
            drawSettingsPopup(canvas, paint)
        } else if (isGameOver) {
            drawGameOver(canvas, paint)
        }
    }

    private fun drawSettingsPopup(canvas: Canvas, paint: Paint) {
        canvas.drawColor(Color.argb(180, 0, 0, 0))

        paint.color = Color.DKGRAY
        val popupRect = RectF(sliderRect.left - 50f, sliderRect.top - 120f, sliderRect.right + 50f, exitBtnRect.bottom + 50f)
        canvas.drawRoundRect(popupRect, 30f, 30f, paint)

        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("PAUSE", popupRect.centerX(), popupRect.top + 60f, paint)

        paint.textSize = 35f
        canvas.drawText("패들 속도", popupRect.centerX(), sliderRect.top - 40f, paint)
        canvas.drawText("느림", sliderRect.left, sliderRect.bottom + 40f, paint)
        canvas.drawText("빠름", sliderRect.right, sliderRect.bottom + 40f, paint)

        paint.color = Color.GRAY
        canvas.drawRoundRect(sliderRect, 10f, 10f, paint)

        paint.color = Color.CYAN
        canvas.drawCircle(knobX, sliderRect.centerY(), knobRadius, paint)

        drawButton(canvas, paint, resumeBtnRect, "계속하기", Color.parseColor("#4CAF50"))
        drawButton(canvas, paint, restartBtnRect, "다시하기", Color.parseColor("#FF9800"))
        drawButton(canvas, paint, exitBtnRect, "나가기", Color.parseColor("#F44336"))
    }

    private fun drawGameOver(canvas: Canvas, paint: Paint) {
        // 배경 어둡게
        canvas.drawColor(Color.argb(150, 0, 0, 0))

        paint.textSize = 100f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.RED
        canvas.drawText("GAME OVER", (screenX / 2).toFloat(), (screenY / 2).toFloat() - 50f, paint)

        // [신규] 버튼 그리기
        drawButton(canvas, paint, goRestartBtnRect, "다시하기", Color.WHITE, Color.BLACK)
        drawButton(canvas, paint, goExitBtnRect, "홈으로", Color.parseColor("#F44336"))
    }

    private fun drawButton(canvas: Canvas, paint: Paint, rect: RectF, text: String, color: Int, textColor: Int = Color.WHITE) {
        paint.color = color
        canvas.drawRoundRect(rect, 20f, 20f, paint)

        paint.color = textColor
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        val baseline = rect.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, rect.centerX(), baseline, paint)
    }
}