# Architecture Proposal

Status: **Proposed, awaiting your review.** Nothing here is built yet. Every
meaningful choice below is also recorded in [`PROJECT_DECISIONS.md`](PROJECT_DECISIONS.md)
with alternatives and "what would make us change it".

This document answers the twelve questions in section 30 of the spec. It is
written for the family directing the project. **Claude implements
everything** (code, art, tests, builds); you two set direction, make the
creative calls, record voices, and run playtests with your son. Sections
marked **For the experience designer** are where the creative decisions live.

---

## The one-paragraph version

One Android app with two Gradle modules. The `:engine` module is plain Kotlin
with no Android in it: it knows about skills, activity templates, generating
questions, judging answers, hints, and difficulty. The `:app` module is
everything a child sees and hears (Compose screens, animation, sound) plus
storage (Room). Educational content is a bundled JSON "content pack" that a
test validates before it ships. The child's progress is an append-only log of
answered questions; everything else (skill levels, parent summaries, future ML
training data) is derived from that log. The app declares **no INTERNET
permission**, so "offline and private" is enforced by Android itself, not by
good intentions.

```text
┌───────────────────────────── :app (Android) ─────────────────────────────┐
│                                                                           │
│  Compose UI ── screens, character, animation, sound                       │
│       │  events (tap, answer)          ▲ UiState (StateFlow)               │
│       ▼                                │                                   │
│  ViewModels ── thin: translate UI events ⇄ engine calls                   │
│       │                                                                   │
│  Data ── Room (learner log), AssetContentSource (JSON), SpriteCatalog,    │
│          SoundPlayer                                                      │
└───────┼───────────────────────────────────────────────────────────────────┘
        │ depends on (never the other way)
┌───────▼──────────────────────── :engine (pure Kotlin) ───────────────────┐
│  GameSession (state machine for one round)                                │
│     ├── LearnerModel  ── chooses next activity + level   (rules now, ML later)
│     ├── Generators    ── template + level + seed → concrete question      │
│     ├── Evaluators    ── answer → correct? which hint next?               │
│     └── DifficultyPolicy ── outcome → updated SkillState                  │
│  Content model + parser + validator                                       │
└───────────────────────────────────────────────────────────────────────────┘

Future: :ml module implementing LearnerModel (and later, drawing/voice
recognisers) — optional, swappable, never required to play.
```

---

## Learning goals: numbers and letters come first

**Recognising numbers and letters is the core learning objective.** Counting,
comparing, shapes and colours support it, but the progression we care most
about is:

| | Numbers | Letters |
|---|---|---|
| Recognise | "Find the 3" among numeral tiles | "Find the B" among letter tiles |
| Connect to meaning | Match a numeral to a quantity (3 ↔ three eggs) | Match a letter to its sound and a picture (B ↔ /b/ ↔ ball) |
| Pair forms | Numeral ↔ number word, spoken | Uppercase ↔ lowercase (B ↔ b) |
| Make it (later) | Trace the numeral with a finger | Trace the letter with a finger |

What this changes in the design:

- **One activity type serves both.** `FIND_SYMBOL` ("tap the one I say")
  works for numerals and letters alike; only the symbol set, the audio, and
  the helper picture differ (section 3). Numbers and letters get the same
  engine, difficulty rules and hint ladder for the price of one.
- **Letters move into the MVP.** The spec put letters in Phase 4. They now
  arrive in Phase 2, right after the content pack exists, as a
  `letter_recognition` skill. `letter_sounds` follows in Phase 3.
- **The first prototype stays counting**, because counting eggs is the
  most delightful first toy and its answer buttons already teach numerals
  1–5.
- **Letter order is content, not code.** Start with the letters in his own
  name (set in parent mode, stored only on the phone), then visually distinct
  uppercase letters. Easily confused letters (b/d/p/q, M/W, E/F) are kept
  apart until the higher levels.
- **Sounds are recorded with care.** Letter sounds are recorded as clean
  sounds ("/b/", not "buh"), which is a job for the experience designer and
  is listed in the art/audio bible.
- **Tracing** (drawing a numeral or letter with a finger) is a Phase 3
  activity checked by simple geometry against a stroke template: no ML. The
  spec's Phase 7 drawing recognition can build on it later.

---

## The no-reading rule (applies to everything below)

He can't read, so **nothing in the game may depend on reading**. Every idea
has to come across through voice, pictures, and motion. This is a hard rule,
not a preference, and the architecture enforces it in four places:

1. **Every instruction is spoken.** Each `PromptKey` must have a recorded
   audio clip, and `ContentValidator` fails the build if one doesn't.
   On-screen text is optional decoration for parents.
2. **Tap the character to hear it again.** The dino is always the "repeat"
   button. A child who wasn't listening is never stuck.
3. **Show, don't tell.** The first time he meets an activity, and after
   every level change, the character demonstrates it (counts "1… 2… 3!"
   while the eggs light up) before asking anything (`Introducing` state,
   section 5). Answer buttons show **dots as well as numerals** at low levels,
   and hints are visual: highlights, glowing choices, counting numbers
   floating over objects.
4. **The only symbols on screen are things being taught** (numerals,
   later letters and shapes). Buttons are pictures: a big egg for "play
   again", the dino's house for "home". `requiresReading: true` is rejected
   by the validator for every template in the MVP.

### Voice input (answering out loud)

Speaking answers ("three!") is a natural fit for a non-reader, and the
engine is already ready for it: the session receives a `ChildResponse` and
doesn't care whether it came from a tap or a voice.

```text
Microphone ──▶ VoiceAnswerListener (:app) ──"three"──▶ NumberWords (en) ──▶ ChildResponse.NumberChosen(3)
Tap        ──────────────────────────────────────────────────────────────▶ ChildResponse.NumberChosen(3)
```

- **On-device only.** Android's built-in `SpeechRecognizer` can run fully on
  the phone (`createOnDeviceSpeechRecognizer`, Android 12+), so no audio
  leaves the device and the app still needs no INTERNET permission. Audio is
  never recorded or stored; only the recognised answer is used.
- **Optional, never required.** Recognisers are much less accurate on a
  4-year-old's voice than an adult's. Tapping always works, and voice is a
  second way to answer, switched on in parent settings.
- **Listening is visible.** When the mic is on, the dino cups its ear; when
  it hears something, it repeats what it heard ("Three? Let's check!") so a
  misheard word is never a silent wrong answer. A mis-recognition doesn't
  count as a wrong first try.
- **When:** after the first prototype, as an experiment in Phase 2–3,
  starting with number answers only (a tiny vocabulary is the easiest case to
  recognise). It needs the `RECORD_AUDIO` permission, which a parent grants
  once.

---

## 1. Recommended Android project structure

