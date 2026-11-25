# Release notes

## 1.1.46
- **Panel animations rebuilt from scratch.** They are now drawn on an overlay over the panel and tied to what each part does:
  - **Note:** a pen writes a line under the word as you type; a ring and a tick stamp in when saved.
  - **Clear all:** the messages drop into the bin, the lid lifts and the bin bounces (or a soft band sweeps the list).
  - **Gear:** one smooth turn that settles (or turns and overshoots), with a soft glow.
  - **Refresh:** an arrow draws a full loop (or a smooth turn).
  - **Mic:** live sound bars move like a voice while the recorder opens (or red rings and a pulse).
  - **Pencil:** draws a trail and rides it (or tilts with ink dots).
  - **Inbox title:** an envelope drops into the title (or a bounce).
  - **Count badge:** the number rolls up to the new count (or a pop).
  - **Panel opening:** the sheet rises with a spring and the header follows (or a fade).
  - **New message tile:** slides in with a highlight (or a fade).
- The icons keep the emoji and colour you chose; the animation draws on top.
- Settings > Animation: the style names changed to match. Speed and the switches are the same.
- Not compiled here (no Android SDK in the build environment): please let the GitHub build run and try it.

## 1.1.45
- **Animation: one section for everything that moves in the panel.** Settings > Show / hide parts no longer has the Alive icons block; the new **Animation** section has it all:
  - **Style for each part** (or Off): Note (writing hand, pen bounce, sparkle), Clear all (basket, broom, poof), gear (big spin, small turn, wobble), refresh (two turns, one turn, pulse), mic (red rings, pulse, wave), pencil (write a line, wiggle, sparkle), Inbox title (flag up, bounce, shake), count badge (pop, grow, wiggle), panel opening (header icons pop in turn, slide down, fade in), new message tile (slide in, fade in, drop in).
  - **Speed** for all of them: Slow, Normal, Fast.
  - Switches: Animation (master), Panel animations, Widget: icons move when something happens.
- The widget is unchanged.
- Not compiled here (no Android SDK in the build environment): please let the GitHub build run and try it.

## 1.1.44
- **Every panel icon has its own animation story** (press the icon):
  - **Note**: becomes a writing hand (✍️ 🖊️) that travels along the line while you type, and a ✅ pops in when the note is saved.
  - **Clear all**: a basket 🧺 catches the messages 📨 💬 📩 one by one, tips over and pours them out while the list empties, then stands up with a ✅.
  - **Gear**: spins up and slows down, then Settings opens.
  - **Refresh**: turns twice while it refreshes.
  - **Mic**: red rings spread out like a live recording, then the recorder opens.
  - **Pencil**: writes a line and ends with a ✨, then its menu opens.
  - **Mail box (Inbox title)**: the flag goes up (📪 📫 📬) when a new message arrives; the count badge pops.
- The widget keeps its own reactions (gear after Settings, refresh while refreshing, writing hand while typing a note, red mic while recording, broom on Clear all, mail flag on a new message).
- Settings > Show / hide parts > Alive icons: same switches as before.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.43
- Fix for the build error of 1.1.42 (Settings had two onPause functions). Nothing else changed.

## 1.1.42
- **Alive icons, done properly: an icon moves when something happens that it stands for, and is still the rest of the time.** (1.1.41 kept every icon moving all the time; that is gone.)
- **Widget:**
  - **Gear** spins and slows down when you come back from Settings.
  - **Refresh** turns while the widget refreshes (weather and counts) and stops when it is done.
  - **Note** shows a writing hand (✍️ 📝 🖊️) while you type a note.
  - **Mic** pulses (red) while recording.
  - **Clear all** shows the broom sweeping (🧹 🧼 🫧 ✨) when you clear the inbox.
  - **Mail box**: the flag goes up (📪 📫 📬) when a new message arrives.
  - **Pencil** wiggles when its menu opens.
