package com.donghwa.gameVerse.item

enum class ItemType {
    WEAPON, RING, NECKLACE
}

data class EquipItem(
    val id: String,
    val name: String,
    val type: ItemType,
    val statBonus: Int, // 공격력 증가량 (%)
    val price: Int = 0,
    val description: String = ""
) {
    companion object {
        // 프리셋 아이템 목록
        val WEAPONS = listOf(
            EquipItem("w_001", "기본 검", ItemType.WEAPON, 0, 0, "기본적인 검입니다."),
            EquipItem("w_002", "철제 검", ItemType.WEAPON, 10, 100, "공격력이 10% 증가합니다."),
            EquipItem("w_003", "황금 검", ItemType.WEAPON, 30, 500, "공격력이 30% 증가합니다."),
            EquipItem("w_004", "다이아 검", ItemType.WEAPON, 50, 1000, "공격력이 50% 증가합니다.")
        )

        val RINGS = listOf(
            EquipItem("r_001", "나무 반지", ItemType.RING, 0, 0, "장식용 반지입니다."),
            EquipItem("r_002", "루비 반지", ItemType.RING, 5, 200, "공격력이 5% 증가합니다."),
            EquipItem("r_003", "전설의 반지", ItemType.RING, 15, 1000, "공격력이 15% 증가합니다.")
        )

        val NECKLACES = listOf(
            EquipItem("n_001", "낡은 목걸이", ItemType.NECKLACE, 0, 0, "평범한 목걸이입니다."),
            EquipItem("n_002", "사파이어 목걸이", ItemType.NECKLACE, 5, 200, "공격력이 5% 증가합니다."),
            EquipItem("n_003", "용의 목걸이", ItemType.NECKLACE, 20, 1500, "공격력이 20% 증가합니다.")
        )

        fun getById(id: String): EquipItem? {
            return (WEAPONS + RINGS + NECKLACES).find { it.id == id }
        }
    }
}