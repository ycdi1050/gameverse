package com.donghwa.gameVerse.defensegame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.donghwa.gameVerse.R

object ResourceManager {
    // 키: "GRADE_TYPE" (예: "NORMAL_BOW")
    private val weaponBitmaps = HashMap<String, Bitmap>()
    val uiBitmaps = HashMap<String, Bitmap>()

    // 유닛(캐릭터) 이미지 저장소
    val unitBitmaps = HashMap<DefenseCharacterType, Bitmap>()

    // [신규] 캐릭터+무기 결합 이미지 저장소 (키: "CHAR_WEAPON", 예: "POTATO_BOW")
    val combinedBitmaps = HashMap<String, Bitmap>()

    private const val GRID_SIZE = 400

    fun init(context: Context) {
        // 1. 모든 무기 타입과 등급 조합에 대해 이미지 로드 시도
        for (type in WeaponType.values()) {
            for (grade in WeaponGrade.values()) {
                val key = makeKey(type, grade)
                val resName = "${grade.name.lowercase()}_${type.name.lowercase()}"

                var bitmap = loadBitmapSafe(context, resName)

                if (bitmap == null && grade != WeaponGrade.NORMAL) {
                    val fallbackName = "normal_${type.name.lowercase()}"
                    bitmap = loadBitmapSafe(context, fallbackName)
                }

                if (bitmap == null) {
                    val legacyName = "char_${type.name.lowercase()}"
                    bitmap = loadBitmapSafe(context, legacyName)
                }

                if (bitmap != null) {
                    weaponBitmaps[key] = scaleBitmap(bitmap)
                } else {
                    weaponBitmaps[key] = createPlaceholderBitmap(type, grade)
                }
            }
        }

        // 2. UI 이미지 로드
        loadBitmapSafe(context, "ui_pause")?.let { uiBitmaps["ui_pause"] = it }
        loadBitmapSafe(context, "ui_play")?.let { uiBitmaps["ui_play"] = it }
        loadBitmapSafe(context, "ui_speed_1")?.let { uiBitmaps["ui_speed_1"] = it }
        loadBitmapSafe(context, "ui_speed_2")?.let { uiBitmaps["ui_speed_2"] = it }
        loadBitmapSafe(context, "ui_speed_3")?.let { uiBitmaps["ui_speed_3"] = it }

        // 3. 유닛(캐릭터) 이미지 로드
        // POTATO -> potato_basic.png 사용
        loadBitmapSafe(context, "potato_basic")?.let { unitBitmaps[DefenseCharacterType.POTATO] = scaleBitmap(it) }
        loadBitmapSafe(context, "unit_robot")?.let { unitBitmaps[DefenseCharacterType.ROBOT] = scaleBitmap(it) }
        loadBitmapSafe(context, "unit_alien")?.let { unitBitmaps[DefenseCharacterType.ALIEN] = scaleBitmap(it) }

        // 4. [신규] 캐릭터+무기 결합 이미지 로드
        // 모든 캐릭터 타입과 무기 타입의 조합에 대해 리소스 로드 시도
        // 예: potato_bow.png, robot_smg.png 등
        for (charType in DefenseCharacterType.values()) {
            for (weaponType in WeaponType.values()) {
                val key = "${charType.name}_${weaponType.name}" // 예: POTATO_BOW
                // 리소스 이름 규칙: potato_bow, robot_smg (모두 소문자)
                // 만약 characterType이 POTATO이고 weaponType이 BOW라면 "potato_bow"를 찾음
                val charName = if (charType == DefenseCharacterType.POTATO) "potato" else "unit_${charType.name.lowercase()}"
                // 위 규칙이 복잡하다면 단순화: type.name.lowercase() 사용
                // 여기서는 "potato_bow" 같은 이름을 가정하므로 아래와 같이 처리

                // 단순화된 규칙 사용: {캐릭터타입소문자}_{무기타입소문자}
                // 단, POTATO는 "potato", 나머지는 ENUM 이름 그대로 사용한다고 가정할 경우:
                val prefix = if (charType == DefenseCharacterType.POTATO) "potato" else charType.name.lowercase()
                val resName = "${prefix}_${weaponType.name.lowercase()}"

                loadBitmapSafe(context, resName)?.let {
                    combinedBitmaps[key] = scaleBitmap(it)
                }
            }
        }
    }

    // 등급과 타입을 받아서 해당 비트맵 반환
    fun getWeaponBitmap(type: WeaponType, grade: WeaponGrade): Bitmap? {
        return weaponBitmaps[makeKey(type, grade)]
    }

    fun getUIBitmap(name: String): Bitmap? {
        return uiBitmaps[name]
    }

    // 유닛 비트맵 반환
    fun getUnitBitmap(type: DefenseCharacterType): Bitmap? {
        return unitBitmaps[type]
    }

    // [신규] 결합된 비트맵 반환
    fun getCombinedBitmap(charType: DefenseCharacterType, weaponType: WeaponType): Bitmap? {
        return combinedBitmaps["${charType.name}_${weaponType.name}"]
    }

    private fun makeKey(type: WeaponType, grade: WeaponGrade): String {
        return "${grade.name}_${type.name}"
    }

    private fun loadBitmapSafe(context: Context, name: String): Bitmap? {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) {
            return BitmapFactory.decodeResource(context.resources, resId)
        }
        return null
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, GRID_SIZE, GRID_SIZE, true)
    }

    private fun createPlaceholderBitmap(type: WeaponType, grade: WeaponGrade): Bitmap {
        val size = GRID_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        canvas.drawColor(Color.TRANSPARENT)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = grade.getColor()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 10, paint)

        paint.style = Paint.Style.FILL
        paint.color = when (type) {
            WeaponType.SMG -> Color.YELLOW
            WeaponType.SHOTGUN -> Color.rgb(139, 69, 19)
            WeaponType.SNIPER -> Color.rgb(34, 139, 34)
            WeaponType.MISSILE -> Color.rgb(75, 0, 130)
            WeaponType.BOW -> Color.LTGRAY
        }
        canvas.drawCircle(size / 2f, size / 2f, 30f, paint)

        paint.color = Color.DKGRAY
        paint.strokeWidth = 10f
        when (type) {
            WeaponType.SMG -> canvas.drawLine(size / 2f, size / 2f, size.toFloat(), size / 2f, paint)
            WeaponType.SHOTGUN -> {
                paint.strokeWidth = 15f
                canvas.drawLine(size / 2f, size / 2f, size * 0.8f, size / 2f, paint)
            }
            WeaponType.BOW -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                canvas.drawArc(size/2f - 40, size/2f - 60, size/2f + 40, size/2f + 60, 270f, 180f, false, paint)
            }
            else -> canvas.drawLine(size / 2f, size / 2f, size.toFloat(), size / 2f, paint)
        }
        return bitmap
    }
}