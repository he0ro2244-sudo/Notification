"""Writes app/src/main/java/com/example/notiftiles/HelperSettings.kt: the list of settings the helper can change by name.
Edit SPEC / EXTRA below (a setting's name in Prefs.kt, its kind, range and the phrases people say), then run:
    python3 tools/helper/gen_settings.py
kinds: b = on/off, i = number, c = colour (0 = automatic where the app has that),
       t = text (a word or sentence the person types), h = choice from a short list (shown as words).
SPEC entries call Prefs.<name>(c) / Prefs.set<Name>(c, v). EXTRA entries give their own Kotlin code for get and set
(for settings that live in another object, such as Weather or SettingsTheme) and families made from key lists
(text size per part, colour per part, shows and hides) so each part of the screen gets its own setting.
The key lists below are copied from Prefs.kt: keep them in step when a part is added there."""
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

# ---- Key lists from Prefs.kt (keep in step with Prefs.kt) ----
WIDGET_PARTS = [
    ("title", "Widget: Inbox title"), ("mic", "Widget: mic icon"), ("note", "Widget: + Note"), ("clear", "Widget: Clear all"),
    ("gear", "Widget: settings icon"), ("refresh", "Widget: refresh icon"), ("edit", "Widget: pencil (arrange) button"),
    ("clock", "Widget: clock"), ("weather", "Widget: weather"), ("date", "Widget: date"),
    ("merge", "Widget: merged shape (clock + weather + date together)"), ("where", "Widget: Where to?"),
    ("empty", "Widget: Nothing new text"), ("updated", "Widget: Updated line (weather)"),
    ("dialletter", "Widget: fast-dial letters (inside circles)"), ("dialnames", "Widget: fast-dial names (under circles)"),
    ("tapp", "Widget tiles: app name"), ("ttitle", "Widget tiles: message title"), ("ttext", "Widget tiles: message text"),
    ("pages", "Widget: page buttons"),
]
WIDGET_SIZE_PARTS = [
    ("title", "Inbox title"), ("note", "+ Note"), ("clear", "Clear all / Read all"), ("clock", "Clock"), ("weather", "Weather"),
    ("date", "Date"), ("merge", "Merged shape (clock, weather, date together)"), ("where", "Where to? text"),
    ("empty", "Nothing new text"), ("dialletter", "Fast-dial letters (inside circles)"), ("dialnames", "Names under fast-dial circles"),
    ("quickletter", "Shortcut letters (inside circles)"), ("quicknames", "Names under shortcut circles"),
    ("tapp", "Tiles: app name"), ("ttitle", "Tiles: message title"), ("ttext", "Tiles: message text"), ("pages", "Page buttons"),
]
PANEL_GROUPS = [
    ("head", "Header title (Inbox, Back)", "header title"), ("hdr", "+ Note button", "note button"),
    ("clear", "Clear all button", "clear all button"), ("gear", "Settings icon", "settings icon button"),
    ("title", "Message titles", "message titles"), ("text", "Message text and notes", "message text"),
    ("small", "Small labels (app name, counts)", "small labels"), ("buttons", "Buttons and reply box", "reply buttons"),
]
SHOW_ITEMS = [
    ("w_title", "Widget: Inbox title"), ("w_count", "Widget: message count badge"), ("w_mic", "Widget: mic icon"),
    ("w_note", "Widget: + Note"), ("w_clear", "Widget: Clear all / Read all"), ("w_gear", "Widget: settings icon"),
    ("t_app", "Widget tiles: app name"), ("t_cnt", "Widget tiles: count badge"), ("t_title", "Widget tiles: message title"),
    ("t_text", "Widget tiles: message text"), ("p_title", "Panel: Inbox title"), ("p_badge", "Panel: message count badge"),
    ("p_note", "Panel: + Note"), ("p_clear", "Panel: Clear all / Read all"), ("p_gear", "Panel: settings icon"),
    ("p_back", "Panel: Back (in an opened message)"), ("p_mic", "Panel: mic (new, off until you switch it on)"),
    ("p_edit", "Panel: pencil (new, off until you switch it on)"), ("p_refresh", "Panel: refresh (new, off until you switch it on)"),
    ("p_icon", "Panel tiles: app icon"), ("p_cnt", "Panel tiles: count badge"), ("p_title_t", "Panel tiles: message title"),
    ("p_text", "Panel tiles: message text"),
]
HIDE_EMPTY_PARTS = [("title", "Inbox title"), ("mic", "Mic icon"), ("note", "+ Note"), ("clear", "Clear all / Read all")]
SHAPE_CHOICES = ["Round", "Rounded square", "Square", "Teardrop", "Leaf", "Diamond", "Hexagon"]
STAMP_CHOICES = ["Smart", "Date + time", "Time only", "None"]

