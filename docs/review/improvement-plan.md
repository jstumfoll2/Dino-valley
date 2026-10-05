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
| T4 | There is one story | 7.1 and up | Planned (later phase) |
| T5 | The world's memory is mostly write-only | 3.1, 3.2 | **Done** |
| T6 | Half of the brief's learning is absent | 2.3, then 6.x | **Partly**: the skills that exist are balanced; maps and recipes still need their minigames (Phase 6) |
| T7 | Class powers and unlocks do nothing | 3.5, 3.6 | **Done** |
| T8 | Two game engines in one codebase | 0.2 | **Done** |
| T9 | Layering bugs on every screen type | 4.1 to 4.4 | **Partly**: gear order, the dialog tray, the foe portrait and the shop counter are fixed in code; backdrops, ground line and a true map need new art (Phase 4) |
| T10 | Docs describe three different games | 0.1 | **Done** |

## What changed in the numbers

From `./gradlew :engine:balanceReport` (ten simulated children, ten journeys each, at 60%, 75% and 90% of puzzles right). "Before" is the review's
Appendix A, measured on `main` with the original harness; the harness was refined when it moved into the repository (it now uses the engine's own
time estimate), so the minutes are close but not exact comparisons. Everything in the "After" column is held by a test unless noted.

| Measure | Before (60% / 75% / 90%) | After (60% / 75% / 90%) |
|---|---|---|
| Median minutes in one sitting | 28.3 / 24.7 / 23.7 (the whole journey; no way to stop) | **10.0 / 9.7 / 9.7** (90th percentile 13.3 / 12.8 / 13.4) |
| Median minutes in a whole journey | 28.3 / 24.7 / 23.7 | 18.8 / 18.2 / 19.2, in two or three days |
| Mean level of counting, numbers, adding, colors, patterns and letters after ten journeys | 4.9 / n.a. / 5.0 | **1.2 / 1.7 / 3.0** |
| Counting level reached by a child who only guesses (40 one-try puzzles) | 3 | **1** |
| How often the right card is in the best-guess position (counting, level 4) | 100% (fair is 20%) | **21%** (fair is 20%) |
| Forced-fight menus per 100 journeys | 18 / 22 / 20 | **0** |
| Named characters met as random monsters per 100 journeys | 92 / 83 / 88 | **0** |
| Faints per journey | 0.91 / 0.42 / 0.13 | 0.15 / 0.09 / 0.01 |
| One-try misses that say "try again" | (not measured) | **0 of 696 / 387 / 152** |
| Share of puzzles: patterns, the biggest skill | 19% | 16% (every everyday skill is 11% to 16%) |

Not changed: tracing, memory, sorting and jigsaws are still 1% to 2% of puzzles each, and maps and recipes are 0%, because they need their minigames
(roadmap Phase 6). A journey is still about 19 minutes; what changed is that it comes in sittings of about ten. (Part of the drop since the first measurement is that a
monster on the way is now a choice, and the simulated child goes away half the time.)

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
| 3.8 | Clues once per world; greeting by state; Book One finale | **Partly**: greeting by state and a shorter second telling are done; clues once per world and the Book One finale are not |

Also fixed under Phase 3 (from the review's table of story and loop inconsistencies):

- The Mossy Golem was an ordinary roaming monster as well as the mine's guardian; it is now a mini-boss that stays in its mine.
- "A wolf pup blocks the way! What will you do?" started the fight whatever the child did. A monster on a road or in a wild place is now a real choice
  (fight, or go away), held by `WorldRulesTest`.
- Captions say "Professor Hoot:" and "Bandit Bess:", not "Elder:" and "Sneak:", when the scene knows who is speaking.

Still open from that table: one story only (row 1), the Book One finale (3), clues that repeat every adventure (4), Dragon's Peak, the Bat King and the old cast
without a role (16), time of day and scene state (17), peace and fight costing different things (18), coin sinks (19), and a journey that is mostly travel and
quizzes (20). Rows 1, 3, 16 and 20 are what Phases 6 and 7 are for.

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

## Later phases (not started on this branch)

| Phase | What | Status |
|---|---|---|
| 5 | Voice bible and unique voices, music and ambience, effects per item and terrain | **5.1 done in code**: Kokoro v1.0, a speaker of their own for each of the 21 named voices (`VoiceCastTest`), captions by name; chosen by measurement, not yet listened to. 5.2 music and 5.3 effects not started |
| 6 | Nine new minigames, one pull request each | **Started**: 6.1 Lucky Roll is a road event, and 6.2 the Potion Workshop is a dungeon room (recipes are now 1.4% to 1.7% of simulated puzzles, short of the 5% target: the world-changing potions of the roadmap are not built). The other seven are not started; each needs a generator, a judge, a screen and art |
| 7 | Eight new story arcs and the Book One finale, one pull request each | Not started: every arc needs new painted people and places and, for most, a new minigame, so none can be made as data alone |
| 8 | Parent gate, settings, progress view | **Started**: a gate (a two-digit sum), a second look after a miss, easiest and hardest puzzle level, the length of a day, and a progress view in words (`SettingsTest`, `ProgressTest`). Not yet: volume, export, moving the feedback tool behind the gate |

## Next five areas to investigate

| Area | What | Status |
|---|---|---|
| X1 | Device performance and APK size | Needs a person (real phones) |
| X2 | Watching the real player | Needs a person (playtest) |
| X3 | Licences, privacy, child safety (espeak-ng is GPL-3.0) | Needs a person |
| X4 | Curriculum review by an early-childhood educator | Needs a person |
| X5 | Accessibility and Spanish readiness | Planned after Phase 2 |
