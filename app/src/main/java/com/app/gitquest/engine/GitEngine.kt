package com.app.gitquest.engine

import kotlin.random.Random

/**
 * The Git simulation engine.
 *
 * Every method is a pure function:
 *     (GitState, GitCommand) -> GitResult
 *
 * No side effects.
 * No Android dependencies.
 * It is trivially unit-testable.
 */
class GitEngine {

    /**
     * Execute a command against the current state of the level.
     * Returns Success with new state, or Error with message to the terminal.
     */
    fun execute(state: GitState, command: GitCommand): GitResult = when (command) {
        is GitCommand.Init -> init(state)
        is GitCommand.Add -> add(state, command.fileName)
        is GitCommand.AddAll -> addAll(state)
        is GitCommand.CommitCmd -> commit(state, command.message)
        is GitCommand.Status -> status(state)
        is GitCommand.Log -> log(state)
        is GitCommand.CreateBranch -> createBranch(state, command.name)
        is GitCommand.CreateBranchAt -> createBranchAt(state, command.name, command.commitRef)
        is GitCommand.DeleteBranch -> deleteBranch(state, command.name)
        is GitCommand.ListBranches -> listBranches(state)
        is GitCommand.Checkout -> checkout(state, command.ref)
        is GitCommand.CheckoutNewBranch -> checkoutNewBranch(state, command.name)
        is GitCommand.Switch -> switchBranch(state, command.branchName)
        is GitCommand.SwitchCreate -> switchCreate(state, command.branchName)
        is GitCommand.Merge -> merge(state, command.branchName)
        is GitCommand.Rebase -> rebase(state, command.baseBranch)
        is GitCommand.RebaseInteractive -> rebaseInteractive(state, command.baseBranch)
        is GitCommand.CherryPick -> cherryPick(state, command.commitId)
        is GitCommand.Reset -> reset(state, command.ref, command.mode)
        is GitCommand.Revert -> revert(state, command.commitId)
        is GitCommand.Stash -> stash(state)
        is GitCommand.StashPop -> stashPop(state)
        is GitCommand.StashList -> stashList(state)
        is GitCommand.Reflog -> reflog(state)
        is GitCommand.Help -> help()
    }

    // ── Repository Setup ──

    private fun init(state: GitState): GitResult {
        if (state.isInitialized) {
            return GitResult.Error("Reinitialized existing Git repository")
        }
        val newState = state.copy(
            isInitialized = true,
            branches = mapOf("main" to Branch("main", "")),
            head = Head.Attached("main"),
        )
        return GitResult.Success(newState, "Initialized empty Git repository")
    }

    // ── Staging ──

    private fun add(state: GitState, fileName: String): GitResult {
        requireInit(state)?.let { return it }

        if (fileName !in state.workingDirectory) {
            return GitResult.Error("pathspec '$fileName' did not match any files")
        }

        val newStaging = state.stagingArea.toMutableMap()
        newStaging[fileName] = state.workingDirectory[fileName]!!
        return GitResult.Success(
            state.copy(stagingArea = newStaging),
            "Changes staged: $fileName",
        )
    }

    private fun addAll(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        if (state.workingDirectory.isEmpty()) {
            return GitResult.Error("Nothing to add")
        }

        val currentFiles = state.headCommit()?.files ?: emptyMap()
        val changes = state.workingDirectory.filter { (name, content) ->
            currentFiles[name] != content
        }

        if (changes.isEmpty()) {
            return GitResult.Error("Nothing to add — working tree clean")
        }

        val newStaging = state.stagingArea.toMutableMap()
        newStaging.putAll(changes)
        return GitResult.Success(
            state.copy(stagingArea = newStaging),
            "All changes staged",
        )
    }

    // ── Committing ──

