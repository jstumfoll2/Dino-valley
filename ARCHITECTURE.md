# Architecture, as built

This describes the game as it is on `main` today. The architecture first proposed for it (a picture book called Dino
Valley, with Room, JSON content packs and a parent mode) is kept in
[`docs/archive/ARCHITECTURE-proposal.md`](docs/archive/ARCHITECTURE-proposal.md); most of it was never built. Why things are the way they are
is in [`PROJECT_DECISIONS.md`](PROJECT_DECISIONS.md). Known problems and what is planned are in
[`docs/review/improvement-plan.md`](docs/review/improvement-plan.md).

## The idea

A child walks across a kingdom, Whisperwood, choosing roads on a painted map. Towns have people with lives of their own; roads and dungeons
have puzzles and monsters; one story (an *arc*) pulls it together and ends in a lair where the boss can be beaten or befriended. There are five
stories, and a Storybook closes with its finale and a ball for the friends the child made. Every puzzle is practice for a skill (counting, numbers,
adding, colors, patterns, letters, skip counting, tracing and writing, memory, sorting, jigsaws, maps, brewing, rhymes, money, sharing, listening, remembering
the trip), and the game chooses puzzles from what the child has shown. There is no network, no account and no AI at run time.

## Two modules

```
:engine   pure Kotlin/JVM: every rule, all content, all words. No Android, so the compiler keeps UI out of the rules.
:app      Android + Jetpack Compose: drawing, sound, saving, the screens.
```

`:app` depends on `:engine`; never the other way. Engine tests run on the JVM in a few minutes (most of it the voice catalog check) and
need no phone. The app can only be compiled with the Android SDK, so the `Build` workflow (`.github/workflows/android.yml`) compiles it.

## The engine

Everything the child does happens in `rpg/run`:

- **`Journey`** is one adventure. (A dungeon deals its rooms by kind, `RoomKind`: sixteen of them, each a skill, a backdrop and three lines; a story's peaceful way is a list of puzzle kinds, `peaceChallenge`.) It hands out a `Beat` (something to show: `Tell`, `Ask`, `Choose`, `Travel`, `Shop`, `Found`, `Roll`,
  `Night`, `Finale`) and takes back a `Reply` (`Next`, `Solved`, `Picked`, `Bought`, `Rolled`). Inside it is a queue of `JStep(beat, then)`;
  `then` says what to line up after the reply. The parts live in extension files that share its state: `JourneyTravel` (the map and roads),
  `JourneyTown` (people, shops, dialog), `JourneyBattle`, `JourneyPuzzles` (obstacles, costumes, rescues), `JourneyDungeon` (rooms, vaults,
  the lair). `JourneyLines`, `RoomLines` and `Say` hold the words.
- **A journey is its seed and the child's taps.** Every random draw comes from `Random(seed)`, and every tap is a `Command` (a `Reply` and its
  time). `Journey.replay(seed, hero, skills, world, commands)` rebuilds the same journey beat for beat. This is how the app saves after every
  tap and how a playtest note can be replayed. Nothing in the engine may use another source of randomness or time.
- **Days.** `Beat.effortSeconds()` estimates how long a beat takes a child. After `Settings.dayMinutes` of it, the party camps (`Beat.Night`).

What the journey is made from, in `rpg/*`:

| Package | What is in it |
|---|---|
| `content` | `Content`: the registry. Items, monsters, locations, roads, arcs, people, shops and flag-opened roads are `ContentPack`s built once, looked up by id. |
| `world` | `Kingdom` (places and roads, `CoreKingdom`), `Terrain`, `RoomKind`, `WorldMemory` (what is remembered between adventures). |
| `story` | `Arc` (a story with variants, moments, setup, peace and fight steps, an optional companion, and a finale flag), `ArcPicker` (the finale when four pages are home), `Npc` and dialog graphs (`Dialog.kt`: conditions and effects), `CoreArcs`, `CoreNpcs`. |
| `hero` | `Hero` (class, stars, coins, bag, worn gear), `HeroClass`, `Power`, `Progression` (levels and unlocks). |
| `battle` | `Monster`, `Tier`, `CoreMonsters`; fights are rounds with a puzzle each. |
| `items` | `Item`, `Slot`, `Obstacle`, shops. |
| `learn` | `Skill`, `SkillBook` (levels), `ChallengeRecord`, `Challenge` kinds (pick one of several, `PictureChallenge` for pictures and numbers on cards, `GridChallenge`, tracing, memory, bells, recipes, sorting, jigsaws), `ChallengeFactory` and `PictureFactory` (make puzzles), `Coach` (hints and explanations), `Words` (numbers and letters said right). |
| `rpg/VoiceCatalog.kt` | Lists every sentence the game can say, for the build to record. |

### Content is data

A new person, monster, item, place, road, shop or story is an entry in a pack, plus a picture named after it (`art_npc_<id>_*`,
`art_monster_<id>_*`, `art_item_<id>`, `art_gear_<id>`, `art_scene_<place>`). `ArtCoverageTest` fails if content has no picture, and
`ContentRegistryTest` checks references (a road to a place that exists, an item a shop sells). See
[`docs/design/adding-content.md`](docs/design/adding-content.md). Puzzles themselves are code (`ChallengeFactory`), not data.

### Learning