def clean(label):
    """'Widget: Inbox title' -> 'inbox title' (what a person would say)."""
    if ":" in label:
        label = label.split(":", 1)[1]
    label = label.split("(")[0]
    return " ".join(label.replace("+", " ").replace("/", " ").replace("-", " ").replace("‹", " ").lower().split())

def extra_entries():
    """Settings that live in other objects, and the families made from key lists."""
    E = []
    def add(id_, kind, mn, mx, st, label, phrases, get, set_, inv=(), choices=()):
        E.append(dict(id=id_, kind=kind, min=mn, max=mx, step=st, label=label, phrases=list(phrases),
                      inverse=list(inv), get=get, set=set_, choices=list(choices)))
    # --- on / off that live elsewhere
    add("pillPos", "b", 0, 0, 0, "Maps search box at the start (left)", ["maps search box start", "search box left", "where box at start"],
        "Prefs.pillPos(c) == 0", "Prefs.setPillPos(c, if (v) 0 else 3)")
    add("showUpdated", "b", 0, 0, 0, "\"Updated\" time under the refresh icon", ["updated time", "update time under refresh"],
        "Weather.showUpdated(c)", "Weather.setShowUpdated(c, v)")
    add("showUpdTime", "b", 0, 0, 0, "Updated line: show the time", ["updated line time", "time in updated line", "updated line clock"],
        "Weather.showUpdTime(c)", "Weather.setShowUpdTime(c, v)")
    add("showUpdSource", "b", 0, 0, 0, "Updated line: show the source", ["updated line source", "weather source", "updated source"],
        "Weather.showUpdSource(c)", "Weather.setShowUpdSource(c, v)")
    add("showUpdNote", "b", 0, 0, 0, "Updated line: show the \"to match phone\" note", ["match phone note", "phone match note", "to match phone"],
        "Weather.showUpdNote(c)", "Weather.setShowUpdNote(c, v)")
    add("picInside", "b", 0, 0, 0, "Pinned pictures sit inside the circle", ["pictures inside circle", "picture inside circle", "pinned pictures inside"],
        "Prefs.picInside(c)", "Prefs.setPicInside(c, v)")
    add("swipeTapName", "b", 0, 0, 0, "Tap the name line too", ["tap name line", "tap the name line", "name line tap"],
        "Prefs.swipeTapName(c)", "Prefs.setSwipeTapName(c, v)")
    add("readRepeatNumbers", "b", 0, 0, 0, "Mark as read also hides repeats with only new numbers", ["repeat numbers", "numbers only repeat", "read repeat numbers"],
        "Prefs.readRepeatNumbers(c)", "Prefs.setReadRepeatNumbers(c, v)")
    add("noteDirect", "b", 0, 0, 0, "+ Note opens the notes app straight away", ["note opens notes app", "notes app straight away", "note straight away"],
        "Notes.direct(c)", "Notes.setDirect(c, v)")
    add("builtinRecorder", "b", 0, 0, 0, "Helper OFF: record inside this app", ["recorder inside app", "record inside app", "built in recorder"],
        "Recorder.builtinOn(c)", "Recorder.setBuiltinOn(c, v)")
    # --- numbers that live elsewhere
    add("weatherOffset", "i", -6, 6, 1, "Temperature adjust", ["temperature adjust", "temperature offset", "temp adjust"],
        "Weather.offset(c)", "Weather.setOffset(c, v)")
    add("recTapX", "i", 20, 80, 2, "Red button position: across", ["red button across", "record button across", "red button left right"],
        "Recorder.tapX(c)", "Recorder.setTapX(c, v)")
    add("recTapY", "i", 60, 95, 2, "Red button position: down", ["red button down", "record button down", "red button up down"],
        "Recorder.tapY(c)", "Recorder.setTapY(c, v)")
    for kind, label in [("panel", "Panel"), ("tiles", "Message tiles"), ("widget", "Widget (home screen)")]:
        add("borderWidth." + kind, "i", 0, 6, 1, label + " border thickness", [kind + " border thickness", kind + " border width"],
            'Prefs.borderWidthOf(c, "%s")' % kind, 'Prefs.setBorderWidthOf(c, "%s", v)' % kind)
    # --- colours of the Settings screen itself
    for key, label, phrases, g, s in [
        ("sBg", "Settings screen background colour", ["settings background color", "settings screen background"], "SettingsTheme.bgOwn(c)", "SettingsTheme.setBg(c, v)"),
        ("sText", "Settings screen text colour", ["settings text color", "settings screen text"], "SettingsTheme.textOwn(c)", "SettingsTheme.setText(c, v)"),
        ("sAccent", "Settings buttons, switches and sliders colour", ["settings buttons color", "settings switch color", "settings slider color"], "SettingsTheme.accentOwn(c)", "SettingsTheme.setAccent(c, v)"),
        ("sCard", "Settings section bars and search box colour", ["settings section color", "settings bars color", "settings search color"], "SettingsTheme.cardOwn(c)", "SettingsTheme.setCard(c, v)"),
    ]:
        add(key, "c", 0, 0, 0, label, phrases, g, s)
    # --- shape and stamp: choices by name
    add("shape", "h", 0, len(SHAPE_CHOICES) - 1, 1, "Shape of the circles and boxes", ["circle shape", "shape of circles", "box shape", "shape"],
        "Prefs.shape(c)", "Prefs.setShape(c, v)", choices=SHAPE_CHOICES)
    add("stampStyle", "h", 0, 3, 1, "Message time stamp", ["time stamp", "message time", "time style", "time stamp style"],
        "Prefs.stampStyle(c)", "Prefs.setStampStyle(c, v)", choices=STAMP_CHOICES)
    # --- wording: typed text
    add("emptyText", "t", 0, 0, 0, "Message when nothing is new", ["nothing new wording", "empty wording", "empty message wording", "wording when empty"],
        "Prefs.emptyText(c)", "Prefs.setEmptyText(c, v)")
    add("updatedLabel", "t", 0, 0, 0, "Update word (before the time)", ["update word", "updated word", "word before the time"],
        "Prefs.updatedLabel(c)", "Prefs.setUpdatedLabel(c, v)")
    # --- more choices, and pictures' strength per background
    add("sortMode", "h", 0, 3, 1, "Order of the messages", ["sort order", "sort messages", "sort tiles", "message order"],
        "Prefs.sortMode(c)", "Prefs.setSortMode(c, v)", choices=["Newest first", "Oldest first", "My own order", "App name A-Z"])
    add("scrollMode", "h", 0, 2, 1, "How the tiles scroll", ["scroll mode", "scrolling", "page mode"],
        "Prefs.scrollMode(c)", "Prefs.setScrollMode(c, v)", choices=["Scroll up and down", "Pages", "My pages"])
    for kind, label in [("widget", "Widget"), ("dial", "Fast-dial strip"), ("panel", "Panel")]:
        add("imgAlpha." + kind, "i", 10, 100, 10, label + " picture strength", [kind + " picture strength", kind + " background strength"],
            'Prefs.imgAlpha(c, "%s")' % kind, 'Prefs.setImgAlpha(c, "%s", v)' % kind)
    # --- the panel's live preview shrinking while scrolling (plain saved setting)
    add("previewMini", "b", 0, 0, 0, "Hide the live preview while I scroll down the settings", ["preview while scrolling", "live preview scroll", "preview shrink"],
        'Prefs.store(c).getBoolean("ui_preview_mini", true)', 'Prefs.store(c).edit().putBoolean("ui_preview_mini", v).apply()')
    # --- families from key lists
    for k, label in WIDGET_SIZE_PARTS:
        add("partSize." + k, "i", 50, 300, 10, "Text size: " + label, [clean(label) + " text size", clean(label) + " size"],
            'Prefs.partSize(c, "%s")' % k, 'Prefs.setPartSize(c, "%s", v)' % k)
    for g, label, short in PANEL_GROUPS:
        add("grpSize." + g, "i", 60, 220, 10, "Panel text size: " + label, ["panel " + short + " size", short + " text size", short + " size"],
            'Prefs.grpSize(c, "%s")' % g, 'Prefs.setGrpSize(c, "%s", v)' % g)
    for k, label in WIDGET_PARTS:
        add("partColor." + k, "c", 0, 0, 0, "Text colour: " + label, [clean(label) + " color"],
            'Prefs.partColor(c, "%s")' % k, 'Prefs.setPartColor(c, "%s", v)' % k)
    for g, label, short in PANEL_GROUPS:
        add("grpColor." + g, "c", 0, 0, 0, "Panel colour: " + label, ["panel " + short + " color", short + " color"],
            'Prefs.grpColor(c, "%s")' % g, 'Prefs.setGrpColor(c, "%s", v)' % g)
    for k, label in SHOW_ITEMS:
        add("showItem." + k, "b", 0, 0, 0, label,
            ["show " + clean(label)] + ([clean(label)] if len(clean(label).split()) > 1 else []),
            "Prefs.showItem(c, \"%s\")" % k, "Prefs.setShowItem(c, \"%s\", v)" % k)
    for k, label in HIDE_EMPTY_PARTS:
        add("hideEmpty." + k, "b", 0, 0, 0, "Hide " + label + " when the Inbox is empty", ["hide " + clean(label) + " when empty", "when empty hide " + clean(label)],
            'Prefs.hideWhenEmpty(c, "%s")' % k, 'Prefs.setHideWhenEmpty(c, "%s", v)' % k)
    for k, label in [("clock", "Clock circle"), ("weather", "Weather circle"), ("date", "Date circle"), ("maps", "Maps \"Where to?\" box"), ("dial", "Fast-dial strip")]:
        add("showPart." + k, "b", 0, 0, 0, label, [clean(label), clean(label) + " box"] if k != "maps" else ["where to box", "maps box", "where to box show"],
            'Prefs.show(c, "%s")' % k, 'Prefs.setShow(c, "%s", v)' % k)
    return E

