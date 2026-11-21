Notif Tiles v0.5

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
Blue mic button next to "+ Note" in the panel: opens your phone's own Recorder app (ready to record).
Change the app in Look & colors > Voice recorder.
Fast dial: Look & colors > Fast-dial contacts lets you change or clear any circle. Turn on "Ask before
calling" and tapping a circle on the widget offers Call / Change contact / Remove.
Borders: Look & colors > Borders (thickness, colour) around the panel and the message tiles.
Text size per part of the panel: Look & colors > Font > "Text size per part of the panel".
