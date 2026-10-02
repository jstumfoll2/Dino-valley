# Dungeon Quest (working title)

A playful, offline Android learning game for a four-year-old, built as a
family project. Kotlin + Jetpack Compose. No accounts, no ads, no network, no
AI at runtime: AI helps us during development, and the finished game runs on
rules, procedural generation, good game design, and local data.

**Status:** first playable counting prototype.

**Try it:** on the phone, open the [latest build](https://github.com/jstumfoll2/dungeonquest/releases/tag/latest),
download `dino-valley.apk`, and open it to install. The first time, Android
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

The first milestone is deliberately small: one adorable dinosaur, one
beautiful valley, one counting game, and satisfying animations. The test is
simple: does he ask to play again?
