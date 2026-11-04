plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val compose = providers.gradleProperty("compose").get()

android {
    namespace = "com.example.repro"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "com.example.repro"
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // One test, run on the JVM with Robolectric (test) and on a device (androidTest).
    sourceSets.getByName("test").kotlin.directories.add("src/sharedTest/java")
    sourceSets.getByName("androidTest").kotlin.directories.add("src/sharedTest/java")
    buildFeatures.compose = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.all {
        // Robolectric reaches into JDK internals on JDK 17+.
        it.jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        )
        it.testLogging {
            events("passed", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStackTraces = false
        }
    }
}

// Every androidx.compose runtime, ui, foundation and animation artifact at the same version.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group in setOf("androidx.compose.runtime", "androidx.compose.ui", "androidx.compose.foundation", "androidx.compose.animation")) {
            useVersion(compose)
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation:$compose")

    testImplementation("androidx.compose.ui:ui-test-junit4:$compose")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    // ui-test still brings Espresso 3.5, whose input injection fails on API 37.
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
