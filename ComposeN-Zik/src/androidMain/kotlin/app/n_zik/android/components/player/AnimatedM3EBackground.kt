
package app.n_zik.android.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import android.graphics.Path as AndroidPath
import android.graphics.Matrix as AndroidMatrix
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect

private var hasAppLaunched = false

@OptIn(UnstableApi::class)
fun Modifier.animatedM3EBackground(
    animating: Boolean,
    D: Color,
    V: Color,
    LV: Color,
    DV: Color,
    M: Color,
    LM: Color,
    DM: Color
): Modifier = composed {
    val accumulatedTime = remember { mutableFloatStateOf(0f) }
    
    val visibilityProgress by animateFloatAsState(
        targetValue = if (animating) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "visibility"
    )
    
    val colors = listOf(V, DV, M)
    
    // Generate static random properties for shapes so they don't change on recomposition
    val shapes = remember {
        val states = mutableListOf<ShapeConfig>()
        for (i in 0 until 20) {
            var startX = 0f
            var startY = 0f
            var overlap = true
            var attempts = 0
            while(overlap && attempts < 50) {
                startX = Random.nextFloat()
                startY = Random.nextFloat()
                overlap = states.any { other -> 
                    val dx = other.x - startX
                    val dy = other.y - startY
                    (dx * dx + dy * dy) < 0.04f // 20% of screen minimum distance
                }
                attempts++
            }
            
            states.add(
                ShapeConfig(
                    x = startX,
                    y = startY,
                    vx = (Random.nextFloat() - 0.5f) * 0.08f, 
                    vy = (Random.nextFloat() - 0.5f) * 0.08f,
                    baseSpeed = Random.nextFloat() * 0.04f + 0.03f, // 3% to 7% of screen per sec
                    radiusScale = Random.nextFloat() * 0.20f + 0.03f, // 3% to 23%
                    colorIndex = i % colors.size,
                    type = i % 10, 
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() - 0.5f) * 0.8f,
                    pulsePhase = Random.nextFloat() * 100f,
                    isMorph = i < 18 
                )
            )
        }
        states
    }

    val context = LocalContext.current
    
    // Shake detection for explosion effect
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        var lastShakeTime = 0L
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    
                    val gX = x / SensorManager.GRAVITY_EARTH
                    
                    // Trigger only on strong left-to-right shake (X axis)
                    if (abs(gX) > 2.2f) { // 2.2G horizontal force
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTime > 1000) { // 1 second cooldown
                            lastShakeTime = now
                            shapes.forEach { shape ->
                                shape.vx += (Random.nextFloat() - 0.5f) * 4.0f // Moderate burst
                                shape.vy += (Random.nextFloat() - 0.5f) * 4.0f
                            }
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    val lastLocalColor = remember { androidx.compose.runtime.mutableStateOf(V.value) }
    val mountTime = remember { System.currentTimeMillis() }
    
    // Explosion on song change (colors change) and startup
    LaunchedEffect(V) {
        val currentColor = V.value
        val now = System.currentTimeMillis()
        if (!hasAppLaunched) {
            hasAppLaunched = true
            shapes.forEach { shape ->
                shape.vx += (Random.nextFloat() - 0.5f) * 3.5f
                shape.vy += (Random.nextFloat() - 0.5f) * 3.5f
            }
        } else if (lastLocalColor.value != currentColor) {
            lastLocalColor.value = currentColor
            // Ignore color changes that happen within the first 800ms of opening the player
            // This prevents explosions caused by async album color loading!
            if (now - mountTime > 800) {
                shapes.forEach { shape ->
                    shape.vx += (Random.nextFloat() - 0.5f) * 3.5f
                    shape.vy += (Random.nextFloat() - 0.5f) * 3.5f
                }
            }
        }
    }

    LaunchedEffect(animating) {
        if (animating) {
            var lastFrameTime = withFrameNanos { it }
            while (true) {
                val currentFrameTime = withFrameNanos { it }
                val delta = (currentFrameTime - lastFrameTime) / 1_000_000_000f // Time in seconds
                
                // --- PHYSICS ENGINE ---
                for (i in shapes.indices) {
                    val shape1 = shapes[i]
                    // 1. Repulsion from other shapes
                    for (j in i + 1 until shapes.size) {
                        val shape2 = shapes[j]
                        val dx = shape2.x - shape1.x
                        val dy = shape2.y - shape1.y
                        val distSq = dx * dx + dy * dy
                        // minimum distance before repulsing (based on their radius)
                        val minDist = shape1.radiusScale + shape2.radiusScale
                        val minDistSq = minDist * minDist
                        
                        if (distSq < minDistSq && distSq > 0.0001f) {
                            val dist = sqrt(distSq)
                            // gentle fluid impulse (increased slightly for more bounce)
                            val force = (minDist - dist) * 0.4f 
                            val fx = (dx / dist) * force
                            val fy = (dy / dist) * force
                            shape1.vx -= fx * delta
                            shape1.vy -= fy * delta
                            shape2.vx += fx * delta
                            shape2.vy += fy * delta
                        }
                    }
                    
                    // 2. Velocity damping (keep base speed)
                    val currentSpeed = sqrt(shape1.vx * shape1.vx + shape1.vy * shape1.vy)
                    if (currentSpeed > 0.001f) {
                        val speedDiff = shape1.baseSpeed - currentSpeed
                        // gradually push velocity towards baseSpeed very softly
                        shape1.vx += (shape1.vx / currentSpeed) * speedDiff * delta * 0.3f
                        shape1.vy += (shape1.vy / currentSpeed) * speedDiff * delta * 0.3f
                    }
                    
                    // Cap maximum velocity to prevent excessive boosting
                    val speedSq = shape1.vx * shape1.vx + shape1.vy * shape1.vy
                    if (speedSq > 16.0f) { // MAX_SPEED = 4.0f
                        val scale = 4.0f / sqrt(speedSq)
                        shape1.vx *= scale
                        shape1.vy *= scale
                    }
                    
                    // 3. Apply velocity to position
                    shape1.x += shape1.vx * delta
                    shape1.y += shape1.vy * delta
                    
                    // 4. Soft wall bounce (magnetic curve instead of hard reverse)
                    val margin = 0.08f
                    val edgeForce = 0.15f
                    if (shape1.x < margin) { shape1.vx += delta * edgeForce }
                    if (shape1.x > 1f - margin) { shape1.vx -= delta * edgeForce }
                    if (shape1.y < margin) { shape1.vy += delta * edgeForce }
                    if (shape1.y > 1f - margin) { shape1.vy -= delta * edgeForce }
                    
                    // Absolute limits (elastic bounce for shake effect)
                    if (shape1.x < -0.1f) { shape1.x = -0.1f; shape1.vx *= -1f }
                    if (shape1.x > 1.1f) { shape1.x = 1.1f; shape1.vx *= -1f }
                    if (shape1.y < -0.1f) { shape1.y = -0.1f; shape1.vy *= -1f }
                    if (shape1.y > 1.1f) { shape1.y = 1.1f; shape1.vy *= -1f }
                }
                
                accumulatedTime.floatValue += delta
                lastFrameTime = currentFrameTime
            }
        }
    }
    
    val time = accumulatedTime.floatValue

    val sequenceMorphs = remember {
        val morphPolygons = listOf(
            RoundedPolygon.star(numVerticesPerRadius = 5, innerRadius = 0.5f, rounding = CornerRounding(0.2f)),
            RoundedPolygon(numVertices = 12, rounding = CornerRounding(1f)), // Circle
            RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)), // Squircle
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRounding(0.15f)), // Flower
            RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.2f)), // Triangle
            RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.2f)), // Pentagon
            RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.3f, rounding = CornerRounding(0.15f)), // Sparkle
            RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.4f)), // Hexagon pill
            RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.7f, rounding = CornerRounding(0.3f)), // Blob
            RoundedPolygon(numVertices = 7, rounding = CornerRounding(0.2f)) // Heptagon
        )
        morphPolygons.mapIndexed { index, poly ->
            Morph(poly, morphPolygons[(index + 1) % morphPolygons.size])
        }
    }

    drawWithCache {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        
        val basePaths = List(6) { type ->
            val polygon = when (type) {
                0 -> RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.2f)) // Rounded Triangle
                1 -> RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)) // Squircle
                2 -> RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.2f)) // Rounded Hexagon
                3 -> RoundedPolygon.star(numVerticesPerRadius = 5, innerRadius = 0.5f, rounding = CornerRounding(0.2f)) // Puffy Star
                4 -> RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.3f, rounding = CornerRounding(0.15f)) // M3E Sparkle
                else -> RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRounding(0.15f)) // Flower
            }
            
            val composePath = ComposePath()
            val scale = minDim
            val cubics = polygon.cubics
            if (cubics.isNotEmpty()) {
                composePath.moveTo(cubics[0].anchor0X * scale, cubics[0].anchor0Y * scale)
                for (cubic in cubics) {
                    composePath.cubicTo(
                        cubic.control0X * scale, cubic.control0Y * scale,
                        cubic.control1X * scale, cubic.control1Y * scale,
                        cubic.anchor1X * scale, cubic.anchor1Y * scale
                    )
                }
                composePath.close()
            }
            composePath
        }
        
        onDrawBehind {
            if (visibilityProgress <= 0.01f) return@onDrawBehind
            
            shapes.forEach { shape ->
                val scaleFactor = shape.radiusScale
                val radius = scaleFactor * minDim
                
                val spanX = width - 2 * radius
                val spanY = height - 2 * radius
                
                // Directly use the physical simulation positions!
                val currentX = radius + shape.x * spanX
                val currentY = radius + shape.y * spanY
                
                val shapeColor = colors[shape.colorIndex].copy(alpha = 0.4f * visibilityProgress) // Fade out!

                withTransform({
                    translate(left = currentX, top = currentY)
                    // MUST use pivot = Offset.Zero because we already translated to the center!
                    rotate(degrees = shape.rotation + (shape.rotSpeed * time * 250f), pivot = Offset.Zero) // Much faster rotation
                    val visualScale = scaleFactor * visibilityProgress
                    scale(scaleX = visualScale, scaleY = visualScale, pivot = Offset.Zero) // Zoom out!
                }) {
                    val pathToDraw = if (shape.isMorph) {
                        val cycleSpeed = 0.35f // morph shapes per second
                        val totalMorphs = sequenceMorphs.size
                        // Ensure positive wrapping for cycleTime
                        val cycleTime = ((time * cycleSpeed + shape.pulsePhase) % totalMorphs + totalMorphs) % totalMorphs
                        val morphIndex = cycleTime.toInt()
                        val progress = cycleTime - morphIndex
                        
                        // Sine easing for smoother transition with slight pauses at keyframes
                        val easedProgress = (sin((progress - 0.5f) * PI.toFloat()) + 1f) / 2f
                        
                        val morph = sequenceMorphs[morphIndex]
                        
                        val androidPath = AndroidPath()
                        val legacyPath = morph.toPath(easedProgress, androidPath)
                        
                        val matrix = AndroidMatrix()
                        matrix.setScale(minDim, minDim)
                        legacyPath.transform(matrix)
                        legacyPath.asComposePath()
                    } else {
                        basePaths[shape.type % basePaths.size]
                    }

                    // Solid classic color (semi-transparent, affected by fade out)
                    drawPath(
                        path = pathToDraw,
                        color = shapeColor,
                        blendMode = BlendMode.SrcOver
                    )
                }
            }
        }
    }
}

private class ShapeConfig(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val baseSpeed: Float,
    val radiusScale: Float,
    val colorIndex: Int,
    val type: Int,
    val rotation: Float,
    val rotSpeed: Float,
    val pulsePhase: Float,
    val isMorph: Boolean
)
