package com.example.repro

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalFlag = compositionLocalOf { false }

/**
 * The root composition provides [flag] and reads it back. Subcomposition A (the outer
 * BoxWithConstraints) creates `next`, which captures what the root read. Subcomposition B (the
 * inner BoxWithConstraints) reads the same local and calls `next` in a branch that depends on it.
 */
@Composable
fun Repro(flag: Boolean, count: Int) {
    CompositionLocalProvider(LocalFlag provides flag) {
        val captured = LocalFlag.current
        BoxWithConstraints { // subcomposition A
            Pages(next = { BasicText("flag=$captured count=$count") })
        }
    }
}

@Composable
fun Pages(next: @Composable () -> Unit) {
    BoxWithConstraints { // subcomposition B
        if (LocalFlag.current) Column { next() } else next()
    }
}
