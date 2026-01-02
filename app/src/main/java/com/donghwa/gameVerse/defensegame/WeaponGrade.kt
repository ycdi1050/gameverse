package com.donghwa.gameVerse.defensegame

import android.graphics.Color

enum class WeaponGrade {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE,
    LEGEND;

    fun getDisplayName(): String {
        return when (this) {
            NORMAL -> "노말 (Normal)"
            MAGIC -> "매직 (Magic)"
            RARE -> "레어 (Rare)"
            UNIQUE -> "유니크 (Unique)"
            LEGEND -> "레전드 (Legend)"
        }
    }

    fun getColor(): Int {
        return when (this) {
            NORMAL -> Color.WHITE
            MAGIC -> Color.CYAN
            RARE -> Color.YELLOW
            UNIQUE -> Color.parseColor("#E040FB") // 보라색
            LEGEND -> Color.RED
        }
    }

    fun getDamageMultiplier(): Float {
        return when (this) {
            NORMAL -> 1.0f
            MAGIC -> 1.2f
            RARE -> 1.5f
            UNIQUE -> 2.0f
            LEGEND -> 3.5f
        }
    }

    // 다음 등급 반환 (없으면 null)
    fun getNextGrade(): WeaponGrade? {
        return when (this) {
            NORMAL -> MAGIC
            MAGIC -> RARE
            RARE -> UNIQUE
            UNIQUE -> LEGEND
            LEGEND -> null
        }
    }

    // 다음 등급으로 가기 위해 필요한 현재 등급 아이템 개수
    fun getUpgradeCost(): Int {
        return when (this) {
            NORMAL -> 3 // 3개 합치면 매직
            MAGIC -> 3  // 3개 합치면 레어
            RARE -> 3   // 3개 합치면 유니크
            UNIQUE -> 2 // 2개 합치면 레전드
            LEGEND -> 0
        }
    }
}