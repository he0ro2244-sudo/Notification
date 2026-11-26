# Notif Tiles - User Manual
Version 1.1.75

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
- **Message tiles**: your notifications. Tap a tile to open it or reply. In the open message card you also get **Snooze** (hide this chat for 15 minutes, 1 hour, 3 hours or until tomorrow 8:00) and **Mute chat** (hide just this chat for good).
- **Refresh icon and "Updated" line**: refreshes the weather and shows when it was last updated.

## 3. The pencil menu
Tap the pencil on the widget.

### Move
Press and hold any item and drag it anywhere. It stays exactly where you drop it (Free layout).
- While dragging, an item clicks into line with its neighbours when it is close.
- **Tidy up: align items in each row** puts the items of a row on one line.
- **Tidy up: align and space evenly** also spaces them equally. It spreads the row between its first and last item, so one item far away leaves big gaps.
- **Tidy up: pack items closer** puts the items of a row next to each other with the **Gap** you choose (0-40 dp). Turn on **Centre each packed row** to centre the row in the widget. Use this when "space evenly" leaves too much room.
- **Undo last change** takes back the last move, tidy-up or resize, one step at a time. The count shows how many steps can be taken back.
- Drag the bar **Messages start here** to choose where the message tiles begin.
- **Back to the old rows layout** forgets all places and returns to the normal rows.
- Places are saved for your widget width and stretch sideways if you resize the widget.

### Arrange & resize
- Order and size of the sections (rows layout).
- For every item: **Size** (scales the item and its text), **Shape width only** and **Shape height only** (stretch the shape, the text stays the same size), a **Colour** button and a show/hide switch.
- **Same size for every item in this row**: a tick box above each group (Top row, Fast-dial circles, Shortcuts row circles, Inbox row). Ticking it sets the whole row to the size of its first item; after that, moving the Size, Shape width or Shape height slider of any item in the row moves all the others too, so nothing gets a different size by mistake. Untick it to size items one by one.
- The **Merged shape** has its own Colour button too.
- **Where to? box width**: far left means it fills the row, otherwise a fixed width.
- **Merge into one shape**: pick two or more of clock, weather and date to show them together, one under the other or side by side ("Side by side" switch).
- **Space between the parts**: a slider (0-30 dp) under Merge into one shape. More space makes each part bigger, so it is easier to tap the clock, weather or date on its own.

### Themes
Pick a ready-made look (colours, icons, font, shape, borders) straight from the pencil, or tap Random. There are 30 themes. Undo is in the same menu.

### Emoji icons
Put an emoji on the Notes, Mic, Reset (refresh), Clear all and Pencil buttons. Choose one from the list or type your own. *Normal icon* goes back to the built-in one. The same screen is in Settings > LOOK > Themes & accent colour.

### Saved layouts
Save the widget's look and places under a name (for example "Work"), load it later, or delete it. Loading replaces the current look and places.

### Reset
Puts the widget back to the standard layout. It removes all themes, colours, fonts, sizes and moved places. Your apps, contacts, shortcuts and notes are kept.

### Undo
**Undo last change** (in the pencil menu and under the preview on Move and Arrange & resize) takes back what you did with the pencil, one step at a time: a move, a size, a colour, a Reset or a loaded layout. Settings has its own, separate Undo, so the two never mix. Custom background pictures are not brought back.

## 4. The settings screen (gear)
Open it with the gear on the widget or from the Notif Tiles app (Look & colors). Settings is split into six groups: LOOK, WIDGET AND PANEL, MESSAGES, TOOLS, SYSTEM and HELP. Each group holds sections; tap a section name to open or close it. Every section starts closed and shows a one-line description. **Open all sections** and **Close all sections** are under the search box. A **live preview** of the widget stays at the top of Settings and of Arrange & resize and redraws itself when you change something; tap its grey bar to switch Off / Small / Medium / Full (tiles are not drawn in the preview). **Open one section at a time** closes the others when you open one. Settings closes all sections when you leave it with Home or recents. The search box at the top shows only the rows that match what you type. "Check everything" tests that the app is set up correctly, and "Button log" shows the last button actions.

