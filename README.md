# A composable lambda keeps stale captured values after a nested subcomposition calls it first

A `@Composable` lambda created in one subcomposition and called from a nested subcomposition keeps showing the values it captured before a change, for good. It happens when, in one frame, the nested subcomposition recomposes first (it read a `CompositionLocal` that changed) and calls the lambda from a newly inserted group, and only then does the outer subcomposition recompose and update the lambda. The update's invalidation of the new call site is ignored and the lambda stops tracking any scope, so later changes to what it captures never reach the screen either.

We hit it through Material3 `Scaffold`: a screen read a `CompositionLocal` above the `Scaffold` and passed a lambda capturing what it read into a component in the `Scaffold` content, and that component read the same local inside its `BoxWithConstraints` to choose between one column and two pages. Material3 isn't needed. Two nested `BoxWithConstraints` reproduce it.

Issue: _(link added once filed)_

## The code

[`Repro.kt`](app/src/main/java/com/example/repro/Repro.kt):

```kotlin
val LocalFlag = compositionLocalOf { false }

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
```

[`StaleLambdaTest.kt`](app/src/sharedTest/java/com/example/repro/StaleLambdaTest.kt) sets `flag = true`, then `count = 1`, and reads the text.

## Reproduction

Needs JDK 17 or later and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`).

```
./gradlew :app:testDebugUnitTest
```

```
StaleLambdaTest > lambdaShowsTheValueItCaptured FAILED
    org.junit.ComparisonFailure: expected:<flag=[tru]e count=0> but was:<flag=[fals]e count=0>
StaleLambdaTest > withoutTheBranch PASSED
StaleLambdaTest > withoutTheOuterSubcomposition PASSED
StaleLambdaTest > laterChangesStillReachTheLambda FAILED
    org.junit.ComparisonFailure: expected:<flag=[true count=1]> but was:<flag=[false count=0]>
4 tests completed, 2 failed
```

The same output, on other versions:

```
./gradlew :app:testDebugUnitTest -Pcompose=1.13.0-beta02
./gradlew :app:testDebugUnitTest -PandroidxBuild=16829701 -Pcompose=1.14.0-SNAPSHOT
```

On a device or emulator, the same test: `./gradlew :app:connectedDebugAndroidTest`. The app shows it too: tap "Toggle flag", then "Increment count".

<img src="device.png" width="360" alt="The app after Toggle flag and two taps on Increment count: the expected line reads flag=true count=2, the lambda's text still reads flag=false count=0">

The two controls pass: B reading the local but calling `next` without a branch, and `next` created in the root composition instead of in A.

## Expected

After `flag = true` the text reads `flag=true count=0`, and after `count = 1` it reads `flag=true count=1`.

## Actual

It reads `flag=false count=0` from then on.

Traced in the frame after `flag = true` (links at androidx-main [`08c80ca`](https://github.com/androidx/androidx/tree/08c80ca74721891166b67b887fc955021cd52745/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime)):

1. The root composition recomposes. The provider takes the new value, the root reads it (`captured = true`), and the outer `BoxWithConstraints` content lambda updates, which invalidates A's scope through `ComposableLambdaImpl.trackWrite`. A lands in `compositionInvalidations`.
2. The trailing recompositions in `Recomposer.runRecomposeAndApplyChanges` ([Recomposer.kt L680-L708](https://github.com/androidx/androidx/blob/08c80ca74721891166b67b887fc955021cd52745/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Recomposer.kt#L680-L708)) first add the compositions that observe the modified values, which is B (it read `LocalFlag`), and only then `compositionInvalidations`, which is A. So B recomposes before A.
3. B switches branch and calls `next` from a newly inserted group with the block it still has (`captured = false`). `trackRead` ([ComposableLambda.kt L78-L106](https://github.com/androidx/androidx/blob/08c80ca74721891166b67b887fc955021cd52745/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/internal/ComposableLambda.kt#L78-L106)) keeps the old call site's scope and adds the new one.
4. A recomposes. `update(block)` calls `trackWrite` ([L60-L76](https://github.com/androidx/androidx/blob/08c80ca74721891166b67b887fc955021cd52745/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/internal/ComposableLambda.kt#L60-L76)), which invalidates both scopes and then clears them. The old scope returns `SCHEDULED`, but its group is about to be removed. The new scope returns `IGNORED`: B's changes aren't applied yet, so the scope isn't in B's slot table ([Composition.kt L1444-L1450](https://github.com/androidx/androidx/blob/08c80ca74721891166b67b887fc955021cd52745/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Composition.kt#L1444-L1450), "The scope has not yet entered the composition"). I checked both results by calling `invalidateForResult` on the lambda's tracked scopes at that point.
5. B's changes apply with the old text. `next` now tracks no scope, so `count = 1` updates its block and invalidates nothing.

## Workaround

Read the local in the composition that creates the lambda, or at the top of the component outside its `BoxWithConstraints`, and pass the result into the `BoxWithConstraints` content. B is then invalidated by A's update and recomposes after it.

## Environment

- androidx.compose runtime, ui and foundation 1.12.1 (BOM 2026.09.00), 1.13.0-beta02, and 1.14.0-SNAPSHOT from androidx.dev build 16829701 (2026-11-04). We first saw it with Material3 1.4.0.
- Robolectric 4.17 on Temurin JDK 25.0.2, macOS. Also an Android 14 (API 34) emulator.
- AGP 9.5.0, Kotlin 2.4.21, Gradle 9.8.2.