def normalise(s):
    """SPEC tuples -> the same dict form as EXTRA, with the default Prefs getter and setter."""
    name, k, mn, mx, st, label, ph, inv = s
    cap = name[0].upper() + name[1:]
    kind = {"b": "b", "i": "i", "c": "c"}[k]
    return dict(id=name, kind=kind, min=mn, max=mx, step=st, label=label, phrases=ph, inverse=inv,
                get="Prefs.%s(c)" % name, set="Prefs.set%s(c, v)" % cap, choices=[])

ALL = [normalise(s) for s in SPEC] + extra_entries()
ids = [e["id"] for e in ALL]
dupes = sorted({i for i in ids if ids.count(i) > 1})
assert not dupes, "duplicate ids: %s" % dupes

kinds = {"b": "BOOL", "i": "INT", "c": "COLOUR", "t": "TEXT", "h": "CHOICE"}
def qlist(xs): return "listOf(" + ", ".join('"%s"' % x.replace('"', '\\"') for x in xs) + ")"

out = []
out.append('''package com.example.notiftiles

import android.content.Context

/**
 * GENERATED by tools/helper/gen_settings.py. Do not edit by hand: change SPEC / EXTRA there and run the script again.
 * The settings the helper can change by name ("set the dial size to 50", "turn the panel border off", "make the accent red",
 * "make the inbox title colour blue", "shape rounded square", "temperature adjust 2").
 * Each one calls the same Prefs function the Settings screen calls, so the app behaves exactly as if you had used the screen.
 */
object HelperSettings {
    const val BOOL = 0
    const val INT = 1
    const val COLOUR = 2
    const val TEXT = 3
    const val CHOICE = 4

    class Entry(val id: String, val kind: Int, val min: Int, val max: Int, val step: Int, val label: String,
                val phrases: List<String>, val inverse: List<String>, val choices: List<String> = emptyList())

    val ENTRIES: List<Entry> = listOf(
''')
rows = []
for e in ALL:
    extra = (", choices = " + qlist(e["choices"])) if e["choices"] else ""
    rows.append('        Entry("%s", %s, %d, %d, %d, "%s", %s, %s%s)' % (
        e["id"], kinds[e["kind"]], e["min"], e["max"], e["step"], e["label"].replace('"', '\\"'),
        qlist(e["phrases"]), qlist(e["inverse"]), extra))
