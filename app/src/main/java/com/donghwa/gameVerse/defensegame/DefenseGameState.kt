package com.donghwa.gameVerse.defensegame

import android.graphics.PointF
import java.util.concurrent.CopyOnWriteArrayList

class DefenseGameState {
    var isGameOver = false
    var isRunning = true
    var isLevelSelection = true
    var isWeaponSelection = false
    var isStageClear = false
    var isPaused = false

    var score = 0
    var lives = 10
    var stage = 1
    var maxUnlockedStage = 1
    var currentPoints = 0

    var globalDamageMultiplier = 1.0f
    var upgradeCost = 100

    // [신규] 선택된 캐릭터 및 무기
    var selectedWeapon: WeaponType = WeaponType.SMG
    var selectedCharacterType: DefenseCharacterType = DefenseCharacterType.HUMAN

    var killsInCurrentStage = 0
    var requiredKills = 10
    var spawnedEnemies = 0

    val characters = CopyOnWriteArrayList<Character>()
    val enemies = CopyOnWriteArrayList<Enemy>()
    val projectiles = CopyOnWriteArrayList<Projectile>()

    var draggingCharacter: Character? = null
    var dragStartPos: PointF? = null

    var path = ArrayList<PointF>()
    val gridSize = 400f
    var rows = 0
    var cols = 0
    lateinit var gridState: Array<BooleanArray>
    var selectedTile: PointF? = null

    var lastSpawnTime = 0L
    var spawnInterval = 1500L
    var enemyHp = 20
    var enemyDefense = 0

    fun resetForStage(newStage: Int) {
        stage = newStage
        score = 0
        lives = 10
        killsInCurrentStage = 0
        spawnedEnemies = 0

        currentPoints = 100 + (stage * 50)
        globalDamageMultiplier = 1.0f
        upgradeCost = 100

        enemyHp = 20 + (stage * 30) + (stage * stage * 5)
        enemyDefense = (stage - 1) * 2
        spawnInterval = (2000 - (stage * 200)).coerceAtLeast(500).toLong()
        requiredKills = 5 + (stage * 5)

        enemies.clear()
        projectiles.clear()
        characters.clear()
        selectedTile = null
        draggingCharacter = null
        dragStartPos = null

        isGameOver = false
        isStageClear = false
        isPaused = false
        isRunning = true
    }
}