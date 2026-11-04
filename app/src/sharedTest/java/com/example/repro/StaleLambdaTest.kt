package com.example.repro

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaleLambdaTest {
    @get:Rule
    val rule = createComposeRule()

    private var flag by mutableStateOf(false)
    private var count by mutableIntStateOf(0)

    @Test
    fun lambdaShowsTheValueItCaptured() {
        rule.setContent { Repro(flag, count) }
        assertEquals("flag=false count=0", shownText())

        flag = true

        assertEquals("flag=true count=0", shownText())
    }

    @Test
    fun laterChangesStillReachTheLambda() {
        rule.setContent { Repro(flag, count) }
        flag = true
        rule.waitForIdle()

        count = 1

        assertEquals("flag=true count=1", shownText())
    }

    // Control: B reads the local but calls `next` without a branch. Passes.
    @Test
    fun withoutTheBranch() {
        rule.setContent {
            CompositionLocalProvider(LocalFlag provides flag) {
                val captured = LocalFlag.current
                BoxWithConstraints {
                    PagesWithoutBranch(next = { BasicText("flag=$captured count=$count") })
                }
            }
        }
        flag = true

        assertEquals("flag=true count=0", shownText())
    }

    // Control: `next` is created in the root composition instead of subcomposition A. Passes.
    @Test
    fun withoutTheOuterSubcomposition() {
        rule.setContent {
            CompositionLocalProvider(LocalFlag provides flag) {
                val captured = LocalFlag.current
                Pages(next = { BasicText("flag=$captured count=$count") })
            }
        }
        flag = true

        assertEquals("flag=true count=0", shownText())
    }

    @Composable
    private fun PagesWithoutBranch(next: @Composable () -> Unit) {
        BoxWithConstraints {
            LocalFlag.current
            next()
        }
    }

    private fun shownText(): String {
        rule.waitForIdle()
        return rule.onAllNodesWithText("flag=", substring = true).fetchSemanticsNodes()
            .joinToString { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString() }
    }
}
