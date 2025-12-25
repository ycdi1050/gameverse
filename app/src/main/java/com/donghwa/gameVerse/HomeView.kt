package com.donghwa.gameVerse

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class HomeView(
    context: Context,
    private val userName: String,
    private val highScore: Int,
    private val runnerHighScore: Int,
    private val leaderboard: List<String>,       // 벽돌 랭킹
    private val runnerLeaderboard: List<String>, // 러닝 랭킹
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onLogout: () -> Unit
) : FrameLayout(context) {

    private val PREFS_NAME = "BrickRushPrefs"
    private val KEY_SENSITIVITY = "paddle_sensitivity"

    init {
        setupUI()
    }

    private fun setupUI() {
        setBackgroundColor(Color.parseColor("#121212"))

        // --- [1. 중앙 콘텐츠 (게임 시작 버튼)] ---
        val centerLayout = LinearLayout(context)
        centerLayout.orientation = LinearLayout.VERTICAL
        centerLayout.gravity = Gravity.CENTER
        val paramsCenter = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        // 정보창 공간 확보를 위해 여백 조정
        paramsCenter.setMargins(0, 350, 0, 0)
        centerLayout.layoutParams = paramsCenter

        val title = TextView(context)
        title.text = "Game Verse"
        title.textSize = 50f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 40)
        centerLayout.addView(title)

        val startBrickBtn = Button(context)
        startBrickBtn.text = "🧱 벽돌 깨기 시작"
        startBrickBtn.textSize = 20f
        startBrickBtn.setBackgroundColor(Color.parseColor("#FF4081"))
        startBrickBtn.setTextColor(Color.WHITE)
        val btnParams = LinearLayout.LayoutParams(600, 140)
        btnParams.setMargins(0, 20, 0, 20)
        startBrickBtn.layoutParams = btnParams
        startBrickBtn.setOnClickListener { onStartBrickGame() }
        centerLayout.addView(startBrickBtn)

        val startRunnerBtn = Button(context)
        startRunnerBtn.text = "🏃 무한 러닝 시작"
        startRunnerBtn.textSize = 20f
        startRunnerBtn.setBackgroundColor(Color.parseColor("#00E5FF"))
        startRunnerBtn.setTextColor(Color.BLACK)
        startRunnerBtn.layoutParams = btnParams
        startRunnerBtn.setOnClickListener { onStartRunnerGame() }
        centerLayout.addView(startRunnerBtn)

        addView(centerLayout)


        // --- [2. 정보 표시 레이아웃 (상단 스크롤 가능)] ---
        val infoLayout = LinearLayout(context)
        infoLayout.orientation = LinearLayout.VERTICAL
        infoLayout.gravity = Gravity.CENTER_HORIZONTAL
        val paramsInfo = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        paramsInfo.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        paramsInfo.setMargins(0, 80, 0, 0) // 상단 여백
        infoLayout.layoutParams = paramsInfo

        // 환영 메시지
        val welcomeText = TextView(context)
        welcomeText.text = "환영합니다, ${userName}님!"
        welcomeText.textSize = 18f
        welcomeText.setTextColor(Color.WHITE)
        welcomeText.gravity = Gravity.CENTER
        welcomeText.setTypeface(null, Typeface.BOLD)
        infoLayout.addView(welcomeText)

        // 점수 요약 (가로 배치)
        val scoreLayout = LinearLayout(context)
        scoreLayout.orientation = LinearLayout.HORIZONTAL
        scoreLayout.gravity = Gravity.CENTER
        scoreLayout.setPadding(0, 10, 0, 20)

        val brickScoreText = TextView(context)
        brickScoreText.text = "벽돌 최고: $highScore  "
        brickScoreText.textSize = 14f
        brickScoreText.setTextColor(Color.YELLOW)
        scoreLayout.addView(brickScoreText)

        val runnerScoreText = TextView(context)
        runnerScoreText.text = "  러닝 최고: $runnerHighScore"
        runnerScoreText.textSize = 14f
        runnerScoreText.setTextColor(Color.GREEN)
        scoreLayout.addView(runnerScoreText)

        infoLayout.addView(scoreLayout)

        // --- 랭킹 컨테이너 (가로로 배치하여 공간 절약) ---
        val rankContainer = LinearLayout(context)
        rankContainer.orientation = LinearLayout.HORIZONTAL
        // [수정] Gravity.CENTER_TOP 오류 해결 -> CENTER_HORIZONTAL or TOP 사용
        rankContainer.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        rankContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // 왼쪽: 벽돌 랭킹
        val leftRank = createRankView("🏆 벽돌 랭킹", leaderboard, Color.YELLOW)
        leftRank.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        // 오른쪽: 러닝 랭킹
        val rightRank = createRankView("🏃 러닝 랭킹", runnerLeaderboard, Color.GREEN)
        rightRank.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        rankContainer.addView(leftRank)
        rankContainer.addView(rightRank)

        infoLayout.addView(rankContainer)
        addView(infoLayout)


        // --- [3. 우측 상단 설정 버튼] ---
        val settingsBtn = TextView(context)
        settingsBtn.text = "⚙️"
        settingsBtn.textSize = 40f
        settingsBtn.setTextColor(Color.LTGRAY)
        settingsBtn.setPadding(30, 30, 30, 30)

        val paramsSettings = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        paramsSettings.gravity = Gravity.TOP or Gravity.END
        paramsSettings.setMargins(0, 30, 30, 0)
        settingsBtn.layoutParams = paramsSettings

        settingsBtn.setOnClickListener {
            showSettingsDialog()
        }

        addView(settingsBtn)
    }

    // [헬퍼 함수] 랭킹 리스트 뷰 생성
    private fun createRankView(title: String, list: List<String>, color: Int): LinearLayout {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL

        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 14f
        titleView.setTextColor(color)
        titleView.setTypeface(null, Typeface.BOLD)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)

        if (list.isEmpty()) {
            val emptyText = TextView(context)
            emptyText.text = "-"
            emptyText.textSize = 12f
            emptyText.setTextColor(Color.GRAY)
            emptyText.gravity = Gravity.CENTER
            layout.addView(emptyText)
        } else {
            for ((index, entry) in list.withIndex()) {
                val itemText = TextView(context)
                // 이름이 너무 길면 잘리게 처리
                val parts = entry.split(" : ")
                val name = if (parts[0].length > 5) parts[0].substring(0, 5) + ".." else parts[0]
                val score = if (parts.size > 1) parts[1] else ""

                itemText.text = "${index + 1}. $name : $score"
                itemText.textSize = 12f
                itemText.setTextColor(Color.LTGRAY)
                itemText.gravity = Gravity.CENTER
                layout.addView(itemText)
            }
        }
        return layout
    }

    private fun showSettingsDialog() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        layout.gravity = Gravity.CENTER_HORIZONTAL

        val sensitivityLabel = TextView(context)
        sensitivityLabel.text = "패들 감도 조절"
        sensitivityLabel.textSize = 20f
        sensitivityLabel.setTextColor(Color.BLACK)
        layout.addView(sensitivityLabel)

        val seekBar = SeekBar(context)
        seekBar.max = 30
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSensitivity = prefs.getFloat(KEY_SENSITIVITY, 1.5f)
        val progress = ((savedSensitivity - 0.5f) * 10).toInt()
        seekBar.progress = progress
        val valueText = TextView(context)
        valueText.text = String.format("x%.1f", savedSensitivity)
        valueText.gravity = Gravity.CENTER
        valueText.setPadding(0, 0, 0, 30)
        layout.addView(valueText)
        layout.addView(seekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = 0.5f + (progress / 10f)
                valueText.text = String.format("x%.1f", value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val logoutBtn = Button(context)
        logoutBtn.text = "로그아웃"
        logoutBtn.setBackgroundColor(Color.RED)
        logoutBtn.setTextColor(Color.WHITE)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 50, 0, 0)
        logoutBtn.layoutParams = params
        layout.addView(logoutBtn)

        val dialog = AlertDialog.Builder(context)
            .setTitle("설정")
            .setView(layout)
            .setPositiveButton("저장") { _, _ ->
                val newSensitivity = 0.5f + (seekBar.progress / 10f)
                prefs.edit().putFloat(KEY_SENSITIVITY, newSensitivity).apply()
                Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .create()

        logoutBtn.setOnClickListener {
            dialog.dismiss()
            onLogout()
        }
        dialog.show()
    }
}