## 5. LOOK
### Themes & accent colour
- **Themes**: ready-made looks. Picking one sets colours for the whole widget and panel.
- **Accent colour**: the main highlight colour. It colours buttons, the count badge (the number bubble with your unread count), selected items, the move bar, and the glow and ink in the panel animations. Settings buttons and switches use it only in *Match my panel theme* mode, or when you set *Buttons, switches and sliders* yourself. Themes set it too.
- **Icons & Clear all colour**: mic, settings and refresh icons. A colour picked for one part under "Text colour per part" still wins.
- **Auto theme (day / night)**: switches theme by itself at the times you set, one small alarm per change.
- **Settings screen look**: the Settings screen has its own look, apart from the panel and widget. Pick Follow phone, Light, Dark, AMOLED black or Match my panel theme, then set your own background, text, button / switch and section-bar colours, text size, font and corner roundness. Reset puts it back. The live preview at the top shrinks while you scroll down the settings and grows again when you scroll up or change a setting; a switch in the same section turns that off.

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
- **Space between the widget rows** (0-30 dp): the gap under the top row, the fast dial, the shortcuts row and the Inbox row. 6 dp is the original look. It applies to the rows layout; with free placement use Move > Tidy up: pack items closer.

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
A second row of circles. Each one opens an app, one screen (activity) inside an app, or a shortcut an app offers. Tap the Add circle on the widget or use Pick in settings. **Pinned pic**: choose a picture; the circle shows a small copy and pressing it opens the picture full screen (tap to close, pinch to zoom). Not available on an app's own card. Size, colour, shape and names follow the fast-dial settings. **Show the + (Add) circle in the shortcuts row** hides the Add circle; use Pick in this section to add shortcuts then.

## 7. MESSAGES
### Pages, shortcuts & grouping
- **Scrolling**: Scroll (list moves up and down), My pages (pages you create), or Pages (a few tiles at a time with the arrow buttons under the widget). A home-screen widget cannot be swiped sideways, that is an Android limit. In the panel you can swipe.
- **Endless**: pages wrap around and the widget list repeats.
- **Swipe-through**: the widget's tiles are fixed instead of a scrolling list, so swiping down over them still reaches your launcher (for example Nova's swipe down). Shows the first 8 tiles (or your "Most tiles" number, up to 12); use Pages for the rest. The tiles only display and take no taps (a view that takes taps also takes the swipe), so open the panel with your launcher gesture or by tapping the Inbox title or count.
- **Group messages**: one tile per chat, or one tile per app.
- **Order of the tiles**: move apps up or down. Pinned apps stay first.
- **My pages**: add pages with their own apps and shortcuts. In the panel, long-press a tile to move its app to another page.

### Apps: pin, mute, colour
Pin an app to the top, mute it (hidden from the widget, panel and counts), change the colour of its tiles and add shortcuts for it.

### Counts & clearing
Choose when the count drops: after opening the app, after leaving the message card, or after replying here. You can also remove the message from the phone's notification shade. **Clear all** can be switched to **Mark all read**.

