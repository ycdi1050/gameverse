package com.donghwa.gameVerse.defensegame

import java.util.Random

class DropManager {
    private val random = Random()

    // 드롭 확률 (0.5 -> 50%)
    var dropChance: Float = 0.5f

    fun shouldDrop(): Boolean {
        return random.nextFloat() < dropChance
    }

    fun createDropItem(x: Float, y: Float): DropItem {
        val rand = random.nextFloat()

        // 70% 확률로 동(Dong), 30% 확률로 무기 드롭 (드롭이 발생했을 때 기준)
        if (rand < 0.7f) {
            // 동 드롭: 10 ~ 50개 랜덤
            val amount = random.nextInt(41) + 10
            return DropItem(x, y, DropType.DONG, amount = amount)
        } else {
            // 무기 드롭
            val weapons = WeaponType.values()
            val randomWeapon = weapons[random.nextInt(weapons.size)]
            val randomGrade = WeaponGrade.NORMAL // 필드 드롭은 노말
            return DropItem(x, y, DropType.WEAPON, weaponType = randomWeapon, weaponGrade = randomGrade)
        }
    }
}