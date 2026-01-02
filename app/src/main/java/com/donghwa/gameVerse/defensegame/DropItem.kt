package com.donghwa.gameVerse.defensegame

enum class DropType {
    WEAPON, DONG
}

class DropItem(
    var x: Float,
    var y: Float,
    val type: DropType,
    // WEAPON 타입일 때 사용
    val weaponType: WeaponType? = null,
    val weaponGrade: WeaponGrade = WeaponGrade.NORMAL,
    // DONG 타입일 때 사용
    val amount: Int = 0
) {
    val creationTime = System.currentTimeMillis()

    // 수거 모드 관련 변수
    var isCollecting = false
    var velocityX = 0f
    var velocityY = 0f

    fun isExpired(): Boolean {
        return false
    }
}