- **Panel (real animation):** tap the gear and it spins before Settings opens; the broom swings before Clear all; refresh turns; the pencil wiggles; the mic pulses; the Note icon wiggles while you type; the count badge pops and the Inbox title bounces when a message arrives.
- Icons still change with the Inbox (📬 → 📭, 🔔 → 🔕, 🧹 → ✅ while it is empty).
- **Settings > Show / hide parts > Alive icons:** master switch, *Widget: icons move when something happens*, *Panel: icons move when you tap them*.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.41
- (Replaced by 1.1.42: icons that moved all the time.)

## 1.1.40
- **Alive icons.** Settings > Show / hide parts > *Alive icons* (on by default, three switches).
- **Icons change with the Inbox.** More icons now look different while the Inbox is empty: 📥 ✉️ 📨 📩 💌 📧 → 📭, 🔔 → 🔕, 💬 → 💤, 🧹 🗑️ 🧼 🪄 → ✅, 🎙️ → 🎤, 📝 → 📓 (and 📬 → 📭, 📫 → 📪 as before). Your own *when empty* emoji always wins.
- **Widget: icons blink one after another when a new message arrives** (title, count, mic, + Note, Clear all, refresh, pencil, gear). A widget cannot run a real animation, so this is a few quick redraws that only change the opacity; the widget looks normal again afterwards.
- **Panel: real animation.** The header icons pop one after another when the panel opens, and the Inbox title breathes slowly while there are messages (it stays still when the Inbox is empty).
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.39
- **Icons that change with the inbox.** The Inbox icon 📬 (mail in the box) now turns into 📭 (empty box) by itself while there are no messages, and back to 📬 with the next one (📫 turns into 📪). Works in the widget and in the panel.
- **Your own icon for the empty state.** Emoji icons (pencil menu or Settings) has new lines: *Inbox title when empty*, *Clear all when empty*, *Mic when empty* and *Notes when empty*. Pick any emoji; "Normal icon" means the same as the full one.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.38
- **Parts that show only when there are messages.** Settings > Show / hide parts > *Only when there are messages*: switch it on for the Inbox title, the mic, + Note and Clear all / Read all. While the inbox is empty (after Clear all, Read all or Mark as read) they go away, and they come back with the next message. The count badge already worked this way. Everything is off by default, so nothing changes until you switch one on.
- The Move screen still draws every part, so you can place them even when the inbox is empty. The settings gear and the pencil never hide.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.37
- **Choose the buttons under a message.** New section in Settings: *Message buttons*. The buttons are: the app's own buttons (View, Notifications ...), Mark as read, Snooze, Mute chat, Open, and Clear all of the app. Tick to show or hide each one, move it with the up and down arrows, or switch on *Sort by how often I use them* so the most used come first. It shows how many times you used each and can reset the counts.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.36
- **Mark as read now counts as really opened and read.** The app's own *Mark as read* button (Facebook, ...) used to do nothing when the app answered it by opening a screen, and the app then posted the same notification again so it came back. Now it is always counted as read here, and **the same notification posted again stays hidden** (the action log says what happened).
- Also hidden when only the numbers change (Facebook "Waiting for you: 20 messages, 7 new..." becoming "21 messages"). Switch: Settings > Counts & clearing, *Mark as read also hides the same notification when the app posts it again with only the numbers changed* (on by default). It is remembered for 3 days.
- Open, Mark as read and Read all all do this. Chats with a message list (WhatsApp, Teams ...) are not affected: they are told apart by the time of each message.
- **The panel buttons no longer squeeze** ("Mar / k as / read"): the row scrolls sideways and each button stays on one line.
- Includes 1.1.33 to 1.1.35. Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.35
- **Clear the undo history.** ☰ History (Settings, Arrange & resize, Move and the pencil menu) has a new **Clear history** button. It asks first, then deletes the undo and redo steps of that list; the widget stays as it is.
- Includes 1.1.33 and 1.1.34. Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.34
- **The pencil menu scrolls.** Emoji icons, Reset and the Undo bar were cut off below the screen when the cards got longer; now the list scrolls.
- **Fix layout button pinned at the bottom of the pencil menu**, always on screen.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.33
- **Auto-fix after every move (Move screen).** When you let go of an item, the widget checks by itself: items that now sit on top of each other are separated (the one that moved last gives way), and if the item lands on the line *Messages start here*, that line moves down below it. One Undo takes back the move and the fix together. Switch: *Auto-fix after every move* at the top of Move (on by default).
- **A Fix layout button that always stays on screen.** The bottom bar of Move (Fix layout, Done) no longer scrolls away.
- **Fix layout lines rows up on the middle of the circles.** Circles and hexagons have a name under them, so a circle and a text button (+ Note, mic, pencil, gear) used to sit at different heights. Rows are now lined up on the middle of the shape, and the space between rows is the same everywhere (follows the gap slider).
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.32
- **Fix layout now puts things where you want them.** Shortcut circles first, then + Note, mic, pencil and gear on the same rows (a new line only when a row is full); **Inbox, Clear all and the count always go on the last line, just above the messages**. The line *Messages start here* moves to sit right under them.
- **Refresh icon ("Reset" icon with the Updated line) can sit at the top.** New switch in Settings > Show / hide parts: *Refresh icon at the top, right under the buttons (off: middle of the widget)*. Fix layout turns it on. It is the icon shown when there are no new messages.
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.31
- **One-tap Fix layout in the pencil menu.** After you delete or add items, pencil > **Fix layout** lays out everything below the clock and fast-dial rows again (shortcut circles, title, count, mic, + Note, Clear all, pencil, gear): in reading order, side by side with a small gap, a new line only when a row is full. Gaps left by deleted items close, and the line *Messages start here* moves to sit just under them. Move opens for about a second and closes; Undo in Move takes it back. The same is in Move as *Fix layout: close gaps and line everything up*. It works in the free layout (after you moved an item).
- **The bottom popup closes after Clear all / Read all** instead of staying open and empty. Switch in Settings > Counts & clearing: *Close the bottom popup after Clear all / Read all* (on by default).
- Nothing else changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.30
- **Voice assistant choices fixed.** Add to shortcuts row > *Voice assistant* now offers two clear choices: **Gemini: talk** (opens Gemini ready to listen, the "What's next" screen) and **Google voice search** (the "Listening... Search, AI mode, Talk, Song" screen). Before, the first choice tried Google voice search first, so it did not open Gemini. Gemini's own Live shortcut, if listed, is still under *Shortcut*.
- **No + circle after you choose a shortcut.** In the free layout, choosing a shortcut now hides the + (Add) circle so no new item appears and crowds the row. Switch it back on in Settings > Shortcuts row > *Show the + (Add) circle*; the new switch *Free layout: after I choose a shortcut, hide the + circle* turns this behaviour off.
- **Overlap fix keeps wrapped items tidy.** When a row is full, the items that do not fit now move to a new line just under it and line up from the left in their order, instead of being scattered. The messages start lower if needed so nothing sits on top of them.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.29
- Fixes the 1.1.28 build error (a helper that closes the automatic Move screen had been removed by mistake). No other change.

