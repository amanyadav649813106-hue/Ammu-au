package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.service.VoiceState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ammu Voice Orb Visualizer.
 * Sleek holographic sphere featuring continuously orbiting glowing sound waves,
 * revolving light dots, concentric rings, and audio pulses.
 */
@Composable
fun AmmuVoiceOrbVisualizer(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    voiceState: VoiceState = VoiceState.IDLE,
    audioRms: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmmuVoiceOrb")

    // Dynamic rotation speeds based on voice state
    val orbitDurationOuter = when (voiceState) {
        VoiceState.LISTENING -> 2500
        VoiceState.SPEAKING -> 2000
        VoiceState.PROCESSING -> 1500
        else -> 4500
    }

    val orbitDurationInner = when (voiceState) {
        VoiceState.LISTENING -> 3500
        VoiceState.SPEAKING -> 2800
        VoiceState.PROCESSING -> 1800
        else -> 6000
    }

    // Continuously revolving outer dot angle (Clockwise)
    val dotAngleOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(orbitDurationOuter, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotAngleOuter"
    )

    // Continuously revolving inner dot angle (Counter-Clockwise)
    val dotAngleInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(orbitDurationInner, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotAngleInner"
    )

    // Segmented ring rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.LISTENING -> 700
                    VoiceState.SPEAKING -> 500
                    VoiceState.PROCESSING -> 350
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // State Colors
    val activeColor = when (voiceState) {
        VoiceState.LISTENING -> NeonEmerald
        VoiceState.SPEAKING -> CyberCyan
        VoiceState.PROCESSING -> NeonAmber
        VoiceState.ERROR -> NeonRose
        VoiceState.IDLE -> CyberCyan
    }

    val dynamicAudioBoost = (audioRms / 10f).coerceIn(0f, 0.45f)
    val effectiveScale = pulseScale + dynamicAudioBoost

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val maxRadius = (size.toPx() / 2f) * 0.88f

            // 1. Ambient Background Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.22f),
                        activeColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.2f
                ),
                radius = maxRadius * 1.15f,
                center = center
            )

            // 2. Outer Circular Orbit Track (Thin holographic circle)
            drawCircle(
                color = activeColor.copy(alpha = 0.20f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Outer Dashed HUD ring with rotation
            rotate(ringRotation, pivot = center) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = maxRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 18f, 32f, 18f), 0f)
                    )
                )

                // 4 Cardinal Axis Accent Notches
                for (i in 0 until 4) {
                    val angle = (i * 90) * (Math.PI / 180.0)
                    val notchStart = Offset(
                        (center.x + (maxRadius - 6.dp.toPx()) * cos(angle)).toFloat(),
                        (center.y + (maxRadius - 6.dp.toPx()) * sin(angle)).toFloat()
                    )
                    val notchEnd = Offset(
                        (center.x + (maxRadius + 6.dp.toPx()) * cos(angle)).toFloat(),
                        (center.y + (maxRadius + 6.dp.toPx()) * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = activeColor.copy(alpha = 0.8f),
                        start = notchStart,
                        end = notchEnd,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. MAIN REVOLVING GLOWING DOTS (Continuous orbiting particles on outer track)
            // Head Dot + 4 Trailing Particles
            val trailCount = 5
            for (step in 0 until trailCount) {
                val lagDegrees = step * 10f
                val currentAngle = (dotAngleOuter - lagDegrees) * (Math.PI / 180.0)
                val dotX = (center.x + maxRadius * cos(currentAngle)).toFloat()
                val dotY = (center.y + maxRadius * sin(currentAngle)).toFloat()
                val dotAlpha = (1f - (step * 0.18f)).coerceIn(0.15f, 1f)
                val dotRadiusPx = when (step) {
                    0 -> 5.5.dp.toPx()
                    1 -> 4.2.dp.toPx()
                    2 -> 3.2.dp.toPx()
                    3 -> 2.4.dp.toPx()
                    else -> 1.8.dp.toPx()
                }

                // Glowing outer halo for the leading dot
                if (step == 0) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.45f),
                        radius = 11.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                // Dot body
                drawCircle(
                    color = if (step == 0) Color.White else activeColor.copy(alpha = dotAlpha),
                    radius = dotRadiusPx,
                    center = Offset(dotX, dotY)
                )
            }

            // Opposite Revolving Dot on outer track (180 degrees offset)
            val oppAngle = (dotAngleOuter + 180f) * (Math.PI / 180.0)
            val oppX = (center.x + maxRadius * cos(oppAngle)).toFloat()
            val oppY = (center.y + maxRadius * sin(oppAngle)).toFloat()
            drawCircle(
                color = activeColor.copy(alpha = 0.35f),
                radius = 7.dp.toPx(),
                center = Offset(oppX, oppY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = Offset(oppX, oppY)
            )

            // 4. Middle Concentric Orbit Track with Counter-Revolving Dots
            val midRadius = maxRadius * 0.68f * effectiveScale
            drawCircle(
                color = activeColor.copy(alpha = 0.28f),
                radius = midRadius,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // Inner Revolving Dots (Counter-clockwise orbit)
            for (step in 0 until 3) {
                val lagDegrees = step * 14f
                val currentAngle = (dotAngleInner + lagDegrees) * (Math.PI / 180.0)
                val dotX = (center.x + midRadius * cos(currentAngle)).toFloat()
                val dotY = (center.y + midRadius * sin(currentAngle)).toFloat()
                val dotAlpha = (0.9f - (step * 0.25f)).coerceIn(0.2f, 1f)

                if (step == 0) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                drawCircle(
                    color = if (step == 0) Color.White else activeColor.copy(alpha = dotAlpha),
                    radius = if (step == 0) 4.5.dp.toPx() else 2.8.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // 5. Innermost Holographic Ring (High-density circular ticks)
            val innerRingRadius = maxRadius * 0.45f * effectiveScale
            drawCircle(
                color = activeColor.copy(alpha = 0.45f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 12 Radar HUD Tick Marks
            for (i in 0 until 12) {
                val angle = (i * 30) * (Math.PI / 180.0)
                val tStart = Offset(
                    (center.x + (innerRingRadius - 4.dp.toPx()) * cos(angle)).toFloat(),
                    (center.y + (innerRingRadius - 4.dp.toPx()) * sin(angle)).toFloat()
                )
                val tEnd = Offset(
                    (center.x + (innerRingRadius + 4.dp.toPx()) * cos(angle)).toFloat(),
                    (center.y + (innerRingRadius + 4.dp.toPx()) * sin(angle)).toFloat()
                )
                drawLine(
                    color = activeColor.copy(alpha = 0.7f),
                    start = tStart,
                    end = tEnd,
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 6. Central Glowing JARVIS Nucleus Core
            val nucleusRadius = maxRadius * 0.28f * effectiveScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        activeColor,
                        activeColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = nucleusRadius * 1.8f
                ),
                radius = nucleusRadius * 1.4f,
                center = center
            )

            // Center solid glowing sphere
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = nucleusRadius * 0.55f,
                center = center
            )

            // Core boundary ring
            drawCircle(
                color = activeColor,
                radius = nucleusRadius * 0.65f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Deprecated("Use AmmuVoiceOrbVisualizer instead", ReplaceWith("AmmuVoiceOrbVisualizer(modifier, size, voiceState, audioRms, onClick)"))
@Composable
fun ArcReactorVisualizer(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    voiceState: VoiceState = VoiceState.IDLE,
    audioRms: Float = 0f,
    onClick: () -> Unit = {}
) {
    AmmuVoiceOrbVisualizer(modifier, size, voiceState, audioRms, onClick)
}
