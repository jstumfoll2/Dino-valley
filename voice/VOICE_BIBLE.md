# Voice bible

Who speaks in The Little Dungeon, how each voice is made today, and what a person has to decide before it gets better. This replaces the
old recording script for the counting prototype (every line is now made by the build, so nobody has to record thirty lines to start).

## How a voice is made

1. Every sentence the game can say is listed by the engine (`./gradlew :engine:voiceLines`, built by `VoiceCatalog`).
2. The build records each one with **Kokoro v1.0** (`kokoro-int8-multi-lang-v1_0`, through sherpa-onnx 1.13.8, which is the latest release; `scripts/render-voice.py`):
   one of the 28 English speakers (ids 0 to 27 of 54), a speed, and for creature kinds a small pitch change. English words are found by its lexicon
   (`lexicon-us-en.txt`), unknown ones by espeak-ng. Held letter sounds ("mmm", "sss") are made from scratch (`scripts/letter_sounds.py`), and the dragon's
   growl is a sound effect (`scripts/make-growl.py`).
3. A recording is named by a fingerprint of the voice and the sentence (`Voice.key`), cached between builds, and played by the phone.
   A sentence that is not recorded (one with a typed dragon name, say) is made on the phone by the same voice, slowly.
4. In the text, `<pet>`, `<hoot>`, `<bess>` and the like change who speaks until the next tag; `{name}` is the dragon's name; `[creak]` is a
   sound effect. Only the character who is speaking moves their mouth.

Changing a voice's speaker, speed or pitch changes its id (`Who.voiceId`), so every one of its lines is recorded again.

## The cast, as it is

The voices are `Who` in [`Speech.kt`](../engine/src/main/kotlin/com/littledungeon/engine/model/Speech.kt). `VoiceCastTest` keeps every named character on a speaker of
their own and every person in the kingdom on a named voice. Named voices are never pitch-shifted (resampling gives a chipmunk edge).

| Character | Kokoro v1.0 speaker (id) | Feel |
|---|---|---|
| Narrator | af_heart (3) | warm storyteller, unhurried |
| Baby dragon | af_nova (7) | bright, eager |
| Professor Hoot | bm_fable (25) | old, kind, a little fussy |
| Baron Grumblewick | bm_george (26) | dry, theatrical, slow |
| Old Merlo | am_santa (19) | jolly old wizard |
| Hermit Hazel | bf_emma (21) | quiet, wry |
| Healer Willow | bf_alice (20) | gentle grandmother |
| Mayor Tilly | af_alloy (0) | brisk, motherly |
| Baker Bun | af_bella (2) | flustered, cheerful |
| Brogan | am_adam (11) | gruff, kind |
| Zig the Merchant | am_michael (16) | showman |
| Rascal the Fox | am_liam (15) | sly, quick |
| Bandit Bess | af_kore (5) | tough, tired |
| Lumi | af_jessica (4) | young, nervous |
| Sir Ribbit | am_puck (18) | pompous, tiny |
| Grumble the Troll | am_onyx (17) | deep, shy giant |
| Captain Hob | am_eric (13) | steady guard |
| Finn the Fisherman | am_echo (12) | easygoing |
| Fern the Fairy | af_aoede (1) | light, airy |
| Henrietta Hen | af_nicole (6) | soft, fretful |
| Otto the Otter | bm_lewis (27) | low, playful |
| The big dragon | bm_daniel (24), pitch 0.8 | huge, slow |
| The Ink Shadow | bf_isabella (22), pitch 0.8 | whispery |
| Skeletons, ghosts | bf_lily (23), pitch 0.9 | spooky |
| Small creatures | af_sarah (9), pitch 1.1 | squeaky |
| The Bat King | af_sarah (9), pitch 1.3 | tiny, regal, squeaky |
| Knocker the Door Warden | am_fenrir (14), pitch 0.75, slow | creaky, low, a little fussy |
| Princess Ruby | speaker 8 | bold, young; shares the speaker with the old sneak voice (nobody speaks as Sneak now but the Inky Imp) |
| Trolls, wolves, golems | am_fenrir (14), pitch 0.85 | growly |

Speaker choices were made by measuring each speaker's pitch on a test sentence and spreading the characters across the range; **nobody has listened to them yet**, so
the casting is a first draft for a person's ear (the audition clips can be remade with `scripts/render-voice.py`).

## What is wrong with it

- **28 English speakers for about 30 voices** (the Bat King is the newest creature voice, a higher pitch of the small creatures' speaker, and has not been heard either): creature kinds (and the big dragon, the Shadow, spooks) share a speaker with a named character but differ in pitch and pace.
- **Pitch by resampling** changes speed and formants too, and gives raised voices (critters, 1.12) a slight metallic edge.
- **Every line has the same energy.** There is no whisper, shout, sleepy or excited delivery.
- **The dragon's name is made on the phone**, not recorded (a grown-up types it), so that sentence sounds a little different.
- **Letter sounds** were wrong for B, D, P, T and A; step 2.5 of the improvement plan fixed the stop sounds and short *a* (tested in `PhonicsTest`).
  The new sounds still need listening to by a person.

## Still open

1. **A listen.** The cast above was chosen by measurement, not by ear. A person should hear it (and the letter sounds) and say which voices to swap.
2. **Real recordings** for the twenty or so lines heard most (the narrator's welcome, catchphrases, "Level up!"). Not wired in: recordings would live in the
   repository and be copied over the build's own by `render-voice.py`.
3. **Delivery** (whisper, shout, sleepy) once there is an engine that can use it; blending two speakers into a new one (Kokoro supports it) for the creatures.
4. **Licences.** Kokoro and sherpa-onnx are Apache-2.0, but the espeak-ng data that ships with the voice is GPL-3.0; this must be settled before
   choosing the repository's licence or going to F-Droid (area X3 in `docs/review/improvement-plan.md`).
