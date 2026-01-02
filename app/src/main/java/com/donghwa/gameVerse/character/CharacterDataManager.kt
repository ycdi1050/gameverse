package com.donghwa.gameVerse.character

import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.defensegame.WeaponGrade
import com.donghwa.gameVerse.item.EquipItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CharacterDataManager {
    private val db = FirebaseFirestore.getInstance()

    // RPG 장비
    var currentWeaponId: String = "w_001"
    var currentRingId: String = "r_001"
    var currentNecklaceId: String = "n_001"

    // [Defense] 디펜스 게임 데이터
    var ownedDefenseCharacters = mutableListOf<DefenseCharacterType>()

    // [신규] 무기 인벤토리: Key="WeaponType_WeaponGrade", Value=Count
    // 예: "SMG_NORMAL" -> 5
    var weaponInventory = mutableMapOf<String, Int>()

    var equippedDefenseCharacter: DefenseCharacterType = DefenseCharacterType.HUMAN
    var equippedDefenseWeapon: WeaponType = WeaponType.SMG
    var equippedDefenseWeaponGrade: WeaponGrade = WeaponGrade.NORMAL // [신규] 장착 등급

    // -----------------------------------------------------------
    // [Defense] 디펜스 게임 인벤토리 로드
    // -----------------------------------------------------------
    fun loadDefenseInventory(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                ownedDefenseCharacters.clear()
                weaponInventory.clear()

                if (document != null && document.exists()) {
                    // 1. 캐릭터 목록
                    val charList = document.get("def_owned_chars") as? List<String>
                    if (charList.isNullOrEmpty()) {
                        ownedDefenseCharacters.add(DefenseCharacterType.HUMAN)
                    } else {
                        charList.forEach { try { ownedDefenseCharacters.add(DefenseCharacterType.valueOf(it)) } catch(e:Exception){} }
                    }

                    // 2. 무기 인벤토리 (Map)
                    val invMap = document.get("def_weapon_inventory") as? Map<String, Long>
                    if (invMap != null) {
                        for ((key, value) in invMap) {
                            weaponInventory[key] = value.toInt()
                        }
                    } else {
                        // 기존 리스트 데이터가 있다면 마이그레이션 (모두 NORMAL로 지급)
                        val oldList = document.get("def_owned_weapons") as? List<String>
                        if (!oldList.isNullOrEmpty()) {
                            oldList.forEach { typeName ->
                                val key = "${typeName}_NORMAL"
                                weaponInventory[key] = (weaponInventory[key] ?: 0) + 1
                            }
                        } else {
                            // 기본 지급
                            weaponInventory["SMG_NORMAL"] = 1
                        }
                    }

                    // 3. 장착 정보
                    val equipChar = document.getString("def_equip_char")
                    val equipWeapon = document.getString("def_equip_weapon")
                    val equipGrade = document.getString("def_equip_weapon_grade")

                    equippedDefenseCharacter = try { if(equipChar != null) DefenseCharacterType.valueOf(equipChar) else DefenseCharacterType.HUMAN } catch(e:Exception) { DefenseCharacterType.HUMAN }
                    equippedDefenseWeapon = try { if(equipWeapon != null) WeaponType.valueOf(equipWeapon) else WeaponType.SMG } catch(e:Exception) { WeaponType.SMG }
                    equippedDefenseWeaponGrade = try { if(equipGrade != null) WeaponGrade.valueOf(equipGrade) else WeaponGrade.NORMAL } catch(e:Exception) { WeaponGrade.NORMAL }
                } else {
                    // 신규 유저
                    ownedDefenseCharacters.add(DefenseCharacterType.HUMAN)
                    weaponInventory["SMG_NORMAL"] = 1
                    equippedDefenseCharacter = DefenseCharacterType.HUMAN
                    equippedDefenseWeapon = WeaponType.SMG
                    equippedDefenseWeaponGrade = WeaponGrade.NORMAL
                }

                // 최소 보장
                if(ownedDefenseCharacters.isEmpty()) ownedDefenseCharacters.add(DefenseCharacterType.HUMAN)
                if(weaponInventory.isEmpty()) weaponInventory["SMG_NORMAL"] = 1

                onComplete()
            }
            .addOnFailureListener { onComplete() }
    }

    // [Defense] 장비 저장
    fun saveDefenseLoadout(uid: String, charType: DefenseCharacterType, weaponType: WeaponType, grade: WeaponGrade) {
        val data = hashMapOf(
            "def_equip_char" to charType.name,
            "def_equip_weapon" to weaponType.name,
            "def_equip_weapon_grade" to grade.name
        )
        db.collection("users").document(uid).set(data, SetOptions.merge())

        equippedDefenseCharacter = charType
        equippedDefenseWeapon = weaponType
        equippedDefenseWeaponGrade = grade
    }

    // [Defense] 무기 획득 (드롭)
    fun unlockWeapon(uid: String, weapon: WeaponType, grade: WeaponGrade, onResult: (Boolean) -> Unit) {
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentMap = snapshot.get("def_weapon_inventory") as? Map<String, Long> ?: mapOf()
            val mutableMap = currentMap.toMutableMap()

            val key = "${weapon.name}_${grade.name}"
            val currentCount = mutableMap[key] ?: 0L
            mutableMap[key] = currentCount + 1

            transaction.update(userRef, "def_weapon_inventory", mutableMap)
            true
        }.addOnSuccessListener {
            // 메모리 업데이트
            val key = "${weapon.name}_${grade.name}"
            weaponInventory[key] = (weaponInventory[key] ?: 0) + 1
            onResult(true)
        }.addOnFailureListener { onResult(false) }
    }

    // [Defense] 무기 합성 (Upgrade)
    fun upgradeWeapon(uid: String, weapon: WeaponType, currentGrade: WeaponGrade, onResult: (Boolean, String) -> Unit) {
        val nextGrade = currentGrade.getNextGrade()
        if (nextGrade == null) {
            onResult(false, "최고 등급입니다.")
            return
        }

        val cost = currentGrade.getUpgradeCost()
        val currentKey = "${weapon.name}_${currentGrade.name}"
        val nextKey = "${weapon.name}_${nextGrade.name}"

        // 메모리 체크
        val currentCount = weaponInventory[currentKey] ?: 0
        if (currentCount < cost) {
            onResult(false, "재료가 부족합니다. (${currentCount}/${cost})")
            return
        }

        // DB 트랜잭션
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentMap = snapshot.get("def_weapon_inventory") as? Map<String, Long> ?: mapOf()
            val mutableMap = currentMap.toMutableMap()

            val dbCount = mutableMap[currentKey] ?: 0L
            if (dbCount < cost) throw Exception("NOT_ENOUGH_MATERIALS")

            // 재료 소모
            mutableMap[currentKey] = dbCount - cost
            // 결과물 지급
            val nextCount = mutableMap[nextKey] ?: 0L
            mutableMap[nextKey] = nextCount + 1

            transaction.update(userRef, "def_weapon_inventory", mutableMap)
        }.addOnSuccessListener {
            // 메모리 반영
            weaponInventory[currentKey] = (weaponInventory[currentKey] ?: 0) - cost
            weaponInventory[nextKey] = (weaponInventory[nextKey] ?: 0) + 1
            onResult(true, "${nextGrade.getDisplayName()} 등급으로 승급했습니다!")
        }.addOnFailureListener { e ->
            onResult(false, if (e.message == "NOT_ENOUGH_MATERIALS") "재료가 부족합니다." else "오류가 발생했습니다.")
        }
    }

    // 헬퍼: 특정 무기/등급 개수 조회
    fun getWeaponCount(type: WeaponType, grade: WeaponGrade): Int {
        return weaponInventory["${type.name}_${grade.name}"] ?: 0
    }

    // --- RPG ---
    fun loadEquipment(uid: String, onComplete: () -> Unit) { /* Existing Code */ }
    fun saveEquipment(uid: String, weaponId: String, ringId: String, necklaceId: String, onComplete: () -> Unit) { /* Existing Code */ }
    fun getTotalDamageMultiplier(): Float { return 1.0f } // Simplified for brevity in this response
    fun getEquippedItemNames(): Triple<String, String, String> { return Triple("","","") }
}