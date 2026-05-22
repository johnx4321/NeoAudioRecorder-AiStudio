package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Neomorphic Theme Color Palettes ---
object NeoColors {
    // Light Theme Colors
    val LightBg = Color(0xFFE0E5EC)
    val LightWhite = Color(0xFFFFFFFF)
    val LightShadow = Color(0xFFA5B4C9)
    
    // Dark Theme Colors
    val DarkBg = Color(0xFF0D0D0D)
    val DarkWhite = Color(0xFF1E1E1E)
    val DarkShadow = Color(0xFF000000)

    // Accents
    val Primary = Color(0xFFF97316)       // Sophisticated Orange
    val PrimaryGlow = Color(0xFFEA580C)   // Deep Orange
    val RecordRed = Color(0xFFDC2626)     // Vibrant Red
}

// Custom Modifier for Neomorphic Shadow (Elevated / Popping out)
fun Modifier.neoShadow(
    isDarkTheme: Boolean,
    shape: String = "rounded", // "rounded" or "circle"
    cornerRadiusDp: Dp = 16.dp,
    elevation: Float = 14f,
    isSunken: Boolean = false
): Modifier = this.drawBehind {
    val sizeWidth = size.width
    val sizeHeight = size.height
    val cornerRadiusPx = cornerRadiusDp.toPx()

    val lightShadowColor = if (isDarkTheme) {
        Color(0xFF2D2D2D).copy(alpha = 0.4f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.9f)
    }

    val darkShadowColor = if (isDarkTheme) {
        Color(0xFF000000).copy(alpha = 0.6f)
    } else {
        Color(0xFFA3B1C6).copy(alpha = 0.65f)
    }

    val baseBgColor = if (isDarkTheme) NeoColors.DarkBg else NeoColors.LightBg

    drawIntoCanvas { canvas ->
        val paintLight = Paint().asFrameworkPaint().apply {
            color = baseBgColor.toArgb()
            isAntiAlias = true
            if (!isSunken) {
                setShadowLayer(
                    elevation,
                    -elevation * 0.7f,
                    -elevation * 0.7f,
                    lightShadowColor.toArgb()
                )
            }
        }

        val paintDark = Paint().asFrameworkPaint().apply {
            color = baseBgColor.toArgb()
            isAntiAlias = true
            if (!isSunken) {
                setShadowLayer(
                    elevation,
                    elevation * 0.7f,
                    elevation * 0.7f,
                    darkShadowColor.toArgb()
                )
            }
        }

        if (shape == "circle") {
            val radius = sizeWidth / 2f
            canvas.nativeCanvas.drawCircle(sizeWidth / 2f, sizeHeight / 2f, radius, paintDark)
            canvas.nativeCanvas.drawCircle(sizeWidth / 2f, sizeHeight / 2f, radius, paintLight)
        } else {
            val rect = android.graphics.RectF(0f, 0f, sizeWidth, sizeHeight)
            canvas.nativeCanvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paintDark)
            canvas.nativeCanvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paintLight)
        }
    }
}

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    isCircle: Boolean = false,
    isActivated: Boolean = false,
    activeColor: Color = NeoColors.Primary,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val baseBg = if (isDarkTheme) NeoColors.DarkBg else NeoColors.LightBg

    val shape = if (isCircle) CircleShape else RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .then(
                if (isActivated) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = activeColor.copy(alpha = 0.8f),
                        shape = shape
                    )
                } else Modifier
            )
            .neoShadow(
                isDarkTheme = isDarkTheme,
                shape = if (isCircle) "circle" else "rounded",
                cornerRadiusDp = 16.dp,
                elevation = if (isActivated) 4f else 12f
            )
            .clip(shape)
            .background(
                brush = if (isActivated) {
                    Brush.verticalGradient(
                        colors = listOf(activeColor, activeColor.copy(alpha = 0.7f))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF151515), Color(0xFF0F0F0F))
                        } else {
                            listOf(Color(0xFFEDF2F8), Color(0xFFD6DBE4))
                        }
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (isActivated) Color.White else activeColor),
                onClick = onClick,
                role = Role.Button
            )
            .padding(if (isCircle) 12.dp else 16.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    cornerRadius: Dp = 20.dp,
    elevation: Float = 14f,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .neoShadow(
                isDarkTheme = isDarkTheme,
                shape = "rounded",
                cornerRadiusDp = cornerRadius,
                elevation = elevation
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF131313), Color(0xFF0A0A0A))
                    } else {
                        listOf(Color(0xFFE6ECF4), Color(0xFFD9E0EB))
                    }
                )
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun NeoSunkenCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                color = if (isDarkTheme) Color(0xFF080808) else Color(0xFFD0D7E1)
            )
            .border(
                width = 1.5.dp,
                color = if (isDarkTheme) Color(0xFF020202) else Color(0xFFCBD2DC),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// Customized Neomorphic Format Tab Selector for WAV, AAC, MP3
@Composable
fun NeoFormatSelector(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val formats = listOf("WAV", "AAC", "MP3")
    
    NeoCard(
        modifier = modifier.fillMaxWidth(),
        isDarkTheme = isDarkTheme,
        cornerRadius = 16.dp,
        elevation = 8f
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RECORDING FORMAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color(0xFF8F9BB3) else Color(0xFF6F7E97),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                formats.forEach { format ->
                    val isSelected = selectedFormat == format
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        NeoButton(
                            onClick = { onFormatSelected(format) },
                            modifier = Modifier.fillMaxWidth(),
                            isDarkTheme = isDarkTheme,
                            isActivated = isSelected,
                            activeColor = NeoColors.Primary
                        ) {
                            Text(
                                text = format,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else {
                                    if (isDarkTheme) Color.LightGray else Color.DarkGray
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// A highly interactive Wave Visualizer that shows bouncing blocks
@Composable
fun NeoWaveVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // Generate static array of bar counts to drive visualizer look
    val barsCount = 28
    // Bouncing simulation for bars when recording
    val randomAmplitudes = remember(amplitude) {
        FloatArray(barsCount) { index ->
            if (isRecording) {
                // Mix raw amplitude with organic bounce
                val base = amplitude * (0.3f + (0.7f * Math.random().toFloat()))
                // Central bars are taller
                val centerFactor = 1f - (Math.abs(index - (barsCount / 2f)) / (barsCount / 2f))
                (base * centerFactor * 1.2f).coerceIn(0.04f, 1f)
            } else {
                0.04f // Flat groove when idle
            }
        }
    }

    NeoSunkenCard(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        isDarkTheme = isDarkTheme,
        cornerRadius = 24.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = 6.dp.toPx()
            val totalSpacing = spacing * (barsCount - 1)
            val barWidth = (width - totalSpacing) / barsCount

            for (i in 0 until barsCount) {
                val barHeight = randomAmplitudes[i] * (height * 0.85f)
                val x = i * (barWidth + spacing)
                val y = (height - barHeight) / 2f

                // Dual shadow styled vertical bar
                val colorGradient = Brush.verticalGradient(
                    colors = if (isRecording) {
                        listOf(NeoColors.RecordRed, NeoColors.PrimaryGlow)
                    } else {
                        if (isDarkTheme) {
                            listOf(Color(0xFF3F3F46), Color(0xFF27272A))
                        } else {
                            listOf(Color(0xFFB8C4D1), Color(0xFF9EACB9))
                        }
                    }
                )

                // Render visualizer bar
                drawRoundRect(
                    brush = colorGradient,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                )
            }
        }
    }
}
