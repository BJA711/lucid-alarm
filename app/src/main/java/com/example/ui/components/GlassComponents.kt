package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderSubtleDark
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.VoidBackground

@Composable
fun GlassScaffoldBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .drawBehind {
                // Top-right soft ethereal cyan orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2E38BDF8),
                            Color(0x0F38BDF8),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.width * 0.75f
                    )
                )
                // Bottom-left deep indigo orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x28818CF8),
                            Color(0x0C818CF8),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.width * 0.85f
                    )
                )
                // Center subtle ambient purple mist
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x18C084FC),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.width * 0.6f
                    )
                )
            }
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurfaceDark,
    borderColor: Color = Color.White.copy(alpha = 0.16f),
    borderSubtleColor: Color = Color.White.copy(alpha = 0.04f),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
            onClick = onClick
        )
    } else {
        Modifier
    }

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier)
            .clip(shape)
            .background(backgroundColor)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(borderColor, borderSubtleColor),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
                shape = shape
            )
            .then(clickModifier)
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "glass_button"
) {
    val shape = RoundedCornerShape(16.dp)

    val backgroundBrush = if (isPrimary) {
        if (enabled) {
            Brush.horizontalGradient(
                listOf(NeonCyan, NeonIndigo)
            )
        } else {
            Brush.horizontalGradient(
                listOf(Color(0xFF334155), Color(0xFF1E293B))
            )
        }
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
        )
    }

    val contentColor = if (isPrimary) {
        if (enabled) Color(0xFF070B13) else Color(0xFF64748B)
    } else {
        if (enabled) TextPrimaryDark else Color(0xFF64748B)
    }

    val borderStroke = if (isPrimary) {
        null
    } else {
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
            )
        )
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(52.dp)
            .clip(shape)
            .background(backgroundBrush)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isPrimary) Color.White.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
        }
    }
}