    private fun commit(state: GitState, message: String): GitResult {
        requireInit(state)?.let { return it }

        if (state.stagingArea.isEmpty()) {
            return GitResult.Error("nothing to commit, working tree clean")
        }

        val parentId = state.resolveHead()
        val parentFiles = parentId?.let { state.commits[it]?.files } ?: emptyMap()
        val newFiles = parentFiles.toMutableMap()
        newFiles.putAll(state.stagingArea)

        val commitId = generateId()
        val newCommit = Commit(
            id = commitId,
            message = message,
            parentIds = if (parentId != null && parentId.isNotEmpty()) listOf(parentId) else emptyList(),
            files = newFiles,
        )

        val newCommits = state.commits.toMutableMap()
        newCommits[commitId] = newCommit

        // Advance the current branch pointer
        val newBranches = state.branches.toMutableMap()
        val newHead: Head
        when (val h = state.head) {
            is Head.Attached -> {
                newBranches[h.branchName] = Branch(h.branchName, commitId)
                newHead = h
            }
            is Head.Detached -> {
                newHead = Head.Detached(commitId)
            }
        }

        return GitResult.Success(
            state.copy(
                commits = newCommits,
                branches = newBranches,
                head = newHead,
                stagingArea = emptyMap(),
            ),
            "[${ state.currentBranchName() ?: commitId.take(7) } $commitId] $message",
        )
    }

    // ── Display Commands ──

    private fun status(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        val branchInfo = when (val h = state.head) {
            is Head.Attached -> "On branch ${h.branchName}"
            is Head.Detached -> "HEAD detached at ${h.commitId.take(7)}"
        }

        val staged = if (state.stagingArea.isNotEmpty()) {
            "\nChanges to be committed:\n" +
                state.stagingArea.keys.joinToString("\n") { "  modified: $it" }
        } else ""

        val currentFiles = state.headCommit()?.files ?: emptyMap()
        val unstaged = state.workingDirectory.filter { (name, content) ->
            currentFiles[name] != content && name !in state.stagingArea
        }
        val unstagedMsg = if (unstaged.isNotEmpty()) {
            "\nChanges not staged for commit:\n" +
                unstaged.keys.joinToString("\n") { "  modified: $it" }
        } else ""

        val message = branchInfo + staged + unstagedMsg +
            if (staged.isEmpty() && unstagedMsg.isEmpty()) "\nnothing to commit, working tree clean" else ""

        return GitResult.Success(state, message)
    }

    private fun log(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        val history = state.log()
        if (history.isEmpty()) {
            return GitResult.Success(state, "No commits yet")
        }

        val logMsg = history.joinToString("\n\n") { commit ->
            val branchLabels = state.branches.values
                .filter { it.commitId == commit.id }
                .joinToString(", ") { it.name }
            val refs = if (branchLabels.isNotEmpty()) " ($branchLabels)" else ""
            "commit ${commit.id}$refs\n    ${commit.message}"
        }

        return GitResult.Success(state, logMsg)
    }

    // ── Branching ──

    private fun createBranch(state: GitState, name: String): GitResult {
        requireInit(state)?.let { return it }

        if (name in state.branches) {
            return GitResult.Error("fatal: a branch named '$name' already exists")
        }

        val headCommitId = state.resolveHead()
            ?: return GitResult.Error("fatal: not a valid object name: 'HEAD'")

        val newBranches = state.branches.toMutableMap()
        newBranches[name] = Branch(name, headCommitId)

        return GitResult.Success(
            state.copy(branches = newBranches),
            "Created branch '$name' at ${headCommitId.take(7)}",
        )
    }

    private fun createBranchAt(state: GitState, name: String, commitRef: String): GitResult {
        requireInit(state)?.let { return it }
        if (name in state.branches) {
            return GitResult.Error("fatal: a branch named '$name' already exists")
        }
        val targetId = resolveRef(state, commitRef)
            ?: return GitResult.Error("fatal: not a valid object name: '$commitRef'")
        val newBranches = state.branches.toMutableMap()
        newBranches[name] = Branch(name, targetId)
        return GitResult.Success(
            state.copy(branches = newBranches),
            "Created branch '$name' at ${targetId.take(7)}",
        )
    }

    private fun deleteBranch(state: GitState, name: String): GitResult {
        requireInit(state)?.let { return it }

        if (name !in state.branches) {
            return GitResult.Error("error: branch '$name' not found")
        }
        if (state.currentBranchName() == name) {
            return GitResult.Error("error: cannot delete branch '$name' checked out at HEAD")
        }

        val newBranches = state.branches.toMutableMap()
        newBranches.remove(name)

        return GitResult.Success(
            state.copy(branches = newBranches),
            "Deleted branch $name",
        )
    }

