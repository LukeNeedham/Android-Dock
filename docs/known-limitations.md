# Known limitations

## Updating turns accessibility off

Every time Fan is updated, Android turns its accessibility service off, and it has to be switched
back on by hand: **Settings > Accessibility > Fan**. Usage access is not affected.

This cannot be fixed from inside the app. With `WRITE_SECURE_SETTINGS` granted over adb, the app
can write the enabled-services setting after an update, and the write returns success. The system
then reverts it within milliseconds (the setting reads back empty and the service never binds),
however many times it is retried. Toggling it in the Settings app works. The cause was not found, on
a Pixel 5 running Android 14. That approach, with the adb grant, the update receiver and the
rechecks, was removed again. It is in the history of PR #4 if it is worth another look on a
different Android version.

So an update means re-enabling accessibility. This is accepted behaviour, not a bug to fix.

## Android versions

- Minimum is Android 6.0 (API 23). Not tested below the versions the author has devices for.
- Below Android 10 the system does not report when an app's activities are destroyed, so apps
  swiped away in the system recents stay listed in the fan.
- Below Android 9 there is no Screenshot action.

## The recents list is a best guess

Android gives ordinary apps no list of running apps, so Fan infers it from usage events (see
[Using Fan](using-fan.md#which-apps-are-listed)). Unusual cases such as picture-in-picture,
multi-window, or a system dialog resuming can make it briefly wrong. Thumbnails are not available.

## Gesture navigation

The system's bottom swipe-up gesture cannot be excluded from the trigger by an app. In a bottom
corner, raise the trigger clear of that strip; see [Setup](setup.md#gesture-navigation).

## Distribution

Google Play restricts the accessibility API, so Fan is sideloaded from GitHub releases.

## Restricted settings

On Android 13 and newer, a sideloaded app's accessibility service can be greyed out until you allow
restricted settings in its App Info page. See [Setup](setup.md#2-accessibility-service).