```text
dungeonquest/
├── README.md
├── ARCHITECTURE.md                 ← this file
├── PROJECT_DECISIONS.md            ← decision log
├── PLAYTEST_LOG.md                 ← what the real tester did (start on day 1)
├── art-bible/                      ← style guide, palette, character sheets (not shipped)
├── content-tools/                  ← prompts we give an AI to draft content, review checklist
├── .github/ISSUE_TEMPLATE/
│   └── playtest.md                 ← how a playtest report becomes a GitHub issue (section 13)
│
├── settings.gradle.kts             ← include(":app", ":engine")
├── build.gradle.kts
├── gradle/libs.versions.toml       ← one place for every dependency version
│
├── engine/                         ← pure Kotlin/JVM library, no Android imports
│   ├── build.gradle.kts            ← kotlin("jvm"), kotlinx-serialization
│   └── src/
│       ├── main/kotlin/…           ← see section 2
│       └── test/kotlin/…           ← fast unit tests, run in seconds, no emulator
│
└── app/                            ← the Android application
    ├── build.gradle.kts            ← android application, compose, room (phase 2)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml ← no INTERNET permission, landscape, no cloud backup
        │   ├── kotlin/…            ← see section 2
        │   ├── assets/
        │   │   ├── content/        ← the content pack (JSON), see section 9
        │   │   └── audio/          ← narration + sound effects (.ogg)
        │   └── res/
        │       ├── drawable/       ← vector drawables (character parts, eggs, UI)
        │       ├── drawable-nodpi/ ← compressed .webp backgrounds
        │       └── font/           ← one friendly rounded font
        ├── test/                   ← JVM tests incl. content-pack validation
        └── androidTest/            ← a few Compose UI tests
```

**Why two modules and not one (or ten)?**

- The single most important rule in the spec is *"do not hard-code educational
  logic into UI screens."* If the engine lives in a module that cannot import
  Android or Compose, the compiler enforces that rule for us. A rule enforced by
  the build is one nobody has to remember.
- Engine tests run on the plain JVM in a couple of seconds. You will tune the
  difficulty rules many times; fast tests make that pleasant.
- Two modules is about ten extra lines of Gradle. Ten modules (`:feature-map`,
  `:core-ui`, `:data`, …) is the standard "large app" template and would be
  ceremony for a two-person project. We can split further later if a module
  gets uncomfortable; merging modules back is harder than splitting.
- A third module, `:ml`, appears only in Phase 7 if we get there.

---

## 2. Package / module structure

Package root is a placeholder (`com.dungeonquest`); renaming it is cheap until the
app is installed somewhere other than your own devices.

### `:engine` — `com.dungeonquest.engine`

| Package | What lives there | Example files |
|---|---|---|
| `model` | IDs, skills, templates, instances, responses, outcomes | `Ids.kt`, `Skill.kt`, `ActivityTemplate.kt`, `ActivityInstance.kt`, `ChildResponse.kt`, `ItemOutcome.kt` |
| `content` | Parse and validate the JSON content pack | `ContentCatalog.kt`, `ContentParser.kt`, `ContentValidator.kt` |
| `activity` | One sub-package per activity type: generator + evaluator + hint ladder | `count/CountObjectsGenerator.kt`, `count/CountObjectsEvaluator.kt`, `findsymbol/…`, `compare/…`, `ActivityRegistry.kt` |
| `learner` | Learner and skill state, confidence | `LearnerState.kt`, `SkillState.kt` |
| `difficulty` | Difficulty rules | `DifficultyPolicy.kt`, `RuleBasedDifficultyPolicy.kt`, `DifficultyRules.kt` |
| `intelligence` | The swappable "brain" (spec §18, §20) | `LearnerModel.kt`, `RuleBasedLearnerModel.kt` |
| `session` | The round state machine | `GameSession.kt`, `SessionState.kt`, `SessionEvent.kt` |
| `reward` | What you earn and what unlocks | `RewardPolicy.kt`, `UnlockRules.kt` |
| `util` | Seeded randomness, clock | `GameRandom.kt`, `Clock.kt` |

Adding a new activity type means adding one new folder under `activity/`,
one new `ActivitySpec` subclass, and registering it. Nothing in `session/`,
`difficulty/` or `intelligence/` changes.

### `:app` — `com.dungeonquest`

| Package | What lives there |
|---|---|
| (root) | `DungeonQuestApp.kt` (Application), `MainActivity.kt`, `AppContainer.kt` (manual dependency wiring) |
| `ui.theme` | Colors, typography, shapes; the art bible translated into code |
| `ui.components` | Reusable kid-sized pieces: `BigChoiceButton`, `CountableObject`, `Character`, `CelebrationBurst`, `RoundProgress` |
| `ui.home` | `HomeScreen` |
| `ui.map` | `MapScreen` (Phase 2) |
| `ui.play` | `PlayScreen`, `PlayViewModel`, `ActivityHost` |
| `ui.play.activities` | One composable per activity type: `CountObjectsActivity`, `FindSymbolActivity`, `CompareGroupsActivity` |
| `ui.reward` | `RewardScreen`, `StickerBookScreen` |
| `ui.parent` | `ParentGate`, `ParentScreen` |
| `data.content` | `AssetContentSource` (reads JSON from `assets/`), `SpriteCatalog` (sprite id → drawable) |
| `data.learner` | Room database, DAOs, entities, `LearnerRepository` |
| `audio` | `SoundPlayer` (SoundPool for short effects), `Narrator` (prompt key → audio clip) |
| `playtest` | The feedback tool (section 13): `FeedbackCapture`, `EventRecorder`, `CrashRecorder`, `FeedbackExporter` |

**Dependency injection:** a single hand-written `AppContainer` created in the
`Application` class that builds the database, content catalog, and engine
objects, and hands them to ViewModels through a small factory. No Hilt/Dagger.
With fewer than ~20 objects to wire, manual DI is easier to read than the
annotations and generated code a framework brings, and it teaches what DI
actually is.

---

## 3. Core Kotlin data classes

These are real Kotlin, trimmed of imports. They live in `:engine`.

### Identifiers

Using small wrapper types instead of raw strings means the compiler stops us
from passing a template id where a skill id is expected.

```kotlin
@Serializable @JvmInline value class SkillId(val value: String)
@Serializable @JvmInline value class TemplateId(val value: String)
@Serializable @JvmInline value class ThemeId(val value: String)
@Serializable @JvmInline value class SpriteId(val value: String)
@Serializable @JvmInline value class PromptKey(val value: String)
```

### Skill: *what* is being learned

```kotlin
@Serializable
data class Skill(
    val id: SkillId,                    // "counting", "number_recognition", "comparing"
    val domain: Domain,
    val maxLevel: Int = 5,
    val unlockedWhen: List<SkillRequirement> = emptyList(),
)

@Serializable
data class SkillRequirement(val skill: SkillId, val minLevel: Int)

@Serializable
enum class Domain { MATH, SHAPES, COLORS, LITERACY }
```

Skills are content (in JSON), not a Kotlin enum, so adding "shapes" in Phase 4
does not require touching engine code.

### ActivityTemplate: *a reusable way of practising a skill*

```kotlin
@Serializable
data class ActivityTemplate(
    val id: TemplateId,                 // "count_dino_eggs"
    val skill: SkillId,
    val theme: ThemeId,                 // "dino_valley"
    val requiresReading: Boolean = false,
    val prompts: PromptSet,
    val activity: ActivitySpec,         // type-specific part, see below
)

@Serializable
data class PromptSet(
    val intro: PromptKey,               // "Mama Dino's eggs! Let's count them."
    val question: PromptKey,            // "How many eggs?"
    val encourage: PromptKey,           // "Let's count together!"
    val celebrate: PromptKey,           // "You found them all!"
)
```

The type-specific part is a sealed hierarchy. The JSON field `"type"` picks
the subclass. Each activity type defines its own per-level parameters, so
"Count objects" can have `arrangement` while "Compare groups" has `minDifference`.

