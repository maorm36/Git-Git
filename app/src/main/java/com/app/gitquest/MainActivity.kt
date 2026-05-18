package com.app.gitquest

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.gitquest.ui.game.GameScreen
import com.app.gitquest.ui.githistory.GitHistoryScreen
import com.app.gitquest.ui.onboarding.OnboardingScreen
import com.app.gitquest.ui.theme.GitQuestTheme
import com.app.gitquest.ui.worldmap.WorldMapScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("gitquest_prefs", Context.MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)

        setContent {
            GitQuestTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    var levelStars by remember {
                        mutableStateOf(
                            loadLevelStars(
                                context = this@MainActivity,
                            )
                        )
                    }

                    fun recordLevelScore(
                        completedLevelId: String,
                        starsEarned: Int,
                    ) {
                        val safeStars = starsEarned.coerceIn(0, 3)

                        if (safeStars <= 0) {
                            return
                        }

                        val previousStars = levelStars[completedLevelId] ?: 0
                        val bestStars = maxOf(previousStars, safeStars)

                        if (bestStars == previousStars) {
                            return
                        }

                        levelStars = levelStars + (completedLevelId to bestStars)

                        saveLevelStars(
                            context = this@MainActivity,
                            levelStars = levelStars,
                        )
                    }

                    val startDestination = if (hasSeenOnboarding) {
                        "worldmap"
                    } else {
                        "onboarding"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinished = {
                                    prefs.edit()
                                        .putBoolean("has_seen_onboarding", true)
                                        .apply()

                                    navController.navigate("worldmap") {
                                        popUpTo("onboarding") {
                                            inclusive = true
                                        }
                                    }
                                },
                            )
                        }

                        composable("worldmap") {
                            WorldMapScreen(
                                onLevelSelected = { levelId ->
                                    navController.navigate("game/$levelId")
                                },
                                onGitHistoryClick = {
                                    navController.navigate("git_history")
                                },
                                levelStars = levelStars,
                            )
                        }

                        composable("game/{levelId}") { backStackEntry ->
                            val levelId = backStackEntry.arguments
                                ?.getString("levelId")
                                ?: return@composable

                            GameScreen(
                                levelId = levelId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onLevelCompleted = { completedLevelId, starsEarned ->
                                    recordLevelScore(
                                        completedLevelId = completedLevelId,
                                        starsEarned = starsEarned,
                                    )
                                },
                                onNextLevel = { nextId, starsEarned ->
                                    recordLevelScore(
                                        completedLevelId = levelId,
                                        starsEarned = starsEarned,
                                    )

                                    navController.navigate("game/$nextId") {
                                        popUpTo("game/$levelId") {
                                            inclusive = true
                                        }
                                    }
                                },
                            )
                        }

                        composable("git_history") {
                            GitHistoryScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun loadLevelStars(
    context: Context,
): Map<String, Int> {
    val prefs = context.getSharedPreferences(
        "gitquest_prefs",
        Context.MODE_PRIVATE,
    )

    val rawValue = prefs.getString("level_stars", null)
        ?: return emptyMap()

    return rawValue
        .split("|")
        .mapNotNull { entry ->
            val parts = entry.split(":")

            if (parts.size != 2) {
                return@mapNotNull null
            }

            val levelId = parts[0]

            val stars = parts[1]
                .toIntOrNull()
                ?.coerceIn(0, 3)
                ?: return@mapNotNull null

            levelId to stars
        }
        .toMap()
}

private fun saveLevelStars(
    context: Context,
    levelStars: Map<String, Int>,
) {
    val prefs = context.getSharedPreferences(
        "gitquest_prefs",
        Context.MODE_PRIVATE,
    )

    val rawValue = levelStars.entries
        .joinToString(separator = "|") { (levelId, stars) ->
            "$levelId:${stars.coerceIn(0, 3)}"
        }

    prefs.edit()
        .putString("level_stars", rawValue)
        .apply()
}