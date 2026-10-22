# Using Fan

Everything starts from the **trigger**, a small strip in a corner of the screen, drawn above the
navigation bar. By default it is a faint translucent white box in the bottom-right corner.

## The fan

**Tap** the trigger. A quarter circle opens from the corner, with the icons of your recently used
apps along its rings. The most recent apps are nearest the corner.

- **Tap an app** to switch to it.
- **Tap empty space** or the trigger again to close.
- **Long press an app** to open its App Info page, where it can be force stopped.
- The ring in the very corner (the "corner ring") holds just the app you are in now. It is blank
  on the home screen.

Closing the fan also removes its own task, so Fan never appears in the system task switcher.

## Double tap: previous app

Tap the trigger twice quickly to switch straight to the app you used before the current one,
without opening the fan.

## Actions mode

**Long press** the trigger. The fan opens with the same layout and colours, but its icons are
actions:

| Action | Notes |
|---|---|
| Back, Home, Recents | Done through the accessibility service, as if you pressed the system buttons |
| Fan settings | Opens Fan's own settings |
| System settings | Opens Android Settings |
| Screenshot | Android 9 and newer |
| Power menu | Opens the system power menu |
| Flashlight | Toggles the torch |

The fan closes first, then the action runs a moment later, so it acts on whatever was underneath.

## Which apps are listed

Android does not give ordinary apps the list of running apps. Fan builds it from the system's
**usage events** over the last 24 hours (this is why usage access is needed):

- An app counts as open from when it is used until the system reports its activities destroyed,
  so apps you swipe away in the system recents drop off the list (Android 10 and newer).
- Launchers (home screens) are never listed. If a launcher is on screen, there is no current app.
- Apps you hide in settings never appear, and double tap never switches to them.
- Fan's own screens are left out, except Fan's settings while they are open.
- The number of apps shown is however many the rings hold.
- **App order** (main settings page) is either **Most recently used** (the default) or **Most used in the
  past 7 days**, which orders apps by foreground time and lists only apps with a launcher icon.
