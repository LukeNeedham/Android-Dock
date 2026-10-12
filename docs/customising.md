# Customising

Open **Fan** from the launcher to reach the settings.

## Screen corner

Choose which corner the trigger and the fan sit in: bottom right (default), bottom left, top left
or top right. All distances are measured from the two edges that meet at that corner.

## Trigger

"Customise trigger" sets:

- **Distance** from each of the two edges, in dp.
- **Width** and **height**, in dp.
- **Colour and opacity** of the trigger (red, green, blue, opacity).

In gesture navigation, raise a bottom-corner trigger until a swipe up from it stays with Fan; see
[Setup](setup.md#gesture-navigation).

## Fan layout

"Customise fan layout" shows a preview with placeholder apps. Tap a ring in the preview to select
it, then adjust it:

- **Rings**: add or delete rings (up to six). Each ring has its own number of **items** (1 to 12)
  and **width** (32 to 150 dp, measured along the screen edge). The widths add up to the size of the
  fan, and each ring sizes its icons to its width.
- **Corner ring**: holds the app you are in now. You can change its width, or hide the app and keep
  only the space.
- **Distance from the edge** to where the fan starts.
- **Colours**: the sheet's colour at the corner, which fades to its colour at the outer edge.

Icons fill ring 1 first, then the next ring out. The fan lists as many apps as all the rings hold.
The defaults are three rings of 3, 5 and 8 items.

## Hidden apps

"Hidden apps" lists apps that never appear in the fan, and that double tap never switches to. Add
one with **Hide an app** (searchable) and remove it with **Show again**.

## Debug

At the bottom of the settings, **Debug Settings** opens the [debug log](development.md#debug-log).
