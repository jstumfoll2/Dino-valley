# Project Decisions

A running log of meaningful architectural choices. Add a new entry whenever
someone might later ask "why did we do it this way?". Don't delete old
entries; if we change our minds, mark the old one **Superseded by #N** and add
a new one.

Entries 1–33 were made on 2026-10-02 together with
[`ARCHITECTURE.md`](ARCHITECTURE.md). Jason approved the architecture the
same day, so they are **Accepted**.

---

### 1. Two Gradle modules: `:app` (Android) and `:engine` (pure Kotlin)

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

**Decision:** Common template fields are shared; type-specific fields live in
an `"activity"` block whose `"type"` selects a Kotlin subclass
(`CountObjectsSpec`, `FindSymbolSpec`, `CompareGroupsSpec`) via
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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

**Decision:** Generation uses an injected `GameRandom`; each item stores its seed.

**Why we made it:** Any odd question seen in a playtest can be reproduced
exactly in a test.

**Alternatives considered:** `Random.Default` everywhere (unreproducible).

**What might cause us to change it:** Nothing likely.

---

### 10. Rules-based difficulty with first-try accuracy and hysteresis

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02 (introduced in Phase 2)

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

**Status:** Accepted · 2026-10-02

**Decision:** A one-row `app_settings` table.

**Why we made it:** Avoids a dependency for a handful of values.

**Alternatives considered:** DataStore Preferences (the official choice for
key-value settings); SharedPreferences (built-in but legacy).

**What might cause us to change it:** Settings needed before the database
is opened, or many settings.

---

### 14. No navigation library for the first prototype; Navigation Compose in Phase 2

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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
many?", encouragement) ship in the first prototype. Built-in audio APIs only.

**Why we made it:** A non-reader can't play without hearing instructions, so
basic audio can't wait for Phase 5. A familiar family voice is warmer than TTS
and works offline everywhere.

**Alternatives considered:** Android TextToSpeech (free, offline voices not
guaranteed, robotic for a 4-year-old); no audio until Phase 5.

**What might cause us to change it:** Needing far more lines than we can
record (then TTS for the long tail, recorded for the core).

---

### 17. No numeric score; progress and rewards are things

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

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

**Status:** Accepted · 2026-10-02

**Decision:** The first prototype builds one character, one background, one counting
activity, great animations and real voice lines, then a playtest. No map,
database, JSON, or difficulty engine yet, but the counting logic still lives
in `:engine`.

**Why we made it:** The MVP question is "will he voluntarily play another
round?". Answer that before building the complicated parts.

**Alternatives considered:** Building the full MVP skeleton first (risks a
correct but joyless app).

**What might cause us to change it:** Nothing; this is the plan for the first prototype.

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

**Status:** Accepted · 2026-10-02

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

---

### 24. Numbers and letters are the core learning objective; letters join the MVP

**Status:** Accepted · 2026-10-02 (Jason: "learning letters/numbers should be
a key learning objective")

**Decision:** Number recognition and letter recognition are the main
progression. Counting, comparing, shapes and colours support them. Letter
recognition moves from Phase 4 into Phase 2, and letter sounds follow in
Phase 3. Letter order is content: it starts with the letters of his name
(entered in parent mode, stored only on the phone) and keeps easily confused
letters (b/d/p/q, M/W, E/F) apart until high levels.

**Why we made it:** It's what you most want him to learn. The engine's
skill/template split means adding letters is content and one activity type,
not new architecture.

**Alternatives considered:** Following the spec's order (counting only in
the MVP, letters in Phase 4); alphabetical letter order (A–Z is arbitrary for
a 4-year-old, and his own name is far more motivating).

**What might cause us to change it:** Playtests showing letters are too
abstract without more counting/visual groundwork first.

---

### 25. One `FIND_SYMBOL` activity type for numerals and letters

**Status:** Accepted · 2026-10-02

