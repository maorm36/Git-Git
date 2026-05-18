package com.app.gitquest.ui.worldmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gitquest.data.levels.LevelRegistry

private val BgDark = Color(0xFF1E1E2E)
private val CardBg = Color(0xFF282A36)
private val LockedCardBg = Color(0xFF1C1C28)
private val Green = Color(0xFF50FA7B)
private val Blue = Color(0xFF8BE9FD)
private val Purple = Color(0xFFBD93F9)
private val Muted = Color(0xFF6272A4)
private val White = Color(0xFFF8F8F2)
private val Yellow = Color(0xFFF1FA8C)
private val MonoFont = FontFamily.Monospace

private val worldColors = listOf(
    Green,
    Blue,
    Purple,
    Color(0xFFFFB86C),
    Color(0xFFFF79C6),
)

private data class LevelRowUi(
    val id: String,
    val title: String,
    val stars: Int,
) {
    val isCompleted: Boolean
        get() = stars > 0
}

@Composable
fun WorldMapScreen(
    onLevelSelected: (String) -> Unit,
    onGitHistoryClick: () -> Unit,
    levelStars: Map<String, Int> = emptyMap(),
) {
    val completedLevels = levelStars
        .filterValues { stars -> stars > 0 }
        .keys

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Git-Git",
                        color = Green,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFont,
                    )

                    Text(
                        text = "Learn Git by playing",
                        color = Muted,
                        fontSize = 14.sp,
                        fontFamily = MonoFont,
                    )
                }

                IconButton(onClick = onGitHistoryClick) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "About Git",
                        tint = Muted,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(LevelRegistry.worlds.size) { worldIndex ->
                    val world = LevelRegistry.worlds[worldIndex]
                    val color = worldColors[worldIndex % worldColors.size]

                    val isUnlocked = if (worldIndex == 0) {
                        true
                    } else {
                        val previousWorld = LevelRegistry.worlds[worldIndex - 1]
                        previousWorld.levels.all { level ->
                            level.id in completedLevels
                        }
                    }

                    val isActive = isUnlocked && !world.levels.all { level ->
                        level.id in completedLevels
                    }

                    WorldCard(
                        worldNumber = worldIndex + 1,
                        title = world.title,
                        description = world.description,
                        levels = world.levels.map { level ->
                            LevelRowUi(
                                id = level.id,
                                title = level.title,
                                stars = levelStars[level.id]?.coerceIn(0, 3) ?: 0,
                            )
                        },
                        accentColor = color,
                        isUnlocked = isUnlocked,
                        isActive = isActive,
                        onLevelSelected = onLevelSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldCard(
    worldNumber: Int,
    title: String,
    description: String,
    levels: List<LevelRowUi>,
    accentColor: Color,
    isUnlocked: Boolean,
    isActive: Boolean,
    onLevelSelected: (String) -> Unit,
) {
    val glowAlpha = if (isActive) {
        val transition = rememberInfiniteTransition(label = "glow")

        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )

        alpha
    } else {
        0f
    }

    val cardModifier = if (isActive) {
        Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = glowAlpha),
                spotColor = accentColor.copy(alpha = glowAlpha),
            )
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = glowAlpha),
                shape = RoundedCornerShape(16.dp),
            )
    } else {
        Modifier
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) CardBg else LockedCardBg,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(if (isUnlocked) 1f else 0.45f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        accentColor.copy(
                            alpha = if (isUnlocked) 0.12f else 0.05f,
                        )
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isUnlocked) "$worldNumber" else "?",
                        color = accentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFont,
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "World $worldNumber: $title",
                        color = if (isUnlocked) White else Muted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFont,
                    )

                    Text(
                        text = if (isUnlocked) {
                            description
                        } else {
                            "Complete previous world to unlock"
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        fontFamily = MonoFont,
                    )
                }
            }

            if (isUnlocked) {
                Spacer(modifier = Modifier.height(12.dp))

                levels.forEachIndexed { index, level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onLevelSelected(level.id)
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (level.isCompleted) {
                                        accentColor.copy(alpha = 0.3f)
                                    } else {
                                        accentColor.copy(alpha = 0.12f)
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MonoFont,
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = level.title,
                            color = White,
                            fontSize = 14.sp,
                            fontFamily = MonoFont,
                            modifier = Modifier.weight(1f),
                        )

                        Text(
                            text = buildStarsText(level.stars),
                            color = if (level.isCompleted) Yellow else Muted,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun buildStarsText(stars: Int): String {
    val safeStars = stars.coerceIn(0, 3)
    return "★".repeat(safeStars) + "☆".repeat(3 - safeStars)
}

@Preview(
    name = "World Map Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
)
@Composable
private fun WorldMapScreenPreview() {
    val previewLevelStars = LevelRegistry.worlds
        .flatMap { world -> world.levels }
        .take(3)
        .mapIndexed { index, level ->
            level.id to (index + 1)
        }
        .toMap()

    WorldMapScreen(
        onLevelSelected = {},
        onGitHistoryClick = {},
        levelStars = previewLevelStars,
    )
}