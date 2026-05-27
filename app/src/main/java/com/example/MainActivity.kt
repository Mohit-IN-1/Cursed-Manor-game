package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioEngine
import com.example.data.SurvivorRecord
import com.example.game.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()
                val currentScreen by gameViewModel.currentScreen.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.Menu -> GateScreen(gameViewModel)
                        Screen.Game -> GameScreen(gameViewModel)
                        Screen.Shop -> DarkShopScreen(gameViewModel)
                        Screen.Records -> SurvivorRecordsScreen(gameViewModel)
                        Screen.Settings -> SettingsScreen(gameViewModel)
                    }
                }
            }
        }
    }
}

// Keep Greeting for testing compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun GateScreen(viewModel: GameViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "titleGlow")
    val titlePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        DeepObsidian,
                        ObsidianBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title text styled beautifully
        Text(
            text = "THE CURSED MANOR",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp * titlePulseScale,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    offset = Offset(0f, 0f),
                    blurRadius = 16f
                )
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "An Atmospheric Retro Escape Challenge",
            fontSize = 14.sp,
            color = GhostGrey,
            textAlign = TextAlign.Center,
            style = TextStyle(letterSpacing = 1.5.sp),
            modifier = Modifier.padding(bottom = 40.dp)
        )

        // Menu Actions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, PolishedNeutralBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        AudioEngine.playTeleportSound()
                        viewModel.setScreen(Screen.Game)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "ENTER THE MANOR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(letterSpacing = 1.sp)
                    )
                }

                Button(
                    onClick = {
                        AudioEngine.playPowerUpSound()
                        viewModel.setScreen(Screen.Shop)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("DARK RITUAL UPGRADES", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            AudioEngine.playFootstep()
                            viewModel.setScreen(Screen.Records)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("SURVIVORS", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            AudioEngine.playFootstep()
                            viewModel.setScreen(Screen.Settings)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Backstory
        Text(
            text = "Collect hidden mystical relics to open the exit gates.\nWatch the warning radar: stalking entities grow bolder and faster each stage.",
            color = GhostGrey.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    val map by viewModel.mazeMap.collectAsStateWithLifecycle()
    val px by viewModel.playerX.collectAsStateWithLifecycle()
    val py by viewModel.playerY.collectAsStateWithLifecycle()
    val lives by viewModel.lives.collectAsStateWithLifecycle()
    val health by viewModel.playerHealth.collectAsStateWithLifecycle()
    val stamina by viewModel.playerStamina.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val collected by viewModel.totalArtifactsCollected.collectAsStateWithLifecycle()
    val required by viewModel.stageArtifactsRequired.collectAsStateWithLifecycle()
    val monsters by viewModel.monstersList.collectAsStateWithLifecycle()
    val artifacts by viewModel.artifactsList.collectAsStateWithLifecycle()
    val powerups by viewModel.powerupsList.collectAsStateWithLifecycle()
    val portals by viewModel.portalsList.collectAsStateWithLifecycle()
    val activePowerups by viewModel.activePowerups.collectAsStateWithLifecycle()
    val isChased by viewModel.chasingMonsters.collectAsStateWithLifecycle()
    val gameOverState by viewModel.gameOverState.collectAsStateWithLifecycle()
    val gameRunning by viewModel.gameRunning.collectAsStateWithLifecycle()
    val upgrades by viewModel.upgrades.collectAsStateWithLifecycle()

    val maxHealthCap = (100 + upgrades.getBonusMaxHealth()).toFloat()

    val isShieldActive = activePowerups.any { it.type == "shield" }
    val isInvisibleActive = activePowerups.any { it.type == "invisibility" }
    val isSpeedActive = activePowerups.any { it.type == "speed" }

    BackHandler {
        viewModel.setScreen(Screen.Menu)
    }

    // Capture physical keyboard press (for Chromebooks/Emulators)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionUp, Key.W -> { viewModel.movePlayer(0, -1); true }
                        Key.DirectionDown, Key.S -> { viewModel.movePlayer(0, 1); true }
                        Key.DirectionLeft, Key.A -> { viewModel.movePlayer(-1, 0); true }
                        Key.DirectionRight, Key.D -> { viewModel.movePlayer(1, 0); true }
                        Key.ShiftLeft, Key.ShiftRight -> { viewModel.setSprinting(true); true }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    if (keyEvent.key == Key.ShiftLeft || keyEvent.key == Key.ShiftRight) {
                        viewModel.setSprinting(false)
                        true
                    } else false
                } else false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBackground)
                .statusBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Warning Info bar
            WarningIndicatorHUD(isChased, px, py, isInvisibleActive)

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics: Health, Stamina and Stage Level
            MetricsStatusBar(
                lives = lives,
                health = health,
                maxHealth = maxHealthCap,
                stamina = stamina,
                level = level,
                collected = collected,
                required = required
            )

            // Game Board Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, SpookyPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF070A0F)),
                contentAlignment = Alignment.Center
            ) {
                if (map.isNotEmpty()) {
                    DungeonCanvas(
                        map = map,
                        px = px,
                        py = py,
                        monsters = monsters,
                        artifacts = artifacts,
                        powerups = powerups,
                        portals = portals,
                        isShield = isShieldActive,
                        isInvis = isInvisibleActive,
                        isSpeed = isSpeedActive,
                        roomThemeIndex = level
                    )
                }

                // Game Over Overlays
                if (gameOverState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = if (gameOverState == "escaped") "RITUAL ESCAPED!" else "THE ENTITY CAUGHT YOU",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gameOverState == "escaped") ToxicGreen else BloodyRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = if (gameOverState == "escaped") 
                                    "You escaped after completing ${level + 1} stages survived!"
                                    else "Your survivor log was sealed on Stage ${level + 1}.",
                                fontSize = 14.sp,
                                color = SoftGrey,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.setScreen(Screen.Menu)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpookyPurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(200.dp)
                            ) {
                                Text("RETURN TO GATES", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Virtual Tactile Controllers
            TouchControlDeck(viewModel, stamina)
        }
    }
}

@Composable
fun WarningIndicatorHUD(
    chasingMonsters: List<Monster>,
    px: Float,
    py: Float,
    isInvisible: Boolean
) {
    var minDistance = Float.MAX_VALUE
    var closestMonster: Monster? = null

    if (chasingMonsters.isNotEmpty() && !isInvisible) {
        for (m in chasingMonsters) {
            val d = sqrt(((px - m.x) * (px - m.x) + (py - m.y) * (py - m.y)).toDouble()).toFloat()
            if (d < minDistance) {
                minDistance = d
                closestMonster = m
            }
        }
    }

    val warningColor = when {
        closestMonster == null -> Color.Gray.copy(alpha = 0.2f)
        minDistance < 5f -> BloodyRed
        minDistance < 10f -> NeonGold
        else -> SpookyPurple
    }

    val infiniteTransition = rememberInfiniteTransition(label = "warningFlash")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (minDistance < 5f) 300 else 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    val displayedMessage = when {
        closestMonster == null -> "MANOR SECURE • RECONRelics"
        isInvisible -> "👻 ACTIVE CAMOUFLAGE • PHANTOM EYE BLIND"
        minDistance < 5f -> "⚔️ !! DANGER: CLOAKED ${closestMonster.name.uppercase()} CHARGING !!"
        else -> "⚠️ SIGNS DETECTED: ${closestMonster.name.uppercase()} IS TRACKING"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(warningColor.copy(alpha = if (closestMonster != null) 0.15f else 0.05f))
            .border(1.dp, warningColor.copy(alpha = if (closestMonster != null) warningAlpha else 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(warningColor.copy(alpha = if (closestMonster != null) warningAlpha else 1f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = displayedMessage,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (closestMonster != null) warningColor else GhostGrey,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(letterSpacing = 1.sp)
        )
    }
}

@Composable
fun MetricsStatusBar(
    lives: Int,
    health: Float,
    maxHealth: Float,
    stamina: Float,
    level: Int,
    collected: Int,
    required: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PolishedNeutralBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level & Stage Indicators
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "STAGE ${level + 1}",
                    color = SpookyPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(letterSpacing = 1.sp)
                )

                val theme = ROOM_THEMES[(level / 6) % ROOM_THEMES.size]
                Text(
                    text = theme.name,
                    color = GhostGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Health & Stamina bars
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Health Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️", fontSize = 10.sp, modifier = Modifier.width(14.dp))
                    LinearProgressIndicator(
                        progress = { (health / maxHealth).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = BloodyRed,
                        trackColor = Color(0xFFFFD8D8)
                    )
                }

                // Stamina indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 10.sp, modifier = Modifier.width(14.dp))
                    LinearProgressIndicator(
                        progress = { (stamina / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = ToxicGreen,
                        trackColor = Color(0xFFC8E6C9)
                    )
                }
            }

            // Keys collected count
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "RELIQUE",
                    color = NeonGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(letterSpacing = 0.8.sp)
                )

                Text(
                    text = "🔑 $collected / $required",
                    color = DeepCharcoal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun DungeonCanvas(
    map: List<List<Char>>,
    px: Float,
    py: Float,
    monsters: List<Monster>,
    artifacts: List<Artifact>,
    powerups: List<PowerUp>,
    portals: List<Portal>,
    isShield: Boolean,
    isInvis: Boolean,
    isSpeed: Boolean,
    roomThemeIndex: Int
) {
    val theme = ROOM_THEMES[(roomThemeIndex / 6) % ROOM_THEMES.size]
    val wallColor = Color(android.graphics.Color.parseColor(theme.wallsHex))
    val floorColor = Color(android.graphics.Color.parseColor(theme.floorHex))

    // Animation variables for pulsing indicators
    val infiniteTransition = rememberInfiniteTransition(label = "pulseIndicators")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "indicator"
    )

    val portalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "portal"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val cols = map[0].size
        val rows = map.size
        
        // Calculate tileSize dynamically in DP objects
        val tileW = maxWidth / cols
        val tileH = maxHeight / rows
        val tileSize = if (tileW < tileH) tileW else tileH
        
        val containerWidth = tileSize * cols
        val containerHeight = tileSize * rows

        Box(
            modifier = Modifier
                .size(containerWidth, containerHeight)
                .align(Alignment.Center)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tileWPix = size.width / cols
                val tileHPix = size.height / rows

                // 1. Draw solid Floors & Walls Grid
                for (y in 0 until rows) {
                    for (x in 0 until cols) {
                        val tileX = x * tileWPix
                        val tileY = y * tileHPix
                        val char = map[y][x]

                        if (char == '#') {
                            drawRoundRect(
                                color = wallColor,
                                topLeft = Offset(tileX + 0.5f, tileY + 0.5f),
                                size = Size(tileWPix - 1f, tileHPix - 1f),
                                cornerRadius = CornerRadius(tileWPix * 0.2f, tileHPix * 0.2f)
                            )
                            // Dark inset overlay for depth
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.25f),
                                topLeft = Offset(tileX + tileWPix * 0.15f, tileY + tileHPix * 0.15f),
                                size = Size(tileWPix * 0.7f, tileHPix * 0.7f),
                                cornerRadius = CornerRadius(tileWPix * 0.15f, tileHPix * 0.15f)
                            )
                        } else {
                            drawRect(
                                color = floorColor.copy(alpha = 0.8f),
                                topLeft = Offset(tileX, tileY),
                                size = Size(tileWPix, tileHPix)
                            )
                        }
                    }
                }

                // 2. Draw rotating Portals (🌀)
                for (portal in portals) {
                    val tileX = portal.x * tileWPix + tileWPix / 2f
                    val tileY = portal.y * tileHPix + tileHPix / 2f
                    val radius = min(tileWPix, tileHPix) * 0.45f

                    // Dual rotating arcs representing gothic portals
                    withTransform({
                        rotate(portalRotation, pivot = Offset(tileX, tileY))
                    }) {
                        drawCircle(
                            color = Color(0xFF6B46C1).copy(alpha = 0.2f),
                            radius = radius * 1.1f,
                            center = Offset(tileX, tileY)
                        )
                        drawArc(
                            color = Color(0xFF3182CE),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(tileX - radius, tileY - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = min(tileWPix, tileHPix) * 0.15f)
                        )
                        drawArc(
                            color = Color(0xFF805AD5),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(tileX - radius, tileY - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = min(tileWPix, tileHPix) * 0.15f)
                        )
                    }
                }

                // 3. Draw Collectable Artifact Keys
                for (art in artifacts) {
                    if (!art.found) {
                        val tileX = art.x * tileWPix + tileWPix / 2f
                        val tileY = art.y * tileHPix + tileHPix / 2f
                        
                        // Pulsing golden aura
                        drawCircle(
                            color = Color(0xFFECC94B).copy(alpha = 0.15f),
                            radius = min(tileWPix, tileHPix) * 0.55f * pulseSize,
                            center = Offset(tileX, tileY)
                        )
                    }
                }

                // 4. Draw Buff Powerups (🛡️, ⚡, ✨)
                for (pw in powerups) {
                    val tileX = pw.x * tileWPix + tileWPix / 2f
                    val tileY = pw.y * tileHPix + tileHPix / 2f
                    val auraColor = when (pw.type) {
                        "shield" -> Color(0xFF3182CE)
                        "speed" -> Color(0xFFECC94B)
                        else -> Color(0xFF805AD5) // invisibility
                    }
                    
                    drawCircle(
                        color = auraColor.copy(alpha = 0.2f),
                        radius = min(tileWPix, tileHPix) * 0.55f * pulseSize,
                        center = Offset(tileX, tileY)
                    )
                }

                // 5. Draw active threat Entities with surrounding neon glow ring
                for (m in monsters) {
                    val tileX = m.x * tileWPix + tileWPix / 2f
                    val tileY = m.y * tileHPix + tileHPix / 2f
                    
                    // Threat glow boundaries
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.12f),
                        radius = min(tileWPix, tileHPix) * 0.65f * pulseSize,
                        center = Offset(tileX, tileY)
                    )
                }

                // 6. Draw Player halo effect representations
                val playX = px * tileWPix + tileWPix / 2f
                val playY = py * tileHPix + tileHPix / 2f

                if (isShield) {
                    // Shield aura sphere representation
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                        radius = min(tileWPix, tileHPix) * 0.75f,
                        center = Offset(playX, playY),
                        style = Stroke(width = min(tileWPix, tileHPix) * 0.10f)
                    )
                }
                if (isSpeed) {
                    // Sprint speed dust rays
                    drawCircle(
                        color = Color(0xFF39FF14).copy(alpha = 0.2f),
                        radius = min(tileWPix, tileHPix) * 0.65f,
                        center = Offset(playX, playY),
                        style = Stroke(width = min(tileWPix, tileHPix) * 0.05f)
                    )
                }
            }

            // 7. Render emojis precisely centered on tiles via Overlay Box layers (highly robust)
            // Artifact texts
            artifacts.forEach { art ->
                if (!art.found) {
                    Text(
                        text = art.char,
                        fontSize = (tileSize.value * 0.7f).sp,
                        modifier = Modifier
                            .offset(
                                x = (tileSize.value * art.x).dp,
                                y = (tileSize.value * art.y).dp
                            )
                            .size(tileSize),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Powerup texts
            powerups.forEach { pu ->
                Text(
                    text = pu.char,
                    fontSize = (tileSize.value * 0.7f).sp,
                    modifier = Modifier
                        .offset(
                            x = (tileSize.value * pu.x).dp,
                            y = (tileSize.value * pu.y).dp
                        )
                        .size(tileSize),
                    textAlign = TextAlign.Center
                )
            }

            // Monster texts
            monsters.forEach { m ->
                Text(
                    text = m.char,
                    fontSize = (tileSize.value * 0.75f).sp,
                    modifier = Modifier
                        .offset(
                            x = (tileSize.value * m.x).dp,
                            y = (tileSize.value * m.y).dp
                        )
                        .size(tileSize),
                    textAlign = TextAlign.Center
                )
            }

            // Player character skull (💀) with opacity bindings
            val alphaValue = if (isInvis) 0.45f else 1.0f
            Text(
                text = "💀",
                fontSize = (tileSize.value * 0.8f).sp,
                modifier = Modifier
                    .offset(
                        x = (tileSize.value * px).dp,
                        y = (tileSize.value * py).dp
                    )
                    .size(tileSize),
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = alphaValue)
            )
        }
    }
}

