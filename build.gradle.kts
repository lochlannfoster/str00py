plugins {
    // The Android Gradle plugin, version can match your preference:
    id("com.android.application") version "8.8.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
    id("org.jetbrains.kotlin.kapt") version "1.8.21" apply false
}

// A simple "clean" task for convenience
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}