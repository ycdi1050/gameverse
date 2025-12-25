package com.donghwa.gameVerse.runnergame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class RunnerGameView(
    context: Context,
    private val onExit: () -> Unit, // 홈으로 나가기 콜백
    private val onGameOver: (Int) -> Unit // 점수 전달 콜백
) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    private var screenX = 0
    private var screenY = 0
    private var isReady = false

    // 게임 객체들
    private lateinit var player: RunnerPlayer
    private lateinit var obstacleManager: ObstacleManager
    private lateinit var difficultyManager: DifficultyManager

    // 게임 상태 변수
    private var score = 0
    private var isGameOver = false
    private var isScoreSaved = false // 점수 중복 저장 방지

    // [신규] 일시정지(설정) 상태 변수
    private var isSettingsOpen = false

    // 버튼 영역들
    private val gearRect = RectF() // 톱니바퀴

    // 일시정지 메뉴 버튼
    private val pauseResumeBtnRect = RectF()
    private val pauseRestartBtnRect = RectF()
    private val pauseExitBtnRect = RectF()

    // 게임 오버 화면 버튼
    private val restartBtnRect = RectF()
    private val exitBtnRect = RectF()

    // 게임 루프
    override fun run() {
        while (isPlaying) {
            // [수정] 설정창이 열려있지 않을 때만 게임 업데이트
            if (isReady && !isGameOver && !isSettingsOpen) {
                update()
            }
            draw()
            control()
        }
    }

    // 화면 크기가 정해지면 초기화
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w
        screenY = h

        // 객체 생성
        player = RunnerPlayer(w, h)
        obstacleManager = ObstacleManager(w, h)
        difficultyManager = DifficultyManager()

        // 1. 톱니바퀴 위치 (우측 상단)
        val gearSize = 80f
        val margin = 50f
        gearRect.set(w - gearSize - margin, margin, w - margin, margin + gearSize)

        // 2. 일시정지 메뉴 버튼 위치 (화면 중앙)
        val btnWidth = 400f
        val btnHeight = 90f
        val btnGap = 40f
        val centerX = w / 2f
        val centerY = h / 2f

        val pResumeTop = centerY - 150f
        pauseResumeBtnRect.set(centerX - btnWidth/2, pResumeTop, centerX + btnWidth/2, pResumeTop + btnHeight)

        val pRestartTop = pauseResumeBtnRect.bottom + btnGap
        pauseRestartBtnRect.set(centerX - btnWidth/2, pRestartTop, centerX + btnWidth/2, pRestartTop + btnHeight)

        val pExitTop = pauseRestartBtnRect.bottom + btnGap
        pauseExitBtnRect.set(centerX - btnWidth/2, pExitTop, centerX + btnWidth/2, pExitTop + btnHeight)

        // 3. 게임 오버 버튼 위치 (화면 중앙 하단)
        val restartTop = centerY + 50f
        restartBtnRect.set(centerX - btnWidth/2, restartTop, centerX + btnWidth/2, restartTop + btnHeight)

        val exitTop = restartBtnRect.bottom + 40f
        exitBtnRect.set(centerX - btnWidth/2, exitTop, centerX + btnWidth/2, exitTop + btnHeight)

        isReady = true
    }

    // 게임 로직 업데이트
    private fun update() {
        // 난이도 업데이트 (점수 기반)
        difficultyManager.update(score)

        player.update()
        // 장애물 업데이트 (난이도 매니저 전달)
        obstacleManager.update(difficultyManager)

        score++

        // 충돌 체크
        if (obstacleManager.checkCollision(player.getRect())) {
            isGameOver = true
            // 게임 오버 시 점수 1회 전달
            if (!isScoreSaved) {
                isScoreSaved = true
                onGameOver(score)
            }
        }
    }

    // 화면 그리기
    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas()

            // 배경색
            canvas.drawColor(Color.parseColor("#202040"))

            if (isReady) {
                // 바닥 그리기
                paint.color = Color.GRAY
                canvas.drawRect(0f, screenY - 200f, screenX.toFloat(), screenY.toFloat(), paint)

                // 캐릭터와 장애물 그리기
                player.draw(canvas, paint)
                obstacleManager.draw(canvas, paint)

                // 점수 표시
                paint.color = Color.WHITE
                paint.textSize = 60f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Score: $score", 50f, 100f, paint)

                // 톱니바퀴 아이콘 (항상 표시)
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 60f
                paint.color = Color.LTGRAY
                canvas.drawText("⚙️", gearRect.centerX(), gearRect.centerY() + 20f, paint)

                // 레벨업 알림 표시
                if (difficultyManager.isLevelUpEffectActive && !isSettingsOpen && !isGameOver) {
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = 120f
                    paint.color = Color.CYAN
                    // 깜빡임 효과
                    if ((score / 10) % 2 == 0) {
                        canvas.drawText("LEVEL UP!", (screenX / 2).toFloat(), (screenY / 2).toFloat() - 200f, paint)
                    }
                }

                // [신규] 일시정지(설정) 메뉴 화면
                if (isSettingsOpen) {
                    drawPauseMenu(canvas, paint)
                }
                // 게임 오버 화면
                else if (isGameOver) {
                    drawGameOverScreen(canvas, paint)
                }
            }

            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawPauseMenu(canvas: Canvas, paint: Paint) {
        // 반투명 배경
        canvas.drawColor(Color.argb(150, 0, 0, 0))

        // 팝업 박스 배경
        paint.color = Color.DKGRAY
        val popupRect = RectF(pauseResumeBtnRect.left - 50f, pauseResumeBtnRect.top - 100f, pauseResumeBtnRect.right + 50f, pauseExitBtnRect.bottom + 50f)
        canvas.drawRoundRect(popupRect, 30f, 30f, paint)

        // 제목
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("PAUSE", popupRect.centerX(), popupRect.top + 70f, paint)

        // 버튼들
        drawButton(canvas, paint, pauseResumeBtnRect, "계속하기", Color.parseColor("#4CAF50"))
        drawButton(canvas, paint, pauseRestartBtnRect, "다시하기", Color.parseColor("#FF9800"))
        drawButton(canvas, paint, pauseExitBtnRect, "나가기", Color.parseColor("#F44336"))
    }

    private fun drawGameOverScreen(canvas: Canvas, paint: Paint) {
        canvas.drawColor(Color.argb(150, 0, 0, 0))

        paint.textSize = 100f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.RED
        canvas.drawText("GAME OVER", (screenX / 2).toFloat(), (screenY / 2).toFloat() - 50f, paint)

        drawButton(canvas, paint, restartBtnRect, "다시하기", Color.parseColor("#FF9800"))
        drawButton(canvas, paint, exitBtnRect, "홈으로", Color.parseColor("#F44336"))
    }

    // 버튼 그리기 헬퍼 함수
    private fun drawButton(canvas: Canvas, paint: Paint, rect: RectF, text: String, color: Int) {
        paint.color = color
        canvas.drawRoundRect(rect, 20f, 20f, paint)

        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        val baseline = rect.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, rect.centerX(), baseline, paint)
    }

    // 프레임 속도 제어
    private fun control() {
        try {
            Thread.sleep(17)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // 터치 이벤트 처리
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            if (isSettingsOpen) {
                // 일시정지 메뉴 터치 처리
                if (pauseResumeBtnRect.contains(x, y)) {
                    isSettingsOpen = false
                } else if (pauseRestartBtnRect.contains(x, y)) {
                    resetGame()
                } else if (pauseExitBtnRect.contains(x, y)) {
                    exitGame()
                } else {
                    // 메뉴 밖 터치 시 닫기
                    isSettingsOpen = false
                }
            } else if (isGameOver) {
                // 게임 오버 버튼 처리
                if (restartBtnRect.contains(x, y)) {
                    resetGame()
                } else if (exitBtnRect.contains(x, y)) {
                    exitGame()
                }
            } else {
                // 게임 플레이 중
                if (gearRect.contains(x, y)) {
                    isSettingsOpen = true // 설정 메뉴 열기
                } else {
                    player.jump() // 점프
                }
            }
        }
        return true
    }

    // 스레드 종료 및 홈으로 이동
    fun exitGame() {
        isPlaying = false
        onExit()
    }

    private fun resetGame() {
        score = 0
        isGameOver = false
        isSettingsOpen = false
        isScoreSaved = false
        player.reset()
        obstacleManager.reset()
        difficultyManager.reset()
    }

    fun pause() {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        if (isPlaying) return
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }
}