    private fun listBranches(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        val current = state.currentBranchName()
        val list = state.branches.keys.sorted().joinToString("\n") { name ->
            if (name == current) "* $name" else "  $name"
        }
        return GitResult.Success(state, list)
    }

    // ── Checkout / Switch ──

    private fun checkout(state: GitState, ref: String): GitResult {
        requireInit(state)?.let { return it }

        // Check if it's a branch name
        if (ref in state.branches) {
            return switchToBranch(state, ref)
        }

        // Check if it's a commit ID (or prefix)
        val commitId = resolveRef(state, ref)
            ?: return GitResult.Error("error: pathspec '$ref' did not match any known ref")

        // Detached HEAD
        val files = state.commits[commitId]?.files ?: emptyMap()
        return GitResult.Success(
            state.copy(
                head = Head.Detached(commitId),
                workingDirectory = files,
                stagingArea = emptyMap(),
            ),
            "Note: switching to '$ref'.\nYou are in 'detached HEAD' state.",
        )
    }

    private fun checkoutNewBranch(state: GitState, name: String): GitResult {
        requireInit(state)?.let { return it }

        val createResult = createBranch(state, name)
        if (createResult is GitResult.Error) return createResult
        val stateWithBranch = (createResult as GitResult.Success).newState

        return switchToBranch(stateWithBranch, name)
    }

    private fun switchBranch(state: GitState, branchName: String): GitResult {
        requireInit(state)?.let { return it }

        if (branchName !in state.branches) {
            return GitResult.Error("fatal: invalid reference: '$branchName'")
        }
        return switchToBranch(state, branchName)
    }

    private fun switchCreate(state: GitState, branchName: String): GitResult {
        return checkoutNewBranch(state, branchName)
    }

    private fun switchToBranch(state: GitState, branchName: String): GitResult {
        val branch = state.branches[branchName]
            ?: return GitResult.Error("fatal: invalid reference: '$branchName'")

        val files = if (branch.commitId.isNotEmpty()) {
            state.commits[branch.commitId]?.files ?: emptyMap()
        } else emptyMap()

        return GitResult.Success(
            state.copy(
                head = Head.Attached(branchName),
                workingDirectory = files,
                stagingArea = emptyMap(),
            ),
            "Switched to branch '$branchName'",
        )
    }

    // ── Merging ──

    private fun merge(state: GitState, branchName: String): GitResult {
        requireInit(state)?.let { return it }

        val currentBranch = state.currentBranchName()
            ?: return GitResult.Error("Cannot merge in detached HEAD state")

        val targetBranch = state.branches[branchName]
            ?: return GitResult.Error("merge: $branchName - not something we can merge")

        val currentCommitId = state.resolveHead()!!
        val targetCommitId = targetBranch.commitId

        if (currentCommitId == targetCommitId) {
            return GitResult.Success(state, "Already up to date.")
        }

        // Fast-forward: target is ahead of current
        if (state.isAncestor(currentCommitId, targetCommitId)) {
            val newBranches = state.branches.toMutableMap()
            newBranches[currentBranch] = Branch(currentBranch, targetCommitId)
            val files = state.commits[targetCommitId]?.files ?: emptyMap()

            return GitResult.Success(
                state.copy(
                    branches = newBranches,
                    workingDirectory = files,
                    stagingArea = emptyMap(),
                ),
                "Fast-forward merge: ${currentCommitId.take(7)}..${targetCommitId.take(7)}",
            )
        }

        // 3-way merge: create a merge commit
        val mergeBase = state.findMergeBase(currentCommitId, targetCommitId)

        val currentFiles = state.commits[currentCommitId]?.files ?: emptyMap()
        val targetFiles = state.commits[targetCommitId]?.files ?: emptyMap()
        val baseFiles = mergeBase?.let { state.commits[it]?.files } ?: emptyMap()

        // Simple merge: combine files, detect conflicts
        val mergedFiles = mutableMapOf<String, String>()
        val allFileNames = (currentFiles.keys + targetFiles.keys + baseFiles.keys).toSet()
        val conflicts = mutableListOf<String>()

        for (fileName in allFileNames) {
            val base = baseFiles[fileName]
            val current = currentFiles[fileName]
            val target = targetFiles[fileName]

            when {
                current == target -> mergedFiles[fileName] = current ?: continue
                current == base -> mergedFiles[fileName] = target ?: continue
                target == base -> mergedFiles[fileName] = current ?: continue
                else -> {
                    // Conflict!
                    conflicts.add(fileName)
                    mergedFiles[fileName] = buildString {
                        appendLine("<<<<<<< HEAD")
                        appendLine(current ?: "")
                        appendLine("=======")
                        appendLine(target ?: "")
                        appendLine(">>>>>>> $branchName")
                    }
                }
            }
        }

        if (conflicts.isNotEmpty()) {
            // Leave conflicts in working directory for resolution
            return GitResult.Success(
                state.copy(
                    workingDirectory = mergedFiles,
                    stagingArea = emptyMap(),
                ),
                "CONFLICT (content): Merge conflict in ${conflicts.joinToString(", ")}\n" +
                    "Automatic merge failed; fix conflicts and then commit the result.",
            )
        }

        // No conflicts — create merge commit
        val mergeId = generateId()
        val mergeCommit = Commit(
            id = mergeId,
            message = "Merge branch '$branchName' into $currentBranch",
            parentIds = listOf(currentCommitId, targetCommitId),
            files = mergedFiles,
        )

        val newCommits = state.commits.toMutableMap()
        newCommits[mergeId] = mergeCommit
        val newBranches = state.branches.toMutableMap()
        newBranches[currentBranch] = Branch(currentBranch, mergeId)

        return GitResult.Success(
            state.copy(
                commits = newCommits,
                branches = newBranches,
                workingDirectory = mergedFiles,
                stagingArea = emptyMap(),
            ),
            "Merge made by the 'ort' strategy.",
        )
    }

