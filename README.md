# Android Dock

An Android overlay that gives you a task switcher from anywhere, in one tap. It is not an app you
launch and use. It stays out of the way until you call it.

> **Status:** early prototype. The corner touch target exists and only logs touches
> (`adb logcat -s DockCorner`); the sheet and app switching are not built yet.

## The idea

1. You tap the **bottom-right corner of the screen, in the navigation bar**.
2. A **bottom sheet** slides up over whatever you were doing, listing your **currently open apps**,
   like the system task switcher.
3. Pick an app to switch to it, or dismiss the sheet.
4. Dismissing the sheet **also kills Android Dock's own task**, so it never appears as an entry
   in the system task switcher.

You can also open Android Dock from the launcher. It shows the same bottom sheet, but that is a
fallback and not the intended way to use it.

## Main points

- **Overlay, not an app.** No main screen. The only UI is the bottom sheet over the current app.
- **Triggered from the navigation bar.** A tap on the bottom-right corner of the nav bar is
  intercepted by an accessibility service and opens the sheet.
- **Open apps list.** The sheet shows the apps currently open, as the system task switcher does.
- **Leaves no trace.** Closing the sheet removes the task (`finishAndRemoveTask()`), and the
  activity is excluded from recents, so the dock never shows up as a task itself.
- **Launcher entry as a fallback.** Opening the app by hand shows the same sheet.

## How it will work

| Piece | Responsibility |
|---|---|
| `DockAccessibilityService` | Watches for a tap on the nav bar's bottom-right corner and launches the sheet. |
| `DockActivity` | Transparent, `excludeFromRecents` activity that shows the bottom sheet and removes its own task when the sheet is dismissed. |

## Open questions

These need working out when the features are built:

- **Intercepting the nav-bar tap.** An accessibility service does not normally receive raw touches
  on system bars. The likely approach is a small transparent `TYPE_ACCESSIBILITY_OVERLAY` window
  placed over the bottom-right corner, which can sit above the navigation bar. This needs testing
  across gesture navigation and 3-button navigation, where that corner holds different things.
- **Listing open apps.** Android does not give third-party apps the system recents list.
  Candidates are usage stats (`UsageStatsManager`, needs the usage-access permission),
  accessibility window information, or a combination. Thumbnails are likely not available.
- **Switching to an app.** Launch it from its launcher intent, or ask the system to bring its task
  forward, depending on what the list can offer.
- **Permissions and onboarding.** The user has to enable the accessibility service, and possibly
  usage access, in system settings. The launcher fallback is a natural place to guide that.
- **Distribution.** Google Play restricts use of the accessibility API. Plan on sideloading unless
  that changes.

## Project setup

- Kotlin, single `app` module, package `com.lukeneedham.androiddock`
- `minSdk` 26, `targetSdk` / `compileSdk` 35
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)

Build a debug APK:

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## CI

Pull requests are built by the shared workflows in
[LukeNeedham/ci-workflows](https://github.com/LukeNeedham/ci-workflows): the debug APK is built
and a download link is posted as a PR comment. See `.github/workflows/`.
