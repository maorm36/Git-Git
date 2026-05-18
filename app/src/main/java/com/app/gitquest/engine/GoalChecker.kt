package com.app.gitquest.engine

/**
 * Defines what a game level requires the player to achieve in it.
 * Not all fields need to be set — only the ones the level cares about.
 */
data class LevelGoal(

    /** Required branch names and the commit messages they should point to */
    val branchToCommitMessage: Map<String, String>? = null,

    /** Required number of total commits */
    val commitCount: Int? = null,

    /** HEAD must be attached to this branch */
    val headOnBranch: String? = null,

    /** HEAD must be detached (for detached HEAD levels) */
    val headDetached: Boolean? = null,

    /** Specific commit messages that must exist in the history */
    val requiredCommitMessages: List<String>? = null,

    /** Commit messages that must NOT exist (for revert/reset levels) */
    val forbiddenCommitMessages: List<String>? = null,

    /** Required file contents at HEAD (for conflict resolution levels) */
    val requiredFiles: Map<String, String>? = null,

    /** A merge commit must exist (for merge levels) */
    val requiresMergeCommit: Boolean = false,

    /** Specific branches that must NOT exist (for cleanup levels) */
    val deletedBranches: List<String>? = null,

    /** The number of branches that should exist */
    val branchCount: Int? = null,

    /** Stash must be empty */
    val stashEmpty: Boolean? = null,
)

/**
 * Checks whether the current GitState satisfies a LevelGoal.
 * Returns a detailed result showing what's met and what's not.
 */
class GoalChecker {

    fun check(state: GitState, goal: LevelGoal): GoalResult {
        val failures = mutableListOf<String>()

        // Check HEAD is on the right branch
        goal.headOnBranch?.let { required ->
            val actual = state.currentBranchName()
            if (actual != required) {
                failures.add("HEAD should be on branch '$required' but is on '${actual ?: "detached"}'")
            }
        }

        // Check detached HEAD
        goal.headDetached?.let { shouldBeDetached ->
            val isDetached = state.head is Head.Detached
            if (shouldBeDetached && !isDetached) {
                failures.add("HEAD should be detached")
            }
            if (!shouldBeDetached && isDetached) {
                failures.add("HEAD should be attached to a branch")
            }
        }

        // Check commit count
        goal.commitCount?.let { required ->
            val actual = state.commits.size
            if (actual != required) {
                failures.add("Expected $required commits but found $actual")
            }
        }

        // Check required commit messages exist
        goal.requiredCommitMessages?.forEach { message ->
            val found = state.commits.values.any { it.message == message }
            if (!found) {
                failures.add("Missing required commit: \"$message\"")
            }
        }

        // Check forbidden commit messages don't exist (for revert/reset levels)
        goal.forbiddenCommitMessages?.forEach { message ->
            val found = state.commits.values.any { it.message == message }
            if (found) {
                failures.add("Commit \"$message\" should have been removed")
            }
        }

        // Check branches point to correct commits (by message)
        goal.branchToCommitMessage?.forEach { (branchName, expectedMessage) ->
            val branch = state.branches[branchName]
            if (branch == null) {
                failures.add("Branch '$branchName' does not exist")
            } else {
                val commit = state.commits[branch.commitId]
                if (commit?.message != expectedMessage) {
                    failures.add("Branch '$branchName' should point to \"$expectedMessage\" but points to \"${commit?.message ?: "nothing"}\"")
                }
            }
        }

        // Check for merge commit
        if (goal.requiresMergeCommit) {
            val hasMerge = state.commits.values.any { it.isMergeCommit }
            if (!hasMerge) {
                failures.add("A merge commit is required")
            }
        }

        // Check file contents at HEAD
        goal.requiredFiles?.forEach { (fileName, expectedContent) ->
            val headFiles = state.headCommit()?.files ?: emptyMap()
            val actual = headFiles[fileName]
            if (actual != expectedContent) {
                failures.add("File '$fileName' has wrong content")
            }
        }

        // Check deleted branches
        goal.deletedBranches?.forEach { branchName ->
            if (branchName in state.branches) {
                failures.add("Branch '$branchName' should be deleted")
            }
        }

        // Check branch count
        goal.branchCount?.let { required ->
            val actual = state.branches.size
            if (actual != required) {
                failures.add("Expected $required branches but found $actual")
            }
        }

        // Check stash is empty
        goal.stashEmpty?.let { shouldBeEmpty ->
            if (shouldBeEmpty && state.stash.isNotEmpty()) {
                failures.add("Stash should be empty")
            }
        }

        return GoalResult(
            isComplete = failures.isEmpty(),
            failures = failures,
            progress = calculateProgress(state, goal, failures.size),
        )
    }

    private fun calculateProgress(state: GitState, goal: LevelGoal, failureCount: Int): Float {
        // Count total checks and passed checks
        var total = 0
        var passed = 0

        goal.headOnBranch?.let { total++ }
        goal.headDetached?.let { total++ }
        goal.commitCount?.let { total++ }
        goal.requiresMergeCommit.let { if (it) total++ }
        goal.branchCount?.let { total++ }
        goal.stashEmpty?.let { total++ }
        goal.requiredCommitMessages?.let { total += it.size }
        goal.forbiddenCommitMessages?.let { total += it.size }
        goal.branchToCommitMessage?.let { total += it.size }
        goal.requiredFiles?.let { total += it.size }
        goal.deletedBranches?.let { total += it.size }

        passed = total - failureCount
        return if (total > 0)
                   (passed.toFloat() / total)
               else
                   0f
    }
}

data class GoalResult(
    val isComplete: Boolean,
    val failures: List<String>,
    val progress: Float,
)