    // ── Rebase ──

    private fun rebase(state: GitState, baseBranch: String): GitResult {
        requireInit(state)?.let { return it }

        val currentBranch = state.currentBranchName()
            ?: return GitResult.Error("Cannot rebase in detached HEAD state")

        val targetBranch = state.branches[baseBranch]
            ?: return GitResult.Error("fatal: invalid upstream '$baseBranch'")

        val currentCommitId = state.resolveHead()!!
        val targetCommitId = targetBranch.commitId

        if (state.isAncestor(currentCommitId, targetCommitId)) {
            // Already up to date — fast-forward
            val newBranches = state.branches.toMutableMap()
            newBranches[currentBranch] = Branch(currentBranch, targetCommitId)
            return GitResult.Success(
                state.copy(branches = newBranches),
                "Current branch $currentBranch is up to date.",
            )
        }

        val mergeBase = state.findMergeBase(currentCommitId, targetCommitId) ?: ""

        // Collect commits to replay (from current back to merge base, exclusive)
        val toReplay = mutableListOf<Commit>()
        var walkId: String? = currentCommitId
        while (walkId != null && walkId != mergeBase && walkId.isNotEmpty()) {
            val commit = state.commits[walkId] ?: break
            toReplay.add(commit)
            walkId = commit.parentIds.firstOrNull()
        }
        toReplay.reverse()

        // Replay each commit on top of target
        var newState = state
        var parentId = targetCommitId
        val newCommits = state.commits.toMutableMap()

        for (original in toReplay) {
            val replayedId = generateId()
            val replayed = original.copy(
                id = replayedId,
                parentIds = listOf(parentId),
            )
            newCommits[replayedId] = replayed
            parentId = replayedId
        }

        val newBranches = state.branches.toMutableMap()
        newBranches[currentBranch] = Branch(currentBranch, parentId)

        val files = newCommits[parentId]?.files ?: emptyMap()

        return GitResult.Success(
            state.copy(
                commits = newCommits,
                branches = newBranches,
                workingDirectory = files,
                stagingArea = emptyMap(),
            ),
            "Successfully rebased and updated refs/heads/$currentBranch.",
        )
    }

    private fun rebaseInteractive(state: GitState, baseBranch: String): GitResult {
        // For the game, interactive rebase is simplified to the same as rebase
        // with a message indicating it is interactive mode
        return rebase(state, baseBranch)
    }

    // ── Cherry-pick ──

