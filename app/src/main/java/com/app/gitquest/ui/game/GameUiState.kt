package com.app.gitquest.ui.game

import com.app.gitquest.engine.GitState
import com.app.gitquest.engine.GoalResult
import com.app.gitquest.ui.aichat.GameContext

/**
 * UI state for the game screen. Wraps the engine's GitState
 * with game-level concerns like output history, hints, and stars.
 */
data class GameUiState(
    val gitState: GitState = GitState(),
    val terminalOutput: List<TerminalLine> = emptyList(),
    val currentInput: String = "",
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val commandCount: Int = 0,
    val hintsUsed: Int = 0,
    val maxHints: Int = 3,
    val hints: List<String> = emptyList(),
    val goalResult: GoalResult? = null,
    val isLevelComplete: Boolean = false,
    val starsEarned: Int = 0,
    val levelTitle: String = "",
    val levelBriefing: String = "",
    val levelObjective: String = "",
    val availableCommands: List<String> = emptyList(),
    val optimalCommandCount: Int = 0,
    val showBriefing: Boolean = true,
    val suggestions: List<String> = emptyList(),
    val stateHistory: List<GitState> = emptyList(),
    val canUndo: Boolean = false,
    val showAiChat: Boolean = false,
) {
    /**
     * Builds a GameContext for the AI chat from the current state.
     */
    fun toGameContext(): GameContext = GameContext(
        levelTitle = levelTitle,
        levelBriefing = levelBriefing,
        levelObjective = levelObjective,
        availableCommands = availableCommands,
        currentBranch = gitState.currentBranchName(),
        commitCount = gitState.commits.size,
        branchNames = gitState.branches.keys.toList(),
        stagedFiles = gitState.stagingArea.keys.toList(),
        workingFiles = gitState.workingDirectory.keys.toList(),
        commandHistory = commandHistory,
        hintsUsed = hintsUsed,
        goalFailures = goalResult?.failures ?: emptyList(),
        isLevelComplete
    )
}

/**
 * A single line in the terminal output. Tagged with a type for styling.
 */
data class TerminalLine(
    val text: String,
    val type: LineType,
)

enum class LineType {
    INPUT,
    OUTPUT,
    ERROR,
    SUCCESS,
    SYSTEM,
}