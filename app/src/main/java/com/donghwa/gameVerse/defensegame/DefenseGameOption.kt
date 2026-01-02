package com.donghwa.gameVerse.defensegame

import android.graphics.Color

// 옵션 등급 정의
enum class OptionGrade(val color: Int, val label: String) {
    NORMAL(Color.WHITE, "NORMAL"),
    MAGIC(Color.CYAN, "MAGIC"),
    RARE(Color.YELLOW, "RARE"),
    UNIQUE(Color.parseColor("#E040FB"), "UNIQUE"), // 보라색
    LEGEND(Color.RED, "LEGEND")
}

enum class DefenseGameOption(
    val title: String,
    val description: String,
    val grade: OptionGrade
) {
    // [NORMAL] - 소폭 상승
    NORMAL_ATK_UP("공격력 증가 (Normal)", "공격력이 1.1배 증가합니다.", OptionGrade.NORMAL),
    NORMAL_SPD_UP("공격 속도 증가 (Normal)", "공격 속도가 1.1배 빨라집니다.", OptionGrade.NORMAL),
    NORMAL_RANGE_UP("사거리 증가 (Normal)", "사거리가 1.1배 증가합니다.", OptionGrade.NORMAL),
    NORMAL_HP_UP("기지 수리 (Normal)", "기지 체력을 3 회복합니다.", OptionGrade.NORMAL),

    // [MAGIC] - 1.2배 / 크리 1.25배
    MAGIC_ATK_UP("공격력 증가 (Magic)", "공격력이 1.2배 증가합니다.", OptionGrade.MAGIC),
    MAGIC_SPD_UP("공격 속도 증가 (Magic)", "공격 속도가 1.2배 빨라집니다.", OptionGrade.MAGIC),
    MAGIC_CRIT_UP("치명타 피해 (Magic)", "치명타 데미지가 1.25배 증가합니다.", OptionGrade.MAGIC),

    // [RARE] - 1.3배
    RARE_ATK_UP("공격력 증가 (Rare)", "공격력이 1.3배 증가합니다.", OptionGrade.RARE),
    RARE_SPD_UP("공격 속도 증가 (Rare)", "공격 속도가 1.3배 빨라집니다.", OptionGrade.RARE),
    RARE_CRIT_CHANCE("치명타 확률 (Rare)", "치명타 확률이 10% 증가합니다.", OptionGrade.RARE),

    // [UNIQUE] - 1.4배 / 크리 1.5배
    UNIQUE_ATK_UP("공격력 증가 (Unique)", "공격력이 1.4배 증가합니다.", OptionGrade.UNIQUE),
    UNIQUE_SPD_UP("공격 속도 증가 (Unique)", "공격 속도가 1.4배 빨라집니다.", OptionGrade.UNIQUE),
    UNIQUE_CRIT_DMG("치명타 피해 (Unique)", "치명타 데미지가 1.5배 증가합니다.", OptionGrade.UNIQUE),

    // [LEGEND] - 특수 능력
    LEGEND_MULTI_SHOT("멀티 샷 (Legend)", "한 번에 2발의 투사체를\n동시에 발사합니다.", OptionGrade.LEGEND),
    LEGEND_DOUBLE_SHOT("더블 샷 (Legend)", "공격 시 아주 짧은 간격으로\n한 발 더 발사합니다. (연사)", OptionGrade.LEGEND),
    LEGEND_RICOCHET("도탄 사격 (Legend)", "투사체가 적중 후 주변 적에게\n3회 튕깁니다.", OptionGrade.LEGEND);

    companion object {
        // 가중치 랜덤 뽑기 (3개 반환)
        fun getRandomOptions(count: Int): List<DefenseGameOption> {
            val allOptions = values().toList()
            val selected = mutableListOf<DefenseGameOption>()

            // 중복 방지를 위한 풀 복사
            val pool = ArrayList(allOptions)

            repeat(count) {
                if (pool.isNotEmpty()) {
                    // 단순 랜덤 대신 등급별 확률을 적용할 수도 있으나,
                    // 여기서는 전체 풀에서 랜덤하게 뽑되, 이미 뽑힌건 제외
                    val pick = pool.random()
                    selected.add(pick)
                    pool.remove(pick)
                }
            }
            // 등급 순(LEGEND 위)으로 정렬해서 보여주기
            return selected.sortedByDescending { it.grade }
        }
    }
}