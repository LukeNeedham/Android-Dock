# Notes for Claude

## Accessibility is switched off by every update; this is accepted

After each install of an updated APK, Android turns the accessibility service off and the user
re-enables it by hand. Do not try to fix this. It was tried and failed: re-enabling from the app
with `WRITE_SECURE_SETTINGS` (granted over adb) is reverted by the system within milliseconds,
however often it is retried. Details are in the README under "Known limitation". Do not re-add the
update receiver, the adb grant, retries or an auto-enable. If the user reports that accessibility is
off after an update, that is expected: tell them to switch it back on.

## Conventions

- Builds: the PR build comment from CI carries the APK link. When it updates, paste the link in the
  chat.
- Nothing can be built locally in the cloud session (no Android plugins offline). CI is the build.
