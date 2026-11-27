"""Writes app/src/main/java/com/example/notiftiles/HelperSettings.kt: the list of settings the helper can change by name.
Edit SPEC below (a setting's name in Prefs.kt, its kind, range and the phrases people say), then run:  python3 tools/helper/gen_settings.py
kind: b = on/off, i = number, c = colour (0 = automatic where the app has that)."""
import os, sys

# (Prefs name, kind, min, max, step, "Label shown to the person", [phrases], [phrases where "more" means less, e.g. transparent])
SPEC = [
    # --- numbers: sizes, opacity, gaps
    ("scrimAlpha", "i", 0, 100, 10, "Panel shadow strength", ["panel shadow", "shadow strength", "scrim strength", "dim behind panel", "panel dim"], []),
    ("widgetAlpha", "i", 0, 100, 10, "Widget opacity", ["widget opacity", "widget background opacity", "background opacity", "widget solidity"],
        ["widget transparency", "widget transparent", "background transparent", "background transparency"]),
    ("dialBgAlpha", "i", 0, 100, 10, "Fast-dial strip opacity", ["dial strip opacity", "fast dial strip opacity", "strip opacity"], ["strip transparent", "dial strip transparent"]),
    ("fontScale", "i", 70, 160, 10, "Text size", ["text size", "font size", "letters size", "writing size"], []),
    ("updSize", "i", 50, 250, 10, "Updated line size", ["updated line size", "updated size", "update line size"], []),
    ("topSize", "i", 36, 72, 4, "Clock / weather / date circles size", ["top circles size", "top size", "clock circle size", "weather circle size", "date circle size"], []),
    ("gearSize", "i", 14, 40, 2, "Settings icon size", ["settings icon size", "gear size", "gear icon size", "cog size"], []),
    ("refreshSize", "i", 10, 32, 2, "Refresh icon size", ["refresh icon size", "refresh size", "reset icon size"], []),
    ("micSize", "i", 14, 40, 2, "Mic icon size", ["mic icon size", "mic size", "microphone size", "recorder size"], []),
    ("dialSize", "i", 30, 64, 4, "Fast-dial circles size", ["dial size", "dial circle size", "dial circles size", "fast dial size", "dials size"], []),
    ("tileRadius", "i", 0, 30, 2, "Tile corner roundness", ["corner roundness", "tile corners", "tile radius", "roundness", "corner radius"], []),
    ("tileFillAlpha", "i", 20, 100, 10, "Tile fill solidity", ["tile fill solidity", "fill solidity", "tile opacity", "tile solidity"], ["tile transparent", "tile transparency"]),
    ("rowGap", "i", 0, 30, 2, "Row gap", ["row gap", "row spacing", "rows gap"], []),
    ("freeGap", "i", 0, 40, 2, "Free layout gap", ["free gap", "free layout gap", "free spacing"], []),
    ("mergeGap", "i", 0, 30, 2, "Merged items gap", ["merge gap", "merged gap", "merged spacing"], []),
    ("whereWidth", "i", 0, 400, 20, "Where to? box width", ["where width", "maps width", "where to width", "maps box width"], []),
    ("maxTiles", "i", 0, 100, 5, "Most tiles shown (0 = all)", ["max tiles", "most tiles", "tiles limit", "number of tiles", "tile limit"], []),
    ("maxAgeHours", "i", 0, 720, 6, "Hide messages older than (hours, 0 = never)", ["message age", "max age", "old messages", "hide old messages"], []),
    ("pageSize", "i", 2, 20, 1, "Tiles per page", ["page size", "tiles per page", "messages per page"], []),
    ("animSpeed", "i", 0, 2, 1, "Animation speed (0 slow, 1 normal, 2 fast)", ["animation speed", "animations speed", "motion speed"], []),
    ("borderWidth", "i", 0, 6, 1, "Border thickness", ["border thickness", "border width", "border size"], []),
    # --- colours
    ("accent", "c", 0, 0, 0, "Accent colour", ["accent color", "accent"], []),
    ("widgetBg", "c", 0, 0, 0, "Widget background colour", ["widget background color", "widget color", "widget background"], []),
    ("panelBg", "c", 0, 0, 0, "Panel background colour", ["panel background color", "panel color", "panel background"], []),
    ("scrimColor", "c", 0, 0, 0, "Panel shadow colour", ["shadow color", "scrim color", "panel shadow color"], []),
    ("borderColor", "c", 0, 0, 0, "Border colour", ["border color"], []),
    ("textColor", "c", 0, 0, 0, "Text colour", ["text color", "font color", "writing color"], []),
    ("circleColor", "c", 0, 0, 0, "Circles colour", ["circles color", "circle color", "top circles color"], []),
    ("dialCircle", "c", 0, 0, 0, "Fast-dial circle colour", ["dial circle color", "dial circles color", "dial color", "fast dial color"], []),
    ("iconColor", "c", 0, 0, 0, "Icons colour", ["icon color", "icons color", "mic color", "gear color"], []),
    ("dialBg", "c", 0, 0, 0, "Fast-dial strip colour", ["dial strip color", "strip color", "dial background color"], []),
    ("tileFill", "c", 0, 0, 0, "Tile fill colour", ["tile fill color", "tile color", "tiles color", "message color"], []),
    # --- on / off
    ("quickOn", "b", 0, 0, 0, "Shortcuts row", ["shortcuts row", "shortcut row"], []),
    ("mergeHoriz", "b", 0, 0, 0, "Merge side by side", ["merge horizontal", "horizontal merge", "merge side"], []),
    ("pillFirst", "b", 0, 0, 0, "Pill first", ["pill first"], []),
    ("editBtn", "b", 0, 0, 0, "Pencil button", ["pencil button", "edit button", "pencil icon"], []),
    ("freeOn", "b", 0, 0, 0, "Free layout", ["free layout", "free placing", "free mode"], []),
    ("freePackCenter", "b", 0, 0, 0, "Centre packed rows", ["center packed", "centre packed", "pack center", "packed rows center"], []),
    ("freeSnap", "b", 0, 0, 0, "Snap to neighbours", ["snap", "snapping", "magnet", "magnetic"], []),
    ("freeAuto", "b", 0, 0, 0, "Auto-fix overlaps", ["auto fix", "autofix", "auto overlap", "fix overlaps automatically"], []),
    ("oneSection", "b", 0, 0, 0, "One section at a time", ["one section", "one section at a time"], []),
    ("rememberPlaces", "b", 0, 0, 0, "Remember recent places", ["remember places", "recent places", "remember recent places"], []),
    ("suggestions", "b", 0, 0, 0, "Place suggestions", ["place suggestions", "suggestions", "typing suggestions"], []),
    ("voiceButton", "b", 0, 0, 0, "Voice button in Where to?", ["where voice", "maps voice", "voice button", "where to voice"], []),
    ("navNow", "b", 0, 0, 0, "Start navigation straight away", ["navigation straight", "start navigation", "navigate straight", "drive straight"], []),
    ("autoLocation", "b", 0, 0, 0, "Turn Location on when I open it", ["location auto", "auto location", "location on open"], []),
    ("autoLocationOff", "b", 0, 0, 0, "Switch Location off when navigation ends", ["location off after", "location auto off", "auto location off"], []),
    ("locOffAlways", "b", 0, 0, 0, "...even if Location was already on", ["location off always", "always location off"], []),
    ("exitOnPopupClose", "b", 0, 0, 0, "Closing the Maps pop-up exits navigation", ["exit navigation", "exit on popup", "popup exits navigation"], []),
    ("mapsSearchDirect", "b", 0, 0, 0, "Where to? opens the Maps search screen", ["maps search screen", "maps search direct", "where search screen"], []),
    ("borderPanel", "b", 0, 0, 0, "Panel border", ["panel border", "panel borders"], []),
    ("borderTiles", "b", 0, 0, 0, "Tile borders", ["tile border", "tiles border", "tile borders", "message border", "message borders"], []),
    ("borderWidget", "b", 0, 0, 0, "Widget border", ["widget border", "widget borders"], []),
    ("showEmptyText", "b", 0, 0, 0, "Empty message text", ["empty text", "empty message", "empty message text"], []),
    ("dialAsk", "b", 0, 0, 0, "Ask before calling", ["ask before calling", "ask before call", "confirm call", "confirm calling"], []),
    ("italic", "b", 0, 0, 0, "Italic text", ["italic", "italics", "slanted text"], []),
    ("squareShape", "b", 0, 0, 0, "Square circles", ["square shape", "square circles", "squares shape"], []),
    ("refreshShow", "b", 0, 0, 0, "Refresh icon", ["refresh icon", "refresh button", "reset icon"], []),
    ("refreshTop", "b", 0, 0, 0, "Refresh icon at the top", ["refresh top", "refresh at top", "refresh icon top"], []),
    ("icons3d", "b", 0, 0, 0, "3D icons", ["3d icons", "3d icon", "icons 3d", "three d icons"], []),
    ("dialAdd", "b", 0, 0, 0, "+ circles for empty fast-dial places", ["dial plus", "dial add circle", "empty dial plus", "plus circles dial", "add circles dial"], []),
    ("quickAdd", "b", 0, 0, 0, "+ circle in the shortcuts row", ["shortcut plus", "shortcut add circle", "plus circle shortcut", "shortcuts plus"], []),
    ("keepPlusHidden", "b", 0, 0, 0, "Hide the + after choosing a shortcut (free layout)", ["keep plus hidden"], []),
    ("dialNames", "b", 0, 0, 0, "Names under fast-dial circles", ["dial names", "names under dial", "names under dials", "fast dial names"], []),
    ("countRead", "b", 0, 0, 0, "Count drops after I open / read", ["count read", "count drops read", "count after read"], []),
    ("countLeave", "b", 0, 0, 0, "Count drops on closing the card", ["count leave", "count drops leave", "count after leave"], []),
    ("countReply", "b", 0, 0, 0, "Count drops after I reply", ["count reply", "count drops reply", "count after reply"], []),
    ("dismissHandled", "b", 0, 0, 0, "Also remove it from the phone's notification panel", ["dismiss handled", "remove from phone panel", "dismiss notification"], []),
    ("cardBtnAuto", "b", 0, 0, 0, "Sort card buttons by use", ["card buttons auto", "sort buttons by use", "sort by use", "most used first"], []),
    ("quietOn", "b", 0, 0, 0, "Quiet hours", ["quiet hours", "quiet time", "quiet mode"], []),
    ("closeAfterClear", "b", 0, 0, 0, "Close the popup after Clear all / Read all", ["close after clear", "close popup after clear", "close after read all"], []),
    ("autoTheme", "b", 0, 0, 0, "Auto day / night theme", ["auto theme", "auto day night", "day night theme", "automatic theme"], []),
    ("aliveOn", "b", 0, 0, 0, "Animations (master switch)", ["animation master", "animations master", "all animations"], []),
    ("aliveWave", "b", 0, 0, 0, "Widget icons move when something happens", ["widget icons move", "icons wave", "widget wave", "wave animation"], []),
    ("alivePanel", "b", 0, 0, 0, "Panel animations", ["panel animation", "panel animations", "panel motion"], []),
    ("swipeThrough", "b", 0, 0, 0, "Swipe through", ["swipe through"], []),
    ("swipeTapAll", "b", 0, 0, 0, "Tap anywhere to swipe", ["swipe tap all", "tap anywhere swipe"], []),
    ("backToTiles", "b", 0, 0, 0, "Back goes to the tiles", ["back to tiles", "back goes tiles"], []),
    ("loop", "b", 0, 0, 0, "Endless pages (loop)", ["endless pages", "loop pages", "pages loop", "endless"], []),
]

