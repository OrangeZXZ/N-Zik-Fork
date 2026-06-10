package app.n_zik.android.extensions.games.pacman

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.it.fast4x.rimusic.ui.components.navigation.header.AppHeader
import app.it.fast4x.rimusic.utils.bold
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- State and Logic ---

enum class GameState {
    IDLE, STARTED, PAUSED, GAME_OVER, WON
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT, NONE
}

data class Coordinate(val x: Int, val y: Int)

data class Ghost(
    var position: Coordinate,
    var direction: Direction,
    val color: Color,
    val id: Int
)

val PACMAN_MAP = listOf(
    "WWWWWWWWWWWWWWWWWWWWW", // 0
    "W.........W.........W", // 1
    "W*WWW.WWW.W.WWW.WWW*W", // 2
    "W...................W", // 3
    "W.WWW.W.WWWWW.W.WWW.W", // 4
    "W.....W...W...W.....W", // 5
    "WWWWW.WWW W WWW.WWWWW", // 6
    "    W.W       W.W    ", // 7
    "WWWWW.W WW WW W.WWWWW", // 8
    "     .  W   W  .     ", // 9 (ghosts start near center)
    "WWWWW.W WWWWW W.WWWWW", // 10
    "    W.W       W.W    ", // 11
    "WWWWW.W WWWWW W.WWWWW", // 12
    "W.........W.........W", // 13
    "W.WWW.WWW.W.WWW.WWW.W", // 14
    "W...W...........W...W", // 15 (pacman start at 10,15)
    "WWW.W.W.WWWWW.W.W.WWW", // 16
    "W.....W...W...W.....W", // 17
    "W.WWWWWWW.W.WWWWWWW.W", // 18
    "W*.................*W", // 19
    "WWWWWWWWWWWWWWWWWWWWW"  // 20
)

data class PacmanGameState(
    val pacmanPos: Coordinate = Coordinate(10, 15),
    val pacmanDir: Direction = Direction.NONE,
    val queuedDir: Direction = Direction.NONE,
    val ghosts: List<Ghost> = listOf(
        Ghost(Coordinate(9, 9), Direction.UP, Color(0xFFFF0000), 1), // Red
        Ghost(Coordinate(11, 9), Direction.UP, Color(0xFFFFB8FF), 2), // Pink
        Ghost(Coordinate(9, 11), Direction.DOWN, Color(0xFF00FFFF), 3), // Cyan
        Ghost(Coordinate(11, 11), Direction.DOWN, Color(0xFFFFB852), 4)  // Orange
    ),
    val score: Int = 0,
    val dots: Set<Coordinate> = emptySet(),
    val powerDots: Set<Coordinate> = emptySet(),
    val gameState: GameState = GameState.IDLE,
    val powerModeTicks: Int = 0,
    val tickCount: Int = 0
) {
    companion object {
        fun initialize(): PacmanGameState {
            val initialDots = mutableSetOf<Coordinate>()
            val initialPowerDots = mutableSetOf<Coordinate>()
            
            for (y in PACMAN_MAP.indices) {
                for (x in PACMAN_MAP[y].indices) {
                    val char = PACMAN_MAP[y][x]
                    if (char == '.') initialDots.add(Coordinate(x, y))
                    if (char == '*') initialPowerDots.add(Coordinate(x, y))
                }
            }
            
            return PacmanGameState(
                dots = initialDots,
                powerDots = initialPowerDots
            )
        }
    }
}

sealed class PacmanEvent {
    data object StartGame : PacmanEvent()
    data object PauseGame : PacmanEvent()
    data object ResetGame : PacmanEvent()
    data class ChangeDirection(val direction: Direction) : PacmanEvent()
}

class PacmanViewModel : ViewModel() {
    private val _state = MutableStateFlow(PacmanGameState.initialize())
    val state = _state.asStateFlow()

    fun onEvent(event: PacmanEvent) {
        when (event) {
            PacmanEvent.StartGame -> {
                _state.update { it.copy(gameState = GameState.STARTED) }
                viewModelScope.launch {
                    while (state.value.gameState == GameState.STARTED) {
                        delay(150L) // Game speed
                        _state.value = updateGame(state.value)
                    }
                }
            }
            PacmanEvent.PauseGame -> {
                _state.update { it.copy(gameState = GameState.PAUSED) }
            }
            PacmanEvent.ResetGame -> {
                _state.value = PacmanGameState.initialize()
            }
            is PacmanEvent.ChangeDirection -> {
                if (state.value.gameState == GameState.STARTED || state.value.gameState == GameState.IDLE) {
                    _state.update { it.copy(queuedDir = event.direction) }
                }
            }
        }
    }

