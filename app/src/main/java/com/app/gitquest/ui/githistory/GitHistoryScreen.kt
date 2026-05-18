package com.app.gitquest.ui.githistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgDark = Color(0xFF1E1E2E)
private val CardBg = Color(0xFF282A36)
private val Green = Color(0xFF50FA7B)
private val Blue = Color(0xFF8BE9FD)
private val Yellow = Color(0xFFF1FA8C)
private val White = Color(0xFFF8F8F2)
private val Muted = Color(0xFF6272A4)
private val MonoFont = FontFamily.Monospace

@Composable
fun GitHistoryScreen(
    onNavigateBack: () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("< Back", color = Muted, fontFamily = MonoFont, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "What is Git?",
                color = Green,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(
                title = "The problem",
                content = "Before Git, developers struggled to track changes in their code.\n" +
                        "Collaborating on the same codebase was chaotic — overwriting each other's work, losing changes, and having no way to go back to a previous version were everyday problems.\n" +
                        "Early version control systems like CVS and SVN existed, but they were slow, centralized, and required constant server access.",
            )

            SectionCard(
                title = "The origin",
                content = "In 2005, Linus Torvalds, the creator of Linux\n" +
                        "built Git out of frustration.\n" +
                        "The Linux kernel team needed a version control system that was fast, distributed, and could handle thousands of contributors working in parallel.\n" +
                        "Linus designed Git in just two weeks.\n" +
                        "It was radical: every developer gets a full copy of the entire project history on their own machine.\n" +
                        "No central server required.",
            )

            SectionCard(
                title = "How it works",
                content = "Git tracks your project as a series of snapshots.\n" +
                        "Every time you \"commit\", Git takes a picture of all your files at that moment and stores a reference to it.\n" +
                        "These snapshots form a chain — a history of your project over time.\n" +
                        "You can branch off to experiment, merge changes together, and travel back in time to any point in your project's history.\n" +
                        "It's like a save system for your code, but infinitely more powerful.",
            )

            SectionCard(
                title = "Key concepts",
                content = "Repository (repo): A folder tracked by Git.\n\nCommit: A saved snapshot of your files at a point in time.\n\nBranch: A parallel line of development.\nThe default branch is called \"main\".\n\nMerge: Combining changes from one branch into another.\n\nStaging area: A preparation zone where you choose which changes go into the next commit.\n\nHEAD: A pointer that tells Git which commit you're currently looking at.",
            )

            SectionCard(
                title = "Why it matters today",
                content = "Git is used by over 100 million developers worldwide.\n" +
                        "Every major tech company — Google, Meta, Microsoft, Amazon — relies on Git.\n" +
                        "Platforms like GitHub, GitLab, and Bitbucket are built entirely around it.\n" +
                        "Whether you're writing code alone or collaborating with a team of thousands, Git is the foundation of modern software development.\n" +
                        "Understanding Git isn't just useful — it's a requirement for any developer career.",
            )

            SectionCard(
                title = "Fun facts",
                content = "The name \"Git\" is British slang for an unpleasant person. Linus named it as a joke.\n\nThe Linux kernel repository has over 1 million commits from more than 20,000 contributors.\n\nGit can handle repositories with millions of files and decades of history without breaking a sweat.",
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = Blue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            color = White,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
    }
}

@Preview(
    name = "Git History Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 2000
)
@Composable
private fun GitHistoryScreenPreview() {
    GitHistoryScreen(
        onNavigateBack = {}
    )
}