def cap(s): return s[0].upper() + s[1:]

out = []
out.append('''package com.example.notiftiles

import android.content.Context

/**
 * GENERATED by tools/helper/gen_settings.py. Do not edit by hand: change SPEC there and run the script again.
 * The settings the helper can change by name ("set the dial size to 50", "turn the panel border off", "make the accent red").
 * Each one calls the same Prefs function the Settings screen calls, so the app behaves exactly as if you had used the screen.
 */
object HelperSettings {
    const val BOOL = 0
    const val INT = 1
    const val COLOUR = 2

    class Entry(val id: String, val kind: Int, val min: Int, val max: Int, val step: Int, val label: String,
                val phrases: List<String>, val inverse: List<String>)

    val ENTRIES: List<Entry> = listOf(
''')
kinds = {"b": "BOOL", "i": "INT", "c": "COLOUR"}
rows = []
for (name, k, mn, mx, st, label, ph, inv) in SPEC:
    q = lambda l: "listOf(" + ", ".join('"%s"' % x for x in l) + ")"
    rows.append('        Entry("%s", %s, %d, %d, %d, "%s", %s, %s)' % (name, kinds[k], mn, mx, st, label.replace('"', '\\"'), q(ph), q(inv)))
out.append(",\n".join(rows))
out.append('\n    )\n\n')

