import random, re, json, sys
import numpy as np
from sklearn.linear_model import LogisticRegression

random.seed(7)
OUT = sys.argv[1] if len(sys.argv) > 1 else "router.json"

# Items: canonical key -> ways people say it
ITEMS = {
    "mic": ["mic", "microphone", "mic icon", "recorder", "the mic", "record button"],
    "note": ["note", "notes", "notes icon", "note icon", "the notes", "pen note"],
    "gear": ["gear", "settings", "setting", "cog", "settings icon", "gear icon"],
    "edit": ["edit", "pencil", "pencil icon", "edit icon"],
    "clear": ["clear", "clear all", "clear button"],
    "count": ["count", "badge", "number badge"],
    "clock": ["clock", "time", "clock circle", "the clock"],
    "weather": ["weather", "temperature", "weather circle", "the weather"],
    "date": ["date", "calendar", "the date", "date circle"],
    "maps": ["where to", "maps", "location box", "where to box"],
}

# Each class: canonical sentence for the rules, and ways people say it (with {a} {b} for items)
CLASSES = {
    "order_after": ("move {a} after {b}", [
        "move {a} after {b}", "put {a} after {b}", "place {a} after the {b}", "move the {a} after the {b}",
        "move the {a} icon after the {b} icon", "{a} should go after {b}", "i want {a} after {b}",
        "can you move {a} after {b}", "shift {a} to after {b}", "make {a} come after {b}", "move {a} behind {b}",
        "{a} after {b} please", "arrange {a} after {b}", "order {a} after {b}"]),
    "order_before": ("move {a} before {b}", [
        "move {a} before {b}", "put {a} before {b}", "place {a} before the {b}", "move the {a} before the {b}",
        "move the {a} icon before the {b} icon", "{a} should go before {b}", "i want {a} before {b}",
        "can you move {a} before {b}", "make {a} come before {b}", "move {a} in front of {b}", "{a} before {b} please"]),
    "gap_bigger": ("increase the row gap", [
        "increase the row gap", "more space between rows", "make the spaces between rows bigger",
        "add more space between the rows", "increase the spacing between rows", "bigger row gap",
        "more gap between the rows", "make the rows further apart", "space the rows out more"]),
    "gap_smaller": ("decrease the row gap", [
        "decrease the row gap", "less space between rows", "make the spaces between rows smaller",
        "reduce the space between rows", "closer rows", "smaller row gap", "tighten the gap between rows",
        "bring the rows closer", "less gap between the rows"]),
    "free_gap_bigger": ("increase the free layout gap", [
        "increase the free layout gap", "more space in the free layout", "bigger free gap",
        "more gap in free layout", "increase the free gap"]),
    "free_gap_smaller": ("decrease the free layout gap", [
        "decrease the free layout gap", "less space in the free layout", "smaller free gap",
        "less gap in free layout", "reduce the free gap"]),
    "width_bigger": ("make {a} wider", [
        "make {a} wider", "widen {a}", "increase the width of {a}", "make the {a} wider",
        "{a} wider please", "stretch {a} wider", "make {a} broader"]),
    "width_smaller": ("make {a} narrower", [
        "make {a} narrower", "narrow {a}", "decrease the width of {a}", "make the {a} thinner",
        "{a} narrower please", "shrink the width of {a}"]),
    "height_bigger": ("make {a} taller", [
        "make {a} taller", "increase the height of {a}", "make the {a} higher", "{a} taller please",
        "stretch {a} taller", "make {a} bigger in height"]),
    "height_smaller": ("make {a} shorter", [
        "make {a} shorter", "decrease the height of {a}", "make the {a} lower in height", "{a} shorter please",
        "reduce the height of {a}"]),
    "size_bigger": ("make {a} bigger", [
        "make {a} bigger", "increase the size of {a}", "make the {a} larger", "{a} bigger please",
        "enlarge {a}", "make {a} big", "grow {a}"]),
    "size_smaller": ("make {a} smaller", [
        "make {a} smaller", "decrease the size of {a}", "make the {a} tiny", "{a} smaller please",
        "shrink {a}", "reduce {a}"]),
    "merge": ("merge {a} and {b}", [
        "merge {a} and {b}", "combine {a} with {b}", "put {a} and {b} together", "join {a} and {b}",
        "merge the {a} with the {b}", "group {a} and {b} together"]),
    "unmerge": ("unmerge the top items", [
        "unmerge", "split the top items", "separate the top items", "unmerge them", "undo the merge",
        "split them apart", "separate the merged items"]),
    "hide": ("hide {a}", [
        "hide {a}", "turn off {a}", "remove {a} from the widget", "i dont want {a}", "get rid of {a}",
        "make {a} invisible", "take away {a}", "switch off {a}", "hide the notes", "hide notes", "remove the notes button",
        "get rid of the notes", "turn off the notes", "hide the notes button", "remove notes", "hide the mic", "remove the mic icon"]),
    "show": ("show {a}", [
        "show {a}", "turn on {a}", "bring back {a}", "i want {a} back", "display {a}", "make {a} visible",
        "switch on {a}", "put back {a}"]),
    "move_top": ("move {a} to the top row", [
        "move {a} to the top", "put {a} at the top", "move {a} up to the top row", "{a} to the top",
        "top row for {a}", "move {a} to top"]),
    "move_bottom": ("move {a} to the bottom row", [
        "move {a} to the bottom", "put {a} at the bottom", "move {a} down to the bottom row", "{a} to the bottom",
        "move {a} under the inbox", "bottom row for {a}"]),
    "nudge_left": ("move {a} left", ["move {a} left", "nudge {a} left", "shift {a} to the left", "{a} go left", "push {a} left"]),
    "nudge_right": ("move {a} right", ["move {a} right", "nudge {a} right", "shift {a} to the right", "{a} go right", "push {a} right"]),
    "nudge_up": ("move {a} up", ["move {a} up", "nudge {a} up", "shift {a} up", "{a} go up", "push {a} up"]),
    "nudge_down": ("move {a} down", ["move {a} down", "nudge {a} down", "shift {a} down", "{a} go down", "push {a} down"]),
    "undo": ("undo", ["undo", "undo that", "take it back", "go back", "undo last change", "revert that", "undo please"]),
    "redo": ("redo", ["redo", "redo that", "do it again", "bring back the change", "redo last change"]),
    "transparent": ("make the widget transparent", [
        "make the widget more transparent", "make it see through", "more see through", "make the background transparent",
        "make the widget faded", "i want it more transparent", "fade the widget", "less solid please"]),
    "solid": ("make the widget solid", [
        "make the widget solid", "make it solid", "make the background opaque", "less transparent",
        "make it not see through", "more solid please"]),
    "text_bigger": ("make the text bigger", [
        "make the text bigger", "bigger font", "increase the font size", "bigger letters", "larger text",
        "make the writing bigger", "make font bigger"]),
    "text_smaller": ("make the text smaller", [
        "make the text smaller", "smaller font", "decrease the font size", "smaller letters", "smaller text",
        "make the writing smaller"]),
    "anim_off": ("turn animations off", [
        "turn animations off", "stop the animation", "no animations", "disable animations", "animations off",
        "stop moving", "i dont want animation"]),
    "anim_on": ("turn animations on", [
        "turn animations on", "start the animation", "enable animations", "animations on", "make it animate"]),
    "dark": ("make the theme dark", ["make it dark", "dark theme", "switch to dark", "night mode", "make it darker"]),
    "light": ("make the theme light", ["make it light", "light theme", "switch to light", "day mode", "make it brighter"]),
    "random": ("make the theme random", ["random theme", "surprise me", "random look", "change the look randomly"]),
    "note_word_off": ("remove the word from the notes icon", [
        "remove the word from the notes icon", "remove notes word from notes icon", "notes icon without the word",
        "remove the note text", "remove the word note", "notes icon with no word", "take the word off the notes icon",
        "just the notes icon", "only the icon for notes", "remove the text from the notes button", "no word on the notes icon",
        "hide the word notes", "remove the notes word", "notes without text", "take away the note word",
        "make the notes button icon only", "remove the word from note icon", "no text on notes"]),
    "none": ("", [
        "hello", "how are you", "what time is it", "thank you", "open the weather app", "call mom", "good morning",
        "what is the capital of france", "tell me a joke", "i like it", "okay", "send a message to ali",
        "read my notes", "what day is it", "play music", "set an alarm", "how do i reset my phone",
        "who won the game", "write a poem", "turn on the flashlight", "is it going to rain",
        "open settings for the phone", "remind me to buy milk", "take a photo", "thanks a lot",
        "good night", "what is my name", "show me the weather in riyadh", "open maps", "check my email",
        "add an event tomorrow", "what is two plus two", "translate hello to arabic", "help",
        "font color", "accent colour", "auto day night", "inbox icon", "mail icon", "widget size",
        "backup and restore", "where is the manual", "emoji for the mic", "background opacity"]),
}

