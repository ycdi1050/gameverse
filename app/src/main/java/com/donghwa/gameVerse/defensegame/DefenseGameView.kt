package com.donghwa.gameVerse.defensegame

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View

// 메인 게임 뷰 (MVC 패턴의 Controller + View 역할)
class DefenseGameView(
    context: Context,
    maxUnlockedStage: Int,
    private val onGameOver: (Int, Int) -> Unit,
    private val onExit: () -> Unit
) : View(context) {

    private val state = DefenseGameState()
    private val logic = DefenseGameLogic(state)
    private val renderer = DefenseGameRenderer(state)

    init {
        state.maxUnlockedStage = maxUnlockedStage
        // [신규] 리소스 매니저 초기화
        ResourceManager.init(context)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        logic.initGrid(w, h)
        logic.generatePath(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width, height)

        if (state.isRunning && !state.isPaused && !state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear) {
            logic.update(
                onStageClear = { invalidate() },
                onGameOver = { invalidate(); onGameOver(state.score, 0) }
            )
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            if (state.isLevelSelection) {
                handleLevelSelectionTouch(x, y)
            } else if (state.isWeaponSelection) {
                handleWeaponSelectionTouch(x, y)
            } else if (state.isGameOver) {
                handleGameOverTouch(x, y)
            } else if (state.isStageClear) {
                handleStageClearTouch(x, y)
            } else if (state.isPaused) {
                handlePausedTouch(x, y)
            } else {
                // 게임 중
                if (x > width - 120 && y < 120) {
                    pause()
                } else {
                    val btnW = 300f
                    val btnH = 100f
                    val btnX = width / 2f
                    val btnY = height - 80f
                    if (x >= btnX - btnW/2 && x <= btnX + btnW/2 &&
                        y >= btnY - btnH/2 && y <= btnY + btnH/2) {
                        if (logic.upgradeDamage()) invalidate()
                    } else {
                        // 드래그/배치 로직은 DOWN, MOVE도 처리하므로 여기서직접 호출 안함 (아래에서)
                    }
                }
            }
        }

        // 게임 중 드래그 로직은 ACTION_DOWN, MOVE도 필요
        if (!state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear && !state.isPaused) {
            if (logic.handleTouchEvent(event.action, event.x, event.y)) {
                invalidate()
                return true
            }
        }
        return true
    }

    private fun handleLevelSelectionTouch(x: Float, y: Float) {
        val startY = 400f
        for (i in 1..5) {
            val btnY = startY + (i - 1) * 180f
            if (x >= width / 2f - 200 && x <= width / 2f + 200 &&
                y >= btnY - 60 && y <= btnY + 60) {
                if (i <= state.maxUnlockedStage) {
                    state.stage = i
                    state.isLevelSelection = false
                    state.isWeaponSelection = true
                    invalidate()
                }
            }
        }
        val lastBtnY = startY + 4 * 180f
        val homeY = lastBtnY + 250f
        if (x >= width / 2f - 150 && x <= width / 2f + 150 &&
            y >= homeY - 50 && y <= homeY + 50) {
            onExit()
        }
    }

    private fun handleWeaponSelectionTouch(x: Float, y: Float) {
        val weapons = WeaponType.values()
        val startY = 400f

        for ((i, weapon) in weapons.withIndex()) {
            val btnY = startY + i * 150f
            if (x >= width / 2f - 250 && x <= width / 2f + 250 &&
                y >= btnY - 50 && y <= btnY + 50) {
                state.selectedWeapon = weapon
                invalidate()
                return
            }
        }

        val startBtnY = height - 200f
        if (x >= width / 2f - 200 && x <= width / 2f + 200 &&
            y >= startBtnY - 50 && y <= startBtnY + 50) {
            state.isWeaponSelection = false
            state.resetForStage(state.stage)
            invalidate()
        }
    }

    private fun handleGameOverTouch(x: Float, y: Float) {
        val btnY1 = height / 2f + 50
        val btnY2 = height / 2f + 200
        if (x >= width / 2f - 250 && x <= width / 2f + 250 && y >= btnY1 - 60 && y <= btnY1 + 60) {
            state.isGameOver = false
            state.isLevelSelection = true
            invalidate()
        } else if (x >= width / 2f - 250 && x <= width / 2f + 250 && y >= btnY2 - 60 && y <= btnY2 + 60) {
            onExit()
        }
    }

    private fun handleStageClearTouch(x: Float, y: Float) {
        val btnY1 = height / 2f + 50
        val btnY2 = height / 2f + 200
        if (x >= width / 2f - 250 && x <= width / 2f + 250 && y >= btnY1 - 60 && y <= btnY1 + 60) {
            onGameOver(state.score, state.stage)
            if (state.stage < 5) {
                state.stage++
                state.isStageClear = false
                state.isWeaponSelection = true
            } else {
                state.isStageClear = false
                state.isLevelSelection = true
            }
            invalidate()
        } else if (x >= width / 2f - 250 && x <= width / 2f + 250 && y >= btnY2 - 60 && y <= btnY2 + 60) {
            onGameOver(state.score, state.stage)
            onExit()
        }
    }

    private fun handlePausedTouch(x: Float, y: Float) {
        val btnY1 = height / 2f - 50
        val btnY2 = height / 2f + 100
        val btnY3 = height / 2f + 250
        if (x >= width / 2f - 200 && x <= width / 2f + 200 && y >= btnY1 - 60 && y <= btnY1 + 60) {
            resume()
        } else if (x >= width / 2f - 200 && x <= width / 2f + 200 && y >= btnY2 - 60 && y <= btnY2 + 60) {
            state.resetForStage(state.stage)
            state.isLevelSelection = false
            state.isPaused = false
            invalidate()
        } else if (x >= width / 2f - 200 && x <= width / 2f + 200 && y >= btnY3 - 60 && y <= btnY3 + 60) {
            onExit()
        }
    }

    fun pause() {
        if (!state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear) {
            state.isPaused = true
            state.isRunning = false
            invalidate()
        }
    }

    fun resume() {
        if (state.isPaused) {
            state.isPaused = false
            state.isRunning = true
            invalidate()
        } else if (!state.isGameOver && !state.isLevelSelection && !state.isWeaponSelection && !state.isStageClear) {
            state.isRunning = true
            invalidate()
        }
    }
}