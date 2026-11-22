NEW IN v0.9.25: Look & colors now shows the installed version under its title.

NEW IN v0.9.24: Exit navigation now proves itself: it presses Exit, checks the Maps notification is really gone, and if not it
taps Exit inside Maps; every result is shown in a message. Look & colors > Where to? has a "Self-check" button (shows
notification access, the Maps Exit button, the adb Location permission and the Accessibility helper) and "Test: exit navigation
now". When Google's Location pop-up appears, a message says why. Nothing from older versions was removed.

NEW IN v0.9.23: Exit navigation is more reliable. If the Google Maps notification has no Exit button to press, the Accessibility
helper now opens Maps and taps its own Exit button (when ON). The right Maps notification is picked even when Maps shows several.
Location: after switching on with the one-time permission, Notif Tiles checks that Android really did it, and if not the normal
Google "Turn on" pop-up is used. Nothing from older versions was removed.

Notif Tiles v0.9.22

LATEST (v0.9.22) - older versions are listed below, newest at the bottom.

NEW IN v0.9.22: Quiet & snooze (Look & colors > Quiet & snooze): snooze the widget for 30 min / 1 h / 3 h / until 7:00, or set quiet
hours; the widget and its count stay empty and say "Snoozed until ..." while the panel still lists your messages.
Mark all read: Look & colors > Counts, choose what the Clear all button does (Clear all or Mark all read; the button then says
Read all). Apps: pin, mute, colour (own section): pinned apps come first, muted apps are hidden, each app can have its own tile
colour. Auto theme (Themes & accent): day theme and night theme switch by themselves at times you choose. While Google Maps is
navigating, the Where to? box on the widget turns into a red "Exit navigation" (one tap). Battery: the Location-off check no
longer runs every few seconds (the notification event does it instantly, with a 20 second safety check), the pop-up watcher
slows down while the screen is off, and snooze / quiet hours / auto theme share a single inexact alarm (none when unused).

NEW IN v0.9.21: Location now switches off straight away when the Google Maps navigation ends (closing the pop-up, Exit
navigation, or any other way), instead of waiting for the next check. The widget's "Nothing new from your apps" text and the
"Updated 14:05" word can be changed (Look & colors > Text, fonts & text colours > Widget wording) and both have their own colour
under Text colour per part.

NEW IN v0.9.20: Themes now set the whole look: icon colour, font, shape of the circles, circle colour and borders (Look & colors >
Themes & accent). Clear all, the settings icon, the refresh icon and the mic now follow the theme's icon colour (new row "Icons &
Clear all colour" under Accent colour; a colour picked per part still wins). More shapes for the clock / weather / date / fast-dial
circles: Round, Rounded square, Square, Teardrop, Leaf, Diamond, Hexagon, Octagon (Look & colors > Shapes, circles & borders).
Picking a theme puts the per-part colours of mic, Clear all, settings and refresh back to automatic so the theme colour shows.

NEW IN v0.9.19: Videos sent in a chat are shown in the opened message card with a play button and play right there (when the chat
app lets Notif Tiles read the video). If the app only gives a still picture, tapping play opens the chat.

NEW IN v0.9.18: Location is now also switched off after a navigation started from Where to? when it was already on (switch
"...even if Location was already on before"), and a test button "Test: switch Location off now" in Look & colors > Where to?
shows whether this phone lets Notif Tiles do it (needs the one-time adb permission).

NEW IN v0.9.17: Closing the small Google Maps pop-up now also exits the navigation (needs the Accessibility helper ON; switch in
Look & colors > Where to?). Opening the pop-up to full screen does not end it.

