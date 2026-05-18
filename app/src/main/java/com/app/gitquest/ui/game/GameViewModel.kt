package com.app.gitquest.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.app.gitquest.data.levels.LevelDef
import com.app.gitquest.data.levels.LevelRegistry
import com.app.gitquest.engine.GitEngine
import com.app.gitquest.engine.GitResult
import com.app.gitquest.engine.GitState
import com.app.gitquest.engine.GoalChecker
import com.app.gitquest.engine.parser.CommandParser
import com.app.gitquest.engine.parser.ParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val engine = GitEngine()
    private val parser = CommandParser()
    private val goalChecker = GoalChecker()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentLevel: LevelDef? = null

    fun loadLevel(levelId: String) {
        val level = LevelRegistry.getLevel(levelId) ?: return

        currentLevel = level
        _uiState.value = GameUiState(
            gitState = level.initialState,
            levelTitle = level.title,
            levelBriefing = level.briefing,
            levelObjective = level.objective,
            hints = level.hints,
            availableCommands = level.availableCommands,
            optimalCommandCount = level.optimalCommandCount,
            showBriefing = true,
            stateHistory = listOf(level.initialState),
        )
    }

    fun dismissBriefing() {
        _uiState.update { it.copy(showBriefing = false) }
    }

    fun onInputChanged(input: String) {
        _uiState.update {
            it.copy(
                currentInput = input,
                suggestions = if (input.length >= 2) generateSuggestions(input, it.gitState) else emptyList(),
            )
        }
    }

    fun executeCommand() {
        val state = _uiState.value
        val input = state.currentInput.trim()
        if (input.isEmpty() || state.isLevelComplete) return

        val newOutput = state.terminalOutput.toMutableList()
        newOutput.add(TerminalLine("$ $input", LineType.INPUT))

        when (val parseResult = parser.parse(input)) {
            is ParseResult.Empty -> return

            is ParseResult.Error -> {
                newOutput.add(TerminalLine(parseResult.message, LineType.ERROR))
                _uiState.update {
                    it.copy(
                        terminalOutput = newOutput,
                        currentInput = "",
                        commandHistory = listOf(input) + it.commandHistory,
                        historyIndex = -1,
                        suggestions = emptyList(),
                    )
                }
            }

            is ParseResult.Ok -> {
                val result = engine.execute(state.gitState, parseResult.command)

                when (result) {
                    is GitResult.Error -> {
                        newOutput.add(TerminalLine(result.message, LineType.ERROR))
                        _uiState.update {
                            it.copy(
                                terminalOutput = newOutput,
                                currentInput = "",
                                commandHistory = listOf(input) + it.commandHistory,
                                historyIndex = -1,
                                commandCount = it.commandCount + 1,
                                suggestions = emptyList(),
                            )
                        }
                    }

                    is GitResult.Success -> {
                        if (result.message.isNotBlank()) {
                            newOutput.add(TerminalLine(result.message, LineType.OUTPUT))
                        }

                        val level = currentLevel
                        val goalResult = if (level != null) {
                            goalChecker.check(result.newState, level.goal)
                        } else null

                        val isComplete = goalResult?.isComplete == true
                        val newCommandCount = state.commandCount + 1

                        if (isComplete) {
                            val stars = calculateStars(newCommandCount, state.hintsUsed, level!!.optimalCommandCount)
                            newOutput.add(TerminalLine("", LineType.OUTPUT))
                            newOutput.add(TerminalLine("Level complete! ${"★".repeat(stars)}${"☆".repeat(3 - stars)}", LineType.SUCCESS))

                            _uiState.update {
                                it.copy(
                                    gitState = result.newState,
                                    terminalOutput = newOutput,
                                    currentInput = "",
                                    commandHistory = listOf(input) + it.commandHistory,
                                    historyIndex = -1,
                                    commandCount = newCommandCount,
                                    goalResult = goalResult,
                                    isLevelComplete = true,
                                    starsEarned = stars,
                                    suggestions = emptyList(),
                                    stateHistory = it.stateHistory + result.newState,
                                    canUndo = true,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    gitState = result.newState,
                                    terminalOutput = newOutput,
                                    currentInput = "",
                                    commandHistory = listOf(input) + it.commandHistory,
                                    historyIndex = -1,
                                    commandCount = newCommandCount,
                                    goalResult = goalResult,
                                    suggestions = emptyList(),
                                    stateHistory = it.stateHistory + result.newState,
                                    canUndo = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun useHint() {
        val state = _uiState.value
        if (state.hintsUsed >= state.maxHints || state.hintsUsed >= state.hints.size) return

        val hint = state.hints[state.hintsUsed]
        val newOutput = state.terminalOutput.toMutableList()
        newOutput.add(TerminalLine("Hint: $hint", LineType.SYSTEM))

        _uiState.update {
            it.copy(
                terminalOutput = newOutput,
                hintsUsed = it.hintsUsed + 1,
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.stateHistory.size <= 1) return

        val newHistory = state.stateHistory.dropLast(1)
        val previousState = newHistory.last()

        val newOutput = state.terminalOutput.toMutableList()
        newOutput.add(TerminalLine("Undid last command", LineType.SYSTEM))

        _uiState.update {
            it.copy(
                gitState = previousState,
                stateHistory = newHistory,
                terminalOutput = newOutput,
                canUndo = newHistory.size > 1,
                isLevelComplete = false,
            )
        }
    }

    fun resetLevel() {
        currentLevel?.let { loadLevel(it.id) }
    }

    fun applySuggestion(suggestion: String) {
        // Place cursor-ready suggestion: if the suggestion ends with a placeholder
        // like <file>, just put the base command so user can type the argument
        val cleanSuggestion = suggestion
            .replace(Regex("<[^>]+>$"), "")
            .trimEnd() + " "

        // If the suggestion has no placeholder (like "git init"), use it directly without trailing space
        val finalSuggestion = if (suggestion.contains("<")) {
            cleanSuggestion
        } else {
            suggestion
        }

        _uiState.update { it.copy(currentInput = finalSuggestion, suggestions = emptyList()) }
    }

    fun navigateHistory(direction: Int) {
        val state = _uiState.value
        if (state.commandHistory.isEmpty()) return

        val newIndex = (state.historyIndex + direction)
            .coerceIn(-1, state.commandHistory.size - 1)

        val newInput = if (newIndex == -1) "" else state.commandHistory[newIndex]

        _uiState.update {
            it.copy(currentInput = newInput, historyIndex = newIndex)
        }
    }

    private fun calculateStars(commands: Int, hints: Int, optimal: Int): Int {
        return when {
            hints == 0 && commands <= optimal -> 3
            hints <= 1 && commands <= optimal * 2 -> 2
            else -> 1
        }
    }

    /**
     * Generate context-aware suggestions based on the current git state.
     * Shows actual file names and branch names instead of generic placeholders.
     */
    private fun generateSuggestions(input: String, gitState: GitState): List<String> {
        val prefix = input.lowercase().trim()
        val suggestions = mutableListOf<String>()

        // Context-aware completions
        val files = gitState.workingDirectory.keys.toList()
        val branches = gitState.branches.keys.toList()
        val currentFiles = gitState.headCommit()?.files ?: emptyMap()

        // Files that have changes (not yet staged)
        val changedFiles = gitState.workingDirectory.filter { (name, content) ->
            currentFiles[name] != content && name !in gitState.stagingArea
        }.keys.toList()

        // Staged files
        val stagedFiles = gitState.stagingArea.keys.toList()

        when {
            // git add <file> — show changed files
            prefix.startsWith("git a") || prefix == "git add" -> {
                if (changedFiles.isNotEmpty()) {
                    suggestions.add("git add .")
                    changedFiles.forEach { suggestions.add("git add $it") }
                } else if (files.isNotEmpty()) {
                    suggestions.add("git add .")
                    files.forEach { suggestions.add("git add $it") }
                } else {
                    suggestions.add("git add <file>")
                }
            }

            // git commit — only suggest if there are staged files
            prefix.startsWith("git co") && !prefix.startsWith("git check") -> {
                suggestions.add("git commit -m \"\"")
            }

            // git checkout — show branches and commit IDs
            prefix.startsWith("git check") -> {
                branches.forEach { suggestions.add("git checkout $it") }
                suggestions.add("git checkout -b <name>")
            }

            // git switch — show branches
            prefix.startsWith("git sw") -> {
                branches.forEach { suggestions.add("git switch $it") }
                suggestions.add("git switch -c <name>")
            }

            // git branch
            prefix.startsWith("git b") -> {
                suggestions.add("git branch")
                suggestions.add("git branch <name>")
            }

            // git merge — show other branches
            prefix.startsWith("git me") -> {
                val current = gitState.currentBranchName()
                branches.filter { it != current }.forEach {
                    suggestions.add("git merge $it")
                }
            }

            // git rebase
            prefix.startsWith("git reb") -> {
                val current = gitState.currentBranchName()
                branches.filter { it != current }.forEach {
                    suggestions.add("git rebase $it")
                }
            }

            // git reset
            prefix.startsWith("git res") && !prefix.startsWith("git rev") -> {
                suggestions.add("git reset --soft HEAD~1")
                suggestions.add("git reset --mixed HEAD~1")
                suggestions.add("git reset --hard HEAD~1")
            }

            // git revert
            prefix.startsWith("git rev") -> {
                suggestions.add("git revert <commit-id>")
            }

            // General matching from available commands
            else -> {
                val available = _uiState.value.availableCommands
                available.filter { it.lowercase().startsWith(prefix) }.forEach {
                    suggestions.add(it)
                }
                // Also add general commands
                listOf("git init", "git status", "git log", "git stash", "git stash pop", "git reflog", "git help")
                    .filter { it.startsWith(prefix) && it !in suggestions }
                    .forEach { suggestions.add(it) }
            }
        }

        return suggestions.distinct().take(5)
    }
}