package com.donghwa.gameVerse.character

import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.WeaponGrade
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.defensegame.Difficulty
import com.donghwa.gameVerse.item.EquipItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Random

enum class GachaBoxType {
    NORMAL, SPECIAL
}

class CharacterDataManager {
    private val db = FirebaseFirestore.getInstance()
    private val random = Random()

    var currentWeaponId: String = "w_001"
    var currentRingId: String = "r_001"
    var currentNecklaceId: String = "n_001"

    var equippedDefenseCharacter: DefenseCharacterType = DefenseCharacterType.POTATO
    var equippedDefenseWeapon: WeaponType = WeaponType.SMG
    var equippedDefenseWeaponGrade: WeaponGrade = WeaponGrade.NORMAL

    val ownedDefenseCharacters = hashSetOf(DefenseCharacterType.POTATO)
    val ownedWeapons = HashMap<String, Int>()

    var userDong: Int = 0
    var userSilver: Int = 0

    // [수정] 스테이지별 난이도 및 단계 보상 획득 기록
    // Key format: "STAGE_DIFFICULTY_TIER" (e.g., "1_NORMAL_3", "1_HARD_10")
    val clearedStageRewards = HashSet<String>()

    // [신규] 스테이지별 최대 도달 웨이브 기록
    // Key format: "STAGE_DIFFICULTY" -> MaxWave (e.g., "1_NORMAL" -> 6)
    val maxWaveRecords = HashMap<String, Int>()

    init {
        ownedWeapons["SMG_NORMAL"] = 1
    }

