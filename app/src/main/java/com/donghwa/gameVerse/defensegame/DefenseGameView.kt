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
    private val onItemCollected: (WeaponType, WeaponGrade) -> Unit,
    // [중요] MainActivity에서 전달한 난이도 (기본값 NORMAL)
    initialDifficulty: Difficulty = Difficulty.NORMAL
) : View(context) {

    private val state = DefenseGameState()
    private val logic = DefenseGameLogic(state)
    private val renderer = DefenseGameRenderer(state)

    // 중복 종료 방지 플래그
    private var isExiting = false

    init {
        state.maxUnlockedStage = maxUnlockedStage
        state.selectedWeapon = initialWeapon
        state.selectedCharacterType = initialCharacter
        state.selectedWeaponGrade = initialGrade

        // 난이도 설정
        state.difficulty = initialDifficulty

        // 스테이지 선택 없이 바로 시작 (기본값 Stage 1, 필요 시 수정 가능)
        state.isLevelSelection = true

        ResourceManager.init(context)
    }

    // [신규] 생성자 오버로딩 (스테이지 지정 가능 버전)
    constructor(
        context: Context,
        maxUnlockedStage: Int,
        initialStage: Int,
        initialWeapon: WeaponType,
        initialCharacter: DefenseCharacterType,
        initialGrade: WeaponGrade,
        initialDifficulty: Difficulty,
        onGameOver: (Int, Int) -> Unit,
        onExit: () -> Unit,
        onItemCollected: (WeaponType, WeaponGrade) -> Unit
    ) : this(context, maxUnlockedStage, initialWeapon, initialCharacter, initialGrade, onGameOver, onExit, onItemCollected, initialDifficulty) {
        if (initialStage > 0) {
            state.stage = initialStage
            state.isLevelSelection = false
            state.isWeaponSelection = false
            state.resetForStage(initialStage)
        }
    }

    // [수정] 동을 가져오면서 0으로 초기화 (중복 저장 방지)
    fun consumeAcquiredDong(): Int {
        val amount = state.acquiredDong
        state.acquiredDong = 0
        return amount
    }

    fun getDifficulty(): Difficulty {
        return state.difficulty
    }

    fun getCurrentWave(): Int {
        return state.currentWave
    }

    // [신규] 현재 스테이지 번호 반환 (게임 오버 시에도 사용)
    fun getStage(): Int {
        return state.stage
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
                onStageClear = {
                    // [핵심 수정] 스테이지 클리어 시 즉시 데이터 저장 요청
                    onGameOver(state.score, state.stage)
                    invalidate()
                },
                onGameOver = {
                    invalidate()
                    onGameOver(state.score, 0) // 실패 시(0)
                },
                onItemCollected = { drop ->
                    // [수정] drop.weaponType이 nullable일 수 있으므로 안전하게 처리
                    val wType = drop.weaponType
                    if (wType != null) {
                        onItemCollected(wType, drop.weaponGrade)
                    }
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
                if (x > width - 80 && y < 120) {
                    pause()
                }
                else if (x > width - 200 && x < width - 80 && y < 120 && event.action == MotionEvent.ACTION_UP) {
                    cycleGameSpeed()
                }
                else {
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

    private fun cycleGameSpeed() {
        state.gameSpeed++
        if (state.gameSpeed > 3) state.gameSpeed = 1
        invalidate()
    }

    private fun handleLogicTouchEvent(event: MotionEvent): Boolean = logic.handleTouchEvent(event.action, event.x, event.y)

    private fun handleOptionSelectionTouch(x: Float, y: Float) {
        val options = state.currentOptions
        val cardW = 400f; val cardH = 250f; val startY = 450f; val gap = 50f
        for (i in options.indices) {
            val cardX = width/2f; val cardY = startY + i * (cardH + gap)
            if (x >= cardX - cardW/2 && x <= cardX + cardW/2 && y >= cardY - cardH/2 && y <= cardY + cardH/2) {
                logic.selectOption(options[i]); invalidate(); return
            }
        }
    }

    private fun handleLevelSelectionTouch(x: Float, y: Float) {
        val startY = 400f
        for (i in 1..5) {
            val btnY = startY + (i - 1) * 180f
            if (x >= width / 2f - 200 && x <= width / 2f + 200 && y >= btnY - 60 && y <= btnY + 60) {
                if (i <= state.maxUnlockedStage) {
                    state.stage = i; state.isLevelSelection = false; state.isWeaponSelection = false
                    state.resetForStage(state.stage); invalidate()
                }
            }
        }
        val lastBtnY = startY + 4 * 180f; val homeY = lastBtnY + 250f
        if (x >= width / 2f - 150 && x <= width / 2f + 150 && y >= homeY - 50 && y <= homeY + 50) safeExit()
    }

    private fun handleWeaponSelectionTouch(x: Float, y: Float) {
        val weapons = WeaponType.values(); val startY = 400f
        for ((i, weapon) in weapons.withIndex()) {
            val btnY = startY + i * 150f
            if (x >= width / 2f - 250 && x <= width / 2f + 250 && y >= btnY - 50 && y <= btnY + 50) {
                state.selectedWeapon = weapon; state.isWeaponSelection = false
                state.resetForStage(state.stage); invalidate(); return
            }
        }
    }

    private fun handleGameOverTouch(x: Float, y: Float) {
        val centerX = width / 2f; val centerY = height / 2f
        if (x >= centerX - 300 && x <= centerX + 300 && y >= centerY + 60 && y <= centerY + 140) safeExit()
    }

    private fun handleStageClearTouch(x: Float, y: Float) {
        val centerX = width / 2f; val centerY = height / 2f
        if (x >= centerX - 300 && x <= centerX + 300 && y >= centerY + 60 && y <= centerY + 140) {
            if (state.stage < 5) {
                state.resetForStage(state.stage + 1); invalidate()
            } else {
                safeExit()
            }
        }
        if (x >= centerX - 300 && x <= centerX + 300 && y >= centerY + 140 && y <= centerY + 220) safeExit()
    }

    private fun handlePausedTouch(x: Float, y: Float) {
        val centerX = width / 2f; val centerY = height / 2f; val boxH = 900f; val buttonBaseY = centerY + boxH/2
        if (x >= centerX - 300 && x <= centerX + 300 && y >= buttonBaseY - 160 && y <= buttonBaseY - 90) resume()
        if (x >= centerX - 300 && x <= centerX + 300 && y >= buttonBaseY - 90 && y <= buttonBaseY - 20) safeExit()
    }

    private fun safeExit() {
        if (isExiting) return
        isExiting = true
        post { onExit() }
    }

    fun pause() { state.isPaused = true; invalidate() }
    fun resume() { state.isPaused = false; invalidate() }
}