NEW IN v0.9.16: Weather city box now shows the current city and a small list of matching places while you type
(tap one to use it). The refresh icon no longer opens the phone's weather app.
Mic: with the Accessibility helper ON it still uses your phone's recorder. With the helper OFF it now records inside this
app (asks for microphone permission once), saved as m4a in Recordings/NotifTiles; tap the mic again, or "Stop & save" in the
notification, to finish. Switch it off in Look & colors > Voice recorder to get the old behaviour back.
While recording the widget mic turns into a red stop square and the phone buzzes; a message says it started and, when you
stop, the saved file name. Files: Internal storage > Recordings > NotifTiles.
Notes: new switch in Look & colors > Notes, "+ Note opens the notes app straight away". Off (default) keeps the type-here box.
Message time stamp (Look & colors > Text, fonts & text colours): Smart = only the time today, "Yesterday, time", older shows the date.
Other choices: Date + time (the old look), Time only, None.
Where to?: with the one-time adb permission (adb shell pm grant com.example.notiftiles android.permission.WRITE_SECURE_SETTINGS)
Location is switched on with no pop-up and switched off again when the Google Maps navigation ends (only if Notif Tiles
switched it on). Without the permission the normal Google Turn on pop-up is used.
Where to? straight to Google Maps: switch in Look & colors > Where to? (off by default). Opens Maps and, with the Accessibility
helper ON, taps its search bar so you get Maps' own suggestions, Home, Work and Recent.
Where to? extras: a red "Exit navigation" button appears at the top while Google Maps is navigating. While typing there is a
row "Show ... in Google Maps (all matches)", and with an empty box a row "Open Google Maps" (its own search and suggestions).
Photos: when a chat app sends a photo and lets other apps read it, the photo is shown inside the opened message card in the panel.

NEW IN v0.9.15
Settings icon is now a real icon (not a text character), so its colour and size follow Look & colors
(widget and panel). New sliders: widget settings icon, widget mic icon, refresh icon size.
Small refresh icon in the middle of the widget (under "Nothing new from your apps"): re-counts messages,
redraws the widget and refreshes the weather. Hide it or recolour it in Look & colors.
Weather refreshes about every 15 minutes by itself and retries after 2 minutes if a fetch fails.
Weather source (Open-Meteo, ECMWF, ICON, Yr), a temperature adjust of +/- 6 degrees, and a small
"Updated 14:05" line under the refresh icon (can be hidden).
Backup & restore (Look & colors): export all settings, notes and background pictures to one file, import it later.
BUILD: the app is now signed with a fixed key (app/notiftiles-debug.keystore), so new builds install over old ones.
You must uninstall once when moving from an older build; after that, updates install over. Every push to
main/master builds automatically, and the APK is attached to a GitHub release (Releases page) as well as the
Actions artifact. If your repo is public, anyone can sign with this key: keep the repo private.

Handled messages leave the phone's notification panel too: when you read a message here (leave it or press Back),
reply, open it, or tap the app's "Mark as read" button, its notification is cleared from the phone's shade.
Turn it off in Look & colors > Counts > "Also remove it from the phone's notification panel".
(This clears the notification only. It does not mark the chat as read inside WhatsApp etc.)

Weather sources: more models plus "Nearest airport (observed)". Look & colors > Widget layout, clock & weather >
"Compare all sources now" lists each source's temperature; tap the one that matches your phone's weather app.
The refresh icon now shows a message ("Refreshing…", then the temperature and source it got).

Phone weather app temperature: Look & colors > Widget layout, clock & weather > "Use the temperature shown in my
phone's weather app". Tap the weather circle on the widget: your phone's weather app opens and the Notif Tiles
Accessibility helper copies the big temperature it shows into the widget (the "Updated" line says "Phone weather app").
Needs the helper ON (Accessibility > Notif Tiles recorder button; after a reinstall switch it on again, and
Allow restricted settings first if it is greyed out). It updates each time you open the app and keeps showing that number; the
online source is only used until the first number is copied, or if you turn the switch off.

Refresh icon: only refreshes the message counts and the online weather. It never opens the phone's weather app.
The phone's weather app opens (and its number is copied) only when you tap the weather circle on the widget.

Weather modes (Look & colors > Widget layout, clock & weather): Both (recommended) / Phone app only / Online only.
Both: after you copy the phone weather app's number (tap the weather circle on the widget) the widget shows that exact number for 20 minutes, learns the difference to the online source, and then
keeps updating from the online source with that difference added, so it stays close to the phone without opening the app.

============================================================
EARLIER VERSIONS
============================================================

NEW: home-screen widget (BlinkFeed-style tile grid with counts and app colors),
two-column colored tiles in the panel, tap a tile to expand it with reply and the
app's own action buttons, per-app Clear, more phone/SMS sources.

BUILD
GitHub Actions: push this folder, Actions > Build APK > Run workflow, download
the notiftiles-apk artifact. Or open the folder in Android Studio.

