# Project Decisions

A running log of meaningful architectural choices. Add a new entry whenever
someone might later ask "why did we do it this way?". Don't delete old
entries; if we change our minds, mark the old one **Superseded by #N** and add
a new one.

Entries 1–23 were proposed on 2026-10-02 together with
[`ARCHITECTURE.md`](ARCHITECTURE.md) and are **Proposed** until you both
agree. Change the status to **Accepted** (or edit them) as you review.

---

### 1. Two Gradle modules: `:app` (Android) and `:engine` (pure Kotlin)

**Status:** Proposed · 2026-10-02

**Decision:** All game rules, content models, generators, evaluators,
difficulty and activity selection live in a plain Kotlin/JVM module with no
Android dependency. The Android app module holds UI, audio, and storage.

**Why we made it:** The spec's key rule is "don't hard-code educational logic
into UI screens"; a module that can't import Android makes the compiler
enforce it. Engine tests run on the JVM in seconds, which makes tuning
difficulty rules pleasant.

**Alternatives considered:** A single module with packages (simpler, but the
boundary is only a convention). A many-module "feature module" layout
(standard for big teams, too much ceremony for two people).

**What might cause us to change it:** One module becoming hard to navigate
(split further), or the engine needing Android APIs directly (it shouldn't;
pass them in through interfaces instead).

---

### 2. Manual dependency injection with one `AppContainer`

**Status:** Proposed · 2026-10-02

**Decision:** Wire objects by hand in an `AppContainer` created by the
`Application` class. No Hilt or Dagger.

**Why we made it:** Fewer than ~20 objects to wire. Plain constructors are
easier to read, debug, and learn from than annotation processing.

**Alternatives considered:** Hilt (official, but adds annotations, a Gradle
plugin, and generated code); Koin (third-party runtime DI).

**What might cause us to change it:** The container growing past a page, or
many screens needing scoped objects.

---

### 3. Unidirectional data flow; the round is a pure state machine

**Status:** Proposed · 2026-10-02

**Decision:** Composables send events to a ViewModel, which forwards them to
`GameSession` (in `:engine`). `GameSession` moves between explicit states
(`Introducing`, `AwaitingAnswer`, `Helping`, `Celebrating`, `RoundComplete`)
and the UI renders the current state from a `StateFlow`. The UI tells the
engine when animations finish.

**Why we made it:** Correctness decisions stay out of UI; a whole round can be
unit-tested without a phone; animation timing stays the designer's call.
The state list doubles as the storyboard of a round.

**Alternatives considered:** Logic inside ViewModels (harder to test, blurs
the boundary); a game loop/engine (overkill for turn-based activities).

**What might cause us to change it:** Real-time activities (e.g. catch the
falling eggs) that need a frame loop; those would get their own small loop
inside one composable.

---

### 4. Separate Skill, ActivityTemplate, and ActivityInstance; difficulty is per skill

**Status:** Proposed · 2026-10-02