@Composable
fun DPADButton(
    icon: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (isPressed) {
                onPress()
                delay(120) // continuous touch tick
            }
        }
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(PolishedPurple, PolishedPurple.copy(alpha = 0.7f))
                    } else {
                        listOf(LightVioletCard, PolishedNeutralCard)
                    }
                )
            )
            .border(1.dp, PolishedNeutralBorder, CircleShape)
            .pointerInput(onPress) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    AudioEngine.playFootstep()
                    waitForUpOrCancellation()
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            color = if (isPressed) Color.White else DeepVioletText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TouchControlDeck(viewModel: GameViewModel, stamina: Float) {
    var isSprinting by remember { mutableStateOf(false) }

    // D-Pad and Sprint panel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Action column: Sprint Trigger Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSprinting && stamina > 0) {
                            Brush.verticalGradient(listOf(BloodyRed, GothicCrimson))
                        } else {
                            Brush.verticalGradient(listOf(LightVioletCard, PolishedNeutralCard))
                        }
                    )
                    .border(
                        1.dp,
                        if (isSprinting) BloodyRed else PolishedNeutralBorder,
                        CircleShape
                    )
                    .pointerInput(viewModel, stamina) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            if (stamina > 10f) {
                                isSprinting = true
                                viewModel.setSprinting(true)
                                AudioEngine.playPowerUpSound()
                            }
                            waitForUpOrCancellation()
                            isSprinting = false
                            viewModel.setSprinting(false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SPRINT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSprinting && stamina > 0) Color.White else DeepVioletText
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "HOLD Turbo",
                fontSize = 10.sp,
                color = GhostGrey,
                style = TextStyle(letterSpacing = 0.5.sp)
            )
        }

        // Right controller deck: Intuitive cross-like tactile D-pad Layout
        Box(
            modifier = Modifier
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circular decorative seal
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .border(0.5.dp, SpookyPurple.copy(alpha = 0.15f), CircleShape)
            )

            // Cross directions placement
            DPADButton("▲", Modifier.align(Alignment.TopCenter)) {
                viewModel.movePlayer(0, -1)
            }
            DPADButton("◀", Modifier.align(Alignment.CenterStart)) {
                viewModel.movePlayer(-1, 0)
            }
            DPADButton("▶", Modifier.align(Alignment.CenterEnd)) {
                viewModel.movePlayer(1, 0)
            }
            DPADButton("▼", Modifier.align(Alignment.BottomCenter)) {
                viewModel.movePlayer(0, 1)
            }
            
            // Core centered decorative skull
            Text("🔮", fontSize = 16.sp)
        }
    }
}

