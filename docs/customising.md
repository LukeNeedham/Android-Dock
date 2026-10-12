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
- **Background gradient**: a small square shows the fan background as it will be drawn, a quarter circle in the chosen corner. **Edit background gradient** opens a bottom sheet with a larger square and a numbered handle for each colour stop (always ringed, so it shows against any colour). Drag a handle along the diagonal through the square to move that stop out from the corner (the far end of the line, nearly at the opposite corner, is the outer edge); the selected handle is larger and haloed. Tap a handle to select it and set its colour with the sliders. **Add colour stop** puts a new one in the widest gap, in the colour already there; **Remove this stop** deletes the selected one. Up to eight stops. A single stop gives one flat colour. The default is black at the corner fading to transparent at the edge, and a gradient saved by an older version carries over.

Icons fill ring 1 first, then the next ring out. The fan lists as many apps as all the rings hold.
The defaults are three rings of 3, 5 and 8 items.

## Hidden apps

"Hidden apps" lists apps that never appear in the fan, and that double tap never switches to. Add
one with **Hide an app** (searchable) and remove it with **Show again**.

## Debug

At the bottom of the settings, **Debug Settings** opens the [debug log](development.md#debug-log).