# ---------------------------------------------------------------------------------------------------------------
# v1.1.80: more kinds of sentence (fast dial, shortcuts, tidy rows, notes), many more ways of saying each thing, and a bigger
# feature set (3 to 5 letter pieces and pairs of words). The first block above is unchanged. A kind whose canonical sentence is
# empty has no sentence for the rules: the app asks HelperActions to do it (see Router.intent).
# ---------------------------------------------------------------------------------------------------------------
NAMES = ["mom", "dad", "ali", "sara", "john", "ahmed", "fatima", "omar", "layla", "dr smith", "uncle sam", "grandma", "boss",
         "taxi", "pizza", "noor", "khalid", "maria", "peter", "zainab", "mike", "anna", "hassan", "yusuf", "the office", "home"]
APPS = ["whatsapp", "instagram", "camera", "maps", "youtube", "gmail", "chrome", "snapchat", "telegram", "calculator", "spotify",
        "facebook", "tiktok", "settings", "clock", "photos", "notes", "netflix", "uber", "messages", "phone", "twitter", "x", "linkedin"]
TEXTS = ["buy milk", "call the bank", "meeting at five", "pick up the kids", "pay the bill", "water the plants", "email the report",
         "book the dentist", "gym tomorrow", "renew the passport", "send the invoice", "check the oven", "birthday gift for dad"]

