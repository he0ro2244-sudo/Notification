# Notif Tiles - User Manual
Version 1.1.2

Notif Tiles is a home-screen widget that shows your notifications as tiles, together with a clock, weather, date, fast-dial contacts, app shortcuts, quick notes, a voice recorder and a "Where to?" button for Google Maps. Everything can be moved, resized and coloured.

## 1. First-time setup
1. Install the APK (GitHub > Releases > latest build). Every build is signed with the same key, so a new build installs over the old one and keeps your settings.
2. Open **Notif Tiles setup** and turn on **Notification access**. The status line shows "on" or "OFF".
3. Choose which apps appear as tiles.
4. Add the widget: long-press the home screen > Widgets > Notif Tiles, then drop it where you want it.
5. When asked, allow the app to run without battery limits. Some phones stop background apps, which freezes the widget.
6. Optional: turn on the Accessibility helper (see section 9) for the voice recorder and for Where to? / Exit navigation.

## 2. What the widget shows
- **Clock, weather, date**: tap the clock or date to open the clock or calendar. Weather comes from Open-Meteo (other sources can be chosen).
- **Merged shape**: clock, weather and date can be shown together in one shape. Tap the clock part to open the clock, the weather part to open the weather app, the date part to open the calendar.
- **Where to?**: opens Google Maps search. While Google Maps is navigating it turns into a red "Exit navigation" button.
- **Fast-dial circles**: up to 5 contacts. Tap to call.
- **Shortcuts row**: up to 5 circles that open an app, a screen inside an app, or an app shortcut (for example a direct WhatsApp chat).
- **Inbox row**: the title, the mic, + Note, Clear all, the pencil and the settings gear.
- **Message tiles**: your notifications. Tap a tile to open it or reply.
- **Refresh icon and "Updated" line**: refreshes the weather and shows when it was last updated.

## 3. The pencil menu
Tap the pencil on the widget.

### Move
Press and hold any item and drag it anywhere. It stays exactly where you drop it (Free layout).
- While dragging, an item clicks into line with its neighbours when it is close.
- **Tidy up: align items in each row** puts the items of a row on one line.
- **Tidy up: align and space evenly** also spaces them equally.
- Drag the bar **Messages start here** to choose where the message tiles begin.
- **Back to the old rows layout** forgets all places and returns to the normal rows.
- Places are saved for your widget width and stretch sideways if you resize the widget.

### Arrange & resize
- Order and size of the sections (rows layout).
- For every item: **Size** (scales the item and its text), **Shape width only** and **Shape height only** (stretch the shape, the text stays the same size), a **Colour** button and a show/hide switch.
- The **Merged shape** has its own Colour button too.
- **Where to? box width**: far left means it fills the row, otherwise a fixed width.
- **Merge into one shape**: pick two or more of clock, weather and date to show them together, one under the other or side by side ("Side by side" switch).
- **Space between the parts**: a slider (0-30 dp) under Merge into one shape. More space makes each part bigger, so it is easier to tap the clock, weather or date on its own.

### Saved layouts
Save the widget's look and places under a name (for example "Work"), load it later, or delete it. Loading replaces the current look and places.

### Reset
Puts the widget back to the standard layout. It removes all themes, colours, fonts, sizes and moved places. Your apps, contacts, shortcuts and notes are kept.

### Undo
Appears after a Reset or a loaded layout and brings back what you had before. Custom background pictures are not brought back.

## 4. The settings screen (gear)
Open it with the gear on the widget or from the Notif Tiles app (Look & colors). Settings is split into six groups: LOOK, WIDGET AND PANEL, MESSAGES, TOOLS, SYSTEM and HELP. Each group holds sections; tap a section name to open or close it. Every section starts closed and shows a one-line description. **Open all sections** and **Close all sections** are under the search box. The search box at the top shows only the rows that match what you type. "Check everything" tests that the app is set up correctly, and "Button log" shows the last button actions.

