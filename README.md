# The Little Dungeon

A playful, offline Android learning game for a four-year-old, built as a
family project. Kotlin + Jetpack Compose. No accounts, no ads, no network, no
AI at runtime: AI helps us during development, and the finished game runs on
rules, procedural generation, good game design, and local data.

**Status:** a playable journey across the kingdom of Whisperwood. The child picks roads on a painted map, makes
friends in towns, solves puzzles that block the way, fights (and befriends) monsters, and brings a lost page of the
Storybook home from Baron Grumblewick, with their own baby dragon along. Counting, numbers, adding, colors, patterns,
letters, skip counting, tracing, memory, sorting and jigsaws are all practiced inside the story, and the game follows
what the child shows: it never asks for much more than they can do. An adventure is played in days of about ten
minutes, is saved after every tap, and carries on from the camp. There is one story so far, told three ways; more
are planned (see below).

**Try it:** on the phone, open the [latest build](https://github.com/jstumfoll2/the-little-dungeon/releases/tag/latest),
download `the-little-dungeon.apk`, and open it to install. The first time, Android
asks to allow installs from your browser or Files app.

**Who does what:** Claude implements everything (code, art, tests, builds).
The family directs: creative calls, voice choices, and playtests with the
real player.

## Where to read

- [`ARCHITECTURE.md`](ARCHITECTURE.md): the game as built: the two modules, the journey, the learning rules, saving, voices,
  art, tests and CI.
- [`PROJECT_DECISIONS.md`](PROJECT_DECISIONS.md): every meaningful choice, why we made it, what else we considered, and what
  would make us change it. Entries that were later replaced say so.
- [`docs/design/`](docs/design/): the family's brief ([`little-dungeon-brief.md`](docs/design/little-dungeon-brief.md)) and how it
  became the game; [`adding-content.md`](docs/design/adding-content.md) is how to add a person, monster, item, place or story.
- [`docs/review/`](docs/review/2026-10-04-review-and-roadmap.md): a critical review of the game (architecture, story, learning,
  art, voice) and a roadmap; [`improvement-plan.md`](docs/review/improvement-plan.md) tracks every finding to the step that fixes
  it and says what is done.
- [`voice/VOICE_BIBLE.md`](voice/VOICE_BIBLE.md): who speaks, how each voice is made, and the choices waiting for a person.
- [`docs/SOUND_CREDITS.md`](docs/SOUND_CREDITS.md): where each sound effect comes from.
- [`docs/archive/`](docs/archive/): plans for versions of the game that no longer exist.

## Working on it

```
./gradlew :engine:test              # the rules, content, learning, voice coverage and balance tests (a few minutes)
./gradlew :engine:balanceReport     # simulated children play many journeys: session length, skill mix, levels
```

The Android app needs the Android SDK and the narrator voice (`scripts/fetch-voice.sh`); the `Build` workflow does both and
attaches the APK to every run. Playtest notes come from the speech-bubble button in the game; paste one into a
[playtest issue](.github/ISSUE_TEMPLATE/playtest.md), which says how to replay the adventure it describes.

The test is still simple: does he ask to play again?