def rand_num():
    r = random.random()
    if r < 0.5:
        return "05" + "".join(random.choice("0123456789") for _ in range(8))
    if r < 0.8:
        return "+9665" + "".join(random.choice("0123456789") for _ in range(8))
    return "".join(random.choice("0123456789") for _ in range(random.randint(7, 11)))

NEW = {
    "dial_add": ("", [
        "add fast dial {name} {num}", "add a fast dial for {name}", "put {name} in the fast dial", "add {name} to fast dial",
        "new fast dial {name} {num}", "set dial {slot} to {name} {num}", "change dial {slot} to {name}", "save {name} {num} as a dial",
        "add contact {name} to my dials", "make {name} a fast dial", "assign {name} to dial {slot}", "put {name} on speed dial",
        "add speed dial {name} {num}", "fast dial for {name} {num}", "i want {name} in the dial", "rename dial {slot} to {name}",
        "create a fast dial for {name} with number {num}", "add {name} {num} to the fast dial", "set the {slot} dial to {name} {num}",
        "change the number of dial {slot} to {num}", "update dial {slot} with {name}", "new dial {name}", "add dial {name} {num}",
        "put {num} on dial {slot} for {name}", "fast dial {slot} {name} {num}", "make dial {slot} {name} {num}",
        "add a circle to call {name}", "add a call circle for {name} {num}", "replace dial {slot} with {name} {num}"]),
    "dial_remove": ("", [
        "remove dial {slot}", "delete fast dial {slot}", "clear dial {slot}", "remove {name} from fast dial", "take {name} off my dials",
        "empty dial {slot}", "get rid of dial {slot}", "erase the {slot} dial", "delete {name} from the dial", "clear fast dial {slot}",
        "remove the fast dial of {name}", "delete dial {name}", "remove fast dial {name}", "drop dial {slot}", "clear the dial for {name}",
        "i dont want {name} in the dial anymore", "take dial {slot} away"]),
    "dial_show": ("", [
        "show the fast dial", "turn on fast dial", "show dials", "bring back the dial row", "display the fast dial row",
        "show fast dial row", "enable fast dial", "i want the fast dial back", "switch on the fast dial", "show the dial circles"]),
    "dial_hide": ("", [
        "hide the fast dial", "turn off fast dial", "hide dials", "hide the dial row", "remove the fast dial row",
        "i dont want the dial row", "disable fast dial", "switch off the fast dial", "hide the dial circles", "no fast dial"]),
    "shortcut_add": ("", [
        "add shortcut {app}", "add a shortcut for {app}", "new shortcut {app}", "put {app} in the shortcuts", "add {app} to shortcuts",
        "make a shortcut to {app}", "set shortcut {slot} to {app}", "change shortcut {slot} to {app}", "create a shortcut for {app}",
        "i want {app} as a shortcut", "add {app} to the shortcuts row", "put a {app} circle in the shortcuts row", "shortcut for {app}",
        "assign {app} to shortcut {slot}", "replace shortcut {slot} with {app}", "add shortcut to open {app}", "new quick shortcut {app}",
        "add {app} shortcut", "create shortcut {app}", "add a launcher for {app}"]),
    "shortcut_remove": ("", [
        "remove shortcut {slot}", "delete shortcut {slot}", "clear shortcut {slot}", "remove the {app} shortcut", "delete the shortcut for {app}",
        "take {app} off the shortcuts", "empty shortcut {slot}", "get rid of shortcut {slot}", "remove {app} from shortcuts",
        "clear the {app} shortcut", "erase shortcut {slot}", "drop the {app} shortcut", "i dont want the {app} shortcut"]),
    "shortcut_show": ("", [
        "show the shortcuts", "turn on shortcuts", "show shortcuts row", "bring back the shortcuts", "display the shortcuts row",
        "enable shortcuts", "switch on the shortcuts", "i want the shortcuts row", "show the shortcut circles"]),
    "shortcut_hide": ("", [
        "hide the shortcuts", "turn off shortcuts", "hide shortcuts row", "remove the shortcuts row", "disable shortcuts",
        "switch off the shortcuts", "i dont want the shortcuts row", "hide the shortcut circles", "no shortcuts"]),
    "tidy": ("", [
        "tidy the rows", "fix the spaces between rows", "make the spaces between rows tidy", "tidy up the widget", "fix layout",
        "organize the rows", "neat spacing between the rows", "make rows neat", "clean up the spaces", "put tidy spaces between rows",
        "tidy the gaps", "make the row gaps the same like the pencil", "fix overlapping items", "same space between all rows",
        "make spaces between rows equal", "tidy up", "tidy the layout", "make the layout neat", "fix the rows", "even out the rows",
        "make the rows spaced the same", "tidy spaces like in the pencil", "do what the pencil fix layout does", "make everything tidy",
        "straighten the rows", "fix the gaps between the rows", "equal spaces between the rows", "put the rows in order with tidy space",
        "tidy up the spaces between the rows", "arrange the rows neatly", "fix the widget layout", "make the widget tidy"]),
    "tidy_even": ("", [
        "space the items evenly", "even spaces between items", "line up and space evenly", "equal spaces between the items in each row",
        "distribute items evenly", "align and space evenly", "spread the items evenly", "evenly space the icons", "make the gaps between items equal",
        "tidy up align and space evenly", "line the items up with equal space"]),
    "tidy_pack": ("", [
        "pack the items closer", "bring the items closer together", "tighten the items", "pack items", "move items closer",
        "squeeze the items together", "tidy up pack items closer", "make the items closer to each other", "put the items next to each other",
        "pack the icons together"]),
    "note_add": ("", [
        "add a note {text}", "new note {text}", "note {text}", "write a note {text}", "save a note {text}", "make a note {text}",
        "add note {text}", "create a note {text}", "add a quick note {text}", "write down {text} as a note", "note down {text}",
        "add a note saying {text}", "add a note that says {text}", "new quick note {text}"]),
}
CLASSES.update(NEW)