## 5. LOOK
### Themes & accent colour
- **Themes**: ready-made looks. Picking one sets colours for the whole widget and panel.
- **Accent colour**: colour of buttons and the count badge.
- **Icons & Clear all colour**: mic, settings and refresh icons. A colour picked for one part under "Text colour per part" still wins.
- **Auto theme (day / night)**: switches theme by itself at the times you set, one small alarm per change.

### Backgrounds & pictures
Background of the control panel, the shadow behind it and the home-screen widget. Pick a photo from your gallery and lower the **Picture strength** to let the colour show through. The fast-dial strip has its own colour and opacity.

### Fonts & overall text size
- **Font**, **Weight**, **Italic**, **Letter case** and the overall **Text size**. A sample line shows the result.

### Text size per part
- **Text size per part of the panel**: extra size for each part, on top of the overall size.
- **Text size per part of the widget**: its own size for the Inbox title, + Note, Clear all, clock, weather, date, merged shape, Where to? text, Nothing new text, the letters and names of the fast-dial and shortcut circles, the tile texts and the page buttons. 100% = as designed.

### Text colours
- **Text colour (widget)**: one colour for all widget text. Auto = follows the theme.
- **Text colour per part**: tap a row to pick its colour with the big palette, the sliders or a hex code. "Auto" puts a part back to the overall colour.

### Wording & time stamp
- **Message time stamp**: smart style (today = time, yesterday = "Yesterday", older = date), or date + time, time only, none.
- **Widget wording**: the "Nothing new" text, the word before the weather update time, and the size of the Updated line.

### Colour of the merged shape
When clock, weather and date are merged into one shape:
- If you pick a colour in **Widget: merged shape**, all the text in the shape uses it.
- If you leave it on Auto, each part keeps its own colour from **Widget: clock**, **Widget: weather** and **Widget: date**. A part with no colour of its own follows the overall text colour.
The same colour button is also in Pencil > Arrange & resize > Every item > Top row > Merged shape.

### Buttons: + Note, Clear all, Settings
One colour per button, used on the widget and in the panel together.

### Shapes & borders
- **Shape** of the circles, and the sizes of the circles, icons and fast-dial.
- **3D icons**, **names under fast-dial circles**, refresh icon on or off.
- **Circle colour** (clock, weather, date, Where to?) and **fast-dial circle colour**.
- **Borders**: each border has its own switch, thickness and colour. Thickness 0 = no border.

## 6. WIDGET AND PANEL
### Arrange the widget
- **Arrange & resize by dragging**: opens the Arrange screen.
- **Order of the circles**: use the up and down arrows; switch an item off to hide it.
- **Maps search box at the start**: puts Where to? first.

### Clock & date
- **Clock** (phone setting, 12 or 24 hour) and **Date** format.