### Filters & limits
- **Hide messages containing these words**: separate words with commas (for example promo, offer, verification code). Matching ignores capital letters and looks at the title and the text.
- **VIP names**: messages from a sender or chat whose name contains one of these words are listed first, even before pinned apps.
- **Hide messages older than**: Never, 1 hour, 6 hours, 1 day or 3 days.
- **Most tiles on the widget**: All, 5, 10 or 20. A shorter list is lighter and faster. It applies to the scrolling list only.
- **Reply templates**: quick replies (one per line, up to 12; the defaults are On my way, Thanks, OK, I'll call you later). They appear as buttons above the reply box in the panel. Tapping one only fills the box; nothing is sent until you tap Send.
- **Muted chats**: chats you muted with **Mute chat** in the panel. Tap Unmute to bring one back. Muting one chat leaves the rest of the app alone.
These filters apply to the widget and to the panel. Nothing is deleted from your phone. A snoozed chat comes back by itself when its time is up if its notification is still on the phone.

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
Does the same as Pencil > Reset: puts the widget back to the standard layout and removes all themes, colours, fonts, sizes, shapes and moved places. Your apps, contacts, shortcuts, notes and message filters are kept. Settings has its own **Undo last change** button under the preview: it takes back every change made in Settings (and in Arrange & resize opened from Settings), one step at a time, including a Reset. The pencil's Undo is separate. Custom background pictures are removed and not brought back.

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


## Emoji icons, Settings and Inbox (1.1.21)
Settings > LOOK > *Choose emoji icons...* (or pencil > *Emoji icons*) lists seven places: Notes, Mic, Reset, Clear all, Pencil, Settings (gear) and the Inbox title. Tap a place, then tap an emoji or type / paste your own and press *Use my emoji*. *Normal icon* puts the standard icon back. The red stop icon still shows while recording.

## Message tiles in the panel (1.1.21)
Settings > LOOK > Shapes & borders > *Message tiles in the panel*: corner roundness, tile colour, fill solidity and border style (Solid, Dashed, Dotted). Border thickness and colour are under *Borders > Message tiles*. *Message tiles back to as designed* undoes all of these.


## Showing or hiding things in the panel (1.1.24)
Settings > WIDGET AND PANEL > Show / hide parts > *Widget header, tiles and panel*: each part of the bottom popup header (Back, Inbox title, count badge, mic, + Note, Clear all, pencil, refresh, settings icon) and of the panel tiles (app icon, count, title, text) has its own switch. Mic, pencil and refresh start hidden. Sizes and colours are in Text size per part / Text colours. The phone's Back button closes the panel; Settings > MESSAGES can make it go to the tiles first.


## Fix overlapping items, voice assistant shortcut (1.1.26)
Move > *Tidy up: fix overlapping items (smart)* moves items that sit on top of each other (for example a new shortcut circle on top of + Note) to the right or the next line. It also runs once by itself when you open Move after adding a shortcut circle. Undo is in Move. Add to shortcuts row > *Voice assistant (Gemini: start talking)* opens the phone's voice assistant.


## Automatic overlap fix (1.1.27)
With the free layout on, switching an item back on (for example the + Add circle) briefly opens Move, which moves overlapping items apart and closes by itself.


## How the overlap fix chooses a place (1.1.28)
An item on top of another goes first to a free gap on its own row (the closest one, the end of the row included); if there is none, the row is packed side by side with the moved item last; only if the row is full does it go to the next line.


## Voice assistant choices, + circle, wrapped items (1.1.30)
Add to shortcuts row > Voice assistant: *Gemini: talk* or *Google voice search*. In the free layout, choosing a shortcut hides the + circle (Settings > Shortcuts row has the switches). The overlap fix lines wrapped items up on a new line under the full row.


## Fix layout, popup after Clear all (1.1.31)
Pencil > Fix layout closes the gaps and lines the circles and buttons up again (free layout). Settings > Counts & clearing can close the bottom popup after Clear all.


## Fix layout order and refresh icon position (1.1.32)
Pencil > Fix layout: circles and buttons at the top, Inbox / Clear all / count on the last line above the messages, refresh icon right under them. Settings > Show / hide parts has the switch for the refresh icon position.

## Auto-fix after every move, pinned Fix layout (1.1.33)
In Move, the switch *Auto-fix after every move* (on by default) separates items that end up on top of each other and keeps the line *Messages start here* below an item you drop on it. The Fix layout and Done buttons stay at the bottom of the screen. Fix layout now lines each row up on the middle of the circles and keeps the same space between rows.


## Animation
Settings > Animation: one section for everything that moves in the panel. For each part pick a style, or Off: Note, Clear all, the gear, refresh, the mic, the pencil, the Inbox title, the count badge, the panel opening, a new message tile, touch feedback on tiles and buttons, how tiles appear, and how the panel closes (swipe the sheet down to close it). Speed (Slow, Normal, Fast) applies to all of them. The switches at the top are: Animation (master), Panel animations, and Widget: icons move when something happens. The mail icon changes with the Inbox (📬 → 📭 while it is empty). Your own emoji is shown as you picked it; set a separate one under "when empty" to change it. **Animation picker**: the button at the top of this section opens a panel like the emoji keyboard (tabs at the bottom, styles above); tap a style to choose it and see it in the live preview. New styles: Flip up, Drop in, Unfold (panel), Wobble and Hop (gear), Pulse and Flip (refresh), Blink (mic), Shake and Flip (title, badge), Pop and Drop (new message), Grow and Tilt (touch), Slide and Zoom (tiles appear).


## Search, number boxes, 30 themes (1.1.63)
- **Search the manual**: type in the *Search the manual* box at the top of the manual (bottom of Settings). Only the matching lines show, with their heading.
- **Number boxes on sliders**: every slider in Settings has a box beside it. Type a number to move the slider, or drag the slider to change the number.
- **8 new themes** (30 in all): Ember, Plum, Olive, Teal, Peach, Paper, Ink and Synthwave. They are at the end of the theme list.
- **Your emoji icon is shown as you picked it.** Only the mail icon changes while the inbox is empty.

## Ask the helper (1.1.65)
At the top of Settings, under HELPER, type what you want in a sentence and press *Make this change*. It works without internet and changes the setting straight away. It understands sentences that name the part (text, widget, animations) and what to do with it, such as *make my text a bit bigger*, *I want the widget see-through* or *turn the animations off*. It can also change an icon: *change the inbox icon to 📬* sets that emoji, and *change the notes icon* opens the emoji list for it. It is a set list of words, so unusual wording may not work; those open the matching Settings in the search box. Examples: *transparent widget*, *solid widget*, *bigger text*, *smaller text*, *animations off*, *animations on*, *dark*, *light*, *random*, or a theme name such as *Ocean*. For any other setting, it finds the one whose name matches your words best. A switch in that setting is turned on or off, and a number box takes the number you say (*gap 12*). Otherwise it shows the setting and says where it is. You can also say *change the font to casual*, *match the gap with the size*, *turn the clock on* or *make the accent red*. Switches, number boxes, choice lists and palette colours can be set this way. Colour rows that open the colour picker are shown instead; reset and delete buttons are never pressed by a sentence. Undo covers these changes.

## Layout by sentence (1.1.70)
In the HELPER box at the top of Settings you can also say: *row gap 10*, *free gap bigger*, *clock width 150*, *make the weather taller*, *move the date to the top row*, *move the mic to the bottom row*, *merge the clock and the date*, *split the top items*, *hide the weather* or *show the title*. Sizes are percentages (50 to 250). Free drag positions are not changed by sentences; drag those in Move.

## Speak, undo, and follow-up sentences (1.1.71)
Press *🎤 Speak a change* to say it instead of typing; the words go into the box and you press *Make this change*. Say *undo* or *redo* to take back or repeat the last change. After a change, *it* and *that* mean that setting: *make it bigger*, *change it to red*. Order inside a row: *move the clock before the weather*. Free places: *move the clock left* nudges it (after you have dragged it once in Move).

## Smarter helper (1.1.72)
The helper understands many ways of saying the same thing, such as *colour* or *color*, *font* or *text*, *space* or *gap*, and small typos. It only picks short setting names. If the setting is in a closed section, it opens that section, scrolls to the setting and highlights it.
