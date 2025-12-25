package com.donghwa.gameVerse.runnergame

import kotlin.math.min

class DifficultyManager {
    // 기본 속도
    var speed = 15f
    // 장애물 생성 간격 범위 (프레임)
    var spawnIntervalRange = 60..120

    // 현재 레벨 (1부터 시작)
    var currentLevel = 1
        private set

    // 레벨업 알림을 위한 변수 (View에서 확인 후 처리)
    var isLevelUpEffectActive = false
    private var levelUpEffectTimer = 0

    fun reset() {
        speed = 15f
        spawnIntervalRange = 60..120
        currentLevel = 1
        isLevelUpEffectActive = false
        levelUpEffectTimer = 0
    }

    fun update(score: Int) {
        // 1000점마다 레벨 증가 (0~999: Lv1, 1000~1999: Lv2 ...)
        val newLevel = (score / 500) + 1

        if (newLevel > currentLevel) {
            currentLevel = newLevel
            triggerLevelUp()
        }

        // 레벨업 효과 타이머 관리 (약 2초간 유지 - 60프레임 기준 120)
        if (isLevelUpEffectActive) {
            levelUpEffectTimer++
            if (levelUpEffectTimer > 120) {
                isLevelUpEffectActive = false
                levelUpEffectTimer = 0
            }
        }
    }

    private fun triggerLevelUp() {
        isLevelUpEffectActive = true
        levelUpEffectTimer = 0

        // 레벨당 속도 3씩 증가 (최대 35까지)
        speed = min(70f, 15f + (currentLevel - 1) * 7f)

        // 생성 간격 좁히기 (더 자주 나옴, 최소 30프레임)
        val minInterval = kotlin.math.max(30, 60 - (currentLevel * 5))
        val maxInterval = kotlin.math.max(50, 120 - (currentLevel * 8))
        spawnIntervalRange = minInterval..maxInterval
    }
}