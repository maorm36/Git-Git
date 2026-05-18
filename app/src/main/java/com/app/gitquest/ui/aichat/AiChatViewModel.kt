package com.app.gitquest.ui.aichat

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.gitquest.BuildConfig
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


@HiltViewModel
class AiChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private companion object {
        private const val MODEL_FILE_NAME = "gemma-2b-it-gpu-int4.bin"

        private const val ASSET_PACK_NAME = "gemma_model_pack"

        private const val DEVICE_TMP_MODEL_PATH =
            "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin"

        private const val MAX_MODEL_TOKENS = 1024
        private const val MAX_MODEL_TOP_K = 40
        private const val MAX_RESPONSE_WORDS = 90

        private const val MIN_FREE_BYTES_FOR_MODEL_DOWNLOAD = 2_500_000_000L
        private const val MIN_RECOMMENDED_RAM_BYTES = 4_000_000_000L
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val assetPackManager: AssetPackManager by lazy {
        AssetPackManagerFactory.getInstance(context)
    }

    private val assetPackListener = AssetPackStateUpdateListener { state ->
        handleAssetPackState(state)
    }

    private var isAssetPackListenerRegistered = false
    private var gameContext: GameContext? = null
    private var llmInference: LlmInference? = null

    fun setGameContext(context: GameContext) {
        gameContext = context
    }

