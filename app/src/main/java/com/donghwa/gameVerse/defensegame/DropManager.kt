package com.donghwa.gameVerse.defensegame

import java.util.Random

/**
 * 게임 내 아이템 드롭 확률 및 생성을 관리하는 클래스
 */
class DropManager {
    private val random = Random()

    // 드롭 확률 (0.5% -> 0.005)
    // 추후 난이도나 아이템 등에 따라 변동 가능하도록 변수화
    var dropChance: Float = 1f

    /**
     * 현재 확률에 따라 드롭 여부를 결정합니다.
     */
    fun shouldDrop(): Boolean {
        return random.nextFloat() < dropChance
    }

    /**
     * 지정된 위치에 드롭 아이템을 생성하여 반환합니다.
     */
    fun createDropItem(x: Float, y: Float): DropItem {
        val weapons = WeaponType.values()
        // 무기 랜덤 선택
        val randomWeapon = weapons[random.nextInt(weapons.size)]
        // 기본적으로 NORMAL 등급 드롭 (추후 확률적으로 상위 등급 드롭 로직 추가 가능)
        val randomGrade = WeaponGrade.NORMAL

        return DropItem(x, y, randomWeapon, randomGrade)
    }
}