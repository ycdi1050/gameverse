package com.donghwa.gameVerse.character

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.donghwa.gameVerse.item.EquipItem

class CharacterPopup(
    private val context: Context,
    private val uid: String,
    private val dataManager: CharacterDataManager,
    private val onDismiss: () -> Unit
) {

    fun show() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        layout.setBackgroundColor(Color.parseColor("#212121"))
        layout.gravity = Gravity.CENTER_HORIZONTAL

        // 제목
        val title = TextView(context)
        title.text = "캐릭터 장비 설정"
        title.textSize = 24f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 30)
        layout.addView(title)

        // 현재 스탯 표시
        val statsText = TextView(context)
        updateStatsText(statsText)
        statsText.setTextColor(Color.WHITE)
        statsText.textSize = 16f
        statsText.gravity = Gravity.CENTER
        statsText.setPadding(0, 0, 0, 30)
        layout.addView(statsText)

        // 무기 선택
        layout.addView(createLabel("무기 (Weapon)"))
        val weaponSpinner = createSpinner(EquipItem.WEAPONS, dataManager.currentWeaponId)
        layout.addView(weaponSpinner)

        // 반지 선택
        layout.addView(createLabel("반지 (Ring)"))
        val ringSpinner = createSpinner(EquipItem.RINGS, dataManager.currentRingId)
        layout.addView(ringSpinner)

        // 목걸이 선택
        layout.addView(createLabel("목걸이 (Necklace)"))
        val necklaceSpinner = createSpinner(EquipItem.NECKLACES, dataManager.currentNecklaceId)
        layout.addView(necklaceSpinner)

        // 저장 버튼
        val saveBtn = Button(context)
        saveBtn.text = "장비 저장"
        saveBtn.setBackgroundColor(Color.parseColor("#4CAF50"))
        saveBtn.setTextColor(Color.WHITE)
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.setMargins(0, 40, 0, 0)
        saveBtn.layoutParams = btnParams
        layout.addView(saveBtn)

        val dialog = AlertDialog.Builder(context)
            .setView(layout)
            .setNegativeButton("닫기", null)
            .create()

        saveBtn.setOnClickListener {
            val selectedWeapon = EquipItem.WEAPONS[weaponSpinner.selectedItemPosition]
            val selectedRing = EquipItem.RINGS[ringSpinner.selectedItemPosition]
            val selectedNecklace = EquipItem.NECKLACES[necklaceSpinner.selectedItemPosition]

            dataManager.saveEquipment(uid, selectedWeapon.id, selectedRing.id, selectedNecklace.id) {
                Toast.makeText(context, "장비가 저장되었습니다!", Toast.LENGTH_SHORT).show()
                updateStatsText(statsText) // 스탯 텍스트 갱신
                dialog.dismiss()
                onDismiss()
            }
        }

        dialog.show()
    }

    private fun createLabel(text: String): TextView {
        val tv = TextView(context)
        tv.text = text
        tv.setTextColor(Color.LTGRAY)
        tv.setPadding(0, 20, 0, 10)
        return tv
    }

    private fun createSpinner(items: List<EquipItem>, currentId: String): Spinner {
        val spinner = Spinner(context)
        val itemNames = items.map { "${it.name} (+${it.statBonus}%)" }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, itemNames)
        spinner.adapter = adapter
        spinner.setBackgroundColor(Color.LTGRAY)

        // 현재 장착된 아이템 선택
        val index = items.indexOfFirst { it.id == currentId }
        if (index >= 0) spinner.setSelection(index)

        return spinner
    }

    private fun updateStatsText(view: TextView) {
        val multiplier = dataManager.getTotalDamageMultiplier()
        val percent = ((multiplier - 1.0f) * 100).toInt()
        view.text = "현재 공격력 보너스: +$percent%"
    }
}