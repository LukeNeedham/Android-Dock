# Setup

Fan needs two permissions before it works. Opening Fan from the launcher shows a checklist that
ticks each step off as you do it.

## 1. Install

Download `Android-Dock.apk` from the
[latest release](https://github.com/LukeNeedham/Android-Dock/releases/latest/download/Android-Dock.apk)
and install it. Android 6.0 (API 23) or newer is required.

Releases and pull-request builds are signed with different keys. Moving from a pull-request build to
a release means uninstalling once; later releases update in place.

## 2. Accessibility service

Settings > Accessibility > **Fan**, then switch it on. This lets Fan place the trigger window over
the corner of the screen and see taps on it. Fan does not read what is on your screen.

**If the switch is greyed out** (Android 13 and newer, for sideloaded apps): open Fan's App Info
page, tap the ⋮ menu, choose **Allow restricted settings**, then go back and try again. The
onboarding screen has a button to open App Info.

## 3. Usage access

Settings > Apps > Special app access > **Usage access** > Fan, then allow it. Fan uses this to see
which apps you have used recently. Without it the fan is empty.

## Gesture navigation

With gesture navigation, the system claims a strip along the bottom edge for its swipe-up gesture.
A trigger inside that strip loses the touch when your finger moves up. If you want to use the
bottom corners, raise the trigger (Customise trigger > distance from the edge) until a swipe up from
it stays with Fan. See [Customising](customising.md).

## After an update

Android switches the accessibility service off on every install. Switch it back on in Settings >
Accessibility > Fan. See [Known limitations](known-limitations.md).