    private fun updateGame(currentState: PacmanGameState): PacmanGameState {
        if (currentState.gameState != GameState.STARTED) return currentState

        val newTickCount = currentState.tickCount + 1
        var pacmanDir = currentState.pacmanDir
        var queuedDir = currentState.queuedDir

        // Try to apply queued direction
        if (queuedDir != Direction.NONE) {
            val nextPos = getNextPosition(currentState.pacmanPos, queuedDir)
            if (!isWall(nextPos)) {
                pacmanDir = queuedDir
                queuedDir = Direction.NONE // Successfully applied
            }
        }

        // Move Pacman
        var nextPacmanPos = getNextPosition(currentState.pacmanPos, pacmanDir)
        if (isWall(nextPacmanPos)) {
            nextPacmanPos = currentState.pacmanPos // Stop if hit wall
        }

        // Teleport tunnel (sides of the map)
        if (nextPacmanPos.x < 0) nextPacmanPos = nextPacmanPos.copy(x = 20)
        if (nextPacmanPos.x > 20) nextPacmanPos = nextPacmanPos.copy(x = 0)

        // Consume dots
        val newDots = currentState.dots.toMutableSet()
        val newPowerDots = currentState.powerDots.toMutableSet()
        var newScore = currentState.score
        var newPowerTicks = currentState.powerModeTicks

        if (newDots.remove(nextPacmanPos)) {
            newScore += 10
        }
        if (newPowerDots.remove(nextPacmanPos)) {
            newScore += 50
            newPowerTicks = 40 // ~6 seconds of power mode
        }

        if (newPowerTicks > 0) newPowerTicks--

        // Win condition
        if (newDots.isEmpty() && newPowerDots.isEmpty()) {
            return currentState.copy(
                pacmanPos = nextPacmanPos,
                dots = newDots,
                powerDots = newPowerDots,
                score = newScore,
                gameState = GameState.WON
            )
        }

        // Move Ghosts (half speed)
        val newGhosts = if (newTickCount % 2 == 0) {
            currentState.ghosts.map { ghost ->
                val possibleDirs = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                    .filter { it != getOppositeDirection(ghost.direction) }
                
                val validDirs = possibleDirs.filter { !isWall(getNextPosition(ghost.position, it)) }
                
                val nextDir = if (validDirs.isEmpty()) {
                    getOppositeDirection(ghost.direction) // Dead end
                } else if (!validDirs.contains(ghost.direction) || Random.nextFloat() < 0.2f) { // 20% chance to turn if at intersection
                    validDirs.random()
                } else {
                    ghost.direction
                }

                var nextGhostPos = getNextPosition(ghost.position, nextDir)
                if (nextGhostPos.x < 0) nextGhostPos = nextGhostPos.copy(x = 20)
                if (nextGhostPos.x > 20) nextGhostPos = nextGhostPos.copy(x = 0)

                ghost.copy(position = nextGhostPos, direction = nextDir)
            }
        } else {
            currentState.ghosts
        }

        // Collision logic
        var isGameOver = false
        val finalGhosts = newGhosts.toMutableList()
        for (i in finalGhosts.indices) {
            val ghost = finalGhosts[i]
            if (ghost.position == nextPacmanPos || ghost.position == currentState.pacmanPos) {
                if (newPowerTicks > 0) {
                    // Eat ghost
                    newScore += 200
                    finalGhosts[i] = ghost.copy(position = Coordinate(10, 10)) // Send to center cage
                } else {
                    isGameOver = true
                }
            }
        }

        return currentState.copy(
            pacmanPos = nextPacmanPos,
            pacmanDir = pacmanDir,
            queuedDir = queuedDir,
            ghosts = finalGhosts,
            dots = newDots,
            powerDots = newPowerDots,
            score = newScore,
            powerModeTicks = newPowerTicks,
            tickCount = newTickCount,
            gameState = if (isGameOver) GameState.GAME_OVER else GameState.STARTED
        )
    }

    private fun getNextPosition(pos: Coordinate, dir: Direction): Coordinate {
        return when (dir) {
            Direction.UP -> Coordinate(pos.x, pos.y - 1)
            Direction.DOWN -> Coordinate(pos.x, pos.y + 1)
            Direction.LEFT -> Coordinate(pos.x - 1, pos.y)
            Direction.RIGHT -> Coordinate(pos.x + 1, pos.y)
            Direction.NONE -> pos
        }
    }

    private fun getOppositeDirection(dir: Direction): Direction {
        return when (dir) {
            Direction.UP -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.LEFT -> Direction.RIGHT
            Direction.RIGHT -> Direction.LEFT
            Direction.NONE -> Direction.NONE
        }
    }