Each skill has a level from 1 to 5 in `SkillBook`, moved by a windowed rule over recent first-try results (decision #54). The hero's own
level never makes puzzles harder. Obstacles are *costumes* (`Costume`): the terrain says what dresses the puzzle and the child's recent practice
picks the skill (`pickSkill`), so no skill takes over a journey. After the last miss a puzzle is explained out loud (`Coach.explain`). Every
answered puzzle is a `ChallengeRecord`, kept in `challenges.jsonl` for tuning and any later model.

### Words and voices

Every sentence the game says is plain text in code or content, with `<pet>`-style tags for who speaks, `{name}` for the dragon's name and
`[creak]` for a sound effect. `VoiceCatalog` lists all of them (by playing simulated adventures and by enumerating every small domain of
numbers, colors and names); the build records each one (`scripts/render-voice.py`, Kokoro through sherpa-onnx), and the phone plays the
recordings by fingerprint (`Voice.key`). A sentence that is not in the catalog is made on the phone by the same voice, slowly: so
`VoiceCatalogTest` fails when one is missing. Rules for writing lines are in decision #59.

## The app

```
MainActivity            hides the system bars, provides the narrator, sound, dragon name; title screen or adventure; Back asks first
ui/game/GameViewModel   holds the Journey; saves every tap; resumes a saved adventure; stores the hero when one ends
ui/game/TitleScreen     the camp: pick a hero, name the dragon, start or continue
ui/game/AdventureScreen draws a Beat: backdrop, cast, then the Tell/Ask/Choose/Travel/Shop/Found/Roll/Night beat on top
ui/game/Challenges.kt   the puzzle screens (AskBeat: counting, adding, colors, patterns, letters, tracing and the lamp wire, memory doors, the bell song, sorting, skip
                        counting, jigsaw, potions, picture cards, the treasure map); judge a try and reply Solved
ui/game/JourneyUi.kt    the map (TravelBeat), shops, people (NpcStand) and monsters (BattleStage) on stage
ui/game/MagicFx.kt      magic effects the narrator's words set off (sparkles, poofs, the wizard's hat); Finale.kt: the end of an adventure
ui/art                  Rigs (layered characters), Pictures (art by name), Pictos (button symbols drawn in code)
audio                   Narrator (plays recordings, makes missing sentences), Sfx, VoiceBank, KokoroVoice
data/Save.kt            save.json, journey.json + journey.log, challenges.jsonl; all in the app's private storage
feedback                FeedbackLog: the playtest note (screen, seed, taps, last things said)
```

The UI decides nothing about the story: it shows a beat and sends back a reply. (One known exception: the puzzle screens still judge
whether a try is right, except the bell song, which `BellChallenge.expects` judges; moving the rest into the engine is step 2.8 of the improvement plan.)

### Saving

`save.json` has the hero, skill levels and what the world remembers, stored when an adventure ends. While one is under way, `journey.json`
(its seed and starting state) and `journey.log` (one line per tap) hold it; leaving, closing or losing the phone leaves it waiting at the camp
(decision #53). `challenges.jsonl` gets each puzzle as it is answered. Nothing leaves the phone: the app has no network permission.

### Art

Art is drawn as code in `art/src/*.py` (SVG), built into layered WebP pictures by `art/build.py` (Chromium and ffmpeg), and committed as
`app/src/main/res/drawable-nodpi/art_*.webp`. New pieces are previewed first with `art/preview.py`; the workflow and what each look found are in [`docs/art-plan.md`](docs/art-plan.md). `Rigs` stacks the layers of a character (body, gear, hat) and animates them; `Art.byName` finds a
picture by name. Backdrops are 1920x1080 scenes. Known layering problems and the fix plan are in the review's section 6.

### Sound

Spoken words are the recordings above. Sound effects are small files in `assets/sfx` (CC0 packs and `scripts/make-sfx.py`, which also synthesizes the three bells of the Bell Song;
credits in [`docs/SOUND_CREDITS.md`](docs/SOUND_CREDITS.md)). Held letter sounds and the dragon's growl are made, not spoken (decision #50). There is no music yet.

## Testing and CI

- `./gradlew :engine:test` runs about 120 tests: rules, content, learning rules, the command log, voice coverage, and the **balance
  harness** (`rpg/balance`): simulated children play many journeys and the tests hold what matters (levels follow ability, answers can't be
  guessed from where they sit, no forced fights, a sitting is about ten minutes). `./gradlew :engine:balanceReport` prints the full table.
- The `Build` workflow: fetches the voice, lists and records every sentence, runs the engine tests, builds the debug APK, prints the balance
  report in the run summary, and on `main` publishes the APK as the `latest` release. A manual run can turn recording off (`record_voice`) to check quickly that the tests pass and the app compiles.
- The app has no tests yet (they need the Android SDK); screenshot tests are step 4.6 of the plan.

## Dependencies

Kotlin 2.3, Android Gradle Plugin 9.3, Jetpack Compose (BOM 2025.09), activity-compose, lifecycle. On the phone: sherpa-onnx 1.13.8 (Apache-2.0) and
the Kokoro v1.0 voice model (Apache-2.0, 54 speakers), which includes espeak-ng data (GPL-3.0; see area X3 of the plan). minSdk 26, targetSdk 36.
