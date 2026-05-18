package com.app.gitquest.engine

import com.app.gitquest.engine.parser.CommandParser
import com.app.gitquest.engine.parser.ParseResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GitEngineTest {

    private lateinit var engine: GitEngine
    private lateinit var parser: CommandParser

    @BeforeEach
    fun setup() {
        engine = GitEngine()
        parser = CommandParser()
    }

    /** Helper: parse and execute a command string against a state */
    private fun exec(state: GitState, command: String): GitResult {
        val parsed = parser.parse(command)
        assertTrue(parsed is ParseResult.Ok, "Failed to parse: $command")
        return engine.execute(state, (parsed as ParseResult.Ok).command)
    }

    /** Helper: exec and assert success, return new state */
    private fun execOk(state: GitState, command: String): GitState {
        val result = exec(state, command)
        assertTrue(result is GitResult.Success, "Expected success for '$command' but got: ${(result as? GitResult.Error)?.message}")
        return (result as GitResult.Success).newState
    }

    /** Helper: exec and assert error */
    private fun execErr(state: GitState, command: String): String {
        val result = exec(state, command)
        assertTrue(result is GitResult.Error, "Expected error for '$command' but got success")
        return (result as GitResult.Error).message
    }

    /** Creates an initialized state with working directory files */
    private fun initializedState(files: Map<String, String> = emptyMap()): GitState {
        return GitState(
            isInitialized = true,
            branches = mapOf("main" to Branch("main", "")),
            head = Head.Attached("main"),
            workingDirectory = files,
        )
    }

    /** Creates a state with one commit on main */
    private fun stateWithOneCommit(): GitState {
        val commit = Commit("abc1234", "Initial commit", emptyList(), files = mapOf("file.txt" to "hello"))
        return GitState(
            isInitialized = true,
            commits = mapOf("abc1234" to commit),
            branches = mapOf("main" to Branch("main", "abc1234")),
            head = Head.Attached("main"),
            workingDirectory = mapOf("file.txt" to "hello"),
        )
    }

    @Nested
    inner class InitTests {
        @Test
        fun `init creates empty repo`() {
            val state = GitState()
            val result = execOk(state, "git init")
            assertTrue(result.isInitialized)
            assertEquals("main", result.currentBranchName())
        }

        @Test
        fun `double init shows error`() {
            val state = initializedState()
            val error = execErr(state, "git init")
            assertTrue(error.contains("Reinitialized"))
        }
    }

    @Nested
    inner class AddAndCommitTests {
        @Test
        fun `add stages a file`() {
            val state = initializedState(mapOf("file.txt" to "content"))
            val result = execOk(state, "git add file.txt")
            assertEquals("content", result.stagingArea["file.txt"])
        }

        @Test
        fun `add nonexistent file returns error`() {
            val state = initializedState()
            val error = execErr(state, "git add ghost.txt")
            assertTrue(error.contains("did not match"))
        }

        @Test
        fun `add all stages everything`() {
            val state = initializedState(mapOf("a.txt" to "1", "b.txt" to "2"))
            val result = execOk(state, "git add .")
            assertEquals(2, result.stagingArea.size)
        }

        @Test
        fun `commit creates a commit node`() {
            var state = initializedState(mapOf("file.txt" to "hello"))
            state = execOk(state, "git add file.txt")
            state = execOk(state, "git commit -m \"Initial commit\"")

            assertEquals(1, state.commits.size)
            assertTrue(state.stagingArea.isEmpty())
            val commit = state.commits.values.first()
            assertEquals("Initial commit", commit.message)
            assertEquals("hello", commit.files["file.txt"])
        }

        @Test
        fun `commit with nothing staged returns error`() {
            val state = initializedState()
            val error = execErr(state, "git commit -m \"empty\"")
            assertTrue(error.contains("nothing to commit"))
        }

        @Test
        fun `second commit has parent`() {
            var state = initializedState(mapOf("file.txt" to "v1"))
            state = execOk(state, "git add file.txt")
            state = execOk(state, "git commit -m \"first\"")

            state = state.copy(workingDirectory = mapOf("file.txt" to "v2"))
            state = execOk(state, "git add file.txt")
            state = execOk(state, "git commit -m \"second\"")

            assertEquals(2, state.commits.size)
            val second = state.headCommit()!!
            assertEquals("second", second.message)
            assertEquals(1, second.parentIds.size)
        }
    }

    @Nested
    inner class BranchTests {
        @Test
        fun `create branch adds new pointer`() {
            val state = stateWithOneCommit()
            val result = execOk(state, "git branch feature")
            assertTrue("feature" in result.branches)
            assertEquals("abc1234", result.branches["feature"]!!.commitId)
        }

        @Test
        fun `duplicate branch name returns error`() {
            val state = stateWithOneCommit()
            val s2 = execOk(state, "git branch feature")
            val error = execErr(s2, "git branch feature")
            assertTrue(error.contains("already exists"))
        }

        @Test
        fun `delete branch removes pointer`() {
            val state = stateWithOneCommit()
            val s2 = execOk(state, "git branch feature")
            val s3 = execOk(s2, "git branch -d feature")
            assertFalse("feature" in s3.branches)
        }

        @Test
        fun `cannot delete current branch`() {
            val state = stateWithOneCommit()
            val error = execErr(state, "git branch -d main")
            assertTrue(error.contains("cannot delete"))
        }

        @Test
        fun `checkout switches branch`() {
            val state = stateWithOneCommit()
            val s2 = execOk(state, "git branch feature")
            val s3 = execOk(s2, "git checkout feature")
            assertEquals("feature", s3.currentBranchName())
        }

        @Test
        fun `checkout -b creates and switches`() {
            val state = stateWithOneCommit()
            val result = execOk(state, "git checkout -b feature")
            assertEquals("feature", result.currentBranchName())
            assertTrue("feature" in result.branches)
        }

        @Test
        fun `checkout commit id detaches HEAD`() {
            val state = stateWithOneCommit()
            val result = execOk(state, "git checkout abc1234")
            assertTrue(result.head is Head.Detached)
        }
    }

    @Nested
    inner class MergeTests {
        @Test
        fun `fast forward merge advances pointer`() {
            var state = stateWithOneCommit()
            state = execOk(state, "git checkout -b feature")

            // Add a commit on feature
            state = state.copy(workingDirectory = state.workingDirectory + ("new.txt" to "data"))
            state = execOk(state, "git add new.txt")
            state = execOk(state, "git commit -m \"feature work\"")

            val featureCommitId = state.resolveHead()!!

            // Switch back to main and merge
            state = execOk(state, "git checkout main")
            state = execOk(state, "git merge feature")

            // Main should now point to the feature commit (fast-forward)
            assertEquals(featureCommitId, state.branches["main"]!!.commitId)
        }

        @Test
        fun `three-way merge creates merge commit`() {
            var state = stateWithOneCommit()

            // Create divergent branches
            state = execOk(state, "git checkout -b feature")
            state = state.copy(workingDirectory = state.workingDirectory + ("feature.txt" to "feature"))
            state = execOk(state, "git add feature.txt")
            state = execOk(state, "git commit -m \"feature commit\"")

            state = execOk(state, "git checkout main")
            state = state.copy(workingDirectory = state.workingDirectory + ("main.txt" to "main"))
            state = execOk(state, "git add main.txt")
            state = execOk(state, "git commit -m \"main commit\"")

            // Merge feature into main
            state = execOk(state, "git merge feature")

            // Should have created a merge commit
            val mergeCommit = state.headCommit()!!
            assertTrue(mergeCommit.isMergeCommit)
            assertEquals(2, mergeCommit.parentIds.size)
        }
    }

    @Nested
    inner class ResetTests {
        @Test
        fun `reset hard moves branch and clears everything`() {
            var state = stateWithOneCommit()
            val firstCommitId = state.resolveHead()!!

            state = state.copy(workingDirectory = state.workingDirectory + ("new.txt" to "v2"))
            state = execOk(state, "git add new.txt")
            state = execOk(state, "git commit -m \"second\"")

            state = execOk(state, "git reset --hard $firstCommitId")

            assertEquals(firstCommitId, state.resolveHead())
            assertFalse("new.txt" in state.workingDirectory)
            assertTrue(state.stagingArea.isEmpty())
        }

        @Test
        fun `reset soft keeps staging and working dir`() {
            var state = stateWithOneCommit()
            val firstCommitId = state.resolveHead()!!

            state = state.copy(workingDirectory = state.workingDirectory + ("new.txt" to "data"))
            state = execOk(state, "git add new.txt")
            state = execOk(state, "git commit -m \"second\"")

            // Add something to staging before reset
            state = state.copy(
                workingDirectory = state.workingDirectory + ("extra.txt" to "extra"),
                stagingArea = mapOf("extra.txt" to "extra"),
            )
            state = execOk(state, "git reset --soft $firstCommitId")

            assertEquals(firstCommitId, state.resolveHead())
            assertTrue(state.stagingArea.isNotEmpty()) // preserved
        }
    }

    @Nested
    inner class StashTests {
        @Test
        fun `stash saves and clears working changes`() {
            var state = stateWithOneCommit()
            state = state.copy(workingDirectory = state.workingDirectory + ("new.txt" to "wip"))
            state = execOk(state, "git stash")

            assertFalse("new.txt" in state.workingDirectory)
            assertEquals(1, state.stash.size)
        }

        @Test
        fun `stash pop restores changes`() {
            var state = stateWithOneCommit()
            state = state.copy(workingDirectory = state.workingDirectory + ("new.txt" to "wip"))
            state = execOk(state, "git stash")
            state = execOk(state, "git stash pop")

            assertEquals("wip", state.workingDirectory["new.txt"])
            assertTrue(state.stash.isEmpty())
        }
    }

    @Nested
    inner class ParserTests {
        @Test
        fun `parses basic commands`() {
            assertTrue(parser.parse("git init") is ParseResult.Ok)
            assertTrue(parser.parse("git add file.txt") is ParseResult.Ok)
            assertTrue(parser.parse("git commit -m \"hello\"") is ParseResult.Ok)
            assertTrue(parser.parse("git branch feature") is ParseResult.Ok)
            assertTrue(parser.parse("git checkout -b new") is ParseResult.Ok)
            assertTrue(parser.parse("git merge main") is ParseResult.Ok)
        }

        @Test
        fun `rejects non-git commands`() {
            val result = parser.parse("ls -la")
            assertTrue(result is ParseResult.Error)
        }

        @Test
        fun `parses reset with modes`() {
            val soft = parser.parse("git reset --soft HEAD~1")
            assertTrue(soft is ParseResult.Ok)
            val cmd = (soft as ParseResult.Ok).command as GitCommand.Reset
            assertEquals(ResetMode.SOFT, cmd.mode)
        }

        @Test
        fun `empty input returns empty`() {
            assertTrue(parser.parse("") is ParseResult.Empty)
        }
    }

    @Nested
    inner class GoalCheckerTests {
        private val checker = GoalChecker()

        @Test
        fun `simple commit goal passes`() {
            var state = initializedState(mapOf("file.txt" to "data"))
            state = execOk(state, "git add file.txt")
            state = execOk(state, "git commit -m \"Initial commit\"")

            val goal = LevelGoal(
                commitCount = 1,
                headOnBranch = "main",
                requiredCommitMessages = listOf("Initial commit"),
            )

            val result = checker.check(state, goal)
            assertTrue(result.isComplete)
            assertEquals(1f, result.progress)
        }

        @Test
        fun `incomplete goal shows failures`() {
            val state = initializedState()
            val goal = LevelGoal(
                commitCount = 3,
                headOnBranch = "main",
            )

            val result = checker.check(state, goal)
            assertFalse(result.isComplete)
            assertTrue(result.failures.any { it.contains("commits") })
        }
    }
}
