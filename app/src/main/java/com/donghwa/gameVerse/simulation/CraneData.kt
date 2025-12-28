package com.donghwa.gameVerse.simulation

import kotlin.math.abs

/**
 * KATO SL-650R (KR-65H) 정밀 제원 데이터
 * - [Update] 아웃리거 폭 및 선회 각도에 따른 하중 감소 로직 추가
 */
object CraneData {
    // --- 1. 차량 물리적 제원 (단위: m, ton) ---
    const val BODY_LENGTH = 12.59f
    const val BODY_WIDTH = 2.99f
    const val BODY_HEIGHT = 3.68f

    const val HOOK_WEIGHT_65T = 0.47f
    const val HOOK_WEIGHT_25T = 0.33f

    // --- 2. 붐 제원 ---
    val BOOM_LENGTHS = listOf(10.0f, 16.9f, 23.8f, 30.7f, 37.6f, 41.5f, 44.5f)
    const val MIN_ANGLE = 0f
    const val MAX_ANGLE = 84f

    // --- 3. 아웃리거 모드 (단위: m) ---
    // 실제 제원표의 아웃리거 확장 단계
    val OUTRIGGER_MODES = listOf(7.6f, 7.2f, 6.5f, 5.4f, 4.3f, 2.69f)

    // --- 4. 위험 각도 ---
    private val CRITICAL_ANGLES = mapOf(
        30.7f to 28f, 37.6f to 30f, 41.5f to 34f, 44.5f to 47f
    )

    // --- 5. 정격 총하중표 (Base: 7.6m Outrigger) ---
    private val LOAD_CHART_BASE = mapOf(
        10.0f to listOf(2.6f to 65.0f, 3.0f to 60.0f, 4.0f to 49.2f, 5.0f to 39.5f, 6.0f to 32.5f, 7.0f to 26.8f, 8.0f to 22.0f),
        16.9f to listOf(3.0f to 32.0f, 4.5f to 32.0f, 6.0f to 30.5f, 7.0f to 26.0f, 9.0f to 17.5f, 12.0f to 9.7f),
        23.8f to listOf(3.5f to 23.0f, 6.0f to 22.0f, 8.0f to 17.2f, 10.0f to 13.9f, 14.0f to 6.75f, 20.0f to 2.45f),
        30.7f to listOf(5.0f to 12.5f, 8.0f to 12.0f, 10.0f to 10.9f, 14.0f to 7.8f, 20.0f to 3.4f, 26.0f to 1.25f),
        37.6f to listOf(6.0f to 12.0f, 10.0f to 12.0f, 14.0f to 7.7f, 20.0f to 4.1f, 28.0f to 1.55f),
        41.5f to listOf(7.0f to 10.0f, 10.0f to 10.0f, 14.0f to 7.2f, 20.0f to 4.2f, 30.0f to 1.3f),
        44.5f to listOf(8.0f to 8.0f, 12.0f to 8.0f, 16.0f to 5.4f, 22.0f to 3.1f, 32.0f to 0.85f)
    )

    /**
     * 안전 하중 계산 (선회 각도 반영)
     * * @param swingAngle 선회 각도 (-180 ~ 180). 0도가 정면(Front).
     * @param outriggerWidth 현재 아웃리거 확장 폭 (m)
     */
    fun getSafeLoad(boomLength: Float, radius: Float, currentAngle: Float, swingAngle: Float, outriggerWidth: Float): Float {
        // 1. 위험 각도 체크
        val criticalAngle = CRITICAL_ANGLES[boomLength] ?: 0f
        if (currentAngle < criticalAngle) return 0f

        // 2. 기본 하중표 조회 (7.6m 기준)
        val baseLoad = getBaseLoad(boomLength, radius)
        if (baseLoad == 0f) return 0f

        // 3. 선회 영역 판별
        // 절대값 기준: 0~30도(전방), 150~180도(후방), 그 외(측면)
        val absAngle = abs(swingAngle)
        val isFront = absAngle <= 30
        val isRear = absAngle >= 150
        val isSide = !isFront && !isRear

        // 4. 하중 감소 계수(Derating) 적용
        // 전/후방은 아웃리거 폭의 영향을 덜 받지만(잭 지지), 측면은 아웃리거 폭에 비례하여 급격히 감소
        var factor = 1.0f

        if (isSide) {
            // 아웃리거가 좁을수록 측면 하중 능력 감소
            // 7.6m일 때 100%, 2.69m일 때 약 20% 수준으로 가정 (물리적 모멘트 비율)
            factor = (outriggerWidth / 7.6f).coerceIn(0.2f, 1.0f)

            // 추가 보정: 붐이 길수록 측면 안정성 더 취약
            if (boomLength > 30f) factor *= 0.9f
        } else {
            // 전/후방 작업 시에도 아웃리거가 최소(2.69m)인 경우 약간의 제한
            if (outriggerWidth < 4.0f) factor = 0.8f
        }

        return baseLoad * factor
    }

    private fun getBaseLoad(boomLength: Float, radius: Float): Float {
        val chart = LOAD_CHART_BASE[boomLength] ?: return 0f
        if (radius <= chart.first().first) return chart.first().second
        if (radius > chart.last().first) return 0f

        for (i in 0 until chart.size - 1) {
            val (r1, load1) = chart[i]
            val (r2, load2) = chart[i+1]
            if (radius in r1..r2) {
                val ratio = (radius - r1) / (r2 - r1)
                return load1 + (load2 - load1) * ratio
            }
        }
        return 0f
    }
}