# More ways of saying the older kinds (v1.1.80)
MORE = {
    "anim_off": ["disable all animations", "i hate the animations", "no more motion", "make it stop moving", "freeze the widget", "kill the animations",
                 "turn the animation off", "stop all the effects", "switch animations off", "i do not want animation", "stop the effects"],
    "anim_on": ["enable all animations", "i want the animations", "bring back the motion", "make it move again", "switch animations on",
                "turn the animation on", "start all the effects", "animate everything", "i like animation turn it on"],
    "transparent": ["make the background see through", "i want the widget transparent", "let me see the wallpaper through the widget",
                    "make the widget fade", "background more see through", "reduce the background opacity", "lower the widget opacity",
                    "make the widget less solid", "give the widget some transparency", "widget background should be transparent"],
    "solid": ["make the background solid", "i want the widget opaque", "stop the widget from being see through", "raise the widget opacity",
              "increase the background opacity", "make the widget not transparent", "widget should be solid", "remove the transparency"],
    "text_bigger": ["i cant read the text make it bigger", "increase the text size", "text is too small", "enlarge the font",
                    "make all the letters bigger", "bigger writing please", "make the words larger"],
    "text_smaller": ["the text is too big", "reduce the text size", "make the font smaller", "shrink the letters", "make all the words smaller",
                     "smaller writing please", "lower the text size"],
    "gap_bigger": ["the rows are too close", "give the rows more room", "put more space between the rows", "spread the rows apart",
                   "widen the gap between rows", "i want bigger spaces between rows"],
    "gap_smaller": ["the rows are too far apart", "less room between the rows", "put less space between rows", "close the gap between rows",
                    "narrow the gap between rows", "i want smaller spaces between rows"],
    "hide": ["i dont need {a}", "remove {a} please", "get {a} off the widget", "dont show {a}", "make {a} disappear", "hide {a} from the widget"],
    "show": ["i need {a} back", "bring {a} back", "make {a} appear", "let me see {a}", "show {a} on the widget", "unhide {a}"],
    "merge": ["make {a} and {b} one shape", "join the {a} and the {b} into one", "put {a} with {b} in one circle", "combine {a} and {b} into one item"],
    "unmerge": ["separate them again", "break the merge", "make them separate", "undo the merged shape", "unjoin the top items"],
    "width_bigger": ["make {a} broader", "widen the {a}", "{a} is too narrow make it wider", "stretch the {a} sideways"],
    "width_smaller": ["make {a} slimmer", "{a} is too wide make it narrower", "squeeze the {a} sideways", "reduce {a} width"],
    "height_bigger": ["raise the height of {a}", "{a} is too short make it taller", "stretch the {a} upward", "make {a} higher"],
    "height_smaller": ["lower the height of {a}", "{a} is too tall make it shorter", "squash the {a} down", "make {a} less tall"],
    "size_bigger": ["{a} is too small make it bigger", "make {a} larger please", "i cant see {a} make it bigger", "scale up {a}"],
    "size_smaller": ["{a} is too big make it smaller", "make {a} tinier", "scale down {a}", "reduce the size of {a} please"],
    "undo": ["undo the last change", "that was wrong undo it", "cancel that change", "put it back how it was", "revert my last change", "oops undo"],
    "redo": ["redo that change", "actually do it again", "redo the last undo", "bring back what i undid"],
    "dark": ["i want a dark look", "use the dark theme", "go dark", "make it black", "night look please"],
    "light": ["i want a light look", "use the light theme", "go light", "make it white", "day look please"],
}
for _k, _v in MORE.items():
    CLASSES[_k][1].extend(_v)

