# Voice bible

Who speaks in The Little Dungeon, how each voice is made today, and what a person has to decide before it gets better. This replaces the
old recording script for the counting prototype (every line is now made by the build, so nobody has to record thirty lines to start).

## How a voice is made

1. Every sentence the game can say is listed by the engine (`./gradlew :engine:voiceLines`, built by `VoiceCatalog`).
2. The build records each one with **Kokoro v0.19** (through sherpa-onnx, `scripts/render-voice.py`): one of its 11 speakers, a speed, and a
   pitch change by resampling. Held letter sounds ("mmm", "sss") are made from scratch (`scripts/letter_sounds.py`), and the dragon's growl is a
   sound effect (`scripts/make-growl.py`).
3. A recording is named by a fingerprint of the voice and the sentence (`Voice.key`), cached between builds, and played by the phone.
   A sentence that is not recorded (one with a typed dragon name, say) is made on the phone, slowly.
4. In the text, `<pet>`, `<wizard>`, `<growler>` and the like change who speaks until the next tag; `{name}` is the dragon's name; `[creak]` is a
   sound effect. Only the character who is speaking moves their mouth.

Changing a voice's speaker, speed or pitch changes its id (`Who.voiceId`), so every one of its lines is recorded again. The narrator's must
never change by accident.

## The cast, as it is

The voices are `Who` in [`Speech.kt`](../engine/src/main/kotlin/com/littledungeon/engine/model/Speech.kt). Characters that share a speaker are told
apart only by speed and pitch.

| Voice (`Who`) | Kokoro speaker, speed, pitch | Who speaks with it |
|---|---|---|
| Narrator | 1, 0.9, 1.0 | The story |
| Pet | 8, 0.88, 1.0 | The baby dragon (called by the name a grown-up types) |
| Elder | 9, 0.88, 1.0 | Professor Hoot, Hermit Hazel, Old Merlo |
| Baron | 9, 0.85, 0.9 | Baron Grumblewick |
| Granny | 2, 0.88, 0.92 | Healer Willow |
| Townswoman | 2, 0.95, 1.0 | Mayor Tilly, Henrietta Hen |
| Townsman | 5, 0.95, 0.95 | Brogan, Finn the Fisherman |
| Merchant | 0, 1.0, 1.0 | Baker Bun, Zig the Merchant |
| Guard | 6, 0.95, 1.0 | Captain Hob |
| Child | 4, 0.95, 1.0 | Lumi, Fern the Fairy |
| Sneak | 0, 1.0, 1.1 | Rascal the Fox, Bandit Bess, the Sneaky Fox, Inky Imp |
| Growler | 10, 0.9, 0.75 | Grumble, cave trolls, moon wolves, the Mossy Golem |
| Critter | 6, 1.0, 1.12 | Sir Ribbit, Otto the Otter, slimes, bats, spiders, boars, crabs, the Spider Queen and the Bat King |
| Spook | 9, 0.85, 0.8 | Skeletons, ghosts, Captain Rattlebones |
| Shadow | 6, 0.9, 0.8 | The Ink Shadow |
| Dragon | 10, 0.9, 0.85 | The big dragon |
| Wizard, Goblin, Ruby | 3, 5 and 7 | From the first dungeon; not used by the journey yet (their stories are the first to port, see `docs/design/story-variations.md`) |

## What is wrong with it

- **Too few voices for twenty characters.** Neighbours in Mossbrook share a speaker, Sir Ribbit sounds like a slime, and the Baron sounds like
  Professor Hoot. Kokoro v0.19 only has 11 speakers.
- **Pitch by resampling** changes speed and formants too, and gives raised voices (critters, 1.12) a slight metallic edge.
- **Every line has the same energy.** There is no whisper, shout, sleepy or excited delivery.
- **The dragon's name is made on the phone**, not recorded (a grown-up types it), so that sentence sounds a little different.
- **Letter sounds** were wrong for B, D, P, T and A; step 2.5 of the improvement plan fixed the stop sounds and short *a* (tested in `PhonicsTest`).
  The new sounds still need listening to by a person.

## Decisions that need a person

These are not code. The proposal for each is in [`docs/review/2026-10-04-review-and-roadmap.md`](../docs/review/2026-10-04-review-and-roadmap.md),
section 8.

1. **Which engine makes the voices.** Kokoro v1.0 has about 50 voices and can blend two into a new one, so every named character could be
   unique. Recording happens in CI, so a bigger model costs build time, not APK size. (If the on-device voice is dropped, the APK also loses about
   120 MB.)
2. **Whether the family records the lines heard most**: the narrator's welcome, the dragon's catchphrases, "Level up!". A real voice for the
   twenty or so most-heard lines, with the synthetic voice for the rest. This is not wired in yet: recordings would live in the repository and
   be copied over the build's own by `render-voice.py`, which is a small change once someone has recordings.
3. **A casting pass.** One line each on how every named character should feel (the review has a first draft: for example the Baron is dry,
   theatrical and sad underneath, and softer once befriended).
4. **Delivery tags** (`{excited}`, `{whisper}`, `{sleepy}`) once there is an engine that can use them.
5. **Licences.** Kokoro and sherpa-onnx are Apache-2.0, but the espeak-ng data that ships with the voice is GPL-3.0; this must be settled before
   choosing the repository's licence or going to F-Droid (area X3 in `docs/review/improvement-plan.md`).