**Decision:** A *skill* is what is learned ("counting"), with a level 1–5 per
child. A *template* is a reusable, themed way to practise it ("count Mama
Dino's eggs") and defines what each of the 5 levels looks like. An *instance*
is one generated question.

**Why we made it:** Matches spec §9–§10: one concept in many contexts, and a
few templates create many questions. Keeping levels inside a template means a
level change doesn't jump to an unrelated scene.

**Alternatives considered:** One template per difficulty (the spec's JSON
example): more files, and progression of one idea gets scattered.

**What might cause us to change it:** Activities whose levels differ so much
that they are really different templates.

---

### 5. Typed activity parameters via a sealed hierarchy and a `"type"` field

**Status:** Proposed · 2026-10-02

**Decision:** Common template fields are shared; type-specific fields live in
an `"activity"` block whose `"type"` selects a Kotlin subclass
(`CountObjectsSpec`, `ChooseNumberSpec`, `CompareGroupsSpec`) via
kotlinx.serialization polymorphism.

**Why we made it:** Typos and missing fields fail at load/test time, not
mid-game. Adding an activity type doesn't touch existing ones. The compiler's
exhaustive `when` lists every place a new type needs handling.

**Alternatives considered:** A loose `Map<String, Any>`/`JsonObject` of
parameters (flexible but errors appear at runtime); one flat class with every
possible field (gets messy fast).

**What might cause us to change it:** Wanting to add activity types from
content alone without shipping code (unlikely; new types need new UI anyway).

---

### 6. Content is a bundled JSON pack, validated by a unit test

**Status:** Proposed · 2026-10-02

**Decision:** Skills, templates, areas, prompts and rewards live as JSON in
`app/src/main/assets/content/`. `ContentValidator` checks references, ranges,
level coverage and the no-reading rule; a test runs it on the real files.
A JSON Schema file is kept only as documentation for AI prompts.

**Why we made it:** Content can grow without engine changes; AI-drafted
content must pass the validator and a human diff review before it ships;
fully offline.

**Alternatives considered:** Content in Kotlin code (no review boundary for
AI output); content in the Room database (harder to review/diff); a runtime
JSON Schema library (extra dependency for checks Kotlin can do).

**What might cause us to change it:** Wanting content updates without app
updates (would require downloads, which conflicts with #18).

---

### 7. Words and sounds are referenced by keys, not embedded

**Status:** Proposed · 2026-10-02

**Decision:** Templates reference `PromptKey`s like `count.how_many`. A
per-language `prompts.<lang>.json` maps keys to text and an audio clip.

**Why we made it:** Spec §23: text, audio and visuals are separate from the
activity. Recording narration or adding a language never touches activity
data.

**Alternatives considered:** Inline text in each template (simpler for one
language, painful for audio and translation); Android `strings.xml` (not
reachable from `:engine`, and splits content across two systems).

**What might cause us to change it:** Nothing likely.

---

### 8. Generators produce the full scene layout

**Status:** Proposed · 2026-10-02

**Decision:** A generator outputs every object's sprite, position (normalized
0–1), scale, rotation and group. The UI only draws it.

**Why we made it:** "4 eggs" is guaranteed and testable in the engine;
arrangement difficulty (line → scatter → clusters) is an engine concern; the
same scene renders on phone or tablet.

**Alternatives considered:** UI decides placement (untestable, logic leaks
into UI).

**What might cause us to change it:** Physics-style or freely draggable
scenes, where the UI owns positions after the start.

---

### 9. All randomness is seeded and the seed is stored

**Status:** Proposed · 2026-10-02

**Decision:** Generation uses an injected `GameRandom`; each item stores its seed.

**Why we made it:** Any odd question seen in a playtest can be reproduced
exactly in a test.

**Alternatives considered:** `Random.Default` everywhere (unreproducible).

**What might cause us to change it:** Nothing likely.

---

### 10. Rules-based difficulty with first-try accuracy and hysteresis

**Status:** Proposed · 2026-10-02

**Decision:** Per skill, look at the last 8 first-try results at the current
level. Up one level when ≥ 85% with at least 5 items and the last 3 correct.
Down one level when < 60% with at least 5 items, or immediately after 3
needed-help items in a row. Reset the window after any change. Response time
is recorded but not used.

**Why we made it:** Transparent, tunable, and matches spec §8. Requiring
several successes avoids sudden jumps; the struggle rule puts frustration
relief first. Preschool response times are too noisy to trust yet.

**Alternatives considered:** Elo/Bayesian knowledge tracing (more accurate
eventually, much harder to explain and tune); ML (explicitly deferred).

**What might cause us to change it:** Playtests showing levels change too
slowly/quickly (tune numbers in `DifficultyRules`), or enough logged data to
justify a model (#11).

---

### 11. `LearnerModel` interface is the seam for future ML

**Status:** Proposed · 2026-10-02

**Decision:** Activity selection and learner updates go through
`LearnerModel`. `RuleBasedLearnerModel` is the only implementation now; an
`OnDeviceMlLearnerModel` in a future optional `:ml` module can replace it.

**Why we made it:** Spec §18/§20: the app must not care which brain runs.

**Alternatives considered:** Separate interfaces for selection and difficulty
everywhere (more seams than we need today).

**What might cause us to change it:** An ML experiment that only wants to
replace one part; we'd then split the interface.

---

### 12. Room with an append-only outcome log as the source of truth

**Status:** Proposed · 2026-10-02 (introduced in Phase 2)

**Decision:** Store every finished question in `item_outcome`; `skill_state`
is a cache rebuildable by replaying the log. All rows carry `profileId` and
`contentVersion`.

**Why we made it:** Rules will change and history can be re-scored; parent
mode is queries over the log; future ML training data is already there.
`profileId` now avoids a migration if a sibling ever plays.

**Alternatives considered:** Only storing aggregates (small, but loses
history); raw SQLite (more boilerplate); SQLDelight (fine, but third-party).

**What might cause us to change it:** Nothing expected at this scale.

---

### 13. Settings in a Room table, not DataStore

**Status:** Proposed · 2026-10-02

**Decision:** A one-row `app_settings` table.

**Why we made it:** Avoids a dependency for a handful of values.

**Alternatives considered:** DataStore Preferences (the official choice for
key-value settings); SharedPreferences (built-in but legacy).

**What might cause us to change it:** Settings needed before the database
is opened, or many settings.

---

### 14. No navigation library for the weekend; Navigation Compose in Phase 2

**Status:** Proposed · 2026-10-02

**Decision:** The prototype switches between 2 screens with a `when`.
Adopt Navigation Compose once there are map, play, reward, sticker book and
parent screens.

**Why we made it:** Nothing to navigate yet; the official library pays off
once back stacks and per-screen ViewModels matter.

**Alternatives considered:** Navigation Compose from day 1; a hand-rolled
back stack forever.

**What might cause us to change it:** Nothing; this is a timing decision.

---

### 15. Animation with Compose's built-in APIs and a layered character

**Status:** Proposed · 2026-10-02

**Decision:** Springs, transitions, `graphicsLayer` and `Canvas`. The
character is a few layered parts (body, head, eyes, mouths) animated in code.

**Why we made it:** No dependency; one drawing set produces blinks, bounces,
talking and moods; small APK.

**Alternatives considered:** Lottie (great with After Effects, but a
dependency and a separate tool chain); sprite sheets (big, every pose drawn
by hand); a game engine (way beyond the need).

**What might cause us to change it:** The designer wanting to author complex
animations in a dedicated tool.

---

### 16. Recorded voice lines from day one; SoundPool/MediaPlayer for audio

**Status:** Accepted · 2026-10-02 (required by #22: he can't read)

**Decision:** A handful of recorded lines (numbers, "Let's count!", "How
many?", encouragement) ship in the weekend prototype. Built-in audio APIs only.

**Why we made it:** A non-reader can't play without hearing instructions, so
basic audio can't wait for Phase 5. A familiar family voice is warmer than TTS
and works offline everywhere.

**Alternatives considered:** Android TextToSpeech (free, offline voices not
guaranteed, robotic for a 4-year-old); no audio until Phase 5.

**What might cause us to change it:** Needing far more lines than we can
record (then TTS for the long tail, recorded for the core).

---

### 17. No numeric score; progress and rewards are things

**Status:** Proposed · 2026-10-02

**Decision:** A round shows 5 nest spots filling up; rewards are hatching
baby dinos, stickers and fossils. No points, no lives, no daily streaks.

**Why we made it:** Spec §14 (no pressure, no streak anxiety); "basic
scoring" in Phase 1 is reinterpreted as visible progress.

**Alternatives considered:** Stars per question (fine, but tends to read as
grading); points.

**What might cause us to change it:** Playtests showing he wants something
to count up (we could count collected dinos, which is also counting practice).

---

### 18. No INTERNET permission; no cloud backup of learning data

**Status:** Proposed · 2026-10-02

**Decision:** The manifest doesn't request INTERNET. Learning data is
excluded from cloud backup but allowed in device-to-device transfer.

**Why we made it:** Offline and private are enforced by Android, not just by
our discipline. No library we add can phone home.

**Alternatives considered:** Requesting INTERNET "just in case" (invites
accidental data flows).

**What might cause us to change it:** A future optional online feature; that
would need an explicit privacy review first (spec §21).

---

### 19. Hints escalate; every question ends in success

**Status:** Proposed · 2026-10-02

**Decision:** Wrong answer → "let's look again" → app counts together with
highlights → only two choices remain with the right one glowing. The item
ends correctly and is logged as "needed help".

**Why we made it:** "Let's figure it out together" (spec §5) as a mechanic;
no failure states, while still producing an honest signal for difficulty.

**Alternatives considered:** Unlimited retries (can frustrate); revealing the
answer (passive).

**What might cause us to change it:** Playtests showing he taps randomly to
reach the glowing answer; then the last step would ask him to count along
before choices appear.

---

### 20. Landscape, phone-first, normalized layout

**Status:** Accepted · 2026-10-02 (Jason confirmed he plays on a phone)

**Decision:** Lock landscape; design for a phone; positions are relative so
tablets scale up.

**Why we made it:** Two side-by-side groups and a character fit landscape
naturally; one orientation halves layout work.

**Alternatives considered:** Portrait; supporting both.

**What might cause us to change it:** His main device being a tablet held in
portrait.

---

### 21. First prototype optimizes delight, not completeness

**Status:** Proposed · 2026-10-02

**Decision:** Weekend 1 builds one character, one background, one counting
activity, great animations and real voice lines, then a playtest. No map,
database, JSON, or difficulty engine yet, but the counting logic still lives
in `:engine`.

**Why we made it:** The MVP question is "will he voluntarily play another
round?". Answer that before building the complicated parts.

**Alternatives considered:** Building the full MVP skeleton first (risks a
correct but joyless app).

**What might cause us to change it:** Nothing; this is the plan for weekend 1.

---

### 22. The no-reading rule: every idea comes across by voice or visuals

**Status:** Accepted · 2026-10-02 (Jason: "he can't read; the game needs to be
voice input/output or visually simple enough to get the ideas across")

**Decision:** Core gameplay never depends on reading. Every prompt is spoken
and must have an audio clip (the content validator fails otherwise); tapping
the character repeats the instruction; activities are demonstrated before
they're asked; hints are visual; answer buttons show dots alongside numerals
at low levels; navigation uses pictures, not words. The only symbols on
screen are the ones being taught.

**Why we made it:** Our player is four and can't read. Instructions he can't
understand turn a game into a guessing exercise and frustration.

**Alternatives considered:** Text with optional audio (fails our actual
player); relying on Android TTS for all speech (robotic, offline voices not
guaranteed).

**What might cause us to change it:** Literacy activities in Phase 4 will put
letters on screen on purpose, as the thing being taught. Instructions stay
spoken.

---

### 23. Voice input is optional, on-device only, and arrives after the prototype

**Status:** Proposed · 2026-10-02

**Decision:** Let him answer out loud ("three!") using Android's on-device
`SpeechRecognizer` (Android 12+), mapped to the same `ChildResponse` a tap
produces. Start with numbers only, as a Phase 2–3 experiment, switched on by
a parent. Tapping always works. Audio is never recorded or stored, nothing
leaves the phone, and the app still has no INTERNET permission. The dino
repeats what it heard, and a mis-recognition never counts as a wrong answer.

**Why we made it:** Speaking is natural for a non-reader. The engine already
treats input as an abstract response, so voice is an `:app`-only addition.
Recognisers are unreliable on young children's speech, so voice must
supplement taps, not replace them.

**Alternatives considered:** Voice as the primary input (too error-prone for
a 4-year-old's speech); cloud speech APIs (more accurate, but sends a child's
voice to a server, violating our privacy rules); a custom on-device model
(Phase 7 territory).

**What might cause us to change it:** Playtests showing recognition works
well for him (make it more prominent) or badly (shelve it), or the phone
lacking on-device recognition (hide the feature on that device).
