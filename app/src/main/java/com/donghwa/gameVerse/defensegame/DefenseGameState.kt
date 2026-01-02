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

    var collectedOptions = ArrayList<DefenseGameOption>()

    var score = 0
    var lives = 10
    var stage = 1
    var maxUnlockedStage = 1
    var currentPoints = 0

    // [신규] 이번 게임에서 획득한 동
    var acquiredDong = 0

    // 게임 진행 속도 (1배, 2배, 3배)
    var gameSpeed = 1

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
    var isMultiShot = false
    var buffRicochetCount = 0

    var globalDamageMultiplier = 1.0f
    var upgradeCost = 100

    var selectedWeapon: WeaponType = WeaponType.SMG
    var selectedCharacterType: DefenseCharacterType = DefenseCharacterType.POTATO
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
    val gridSize = 200f
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
        gameSpeed = 1
        acquiredDong = 0 // 초기화

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
        enemyHp = 10 + (difficultyFactor * 5) + (currentWave * currentWave)
        enemyDefense = (stage - 1) + (currentWave / 5)
        spawnInterval = (1800 - (stage * 80) - (currentWave * 60)).coerceAtLeast(300).toLong()
        requiredKills = 10 + (stage * 3) + (currentWave * 3)
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
        isMultiShot = false
        buffRicochetCount = 0
        collectedOptions.clear()
    }
}