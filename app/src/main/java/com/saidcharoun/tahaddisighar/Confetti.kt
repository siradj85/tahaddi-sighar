package com.saidcharoun.tahaddisighar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Piece(
    val xRatio: Float,
    val delay: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val swing: Float,
    val rotationSpeed: Float
)

private val confettiColors = listOf(
    Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF4CAF50),
    Color(0xFF2196F3), Color(0xFFFF5722), Color(0xFFFFFFFF)
)

/**
 * مطر من الورق الملوّن يتساقط — يُعرض عند الفوز واجتياز المراحل.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, pieceCount: Int = 90) {
    val pieces = remember {
        List(pieceCount) {
            Piece(
                xRatio = Random.nextFloat(),
                delay = Random.nextFloat() * 0.4f,
                speed = 0.7f + Random.nextFloat() * 0.6f,
                size = 14f + Random.nextFloat() * 18f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                swing = 20f + Random.nextFloat() * 40f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(3500, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        pieces.forEach { p ->
            val local = ((progress.value - p.delay) * p.speed).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = local * (h + 60f) - 30f
            val x = p.xRatio * w + kotlin.math.sin(local * 6.28f * 2f) * p.swing
            val angle = local * p.rotationSpeed
            rotate(degrees = angle, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x - p.size / 2, y - p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
