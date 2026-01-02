package com.donghwa.gameVerse.defensegame

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.donghwa.gameVerse.character.CharacterDataManager

class DefenseCharacterPopup(
    private val context: Context,
    private val uid: String,
    private val onStart: (DefenseCharacterType, WeaponType, WeaponGrade) -> Unit
) {
    private val dataManager = CharacterDataManager()

    // 현재 선택된 장착 정보
    private var equippedCharacter: DefenseCharacterType? = null
    private var equippedWeapon: WeaponType? = null
    private var equippedWeaponGrade: WeaponGrade = WeaponGrade.NORMAL

    // 현재 선택된 슬롯 (무엇을 변경하려고 하는지)
    private var currentSelectedSlot: SlotType = SlotType.UNIT

    // 슬롯 타입 정의
    enum class SlotType(val title: String) {
        UNIT("캐릭터"),
        WEAPON("무기"),
        HELMET("투구"),
        ARMOR("갑옷"),
        SHOES("신발"),
        NECKLACE("목걸이"),
        RING("반지")
    }

    // UI 요소
    private lateinit var inventoryGridContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var dialog: AlertDialog

    // 슬롯 버튼 참조 (업데이트용)
    private var slotButtons = mutableMapOf<SlotType, Button>()

    fun show() {
        Toast.makeText(context, "인벤토리 정보를 불러오는 중...", Toast.LENGTH_SHORT).show()
        dataManager.loadDefenseInventory(uid) {
            showInventoryDialog()
        }
    }

    private fun showInventoryDialog() {
        // 데이터 로드
        equippedCharacter = dataManager.equippedDefenseCharacter
        equippedWeapon = dataManager.equippedDefenseWeapon
        equippedWeaponGrade = dataManager.equippedDefenseWeaponGrade

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        // 1. 상단 타이틀
        val titleBar = TextView(context).apply {
            text = " 격납고 (Hangar) "
            textSize = 22f
            setTextColor(Color.CYAN)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
            typeface = Typeface.DEFAULT_BOLD
        }
        mainLayout.addView(titleBar)

        // 2. 캐릭터 중심 장착 슬롯 UI (Paper Doll UI)
        val equipSection = createEquipSection()
        mainLayout.addView(equipSection)

        // 3. 인벤토리 영역 헤더
        val invHeader = TextView(context).apply {
            text = "▼ 보유 아이템 목록 (Inventory)"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(40, 20, 0, 20)
            setBackgroundColor(Color.parseColor("#252525"))
        }
        mainLayout.addView(invHeader)

        // 4. 인벤토리 그리드 (ScrollView)
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setBackgroundColor(Color.parseColor("#121212"))
        }

        inventoryGridContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        scrollView.addView(inventoryGridContainer)
        mainLayout.addView(scrollView)

        // 5. 하단 상태 및 버튼
        val bottomLayout = createBottomSection()
        mainLayout.addView(bottomLayout)

        // 초기 인벤토리 로드 (캐릭터 탭)
        refreshInventoryGrid()

        // 다이얼로그 생성
        dialog = AlertDialog.Builder(context)
            .setView(mainLayout)
            .create()

        dialog.show()
        // 화면 크기의 95% 사용
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.9).toInt()
        )
    }

    // --- [섹션 2] 캐릭터 중심 장착 UI 생성 ---
    private fun createEquipSection(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(20, 40, 20, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // 왼쪽 컬럼 (투구, 무기, 반지)
        val leftCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        leftCol.addView(createSlotButton(SlotType.HELMET))
        leftCol.addView(createSlotButton(SlotType.WEAPON))
        leftCol.addView(createSlotButton(SlotType.RING))

        // 중앙 컬럼 (캐릭터 - 크게)
        val centerCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 0, 30, 0)
        }
        centerCol.addView(createSlotButton(SlotType.UNIT, isLarge = true))

        // 오른쪽 컬럼 (목걸이, 갑옷, 신발)
        val rightCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        rightCol.addView(createSlotButton(SlotType.NECKLACE))
        rightCol.addView(createSlotButton(SlotType.ARMOR))
        rightCol.addView(createSlotButton(SlotType.SHOES))

        container.addView(leftCol)
        container.addView(centerCol)
        container.addView(rightCol)

        return container
    }

    // --- [섹션 5] 하단 버튼 생성 ---
    private fun createBottomSection(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(30, 20, 30, 30)
        }

        statusText = TextView(context).apply {
            text = "설정 저장 대기 중..."
            setTextColor(Color.YELLOW)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        updateStatusText()
        container.addView(statusText)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        val saveBtn = Button(context).apply {
            text = "저장 (Save)"
            setBackgroundColor(Color.DKGRAY)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 10
            }
            setOnClickListener {
                saveData()
                Toast.makeText(context, "장비 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        val startBtn = Button(context).apply {
            text = "출격 (Deploy)"
            setBackgroundColor(Color.parseColor("#76FF03"))
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 10
            }
            setOnClickListener {
                saveData()
                dialog.dismiss()
                if (equippedCharacter != null && equippedWeapon != null) {
                    onStart(equippedCharacter!!, equippedWeapon!!, equippedWeaponGrade)
                }
            }
        }

        btnRow.addView(saveBtn)
        btnRow.addView(startBtn)
        container.addView(btnRow)
        return container
    }

    // --- 슬롯 버튼 생성 및 관리 ---
    private fun createSlotButton(type: SlotType, isLarge: Boolean = false): View {
        val size = if (isLarge) 240 else 160 // 버튼 크기

        val frame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 10, 0, 10)
            }
        }

        val btn = Button(context).apply {
            background = null // 기본 배경 제거하고 색상 지정
            setBackgroundColor(Color.parseColor("#333333")) // 기본 색상
            text = "${type.title}\n(비어있음)"
            setTextColor(Color.LTGRAY)
            textSize = if(isLarge) 16f else 11f
            gravity = Gravity.CENTER
            setOnClickListener {
                selectSlot(type)
            }
        }

        slotButtons[type] = btn
        frame.addView(btn, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // 초기 텍스트 설정
        updateSlotButtonUI(type)

        return frame
    }

    private fun selectSlot(type: SlotType) {
        // 이전 슬롯 색상 복구
        slotButtons[currentSelectedSlot]?.setBackgroundColor(Color.parseColor("#333333"))

        currentSelectedSlot = type

        // 선택된 슬롯 강조 (노란 테두리 느낌의 밝은 배경)
        slotButtons[currentSelectedSlot]?.setBackgroundColor(Color.parseColor("#555555")) // 선택됨

        Toast.makeText(context, "${type.title} 슬롯 선택됨", Toast.LENGTH_SHORT).show()
        refreshInventoryGrid()
    }

    private fun updateSlotButtonUI(type: SlotType) {
        val btn = slotButtons[type] ?: return
        when (type) {
            SlotType.UNIT -> {
                btn.text = "${type.title}\n${equippedCharacter?.name ?: "없음"}"
                btn.setTextColor(Color.CYAN)
            }
            SlotType.WEAPON -> {
                val gradeMark = equippedWeaponGrade.name.first()
                btn.text = "${type.title}\n${equippedWeapon?.name ?: "없음"}\n[$gradeMark]"
                btn.setTextColor(equippedWeaponGrade.getColor())
            }
            else -> {
                // 아직 구현되지 않은 장비들
                btn.text = "${type.title}\n(미구현)"
                btn.setTextColor(Color.GRAY)
            }
        }
    }

    // --- 인벤토리 그리드 생성 로직 (4열) ---
    private fun refreshInventoryGrid() {
        inventoryGridContainer.removeAllViews()

        // 화면 너비 기반으로 아이템 크기 계산 (4열)
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels * 0.9 // 다이얼로그 너비
        val itemSize = ((screenWidth - 100) / 4).toInt() // 패딩 고려하여 4등분

        when (currentSelectedSlot) {
            SlotType.UNIT -> loadUnitGrid(itemSize)
            SlotType.WEAPON -> loadWeaponGrid(itemSize)
            else -> loadEmptyGrid("이 슬롯에 장착할 수 있는 아이템이 없습니다.\n(업데이트 예정)")
        }
    }

    private fun loadUnitGrid(itemSize: Int) {
        val items = DefenseCharacterType.values()
        var currentRow: LinearLayout? = null

        for ((index, type) in items.withIndex()) {
            if (index % 4 == 0) {
                currentRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 4f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = 10
                    }
                }
                inventoryGridContainer.addView(currentRow)
            }

            val isOwned = dataManager.ownedDefenseCharacters.contains(type)
            val isEquipped = equippedCharacter == type

            val itemView = createGridItemView(itemSize, type.name, "UNIT", Color.WHITE, isOwned, isEquipped) {
                if (isOwned) {
                    equippedCharacter = type
                    updateSlotButtonUI(SlotType.UNIT)
                    updateStatusText()
                    refreshInventoryGrid() // 갱신해서 장착 표시 이동
                } else {
                    Toast.makeText(context, "미보유 캐릭터입니다.", Toast.LENGTH_SHORT).show()
                }
            }

            currentRow?.addView(itemView)
        }

        // 마지막 줄 빈 공간 채우기 (레이아웃 깨짐 방지)
        fillEmptySlotsInRow(currentRow, items.size)
    }

    private fun loadWeaponGrid(itemSize: Int) {
        // 보유한 무기 + 등급 조합을 모두 표시
        // 무기 타입별로 돌면서 등급별 보유량을 체크
        val weaponList = mutableListOf<Triple<WeaponType, WeaponGrade, Int>>()

        for (type in WeaponType.values()) {
            for (grade in WeaponGrade.values()) {
                val count = dataManager.getWeaponCount(type, grade)
                if (count > 0) {
                    weaponList.add(Triple(type, grade, count))
                }
            }
        }

        if (weaponList.isEmpty()) {
            loadEmptyGrid("보유한 무기가 없습니다.")
            return
        }

        var currentRow: LinearLayout? = null

        for ((index, item) in weaponList.withIndex()) {
            if (index % 4 == 0) {
                currentRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 4f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = 10
                    }
                }
                inventoryGridContainer.addView(currentRow)
            }

            val (type, grade, count) = item
            val isEquipped = (equippedWeapon == type && equippedWeaponGrade == grade)

            val displayName = "${type.name}\n${grade.name}\nx$count"
            val color = grade.getColor()

            val itemView = createGridItemView(itemSize, displayName, "WEAPON", color, true, isEquipped) {
                equippedWeapon = type
                equippedWeaponGrade = grade
                updateSlotButtonUI(SlotType.WEAPON)
                updateStatusText()
                refreshInventoryGrid()
            }

            // 합성 기능 추가 (롱클릭 시)
            itemView.setOnLongClickListener {
                tryUpgrade(type, grade)
                true
            }

            currentRow?.addView(itemView)
        }

        fillEmptySlotsInRow(currentRow, weaponList.size)

        // 안내 문구 추가
        val hintText = TextView(context).apply {
            text = "Tip: 같은 등급 무기가 모이면 길게 눌러 합성하세요!"
            setTextColor(Color.GRAY)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        inventoryGridContainer.addView(hintText)
    }

    private fun fillEmptySlotsInRow(row: LinearLayout?, itemCount: Int) {
        val remainder = itemCount % 4
        if (remainder != 0 && row != null) {
            val emptySlotsNeeded = 4 - remainder
            for (i in 0 until emptySlotsNeeded) {
                val spacer = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 10, 1f).apply {
                        marginStart = 5; marginEnd = 5
                    }
                }
                row.addView(spacer)
            }
        }
    }

    private fun loadEmptyGrid(msg: String) {
        val tv = TextView(context).apply {
            text = msg
            setTextColor(Color.DKGRAY)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300)
        }
        inventoryGridContainer.addView(tv)
    }

    private fun createGridItemView(
        size: Int,
        mainText: String,
        subText: String,
        textColor: Int,
        isOwned: Boolean,
        isEquipped: Boolean,
        onClick: () -> Unit
    ): View {
        val container = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, size, 1f).apply {
                marginStart = 5
                marginEnd = 5
            }

            // 배경 설정
            if (isEquipped) {
                setBackgroundColor(Color.parseColor("#1B5E20")) // 장착중 (녹색)
            } else if (isOwned) {
                setBackgroundColor(Color.parseColor("#333333")) // 보유중
            } else {
                setBackgroundColor(Color.parseColor("#121212")) // 미보유
            }

            setOnClickListener { onClick() }
        }

        // 내용물
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val title = TextView(context).apply {
            text = if(isOwned) mainText else "🔒"
            setTextColor(if(isOwned) textColor else Color.DKGRAY)
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        layout.addView(title)
        container.addView(layout)

        // 장착 표시 뱃지
        if (isEquipped) {
            val badge = TextView(context).apply {
                text = "E"
                textSize = 10f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.RED)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(40, 40).apply {
                    gravity = Gravity.TOP or Gravity.END
                }
            }
            container.addView(badge)
        }

        return container
    }

    private fun tryUpgrade(type: WeaponType, grade: WeaponGrade) {
        val cost = grade.getUpgradeCost()
        if (cost == 0) {
            Toast.makeText(context, "최고 등급입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val count = dataManager.getWeaponCount(type, grade)
        if (count >= cost) {
            AlertDialog.Builder(context)
                .setTitle("합성 확인")
                .setMessage("${grade.name} 무기 ${cost}개를 소모하여 승급하시겠습니까?")
                .setPositiveButton("합성") { _, _ ->
                    dataManager.upgradeWeapon(uid, type, grade) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (success) refreshInventoryGrid()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            Toast.makeText(context, "합성 재료가 부족합니다. ($count/$cost)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveData() {
        if (equippedCharacter != null && equippedWeapon != null) {
            dataManager.saveDefenseLoadout(uid, equippedCharacter!!, equippedWeapon!!, equippedWeaponGrade)
        }
    }

    private fun updateStatusText() {
        val charName = equippedCharacter?.name ?: "-"
        val weaponName = equippedWeapon?.name ?: "-"
        val gradeName = equippedWeaponGrade.name
        statusText.text = "현재 상태: $charName 착용 중 / 무기: $weaponName [$gradeName]"
    }
}