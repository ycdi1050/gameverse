package com.donghwa.gameVerse.defensegame

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.sqrt

class DefenseGameLogic(private val state: DefenseGameState) {

    fun generatePath(w: Int, h: Int) {
        state.path.clear()
        val gs = state.gridSize
        state.path.add(PointF(gs/2, 0f))
        state.path.add(PointF(gs/2, h * 0.2f))
        state.path.add(PointF(w - gs/2, h * 0.2f))
        state.path.add(PointF(w - gs/2, h * 0.4f))
        state.path.add(PointF(gs/2, h * 0.4f))
        state.path.add(PointF(gs/2, h * 0.6f))
        state.path.add(PointF(w - gs/2, h * 0.6f))
        state.path.add(PointF(w - gs/2, h * 0.8f))
        state.path.add(PointF(w / 2f, h * 0.8f))
        state.path.add(PointF(w / 2f, h.toFloat()))
    }

    fun initGrid(w: Int, h: Int) {
        state.cols = (w / state.gridSize).toInt()
        state.rows = (h / state.gridSize).toInt()
        state.gridState = Array(state.cols) { BooleanArray(state.rows) { true } }
        markPathAsUnbuildable()
    }

    private fun markPathAsUnbuildable() {
        for (i in 0 until state.cols) {
            for (j in 0 until state.rows) state.gridState[i][j] = true
        }
        for (k in 0 until state.path.size - 1) {
            val p1 = state.path[k]
            val p2 = state.path[k+1]
            for (i in 0 until state.cols) {
                for (j in 0 until state.rows) {
                    val cx = i * state.gridSize + state.gridSize / 2
                    val cy = j * state.gridSize + state.gridSize / 2
                    if (isPointNearSegment(cx, cy, p1.x, p1.y, p2.x, p2.y, state.gridSize * 0.8f)) {
                        state.gridState[i][j] = false
                    }
                }
            }
        }
        for (i in 0 until state.cols) {
            state.gridState[i][0] = false
            state.gridState[i][state.rows - 1] = false
            if (state.rows > 1) state.gridState[i][1] = false
        }
    }

