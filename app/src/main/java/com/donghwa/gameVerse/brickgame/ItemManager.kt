package com.donghwa.gameVerse

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList

// [통합] 아이템 데이터 클래스
// type - 0: 멀티볼, 1: 패들 확대, 2: 패들 축소, 3: 폭탄, 4: 무적볼, 5: 거대볼, 6: 목숨추가
data class Item(
    var x: Float,
    var y: Float,
    val type: Int,
    val width: Float = 40f,
    val height: Float = 40f
)

// 아이템 관리 클래스
class ItemManager(private val screenX: Int, private val screenY: Int) {
    val items = CopyOnWriteArrayList<Item>()
    private val random = Random()

    fun clear() {
        items.clear()
    }

    fun spawnItem(x: Float, y: Float, level: Int) {
        if (random.nextFloat() < 0.4f) {
            var type = random.nextInt(3) // 0, 1, 2
            val rand = random.nextFloat()

            if (level >= 2 && rand < 0.3f) type = 3
            else if (rand < 0.35f) type = 4
            else if (rand < 0.45f) type = 5
            else if (rand < 0.50f) type = 6

            items.add(Item(x, y, type))
        }
    }

    fun update() {
        for (item in items) {
            item.y += 12f
            if (item.y > screenY) {
                items.remove(item)
            }
        }
    }

    fun checkCollision(paddleRect: RectF, onConsume: (Int) -> Unit) {
        for (item in items) {
            val itemRect = RectF(item.x - item.width/2, item.y - item.height/2, item.x + item.width/2, item.y + item.height/2)
            if (RectF.intersects(paddleRect, itemRect)) {
                onConsume(item.type)
                items.remove(item)
            }
        }
    }

    fun draw(canvas: Canvas, paint: Paint) {
        for (item in items) {
            paint.color = when (item.type) {
                0 -> Color.GREEN
                1 -> Color.CYAN
                2 -> Color.RED
                3 -> Color.DKGRAY
                4 -> Color.parseColor("#FFC107")
                5 -> Color.parseColor("#9C27B0")
                6 -> Color.parseColor("#E91E63")
                else -> Color.WHITE
            }

            if (item.type == 6) {
                paint.textSize = 50f
                paint.textAlign = Paint.Align.CENTER
                val baseline = item.y - (paint.descent() + paint.ascent()) / 2
                canvas.drawText("♥", item.x, baseline, paint)
            } else {
                val itemRect = RectF(item.x - item.width/2, item.y - item.height/2, item.x + item.width/2, item.y + item.height/2)
                canvas.drawRoundRect(itemRect, 5f, 5f, paint)

                if (item.type >= 3 && item.type != 6) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    paint.color = if(item.type==3) Color.RED else Color.WHITE
                    canvas.drawRoundRect(itemRect, 5f, 5f, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }
    }
}