package com.donghwa.gameVerse.defensegame

class DropItem(
    var x: Float,
    var y: Float,
    val weaponType: WeaponType,
    val weaponGrade: WeaponGrade
) {
    val creationTime = System.currentTimeMillis()
    val lifeTime = 15000L // 자동 획득을 기다리기 위해 수명 연장 (15초)

    // [신규] 수거 모드 관련 변수
    var isCollecting = false
    var velocityX = 0f
    var velocityY = 0f

    fun isExpired(): Boolean {
        // 수거 중일 때는 사라지지 않음
        if (isCollecting) return false
        return System.currentTimeMillis() - creationTime > lifeTime
    }
}