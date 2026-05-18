package com.app.gitquest.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.gitquest.ui.aichat.AiChatScreen
import com.app.gitquest.ui.visualizer.BranchVisualizer
import com.app.gitquest.ui.visualizer.BranchVisualizerStaticPreviewContent
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.zIndex

private val TerminalBg = Color(0xFF1E1E2E)
private val TerminalGreen = Color(0xFF50FA7B)
private val TerminalRed = Color(0xFFFF5555)
private val TerminalWhite = Color(0xFFF8F8F2)
private val TerminalYellow = Color(0xFFF1FA8C)
private val TerminalBlue = Color(0xFF8BE9FD)
private val TerminalMuted = Color(0xFF6272A4)
private val TerminalPurple = Color(0xFFBD93F9)
private val MonoFont = FontFamily.Monospace

@Composable
fun GameScreen(
    levelId: String,
    onNavigateBack: () -> Unit,
    onLevelCompleted: (String, Int) -> Unit,
    onNextLevel: (String, Int) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }

    var reportedStarsForCurrentCompletion by remember(levelId) {
        mutableIntStateOf(0)
    }

    val focusManager = LocalFocusManager.current

    val isCommandInputEnabled =
        !state.showBriefing &&
                !state.isLevelComplete &&
                !showResetDialog &&
                !showAiChat

    LaunchedEffect(levelId) {
        viewModel.loadLevel(levelId)
    }

    LaunchedEffect(
        state.isLevelComplete,
        state.starsEarned,
        levelId,
    ) {
        if (!state.isLevelComplete) {
            reportedStarsForCurrentCompletion = 0
            return@LaunchedEffect
        }

        val safeStars = state.starsEarned.coerceIn(0, 3)

        if (safeStars > reportedStarsForCurrentCompletion) {
            reportedStarsForCurrentCompletion = safeStars
            onLevelCompleted(levelId, safeStars)
        }
    }

    LaunchedEffect(
        state.showBriefing,
        state.isLevelComplete,
        showResetDialog,
        showAiChat,
    ) {
        if (!isCommandInputEnabled) {
            focusManager.clearFocus(force = true)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = TerminalBg) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    GameToolbar(
                        title = state.levelTitle,
                        commandCount = state.commandCount,
                        hintsRemaining = state.maxHints - state.hintsUsed,
                        canUndo = state.canUndo,
                        onHint = { viewModel.useHint() },
                        onUndo = { viewModel.undo() },
                        onReset = { showResetDialog = true },
                        onBack = onNavigateBack,
                        onAiChat = { showAiChat = true },
                    )

                    ObjectiveBanner(
                        objective = state.levelObjective,
                        progress = state.goalResult?.progress ?: 0f,
                    )

                    MainGameContent(
                        hasVisualizer = state.gitState.commits.isNotEmpty(),
                        visualizer = {
                            BranchVisualizer(
                                gitState = state.gitState,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        terminalOutput = {
                            TerminalOutput(
                                lines = state.terminalOutput,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )

                    AnimatedVisibility(
                        visible = state.suggestions.isNotEmpty() && isCommandInputEnabled,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.zIndex(50f),
                    ) {
                        SuggestionBar(
                            suggestions = state.suggestions,
                            onSelect = { suggestion ->
                                viewModel.applySuggestion(suggestion)
                            },
                        )
                    }

                    TerminalInput(
                        value = state.currentInput,
                        onValueChange = { viewModel.onInputChanged(it) },
                        onSubmit = { viewModel.executeCommand() },
                        enabled = isCommandInputEnabled,
                    )
                }
            }

            if (state.showBriefing) {
                BriefingOverlay(
                    title = state.levelTitle,
                    briefing = state.levelBriefing,
                    objective = state.levelObjective,
                    availableCommands = state.availableCommands,
                    onStart = {
                        focusManager.clearFocus(force = true)
                        viewModel.dismissBriefing()
                    },
                )
            }

            if (state.isLevelComplete) {
                LevelCompleteOverlay(
                    stars = state.starsEarned,
                    commandCount = state.commandCount,
                    optimalCount = state.optimalCommandCount,
                    hintsUsed = state.hintsUsed,
                    levelId = levelId,
                    onReplay = { viewModel.resetLevel() },
                    onWorldMap = {
                        val safeStars = state.starsEarned.coerceIn(0, 3)
                        onLevelCompleted(levelId, safeStars)
                        onNavigateBack()
                    },
                    onNext = onNextLevel,
                )
            }

            AnimatedVisibility(
                visible = showAiChat,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
            ) {
                AiChatScreen(
                    gameContext = state.toGameContext(),
                    onNavigateBack = { showAiChat = false },
                )
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    containerColor = Color(0xFF282A36),
                    title = {
                        Text(
                            text = "Reset level?",
                            color = TerminalWhite,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Text(
                            text = "All your progress on this level will be lost. Are you sure?",
                            color = TerminalMuted,
                            fontSize = 14.sp,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResetDialog = false
                                viewModel.resetLevel()
                            },
                        ) {
                            Text(
                                text = "Reset",
                                color = TerminalRed,
                                fontFamily = MonoFont,
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(
                                text = "Cancel",
                                color = TerminalMuted,
                                fontFamily = MonoFont,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MainGameContent(
    hasVisualizer: Boolean,
    visualizer: @Composable () -> Unit,
    terminalOutput: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        if (hasVisualizer) {
            BranchVisualizerViewport(
                modifier = Modifier.weight(0.45f),
            ) {
                visualizer()
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = TerminalMuted.copy(alpha = 0.3f),
            )

            TerminalOutputViewport(
                modifier = Modifier.weight(0.55f),
            ) {
                terminalOutput()
            }
        } else {
            TerminalOutputViewport(
                modifier = Modifier.weight(1f),
            ) {
                terminalOutput()
            }
        }
    }
}

@Composable
private fun BranchVisualizerViewport(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF181825))
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            content()
        }
    }
}

@Composable
private fun TerminalOutputViewport(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBg)
            .clipToBounds(),
    ) {
        content()
    }
}

@Composable
private fun GameToolbar(
    title: String,
    commandCount: Int,
    hintsRemaining: Int,
    canUndo: Boolean,
    onHint: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onAiChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "<",
                color = TerminalMuted,
                fontSize = 20.sp,
                fontFamily = MonoFont,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TerminalWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
            )

            Text(
                text = "Commands: $commandCount",
                color = TerminalMuted,
                fontSize = 11.sp,
                fontFamily = MonoFont,
            )
        }

        IconButton(onClick = onAiChat) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "AI Assistant",
                tint = TerminalPurple,
            )
        }

        IconButton(
            onClick = onHint,
            enabled = hintsRemaining > 0,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = "Hint ($hintsRemaining left)",
                tint = if (hintsRemaining > 0) TerminalYellow else TerminalMuted,
            )
        }

        IconButton(
            onClick = onUndo,
            enabled = canUndo,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) TerminalBlue else TerminalMuted,
            )
        }

        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset level",
                tint = TerminalMuted,
            )
        }
    }
}

