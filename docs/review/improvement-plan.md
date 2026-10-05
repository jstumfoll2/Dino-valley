# Improvement plan: tracker

This turns [the review](2026-10-04-review-and-roadmap.md) into work that can be checked off. Each row is a
finding from the review, the step that fixes it, and where it stands. Statuses:

- **Done**: fixed on this branch and covered by a test, the balance report, or a mock where a test can't reach.
- **Partly**: some of it fixed; the row says what is left.
- **Planned**: not started; the step names where it fits.
- **Needs a person**: needs a decision, a recording or a playtest, not code.

## How each stage is verified

- **Engine** (`:engine`): the full test suite and the balance report run on every commit
  (`./gradlew :engine:test :engine:balanceReport`). Numbers the review measured are held by tests in
  `engine/src/test/.../balance`, added in the same commit as the fix that makes them true.
- **App** (`:app`): the Android SDK can't be downloaded in the authoring environment (the network policy blocks
  `dl.google.com`), so app changes are compiled and packaged by the repository's `Build` workflow on GitHub Actions.
  Each app-touching stage is dispatched on this branch, and the result is noted in the stage's row.
- **Art**: layering fixes are checked with composite mock-ups saved in `docs/review/img/`.

## The ten findings that matter most

| ID | Finding | Step | Status |
|---|---|---|---|
| T1 | A journey takes about 25 minutes and can't be saved | 1.1 to 1.4 | **Done**: saved after every tap; sittings of about 10 minutes (see "What changed in the numbers") |
| T2 | Difficulty only ever goes up | 2.1 | **Done** |
| T3 | The right answer is usually the middle card | 2.2 | **Done** |
| T4 | There is one story | 7.1 and up | **Partly**: four stories (the Baron, the dragon, the lanterns and the Ink Shadow finale with the Storybook Ball); five more in the roadmap need people or places that do not exist yet |
| T5 | The world's memory is mostly write-only | 3.1, 3.2 | **Done** |
| T6 | Half of the brief's learning is absent | 2.3, then 6.x | **Mostly done**: maps, recipes, rhymes, money, sharing, listening and story all have puzzles and rooms (Phase 6); a few ladders stop short (see the table) |
| T7 | Class powers and unlocks do nothing | 3.5, 3.6 | **Done** |
| T8 | Two game engines in one codebase | 0.2 | **Done** |
| T9 | Layering bugs on every screen type | 4.1 to 4.4 | **Partly**: gear order, the dialog tray, the foe portrait and the shop counter are fixed in code; backdrops, ground line and a true map need new art (Phase 4) |
| T10 | Docs describe three different games | 0.1 | **Done** |

## What changed in the numbers

From `./gradlew :engine:balanceReport` (ten simulated children, ten journeys each, at 60%, 75% and 90% of puzzles right). "Before" is the review's
Appendix A, measured on `main` with the original harness; the harness was refined when it moved into the repository (it now uses the engine's own
time estimate), so the minutes are close but not exact comparisons. The sitting length, the levels, the guessing and answer-position checks and the zero counts are held by tests (`BalanceTest`, `AnswerLayoutTest`, `WorldRulesTest`); the whole-journey minutes and the skill shares are reported, not held.

| Measure | Before (60% / 75% / 90%) | After phases 0 to 4 | Now, with all of the new content |
|---|---|---|---|
| Median minutes in one sitting | 28.3 / 24.7 / 23.7 (the whole journey; no way to stop) | **10.0 / 9.7 / 9.7** | **10.2 / 10.2 / 10.0** (90th percentile 15.1 / 13.9 / 14.0) |
| Median minutes in a whole journey | 28.3 / 24.7 / 23.7 | 18.8 / 18.2 / 19.2 | **30.0 / 24.9 / 25.4** in two or three nights (90th percentile 86 / 68 / 61); the Baron's story alone is 21.5 / 23.4 / 19.2 |
| Mean level of counting, numbers, adding, colors, patterns and letters after ten journeys | 4.9 / n.a. / 5.0 | 1.2 / 1.7 / 3.0 | **1.2 / 2.1 / 3.2** |
| Counting level reached by a child who only guesses (40 one-try puzzles) | 3 | **1** | **1** |
| How often the right card is in the best-guess position (counting, level 4) | 100% (fair is 20%) | **21%** | **21%** |
| Forced-fight menus per 100 journeys | 18 / 22 / 20 | **0** | **0** |
| Named characters met as random monsters per 100 journeys | 92 / 83 / 88 | **0** | **0** |
| Faints per journey | 0.91 / 0.42 / 0.13 | 0.15 / 0.09 / 0.01 | 0.45 / 0.12 / 0.02 (three more bosses to meet) |
| One-try misses that say "try again" | (not measured) | **0 of 696 / 387 / 152** | **0 of 1259 / 589 / 216** |
| Share of puzzles: every everyday skill (counting, numbers, adding, colors, patterns, letters, skip counting) | 19% for the biggest | 11% to 16% | **9% to 12%** |
| Share of puzzles: rhymes / money / sharing / maps / story (new) | 0% | not yet | **3.5% / 3.7% / 5.3% / 3.4% / 2.9%** at 60% (about the same at 75% and 90%) |
| Share of puzzles: listening / recipes / tracing / memory / sorting / jigsaws | 0% / 0% / 1% / 1% / 1% / 1% | the same | **1.4% / 1.1% / 1.4% / 1.1% / 1.0% / 0.7%** |

