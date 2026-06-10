package app.n_zik.android.extensions.games.snake

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.compose.material3.Scaffold
import app.it.fast4x.rimusic.ui.components.navigation.header.AppHeader
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.utils.bold
import app.it.fast4x.rimusic.utils.secondary
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

// --- State and Logic ---

enum class GameState {
    IDLE,
    STARTED,
    PAUSED
}

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

data class Coordinate(
    val x: Int,
    val y: Int
)

data class SnakeGameState(
    val xAxisGridSize: Int = 20,
    val yAxisGridSize: Int = 20,
    val direction: Direction = Direction.RIGHT,
    val snake: List<Coordinate> = listOf(Coordinate(x = 5, y = 5)),
    val food: Coordinate = generateRandomFoodCoordinate(),
    val isGameOver: Boolean = false,
    val gameState: GameState = GameState.IDLE
) {
    companion object {
        fun generateRandomFoodCoordinate(): Coordinate {
            return Coordinate(
                x = Random.nextInt(from = 1, until = 19),
                y = Random.nextInt(from = 1, until = 19)
            )
        }
    }
}

sealed class SnakeGameEvent {
    data object StartGame : SnakeGameEvent()
    data object PauseGame : SnakeGameEvent()
    data object ResetGame : SnakeGameEvent()
    data class UpdateDirection(val offset: Offset, val canvasWidth: Int) : SnakeGameEvent()
    data class ChangeDirection(val direction: Direction) : SnakeGameEvent()
}

class SnakeGameViewModel : ViewModel() {

    private val _state = MutableStateFlow(SnakeGameState())
    val state = _state.asStateFlow()

    fun onEvent(event: SnakeGameEvent) {
        when (event) {
            SnakeGameEvent.StartGame -> {
                _state.update { it.copy(gameState = GameState.STARTED) }
                viewModelScope.launch {
                    while (state.value.gameState == GameState.STARTED) {
                        val delayMillis = when (state.value.snake.size) {
                            in 1..5 -> 120L
                            in 6..10 -> 110L
                            else -> 100L
                        }
                        delay(delayMillis)
                        _state.value = updateGame(state.value)
                    }
                }
            }
            SnakeGameEvent.PauseGame -> {
                _state.update { it.copy(gameState = GameState.PAUSED) }
            }
            SnakeGameEvent.ResetGame -> {
                _state.value = SnakeGameState()
            }
            is SnakeGameEvent.UpdateDirection -> {
                updateDirection(event.offset, event.canvasWidth)
            }
            is SnakeGameEvent.ChangeDirection -> {
                if (!state.value.isGameOver) {
                    val currentDirection = state.value.direction
                    val newDirection = event.direction
                    // Prevent reversing direction
                    val isValidChange = when (currentDirection) {
                        Direction.UP -> newDirection != Direction.DOWN
                        Direction.DOWN -> newDirection != Direction.UP
                        Direction.LEFT -> newDirection != Direction.RIGHT
                        Direction.RIGHT -> newDirection != Direction.LEFT
                    }
                    if (isValidChange) {
                        _state.update { it.copy(direction = newDirection) }
                    }
                }
            }
        }
    }

    private fun updateDirection(offset: Offset, canvasWidth: Int) {
        if (!state.value.isGameOver) {
            val cellSize = canvasWidth / state.value.xAxisGridSize
            val tapX = (offset.x / cellSize).toInt()
            val tapY = (offset.y / cellSize).toInt()
            val head = state.value.snake.first()

            _state.update {
                it.copy(
                    direction = when (state.value.direction) {
                        Direction.UP, Direction.DOWN -> {
                            if (tapX < head.x) Direction.LEFT else Direction.RIGHT
                        }
                        Direction.LEFT, Direction.RIGHT -> {
                            if (tapY < head.y) Direction.UP else Direction.DOWN
                        }
                    }
                )
            }
        }
    }

    private fun updateGame(currentGame: SnakeGameState): SnakeGameState {
        if (currentGame.isGameOver) {
            return currentGame
        }

        val head = currentGame.snake.first()
        val xAxisGridSize = currentGame.xAxisGridSize
        val yAxisGridSize = currentGame.yAxisGridSize

        val newHead = when (currentGame.direction) {
            Direction.UP -> Coordinate(x = head.x, y = (head.y - 1))
            Direction.DOWN -> Coordinate(x = head.x, y = (head.y + 1))
            Direction.LEFT -> Coordinate(x = head.x - 1, y = (head.y))
            Direction.RIGHT -> Coordinate(x = head.x + 1, y = (head.y))
        }

        if (currentGame.snake.contains(newHead) || !isWithinBounds(newHead, xAxisGridSize, yAxisGridSize)) {
            return currentGame.copy(isGameOver = true)
        }

        var newSnake = mutableListOf(newHead) + currentGame.snake
        val newFood = if (newHead == currentGame.food) SnakeGameState.generateRandomFoodCoordinate() else currentGame.food

        if (newHead != currentGame.food) {
            newSnake = newSnake.toMutableList()
            newSnake.removeAt(newSnake.size - 1)
        }

        return currentGame.copy(snake = newSnake, food = newFood)
    }

