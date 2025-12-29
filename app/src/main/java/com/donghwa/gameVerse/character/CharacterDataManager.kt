package com.donghwa.gameVerse.character

import com.donghwa.gameVerse.item.EquipItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CharacterDataManager {
    private val db = FirebaseFirestore.getInstance()

    // 현재 장착중인 아이템 ID들 (메모리 캐시)
    var currentWeaponId: String = "w_001"
    var currentRingId: String = "r_001"
    var currentNecklaceId: String = "n_001"

    // 장비 데이터 불러오기
    fun loadEquipment(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentWeaponId = document.getString("equip_weapon") ?: "w_001"
                    currentRingId = document.getString("equip_ring") ?: "r_001"
                    currentNecklaceId = document.getString("equip_necklace") ?: "n_001"
                }
                onComplete()
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    // 장비 저장하기
    fun saveEquipment(uid: String, weaponId: String, ringId: String, necklaceId: String, onComplete: () -> Unit) {
        val data = hashMapOf(
            "equip_weapon" to weaponId,
            "equip_ring" to ringId,
            "equip_necklace" to necklaceId
        )

        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                currentWeaponId = weaponId
                currentRingId = ringId
                currentNecklaceId = necklaceId
                onComplete()
            }
    }

    // 총 공격력 배율 계산 (기본 1.0 + 아이템 보너스)
    fun getTotalDamageMultiplier(): Float {
        val weapon = EquipItem.getById(currentWeaponId)?.statBonus ?: 0
        val ring = EquipItem.getById(currentRingId)?.statBonus ?: 0
        val necklace = EquipItem.getById(currentNecklaceId)?.statBonus ?: 0

        // 예: 10% + 5% = 15% 증가 -> 1.15배
        val totalBonusPercent = weapon + ring + necklace
        return 1.0f + (totalBonusPercent / 100f)
    }

    // 장착 중인 아이템 이름 가져오기 (UI 표시용)
    fun getEquippedItemNames(): Triple<String, String, String> {
        val w = EquipItem.getById(currentWeaponId)?.name ?: "기본"
        val r = EquipItem.getById(currentRingId)?.name ?: "기본"
        val n = EquipItem.getById(currentNecklaceId)?.name ?: "기본"
        return Triple(w, r, n)
    }
}