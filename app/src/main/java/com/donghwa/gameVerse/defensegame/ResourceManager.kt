package com.donghwa.gameVerse.defensegame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.donghwa.gameVerse.R // 프로젝트의 R 클래스 import

object ResourceManager {
    private val charBitmaps = HashMap<WeaponType, Bitmap>()

    // 격자 크기 (DefenseGameLogic.kt의 gridSize와 동일해야 함)
    private const val GRID_SIZE = 400 // 400px (격자 크기 변경)

    fun init(context: Context) {
        // [수정] 실제 이미지 리소스 로드
        // drawable 폴더에 해당 이름의 png 파일이 있어야 합니다.
        // 파일이 없으면 앱이 실행되다가 꺼질 수 있으므로, 이미지를 넣은 후 주석을 해제하세요.

        try {
            // 예시: char_smg.png 파일이 있다면 아래 주석 해제 및 크기 조정 추가

            loadAndScaleBitmap(context, WeaponType.SMG, R.drawable.char_smg)
            loadAndScaleBitmap(context, WeaponType.SHOTGUN, R.drawable.char_shotgun)
            loadAndScaleBitmap(context, WeaponType.SNIPER, R.drawable.char_sniper)
            loadAndScaleBitmap(context, WeaponType.MISSILE, R.drawable.char_missile)
            loadAndScaleBitmap(context, WeaponType.BOW, R.drawable.char_bow)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 이미지가 없거나 로드 실패 시, 임시 이미지를 생성하여 사용 (안전 장치)
        for (type in WeaponType.values()) {
            if (!charBitmaps.containsKey(type) || charBitmaps[type] == null) {
                charBitmaps[type] = createPlaceholderBitmap(type)
            }
        }
    }

    // 이미지를 로드하고 격자 크기에 맞춰 조정하는 함수
    private fun loadAndScaleBitmap(context: Context, type: WeaponType, resourceId: Int) {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)
        if (originalBitmap != null) {
            // 격자 크기에 맞게 스케일링 (약간의 여백을 위해 0.8~0.9배로 설정 가능)
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, GRID_SIZE, GRID_SIZE, true)
            charBitmaps[type] = scaledBitmap
        }
    }

    fun getCharacterBitmap(type: WeaponType): Bitmap? {
        return charBitmaps[type]
    }

    // 이미지가 없을 때 대신 보여줄 임시 그래픽 생성 함수
    private fun createPlaceholderBitmap(type: WeaponType): Bitmap {
        val size = GRID_SIZE // 격자 크기 사용
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // 배경 투명
        canvas.drawColor(Color.TRANSPARENT)

        // 1. 캐릭터 몸통
        paint.color = when (type) {
            WeaponType.SMG -> Color.YELLOW
            WeaponType.SHOTGUN -> Color.rgb(139, 69, 19) // Brown
            WeaponType.SNIPER -> Color.rgb(34, 139, 34) // Forest Green
            WeaponType.MISSILE -> Color.rgb(75, 0, 130) // Indigo
            WeaponType.BOW -> Color.LTGRAY
        }
        canvas.drawCircle(size / 2f, size / 2f, 30f, paint)

        // 2. 무기 (간단한 선으로 표현)
        paint.color = Color.DKGRAY
        paint.strokeWidth = 10f
        when (type) {
            WeaponType.SMG -> canvas.drawLine(size / 2f, size / 2f + 10, size.toFloat(), size / 2f + 10, paint)
            WeaponType.SHOTGUN -> {
                paint.strokeWidth = 15f
                canvas.drawLine(size / 2f, size / 2f + 10, size * 0.8f, size / 2f + 10, paint)
            }
            WeaponType.SNIPER -> {
                paint.strokeWidth = 5f
                canvas.drawLine(size / 2f, size / 2f + 10, size.toFloat() + 20, size / 2f + 10, paint)
            }
            WeaponType.MISSILE -> {
                paint.strokeWidth = 20f
                canvas.drawLine(size / 2f, size / 2f + 10, size * 0.9f, size / 2f + 10, paint)
            }
            WeaponType.BOW -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawArc(size / 2f, size / 2f - 20, size * 0.9f, size / 2f + 40, -90f, 180f, false, paint)
            }
        }

        return bitmap
    }
}