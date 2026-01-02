package com.donghwa.gameVerse.character

import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.WeaponGrade
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.item.EquipItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Random

class CharacterDataManager {
    private val db = FirebaseFirestore.getInstance()
    private val random = Random()

    // --- [섹션 1] 일반 RPG 캐릭터 장비 (CharacterPopup용) ---
    var currentWeaponId: String = "w_001"
    var currentRingId: String = "r_001"
    var currentNecklaceId: String = "n_001"

    // --- [섹션 2] 디펜스 게임 전용 장비 (DefenseCharacterPopup용) ---
    // 디펜스 게임 장착 정보 (기본값 POTATO로 변경)
    var equippedDefenseCharacter: DefenseCharacterType = DefenseCharacterType.POTATO
    var equippedDefenseWeapon: WeaponType = WeaponType.SMG
    var equippedDefenseWeaponGrade: WeaponGrade = WeaponGrade.NORMAL

    // 보유한 캐릭터 목록 (기본적으로 POTATO는 가지고 시작)
    val ownedDefenseCharacters = hashSetOf(DefenseCharacterType.POTATO)

    // 보유한 무기 목록 (타입, 등급) -> 개수
    // 키: "TYPE_GRADE" (예: "SMG_NORMAL"), 값: 개수
    val ownedWeapons = HashMap<String, Int>()

    // [신규] 화폐 (동)
    var userDong: Int = 0

    init {
        // 테스트용 초기 데이터
        ownedWeapons["SMG_NORMAL"] = 1
    }

    // ==================================================================================
    // [메서드 그룹 1] 일반 RPG 장비 관리 (CharacterPopup 연동)
    // ==================================================================================

