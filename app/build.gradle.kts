plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Version name and code (from the git tags) and release signing (from the environment) come from the
// shared script in ci-workflows. See docs/setup.md there.
apply(from = "https://raw.githubusercontent.com/LukeNeedham/ci-workflows/claude/fan-corner-positioning-6vg5qa/gradle/android-release.gradle")

android {
    namespace = "com.lukeneedham.androiddock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lukeneedham.androiddock"
        minSdk = 23
        targetSdk = 35
    }

    // A fixed debug key, committed to the repo. CI builds on a fresh machine each time, which would
    // otherwise sign every APK with a different key, and Android refuses to update an installed app
    // with an APK signed by another key. This key is public and only for debug builds.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
