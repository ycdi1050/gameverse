package com.donghwa.gameVerse

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.donghwa.gameVerse.character.CharacterDataManager
import com.donghwa.gameVerse.defensegame.DefenseCharacterPopup
import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.ResourceManager
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
    private val characterDataManager: CharacterDataManager,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit,
    private val onStartDefenseGame: (DefenseCharacterType, WeaponType, WeaponGrade) -> Unit,
    private val onLogout: () -> Unit
) : RelativeLayout(context) {

    private lateinit var contentContainer: FrameLayout
    private lateinit var tabContainer: LinearLayout

    // 탭 버튼들
    private lateinit var btnTabHangar: LinearLayout
    private lateinit var btnTabOperation: LinearLayout
    private lateinit var btnTabMiniGames: LinearLayout

    private enum class Tab { HANGAR, OPERATION, MINI_GAMES }
    private var currentTab = Tab.OPERATION

    init {
        ResourceManager.init(context)
        setupUI()
        // 초기 탭 설정 (작전 시작)
        switchTab(Tab.OPERATION)
    }

    private fun setupUI() {
        setBackgroundColor(Color.parseColor("#121212"))

        // 1. 상단 정보 영역 (Top Bar)
        val topBar = createTopInfoBar()
        topBar.id = View.generateViewId()
        val topParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        topParams.addRule(ALIGN_PARENT_TOP)
        addView(topBar, topParams)

        // 2. 하단 네비게이션 바 (Bottom Navigation)
        val bottomNav = createBottomNavBar()
        bottomNav.id = View.generateViewId()
        val bottomParams = LayoutParams(LayoutParams.MATCH_PARENT, 180) // 높이 고정
        bottomParams.addRule(ALIGN_PARENT_BOTTOM)
        addView(bottomNav, bottomParams)

        // 3. 중앙 콘텐츠 영역 (Content Container)
        contentContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        val contentParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        contentParams.addRule(BELOW, topBar.id)
        contentParams.addRule(ABOVE, bottomNav.id)
        addView(contentContainer, contentParams)
    }

    private fun createTopInfoBar(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(30, 30, 30, 30)
            elevation = 10f
        }

        // 1행: 레벨 및 이름 + 설정 버튼
        val row1 = RelativeLayout(context)

        val welcomeText = TextView(context).apply {
            text = "Lv.$level $userName"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        val nameParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        nameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        nameParams.addRule(RelativeLayout.CENTER_VERTICAL)
        row1.addView(welcomeText, nameParams)

        val settingsBtn = TextView(context).apply {
            text = "⚙️"
            textSize = 24f
            setPadding(20, 10, 20, 10)
            setOnClickListener { showSettingsDialog() }
        }
        val settingsParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        settingsParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        settingsParams.addRule(RelativeLayout.CENTER_VERTICAL)
        row1.addView(settingsBtn, settingsParams)

        container.addView(row1)

        // 2행: 경험치 바
        val xpLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 15, 0, 15)
        }
        val requiredXp = level * 100
        val xpBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = requiredXp
            progress = currentXp
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                progressTintList = ColorStateList.valueOf(Color.CYAN)
            }
        }
        val xpParams = LinearLayout.LayoutParams(0, 15, 1f)
        xpLayout.addView(xpBar, xpParams)

        val xpText = TextView(context).apply {
            text = "${(currentXp.toFloat()/requiredXp * 100).toInt()}%"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            setPadding(20, 0, 0, 0)
        }
        xpLayout.addView(xpText)

        container.addView(xpLayout)

        // 3행: 점수 요약
        val scoreText = TextView(context).apply {
            text = "🏆 벽돌: $highScore | 러닝: $runnerHighScore | 디펜스: $defenseHighScore"
            textSize = 12f
            setTextColor(Color.parseColor("#B0BEC5"))
            gravity = Gravity.CENTER
        }
        container.addView(scoreText)

        return container
    }

    private fun createBottomNavBar(): View {
        val navContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151515"))
            weightSum = 3f
        }

        btnTabHangar = createNavButton("격납고", "🛠️", Tab.HANGAR)
        btnTabOperation = createNavButton("작전 시작", "🚀", Tab.OPERATION)
        btnTabMiniGames = createNavButton("미니게임", "🕹️", Tab.MINI_GAMES)

        navContainer.addView(btnTabHangar)
        navContainer.addView(btnTabOperation)
        navContainer.addView(btnTabMiniGames)

        return navContainer
    }

    private fun createNavButton(title: String, icon: String, tab: Tab): LinearLayout {
        val btnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

            // [수정] 오류 발생 라인 제거 (배경색은 updateTabUI에서 직접 관리)
            // setBackgroundResource(android.R.drawable.selectable_item_background)

            setOnClickListener { switchTab(tab) }
        }

        val iconTv = TextView(context).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
        }
        btnLayout.addView(iconTv)

        val titleTv = TextView(context).apply {
            text = title
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        btnLayout.addView(titleTv)

        // 태그에 TextView 저장 (색상 변경용)
        btnLayout.tag = titleTv

        return btnLayout
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        contentContainer.removeAllViews()

        // 탭 UI 업데이트 (선택된 탭 강조)
        updateTabUI(btnTabHangar, tab == Tab.HANGAR)
        updateTabUI(btnTabOperation, tab == Tab.OPERATION)
        updateTabUI(btnTabMiniGames, tab == Tab.MINI_GAMES)

        // 콘텐츠 로드
        val contentView = when (tab) {
            Tab.HANGAR -> createHangarView()
            Tab.OPERATION -> createOperationView()
            Tab.MINI_GAMES -> createMiniGamesView()
        }
        contentContainer.addView(contentView)
    }

    private fun updateTabUI(tabLayout: LinearLayout, isSelected: Boolean) {
        val titleTv = tabLayout.tag as TextView
        if (isSelected) {
            tabLayout.setBackgroundColor(Color.parseColor("#252525"))
            titleTv.setTextColor(Color.CYAN)
        } else {
            tabLayout.setBackgroundColor(Color.TRANSPARENT)
            titleTv.setTextColor(Color.GRAY)
        }
    }

    // --- 각 탭의 콘텐츠 생성 ---

    private fun createHangarView(): View {
        // 기존 팝업 로직을 재사용하여 View만 가져옴
        val popupLogic = DefenseCharacterPopup(
            context,
            uid,
            characterDataManager
        ) { charType, weaponType, grade ->
            onStartDefenseGame(charType, weaponType, grade)
        }
        return popupLogic.getContentView()
    }

    private fun createOperationView(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val logoText = TextView(context).apply {
            text = "GAME VERSE"
            textSize = 50f
            setTextColor(Color.CYAN)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            setShadowLayer(10f, 0f, 0f, Color.BLUE)
            gravity = Gravity.CENTER
        }
        layout.addView(logoText)

        val subText = TextView(context).apply {
            text = "Sector 7 - Defense Operation"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 100)
        }
        layout.addView(subText)

        // 현재 장착 정보 표시
        val currentInfo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            layoutParams = LinearLayout.LayoutParams(800, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 50
            }
        }

        val charName = characterDataManager.equippedDefenseCharacter.name
        val wepName = characterDataManager.equippedDefenseWeapon.name
        val grade = characterDataManager.equippedDefenseWeaponGrade.name

        val infoText = TextView(context).apply {
            text = "현재 장비 상태\n\n캐릭터: $charName\n무기: $wepName [$grade]"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
        }
        currentInfo.addView(infoText)
        layout.addView(currentInfo)

        // 게임 시작 버튼
        val startBtn = Button(context).apply {
            text = "START OPERATION"
            textSize = 24f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(600, 180)

            val gd = GradientDrawable().apply {
                setColor(Color.parseColor("#76FF03"))
                cornerRadius = 20f
                setStroke(5, Color.WHITE)
            }
            background = gd

            setOnClickListener { startDefenseGameImmediately() }
        }
        layout.addView(startBtn)

        return layout
    }

    private fun createMiniGamesView(): View {
        // 기존 팝업 로직을 재사용하여 View만 가져옴
        val popupLogic = MiniGamePopup(
            context,
            onStartBrickGame,
            onStartRunnerGame,
            onStartSimulation
        )
        return popupLogic.getContentView()
    }

    // --- 유틸리티 메서드 ---

    private fun startDefenseGameImmediately() {
        val charType = characterDataManager.equippedDefenseCharacter
        val weaponType = characterDataManager.equippedDefenseWeapon
        val weaponGrade = characterDataManager.equippedDefenseWeaponGrade
        onStartDefenseGame(charType, weaponType, weaponGrade)
    }

    private fun showSettingsDialog() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(context).apply {
            text = "Settings"
            textSize = 24f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)

        val logoutBtn = Button(context).apply {
            text = "로그아웃 (Logout)"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
        layout.addView(logoutBtn)

        val dialog = AlertDialog.Builder(context)
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