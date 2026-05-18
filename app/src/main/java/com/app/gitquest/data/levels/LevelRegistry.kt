package com.app.gitquest.data.levels

/**
 * Central registry of all the world levels.
 * Each world provides its own list, and this object indexes them for fast lookup by ID.
 */
object LevelRegistry {

    data class WorldDef(
        val id: String,
        val title: String,
        val description: String,
        val levels: List<LevelDef>,
    )

    val worlds: List<WorldDef> = listOf(
        WorldDef(
            id = "world1",
            title = "First Steps",
            description = "Learn the basics: init, add, commit, status, log",
            levels = World1Levels.LEVELS,
        ),
        WorldDef(
            id = "world2",
            title = "Branching Out",
            description = "Create branches, switch between them, and merge",
            levels = World2Levels.LEVELS,
        ),
        WorldDef(
            id = "world3",
            title = "Collaboration",
            description = "Merge conflicts, 3-way merge, and git stash",
            levels = World3Levels.LEVELS,
        ),
        WorldDef(
            id = "world4",
            title = "Rewriting History",
            description = "Reset, revert, cherry-pick, and rebase",
            levels = World4Levels.LEVELS,
        ),
        WorldDef(
            id = "world5",
            title = "Rescue Missions",
            description = "Reflog, detached HEAD recovery, and disaster repair",
            levels = World5Levels.LEVELS,
        ),
    )

    private val levelIndex: Map<String, LevelDef> by lazy {
        worlds.flatMap { it.levels }.associateBy { it.id }
    }

    private val levelOrder: List<String> by lazy {
        worlds.flatMap { world -> world.levels.map { it.id } }
    }

    fun getLevel(id: String): LevelDef? = levelIndex[id]

    fun getNextLevelId(currentId: String): String? {
        val index = levelOrder.indexOf(currentId)
        return if (index >= 0 && index < levelOrder.size - 1) {
            levelOrder[index + 1]
        } else null
    }

    fun getWorldForLevel(levelId: String): WorldDef? {
        return worlds.find { world -> world.levels.any { it.id == levelId } }
    }

    fun allLevelIds(): List<String> = levelOrder
}