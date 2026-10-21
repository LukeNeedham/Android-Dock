# Fan: Fast App Nav

<img src="docs/fan-logo.svg" alt="Fan logo" width="96" align="right">

Switch apps from anywhere, in one tap. Fan puts a small trigger in a corner of your screen. Tap
it to open a fan filled with your recent apps. Pick one and you're there.

## What you can do

| Gesture on the trigger | Result |
|---|---|
| **Tap** | Opens the fan of recent apps (tap again to close) |
| **Double tap** | Jumps straight back to the previous app |
| **Long press** | Opens the same fan showing actions: Back, Home, Recents, Screenshot, Power menu, Flashlight, System settings, Fan settings |
| **Long press an app** in the fan | Opens its App Info page (where it can be force stopped) |

You choose the screen corner, the trigger's size, position and colour, how many rings of apps the
fan has, and which apps never appear.

Details: [Using Fan](docs/using-fan.md) · [Customising](docs/customising.md)

## Get started

1. Download the latest APK from the
   [latest release](https://github.com/LukeNeedham/Android-Dock/releases/latest/download/Android-Dock.apk)
   and install it (Android 6.0 or newer).
2. Open **Fan** once. It walks you through two permissions: the **accessibility service** (to
   see taps on the trigger) and **usage access** (to know which apps you used recently).
3. Customise the fan and trigger to your heart's content
4. Tap the trigger in the corner of your screen to open your fan!

Step-by-step, including what to do if the accessibility switch is greyed out:
[Setup](docs/setup.md)

> **After every update, switch accessibility back on** (Settings > Accessibility > Fan). Android
> turns it off on each install and it cannot be fixed from inside the app. See
> [Known limitations](docs/known-limitations.md).

## Documentation

| Doc | Covers |
|---|---|
| [Setup](docs/setup.md) | Installing, the two permissions, restricted settings, gesture navigation |
| [Using Fan](docs/using-fan.md) | The fan, the actions mode, double tap, how the recents list is chosen |
| [Customising](docs/customising.md) | Corner, trigger, fan layout, colours, hidden apps |
| [Known limitations](docs/known-limitations.md) | Accessibility reset on update, older Android versions, Google Play |
| [How it works](docs/how-it-works.md) | Architecture: the overlay, the sheet, the recents list |
| [Development](docs/development.md) | Building, CI, releases, project layout, the debug log |