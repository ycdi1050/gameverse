package com.donghwa.gameVerse.defensegame

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View

class DefenseGameView(
    context: Context,
    maxUnlockedStage: Int,
    initialWeapon: WeaponType,
    initialCharacter: DefenseCharacterType,
    initialGrade: WeaponGrade,
    private val onGameOver: (Int, Int) -> Unit,
    private val onExit: () -> Unit,
    private val onItemCollected: (WeaponType, WeaponGrade) -> Unit
) : View(context) {

    private val state = DefenseGameState()
    private val logic = DefenseGameLogic(state)
    private val renderer = DefenseGameRenderer(state)

    init {
        state.maxUnlockedStage = maxUnlockedStage
        state.selectedWeapon = initialWeapon
        state.selectedCharacterType = initialCharacter
        state.selectedWeaponGrade = initialGrade
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

        if (state.isRunning && !state.isPaused && !state.isLevelSelection && !state.isWeaponSelection &&
            (!state.isGameOver && !state.isStageClear || state.isCollectingItems) && !state.isOptionSelection) {

            logic.update(
                onStageClear = { invalidate() },
                onGameOver = { invalidate(); onGameOver(state.score, 0) },
                onItemCollected = { drop ->
                    onItemCollected(drop.weaponType, drop.weaponGrade)
                }
            )
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            if (!state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear && !state.isPaused && !state.isOptionSelection) {
                if (x > width - 120 && y < 120) {
                    pause()
                } else {
                    // Upgrade button area check (approximate from renderer)
                    val btnX = width / 2f
                    val btnY = height - 80f
                    if (x >= btnX - 150 && x <= btnX + 150 &&
                        y >= btnY - 50 && y <= btnY + 50) {
                        if (logic.upgradeDamage()) invalidate()
                    }
                }
            } else if (state.isLevelSelection) {
                handleLevelSelectionTouch(x, y)
            } else if (state.isWeaponSelection) {
                handleWeaponSelectionTouch(x, y)
            } else if (state.isOptionSelection) {
                handleOptionSelectionTouch(x, y)
            } else if (state.isGameOver) {
                handleGameOverTouch(x, y)
            } else if (state.isStageClear) {
                handleStageClearTouch(x, y)
            } else if (state.isPaused) {
                handlePausedTouch(x, y)
            }
        }

        if (!state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear && !state.isPaused && !state.isCollectingItems && !state.isOptionSelection) {
            if (handleLogicTouchEvent(event)) {
                invalidate()
                return true
            }
        }
        return true
    }

    private fun handleLogicTouchEvent(event: MotionEvent): Boolean {
        return logic.handleTouchEvent(event.action, event.x, event.y)
    }

    private fun handleOptionSelectionTouch(x: Float, y: Float) {
        val options = state.currentOptions
        val cardW = 400f
        val cardH = 300f
        val startY = 500f

        for (i in options.indices) {
            val cardX = width/2f
            val cardY = startY + i * (cardH + 50f)

            if (x >= cardX - cardW/2 && x <= cardX + cardW/2 &&
                y >= cardY - cardH/2 && y <= cardY + cardH/2) {
                logic.selectOption(options[i])
                invalidate()
                return
            }
        }
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
                    state.isWeaponSelection = false
                    state.resetForStage(state.stage)
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
                state.isWeaponSelection = false
                state.resetForStage(state.stage)
                invalidate()
                return
            }
        }
    }

    private fun handleGameOverTouch(x: Float, y: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        if (x >= centerX - 300 && x <= centerX + 300 &&
            y >= centerY + 60 && y <= centerY + 140) {
            onExit()
        }
    }

    private fun handleStageClearTouch(x: Float, y: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        // Continue
        if (x >= centerX - 300 && x <= centerX + 300 &&
            y >= centerY + 60 && y <= centerY + 140) {
            if (state.stage < 5) {
                state.resetForStage(state.stage + 1)
                invalidate()
            } else {
                onExit()
            }
        }
        // Exit
        if (x >= centerX - 300 && x <= centerX + 300 &&
            y >= centerY + 140 && y <= centerY + 220) {
            onExit()
        }
    }

    private fun handlePausedTouch(x: Float, y: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        // Resume
        if (x >= centerX - 300 && x <= centerX + 300 &&
            y >= centerY + 60 && y <= centerY + 140) {
            resume()
        }
        // Exit
        if (x >= centerX - 300 && x <= centerX + 300 &&
            y >= centerY + 140 && y <= centerY + 220) {
            onExit()
        }
    }

    fun pause() {
        state.isPaused = true
        invalidate()
    }

    fun resume() {
        state.isPaused = false
        invalidate()
    }
}