    private fun isWall(pos: Coordinate): Boolean {
        if (pos.y < 0 || pos.y >= PACMAN_MAP.size) return true
        if (pos.x < 0 || pos.x >= PACMAN_MAP[pos.y].length) return false // tunnel bypass
        return PACMAN_MAP[pos.y][pos.x] == 'W'
    }
}

// --- UI ---

@Composable
fun Pacman(navController: NavController) {
    val viewModel: PacmanViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    PacmanGameScreen(
        state = state,
        onEvent = viewModel::onEvent,
        navController = navController
    )
}

@Composable
fun PacmanGameScreen(
    state: PacmanGameState,
    onEvent: (PacmanEvent) -> Unit,
    navController: NavController
) {
    val ghostRed = ImageBitmap.imageResource(R.drawable.ghost_red)
    val ghostOrange = ImageBitmap.imageResource(R.drawable.ghost_orange)
    val ghostPink = ImageBitmap.imageResource(R.drawable.ghost_pink)
    val ghostBlue = ImageBitmap.imageResource(R.drawable.ghost_blue)
    val ghostScared = ImageBitmap.imageResource(R.drawable.ghost_reverse)
    val pacmanTittle = ImageBitmap.imageResource(R.drawable.pacman_tittle)

    val animatedPacmanX by animateFloatAsState(
        targetValue = state.pacmanPos.x.toFloat(),
        animationSpec = tween(150, easing = LinearEasing)
    )
    val animatedPacmanY by animateFloatAsState(
        targetValue = state.pacmanPos.y.toFloat(),
        animationSpec = tween(150, easing = LinearEasing)
    )

    val animatedGhosts = state.ghosts.map { ghost ->
        val duration = if (state.powerModeTicks > 0) 450 else 300
        val x by animateFloatAsState(
            targetValue = ghost.position.x.toFloat(),
            animationSpec = tween(duration, easing = LinearEasing)
        )
        val y by animateFloatAsState(
            targetValue = ghost.position.y.toFloat(),
            animationSpec = tween(duration, easing = LinearEasing)
        )
        Pair(x, y)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorPalette().background0,
        topBar = {
            AppHeader(navController).Draw()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette().background0)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = pacmanTittle,
                        contentDescription = "Pacman Logo",
                        modifier = Modifier.height(64.dp),
                        contentScale = ContentScale.FillHeight
                    )
                }
                Card(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorPalette().background1),
                    shape = uiRoundnessShape()
                ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.snake_score, state.score),
                    style = typography().l.bold,
                    color = colorPalette().text
                )
            }

            val wallColor = Color(0xFF1919A6) // Classic blue walls
            val dotColor = Color(0xFFFFB8AE) // Peach dots
            val pacmanColor = Color(0xFFFFFF00) // Yellow

            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .background(Color.Black)
            ) {
                val cols = 21
                val rows = 21
                val cellWidth = size.width / cols
                val cellHeight = size.height / rows
                val cellSize = minOf(cellWidth, cellHeight)

                // Center the maze if aspect ratio is perfectly square
                val xOffset = (size.width - (cols * cellSize)) / 2f
                val yOffset = (size.height - (rows * cellSize)) / 2f

                // Draw Walls
                for (y in PACMAN_MAP.indices) {
                    for (x in PACMAN_MAP[y].indices) {
                        if (PACMAN_MAP[y][x] == 'W') {
                            drawRect(
                                color = wallColor,
                                topLeft = Offset(xOffset + x * cellSize, yOffset + y * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }

                // Draw Dots
                state.dots.forEach { dot ->
                    drawCircle(
                        color = dotColor,
                        center = Offset(
                            xOffset + dot.x * cellSize + cellSize / 2f,
                            yOffset + dot.y * cellSize + cellSize / 2f
                        ),
                        radius = cellSize / 6f
                    )
                }

                // Draw Power Dots
                state.powerDots.forEach { pDot ->
                    drawCircle(
                        color = dotColor,
                        center = Offset(
                            xOffset + pDot.x * cellSize + cellSize / 2f,
                            yOffset + pDot.y * cellSize + cellSize / 2f
                        ),
                        radius = cellSize / 2.5f
                    )
                }

                // Draw Pacman
                val mouthAnim = (state.tickCount % 4)
                // Use animatedPacmanX and Y fractional parts to smooth the mouth opening
                val fraction = (animatedPacmanX % 1f + animatedPacmanY % 1f) % 1f
                val mouthAngle = 45f * if (fraction < 0.5f) fraction * 2f else (1f - fraction) * 2f
                
                val startAngle = when (state.pacmanDir) {
                    Direction.RIGHT, Direction.NONE -> mouthAngle
                    Direction.DOWN -> 90f + mouthAngle
                    Direction.LEFT -> 180f + mouthAngle
                    Direction.UP -> 270f + mouthAngle
                }
                val sweepAngle = 360f - (2 * mouthAngle)

                drawArc(
                    color = pacmanColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(
                        xOffset + animatedPacmanX * cellSize + cellSize * 0.1f,
                        yOffset + animatedPacmanY * cellSize + cellSize * 0.1f
                    ),
                    size = Size(cellSize * 0.8f, cellSize * 0.8f)
                )

                // Draw Ghosts
                state.ghosts.forEachIndexed { index, ghost ->
                    val isVulnerable = state.powerModeTicks > 0
                    val isFlashing = isVulnerable && state.powerModeTicks < 15 && state.powerModeTicks % 2 == 0
                    
                    val baseBitmap = when (index % 4) {
                        0 -> ghostRed
                        1 -> ghostPink
                        2 -> ghostBlue
                        else -> ghostOrange
                    }
                    val bitmap = if (isVulnerable && !isFlashing) ghostScared else baseBitmap
                    
                    val animPos = animatedGhosts[index]
                    val left = xOffset + animPos.first * cellSize + cellSize * 0.1f
                    val top = yOffset + animPos.second * cellSize + cellSize * 0.1f
                    val width = cellSize * 0.8f
                    val height = cellSize * 0.8f

                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = IntSize(width.toInt(), height.toInt())
                    )
                }
            }

            // Directional Pad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onEvent(PacmanEvent.ChangeDirection(Direction.UP)) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(colorPalette().background1, shape = CircleShape)
                    ) {
                        Icon(painterResource(R.drawable.arrow_up), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                    }
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(52.dp)
                    ) {
                        IconButton(
                            onClick = { onEvent(PacmanEvent.ChangeDirection(Direction.LEFT)) },
                            modifier = Modifier
                                .size(52.dp)
                                .background(colorPalette().background1, shape = CircleShape)
                        ) {
                            Icon(painterResource(R.drawable.arrow_left), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                        }
                        IconButton(
                            onClick = { onEvent(PacmanEvent.ChangeDirection(Direction.RIGHT)) },
                            modifier = Modifier
                                .size(52.dp)
                                .background(colorPalette().background1, shape = CircleShape)
                        ) {
                            Icon(painterResource(R.drawable.arrow_right), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                        }
                    }
                    IconButton(
                        onClick = { onEvent(PacmanEvent.ChangeDirection(Direction.DOWN)) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(colorPalette().background1, shape = CircleShape)
                    ) {
                        Icon(painterResource(R.drawable.arrow_down), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(PacmanEvent.ResetGame) },
                    enabled = state.gameState == GameState.PAUSED || state.gameState == GameState.GAME_OVER || state.gameState == GameState.WON
                ) {
                    Text(text = stringResource(if (state.gameState == GameState.GAME_OVER || state.gameState == GameState.WON) R.string.snake_new_game else R.string.snake_reset)) // Reuse strings for simplicity
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        when (state.gameState) {
                            GameState.IDLE, GameState.PAUSED -> onEvent(PacmanEvent.StartGame)
                            GameState.STARTED -> onEvent(PacmanEvent.PauseGame)
                            else -> {}
                        }
                    },
                    enabled = state.gameState != GameState.GAME_OVER && state.gameState != GameState.WON
                ) {
                    Text(
                        text = stringResource(when (state.gameState) {
                            GameState.IDLE -> R.string.snake_start
                            GameState.STARTED -> R.string.snake_pause
                            else -> R.string.snake_resume
                        })
                    )
                }
            }
        }

        }
        
        if (state.gameState == GameState.GAME_OVER || state.gameState == GameState.WON) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = pacmanTittle,
                        contentDescription = "Pacman Logo",
                        modifier = Modifier.height(100.dp),
                        contentScale = ContentScale.FillHeight
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(if (state.gameState == GameState.WON) R.string.pacman_you_won else R.string.pacman_game_over),
                        style = typography().xxl.bold,
                        color = if (state.gameState == GameState.WON) colorPalette().accent else colorPalette().red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Score: ${state.score}",
                        style = typography().l.bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                onEvent(PacmanEvent.ResetGame)
                                onEvent(PacmanEvent.StartGame)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPalette().accent,
                                contentColor = colorPalette().text
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.snake_retry),
                                style = typography().m.bold
                            )
                        }
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPalette().background2,
                                contentColor = colorPalette().text
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.snake_exit),
                                style = typography().m.bold
                            )
                        }
                    }
                }
            }
        }
    }
}