    fun initialize() {
        registerAssetPackListener()

        viewModelScope.launch {
            if (llmInference != null) {
                _uiState.update {
                    it.copy(
                        useFallback = false,
                        isModelLoaded = true,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_READY,
                        modelError = null,
                    )
                }
                return@launch
            }

            val modelFile = findModelFile()

            if (modelFile != null) {
                loadModel(modelFile)
            } else {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_NOT_DOWNLOADED,
                        modelDownloadProgress = 0f,
                        downloadProgress = 0f,
                        modelWarning = buildDeviceCompatibilityWarning(),
                        modelError = null,
                    )
                }
            }
        }
    }

    /**
     * Step 8:
     * Called from the UI when the user taps "Download AI model".
     */
    fun downloadAiModel() {
        registerAssetPackListener()

        viewModelScope.launch {
            val existingModelFile = findModelFile()

            if (existingModelFile != null) {
                loadModel(existingModelFile)
                return@launch
            }

            val freeStorageOk = hasEnoughFreeStorageForModel()

            if (!freeStorageOk) {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_DOWNLOAD_FAILED,
                        modelError = "Not enough free storage for the AI model. Free at least 2.5 GB and try again.",
                        modelWarning = buildDeviceCompatibilityWarning(),
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    useFallback = true,
                    isModelLoaded = false,
                    isModelDownloading = true,
                    downloadProgress = 0f,
                    modelDownloadProgress = 0f,
                    modelStatus = AiModelStatus.MODEL_DOWNLOADING,
                    modelError = null,
                    modelWarning = buildDeviceCompatibilityWarning(),
                )
            }

            try {
                assetPackManager
                    .fetch(listOf(ASSET_PACK_NAME))
                    .addOnFailureListener { throwable ->
                        _uiState.update {
                            it.copy(
                                useFallback = true,
                                isModelLoaded = false,
                                isModelDownloading = false,
                                modelStatus = AiModelStatus.MODEL_DOWNLOAD_FAILED,
                                modelError = "AI model download failed: ${throwable.message ?: throwable::class.java.simpleName}",
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_DOWNLOAD_FAILED,
                        modelError = "AI model download failed: ${e.message ?: e::class.java.simpleName}",
                    )
                }
            }
        }
    }

    private fun registerAssetPackListener() {
        if (!isAssetPackListenerRegistered) {
            assetPackManager.registerListener(assetPackListener)
            isAssetPackListenerRegistered = true
        }
    }

    private fun handleAssetPackState(state: AssetPackState) {
        if (state.name() != ASSET_PACK_NAME) {
            return
        }

        val progress = calculateDownloadProgress(
            downloadedBytes = state.bytesDownloaded(),
            totalBytes = state.totalBytesToDownload(),
        )

        when (state.status()) {
            AssetPackStatus.PENDING -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelDownloading = true,
                        modelStatus = AiModelStatus.MODEL_DOWNLOADING,
                        modelDownloadProgress = progress,
                        downloadProgress = progress,
                        modelError = null,
                    )
                }
            }

            AssetPackStatus.DOWNLOADING -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelDownloading = true,
                        modelStatus = AiModelStatus.MODEL_DOWNLOADING,
                        modelDownloadProgress = progress,
                        downloadProgress = progress,
                        modelError = null,
                    )
                }
            }

            AssetPackStatus.TRANSFERRING -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelDownloading = true,
                        modelStatus = AiModelStatus.MODEL_DOWNLOADING,
                        modelDownloadProgress = 1f,
                        downloadProgress = 1f,
                        modelError = null,
                    )
                }
            }

            AssetPackStatus.WAITING_FOR_WIFI -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_DOWNLOAD_WAITING_FOR_WIFI,
                        modelDownloadProgress = progress,
                        downloadProgress = progress,
                        modelError = "AI model download is waiting for Wi-Fi.",
                    )
                }
            }

            AssetPackStatus.COMPLETED -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_LOADING,
                        modelDownloadProgress = 1f,
                        downloadProgress = 1f,
                        modelError = null,
                    )
                }

                viewModelScope.launch {
                    val modelFile = findModelInAssetPack()

                    if (modelFile == null) {
                        _uiState.update {
                            it.copy(
                                useFallback = true,
                                isModelLoaded = false,
                                isModelDownloading = false,
                                modelStatus = AiModelStatus.MODEL_FAILED,
                                modelError = "AI model asset pack was downloaded, but $MODEL_FILE_NAME was not found.",
                            )
                        }
                    } else {
                        loadModel(modelFile)
                    }
                }
            }

            AssetPackStatus.FAILED -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_DOWNLOAD_FAILED,
                        modelError = "AI model download failed. Error code: ${state.errorCode()}",
                    )
                }
            }

            AssetPackStatus.CANCELED -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_DOWNLOAD_FAILED,
                        modelError = "AI model download was canceled.",
                    )
                }
            }

            AssetPackStatus.NOT_INSTALLED -> {
                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        isModelDownloading = false,
                        modelStatus = AiModelStatus.MODEL_NOT_DOWNLOADED,
                        modelDownloadProgress = 0f,
                        downloadProgress = 0f,
                        modelError = null,
                    )
                }
            }
        }
    }

    private fun calculateDownloadProgress(
        downloadedBytes: Long,
        totalBytes: Long,
    ): Float {
        if (totalBytes <= 0L) {
            return 0f
        }

        return (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Step 9:
     * Production loading order:
     * 1. Internal app model path, if you later copy/download manually.
     * 2. Play Asset Delivery asset pack path.
     * 3. Debug-only /data/local/tmp path.
     */
    private fun findModelFile(): File? {
        val internalModelFile = File(
            context.filesDir,
            "models/$MODEL_FILE_NAME",
        )

        if (internalModelFile.exists() && internalModelFile.canRead()) {
            return internalModelFile
        }

        val assetPackModelFile = findModelInAssetPack()

        if (assetPackModelFile != null) {
            return assetPackModelFile
        }

        if (BuildConfig.DEBUG) {
            val debugModelFile = File(DEVICE_TMP_MODEL_PATH)

            if (debugModelFile.exists() && debugModelFile.canRead()) {
                return debugModelFile
            }
        }

        return null
    }

    private fun findModelInAssetPack(): File? {
        val location = assetPackManager.getPackLocation(ASSET_PACK_NAME)
            ?: return null

        val assetsPath = location.assetsPath()
            ?: return null

        val modelFile = File(assetsPath, MODEL_FILE_NAME)

        if (modelFile.exists() && modelFile.canRead()) {
            return modelFile
        }

        return null
    }

    private suspend fun loadModel(modelFile: File) {
        _uiState.update {
            it.copy(
                useFallback = true,
                isModelLoaded = false,
                isModelDownloading = false,
                modelStatus = AiModelStatus.MODEL_LOADING,
                modelError = null,
            )
        }

        try {
            withContext(Dispatchers.IO) {
                val taskOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(MAX_MODEL_TOKENS)
                    .setMaxTopK(MAX_MODEL_TOP_K)
                    .build()

                llmInference = LlmInference.createFromOptions(
                    context,
                    taskOptions,
                )
            }

            _uiState.update {
                it.copy(
                    useFallback = false,
                    isModelLoaded = true,
                    isModelDownloading = false,
                    modelStatus = AiModelStatus.MODEL_READY,
                    modelDownloadProgress = 1f,
                    downloadProgress = 1f,
                    modelError = null,
                )
            }
        } catch (e: Exception) {
            llmInference = null

            _uiState.update {
                it.copy(
                    useFallback = true,
                    isModelLoaded = false,
                    isModelDownloading = false,
                    modelStatus = AiModelStatus.MODEL_FAILED,
                    modelError = "Could not load AI model: ${e.message ?: e::class.java.simpleName}",
                    modelWarning = buildDeviceCompatibilityWarning(),
                )
            }
        }
    }

    /**
     * Step 10:
     * Compatibility strategy.
     *
     * We do not completely block AI mode for weak devices because some devices
     * may still run the model slowly. Instead, we warn and let model loading fail
     * safely if MediaPipe cannot initialize it.
     */
    private fun buildDeviceCompatibilityWarning(): String? {
        val totalRam = getTotalRamBytes()

        return when {
            totalRam != null && totalRam < MIN_RECOMMENDED_RAM_BYTES -> {
                "AI mode may be slow or unavailable on this device. Smart mode will remain available."
            }

            else -> null
        }
    }

    private fun getTotalRamBytes(): Long? {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            memoryInfo.totalMem
        } catch (_: Exception) {
            null
        }
    }

    private fun hasEnoughFreeStorageForModel(): Boolean {
        return try {
            val statFs = StatFs(context.filesDir.absolutePath)
            statFs.availableBytes >= MIN_FREE_BYTES_FOR_MODEL_DOWNLOAD
        } catch (_: Exception) {
            true
        }
    }

    fun onInputChanged(input: String) {
        _uiState.update {
            it.copy(currentInput = input)
        }
    }

    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()

        if (input.isEmpty() || _uiState.value.isGenerating) {
            return
        }

        val userMessage = ChatMessage(
            text = input,
            isUser = true,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                currentInput = "",
                isGenerating = true,
            )
        }

        viewModelScope.launch {
            val response = generateAssistantResponse(input)

            val assistantMessage = ChatMessage(
                text = response,
                isUser = false,
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + assistantMessage,
                    isGenerating = false,
                )
            }
        }
    }

    /**
     * Step 11:
     * Game-state questions use deterministic smart mode.
     * Conceptual Git questions use Gemma when AI mode is ready.
     */
    private suspend fun generateAssistantResponse(userInput: String): String {
        if (isProbablyNonEnglish(userInput)) {
            return buildEnglishOnlyResponse()
        }

        val input = normalizeUserInput(userInput)

        val localResponse = generateLocalResponse(input)
        if (localResponse != null) {
            return localResponse
        }

        val ctx = gameContext
            ?: return "I need to be connected to a level to help you. Open a level first."

        val gameStateResponse = generateGameStateResponse(input, ctx)
        if (gameStateResponse != null) {
            return gameStateResponse
        }

        if (shouldAskGemma(input)) {
            val state = _uiState.value

            return when (state.modelStatus) {
                AiModelStatus.MODEL_READY -> {
                    if (llmInference == null) {
                        buildAiModelUnavailableResponse()
                    } else {
                        generateLlmResponse(userInput)
                    }
                }

                AiModelStatus.MODEL_DOWNLOADING -> {
                    val percent = (state.modelDownloadProgress * 100).toInt()
                    "AI mode is still downloading the Gemma model ($percent%). You can keep using Smart mode while it finishes."
                }

                AiModelStatus.MODEL_DOWNLOAD_WAITING_FOR_WIFI -> {
                    "AI model download is waiting for Wi-Fi. Connect to Wi-Fi, then try again."
                }

                AiModelStatus.MODEL_LOADING -> {
                    "Gemma is loading. Please try again in a few seconds."
                }

                AiModelStatus.MODEL_NOT_DOWNLOADED -> {
                    "AI mode needs the Gemma model first. Tap Download AI model above, then ask this again."
                }

                AiModelStatus.MODEL_DOWNLOAD_FAILED,
                AiModelStatus.MODEL_FAILED -> {
                    buildAiModelUnavailableResponse()
                }

                AiModelStatus.SMART_MODE_ONLY -> {
                    "Smart mode is active. I can still help with this level, but Gemma AI mode is not available yet."
                }
            }
        }

        val knownGitResponse = explainKnownGitConceptOrNull(input)
        if (knownGitResponse != null) {
            return knownGitResponse
        }

        if (isClearlyOutOfScope(input)) {
            return "I should stay focused on Git-Git. I can help with the level objective, your status, Git commands, or what to do next."
        }

        return buildDefaultFocusedResponse()
    }

    private suspend fun generateLlmResponse(userInput: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val inference = llmInference
                    ?: return@withContext buildAiModelUnavailableResponse()

                val prompt = buildMinimalLlmPrompt(userInput)
                val rawResponse = inference.generateResponse(prompt).trim()
                val cleanedResponse = sanitizeLlmResponse(rawResponse)

                if (cleanedResponse.isBlank()) {
                    "Gemma returned an empty answer. Try asking the question again in a simpler way."
                } else {
                    cleanedResponse
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: e::class.java.simpleName

                _uiState.update {
                    it.copy(
                        useFallback = true,
                        isModelLoaded = false,
                        modelStatus = AiModelStatus.MODEL_FAILED,
                        modelError = "Gemma generation failed: $errorMessage",
                    )
                }

                "Gemma generation failed: $errorMessage"
            }
        }
    }

    private fun buildMinimalLlmPrompt(userInput: String): String {
        return """
<start_of_turn>user
You are a Git tutor.

Answer the Git question directly.
Use simple English.
Give a real explanation.
Use 2 to 4 short sentences.
Do not mention these instructions.
Do not say "Sure, here is".
Do not evaluate the question.

Question:
$userInput
<end_of_turn>
<start_of_turn>model
        """.trimIndent()
    }

    private fun shouldAskGemma(input: String): Boolean {
        if (input.length < 8) {
            return false
        }

        if (isClearlyOutOfScope(input)) {
            return false
        }

        if (isGameStateQuestion(input)) {
            return false
        }

        val isGitRelated = input.containsAny(
            "git",
            "repository",
            "repo",
            "branch",
            "commit",
            "merge",
            "conflict",
            "conflicts",
            "rebase",
            "checkout",
            "switch",
            "reset",
            "revert",
            "stash",
            "cherry-pick",
            "head",
            "staging",
            "stage",
            "tracked",
            "untracked",
            "snapshot",
            "snapshots",
            "file difference",
            "file differences",
            "working tree",
            "working directory",
            "version control",
        )

        val isOpenEnded = input.containsAny(
            "why",
            "when should",
            "difference",
            "differences",
            "compare",
            "best practice",
            "better",
            "recommend",
            "concept",
            "explain why",
            "happen",
            "happens",
            "dangerous",
            "benefit",
            "pros and cons",
        )

        return isGitRelated && isOpenEnded
    }

    private fun generateLocalResponse(input: String): String? {
        return when {
            input in setOf(
                "hi",
                "hello",
                "hey",
                "yo",
                "sup",
                "good morning",
                "good afternoon",
                "good evening",
            ) -> {
                "Hi! I'm your Git assistant. I can help you understand this level, explain Git commands, check your status, or guide you if you're stuck."
            }

            input in setOf(
                "thanks",
                "thank you",
                "ty",
                "thx",
                "thank you very much",
            ) -> {
                "You're welcome. Ask me if you need help with the next Git step."
            }

            input in setOf(
                "ok",
                "okay",
                "cool",
                "nice",
                "got it",
                "understood",
            ) -> {
                "Got it. I’m here if you want help with the objective, your status, or a Git command."
            }

            input.containsAny(
                "your name",
                "who are you",
                "what are you",
            ) -> {
                "I'm your Git assistant inside Git-Git. My job is to help you understand the current level, Git commands, and what to do next."
            }

            input in setOf(
                "talk with you",
                "talk to you",
                "chat with you",
                "can we talk",
                "i want to talk",
            ) -> {
                "Sure. I can talk with you, but I’ll stay focused on helping you with this Git level. Ask me about the objective, your status, or any Git command."
            }

            input.containsAny(
                "what can you do",
                "how can you help",
                "help me with what",
            ) -> {
                "I can explain the current objective, check your game status, explain Git commands, and guide you step by step if you're stuck."
            }

            else -> null
        }
    }

    private fun generateGameStateResponse(
        input: String,
        ctx: GameContext,
    ): String? {
        return when {
            input.containsAny(
                "stuck",
                "help",
                "what do i do",
                "what should i do",
                "next step",
                "next command",
                "don't know",
                "no idea",
            ) -> {
                generateStuckHelp(ctx)
            }

            input.containsAny(
                "objective",
                "goal",
                "mission",
                "what should i complete",
            ) -> {
                buildObjectiveResponse(ctx)
            }

            input.containsAny(
                "status",
                "where am i",
                "current state",
                "my state",
                "what happened",
            ) -> {
                buildCurrentStatusResponse(ctx)
            }

            input.containsAny(
                "story",
                "scenario",
                "briefing",
            ) -> {
                "Story:\n${ctx.levelBriefing}\n\nObjective:\n${ctx.levelObjective}"
            }

            input.containsAny(
                "available commands",
                "commands can i use",
                "what commands",
                "allowed commands",
            ) -> {
                "Available commands for this level:\n${ctx.availableCommands.joinToString("\n") { "• $it" }}"
            }

            input.containsAny(
                "readme",
                "read me",
                "readme file",
            ) && !shouldAskGemma(input) -> {
                buildReadmeResponse(ctx)
            }

            isFilesStatusQuestion(input) -> {
                buildFilesResponse(ctx)
            }

            else -> null
        }
    }

    private fun buildObjectiveResponse(ctx: GameContext): String {
        return buildString {
            appendLine("Your objective:")
            appendLine(ctx.levelObjective)

            if (ctx.isLevelCompleted) {
                appendLine()
                append("All goal conditions are currently met.")
            } else if (ctx.goalFailures.isNotEmpty()) {
                appendLine()
                appendLine("Still needed:")
                ctx.goalFailures.forEach { failure ->
                    appendLine("• $failure")
                }
            }
        }.trim()
    }

    private fun buildCurrentStatusResponse(ctx: GameContext): String {
        return buildString {
            appendLine("Current state:")
            appendLine("• Branch: ${ctx.currentBranch ?: "detached HEAD"}")
            appendLine("• Commits: ${ctx.commitCount}")
            appendLine("• Branches: ${ctx.branchNames.joinToString(", ")}")
            appendLine("• Staged: ${ctx.stagedFiles.ifEmpty { listOf("nothing") }.joinToString(", ")}")
            appendLine("• Files: ${ctx.workingFiles.ifEmpty { listOf("none") }.joinToString(", ")}")

            if (ctx.goalFailures.isNotEmpty()) {
                appendLine()
                appendLine("Still needed:")
                ctx.goalFailures.forEach { failure ->
                    appendLine("• $failure")
                }
            }
        }.trim()
    }

    private fun buildReadmeResponse(ctx: GameContext): String {
        val hasReadme = ctx.workingFiles.any { file ->
            file.equals("README.md", ignoreCase = true) ||
                    file.equals("README", ignoreCase = true) ||
                    file.contains("readme", ignoreCase = true)
        }

        return buildString {
            appendLine("A README file explains what a project is about.")
            appendLine()
            appendLine("It usually includes:")
            appendLine("• Project description")
            appendLine("• How to install or run it")
            appendLine("• Basic usage instructions")
            appendLine("• Notes for other developers")

            appendLine()

            if (hasReadme) {
                appendLine("In this level, the README is one of your project files.")
                appendLine("If you need to save it in Git, stage it with `git add README.md`, then commit it.")
            } else {
                appendLine("I do not currently see a README file in this level's working files.")
            }
        }.trim()
    }

    private fun buildFilesResponse(ctx: GameContext): String {
        return buildString {
            appendLine("Current files:")
            appendLine(
                ctx.workingFiles
                    .ifEmpty { listOf("none") }
                    .joinToString("\n") { "• $it" },
            )

            appendLine()
            appendLine("Staged files:")
            appendLine(
                ctx.stagedFiles
                    .ifEmpty { listOf("nothing") }
                    .joinToString("\n") { "• $it" },
            )

            appendLine()
            appendLine("Files must be staged with `git add <file>` before they can be saved in a commit.")
        }.trim()
    }

    private fun generateStuckHelp(ctx: GameContext): String {
        if (ctx.isLevelCompleted) {
            return "It looks like you've met all the goals. The level should be complete now."
        }

        if (ctx.goalFailures.isEmpty()) {
            return "I don't see recorded unmet goals right now. Check the objective and your current status."
        }

        val hints = ctx.goalFailures.map { failure ->
            when {
                failure.contains("Expected") && failure.contains("commits") -> {
                    val needed =
                        Regex("Expected (\\d+)")
                            .find(failure)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull()

                    if (needed != null) {
                        "You need $needed commit(s), but you currently have ${ctx.commitCount}. Stage a file with `git add`, then commit with `git commit -m \"message\"`."
                    } else {
                        "You still need more commits."
                    }
                }

                failure.contains("Missing required commit") -> {
                    val message =
                        Regex("\"(.+?)\"")
                            .find(failure)
                            ?.groupValues
                            ?.get(1)

                    if (message != null) {
                        "You need a commit with the exact message \"$message\"."
                    } else {
                        "You are missing a required commit message."
                    }
                }

                failure.contains("HEAD should be on branch") -> {
                    val branch =
                        Regex("'(.+?)'")
                            .find(failure)
                            ?.groupValues
                            ?.get(1)

                    if (branch != null) {
                        "You need to be on the `$branch` branch. Use `git checkout $branch`."
                    } else {
                        "You need to switch to the required branch."
                    }
                }

                failure.contains("merge commit is required") -> {
                    "This level requires a merge. Create commits on separate branches, then merge with `git merge <branch>`."
                }

                failure.contains("should be deleted") -> {
                    val branch =
                        Regex("'(.+?)'")
                            .find(failure)
                            ?.groupValues
                            ?.get(1)

                    if (branch != null) {
                        "You need to delete the `$branch` branch with `git branch -d $branch`."
                    } else {
                        "You still need to delete a branch."
                    }
                }

                else -> {
                    "Goal not yet met: $failure"
                }
            }
        }

        return "Here's what you still need:\n\n${hints.joinToString("\n\n") { "→ $it" }}"
    }

    private fun explainKnownGitConceptOrNull(input: String): String? {
        val explanations = mapOf(
            "git" to "Git is a version control system. It tracks changes in your project so you can save snapshots, compare work, and move between versions.",
            "init" to "git init creates a new Git repository. It starts version control for the current project.",
            "add" to "git add stages files for the next commit. Use `git add <file>` for one file or `git add .` for all files.",
            "status" to "git status shows what changed, what is staged, and what is untracked.",
            "log" to "git log shows commit history from newest to oldest.",
            "checkout" to "git checkout switches branches or commits. Example: `git checkout main`.",
            "switch" to "git switch is the modern command for switching branches.",
            "reflog" to "git reflog shows recent HEAD movements and can help recover lost commits.",
        )

        for ((key, explanation) in explanations) {
            if (input.contains(key)) {
                return explanation
            }
        }

        return null
    }

    private fun sanitizeLlmResponse(response: String): String {
        val cleaned = response
            .replace("<start_of_turn>model", "")
            .replace("<start_of_turn>user", "")
            .replace("<end_of_turn>", "")
            .replace("```", "")
            .trim()
            .removePrefix("Sure, here is a concise answer:")
            .removePrefix("Sure, here is a simple answer:")
            .removePrefix("Sure, here is")
            .removePrefix("Sure,")
            .removePrefix("1.")
            .trim()

        return limitWords(cleaned, MAX_RESPONSE_WORDS)
    }

    private fun limitWords(text: String, maxWords: Int): String {
        val trimmed = text.trim()

        if (trimmed.isBlank()) {
            return trimmed
        }

        val words = trimmed.split(Regex("\\s+"))

        if (words.size <= maxWords) {
            return trimmed
        }

        return words
            .take(maxWords)
            .joinToString(" ")
            .trim() + "..."
    }

    private fun isProbablyNonEnglish(input: String): Boolean {
        val nonEnglishScriptRegex = Regex(
            pattern =
                "[\\u0590-\\u05FF" +
                        "\\u0600-\\u06FF" +
                        "\\u0400-\\u04FF" +
                        "\\u0370-\\u03FF" +
                        "\\u4E00-\\u9FFF" +
                        "\\u3040-\\u30FF" +
                        "\\uAC00-\\uD7AF" +
                        "\\u00C0-\\u024F]",
        )

        return nonEnglishScriptRegex.containsMatchIn(input)
    }

    private fun buildEnglishOnlyResponse(): String {
        return "I can currently help only in English. Please write your question in English."
    }

    private fun isGameStateQuestion(input: String): Boolean {
        return input.containsAny(
            "status",
            "where am i",
            "current state",
            "my state",
            "objective",
            "goal",
            "mission",
            "stuck",
            "what do i do",
            "next step",
            "next command",
            "available commands",
            "story",
            "briefing",
        )
    }

    private fun isFilesStatusQuestion(input: String): Boolean {
        return input.containsAny(
            "current files",
            "what files",
            "which files",
            "show files",
            "working files",
            "working file",
            "staged files",
            "what is staged",
            "which files are staged",
            "working directory files",
            "files in this level",
        )
    }

    private fun isClearlyOutOfScope(input: String): Boolean {
        val outOfScopeTerms = listOf(
            "movie",
            "music",
            "food",
            "weather",
            "relationship",
            "football",
            "basketball",
            "instagram",
            "tiktok",
        )

        return outOfScopeTerms.any { term ->
            input.contains(term)
        }
    }

    private fun normalizeUserInput(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace(Regex("[\\s.!?]+$"), "")
    }

    private fun String.containsAny(vararg terms: String): Boolean {
        return terms.any { term -> contains(term) }
    }

    private fun buildDefaultFocusedResponse(): String {
        return "I can help with this Git level. Ask me about the objective, your status, a Git command, or say \"I'm stuck\" for guidance."
    }

    private fun buildAiModelUnavailableResponse(): String {
        return "AI mode is not ready on this device. Smart mode is still available for level help, status, objectives, and Git commands."
    }

    fun submitAiResponseReport(
        message: ChatMessage,
        reason: AiReportReason,
        userFeedback: String,
    ) {
        val ctx = gameContext

        val reportData = hashMapOf(
            "messageText" to message.text.take(2_000),
            "reason" to reason.name,
            "reasonLabel" to reason.label,
            "userFeedback" to userFeedback.trim().take(1_000),
            "levelTitle" to (ctx?.levelTitle ?: ""),
            "levelObjective" to (ctx?.levelObjective ?: ""),
            "currentBranch" to (ctx?.currentBranch ?: ""),
            "commitCount" to (ctx?.commitCount ?: 0),
            "isLevelCompleted" to (ctx?.isLevelCompleted ?: false),
            "appVersionName" to BuildConfig.VERSION_NAME,
            "appVersionCode" to BuildConfig.VERSION_CODE,
            "createdAt" to FieldValue.serverTimestamp(),
        )

        firestore
            .collection("ai_response_reports")
            .add(reportData)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            text = "Thanks. This AI response was reported.",
                            isUser = false,
                        ),
                    )
                }
            }
            .addOnFailureListener { error ->
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            text = "Could not submit the report right now. Please try again later.",
                            isUser = false,
                        ),
                        modelError = "AI report submission failed: ${error.message}",
                    )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()

        if (isAssetPackListenerRegistered) {
            assetPackManager.unregisterListener(assetPackListener)
            isAssetPackListenerRegistered = false
        }
    }
}