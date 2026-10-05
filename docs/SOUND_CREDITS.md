# Sound effect credits

All sound effects in `app/src/main/assets/sfx/` are either **CC0 1.0 (public domain dedication)** or were
**synthesized procedurally for this project** (no third-party material, dedicated to the public domain under
CC0 like the rest of the project's own assets). No sound needs attribution, and none carries a CC-BY,
non-commercial or share-alike condition, so the set is fine for F-Droid.

Every file was converted to mono Ogg Vorbis, 44.1 kHz, `-q:a 4`. Leading silence was trimmed, a 20 ms
fade-out was added, and levels were normalized to about -18 dBFS RMS over the audible part with a -3 dBFS
peak ceiling. Very clicky sounds (dice, coins, creak, fizz, unlock) were soft-limited so they are not too quiet.

## Where the found sounds came from

All found sounds were taken from one GitHub repository that redistributes the original packs unchanged,
each with its original licence file:

- Repository: <https://github.com/series-ai/jam-ready-assets>, commit `782e3a09566b4bb3d98fe2ed07f5a8545e6fcfd4`
- The licence was checked in each pack's own licence file in that repository:
  - Kenney packs: `kenney-<pack>/audio/License.txt` says "License: (Creative Commons Zero, CC0)
    http://creativecommons.org/publicdomain/zero/1.0/". Packs used: Interface Sounds 1.0, Impact Sounds 1.0,
    RPG Audio 1.0, Casino Audio 1.1, Foley Sounds 1.0 and Music Jingles 1.1, all by Kenney (www.kenney.nl).
  - Ninja Adventure: `ninja-adventure/2D/top-down-rpg/LICENSE.txt` is the full CC0 1.0 Universal text, and
    `README.md` says "released under the Creative Commons Zero (CC0) license". Authors: Pixel-boy and AAA
    (<https://pixel-boy.itch.io/ninja-adventure-asset-pack>).

Crediting the authors is not required, but it is a nice thing to do: "Sound effects by Kenney (www.kenney.nl)
and Pixel-boy & AAA (Ninja Adventure)."

## Files

Every "Original file" path below is relative to the root of the repository above.

| File | Use | Source | Original file(s) | Author | License |
|---|---|---|---|---|---|
| `tap.ogg` | soft pop on tap | Kenney Interface Sounds | `kenney-interface-sounds/audio/Audio/drop_002.ogg` | Kenney | CC0 1.0 |
| `right.ogg` | correct answer chime (G-D-G rising) | Kenney Interface Sounds | `kenney-interface-sounds/audio/Audio/confirmation_001.ogg` | Kenney | CC0 1.0 |
| `wrong.ogg` | gentle hummed "uh-oh" | synthesized | none (formant-synthesized two-note hum, falling minor third) | project | CC0 1.0 |
| `cheer.ogg` | short happy fanfare (D-E-F#-G) | Kenney Music Jingles | `kenney-music-jingles/audio/Audio (Pizzicato)/jingles-pizzicato_08.ogg` | Kenney | CC0 1.0 |
| `levelup.ogg` | bigger rising fanfare | Kenney Music Jingles | `kenney-music-jingles/audio/Audio (Steeldrum)/jingles-steel_00.ogg` | Kenney | CC0 1.0 |
| `dice.ogg` | dice shake + roll | Kenney Casino Audio | `kenney-casino-audio/audio/Audio/dice-shake-1.ogg` (first 0.6 s) followed by `kenney-casino-audio/audio/Audio/dice-throw-3.ogg` | Kenney | CC0 1.0 |
| `coins.ogg` | coins clinking | Kenney RPG Audio | `kenney-rpg-audio/audio/Audio/handleCoins.ogg` | Kenney | CC0 1.0 |
| `creak.ogg` | door creaking open | Kenney RPG Audio | `kenney-rpg-audio/audio/Audio/doorOpen_1.ogg` | Kenney | CC0 1.0 |
| `unlock.ogg` | lock clicking open | Kenney RPG Audio | `kenney-rpg-audio/audio/Audio/metalLatch.ogg` | Kenney | CC0 1.0 |
| `clunk.ogg` | heavy thing that won't move | Kenney Impact Sounds | `kenney-impact-sounds/audio/Audio/impactWood_heavy_002.ogg` | Kenney | CC0 1.0 |
| `bubble.ogg` | bubbling cauldron | Ninja Adventure | `ninja-adventure/2D/top-down-rpg/Audio/Sounds/Elemental/Bubble.wav` | Pixel-boy & AAA | CC0 1.0 |
| `splash.ogg` | small plip into water | Kenney Foley Sounds | `kenney-foley-sounds/audio/Audio/Water/drip2.ogg` | Kenney | CC0 1.0 |
| `boing.ogg` | cartoon boing | synthesized | none (pitch glide with a decaying 16 Hz wobble and a moving formant, jaw-harp style) | project | CC0 1.0 |
| `poof.ogg` | magic poof + sparkle | synthesized | none (filtered noise puff plus 7 high sine "sparkle" pings) | project | CC0 1.0 |
| `zap.ogg` | small magic zap | Ninja Adventure | `ninja-adventure/2D/top-down-rpg/Audio/Sounds/Magic & Skill/Magic2.wav` | Pixel-boy & AAA | CC0 1.0 |
| `whoosh.ogg` | flapping wings / whoosh | Ninja Adventure | `ninja-adventure/2D/top-down-rpg/Audio/Sounds/Creature/Wings.wav` | Pixel-boy & AAA | CC0 1.0 |
| `ribbit.ogg` | frog "rib-bit" | synthesized | none (two pulsed, formant-filtered croaks) | project | CC0 1.0 |
| `toot.ogg` | little trumpet "ta-toot" | synthesized | none (additive brass tone, C5 then E5) | project | CC0 1.0 |
| `sneeze.ogg` | cartoon "ah-ah-choo" | synthesized | none (formant vowels + noise burst) | project | CC0 1.0 |
| `burp.ogg` | cartoon burp | synthesized | none (low jittered formant voice) | project | CC0 1.0 |
| `raspberry.ogg` | blowing a raspberry | synthesized | none (32 Hz lip-flap pulses over buzz + noise) | project | CC0 1.0 |
| `munch.ogg` | "nom nom nom" | synthesized | none (three crunchy click bursts with a soft thud) | project | CC0 1.0 |
| `giggle.ogg` | cartoon "hee-hee-hee-hee" | synthesized | none (four high /i/ vowel syllables with breathy onsets) | project | CC0 1.0 |
| `stomp.ogg` | two stomping footsteps | Kenney Impact Sounds | `kenney-impact-sounds/audio/Audio/footstep_wood_000.ogg` + `kenney-impact-sounds/audio/Audio/footstep_wood_002.ogg` (0.38 s apart) | Kenney | CC0 1.0 |
| `heart.ogg` | soft pop for losing a heart | Kenney Interface Sounds | `kenney-interface-sounds/audio/Audio/drop_004.ogg` | Kenney | CC0 1.0 |
| `tweet.ogg` | bird chirp (twice) | Ninja Adventure | `ninja-adventure/2D/top-down-rpg/Audio/Sounds/Creature/Bird.wav` (played twice, 0.2 s apart) | Pixel-boy & AAA | CC0 1.0 |
| `rumble.ogg` | stone door rumbling open | Kenney Foley Sounds | `kenney-foley-sounds/audio/Audio/Rocks/stoneDrag2.ogg` | Kenney | CC0 1.0 |
| `fizz.ogg` | fizzing potion | synthesized | none (sparse high crackles, hiss and tiny rising bubble blips) | project | CC0 1.0 |
| `bell_1.ogg` | the Bell Song's low bell (G4, 392 Hz) | synthesized | none (a sine at the note with four faster-fading overtones and a tick of strike) | project | CC0 1.0 |
| `bell_2.ogg` | the Bell Song's middle bell (C5, 523 Hz) | synthesized | none (as `bell_1.ogg`) | project | CC0 1.0 |
| `bell_3.ogg` | the Bell Song's high bell (G5, 784 Hz) | synthesized | none (as `bell_1.ogg`) | project | CC0 1.0 |

The synthesized sounds were generated with Python (numpy and scipy) and contain no sampled material.

## growl.ogg

Made from scratch by `scripts/make-growl.py` (a low rattling buzz shaped by mouth-like filters). No outside sound used.
