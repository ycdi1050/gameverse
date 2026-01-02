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
import android.widget.TextView
import android.widget.Toast
import com.donghwa.gameVerse.character.CharacterDataManager
import com.donghwa.gameVerse.defensegame.DefenseCharacterPopup
import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.defensegame.WeaponGrade

class HomeView(
    context: Context,
    private val uid: String,
    private val userName: String,
    private val highScore: Int,
    private val runnerHighScore: Int,
    private val defenseHighScore: Int,
    private val level: Int,
    private val currentXp: Int,
    private val leaderboard: List<String>,
    private val runnerLeaderboard: List<String>,
    private val defenseLeaderboard: List<String>,
    // [수정] 외부에서 초기화된 데이터 매니저를 전달받음
    private val characterDataManager: CharacterDataManager,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit,
    private val onStartDefenseGame: (DefenseCharacterType, WeaponType, WeaponGrade) -> Unit,
    private val onLogout: () -> Unit
) : FrameLayout(context) {

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

        // [디펜스 게임 버튼 1: 바로 시작]
        val quickStartBtn = Button(context)
        quickStartBtn.text = "🚀 작전 시작 (Quick Start)"
        quickStartBtn.textSize = 18f
        quickStartBtn.setBackgroundColor(Color.parseColor("#76FF03"))
        quickStartBtn.setTextColor(Color.BLACK)
        quickStartBtn.layoutParams = btnParams
        quickStartBtn.setOnClickListener {
            // [수정] 중복 로딩 제거하고 즉시 시작
            startDefenseGameImmediately()
        }
        centerLayout.addView(quickStartBtn)

        // [디펜스 게임 버튼 2: 설정(격납고)]
        val hangarBtn = Button(context)
        hangarBtn.text = "⚙️ 격납고 (Hangar)"
        hangarBtn.textSize = 18f
        hangarBtn.setBackgroundColor(Color.parseColor("#424242"))
        hangarBtn.setTextColor(Color.WHITE)
        hangarBtn.layoutParams = btnParams
        hangarBtn.setOnClickListener {
            // 팝업 표시 (DefenseCharacterPopup 내부에서 데이터를 확인하므로 약간의 로딩이 있을 수 있으나,
            // MainActivity에서 이미 로드했으므로 Firestore 캐시 덕분에 훨씬 빠를 것입니다)
            DefenseCharacterPopup(context, uid) { charType, weaponType, grade ->
                onStartDefenseGame(charType, weaponType, grade)
            }.show()
        }
        centerLayout.addView(hangarBtn)

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

    // [수정] 이미 로드된 데이터를 사용하여 즉시 시작 (DB 재요청 없음)
    private fun startDefenseGameImmediately() {
        // MainActivity에서 이미 loadDefenseInventory를 완료했으므로
        // characterDataManager의 변수에는 최신 데이터가 들어있습니다.
        // 따라서 별도 로딩 없이 바로 게임을 시작합니다.

        val charType = characterDataManager.equippedDefenseCharacter
        val weaponType = characterDataManager.equippedDefenseWeapon
        val weaponGrade = characterDataManager.equippedDefenseWeaponGrade

        // 바로 게임 시작 콜백 호출
        onStartDefenseGame(charType, weaponType, weaponGrade)
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
        MiniGamePopup(context, onStartBrickGame, onStartRunnerGame, onStartSimulation).show()
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