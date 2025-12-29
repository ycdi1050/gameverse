package com.donghwa.gameVerse

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MiniGameSelectionView(
    context: Context,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit,
    private val onBack: () -> Unit
) : FrameLayout(context) {

    init {
        setupUI()
    }

    private fun setupUI() {
        setBackgroundColor(Color.parseColor("#121212"))

        // 전체 레이아웃
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        layout.layoutParams = params

        // 타이틀
        val title = TextView(context)
        title.text = "미니게임 선택"
        title.textSize = 36f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 60)
        layout.addView(title)

        // 버튼 공통 파라미터
        val btnParams = LinearLayout.LayoutParams(600, 130)
        btnParams.setMargins(0, 20, 0, 20)

        // 벽돌깨기 버튼
        val startBrickBtn = Button(context)
        startBrickBtn.text = "🧱 벽돌 깨기 시작"
        startBrickBtn.textSize = 18f
        startBrickBtn.setBackgroundColor(Color.parseColor("#FF4081"))
        startBrickBtn.setTextColor(Color.WHITE)
        startBrickBtn.layoutParams = btnParams
        startBrickBtn.setOnClickListener { onStartBrickGame() }
        layout.addView(startBrickBtn)

        // 러닝게임 버튼
        val startRunnerBtn = Button(context)
        startRunnerBtn.text = "🏃 무한 러닝 시작"
        startRunnerBtn.textSize = 18f
        startRunnerBtn.setBackgroundColor(Color.parseColor("#00E5FF"))
        startRunnerBtn.setTextColor(Color.BLACK)
        startRunnerBtn.layoutParams = btnParams
        startRunnerBtn.setOnClickListener { onStartRunnerGame() }
        layout.addView(startRunnerBtn)

        // 크레인 시뮬레이션 버튼
        val startSimBtn = Button(context)
        startSimBtn.text = "🏗️ 크레인 시뮬레이션"
        startSimBtn.textSize = 18f
        startSimBtn.setBackgroundColor(Color.parseColor("#FF9800"))
        startSimBtn.setTextColor(Color.WHITE)
        startSimBtn.layoutParams = btnParams
        startSimBtn.setOnClickListener { onStartSimulation() }
        layout.addView(startSimBtn)

        // 뒤로가기 버튼
        val backBtn = Button(context)
        backBtn.text = "뒤로가기"
        backBtn.textSize = 16f
        backBtn.setBackgroundColor(Color.GRAY)
        backBtn.setTextColor(Color.WHITE)
        backBtn.layoutParams = btnParams
        backBtn.setOnClickListener { onBack() }
        layout.addView(backBtn)

        addView(layout)
    }
}