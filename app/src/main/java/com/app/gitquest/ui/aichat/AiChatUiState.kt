package com.app.gitquest.ui.aichat

/**
 * A single message in the AI chat conversation.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * An Ai Report Reason
 */
enum class AiReportReason(
    val label: String,
) {
    INCORRECT("Incorrect answer"),
    OFFENSIVE("Offensive or inappropriate"),
    UNSAFE("Unsafe content"),
    OFF_TOPIC("Off-topic"),
    OTHER("Other"),
}

data class AiResponseReport(
    val messageText: String,
    val reason: AiReportReason,
    val userFeedback: String,
    val levelTitle: String?,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Context passed to the AI so it understands the current game state.
 */
data class GameContext(
    val levelTitle: String,
    val levelBriefing: String,
    val levelObjective: String,
    val availableCommands: List<String>,
    val currentBranch: String?,
    val commitCount: Int,
    val branchNames: List<String>,
    val stagedFiles: List<String>,
    val workingFiles: List<String>,
    val commandHistory: List<String>,
    val hintsUsed: Int,
    val goalFailures: List<String>,
    val isLevelCompleted: Boolean,
)

/**
 * UI state for the AI chat screen.
 */
data class AiChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Hi! I'm your Git assistant. Ask me anything about this level — " +
                    "why you're stuck, what a command does, or how to approach the objective.",
            isUser = false,
        ),
    ),
    val currentInput: String = "",
    val isGenerating: Boolean = false,

    val isModelLoaded: Boolean = false,
    val isModelDownloading: Boolean = false,
    val downloadProgress: Float = 0f,

    val modelStatus: AiModelStatus = AiModelStatus.MODEL_NOT_DOWNLOADED,
    val modelDownloadProgress: Float = 0f,
    val modelError: String? = null,
    val modelWarning: String? = null,

    val useFallback: Boolean = true,
)

/**
 * AI model status.
 */
enum class AiModelStatus {
    SMART_MODE_ONLY,
    MODEL_NOT_DOWNLOADED,
    MODEL_DOWNLOAD_WAITING_FOR_WIFI,
    MODEL_DOWNLOADING,
    MODEL_DOWNLOAD_FAILED,
    MODEL_LOADING,
    MODEL_READY,
    MODEL_FAILED,
}