**Decision:** Replace the counting-only `CHOOSE_NUMBER` with `FIND_SYMBOL`,
which has a `symbolSet` (numerals, uppercase, lowercase) and a `mode`
(find by name, by quantity, by sound, match case). Numbers and letters share
one generator, evaluator, hint ladder and composable.

**Why we made it:** "Tap the one I say" is the same game for 3 and for B.
One implementation means both get the same polish and the same bug fixes.

**Alternatives considered:** Separate `CHOOSE_NUMBER` and `CHOOSE_LETTER`
types (duplicated code that would drift apart).

**What might cause us to change it:** Letter activities needing interaction
that numbers don't (e.g. dragging pictures to letters); that becomes a new
activity type rather than a mode.

---

### 26. Built-in playtest feedback tool, saved on the phone and shared by hand

**Status:** Accepted · 2026-10-02 (Jason asked for an easy way to report bugs
during playtests)

**Decision:** A two-finger press and hold anywhere pauses the game and opens a
report sheet with one-tap tags (including "he loved this") and an optional voice note.
The report automatically saves a screenshot, the current state and seed, the
last ~100 events, a learner snapshot and device/volume info. Crashes are
captured the same way. Reports stay in app-private storage until an adult
shares them through Android's share sheet. A `playtest` GitHub issue
template turns them into issues. The first prototype has crash capture and a
minimal screenshot-and-state capture (plus the phone's screen recorder); the
full tool is the first item in Phase 2.

**Why we made it:** You can't take notes while watching a preschooler.
Seeds and event logs make "it did something weird" reproducible. Sharing by
hand keeps the no-INTERNET rule (#18) intact.

**Alternatives considered:** A crash/feedback SDK such as Firebase
Crashlytics (needs network, sends data to a third party); paper notes only
(miss the state needed to reproduce); a visible bug button (he'd tap it).

**What might cause us to change it:** Testers outside the family, which
would need an easier upload path and a privacy review first.

---

### 27. Claude implements; the family directs

**Status:** Accepted · 2026-10-02 (Jason: "you are going to be implementing
everything. We are only here for direction and guidance.")

**Decision:** Claude writes all code, tests, art (as vector drawings in code)
and build automation. A GitHub Actions workflow builds an installable APK on
every push, so nobody needs Android Studio to try a build. The family makes
the creative calls, records voice lines, and runs playtests.

**Why we made it:** That's how the family wants to work. Vector art drawn in
code is consistent, animatable in layers, tiny, and something Claude can
produce directly, which is why it replaces AI image generation for the
in-game art.

**Alternatives considered:** The family writes code with AI help (the
spec's original plan); AI-generated raster art (needs an image tool Claude
doesn't have here, and is hard to animate in layers).

**What might cause us to change it:** Wanting a hand-illustrated or
generated art style that vector shapes can't match; that art would be
dropped in as assets and the layered-parts approach kept.

---

### 28. Support Android 8 and up; target Android 16

**Status:** Accepted · 2026-10-02 (Jason's phone runs Android 15, his wife's
Android 16)

**Decision:** `minSdk 26`, `compileSdk` and `targetSdk 36` (Android 16). Both
family phones get every feature, including on-device voice answers (Android 12+).

**Why we made it:** Targeting the newest Android keeps the app installable
and F-Droid-friendly; a low minimum costs nothing for this app.

**Alternatives considered:** `minSdk 31` (simpler voice-input code, but
excludes older hand-me-down phones).

**What might cause us to change it:** A Jetpack library raising its minimum.

---

### 29. A play session is 5 rounds

**Status:** Accepted · 2026-10-02 (Jason)

**Decision:** After 5 rounds (about 10–15 minutes) the dino gets sleepy and
the session ends gently. Parents will be able to change it.

**Why we made it:** Healthy, natural stopping point without timers or
pressure.

**Alternatives considered:** No limit; a visible timer (pressure).

**What might cause us to change it:** Playtests showing he wants shorter or
longer sessions.

---

### 30. English now, Spanish later

**Status:** Accepted · 2026-10-02 (Jason: "English only. Future goal add in
basic Spanish.")

**Decision:** Ship English only. Keep every spoken and written line behind
prompt keys with per-language files (`prompts.en.json`, `audio/en/`), and
keep number words and letter sets per language, so Spanish is a content
addition (`prompts.es.json`, `audio/es/`, Spanish letter set including Ñ).

**Why we made it:** Decision #7 already separates words from activities, so
being ready for Spanish costs nothing now.

**Alternatives considered:** Hard-coding English strings (cheap now, painful
later).

**What might cause us to change it:** Nothing expected.

---

### 31. Built to be publishable on F-Droid

**Status:** Accepted · 2026-10-02 (Jason: "I want to post to F-Droid
eventually"); licence choice pending

**Decision:** Keep the app F-Droid-ready from the start: only free-software
dependencies (AndroidX, Kotlin), no Google Play Services, no tracking, no
network, reproducible build from source with the Gradle wrapper, an
F-Droid-style application id (`io.github.jstumfoll2.dinovalley`), and
store-listing metadata in `fastlane/metadata/android/` when we publish.
Before publishing we need a free-software licence for the code and a
Creative Commons licence for art and voice recordings.

**Why we made it:** Avoiding a proprietary dependency now is far easier
than removing one later.

**Alternatives considered:** Google Play (needs a developer account and
Families policy review); family-only sideloading forever.

**What might cause us to change it:** Deciding not to publish.

---

### 32. The game is called "Dino Valley" everywhere

**Status:** Accepted · 2026-10-02 (Jason renamed the repo to `Dino-valley`)

**Decision:** The app shows "Dino Valley" on the phone. The repo is
`Dino-valley`, the code package is `com.dinovalley`, and the install id is
`io.github.jstumfoll2.dinovalley`.

**Why we made it:** One name everywhere is easier to follow. The rename
happened before anyone installed the app, which is the last cheap moment to
change the install id: after that, a new id installs as a second app.

**Alternatives considered:** Keeping the old `dungeonquest` names inside
the code (confusing for no benefit).

**What might cause us to change it:** Your son naming the dino; the app's
display name can take it at any time, while the install id stays fixed.

---

### 33. Builds come from GitHub Actions, signed with a shared debug key

**Status:** Accepted · 2026-10-02

**Decision:** Every pull request builds and tests the app on GitHub
Actions. Every push to `main` replaces a "latest" pre-release with
`dino-valley.apk`, so the newest build is one tap away on the phone. Builds
are signed with a debug key kept in the repo, so each new build installs
over the old one without erasing progress.

**Why we made it:** Nobody in the family needs Android Studio. Claude's
own environment can't download the Android SDK, so CI is also where the app
is compiled and checked.

**Alternatives considered:** A random debug key per build (every update
would need an uninstall); a private release key (needed only for a store,
and F-Droid signs its own builds).

**What might cause us to change it:** Publishing outside F-Droid, which
would need a private release key kept out of the repo.

### 34. The game is a picture book with games baked into its pages

**Status:** Accepted · 2026-10-02

**Decision:** The game is now a storybook, "The Lost Eggs". A storm blows
Mama Dino's eggs across the valley and the child's dino finds them. There
are seven pages after the cover, each read aloud with its words printed like
a book. Most pages hide a small game: tap clouds to make thunder, count eggs
in the meadow, hop on the river stone with a numeral, find the bush with a
letter, light a dark cave and count, then hatch the eggs at home. The words
stay the same each read. The numbers and letters change every time, chosen
by the engine (`engine/story`).

**Why we made it:** After the first playtest, Jason said the counting game
was too simple and needed a strong story, "a mixture of a book with some
game elements baked in." A story gives every number and letter a reason to
matter: the egg is on the far side of the river.

**Alternatives considered:** A map of separate mini-games (no story to carry
him along); a fully branching story (much more to write and record, and
harder for a four-year-old to follow).

**What might cause us to change it:** He loses interest once he knows the
story. Then we add a second book with the same page machinery.

### 35. Art is drawn as code (SVG) and turned into WebP pictures for the app

**Status:** Accepted · 2026-10-02

**Decision:** Characters, props and backgrounds are written as SVG by
Python scripts in `art/src`. `art/build.py` renders them with a headless
browser and saves WebP files into `app/src/main/res/drawable-nodpi`.
Characters are cut into layers (tail, body, arm, head, eyes, mouths) so the
app can make them breathe, blink, talk, wag and hop.

**Why we made it:** It gives much more detail than shapes drawn in Kotlin
(shading, outlines, texture). It stays free and open, so F-Droid can rebuild
it, and anyone can change a color and re-run one command.

**Alternatives considered:** Drawing in Kotlin on a Canvas (what the first
prototype did, too plain); hand-painted bitmaps (nobody in the project
paints, and they can't be regenerated); AI-generated art (licensing is
unclear for F-Droid and it is hard to keep consistent).

**What might cause us to change it:** An illustrator joins in, or the APK
grows too big. Right now all the art is under 1 MB.

### 36. The child names the dino with their own voice

**Status:** Superseded by #46 · 2026-10-02

**Decision:** The cover has a big microphone button. While it is held, the
phone records, and the dino tilts its head to listen. Then the dino says
"My name is …" using the child's own recording, and the story uses that
clip wherever the dino's name comes up. The clip is stored only in the
app's private storage. Without a recording, the dino is called "Rex". The
microphone permission is asked for the first time the button is held.

**Why we made it:** Jason asked for it. Naming the dino makes it his. He
can't type or read, so his voice is the natural way to do it.

**Alternatives considered:** Picking a name from pictures (not really his
name); speech recognition to turn the name into text (it needs Google's
online service on most phones, which breaks the no-network rule).

**What might cause us to change it:** Two children want two dinos, which
means a dino per profile.

### 37. Each read of the story adjusts difficulty with one simple rule

**Status:** Accepted · 2026-10-02

**Decision:** The story has five levels (`StoryLevels`). Each level sets the
counting range, the highest stone numeral, which letters appear and how many
choices each question has. The rule is checked at "The End". If every
question was right on the first try, the next read is one level harder. If
two or more needed help, it is one level easier. Otherwise it stays the
same. The level is saved on the phone.

**Why we made it:** It's easy to explain, and it keeps him near the edge of
what he knows without any tracking beyond the phone. It replaces the fixed
level 2 used by the first prototype.

**Alternatives considered:** Per-skill mastery tracking from the
architecture (planned for later, once there is more than one book);
difficulty chosen by a parent (more work for the parents).

**What might cause us to change it:** Numbers and letters move at different
speeds for him. Then each gets its own level.

### 38. The game becomes The Little Dungeon

**Status:** Accepted · 2026-10-02

**Decision:** We replace the Lost Eggs storybook with The Little Dungeon, the
original fantasy RPG from the family's brief (`docs/design/little-dungeon-brief.md`).
Learning is how you get through the world: patterns open rune doors, counting
fixes bridges, colors find crystals, letters power spells, tracing lights paths,
and recipes brew potions. The first build is the brief's vertical slice. The
app keeps its install id, so it updates over Dino Valley.

**Why we made it:** Jason found the storybook too simple and asked for a
strong story with real game elements. His wife designed the brief.

**Alternatives considered:** Growing the dino storybook into more books (still
linear, and less to come back for).

**What might cause us to change it:** He finds the dungeon scary or confusing in
playtests.

### 39. Every adventure is generated, and choices change it

**Status:** Accepted · 2026-10-02

**Decision:** Each run is made from a seed. The dungeon map, which rooms are
behind which doors, the storyline (one of the brief's three), its twist, names,
treasure and funny events all change from run to run. The rooms lean toward the
skills he needs to practice. Choices matter inside a run (doors, how to treat the
goblin, friend or spell at the boss) and between runs, because the world remembers
friends, endings and treasure. Nothing is ever lost and there is no game over:
every room ends in success, and dice results of 1–2 are silly rather than bad.

**Why we made it:** Jason asked for randomness, roguelike elements and choices
that matter, so the game stays fresh for repeated play.

**Alternatives considered:** Hand-written levels (they run out); real roguelike
permadeath and losing items (too harsh for a four-year-old).

**What might cause us to change it:** He wants the same favorite adventure again.
A "play that one again" button could replay a seed.

### 40. Stars, levels and unlocks for the hero

**Status:** Accepted · 2026-10-02

**Decision:** Every challenge, roll and kind choice earns stars (XP) in one of
the brief's five attributes. Stars add up to hero levels (180, 420, 720, 1080, …),
and levels unlock a feather hat, the Guardian class, a star cape, the
Spellkeeper class and later rewards. Each class has a power: the Knight rerolls
a low die once, the Wizard gets hints one step sooner, the Ranger peeks behind
doors, and the Guardian makes friends easily. Levels are never shown as grades.

**Why we made it:** Jason asked for experience and level-ups as gamification.
Visible things to earn give a reason to play again.

**Alternatives considered:** Collectible stickers only (no sense of growing
stronger).

**What might cause us to change it:** Rewards start to matter more to him than
the adventure, or later unlocks feel too slow.

### 41. His named dino becomes a baby dragon companion

**Status:** Accepted · 2026-10-02

**Decision:** The child's companion is a baby dragon that comes on every
adventure. The name he recorded for the dino carries over; without a recording
it is called "Sparky".

**Why we made it:** Jason chose "Baby dragon" when asked how to keep the dino
in the new fantasy world.

**Alternatives considered:** Keeping the T. rex as is; dropping the companion.

**What might cause us to change it:** He misses the T. rex. It could come back
as a second companion.

### 42. Progress and every challenge are saved on the phone

**Status:** Accepted · 2026-10-02

**Decision:** The hero, skill levels and world memory are saved to
`save.json` in the app's private storage when an adventure ends. Every challenge
is appended to `challenges.jsonl` with its skill, level, tries, hints, time and
seed. That log is for tuning and for a possible small on-device model later.
Nothing leaves the phone.

**Why we made it:** The brief asks for adaptive learning that is ready for a
model later, and the no-network rule stays.

**Alternatives considered:** A database (Room is more setup than a few small
files need right now).

**What might cause us to change it:** The log grows large, or a parent view needs
queries. Then it moves to Room.

### 43. The narrator is Kokoro, a natural voice that runs on the phone

**Status:** Accepted · 2026-10-02

**Decision:** The narrator speaks with Kokoro (the int8 English model, voice
"af_bella"), run on the phone through sherpa-onnx. Both are Apache-2.0, so they
stay F-Droid friendly. They are too big for git, so CI downloads them
(`scripts/fetch-voice.sh`) and packs them into the app, which grows to about
120 MB and runs on 64-bit ARM phones. Sentences are made one at a time while the
previous one plays, and recent ones are kept. The phone's own text-to-speech
takes over if the model can't load. The child's recording of the dragon's name
is trimmed of silence and brought to the same loudness as the narrator.

**Why we made it:** After the first playtest Jason found the phone's voice too
robotic and chose Kokoro over a smaller voice (Piper) or tuning the phone's
voice.

**Alternatives considered:** Piper (smaller and faster, less natural); the
phone's voice tuned; recorded family lines with a computer voice for the
changing parts (still a good later layer).

**What might cause us to change it:** Pauses before speech are too long on his
phone, or the download size becomes a problem.

### 44. Two dice to add, and hearts

**Status:** Accepted · 2026-10-02

**Decision:** Every roll uses two dice, and the child adds them up before the
roll counts. A wrong first answer pops one of three hearts, and the narrator
counts the dots together with him, lighting each one, before he tries again.
Hearts never run out: when the last one pops, the baby dragon shares a magic
berry and they all come back. Hearts left at the end earn bonus stars. At every
fork a clue says where treasure is; any door can be taken, but only the clue's
door has the treasure. After each room behind a door, a chest with a magic lock
asks for a number or a letter. The map is shown at the start and above every fork.

**Why we made it:** Jason's playtest notes: not enough learning, roll two dice
and count the total with something at stake, a real map, and door colors said
out loud.

**Alternatives considered:** Losing a heart for every wrong answer (too harsh);
a wrong total sending the hero the wrong way (kept for the map clue instead).

**What might cause us to change it:** Hearts make him anxious rather than
careful.

### 45. Every narrator sentence is recorded when the app is built

**Status:** Accepted · 2026-10-02 · refines #43

**Decision:** The engine can list every sentence the game might say
(`./gradlew :engine:voiceLines`, which plays thousands of simulated adventures
and adds every screen phrase). CI speaks each one with the same Kokoro voice and
packs them into the app as small Ogg Opus files named by a fingerprint of the
voice and the sentence (`scripts/render-voice.py`, cached between builds). The
phone plays those files. Kokoro on the phone only makes a sentence that is
missing (for example one with a typed dragon name), and keeps it. All sound goes
through one continuous audio stream, so endings are not clipped, and mouths move
with how loud the voice is at each moment. While the current scene plays, the
next scenes' sentences are loaded ahead; if a sentence still needs making, a
loading bubble shows.

**Why we made it:** Playtest three: speech started late and was cut off between
scenes, mouths moved with no words, and the phone got hot. Making speech on the
phone was the cause.

**Alternatives considered:** Faster phone settings for Kokoro (still hot and
slow); a smaller voice (less natural).

**What might cause us to change it:** The app grows too big, or the build takes
too long.

### 46. A grown-up types the dragon's name; the narrator says it

**Status:** Accepted · 2026-10-02 · supersedes #36

**Decision:** The child's recording of the name is gone. The dragon is "Sparky"
until a grown-up taps the pencil on the name tag on the cover and types a name.
The narrator says it inside its own sentences. The microphone permission is no
longer asked.

**Why we made it:** Jason found the switch between the recorded name and the
narrator's voice too jarring.

**What might cause us to change it:** He wants his own voice back in the game
in some other form.

### 47. Sound effects instead of spoken noises

**Status:** Accepted · 2026-10-02

**Decision:** Story noises ("creak", "splash", "ribbit") are sound effects,
written in story text as `[creak]` and played in place. Taps, right and wrong
answers, dice and hearts have sounds too. The files are CC0 (Kenney and Ninja
Adventure packs) or made by `scripts/make-sfx.py`; sources are in
`docs/SOUND_CREDITS.md`. A wrong answer plays a soft "uh-oh", shows a red cross
on that choice, and blocks taps until the hint has been said; tapping many
answers quickly makes the narrator ask him to slow down and look.

**Why we made it:** Jason asked for real sound effects instead of the voice
saying "blub blub", and for clearer feedback because his son tapped everything
until something was right.

**What might cause us to change it:** A sound is scary or annoying to him.

### 48. More kinds of learning, and doors choose the game

**Status:** Accepted · 2026-10-02

**Decision:** New rooms: a storeroom where things are dragged into baskets by
color, kind or size; a frog pond for counting by 2s, 5s, 3s and 10s; a mosaic
hall with a jigsaw puzzle; tracing real letters and numbers stroke by stroke in
the tunnel. Other rooms now also ask to find the written number, letter or color
after counting or matching. At each fork, every door's sign shows and the
narrator says which game is behind it, so choosing a path means choosing the
kind of puzzle.

**Why we made it:** Jason asked for sorting, colors, writing letters, picking
written numbers and letters, skip counting and puzzles, and for path choices to
pick the puzzle type.

**What might cause us to change it:** He always picks the same game, and some
skills never get practiced.
