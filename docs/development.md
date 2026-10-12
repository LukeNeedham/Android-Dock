# Development

## Project setup

- Kotlin, single `app` module, package `com.lukeneedham.androiddock`.
- `minSdk` 23, `targetSdk` 35, `compileSdk` 36. The floor is set by Compose and the libraries around
  it, and by the trigger overlay and the flashlight.
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).

## Building

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Debug builds are signed with
the committed `app/debug.keystore`, so every build, including CI's, can update an installed one.
That key is public and only for debug builds.

## CI

Pull requests are built by the shared workflows in
[LukeNeedham/ci-workflows](https://github.com/LukeNeedham/ci-workflows). The debug APK is built and
a download link is posted as a PR comment. See `.github/workflows/`.

## Releases

A release is a permanent, signed build, published by the **Release** workflow (Actions tab >
Release > Run workflow, on `main`). Give it a version such as `0.3.0`. It tags `0.3.0` (no `v`),
builds the release APK, signs it with the key in the repo secrets (`RELEASE_KEYSTORE_BASE64`,
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`), and publishes a release
with the APK as `Android-Dock.apk`. The newest is always at
`https://github.com/LukeNeedham/Android-Dock/releases/latest/download/Android-Dock.apk`.

The version name and code are read from the git tag by `app/build.gradle.kts`, through a shared
script that is fetched from ci-workflows `main` at build time (unversioned). `0.3.0` becomes code
300. An untagged build is named like `0.3.0-5-gabc1234` (or `0.0.0-dev` where no tags are fetched,
as on PR builds) and keeps the nearest tag's code.

## Debug log

Settings > **Debug Settings** > **View debug log**. It records service start (with device and
navigation mode), trigger touches, sheet open and close, and crashes, in a file in the app's private
storage that survives the process being killed. Use **Copy** to share it. Capped at about 256 KB.

## Project layout

```
app/src/main/java/com/lukeneedham/androiddock/   sources (see How it works)
app/src/main/res/                                strings, themes, icons, accessibility config
docs/                                            these docs, and the logo
.github/workflows/                               thin callers of the shared CI workflows
```

See [How it works](how-it-works.md) for what each source file does.
