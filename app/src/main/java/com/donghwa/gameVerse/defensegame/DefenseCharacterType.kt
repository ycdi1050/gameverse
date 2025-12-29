package com.donghwa.gameVerse.defensegame

enum class DefenseCharacterType {
    HUMAN,
    ROBOT,
    ALIEN;

    fun getDisplayName(): String {
        return when (this) {
            HUMAN -> "👨‍🚀 휴먼 (Human)"
            ROBOT -> "🤖 로봇 (Robot)"
            ALIEN -> "👽 에일리언 (Alien)"
        }
    }

    fun getDescription(): String {
        return when (this) {
            HUMAN -> "밸런스형: 표준적인 능력치를 가집니다."
            ROBOT -> "공격형: 공격 속도가 10% 더 빠릅니다."
            ALIEN -> "범위형: 사거리가 10% 더 깁니다."
        }
    }
}