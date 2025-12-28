package com.donghwa.gameVerse.simulation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * KATO SL-650R 실제 제원 시뮬레이션 뷰
 * - [Update] 실제 크레인 형상(타이어, 운전석, 6단 붐)을 정교한 벡터 그래픽으로 구현
 */
class CraneSimulationView(context: Context, private val onExit: () -> Unit) : View(context) {

    private val state = SimulationState()
    private val renderer = CraneRenderer(state)
    private val uiManager = CraneUIManager(this, state, onExit)

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.scaleFactor *= detector.scaleFactor
            renderer.scaleFactor = renderer.scaleFactor.coerceIn(0.5f, 5.0f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (uiManager.isDraggingUI) return false
            if (e1 != null && !uiManager.isTouchOnUI(e1.x, e1.y)) {
                renderer.translateX -= distanceX
                renderer.translateY -= distanceY
                invalidate()
                return true
            }
            return false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        renderer.updateDimensions(w, h)
        uiManager.updateLayout(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#263238")) // 배경
        renderer.draw(canvas)
        uiManager.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (uiManager.onTouchEvent(event)) {
            invalidate()
            return true
        }
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    // ==========================================
    // Inner Class 1: SimulationState
    // ==========================================
    class SimulationState {
        var boomLengthIndex = 0
        var currentBoomAngle = 60.0f
        var chassisRotation = 0.0f
        var loadWeight = 10.0f
        val hookWeight = CraneData.HOOK_WEIGHT_65T
        var outriggerModeIndex = 0

        val currentBoomLen: Float get() = CraneData.BOOM_LENGTHS[boomLengthIndex]
        val currentORWidth: Float get() = CraneData.OUTRIGGER_MODES[outriggerModeIndex]
        val workingRadius: Float get() = currentBoomLen * cos(Math.toRadians(currentBoomAngle.toDouble())).toFloat()
        val totalWeight: Float get() = loadWeight + hookWeight
        val safeLoad: Float get() = CraneData.getSafeLoad(currentBoomLen, workingRadius, currentBoomAngle, chassisRotation, currentORWidth)
    }

    // ==========================================
    // Inner Class 2: CraneRenderer (그래픽 담당)
    // ==========================================
    class CraneRenderer(private val state: SimulationState) {
        var scaleFactor = 1.8f
        var translateX = 0f
        var translateY = 0f

        private val topMargin = 100f
        private val bottomUiHeight = 850f

        // Paints
        private val katoYellowPaint = Paint().apply { color = Color.parseColor("#FFC107"); style = Paint.Style.FILL; isAntiAlias = true } // KATO 노란색
        private val katoBluePaint = Paint().apply { color = Color.parseColor("#01579B"); style = Paint.Style.FILL; isAntiAlias = true } // 포인트 파란색
        private val darkMetalPaint = Paint().apply { color = Color.parseColor("#37474F"); style = Paint.Style.FILL; isAntiAlias = true } // 하부 프레임
        private val tirePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
        private val glassPaint = Paint().apply { color = Color.parseColor("#884FC3F7"); style = Paint.Style.FILL; isAntiAlias = true } // 유리창
        private val boomPaint = Paint().apply { color = Color.parseColor("#FFD54F"); style = Paint.Style.FILL; isAntiAlias = true; strokeWidth = 2f } // 붐
        private val strokePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        private val dimTextPaint = Paint().apply { color = Color.WHITE; textSize = 50f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(3f, 0f, 0f, Color.BLACK); isAntiAlias = true }
        private val dimLinePaint = Paint().apply { color = Color.WHITE; strokeWidth = 3f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f); isAntiAlias = true }

        fun updateDimensions(w: Int, h: Int) {
            translateX = w / 2f
            val visibleHeight = h - bottomUiHeight - topMargin
            translateY = topMargin + (visibleHeight / 1.8f)
        }

        fun draw(canvas: Canvas) {
            canvas.save()
            canvas.translate(translateX, translateY)
            canvas.scale(scaleFactor, scaleFactor)

            // 그리기 순서: 바닥 -> 하부체 -> 상부체(회전) -> 붐
            drawOutrigger(canvas)
            drawChassis(canvas)
            drawUpperStructure(canvas)

            canvas.restore()
        }

        // 3D -> 2D 투영 함수 (등각 투영 느낌)
        private fun project(x: Float, y: Float, z: Float): Pair<Float, Float> {
            val scale = 20f
            val px = (x - z) * scale * 0.8f
            val py = -(y + (x + z) * 0.4f) * scale
            return Pair(px, py)
        }

        // 2D 다각형 그리기 헬퍼
        private fun drawPolygon3D(canvas: Canvas, points: List<Triple<Float, Float, Float>>, paint: Paint, drawStroke: Boolean = true) {
            if (points.isEmpty()) return
            val path = Path()
            val start = project(points[0].first, points[0].second, points[0].third)
            path.moveTo(start.first, start.second)
            for (i in 1 until points.size) {
                val p = project(points[i].first, points[i].second, points[i].third)
                path.lineTo(p.first, p.second)
            }
            path.close()
            canvas.drawPath(path, paint)
            if (drawStroke) canvas.drawPath(path, strokePaint)
        }

        // [하부] 아웃리거
        private fun drawOutrigger(canvas: Canvas) {
            val bl = CraneData.BODY_LENGTH / 2
            val bw = CraneData.BODY_WIDTH / 2
            val w = state.currentORWidth / 2.0f
            val h = 0.5f // 잭 높이

            // X자 빔
            val beamPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 8f; style = Paint.Style.STROKE; isAntiAlias = true }

            val cFL = project(-bw, h, -bl + 1.5f); val tFL = project(-w, 0f, -bl + 1.5f)
            val cFR = project(bw, h, -bl + 1.5f);  val tFR = project(w, 0f, -bl + 1.5f)
            val cRL = project(-bw, h, bl - 1.5f);  val tRL = project(-w, 0f, bl - 1.5f)
            val cRR = project(bw, h, bl - 1.5f);   val tRR = project(w, 0f, bl - 1.5f)

            canvas.drawLine(cFL.first, cFL.second, tFL.first, tFL.second, beamPaint)
            canvas.drawLine(cFR.first, cFR.second, tFR.first, tFR.second, beamPaint)
            canvas.drawLine(cRL.first, cRL.second, tRL.first, tRL.second, beamPaint)
            canvas.drawLine(cRR.first, cRR.second, tRR.first, tRR.second, beamPaint)

            // 접지판(Float)
            val floatPaint = Paint().apply { color = Color.parseColor("#FFC107"); style = Paint.Style.FILL }
            for (p in listOf(tFL, tFR, tRL, tRR)) {
                canvas.drawCircle(p.first, p.second, 10f, floatPaint)
                canvas.drawCircle(p.first, p.second, 10f, strokePaint)
            }
        }

        // [하부] 주행체 (차체 + 타이어)
        private fun drawChassis(canvas: Canvas) {
            val bl = CraneData.BODY_LENGTH / 2
            val bw = CraneData.BODY_WIDTH / 2
            val deckH = 1.6f // 데크 높이
            val wheelY = 0.8f // 바퀴 중심 높이
            val wheelR = 0.8f // 바퀴 반지름
            val wheelW = 0.4f // 바퀴 폭
            val wheelZ = 2.4f // 축간거리 절반 (Wheelbase/2)

            // 1. 타이어 (4개)
            fun drawWheel(cx: Float, cz: Float) {
                // 육면체 형태로 타이어 간략화
                val pts = listOf(
                    Triple(cx - wheelW, wheelY + wheelR, cz - wheelR),
                    Triple(cx + wheelW, wheelY + wheelR, cz - wheelR),
                    Triple(cx + wheelW, wheelY - wheelR, cz - wheelR),
                    Triple(cx - wheelW, wheelY - wheelR, cz - wheelR)
                )
                // 측면만 그림 (간략화)
                drawPolygon3D(canvas, pts, tirePaint)
            }
            drawWheel(-bw, -wheelZ) // FL
            drawWheel(bw, -wheelZ)  // FR
            drawWheel(-bw, wheelZ)  // RL
            drawWheel(bw, wheelZ)   // RR

            // 2. 차체 프레임 (데크)
            val deckPoints = listOf(
                Triple(-bw, deckH, -bl), Triple(bw, deckH, -bl),
                Triple(bw, deckH, bl), Triple(-bw, deckH, bl)
            )
            // 윗면
            drawPolygon3D(canvas, deckPoints, darkMetalPaint)

            // 옆면 (두께감)
            val sidePoints = listOf(
                Triple(-bw, deckH, bl), Triple(bw, deckH, bl),
                Triple(bw, deckH - 0.5f, bl), Triple(-bw, deckH - 0.5f, bl)
            )
            drawPolygon3D(canvas, sidePoints, darkMetalPaint)

            // 치수선 (전장/전폭) - 고정체이므로 여기서 그림
            val lenStart = project(bw + 1.5f, 0f, -bl)
            val lenEnd = project(bw + 1.5f, 0f, bl)
            drawDimensionLine(canvas, lenStart, lenEnd, "${CraneData.BODY_LENGTH}m")

            val widStart = project(-bw, 0f, bl + 1.5f)
            val widEnd = project(bw, 0f, bl + 1.5f)
            drawDimensionLine(canvas, widStart, widEnd, "${CraneData.BODY_WIDTH}m")
        }

        // [상부] 선회체 (운전석 + 붐 + 카운터웨이트)
        private fun drawUpperStructure(canvas: Canvas) {
            val rotRad = Math.toRadians(state.chassisRotation.toDouble())
            val cosR = cos(rotRad).toFloat()
            val sinR = sin(rotRad).toFloat()

            // 회전 변환 함수
            fun rotate(x: Float, z: Float): Pair<Float, Float> {
                // Y축 회전 (0도 = -Z 방향 기준)
                // x' = x*cos - z*sin
                // z' = x*sin + z*cos
                // 좌표계에 맞춰 조정: 0도일때 Z음수방향
                val nx = x * cosR + z * sinR
                val nz = -x * sinR + z * cosR
                return Pair(nx, nz)
            }

            // Pivot 높이
            val pH = 2.0f

            // 1. 운전석 (Cabin) - 붐 왼쪽에 위치
            // 로컬 좌표: x(-1.2 ~ -0.2), z(-1.0 ~ 1.0), y(pH ~ pH+1.5)
            val cabPts = listOf(
                Triple(-1.4f, pH + 1.5f, -0.5f), Triple(-0.2f, pH + 1.5f, -0.5f),
                Triple(-0.2f, pH + 1.5f, 1.0f), Triple(-1.4f, pH + 1.5f, 1.0f)
            )
            val rotCabPts = cabPts.map {
                val (rx, rz) = rotate(it.first, it.third)
                Triple(rx, it.second, rz)
            }
            drawPolygon3D(canvas, rotCabPts, katoBluePaint)
            // 운전석 창문
            val winPts = cabPts.map { Triple(it.first * 0.9f, it.second + 0.1f, it.third * 0.9f) } // 약간 위로 띄움(가짜 3D)
            // 실제 구현은 복잡하니 색상으로 구분

            // 2. 붐 (Boom)
            drawBoom(canvas, pH) { x, z -> rotate(x, z) }

            // 3. 카운터웨이트 (Counterweight) - 뒤쪽
            val cwPts = listOf(
                Triple(-1.4f, pH, 1.5f), Triple(1.4f, pH, 1.5f),
                Triple(1.4f, pH + 1.0f, 1.5f), Triple(-1.4f, pH + 1.0f, 1.5f)
            )
            val rotCwPts = cwPts.map {
                val (rx, rz) = rotate(it.first, it.third)
                Triple(rx, it.second, rz)
            }
            drawPolygon3D(canvas, rotCwPts, katoYellowPaint)
        }

        private fun drawBoom(canvas: Canvas, pivotH: Float, rotateFn: (Float, Float) -> Pair<Float, Float>) {
            val boomLen = state.currentBoomLen
            val boomAngleRad = Math.toRadians(state.currentBoomAngle.toDouble())

            // 붐 끝의 로컬 좌표 (회전 전)
            // 0도일 때 -Z 방향을 바라본다고 가정
            val hDist = boomLen * cos(boomAngleRad).toFloat()
            val vDist = boomLen * sin(boomAngleRad).toFloat()

            // 로컬 좌표계: 붐은 Z축 음의 방향으로 뻗음
            val tipLocalX = 0f
            val tipLocalZ = -hDist

            // 회전 적용
            val (tipX, tipZ) = rotateFn(tipLocalX, tipLocalZ)
            val tipY = pivotH + vDist

            // 붐 그리기 (원통형 느낌의 다각형 대신 두꺼운 선으로 텔레스코픽 표현)
            val segments = 6 // 6단 붐
            val segmentLen = boomLen / segments

            // 시작점
            var currX = 0f
            var currZ = 0f
            var currY = pivotH

            val paint = Paint(boomPaint)

            // 붐 6단 그리기 (점점 얇아지게)
            for (i in 0 until segments) {
                val thickness = 18f - (i * 2f)
                paint.strokeWidth = thickness

                // 이번 세그먼트의 끝점 계산 (보간)
                val ratio = (i + 1).toFloat() / segments
                val nextX = tipX * ratio
                val nextZ = tipZ * ratio
                val nextY = pivotH + (vDist * ratio)

                // 투영
                val p1 = project(currX, currY, currZ)
                val p2 = project(nextX, nextY, nextZ)

                canvas.drawLine(p1.first, p1.second, p2.first, p2.second, paint)
                // 마디 표시
                canvas.drawCircle(p2.first, p2.second, thickness/2 + 2f, darkMetalPaint)

                currX = nextX
                currY = nextY
                currZ = nextZ
            }

            // 와이어 & 후크
            val hookGround = project(tipX, 0f, tipZ)
            val boomTip2D = project(tipX, tipY, tipZ)

            val wirePaint = Paint().apply { color = Color.WHITE; strokeWidth = 3f }
            canvas.drawLine(boomTip2D.first, boomTip2D.second, hookGround.first, hookGround.second, wirePaint)

            // 후크 (Load)
            val loadPaint = Paint().apply { color = if (state.loadWeight > 0) Color.RED else Color.LTGRAY; style = Paint.Style.FILL }
            val loadR = 10f + (state.loadWeight / 5f)
            canvas.drawCircle(hookGround.first, hookGround.second, loadR, loadPaint)

            // 작업 반경 치수선
            val center = project(0f, 0f, 0f)
            dimLinePaint.color = Color.GREEN
            canvas.drawLine(center.first, center.second, hookGround.first, hookGround.second, dimLinePaint)

            val midX = (center.first + hookGround.first) / 2
            val midY = (center.second + hookGround.second) / 2
            dimTextPaint.color = Color.GREEN
            canvas.drawText("R: ${String.format("%.1f", state.workingRadius)}m", midX, midY - 20f, dimTextPaint)
        }

        private fun drawDimensionLine(canvas: Canvas, start: Pair<Float, Float>, end: Pair<Float, Float>, text: String) {
            dimLinePaint.color = Color.WHITE
            canvas.drawLine(start.first, start.second, end.first, end.second, dimLinePaint)
            val cap = 10f
            canvas.drawLine(start.first, start.second - cap, start.first, start.second + cap, dimLinePaint)
            canvas.drawLine(end.first, end.second - cap, end.first, end.second + cap, dimLinePaint)
            val midX = (start.first + end.first) / 2
            val midY = (start.second + end.second) / 2
            dimTextPaint.color = Color.WHITE
            canvas.drawText(text, midX, midY - 20f, dimTextPaint)
        }
    }

