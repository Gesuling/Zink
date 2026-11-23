package com.gesuling.zink.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gesuling.zink.ui.anim.pressScale
import com.gesuling.zink.ui.theme.GeistFontFamily
import com.gesuling.zink.ui.theme.LocalTitleFont

private val Silver = Color(0xFFE0E0E0)
private val Muted = Color(0xFF888888)

@Composable
fun ZinkPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    ZinkPageHeader(
        title = title,
        subtitleContent = {
            Text(
                text = subtitle,
                color = Muted,
                fontFamily = GeistFontFamily,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 14.sp
                )
            )
        },
        modifier = modifier
    )
}

@Composable
fun ZinkPageHeader(
    title: String,
    subtitleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleFont = LocalTitleFont.current.fontFamily
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = Silver,
            fontFamily = titleFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 4.sp,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeight = 28.sp
            )
        )
        Spacer(Modifier.height(4.dp))
        subtitleContent()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ZinkToolbarHeader(
    title: String,
    subtitle: String,
    onBackClick: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isSettingsOpen: Boolean = true,
    modifier: Modifier = Modifier
) {
    val titleFont = LocalTitleFont.current.fontFamily
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (onBackClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .let { mod ->
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                mod.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "settings_gear_button"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else {
                            mod
                        }
                    }
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .pressScale(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = isSettingsOpen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "back_icon_anim"
                ) { isOpen ->
                    val rotation by animateFloatAsState(
                        targetValue = if (isOpen) 0f else -180f,
                        animationSpec = tween(300),
                        label = "icon_rotation"
                    )
                    Icon(
                        imageVector = if (isOpen) Icons.AutoMirrored.Filled.ArrowBack else Icons.Outlined.Settings,
                        contentDescription = if (isOpen) "Back" else "Settings",
                        tint = Silver,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(rotation)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = title,
                color = Silver,
                fontFamily = titleFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 28.sp
                )
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Muted,
                fontFamily = GeistFontFamily,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 14.sp
                )
            )
        }
    }
}
