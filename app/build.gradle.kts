plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Version name and code (from the git tags) and release signing (from the environment) come from the
// shared script in ci-workflows. See docs/setup.md there.
apply(from = "https://raw.githubusercontent.com/LukeNeedham/ci-workflows/main/gradle/android-release.gradle")

android {
    namespace = "com.lukeneedham.androiddock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lukeneedham.androiddock"
        minSdk = 23
        targetSdk = 37
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
            // R8 shrinks and obfuscates the code, and shrinkResources drops resources it no longer
            // reaches. The activities and the accessibility service are kept by the rules that AAPT
            // generates from the manifest; nothing else is looked up by name.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.common)
    testImplementation(libs.junit)
}
