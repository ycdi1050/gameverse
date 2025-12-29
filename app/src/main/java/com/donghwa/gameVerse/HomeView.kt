package com.donghwa.gameVerse

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class HomeView(
    context: Context,
    private val userName: String,
    private val highScore: Int,
    private val runnerHighScore: Int,
    private val defenseHighScore: Int,
    private val level: Int,
    private val currentXp: Int,
    private val leaderboard: List<String>,
    private val runnerLeaderboard: List<String>,
    private val defenseLeaderboard: List<String>,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit,
    private val onStartDefenseGame: () -> Unit,
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
        paramsCenter.setMargins(0, 500, 0, 0)
        centerLayout.layoutParams = paramsCenter

        val title = TextView(context)
        title.text = "Game Verse"
        title.textSize = 50f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 20)
        centerLayout.addView(title)

        val btnParams = LinearLayout.LayoutParams(600, 130)
        btnParams.setMargins(0, 15, 0, 15)

        // 벽돌깨기 버튼
        val startBrickBtn = Button(context)
        startBrickBtn.text = "🧱 벽돌 깨기 시작"
        startBrickBtn.textSize = 18f
        startBrickBtn.setBackgroundColor(Color.parseColor("#FF4081"))
        startBrickBtn.setTextColor(Color.WHITE)
        startBrickBtn.layoutParams = btnParams
        startBrickBtn.setOnClickListener { onStartBrickGame() }
        centerLayout.addView(startBrickBtn)

        // 러닝게임 버튼
        val startRunnerBtn = Button(context)
        startRunnerBtn.text = "🏃 무한 러닝 시작"
        startRunnerBtn.textSize = 18f
        startRunnerBtn.setBackgroundColor(Color.parseColor("#00E5FF"))
        startRunnerBtn.setTextColor(Color.BLACK)
        startRunnerBtn.layoutParams = btnParams
        startRunnerBtn.setOnClickListener { onStartRunnerGame() }
        centerLayout.addView(startRunnerBtn)

        // 디펜스 게임 버튼
        val startDefenseBtn = Button(context)
        startDefenseBtn.text = "🛡️ 디펜스 게임 시작"
        startDefenseBtn.textSize = 18f
        startDefenseBtn.setBackgroundColor(Color.parseColor("#76FF03")) // 연두색
        startDefenseBtn.setTextColor(Color.BLACK)
        startDefenseBtn.layoutParams = btnParams
        startDefenseBtn.setOnClickListener { onStartDefenseGame() }
        centerLayout.addView(startDefenseBtn)

        // 크레인 시뮬레이션 버튼
        val startSimBtn = Button(context)
        startSimBtn.text = "🏗️ 크레인 시뮬레이션"
        startSimBtn.textSize = 18f
        startSimBtn.setBackgroundColor(Color.parseColor("#FF9800"))
        startSimBtn.setTextColor(Color.WHITE)
        startSimBtn.layoutParams = btnParams
        startSimBtn.setOnClickListener { onStartSimulation() }
        centerLayout.addView(startSimBtn)

        addView(centerLayout)


        // --- [2. 정보 표시 레이아웃 (상단)] ---
        val infoLayout = LinearLayout(context)
        infoLayout.orientation = LinearLayout.VERTICAL
        infoLayout.gravity = Gravity.CENTER_HORIZONTAL
        val paramsInfo = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        paramsInfo.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        paramsInfo.setMargins(0, 50, 0, 0) // 상단 여백
        infoLayout.layoutParams = paramsInfo

        // 환영 메시지
        val welcomeText = TextView(context)
        welcomeText.text = "Lv.$level $userName"
        welcomeText.textSize = 24f
        welcomeText.setTextColor(Color.WHITE)
        welcomeText.gravity = Gravity.CENTER
        welcomeText.setTypeface(null, Typeface.BOLD)
        infoLayout.addView(welcomeText)

        // 경험치 바
        val xpLayout = LinearLayout(context)
        xpLayout.orientation = LinearLayout.VERTICAL
        xpLayout.gravity = Gravity.CENTER
        xpLayout.setPadding(100, 5, 100, 5)

        val requiredXp = level * 100
        val xpBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        xpBar.max = requiredXp
        xpBar.progress = currentXp
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            xpBar.progressTintList = ColorStateList.valueOf(Color.CYAN)
        }
        xpBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20)

        val xpText = TextView(context)
        xpText.text = "EXP: $currentXp / $requiredXp"
        xpText.textSize = 12f
        xpText.setTextColor(Color.LTGRAY)
        xpText.gravity = Gravity.END

        xpLayout.addView(xpBar)
        xpLayout.addView(xpText)
        infoLayout.addView(xpLayout)

        // 점수 요약
        val scoreLayout = LinearLayout(context)
        scoreLayout.orientation = LinearLayout.HORIZONTAL
        scoreLayout.gravity = Gravity.CENTER
        scoreLayout.setPadding(0, 5, 0, 10)

        val brickScoreText = TextView(context)
        brickScoreText.text = "벽돌: $highScore  "
        brickScoreText.textSize = 12f
        brickScoreText.setTextColor(Color.YELLOW)
        scoreLayout.addView(brickScoreText)

        val runnerScoreText = TextView(context)
        runnerScoreText.text = "러닝: $runnerHighScore  "
        runnerScoreText.textSize = 12f
        runnerScoreText.setTextColor(Color.GREEN)
        scoreLayout.addView(runnerScoreText)

        // 디펜스 점수 표시
        val defenseScoreText = TextView(context)
        defenseScoreText.text = "디펜스: $defenseHighScore"
        defenseScoreText.textSize = 12f
        defenseScoreText.setTextColor(Color.parseColor("#76FF03"))
        scoreLayout.addView(defenseScoreText)

        infoLayout.addView(scoreLayout)

        // --- 랭킹 컨테이너 (3열로 수정) ---
        val rankContainer = LinearLayout(context)
        rankContainer.orientation = LinearLayout.HORIZONTAL
        rankContainer.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        rankContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val leftRank = createRankView("🏆 벽돌 랭킹", leaderboard, Color.YELLOW)
        leftRank.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val centerRank = createRankView("🏃 러닝 랭킹", runnerLeaderboard, Color.GREEN)
        centerRank.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        // 디펜스 랭킹
        val rightRank = createRankView("🛡️ 디펜스 랭킹", defenseLeaderboard, Color.parseColor("#76FF03"))
        rightRank.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        rankContainer.addView(leftRank)
        rankContainer.addView(centerRank)
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

    private fun createRankView(title: String, list: List<String>, color: Int): LinearLayout {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL
        layout.setPadding(5, 0, 5, 0)

        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 12f
        titleView.setTextColor(color)
        titleView.setTypeface(null, Typeface.BOLD)
        titleView.gravity = Gravity.CENTER
        layout.addView(titleView)

        if (list.isEmpty()) {
            val emptyText = TextView(context)
            emptyText.text = "-"
            emptyText.textSize = 10f
            emptyText.setTextColor(Color.GRAY)
            emptyText.gravity = Gravity.CENTER
            layout.addView(emptyText)
        } else {
            for ((index, entry) in list.withIndex()) {
                val itemText = TextView(context)
                val parts = entry.split(" : ")
                val name = if (parts[0].length > 4) parts[0].substring(0, 4) + "." else parts[0]
                val score = if (parts.size > 1) parts[1] else ""

                itemText.text = "${index + 1}.$name:$score"
                itemText.textSize = 10f
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