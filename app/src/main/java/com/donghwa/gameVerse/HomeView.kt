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
import com.donghwa.gameVerse.defensegame.DefenseCharacterPopup
import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.WeaponType

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
    // [수정] 캐릭터 타입과 무기 타입을 모두 전달받음
    private val onStartDefenseGame: (DefenseCharacterType, WeaponType) -> Unit,
    private val onLogout: () -> Unit
) : FrameLayout(context) {

    private val PREFS_NAME = "BrickRushPrefs"
    private val KEY_SENSITIVITY = "paddle_sensitivity"

    init {
        setupUI()
    }

    private fun setupUI() {
        setBackgroundColor(Color.parseColor("#121212"))

        // --- 중앙 버튼 영역 ---
        val centerLayout = LinearLayout(context)
        centerLayout.orientation = LinearLayout.VERTICAL
        centerLayout.gravity = Gravity.CENTER
        val paramsCenter = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
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

        // [디펜스 게임 버튼]
        val startDefenseBtn = Button(context)
        startDefenseBtn.text = "🛡️ 디펜스 게임 시작"
        startDefenseBtn.textSize = 18f
        startDefenseBtn.setBackgroundColor(Color.parseColor("#76FF03"))
        startDefenseBtn.setTextColor(Color.BLACK)
        startDefenseBtn.layoutParams = btnParams
        startDefenseBtn.setOnClickListener {
            // [수정] 캐릭터+무기 선택 팝업 호출
            DefenseCharacterPopup(context) { charType, weaponType ->
                onStartDefenseGame(charType, weaponType)
            }.show()
        }
        centerLayout.addView(startDefenseBtn)

        // [미니게임 버튼]
        val miniGameBtn = Button(context)
        miniGameBtn.text = "🕹️ 미니게임 모음"
        miniGameBtn.textSize = 18f
        miniGameBtn.setBackgroundColor(Color.parseColor("#9C27B0"))
        miniGameBtn.setTextColor(Color.WHITE)
        miniGameBtn.layoutParams = btnParams
        miniGameBtn.setOnClickListener { showMiniGamePopup() }
        centerLayout.addView(miniGameBtn)

        addView(centerLayout)

        // --- 상단 정보 표시 ---
        setupTopInfo(context)
    }

    private fun setupTopInfo(context: Context) {
        val infoLayout = LinearLayout(context)
        infoLayout.orientation = LinearLayout.VERTICAL
        infoLayout.gravity = Gravity.CENTER_HORIZONTAL
        val paramsInfo = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        paramsInfo.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        paramsInfo.setMargins(0, 50, 0, 0)
        infoLayout.layoutParams = paramsInfo

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
        xpLayout.addView(xpBar)
        infoLayout.addView(xpLayout)

        // 점수 표시
        val scoreLayout = LinearLayout(context)
        scoreLayout.orientation = LinearLayout.HORIZONTAL
        scoreLayout.gravity = Gravity.CENTER
        scoreLayout.setPadding(0, 5, 0, 10)

        fun addScoreText(text: String, color: Int) {
            val tv = TextView(context)
            tv.text = text
            tv.textSize = 12f
            tv.setTextColor(color)
            scoreLayout.addView(tv)
        }
        addScoreText("벽돌: $highScore  ", Color.YELLOW)
        addScoreText("러닝: $runnerHighScore  ", Color.GREEN)
        addScoreText("디펜스: $defenseHighScore", Color.parseColor("#76FF03"))
        infoLayout.addView(scoreLayout)

        addView(infoLayout)

        // 설정 버튼
        val settingsBtn = TextView(context)
        settingsBtn.text = "⚙️"
        settingsBtn.textSize = 40f
        settingsBtn.setTextColor(Color.LTGRAY)
        val paramsSettings = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        paramsSettings.gravity = Gravity.TOP or Gravity.END
        paramsSettings.setMargins(0, 30, 30, 0)
        settingsBtn.layoutParams = paramsSettings
        settingsBtn.setOnClickListener { showSettingsDialog() }
        addView(settingsBtn)
    }

    private fun showMiniGamePopup() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(50, 50, 50, 50)
        layout.setBackgroundColor(Color.parseColor("#212121"))

        val btnParams = LinearLayout.LayoutParams(500, 130)
        btnParams.setMargins(0, 20, 0, 20)

        fun addGameBtn(text: String, color: Int, action: () -> Unit) {
            val btn = Button(context)
            btn.text = text
            btn.textSize = 16f
            btn.setBackgroundColor(color)
            btn.setTextColor(if(color==Color.parseColor("#00E5FF")) Color.BLACK else Color.WHITE)
            btn.layoutParams = btnParams
            btn.setOnClickListener { action() }
            layout.addView(btn)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("미니게임 선택")
            .setView(layout)
            .setNegativeButton("닫기", null)
            .create()

        addGameBtn("🧱 벽돌 깨기 시작", Color.parseColor("#FF4081")) { dialog.dismiss(); onStartBrickGame() }
        addGameBtn("🏃 무한 러닝 시작", Color.parseColor("#00E5FF")) { dialog.dismiss(); onStartRunnerGame() }
        addGameBtn("🏗️ 크레인 시뮬레이션", Color.parseColor("#FF9800")) { dialog.dismiss(); onStartSimulation() }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        layout.gravity = Gravity.CENTER_HORIZONTAL

        val logoutBtn = Button(context)
        logoutBtn.text = "로그아웃"
        logoutBtn.setBackgroundColor(Color.RED)
        logoutBtn.setTextColor(Color.WHITE)
        layout.addView(logoutBtn)

        val dialog = AlertDialog.Builder(context)
            .setTitle("설정")
            .setView(layout)
            .setNegativeButton("닫기", null)
            .create()

        logoutBtn.setOnClickListener {
            dialog.dismiss()
            onLogout()
        }
        dialog.show()
    }
}