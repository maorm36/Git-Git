package com.app.gitquest.ui.visualizer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gitquest.engine.GitState

private data class VisualizerMetrics(
    val nodeRadius: Float,
    val laneWidth: Float,
    val minLaneWidth: Float,
    val rowHeight: Float,
    val paddingTop: Float,
    val paddingBottom: Float,
    val minSidePadding: Float,
    val labelColumnGap: Float,
    val labelColumnMinWidth: Float,
    val labelColumnPreferredWidth: Float,
    val connectionWidth: Float,
    val headRingWidth: Float,
    val hashTextSize: Float,
    val messageTextSize: Float,
    val branchPillTextSize: Float,
    val branchPillHorizontalPadding: Float,
    val branchPillHeight: Float,
    val minCanvasHeight: Float,
)

private data class VisualizerLayoutBounds(
    val treeStartX: Float,
    val laneWidth: Float,
    val treeWidth: Float,
    val labelX: Float,
    val labelWidth: Float,
)

@Composable
private fun rememberVisualizerMetrics(): VisualizerMetrics {
    return with(LocalDensity.current) {
        VisualizerMetrics(
            nodeRadius = 8.dp.toPx(),
            laneWidth = 70.dp.toPx(),
            minLaneWidth = 42.dp.toPx(),
            rowHeight = 60.dp.toPx(),
            paddingTop = 46.dp.toPx(),
            paddingBottom = 48.dp.toPx(),
            minSidePadding = 16.dp.toPx(),
            labelColumnGap = 28.dp.toPx(),
            labelColumnMinWidth = 96.dp.toPx(),
            labelColumnPreferredWidth = 190.dp.toPx(),
            connectionWidth = 2.dp.toPx(),
            headRingWidth = 2.dp.toPx(),
            hashTextSize = 12.sp.toPx(),
            messageTextSize = 9.sp.toPx(),
            branchPillTextSize = 8.sp.toPx(),
            branchPillHorizontalPadding = 8.dp.toPx(),
            branchPillHeight = 16.dp.toPx(),
            minCanvasHeight = 260.dp.toPx(),
        )
    }
}

@Composable
fun BranchVisualizer(
    gitState: GitState,
    modifier: Modifier = Modifier,
) {
    val metrics = rememberVisualizerMetrics()
    val density = LocalDensity.current

    val layoutEngine = remember { GraphLayoutEngine() }
    var currentLayout by remember { mutableStateOf<GraphLayout?>(null) }
    var previousLayout by remember { mutableStateOf<GraphLayout?>(null) }

    val animProgress = remember { Animatable(1f) }

    LaunchedEffect(gitState) {
        val newLayout = layoutEngine.layout(gitState)

        if (currentLayout != null && currentLayout != newLayout) {
            previousLayout = currentLayout
            currentLayout = newLayout

            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        } else {
            currentLayout = newLayout
        }
    }

    val layout = currentLayout ?: return

    val canvasHeight = (
            layout.rowCount * metrics.rowHeight +
                    metrics.paddingTop +
                    metrics.paddingBottom
            ).coerceAtLeast(metrics.minCanvasHeight)

    var scrollOffset by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { canvasHeight.toDp() })
            .pointerInput(canvasHeight) {
                detectTransformGestures { _, pan, _, _ ->
                    scrollOffset = (scrollOffset + pan.y)
                        .coerceIn(
                            minimumValue = -(canvasHeight - metrics.minCanvasHeight)
                                .coerceAtLeast(0f),
                            maximumValue = 0f,
                        )
                }
            },
    ) {
        val progress = animProgress.value
        val bounds = calculateLayoutBounds(
            laneCount = layout.laneCount(),
            metrics = metrics,
        )

        drawLabelColumnGuide(
            bounds = bounds,
        )

        drawConnections(
            layout = layout,
            bounds = bounds,
            metrics = metrics,
            progress = progress,
            prevLayout = previousLayout,
            scrollY = scrollOffset,
        )

        drawNodes(
            layout = layout,
            bounds = bounds,
            metrics = metrics,
            progress = progress,
            prevLayout = previousLayout,
            scrollY = scrollOffset,
        )
    }
}