    private fun isWithinBounds(coordinate: Coordinate, xAxisGridSize: Int, yAxisGridSize: Int): Boolean {
        return coordinate.x in 1 until xAxisGridSize - 1 && coordinate.y in 1 until yAxisGridSize - 1
    }
}

// --- UI Components ---

@Composable
fun SnakeGame(navController: NavController) {
    val viewModel: SnakeGameViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    SnakeGameScreen(
        state = state,
        onEvent = viewModel::onEvent,
        navController = navController
    )
}

@Composable
fun SnakeGameScreen(
    state: SnakeGameState,
    onEvent: (SnakeGameEvent) -> Unit,
    navController: NavController
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorPalette().background0,
        topBar = {
            AppHeader(navController).Draw()
        }
    ) { paddingValues ->
        val animatedSnake = state.snake.map { coord ->
            val x by animateFloatAsState(
                targetValue = coord.x.toFloat(),
                animationSpec = tween(100, easing = LinearEasing)
            )
            val y by animateFloatAsState(
                targetValue = coord.y.toFloat(),
                animationSpec = tween(100, easing = LinearEasing)
            )
            Pair(x, y)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette().background0)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val snakeTittle = ImageBitmap.imageResource(R.drawable.snake_tittle)
                    Image(
                        bitmap = snakeTittle,
                        contentDescription = "Snake Logo",
                        modifier = Modifier.height(64.dp),
                        contentScale = ContentScale.FillHeight
                    )
                }
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = uiRoundnessShape(),
                    colors = CardDefaults.cardColors(containerColor = colorPalette().background1)
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.snake_score, state.snake.size),
                        style = typography().l.bold,
                        color = colorPalette().text
                    )
                }
                
                val primaryColor = colorPalette().accent
                val secondaryColor = colorPalette().accent.copy(alpha = 0.7f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .padding(8.dp)
                        .clip(uiRoundnessShape())
                        .background(Color.Black)
                        .pointerInput(state.gameState) {
                            if (state.gameState != GameState.STARTED) {
                                return@pointerInput
                            }
                            detectTapGestures(
                                onPress = { offset ->
                                    onEvent(SnakeGameEvent.UpdateDirection(offset, size.width))
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val cellSize = size.width / state.xAxisGridSize
                        drawGameBoard(
                            cellSize = cellSize,
                            cellColor = Color.DarkGray,
                            borderCellColor = primaryColor,
                            gridWidth = state.xAxisGridSize,
                            gridHeight = state.yAxisGridSize
                        )
                        drawFood(
                            cellSize = cellSize.toInt(),
                            coordinate = state.food,
                            color = Color.Red
                        )
                        drawSnake(
                            cellSize = cellSize,
                            snake = animatedSnake,
                            headColor = primaryColor,
                            bodyColor = secondaryColor,
                            direction = state.direction
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
                            onClick = { onEvent(SnakeGameEvent.ChangeDirection(Direction.UP)) },
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
                                onClick = { onEvent(SnakeGameEvent.ChangeDirection(Direction.LEFT)) },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(colorPalette().background1, shape = CircleShape)
                            ) {
                                Icon(painterResource(R.drawable.arrow_left), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                            }
                            IconButton(
                                onClick = { onEvent(SnakeGameEvent.ChangeDirection(Direction.RIGHT)) },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(colorPalette().background1, shape = CircleShape)
                            ) {
                                Icon(painterResource(R.drawable.arrow_right), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                            }
                        }
                        IconButton(
                            onClick = { onEvent(SnakeGameEvent.ChangeDirection(Direction.DOWN)) },
                            modifier = Modifier
                                .size(52.dp)
                                .background(colorPalette().background1, shape = CircleShape)
                        ) {
                            Icon(painterResource(R.drawable.arrow_down), null, tint = colorPalette().text, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                // Bottom Actions
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onEvent(SnakeGameEvent.ResetGame) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = colorPalette().text
                        )
                    ) {
                        Text(
                            text = "Reset",
                            style = typography().m.bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (state.gameState == GameState.STARTED) onEvent(SnakeGameEvent.PauseGame)
                            else onEvent(SnakeGameEvent.StartGame)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorPalette().accent,
                            contentColor = colorPalette().text
                        )
                    ) {
                        Text(
                            text = if (state.gameState == GameState.STARTED) "Pause" else "Start",
                            style = typography().m.bold
                        )
                    }
                }
            }
            
            if (state.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val snakeTittle = ImageBitmap.imageResource(R.drawable.snake_tittle)
                        Image(
                            bitmap = snakeTittle,
                            contentDescription = "Snake Logo",
                            modifier = Modifier.height(100.dp),
                            contentScale = ContentScale.FillHeight
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.snake_game_over),
                            style = typography().xxl.bold,
                            color = colorPalette().red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.snake_score, state.snake.size),
                            style = typography().l.bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    onEvent(SnakeGameEvent.ResetGame)
                                    onEvent(SnakeGameEvent.StartGame)
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
}

private fun DrawScope.drawGameBoard(
    cellSize: Float,
    cellColor: Color,
    borderCellColor: Color,
    gridWidth: Int,
    gridHeight: Int
) {
    for (i in 0 until gridWidth) {
        for (j in 0 until gridHeight) {
            val isBorderCell = i == 0 || j == 0 || i == gridWidth - 1 || j == gridHeight - 1
            drawRect(
                color = if (isBorderCell) borderCellColor
                else if ((i + j) % 2 == 0) cellColor
                else cellColor.copy(alpha = 0.5f),
                topLeft = Offset(x = i * cellSize, y = j * cellSize),
                size = Size(cellSize, cellSize)
            )
        }
    }
}

private fun DrawScope.drawFood(
    cellSize: Int,
    coordinate: Coordinate,
    color: Color
) {
    val centerX = (coordinate.x * cellSize) + (cellSize / 2f)
    val centerY = (coordinate.y * cellSize) + (cellSize / 2f)
    val radius = cellSize / 2.2f

    drawCircle(
        color = color,
        center = Offset(centerX, centerY),
        radius = radius
    )
    drawArc(
        color = Color.Green,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = Offset(centerX - radius / 2f, centerY - radius * 1.5f),
        size = Size(radius, radius)
    )
}

private fun DrawScope.drawSnake(
    cellSize: Float,
    snake: List<Pair<Float, Float>>,
    headColor: Color,
    bodyColor: Color,
    direction: Direction
) {
    if (snake.isEmpty()) return

    val path = Path()
    snake.forEachIndexed { index, coordinate ->
        val centerX = (coordinate.first * cellSize) + (cellSize / 2f)
        val centerY = (coordinate.second * cellSize) + (cellSize / 2f)
        if (index == 0) {
            path.moveTo(centerX, centerY)
        } else {
            path.lineTo(centerX, centerY)
        }
    }

    drawPath(
        path = path,
        color = bodyColor,
        style = Stroke(
            width = cellSize * 0.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw Head
    val headX = (snake.first().first * cellSize) + (cellSize / 2f)
    val headY = (snake.first().second * cellSize) + (cellSize / 2f)
    drawCircle(
        color = headColor,
        radius = cellSize / 2f,
        center = Offset(headX, headY)
    )

    // Draw Eyes
    val eyeRadius = cellSize * 0.15f
    val eyeOffset = cellSize * 0.2f
    val pupilRadius = eyeRadius * 0.5f

    val (leftEye, rightEye) = when (direction) {
        Direction.UP -> Pair(Offset(headX - eyeOffset, headY - eyeOffset), Offset(headX + eyeOffset, headY - eyeOffset))
        Direction.DOWN -> Pair(Offset(headX - eyeOffset, headY + eyeOffset), Offset(headX + eyeOffset, headY + eyeOffset))
        Direction.LEFT -> Pair(Offset(headX - eyeOffset, headY - eyeOffset), Offset(headX - eyeOffset, headY + eyeOffset))
        Direction.RIGHT -> Pair(Offset(headX + eyeOffset, headY - eyeOffset), Offset(headX + eyeOffset, headY + eyeOffset))
    }

    drawCircle(Color.White, radius = eyeRadius, center = leftEye)
    drawCircle(Color.White, radius = eyeRadius, center = rightEye)
    drawCircle(Color.Black, radius = pupilRadius, center = leftEye)
    drawCircle(Color.Black, radius = pupilRadius, center = rightEye)
}
