package com.donghwa.gameVerse

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.donghwa.gameVerse.character.CharacterDataManager
import com.donghwa.gameVerse.character.GachaBoxType
import com.donghwa.gameVerse.defensegame.DefenseCharacterPopup
import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.Difficulty
import com.donghwa.gameVerse.defensegame.ResourceManager
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.defensegame.WeaponGrade
import kotlin.math.abs

class HomeView(
    context: Context,
    private val uid: String,
    private val userName: String,
    private val highScore: Int,
    private val runnerHighScore: Int,
    private val defenseHighScore: Int,
    private val defenseMaxStage: Int,
    private val level: Int,
    private val currentXp: Int,
    private val leaderboard: List<String>,
    private val runnerLeaderboard: List<String>,
    private val defenseLeaderboard: List<String>,
    private val characterDataManager: CharacterDataManager,
    private val onStartBrickGame: () -> Unit,
    private val onStartRunnerGame: () -> Unit,
    private val onStartSimulation: () -> Unit,
    private val onStartDefenseGame: (DefenseCharacterType, WeaponType, WeaponGrade, Int, Difficulty) -> Unit,
    private val onLogout: () -> Unit
) : RelativeLayout(context) {

    // ... (기존 변수들) ...
    private lateinit var contentContainer: FrameLayout
    private lateinit var tabContainer: LinearLayout
    private lateinit var btnTabGacha: LinearLayout
    private lateinit var btnTabHangar: LinearLayout
    private lateinit var btnTabOperation: LinearLayout
    private lateinit var btnTabMiniGames: LinearLayout
    private enum class Tab { GACHA, HANGAR, OPERATION, MINI_GAMES }
    private var currentTab = Tab.OPERATION
    private lateinit var topBarCurrencyText: TextView
    private var selectedStage = 1
    private lateinit var startOperationBtn: Button
    private lateinit var stageContainerLayout: LinearLayout
    private lateinit var operationScrollView: ScrollView

    init {
        ResourceManager.init(context)
        setupUI()
        switchTab(Tab.OPERATION)
    }

    // ... (setupUI, createTopInfoBar, createBottomNavBar 등 기존 코드 유지) ...
    private fun setupUI() {
        setBackgroundColor(Color.parseColor("#121212"))
        val topBar = createTopInfoBar()
        topBar.id = View.generateViewId()
        val topParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        topParams.addRule(ALIGN_PARENT_TOP)
        addView(topBar, topParams)

        val bottomNav = createBottomNavBar()
        bottomNav.id = View.generateViewId()
        val bottomParams = LayoutParams(LayoutParams.MATCH_PARENT, 180)
        bottomParams.addRule(ALIGN_PARENT_BOTTOM)
        addView(bottomNav, bottomParams)

        contentContainer = FrameLayout(context).apply { setBackgroundColor(Color.TRANSPARENT) }
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

        topBarCurrencyText = TextView(context).apply {
            text = "💰 ${characterDataManager.userDong}  🥈 ${characterDataManager.userSilver}"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 80, 0)
        }
        val currencyParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        currencyParams.addRule(RelativeLayout.LEFT_OF, View.generateViewId())
        currencyParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        currencyParams.rightMargin = 100
        currencyParams.addRule(RelativeLayout.CENTER_VERTICAL)
        row1.addView(topBarCurrencyText, currencyParams)

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

        val xpLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 15, 0, 15)
        }
        val requiredXp = level * 100
        val xpBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = requiredXp
            progress = currentXp
            progressTintList = ColorStateList.valueOf(Color.CYAN)
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
        return container
    }

    private fun updateTopBarCurrency() {
        topBarCurrencyText.text = "💰 ${characterDataManager.userDong}  🥈 ${characterDataManager.userSilver}"
    }

    private fun createBottomNavBar(): View {
        val navContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151515"))
            weightSum = 4f
        }
        btnTabGacha = createNavButton("뽑기", "🎁", Tab.GACHA)
        btnTabHangar = createNavButton("격납고", "🛠️", Tab.HANGAR)
        btnTabOperation = createNavButton("작전", "🚀", Tab.OPERATION)
        btnTabMiniGames = createNavButton("미니게임", "🕹️", Tab.MINI_GAMES)
        navContainer.addView(btnTabGacha)
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
            setOnClickListener { switchTab(tab) }
        }
        val iconTv = TextView(context).apply { text = icon; textSize = 24f; gravity = Gravity.CENTER }
        btnLayout.addView(iconTv)
        val titleTv = TextView(context).apply { text = title; textSize = 12f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }
        btnLayout.addView(titleTv)
        btnLayout.tag = titleTv
        return btnLayout
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        contentContainer.removeAllViews()
        updateTabUI(btnTabGacha, tab == Tab.GACHA)
        updateTabUI(btnTabHangar, tab == Tab.HANGAR)
        updateTabUI(btnTabOperation, tab == Tab.OPERATION)
        updateTabUI(btnTabMiniGames, tab == Tab.MINI_GAMES)
        val contentView = when (tab) {
            Tab.GACHA -> createGachaView()
            Tab.HANGAR -> createHangarView()
            Tab.OPERATION -> createOperationView()
            Tab.MINI_GAMES -> createMiniGamesView()
        }
        contentContainer.addView(contentView)
        updateTopBarCurrency()
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

    // ... (GachaView, HangarView 유지) ...
    private fun createGachaView(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(30, 30, 30, 30)
        }
        val title = TextView(context).apply { text = "무기 보급소 (Armory)"; textSize = 28f; setTextColor(Color.CYAN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, 50) }
        layout.addView(title)
        val normalBox = createGachaBoxUI("일반 보급상자 (Normal)", "📦", "100 은 (Silver)", "Normal 65% | Magic 25% | Rare 7% | Unique 2.5% | Legend 0.5%", Color.parseColor("#757575")) { performGacha(GachaBoxType.NORMAL, 100) }
        layout.addView(normalBox)
        layout.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(1, 50) })
        val specialBox = createGachaBoxUI("특수 보급상자 (Special)", "🎁", "350 은 (Silver)", "Normal 10% | Magic 45% | Rare 30% | Unique 10% | Legend 5%", Color.parseColor("#FFD700")) { performGacha(GachaBoxType.SPECIAL, 350) }
        layout.addView(specialBox)
        return layout
    }

    private fun createGachaBoxUI(title: String, emoji: String, costText: String, desc: String, btnColor: Int, onClick: () -> Unit): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(30, 30, 30, 30)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.parseColor("#1E1E1E")); cornerRadius = 20f; setStroke(2, Color.DKGRAY) }
        }
        val headerLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val emojiView = TextView(context).apply { text = emoji; textSize = 40f }
        headerLayout.addView(emojiView)
        val titleView = TextView(context).apply { text = "  $title"; textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD }
        headerLayout.addView(titleView)
        container.addView(headerLayout)
        val descView = TextView(context).apply { text = desc; textSize = 12f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(0, 20, 0, 20) }
        container.addView(descView)
        val btn = Button(context).apply { text = costText; setBackgroundColor(btnColor); setTextColor(Color.BLACK); typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(500, 120); setOnClickListener { onClick() } }
        container.addView(btn)
        return container
    }

    private fun performGacha(type: GachaBoxType, cost: Int) {
        characterDataManager.drawGachaWeapon(uid, type) { success, msg, weaponType, weaponGrade ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            updateTopBarCurrency()
            if (success && weaponType != null && weaponGrade != null) showGachaResult(weaponType, weaponGrade)
        }
    }

    private fun showGachaResult(type: WeaponType, grade: WeaponGrade) {
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#212121"))
        }
        val resultTitle = TextView(context).apply { text = "획득 성공!"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        dialogView.addView(resultTitle)
        val itemIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300).apply { topMargin = 30; bottomMargin = 30 }
            val bitmap = ResourceManager.getWeaponBitmap(type, grade)
            if (bitmap != null) setImageBitmap(bitmap) else setBackgroundColor(grade.getColor())
        }
        dialogView.addView(itemIcon)
        val itemName = TextView(context).apply { text = "[${grade.name}] ${type.name}"; textSize = 20f; setTextColor(grade.getColor()); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }
        dialogView.addView(itemName)
        val closeBtn = Button(context).apply { text = "확인"; layoutParams = LinearLayout.LayoutParams(300, 120).apply { topMargin = 50 }; setOnClickListener { (tag as? AlertDialog)?.dismiss() } }
        dialogView.addView(closeBtn)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        closeBtn.tag = dialog
        dialog.show()
    }

    private fun createHangarView(): View {
        val popupLogic = DefenseCharacterPopup(context, uid, characterDataManager) { charType, weaponType, grade ->
            onStartDefenseGame(charType, weaponType, grade, 0, Difficulty.NORMAL)
        }
        return popupLogic.getContentView()
    }

    private fun createOperationView(): View {
        val layout = RelativeLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val header = LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 30)
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply { addRule(RelativeLayout.ALIGN_PARENT_TOP) }
        }
        val logoText = TextView(context).apply { text = "OPERATION"; textSize = 40f; setTextColor(Color.CYAN); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); gravity = Gravity.CENTER }
        header.addView(logoText)
        val subText = TextView(context).apply { text = "Select Operation Area"; textSize = 16f; setTextColor(Color.GRAY); gravity = Gravity.CENTER }
        header.addView(subText)
        layout.addView(header)

        startOperationBtn = Button(context).apply {
            id = View.generateViewId()
            textSize = 22f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            val gd = GradientDrawable().apply { setColor(Color.parseColor("#76FF03")); cornerRadius = 20f }
            background = gd
            text = "MISSION SETUP [ STAGE 1 ]"
            setOnClickListener {
                if (selectedStage <= defenseMaxStage) showDifficultySelectionDialog(selectedStage) else Toast.makeText(context, "잠금 해제되지 않은 스테이지입니다.", Toast.LENGTH_SHORT).show()
            }
        }
        val btnParams = RelativeLayout.LayoutParams(600, 150).apply { addRule(RelativeLayout.ALIGN_PARENT_BOTTOM); addRule(RelativeLayout.CENTER_HORIZONTAL); bottomMargin = 50 }
        layout.addView(startOperationBtn, btnParams)

        operationScrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT).apply { addRule(RelativeLayout.BELOW, header.id); addRule(RelativeLayout.ABOVE, startOperationBtn.id) }
        }
        stageContainerLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; clipToPadding = false }

        operationScrollView.post {
            val scrollHeight = operationScrollView.height
            val itemHeight = (scrollHeight * 0.25).toInt()
            val displayMetrics = resources.displayMetrics
            val itemWidth = (displayMetrics.widthPixels * 0.6).toInt()
            val padding = (scrollHeight - itemHeight) / 2
            stageContainerLayout.setPadding(0, padding, 0, padding)
            stageContainerLayout.removeAllViews()

            for (i in 1..5) {
                val isLocked = i > defenseMaxStage
                val stageCard = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(itemWidth, itemHeight).apply { bottomMargin = 40 }
                    val bg = View(context).apply {
                        val color = if (isLocked) Color.parseColor("#263238") else Color.parseColor("#37474F")
                        background = GradientDrawable().apply { setColor(color); cornerRadius = 40f; setStroke(5, if (isLocked) Color.GRAY else Color.CYAN) }
                    }
                    addView(bg)
                    val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
                    val title = TextView(context).apply { text = "STAGE $i"; textSize = 30f; setTextColor(if (isLocked) Color.GRAY else Color.WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER }
                    content.addView(title)
                    if (isLocked) {
                        val lockText = TextView(context).apply { text = "🔒 Locked"; textSize = 20f; setTextColor(Color.GRAY); gravity = Gravity.CENTER }
                        content.addView(lockText)
                    } else {
                        val infoText = TextView(context).apply { text = "Recommended Lv.${i * 5}"; textSize = 14f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
                        content.addView(infoText)
                    }
                    addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

                    // [신규] 클리어 뱃지 표시
                    if (!isLocked) {
                        val normalClearKey = "${i}_${Difficulty.NORMAL.name}"
                        val hardClearKey = "${i}_${Difficulty.HARD.name}"

                        // DB에 저장된 최대 웨이브가 10(클리어 기준) 이상이면 클리어로 간주
                        val normalCleared = (characterDataManager.maxWaveRecords[normalClearKey] ?: 0) >= 10
                        val hardCleared = (characterDataManager.maxWaveRecords[hardClearKey] ?: 0) >= 10

                        if (normalCleared || hardCleared) {
                            val badge = TextView(context).apply {
                                text = if (hardCleared) "MASTER" else "CLEAR"
                                textSize = 12f
                                setTextColor(Color.BLACK)
                                typeface = Typeface.DEFAULT_BOLD
                                gravity = Gravity.CENTER
                                setPadding(15, 5, 15, 5)
                                background = GradientDrawable().apply {
                                    // Hard 클리어 시 골드, Normal 클리어 시 실버
                                    setColor(if (hardCleared) Color.parseColor("#FFD700") else Color.parseColor("#C0C0C0"))
                                    cornerRadius = 10f
                                }
                            }
                            val badgeParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                                gravity = Gravity.TOP or Gravity.END
                                topMargin = 20
                                rightMargin = 20
                            }
                            addView(badge, badgeParams)
                        }
                    }

                    // Reward Info (미수령 보상이 있으면 반짝임)
                    if (!isLocked) {
                        val rewardInfo = TextView(context).apply {
                            text = "🎁 Reward Info"
                            textSize = 12f
                            setTextColor(Color.YELLOW)
                            gravity = Gravity.CENTER
                            setPadding(20, 10, 20, 10)
                            background = GradientDrawable().apply { setColor(Color.parseColor("#424242")); cornerRadius = 20f; setStroke(2, Color.YELLOW) }
                            setOnClickListener { showRewardInfoPopup(i) }
                        }

                        if (characterDataManager.hasUnclaimedRewards(i)) {
                            val anim = ObjectAnimator.ofFloat(rewardInfo, "alpha", 1f, 0.3f, 1f)
                            anim.duration = 1000
                            anim.repeatCount = ValueAnimator.INFINITE
                            anim.repeatMode = ValueAnimator.REVERSE
                            anim.start()
                        }

                        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; bottomMargin = 20 }
                        addView(rewardInfo, params)
                    }
                    tag = i
                }
                stageContainerLayout.addView(stageCard)
            }

            // [수정] 마지막 도전 스테이지(defenseMaxStage)가 중앙에 오도록 스크롤 설정
            // 스테이지가 1부터 시작하므로 index는 defenseMaxStage - 1
            // 5 스테이지까지 있으므로 범위 제한
            val targetIndex = (defenseMaxStage - 1).coerceIn(0, 4)
            // 아이템 하나 높이 + 마진
            val itemFullHeight = itemHeight + 40 // bottomMargin 40
            // 해당 아이템의 상단 위치 = index * itemFullHeight
            val targetScrollY = targetIndex * itemFullHeight

            operationScrollView.scrollTo(0, targetScrollY)
            selectedStage = targetIndex + 1
            updateStartButtonState()
            updateStageScale()
        }
        operationScrollView.addView(stageContainerLayout)
        layout.addView(operationScrollView)
        operationScrollView.viewTreeObserver.addOnScrollChangedListener { updateStageScale() }
        operationScrollView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                val scrollY = operationScrollView.scrollY
                val centerY = scrollY + operationScrollView.height / 2
                var minInfo: Pair<View?, Int> = Pair(null, Int.MAX_VALUE)
                var targetIndex = 0
                for (i in 0 until stageContainerLayout.childCount) {
                    val child = stageContainerLayout.getChildAt(i)
                    val childCenter = child.top + child.height / 2
                    val dist = abs(centerY - childCenter)
                    if (dist < minInfo.second) { minInfo = Pair(child, dist); targetIndex = i }
                }
                minInfo.first?.let { target ->
                    val padding = stageContainerLayout.paddingTop
                    val targetScrollY = target.top - padding
                    operationScrollView.smoothScrollTo(0, targetScrollY)
                    selectedStage = targetIndex + 1
                    updateStartButtonState()
                }
                return@setOnTouchListener true
            }
            false
        }
        return layout
    }

    // [수정] 보상 수령 팝업 (이미 클리어한 기록이 있다면 언제든 수령 가능)
    private fun showRewardInfoPopup(stage: Int) {
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#212121"))
        }
        val title = TextView(context).apply { text = "STAGE $stage Rewards"; textSize = 24f; setTextColor(Color.CYAN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, 30) }
        dialogView.addView(title)

        val difficulties = Difficulty.values()
        val tiers = listOf(3, 6, 10)

        for (diff in difficulties) {
            val diffTitle = TextView(context).apply { text = "[ ${diff.label} Difficulty ]"; textSize = 18f; setTextColor(Color.WHITE); setPadding(0, 20, 0, 10) }
            dialogView.addView(diffTitle)

            // DB에 저장된 해당 스테이지-난이도의 최고 기록 가져오기
            val maxWave = characterDataManager.maxWaveRecords["${stage}_${diff.name}"] ?: 0

            for (tier in tiers) {
                val rewardAmount = stage * 50 * diff.rewardMultiplier * (if(tier==10) 2 else 1)
                val isClaimed = characterDataManager.isRewardTierClaimed(stage, diff, tier)

                // [수정] maxWave가 tier 이상이면 (과거에 클리어했다면) 수령 자격 있음
                val canClaim = maxWave >= tier && !isClaimed

                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 10, 0, 10)
                    gravity = Gravity.CENTER_VERTICAL
                }

                val infoText = TextView(context).apply {
                    text = "Wave $tier Clear : 🥈 $rewardAmount"
                    textSize = 14f
                    setTextColor(if(isClaimed) Color.GRAY else Color.LTGRAY)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(infoText)

                val actionBtn = Button(context).apply {
                    text = if(isClaimed) "완료" else if(canClaim) "수령" else "잠김"
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(180, 100)
                    isEnabled = canClaim

                    if (isClaimed) {
                        setBackgroundColor(Color.DKGRAY)
                        setTextColor(Color.GRAY)
                    } else if (canClaim) {
                        setBackgroundColor(Color.parseColor("#76FF03")) // 녹색
                        setTextColor(Color.BLACK)
                        setOnClickListener {
                            characterDataManager.claimRewardTier(uid, stage, diff, tier, rewardAmount) {
                                text = "완료"
                                isEnabled = false
                                setBackgroundColor(Color.DKGRAY)
                                setTextColor(Color.GRAY)
                                infoText.setTextColor(Color.GRAY)
                                updateTopBarCurrency()
                                Toast.makeText(context, "보상 수령 완료!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        setBackgroundColor(Color.parseColor("#37474F"))
                        setTextColor(Color.GRAY)
                    }
                }
                row.addView(actionBtn)
                dialogView.addView(row)
            }
        }

        val closeBtn = Button(context).apply { text = "Close"; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 50 }; setOnClickListener { (tag as? AlertDialog)?.dismiss() } }
        dialogView.addView(closeBtn)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        closeBtn.tag = dialog
        dialog.show()
    }

    private fun showDifficultySelectionDialog(stage: Int) {
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#263238"))
            gravity = Gravity.CENTER
        }
        val title = TextView(context).apply { text = "Select Difficulty"; textSize = 28f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, 50) }
        dialogView.addView(title)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val difficulties = Difficulty.values()
        for (diff in difficulties) {
            val btn = Button(context).apply {
                text = "${diff.label} (Reward x${diff.rewardMultiplier})"
                textSize = 18f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(600, 150).apply { bottomMargin = 30 }
                val color = when(diff) {
                    Difficulty.NORMAL -> Color.parseColor("#64B5F6")
                    Difficulty.HARD -> Color.parseColor("#E57373")
                }
                background = GradientDrawable().apply { setColor(color); cornerRadius = 20f }
                setOnClickListener { dialog.dismiss(); startDefenseGameWithStage(stage, diff) }
            }
            dialogView.addView(btn)
        }
        dialog.show()
    }

    private fun updateStageScale() {
        val scrollY = operationScrollView.scrollY
        val centerY = scrollY + operationScrollView.height / 2
        for (i in 0 until stageContainerLayout.childCount) {
            val child = stageContainerLayout.getChildAt(i)
            val childCenter = child.top + child.height / 2
            val dist = abs(centerY - childCenter)
            val maxDist = child.height.toFloat() * 1.5f
            val scale = 1f + (0.2f * (1f - (dist / maxDist).coerceIn(0f, 1f)))
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = 0.5f + (0.5f * (1f - (dist / maxDist).coerceIn(0f, 1f)))
        }
    }

    private fun updateStartButtonState() {
        if (selectedStage <= defenseMaxStage) {
            startOperationBtn.text = "MISSION SETUP [ STAGE $selectedStage ]"
            val gd = startOperationBtn.background as GradientDrawable
            gd.setColor(Color.parseColor("#76FF03"))
            startOperationBtn.isEnabled = true
        } else {
            startOperationBtn.text = "LOCKED [ STAGE $selectedStage ]"
            val gd = startOperationBtn.background as GradientDrawable
            gd.setColor(Color.GRAY)
            startOperationBtn.isEnabled = true
        }
    }

    private fun createMiniGamesView(): View {
        val popupLogic = MiniGamePopup(context, onStartBrickGame, onStartRunnerGame, onStartSimulation)
        return popupLogic.getContentView()
    }

    private fun startDefenseGameWithStage(stage: Int, difficulty: Difficulty = Difficulty.NORMAL) {
        val charType = characterDataManager.equippedDefenseCharacter
        val weaponType = characterDataManager.equippedDefenseWeapon
        val weaponGrade = characterDataManager.equippedDefenseWeaponGrade
        onStartDefenseGame(charType, weaponType, weaponGrade, stage, difficulty)
    }

    private fun showSettingsDialog() {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50); gravity = Gravity.CENTER_HORIZONTAL }
        val title = TextView(context).apply { text = "Settings"; textSize = 24f; setTextColor(Color.BLACK); setPadding(0, 0, 0, 30) }
        layout.addView(title)
        val logoutBtn = Button(context).apply { text = "로그아웃 (Logout)"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE) }
        layout.addView(logoutBtn)
        val dialog = AlertDialog.Builder(context).setView(layout).setNegativeButton("닫기", null).create()
        logoutBtn.setOnClickListener { dialog.dismiss(); onLogout() }
        dialog.show()
    }
}