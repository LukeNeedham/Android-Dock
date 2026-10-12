# How it works

Fan is a single Kotlin module, package `com.lukeneedham.androiddock` (kept from the original name
"Android Dock" so installs update in place). The UI is Compose for settings and plain views for the
sheet.

## Pieces

| Piece | Responsibility |
|---|---|
| `DockAccessibilityService` | Adds a `TYPE_ACCESSIBILITY_OVERLAY` window (a `CornerTouchView`) over the chosen corner, above the navigation bar. Turns presses into: open or close the sheet (tap), previous app (double tap), actions mode (long press). Also performs global actions (Back, Home, Recents, Screenshot, Power menu). |
| `CornerTouchView` | The trigger. Sees the whole press-drag-release gesture in raw coordinates and reports press and long press. Excludes itself from the system back gesture where it can. |
| `DockActivity` | A transparent, `excludeFromRecents`, `singleInstance` activity that draws the sheet: a quarter-circle background and rings of icons. Closing it calls `finishAndRemoveTask()`, so it never shows up as a task. |
| `RecentApps` | Builds the recents list from `UsageStatsManager` events. Also launches an app. |
| `DockPrefs` | All settings in `SharedPreferences`: corner, trigger geometry, colours, rows, hidden apps, dismissed apps. |
| `SettingsActivity` | The only launcher entry. Onboarding checklist plus the settings pages (trigger, fan layout, hidden apps, debug). |
| `DockLog` | A file-backed log that survives the process dying. |

## A tap, end to end

1. The finger lands on the overlay window. `CornerTouchView` calls `onPress`.
2. The service decides if this is a double tap (within the system double-tap timeout) and gives
   haptic feedback.
3. For a single tap it starts `DockActivity`, setting a static `starting` flag so a tap that
   arrives before the activity exists is not lost.
4. `DockActivity` loads `RecentApps.loadRecents` off the main thread and lays the icons out in the
   rings from `DockPrefs.getRows`.
5. Choosing an app launches its launcher intent and closes the sheet, which removes its own task.

## Open apps

Android does not hand third-party apps the system recents list. `RecentApps` reads the last 24
hours of usage events (resumed, paused, stopped, destroyed), tracks which activities of each app
are still alive, and drops apps with none left. See [Using Fan](using-fan.md#which-apps-are-listed)
for the rules.

## Why an accessibility overlay

An accessibility service may add `TYPE_ACCESSIBILITY_OVERLAY` windows, which can sit above the
navigation bar. That is what makes a tappable corner on top of the nav bar possible without
root. Back, Home, Recents and similar actions are also only available through
`performGlobalAction`. This is also why Google Play restricts the approach.
