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
| T1 | A journey takes about 25 minutes and can't be saved | 1.1 to 1.4 | Planned |
| T2 | Difficulty only ever goes up | 2.1 | Planned |
| T3 | The right answer is usually the middle card | 2.2 | Planned |
| T4 | There is one story | 7.1 and up | Planned (later phase) |
| T5 | The world's memory is mostly write-only | 3.1, 3.2 | Planned |
| T6 | Half of the brief's learning is absent | 2.3, then 6.x | Planned |
| T7 | Class powers and unlocks do nothing | 3.5, 3.6 | Planned |
| T8 | Two game engines in one codebase | 0.2 | Planned |
| T9 | Layering bugs on every screen type | 4.1 to 4.4 | Planned |
| T10 | Docs describe three different games | 0.1 | Planned |

## Phase 0: clean foundation

| Step | What | Status |
|---|---|---|
| 0.4 | Balance harness in the repo, `:engine:balanceReport`, CI summary | Done |
| 0.2 | Remove the legacy single-dungeon engine and its dead UI, migrate saves | Planned |
| 0.3 | `dealt` becomes a field of `Journey`; immutable content registry with id maps | Planned |
| 0.1 | Truth pass on the docs: README, architecture as built, decisions, plan banners, voice bible, issue template | Planned |

## Phase 1: never lose progress; shorter sessions

| Step | What | Status |
|---|---|---|
| 1.1 | Command log, autosave after every command, "Continue" on the title screen, determinism test | Planned |
| 1.2 | Challenge records appended as they happen | Planned |
| 1.3 | Back asks first | Planned |
| 1.4 | Shorter journeys with a camp-for-the-night stop; simulated median 14 minutes or less | Planned |

## Phase 2: learning integrity

| Step | What | Status |
|---|---|---|
| 2.1 | Honest failure records, windowed mastery, hero level affects monsters only, warm-up | Planned |
| 2.2 | Unbiased answer layouts | Planned |
| 2.3 | Skill quota: terrain picks the costume, the learner picks the skill | Planned |
| 2.4 | Teach after a miss; one-try miss lines; skip-count wording | Planned |
| 2.5 | Phonics: stop sounds without the vowel, short a, lowercase track, name letters first | Planned |
| 2.6 | NPC puzzles that fit their story | Planned |
| 2.7 | Color-blind support: a shape or mark with every hue | Planned |
| 2.8 | Judging moves into the engine (`ChallengeSession`) | Planned (later phase) |

## Phase 3: story and world continuity

| Step | What | Status |
|---|---|---|
| 3.1 | Flags that are written are read (validator rule); the Baron after peace and after a fight | Planned |
| 3.2 | Named characters are never random monsters | Planned |
| 3.3 | A peaceful way out of every conversation; tolls that gate something; "find another way" offers one | Planned |
| 3.4 | Kindness economy | Planned |
| 3.5 | Class powers in the journey | Planned |
| 3.6 | Deliver or drop every unlock | Planned |
| 3.7 | The dragon's name is spoken; "Sparky" never hard-coded | Planned |
| 3.8 | Clues once per world; greeting by state; Book One finale | Planned |

## Phase 4: presentation and art fixes

| Step | What | Status |
|---|---|---|
| 4.1 | Gear z-order per slot, one weapon per hand | Planned |
| 4.2 | Stage layout: choice tray, battle portrait during puzzles, shop keeper visible, feet anchored | Planned |
| 4.3 | Backdrop stage contract, floors out of the water, landmarks out of the puzzle zone | Planned (art redraw) |
| 4.4 | World map from data, aspect kept, fog for later places | Planned |
| 4.5 | Painted icons replace emoji | Planned |
| 4.6 | Screenshot tests at 16:9 and 20:9 | Planned (needs the Android SDK) |

## Later phases (not started on this branch)

| Phase | What | Status |
|---|---|---|
| 5 | Voice bible and unique voices, music and ambience, effects per item and terrain | Needs a person (voice choices, licences), then planned |
| 6 | Nine new minigames, one pull request each | Planned |
| 7 | Eight new story arcs and the Book One finale, one pull request each | Planned |
| 8 | Parent gate, settings, progress view | Planned |

## Next five areas to investigate

| Area | What | Status |
|---|---|---|
| X1 | Device performance and APK size | Needs a person (real phones) |
| X2 | Watching the real player | Needs a person (playtest) |
| X3 | Licences, privacy, child safety (espeak-ng is GPL-3.0) | Needs a person |
| X4 | Curriculum review by an early-childhood educator | Needs a person |
| X5 | Accessibility and Spanish readiness | Planned after Phase 2 |
