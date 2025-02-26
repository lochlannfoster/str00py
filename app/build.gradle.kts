plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    // The namespace line is also required in modern AGP
    namespace = "com.example.strooplocker"

    // **Add compileSdk** (the old “compileSdkVersion”)
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.strooplocker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android libraries
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // JUnit & Espresso (for testing)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

        // For instrumentation tests with JUnit4 runner
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        // For Espresso
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // **Room** dependencies:
    val roomVersion = "2.5.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    // If you want Kotlin coroutines support for queries:
    implementation("androidx.room:room-ktx:$roomVersion")

    // Kapt compiler for Room
    kapt("androidx.room:room-compiler:$roomVersion")
}
