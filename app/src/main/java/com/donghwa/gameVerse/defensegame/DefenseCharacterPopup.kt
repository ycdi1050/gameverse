package com.donghwa.gameVerse.defensegame

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class DefenseCharacterPopup(
    private val context: Context,
    private val onStart: (DefenseCharacterType, WeaponType) -> Unit
) {
    private var selectedCharacter: DefenseCharacterType? = null
    private var selectedWeapon: WeaponType? = null

    private val characterButtons = mutableListOf<Button>()
    private val weaponButtons = mutableListOf<Button>()
    private lateinit var startButton: Button

    fun show() {
        val scrollView = ScrollView(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)
        layout.setBackgroundColor(Color.parseColor("#212121"))
        scrollView.addView(layout)

        // 1. 타이틀
        val title = TextView(context)
        title.text = "출격 준비"
        title.textSize = 28f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 40)
        layout.addView(title)

        // 2. 캐릭터 선택 섹션
        addSectionTitle(layout, "1. 캐릭터(Unit) 선택")
        val charLayout = LinearLayout(context)
        charLayout.orientation = LinearLayout.VERTICAL
        for (type in DefenseCharacterType.values()) {
            val btn = Button(context)
            btn.text = "${type.getDisplayName()}\n${type.getDescription()}"
            btn.textSize = 14f
            btn.setOnClickListener { selectCharacter(type) }
            styleButton(btn, false)
            characterButtons.add(btn)
            charLayout.addView(btn)
        }
        layout.addView(charLayout)

        // 3. 무기 선택 섹션
        addSectionTitle(layout, "2. 무기(Weapon) 선택")
        val weaponLayout = LinearLayout(context)
        weaponLayout.orientation = LinearLayout.VERTICAL
        for (type in WeaponType.values()) {
            val btn = Button(context)
            btn.text = "${getWeaponName(type)}\n${getWeaponDescription(type)}"
            btn.textSize = 14f
            btn.setOnClickListener { selectWeapon(type) }
            styleButton(btn, false)
            weaponButtons.add(btn)
            weaponLayout.addView(btn)
        }
        layout.addView(weaponLayout)

        // 4. 시작 버튼
        startButton = Button(context)
        startButton.text = "GAME START"
        startButton.textSize = 20f
        startButton.isEnabled = false // 둘 다 선택해야 활성화
        startButton.setBackgroundColor(Color.DKGRAY)
        startButton.setTextColor(Color.GRAY)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 150
        )
        params.setMargins(0, 50, 0, 0)
        startButton.layoutParams = params
        startButton.setOnClickListener {
            if (selectedCharacter != null && selectedWeapon != null) {
                onStart(selectedCharacter!!, selectedWeapon!!)
            }
        }
        layout.addView(startButton)

        val dialog = AlertDialog.Builder(context)
            .setView(scrollView)
            .create()

        // 시작 버튼 클릭 시 다이얼로그 닫기 추가
        startButton.setOnClickListener {
            if (selectedCharacter != null && selectedWeapon != null) {
                dialog.dismiss()
                onStart(selectedCharacter!!, selectedWeapon!!)
            }
        }

        dialog.show()
    }

    private fun addSectionTitle(layout: LinearLayout, text: String) {
        val tv = TextView(context)
        tv.text = text
        tv.textSize = 18f
        tv.setTextColor(Color.YELLOW)
        tv.setPadding(0, 30, 0, 10)
        layout.addView(tv)
    }

    private fun selectCharacter(type: DefenseCharacterType) {
        selectedCharacter = type
        updateButtonStyles(characterButtons, DefenseCharacterType.values().indexOf(type))
        checkStartEnabled()
    }

    private fun selectWeapon(type: WeaponType) {
        selectedWeapon = type
        updateButtonStyles(weaponButtons, WeaponType.values().indexOf(type))
        checkStartEnabled()
    }

    private fun updateButtonStyles(buttons: List<Button>, selectedIndex: Int) {
        for ((index, btn) in buttons.withIndex()) {
            if (index == selectedIndex) {
                btn.setBackgroundColor(Color.parseColor("#76FF03")) // 선택됨 (연두색)
                btn.setTextColor(Color.BLACK)
            } else {
                styleButton(btn, false)
            }
        }
    }

    private fun styleButton(btn: Button, isSelected: Boolean) {
        btn.setBackgroundColor(Color.parseColor("#424242"))
        btn.setTextColor(Color.WHITE)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 5, 0, 5)
        btn.layoutParams = params
    }

    private fun checkStartEnabled() {
        if (selectedCharacter != null && selectedWeapon != null) {
            startButton.isEnabled = true
            startButton.setBackgroundColor(Color.CYAN)
            startButton.setTextColor(Color.BLACK)
        }
    }

    private fun getWeaponName(type: WeaponType): String {
        return when (type) {
            WeaponType.SMG -> "🔫 기관단총"
            WeaponType.SHOTGUN -> "💥 샷건"
            WeaponType.SNIPER -> "🎯 스나이퍼"
            WeaponType.MISSILE -> "🚀 미사일"
            WeaponType.BOW -> "🏹 활"
        }
    }

    private fun getWeaponDescription(type: WeaponType): String {
        return when (type) {
            WeaponType.SMG -> "빠른 연사"
            WeaponType.SHOTGUN -> "범위 공격"
            WeaponType.SNIPER -> "장거리 저격"
            WeaponType.MISSILE -> "유도탄"
            WeaponType.BOW -> "독 데미지"
        }
    }
}