package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.data.AppDatabase
import com.example.data.RecordRepository
import com.example.data.SurvivorRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = RecordRepository(database.recordDao())

    val survivorHistory: StateFlow<List<SurvivorRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Configuration / Shop Progression
    private val _currentScreen = MutableStateFlow(Screen.Menu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _upgrades = MutableStateFlow(GameUpgrades())
    val upgrades: StateFlow<GameUpgrades> = _upgrades.asStateFlow()

    private val _manaShards = MutableStateFlow(0) // Currency for shop
    val manaShards: StateFlow<Int> = _manaShards.asStateFlow()

    // Game Runtime States
    private val _level = MutableStateFlow(0)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives: StateFlow<Int> = _lives.asStateFlow()

    private val _playerHealth = MutableStateFlow(100f)
    val playerHealth: StateFlow<Float> = _playerHealth.asStateFlow()

    private val _playerStamina = MutableStateFlow(100f)
    val playerStamina: StateFlow<Float> = _playerStamina.asStateFlow()

    private val _playerX = MutableStateFlow(1f)
    val playerX: StateFlow<Float> = _playerX.asStateFlow()

    private val _playerY = MutableStateFlow(1f)
    val playerY: StateFlow<Float> = _playerY.asStateFlow()

    private val _staminaIsSprinting = MutableStateFlow(false)
    val staminaIsSprinting: StateFlow<Boolean> = _staminaIsSprinting.asStateFlow()

    // Active powerup buffers
    private val _invincible = MutableStateFlow(false)
    val invincible: StateFlow<Boolean> = _invincible.asStateFlow()

    private val _speedBoost = MutableStateFlow(false)
    val speedBoost: StateFlow<Boolean> = _speedBoost.asStateFlow()

    private val _invisible = MutableStateFlow(false)
    val invisible: StateFlow<Boolean> = _invisible.asStateFlow()

    private val _activePowerups = MutableStateFlow<List<ActivePowerUp>>(emptyList())
    val activePowerups: StateFlow<List<ActivePowerUp>> = _activePowerups.asStateFlow()

    // Level Entity lists
    private val _mazeMap = MutableStateFlow<List<List<Char>>>(emptyList())
    val mazeMap: StateFlow<List<List<Char>>> = _mazeMap.asStateFlow()

    private val _monstersList = MutableStateFlow<List<Monster>>(emptyList())
    val monstersList: StateFlow<List<Monster>> = _monstersList.asStateFlow()

    private val _artifactsList = MutableStateFlow<List<Artifact>>(emptyList())
    val artifactsList: StateFlow<List<Artifact>> = _artifactsList.asStateFlow()

    private val _powerupsList = MutableStateFlow<List<PowerUp>>(emptyList())
    val powerupsList: StateFlow<List<PowerUp>> = _powerupsList.asStateFlow()

    private val _portalsList = MutableStateFlow<List<Portal>>(emptyList())
    val portalsList: StateFlow<List<Portal>> = _portalsList.asStateFlow()

    private val _chasingMonsters = MutableStateFlow<List<Monster>>(emptyList())
    val chasingMonsters: StateFlow<List<Monster>> = _chasingMonsters.asStateFlow()

    private val _gameRunning = MutableStateFlow(false)
    val gameRunning: StateFlow<Boolean> = _gameRunning.asStateFlow()

    private val _gameOverState = MutableStateFlow<String?>(null) // "escaped", "caught" or null
    val gameOverState: StateFlow<String?> = _gameOverState.asStateFlow()

    private val _totalArtifactsCollected = MutableStateFlow(0)
    val totalArtifactsCollected: StateFlow<Int> = _totalArtifactsCollected.asStateFlow()

    private val _stageArtifactsRequired = MutableStateFlow(1)
    val stageArtifactsRequired: StateFlow<Int> = _stageArtifactsRequired.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Background threads monitoring
    private var gameLoopJob: Job? = null
    private var monsterTickJob: Job? = null
    private var heartbeatLoopJob: Job? = null

    private var playerSpawnX = 1f
    private var playerSpawnY = 1f

    private var mazeWidth = 15
    private var mazeHeight = 15

    private var startTimeMs: Long = 0
    private var accumulatedSeconds: Long = 0

    private val monsterTypes = listOf(
        Pair("chaser", "👹"),     // Evil Nun
        Pair("patroller", "👻"),  // Phantom Wraith
        Pair("teleporter", "🤡"), // Jester Ghoul
        Pair("zombie", "🧟"),     // Zombie Minion
        Pair("brute", "🦍"),      // Brute
        Pair("vortex", "🌪️")       // Vortex Demon
    )

    private val artifactEmojis = listOf("🔑", "🗝️", "💎", "🏺", "👑", "📜")

    init {
        _isMuted.value = AudioEngine.getMuted()
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.Game) {
            _lives.value = 3
            _level.value = 0
            _manaShards.value = 0
            _totalArtifactsCollected.value = 0
            accumulatedSeconds = 0
            setupLevel()
            startGameLoops()
        } else {
            stopGameLoops()
        }
    }

    fun toggleMute() {
        val muted = AudioEngine.toggleMute()
        _isMuted.value = muted
    }

    private fun startGameLoops() {
        stopGameLoops()
        _gameRunning.value = true
        _gameOverState.value = null
        startTimeMs = System.currentTimeMillis()

        // 1. Primary tick loop: handles Stamina, sprint, power-up timer countdowns
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastSecondTime = System.currentTimeMillis()
            while (_gameRunning.value) {
                val now = System.currentTimeMillis()
                
                // Track time survived seconds
                if (now - lastSecondTime >= 1000) {
                    accumulatedSeconds++
                    lastSecondTime = now
                }

                // Stamina tick based on upgrades
                val sprRate = 1.0f
                val regRate = 0.5f * _upgrades.value.getStaminaMultiplier()
                if (_staminaIsSprinting.value && _playerStamina.value > 0) {
                    _playerStamina.value = (_playerStamina.value - sprRate).coerceAtLeast(0f)
                } else {
                    _staminaIsSprinting.value = false
                    _playerStamina.value = (_playerStamina.value + regRate).coerceAtMost(100f)
                }

                // Power up countdowns
                val currentPowerups = _activePowerups.value.map { pu ->
                    pu.copy(remainingTimeMs = pu.remainingTimeMs - 100)
                }.filter { it.remainingTimeMs > 0 }

                _activePowerups.value = currentPowerups
                _invincible.value = currentPowerups.any { it.type == "shield" }
                _speedBoost.value = currentPowerups.any { it.type == "speed" }
                _invisible.value = currentPowerups.any { it.type == "invisibility" }

                delay(100)
            }
        }

        // 2. Monsters Tick loop
        monsterTickJob = viewModelScope.launch(Dispatchers.Default) {
            while (_gameRunning.value) {
                // Calculate speed increment per level
                val stepDelay = (500 - (_level.value * 12)).coerceAtLeast(200).toLong()
                moveMonsters()
                checkCollisions()
                delay(stepDelay)
            }
        }

        // 3. Heartbeat & Warning loop
        heartbeatLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (_gameRunning.value) {
                val chasers = _chasingMonsters.value
                if (chasers.isNotEmpty() && !_invisible.value) {
                    var minDistance = Float.MAX_VALUE
                    var closestMonster: Monster? = null
                    val px = _playerX.value
                    val py = _playerY.value
                    
                    for (m in chasers) {
                        val d = sqrt(((px - m.x) * (px - m.x) + (py - m.y) * (py - m.y)).toDouble()).toFloat()
                        if (d < minDistance) {
                            minDistance = d
                            closestMonster = m
                        }
                    }

                    if (closestMonster != null && minDistance < 15) {
                        AudioEngine.playHeartbeat(minDistance)
                        if (Math.random() < 0.25) {
                            AudioEngine.playMonsterSound(closestMonster.char)
                        }
                        val beatDelay = (minDistance * 110).coerceIn(250f, 1500f).toLong()
                        delay(beatDelay)
                    } else {
                        delay(1000)
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    private fun stopGameLoops() {
        _gameRunning.value = false
        gameLoopJob?.cancel()
        monsterTickJob?.cancel()
        heartbeatLoopJob?.cancel()
    }

    private fun setupLevel() {
        // Compute scaled dimensions for Stage
        _gameOverState.value = null
        val currentLvl = _level.value
        mazeWidth = 17 + (currentLvl / 3) * 2
        mazeHeight = 13 + (currentLvl / 4) * 2

        if (mazeWidth % 2 == 0) mazeWidth++
        if (mazeHeight % 2 == 0) mazeHeight++

        // Cap dimensions
        mazeWidth = mazeWidth.coerceAtMost(31)
        mazeHeight = mazeHeight.coerceAtMost(25)

        _stageArtifactsRequired.value = 1 + (currentLvl / 5)

        val map = generateMaze(mazeWidth, mazeHeight)
        _mazeMap.value = map

        val emptyTiles = mutableListOf<Pair<Int, Int>>()
        for (y in 1 until mazeHeight - 1) {
            for (x in 1 until mazeWidth - 1) {
                if (map[y][x] == '.') {
                    emptyTiles.add(Pair(x, y))
                }
            }
        }

        // 1. Place player far as possible from bottom right
        val pSpawn = emptyTiles.removeLastOrNull() ?: Pair(1, 1)
        playerSpawnX = pSpawn.first.toFloat()
        playerSpawnY = pSpawn.second.toFloat()
        _playerX.value = playerSpawnX
        _playerY.value = playerSpawnY

        _playerHealth.value = (100 + _upgrades.value.getBonusMaxHealth()).toFloat()
        _playerStamina.value = 100f

        // Reset active power buffs
        val tempPowerups = mutableListOf<ActivePowerUp>()
        if (_upgrades.value.startsWithShield()) {
            tempPowerups.add(ActivePowerUp("shield", 10000L))
        }
        _activePowerups.value = tempPowerups
        _invincible.value = tempPowerups.any { it.type == "shield" }
        _speedBoost.value = false
        _invisible.value = false

        // 2. Place Artifacts
        val required = _stageArtifactsRequired.value
        val artifacts = mutableListOf<Artifact>()
        for (i in 0 until required) {
            val elementIndex = emptyTiles.indices.randomOrNull() ?: continue
            val tile = emptyTiles.removeAt(elementIndex)
            val char = artifactEmojis[i % artifactEmojis.size]
            artifacts.add(Artifact(tile.first, tile.second, char))
        }
        _artifactsList.value = artifacts

        // 3. Place Portals
        val portals = mutableListOf<Portal>()
        if (emptyTiles.size >= 2) {
            val t1 = emptyTiles.removeAt(emptyTiles.indices.random())
            val t2 = emptyTiles.removeAt(emptyTiles.indices.random())
            portals.add(Portal(t1.first, t1.second))
            portals.add(Portal(t2.first, t2.second))
        }
        _portalsList.value = portals

        // 4. Place Monsters (Max 7, scales with level)
        val numMonsters = (2 + (currentLvl / 4)).coerceAtMost(7)
        val monsters = mutableListOf<Monster>()
        for (i in 0 until numMonsters) {
            val tileIndex = emptyTiles.indices.randomOrNull() ?: continue
            val tile = emptyTiles.removeAt(tileIndex)
            
            val typePair = monsterTypes[i % monsterTypes.size]
            val type = typePair.first
            val emoji = typePair.second
            
            val (name, dmg, bSpeed) = when (type) {
                "chaser" -> Triple("Evil Nun", 15, 1.4f)
                "patroller" -> Triple("Phantom Wraith", 10, 1.1f)
                "teleporter" -> Triple("Jester Ghoul", 20, 0.8f)
                "zombie" -> Triple("Zombie Minion", 5, 0.9f)
                "brute" -> Triple("Brute", 30, 0.4f)
                else -> Triple("Vortex Demon", 25, 0.7f) // "vortex"
            }

            monsters.add(
                Monster(
                    x = tile.first.toFloat(),
                    y = tile.second.toFloat(),
                    char = emoji,
                    type = type,
                    baseSpeed = bSpeed,
                    damage = dmg,
                    name = name,
                    sound = emoji,
                    currentPath = findRandomPath(tile.first, tile.second, map)
                )
            )
        }
        _monstersList.value = monsters

        // 5. Place powerups
        val numPowerUps = 1 + (currentLvl / 10)
        val powerups = mutableListOf<PowerUp>()
        val pTypes = listOf(
            Triple("shield", "🛡️", 10000L),
            Triple("speed", "⚡", 15000L),
            Triple("invisibility", "✨", 10000L)
        )
        for (i in 0 until numPowerUps) {
            val tileIndex = emptyTiles.indices.randomOrNull() ?: continue
            val tile = emptyTiles.removeAt(tileIndex)
            val pType = pTypes.random()
            powerups.add(
                PowerUp(
                    x = tile.first,
                    y = tile.second,
                    char = pType.second,
                    type = pType.first,
                    durationMs = pType.third
                )
            )
        }
        _powerupsList.value = powerups
        _chasingMonsters.value = emptyList()
    }

    private fun generateMaze(width: Int, height: Int): List<List<Char>> {
        val maze = Array(height) { CharArray(width) { '#' } }
        fun carve(cx: Int, cy: Int) {
            maze[cy][cx] = '.'
            val dirs = listOf(
                Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
            ).shuffled()
            for ((dx, dy) in dirs) {
                val nx = cx + dx * 2
                val ny = cy + dy * 2
                if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1 && maze[ny][nx] == '#') {
                    maze[cy + dy][cx + dx] = '.'
                    carve(nx, ny)
                }
            }
        }
        carve(1, 1)

        // Make outer limits solid walls
        for (y in 0 until height) {
            maze[y][0] = '#'
            maze[y][width - 1] = '#'
        }
        for (x in 0 until width) {
            maze[0][x] = '#'
            maze[height - 1][x] = '#'
        }

        return maze.map { it.toList() }
    }

    // Direct movement commands that supports keyboard & D-pads
    fun movePlayer(dx: Int, dy: Int) {
        if (!_gameRunning.value) return
        
        val map = _mazeMap.value
        val px = (_playerX.value + dx).toInt()
        val py = (_playerY.value + dy).toInt()

        if (py >= 0 && py < map.size && px >= 0 && px < map[0].size && map[py][px] != '#') {
            _playerX.value = px.toFloat()
            _playerY.value = py.toFloat()
            AudioEngine.playFootstep()
            checkCollisions()
        }
    }

    fun setSprinting(sprint: Boolean) {
        if (sprint && _playerStamina.value > 10f) {
            _staminaIsSprinting.value = true
        } else {
            _staminaIsSprinting.value = false
        }
    }

    // Basic line-of-sight check
    private fun hasLineOfSight(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        val map = _mazeMap.value
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy

        var x = x1
        var y = y1

        while (true) {
            if (x == x2 && y == y2) return true
            if (y < 0 || y >= map.size || x < 0 || x >= map[0].size) return false
            if (map[y][x] == '#') return false

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy; x += sx
            }
            if (e2 < dx) {
                err += dx; y += sy
            }
        }
    }

    private fun moveMonsters() {
        if (!_gameRunning.value) return
        val map = _mazeMap.value
        val px = _playerX.value.toInt()
        val py = _playerY.value.toInt()
        
        val activeChasers = mutableListOf<Monster>()

        val currentMonsters = _monstersList.value.map { m ->
            val mx = m.x.toInt()
            val my = m.y.toInt()
            var dx = 0
            var dy = 0
            var isChasing = false

            when (m.type) {
                "chaser" -> {
                    if (!_invisible.value && hasLineOfSight(mx, my, px, py)) {
                        isChasing = true
                        if (px > mx) dx = 1
                        else if (px < mx) dx = -1
                        if (py > my) dy = 1
                        else if (py < my) dy = -1
                    } else {
                        val next = m.currentPath.firstOrNull()
                        if (next != null) {
                            dx = next.first - mx
                            dy = next.second - my
                        }
                    }
                }
                "patroller" -> {
                    val next = m.currentPath.firstOrNull()
                    if (next != null) {
                        dx = next.first - mx
                        dy = next.second - my
                    }
                }
                "teleporter" -> {
                    val timer = m.teleportTimer + 1
                    if (timer >= 6) {
                        // Teleport to a random open tile far from player
                        val emptyTiles = getEmptyTiles()
                        val farTile = emptyTiles.filter { 
                            abs(it.first - px) + abs(it.second - py) > 6 
                        }.randomOrNull() ?: emptyTiles.random()
                        return@map m.copy(x = farTile.first.toFloat(), y = farTile.second.toFloat(), teleportTimer = 0)
                    } else {
                        return@map m.copy(teleportTimer = timer)
                    }
                }
                "zombie" -> {
                    val dist = abs(mx - px) + abs(my - py)
                    if (!_invisible.value && dist < 10) {
                        isChasing = true
                        if (px > mx) dx = 1
                        else if (px < mx) dx = -1
                        if (py > my) dy = 1
                        else if (py < my) dy = -1
                    } else {
                        val next = m.currentPath.firstOrNull()
                        if (next != null) {
                            dx = next.first - mx
                            dy = next.second - my
                        }
                    }
                }
                "brute" -> {
                    val dist = abs(mx - px) + abs(my - py)
                    if (!_invisible.value && dist < 14) {
                        isChasing = true
                        if (px > mx) dx = 1
                        else if (px < mx) dx = -1
                        if (py > my) dy = 1
                        else if (py < my) dy = -1
                    } else {
                        val next = m.currentPath.firstOrNull()
                        if (next != null) {
                            dx = next.first - mx
                            dy = next.second - my
                        }
                    }
                }
                else -> { // "vortex"
                    val dist = abs(mx - px) + abs(my - py)
                    val timer = m.teleportTimer + 1
                    if (timer % 10 == 0 && dist < 11) {
                        isChasing = true
                        // Teleport directly to player space
                        return@map m.copy(x = px.toFloat(), y = py.toFloat(), teleportTimer = timer)
                    } else {
                        val next = m.currentPath.firstOrNull()
                        if (next != null) {
                            dx = next.first - mx
                            dy = next.second - my
                        }
                    }
                }
            }

            if (isChasing) {
                activeChasers.add(m)
            }

            val nextX = mx + dx
            val nextY = my + dy

            val validMove = nextY >= 0 && nextY < map.size && nextX >= 0 && nextX < map[0].size && map[nextY][nextX] != '#'
            var newX = mx.toFloat()
            var newY = my.toFloat()
            var path = if (m.currentPath.isNotEmpty()) m.currentPath.drop(1) else emptyList()

            if (validMove) {
                newX = nextX.toFloat()
                newY = nextY.toFloat()
            } else {
                path = findRandomPath(mx, my, map)
            }

            m.copy(
                x = newX,
                y = newY,
                currentPath = path,
                teleportTimer = m.teleportTimer + 1
            )
        }

        _monstersList.value = currentMonsters
        _chasingMonsters.value = activeChasers
    }

    private fun findRandomPath(sx: Int, sy: Int, map: List<List<Char>>): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        var currX = sx
        var currY = sy
        
        for (i in 0..6) {
            val validDirs = listOf(
                Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
            ).filter { (dx, dy) ->
                val nx = currX + dx
                val ny = currY + dy
                ny >= 0 && ny < map.size && nx >= 0 && nx < map[0].size && map[ny][nx] != '#'
            }
            val dir = validDirs.randomOrNull() ?: break
            currX += dir.first
            currY += dir.second
            path.add(Pair(currX, currY))
        }
        return path
    }

    private fun getEmptyTiles(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        val map = _mazeMap.value
        for (y in 1 until map.size - 1) {
            for (x in 1 until map[0].size - 1) {
                if (map[y][x] == '.') list.add(Pair(x, y))
            }
        }
        return list
    }

    private fun checkCollisions() {
        if (!_gameRunning.value) return
        val px = _playerX.value.toInt()
        val py = _playerY.value.toInt()

        // 1. Check artifact collection
        val artifacts = _artifactsList.value
        val colArtifactIndex = artifacts.indexOfFirst { !it.found && it.x == px && it.y == py }
        if (colArtifactIndex != -1) {
            val updated = artifacts.toMutableList()
            updated[colArtifactIndex] = artifacts[colArtifactIndex].copy(found = true)
            _artifactsList.value = updated
            
            _manaShards.value++
            _totalArtifactsCollected.value++
            AudioEngine.playArtifactSound()
            
            val activeFoundCount = updated.count { it.found }
            if (activeFoundCount >= _stageArtifactsRequired.value) {
                // Instantly level up / Go to shop
                goToShop()
            }
        }

        // 2. Check Powerup pick
        val powerups = _powerupsList.value
        val colPowerupIndex = powerups.indexOfFirst { it.x == px && it.y == py }
        if (colPowerupIndex != -1) {
            val powerup = powerups[colPowerupIndex]
            val updatedList = powerups.filterIndexed { index, _ -> index != colPowerupIndex }
            _powerupsList.value = updatedList

            val customDuration = powerup.durationMs + _upgrades.value.getCamouflageDurationBonusMs()
            _activePowerups.value = _activePowerups.value + ActivePowerUp(powerup.type, customDuration)
            AudioEngine.playPowerUpSound()
        }

        // 3. Portals teleport
        val portals = _portalsList.value
        val colPortalIndex = portals.indexOfFirst { it.x == px && it.y == py }
        if (colPortalIndex != -1) {
            val destPortal = portals[(colPortalIndex + 1) % 2]
            _playerX.value = destPortal.x.toFloat()
            _playerY.value = destPortal.y.toFloat()
            AudioEngine.playTeleportSound()
        }

        // 4. Monsters attack player
        if (!_invincible.value) {
            val monsters = _monstersList.value
            val hittingMonster = monsters.firstOrNull { 
                abs(it.x.toInt() - px) < 1 && abs(it.y.toInt() - py) < 1 
            }

            if (hittingMonster != null) {
                AudioEngine.playHurtSound()
                val remainingHealth = _playerHealth.value - hittingMonster.damage
                if (remainingHealth <= 0) {
                    deductLife(hittingMonster.name)
                } else {
                    _playerHealth.value = remainingHealth
                }
            }
        }
    }

    private fun deductLife(killerName: String) {
        val currentLives = _lives.value - 1
        _lives.value = currentLives
        if (currentLives <= 0) {
            endGame(false, killerName)
        } else {
            // Respawn player at initial place
            _playerHealth.value = (100 + _upgrades.value.getBonusMaxHealth()).toFloat()
            _playerX.value = playerSpawnX
            _playerY.value = playerSpawnY
        }
    }

    private fun goToShop() {
        stopGameLoops()
        _currentScreen.value = Screen.Shop
    }

    fun proceedToNextLevel() {
        _level.value++
        _currentScreen.value = Screen.Game
        setupLevel()
        startGameLoops()
    }

    fun buyUpgrade(type: String) {
        val currentShards = _manaShards.value
        val cost = when (type) {
            "iron" -> 1 + _upgrades.value.ironHeartLevel
            "lungs" -> 2 + _upgrades.value.asceticLungsLevel
            "veil" -> 2 + _upgrades.value.witchVeilLevel
            "steps" -> 3 + _upgrades.value.ghostlyStepsLevel
            else -> 3 + _upgrades.value.divineShieldLevel
        }

        if (currentShards >= cost) {
            _manaShards.value = currentShards - cost
            val newUpgrades = _upgrades.value.copy()
            when (type) {
                "iron" -> newUpgrades.ironHeartLevel++
                "lungs" -> newUpgrades.asceticLungsLevel++
                "veil" -> newUpgrades.witchVeilLevel++
                "steps" -> newUpgrades.ghostlyStepsLevel++
                "shield" -> newUpgrades.divineShieldLevel++
            }
            _upgrades.value = newUpgrades
            AudioEngine.playPowerUpSound()
        }
    }

    private fun endGame(escaped: Boolean, killer: String = "") {
        stopGameLoops()
        _gameOverState.value = if (escaped) "escaped" else "caught"
        
        val deathReason = if (escaped) {
            "Ritually Cleansed the Manor"
        } else {
            "Consumed by $killer on Stage ${_level.value + 1}"
        }

        // Insert results into Survivor Journal (Room)
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(
                SurvivorRecord(
                    stageReached = _level.value + 1,
                    artifactsCollected = _totalArtifactsCollected.value,
                    timeSurvivedSeconds = accumulatedSeconds,
                    isEscape = escaped,
                    deathReason = deathReason
                )
            )
        }
    }

    fun completeRitualEscape() {
        endGame(true)
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
    }
}