## 1.1.28
- **Smarter overlap fix: keeps things on their row.** In 1.1.27 an item that overlapped another (for example + Note on the + Add circle) was pushed down to a new line even when the row had room for it. Now, in this order: (1) it goes to a free gap on its own row, the one closest to where it was, including the end of the row; (2) if there is no gap, the items of that row are packed side by side in their order with the moved item last, when they all fit; (3) only then does it go to the right of what it overlaps or to the next line.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.27
- **Hiding and re-showing an item no longer leaves it crammed.** In the free layout, every time something is switched on again (the + Add circle of the shortcuts row or fast dial, an item in Show / hide parts or in Arrange > Every item, the pencil, refresh, or a new shortcut circle), Move now opens for about a second by itself, moves whatever overlaps to a free place, and closes. You do not have to open Move. If you switch an item on from somewhere that cannot open it, the fix runs the next time you open Move. *Tidy up: fix overlapping items (smart)* is still there to run by hand, and Undo in Move takes a fix back.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.26
- **Smart fix for crowded items.** In the free layout (after you moved an item), a new shortcut circle could land on top of + Note, the mic and the other buttons. New button in Move: **Tidy up: fix overlapping items (smart)**. Items on top of each other are moved to the right of what they overlap, or to the next line when there is no room; the rest stays. It also runs by itself, once, the next time you open Move after you add a shortcut circle. Undo (in Move) takes it back.
- **Voice assistant shortcut.** Add to shortcuts row now has **Voice assistant (Gemini: start talking)**. It opens the phone's voice assistant (Gemini, when it is the default assistant) trying voice mode first, then the assistant screen, then the app. Which of these starts listening straight away depends on the phone and the assistant. Gemini's own Live shortcut, if the app lists it, can also be found under *Shortcut*.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.25
- **New items in the bottom popup (panel) header**, each with its own show / hide switch in Settings > WIDGET AND PANEL > Show / hide parts > *Widget header, tiles and panel*: **‹ Back** (shown as before; switch it off to hide it), **Mic** (opens the recorder), **Pencil** (opens the pencil menu) and **Refresh** (refreshes counts and weather). Mic, Pencil and Refresh start **hidden**, so nothing changes until you switch them on. They use the emoji you chose for Mic, Pencil and Reset, if any.
- Correction to 1.1.24's notes: the panel's Inbox title, count, + Note, Clear all and settings icon switches are in that same Show / hide parts list, not in Arrange > Every item.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.24
- **Back button is back to closing the panel.** In 1.1.22 Back inside an opened message went to the tile list first. Now Back closes the panel again, as before. If you like the 1.1.22 way, Settings > MESSAGES > Pages, shortcuts & grouping > *Back button in an opened message goes to the tiles first* turns it on.
- **Hide the panel's settings icon:** new item *Panel: settings icon* in Arrange & resize > Every item (and Settings > Arrange the widget). The Inbox title, count badge, + Note and Clear all already have their own show / hide switches there.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.23
- **Swipe-through: tap the name line too.** With Swipe-through on, only the top line of a tile (app name and count) opened it. New switch under Swipe-through, **Tap the name line too**: the sender / chat name under the app name opens the message as well, while the message text below still passes swipes to your launcher. (*Tap anywhere on a tile* still opens from the whole tile.)
- **Less flicker in the panel.** The panel redrew itself on every notification update, even when nothing visible had changed, which made an opened message flicker. It now redraws only when the messages, notes or the opened message actually changed.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.22
- **Snapchat (and other empty alerts): the opened message is no longer blank.** When an app gives no text and no picture (Snapchat only sends a short alert, or a "someone is typing" alert was already replaced), the card now says so and a tap opens the app, instead of showing an empty card.
- **Back button:** the phone's Back button now goes from an opened message back to the tiles first, and a second Back closes the panel.
- **Panel tile taps are written to the action log** ("tile tapped: ..."), to help find why tiles may stop responding on some phones.
- Nothing existing was changed. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.21
- **Emoji for Settings and Inbox.** *Choose emoji icons...* (Settings > LOOK, and the pencil menu > *Emoji icons*) now has two more places: **Settings (gear)** on the widget and in the panel, and the **Inbox title** (shows e.g. "📥 Inbox" on the widget and the panel). *Normal icon* goes back, as before.
- **More emoji to choose from** in every place (Notes, Mic, Reset, Clear all, Pencil: 15 more each), plus full lists for the gear and Inbox. You can still type or paste any emoji of your own.
- **Message tiles in the panel: new looks** (Settings > LOOK > Shapes & borders > *Message tiles in the panel*): corner roundness, your own tile colour (or each app's own colour), fill solidity, and border style Solid / Dashed / Dotted (thickness and colour stay in *Borders > Message tiles*). *Message tiles back to as designed* clears them. Undo / Redo / History cover these too.
- Nothing existing was changed: with these left alone everything looks exactly as in 1.1.20. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.20
- **Emoji icons for Notes, Mic, Reset, Clear all and the Pencil.** Settings > LOOK > Themes & accent colour > *Choose emoji icons...* and the pencil menu > *Emoji icons* let you put an emoji in place of the normal icon or word: Notes shows e.g. "📝 Note" instead of "+ Note", the mic and the refresh (reset) button become emoji pictures, Clear all gets an emoji in front, and the pencil itself can be changed. Pick from a list or type / paste any emoji of your own; *Normal icon* goes back. The red stop icon still shows while recording. The panel's + Note and Clear all follow too.
- **More themes (14 new):** Lavender, Mint, Coffee, Neon, Candy, Slate, Crimson, Gold, Sky, Sand, Cyber, Aurora, Cherry and Graphite. Some (Candy, Gold, Cherry) also set their own emoji icons. The old themes keep their places, so a saved Auto day / night choice still points to the same theme.
- **Theme buttons show their own colours**, and a **🎲 Random** button picks a theme for you (Settings and pencil).
- **Pencil menu: Themes** (pick a theme straight from the pencil, or Random) and **Emoji icons**. Undo / Redo / History cover both.
- **More colours everywhere:** the colour picker palette grew from 40 to 80 colours (pastels, neon, deep, earth and strong colours), and the quick swatch rows for accent, backgrounds, text, borders and the dim layer have extra colours.
- Nothing existing was changed: with no emoji chosen the widget and panel look exactly as before. Not compiled or tested here: please let the GitHub build run and try it.

## 1.1.9
- **Fix: Pencil > Reset left the widget piled up.** The pencil's Reset only refreshed the texts, so the old moved places stayed on screen (clock, weather, date and Inbox on top of each other) while the saved values were already back to standard. It now redraws the whole widget exactly like Settings > Reset everything to automatic (both use the same reset).
- **Undo works one step at a time, separately for the pencil and for Settings.** Settings has an **Undo last change (N)** button pinned under the preview; the pencil has the same in its menu and under the preview on Move and Arrange & resize. Each keeps its own list (up to 25 steps) and never touches the other's. A Reset or a loaded layout is one step like any other change; a Reset that changes nothing is not a step, so it can no longer overwrite the copy you wanted to go back to.
- **Undo now covers everything you change**, not only the look: contacts, shortcuts, notes, filters, weather place and switches too (also what you change in screens opened from Settings). Not covered: what the app fills in itself (weather readings, page numbers, snooze timers, recent searches), saved layouts and background pictures.
- **Undo, Redo and History.** The bar now has three buttons: Undo (one step), Redo (puts back a step you undid) and History, a list of everything done with a short description and the time (for example Look: font size). Tap a line to go back to just before it, or tap a line above 'now' to go forward again, so you decide how much to undo. The pencil and Settings keep separate lists; a new change clears the Redo list.
- **Gmail (and other grouped apps) leave the phone's notification shade too.** The × in the panel, Clear all and marking read removed only the messages and left the app's group bar ("1 new message") behind. When the last message of a group goes, its group bar is now removed as well.
- **Swipe-through tiles can be tapped again.** Tap the top line of a tile (app name and count) to open it; swipes over the rest of the tile, the rows and the empty space still reach your launcher. A new switch under Swipe-through, "Tap anywhere on a tile", makes the whole tile open on a tap (a swipe that starts on a tile then stays with the widget).
- **Swipe-through:** with it on, the hidden scrolling list is now removed from the widget completely, because even an empty list can take a swipe from the launcher.
- Undo after a Reset no longer leaves a stale "Undo last move" behind.

## 1.1.8
- **Swipe-through tiles are now display-only.** In 1.1.5-1.1.7 the fixed tiles could still be tapped, and a view that takes taps also takes the touch, so a swipe starting on a tile still did nothing in Nova. They now take no taps, so swiping down over them reaches the launcher. Open the panel with your launcher gesture or by tapping the Inbox title or count. Off: the list scrolls and tiles open as before.

## 1.1.7
- **Same size for a whole row** (Arrange & resize > Every item): each group (Top row, Fast-dial circles, Shortcuts row circles, Inbox row) has a tick box "Same size for every item in this row". Ticking it sets the whole row to the size of its first item, and from then on moving the Size, Shape width or Shape height slider of any item in that row moves all the others too, so sizes can't get mismatched. Untick it to size items one by one again. Reset clears the ticks.

## 1.1.6
- **Fix: blank white widget in 1.1.5.** A widget layout may only use certain view types, and 1.1.5 added a plain View that Android refuses to draw, so the whole widget came out empty. It now uses an allowed one. Swipe-through is unchanged.

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
