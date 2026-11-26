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
        "make {a} invisible", "take away {a}", "switch off {a}"]),
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

FILLERS = ["", "", "", "please ", "can you ", "hey ", "could you ", "i want to ", "pls "]

def typo(word):
    if len(word) < 4 or random.random() > 0.35:
        return word
    op = random.randint(0, 4)
    i = random.randint(1, len(word) - 2)
    if op == 0:  # swap two letters
        return word[:i] + word[i + 1] + word[i] + word[i + 2:]
    if op == 1:  # drop a letter
        return word[:i] + word[i + 1:]
    if op == 2:  # double a letter
        return word[:i] + word[i] + word[i:]
    if op == 3:  # replace a letter with a neighbour
        return word[:i] + random.choice("aeiourstnlm") + word[i + 1:]
    return word[:i] + word[i + 1:] + random.choice("aeiou")  # extra vowel at the end

def noisy(s):
    words = s.split(" ")
    words = [typo(w) if w.isalpha() else w for w in words]
    out = " ".join(words)
    if random.random() < 0.3:
        out = out.capitalize()
    if random.random() < 0.2:
        out = out + random.choice(["!", ".", "?", ""])
    return out

def fill(template):
    a, b = random.sample(list(ITEMS), 2)
    sa = random.choice(ITEMS[a])
    sb = random.choice(ITEMS[b])
    return template.replace("{a}", sa).replace("{b}", sb)

def samples(n_per_phrase=12):
    data = []
    for name, (_, phrases) in CLASSES.items():
        for p in phrases:
            for _ in range(n_per_phrase):
                s = p
                if "{" in s:
                    s = fill(s)
                s = random.choice(FILLERS) + s
                data.append((name, noisy(s.lower())))
    return data

# --- features: the same rules are written in Kotlin (Router.kt), keep them in step
def features(text):
    words = [w for w in re.split(r"[^a-z]+", text.lower()) if w]
    counts = {}
    for w in words:
        counts["w:" + w] = counts.get("w:" + w, 0) + 1
        p = " " + w + " "
        for n in (3, 4):
            for i in range(len(p) - n + 1):
                g = "c:" + p[i:i + n]
                counts[g] = counts.get(g, 0) + 1
    norm = sum(v * v for v in counts.values()) ** 0.5 or 1.0
    return {k: v / norm for k, v in counts.items()}

def build_matrix(feats, vocab):
    X = np.zeros((len(feats), len(vocab)))
    for r, f in enumerate(feats):
        for k, v in f.items():
            j = vocab.get(k)
            if j is not None:
                X[r, j] = v
    return X

if __name__ == "__main__":
    random.seed(7)
    train = samples(12)
    test = samples(3)
    names = list(CLASSES)
    # vocabulary from training data only
    vocab = {}
    for _, t in train:
        for k in features(t):
            if k not in vocab:
                vocab[k] = len(vocab)
    Xtr = build_matrix([features(t) for _, t in train], vocab)
    ytr = [names.index(c) for c, _ in train]
    clf = LogisticRegression(C=20.0, max_iter=2000)
    clf.fit(Xtr, ytr)
    Xte = build_matrix([features(t) for _, t in test], vocab)
    yte = [names.index(c) for c, _ in test]
    pred = clf.predict(Xte)
    acc = np.mean(np.array(pred) == np.array(yte))
    print("vocab", len(vocab), "train", len(train), "test accuracy", round(float(acc), 3))
    # Keep only features with non-zero weight to shrink the file
    W = clf.coef_
    keep = [j for j in range(W.shape[1]) if np.any(np.abs(W[:, j]) > 1e-3)]
    inv = {v: k for k, v in vocab.items()}
    model = {
        "classes": names,
        "features": [inv[j] for j in keep],
        "weights": [[round(float(W[k, j]), 3) for j in keep] for k in range(W.shape[0])],
        "bias": [round(float(b), 3) for b in clf.intercept_],
        "canon": {k: v[0] for k, v in CLASSES.items()},
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(model, f, separators=(",", ":"))
    import os
    print("kept features", len(keep), "file bytes", os.path.getsize(OUT))
