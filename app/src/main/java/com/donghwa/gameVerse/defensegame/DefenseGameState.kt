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
    var isCollectingItems = false

    var isOptionSelection = false
    var currentOptions = listOf<DefenseGameOption>()

    var score = 0
    var lives = 10
    var stage = 1
    var maxUnlockedStage = 1
    var currentPoints = 0

    var currentWave = 1
    val maxWaves = 10

    // 강화 효과
    var buffAtkSpeed = 1.0f
    var buffAtkDamage = 1.0f
    var buffRange = 1.0f
    var buffEnemySpeed = 1.0f
    var buffProjSpeed = 1.0f
    var buffCritChance = 0.0f
    var buffCritDamage = 1.5f
    var isDoubleShot = false
    var buffRicochetCount = 0

    var globalDamageMultiplier = 1.0f
    var upgradeCost = 100

    var selectedWeapon: WeaponType = WeaponType.SMG
    var selectedCharacterType: DefenseCharacterType = DefenseCharacterType.HUMAN
    var selectedWeaponGrade: WeaponGrade = WeaponGrade.NORMAL

    var killsInCurrentStage = 0
    var requiredKills = 10
    var spawnedEnemies = 0

    val characters = CopyOnWriteArrayList<Character>()
    val enemies = CopyOnWriteArrayList<Enemy>()
    val projectiles = CopyOnWriteArrayList<Projectile>()
    val drops = CopyOnWriteArrayList<DropItem>()

    var draggingCharacter: Character? = null
    var dragStartPos: PointF? = null

    var mapWidth = 0
    var mapHeight = 0

    var path = ArrayList<PointF>()

    // [수정] 배치 공간을 촘촘하게 하기 위해 그리드 크기 축소 (400f -> 130f)
    val gridSize = 130f

    var rows = 0
    var cols = 0

    var gridState: Array<BooleanArray>? = null

    var selectedTile: PointF? = null

    var lastSpawnTime = 0L
    var spawnInterval = 1500L
    var enemyHp = 20
    var enemyDefense = 0

    fun resetForStage(newStage: Int) {
        stage = newStage
        currentWave = 1
        resetBuffs()

        score = 0
        lives = 10
        currentPoints = 100 + (stage * 50)
        globalDamageMultiplier = 1.0f
        upgradeCost = 100

        setupWaveDifficulty()

        enemies.clear()
        projectiles.clear()
        characters.clear()
        drops.clear()
        selectedTile = null
        draggingCharacter = null
        dragStartPos = null
        isGameOver = false
        isStageClear = false
        isPaused = false
        isRunning = true
        isCollectingItems = false
        isOptionSelection = false
    }

    fun setupWaveDifficulty() {
        killsInCurrentStage = 0
        spawnedEnemies = 0

        val difficultyFactor = stage * 2 + currentWave

        enemyHp = 20 + (difficultyFactor * 25) + (currentWave * currentWave * 10)
        enemyDefense = (stage - 1) * 2 + (currentWave / 2)

        spawnInterval = (2000 - (stage * 100) - (currentWave * 80)).coerceAtLeast(400).toLong()
        requiredKills = 5 + (stage * 2) + (currentWave * 2)
    }

    private fun resetBuffs() {
        buffAtkSpeed = 1.0f
        buffAtkDamage = 1.0f
        buffRange = 1.0f
        buffEnemySpeed = 1.0f
        buffProjSpeed = 1.0f
        buffCritChance = 0.0f
        buffCritDamage = 1.5f
        isDoubleShot = false
        buffRicochetCount = 0
    }
}