UPDATING FROM v0.1
Debug builds from GitHub are signed with a different key each run, so Android
will refuse to install over v0.1. Uninstall v0.1 first, then install v0.2, and
redo the setup (notification access, usual apps).

SET UP
1. Open "Notif Tiles setup". Grant notification access (Allow restricted settings
   first if it is greyed out). Set battery to unrestricted.
2. Tap "Turn on my usual apps".
3. Long-press Nova home screen > Widgets > Notif Tiles, drop it on an empty page.

NEW IN v0.5
Widget top strip: clock circle, weather circle (Amman, Jordan, tap it to refresh),
and a location pin. Five fast-dial circles: tap an empty one to pick a contact, tap
a filled one to call. Change or clear them in "Notif Tiles setup" > Fast dial.
The first call asks for phone permission; if you refuse it opens the dialer instead.
The new widget is taller: if your existing widget looks cramped, resize it or re-add it.
Tap the Maps "Where to?" pill: Location is switched on (one-tap Google prompt) and Google Maps opens.
Widget taps: clock opens the Clock app, date circle opens Calendar, weather circle opens your
installed weather app (and refreshes the temperature), pin asks where to go.
Look & colors: tap the gear on the widget header or on the panel header (or the button
in Notif Tiles setup). Control panel background, shadow behind the panel, widget background
and opacity. "Reset" goes back to automatic light/dark.
Themes + accent colour + "Arrange the widget" (move/hide clock, weather, date, Maps box, fast-dial) are in Look & colors.

NEW IN v0.6
"Where to?" pill now turns Location on (one-tap prompt), then opens its own search box with the
keyboard already up: type or use the mic, pick Home/Work/recent places or suggestions, choose
Drive/Walk/Bike/Transit, and Google Maps opens straight to that destination.
Customise it in Look & colors > "Where to?" search (default mode, Home/Work addresses, voice button,
suggestions, recents, auto-start navigation, auto Location).

NEW IN v0.7
Look & colors now also has: Font (family, weight, italic, text size) for the widget, panel and
Where to? box; text colour; circle and fast-dial circle colours; round or rounded-square shape;
circle sizes; names under fast-dial on/off; background PICTURES for the widget, the fast-dial strip
and the panel (with strength slider); fast-dial strip colour + opacity; 12/24-hour clock; date
format; weather city and C/F.

NEW IN v0.8
The count now goes down when you read a message in the panel (leave it or press Back) and when you
reply from the panel (the notification is also tidied away). A newer message from the same chat
counts again. Turn either off in Look & colors > Counts. The widget also re-counts a moment after
any notification is removed, so reading a chat in its own app updates the count more reliably.

NEW IN v0.9
"+ Note" now sends what you type to the notes app on the phone (Google Keep, Samsung Notes, ...).
Choose the app, "Ask each time" or "Inside Notif Tiles" in Look & colors > Notes. Notes you already
saved inside Notif Tiles stay until you delete them.

NEW IN v0.9.1
Notes: if your notes app isn't one of the known ones, you pick it once from a list of every app that
accepts text (also in Look & colors > Notes > "Choose any other app"), and notes then go straight to it.

NEW IN v0.9.2
Notes: "Phone default" now asks Android for the phone's own default Notes app. The pick list shows all
your apps (notes-like names first). If the app you pick can't take shared text, the note is copied
and the app opens so you can paste it.

NEW IN v0.9.4
Blue mic at the start of the widget header (before "+ Note"): tap it and the phone's Recorder (ColorOS) starts
recording straight away using its own start action; tap again to save (Cube button action). If it only opens the
Recorder, pick another start action in Look & colors > Voice recorder and use the test button.
Fast dial: Look & colors > Fast-dial contacts lets you change or clear any circle. Turn on "Ask before
calling" and tapping a circle on the widget offers Call / Change contact / Remove.
Borders: Look & colors > Borders (thickness, colour) around the panel and the message tiles.
Text size per part of the panel: Look & colors > Font > "Text size per part of the panel".

NEW IN v0.9.6
The phone's Recorder ignores start requests from other apps, so the mic now has a helper: turn on
"Notif Tiles recorder button" in Accessibility and the mic opens the Recorder and presses its red record
button for you. Adjust where it taps in Look & colors > Voice recorder > Red button position.