private fun GraphLayout.laneCount(): Int {
    return (nodes.maxOfOrNull { it.lane } ?: 0) + 1
}

private fun DrawScope.calculateLayoutBounds(
    laneCount: Int,
    metrics: VisualizerMetrics,
): VisualizerLayoutBounds {
    val availableWidth = (size.width - metrics.minSidePadding * 2f)
        .coerceAtLeast(1f)

    val maxTreeWidth = (
            availableWidth -
                    metrics.nodeRadius * 2f -
                    metrics.labelColumnGap -
                    metrics.labelColumnMinWidth
            ).coerceAtLeast(0f)

    val actualLaneWidth = if (laneCount > 1) {
        (maxTreeWidth / (laneCount - 1))
            .coerceIn(
                minimumValue = metrics.minLaneWidth,
                maximumValue = metrics.laneWidth,
            )
    } else {
        metrics.laneWidth
    }

    val treeWidth = if (laneCount <= 1) {
        0f
    } else {
        (laneCount - 1) * actualLaneWidth
    }

    val remainingForLabel = (
            availableWidth -
                    metrics.nodeRadius * 2f -
                    treeWidth -
                    metrics.labelColumnGap
            ).coerceAtLeast(metrics.labelColumnMinWidth)

    val labelWidth = remainingForLabel.coerceIn(
        minimumValue = metrics.labelColumnMinWidth,
        maximumValue = metrics.labelColumnPreferredWidth,
    )

    val contentWidth =
        metrics.nodeRadius * 2f +
                treeWidth +
                metrics.labelColumnGap +
                labelWidth

    val contentStartX = ((size.width - contentWidth) / 2f)
        .coerceAtLeast(metrics.minSidePadding)

    val treeStartX = contentStartX + metrics.nodeRadius

    val labelX =
        treeStartX +
                treeWidth +
                metrics.nodeRadius +
                metrics.labelColumnGap

    return VisualizerLayoutBounds(
        treeStartX = treeStartX,
        laneWidth = actualLaneWidth,
        treeWidth = treeWidth,
        labelX = labelX,
        labelWidth = labelWidth,
    )
}

private fun DrawScope.drawLabelColumnGuide(
    bounds: VisualizerLayoutBounds,
) {
    val guideX = bounds.labelX - 12.dp.toPx()

    drawLine(
        color = VisualizerColors.hashText.copy(alpha = 0.08f),
        start = Offset(guideX, 0f),
        end = Offset(guideX, size.height),
        strokeWidth = 1.dp.toPx(),
    )
}

/**
 * Draw connection lines between parent and child commits.
 * Uses cubic bezier curves for cross-lane connections.
 */
private fun DrawScope.drawConnections(
    layout: GraphLayout,
    bounds: VisualizerLayoutBounds,
    metrics: VisualizerMetrics,
    progress: Float,
    prevLayout: GraphLayout?,
    scrollY: Float,
) {
    for (node in layout.nodes) {
        for (parentId in node.parentIds) {
            val parentNode = layout.nodeMap[parentId] ?: continue

            val childPos = nodePosition(
                node = node,
                bounds = bounds,
                metrics = metrics,
                scrollY = scrollY,
            )

            val parentPos = nodePosition(
                node = parentNode,
                bounds = bounds,
                metrics = metrics,
                scrollY = scrollY,
            )

            val isNewNode = prevLayout != null && node.commitId !in prevLayout.nodeMap
            val lineAlpha = if (isNewNode) progress else 1f

            val color = VisualizerColors
                .laneColor(node.lane)
                .copy(alpha = lineAlpha * 0.72f)

            if (node.lane == parentNode.lane) {
                drawLine(
                    color = color,
                    start = Offset(
                        x = childPos.x,
                        y = childPos.y + metrics.nodeRadius,
                    ),
                    end = Offset(
                        x = parentPos.x,
                        y = parentPos.y - metrics.nodeRadius,
                    ),
                    strokeWidth = metrics.connectionWidth,
                )
            } else {
                val midY = (childPos.y + parentPos.y) / 2f

                val path = Path().apply {
                    moveTo(
                        x = childPos.x,
                        y = childPos.y + metrics.nodeRadius,
                    )

                    cubicTo(
                        x1 = childPos.x,
                        y1 = midY,
                        x2 = parentPos.x,
                        y2 = midY,
                        x3 = parentPos.x,
                        y3 = parentPos.y - metrics.nodeRadius,
                    )
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = metrics.connectionWidth),
                )
            }
        }
    }
}

