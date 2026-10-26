# Development

## Project setup

- Kotlin, single `app` module, package `com.lukeneedham.androiddock`.
- `minSdk` 23, `targetSdk` 37, `compileSdk` 37. The floor is set by Compose and the libraries around
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

Debug builds only: Settings > **Debug Settings** opens the shared library's debug page, with **Logs** and
**Crashes**. The log records service start (with device and navigation mode), the trigger's position and
sheet open and close, in a file in the app's private storage that survives the process being killed. A
crash is saved with the last 50 log lines. Text is selectable, so it can be copied. The log is trimmed at
about 512,000 characters. The page also has an animation speed setting, which Fan does not use.

All of it comes from `com.lukeneedham:common` (`Logger`, `DebugKit`, `DebugPage`, `LogsPage`,
`CrashesPage`), published from [ci-workflows](https://github.com/LukeNeedham/ci-workflows/tree/main/common).
Log with `Logger.i/d/w/e(...)`, which needs no `Context`. `App` starts it with
`DebugKit.init`, before the service or any activity can log. Crashes are saved in release builds too;
there is just no page to read them on.

## Project layout

```
app/src/main/java/com/lukeneedham/androiddock/   sources (see How it works)
app/src/main/res/                                strings, themes, icons, accessibility config
docs/                                            these docs, and the logo
.github/workflows/                               thin callers of the shared CI workflows
```

See [How it works](how-it-works.md) for what each source file does.
