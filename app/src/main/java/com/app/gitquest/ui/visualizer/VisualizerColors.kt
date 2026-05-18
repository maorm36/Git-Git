package com.app.gitquest.ui.visualizer

import androidx.compose.ui.graphics.Color

object VisualizerColors {
    val background = Color(0xFF1E1E2E)
    val nodeBorder = Color(0xFFF8F8F2)

    // Branch lane colors — each branch gets its own color
    val laneColors = listOf(
        Color(0xFF50FA7B),  // green  — main
        Color(0xFF8BE9FD),  // cyan   — feature 1
        Color(0xFFFFB86C),  // orange — feature 2
        Color(0xFFBD93F9),  // purple — feature 3
        Color(0xFFFF79C6),  // pink   — feature 4
        Color(0xFFF1FA8C),  // yellow — feature 5
    )

    val headIndicator = Color(0xFFF8F8F2)
    val detachedHead = Color(0xFFFF5555)
    val mergeNode = Color(0xFFFFB86C)

    val labelBackground = Color(0xFF282A36)
    val labelText = Color(0xFFF8F8F2)
    val hashText = Color(0xFF6272A4)
    val messageText = Color(0xFFF8F8F2)

    val connectionLine = Color(0xFF44475A)

    fun laneColor(lane: Int): Color = laneColors[lane % laneColors.size]
}