/**
 * Draw commit nodes with labels, branch pills, and HEAD indicator.
 */
private fun DrawScope.drawNodes(
    layout: GraphLayout,
    bounds: VisualizerLayoutBounds,
    metrics: VisualizerMetrics,
    progress: Float,
    prevLayout: GraphLayout?,
    scrollY: Float,
) {
    for (node in layout.nodes) {
        val pos = nodePosition(
            node = node,
            bounds = bounds,
            metrics = metrics,
            scrollY = scrollY,
        )

        val isNewNode = prevLayout != null && node.commitId !in prevLayout.nodeMap

        val nodeScale = if (isNewNode) progress else 1f
        val nodeAlpha = if (isNewNode) progress else 1f
        val radius = metrics.nodeRadius * nodeScale

        if (radius <= 0f) continue

        val nodeColor = when {
            node.isMergeCommit -> VisualizerColors.mergeNode
            else -> VisualizerColors.laneColor(node.lane)
        }

        if (node.isHead) {
            val ringColor = if (node.isDetachedHead) {
                VisualizerColors.detachedHead
            } else {
                VisualizerColors.headIndicator
            }

            drawCircle(
                color = ringColor.copy(alpha = nodeAlpha),
                radius = radius + 5.dp.toPx(),
                center = pos,
                style = Stroke(width = metrics.headRingWidth),
            )
        }

        drawCircle(
            color = nodeColor.copy(alpha = nodeAlpha),
            radius = radius,
            center = pos,
        )

        drawCircle(
            color = VisualizerColors.background.copy(alpha = nodeAlpha),
            radius = radius * 0.4f,
            center = pos,
        )

        drawCommitLabelInColumn(
            node = node,
            bounds = bounds,
            metrics = metrics,
            y = pos.y,
            nodeColor = nodeColor,
            alpha = nodeAlpha,
        )

        if (node.branchLabels.isNotEmpty()) {
            var pillX = pos.x - radius - 12.dp.toPx()

            for (label in node.branchLabels.reversed()) {
                drawBranchPill(
                    text = label,
                    x = pillX,
                    y = pos.y - radius - 18.dp.toPx(),
                    color = nodeColor.copy(alpha = nodeAlpha),
                    alpha = nodeAlpha,
                    metrics = metrics,
                )

                pillX -= 44.dp.toPx()
            }
        }
    }
}

