package com.app.gitquest.engine

/**
 * Sealed hierarchy of all Git commands the GitEngine understands.
 * CommandParser converts raw string input into these typed git commands.
 * GitEngine.execute() pattern-matches on them.
 */
sealed interface GitCommand {

    // ── Repository Setup ──
    data object Init : GitCommand

    // ── Staging & Committing ──
    data class Add(val fileName: String) : GitCommand
    data object AddAll : GitCommand
    data class CommitCmd(val message: String) : GitCommand
    data object Status : GitCommand
    data object Log : GitCommand

    // ── Branching ──
    data class CreateBranch(val name: String) : GitCommand
    data class CreateBranchAt(val name: String, val commitRef: String) : GitCommand
    data class DeleteBranch(val name: String) : GitCommand
    data object ListBranches : GitCommand
    data class Checkout(val ref: String) : GitCommand
    data class CheckoutNewBranch(val name: String) : GitCommand
    data class Switch(val branchName: String) : GitCommand
    data class SwitchCreate(val branchName: String) : GitCommand

    // ── Merging ──
    data class Merge(val branchName: String) : GitCommand

    // ── Rewriting History ──
    data class Rebase(val baseBranch: String) : GitCommand
    data class RebaseInteractive(val baseBranch: String) : GitCommand
    data class CherryPick(val commitId: String) : GitCommand
    data class Reset(val ref: String, val mode: ResetMode) : GitCommand
    data class Revert(val commitId: String) : GitCommand

    // ── Stash ──
    data object Stash : GitCommand
    data object StashPop : GitCommand
    data object StashList : GitCommand

    // ── Recovery ──
    data object Reflog : GitCommand

    // ── Display Only (no state change) ──
    data object Help : GitCommand
}

enum class ResetMode {
    SOFT,
    MIXED,
    HARD,
}