    // ==========================================
    // Inner Class 3: CraneUIManager (UI 담당)
    // ==========================================
    class CraneUIManager(private val view: View, private val state: SimulationState, private val onExit: () -> Unit) {
        var isDraggingUI = false
        private var draggingTarget: DragTarget? = null
        enum class DragTarget { NONE, BOOM_LEN, ANGLE, ROTATE, LOAD, OUTRIGGER }

        private val bottomPanelHeight = 850f
        private val topMargin = 100f

        // UI Rects
        private val btnExit = RectF(); private val btnZoomIn = RectF(); private val btnZoomOut = RectF()
        private val sliderAngleVerticalRect = RectF(); private val sliderAngleTrackRect = RectF()
        private val btnOutriggerPrev = RectF(); private val btnOutriggerNext = RectF(); private val sliderOutriggerRect = RectF()
        private val btnBoomPrev = RectF(); private val btnBoomNext = RectF(); private val sliderBoomRect = RectF()
        private val btnRotatePrev = RectF(); private val btnRotateNext = RectF(); private val sliderRotateRect = RectF()
        private val btnLoadPrev = RectF(); private val btnLoadNext = RectF(); private val sliderLoadRect = RectF()

        // Paints
        private val hudBgPaint = Paint().apply { color = Color.parseColor("#CC121212"); style = Paint.Style.FILL; isAntiAlias = true }
        private val panelBgPaint = Paint().apply { color = Color.parseColor("#EE1E1E1E"); style = Paint.Style.FILL; isAntiAlias = true }
        private val titlePaint = Paint().apply { color = Color.CYAN; textSize = 60f; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(2f, 0f, 0f, Color.BLACK); isAntiAlias = true }
        private val labelPaint = Paint().apply { color = Color.LTGRAY; textSize = 40f; textAlign = Paint.Align.LEFT; setShadowLayer(2f, 0f, 0f, Color.BLACK); isAntiAlias = true }
        private val valuePaint = Paint().apply { color = Color.WHITE; textSize = 50f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        private val buttonPaint = Paint().apply { color = Color.parseColor("#37474F"); style = Paint.Style.FILL; isAntiAlias = true }
        private val btnTextPaint = Paint().apply { color = Color.WHITE; textSize = 50f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        private val trackPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 20f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
        private val thumbPaint = Paint().apply { color = Color.parseColor("#FFAB40"); style = Paint.Style.FILL; isAntiAlias = true; setShadowLayer(3f, 0f, 0f, Color.BLACK) }

        fun updateLayout(w: Int, h: Int) {
            val padding = 30f
            val btnSize = 130f
            val topBtnY = topMargin + padding
            btnExit.set(w - btnSize * 1.5f - padding, topBtnY, w - padding, topBtnY + 80f)

            val zoomY = btnExit.bottom + 40f
            btnZoomIn.set(w - btnSize - padding, zoomY, w - padding, zoomY + btnSize)
            btnZoomOut.set(w - btnSize - padding, zoomY + btnSize + 30f, w - padding, zoomY + btnSize * 2 + 30f)

            val sliderTop = btnZoomOut.bottom + 60f
            val sliderBottom = h - bottomPanelHeight - 60f
            val sliderRight = w - 40f
            sliderAngleVerticalRect.set(sliderRight - 100f, sliderTop, sliderRight, sliderBottom)
            sliderAngleTrackRect.set(sliderRight - 60f, sliderTop, sliderRight - 40f, sliderBottom)

            val panelTop = h - bottomPanelHeight
            val startY = panelTop + 80f
            val sideMargin = 40f
            val btnW = 140f
            val sliderW = w - (sideMargin * 2) - (btnW * 2) - 40f
            val rowHeight = 140f
            val rowSpacing = 40f

            var currY = startY
            setupControlRow(currY, sideMargin, btnW, sliderW, btnOutriggerPrev, sliderOutriggerRect, btnOutriggerNext); currY += rowHeight + rowSpacing
            setupControlRow(currY, sideMargin, btnW, sliderW, btnBoomPrev, sliderBoomRect, btnBoomNext); currY += rowHeight + rowSpacing
            setupControlRow(currY, sideMargin, btnW, sliderW, btnRotatePrev, sliderRotateRect, btnRotateNext); currY += rowHeight + rowSpacing
            setupControlRow(currY, sideMargin, btnW, sliderW, btnLoadPrev, sliderLoadRect, btnLoadNext)
        }

        private fun setupControlRow(topY: Float, margin: Float, btnW: Float, sliderW: Float, prev: RectF, slider: RectF, next: RectF) {
            val bottomY = topY + 100f
            prev.set(margin, topY, margin + btnW, bottomY)
            slider.set(prev.right + 20f, topY, prev.right + 20f + sliderW, bottomY)
            next.set(slider.right + 20f, topY, slider.right + 20f + btnW, bottomY)
        }

        fun draw(canvas: Canvas) {
            drawHUD(canvas)
            drawRightSlider(canvas)
            drawBottomPanel(canvas)
        }

        private fun drawHUD(canvas: Canvas) {
            val margin = 30f
            val width = 1000f; val height = 650f
            val bgTop = topMargin + margin
            canvas.drawRoundRect(RectF(margin, bgTop, margin + width, bgTop + height), 30f, 30f, hudBgPaint)

            var y = bgTop + 80f
            val x = margin + 50f
            val col2X = x + 500f

            canvas.drawText("KATO SL-650R MONITOR", x, y, titlePaint); y += 100f
            drawLabelValue(canvas, "붐 길이", "${state.currentBoomLen}m", x, y)
            drawLabelValue(canvas, "작업 반경", String.format("%.1fm", state.workingRadius), col2X, y); y += 110f
            drawLabelValue(canvas, "붐 각도", "${state.currentBoomAngle.toInt()}°", x, y)
            val rotText = if (abs(state.chassisRotation) <= 30) "전방" else if (abs(state.chassisRotation) >= 150) "후방" else "측면"
            drawLabelValue(canvas, "선회", "${state.chassisRotation.toInt()}° ($rotText)", col2X, y); y += 110f
            drawLabelValue(canvas, "현재 하중", String.format("%.1ft", state.totalWeight), x, y)
            drawLabelValue(canvas, "정격 하중", String.format("%.1ft", state.safeLoad), col2X, y); y += 120f

            val statusText = if (state.safeLoad == 0f) "작업 불가" else if (state.totalWeight > state.safeLoad) "과부하 경고!" else "안전 작업 중"
            val statusColor = if (state.safeLoad == 0f || state.totalWeight > state.safeLoad) Color.parseColor("#FF5252") else Color.parseColor("#69F0AE")
            val statusPaint = Paint(valuePaint).apply { color = statusColor; textSize = 64f }
            canvas.drawText(statusText, x, y, statusPaint)

            canvas.drawRoundRect(btnExit, 25f, 25f, buttonPaint)
            btnTextPaint.textSize = 40f
            drawCenteredText(canvas, "EXIT", btnExit, btnTextPaint)
            canvas.drawRoundRect(btnZoomIn, 25f, 25f, buttonPaint)
            btnTextPaint.textSize = 60f
            drawCenteredText(canvas, "+", btnZoomIn, btnTextPaint)
            canvas.drawRoundRect(btnZoomOut, 25f, 25f, buttonPaint)
            drawCenteredText(canvas, "-", btnZoomOut, btnTextPaint)
        }

        private fun drawLabelValue(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
            canvas.drawText(label, x, y - 45f, labelPaint)
            canvas.drawText(value, x, y, valuePaint)
        }

        private fun drawRightSlider(canvas: Canvas) {
            val centerX = sliderAngleTrackRect.centerX()
            canvas.drawLine(centerX, sliderAngleVerticalRect.top, centerX, sliderAngleVerticalRect.bottom, trackPaint)
            labelPaint.textAlign = Paint.Align.CENTER; labelPaint.color = Color.WHITE
            canvas.drawText("붐 각도", centerX - 70f, sliderAngleVerticalRect.top - 20f, labelPaint)
            canvas.drawText("${state.currentBoomAngle.toInt()}°", centerX - 70f, sliderAngleVerticalRect.top + 30f, titlePaint)
            val p = (state.currentBoomAngle - CraneData.MIN_ANGLE) / (CraneData.MAX_ANGLE - CraneData.MIN_ANGLE)
            val thumbY = sliderAngleVerticalRect.bottom - (p * sliderAngleVerticalRect.height())
            canvas.drawCircle(centerX, thumbY, 35f, thumbPaint)
        }

        private fun drawBottomPanel(canvas: Canvas) {
            val panelRect = RectF(0f, view.height - bottomPanelHeight, view.width.toFloat(), view.height.toFloat())
            canvas.drawRect(panelRect, panelBgPaint)
            val paint = Paint().apply { color = Color.parseColor("#FF9800"); strokeWidth = 6f }
            canvas.drawLine(0f, panelRect.top, view.width.toFloat(), panelRect.top, paint)

            val orMax = CraneData.OUTRIGGER_MODES.size - 1
            drawControlRow(canvas, "아웃리거 폭 (${state.currentORWidth}m)", btnOutriggerPrev, btnOutriggerNext, sliderOutriggerRect, 1.0f - (state.outriggerModeIndex.toFloat() / orMax))
            val lenMax = CraneData.BOOM_LENGTHS.size - 1
            drawControlRow(canvas, "붐 길이 (${state.currentBoomLen}m)", btnBoomPrev, btnBoomNext, sliderBoomRect, state.boomLengthIndex.toFloat() / lenMax)
            drawControlRow(canvas, "선회 각도 (${state.chassisRotation.toInt()}°)", btnRotatePrev, btnRotateNext, sliderRotateRect, (state.chassisRotation + 180f) / 360f)
            drawControlRow(canvas, "화물 무게 (${String.format("%.1f", state.loadWeight)}t)", btnLoadPrev, btnLoadNext, sliderLoadRect, state.loadWeight / 70f)
        }

        private fun drawControlRow(canvas: Canvas, label: String, prev: RectF, next: RectF, slider: RectF, p: Float) {
            val labelY = slider.top - 25f
            labelPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, slider.left, labelY, valuePaint)
            canvas.drawRoundRect(prev, 15f, 15f, buttonPaint)
            canvas.drawRoundRect(next, 15f, 15f, buttonPaint)
            btnTextPaint.textSize = 50f
            drawCenteredText(canvas, "◀", prev, btnTextPaint)
            drawCenteredText(canvas, "▶", next, btnTextPaint)
            val centerY = slider.centerY()
            canvas.drawLine(slider.left, centerY, slider.right, centerY, trackPaint)
            canvas.drawCircle(slider.left + p.coerceIn(0f, 1f) * slider.width(), centerY, 30f, thumbPaint)
        }

        private fun drawCenteredText(canvas: Canvas, text: String, rect: RectF, paint: Paint) {
            val x = rect.centerX()
            val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(text, x, y, paint)
        }

        fun isTouchOnUI(x: Float, y: Float): Boolean = y > view.height - bottomPanelHeight || x > view.width - 250f || y < 800f

        fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x; val y = event.y
            fun isTouch(r: RectF) = x >= r.left - 40 && x <= r.right + 40 && y >= r.top - 40 && y <= r.bottom + 40

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDraggingUI = true
                    when {
                        isTouch(sliderBoomRect) -> draggingTarget = DragTarget.BOOM_LEN
                        isTouch(sliderAngleTrackRect) || isTouch(sliderAngleVerticalRect) -> draggingTarget = DragTarget.ANGLE
                        isTouch(sliderRotateRect) -> draggingTarget = DragTarget.ROTATE
                        isTouch(sliderLoadRect) -> draggingTarget = DragTarget.LOAD
                        isTouch(sliderOutriggerRect) -> draggingTarget = DragTarget.OUTRIGGER
                        else -> { isDraggingUI = false; draggingTarget = DragTarget.NONE }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (draggingTarget != DragTarget.NONE) {
                        when (draggingTarget) {
                            DragTarget.BOOM_LEN -> state.boomLengthIndex = (((x - sliderBoomRect.left) / sliderBoomRect.width()).coerceIn(0f, 1f) * (CraneData.BOOM_LENGTHS.size - 1)).toInt()
                            DragTarget.ANGLE -> state.currentBoomAngle = CraneData.MIN_ANGLE + ((sliderAngleVerticalRect.bottom - y) / sliderAngleVerticalRect.height()).coerceIn(0f, 1f) * (CraneData.MAX_ANGLE - CraneData.MIN_ANGLE)
                            DragTarget.ROTATE -> state.chassisRotation = (((x - sliderRotateRect.left) / sliderRotateRect.width()).coerceIn(0f, 1f) * 360f) - 180f
                            DragTarget.LOAD -> state.loadWeight = ((x - sliderLoadRect.left) / sliderLoadRect.width()).coerceIn(0f, 1f) * 70f
                            DragTarget.OUTRIGGER -> state.outriggerModeIndex = ((1.0f - ((x - sliderOutriggerRect.left) / sliderOutriggerRect.width()).coerceIn(0f, 1f)) * (CraneData.OUTRIGGER_MODES.size - 1)).roundToInt()
                            else -> {}
                        }
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDraggingUI = false; draggingTarget = DragTarget.NONE
                    handleClicks(x, y)
                }
            }
            return isDraggingUI
        }

