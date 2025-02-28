plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
    id("org.jetbrains.kotlin.kapt") version "1.8.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}