@Composable
fun DarkShopScreen(viewModel: GameViewModel) {
    val upgrades by viewModel.upgrades.collectAsStateWithLifecycle()
    val shards by viewModel.manaShards.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.setScreen(Screen.Menu)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Panel Header
        Text(
            text = "ALTAR OF DESCENSION",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                shadow = Shadow(color = SpookyPurple.copy(alpha = 0.4f), blurRadius = 8f)
            ),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        Text(
            text = "Spend collected mystical relics as currency inside the manor gates.",
            color = GhostGrey,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Shards Vault Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, NeonGold.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🔑", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Mana Relics Available", fontSize = 11.sp, color = GhostGrey)
                    Text("$shards Relics", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NeonGold)
                }
            }
        }

        // Shop Items Scroll block
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                UpgradeItemCard(
                    title = "Iron Heart Blessing",
                    description = "Permanently increases Max Health attribute by +20 points.",
                    level = upgrades.ironHeartLevel,
                    cost = 1 + upgrades.ironHeartLevel,
                    icon = "❤️",
                    shardsAvailable = shards
                ) {
                    viewModel.buyUpgrade("iron")
                }
            }

            item {
                UpgradeItemCard(
                    title = "Ascetic Lungs blessing",
                    description = "Dramatically lifts turbo Sprint stamina recovery rate by +35%.",
                    level = upgrades.asceticLungsLevel,
                    cost = 2 + upgrades.asceticLungsLevel,
                    icon = "⚡",
                    shardsAvailable = shards
                ) {
                    viewModel.buyUpgrade("lungs")
                }
            }

            item {
                UpgradeItemCard(
                    title = "Witch's Veil Aura",
                    description = "Extends the duration of active invisibility buffer by +3 seconds.",
                    level = upgrades.witchVeilLevel,
                    cost = 2 + upgrades.witchVeilLevel,
                    icon = "✨",
                    shardsAvailable = shards
                ) {
                    viewModel.buyUpgrade("veil")
                }
            }

            item {
                UpgradeItemCard(
                    title = "Ghostly Steps Boots",
                    description = "Hastens default walking / movement coordination speeds by +12%.",
                    level = upgrades.ghostlyStepsLevel,
                    cost = 3 + upgrades.ghostlyStepsLevel,
                    icon = "👢",
                    shardsAvailable = shards
                ) {
                    viewModel.buyUpgrade("steps")
                }
            }

            item {
                UpgradeItemCard(
                    title = "Divine Shield Bubble",
                    description = "Spawns with a high protection shield coverage bubble active.",
                    level = upgrades.divineShieldLevel,
                    cost = 3 + upgrades.divineShieldLevel,
                    icon = "🛡️",
                    shardsAvailable = shards
                ) {
                    viewModel.buyUpgrade("shield")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Return Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.setScreen(Screen.Menu)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BACK TO GATES")
            }

            Button(
                onClick = {
                    viewModel.proceedToNextLevel()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpookyPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ENTER STAGE ${level + 2}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UpgradeItemCard(
    title: String,
    description: String,
    level: Int,
    cost: Int,
    icon: String,
    shardsAvailable: Int,
    onPurchase: () -> Unit
) {
    val canAfford = shardsAvailable >= cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(1.dp, PolishedNeutralBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left graphical asset
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LightVioletCard),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body text information
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                Text(description, fontSize = 11.sp, color = GhostGrey, lineHeight = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "Lvl $level",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishedPurple,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = "Cost: $cost Relics",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) NeonGold else BloodyRed
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Purchase dynamic action card
            Button(
                onClick = onPurchase,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PolishedPurple,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Black.copy(alpha = 0.05f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("BUY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun SurvivorRecordsScreen(viewModel: GameViewModel) {
    val records by viewModel.survivorHistory.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    BackHandler {
        viewModel.setScreen(Screen.Menu)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Log Header
        Text(
            text = "SURVIVOR JOURNAL",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.error,
                shadow = Shadow(color = GothicCrimson.copy(alpha = 0.5f), blurRadius = 8f)
            ),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        Text(
            text = "Historic archive of previous souls trapped in the Cursed Manor.",
            color = GhostGrey,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Records Scroll area
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕯️", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No logs available yet...",
                        fontSize = 14.sp,
                        color = GhostGrey,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "The manor gates lie silent and waiting.",
                        fontSize = 11.sp,
                        color = GhostGrey.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records) { record ->
                    SurvivorLogCard(record, dateFormat)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.setScreen(Screen.Menu)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RETURN")
            }

            if (records.isNotEmpty()) {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("PURGE LOGS")
                }
            }
        }
    }
}

@Composable
fun SurvivorLogCard(record: SurvivorRecord, dateFormat: SimpleDateFormat) {
    val blockColor = if (record.isEscape) ToxicGreen else BloodyRed
    val formattedDate = remember(record.dateTimestamp) {
        dateFormat.format(Date(record.dateTimestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PolishedNeutralBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (record.isEscape) "🕯️ RITUAL COMPLETED" else "💀 CONSUMED BY ENTITY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = blockColor
                )

                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = GhostGrey
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = PolishedNeutralBorder)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Max Stage reached", fontSize = 9.sp, color = GhostGrey)
                    Text("Stage ${record.stageReached}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                }

                Column {
                    Text("Mana relics found", fontSize = 9.sp, color = GhostGrey)
                    Text("🔑 ${record.artifactsCollected}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGold)
                }

                Column {
                    Text("Survival run duration", fontSize = 9.sp, color = GhostGrey)
                    Text("${record.timeSurvivedSeconds}s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = record.deathReason,
                fontSize = 11.sp,
                color = GhostGrey,
                style = TextStyle(fontFamily = FontFamily.Monospace)
            )
        }
    }
}

@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.setScreen(Screen.Menu)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM SETTINGS",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                shadow = Shadow(color = SpookyPurple.copy(alpha = 0.4f), blurRadius = 8f)
            ),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        Text(
            text = "Fine-tune synthesizer sound settings and diagnostic capabilities.",
            color = GhostGrey,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Mutex Toggle box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, PolishedNeutralBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Spooky Sound Effects", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                    Text("Enable/Disable synthetic horror sound.", fontSize = 11.sp, color = GhostGrey)
                }

                Switch(
                    checked = !isMuted,
                    onCheckedChange = {
                        viewModel.toggleMute()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PolishedPurple,
                        checkedTrackColor = PolishedPurple.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // Diagnoses Playback Tone check
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, PolishedNeutralBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Synthesizer Diagnostic Tone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                Text("Triggers deep FM sweep synthetic test.", fontSize = 11.sp, color = GhostGrey)
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        AudioEngine.playArtifactSound()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpookyPurple),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("TRIGGER SYNTH TEST TONE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Return core button
        Button(
            onClick = {
                viewModel.setScreen(Screen.Menu)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("RETURN TO GATES")
        }
    }
}
