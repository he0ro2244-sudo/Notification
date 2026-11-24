# Release notes

## 1.1.5
- **Swipe-through** (Settings > MESSAGES > Pages, shortcuts & grouping): the widget's tiles are drawn as fixed views instead of a scrolling list, so a swipe that starts on them is no longer taken by the widget and your launcher (for example Nova's swipe down) still gets it. Shows the first 8 tiles (or "Most tiles on the widget", up to 12); Pages reach the rest. Tapping a tile works as before. Off by default; with it off nothing changes.

## 1.1.4
- **Settings > Reset everything to automatic now does the same as Pencil > Reset.** Before, it left moved places (the free layout) in place while resetting sizes, which could pile the clock, weather, date and Inbox on top of each other. Now it also clears moved places and restores the standard layout.
- **Undo in Settings**: after a reset or loaded layout, an "Undo last reset or load" button appears under the reset button (the same undo as in the pencil menu). The reset asks for confirmation first and explains what it removes and what it keeps.

## 1.1.3
- **Mute one chat**: new **Mute chat** button in the open message card hides just that chat (not the whole app) from the widget and the panel. Unmute it in Settings > MESSAGES > Filters & limits > Muted chats. Nothing is deleted from your phone.
- **Reply templates**: quick replies such as "On my way" shown above the reply box. Tap one to fill the box, then Send. Edit them in Settings > MESSAGES > Filters & limits > Reply templates.
- **Snooze one chat**: new **Snooze** button in the open message card (15 minutes, 1 hour, 3 hours or until tomorrow 8:00). The chat returns by itself. The existing widget-wide Snooze and Quiet hours are unchanged.

## 1.1.2
- **Full user manual inside the app**: Look & colors > HELP > User manual (read me), at the bottom of Settings. The same text is in USER_MANUAL.md.
- **Merged shape colours fixed**: the merged clock / weather / date shape now uses the colours picked for Clock, Weather and Date (before, it ignored them and always used the shared circle text colour).
- **Each part of the merged shape opens its own app**: tap the clock part for the clock, the weather part for the weather app, the date part for the calendar (before, the whole shape opened the clock).
- **Back from Settings returns to the widget**: when Settings is opened with the widget's gear, Back goes straight to the home screen instead of one step up.
- **Settings starts with every group closed** (Themes is no longer open by default), and Back closes all open groups.
- **Space between the parts of the merged shape**: a slider in Arrange & resize > Merge into one shape makes each part bigger and easier to tap.
- **Text size for every part of the widget**: Text size per part (Inbox title, + Note, Clear all, clock, weather, date, merged shape, Where to? text, Nothing new text, fast-dial letters and names, shortcut letters and names, tile texts, page buttons).
- **Show or hide each part of the Updated line**: the time, the source (Open-Meteo / Phone weather app) and the "to match phone" note have their own switches. The master switch still hides the whole line.
- **Hide or show the + (Add) circles**: separate switches for the fast dial and for the shortcuts row (Fast dial and Shortcuts row sections). With them off, add contacts and shortcuts from Settings.
- **Tidier Settings**: the two biggest sections are split into smaller ones (Fonts & overall text size, Text size per part, Text colours, Wording & time stamp; Arrange the widget, Clock & date, Weather). Every section has a one-line description, groups have clearer headings, and there are new **Open all sections** / **Close all sections** buttons.
- **Move: pack items closer**: new "Tidy up: pack items closer" button with a gap slider (0-40 dp) and an option to centre each row. Items of a row sit side by side with your gap, so there are no big empty spaces like "space evenly" can leave. "Space evenly" is unchanged.
- **Move: Undo last move or tidy-up** button.
- **Settings closes every section when you leave it** (Home or recents), and there is a new switch **Open one section at a time**.
- **Space between the widget rows**: slider in Settings > Arrange the widget (0-30 dp) for the gap under the top row, fast dial, shortcuts row and Inbox row.
- **Live preview**: a copy of the widget is pinned at the top of Settings and Arrange & resize and redraws by itself while you change things. Tap its grey bar to switch Off / Small / Medium / Full.
- **Filters & limits** (Settings > MESSAGES): hide messages that contain chosen words, always list VIP names first, hide messages older than 1 hour to 3 days, and limit how many tiles the widget draws.
- **New colour for the merged shape**: Text colour per part > "Widget: merged shape" (also a Colour button in Arrange & resize). When set it colours the whole merged shape; on Auto each part keeps its own colour.

## 1.1.1
- Added the user manual (USER_MANUAL.md) and these release notes.
- GitHub releases now show these notes.

## 1.1.0
- **Undo** after Reset or loading a layout (pencil menu).
- **Saved layouts**: save, load and delete named layouts (pencil menu).
- **Free layout follows the widget width**: saved places stretch sideways when the widget is resized.
- **Faster redraws**: app icons are cached for 10 minutes; saved places are parsed once.
- **Release build**: a smaller, faster release APK is built next to the debug APK, signed with the same key.
- One-time prompt to allow running without battery limits; the notification service asks to be reconnected if the system drops it.
- Optional own signing key through GitHub Secrets (`KEYSTORE_B64`, `KEYSTORE_PASS`).
- Unit tests for the saved-places reading and writing.
- Accessibility label for the pencil.

## 1.0.23
- Where to? **Box width** slider is in the Where to? card in both layouts.

## 1.0.22
- Alignment guides while dragging in Move.
- **Tidy up** buttons in Move (align rows, align and space evenly).

## 1.0.21
- Where to? box width in the rows layout (far left = fills the row).

## 1.0.20
- Reset restores the standard layout (shortcuts row on, pencil on, refresh icon and weather line shown).
- The pencil is on by default so Reset can always be reached.

## 1.0.19
- **Reset** is the third option of the pencil menu (Move, Arrange & resize, Reset).

## 1.0.18
- Clearer names for the width and height sliders (they stretch only the shape).

## 1.0.17
- Separate **Width** and **Height** sliders for clock, weather, date, merged shape, fast-dial and shortcut circles, and **Height** for Where to?.

## 1.0.16
- Back from Move or Arrange returns to the pencil menu.
- Merged shape can be shown side by side instead of one under the other.
