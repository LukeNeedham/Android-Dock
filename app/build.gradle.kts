plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The version comes from the git tags, which are the single source of truth: a release is made by
// tagging a commit `MAJOR.MINOR.PATCH` (no `v` prefix).
//  - On a tagged commit the version name is the tag, e.g. 0.3.0.
//  - Anywhere else it is `git describe`, e.g. 0.3.0-5-gabc1234 (five commits after 0.3.0), and with
//    no tag at all (or no tags fetched, as on CI's shallow PR checkouts) it is 0.0.0-dev.
// The version code is major*10000 + minor*100 + patch of the nearest tag (1 with none), so it only
// rises with releases and an untagged build never outranks the next release.
fun git(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args)
        .directory(rootDir)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    output.takeIf { process.waitFor() == 0 && it.isNotEmpty() }
} catch (e: java.io.IOException) {
    null
}

val releaseTagGlob = "[0-9]*.[0-9]*.[0-9]*"
val nearestTag = git("describe", "--tags", "--abbrev=0", "--match", releaseTagGlob)
val gitVersionName = nearestTag
    ?.let { git("describe", "--tags", "--match", releaseTagGlob) }
    ?: "0.0.0-dev"
val gitVersionCode = nearestTag
    ?.let { Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(it) }
    ?.destructured
    ?.let { (major, minor, patch) -> major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt() }
    ?: 1

android {
    namespace = "com.lukeneedham.androiddock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lukeneedham.androiddock"
        minSdk = 23
        targetSdk = 35
        versionCode = gitVersionCode
        versionName = gitVersionName
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
        // The real release key, which only exists on the release workflow, from repo secrets. Without
        // it a release build is left unsigned, so nothing can be shipped with the wrong key.
        System.getenv("RELEASE_KEYSTORE_FILE")?.let { keystore ->
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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