# More near-miss sentences that are NOT commands (so ordinary talk is not taken for one)
CLASSES["none"][1].extend([
    "call mom", "open whatsapp", "what is a shortcut", "how do i add a contact", "dial 911", "call the taxi", "open the camera",
    "what is a fast dial", "who is ali", "message sara", "my note is long", "show me my photos", "delete my account",
    "install an app", "how many shortcuts do i have", "is whatsapp installed", "play spotify", "open youtube", "navigate home",
    "weather in dubai", "remind me about the bill", "what does tidy mean", "what is the weather", "turn up the volume"])

PREFIX = ["", "", "", "", "please ", "can you ", "could you please ", "i want to ", "i would like to ", "hey ", "helper ", "pls ", "just ", "ok "]
SUFFIX = ["", "", "", "", " please", " for me", " now", " thanks", " pls", " quickly"]
SWAPS = {"make": ["turn", "set"], "remove": ["delete", "erase"], "show": ["display"], "hide": ["remove"], "put": ["place"],
         "bigger": ["larger"], "smaller": ["tinier"], "tidy": ["neat"], "rows": ["lines"]}

def typo(word):
    if len(word) < 4 or random.random() > 0.2:
        return word
    op = random.randint(0, 4)
    i = random.randint(1, len(word) - 2)
    if op == 0:
        return word[:i] + word[i + 1] + word[i] + word[i + 2:]
    if op == 1:
        return word[:i] + word[i + 1:]
    if op == 2:
        return word[:i] + word[i] + word[i:]
    if op == 3:
        return word[:i] + random.choice("aeiourstnlm") + word[i + 1:]
    return word[:i] + word[i + 1:] + random.choice("aeiou")