```kotlin
@Serializable
sealed interface ActivitySpec { val levels: List<LevelSpec> }

@Serializable
sealed interface LevelSpec { val level: Int }

@Serializable @SerialName("COUNT_OBJECTS")
data class CountObjectsSpec(
    val sprites: List<SpriteId>,
    override val levels: List<CountObjectsLevel>,
) : ActivitySpec

@Serializable
data class CountObjectsLevel(
    override val level: Int,
    val minCount: Int,
    val maxCount: Int,
    val arrangement: Arrangement,       // LINE, GRID, SCATTER, CLUSTERS
    val choiceCount: Int,               // how many answer buttons (2–4)
    val distractors: Int = 0,           // other objects that should NOT be counted
    val tapToCount: Boolean = true,     // child taps each object; app says the number
    val objectSize: ObjectSize = ObjectSize.LARGE,
) : LevelSpec

/** "Tap the one I say": numeral recognition AND letter recognition. */
@Serializable @SerialName("FIND_SYMBOL")
data class FindSymbolSpec(
    val symbolSet: SymbolSet,
    val sprites: List<SpriteId> = emptyList(),  // objects for BY_QUANTITY, pictures for BY_SOUND
    override val levels: List<FindSymbolLevel>,
) : ActivitySpec

@Serializable
data class FindSymbolLevel(
    override val level: Int,
    val pool: List<String>? = null,     // explicit symbols, e.g. ["1","2","3"]; null = parent's
                                        // "starter letters" (his name) for letter templates
    val choiceCount: Int,
    val mode: FindSymbolMode,
    val showHelper: Boolean,            // dots under numerals / picture next to letters; fades out
    val allowConfusable: Boolean = false, // b/d/p/q, M/W, 6/9 only together at high levels
) : LevelSpec

@Serializable @SerialName("COMPARE_GROUPS")
data class CompareGroupsSpec(
    val sprites: List<SpriteId>,
    override val levels: List<CompareGroupsLevel>,
) : ActivitySpec

@Serializable
data class CompareGroupsLevel(
    override val level: Int,
    val minCount: Int,
    val maxCount: Int,
    val minDifference: Int,             // 4 vs 1 is easy, 5 vs 4 is hard
    val askFewer: Boolean = false,      // "more" first; "fewer" at higher levels
    val sameSizeObjects: Boolean = true,// later: big objects in the smaller group
) : LevelSpec

@Serializable enum class Arrangement { LINE, GRID, SCATTER, CLUSTERS }
@Serializable enum class ObjectSize { LARGE, MEDIUM, SMALL }
@Serializable enum class SymbolSet { NUMERALS, UPPERCASE, LOWERCASE }
@Serializable enum class FindSymbolMode {
    BY_NAME,        // "Find the 3" / "Find the B"
    BY_QUANTITY,    // numerals: "Which number tells how many eggs?"
    BY_SOUND,       // letters: "Which letter says /b/, like ball?"
    MATCH_CASE,     // letters: "Find the little b that goes with big B"
}
```

### ActivityInstance: *one concrete question the child sees*

Generated at runtime from a template + level + random seed. The generator
decides **everything visual about the layout**, as normalized coordinates
(0.0–1.0), so the UI only draws what it is told and the engine can test that
"4 eggs" really means 4 eggs on screen.

```kotlin
sealed interface ActivityInstance {
    val templateId: TemplateId
    val skill: SkillId
    val level: Int
    val prompts: PromptSet
    val scene: Scene
    val seed: Long                      // replay any question exactly (bug reports!)
}

data class Scene(val objects: List<PlacedObject>)

data class PlacedObject(
    val sprite: SpriteId,
    val x: Float, val y: Float,         // 0..1, relative to the play area
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val group: Int = 0,                 // for compare: 0 = left nest, 1 = right nest
    val countable: Boolean = true,      // false for distractors
)

data class CountObjectsInstance(
    override val templateId: TemplateId, override val skill: SkillId,
    override val level: Int, override val prompts: PromptSet,
    override val scene: Scene, override val seed: Long,
    val answer: Int,
    val choices: List<Int>,
    val tapToCount: Boolean,
) : ActivityInstance

data class FindSymbolInstance(/* common fields… */ val symbolSet: SymbolSet, val target: String,
    val choices: List<String>, val mode: FindSymbolMode, val showHelper: Boolean) : ActivityInstance

data class CompareGroupsInstance(/* common fields… */ val leftCount: Int, val rightCount: Int,
    val askFewer: Boolean) : ActivityInstance
```

### What the child does, and how it went

```kotlin
sealed interface ChildResponse {
    data class NumberChosen(val value: Int) : ChildResponse
    data class SymbolChosen(val symbol: String) : ChildResponse
    data class GroupChosen(val side: Side) : ChildResponse
}
enum class Side { LEFT, RIGHT }

/** Which kind of help to give next. Ordered from lightest to strongest. */
sealed interface Hint {
    data object TryAgain : Hint                         // "Hmm, let's look again!" + gentle wiggle
    data object CountTogether : Hint                    // app counts aloud, highlighting each object
    data class NarrowChoices(val keep: List<String>) : Hint // only 2 buttons left, right one glows softly
}

data class Evaluation(val correct: Boolean, val nextHint: Hint?)

/** One finished question. This is the row we store and learn from. */
data class ItemOutcome(
    val templateId: TemplateId,
    val activityType: String,           // "COUNT_OBJECTS"
    val skill: SkillId,
    val level: Int,
    val firstTryCorrect: Boolean,       // THE signal difficulty uses
    val tries: Int,                     // 1 = first try
    val hintsUsed: Int,
    val responseTimeMs: Long,           // recorded, not used for decisions in v1
    val completedAtMillis: Long,
    val seed: Long,
)
```

Note: an item **always ends in success**. After at most three tries with
growing help, the right answer is easy to find. The child never "fails out";
we simply record that they needed help (`firstTryCorrect = false`, `hintsUsed = 2`).
This is how "Let's figure it out together" becomes a data model.

### Learner state

```kotlin
data class LearnerState(
    val profileId: Long,
    val skills: Map<SkillId, SkillState>,
)

data class SkillState(
    val skill: SkillId,
    val level: Int = 1,
    val attempts: Int = 0,              // lifetime items
    val firstTryCorrect: Int = 0,       // lifetime
    val hintsUsed: Int = 0,             // lifetime
    val recent: List<Boolean> = emptyList(), // first-try results AT CURRENT LEVEL, newest last
    val itemsAtLevel: Int = 0,
    val consecutiveCorrect: Int = 0,    // the spec's "streak" — internal only, never shown
    val consecutiveMisses: Int = 0,
    val averageResponseMs: Long? = null,
    val lastPlayedAtMillis: Long? = null,
) {
    val incorrectAttempts get() = attempts - firstTryCorrect
    val recentAccuracy: Double? get() = if (recent.isEmpty()) null else recent.count { it } / recent.size.toDouble()
    val confidence: Confidence get() = Confidence.from(this)
}

enum class Confidence { NEW, DEVELOPING, MODERATE, STRONG;
    companion object {
        fun from(s: SkillState): Confidence {
            val acc = s.recentAccuracy ?: return NEW
            return when {
                s.attempts < 5 -> NEW
                acc < 0.60 -> DEVELOPING
                acc < 0.85 -> MODERATE
                else -> STRONG
            }
        }
    }
}
```

This covers every field listed in spec §8 (`correctAttempts` is
`firstTryCorrect`, `streak` is `consecutiveCorrect`, `currentDifficulty` is
`level`).

---

## 4. Compose screen hierarchy

