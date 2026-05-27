package com.example.game

enum class Screen {
    Menu, Game, Shop, Records, Settings
}

data class GameUpgrades(
    var ironHeartLevel: Int = 0,    // +20 Max Health per level
    var asceticLungsLevel: Int = 0, // +35% Stamina Recovery per level
    var witchVeilLevel: Int = 0,    // +3s Camouflage/Invisibility duration
    var ghostlyStepsLevel: Int = 0, // +12% Default Movement Speed
    var divineShieldLevel: Int = 0  // Start with shield bubble active
) {
    fun getBonusMaxHealth() = ironHeartLevel * 20
    fun getStaminaMultiplier() = 1f + asceticLungsLevel * 0.35f
    fun getCamouflageDurationBonusMs() = witchVeilLevel * 3000L
    fun getSpeedMultiplier() = 1f + ghostlyStepsLevel * 0.12f
    fun startsWithShield() = divineShieldLevel > 0
}

data class Monster(
    var x: Float,
    var y: Float,
    val char: String,
    val type: String, // "chaser", "patroller", "teleporter", "zombie", "brute", "vortex"
    val baseSpeed: Float,
    val damage: Int,
    val name: String,
    val sound: String,
    var currentPath: List<Pair<Int, Int>> = emptyList(),
    var teleportTimer: Int = 0
)

data class Artifact(
    val x: Int,
    val y: Int,
    val char: String,
    var found: Boolean = false
)

data class PowerUp(
    val x: Int,
    val y: Int,
    val char: String,
    val type: String, // "shield", "speed", "invisibility"
    val durationMs: Long
)

data class ActivePowerUp(
    val type: String,
    var remainingTimeMs: Long
)

data class Portal(
    val x: Int,
    val y: Int
)

data class RoomTheme(
    val name: String,
    val bgGradientHex: List<String>,
    val wallsHex: String,
    val floorHex: String
)

val ROOM_THEMES = listOf(
    RoomTheme("Cobwebbed Cellar", listOf("#0F2038", "#050914"), "#2B6CB0", "#1A202C"),
    RoomTheme("Crimson Gallery", listOf("#3C0C0C", "#0C0202"), "#C53030", "#1F1111"),
    RoomTheme("Overgrown Crypt", listOf("#0D2E14", "#020A05"), "#2F855A", "#142218"),
    RoomTheme("Obsidian Dungeon", listOf("#1D2533", "#0E101A"), "#4A5568", "#111425"),
    RoomTheme("Cursed Portal Chamber", listOf("#320E3E", "#0B040F"), "#6B46C1", "#191122")
)