def noisy(s):
    words = s.split(" ")
    words = [typo(w) if w.isalpha() and w not in NAMES and w not in APPS else w for w in words]
    if random.random() < 0.15:
        words = [random.choice(SWAPS[w]) if w in SWAPS and random.random() < 0.5 else w for w in words]
    out = " ".join(words)
    if random.random() < 0.3:
        out = out.capitalize()
    if random.random() < 0.2:
        out = out + random.choice(["!", ".", "?", ""])
    return out

def fill(t):
    if "{a}" in t or "{b}" in t:
        a, b = random.sample(list(ITEMS), 2)
        t = t.replace("{a}", random.choice(ITEMS[a])).replace("{b}", random.choice(ITEMS[b]))
    t = t.replace("{name}", random.choice(NAMES)).replace("{app}", random.choice(APPS)).replace("{text}", random.choice(TEXTS))
    t = t.replace("{slot}", str(random.randint(1, 5)))
    while "{num}" in t:
        t = t.replace("{num}", rand_num(), 1)
    return t

# Words that can stand for each other in a sentence (one word of a phrase is swapped now and then, so the model sees more wordings)
SYN_SWAPS = {
    "remove": ["delete", "erase", "drop", "clear", "take away", "get rid of"],
    "delete": ["remove", "erase", "clear"],
    "hide": ["remove", "turn off", "switch off", "disable", "stop showing", "get rid of"],
    "show": ["display", "turn on", "switch on", "enable", "bring back"],
    "disable": ["turn off", "stop", "switch off", "deactivate"],
    "enable": ["turn on", "start", "switch on", "activate"],
    "bigger": ["larger", "greater", "higher"],
    "larger": ["bigger", "greater"],
    "smaller": ["tinier", "lower"],
    "transparent": ["see through", "faded", "translucent"],
    "opaque": ["solid", "not see through", "not transparent"],
    "animations": ["animation", "motion"],
    "tidy": ["neat", "clean", "organize", "straighten", "sort out"],
    "fix": ["tidy", "repair", "sort out"],
    "add": ["create", "put", "insert", "make"],
    "new": ["fresh", "another"],
    "space": ["gap", "spacing"],
    "spaces": ["gaps", "spacing"],
    "rows": ["lines"],
    "items": ["icons", "buttons"],
    "closer": ["tighter", "nearer"],
    "evenly": ["equally"],
    "dial": ["dialer"],
}

def swap_words(p):
    words = p.split(" ")
    idx = [i for i, w in enumerate(words) if w in SYN_SWAPS]
    if idx and random.random() < 0.4:
        i = random.choice(idx)
        words[i] = random.choice(SYN_SWAPS[words[i]])
    return " ".join(words)

def make_samples(pairs, n_per_phrase):
    data = []
    for name, p in pairs:
        for _ in range(n_per_phrase):
            s = fill(p) if "{" in p else p
            s = swap_words(s)
            s = random.choice(PREFIX) + s + random.choice(SUFFIX)
            data.append((name, noisy(s.lower())))
    return data