    private fun isPointNearSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float, dist: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return false
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val nearestX = if (t < 0) x1 else if (t > 1) x2 else x1 + t * dx
        val nearestY = if (t < 0) y1 else if (t > 1) y2 else y1 + t * dy
        val distSq = (px - nearestX) * (px - nearestX) + (py - nearestY) * (py - nearestY)
        return distSq < dist * dist
    }

    fun update(onStageClear: () -> Unit, onGameOver: () -> Unit) {
        val now = System.currentTimeMillis()

        // 적 스폰
        if (state.spawnedEnemies < state.requiredKills + 5) {
            if (now - state.lastSpawnTime > state.spawnInterval) {
                state.enemies.add(Enemy(state.path, state.enemyHp, state.enemyDefense, 3f + (state.stage * 0.5f), state.stage))
                state.lastSpawnTime = now
                state.spawnedEnemies++
            }
        } else if (state.enemies.isEmpty() && state.killsInCurrentStage < state.requiredKills) {
            state.enemies.add(Enemy(state.path, state.enemyHp, state.enemyDefense, 3f + (state.stage * 0.5f), state.stage))
            state.lastSpawnTime = now
            state.spawnedEnemies++
        }

        // 캐릭터 자동 공격
        for (char in state.characters) {
            char.autoAttack(state.enemies, now, state.globalDamageMultiplier)?.let {
                state.projectiles.add(it)
            }
        }

        // 투사체 처리
        val projToRemove = ArrayList<Projectile>()
        for (p in state.projectiles) {
            val oldHit = p.hasHit
            p.update()
            if (p.hasHit && !oldHit) {
                handleProjectileHit(p)
            }
            if (p.hasHit) projToRemove.add(p)
        }
        state.projectiles.removeAll(projToRemove)

        // 적 업데이트
        val enemiesToRemove = ArrayList<Enemy>()
        for (e in state.enemies) {
            e.update()
            if (e.reachedEnd) {
                state.lives--
                enemiesToRemove.add(e)
                if (state.lives <= 0) {
                    state.isGameOver = true
                    state.isRunning = false
                    onGameOver()
                }
            } else if (e.isDead) {
                enemiesToRemove.add(e)
            }
        }
        state.enemies.removeAll(enemiesToRemove)
    }

    private fun handleProjectileHit(p: Projectile) {
        val hitEnemies = ArrayList<Enemy>()
        val splashRadius = if (p.weaponType == WeaponType.SHOTGUN) 150f else 0f

        for (e in state.enemies) {
            if (e.isDead || e.reachedEnd) continue
            val dx = e.x - p.x
            val dy = e.y - p.y
            val dist = sqrt((dx * dx + dy * dy).toDouble())
            if (dist < e.radius + 20f || (splashRadius > 0 && dist < splashRadius)) {
                hitEnemies.add(e)
            }
        }

        for (e in hitEnemies) {
            var dmg = p.baseDamage
            if (p.weaponType == WeaponType.MISSILE) {
                val travelDist = sqrt(((p.x - p.originX) * (p.x - p.originX) + (p.y - p.originY) * (p.y - p.originY)).toDouble()).toFloat()
                dmg += (travelDist / 20).toInt()
            }
            if (p.weaponType == WeaponType.BOW) {
                e.applyPoison(damage = 5, durationSeconds = 5)
            }

            if (e.takeDamage(dmg)) {
                state.score += 10
                state.currentPoints += 10 + state.stage
                state.killsInCurrentStage++
                if (state.killsInCurrentStage >= state.requiredKills) {
                    state.isStageClear = true
                    state.isRunning = false
                }
            }
        }
    }

    fun upgradeDamage(): Boolean {
        if (state.currentPoints >= state.upgradeCost) {
            state.currentPoints -= state.upgradeCost
            state.globalDamageMultiplier += 0.2f
            state.upgradeCost += 50
            return true
        }
        return false
    }

    fun handleTouchEvent(action: Int, x: Float, y: Float): Boolean {
        val col = (x / state.gridSize).toInt()
        val row = (y / state.gridSize).toInt()
        val isValidGrid = col in 0 until state.cols && row in 0 until state.rows

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (isValidGrid) {
                    val centerX = col * state.gridSize + state.gridSize / 2
                    val centerY = row * state.gridSize + state.gridSize / 2
                    val clickedChar = state.characters.find { Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                    if (clickedChar != null) {
                        state.draggingCharacter = clickedChar
                        state.dragStartPos = PointF(clickedChar.x, clickedChar.y)
                        state.selectedTile = PointF(x, y)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                state.draggingCharacter?.let {
                    it.setPosition(x, y)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (state.draggingCharacter != null) {
                    val char = state.draggingCharacter!!
                    if (isValidGrid && state.gridState[col][row]) {
                        val centerX = col * state.gridSize + state.gridSize / 2
                        val centerY = row * state.gridSize + state.gridSize / 2
                        val targetChar = state.characters.find { it != char && Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                        if (targetChar != null) {
                            // [수정] 합칠 때 캐릭터 타입도 같은지 확인
                            if (targetChar.level == char.level &&
                                targetChar.weaponType == char.weaponType &&
                                targetChar.characterType == char.characterType) {
                                targetChar.upgrade()
                                state.characters.remove(char)
                                state.selectedTile = null
                            } else {
                                state.dragStartPos?.let { char.setPosition(it.x, it.y) }
                            }
                        } else {
                            char.setPosition(centerX, centerY)
                            state.selectedTile = PointF(x, y)
                        }
                    } else {
                        state.dragStartPos?.let { char.setPosition(it.x, it.y) }
                    }
                    state.draggingCharacter = null
                    state.dragStartPos = null
                    return true
                } else {
                    if (isValidGrid && state.gridState[col][row]) {
                        val centerX = col * state.gridSize + state.gridSize / 2
                        val centerY = row * state.gridSize + state.gridSize / 2
                        val existing = state.characters.find { Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                        if (existing == null) {
                            val buildCost = 50
                            if (state.currentPoints >= buildCost) {
                                // [수정] 캐릭터 생성 시 선택된 캐릭터 타입(스킨)과 무기 적용
                                state.characters.add(Character(centerX, centerY, state.selectedWeapon, state.selectedCharacterType))
                                state.currentPoints -= buildCost
                                state.selectedTile = PointF(x, y)
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }
}