    // ... (일반 장비 관리 메서드 생략 - 기존 유지) ...
    fun saveEquipment(uid: String, weaponId: String, ringId: String, necklaceId: String, onComplete: () -> Unit) {
        currentWeaponId = weaponId
        currentRingId = ringId
        currentNecklaceId = necklaceId
        val data = hashMapOf("weaponId" to weaponId, "ringId" to ringId, "necklaceId" to necklaceId)
        db.collection("users").document(uid).collection("equipment").document("current")
            .set(data, SetOptions.merge()).addOnSuccessListener { onComplete() }.addOnFailureListener { onComplete() }
    }
    fun loadInventory(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).collection("equipment").document("current").get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentWeaponId = document.getString("weaponId") ?: "w_001"
                    currentRingId = document.getString("ringId") ?: "r_001"
                    currentNecklaceId = document.getString("necklaceId") ?: "n_001"
                } else { saveEquipment(uid, "w_001", "r_001", "n_001") {} }
                onComplete()
            }.addOnFailureListener { onComplete() }
    }
    fun getTotalDamageMultiplier(): Float {
        var bonus = 0
        EquipItem.getById(currentWeaponId)?.let { bonus += it.statBonus }
        EquipItem.getById(currentRingId)?.let { bonus += it.statBonus }
        EquipItem.getById(currentNecklaceId)?.let { bonus += it.statBonus }
        return 1.0f + (bonus / 100f)
    }

    fun loadDefenseInventory(uid: String, onComplete: () -> Unit) {
        val docRef = db.collection("users").document(uid).collection("defense_inventory").document("data")

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val charStr = document.getString("equippedCharacter")
                if (charStr != null) try { equippedDefenseCharacter = DefenseCharacterType.valueOf(charStr) } catch (e: Exception) { equippedDefenseCharacter = DefenseCharacterType.POTATO }

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

                val charList = document.get("ownedCharacters") as? List<String>
                if (charList != null) {
                    ownedDefenseCharacters.clear()
                    for (c in charList) try { ownedDefenseCharacters.add(DefenseCharacterType.valueOf(c)) } catch (e: Exception) {}
                }
                ownedDefenseCharacters.add(DefenseCharacterType.POTATO)

                ownedWeapons.clear()
                val currentMap = document.get("ownedWeapons") as? Map<*, *>
                if (currentMap != null) {
                    for ((k, v) in currentMap) {
                        if (k is String) {
                            val count = when (v) { is Int -> v; is Long -> v.toInt(); is String -> v.toIntOrNull() ?: 0; else -> 0 }
                            if (count > 0) ownedWeapons[k] = count
                        }
                    }
                }
                if (ownedWeapons.isEmpty()) ownedWeapons["SMG_NORMAL"] = 1

                userDong = document.getLong("userDong")?.toInt() ?: 0
                userSilver = document.getLong("userSilver")?.toInt() ?: 0

                val rewardList = document.get("clearedStageRewards") as? List<String>
                if (rewardList != null) {
                    clearedStageRewards.clear()
                    clearedStageRewards.addAll(rewardList)
                }

                // [신규] 웨이브 기록 로드
                val waveMap = document.get("maxWaveRecords") as? Map<*, *>
                if (waveMap != null) {
                    maxWaveRecords.clear()
                    for ((k, v) in waveMap) {
                        if (k is String && v is Long) maxWaveRecords[k] = v.toInt()
                        else if (k is String && v is Int) maxWaveRecords[k] = v
                    }
                }

            } else {
                saveDefenseInventory(uid)
            }
            loadInventory(uid) { onComplete() }
        }.addOnFailureListener { onComplete() }
    }

    fun saveDefenseLoadout(uid: String, charType: DefenseCharacterType, weaponType: WeaponType, grade: WeaponGrade) {
        equippedDefenseCharacter = charType
        equippedDefenseWeapon = weaponType
        equippedDefenseWeaponGrade = grade
        saveDefenseInventory(uid)
    }

    fun unlockWeapon(uid: String, type: WeaponType, grade: WeaponGrade, count: Int = 1, onComplete: ((Boolean) -> Unit)? = null) {
        val key = "${type.name}_${grade.name}"
        val currentCount = ownedWeapons[key] ?: 0
        ownedWeapons[key] = currentCount + count
        saveDefenseInventory(uid)
        onComplete?.invoke(true)
    }

    fun addDong(uid: String, amount: Int, onComplete: (() -> Unit)? = null) {
        userDong += amount
        saveDefenseInventory(uid)
        onComplete?.invoke()
    }

    fun addSilver(uid: String, amount: Int, onComplete: (() -> Unit)? = null) {
        userSilver += amount
        saveDefenseInventory(uid)
        onComplete?.invoke()
    }

    // [신규] 보상 획득 여부
    fun isRewardTierClaimed(stage: Int, difficulty: Difficulty, tier: Int): Boolean {
        return clearedStageRewards.contains("${stage}_${difficulty.name}_$tier")
    }

    // [신규] 보상 획득 처리
    fun claimRewardTier(uid: String, stage: Int, difficulty: Difficulty, tier: Int, rewardAmount: Int, onComplete: () -> Unit) {
        val key = "${stage}_${difficulty.name}_$tier"
        if (!clearedStageRewards.contains(key)) {
            clearedStageRewards.add(key)
            userSilver += rewardAmount
            saveDefenseInventory(uid)
            onComplete()
        }
    }

    // [신규] 스테이지 클리어(웨이브 도달) 기록
    fun recordStageClear(uid: String, stage: Int, difficulty: Difficulty, maxWaveReached: Int) {
        val key = "${stage}_${difficulty.name}"
        val currentMax = maxWaveRecords[key] ?: 0
        if (maxWaveReached > currentMax) {
            maxWaveRecords[key] = maxWaveReached
            saveDefenseInventory(uid)
        }
    }

    // [신규] 미수령 보상 확인 (UI 반짝임용)
    fun hasUnclaimedRewards(stage: Int): Boolean {
        for (diff in Difficulty.values()) {
            val key = "${stage}_${diff.name}"
            val maxWave = maxWaveRecords[key] ?: 0

            val tiers = listOf(3, 6, 10)
            for (tier in tiers) {
                if (maxWave >= tier && !isRewardTierClaimed(stage, diff, tier)) {
                    return true
                }
            }
        }
        return false
    }

    fun drawGachaWeapon(uid: String, boxType: GachaBoxType, callback: (Boolean, String, WeaponType?, WeaponGrade?) -> Unit) {
        val cost = if (boxType == GachaBoxType.NORMAL) 100 else 350
        if (userSilver < cost) { callback(false, "은(Silver)이 부족합니다.", null, null); return }
        userSilver -= cost
        val rand = random.nextFloat()
        val grade = if (boxType == GachaBoxType.NORMAL) {
            when { rand < 0.005f -> WeaponGrade.LEGEND; rand < 0.03f -> WeaponGrade.UNIQUE; rand < 0.10f -> WeaponGrade.RARE; rand < 0.35f -> WeaponGrade.MAGIC; else -> WeaponGrade.NORMAL }
        } else {
            when { rand < 0.05f -> WeaponGrade.LEGEND; rand < 0.15f -> WeaponGrade.UNIQUE; rand < 0.45f -> WeaponGrade.RARE; rand < 0.90f -> WeaponGrade.MAGIC; else -> WeaponGrade.NORMAL }
        }
        val types = WeaponType.values()
        val type = types[random.nextInt(types.size)]
        unlockWeapon(uid, type, grade, 1) {
            val boxName = if (boxType == GachaBoxType.NORMAL) "일반" else "특수"
            callback(true, "[$boxName] 획득! ${grade.name} ${type.name}", type, grade)
        }
    }

    fun upgradeWeapon(uid: String, type: WeaponType, currentGrade: WeaponGrade, callback: (Boolean, String) -> Unit) {
        val currentKey = "${type.name}_${currentGrade.name}"
        val count = ownedWeapons[currentKey] ?: 0
        val cost = currentGrade.getUpgradeCost()
        if (cost == 0) { callback(false, "최고 등급입니다."); return }
        if (count >= cost) {
            ownedWeapons[currentKey] = count - cost
            val nextGrade = WeaponGrade.values()[currentGrade.ordinal + 1]
            val nextKey = "${type.name}_${nextGrade.name}"
            val nextCount = ownedWeapons[nextKey] ?: 0
            ownedWeapons[nextKey] = nextCount + 1
            saveDefenseInventory(uid)
            callback(true, "승급 성공! ${nextGrade.name} ${type.name} 획득")
        } else { callback(false, "재료가 부족합니다.") }
    }

    fun getWeaponCount(type: WeaponType, grade: WeaponGrade): Int { return ownedWeapons["${type.name}_${grade.name}"] ?: 0 }

    private fun saveDefenseInventory(uid: String) {
        val data = hashMapOf(
            "equippedCharacter" to equippedDefenseCharacter.name,
            "equippedWeapon" to equippedDefenseWeapon.name,
            "equippedWeaponGrade" to equippedDefenseWeaponGrade.name,
            "ownedCharacters" to ownedDefenseCharacters.map { it.name }.toList(),
            "ownedWeapons" to ownedWeapons,
            "userDong" to userDong,
            "userSilver" to userSilver,
            "clearedStageRewards" to clearedStageRewards.toList(),
            "maxWaveRecords" to maxWaveRecords // [신규] 저장
        )
        db.collection("users").document(uid).collection("defense_inventory").document("data")
            .set(data, SetOptions.merge())
    }
}