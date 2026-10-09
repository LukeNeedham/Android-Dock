# Fan: Fast App Nav

An Android overlay that gives you a task switcher from anywhere, in one tap. It is not an app you
launch and use. It stays out of the way until you call it.

> **Status:** early prototype. The corner trigger opens a corner sheet of recent apps, and a
> long press opens the settings. Touches are recorded in the in-app debug log.

## The idea

1. You tap the **bottom-right corner of the screen, in the navigation bar**.
2. A **corner sheet**, a quarter circle in the bottom-right corner of the screen, opens over
   whatever you were doing. The icons of your **recently used apps** run along its arc, the most
   recent nearest the bottom edge.
3. Pick an app to switch to it, or dismiss the sheet.
4. Dismissing the sheet **also kills Fan's own task**, so it never appears as an entry
   in the system task switcher.

Opening Fan from the launcher does not show the sheet. It shows a settings screen with
the setup steps, which is where a new user starts.

## Main points

- **Overlay, not an app.** No main screen. The only UI is the corner sheet over the current app.
- **Triggered from the navigation bar.** A tap on the bottom-right corner of the nav bar is
  intercepted by an accessibility service and opens the sheet.
- **Open apps list.** The sheet shows the apps currently open, as the system task switcher does.
- **Leaves no trace.** Closing the sheet removes the task (`finishAndRemoveTask()`), and the
  activity is excluded from recents, so the dock never shows up as a task itself.
- **Onboarding on first open.** Opening the app shows a settings screen that walks the user
  through enabling the accessibility service and usage access, with each step ticked off as it
  is done. Fan can't work until both are on.

## How it will work

| Piece | Responsibility |
|---|---|
| `DockAccessibilityService` | Watches for a tap on the nav bar's bottom-right corner and launches the sheet. |
| `SettingsActivity` | The launcher entry. Onboarding checklist for the accessibility service and usage access, re-checked each time the user returns from system settings. |
| `DockActivity` | Transparent, `excludeFromRecents` activity that shows the corner sheet and removes its own task when the sheet is dismissed. |

## Known limitation: updating turns accessibility off

Every time the app is updated, Android turns the accessibility service off, and it has to be
switched back on by hand: **Settings > Accessibility > Fan**. Usage access is not affected.

This was investigated and cannot be fixed from inside the app. With `WRITE_SECURE_SETTINGS`
granted over adb, the app can write the enabled-services setting after an update, and the write
returns success. The system then reverts it within milliseconds (the setting reads back empty
and the service never binds), however many times it is retried. Toggling it in the Settings app
works. The cause was not found, on a Pixel 5 running Android 14. That approach, with the adb grant,
the update receiver and the rechecks, was removed again. It is in the history of PR #4 if it is
worth another look on a different Android version.

So an update means re-enabling accessibility; this is accepted behaviour, not a bug to fix.

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
- **Restricted settings.** On Android 13+, a sideloaded app's accessibility service can be greyed
  out until the user allows restricted settings in app info. The onboarding hints at this; it may
  need a clearer, detected step.
- **Distribution.** Google Play restricts use of the accessibility API. Plan on sideloading unless
  that changes.

## Project setup

- Kotlin, single `app` module, package `com.lukeneedham.androiddock` (kept from the old name, "Android Dock", so installs update in place)
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