# Words that mean the same kind of thing share a "concept" feature, so a wording the model never saw ("greater", "enlarge") still
# points the same way as the ones it did. The table is saved in router.json and read by Router.kt, so the two always agree.
CONCEPT_LISTS = {
    "UP": "bigger larger greater increase enlarge grow higher raise more taller wider longer huge big large up broader scale",
    "DOWN": "smaller tinier decrease reduce shrink lower less shorter narrower thinner slimmer tiny little down fewer tighter",
    "OFF": "hide disable off stop kill deactivate without disappear away unwanted",
    "ON": "show enable on start display activate appear back unhide",
    "REMOVE": "remove delete erase drop clear empty rid",
    "ADD": "add create new put insert assign save",
    "SPACE": "space spaces gap gaps spacing room distance",
    "ROW": "row rows line lines",
    "TIDY": "tidy neat clean organize organise straighten fix arrange sort even evenly equal equally same",
    "PACK": "pack closer together tighten squeeze nearer",
    "DIAL": "dial dials dialer speed",
    "SHORT": "shortcut shortcuts launcher",
    "NOTEW": "note notes",
    "SEE": "transparent transparency translucent faded fade opacity see through",
    "SOLID": "solid opaque",
    "ANIM": "animation animations animate animated motion effects moving",
    "UNDOW": "undo revert cancel oops",
    "MERGE": "merge combine join group",
    "SPLIT": "unmerge split separate unjoin break",
    "WIDE": "width wide wider narrower broader slimmer sideways",
    "TALLW": "height tall taller shorter higher",
    "TEXTW": "text font letters writing words",
}
CONCEPTS = {}
for _c, _ws in CONCEPT_LISTS.items():
    for _w in _ws.split():
        CONCEPTS.setdefault(_w, []).append(_c)

# --- spelling correction before the features (the same rules are in Router.kt: near() and correct())
def near(a, b):
    if a == b:
        return True
    if len(a) == len(b) and len(a) >= 4:
        diff = [i for i in range(len(a)) if a[i] != b[i]]
        if len(diff) == 2 and diff[1] == diff[0] + 1 and a[diff[0]] == b[diff[1]] and a[diff[1]] == b[diff[0]]:
            return True
    mx = 2 if len(a) >= 8 else (1 if len(a) >= 4 else 0)
    if mx == 0 or abs(len(a) - len(b)) > mx:
        return False
    d = list(range(len(b) + 1))
    for i in range(1, len(a) + 1):
        prev, d[0] = d[0], i
        for j in range(1, len(b) + 1):
            cur = d[j]
            d[j] = min(d[j] + 1, d[j - 1] + 1, prev + (0 if a[i - 1] == b[j - 1] else 1))
            prev = cur
    return d[len(b)] <= mx

WORDS = set()
def collect_words():
    texts = []
    for _, (_, phrases) in CLASSES.items():
        texts += phrases
    for v in ITEMS.values():
        texts += v
    texts += NAMES + APPS + TEXTS + [x for x in PREFIX + SUFFIX if x.strip()]
    for k, v in SYN_SWAPS.items():
        texts += [k] + v
    for t in texts:
        for w in re.split(r"[^a-z]+", t.lower()):
            if w:
                WORDS.add(w)

def correct(text):
    out = []
    for w in [w for w in re.split(r"[^a-z]+", text.lower()) if w]:
        if w in WORDS or len(w) < 4:
            out.append(w)
            continue
        cands = sorted(v for v in WORDS if len(v) >= 4 and near(w, v))
        pick = (next((v for v in cands if v[0] == w[0]), cands[0])) if cands else w
        out.append(pick)
    return " ".join(out)

# --- features: the same rules are written in Kotlin (Router.kt), keep them in step
def features(text):
    words = [w for w in re.split(r"[^a-z]+", text.lower()) if w]
    counts = {}
    for idx, w in enumerate(words):
        counts["w:" + w] = counts.get("w:" + w, 0) + 1
        p = " " + w + " "
        for n in (3, 4, 5):
            for i in range(len(p) - n + 1):
                g = "c:" + p[i:i + n]
                counts[g] = counts.get(g, 0) + 1
        if idx > 0:
            g = "b:" + words[idx - 1] + " " + w
            counts[g] = counts.get(g, 0) + 1
        for cc in CONCEPTS.get(w, []):
            g = "k:" + cc
            counts[g] = counts.get(g, 0) + 1
    norm = sum(v * v for v in counts.values()) ** 0.5 or 1.0
    return {k: v / norm for k, v in counts.items()}

