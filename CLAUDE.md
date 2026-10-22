# Notes for Claude

## Accessibility is switched off by every update; this is accepted

After each install of an updated APK, Android turns the accessibility service off and the user
re-enables it by hand. Do not try to fix this. It was tried and failed: re-enabling from the app
with `WRITE_SECURE_SETTINGS` (granted over adb) is reverted by the system within milliseconds,
however often it is retried. Details are in docs/known-limitations.md. Do not re-add the
update receiver, the adb grant, retries or an auto-enable. If the user reports that accessibility is
off after an update, that is expected: tell them to switch it back on.

## Conventions

- Logging and crash capture come from the shared `com.lukeneedham:common` library, started by
  `DebugKit.init` in `DockApp`. Log with `Logger.i/d/w/e(...)` (no `Context` needed), never `Log` or
  `println`. Do not add another crash handler or log file. The debug, logs and crashes pages are the
  library's, wired into the `Route` enum in `SettingsActivity`.
- `SettingsActivity` wraps its Compose content in `ProvideDebugSettings` (the library's single provider of debug
  composition locals). Keep it wrapped, even though no debug setting is read yet, so a setting added to the library
  later reaches the app with only a version bump.
- Theme: the UI reads `AppTheme.current` only, never `MaterialTheme`. `AppColors` (in `AppTheme.kt`) names a colour
  per use, so a new use gets a new name there instead of borrowing a generic slot. Buttons are `AppButton` and
  `AppIconButton` in one of three `AppButtonStyle`s (Primary, Secondary, Text), each with one set of colours and
  a 48dp minimum size; do not add a one-off button. Text is `AppText` and icons `AppIcon`. Slider, bottom sheet, text
  field and dropdown menu are still Material components, wrapped (`appSliderColors`, `AppBottomSheet`,
  `AppTextField`) so they are given every colour and shape from the app theme.
- Builds: the PR build comment from CI carries the APK link. When it updates, paste the link in the
  chat.
- Nothing can be built locally in the cloud session (no Android plugins offline). CI is the build.

Before starting work, check that `.claude/skills/lukeneedham-conventions` matches `skills/lukeneedham-conventions` on `main` of `LukeNeedham/ci-workflows`; if not, copy it over and include that in this work's PR. See that repo's `docs/skill.md`.
