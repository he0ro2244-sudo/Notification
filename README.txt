Notif Tiles v0.4

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