private fun DrawScope.drawCommitLabelInColumn(
    node: PositionedNode,
    bounds: VisualizerLayoutBounds,
    metrics: VisualizerMetrics,
    y: Float,
    nodeColor: Color,
    alpha: Float,
) {
    val markerStartX = bounds.labelX - 12.dp.toPx()
    val markerEndX = bounds.labelX - 5.dp.toPx()

    drawLine(
        color = nodeColor.copy(alpha = alpha * 0.45f),
        start = Offset(markerStartX, y),
        end = Offset(markerEndX, y),
        strokeWidth = 1.dp.toPx(),
    )

    drawContext.canvas.nativeCanvas.apply {
        val hashPaint = android.graphics.Paint().apply {
            color = VisualizerColors.hashText
                .copy(alpha = alpha)
                .toArgb()
            textSize = metrics.hashTextSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.LEFT
        }

        val messagePaint = android.graphics.Paint().apply {
            color = VisualizerColors.messageText
                .copy(alpha = alpha * 0.84f)
                .toArgb()
            textSize = metrics.messageTextSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
        }

        val hash = node.commitId.take(7)

        val message = ellipsizeToWidth(
            text = node.message,
            paint = messagePaint,
            maxWidth = bounds.labelWidth,
        )

        drawText(
            hash,
            bounds.labelX,
            y - 2.dp.toPx(),
            hashPaint,
        )

        drawText(
            message,
            bounds.labelX,
            y + 12.dp.toPx(),
            messagePaint,
        )
    }
}

/**
 * Draw a rounded pill showing a branch name.
 */
private fun DrawScope.drawBranchPill(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    alpha: Float,
    metrics: VisualizerMetrics,
) {
    val pillWidth =
        text.length * metrics.branchPillTextSize * 0.62f +
                metrics.branchPillHorizontalPadding * 2f

    val pillHeight = metrics.branchPillHeight

    drawRoundRect(
        color = color.copy(alpha = alpha * 0.25f),
        topLeft = Offset(
            x = x - pillWidth / 2f,
            y = y - pillHeight / 2f,
        ),
        size = Size(
            width = pillWidth,
            height = pillHeight,
        ),
        cornerRadius = CornerRadius(pillHeight / 2f),
    )

    drawRoundRect(
        color = color.copy(alpha = alpha * 0.65f),
        topLeft = Offset(
            x = x - pillWidth / 2f,
            y = y - pillHeight / 2f,
        ),
        size = Size(
            width = pillWidth,
            height = pillHeight,
        ),
        cornerRadius = CornerRadius(pillHeight / 2f),
        style = Stroke(width = 1.dp.toPx()),
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = VisualizerColors.labelText
                .copy(alpha = alpha)
                .toArgb()
            textSize = metrics.branchPillTextSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawText(
            text,
            x,
            y + metrics.branchPillTextSize * 0.35f,
            paint,
        )
    }
}

/**
 * Calculate pixel position for a node.
 *
 * The graph tree and the commit-label column are separate areas.
 */
private fun nodePosition(
    node: PositionedNode,
    bounds: VisualizerLayoutBounds,
    metrics: VisualizerMetrics,
    scrollY: Float,
): Offset {
    val x = bounds.treeStartX + node.lane * bounds.laneWidth
    val y = metrics.paddingTop + node.row * metrics.rowHeight + scrollY

    return Offset(x, y)
}

private fun ellipsizeToWidth(
    text: String,
    paint: android.graphics.Paint,
    maxWidth: Float,
): String {
    if (paint.measureText(text) <= maxWidth) {
        return text
    }

    val ellipsis = ".."
    var end = text.length

    while (end > 0) {
        val candidate = text.take(end) + ellipsis

        if (paint.measureText(candidate) <= maxWidth) {
            return candidate
        }

        end--
    }

    return ellipsis
}

