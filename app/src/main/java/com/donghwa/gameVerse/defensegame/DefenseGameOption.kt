package com.donghwa.gameVerse.defensegame

enum class DefenseGameOption(
    val title: String,
    val description: String
) {
    ATK_SPEED_UP("공격 속도 증가", "공격 속도가 1.25배 빨라집니다."),
    ATK_DAMAGE_UP("공격력 증가", "공격력이 1.3배 증가합니다."),
    DOUBLE_SHOT("더블 샷", "투사체가 2개 발사되지만,\n공격력이 80%로 감소합니다."),
    ENEMY_SLOW("적 이동 속도 감소", "적의 이동 속도가 20% 느려집니다."),
    RANGE_UP("사거리 증가", "공격 사거리가 1.2배 증가합니다."),
    CRIT_CHANCE_UP("치명타 확률 증가", "치명타 확률이 15% 증가합니다."),
    CRIT_DAMAGE_UP("치명타 피해 증가", "치명타 피해량이 50% 증가합니다."),
    PROJ_SPEED_UP("투사체 속도 증가", "투사체 비행 속도가 1.5배 빨라집니다."),
    MAX_HP_UP("최대 체력 증가", "기지 체력이 2 증가합니다."),
    INSTANT_REPAIR("긴급 수리", "기지 체력을 5 회복합니다."),
    // [신규] 도탄 옵션 추가
    RICOCHET("도탄 사격 (Ricochet)", "투사체가 적중 시 주변 적에게\n튕깁니다. (최대 3회)\n튕길 때마다 데미지 30% 감소.");

    companion object {
        fun getRandomOptions(count: Int): List<DefenseGameOption> {
            return values().toList().shuffled().take(count)
        }
    }
}