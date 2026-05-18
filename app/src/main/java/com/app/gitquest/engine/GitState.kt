package com.app.gitquest.engine

/**
 * Represents a single commit in the repository.
 * Immutable — engine operations create new Commit instances.
 */
data class Commit(
    val id: String,
    val message: String,
    val parentIds: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val files: Map<String, String> = emptyMap(),
) {
    val isMergeCommit: Boolean get() = parentIds.size > 1
}

/**
 * A branch is just a named pointer to a commit.
 */
data class Branch(
    val name: String,
    val commitId: String,
)

/**
 * HEAD can be attached to a branch or detached pointing at a commit.
 */
sealed interface Head {
    data class Attached(val branchName: String) : Head
    data class Detached(val commitId: String) : Head
}

/**
 * The complete, immutable state of a Git repository at a point in time.
 * Every engine operation takes a GitState and returns a new GitState.
 * This makes undo trivial (pop the history stack) and testing easy.
 */
data class GitState(
    val commits: Map<String, Commit> = emptyMap(),
    val branches: Map<String, Branch> = emptyMap(),
    val head: Head = Head.Attached("main"),
    val stagingArea: Map<String, String> = emptyMap(),
    val workingDirectory: Map<String, String> = emptyMap(),
    val stash: List<Map<String, String>> = emptyList(),
    val isInitialized: Boolean = false,
) {
    /**
     * Resolves HEAD to the commit ID it currently points to.
     */
    fun resolveHead(): String? = when (head) {
        is Head.Attached -> branches[head.branchName]?.commitId
        is Head.Detached -> head.commitId
    }

    /**
     * Gets the current branch name, or null if HEAD is detached.
     */
    fun currentBranchName(): String? = when (head) {
        is Head.Attached -> head.branchName
        is Head.Detached -> null
    }

    /**
     * Gets the commit that HEAD currently points to.
     */
    fun headCommit(): Commit? = resolveHead()?.let { commits[it] }

    /**
     * Returns the full commit history reachable from HEAD, newest first.
     */
    fun log(): List<Commit> {
        val headId = resolveHead() ?: return emptyList()
        val visited = mutableSetOf<String>()
        val result = mutableListOf<Commit>()
        val queue = ArrayDeque<String>()
        queue.add(headId)

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id in visited) continue
            visited.add(id)
            val commit = commits[id] ?: continue
            result.add(commit)
            commit.parentIds.forEach { queue.add(it) }
        }

        return result.sortedByDescending { it.timestamp }
    }

    /**
     * Checks if commitId is an ancestor of descendantId.
     */
    fun isAncestor(commitId: String, descendantId: String): Boolean {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(descendantId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == commitId) return true
            if (current in visited) continue
            visited.add(current)
            commits[current]?.parentIds?.forEach { queue.add(it) }
        }
        return false
    }

    /**
     * Finds the common ancestor of two commits (for 3-way merge).
     */
    fun findMergeBase(commitId1: String, commitId2: String): String? {
        val ancestors1 = mutableSetOf<String>()
        val queue1 = ArrayDeque<String>()
        queue1.add(commitId1)
        while (queue1.isNotEmpty()) {
            val id = queue1.removeFirst()
            if (id in ancestors1) continue
            ancestors1.add(id)
            commits[id]?.parentIds?.forEach { queue1.add(it) }
        }

        val queue2 = ArrayDeque<String>()
        queue2.add(commitId2)
        val visited2 = mutableSetOf<String>()
        while (queue2.isNotEmpty()) {
            val id = queue2.removeFirst()
            if (id in ancestors1) return id
            if (id in visited2) continue
            visited2.add(id)
            commits[id]?.parentIds?.forEach { queue2.add(it) }
        }
        return null
    }
}

/**
 * Result of executing a Git command.
 */
sealed interface GitResult {
    data class Success(
        val newState: GitState,
        val message: String = "",
    ) : GitResult

    data class Error(
        val message: String,
    ) : GitResult
}