    // 장비 저장 (CharacterPopup에서 호출)
    fun saveEquipment(uid: String, weaponId: String, ringId: String, necklaceId: String, onComplete: () -> Unit) {
        currentWeaponId = weaponId
        currentRingId = ringId
        currentNecklaceId = necklaceId

        val data = hashMapOf(
            "weaponId" to weaponId,
            "ringId" to ringId,
            "necklaceId" to necklaceId
        )

        db.collection("users").document(uid).collection("equipment").document("current")
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onComplete() }
    }

    // 일반 장비 데이터 로드
    fun loadInventory(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).collection("equipment").document("current").get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentWeaponId = document.getString("weaponId") ?: "w_001"
                    currentRingId = document.getString("ringId") ?: "r_001"
                    currentNecklaceId = document.getString("necklaceId") ?: "n_001"
                } else {
                    // 데이터가 없으면 초기값으로 저장 후 로드한 것으로 간주
                    saveEquipment(uid, "w_001", "r_001", "n_001") {}
                }
                onComplete()
            }
            .addOnFailureListener { onComplete() }
    }

    // 총 공격력 배율 계산 (CharacterPopup 스탯 표시용)
    fun getTotalDamageMultiplier(): Float {
        var bonus = 0
        EquipItem.getById(currentWeaponId)?.let { bonus += it.statBonus }
        EquipItem.getById(currentRingId)?.let { bonus += it.statBonus }
        EquipItem.getById(currentNecklaceId)?.let { bonus += it.statBonus }
        return 1.0f + (bonus / 100f)
    }


    // ==================================================================================
    // [메서드 그룹 2] 디펜스 게임 전용 로직 (DefenseCharacterPopup 연동)
    // ==================================================================================

    fun loadDefenseInventory(uid: String, onComplete: () -> Unit) {
        val docRef = db.collection("users").document(uid).collection("defense_inventory").document("data")

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                // 1. 장착 정보 로드
                val charStr = document.getString("equippedCharacter")
                if (charStr != null) {
                    try {
                        equippedDefenseCharacter = DefenseCharacterType.valueOf(charStr)
                    } catch (e: Exception) {
                        equippedDefenseCharacter = DefenseCharacterType.POTATO
                    }
                }

                val weaponStr = document.getString("equippedWeapon")
                val gradeStr = document.getString("equippedWeaponGrade")
                if (weaponStr != null && gradeStr != null) {
                    try {
                        equippedDefenseWeapon = WeaponType.valueOf(weaponStr)
                        equippedDefenseWeaponGrade = WeaponGrade.valueOf(gradeStr)
                    } catch (e: Exception) {
                        equippedDefenseWeapon = WeaponType.SMG
                        equippedDefenseWeaponGrade = WeaponGrade.NORMAL
                    }
                }

                // 2. 보유 캐릭터 로드
                val charList = document.get("ownedCharacters") as? List<String>
                if (charList != null) {
                    ownedDefenseCharacters.clear()
                    for (c in charList) {
                        try {
                            ownedDefenseCharacters.add(DefenseCharacterType.valueOf(c))
                        } catch (e: Exception) {}
                    }
                }
                // 기본 캐릭터는 항상 보유
                ownedDefenseCharacters.add(DefenseCharacterType.POTATO)

                // 3. 보유 무기 로드
                ownedWeapons.clear()
                val currentMap = document.get("ownedWeapons") as? Map<*, *>
                if (currentMap != null) {
                    for ((k, v) in currentMap) {
                        if (k is String) {
                            val count = when (v) {
                                is Int -> v
                                is Long -> v.toInt()
                                is String -> v.toIntOrNull() ?: 0
                                else -> 0
                            }
                            if (count > 0) {
                                ownedWeapons[k] = count
                            }
                        }
                    }
                }

                if (ownedWeapons.isEmpty()) {
                    ownedWeapons["SMG_NORMAL"] = 1
                }

                // 4. [신규] 화폐(동) 로드
                userDong = document.getLong("userDong")?.toInt() ?: 0

            } else {
                // 데이터가 없으면 초기값 저장
                saveDefenseInventory(uid)
            }
            // 일반 장비 데이터도 함께 로드 시도 (순차 처리)
            loadInventory(uid) {
                onComplete()
            }
        }.addOnFailureListener {
            onComplete()
        }
    }

    fun saveDefenseLoadout(uid: String, charType: DefenseCharacterType, weaponType: WeaponType, grade: WeaponGrade) {
        equippedDefenseCharacter = charType
        equippedDefenseWeapon = weaponType
        equippedDefenseWeaponGrade = grade

        saveDefenseInventory(uid)
    }

    // 무기 획득 함수
    fun unlockWeapon(uid: String, type: WeaponType, grade: WeaponGrade, count: Int = 1, onComplete: ((Boolean) -> Unit)? = null) {
        val key = "${type.name}_${grade.name}"
        val currentCount = ownedWeapons[key] ?: 0
        ownedWeapons[key] = currentCount + count

        saveDefenseInventory(uid)
        onComplete?.invoke(true)
    }

    // [신규] 동 획득 함수 (MainActivity에서 호출)
    fun addDong(uid: String, amount: Int, onComplete: (() -> Unit)? = null) {
        userDong += amount
        saveDefenseInventory(uid)
        onComplete?.invoke()
    }

    // [신규] 뽑기 로직
    fun drawGachaWeapon(uid: String, cost: Int, callback: (Boolean, String, WeaponType?, WeaponGrade?) -> Unit) {
        if (userDong < cost) {
            callback(false, "동이 부족합니다.", null, null)
            return
        }

        // 동 차감
        userDong -= cost

        // 확률 계산 (노말 > 매직 > 레어 > 유니크 > 레전드)
        val rand = random.nextFloat() // 0.0 ~ 1.0
        val grade = when {
            rand < 0.005f -> WeaponGrade.LEGEND  // 0.5% (매우 희박)
            rand < 0.03f  -> WeaponGrade.UNIQUE  // 2.5%
            rand < 0.10f  -> WeaponGrade.RARE    // 7%
            rand < 0.35f  -> WeaponGrade.MAGIC   // 25%
            else          -> WeaponGrade.NORMAL  // 65%
        }

        // 무기 타입 랜덤
        val types = WeaponType.values()
        val type = types[random.nextInt(types.size)]

        // 인벤토리에 추가
        unlockWeapon(uid, type, grade, 1) {
            callback(true, "획득! ${grade.name} ${type.name}", type, grade)
        }
    }

    fun upgradeWeapon(uid: String, type: WeaponType, currentGrade: WeaponGrade, callback: (Boolean, String) -> Unit) {
        val currentKey = "${type.name}_${currentGrade.name}"
        val count = ownedWeapons[currentKey] ?: 0
        val cost = currentGrade.getUpgradeCost()

        if (cost == 0) {
            callback(false, "최고 등급입니다.")
            return
        }

        if (count >= cost) {
            // 재료 소모
            ownedWeapons[currentKey] = count - cost

            // 다음 등급 획득
            val nextGrade = WeaponGrade.values()[currentGrade.ordinal + 1]
            val nextKey = "${type.name}_${nextGrade.name}"
            val nextCount = ownedWeapons[nextKey] ?: 0
            ownedWeapons[nextKey] = nextCount + 1

            saveDefenseInventory(uid)
            callback(true, "승급 성공! ${nextGrade.name} ${type.name} 획득")
        } else {
            callback(false, "재료가 부족합니다.")
        }
    }

    fun getWeaponCount(type: WeaponType, grade: WeaponGrade): Int {
        return ownedWeapons["${type.name}_${grade.name}"] ?: 0
    }

    private fun saveDefenseInventory(uid: String) {
        val data = hashMapOf(
            "equippedCharacter" to equippedDefenseCharacter.name,
            "equippedWeapon" to equippedDefenseWeapon.name,
            "equippedWeaponGrade" to equippedDefenseWeaponGrade.name,
            "ownedCharacters" to ownedDefenseCharacters.map { it.name }.toList(),
            "ownedWeapons" to ownedWeapons,
            "userDong" to userDong // [신규] 저장
        )

        db.collection("users").document(uid).collection("defense_inventory").document("data")
            .set(data, SetOptions.merge())
    }
}