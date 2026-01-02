package com.donghwa.gameVerse.defensegame

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.donghwa.gameVerse.character.CharacterDataManager

class DefenseCharacterPopup(
    private val context: Context,
    private val uid: String,
    private val dataManager: CharacterDataManager,
    private val onStart: (DefenseCharacterType, WeaponType, WeaponGrade) -> Unit
) {
    private var equippedCharacter: DefenseCharacterType? = null
    private var equippedWeapon: WeaponType? = null
    private var equippedWeaponGrade: WeaponGrade = WeaponGrade.NORMAL

    private var currentSelectedSlot: SlotType = SlotType.UNIT

    enum class SlotType(val title: String) {
        UNIT("캐릭터"),
        WEAPON("무기"),
        HELMET("투구"),
        ARMOR("갑옷"),
        SHOES("신발"),
        NECKLACE("목걸이"),
        RING("반지")
    }

    private lateinit var inventoryGridContainer: LinearLayout
    private lateinit var statusText: TextView
    private var dialog: AlertDialog? = null // Nullable로 변경 (View 모드 지원)

    private data class SlotViews(val container: View, val icon: ImageView, val label: TextView)
    private var slotViewsMap = mutableMapOf<SlotType, SlotViews>()

    // [신규] 팝업 없이 View만 리턴하는 메서드 (HomeView 탭 임베딩용)
    fun getContentView(): View {
        ResourceManager.init(context)
        return createMainLayout(isEmbedded = true)
    }

    // 기존 팝업 표시 메서드
    fun show() {
        ResourceManager.init(context)
        val view = createMainLayout(isEmbedded = false)

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog?.show()
        dialog?.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.9).toInt()
        )
    }

    private fun createMainLayout(isEmbedded: Boolean): View {
        equippedCharacter = dataManager.equippedDefenseCharacter
        equippedWeapon = dataManager.equippedDefenseWeapon
        equippedWeaponGrade = dataManager.equippedDefenseWeaponGrade

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 임베딩 모드가 아닐 때만 상단 타이틀 바 표시 (HomeView에서는 탭이 타이틀 역할)
        if (!isEmbedded) {
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
        }

        val equipSection = createEquipSection()
        mainLayout.addView(equipSection)

        val invHeader = TextView(context).apply {
            text = "▼ 보유 아이템 목록 (Inventory)"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(40, 20, 0, 20)
            setBackgroundColor(Color.parseColor("#252525"))
        }
        mainLayout.addView(invHeader)

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

        val bottomLayout = createBottomSection(isEmbedded)
        mainLayout.addView(bottomLayout)

        refreshInventoryGrid()

        return mainLayout
    }

    private fun createEquipSection(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(20, 40, 20, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val leftCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        leftCol.addView(createSlotButton(SlotType.HELMET))
        leftCol.addView(createSlotButton(SlotType.WEAPON))
        leftCol.addView(createSlotButton(SlotType.RING))

        val centerCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 0, 30, 0)
        }
        centerCol.addView(createSlotButton(SlotType.UNIT, isLarge = true))

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

    private fun createBottomSection(isEmbedded: Boolean): View {
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
                dialog?.dismiss() // 팝업일 때만 닫기
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
                dialog?.dismiss()
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

    private fun createSlotButton(type: SlotType, isLarge: Boolean = false): View {
        val size = if (isLarge) 240 else 160

        val frame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 10, 0, 10)
            }
            setBackgroundColor(Color.parseColor("#333333"))
            setOnClickListener {
                selectSlot(type)
            }
        }

        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                (size * 0.7).toInt(), (size * 0.7).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        frame.addView(imageView)

        val textView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 10
            }
            text = type.title
            setTextColor(Color.LTGRAY)
            textSize = if(isLarge) 14f else 10f
            gravity = Gravity.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }
        frame.addView(textView)

        slotViewsMap[type] = SlotViews(frame, imageView, textView)
        updateSlotButtonUI(type)

        return frame
    }

    private fun selectSlot(type: SlotType) {
        val prevSlot = slotViewsMap[currentSelectedSlot]
        prevSlot?.container?.setBackgroundColor(Color.parseColor("#333333"))

        currentSelectedSlot = type

        val currSlot = slotViewsMap[currentSelectedSlot]
        currSlot?.container?.setBackgroundColor(Color.parseColor("#555555"))

        refreshInventoryGrid()
    }

    private fun updateSlotButtonUI(type: SlotType) {
        val views = slotViewsMap[type] ?: return

        when (type) {
            SlotType.UNIT -> {
                views.label.text = "${type.title}\n${equippedCharacter?.name ?: "없음"}"
                views.label.setTextColor(Color.CYAN)
                val bitmap = if (equippedCharacter != null) ResourceManager.getUnitBitmap(equippedCharacter!!) else null
                if (bitmap != null) {
                    views.icon.setImageBitmap(bitmap)
                } else {
                    views.icon.setImageDrawable(null)
                }
            }
            SlotType.WEAPON -> {
                val gradeMark = equippedWeaponGrade.name.first()
                views.label.text = "${type.title}\n${equippedWeapon?.name ?: "없음"} [$gradeMark]"
                views.label.setTextColor(equippedWeaponGrade.getColor())

                if (equippedWeapon != null) {
                    val bitmap = ResourceManager.getWeaponBitmap(equippedWeapon!!, equippedWeaponGrade)
                    if (bitmap != null) {
                        views.icon.setImageBitmap(bitmap)
                    } else {
                        views.icon.setImageDrawable(null)
                    }
                } else {
                    views.icon.setImageDrawable(null)
                }
            }
            else -> {
                views.label.text = "${type.title}\n(미구현)"
                views.label.setTextColor(Color.GRAY)
                views.icon.setImageDrawable(null)
            }
        }
    }

    private fun refreshInventoryGrid() {
        inventoryGridContainer.removeAllViews()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels * 0.9
        val itemSize = ((screenWidth - 100) / 4).toInt()

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

            val bitmap = ResourceManager.getUnitBitmap(type)

            val itemView = createGridItemView(
                size = itemSize,
                mainText = type.name,
                subText = "UNIT",
                textColor = Color.WHITE,
                borderColor = if(isOwned) Color.CYAN else Color.DKGRAY,
                isOwned = isOwned,
                isEquipped = isEquipped,
                bitmap = bitmap
            ) {
                if (isOwned) {
                    equippedCharacter = type
                    updateSlotButtonUI(SlotType.UNIT)
                    updateStatusText()
                    refreshInventoryGrid()
                } else {
                    Toast.makeText(context, "미보유 캐릭터입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            currentRow?.addView(itemView)
        }
        fillEmptySlotsInRow(currentRow, items.size)
    }

    private fun loadWeaponGrid(itemSize: Int) {
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
            val color = grade.getColor()

            val bitmap = ResourceManager.getWeaponBitmap(type, grade)

            val itemView = createGridItemView(
                size = itemSize,
                mainText = "${grade.name}",
                subText = "x$count",
                textColor = color,
                borderColor = color,
                isOwned = true,
                isEquipped = isEquipped,
                bitmap = bitmap
            ) {
                equippedWeapon = type
                equippedWeaponGrade = grade
                updateSlotButtonUI(SlotType.WEAPON)
                updateStatusText()
                refreshInventoryGrid()
            }

            itemView.setOnLongClickListener {
                tryUpgrade(type, grade)
                true
            }
            currentRow?.addView(itemView)
        }

        fillEmptySlotsInRow(currentRow, weaponList.size)

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
        borderColor: Int,
        isOwned: Boolean,
        isEquipped: Boolean,
        bitmap: Bitmap?,
        onClick: () -> Unit
    ): View {
        val container = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, size, 1f).apply {
                marginStart = 5
                marginEnd = 5
            }
            val gd = GradientDrawable().apply {
                setColor(if (isEquipped) Color.parseColor("#1B5E20") else Color.parseColor("#222222"))
                setStroke(if (isEquipped) 6 else 2, borderColor)
                cornerRadius = 15f
            }
            background = gd
            setOnClickListener { onClick() }
        }

        if (bitmap != null && isOwned) {
            val imageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (size * 0.7).toInt(), (size * 0.7).toInt()
                ).apply {
                    gravity = Gravity.CENTER
                    bottomMargin = 10
                }
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            container.addView(imageView)
        } else if (!isOwned) {
            val lockTv = TextView(context).apply {
                text = "🔒"
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            container.addView(lockTv)
        }

        val infoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                bottomMargin = 5
            }
        }

        val title = TextView(context).apply {
            text = mainText
            setTextColor(textColor)
            textSize = 10f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 0f, 0f, Color.BLACK)
        }

        val sub = TextView(context).apply {
            text = subText
            setTextColor(Color.LTGRAY)
            textSize = 9f
            gravity = Gravity.CENTER
        }

        infoLayout.addView(title)
        infoLayout.addView(sub)
        container.addView(infoLayout)

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