    private fun cherryPick(state: GitState, commitId: String): GitResult {
        requireInit(state)?.let { return it }

        val resolvedId = resolveRef(state, commitId)
            ?: return GitResult.Error("fatal: bad object $commitId")

        val original = state.commits[resolvedId]
            ?: return GitResult.Error("fatal: bad object $commitId")

        val currentCommitId = state.resolveHead()
            ?: return GitResult.Error("HEAD is not pointing to a valid commit")

        val newId = generateId()
        val pickedCommit = Commit(
            id = newId,
            message = original.message,
            parentIds = listOf(currentCommitId),
            files = original.files,
        )

        val newCommits = state.commits.toMutableMap()
        newCommits[newId] = pickedCommit

        val newBranches = state.branches.toMutableMap()
        val newHead: Head
        when (val h = state.head) {
            is Head.Attached -> {
                newBranches[h.branchName] = Branch(h.branchName, newId)
                newHead = h
            }
            is Head.Detached -> {
                newHead = Head.Detached(newId)
            }
        }

        return GitResult.Success(
            state.copy(
                commits = newCommits,
                branches = newBranches,
                head = newHead,
                workingDirectory = pickedCommit.files,
                stagingArea = emptyMap(),
            ),
            "[${state.currentBranchName() ?: newId.take(7)} $newId] ${original.message}",
        )
    }

    // ── Reset ──

    private fun reset(state: GitState, ref: String, mode: ResetMode): GitResult {
        requireInit(state)?.let { return it }

        val targetId = resolveRef(state, ref)
            ?: return GitResult.Error("fatal: ambiguous argument '$ref'")

        val targetCommit = state.commits[targetId]
            ?: return GitResult.Error("fatal: ambiguous argument '$ref'")

        val newBranches = state.branches.toMutableMap()
        when (val h = state.head) {
            is Head.Attached -> {
                newBranches[h.branchName] = Branch(h.branchName, targetId)
            }
            is Head.Detached -> {
                // Reset in detached HEAD just moves HEAD
            }
        }

        val newHead = when (val h = state.head) {
            is Head.Attached -> h
            is Head.Detached -> Head.Detached(targetId)
        }

        val targetFiles = targetCommit.files

        val newState = when (mode) {
            ResetMode.SOFT -> state.copy(
                branches = newBranches,
                head = newHead,
                // Keep staging area and working directory as-is
            )
            ResetMode.MIXED -> state.copy(
                branches = newBranches,
                head = newHead,
                stagingArea = emptyMap(),
                // Keep working directory as-is
            )
            ResetMode.HARD -> state.copy(
                branches = newBranches,
                head = newHead,
                stagingArea = emptyMap(),
                workingDirectory = targetFiles,
            )
        }

        return GitResult.Success(newState, "HEAD is now at ${targetId.take(7)} ${targetCommit.message}")
    }

    // ── Revert ──

    private fun revert(state: GitState, commitId: String): GitResult {
        requireInit(state)?.let { return it }

        val resolvedId = resolveRef(state, commitId)
            ?: return GitResult.Error("fatal: bad object $commitId")

        val targetCommit = state.commits[resolvedId]
            ?: return GitResult.Error("fatal: bad object $commitId")

        val currentFiles = state.headCommit()?.files ?: emptyMap()
        val parentFiles = targetCommit.parentIds.firstOrNull()
            ?.let { state.commits[it]?.files } ?: emptyMap()

        // Revert = apply the inverse of the target commit's changes
        val revertedFiles = currentFiles.toMutableMap()
        for ((name, content) in targetCommit.files) {
            val parentContent = parentFiles[name]
            if (parentContent != null) {
                revertedFiles[name] = parentContent
            } else {
                revertedFiles.remove(name)
            }
        }

        val revertId = generateId()
        val revertCommit = Commit(
            id = revertId,
            message = "Revert \"${targetCommit.message}\"",
            parentIds = listOfNotNull(state.resolveHead()),
            files = revertedFiles,
        )

        val newCommits = state.commits.toMutableMap()
        newCommits[revertId] = revertCommit

        val newBranches = state.branches.toMutableMap()
        val newHead: Head
        when (val h = state.head) {
            is Head.Attached -> {
                newBranches[h.branchName] = Branch(h.branchName, revertId)
                newHead = h
            }
            is Head.Detached -> {
                newHead = Head.Detached(revertId)
            }
        }

        return GitResult.Success(
            state.copy(
                commits = newCommits,
                branches = newBranches,
                head = newHead,
                workingDirectory = revertedFiles,
                stagingArea = emptyMap(),
            ),
            "[${state.currentBranchName() ?: revertId.take(7)} $revertId] Revert \"${targetCommit.message}\"",
        )
    }