        private fun handleClicks(x: Float, y: Float) {
            if (btnExit.contains(x, y)) onExit()
            if (btnOutriggerPrev.contains(x, y)) if (state.outriggerModeIndex < CraneData.OUTRIGGER_MODES.size - 1) state.outriggerModeIndex++
            if (btnOutriggerNext.contains(x, y)) if (state.outriggerModeIndex > 0) state.outriggerModeIndex--
            if (btnBoomPrev.contains(x, y)) if (state.boomLengthIndex > 0) state.boomLengthIndex--
            if (btnBoomNext.contains(x, y)) if (state.boomLengthIndex < CraneData.BOOM_LENGTHS.size - 1) state.boomLengthIndex++
            if (btnRotatePrev.contains(x, y)) state.chassisRotation = (state.chassisRotation - 5f).coerceIn(-180f, 180f)
            if (btnRotateNext.contains(x, y)) state.chassisRotation = (state.chassisRotation + 5f).coerceIn(-180f, 180f)
            if (btnLoadPrev.contains(x, y)) state.loadWeight = (state.loadWeight - 0.5f).coerceAtLeast(0f)
            if (btnLoadNext.contains(x, y)) state.loadWeight = (state.loadWeight + 0.5f).coerceAtMost(70f)
        }
    }
}