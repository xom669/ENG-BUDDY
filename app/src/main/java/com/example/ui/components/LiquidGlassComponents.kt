package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.MainDestination
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.googleFluidGradientBrush
import com.example.ui.theme.liquidGlassRimBrush
import kotlin.math.cos
import kotlin.math.sin

/**
 * "A Pinch of Salt" Ambient 4-Color Fluid Mesh Animation:
 * Slow-moving 15-second loop at 14% opacity rendered ONLY behind the top hero header
 * and active AI/Scanner surfaces so it feels alive beneath frosted glass without draining battery.
 */
@Composable
fun AmbientFluidMeshHeaderBox(
    modifier: Modifier = Modifier,
    isActiveGlowingBorder: Boolean = false,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pinch_of_salt_fluid_mesh")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fluid_phase"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val glassFill = if (isDark) {
        Color.White.copy(alpha = 0.045f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }

    val borderBrush = if (isActiveGlowingBorder) {
        googleFluidGradientBrush(
            alpha = 1f,
            start = Offset(0f, 0f),
            end = Offset(800f * cos(phase).coerceAtLeast(0.2f), 800f)
        )
    } else {
        liquidGlassRimBrush(isDark = isDark)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassFill)
            .border(
                width = if (isActiveGlowingBorder) 2.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        // Ultra-subtle 14% opacity fluid radial blobs ("A Pinch of Salt")
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val r = maxOf(w, h) * 0.65f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoogleBlue.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(
                        x = w * (0.25f + 0.15f * cos(phase)),
                        y = h * (0.30f + 0.15f * sin(phase))
                    ),
                    radius = r
                ),
                radius = r,
                center = Offset(
                    x = w * (0.25f + 0.15f * cos(phase)),
                    y = h * (0.30f + 0.15f * sin(phase))
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoogleRed.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(
                        x = w * (0.75f - 0.15f * sin(phase)),
                        y = h * (0.35f + 0.12f * cos(phase))
                    ),
                    radius = r * 0.9f
                ),
                radius = r * 0.9f,
                center = Offset(
                    x = w * (0.75f - 0.15f * sin(phase)),
                    y = h * (0.35f + 0.12f * cos(phase))
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoogleYellow.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(
                        x = w * (0.60f + 0.12f * cos(phase + 1.5f)),
                        y = h * (0.75f - 0.10f * sin(phase))
                    ),
                    radius = r * 0.85f
                ),
                radius = r * 0.85f,
                center = Offset(
                    x = w * (0.60f + 0.12f * cos(phase + 1.5f)),
                    y = h * (0.75f - 0.10f * sin(phase))
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoogleGreen.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(
                        x = w * (0.30f + 0.10f * sin(phase + 2.2f)),
                        y = h * (0.70f + 0.10f * cos(phase))
                    ),
                    radius = r * 0.8f
                ),
                radius = r * 0.8f,
                center = Offset(
                    x = w * (0.30f + 0.10f * sin(phase + 2.2f)),
                    y = h * (0.70f + 0.10f * cos(phase))
                )
            )
        }

        content()
    }
}

/**
 * Frosted Liquid-Glass Card:
 * Surface Fill: rgba(255, 255, 255, 0.04) in Dark Mode / rgba(255, 255, 255, 0.75) in Light Mode
 * Specular Rim Border: 1dp translucent top-left highlight.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentTint: Color? = null,
    isGlowingBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val baseFill = if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    val fill = if (accentTint != null) {
        accentTint.copy(alpha = if (isDark) 0.10f else 0.14f)
    } else {
        baseFill
    }

    val borderBrush = when {
        isGlowingBorder -> googleFluidGradientBrush(alpha = 1f)
        accentTint != null -> Brush.linearGradient(
            listOf(accentTint.copy(alpha = 0.45f), accentTint.copy(alpha = 0.12f))
        )
        else -> liquidGlassRimBrush(isDark = isDark)
    }

    val clickMod = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                width = if (isGlowingBorder) 2.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .then(clickMod),
        content = content
    )
}

/**
 * Floating Liquid-Glass Pill Bottom Navigation Bar (inspired by uploaded reference image_837c87dc.png)
 * Respects WindowInsets.navigationBars so gesture pill / 3-button nav never overlaps.
 */
@Composable
fun FloatingLiquidGlassBottomBar(
    currentDestination: MainDestination,
    onNavigate: (MainDestination) -> Unit,
    iconForDestination: (MainDestination) -> ImageVector,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val dockFill = if (isDark) {
        Color(0xFF16181D).copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(dockFill)
                .border(
                    width = 1.dp,
                    brush = liquidGlassRimBrush(isDark = isDark),
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainDestination.entries.forEach { dest ->
                val selected = currentDestination == dest
                val itemShape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .clip(itemShape)
                        .background(
                            if (selected) {
                                GoogleBlue.copy(alpha = 0.18f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .then(
                            if (selected) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = GoogleBlue.copy(alpha = 0.55f),
                                    shape = itemShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onNavigate(dest) }
                        .padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 10.dp)
                        .testTag("nav_item_${dest.route}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = iconForDestination(dest),
                            contentDescription = dest.label,
                            tint = if (selected) {
                                GoogleBlue
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        if (selected) {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = GoogleBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