```text
MainActivity
└── DungeonQuestTheme
    └── AppNavigation                     (prototype: a simple `when(screen)`; Phase 2: Navigation Compose)
        ├── HomeScreen                    big Play button, character waving, sleepy/awake by time of day
        │     └── ParentGate (hidden: hold top-right corner 3 s) → ParentScreen
        ├── MapScreen                     Phase 2: Dino Valley with 3–4 stops, locked stops are "foggy"
        ├── PlayScreen  ← PlayViewModel
        │     ├── SceneBackground         layered background, gentle parallax/sway
        │     ├── Character               state: Idle, Talking, Thinking, Cheering, Encouraging
        │     ├── ActivityHost            `when (instance)` → picks the composable:
        │     │     ├── CountObjectsActivity
        │     │     ├── FindSymbolActivity  (numerals and letters)
        │     │     └── CompareGroupsActivity
        │     ├── HintOverlay             highlights, counting numbers floating above objects
        │     ├── RoundProgress           e.g. 5 empty nest spots that fill — not a score
        │     └── CelebrationBurst        leaves/stars particle burst on Canvas
        ├── RewardScreen                  sticker/fossil flies into the collection
        └── StickerBookScreen             Phase 2: everything collected, tappable for a little animation
```

**How a screen talks to the engine (unidirectional data flow):**

```text
PlayScreen ──(onNumberChosen(3))──▶ PlayViewModel ──▶ GameSession.onEvent(Answered(3))
    ▲                                                        │
    └────── collectAsStateWithLifecycle() ◀── StateFlow<PlayUiState> ◀── new SessionState
```

- Composables never decide whether an answer is right. They send events and
  draw state.
- `ActivityHost` uses an exhaustive `when` over the sealed `ActivityInstance`.
  When we add a fourth activity type, the build fails until it has a
  composable. That is a feature: the compiler gives us a to-do list.