### Weather
- City, unit, mode and source, and **Temperature adjust** (to match your phone's weather app). "Compare all sources now" shows every source side by side.
- **Updated line**: three extra switches show or hide only the time ("Updated 12:50 AM"), only the source ("Open-Meteo" or "Phone weather app"), or only the "to match phone" note. **Show "Updated" time** switches the whole line off or on. The same switches are in Show / hide parts.

### Show / hide parts
Switch off anything you do not want to see. Nothing is deleted, switch it on again any time.

### Fast dial
Choose up to 5 contacts. Each circle has its own background and text colour. With **Ask before calling** on, tapping a circle offers Call, Change and Remove. Use Change or Clear here to fix a wrong person.

The **+ (Add) circles** of empty places can be hidden with **Show the + (Add) circles for empty fast-dial places**. Add and change contacts from this section when they are hidden.

### Shortcuts row
A second row of circles. Each one opens an app, one screen (activity) inside an app, or a shortcut an app offers. Tap the Add circle on the widget or use Pick in settings. Size, colour, shape and names follow the fast-dial settings. **Show the + (Add) circle in the shortcuts row** hides the Add circle; use Pick in this section to add shortcuts then.

## 7. MESSAGES
### Pages, shortcuts & grouping
- **Scrolling**: Scroll (list moves up and down), My pages (pages you create), or Pages (a few tiles at a time with the arrow buttons under the widget). A home-screen widget cannot be swiped sideways, that is an Android limit. In the panel you can swipe.
- **Endless**: pages wrap around and the widget list repeats.
- **Group messages**: one tile per chat, or one tile per app.
- **Order of the tiles**: move apps up or down. Pinned apps stay first.
- **My pages**: add pages with their own apps and shortcuts. In the panel, long-press a tile to move its app to another page.

### Apps: pin, mute, colour
Pin an app to the top, mute it (hidden from the widget, panel and counts), change the colour of its tiles and add shortcuts for it.

### Counts & clearing
Choose when the count drops: after opening the app, after leaving the message card, or after replying here. You can also remove the message from the phone's notification shade. **Clear all** can be switched to **Mark all read**.

### Quiet hours & snooze
Snooze the widget for a while, or keep it quiet between two times.

## 8. TOOLS
### Notes
**+ Note** saves what you type. Choose a notes app once; it is used every time. With "+ Note opens the notes app straight away" on, your notes app opens with a new empty note.

### Voice recorder
The blue mic at the start of the Inbox row starts recording. While recording the mic becomes a red stop square and the phone buzzes; tap it again to stop and save. Recordings are saved as m4a in Internal storage > Recordings > NotifTiles.
- Helper ON: uses your phone's recorder app and presses its red button for you.
- Helper OFF: records inside Notif Tiles (switch "Helper OFF: record inside this app").
- "Learn the red button", the position sliders and "Check recorder entry points" help when your recorder app is different.

### Where to? (maps & location)
- **Travel mode** used when you search, and **Drive: start navigation straight away**.
- **Turn on Location when I open it** and **Switch Location off again when navigation ends** (and "...even if Location was already on before").
- **Where to? goes straight to the Google Maps search screen**: with the helper ON it taps Maps' search bar for you, so you get Maps' own suggestions, Home, Work and Recent.
- **Closing the Maps pop-up also exits navigation**: needs the helper ON.
- **Voice button**, **Place suggestions**, **Remember recent places**, **Clear recent places**.
- Test buttons: "Self-check: what works and what does not", "Test: exit navigation now", "Test: switch Location off now".

## 9. The Accessibility helper
Some features need the helper: the recorder red button, opening Maps search, and Exit navigation. Open Accessibility settings from the Voice recorder or Widget section and turn on **Notif Tiles recorder button**. If the switch is greyed out: App info > the three dots > Allow restricted settings. The helper only presses buttons for these features.

## 10. SYSTEM
### Backup & restore
**Export settings to a file** saves every Look & colors choice, contacts, notes and background pictures as one file. **Import settings from a file** loads it again. Keep a copy before big changes. Saved layouts are part of the backup.

### Reset everything to automatic
Puts all colours, fonts and sizes back to automatic.

## 11. Tips
- Hidden items are shown again in Arrange & resize first, then they can be moved.
- If the widget looks crooked after moving items, use Tidy up in Move.
- If notifications stop arriving, open the app once: it reconnects the notification service.
- The settings search box finds any option by name, for example "merged" or "colour".

## 12. Troubleshooting
- **Widget empty or frozen**: open Notif Tiles setup, check Notification access is on, and allow running without battery limits.
- **Merged shape text is the wrong colour**: Look & colors > Text colours > Text colour per part > Widget: merged shape. Set Auto to use the clock, weather and date colours instead.
- **Where to? fills the whole row**: Arrange & resize > Where to? box > Box width.
- **Items overlap after resizing the widget**: open Move and use Tidy up, or drag the items again.
- **Where to? or Exit navigation does nothing**: check the Accessibility helper is ON, then use the Self-check in Where to? settings.
- **Recorder only opens the recorder app**: turn the helper ON, or use "Helper OFF: record inside this app".
- **Everything is messy**: Pencil > Reset (you can undo it).
