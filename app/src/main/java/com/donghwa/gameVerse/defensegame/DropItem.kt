package com.donghwa.gameVerse.defensegame

class DropItem(
    var x: Float,
    var y: Float,
    val weaponType: WeaponType,
    val weaponGrade: WeaponGrade
) {
    val creationTime = System.currentTimeMillis()
    val lifeTime = 15000L // (참고용으로 남겨둠, 실제 만료 로직에는 사용 안 함)

    // [신규] 수거 모드 관련 변수
    var isCollecting = false
    var velocityX = 0f
    var velocityY = 0f

    fun isExpired(): Boolean {
        // [수정] 시간이 지나도 아이템이 사라지지 않도록 항상 false 반환
        return false
    }
}