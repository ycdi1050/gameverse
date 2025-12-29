package com.donghwa.gameVerse

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout

/**
 * 미니게임 선택 팝업을 관리하는 클래스
 */
class MiniGamePopup(
    private val context: Context,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit
) {

    fun show() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(50, 50, 50, 50)
        // 팝업 배경색 (어두운 회색)
        layout.setBackgroundColor(Color.parseColor("#212121"))

        val btnParams = LinearLayout.LayoutParams(500, 130)
        btnParams.setMargins(0, 20, 0, 20)

        // 1. 벽돌깨기 버튼
        val startBrickBtn = Button(context)
        startBrickBtn.text = "🧱 벽돌 깨기 시작"
        startBrickBtn.textSize = 16f
        startBrickBtn.setBackgroundColor(Color.parseColor("#FF4081"))
        startBrickBtn.setTextColor(Color.WHITE)
        startBrickBtn.layoutParams = btnParams
        layout.addView(startBrickBtn)

        // 2. 러닝게임 버튼
        val startRunnerBtn = Button(context)
        startRunnerBtn.text = "🏃 무한 러닝 시작"
        startRunnerBtn.textSize = 16f
        startRunnerBtn.setBackgroundColor(Color.parseColor("#00E5FF"))
        startRunnerBtn.setTextColor(Color.BLACK)
        startRunnerBtn.layoutParams = btnParams
        layout.addView(startRunnerBtn)

        // 3. 크레인 시뮬레이션 버튼
        val startSimBtn = Button(context)
        startSimBtn.text = "🏗️ 크레인 시뮬레이션"
        startSimBtn.textSize = 16f
        startSimBtn.setBackgroundColor(Color.parseColor("#FF9800"))
        startSimBtn.setTextColor(Color.WHITE)
        startSimBtn.layoutParams = btnParams
        layout.addView(startSimBtn)

        val dialog = AlertDialog.Builder(context)
            .setTitle("미니게임 선택")
            .setView(layout)
            .setNegativeButton("닫기", null)
            .create()

        // 클릭 이벤트 설정 (다이얼로그 닫기 + 게임 시작)
        startBrickBtn.setOnClickListener {
            dialog.dismiss()
            onStartBrickGame()
        }
        startRunnerBtn.setOnClickListener {
            dialog.dismiss()
            onStartRunnerGame()
        }
        startSimBtn.setOnClickListener {
            dialog.dismiss()
            onStartSimulation()
        }

        dialog.show()
    }
}