ints = [s for s in SPEC if s[1] in "ic"]
bools = [s for s in SPEC if s[1] == "b"]

out.append('    fun getInt(c: Context, id: String): Int = when (id) {\n')
for s in ints:
    if s[0] == "tileRadius":
        out.append('        "tileRadius" -> Prefs.tileRadius(c).let { if (it >= 0) it else 16 }\n')
    else:
        out.append('        "%s" -> Prefs.%s(c)\n' % (s[0], s[0]))
out.append('        else -> 0\n    }\n\n')
out.append('    fun setInt(c: Context, id: String, v: Int) {\n        when (id) {\n')
for s in ints:
    out.append('            "%s" -> Prefs.set%s(c, v)\n' % (s[0], cap(s[0])))
out.append('            else -> {}\n        }\n    }\n\n')
out.append('    fun getBool(c: Context, id: String): Boolean = when (id) {\n')
for s in bools:
    out.append('        "%s" -> Prefs.%s(c)\n' % (s[0], s[0]))
out.append('        else -> false\n    }\n\n')
out.append('    fun setBool(c: Context, id: String, v: Boolean) {\n        when (id) {\n')
for s in bools:
    out.append('            "%s" -> Prefs.set%s(c, v)\n' % (s[0], cap(s[0])))
out.append('            else -> {}\n        }\n    }\n}\n')

path = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "java", "com", "example", "notiftiles", "HelperSettings.kt")
open(path, "w", encoding="utf-8").write("".join(out))
print("wrote", os.path.normpath(path), len(SPEC), "settings")