def build_matrix(feats, vocab):
    from scipy.sparse import csr_matrix
    rows, cols, vals = [], [], []
    for r, f in enumerate(feats):
        for k, v in f.items():
            j = vocab.get(k)
            if j is not None:
                rows.append(r); cols.append(j); vals.append(v)
    return csr_matrix((vals, (rows, cols)), shape=(len(feats), len(vocab)))

if __name__ == "__main__":
    import os, time
    K = int(sys.argv[2]) if len(sys.argv) > 2 else 60000      # features kept: more = bigger and smarter file
    REPS = int(sys.argv[3]) if len(sys.argv) > 3 else 40       # how many noisy sentences are made from each phrase
    names = list(CLASSES)
    collect_words()
    random.seed(7)
    # 1. honest test: a fifth of the phrases of each kind are kept back and never trained on
    train_pairs, test_pairs = [], []
    for n, (_, phrases) in CLASSES.items():
        ph = list(phrases)
        random.shuffle(ph)
        cut = max(1, len(ph) // 5) if len(ph) >= 6 else 0
        test_pairs += [(n, p) for p in ph[:cut]]
        train_pairs += [(n, p) for p in ph[cut:]]
    def fit(pairs, reps):
        data = make_samples(pairs, reps)
        vocab = {}
        for _, t in data:
            for k in features(correct(t)):
                if k not in vocab:
                    vocab[k] = len(vocab)
        X = build_matrix([features(correct(t)) for _, t in data], vocab)
        y = [names.index(c) for c, _ in data]
        clf = LogisticRegression(C=30.0, max_iter=400)
        clf.fit(X, y)
        return clf, vocab, len(data)
    t0 = time.time()
    clf, vocab, n = fit(train_pairs, REPS)
    W = clf.coef_
    keepj = np.argsort(-np.abs(W).max(axis=0))[:K]
    mask = np.zeros(W.shape[1]); mask[keepj] = 1.0
    test = make_samples(test_pairs, 6)
    Xte = build_matrix([features(correct(t)) for _, t in test], vocab)
    Z = Xte @ (W * mask).T + clf.intercept_
    Z = Z - Z.max(axis=1, keepdims=True)
    P = np.exp(Z); P = P / P.sum(axis=1, keepdims=True)
    pred = P.argmax(axis=1)
    yte = np.array([names.index(c) for c, _ in test])
    for th in (0.5, 0.65):
        sure = P.max(axis=1) >= th
        print("held-out phrases, confidence >=", th, ": acted-on", round(float(sure.mean()), 3),
              " wrong when acted-on", round(float(np.mean((pred != yte) & sure) / max(sure.mean(), 1e-9)), 3))
    print("held-out accuracy", round(float(np.mean(pred == yte)), 3), "train samples", n, "secs", int(time.time() - t0))
    # 2. the model that ships is trained on all phrases
    all_pairs = train_pairs + test_pairs
    clf, vocab, n = fit(all_pairs, REPS)
    W = clf.coef_
    score = np.abs(W).max(axis=0)
    keep = sorted(np.argsort(-score)[:K].tolist())
    inv = {v: k for k, v in vocab.items()}
    model = {
        "classes": names,
        "features": [inv[j] for j in keep],
        "weights": [[round(float(W[k, j]), 2) for j in keep] for k in range(W.shape[0])],
        "bias": [round(float(b), 3) for b in clf.intercept_],
        "canon": {k: v[0] for k, v in CLASSES.items()},
        "words": sorted(WORDS),
        "concepts": CONCEPTS,
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(model, f, separators=(",", ":"))
    print("classes", len(names), "vocab", len(vocab), "kept features", len(keep), "file MB", round(os.path.getsize(OUT) / 1e6, 2))