@Preview(
    name = "Branch Visualizer Preview",
    showBackground = true,
    backgroundColor = 0xFF181825,
    widthDp = 412,
    heightDp = 360,
)
@Composable
private fun BranchVisualizerPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181825)),
    ) {
        BranchVisualizerStaticPreviewContent(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Preview-only visualizer content.
 *
 * This does not affect the real BranchVisualizer logic.
 * It exists only so Android Studio Preview can render the visualizer UI
 * without needing a real GitState instance.
 */
@Composable
internal fun BranchVisualizerStaticPreviewContent(
    modifier: Modifier = Modifier,
) {
    val metrics = rememberVisualizerMetrics()

    Canvas(modifier = modifier) {
        val nodes = listOf(
            PreviewGraphNode(
                commitId = "a1b2c3d",
                message = "Initial commit",
                lane = 0,
                row = 4,
                parentIds = emptyList(),
                branchLabels = emptyList(),
                isHead = false,
                isMergeCommit = false,
            ),
            PreviewGraphNode(
                commitId = "b2c3d4e",
                message = "Add README",
                lane = 0,
                row = 3,
                parentIds = listOf("a1b2c3d"),
                branchLabels = emptyList(),
                isHead = false,
                isMergeCommit = false,
            ),
            PreviewGraphNode(
                commitId = "c3d4e5f",
                message = "Feature work",
                lane = 1,
                row = 2,
                parentIds = listOf("b2c3d4e"),
                branchLabels = listOf("feature"),
                isHead = false,
                isMergeCommit = false,
            ),
            PreviewGraphNode(
                commitId = "d4e5f6g",
                message = "Main update",
                lane = 0,
                row = 1,
                parentIds = listOf("b2c3d4e"),
                branchLabels = emptyList(),
                isHead = false,
                isMergeCommit = false,
            ),
            PreviewGraphNode(
                commitId = "e5f6g7h",
                message = "Merge feature",
                lane = 0,
                row = 0,
                parentIds = listOf("d4e5f6g", "c3d4e5f"),
                branchLabels = listOf("main"),
                isHead = true,
                isMergeCommit = true,
            ),
        )

        val nodeMap = nodes.associateBy { it.commitId }

        val bounds = calculateLayoutBounds(
            laneCount = (nodes.maxOfOrNull { it.lane } ?: 0) + 1,
            metrics = metrics,
        )

        fun nodePosition(node: PreviewGraphNode): Offset {
            return Offset(
                x = bounds.treeStartX + node.lane * bounds.laneWidth,
                y = metrics.paddingTop + node.row * metrics.rowHeight,
            )
        }

        drawLabelColumnGuide(bounds = bounds)

        nodes.forEach { node ->
            node.parentIds.forEach { parentId ->
                val parentNode = nodeMap[parentId] ?: return@forEach

                val childPos = nodePosition(node)
                val parentPos = nodePosition(parentNode)

                val connectionColor = VisualizerColors
                    .laneColor(node.lane)
                    .copy(alpha = 0.72f)

                if (node.lane == parentNode.lane) {
                    drawLine(
                        color = connectionColor,
                        start = Offset(
                            x = childPos.x,
                            y = childPos.y + metrics.nodeRadius,
                        ),
                        end = Offset(
                            x = parentPos.x,
                            y = parentPos.y - metrics.nodeRadius,
                        ),
                        strokeWidth = metrics.connectionWidth,
                    )
                } else {
                    val midY = (childPos.y + parentPos.y) / 2f

                    val path = Path().apply {
                        moveTo(
                            x = childPos.x,
                            y = childPos.y + metrics.nodeRadius,
                        )

                        cubicTo(
                            x1 = childPos.x,
                            y1 = midY,
                            x2 = parentPos.x,
                            y2 = midY,
                            x3 = parentPos.x,
                            y3 = parentPos.y - metrics.nodeRadius,
                        )
                    }

                    drawPath(
                        path = path,
                        color = connectionColor,
                        style = Stroke(width = metrics.connectionWidth),
                    )
                }
            }
        }

        nodes.forEach { node ->
            val pos = nodePosition(node)

            val nodeColor = if (node.isMergeCommit) {
                VisualizerColors.mergeNode
            } else {
                VisualizerColors.laneColor(node.lane)
            }

            if (node.isHead) {
                drawCircle(
                    color = VisualizerColors.headIndicator,
                    radius = metrics.nodeRadius + 5.dp.toPx(),
                    center = pos,
                    style = Stroke(width = metrics.headRingWidth),
                )
            }

            drawCircle(
                color = nodeColor,
                radius = metrics.nodeRadius,
                center = pos,
            )

            drawCircle(
                color = VisualizerColors.background,
                radius = metrics.nodeRadius * 0.4f,
                center = pos,
            )

            drawPreviewCommitLabelInColumn(
                node = node,
                bounds = bounds,
                metrics = metrics,
                y = pos.y,
                nodeColor = nodeColor,
            )

            if (node.branchLabels.isNotEmpty()) {
                var pillX = pos.x - metrics.nodeRadius - 12.dp.toPx()

                node.branchLabels.reversed().forEach { label ->
                    drawPreviewBranchPill(
                        text = label,
                        x = pillX,
                        y = pos.y - metrics.nodeRadius - 18.dp.toPx(),
                        color = nodeColor,
                        metrics = metrics,
                    )

                    pillX -= 44.dp.toPx()
                }
            }
        }
    }
}

private data class PreviewGraphNode(
    val commitId: String,
    val message: String,
    val lane: Int,
    val row: Int,
    val parentIds: List<String>,
    val branchLabels: List<String>,
    val isHead: Boolean,
    val isMergeCommit: Boolean,
)

private fun DrawScope.drawPreviewCommitLabelInColumn(
    node: PreviewGraphNode,
    bounds: VisualizerLayoutBounds,
    metrics: VisualizerMetrics,
    y: Float,
    nodeColor: Color,
) {
    val markerStartX = bounds.labelX - 12.dp.toPx()
    val markerEndX = bounds.labelX - 5.dp.toPx()

    drawLine(
        color = nodeColor.copy(alpha = 0.45f),
        start = Offset(markerStartX, y),
        end = Offset(markerEndX, y),
        strokeWidth = 1.dp.toPx(),
    )

    drawContext.canvas.nativeCanvas.apply {
        val hashPaint = android.graphics.Paint().apply {
            color = VisualizerColors.hashText.toArgb()
            textSize = metrics.hashTextSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.LEFT
        }

        val messagePaint = android.graphics.Paint().apply {
            color = VisualizerColors.messageText.copy(alpha = 0.84f).toArgb()
            textSize = metrics.messageTextSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
        }

        val message = ellipsizeToWidth(
            text = node.message,
            paint = messagePaint,
            maxWidth = bounds.labelWidth,
        )

        drawText(
            node.commitId.take(7),
            bounds.labelX,
            y - 2.dp.toPx(),
            hashPaint,
        )

        drawText(
            message,
            bounds.labelX,
            y + 12.dp.toPx(),
            messagePaint,
        )
    }
}

private fun DrawScope.drawPreviewBranchPill(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    metrics: VisualizerMetrics,
) {
    val pillWidth =
        text.length * metrics.branchPillTextSize * 0.62f +
                metrics.branchPillHorizontalPadding * 2f

    val pillHeight = metrics.branchPillHeight

    drawRoundRect(
        color = color.copy(alpha = 0.25f),
        topLeft = Offset(
            x = x - pillWidth / 2f,
            y = y - pillHeight / 2f,
        ),
        size = Size(
            width = pillWidth,
            height = pillHeight,
        ),
        cornerRadius = CornerRadius(pillHeight / 2f),
    )

    drawRoundRect(
        color = color.copy(alpha = 0.65f),
        topLeft = Offset(
            x = x - pillWidth / 2f,
            y = y - pillHeight / 2f,
        ),
        size = Size(
            width = pillWidth,
            height = pillHeight,
        ),
        cornerRadius = CornerRadius(pillHeight / 2f),
        style = Stroke(width = 1.dp.toPx()),
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = VisualizerColors.labelText.toArgb()
            textSize = metrics.branchPillTextSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawText(
            text,
            x,
            y + metrics.branchPillTextSize * 0.35f,
            paint,
        )
    }
}