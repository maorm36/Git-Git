package com.app.gitquest.data.levels

import com.app.gitquest.engine.Branch
import com.app.gitquest.engine.Commit
import com.app.gitquest.engine.GitState
import com.app.gitquest.engine.Head
import com.app.gitquest.engine.LevelGoal

/**
 * Definition of a single game level structure in a game world.
 * Levels are defined as data rather than code - this makes them easy to add, test,
 * and eventually load from a file
 */
data class LevelDef(

    val id: String,

    val worldId: String,

    val title: String,

    val briefing: String,

    val objective: String,

    val initialState: GitState,

    val goal: LevelGoal,

    val availableCommands: List<String>,

    val hints: List<String>,

    val optimalCommandCount: Int,
)

/**
 * World 1: First Steps
 * Teaches: init, add, commit, status, log
 */
object World1Levels {

    val LEVELS: List<LevelDef> = listOf(
        LevelDef(
            id = "w1_l1",
            worldId = "world1",
            title = "Hello, Git!",
            briefing =
                "Every journey starts with a single step.\n" +
                        "In Git, that step is initializing a repository.\n\n" +
                        "A colleague just created a project folder but forgot to set up version control.",
            objective = "Initialize a Git repository and make your first commit.",
            initialState = GitState(
                workingDirectory = mapOf("README.md" to "# My Project"),
            ),
            goal = LevelGoal(
                commitCount = 1,
                headOnBranch = "main",
                requiredCommitMessages = listOf("Initial commit"),
            ),
            availableCommands = listOf("git init", "git add", "git commit"),
            hints = listOf(
                "Start by creating a repository with 'git init'",
                "Use 'git add' to stage your file before committing",
                "Use: git init → git add README.md → git commit -m \"Initial commit\"",
            ),
            optimalCommandCount = 3,
        ),

        LevelDef(
            id = "w1_l2",
            worldId = "world1",
            title = "Stage & Commit",
            briefing = "Your team lead left two files on your desk:\n" +
                    "a config file and a utility script.\n" +
                    "The repo is already initialized with one commit.\n" +
                    "Stage both files and commit them together.",
            objective = "Add both new files and create a single commit with the message \"Add config and utils\".",
            initialState = run {
                val initCommit = com.app.gitquest.engine.Commit(
                    id = "a1b2c3d",
                    message = "Initial commit",
                    parentIds = emptyList(),
                    files = mapOf("README.md" to "# Project"),
                )
                GitState(
                    isInitialized = true,
                    commits = mapOf("a1b2c3d" to initCommit),
                    branches = mapOf("main" to com.app.gitquest.engine.Branch("main", "a1b2c3d")),
                    head = com.app.gitquest.engine.Head.Attached("main"),
                    workingDirectory = mapOf(
                        "README.md" to "# Project",
                        "config.yml" to "debug: true",
                        "utils.kt" to "fun helper() {}",
                    ),
                )
            },
            goal = LevelGoal(
                commitCount = 2,
                headOnBranch = "main",
                requiredCommitMessages = listOf("Add config and utils"),
            ),
            availableCommands = listOf("git add", "git commit", "git status"),
            hints = listOf(
                "You need to stage BOTH files before committing",
                "Use 'git add .' to stage all changes at once, or add each file individually",
                "Use: git add . → git commit -m \"Add config and utils\"",
            ),
            optimalCommandCount = 2,
        ),

        LevelDef(
            id = "w1_l3",
            worldId = "world1",
            title = "Three in a Row",
            briefing = "Your project needs a series of improvements.\n" +
                    "Create three commits, each adding a new file.\n" +
                    "This teaches you that Git tracks history as a chain of snapshots.",
            objective = "Create 3 commits total (repo starts with 1). Each commit should add one new file.",
            initialState = run {
                val initCommit = com.app.gitquest.engine.Commit(
                    id = "abc1234",
                    message = "Initial commit",
                    parentIds = emptyList(),
                    files = mapOf("main.kt" to "fun main() {}"),
                )
                GitState(
                    isInitialized = true,
                    commits = mapOf("abc1234" to initCommit),
                    branches = mapOf("main" to com.app.gitquest.engine.Branch("main", "abc1234")),
                    head = com.app.gitquest.engine.Head.Attached("main"),
                    workingDirectory = mapOf(
                        "main.kt" to "fun main() {}",
                        "model.kt" to "data class User(val name: String)",
                        "view.kt" to "class HomeScreen",
                        "controller.kt" to "class AppController",
                    ),
                )
            },
            goal = LevelGoal(
                commitCount = 4, // 1 initial + 3 new
                headOnBranch = "main",
            ),
            availableCommands = listOf("git add", "git commit", "git status", "git log"),
            hints = listOf(
                "Add and commit one file at a time to create separate commits",
                "Use 'git add model.kt' then 'git commit -m \"...\"', repeat for each file",
                "Full solution: git add model.kt → git commit -m \"Add model\" → git add view.kt → git commit -m \"Add view\" → git add controller.kt → git commit -m \"Add controller\"",
            ),
            optimalCommandCount = 6,
        ),

        LevelDef(
            id = "w1_l4",
            worldId = "world1",
            title = "Status Check",
            briefing = "You've been working on multiple files but lost track of what's staged and what isn't.\n" +
                "Some files are already staged, others have changes only in the working directory.\n" +
                "Your task: commit ONLY the staged files, leaving the rest for later.",
            objective = "Create a commit with message \"Save progress\" using only the already-staged changes.",
            initialState = run {
                val initCommit = com.app.gitquest.engine.Commit(
                    id = "def5678",
                    message = "Initial commit",
                    parentIds = emptyList(),
                    files = mapOf("app.kt" to "v1", "tests.kt" to "v1"),
                )
                GitState(
                    isInitialized = true,
                    commits = mapOf("def5678" to initCommit),
                    branches = mapOf("main" to com.app.gitquest.engine.Branch("main", "def5678")),
                    head = com.app.gitquest.engine.Head.Attached("main"),
                    workingDirectory = mapOf(
                        "app.kt" to "v2",
                        "tests.kt" to "v2",
                        "debug.kt" to "temp code",
                    ),
                    stagingArea = mapOf("app.kt" to "v2"),
                )
            },
            goal = LevelGoal(
                commitCount = 2,
                headOnBranch = "main",
                requiredCommitMessages = listOf("Save progress"),
            ),
            availableCommands = listOf("git commit", "git status", "git add", "git log"),
            hints = listOf(
                "Use 'git status' to see what's already staged",
                "You don't need to add anything — just commit what's already staged",
                "Just run: git commit -m \"Save progress\"",
            ),
            optimalCommandCount = 1,
        ),

        LevelDef(
            id = "w1_l5",
            worldId = "world1",
            title = "Time Machine",
            briefing = "Your project has several commits.\n" +
                    "Use 'git log' to explore the history.\n" +
                    "Then checkout an older commit to see what the code looked like before.\n" +
                    "Finally, return to main.\n" +
                    "This teaches you that Git lets you travel through time.",
            objective = "View the log, then switch back to main (HEAD must end on main).",
            initialState = run {
                val c1 = com.app.gitquest.engine.Commit("c000001", "Initial commit", emptyList(), files = mapOf("readme.md" to "v1"))
                val c2 = com.app.gitquest.engine.Commit("c000002", "Add feature", listOf("c000001"), files = mapOf("readme.md" to "v1", "feature.kt" to "done"))
                val c3 = com.app.gitquest.engine.Commit("c000003", "Fix bug", listOf("c000002"), files = mapOf("readme.md" to "v2", "feature.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("c000001" to c1, "c000002" to c2, "c000003" to c3),
                    branches = mapOf("main" to com.app.gitquest.engine.Branch("main", "c000003")),
                    head = com.app.gitquest.engine.Head.Attached("main"),
                    workingDirectory = mapOf("readme.md" to "v2", "feature.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
            ),
            availableCommands = listOf("git log", "git checkout", "git switch"),
            hints = listOf(
                "Try 'git log' to see all the commits and their IDs",
                "Use 'git checkout <commit-id>' to visit an old commit, then 'git checkout main' to return",
                "Run: git log → git checkout c000001 → git checkout main",
            ),
            optimalCommandCount = 3,
        ),
    )
}

/**
 * World 2: Branching Out
 * Teaches: git branch, git checkout, git switch, git merge (fast-forward)
 */
object World2Levels {

    val LEVELS: List<LevelDef> = listOf(

        // ── Level 1: Your First Branch ──
        LevelDef(
            id = "w2_l1",
            worldId = "world2",
            title = "Your First Branch",
            briefing = "Your team wants to add a login feature, but they don't want to risk breaking the stable main branch.\n" +
                    "The solution? Create a separate branch to work on the feature in isolation.\n" +
                    "Branches in Git are lightweight — they're just pointers to commits.",
            objective = "Create a branch called 'feature' and switch to it.",
            initialState = run {
                val c1 = Commit("b000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("b000002", "Add homepage", listOf("b000001"), files = mapOf("app.kt" to "v1", "home.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("b000001" to c1, "b000002" to c2),
                    branches = mapOf("main" to Branch("main", "b000002")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1", "home.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "feature",
                branchCount = 2,
            ),
            availableCommands = listOf("git branch", "git checkout", "git switch"),
            hints = listOf(
                "Use 'git branch feature' to create a new branch",
                "After creating the branch, use 'git checkout feature' or 'git switch feature' to move to it",
                "Solution: git branch feature → git checkout feature",
            ),
            optimalCommandCount = 2,
        ),

        // ── Level 2: Shortcut Switch ──
        LevelDef(
            id = "w2_l2",
            worldId = "world2",
            title = "Shortcut Switch",
            briefing = "Creating a branch and switching to it is so common that Git has a shortcut for it.\n" +
                    "Instead of two commands, you can do it in one.\n" +
                    "Your PM just asked you to start a 'hotfix' branch urgently — every second counts!",
            objective = "Create and switch to a branch called 'hotfix' in a single command.",
            initialState = run {
                val c1 = Commit("c100001", "Initial commit", emptyList(), files = mapOf("server.kt" to "v1"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("c100001" to c1),
                    branches = mapOf("main" to Branch("main", "c100001")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("server.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "hotfix",
                branchCount = 2,
            ),
            availableCommands = listOf("git checkout", "git switch", "git branch"),
            hints = listOf(
                "There's a shortcut that creates AND switches in one command",
                "Try 'git checkout -b hotfix' or 'git switch -c hotfix'",
                "Solution: git checkout -b hotfix",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 3: Parallel Work ──
        LevelDef(
            id = "w2_l3",
            worldId = "world2",
            title = "Parallel Work",
            briefing = "You're on the 'feature' branch and need to add a new file and commit it.\n" +
                    "This commit will only exist on the feature branch — main stays untouched.\n" +
                    "This is the power of branches: parallel, isolated work.",
            objective = "While on the 'feature' branch, add login.kt and commit with message \"Add login\".",
            initialState = run {
                val c1 = Commit("d000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("d000001" to c1),
                    branches = mapOf(
                        "main" to Branch("main", "d000001"),
                        "feature" to Branch("feature", "d000001"),
                    ),
                    head = Head.Attached("feature"),
                    workingDirectory = mapOf("app.kt" to "v1", "login.kt" to "class LoginScreen"),
                )
            },
            goal = LevelGoal(
                commitCount = 2,
                headOnBranch = "feature",
                requiredCommitMessages = listOf("Add login"),
            ),
            availableCommands = listOf("git add", "git commit", "git status", "git log"),
            hints = listOf(
                "You're already on the feature branch — just add and commit",
                "Stage the new file first with 'git add login.kt'",
                "Solution: git add login.kt → git commit -m \"Add login\"",
            ),
            optimalCommandCount = 2,
        ),

        // ── Level 4: Fast Forward ──
        LevelDef(
            id = "w2_l4",
            worldId = "world2",
            title = "Fast Forward",
            briefing = "Your feature is done! Time to bring those changes back into main.\n" +
                    "Since main hasn't moved since you branched off, Git can do a 'fast-forward' merge.\n" +
                    "it simply moves the main pointer forward to where feature is.\n" +
                    "No merge commit needed.",
            objective = "Switch to main and merge the 'feature' branch into it.",
            initialState = run {
                val c1 = Commit("e000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("e000002", "Add login", listOf("e000001"), files = mapOf("app.kt" to "v1", "login.kt" to "done"))
                val c3 = Commit("e000003", "Add signup", listOf("e000002"), files = mapOf("app.kt" to "v1", "login.kt" to "done", "signup.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("e000001" to c1, "e000002" to c2, "e000003" to c3),
                    branches = mapOf(
                        "main" to Branch("main", "e000001"),
                        "feature" to Branch("feature", "e000003"),
                    ),
                    head = Head.Attached("feature"),
                    workingDirectory = mapOf("app.kt" to "v1", "login.kt" to "done", "signup.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                branchToCommitMessage = mapOf("main" to "Add signup"),
            ),
            availableCommands = listOf("git checkout", "git switch", "git merge", "git log"),
            hints = listOf(
                "First switch to main — you merge INTO the branch you're on",
                "Then use 'git merge feature' to bring feature's commits into main",
                "Solution: git checkout main → git merge feature",
            ),
            optimalCommandCount = 2,
        ),

        // ── Level 5: Branch Cleanup ──
        LevelDef(
            id = "w2_l5",
            worldId = "world2",
            title = "Branch Cleanup",
            briefing = "After merging, the feature branch is no longer needed.\n" +
                    "Keeping stale branches around clutters your repository.\n" +
                    "Good developers clean up after themselves.\n" +
                    "Delete the merged branch to keep things tidy.",
            objective = "Merge 'feature' into main, then delete the 'feature' branch.",
            initialState = run {
                val c1 = Commit("f000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("f000002", "Feature work", listOf("f000001"), files = mapOf("app.kt" to "v1", "feature.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("f000001" to c1, "f000002" to c2),
                    branches = mapOf(
                        "main" to Branch("main", "f000001"),
                        "feature" to Branch("feature", "f000002"),
                    ),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                deletedBranches = listOf("feature"),
                branchCount = 1,
            ),
            availableCommands = listOf("git merge", "git branch", "git log"),
            hints = listOf(
                "First merge feature into main, then delete the branch",
                "Use 'git branch -d feature' to delete a branch after merging",
                "Solution: git merge feature → git branch -d feature",
            ),
            optimalCommandCount = 2,
        ),

        // ── Level 6: Branch Explorer ──
        LevelDef(
            id = "w2_l6",
            worldId = "world2",
            title = "Branch Explorer",
            briefing = "Your repo has three branches with different work on each.\n" +
                    "Your task is to explore each branch using checkout, see what's there, and then merge everything into main.\n" +
                    "This is what real-world Git looks like — multiple streams of work converging into one.",
            objective = "Merge both 'auth' and 'ui' branches into main. All three branches' commits must be reachable from main.",
            initialState = run {
                val c1 = Commit("g000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("g000002", "Add auth", listOf("g000001"), files = mapOf("app.kt" to "v1", "auth.kt" to "done"))
                val c3 = Commit("g000003", "Add UI", listOf("g000001"), files = mapOf("app.kt" to "v1", "ui.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("g000001" to c1, "g000002" to c2, "g000003" to c3),
                    branches = mapOf(
                        "main" to Branch("main", "g000001"),
                        "auth" to Branch("auth", "g000002"),
                        "ui" to Branch("ui", "g000003"),
                    ),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                commitCount = 5, // 3 original + 2 merge commits
                requiresMergeCommit = true,
            ),
            availableCommands = listOf("git checkout", "git switch", "git merge", "git branch", "git log"),
            hints = listOf(
                "You need to merge each branch separately into main",
                "Stay on main and merge one branch at a time: 'git merge auth' then 'git merge ui'",
                "Solution: git merge auth → git merge ui",
            ),
            optimalCommandCount = 2,
        ),
    )
}

/**
 * World 3: Collaboration
 * Teaches: 3-way merge, merge conflicts, git stash
 */
object World3Levels {

    val LEVELS: List<LevelDef> = listOf(

        // ── Level 1: Divergent Paths ──
        LevelDef(
            id = "w3_l1",
            worldId = "world3",
            title = "Divergent Paths",
            briefing = "Something new is happening: both you and your teammate have made commits on different branches since you last synced.\n" +
                    "Main has moved forward, AND your feature branch has moved forward.\n" +
                    "Git can't fast-forward anymore — it needs to create a merge commit that combines both histories.",
            objective = "Merge 'feature' into main. A merge commit will be created automatically.",
            initialState = run {
                val c1 = Commit("h000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("h000002", "Main update", listOf("h000001"), files = mapOf("app.kt" to "v1", "docs.md" to "readme"))
                val c3 = Commit("h000003", "Feature work", listOf("h000001"), files = mapOf("app.kt" to "v1", "feature.kt" to "new"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("h000001" to c1, "h000002" to c2, "h000003" to c3),
                    branches = mapOf(
                        "main" to Branch("main", "h000002"),
                        "feature" to Branch("feature", "h000003"),
                    ),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1", "docs.md" to "readme"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                requiresMergeCommit = true,
            ),
            availableCommands = listOf("git merge", "git log", "git branch"),
            hints = listOf(
                "You're already on main — just merge the feature branch",
                "Use 'git merge feature' and Git will create a merge commit",
                "Solution: git merge feature",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 2: Conflict Zone ──
        LevelDef(
            id = "w3_l2",
            worldId = "world3",
            title = "Conflict Zone",
            briefing = "Disaster!!!\n" +
                    "You and your teammate both edited the same file on different branches.\n" +
                    "When you try to merge, Git doesn't know which version to keep.\n" +
                    "This is a merge conflict.\n" +
                    "You'll see conflict markers (<<<<<<< and >>>>>>>) in the file.\n" +
                    "Your job: resolve the conflict by choosing the right content, then commit the resolution.",
            objective = "Merge 'feature' into main, resolve the conflict by staging the fixed file, and commit with message \"Resolve conflict\".",
            initialState = run {
                val c1 = Commit("i000001", "Initial commit", emptyList(), files = mapOf("config.kt" to "original"))
                val c2 = Commit("i000002", "Update config on main", listOf("i000001"), files = mapOf("config.kt" to "main version"))
                val c3 = Commit("i000003", "Update config on feature", listOf("i000001"), files = mapOf("config.kt" to "feature version"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("i000001" to c1, "i000002" to c2, "i000003" to c3),
                    branches = mapOf(
                        "main" to Branch("main", "i000002"),
                        "feature" to Branch("feature", "i000003"),
                    ),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("config.kt" to "main version"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                requiredCommitMessages = listOf("Resolve conflict"),
                requiresMergeCommit = false, // The resolution commit is a regular commit in our engine
            ),
            availableCommands = listOf("git merge", "git add", "git commit", "git status"),
            hints = listOf(
                "First try 'git merge feature' — you'll see a conflict message",
                "After the conflict, the file has conflict markers. Stage it with 'git add config.kt' to accept the merged content",
                "Solution: git merge feature → git add config.kt → git commit -m \"Resolve conflict\"",
            ),
            optimalCommandCount = 3,
        ),

        // ── Level 3: Stash It ──
        LevelDef(
            id = "w3_l3",
            worldId = "world3",
            title = "Stash It",
            briefing = "You're halfway through coding a feature when your boss says 'drop everything, there's a critical bug on main.\n" +
                    "' You can't commit half-finished work, and you can't switch branches with uncommitted changes.\n" +
                    "The solution: stash your work, fix the bug, then pop your stash to continue where you left off.",
            objective = "Stash your changes, switch to main, then switch back to feature and pop the stash.",
            initialState = run {
                val c1 = Commit("j000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("j000001" to c1),
                    branches = mapOf(
                        "main" to Branch("main", "j000001"),
                        "feature" to Branch("feature", "j000001"),
                    ),
                    head = Head.Attached("feature"),
                    workingDirectory = mapOf("app.kt" to "v1", "wip.kt" to "work in progress"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "feature",
                stashEmpty = true,
            ),
            availableCommands = listOf("git stash", "git stash pop", "git checkout", "git switch", "git status"),
            hints = listOf(
                "Use 'git stash' to save your uncommitted work temporarily",
                "After stashing, switch to main, then back to feature, then 'git stash pop'",
                "Solution: git stash → git checkout main → git checkout feature → git stash pop",
            ),
            optimalCommandCount = 4,
        ),

        // ── Level 4: The Full Workflow ──
        LevelDef(
            id = "w3_l4",
            worldId = "world3",
            title = "The Full Workflow",
            briefing = "Time to put it all together.\n" +
                    "Create a feature branch, make a commit on it, switch back to main, make a different commit there, then merge the feature branch into main.\n" +
                    "This is the standard Git collaboration workflow that every team uses every single day.",
            objective = "Create 'feature' branch, commit on it, commit on main separately, then merge feature into main.",
            initialState = run {
                val c1 = Commit("k000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("k000001" to c1),
                    branches = mapOf("main" to Branch("main", "k000001")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf(
                        "app.kt" to "v1",
                        "feature.kt" to "new feature",
                        "hotfix.kt" to "urgent fix",
                    ),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                requiresMergeCommit = true,
                branchCount = 2,
            ),
            availableCommands = listOf("git checkout", "git switch", "git branch", "git add", "git commit", "git merge", "git log"),
            hints = listOf(
                "Create feature branch, switch to it, add and commit feature.kt. Then switch to main, add and commit hotfix.kt. Then merge feature.",
                "Step by step: checkout -b feature → add feature.kt → commit → checkout main → add hotfix.kt → commit → merge feature",
                "Full: git checkout -b feature → git add feature.kt → git commit -m \"Add feature\" → git checkout main → git add hotfix.kt → git commit -m \"Add hotfix\" → git merge feature",
            ),
            optimalCommandCount = 7,
        ),

        // ── Level 5: Stash and Fix ──
        LevelDef(
            id = "w3_l5",
            worldId = "world3",
            title = "Stash and Fix",
            briefing = "You're deep in feature development when an urgent bug report comes in.\n" +
                    "Stash your work, switch to main, create a hotfix branch, fix the bug with a commit,\n" +
                    "merge the hotfix into main, then return to your feature and restore your stashed work.\n" +
                    "This is real-world Git under pressure.",
            objective = "Stash your changes, fix the bug on a 'hotfix' branch, merge hotfix into main, return to feature and pop stash.",
            initialState = run {
                val c1 = Commit("l000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1", "server.kt" to "buggy"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("l000001" to c1),
                    branches = mapOf(
                        "main" to Branch("main", "l000001"),
                        "feature" to Branch("feature", "l000001"),
                    ),
                    head = Head.Attached("feature"),
                    workingDirectory = mapOf("app.kt" to "v1", "server.kt" to "buggy", "newfile.kt" to "wip"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "feature",
                stashEmpty = true,
                commitCount = 2, // original + hotfix
            ),
            availableCommands = listOf("git stash", "git stash pop", "git checkout", "git switch", "git branch", "git add", "git commit", "git merge"),
            hints = listOf(
                "Stash first, then switch to main, create hotfix branch, fix and commit, merge to main, return to feature, pop stash",
                "git stash → git checkout main → git checkout -b hotfix → git add server.kt → git commit → git checkout main → git merge hotfix → git checkout feature → git stash pop",
                "Don't forget to modify the file content before committing on the hotfix branch",
            ),
            optimalCommandCount = 9,
        ),

        // ── Level 6: Multi-Branch Merge ──
        LevelDef(
            id = "w3_l6",
            worldId = "world3",
            title = "Multi-Branch Merge",
            briefing = "The sprint is ending.\n" +
                    "Three developers worked on three separate branches:\n" +
                    "'auth', 'payments', and 'notifications'.\n" +
                    "All three need to be merged into main before the release.\n" +
                    "Each branch has diverged from main, so you'll get three merge commits.\n" +
                    "Welcome to release day.",
            objective = "Merge all three branches (auth, payments, notifications) into main.",
            initialState = run {
                val c1 = Commit("m000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("m000002", "Add auth", listOf("m000001"), files = mapOf("app.kt" to "v1", "auth.kt" to "done"))
                val c3 = Commit("m000003", "Add payments", listOf("m000001"), files = mapOf("app.kt" to "v1", "pay.kt" to "done"))
                val c4 = Commit("m000004", "Add notifications", listOf("m000001"), files = mapOf("app.kt" to "v1", "notif.kt" to "done"))
                val c5 = Commit("m000005", "Main hotfix", listOf("m000001"), files = mapOf("app.kt" to "v2"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("m000001" to c1, "m000002" to c2, "m000003" to c3, "m000004" to c4, "m000005" to c5),
                    branches = mapOf(
                        "main" to Branch("main", "m000005"),
                        "auth" to Branch("auth", "m000002"),
                        "payments" to Branch("payments", "m000003"),
                        "notifications" to Branch("notifications", "m000004"),
                    ),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v2"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                commitCount = 8, // 5 original + 3 merge commits
            ),
            availableCommands = listOf("git merge", "git branch", "git log"),
            hints = listOf(
                "You're on main — merge each branch one at a time",
                "Order doesn't matter: git merge auth → git merge payments → git merge notifications",
                "Each merge creates a merge commit since main has diverged from each branch",
            ),
            optimalCommandCount = 3,
        ),
    )
}

/**
 * World 4: Rewriting History
 * Teaches: git rebase, git cherry-pick, git reset, git revert
 */
object World4Levels {

    val LEVELS: List<LevelDef> = listOf(

        // ── Level 1: Oops, Undo That ──
        LevelDef(
            id = "w4_l1",
            worldId = "world4",
            title = "Oops, Undo That",
            briefing = "You just committed a file containing your database password.\n" +
                    " It's the latest commit and nobody has seen it yet.\n" +
                    "You need to undo it — but you want to keep the changes in your working directory so you can remove the password before recommitting.\n" +
                    "This is what 'git reset --soft' is for.",
            objective = "Undo the last commit using soft reset. The commit should disappear but changes stay staged.",
            initialState = run {
                val c1 = Commit("n000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("n000002", "Add config with password", listOf("n000001"),
                    files = mapOf("app.kt" to "v1", "config.kt" to "password=secret123"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("n000001" to c1, "n000002" to c2),
                    branches = mapOf("main" to Branch("main", "n000002")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1", "config.kt" to "password=secret123"),
                )
            },
            goal = LevelGoal(
                commitCount = 1,
                headOnBranch = "main",
                branchToCommitMessage = mapOf("main" to "Initial commit"),
            ),
            availableCommands = listOf("git reset", "git log", "git status"),
            hints = listOf(
                "git reset moves HEAD backward. --soft keeps your changes staged",
                "Use HEAD~1 to go back one commit",
                "Solution: git reset --soft HEAD~1",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 2: Hard Reset ──
        LevelDef(
            id = "w4_l2",
            worldId = "world4",
            title = "Nuclear Option",
            briefing = "Your last two commits were a complete mess — wrong branch, wrong files, everything.\n" +
                    "You want to completely erase them and go back to the clean state two commits ago.\n" +
                    "This is 'git reset --hard' — the nuclear option.\n" +
                    "It erases commits AND throws away all uncommitted changes.\n" +
                    "Use with extreme caution.",
            objective = "Hard reset back to the 'Initial commit', erasing the last two commits entirely.",
            initialState = run {
                val c1 = Commit("o000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "clean"))
                val c2 = Commit("o000002", "Wrong file 1", listOf("o000001"),
                    files = mapOf("app.kt" to "clean", "junk1.kt" to "bad"))
                val c3 = Commit("o000003", "Wrong file 2", listOf("o000002"),
                    files = mapOf("app.kt" to "clean", "junk1.kt" to "bad", "junk2.kt" to "worse"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("o000001" to c1, "o000002" to c2, "o000003" to c3),
                    branches = mapOf("main" to Branch("main", "o000003")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "clean", "junk1.kt" to "bad", "junk2.kt" to "worse"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                branchToCommitMessage = mapOf("main" to "Initial commit"),
            ),
            availableCommands = listOf("git reset", "git log", "git status"),
            hints = listOf(
                "--hard erases everything: commits, staging, and working directory",
                "HEAD~2 goes back two commits",
                "Solution: git reset --hard HEAD~2",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 3: Safe Undo ──
        LevelDef(
            id = "w4_l3",
            worldId = "world4",
            title = "Safe Undo",
            briefing = "A commit from earlier introduced a bug, but other commits came after it.\n" +
                    "You can't just reset — that would erase the good commits too.\n" +
                    "Instead, use 'git revert' to create a NEW commit that undoes the bad one, preserving all history.\n" +
                    "This is the safe way to undo on shared branches.",
            objective = "Revert the 'Add bug' commit without losing 'Fix typo'.",
            initialState = run {
                val c1 = Commit("p000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("p000002", "Add bug", listOf("p000001"),
                    files = mapOf("app.kt" to "v1", "bug.kt" to "broken code"))
                val c3 = Commit("p000003", "Fix typo", listOf("p000002"),
                    files = mapOf("app.kt" to "v1 fixed", "bug.kt" to "broken code"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("p000001" to c1, "p000002" to c2, "p000003" to c3),
                    branches = mapOf("main" to Branch("main", "p000003")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1 fixed", "bug.kt" to "broken code"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                commitCount = 4, // 3 original + 1 revert
                requiredCommitMessages = listOf("Fix typo"), // Must still exist
            ),
            availableCommands = listOf("git revert", "git log", "git status"),
            hints = listOf(
                "git revert creates a new commit that undoes a specific old commit",
                "Use the commit ID of 'Add bug' — find it with git log",
                "Solution: git revert p000002",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 4: Cherry Pick ──
        LevelDef(
            id = "w4_l4",
            worldId = "world4",
            title = "Cherry Pick",
            briefing = "A teammate made an excellent commit on the 'experiment' branch, but the rest of that branch is a mess and shouldn't be merged.\n" +
                    "You want JUST that one commit on main.\n" +
                    "This is cherry-picking: copying a single commit from one branch to another without merging the entire branch.",
            objective = "Cherry-pick the 'Add utility function' commit from the experiment branch onto main.",
            initialState = run {
                val c1 = Commit("q000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("q000002", "Bad experiment", listOf("q000001"),
                    files = mapOf("app.kt" to "broken", "mess.kt" to "bad"))
                val c3 = Commit("q000003", "Add utility function", listOf("q000002"),
                    files = mapOf("app.kt" to "broken", "mess.kt" to "bad", "utils.kt" to "useful"))
                val c4 = Commit("q000004", "More mess", listOf("q000003"),
                    files = mapOf("app.kt" to "very broken", "mess.kt" to "worse", "utils.kt" to "useful"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("q000001" to c1, "q000002" to c2, "q000003" to c3, "q000004" to c4),
                    branches = mapOf(
                        "main" to Branch("main", "q000001"),
                        "experiment" to Branch("experiment", "q000004"),
                    ),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                commitCount = 5, // 4 original + 1 cherry-picked
                requiredCommitMessages = listOf("Add utility function"),
            ),
            availableCommands = listOf("git cherry-pick", "git log", "git checkout", "git switch"),
            hints = listOf(
                "You need the commit ID of 'Add utility function' — use git log or check the experiment branch",
                "Stay on main and use 'git cherry-pick <commit-id>'",
                "Solution: git cherry-pick q000003",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 5: Clean Rebase ──
        LevelDef(
            id = "w4_l5",
            worldId = "world4",
            title = "Clean Rebase",
            briefing = "Your feature branch has fallen behind main.\n" +
                    "Instead of creating a messy merge commit, your team prefers rebasing — replaying your feature commits on top of the latest main.\n" +
                    "This creates a clean, linear history as if you started your work from the latest main commit.\n" +
                    "Many teams require this before merging pull requests.",
            objective = "Rebase the 'feature' branch onto main so feature's commits sit on top of main's latest.",
            initialState = run {
                val c1 = Commit("r000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("r000002", "Main update 1", listOf("r000001"),
                    files = mapOf("app.kt" to "v2"))
                val c3 = Commit("r000003", "Main update 2", listOf("r000002"),
                    files = mapOf("app.kt" to "v3"))
                val c4 = Commit("r000004", "Feature commit 1", listOf("r000001"),
                    files = mapOf("app.kt" to "v1", "feature.kt" to "wip"))
                val c5 = Commit("r000005", "Feature commit 2", listOf("r000004"),
                    files = mapOf("app.kt" to "v1", "feature.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("r000001" to c1, "r000002" to c2, "r000003" to c3, "r000004" to c4, "r000005" to c5),
                    branches = mapOf(
                        "main" to Branch("main", "r000003"),
                        "feature" to Branch("feature", "r000005"),
                    ),
                    head = Head.Attached("feature"),
                    workingDirectory = mapOf("app.kt" to "v1", "feature.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "feature",
                requiredCommitMessages = listOf("Feature commit 1", "Feature commit 2"),
            ),
            availableCommands = listOf("git rebase", "git log", "git branch"),
            hints = listOf(
                "While on the feature branch, rebase onto main",
                "git rebase replays your commits on top of the target branch",
                "Solution: git rebase main",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 6: Rebase Then Merge ──
        LevelDef(
            id = "w4_l6",
            worldId = "world4",
            title = "Rebase Then Merge",
            briefing = "The ideal workflow: rebase your feature onto main to get a linear history, then switch to main and fast-forward merge.\n" +
                    "The result is a perfectly clean, linear commit history with no merge commits.\n" +
                    "This is what top engineering teams aim for.",
            objective = "Rebase feature onto main, then fast-forward merge feature into main. Final history should be linear.",
            initialState = run {
                val c1 = Commit("s000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("s000002", "Main work", listOf("s000001"), files = mapOf("app.kt" to "v2"))
                val c3 = Commit("s000003", "Feature work", listOf("s000001"), files = mapOf("app.kt" to "v1", "feat.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("s000001" to c1, "s000002" to c2, "s000003" to c3),
                    branches = mapOf(
                        "main" to Branch("main", "s000002"),
                        "feature" to Branch("feature", "s000003"),
                    ),
                    head = Head.Attached("feature"),
                    workingDirectory = mapOf("app.kt" to "v1", "feat.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                requiredCommitMessages = listOf("Main work", "Feature work"),
            ),
            availableCommands = listOf("git rebase", "git checkout", "git switch", "git merge", "git log"),
            hints = listOf(
                "First rebase feature onto main, then switch to main and merge feature",
                "After rebase, feature's commits are on top of main — so merge will fast-forward",
                "Solution: git rebase main → git checkout main → git merge feature",
            ),
            optimalCommandCount = 3,
        ),
    )
}

/**
 * World 5: Rescue Missions
 * Teaches: git reflog, detached HEAD recovery, complex reset/revert scenarios
 */
object World5Levels {

    val LEVELS: List<LevelDef> = listOf(

        // ── Level 1: Detached HEAD ──
        LevelDef(
            id = "w5_l1",
            worldId = "world5",
            title = "Detached HEAD",
            briefing = "You accidentally checked out a commit ID instead of a branch name.\n" +
                    "Now you're in 'detached HEAD' state — Git is warning you that any commits you make here could be lost.\n" +
                    "Don't panic! Just get back to a branch.",
            objective = "You're in detached HEAD state. Get back to the main branch safely.",
            initialState = run {
                val c1 = Commit("t000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("t000002", "Add feature", listOf("t000001"), files = mapOf("app.kt" to "v1", "feat.kt" to "done"))
                val c3 = Commit("t000003", "Update docs", listOf("t000002"), files = mapOf("app.kt" to "v1", "feat.kt" to "done", "docs.md" to "v1"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("t000001" to c1, "t000002" to c2, "t000003" to c3),
                    branches = mapOf("main" to Branch("main", "t000003")),
                    head = Head.Detached("t000001"), // Stuck at an old commit!
                    workingDirectory = mapOf("app.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
            ),
            availableCommands = listOf("git checkout", "git switch", "git log", "git branch"),
            hints = listOf(
                "You're detached at an old commit — check 'git branch' to see where main is",
                "Simply switch back to main with 'git checkout main'",
                "Solution: git checkout main",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 2: Save from Detached ──
        LevelDef(
            id = "w5_l2",
            worldId = "world5",
            title = "Save from Detached",
            briefing = "You made commits while in detached HEAD state — they're valuable but not on any branch.\n" +
                    "If you just switch to main, those commits become 'orphaned' and will eventually be garbage collected.\n" +
                    "Save them by creating a new branch at your current position!",
            objective = "Create a branch called 'rescue' at your current detached position, then switch to it.",
            initialState = run {
                val c1 = Commit("u000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("u000002", "Main work", listOf("u000001"), files = mapOf("app.kt" to "v2"))
                val c3 = Commit("u000003", "Detached work 1", listOf("u000001"), files = mapOf("app.kt" to "v1", "experiment.kt" to "wip"))
                val c4 = Commit("u000004", "Detached work 2", listOf("u000003"), files = mapOf("app.kt" to "v1", "experiment.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("u000001" to c1, "u000002" to c2, "u000003" to c3, "u000004" to c4),
                    branches = mapOf("main" to Branch("main", "u000002")),
                    head = Head.Detached("u000004"), // Detached with valuable work!
                    workingDirectory = mapOf("app.kt" to "v1", "experiment.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "rescue",
                branchCount = 2,
            ),
            availableCommands = listOf("git checkout", "git switch", "git branch", "git log"),
            hints = listOf(
                "You need to create a branch at your current position to save the commits",
                "Use 'git checkout -b rescue' to create and switch in one step",
                "Solution: git checkout -b rescue",
            ),
            optimalCommandCount = 1,
        ),

        // ── Level 3: Reflog to the Rescue ──
        LevelDef(
            id = "w5_l3",
            worldId = "world5",
            title = "Reflog to the Rescue",
            briefing = "Someone ran 'git reset --hard' and erased the last two commits.\n" +
                    "The work is gone from the branch history.\n" +
                    "But not from Git's memory! The reflog records every move HEAD has ever made.\n" +
                    "Use it to find the lost commit and bring it back.",
            objective = "Use reflog to find the lost 'Important feature' commit, then reset main back to it.",
            initialState = run {
                // The "current" state: main was reset back, but the commits still exist in the store
                val c1 = Commit("v000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("v000002", "Setup database", listOf("v000001"), files = mapOf("app.kt" to "v1", "db.kt" to "done"))
                val c3 = Commit("v000003", "Important feature", listOf("v000002"), files = mapOf("app.kt" to "v1", "db.kt" to "done", "feature.kt" to "critical"))
                GitState(
                    isInitialized = true,
                    // All commits still exist in the object store
                    commits = mapOf("v000001" to c1, "v000002" to c2, "v000003" to c3),
                    // But main has been reset back to c1!
                    branches = mapOf("main" to Branch("main", "v000001")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                branchToCommitMessage = mapOf("main" to "Important feature"),
            ),
            availableCommands = listOf("git reflog", "git reset", "git log", "git checkout"),
            hints = listOf(
                "Use 'git reflog' to see ALL commits, including ones not on any branch",
                "Find the commit ID of 'Important feature' in the reflog output",
                "Solution: git reflog (find v000003) → git reset --hard v000003",
            ),
            optimalCommandCount = 2,
        ),

        // ── Level 4: Wrong Branch Fix ──
        LevelDef(
            id = "w5_l4",
            worldId = "world5",
            title = "Wrong Branch Fix",
            briefing = "You made three commits on main that should have been on a feature branch.\n" +
                    "The commits are good, but they're in the wrong place.\n" +
                    "You need to: create a feature branch (to save the commits), then reset main back to where it should be.\n" +
                    "This is a common real-world mistake.",
            objective = "Move the last 2 commits to a new 'feature' branch and reset main to 'Initial setup'.",
            initialState = run {
                val c1 = Commit("w000001", "Initial setup", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("w000002", "Feature part 1", listOf("w000001"), files = mapOf("app.kt" to "v1", "feat1.kt" to "done"))
                val c3 = Commit("w000003", "Feature part 2", listOf("w000002"), files = mapOf("app.kt" to "v1", "feat1.kt" to "done", "feat2.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("w000001" to c1, "w000002" to c2, "w000003" to c3),
                    branches = mapOf("main" to Branch("main", "w000003")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1", "feat1.kt" to "done", "feat2.kt" to "done"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                branchToCommitMessage = mapOf(
                    "main" to "Initial setup",
                    "feature" to "Feature part 2",
                ),
                branchCount = 2,
            ),
            availableCommands = listOf("git branch", "git reset", "git checkout", "git switch", "git log"),
            hints = listOf(
                "Create a feature branch first (it will point to the current commit), THEN reset main",
                "git branch feature saves a pointer to the current commit before you reset",
                "Solution: git branch feature → git reset --hard w000001",
            ),
            optimalCommandCount = 2,
        ),

        // ── Level 5: The Recovery Expert ──
        LevelDef(
            id = "w5_l5",
            worldId = "world5",
            title = "The Recovery Expert",
            briefing = "Final challenge.\n" +
                    "A junior developers panicked and ran several destructive commands:\n" +
                    "They deleted a branch, reset main backward, and left commits orphaned.\n" +
                    "Your job as the senior dev: use reflog and your Git knowledge to reconstruct the repository.\n" +
                    "Recover the deleted branch and restore main to its correct position.",
            objective = "Restore main to the 'Release v1.0' commit and recreate the 'develop' branch at 'Add tests'.",
            initialState = run {
                val c1 = Commit("x000001", "Initial commit", emptyList(), files = mapOf("app.kt" to "v1"))
                val c2 = Commit("x000002", "Core features", listOf("x000001"), files = mapOf("app.kt" to "v2", "core.kt" to "done"))
                val c3 = Commit("x000003", "Release v1.0", listOf("x000002"), files = mapOf("app.kt" to "v2", "core.kt" to "done", "release.md" to "v1.0"))
                val c4 = Commit("x000004", "Add tests", listOf("x000003"), files = mapOf("app.kt" to "v2", "core.kt" to "done", "release.md" to "v1.0", "tests.kt" to "done"))
                GitState(
                    isInitialized = true,
                    commits = mapOf("x000001" to c1, "x000002" to c2, "x000003" to c3, "x000004" to c4),
                    // Main was reset back to c1, develop branch was deleted
                    branches = mapOf("main" to Branch("main", "x000001")),
                    head = Head.Attached("main"),
                    workingDirectory = mapOf("app.kt" to "v1"),
                )
            },
            goal = LevelGoal(
                headOnBranch = "main",
                branchToCommitMessage = mapOf(
                    "main" to "Release v1.0",
                    "develop" to "Add tests",
                ),
                branchCount = 2,
            ),
            availableCommands = listOf("git reflog", "git reset", "git branch", "git checkout", "git log"),
            hints = listOf(
                "Use git reflog to find the commit IDs for 'Release v1.0' and 'Add tests'",
                "Reset main to 'Release v1.0', then create develop at 'Add tests'",
                "Solution: git reflog → git reset --hard x000003 → git branch develop x000004",
            ),
            optimalCommandCount = 3,
        ),
    )
}