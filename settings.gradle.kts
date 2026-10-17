pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://lukeneedham.github.io/ci-workflows/maven") {
            content { includeGroup("com.lukeneedham") }
        }
    }
}

rootProject.name = "Android-Dock"
include(":app")
