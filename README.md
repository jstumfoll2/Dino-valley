# The Little Dungeon

A playful, offline Android learning game for a four-year-old, built as a
family project. Kotlin + Jetpack Compose. No accounts, no ads, no network, no
AI at runtime: AI helps us during development, and the finished game runs on
rules, procedural generation, good game design, and local data.

**Status:** first dungeon. Each adventure is a new map and story: pick doors, solve
rune doors, crystal caves, spell books, magic paths and potions, befriend the goblin, and
meet the dragon, with your own baby dragon along. Stars level your hero up between runs.

**Try it:** on the phone, open the [latest build](https://github.com/jstumfoll2/the-little-dungeon/releases/tag/latest),
download `the-little-dungeon.apk`, and open it to install. The first time, Android
asks to allow installs from your browser or Files app.

**Who does what:** Claude implements everything (code, art, tests, builds).
The family directs: creative calls, voice recordings, and playtests with the
real player.

- [`ARCHITECTURE.md`](ARCHITECTURE.md): project structure, engine design,
  data model, difficulty rules, content schema, dependencies, the first-prototype
  plan, and open questions.
- [`voice/VOICE_SCRIPT.md`](voice/VOICE_SCRIPT.md): the lines to record for
  the game's voice, and how to record them.
- [`PROJECT_DECISIONS.md`](PROJECT_DECISIONS.md): every meaningful
  architectural choice, why we made it, what else we considered, and what
  would make us change it.
- [`docs/design/`](docs/design/): the family's brief for The Little Dungeon and
  how it became the game.

The test is still simple: does he ask to play again?
