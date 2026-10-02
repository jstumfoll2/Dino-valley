# Architecture Proposal

Status: **Proposed, awaiting your review.** Nothing here is built yet. Every
meaningful choice below is also recorded in [`PROJECT_DECISIONS.md`](PROJECT_DECISIONS.md)
with alternatives and "what would make us change it".

This document answers the twelve questions in section 30 of the spec. It is
written for two people: one who will write most of the Kotlin, and one who will
design the experience. Sections marked **For the experience designer** are the
parts where the creative decisions live.

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

## 1. Recommended Android project structure

```text
dungeonquest/
├── README.md
├── ARCHITECTURE.md                 ← this file
├── PROJECT_DECISIONS.md            ← decision log
├── PLAYTEST_LOG.md                 ← what the real tester did (start on day 1)
├── art-bible/                      ← style guide, palette, character sheets (not shipped)
├── content-tools/                  ← prompts we give an AI to draft content, review checklist
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
| `activity` | One sub-package per activity type: generator + evaluator + hint ladder | `count/CountObjectsGenerator.kt`, `count/CountObjectsEvaluator.kt`, `choosenumber/…`, `compare/…`, `ActivityRegistry.kt` |
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
| `ui.play.activities` | One composable per activity type: `CountObjectsActivity`, `ChooseNumberActivity`, `CompareGroupsActivity` |
| `ui.reward` | `RewardScreen`, `StickerBookScreen` |
| `ui.parent` | `ParentGate`, `ParentScreen` |
| `data.content` | `AssetContentSource` (reads JSON from `assets/`), `SpriteCatalog` (sprite id → drawable) |
| `data.learner` | Room database, DAOs, entities, `LearnerRepository` |
| `audio` | `SoundPlayer` (SoundPool for short effects), `Narrator` (prompt key → audio clip) |

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

@Serializable @SerialName("CHOOSE_NUMBER")
data class ChooseNumberSpec(
    val sprites: List<SpriteId>,
    override val levels: List<ChooseNumberLevel>,
) : ActivitySpec

@Serializable
data class ChooseNumberLevel(
    override val level: Int,
    val minNumber: Int,
    val maxNumber: Int,
    val choiceCount: Int,
    val showDotsUnderNumerals: Boolean, // scaffolding that fades out at higher levels
    val mode: ChooseNumberMode,         // FIND_NUMERAL ("Which is 3?") or MATCH_QUANTITY
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
@Serializable enum class ChooseNumberMode { FIND_NUMERAL, MATCH_QUANTITY }
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

data class ChooseNumberInstance(/* common fields… */ val target: Int, val choices: List<Int>,
    val showDots: Boolean, val mode: ChooseNumberMode) : ActivityInstance

data class CompareGroupsInstance(/* common fields… */ val leftCount: Int, val rightCount: Int,
    val askFewer: Boolean) : ActivityInstance
```

### What the child does, and how it went

