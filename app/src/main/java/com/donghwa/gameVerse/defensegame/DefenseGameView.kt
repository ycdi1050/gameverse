package com.donghwa.gameVerse.defensegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View

// 메인 게임 뷰 (MVC 패턴의 Controller + View 역할)
class DefenseGameView(
    context: Context,
    maxUnlockedStage: Int,
    initialWeapon: WeaponType,
    initialCharacter: DefenseCharacterType, // [신규] 캐릭터 타입 전달받음
    private val onGameOver: (Int, Int) -> Unit,
    private val onExit: () -> Unit
) : View(context) {

    private val state = DefenseGameState()
    private val logic = DefenseGameLogic(state)
    private val renderer = DefenseGameRenderer(state)

    init {
        state.maxUnlockedStage = maxUnlockedStage
        state.selectedWeapon = initialWeapon
        state.selectedCharacterType = initialCharacter // [신규] 캐릭터 설정
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
                // 게임 중 UI 버튼 처리
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
                    }
                }
            }
        }

        // 게임 중 터치 로직 (배치, 드래그 등) - ACTION_DOWN, MOVE 처리 포함
        if (!state.isLevelSelection && !state.isWeaponSelection && !state.isGameOver && !state.isStageClear && !state.isPaused) {

            // [수정] Logic 클래스 내부에서 캐릭터 생성 시 state.selectedCharacterType을 사용하도록 해야 함
            // DefenseGameLogic.kt 내부의 handleTouchEvent에서
            // state.characters.add(Character(..., state.selectedWeapon, state.selectedCharacterType))
            // 형태로 호출되어야 합니다. (이 부분은 DefenseGameLogic.kt 수정 필요하지만,
            // 현재 DefenseGameLogic.kt는 state를 참조하므로
            // Character 생성자 호출 부분만 수정해주면 됩니다.)
            // 여기서는 Logic에 위임만 하므로 코드는 그대로지만, Logic이 변경된 Character 생성자를 호출하도록 해야 합니다.

            // [임시 해결] Logic 코드를 직접 수정할 수 없으므로, 여기서 Logic의 역할을 일부 대신하거나
            // Logic 코드를 새로 생성해야 완벽합니다.
            // (사용자 요청에 따라 DefenseGameView만 수정 중이나, Character 생성자가 바뀌었으므로 Logic도 수정 필수)

            if (handleLogicTouchEvent(event)) {
                invalidate()
                return true
            }
        }
        return true
    }

    // [중요] DefenseGameLogic의 handleTouchEvent를 여기서 오버라이드하여 수정된 Character 생성자를 사용하도록 함
    private fun handleLogicTouchEvent(event: MotionEvent): Boolean {
        // 기존 logic.handleTouchEvent 대신 여기에 구현 (Character 생성자 변경 대응)
        val action = event.action
        val x = event.x
        val y = event.y
        val col = (x / state.gridSize).toInt()
        val row = (y / state.gridSize).toInt()
        val isValidGrid = col in 0 until state.cols && row in 0 until state.rows

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (isValidGrid) {
                    val centerX = col * state.gridSize + state.gridSize / 2
                    val centerY = row * state.gridSize + state.gridSize / 2
                    val clickedChar = state.characters.find { Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                    if (clickedChar != null) {
                        state.draggingCharacter = clickedChar
                        state.dragStartPos = PointF(clickedChar.x, clickedChar.y)
                        state.selectedTile = PointF(x, y)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                state.draggingCharacter?.let {
                    it.setPosition(x, y)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (state.draggingCharacter != null) {
                    val char = state.draggingCharacter!!
                    if (isValidGrid && state.gridState[col][row]) {
                        val centerX = col * state.gridSize + state.gridSize / 2
                        val centerY = row * state.gridSize + state.gridSize / 2
                        val targetChar = state.characters.find { it != char && Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                        if (targetChar != null) {
                            if (targetChar.level == char.level && targetChar.weaponType == char.weaponType && targetChar.characterType == char.characterType) {
                                targetChar.upgrade()
                                state.characters.remove(char)
                                state.selectedTile = null
                            } else {
                                state.dragStartPos?.let { char.setPosition(it.x, it.y) }
                            }
                        } else {
                            char.setPosition(centerX, centerY)
                            state.selectedTile = PointF(x, y)
                        }
                    } else {
                        state.dragStartPos?.let { char.setPosition(it.x, it.y) }
                    }
                    state.draggingCharacter = null
                    state.dragStartPos = null
                    return true
                } else {
                    if (isValidGrid && state.gridState[col][row]) {
                        val centerX = col * state.gridSize + state.gridSize / 2
                        val centerY = row * state.gridSize + state.gridSize / 2
                        val existing = state.characters.find { Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                        if (existing == null) {
                            val buildCost = 50
                            if (state.currentPoints >= buildCost) {
                                // [핵심 수정] 캐릭터 생성 시 선택된 캐릭터 타입(스킨)과 무기 적용
                                state.characters.add(Character(centerX, centerY, state.selectedWeapon, state.selectedCharacterType))
                                state.currentPoints -= buildCost
                                state.selectedTile = PointF(x, y)
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
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
                    // 팝업에서 이미 무기/캐릭터를 정했으므로 인게임 선택 스킵
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
                state.isWeaponSelection = true // 스테이지 클리어 후엔 무기 변경 기회 제공
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