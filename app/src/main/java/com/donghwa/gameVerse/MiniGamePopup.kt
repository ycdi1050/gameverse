package com.donghwa.gameVerse

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * 미니게임 선택 팝업을 관리하는 클래스
 */
class MiniGamePopup(
    private val context: Context,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit
) {

    // [신규] 팝업 없이 View만 리턴하는 메서드 (HomeView 탭 임베딩용)
    fun getContentView(): View {
        return createLayout(isEmbedded = true, dialog = null)
    }

    fun show() {
        val dialog = AlertDialog.Builder(context).create()
        val layout = createLayout(isEmbedded = false, dialog = dialog)

        dialog.setView(layout)
        dialog.setTitle("미니게임 선택")
        dialog.show()
    }

    private fun createLayout(isEmbedded: Boolean, dialog: AlertDialog?): View {
        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#212121"))
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
        btnParams.setMargins(0, 20, 0, 20)

        // 1. 벽돌깨기 버튼
        val startBrickBtn = createGameButton("🧱 벽돌 깨기 시작", "#FF4081", Color.WHITE)
        startBrickBtn.layoutParams = btnParams
        startBrickBtn.setOnClickListener {
            dialog?.dismiss()
            onStartBrickGame()
        }
        layout.addView(startBrickBtn)

        // 2. 러닝게임 버튼
        val startRunnerBtn = createGameButton("🏃 무한 러닝 시작", "#00E5FF", Color.BLACK)
        startRunnerBtn.layoutParams = btnParams
        startRunnerBtn.setOnClickListener {
            dialog?.dismiss()
            onStartRunnerGame()
        }
        layout.addView(startRunnerBtn)

        // 3. 크레인 시뮬레이션 버튼
        val startSimBtn = createGameButton("🏗️ 크레인 시뮬레이션", "#FF9800", Color.WHITE)
        startSimBtn.layoutParams = btnParams
        startSimBtn.setOnClickListener {
            dialog?.dismiss()
            onStartSimulation()
        }
        layout.addView(startSimBtn)

        // 임베딩이 아닐 때만 닫기 버튼 표시
        if (!isEmbedded && dialog != null) {
            val closeBtn = Button(context).apply {
                text = "닫기"
                setBackgroundColor(Color.DKGRAY)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(200, 100).apply {
                    topMargin = 50
                }
                setOnClickListener { dialog.dismiss() }
            }
            layout.addView(closeBtn)
        }

        scrollView.addView(layout)
        return scrollView
    }

    private fun createGameButton(text: String, bgColor: String, textColor: Int): Button {
        return Button(context).apply {
            this.text = text
            this.textSize = 18f
            setBackgroundColor(Color.parseColor(bgColor))
            setTextColor(textColor)
            elevation = 10f
        }
    }
}