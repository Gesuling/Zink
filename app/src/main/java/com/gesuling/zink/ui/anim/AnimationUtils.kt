package com.gesuling.zink.ui.anim

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource? = null,
    scalePressed: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by currentInteractionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scalePressed else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = currentInteractionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

fun Modifier.glowingBorderOnPress(
    shape: CornerBasedShape,
    interactionSource: MutableInteractionSource? = null,
    normalColor: Color = Color.White.copy(alpha = 0.10f),
    pressedColor: Color = Color.White.copy(alpha = 0.55f),
    borderWidth: Float = 1f
): Modifier = composed {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by currentInteractionSource.collectIsPressedAsState()

    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) pressedColor.alpha else normalColor.alpha,
        animationSpec = tween(durationMillis = 150),
        label = "border_glow_alpha"
    )

    this.border(
        border = BorderStroke(
            width = borderWidth.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = (borderAlpha * 0.4f).coerceIn(0f, 1f))
                )
            )
        ),
        shape = shape
    )
}

public object NaturalOrderComparator : Comparator<String> {
    override fun compare(s1: String?, s2: String?): Int {
        if (s1 === s2) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        val str1 = s1.trim()
        val str2 = s2.trim()
        var i = 0
        var j = 0
        val len1 = str1.length
        val len2 = str2.length

        while (i < len1 && j < len2) {
            val c1 = str1[i]
            val c2 = str2[j]

            if (c1.isDigit() && c2.isDigit()) {
                val start1 = i
                while (i < len1 && str1[i].isDigit()) {
                    i++
                }
                val num1Str = str1.substring(start1, i)

                val start2 = j
                while (j < len2 && str2[j].isDigit()) {
                    j++
                }
                val num2Str = str2.substring(start2, j)

                val trimmed1 = num1Str.trimStart('0')
                val trimmed2 = num2Str.trimStart('0')

                val lenDiff = trimmed1.length - trimmed2.length
                if (lenDiff != 0) return lenDiff

                val comp = trimmed1.compareTo(trimmed2)
                if (comp != 0) return comp

                val rawLenDiff = num1Str.length - num2Str.length
                if (rawLenDiff != 0) return rawLenDiff
            } else {
                val comp = c1.lowercaseChar().compareTo(c2.lowercaseChar())
                if (comp != 0) return comp
                i++
                j++
            }
        }
        return (len1 - i) - (len2 - j)
    }
}
