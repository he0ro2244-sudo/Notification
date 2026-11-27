Helper test list
================
sentences.tsv holds one sentence per line and what the helper should do with it.
Lines starting with # are comments.

How to use it
- Run each sentence through the Settings helper box on a phone or emulator build.
- Compare the message shown with the expected result. "Changed" lines are about the effect;
  the wording of the message may differ slightly.
- Typos, short forms and "make" / "change" / "set" variants count as the same request.
- Any sentence that shows "Not understood" is also written to the helper's missed list
  (help > Sentences it missed). Add those to this file once you have decided what they should do.

This file is a checklist. It is not run automatically: the helper needs the Android app to
run, so these checks happen on a device.