What did not move: the skills that are only dealt in dungeon rooms (listening, recipes, tracing, memory, sorting, jigsaws) are still 1% to 2% of puzzles each, short of the roadmap's 5%,
because a journey visits few dungeons and each deals three of fifteen kinds of room. Raising that means more rooms per journey (longer) or putting them on the roads (a different
design); neither is done. The whole-journey time grew from about 19 minutes to about 25 to 30: the Baron's story alone is up to five minutes longer than it was (probably the new rooms and the recap at the end; not separated out), and
the other stories have lairs about six roads from the camp, which the simulated child (who follows the dragon's marked road only 60% of the time) wanders towards for longer. What the
child feels, a sitting of about ten minutes, did not change. A real child follows the marked road more often than this one does, so the simulation is pessimistic here.

## Phase 0: clean foundation

| Step | What | Status |
|---|---|---|
| 0.4 | Balance harness in the repo, `:engine:balanceReport`, CI summary | **Done** (`BalanceSim`, `BalanceTest`) |
| 0.2 | Remove the legacy single-dungeon engine and its dead UI, migrate saves | **Done**; old saves still load (decision #57) |
| 0.3 | `dealt` becomes a field of `Journey`; immutable content registry with id maps | **Done** (`ContentRegistryTest`) |
| 0.1 | Truth pass on the docs: README, architecture as built, decisions, plan banners, voice bible, issue template | **Done**: `ARCHITECTURE.md` rewritten (proposal archived), decisions marked and #53 to #61 added, banners on both plans, `adding-content.md` corrected, `voice/VOICE_BIBLE.md`, the playtest template |

## Phase 1: never lose progress; shorter sessions

| Step | What | Status |
|---|---|---|
| 1.1 | Command log, autosave after every command, "Continue" on the title screen, determinism test | **Done** (`CommandLogTest`; app compiled by the Build workflow) |
| 1.2 | Challenge records appended as they happen | **Done** |
| 1.3 | Back asks first | **Done** |
| 1.4 | Shorter journeys with a camp-for-the-night stop; simulated median 14 minutes or less | **Done as sittings**: median sitting about 10 minutes, a whole journey still about 19 minutes over two or three days (`BalanceTest`). The story itself was not shortened. |

## Phase 2: learning integrity

| Step | What | Status |
|---|---|---|
| 2.1 | Honest failure records, windowed mastery, hero level affects monsters only, warm-up | **Done** (`SkillBookTest`, `BalanceTest`) |
| 2.2 | Unbiased answer layouts | **Done** (`AnswerLayoutTest`: the best position's share is close to fair) |
| 2.3 | Skill quota: terrain picks the costume, the learner picks the skill | **Done** (`SkillQuotaTest`) |
| 2.4 | Teach after a miss; one-try miss lines; skip-count wording | **Done** (`LearningFixesTest`) |
| 2.5 | Phonics: stop sounds without the vowel, short a, lowercase track, name letters first | **Done** (`PhonicsTest`); the new sounds need a person to listen |
| 2.6 | NPC puzzles that fit their story | **Done** |
| 2.7 | Color-blind support: a shape or mark with every hue | **Done in code** (a mark for each color on gems, doors and color-only patterns); needs a look on a phone |
| 2.8 | Judging moves into the engine (`ChallengeSession`) | Planned (later phase) |

## Phase 3: story and world continuity

| Step | What | Status |
|---|---|---|
| 3.1 | Flags that are written are read (validator rule); the Baron after peace and after a fight | **Done** (`StoryMemoryTest`) |
| 3.2 | Named characters are never random monsters | **Done** (`BalanceTest`: zero ambushes by named characters) |
| 3.3 | A peaceful way out of every conversation; tolls that gate something; "find another way" offers one | **Done** (`BalanceTest`: zero forced-fight menus; `WorldRulesTest`) |
| 3.4 | Kindness economy | **Done**: friendship earns kindness, kindness lowers prices and opens kind choices |
| 3.5 | Class powers in the journey | **Done** (`ClassPowersTest`; as built: decision #40; the dice are gone, so the Knight's power is a second wind) |
| 3.6 | Deliver or drop every unlock | **Done**: unlocks that need art that does not exist are no longer announced |
| 3.7 | The dragon's name is spoken; "Sparky" never hard-coded | **Partly**: spoken, and never hard-coded. Sentences with a typed name are still made on the phone the first time, not pre-made when the name is set |
| 3.8 | Clues once per world; greeting by state; Book One finale | **Partly**: greeting by state, a shorter second telling and the Book One finale (7.5) are done; clues once per world are not |

Also fixed under Phase 3 (from the review's table of story and loop inconsistencies):

- The Mossy Golem was an ordinary roaming monster as well as the mine's guardian; it is now a mini-boss that stays in its mine.
- "A wolf pup blocks the way! What will you do?" started the fight whatever the child did. A monster on a road or in a wild place is now a real choice
  (fight, or go away), held by `WorldRulesTest`.
- Captions say "Professor Hoot:" and "Bandit Bess:", not "Elder:" and "Sneak:", when the scene knows who is speaking.

Still open from that table: clues that repeat every adventure (4), time of day and scene state (17), peace and fight costing different things (18), coin sinks (19), and a journey that is mostly
travel and quizzes (20). Since this was written, row 1 (one story only) and row 3 (the Book One finale) are done in part: there are four stories and a finale with the Storybook Ball (Phase 7),
and Dragon's Peak, the Bat King and the Ink Shadow now have roles (row 16; Ruby and the goblin still have none).

## Phase 4: presentation and art fixes

These are app drawing changes. The Android SDK is not available where this was written, so the `Build` workflow compiles them, and how they
look is for a playtest (or the screenshot tests of 4.6). Each fix was worked out from the screen's own numbers (the review measured where things sit).

| Step | What | Status |
|---|---|---|
| 4.1 | Gear z-order per slot, one weapon per hand | **Done in code**: a fixed order by body part; head gear and a held item replace the class hat and weapon; body armor breathes. Not yet seen on a phone |
| 4.2 | Stage layout: choice tray, battle portrait during puzzles, shop keeper visible, feet anchored | **Partly**: choices in a bottom tray clear of the face, the foe a portrait during puzzles with both health bars in fixed HUD rows, a narrower shop counter with the keeper at the edge. Feet on a common ground line need per-picture foot positions (4.3) |
| 4.3 | Backdrop stage contract, floors out of the water, landmarks out of the puzzle zone | Planned (art redraw: a floor band, a landmark box and a 20:9 safe area for every scene, then metadata the app reads) |
| 4.4 | World map from data, aspect kept, fog for later places | **Partly**: stretched at most a quarter instead of 1.55 times. A true-shaped map overlaps nine pairs of places at the current marker size, so it needs the places moved and the painting redone, with roads painted from the same data |
| 4.5 | Painted icons replace emoji | **Partly**: the feedback bubble, loading book, stars-earned and gift icons are drawn in code. The magic bunny and frog (and the dice screen's broken heart) still need art |
| 4.6 | Screenshot tests at 16:9 and 20:9 | Planned (needs the Android SDK) |

## Phase 5: voice, music and sound

| Step | What | Status |
|---|---|---|
| 5.1 | Voice bible and a voice for every named character | **Done in code**: Kokoro v1.0 (54 speakers, 28 English) through sherpa-onnx 1.13.8 (the latest release), each of the 21 named voices on a speaker of its own (`VoiceCastTest`), creature kinds on a shared speaker with a different pitch and pace, captions by name, the Bat King's voice. Chosen by measurement, **not yet listened to** |
| 5.2 | Music and ambience, ducking under the narrator | Not started (needs a licence-checked source and a `Music` player) |
| 5.3 | Effects per item and terrain | **Partly**: three synthesized bells (checked by FFT) for the Bell Song; the rest not started |

## Phase 6: new minigames

Each needs a generator, a judge, a screen, a place in the world, tests, voice lines and art. The simulated share of each skill is in "What changed in the numbers".

| Step | Minigame | Status |
|---|---|---|
| 6.1 | Lucky Roll (dice) | **Done**: a road event (decision #63) |
| 6.2 | The Potion Workshop | **Done as a dungeon room**. The roadmap's potions that change the world (a glow potion lighting a swamp road) are not built |
| 6.3 | Hoot's Treasure Map | **Done**: a route on a grid, "go right two steps", north and south at the hard levels (`GridChallenge`, `TreasureMap` screen); an obstacle costume and a room at Hoot's map table (the old parchment backdrop). The seven-step ladder stops at following a route: "plan around an obstacle" is not built |
| 6.4 | Lumi's Lanterns (the writing ladder) | **Partly**: tracing now climbs lines, curves, zigzags, loops and shapes before letters (decision #66), in the tunnel room and as the Bat King's glowing path. The screen where the child draws a wire from lamp to lamp is not built (the lamp pictures are drawn and unused) |
| 6.5 | The Pennywhistle Market Stall | **Partly**: money puzzles (exact coins, coins left, the price of both) as obstacle costumes, a step on the dragon's peaceful way, and a market stall room with its own backdrop. A stall where several customers are served in a row is not built |
| 6.6 | Grumble's Rhyme Bridge | **Done as rhyme puzzles** (which picture rhymes; the odd one out; which starts like) at the river crossing and in a rope-bridge room with its own backdrop. Clapping syllables is not built |
| 6.7 | Sir Ribbit's Bell Song | **Done**: a listening skill, three bells, songs of two to five bells, judged in the engine; a bell-tower room (decision #67). Sir Ribbit himself does not appear in it |
| 6.8 | Fair Shares for the Bats | **Done**: sharing puzzles (berries for bats, pies on plates), a bat cave room with its own backdrop, and a step on the Bat King's peaceful way |
| 6.9 | Tell It Back | **Done**: at the end of a journey, remember the trip in order, from the child's own trail (`recap`) |

## Phase 7: new stories

| Step | Story | Status |
|---|---|---|
| 7.1 | The Night of a Thousand Lanterns | **Done as data** (decision #68): glow-silk from the Spider Caves, the Bat Belfry (a new lair, drawn on the map and as a backdrop), three ways to tell it, the Bell Song, a glowing path and shared berries on the peaceful way. The roadmap's lit-lantern map state is not built |
| 7.2 | The Dragon Who Didn't Want to Fight | **Done as data**: the stone key from Gloomwood Mine, Dragon's Peak, three ways to tell it, rhymes, sharing and money on the peaceful way, Brogan's courage. The roadmap's "sorting the hoard" is not built |
| 7.3 | Ruby's Quest | Not started: needs a second companion on stage and the mine's doors |
| 7.4 | Sparky Is Missing! | Not started: needs the engine to run a journey without the companion |
| 7.5 | The Ink Shadow and the Storybook Ball | **Done**: the finale (told when four pages are home, `Arc.finale`), reusing the Baron's lair and quill, three ways to tell it, a Baron who is a friend or a rival; the Ball brings up to five friends the world remembers on stage in a ballroom backdrop. The peaceful ending gives the shadow a page of its own; the fight rubs it back into the book |
| 7.6 | Grumble's Raft | Not started: needs a lake map |
| 7.7 | The Pennywhistle Race | Not started: a story with no boss needs a different ending in the engine |
| 7.8 | The Grey Knight | Not started: needs a lair of its own |

A story is checked by `ArcsTest`: wired to places and things that exist, played to its end both ways, remembered, a clue that spares a puzzle, a friend asks for help the next time, and the finale comes only when four pages are home.

## Phase 8: a space for grown-ups

| Step | What | Status |
|---|---|---|
| 8.1 | Parent gate, settings (second look, puzzle levels, the length of a day), progress in words | **Done** (decision #62; `SettingsTest`, `ProgressTest`) |
| 8.2 | Volume, export, the feedback tool behind the gate | Not started |

## Next five areas to investigate

| Area | What | Status |
|---|---|---|
| X1 | Device performance and APK size | Needs a person (real phones) |
| X2 | Watching the real player | Needs a person (playtest) |
| X3 | Licences, privacy, child safety (espeak-ng is GPL-3.0) | Needs a person |
| X4 | Curriculum review by an early-childhood educator | Needs a person |
| X5 | Accessibility and Spanish readiness | Planned after Phase 2 |
