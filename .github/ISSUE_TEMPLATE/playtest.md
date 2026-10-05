---
name: Playtest report
about: Something we noticed while he was playing (a bug, a confusing moment, or something he loved)
labels: playtest
---

**What kind?** (Sound / Looks wrong / Confusing / Too hard / Too easy / Broken / He loved this)

**What happened?**
<!-- One or two sentences. What he did, what the game did. -->

**What did he do or say?**
<!-- His reaction is often the most useful part. -->

**Screenshot / voice note**
<!-- Drag in a screenshot, or summarize what he said. -->

**The note from the game**
<!--
The feedback button (the small speech bubble: top left under the speaker in an adventure, bottom left at camp) writes a text note and puts the whole of
it on the clipboard. Paste it here. It has, in order:

- "Where it happened": the beat on screen (story, challenge with its level and seed, map, shop, night, ...).
- "Adventure (replayable)": the seed, hero class and level, and skill levels. Every reply so far.
- "Replay data": journey.json and journey.log exactly as saved. Together they rebuild the adventure beat for
  beat (see below). Only there while an adventure is under way.
- "Last things heard and done": the last 60 sentences said and taps made.
- "Phone": app version, phone and Android version.
-->

**Crash?**
<!-- If the game closed by itself, say when. The adventure is saved after every tap, so Continue on the camp screen brings it back. -->

<!--
For whoever picks this up: to see exactly what he saw, put the "Replay data" into a test.
journey.json is { seed, hero, skills, world }; journey.log is one line per tap (`CommandCodec`).

    val commands = CommandCodec.decodeAll(logText)
    val journey = Journey.replay(seed, hero, skills, world, commands)   // journey.beat is what was on screen
-->
