package com.app.gitquest.engine.parser

import com.app.gitquest.engine.GitCommand
import com.app.gitquest.engine.ResetMode

/**
 * Parses raw user input from the terminal into typed GitCommand objects.
 * Returns null if the input doesn't match any known command pattern.
 */
class CommandParser {

    fun parse(input: String): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty())
            return ParseResult.Empty

        // Strip leading "git " if present
        val command =
            if (trimmed.startsWith("git ")) {
                trimmed.removePrefix("git ").trim()
            } else {
                return ParseResult.Error("Commands must start with 'git'. Try: git $trimmed")
            }

        return when {
            command == "init" -> ParseResult.Ok(GitCommand.Init)
            command == "add ." || command == "add -A" -> ParseResult.Ok(GitCommand.AddAll)
            command.startsWith("add ") -> parseAdd(command)
            command.startsWith("commit") -> parseCommit(command)
            command == "status" -> ParseResult.Ok(GitCommand.Status)
            command == "log" || command == "log --oneline" -> ParseResult.Ok(GitCommand.Log)
            command == "branch" -> ParseResult.Ok(GitCommand.ListBranches)
            command.startsWith("branch -d ") || command.startsWith("branch -D ") -> parseDeleteBranch(command)
            command.startsWith("branch ") -> parseCreateBranch(command)
            command.startsWith("checkout -b ") -> parseCheckoutNewBranch(command)
            command.startsWith("checkout ") -> parseCheckout(command)
            command.startsWith("switch -c ") -> parseSwitchCreate(command)
            command.startsWith("switch ") -> parseSwitch(command)
            command.startsWith("merge ") -> parseMerge(command)
            command.startsWith("rebase -i ") -> parseRebaseInteractive(command)
            command.startsWith("rebase ") -> parseRebase(command)
            command.startsWith("cherry-pick ") -> parseCherryPick(command)
            command.startsWith("reset") -> parseReset(command)
            command.startsWith("revert ") -> parseRevert(command)
            command == "stash" || command == "stash push" -> ParseResult.Ok(GitCommand.Stash)
            command == "stash pop" -> ParseResult.Ok(GitCommand.StashPop)
            command == "stash list" -> ParseResult.Ok(GitCommand.StashList)
            command == "reflog" -> ParseResult.Ok(GitCommand.Reflog)
            command == "help" || command == "--help" -> ParseResult.Ok(GitCommand.Help)
            else -> ParseResult.Error("Unknown command: git $command. Type 'git help' for available commands.")
        }
    }

    private fun parseAdd(command: String): ParseResult {
        val fileName = command.removePrefix("add ").trim()
        if (fileName.isEmpty()) return ParseResult.Error("Specify a file: git add <filename>")
        return ParseResult.Ok(GitCommand.Add(fileName))
    }

    private fun parseCommit(command: String): ParseResult {
        val regex = Regex("""commit\s+-m\s+["'](.+?)["']""")
        val match = regex.find(command)
            ?: return ParseResult.Error("Usage: git commit -m \"your message\"")
        return ParseResult.Ok(GitCommand.CommitCmd(match.groupValues[1]))
    }

    private fun parseCreateBranch(command: String): ParseResult {
        val parts = command.removePrefix("branch ").trim().split("\\s+".toRegex())
        val name = parts.getOrNull(0)
        if (name.isNullOrEmpty()) return ParseResult.Error("Specify a branch name: git branch <n>")
        if (name.contains(" ")) return ParseResult.Error("Branch names cannot contain spaces")

        // git branch <name> <commitRef> — create branch at a specific commit
        val commitRef = parts.getOrNull(1)
        return if (commitRef != null) {
            ParseResult.Ok(GitCommand.CreateBranchAt(name, commitRef))
        } else {
            ParseResult.Ok(GitCommand.CreateBranch(name))
        }
    }

    private fun parseDeleteBranch(command: String): ParseResult {
        val name = command.removePrefix("branch -d ").removePrefix("branch -D ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a branch: git branch -d <n>")
        return ParseResult.Ok(GitCommand.DeleteBranch(name))
    }

    private fun parseCheckoutNewBranch(command: String): ParseResult {
        val name = command.removePrefix("checkout -b ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a branch name: git checkout -b <n>")
        return ParseResult.Ok(GitCommand.CheckoutNewBranch(name))
    }

    private fun parseCheckout(command: String): ParseResult {
        val ref = command.removePrefix("checkout ").trim()
        if (ref.isEmpty()) return ParseResult.Error("Specify a branch or commit: git checkout <ref>")
        return ParseResult.Ok(GitCommand.Checkout(ref))
    }

    private fun parseSwitch(command: String): ParseResult {
        val name = command.removePrefix("switch ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a branch: git switch <branch>")
        return ParseResult.Ok(GitCommand.Switch(name))
    }

    private fun parseSwitchCreate(command: String): ParseResult {
        val name = command.removePrefix("switch -c ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a branch name: git switch -c <n>")
        return ParseResult.Ok(GitCommand.SwitchCreate(name))
    }

    private fun parseMerge(command: String): ParseResult {
        val name = command.removePrefix("merge ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a branch: git merge <branch>")
        return ParseResult.Ok(GitCommand.Merge(name))
    }

    private fun parseRebase(command: String): ParseResult {
        val name = command.removePrefix("rebase ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a base: git rebase <branch>")
        return ParseResult.Ok(GitCommand.Rebase(name))
    }

    private fun parseRebaseInteractive(command: String): ParseResult {
        val name = command.removePrefix("rebase -i ").trim()
        if (name.isEmpty()) return ParseResult.Error("Specify a base: git rebase -i <branch>")
        return ParseResult.Ok(GitCommand.RebaseInteractive(name))
    }

    private fun parseCherryPick(command: String): ParseResult {
        val id = command.removePrefix("cherry-pick ").trim()
        if (id.isEmpty()) return ParseResult.Error("Specify a commit: git cherry-pick <commit-id>")
        return ParseResult.Ok(GitCommand.CherryPick(id))
    }

    private fun parseReset(command: String): ParseResult {
        val parts = command.removePrefix("reset").trim().split("\\s+".toRegex())
        var mode = ResetMode.MIXED
        var ref = "HEAD~1"
        for (part in parts) {
            when (part) {
                "--soft" -> mode = ResetMode.SOFT
                "--mixed" -> mode = ResetMode.MIXED
                "--hard" -> mode = ResetMode.HARD
                "" -> {}
                else -> ref = part
            }
        }
        return ParseResult.Ok(GitCommand.Reset(ref, mode))
    }

    private fun parseRevert(command: String): ParseResult {
        val id = command.removePrefix("revert ").trim()
        if (id.isEmpty()) return ParseResult.Error("Specify a commit: git revert <commit-id>")
        return ParseResult.Ok(GitCommand.Revert(id))
    }
}

sealed interface ParseResult {
    data class Ok(val command: GitCommand) : ParseResult
    data class Error(val message: String) : ParseResult
    data object Empty : ParseResult
}