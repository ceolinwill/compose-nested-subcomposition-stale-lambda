pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // An androidx-main snapshot build: -PandroidxBuild=<build id> -Pcompose=<its SNAPSHOT version>.
        providers.gradleProperty("androidxBuild").orNull?.let { maven("https://androidx.dev/snapshots/builds/$it/artifacts/repository") }
    }
}

rootProject.name = "compose-nested-subcomposition-stale-lambda"
include(":app")