```kotlin
sealed interface ChildResponse {
    data class NumberChosen(val value: Int) : ChildResponse
    data class GroupChosen(val side: Side) : ChildResponse
}
enum class Side { LEFT, RIGHT }

/** Which kind of help to give next. Ordered from lightest to strongest. */
sealed interface Hint {
    data object TryAgain : Hint                         // "Hmm, let's look again!" + gentle wiggle
    data object CountTogether : Hint                    // app counts aloud, highlighting each object
    data class NarrowChoices(val keep: List<Int>) : Hint // only 2 buttons left, right one glows softly
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
    └── AppNavigation                     (weekend: a simple `when(screen)`; Phase 2: Navigation Compose)
        ├── HomeScreen                    big Play button, character waving, sleepy/awake by time of day
        │     └── ParentGate (hidden: hold top-right corner 3 s) → ParentScreen
        ├── MapScreen                     Phase 2: Dino Valley with 3–4 stops, locked stops are "foggy"
        ├── PlayScreen  ← PlayViewModel
        │     ├── SceneBackground         layered background, gentle parallax/sway
        │     ├── Character               state: Idle, Talking, Thinking, Cheering, Encouraging
        │     ├── ActivityHost            `when (instance)` → picks the composable:
        │     │     ├── CountObjectsActivity
        │     │     ├── ChooseNumberActivity
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

| Level | Counting (`COUNT_OBJECTS`) | Number recognition (`CHOOSE_NUMBER`) | Comparing (`COMPARE_GROUPS`) |
|---|---|---|---|
| 1 | 1–3 big objects in a line, tap to count, 2 choices | Numerals 1–3 with dots underneath, 2 choices | 1 vs 4 ("more"), unlocks at counting L2 |
| 2 | 1–5 in a line, 3 choices | 1–5 with dots, 3 choices | Difference ≥ 2, up to 5 |
| 3 | 1–5 scattered | 1–5, no dots | Difference ≥ 1, up to 5 |
| 4 | 1–7 scattered + 1–2 distractors | Match quantity to numeral, up to 7 | Ask "fewer" sometimes |
| 5 | 1–10, clusters, no tap-to-count aid | 1–10 | Up to 10; bigger objects in the smaller group |

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

- **Weekend prototype uses no database at all.** Progress is in memory. Room
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

### Skills and prompts

```json
[
  { "id": "counting",           "domain": "MATH" },
  { "id": "number_recognition", "domain": "MATH", "unlockedWhen": [{ "skill": "counting", "minLevel": 2 }] },
  { "id": "comparing",          "domain": "MATH", "unlockedWhen": [{ "skill": "counting", "minLevel": 2 }] }
]
```

```json
{
  "count.how_many":        { "text": "How many eggs?",            "audio": "count_how_many" },
  "count.eggs.intro":      { "text": "Mama Dino's eggs rolled away! Let's count them.", "audio": "count_eggs_intro" },
  "common.count_together": { "text": "Let's count together!",     "audio": "count_together" },
  "number.1":              { "text": "one",                       "audio": "n1" }
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
  `requiresReading` is false for MVP, and so on. A unit test runs it on the real
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
| Compose BOM, `ui`, `foundation`, `material3`, `ui-tooling-preview` | Weekend | The UI toolkit you chose. BOM keeps Compose versions consistent. | It *is* the native toolkit. | Low; one version to bump. |
| `activity-compose` | Weekend | Hosts Compose in `MainActivity`. | No. | Low. |
| `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | Weekend | ViewModels survive rotation; `collectAsStateWithLifecycle`. | Partially, with more code. | Low; Jetpack. |
| `kotlinx-coroutines-core` | Weekend (engine) | `StateFlow` for session state, Room's suspend APIs later. | No; it's Kotlin's official async library. | Low. |
| `kotlinx-serialization-json` + plugin | Phase 2 (content pack) | Typed JSON → data classes, including the sealed `activity` types. Works in pure-Kotlin `:engine`. | `org.json` is built into Android but is untyped, verbose, and not available in a JVM module. | Low; JetBrains. |
| Room (`runtime`, `ktx`, `compiler`) + KSP plugin | Phase 2 | Learner database: typed queries, migrations, Flow. | Raw `SQLiteOpenHelper` works but means hand-written SQL plumbing and migrations. | Low-medium; migrations need care when schema changes. |
| `navigation-compose` | Phase 2 (optional) | Back stack, system back handling, per-screen ViewModels once there are 5+ screens. | A `when(screen)` + `BackHandler` is fine for 2–3 screens, which is why the weekend skips it. | Low. |
| JUnit, `kotlinx-coroutines-test`, Compose `ui-test-junit4` | Weekend | Tests. | No. | Low. |

**Deliberately not used:** Hilt/Dagger (manual DI is enough), Retrofit/OkHttp
(no network), Firebase/analytics/crash SDKs (privacy), Lottie (Compose
animation is enough; revisit if the designer wants After Effects animations),
Coil/Glide (no remote images), DataStore (Room table instead), any game
engine, any TTS/AI SDK. Sound uses Android's built-in `SoundPool` and
`MediaPlayer`.

**Platform settings:** minSdk 26 (Android 8, covers effectively every device
still in a house), target/compile the latest stable SDK, Kotlin 2.x with the
Compose compiler Gradle plugin.

---

## 11. Realistic first-weekend plan

**Goal: a delightful 2-minute toy, not a foundation.** Success test: *does he
grab the phone and ask to play again?* We cut everything except what makes one
counting game feel magical, but we still put the counting logic in `:engine`
so the weekend's work isn't thrown away.

### Before the weekend (an evening, mostly the experience designer)

- **Pick the character with your son.** Show him 3–4 AI-generated concepts
  (dino variations), let him choose and name it. He'll care more about a
  friend he named.
- Lock a tiny style: flat shapes, thick soft outlines, 5-colour palette, big
  eyes. Write it as the first page of `art-bible/`.
- Decide the round's story in one sentence: *"Mama Dino's eggs rolled away;
  help her count them back into the nest."*

### Saturday

| Who | Morning | Afternoon |
|---|---|---|
| **Developer** | New project from Android Studio's Empty Activity template; add `:engine` module; landscape, full-screen, no INTERNET permission. In `:engine`: `CountObjectsGenerator` (levels hard-coded in Kotlin, no JSON yet), evaluator with the 3-step hint ladder, `GameSession` state machine, unit tests. | `PlayScreen`: background, character, eggs placed from the generated `Scene`. Eggs pop in one by one with a spring. Tapping an egg makes it wiggle, shows a big number above it, and plays the number sound. Big round answer buttons showing numeral **and** dots. |
| **Experience designer** | Final character as **layered parts** (body, head, 2 eye states, 3 mouths: smile, open "ooh", big grin). One background (valley, nest, a volcano puffing in the distance). Egg sprites in 2–3 patterns. | Record voice lines on a phone in a quiet room: "Let's count!", "one" … "five", "How many eggs?", "Let's count together!", "You did it!", "Yay!", a giggle. A few sound effects (pop, boing, chime). |

### Sunday

| Morning | Afternoon |
|---|---|
| **Make it feel good.** Correct: character squash-and-stretch jump, **the eggs hatch into that many baby dinos** (the reward *is* the quantity), leaf/star burst, chime. Wrong: character tilts head, "hmm", eggs gently re-count themselves with highlights, try again; third try leaves two choices. Idle: blink, breathe, look at whatever was tapped. A round = 5 questions; a progress nest with 5 spots fills up; end with a big hatch party and a "play again?" button that's just a giant egg. | **Playtest with your son** (the most important hour). One parent plays nothing, just watches and writes in `PLAYTEST_LOG.md`: where he tapped first, what he said, what made him laugh, where he looked confused, whether he asked for another round. Then fix the *one* thing that mattered most. |

### Explicitly not this weekend

Map, Room, JSON content, difficulty engine, the other two activity types, parent
mode, settings. The back button simply returns to the home screen; use
Android's built-in **app pinning** during playtests so little hands can't
leave the app.

### Phase 2 order (for orientation)

JSON content pack + validator → Room + learner log → difficulty policy and
`RuleBasedLearnerModel` → `CHOOSE_NUMBER` → `COMPARE_GROUPS` → sticker book
→ a three-stop Dino Valley map → minimal parent screen.

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
6. **Audio can't wait for Phase 5.** A non-reader cannot know what "Which
   group has more?" means without hearing it. *Default:* a handful of
   recorded voice lines from day one; full narration polish stays in Phase 5.
   Recorded family voices beat Android TTS for warmth and work offline.
7. **Phone or tablet? Whose device?** Affects layout, touch target sizes, and
   art resolution. *Default:* landscape, designed for a phone, layouts in
   normalized coordinates so a tablet just gets bigger.
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
12. **Healthy play length.** Not in the spec. *Default:* after N rounds
    (parent setting, default 4) the character yawns and "goes to sleep", a
    gentle natural ending instead of an endless loop.
13. **AI art consistency and animation.** Image generators produce flat
    raster images with drifting style, and a single picture can't be animated
    into blinks and bounces. *Default:* use AI for concepts, then make one
    clean layered master (traced to vector if possible) and reuse it.
14. **Distribution.** Installing from Android Studio onto family devices
    needs no Play Store account. If you ever publish on Google Play, its
    Families policy applies; the no-network, no-SDK design already fits it.
15. **Repo name vs. game.** The repo is "dungeonquest", the game is a dino
    valley for a preschooler. Harmless, but the app name and package should
    be decided before installing on anyone else's device.

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
