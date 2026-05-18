package com.app.gitquest.ui.aichat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

private val BgDark = Color(0xFF1E1E2E)
private val CardBg = Color(0xFF282A36)
private val UserBubble = Color(0xFF44475A)
private val AiBubble = Color(0xFF2D3250)
private val Green = Color(0xFF50FA7B)
private val Blue = Color(0xFF8BE9FD)
private val Orange = Color(0xFFFFB86C)
private val Red = Color(0xFFFF5555)
private val White = Color(0xFFF8F8F2)
private val Muted = Color(0xFF6272A4)
private val MonoFont = FontFamily.Monospace

@Composable
fun AiChatScreen(
    gameContext: GameContext,
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var messageToReport by remember {
        mutableStateOf<ChatMessage?>(null)
    }

    LaunchedEffect(gameContext) {
        viewModel.setGameContext(gameContext)
        viewModel.initialize()
    }

    val density = LocalDensity.current

    val keyboardHeight = with(density) {
        WindowInsets.ime.getBottom(this).toDp()
    }

    val isKeyboardVisible = keyboardHeight > 0.dp
    val bottomPanelReservedHeight = 160.dp

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                ChatHeader(
                    levelTitle = gameContext.levelTitle,
                    modelStatus = state.modelStatus,
                    progress = state.modelDownloadProgress,
                    onBack = onNavigateBack,
                )

                AiModelPanel(
                    modelStatus = state.modelStatus,
                    progress = state.modelDownloadProgress,
                    modelError = state.modelError,
                    modelWarning = state.modelWarning,
                    onDownloadClick = { viewModel.downloadAiModel() },
                )

                ChatMessages(
                    messages = state.messages,
                    isGenerating = state.isGenerating,
                    bottomContentPadding = bottomPanelReservedHeight + keyboardHeight,
                    onReportMessage = { message ->
                        messageToReport = message
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(50f)
                    .offset(y = -keyboardHeight)
                    .fillMaxWidth()
                    .background(BgDark)
                    .then(
                        if (isKeyboardVisible) {
                            Modifier
                        } else {
                            Modifier.navigationBarsPadding()
                        },
                    ),
            ) {
                QuickActions(
                    onAction = { question ->
                        viewModel.onInputChanged(question)
                    },
                    enabled = !state.isGenerating,
                )

                ChatInput(
                    value = state.currentInput,
                    onValueChange = { viewModel.onInputChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    isGenerating = state.isGenerating,
                )
            }

            messageToReport?.let { message ->
                AiReportDialog(
                    message = message,
                    onDismiss = {
                        messageToReport = null
                    },
                    onSubmit = { reason, feedback ->
                        viewModel.submitAiResponseReport(
                            message = message,
                            reason = reason,
                            userFeedback = feedback,
                        )
                        messageToReport = null
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    levelTitle: String,
    modelStatus: AiModelStatus,
    progress: Float,
    onBack: () -> Unit,
) {
    val subtitle = when (modelStatus) {
        AiModelStatus.MODEL_READY -> "AI mode • $levelTitle"
        AiModelStatus.MODEL_LOADING -> "AI loading • $levelTitle"
        AiModelStatus.MODEL_DOWNLOADING -> {
            "Downloading AI ${(progress * 100).toInt()}% • $levelTitle"
        }

        AiModelStatus.MODEL_DOWNLOAD_WAITING_FOR_WIFI -> {
            "Waiting for Wi-Fi • $levelTitle"
        }

        AiModelStatus.MODEL_DOWNLOAD_FAILED,
        AiModelStatus.MODEL_FAILED -> {
            "Smart mode • AI unavailable • $levelTitle"
        }

        AiModelStatus.SMART_MODE_ONLY,
        AiModelStatus.MODEL_NOT_DOWNLOADED -> {
            "Smart mode • AI model needed • $levelTitle"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "<",
                color = Muted,
                fontSize = 20.sp,
                fontFamily = MonoFont,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Green.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "AI",
                color = Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Git Assistant",
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
            )

            Text(
                text = subtitle,
                color = Muted,
                fontSize = 11.sp,
                fontFamily = MonoFont,
            )
        }
    }
}

@Composable
private fun AiModelPanel(
    modelStatus: AiModelStatus,
    progress: Float,
    modelError: String?,
    modelWarning: String?,
    onDownloadClick: () -> Unit,
) {
    val shouldShowPanel = modelStatus != AiModelStatus.MODEL_READY

    if (!shouldShowPanel) {
        return
    }

    val title: String
    val body: String
    val showProgress: Boolean
    val buttonText: String?
    val accentColor: Color

    when (modelStatus) {
        AiModelStatus.MODEL_NOT_DOWNLOADED,
        AiModelStatus.SMART_MODE_ONLY -> {
            title = "Enable AI Mode"
            body = "Download the on-device Gemma model for broader Git explanations. The download is large, so Wi-Fi is recommended. Smart mode still works while AI mode is not ready."
            showProgress = false
            buttonText = "Download AI model"
            accentColor = Blue
        }

        AiModelStatus.MODEL_DOWNLOADING -> {
            title = "Downloading AI model"
            body = "Please keep the app installed while Google Play prepares the Gemma model."
            showProgress = true
            buttonText = null
            accentColor = Green
        }

        AiModelStatus.MODEL_DOWNLOAD_WAITING_FOR_WIFI -> {
            title = "Waiting for Wi-Fi"
            body = modelError ?: "The AI model download is waiting for Wi-Fi."
            showProgress = true
            buttonText = "Retry"
            accentColor = Orange
        }

        AiModelStatus.MODEL_LOADING -> {
            title = "Loading AI model"
            body = "Gemma is being loaded on this device. This can take a few seconds."
            showProgress = false
            buttonText = null
            accentColor = Green
        }

        AiModelStatus.MODEL_DOWNLOAD_FAILED -> {
            title = "AI model download failed"
            body = modelError ?: "The Gemma model could not be downloaded."
            showProgress = false
            buttonText = "Retry download"
            accentColor = Red
        }

        AiModelStatus.MODEL_FAILED -> {
            title = "AI model unavailable"
            body = modelError ?: "The Gemma model could not run on this device. Smart mode is still available."
            showProgress = false
            buttonText = "Try again"
            accentColor = Red
        }

        AiModelStatus.MODEL_READY -> {
            return
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg.copy(alpha = 0.65f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
        )

        Text(
            text = body,
            color = White,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        if (modelWarning != null) {
            Text(
                text = modelWarning,
                color = Orange,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }

        if (showProgress) {
            ProgressBar(
                progress = progress,
                accentColor = accentColor,
            )

            Text(
                text = "${(progress * 100).toInt()}%",
                color = Muted,
                fontSize = 11.sp,
                fontFamily = MonoFont,
            )
        }

        if (buttonText != null) {
            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.22f),
                    contentColor = White,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = buttonText,
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Muted.copy(alpha = 0.35f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(accentColor),
        )
    }
}

@Composable
private fun ChatMessages(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    bottomContentPadding: Dp = 0.dp,
    onReportMessage: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(
        messages.size,
        isGenerating,
        bottomContentPadding,
    ) {
        val lastIndex = when {
            isGenerating -> messages.size
            messages.isNotEmpty() -> messages.lastIndex
            else -> -1
        }

        if (lastIndex >= 0) {
            delay(80)
            listState.animateScrollToItem(lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = bottomContentPadding + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message ->
            ChatBubble(
                message = message,
                onReportMessage = onReportMessage,
            )
        }

        if (isGenerating) {
            item {
                TypingIndicator()
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onReportMessage: (ChatMessage) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
    ) {
        Column(
            horizontalAlignment = if (message.isUser) {
                Alignment.End
            } else {
                Alignment.Start
            },
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp,
                        ),
                    )
                    .background(
                        if (message.isUser) {
                            UserBubble
                        } else {
                            AiBubble
                        },
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = message.text,
                    color = White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }

            if (!message.isUser) {
                Text(
                    text = "Report",
                    color = Muted,
                    fontSize = 10.sp,
                    fontFamily = MonoFont,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 2.dp)
                        .clickable {
                            onReportMessage(message)
                        },
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")

    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "typingAlpha",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .background(AiBubble)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        Green.copy(
                            alpha = when (index) {
                                0 -> alpha
                                1 -> alpha * 0.7f
                                else -> alpha * 0.4f
                            },
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun QuickActions(
    onAction: (String) -> Unit,
    enabled: Boolean,
) {
    val actions = listOf(
        "I'm stuck",
        "What's the objective?",
        "What's my status?",
        "Tell me the story",
    )

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(50f)
            .background(BgDark)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (enabled) {
                            Blue.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .pointerInput(action, enabled) {
                        detectTapGestures(
                            onTap = {
                                if (enabled) {
                                    onAction(action)
                                }
                            },
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = action,
                    color = if (enabled) Blue else Muted,
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
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = !isGenerating,
            textStyle = TextStyle(
                color = White,
                fontSize = 14.sp,
            ),
            placeholder = {
                Text(
                    text = "Ask about Git or this level...",
                    color = Muted,
                    fontSize = 14.sp,
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    onSend()
                },
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Green,
            ),
        )

        IconButton(
            onClick = onSend,
            enabled = !isGenerating && value.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (!isGenerating && value.isNotBlank()) {
                    Green
                } else {
                    Muted
                },
            )
        }
    }
}

@Composable
private fun AiReportDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onSubmit: (AiReportReason, String) -> Unit,
) {
    var selectedReason by remember {
        mutableStateOf(AiReportReason.INCORRECT)
    }

    var feedback by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text(
                text = "Report AI response",
                color = White,
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Why are you reporting this response?",
                    color = White,
                    fontSize = 13.sp,
                )

                AiReportReason.values().forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedReason = reason
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = {
                                selectedReason = reason
                            },
                        )

                        Text(
                            text = reason.label,
                            color = White,
                            fontSize = 13.sp,
                        )
                    }
                }

                OutlinedTextField(
                    value = feedback,
                    onValueChange = {
                        feedback = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Optional details",
                            color = Muted,
                        )
                    },
                    textStyle = TextStyle(
                        color = White,
                        fontSize = 13.sp,
                    ),
                    maxLines = 3,
                )

                Text(
                    text = "Reported response preview:",
                    color = Muted,
                    fontSize = 11.sp,
                    fontFamily = MonoFont,
                )

                Text(
                    text = message.text.take(180),
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(selectedReason, feedback)
                },
            ) {
                Text(
                    text = "Submit",
                    color = Green,
                    fontFamily = MonoFont,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "Cancel",
                    color = Muted,
                    fontFamily = MonoFont,
                )
            }
        },
    )
}

@Preview(
    name = "AI Chat Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
)
@Composable
private fun AiChatScreenPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                ChatHeader(
                    levelTitle = "Branch Basics",
                    modelStatus = AiModelStatus.MODEL_NOT_DOWNLOADED,
                    progress = 0f,
                    onBack = {},
                )

                AiModelPanel(
                    modelStatus = AiModelStatus.MODEL_NOT_DOWNLOADED,
                    progress = 0f,
                    modelError = null,
                    modelWarning = "AI mode may be slow or unavailable on this device. Smart mode will remain available.",
                    onDownloadClick = {},
                )

                ChatMessages(
                    messages = listOf(
                        ChatMessage(
                            text = "Hi, I need help with this level.",
                            isUser = true,
                        ),
                        ChatMessage(
                            text = "Sure. Your goal is to create a new branch, switch to it, and make a commit.",
                            isUser = false,
                        ),
                    ),
                    isGenerating = false,
                    bottomContentPadding = 140.dp,
                    onReportMessage = {},
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(BgDark)
                    .navigationBarsPadding(),
            ) {
                QuickActions(
                    onAction = {},
                    enabled = true,
                )

                ChatInput(
                    value = "help",
                    onValueChange = {},
                    onSend = {},
                    isGenerating = false,
                )
            }
        }
    }
}