@Composable
private fun ObjectiveBanner(
    objective: String,
    progress: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Objective: $objective",
            color = TerminalBlue,
            fontSize = 12.sp,
            fontFamily = MonoFont,
            lineHeight = 16.sp,
        )

        if (progress > 0f && progress < 1f) {
            Spacer(modifier = Modifier.height(3.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        color = TerminalMuted.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(
                            color = TerminalGreen,
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun TerminalOutput(
    lines: List<TerminalLine>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBg)
            .clipToBounds()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 8.dp,
        ),
    ) {
        items(lines) { line ->
            Text(
                text = line.text,
                color = when (line.type) {
                    LineType.INPUT -> TerminalGreen
                    LineType.OUTPUT -> TerminalWhite
                    LineType.ERROR -> TerminalRed
                    LineType.SUCCESS -> TerminalGreen
                    LineType.SYSTEM -> TerminalYellow
                },
                fontSize = 12.sp,
                fontFamily = MonoFont,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun SuggestionBar(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(50f)
            .background(TerminalBg)
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(TerminalMuted.copy(alpha = 0.24f))
                    .pointerInput(suggestion) {
                        detectTapGestures(
                            onTap = {
                                onSelect(suggestion)
                            },
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = suggestion,
                    color = TerminalBlue,
                    fontSize = 11.sp,
                    fontFamily = MonoFont,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun TerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181825))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$",
            color = TerminalGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
        )

        Spacer(modifier = Modifier.width(8.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            textStyle = TextStyle(
                color = TerminalWhite,
                fontSize = 14.sp,
                fontFamily = MonoFont,
            ),
            placeholder = {
                Text(
                    text = "Type a git command...",
                    color = TerminalMuted,
                    fontFamily = MonoFont,
                    fontSize = 14.sp,
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() },
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = TerminalGreen,
            ),
        )
    }
}

@Composable
private fun BriefingOverlay(
    title: String,
    briefing: String,
    objective: String,
    availableCommands: List<String>,
    onStart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(
                            pass = PointerEventPass.Final,
                        )

                        event.changes.forEach { pointerInputChange ->
                            pointerInputChange.consume()
                        }
                    }
                }
            }
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF282A36),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(
                    text = title,
                    color = TerminalGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Story:",
                    color = Color(0xFFFFB86C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = briefing,
                    color = TerminalWhite,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Objective:",
                    color = TerminalYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = objective,
                    color = TerminalWhite,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Commands available:",
                    color = TerminalMuted,
                    fontSize = 12.sp,
                    fontFamily = MonoFont,
                )

                Spacer(Modifier.height(6.dp))

                availableCommands.forEach { cmd ->
                    val explanation = commandExplanations[cmd]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            text = "  $cmd",
                            color = TerminalBlue,
                            fontSize = 13.sp,
                            fontFamily = MonoFont,
                        )

                        if (explanation != null) {
                            Text(
                                text = "    $explanation",
                                color = TerminalMuted,
                                fontSize = 11.sp,
                                fontFamily = MonoFont,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "Start Level",
                        fontFamily = MonoFont,
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    stars: Int,
    commandCount: Int,
    optimalCount: Int,
    hintsUsed: Int,
    levelId: String,
    onReplay: () -> Unit,
    onWorldMap: () -> Unit,
    onNext: (String, Int) -> Unit,
) {
    val nextLevelId = com.app.gitquest.data.levels.LevelRegistry.getNextLevelId(levelId)
    val safeStars = stars.coerceIn(0, 3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF282A36),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Level Complete!",
                    color = TerminalGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "★".repeat(safeStars) + "☆".repeat(3 - safeStars),
                    fontSize = 36.sp,
                    color = TerminalYellow,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Commands: $commandCount (optimal: $optimalCount)",
                    color = TerminalWhite,
                    fontSize = 13.sp,
                    fontFamily = MonoFont,
                )

                Text(
                    text = "Hints used: $hintsUsed",
                    color = TerminalMuted,
                    fontSize = 13.sp,
                    fontFamily = MonoFont,
                )

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (nextLevelId != null) {
                        FilledTonalButton(
                            onClick = {
                                onNext(nextLevelId, safeStars)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = "Next Level >",
                                fontFamily = MonoFont,
                                maxLines = 1,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onReplay) {
                            Text(
                                text = "Replay",
                                color = TerminalMuted,
                                fontFamily = MonoFont,
                                maxLines = 1,
                            )
                        }

                        TextButton(onClick = onWorldMap) {
                            Text(
                                text = "World Map",
                                color = TerminalBlue,
                                fontFamily = MonoFont,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val commandExplanations = mapOf(
    "git init" to "Create a new Git repository",
    "git add" to "Stage a file (e.g. git add file.txt)",
    "git add ." to "Stage all changed files",
    "git commit" to "Save snapshot\n    (e.g. git commit -m \"msg\")",
    "git status" to "Show staged/modified/untracked files",
    "git log" to "Display commit history",
    "git branch" to "List or create branches",
    "git checkout" to "Switch to a branch or commit",
    "git switch" to "Switch branches (modern)",
    "git merge" to "Combine branch changes",
    "git rebase" to "Replay commits on another branch",
    "git cherry-pick" to "Copy a specific commit",
    "git reset" to "Move HEAD backward",
    "git revert" to "Undo a commit with a new commit",
    "git stash" to "Save uncommitted changes temporarily",
    "git stash pop" to "Restore stashed changes",
    "git reflog" to "Show all HEAD movements",
)

@Preview(
    name = "Game Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF1E1E2E,
    widthDp = 412,
    heightDp = 915,
)
@Composable
private fun GameScreenPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(containerColor = TerminalBg) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                GameToolbar(
                    title = "Branch Basics",
                    commandCount = 4,
                    hintsRemaining = 2,
                    canUndo = true,
                    onHint = {},
                    onUndo = {},
                    onReset = {},
                    onBack = {},
                    onAiChat = {},
                )

                ObjectiveBanner(
                    objective = "Create a feature branch, make a commit, and merge it back into main.",
                    progress = 0.65f,
                )

                MainGameContent(
                    hasVisualizer = true,
                    visualizer = {
                        BranchVisualizerStaticPreviewContent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                        )
                    },
                    terminalOutput = {
                        PreviewTerminalOutput(
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                SuggestionBar(
                    suggestions = listOf(
                        "git status",
                        "git branch feature",
                        "git checkout feature",
                        "git merge feature",
                    ),
                    onSelect = {},
                )

                TerminalInput(
                    value = "git commit -m \"Add feature\"",
                    onValueChange = {},
                    onSubmit = {},
                    enabled = true,
                )
            }
        }
    }
}

@Composable
private fun PreviewTerminalOutput(
    modifier: Modifier = Modifier,
) {
    val previewLines = listOf(
        LineType.SYSTEM to "Welcome to GitQuest.",
        LineType.INPUT to "$ git status",
        LineType.OUTPUT to "On branch main",
        LineType.OUTPUT to "nothing to commit, working tree clean",
        LineType.INPUT to "$ git branch feature",
        LineType.INPUT to "$ git checkout feature",
        LineType.OUTPUT to "Switched to branch 'feature'",
        LineType.SUCCESS to "Progress: branch created and selected.",
        LineType.INPUT to "$ git add feature.txt",
        LineType.INPUT to "$ git commit -m \"Add feature\"",
        LineType.SUCCESS to "[feature c3d4e5f] Add feature",
        LineType.INPUT to "$ git checkout main",
        LineType.OUTPUT to "Switched to branch 'main'",
        LineType.INPUT to "$ git merge feature",
        LineType.SUCCESS to "Merge completed successfully.",
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBg)
            .clipToBounds()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 8.dp,
        ),
    ) {
        items(previewLines) { (type, text) ->
            Text(
                text = text,
                color = when (type) {
                    LineType.INPUT -> TerminalGreen
                    LineType.OUTPUT -> TerminalWhite
                    LineType.ERROR -> TerminalRed
                    LineType.SUCCESS -> TerminalGreen
                    LineType.SYSTEM -> TerminalYellow
                },
                fontSize = 12.sp,
                fontFamily = MonoFont,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}