out.append(",\n".join(rows))
out.append('\n    )\n\n')

def block(fn, kind_set, ret, sig_extra=""):
    pass

numeric = [e for e in ALL if e["kind"] in ("i", "c", "h")]
bools = [e for e in ALL if e["kind"] == "b"]
texts = [e for e in ALL if e["kind"] == "t"]

def esc(s): return s.replace('\\', '\\\\').replace('"', '\\"')

out.append('    fun getInt(c: Context, id: String): Int = when (id) {\n')
for e in numeric:
    expr = e["get"]
    if e["id"] == "tileRadius":
        expr = "Prefs.tileRadius(c).let { if (it >= 0) it else 16 }"
    out.append('        "%s" -> %s\n' % (esc(e["id"]), expr))
out.append('        else -> 0\n    }\n\n')
out.append('    fun setInt(c: Context, id: String, v: Int) {\n        when (id) {\n')
for e in numeric:
    out.append('            "%s" -> %s\n' % (esc(e["id"]), e["set"]))
out.append('            else -> {}\n        }\n    }\n\n')
out.append('    fun getBool(c: Context, id: String): Boolean = when (id) {\n')
for e in bools:
    out.append('        "%s" -> %s\n' % (esc(e["id"]), e["get"]))
out.append('        else -> false\n    }\n\n')
out.append('    fun setBool(c: Context, id: String, v: Boolean) {\n        when (id) {\n')
for e in bools:
    out.append('            "%s" -> %s\n' % (esc(e["id"]), e["set"]))
out.append('            else -> {}\n        }\n    }\n\n')
out.append('    fun getText(c: Context, id: String): String = when (id) {\n')
for e in texts:
    out.append('        "%s" -> %s\n' % (esc(e["id"]), e["get"]))
out.append('        else -> ""\n    }\n\n')
out.append('    fun setText(c: Context, id: String, v: String) {\n        when (id) {\n')
for e in texts:
    out.append('            "%s" -> %s\n' % (esc(e["id"]), e["set"]))
out.append('            else -> {}\n        }\n    }\n}\n')

path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "app", "src", "main", "java", "com", "example", "notiftiles", "HelperSettings.kt")
open(path, "w", encoding="utf-8").write("".join(out))
print("wrote", os.path.normpath(path), len(ALL), "settings")
