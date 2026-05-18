package com.app.gitquest.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val BgDark = Color(0xFF1E1E2E)
private val Green = Color(0xFF50FA7B)
private val Blue = Color(0xFF8BE9FD)
private val Yellow = Color(0xFFF1FA8C)
private val White = Color(0xFFF8F8F2)
private val Muted = Color(0xFF6272A4)
private val MonoFont = FontFamily.Monospace

data class OnboardingPage(
    val icon: String,
    val title: String,
    val description: String,
    val accentColor: Color,
)

private val pages = listOf(
    OnboardingPage(
        icon = ">_",
        title = "Welcome to Git-Git",
        description = "The interactive game that teaches you Git.\n" +
                "the most essential tool in every developer's toolkit.\n" +
                "No boring tutorials.\n" +
                "Just play, learn, and master version control.",
        accentColor = Green,
    ),
    OnboardingPage(
        icon = "{}",
        title = "How to play",
        description = "Each level presents a real-world scenario.\n" +
                "Type Git commands in the terminal, watch the branch tree update in real time, and reach the goal state to earn stars.\n" +
                "Hints are available if you get stuck.",
        accentColor = Blue,
    ),
    OnboardingPage(
        icon = "/*",
        title = "Why Git matters",
        description = "Git is used by over 90% of developers worldwide.\n" +
                "It tracks every change to your code, enables team collaboration, and protects your work.\n" +
                "Mastering Git is not optional — it's essential.",
        accentColor = Yellow,
    ),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onFinished) {
                        Text("Skip", color = Muted, fontFamily = MonoFont, fontSize = 14.sp)
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val page = pages[pageIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(page.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = page.icon,
                            color = page.accentColor,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonoFont,
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = page.title,
                        color = White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFont,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = page.description,
                        color = Muted,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Page indicators
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pages.forEachIndexed { index, page ->
                    val isActive = pagerState.currentPage == index
                    val color by animateColorAsState(
                        targetValue = if (isActive) page.accentColor else Muted.copy(alpha = 0.4f),
                        label = "dot",
                    )
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Button
            if (pagerState.currentPage == pages.size - 1) {
                FilledTonalButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "Start your quest",
                        fontFamily = MonoFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Next", fontFamily = MonoFont, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(
    name = "Onboarding Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(
        onFinished = {}
    )
}