    // ── Stash ──

    private fun stash(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        val currentFiles = state.headCommit()?.files ?: emptyMap()
        val hasChanges = state.workingDirectory != currentFiles || state.stagingArea.isNotEmpty()

        if (!hasChanges) {
            return GitResult.Error("No local changes to save")
        }

        val stashEntry = state.workingDirectory
        val newStash = state.stash.toMutableList()
        newStash.add(0, stashEntry)

        return GitResult.Success(
            state.copy(
                stash = newStash,
                workingDirectory = currentFiles,
                stagingArea = emptyMap(),
            ),
            "Saved working directory and index state WIP on ${state.currentBranchName() ?: "HEAD"}",
        )
    }

    private fun stashPop(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        if (state.stash.isEmpty()) {
            return GitResult.Error("No stash entries found")
        }

        val restored = state.stash.first()
        val newStash = state.stash.drop(1)

        return GitResult.Success(
            state.copy(
                stash = newStash,
                workingDirectory = restored,
            ),
            "Restored working directory from stash",
        )
    }

    private fun stashList(state: GitState): GitResult {
        requireInit(state)?.let { return it }

        if (state.stash.isEmpty()) {
            return GitResult.Success(state, "No stash entries")
        }

        val list = state.stash.mapIndexed { index, _ ->
            "stash@{$index}: WIP"
        }.joinToString("\n")

        return GitResult.Success(state, list)
    }

    // ── Recovery ──

    private fun reflog(state: GitState): GitResult {
        requireInit(state)?.let { return it }
        // In the game, reflog shows all commits (including "orphaned" ones)
        val all = state.commits.values.sortedByDescending { it.timestamp }
        if (all.isEmpty()) {
            return GitResult.Success(state, "No reflog entries")
        }
        val msg = all.joinToString("\n") { "${it.id.take(7)} ${it.message}" }
        return GitResult.Success(state, msg)
    }

    private fun help(): GitResult {
        return GitResult.Success(
            GitState(),
            """
            Available commands:
              git init                    Initialize a repository
              git add <file>              Stage a file
              git add .                   Stage all changes
              git commit -m "message"     Create a commit
              git status                  Show working tree status
              git log                     Show commit history
              git branch <name>           Create a branch
              git branch -d <name>        Delete a branch
              git branch                  List branches
              git checkout <ref>          Switch to branch or commit
              git checkout -b <name>      Create and switch to branch
              git switch <branch>         Switch to branch
              git switch -c <branch>      Create and switch to branch
              git merge <branch>          Merge a branch
              git rebase <branch>         Rebase onto a branch
              git cherry-pick <id>        Copy a commit
              git reset [--soft|--mixed|--hard] <ref>
              git revert <id>             Revert a commit
              git stash                   Stash changes
              git stash pop               Restore stashed changes
              git reflog                  Show all HEAD movements
            """.trimIndent(),
        )
    }

    // ── Helpers ──

    private fun requireInit(state: GitState): GitResult.Error? {
        return if (!state.isInitialized) {
            GitResult.Error("fatal: not a git repository (or any of the parent directories)")
        } else null
    }

    /**
     * Resolves a ref (branch name, commit ID, or HEAD~N) to a full commit ID.
     */
    private fun resolveRef(state: GitState, ref: String): String? {
        // Branch name
        state.branches[ref]?.let { return it.commitId }

        // HEAD~N syntax
        val headTildeMatch = Regex("^HEAD~(\\d+)$").find(ref)
        if (headTildeMatch != null) {
            val n = headTildeMatch.groupValues[1].toInt()
            var current = state.resolveHead() ?: return null
            repeat(n) {
                val commit = state.commits[current] ?: return null
                current = commit.parentIds.firstOrNull() ?: return null
            }
            return current
        }

        // Full or partial commit ID
        if (ref in state.commits) return ref
        val matches = state.commits.keys.filter { it.startsWith(ref) }
        return matches.singleOrNull()
    }

    private fun generateId(): String {
        val chars = "0123456789abcdef"
        return (1..7).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
