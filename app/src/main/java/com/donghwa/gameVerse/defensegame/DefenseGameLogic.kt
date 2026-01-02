package com.donghwa.gameVerse.defensegame

import android.graphics.PointF
import android.view.MotionEvent
import java.util.Random
import java.util.ArrayList
import java.util.HashSet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DefenseGameLogic(private val state: DefenseGameState) {

    private val random = Random()
    private val dropManager = DropManager()

    private var lastOptionSelectTime: Long = 0

    fun generatePath(w: Int, h: Int) {
        state.mapWidth = w
        state.mapHeight = h
        state.path.clear()
        val gs = state.gridSize

        val leftX = gs * 1.5f
        val rightX = w - gs * 1.5f

        state.path.add(PointF(leftX, 0f))
        state.path.add(PointF(leftX, h * 0.2f))
        state.path.add(PointF(rightX, h * 0.2f))
        state.path.add(PointF(rightX, h * 0.4f))
        state.path.add(PointF(leftX, h * 0.4f))
        state.path.add(PointF(leftX, h * 0.6f))
        state.path.add(PointF(rightX, h * 0.6f))
        state.path.add(PointF(rightX, h * 0.8f))
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
        val grid = state.gridState ?: return
        for (i in 0 until state.cols) {
            for (j in 0 until state.rows) grid[i][j] = true
        }
        for (k in 0 until state.path.size - 1) {
            val p1 = state.path[k]
            val p2 = state.path[k+1]
            for (i in 0 until state.cols) {
                for (j in 0 until state.rows) {
                    val cx = i * state.gridSize + state.gridSize / 2
                    val cy = j * state.gridSize + state.gridSize / 2
                    if (isPointNearSegment(cx, cy, p1.x, p1.y, p2.x, p2.y, state.gridSize * 0.8f)) {
                        grid[i][j] = false
                    }
                }
            }
        }
        for (i in 0 until state.cols) {
            grid[i][0] = false
            grid[i][state.rows - 1] = false
            if (state.rows > 1) grid[i][1] = false
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

    fun update(onStageClear: () -> Unit, onGameOver: () -> Unit, onItemCollected: (DropItem) -> Unit) {
        if (state.isCollectingItems) {
            updateCollectionPhase(onStageClear, onGameOver, onItemCollected)
            return
        }

        if (state.isOptionSelection) {
            return
        }

        val now = System.currentTimeMillis()
        val currentSpeed = state.gameSpeed

        // [수정] 스폰 간격에 배속 적용
        val effectiveSpawnInterval = state.spawnInterval / currentSpeed

        if (state.spawnedEnemies < state.requiredKills + 5) {
            if (now - state.lastSpawnTime > effectiveSpawnInterval) {
                var enemySpeed = 1.2f + (state.stage * 0.3f) + (state.currentWave * 0.1f)
                enemySpeed *= state.buffEnemySpeed

                state.enemies.add(Enemy(state.path, state.enemyHp, state.enemyDefense, enemySpeed, state.stage))
                state.lastSpawnTime = now
                state.spawnedEnemies++
            }
        } else if (state.enemies.isEmpty() && state.killsInCurrentStage < state.requiredKills) {
            var enemySpeed = 1.2f + (state.stage * 0.3f) + (state.currentWave * 0.1f)
            enemySpeed *= state.buffEnemySpeed

            state.enemies.add(Enemy(state.path, state.enemyHp, state.enemyDefense, enemySpeed, state.stage))
            state.lastSpawnTime = now
            state.spawnedEnemies++
        }

        // [수정] 캐릭터 공격 로직에 배속 전달
        for (char in state.characters) {
            val mainProj = char.autoAttack(state.enemies, now, state, currentSpeed)
            if (mainProj != null) {
                mainProj.speed *= state.buffProjSpeed

                val shotsToFire = ArrayList<Projectile>()

                if (state.isMultiShot) {
                    val target = mainProj.target
                    if (target != null) {
                        val baseAngle = atan2((target.y - mainProj.y).toDouble(), (target.x - mainProj.x).toDouble())
                        val p1 = createCloneProjectile(mainProj, baseAngle - 0.5)
                        val p2 = createCloneProjectile(mainProj, baseAngle + 0.5)
                        shotsToFire.add(p1)
                        shotsToFire.add(p2)
                    } else {
                        shotsToFire.add(mainProj)
                    }
                } else {
                    shotsToFire.add(mainProj)
                }

                if (state.isDoubleShot) {
                    val doubleShots = ArrayList<Projectile>()
                    for (p in shotsToFire) {
                        val currentTarget = p.target
                        val angle = if (currentTarget != null) {
                            atan2((currentTarget.y - p.y).toDouble(), (currentTarget.x - p.x).toDouble())
                        } else {
                            0.0
                        }
                        val offsetX = -(cos(angle) * 30f).toFloat()
                        val offsetY = -(sin(angle) * 30f).toFloat()
                        val clone = createCloneProjectile(p, angle)
                        clone.x += offsetX
                        clone.y += offsetY
                        doubleShots.add(clone)
                    }
                    shotsToFire.addAll(doubleShots)
                }

                state.projectiles.addAll(shotsToFire)
            }
        }

        // [수정] 투사체 업데이트에 배속 전달
        val projToRemove = ArrayList<Projectile>()
        for (p in state.projectiles) {
            p.update(currentSpeed)
            if (!p.hasHit) {
                for (e in state.enemies) {
                    if (e.isDead || e.reachedEnd) continue
                    val dx = e.x - p.x
                    val dy = e.y - p.y
                    val dist = sqrt((dx * dx + dy * dy).toDouble())
                    if (dist < 40f) {
                        p.hasHit = true
                        handleProjectileHit(p)
                        break
                    }
                }
            }
            if (p.hasHit) projToRemove.add(p)
        }
        state.projectiles.removeAll(projToRemove)

        // [수정] 적군 업데이트에 배속 전달
        val enemiesToRemove = ArrayList<Enemy>()
        for (e in state.enemies) {
            e.update(currentSpeed)
            if (e.reachedEnd) {
                state.lives--
                enemiesToRemove.add(e)
                if (state.lives <= 0) {
                    state.isCollectingItems = true
                }
            } else if (e.isDead) {
                enemiesToRemove.add(e)
                if (dropManager.shouldDrop()) {
                    val dropItem = dropManager.createDropItem(e.x, e.y)
                    state.drops.add(dropItem)
                }
            }
        }
        state.enemies.removeAll(enemiesToRemove)

        val dropsToRemove = ArrayList<DropItem>()
        for (drop in state.drops) {
            if (drop.isExpired()) {
                dropsToRemove.add(drop)
            }
        }
        state.drops.removeAll(dropsToRemove)

        if (!state.isCollectingItems && !state.isGameOver) {
            if (state.killsInCurrentStage >= state.requiredKills && state.enemies.isEmpty()) {
                // 난이도 증가 시점(웨이브 종료 시) 아이템 자동 수집 트리거
                state.isCollectingItems = true

                if (state.currentWave < state.maxWaves) {
                    // 웨이브 종료 -> 옵션 선택 -> 다음 웨이브
                    // 수집이 끝난 후 옵션 선택 창으로 이동하도록 로직 변경 필요하지만
                    // 현재 구조상 updateCollectionPhase 종료 시 분기 처리함
                } else {
                    state.isStageClear = true
                    // 스테이지 클리어 -> 수집 -> 종료
                }
            }
        }
    }

    private fun createCloneProjectile(original: Projectile, angleRad: Double): Projectile {
        val offsetDist = 15f
        val perpAngle = angleRad + Math.PI / 2
        val offX = (cos(perpAngle) * offsetDist).toFloat()
        val offY = (sin(perpAngle) * offsetDist).toFloat()

        val newX = original.x + offX
        val newY = original.y + offY

        val clone = Projectile(newX, newY, original.originX, original.originY, original.target, original.damage, original.color, original.weaponType)
        clone.speed = original.speed
        clone.ricochetCount = original.ricochetCount
        return clone
    }

    fun selectOption(option: DefenseGameOption) {
        state.collectedOptions.add(option)

        when (option) {
            DefenseGameOption.NORMAL_ATK_UP -> state.buffAtkDamage *= 1.1f
            DefenseGameOption.NORMAL_SPD_UP -> state.buffAtkSpeed *= 1.1f
            DefenseGameOption.NORMAL_RANGE_UP -> state.buffRange *= 1.1f
            DefenseGameOption.NORMAL_HP_UP -> state.lives += 3

            DefenseGameOption.MAGIC_ATK_UP -> state.buffAtkDamage *= 1.2f
            DefenseGameOption.MAGIC_SPD_UP -> state.buffAtkSpeed *= 1.2f
            DefenseGameOption.MAGIC_CRIT_UP -> state.buffCritDamage += 0.25f

            DefenseGameOption.RARE_ATK_UP -> state.buffAtkDamage *= 1.3f
            DefenseGameOption.RARE_SPD_UP -> state.buffAtkSpeed *= 1.3f
            DefenseGameOption.RARE_CRIT_CHANCE -> state.buffCritChance += 0.1f

            DefenseGameOption.UNIQUE_ATK_UP -> state.buffAtkDamage *= 1.4f
            DefenseGameOption.UNIQUE_SPD_UP -> state.buffAtkSpeed *= 1.4f
            DefenseGameOption.UNIQUE_CRIT_DMG -> state.buffCritDamage += 0.5f

            DefenseGameOption.LEGEND_MULTI_SHOT -> state.isMultiShot = true
            DefenseGameOption.LEGEND_DOUBLE_SHOT -> state.isDoubleShot = true
            DefenseGameOption.LEGEND_RICOCHET -> state.buffRicochetCount += 3
        }

        startNextWave()
        lastOptionSelectTime = System.currentTimeMillis()
    }

    private fun startNextWave() {
        state.isOptionSelection = false
        state.currentWave++
        state.setupWaveDifficulty()
    }

    private fun updateCollectionPhase(onStageClear: () -> Unit, onGameOver: () -> Unit, onItemCollected: (DropItem) -> Unit) {
        val targetX = state.mapWidth / 2f
        val targetY = state.mapHeight / 2f
        val speed = 40f

        val collected = ArrayList<DropItem>()

        for (drop in state.drops) {
            drop.isCollecting = true
            val dx = targetX - drop.x
            val dy = targetY - drop.y
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (dist < speed) {
                drop.x = targetX
                drop.y = targetY
                collected.add(drop)
                onItemCollected(drop)
            } else {
                val angle = atan2(dy.toDouble(), dx.toDouble())
                drop.x += (cos(angle) * speed).toFloat()
                drop.y += (sin(angle) * speed).toFloat()
            }
        }
        state.drops.removeAll(collected)

        if (state.drops.isEmpty()) {
            state.isCollectingItems = false

            if (state.lives <= 0) {
                state.isGameOver = true
                state.isRunning = false
                onGameOver()
            } else if (state.isStageClear) { // 스테이지 클리어
                state.isRunning = false
                onStageClear()
            } else { // 웨이브 클리어 후 아이템 수집 완료 시
                if (state.currentWave < state.maxWaves) {
                    state.isOptionSelection = true
                    state.currentOptions = DefenseGameOption.getRandomOptions(3)
                }
            }
        }
    }

    private fun handleProjectileHit(p: Projectile) {
        val hitEnemies = HashSet<Enemy>()
        val splashRadius = if (p.weaponType == WeaponType.SHOTGUN) 150f else 0f

        p.target?.let { target ->
            if (!target.isDead && !target.reachedEnd) {
                hitEnemies.add(target)
            }
        }

        for (e in state.enemies) {
            if (e.isDead || e.reachedEnd) continue
            val dx = e.x - p.x
            val dy = e.y - p.y
            val dist = sqrt((dx * dx + dy * dy).toDouble())
            if (dist < e.radius + 20f || (splashRadius > 0 && dist < splashRadius)) {
                hitEnemies.add(e)
            }
        }

        var bounced = false
        if (p.ricochetCount > 0 && hitEnemies.isNotEmpty()) {
            p.hitTargets.addAll(hitEnemies)

            var nearestEnemy: Enemy? = null
            var minDist = Float.MAX_VALUE
            val bounceRange = 400f

            for (e in state.enemies) {
                if (e.isDead || e.reachedEnd || p.hitTargets.contains(e)) continue
                val dist = sqrt(((e.x - p.x) * (e.x - p.x) + (e.y - p.y) * (e.y - p.y)).toDouble()).toFloat()
                if (dist < bounceRange && dist < minDist) {
                    minDist = dist
                    nearestEnemy = e
                }
            }

            if (nearestEnemy != null) {
                p.ricochetCount--
                p.damage = (p.damage * 0.7f).toInt()

                p.target = nearestEnemy
                p.updateAngle()
                p.hasHit = false
                bounced = true
            }
        }

        for (e in hitEnemies) {
            var dmg = p.damage
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
            }
        }

        if (bounced) {
            p.hasHit = false
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
        if (state.isCollectingItems || state.isOptionSelection) return false

        if (System.currentTimeMillis() - lastOptionSelectTime < 500) return false

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
                    if (isValidGrid && state.gridState?.get(col)?.get(row) == true) {
                        val centerX = col * state.gridSize + state.gridSize / 2
                        val centerY = row * state.gridSize + state.gridSize / 2
                        val targetChar = state.characters.find { it != char && Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                        if (targetChar != null) {
                            if (targetChar.level == char.level &&
                                targetChar.weaponType == char.weaponType &&
                                targetChar.characterType == char.characterType &&
                                targetChar.weaponGrade == char.weaponGrade) {
                                targetChar.upgrade()
                                state.characters.remove(char)
                                state.selectedTile = null
                            } else {
                                state.dragStartPos?.let { char.setPosition(it.x, it.y) }
                            }
                        } else {
                            char.setPosition(centerX, centerY)
                            state.selectedTile = null
                        }
                    } else {
                        state.dragStartPos?.let { char.setPosition(it.x, it.y) }
                    }
                    state.draggingCharacter = null
                    state.dragStartPos = null
                    return true
                } else {
                    if (isValidGrid && state.gridState?.get(col)?.get(row) == true) {
                        val centerX = col * state.gridSize + state.gridSize / 2
                        val centerY = row * state.gridSize + state.gridSize / 2
                        val existing = state.characters.find { Math.abs(it.x - centerX) < 10 && Math.abs(it.y - centerY) < 10 }

                        if (existing == null) {
                            val buildCost = 50
                            if (state.currentPoints >= buildCost) {
                                state.characters.add(Character(centerX, centerY, state.selectedWeapon, state.selectedCharacterType, state.selectedWeaponGrade))
                                state.currentPoints -= buildCost
                                state.selectedTile = null
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