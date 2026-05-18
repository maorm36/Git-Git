package com.app.gitquest.ui.visualizer

import com.app.gitquest.engine.Commit
import com.app.gitquest.engine.GitState
import com.app.gitquest.engine.Head

data class PositionedNode(
    val commitId: String,
    val message: String,
    val lane: Int,
    val row: Int,
    val parentIds: List<String>,
    val isMergeCommit: Boolean,
    val branchLabels: List<String>,
    val isHead: Boolean,
    val isDetachedHead: Boolean,
)

data class GraphLayout(
    val nodes: List<PositionedNode>,
    val laneCount: Int,
    val rowCount: Int,
    val nodeMap: Map<String, PositionedNode>,
)

class GraphLayoutEngine {

    fun layout(state: GitState): GraphLayout {
        if (state.commits.isEmpty()) {
            return GraphLayout(emptyList(), 0, 0, emptyMap())
        }

        val sorted = topologicalSort(state)
        val laneAssignment = assignLanes(sorted, state)
        val branchPointers = state.branches.values
            .groupBy { it.commitId }
            .mapValues { (_, branches) -> branches.map { it.name } }
        val headCommitId = state.resolveHead()
        val isDetached = state.head is Head.Detached

        val nodes = sorted.mapIndexed { row, commit ->
            PositionedNode(
                commitId = commit.id,
                message = commit.message,
                lane = laneAssignment[commit.id] ?: 0,
                row = row,
                parentIds = commit.parentIds,
                isMergeCommit = commit.isMergeCommit,
                branchLabels = branchPointers[commit.id] ?: emptyList(),
                isHead = commit.id == headCommitId,
                isDetachedHead = commit.id == headCommitId && isDetached,
            )
        }

        val nodeMap = nodes.associateBy { it.commitId }
        val laneCount = (laneAssignment.values.maxOrNull() ?: 0) + 1

        return GraphLayout(nodes, laneCount, sorted.size, nodeMap)
    }

    private fun topologicalSort(state: GitState): List<Commit> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<Commit>()
        val tips = state.branches.values.map { it.commitId }.distinct().toMutableList()
        val headId = state.resolveHead()
        if (headId != null && headId !in tips) tips.add(0, headId)
        val queue = ArrayDeque<String>()
        tips.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id.isEmpty() || id in visited) continue
            visited.add(id)
            val commit = state.commits[id] ?: continue
            result.add(commit)
            commit.parentIds.forEach { queue.add(it) }
        }
        return result.sortedByDescending { it.timestamp }
    }

    private fun assignLanes(sorted: List<Commit>, state: GitState): Map<String, Int> {
        val assignment = mutableMapOf<String, Int>()
        var nextLane = 0

        val mainBranchName = if ("main" in state.branches) "main" else state.branches.keys.firstOrNull()
        val mainCommits = if (mainBranchName != null) {
            collectAncestors(state.branches[mainBranchName]!!.commitId, state)
        } else emptySet()

        sorted.filter { it.id in mainCommits }.forEach { assignment[it.id] = 0 }
        nextLane = 1

        for (branch in state.branches.values.filter { it.name != mainBranchName }.sortedBy { it.name }) {
            val branchCommits = collectAncestors(branch.commitId, state)
            val unassigned = branchCommits.filter { it !in assignment }
            if (unassigned.isNotEmpty()) {
                val lane = nextLane++
                unassigned.forEach { id -> if (id !in assignment) assignment[id] = lane }
            }
        }

        sorted.filter { it.id !in assignment }.forEach { assignment[it.id] = 0 }
        return assignment
    }

    private fun collectAncestors(tipId: String, state: GitState): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(tipId)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id.isEmpty() || id in result) continue
            result.add(id)
            state.commits[id]?.parentIds?.forEach { queue.add(it) }
        }
        return result
    }
}