- The engine does not know how long an animation takes. The UI tells the
  engine when it's done: `IntroFinished`, `HintShown`, `CelebrationFinished`.
  That keeps animation timing (the designer's domain) out of game logic.

**For the experience designer:** every visual or audio moment in a round maps
to one `SessionState`. That list (in section 5) is effectively the storyboard
of a round. You can design each state in isolation: "what does the character
do in `Thinking`?", "what does `Encouraging` sound like?".

**Animation approach.** Compose's built-in animation APIs are enough:
`Animatable` and spring physics for bouncy pops, `updateTransition` for the
character's moods, `graphicsLayer` for squash-and-stretch, `InfiniteTransition`
for idle breathing and blinking, `Canvas` for particles. The character is
built from a few **layered parts** (body, head, eyes-open, eyes-closed, a few
mouths) so we can blink, look, bounce, and talk from a handful of drawings
instead of a frame-by-frame sprite sheet. No Lottie or game engine.

---

## 5. Activity / game engine architecture

### The flow of one question

```text
          ContentCatalog (templates, skills)          LearnerState
                       │                                   │
                       └──────────┐        ┌───────────────┘
                                  ▼        ▼
                  LearnerModel.recommendNext(...)  →  ActivityRequest(template, level, ease)
                                  │
                                  ▼
              ActivityRegistry[type].generator.generate(request, seed)  →  ActivityInstance
                                  │
                                  ▼
                       GameSession (state machine)  ⇄  UI events
                                  │
                       evaluator.evaluate(instance, response, tries) → Evaluation(correct, nextHint)
                                  │  (item finished)
                                  ▼
                              ItemOutcome
                         ┌────────┴──────────┐
                         ▼                   ▼
              LearnerRepository.record()   LearnerModel.onItemCompleted() → new LearnerState
```

### Activity types are plugins

```kotlin
interface ActivityGenerator<S : ActivitySpec> {
    fun generate(template: ActivityTemplate, spec: S, level: Int, ease: Ease, random: GameRandom): ActivityInstance
}

interface ActivityEvaluator<I : ActivityInstance> {
    fun evaluate(instance: I, response: ChildResponse, triesSoFar: Int): Evaluation
}

class ActivityRegistry(private val entries: Map<KClass<out ActivitySpec>, ActivityEntry<*, *>>) { … }
```

Each activity type provides exactly a generator, an evaluator (which owns its
hint ladder), and a composable in `:app`. That is the whole contract.

### A round (the session state machine)

A **round** is about 5 questions (~2–3 minutes), framed as a tiny story:
*"Mama Dino lost her eggs! Help her find 5 nests."* Then a reward.

```kotlin
sealed interface SessionState {
    data class Introducing(val item: ActivityInstance, val demonstrate: Boolean) : SessionState
    data class AwaitingAnswer(val item: ActivityInstance, val tries: Int, val hintsUsed: Int,
                              val startedAtMillis: Long) : SessionState
    data class Helping(val item: ActivityInstance, val hint: Hint, val tries: Int,
                       val hintsUsed: Int, val startedAtMillis: Long) : SessionState
    data class Celebrating(val item: ActivityInstance, val outcome: ItemOutcome,
                           val itemsDone: Int, val itemsInRound: Int) : SessionState
    data class RoundComplete(val reward: Reward) : SessionState
}

sealed interface SessionEvent {
    data object IntroFinished : SessionEvent
    data class Answered(val response: ChildResponse) : SessionEvent
    data object HintShown : SessionEvent
    data object CelebrationFinished : SessionEvent
}
```

```text
Introducing ──IntroFinished──▶ AwaitingAnswer ──Answered(right)──▶ Celebrating ──CelebrationFinished──┐
     ▲                              │    ▲                                                             │
     │                     Answered(wrong)│HintShown                                                    │
     │                              ▼    │                                                             │
     │                            Helping                                                              │
     └──────────────── next item (if itemsDone < itemsInRound) ◀───────────────────────────────────────┘
                                                                   else ──▶ RoundComplete
```

- **Demonstrate before expecting** (spec §7): `Introducing(demonstrate = true)`
  the first time a child meets an activity type, and again after a level
  change. The character counts "1… 2… 3!" with highlights before asking.
- `GameSession` is plain Kotlin with an injected `Clock` and `GameRandom`, so
  a test can play a whole round in milliseconds and assert on every state.
- Persistence happens once per finished item. If the app is closed mid-question,
  nothing is lost except that question.

### Randomness

All generation goes through a seeded `GameRandom`. Every stored outcome keeps
its seed, so "he got stuck on that weird one with the eggs at the edge" can be
replayed exactly in a test.

---

## 6. Learner-progress data model

Principle: **the log is the truth; everything else is a summary of the log.**

| Table | Kind | Purpose |
|---|---|---|
| `learner_profile` | 1 row | `id`, `displayName` (optional, default "Explorer"), `avatar`, `createdAt`. No birthday, no real name required. |
| `play_session` | append | `id`, `profileId`, `startedAt`, `endedAt`, `itemsCompleted`. Answers "time played". |
| `item_outcome` | append-only log | Every `ItemOutcome` field + `profileId`, `sessionId`, `contentVersion`. The source of truth. |
| `skill_state` | cache | One row per (profile, skill): the `SkillState` fields. Can be rebuilt by replaying `item_outcome` through the difficulty rules. |
| `reward_earned` | append | `rewardId`, `earnedAt`, `sessionId`. The sticker book. |
| `unlock` | append | `unlockId` (map stop, accessory), `unlockedAt`. |
| `app_settings` | 1 row | Sound on/off, music volume, round length, daily-play gentle limit. |

```kotlin
@Entity(
    tableName = "item_outcome",
    indices = [Index(value = ["profileId", "skillId", "completedAtMillis"])],
)
data class ItemOutcomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val sessionId: Long,
    val templateId: String,
    val activityType: String,
    val skillId: String,
    val level: Int,
    val firstTryCorrect: Boolean,
    val tries: Int,
    val hintsUsed: Int,
    val responseTimeMs: Long,
    val completedAtMillis: Long,
    val seed: Long,
    val contentVersion: Int,
)
```

**Why a log plus a cache:**

- If we change the difficulty rules (we will), we can replay history and
  recompute every skill level fairly instead of being stuck with numbers
  produced by old rules.
- Parent mode's "accuracy trends" and "skills becoming stronger" are simple
  queries over the log.
- A future on-device ML model needs exactly this: a local table of
  `(skill, level, context) → outcome`. No extra collection needed later.
- Size is a non-issue: even 100 questions a day for two years is ~73,000 small
  rows.

Everything is keyed by `profileId` even though v1 has one profile. Adding a
column now is free; adding it later is a migration.

---

## 7. First version of the rules-based difficulty engine

### What signal it uses

- **First-try correctness** at the **current level** is the only input to
  level changes. "Correct after a hint" is good learning, but it means the
  level isn't comfortable yet.
- **Response time is recorded but ignored** in v1. A 4-year-old's response
  time mostly measures whether the dog walked past. Once we have weeks of
  data we can look at whether it means anything.

### The rules

```kotlin
data class DifficultyRules(
    val windowSize: Int = 8,                // look at the last 8 items at this level
    val minItemsBeforeChange: Int = 5,      // no change until 5 items at this level
    val promoteAccuracy: Double = 0.85,     // spec: > 85% → go up gradually
    val promoteConsecutive: Int = 3,        // …and the last 3 were first-try correct
    val demoteAccuracy: Double = 0.60,      // spec: < 60% → go down
    val struggleMisses: Int = 3,            // 3 needed-help items in a row → step down now
)

class RuleBasedDifficultyPolicy(private val rules: DifficultyRules = DifficultyRules()) : DifficultyPolicy {

    override fun update(state: SkillState, skill: Skill, outcome: ItemOutcome): SkillState {
        val correct = outcome.firstTryCorrect
        val lifetime = state.copy(
            attempts = state.attempts + 1,
            firstTryCorrect = state.firstTryCorrect + if (correct) 1 else 0,
            hintsUsed = state.hintsUsed + outcome.hintsUsed,
            lastPlayedAtMillis = outcome.completedAtMillis,
        )
        // Warm-up items below the current level count toward totals only.
        if (outcome.level != state.level) return lifetime

        val updated = lifetime.copy(
            recent = (state.recent + correct).takeLast(rules.windowSize),
            itemsAtLevel = state.itemsAtLevel + 1,
            consecutiveCorrect = if (correct) state.consecutiveCorrect + 1 else 0,
            consecutiveMisses = if (correct) 0 else state.consecutiveMisses + 1,
        )
        return when (decide(updated, skill.maxLevel)) {
            LevelChange.UP -> updated.movedTo(updated.level + 1)
            LevelChange.DOWN -> updated.movedTo(updated.level - 1)
            LevelChange.HOLD -> updated
        }
    }

    private fun decide(s: SkillState, maxLevel: Int): LevelChange {
        // Struggle relief comes first and ignores the minimum-items rule.
        if (s.consecutiveMisses >= rules.struggleMisses && s.level > 1) return LevelChange.DOWN
        if (s.itemsAtLevel < rules.minItemsBeforeChange) return LevelChange.HOLD
        val accuracy = s.recentAccuracy ?: return LevelChange.HOLD
        return when {
            accuracy < rules.demoteAccuracy && s.level > 1 -> LevelChange.DOWN
            accuracy >= rules.promoteAccuracy &&
                s.consecutiveCorrect >= rules.promoteConsecutive &&
                s.level < maxLevel -> LevelChange.UP
            else -> LevelChange.HOLD
        }
    }

    private fun SkillState.movedTo(level: Int) = copy(
        level = level, recent = emptyList(), itemsAtLevel = 0,
        consecutiveCorrect = 0, consecutiveMisses = 0,
    )
}

enum class LevelChange { UP, DOWN, HOLD }
```

What this means in practice:

- Going **up** takes at least 5 questions at a level, roughly 7 of the last 8
  right first time, and the last 3 in a row right. One level at a time.
- Going **down** happens after 5+ items below 60%, or immediately after 3
  questions in a row that needed help. Frustration is more costly than
  boredom at this age.
- After any change the window resets, which gives natural hysteresis: no
  bouncing between levels on alternate questions.

### Gentler, smaller adjustments (no level change)

These live in `RuleBasedLearnerModel`, not the difficulty policy:

- **Warm-up:** the first item of a play session is one level below the current
  level (minimum 1). Easy wins first.
- **After a miss:** the next item at the same level is generated with
  `Ease.GENTLE` (e.g. counts from the lower half of the level's range).
- **Back after a break:** if a skill wasn't played for 14+ days, its first two
  items are warm-ups.

### Choosing what to play next (`RuleBasedLearnerModel`)

```kotlin
interface LearnerModel {
    fun recommendNext(learner: LearnerState, catalog: ContentCatalog,
                      recent: List<ItemOutcome>, area: AreaId, random: GameRandom): ActivityRequest
    fun onItemCompleted(learner: LearnerState, outcome: ItemOutcome): LearnerState
}

data class ActivityRequest(val template: ActivityTemplate, val level: Int, val ease: Ease)
```

1. Candidates = templates in the current map area whose skill is unlocked
   (`Skill.unlockedWhen`, e.g. *comparing* unlocks at *counting* level 2).
2. Drop the template used last, and any activity type used in each of the last 2 items (variety).
3. Pick a skill: 60% the unlocked skill with the lowest confidence, 40% a
   random other one (keeps strong skills fun and avoids drilling).
4. Pick a template for that skill at random (different theme/objects).
5. Level = the skill's level (or warm-up level), ease from the rules above.

This is the class a future `OnDeviceMlLearnerModel` replaces. `GameSession`,
the UI and storage never know which one is running (spec §18, §20).

### MVP skills and what the 5 levels mean

| Level | Counting (`COUNT_OBJECTS`) | Number recognition (`FIND_SYMBOL`, numerals) | Letter recognition (`FIND_SYMBOL`, letters) | Comparing (`COMPARE_GROUPS`) |
|---|---|---|---|---|
| 1 | 1–3 big objects in a line, tap to count, 2 choices | Numerals 1–3 with dots underneath, 2 choices | 2 uppercase letters from his name, very different shapes, picture helper | 1 vs 4 ("more"), unlocks at counting L2 |
| 2 | 1–5 in a line, 3 choices | 1–5 with dots, 3 choices | 3 choices, all name letters | Difference ≥ 2, up to 5 |
| 3 | 1–5 scattered | 1–5, no dots | 3 choices, name letters + 6 common letters, no helper | Difference ≥ 1, up to 5 |
| 4 | 1–7 scattered + 1–2 distractors | Match quantity to numeral, up to 7 | 4 choices, all uppercase, similar shapes allowed | Ask "fewer" sometimes |
| 5 | 1–10, clusters, no tap-to-count aid | 1–10, 4 choices | Match uppercase to lowercase | Up to 10; bigger objects in the smaller group |

`letter_sounds` (`BY_SOUND`) is its own skill in Phase 3, unlocked at
letter recognition level 3.

These numbers are a first guess to be tuned by watching your son, which is
why they are content (JSON), not code.

---

## 8. Local storage approach

| What | Where | Why |
|---|---|---|
| Learner progress, rewards, settings | **Room** (SQLite) | Relational, queryable for parent mode, typed DAOs, migration support, coroutine/Flow APIs. Official Jetpack. |
| Educational content | **JSON in `assets/content/`**, read-only, parsed at startup with kotlinx.serialization | Human- and AI-readable, reviewable in a diff, versioned with the code. Small enough (KBs) to load fully into memory. |
| Images | `res/drawable` (vectors) and `res/drawable-nodpi` (WebP) | Native resource system, density handling, compile-time IDs. |
| Audio | `assets/audio/*.ogg` | Small, Android-native format, loaded by key. |
| Settings | A one-row Room table | Avoids adding DataStore for three values. |

Details:

- **The first prototype uses no database at all.** Progress is in memory. Room
  arrives in Phase 2 when there is something worth keeping.
- **Sprite and prompt lookups are explicit maps**, not `getIdentifier()`
  reflection. A content test fails if JSON references a sprite id that has no
  drawable, or a prompt key that has no text.
- **Backups:** learning data is excluded from Google cloud backup
  (`dataExtractionRules`) but allowed in direct device-to-device transfer, so
  a new phone keeps progress without the data ever going to a server. See
  ambiguity #9.
- **Content versioning:** the content pack has a `contentVersion`; each stored
  outcome records it, so old history stays interpretable after content changes.

---

## 9. Content JSON schema

The content pack is a few files in `app/src/main/assets/content/`:

```text
content/
├── manifest.json        contentVersion, schemaVersion, list of files
├── skills.json          skills and unlock requirements
├── areas.json           map stops and which templates/skills they host
├── templates/
│   ├── counting.json
│   ├── number_recognition.json
│   ├── letter_recognition.json
│   └── comparing.json
├── prompts.en.json      prompt key → text + audio clip (one file per language)
└── rewards.json         stickers/fossils and when they're given
```

### A template (counting)

```json
{
  "id": "count_dino_eggs",
  "skill": "counting",
  "theme": "dino_valley",
  "requiresReading": false,
  "prompts": {
    "intro": "count.eggs.intro",
    "question": "count.how_many",
    "encourage": "common.count_together",
    "celebrate": "count.eggs.celebrate"
  },
  "activity": {
    "type": "COUNT_OBJECTS",
    "sprites": ["egg_blue_spots", "egg_green_stripes"],
    "levels": [
      { "level": 1, "minCount": 1, "maxCount": 3,  "arrangement": "LINE",     "choiceCount": 2, "tapToCount": true,  "objectSize": "LARGE" },
      { "level": 2, "minCount": 1, "maxCount": 5,  "arrangement": "LINE",     "choiceCount": 3, "tapToCount": true,  "objectSize": "LARGE" },
      { "level": 3, "minCount": 1, "maxCount": 5,  "arrangement": "SCATTER",  "choiceCount": 3, "tapToCount": true,  "objectSize": "MEDIUM" },
      { "level": 4, "minCount": 2, "maxCount": 7,  "arrangement": "SCATTER",  "choiceCount": 3, "distractors": 2, "tapToCount": true, "objectSize": "MEDIUM" },
      { "level": 5, "minCount": 3, "maxCount": 10, "arrangement": "CLUSTERS", "choiceCount": 4, "distractors": 2, "tapToCount": false, "objectSize": "SMALL" }
    ]
  }
}
```

### A template (comparing)

```json
{
  "id": "compare_nest_eggs",
  "skill": "comparing",
  "theme": "dino_valley",
  "prompts": {
    "intro": "compare.nests.intro",
    "question": "compare.which_more",
    "encourage": "common.count_together",
    "celebrate": "compare.nests.celebrate"
  },
  "activity": {
    "type": "COMPARE_GROUPS",
    "sprites": ["egg_blue_spots"],
    "levels": [
      { "level": 1, "minCount": 1, "maxCount": 4, "minDifference": 3 },
      { "level": 2, "minCount": 1, "maxCount": 5, "minDifference": 2 },
      { "level": 3, "minCount": 1, "maxCount": 5, "minDifference": 1 },
      { "level": 4, "minCount": 1, "maxCount": 7, "minDifference": 1, "askFewer": true },
      { "level": 5, "minCount": 2, "maxCount": 10, "minDifference": 1, "askFewer": true, "sameSizeObjects": false }
    ]
  }
}
```

### A template (letters)

```json
{
  "id": "find_letter_dino_footprints",
  "skill": "letter_recognition",
  "theme": "dino_valley",
  "prompts": {
    "intro": "letters.footprints.intro",
    "question": "letters.find_letter",
    "encourage": "letters.look_together",
    "celebrate": "letters.footprints.celebrate"
  },
  "activity": {
    "type": "FIND_SYMBOL",
    "symbolSet": "UPPERCASE",
    "levels": [
      { "level": 1, "choiceCount": 2, "mode": "BY_NAME",    "showHelper": true },
      { "level": 2, "choiceCount": 3, "mode": "BY_NAME",    "showHelper": true },
      { "level": 3, "choiceCount": 3, "mode": "BY_NAME",    "showHelper": false, "pool": ["A","M","O","S","T","X"] },
      { "level": 4, "choiceCount": 4, "mode": "BY_NAME",    "showHelper": false, "allowConfusable": true },
      { "level": 5, "choiceCount": 3, "mode": "MATCH_CASE", "showHelper": false }
    ]
  }
}
```

Levels 1–2 leave `pool` empty, so they use the parent's "starter letters"
(the letters of his name). The prompt `letters.find_letter` is spoken with
the target letter filled in from the `letter.B` clip.

### Skills and prompts

```json
[
  { "id": "counting",           "domain": "MATH" },
  { "id": "number_recognition", "domain": "MATH", "unlockedWhen": [{ "skill": "counting", "minLevel": 2 }] },
  { "id": "comparing",          "domain": "MATH", "unlockedWhen": [{ "skill": "counting", "minLevel": 2 }] },
  { "id": "letter_recognition", "domain": "LITERACY" },
  { "id": "letter_sounds",      "domain": "LITERACY", "unlockedWhen": [{ "skill": "letter_recognition", "minLevel": 3 }] }
]
```

```json
{
  "count.how_many":        { "text": "How many eggs?",            "audio": "count_how_many" },
  "count.eggs.intro":      { "text": "Mama Dino's eggs rolled away! Let's count them.", "audio": "count_eggs_intro" },
  "common.count_together": { "text": "Let's count together!",     "audio": "count_together" },
  "number.1":              { "text": "one",                       "audio": "n1" },
  "letter.B":              { "text": "B",                         "audio": "letter_b_name" },
  "letter_sound.B":        { "text": "/b/",                       "audio": "letter_b_sound" }
}
```

### Why this shape

- **Common fields + a typed `activity` block.** Everything every activity has
  (skill, theme, prompts, reading flag) is outside; everything type-specific
  is inside, selected by `"type"`. New activity types add a new `"type"`
  without touching existing templates or the parser's common code.
- **Levels live inside the template**, rather than one template per
  difficulty (the spec's example had `"difficulty": 1` on the template). One
  template then describes a complete progression for one idea, and the
  difficulty engine can move a child up and down without switching to a
  visually unrelated template. See ambiguity #2.
- **Words are keys, not text.** The activity says `count.how_many`; text and
  audio are looked up separately (spec §23). Narration, a second language, or
  re-recording a voice line never touches activity data.
- **Validated before it ships.** `ContentValidator` (in `:engine`) checks that
  ids are unique, every skill/sprite/prompt reference exists, every template
  has levels 1–5 exactly once, `min <= max`, `choiceCount` fits the range,
  `requiresReading` is false for MVP, every prompt key has an audio clip,
  and so on. A unit test runs it on the real
  content pack, so **AI-generated content cannot ship unless it passes the
  validator and a human has reviewed the diff.** We also keep a JSON Schema
  file in `content-tools/` purely as documentation to paste into an AI
  prompt; the app doesn't need a JSON Schema library.

---

## 10. Minimum dependencies

Everything below is official Kotlin or Android Jetpack. Use whatever stable
versions Android Studio's "Empty Activity" template offers on the day, kept
in `gradle/libs.versions.toml`.

| Dependency | Needed when | Why / what problem | Could we do it natively? | Maintenance |
|---|---|---|---|---|
| Compose BOM, `ui`, `foundation`, `material3`, `ui-tooling-preview` | Prototype | The UI toolkit you chose. BOM keeps Compose versions consistent. | It *is* the native toolkit. | Low; one version to bump. |
| `activity-compose` | Prototype | Hosts Compose in `MainActivity`. | No. | Low. |
| `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | Prototype | ViewModels survive rotation; `collectAsStateWithLifecycle`. | Partially, with more code. | Low; Jetpack. |
| `kotlinx-coroutines-core` | Prototype (engine) | `StateFlow` for session state, Room's suspend APIs later. | No; it's Kotlin's official async library. | Low. |
| `kotlinx-serialization-json` + plugin | Phase 2 (content pack) | Typed JSON → data classes, including the sealed `activity` types. Works in pure-Kotlin `:engine`. | `org.json` is built into Android but is untyped, verbose, and not available in a JVM module. | Low; JetBrains. |
| Room (`runtime`, `ktx`, `compiler`) + KSP plugin | Phase 2 | Learner database: typed queries, migrations, Flow. | Raw `SQLiteOpenHelper` works but means hand-written SQL plumbing and migrations. | Low-medium; migrations need care when schema changes. |
| `navigation-compose` | Phase 2 (optional) | Back stack, system back handling, per-screen ViewModels once there are 5+ screens. | A `when(screen)` + `BackHandler` is fine for 2–3 screens, which is why the prototype skips it. | Low. |
| JUnit, `kotlinx-coroutines-test`, Compose `ui-test-junit4` | Prototype | Tests. | No. | Low. |

**Deliberately not used:** Hilt/Dagger (manual DI is enough), Retrofit/OkHttp
(no network), Firebase/analytics/crash SDKs (privacy), Lottie (Compose
animation is enough; revisit if the designer wants After Effects animations),
Coil/Glide (no remote images), DataStore (Room table instead), any game
engine, any TTS/AI SDK. Sound uses Android's built-in `SoundPool` and
`MediaPlayer`.

**Platform settings:** minSdk 26 (Android 8, covers effectively every device
still in a house; on-device voice input needs Android 12+ and simply stays
hidden on older phones), target/compile the latest stable SDK, Kotlin 2.x with the
Compose compiler Gradle plugin.

---

## 11. Realistic first-prototype plan

**Goal: a delightful 2-minute toy, not a foundation.** Success test: *does he
grab the phone and ask to play again?* We cut everything except what makes one
counting game feel magical, but we still put the counting logic in `:engine`
so the prototype's work isn't thrown away.

### Who does what

| Claude (builds) | You two (direct) |
|---|---|
| All code, tests, and the build that installs on the phone | Approve the character, colours and feel |
| Draws the character, background and eggs as vector art in code | Pick and name the dino with your son |
| Writes the voice script and wires in clips | Record the voice lines (a family voice beats a robot) |
| Placeholder text-to-speech clips until your recordings arrive | Run playtests and send reports |
| Fixes what playtests find | Decide what matters most next |

### Step 1: before building (your input)

- **Choose the character with your son.** Claude draws 3–4 dino variations
  as a page you can show him; he picks one and names it. He'll care more
  about a friend he named.
- Lock a tiny style: flat shapes, thick soft outlines, 5-colour palette, big
  eyes. It becomes the first page of `art-bible/`.
- The round's story in one sentence: *"Mama Dino's eggs rolled away; help her
  count them back into the nest."*

### Step 2: the build (Claude)

1. Android project with `:app` and `:engine`; landscape, full-screen, no
   INTERNET permission; a GitHub Actions workflow that builds an installable
   APK on every push.
2. In `:engine`: `CountObjectsGenerator` (levels hard-coded in Kotlin, no JSON
   yet), evaluator with the 3-step hint ladder, `GameSession` state machine,
   unit tests.
3. `PlayScreen`: background, the chosen character as layered vector parts
   (body, head, 2 eye states, 3 mouths), eggs placed from the generated
   `Scene`. Eggs pop in one by one with a spring. Tapping an egg makes it
   wiggle, shows a big number, and says the number. Big round answer buttons
   show the numeral **and** dots.
4. **Make it feel good.** Correct: character squash-and-stretch jump, **the
   eggs hatch into that many baby dinos** (the reward *is* the quantity),
   leaf/star burst, chime. Wrong: character tilts head, "hmm", eggs gently
   re-count themselves with highlights, try again; third try leaves two
   choices. Idle: blink, breathe, look at whatever was tapped. A round = 5
   questions; a nest with 5 spots fills up; end with a hatch party and a
   "play again?" button that's a giant egg.
5. Crash capture and a minimal two-finger-hold screenshot-and-state report.

### Step 3: voices (you, any time)

Claude provides a script of ~25 short lines ("Let's count!", "one" … "ten",
"How many eggs?", "Let's count together!", "You did it!", a giggle). Record
them on a phone in a quiet room and drop the files in the project; Claude
trims and wires them in, replacing the placeholder voice.

### Step 4: playtest (you, the most important hour)

Install the APK from GitHub on the phone, turn on the phone's screen
recorder and Android's app pinning, and let him play. One parent just
watches and notes where he tapped first, what he said, what made him laugh,
where he looked confused, and whether he asked for another round. Share what
you saw in the project, and Claude fixes the one thing that mattered most.

### Explicitly not in the first prototype

Map, Room, JSON content, difficulty engine, the other two activity types, parent
mode, settings. The back button simply returns to the home screen; use
Android's built-in **app pinning** during playtests so little hands can't
leave the app.

### Phase 2 order (for orientation)

Playtest feedback tool (section 13) → JSON content pack + validator → Room + learner log → difficulty policy and
`RuleBasedLearnerModel` → `FIND_SYMBOL` with numerals, then letters → `COMPARE_GROUPS` → sticker book
→ a three-stop Dino Valley map → minimal parent screen → voice-answer
experiment (numbers only, on-device).

---

## 12. Architectural problems and ambiguities

Each has a recommended default so work isn't blocked; the **bold** ones are
where your answer would change something soon.

1. **"Basic scoring" (Phase 1) vs. "no pressure" (§14).** A number score
   invites comparison and pressure. *Default:* no numbers; round progress is 5
   nest spots that fill, and rewards are things (baby dinos, stickers).
2. **Difficulty per template vs. per skill.** The spec's JSON puts
   `difficulty` on a template, while §8 tracks difficulty per skill.
   *Default:* level is per skill; templates describe all 5 levels.
3. **What is an "attempt"?** With retries and hints, "correct" is ambiguous.
   *Default:* one question = one item; difficulty uses first-try-without-help
   correctness; tries and hints are stored too.
4. **"Streak" vs. "avoid streak anxiety."** *Default:* `consecutiveCorrect`
   exists internally for the rules and is never shown. No daily streaks.
5. **"Count objects" vs. "Choose the correct number" overlap.** *Default:*
   Count objects = tap each, then say how many (counting, one-to-one).
   Choose the number = find a numeral (number recognition). Separate skills.
6. ~~Audio can't wait for Phase 5.~~ **Answered: he can't read, so voice
   is required from day one** (see "The no-reading rule" above). Recorded
   voice lines ship in the first prototype; voice *input* is an optional
   experiment in Phase 2–3; full narration polish stays in Phase 5.
   Recorded family voices beat Android TTS for warmth and work offline.
7. ~~Phone or tablet?~~ **Answered: phone.** Landscape, phone-first, layouts in
   normalized coordinates so a tablet would just get bigger.
8. **Counting range for MVP.** The table in §7 goes to 10 at level 5.
   *Default:* start there and tune from observation.
9. **Backups.** Excluding data from cloud backup protects privacy but means a
   lost phone loses progress. *Default:* no cloud backup, allow
   device-to-device transfer, and later a parent-mode "export to file".
10. **Parent gate strength.** A "hold" gesture is beatable by a determined
    4-year-old. *Default:* hold a corner for 3 seconds, then a simple adult
    check (e.g. "tap the numbers 7, 2, 9 in order", spoken nowhere). It's a
    speed bump, not security, and nothing behind it is sensitive.
11. **Themes vs. subjects on the map.** §15 maps areas to subjects (Forest =
    counting) while the MVP theme is dinosaurs. *Default:* keep theme and
    skill independent in data; an area is a list of templates. Which subject
    lives where is a content decision, not code.
12. ~~Healthy play length?~~ **Answered: 5 rounds.** After 5 rounds (about
    10–15 minutes) the dino yawns and "goes to sleep", a gentle natural ending
    instead of an endless loop. Parents can change it later.
13. **AI art consistency and animation.** Image generators produce flat
    raster images with drifting style, and a single picture can't be animated
    into blinks and bounces. *Default:* use AI for concepts, then make one
    clean layered master (traced to vector if possible) and reuse it.
14. ~~Distribution?~~ **Answered: install builds from GitHub now, publish on
    F-Droid eventually.** Every push to `main` publishes an installable APK
    as the "latest" GitHub release. F-Droid needs a free-software licence,
    no proprietary libraries (we have none), and a build from source; see
    decision #31.
15. ~~Are letters core?~~ **Answered: numbers and letters are the key
    objective.** Letters move from Phase 4 into Phase 2 (see "Learning goals").
16. **His name inside the app.** Starting letters with his name means the
    app knows his name. *Default:* typed in parent mode, stored only on the
    phone, never in the content pack or the repo.
17. **Repo name vs. game.** The repo is "dungeonquest", the game is a dino
    valley for a preschooler. Harmless, but the app name and package should
    be decided before installing on anyone else's device.

---

## 13. Playtest feedback tool

During a playtest you're watching a 4-year-old, not taking notes. Reporting a
problem has to take **one gesture and a few seconds**, and the report has to
contain enough to reproduce the problem later without asking "what was on
screen?".

### Capturing a report

```text
Two-finger press and hold for 2 s (anywhere, any screen)
        │   game pauses; the dino "freezes" mid-pose
        ▼
Report sheet (adult-sized, behind the hold)
   ┌─────────────────────────────────────────────────────────┐
   │  [screenshot thumbnail]                                 │
   │  What kind?   🔊 Sound   🖼 Looks wrong   😕 Confusing    │
   │               😣 Too hard  🥱 Too easy   🐞 Broken  ⭐ He loved this │
   │  🎙 Hold to record a voice note (up to 30 s)            │
   │  [ Save & keep playing ]                                │
   └─────────────────────────────────────────────────────────┘
```

- **Two-finger hold**, not a button. A visible button gets tapped by a
  preschooler; a deliberate two-finger hold rarely does. The sheet itself
  needs a tap on a tag or "Save", so an accidental trigger costs nothing.
- **Tags are one tap**, and there's a positive tag. Knowing what he loved is
  as valuable as knowing what broke.
- **A voice note** is the fastest way to describe an audio or animation
  problem ("the 'four' clip is cut off", "he thought the volcano was a
  button"). It needs the `RECORD_AUDIO` permission, the same one voice
  answers use. Notes are saved only on the phone, and only when an adult
  records one.
- **Shake to report** is a fallback. Some parents prefer it, but shaking also
  happens when a child holds the phone, so it's off by default.

### What a report contains (captured automatically)

| Item | Why |
|---|---|
| Screenshot (PNG) | "Looks wrong" bugs need to be seen. |
| Current screen + `SessionState`, template id, level, **seed** | Reproduce the exact question in a test (decision #9 stores seeds for this). |
| Last ~100 events from an in-memory ring buffer: taps (with position), session events, audio clips started/finished, animation start/end | Explains audio overlaps, missed taps, "it skipped a step". |
| Learner snapshot: skill levels and recent results | Explains "too hard / too easy". |
| App version, content version, device model, Android version, volume level, whether audio was muted | Half of all "no sound" bugs are the volume. |
| Tags, voice note, time | What the adult saw. |

Crashes are captured too. A `CrashRecorder` installed in the `Application`
class writes the stack trace plus the same event buffer to a report before
the app dies. On the next launch, a small badge in parent mode says "1 new
report".

### Where reports go

Everything stays on the phone until an adult sends it. Reports are saved as
folders in app-private storage:

```text
files/playtest/2026-10-04_10-32-15/
├── report.json        tags, state, events, learner snapshot, device info
├── screenshot.png
├── voice-note.m4a     (if recorded)
└── crash.txt          (if it was a crash)
```

Parent mode has a **Playtest reports** list. Each report can be viewed,
deleted, or shared. **Share all** zips them and opens Android's share sheet,
so you can send them to yourself by email, Quick Share to a laptop, or to Google
Drive. Sharing goes through the share sheet's own apps, so our app still has
no INTERNET permission.

From there, a report becomes a GitHub issue using the `playtest` issue
template (screenshot, tags, steps, seed). You can also drop the zip into this
project and ask Claude to triage it. A debug-only "replay report" screen
loads `report.json` and regenerates the exact question from its seed.

### How it fits the architecture

- `:engine` needs nothing new. `SessionEvent`s and seeds already exist.
- `:app` gets a `playtest` package: `EventRecorder` (ring buffer fed by the
  ViewModel, `Narrator`, and a pointer-input modifier on the root),
  `FeedbackCapture` (screenshot via `PixelCopy` + snapshot), `CrashRecorder`,
  `FeedbackExporter` (zip + `FileProvider` + share intent), and the report
  sheet composable.
- **On in every build.** This is a family app, and playtests happen on the
  real build. A parent setting can turn the gesture off later.
- **No new dependencies.** Screenshot, zip, audio recording, `FileProvider`
  and the share sheet are all built into Android and AndroidX core.

### When

- **First prototype:** the `CrashRecorder` and a two-finger hold that saves
  a screenshot plus state. During playtests, also use Android's built-in
  **screen recorder** (it captures audio, taps and his reactions for free).
- **Phase 2, first item:** the full tool (tags, voice notes, report list,
  share). It goes first because every later feature is tuned through it.

---

## How we'll keep this understandable

- `PROJECT_DECISIONS.md` gets a new entry for every choice that someone
  might later ask "why did we do it this way?"
- One activity type = one folder in `:engine` + one composable. If you can
  understand one, you can understand all of them.
- The engine has no Android in it, so its tests read like the rules of the
  game.
- `PLAYTEST_LOG.md` keeps the real tester's behaviour next to the code it
  should change.
