# Art plan

How the game's pictures are made, how a new piece gets checked before it goes in, what has been drawn for the new minigames and
stories (and what was wrong with it the first time), and what is still to draw. This is the art side of
[the improvement plan](review/improvement-plan.md); the review's section 6 explains why the layering rules below exist.

## How the art is made

Every picture is drawn as code, not painted in a tool.

```
art/src/*.py     one module per kind of picture; each defines sprites() and/or scenes() that return SVG text
art/build.py     SVG -> PNG (Chromium, through node and playwright) -> WebP (ffmpeg) -> app/src/main/res/drawable-nodpi/art_<name>.webp
art/preview.py   the same build for a few named pieces, written to art/out/preview so they can be looked at first
```

| Module | What it draws |
|---|---|
| `heroes`, `cast`, `creatures`, `gear` | Layered characters (body, eyes, mouths, hats, arms, shadow), monsters, worn gear |
| `props`, `items` | Props, item icons (160 by 160), the picture icons on dialog choices |
| `dungeon_scenes` | The 1920 by 1080 backdrops of dungeon rooms and the helpers every scene uses (`Doc`, gradients, glow, planks, arches) |
| `world` | The kingdom map painting, road backdrops, one backdrop per place |
| `minigames` | Small pictures for the newer puzzles: bells, lamps, coins, a market stall, map pieces, rhyme cards (see the inventory) |

A piece is found by name: `art_<name>.webp`. People are `npc_<id>_*` and monsters `monster_<id>_*` with the same five face layers,
items are `item_<id>` (and `gear_<id>` when worn), places are `scene_<theme>`. `ArtCoverageTest` fails when a person, monster, item, place or
dialog icon that the content mentions has no picture, so a new pack that forgets its art fails in the tests and not on a child's screen.

Builds are deterministic: every scene has a fixed seed, so a rebuild changes only what the code changed (checked by comparing the
rebuilt world map with the committed one: only the new tower's pixels differed).

### Needs

`node` with `playwright` (and Chromium), `ffmpeg`, and Python 3. On the authoring machine used for this branch the node tools are in
`/opt/node-tools` (`PATH=/opt/node-tools/bin NODE_PATH=/opt/node-tools/node_modules`). A Python environment with Pillow is handy for
contact sheets but not needed by the build.

## The workflow for a new piece

1. **Write it** in the module it belongs to, using that module's helpers. Do not write a new style; borrow the neighbours' gradients and outlines.
2. **Preview it**, never straight into the app: `python3 art/preview.py items item_mountain_key`. Open the WebP and look at it at full size.
3. **Look for the usual faults** (the checklist below), fix them in the code, build again. Repeat until nothing is wrong.
4. **Compose it where it will be used** when it is part of a screen: a backdrop with the hero's area, the caption bar and the answer cards laid over it (the
   mock-ups made for the bell room, the map puzzle and the rhyme cards did this with the real pieces), because a picture can be fine alone and still wrong on stage.
5. **Install it**: `ART_RES=app/src/main/res/drawable-nodpi python3 art/preview.py ...`, or build the whole module with `python3 art/build.py <module>`.
6. Commit the Python and the WebP together, and note what was found in the log below.

### Checklist (what went wrong in practice)

- **Does it read at a glance?** An icon is seen small. Two things that are different in the game's logic (a bell and a lamp, two coins) must differ by shape, not only by color.
- **Color-blind safe.** A hue always comes with a mark (the bells carry a triangle, a plus and a ring).
- **Stray text.** Anything with letters on it must spell them right and be needed. (A doormat once said "WELCOME" with a mirrored letter.)
- **Overlap and layering.** Shadows go under the object, highlights on top; nothing the child must tap sits under something else.
- **Backdrop zones** (see `dungeon_scenes`): bottom left stays open for the hero and the dragon (x 0 to 700, y 450 to 1080); the top centre is the caption bar and
  stays quiet (x 380 to 1540, y 0 to 260); the centre right (x 700 to 1880, y 300 to 1000) is where the answers are drawn and stays a calm mid-tone. No landmark,
  window, bright lamp or character in the answer zone.
- **House style.** A thick warm outline (`#3d2a1c`), a gradient for shading, one white highlight, a soft ground shadow, no pure black.
- **Same on a dark and a light ground.** Icons are viewed on the answer card, on a spot, and in the bag.

## Inspection log

What was drawn on this branch, and what was found when it was looked at. Everything below was rendered, opened and looked at before it was installed.

### Minigame pictures (`minigames.py`, 34 pieces)

| Piece | Found on the first look | Fixed |
|---|---|---|
| Coins (1, 2 and 5) | The shadows did not sit under each coin | Moved under each coin |
| Compass | A label crowded the needle | The label was removed |
| Lamp (off and on) | The flame did not read as a flame | Redrawn as a flame; the halo is drawn on the lit lamp only |
| Doormat (the "mat" rhyme card) | It had text on it, which a child who cannot read yet gets nothing from | The text was removed; a plain woven mat with a border |
| Wig (the "wig" rhyme card) | It read as a heap of pink balls | Redrawn as a powdered wig |
| Bells (red, yellow, blue) | Not a fault: made with a mark on each from the start (triangle, plus, ring), the same marks the color puzzles use | None needed |
| The other pieces (plate, apple, pie, stall, chest, X, map scroll, small bat, twelve more rhyme cards) | Nothing found on the contact sheet | None needed |

### The bell tower and its sounds

- **Backdrop** (`scene_belfry`): the first version had a second window on the right, which sat exactly under the blue bell in the answer zone; it was removed, and the
  pigeons that sat on its sill moved to the yoke of the great bell. The moon had a pink disc next to the crescent (a covering circle that did not match the sky); it is now a mask,
  so only the crescent is drawn. The first spill of moonlight had hard edges and was blurred. Checked with a mock-up of the bell room laid over it with the real bell pictures: the
  bells hang from the beam (their loops touch it) and clear the hero's area.
- **Sounds** (`scripts/make-sfx.py`): three synthesized hand bells at G4, C5 and G5 (392, 523 and 784 hertz), each a clear note with four overtones that die away faster. Their
  pitches were checked by FFT (peaks at 392.0, 523.0 and 784.0).

### Backdrops for the new rooms and the Ball (`dungeon_scenes.py`)

| Backdrop | Found on the first look | Fixed |
|---|---|---|
| `scene_market_stall` (money room) | The awning hid the top shelf, so only the tips of the bottles showed; a dark arched doorway at the right edge sat in the answer zone | The shelf was moved below the awning; the doorway was taken out (a barrel and a crate stay at the far edge) |
| `scene_rhyme_bridge` (rhyme room) | The band under the rope rails read as a grass field, not a gorge | Drawn as a blue gorge with a river far below, and mist on top |
| `scene_bat_cave` (bat cave) | Nothing that needed changing: the stolen lanterns are warm spots high up, the berry bush is at the far edge, and the floor is clear | None |
| `scene_ballroom` (the Storybook Ball) | Nothing that needed changing for a scene with no answers on it. The right window sits where answers would be, but the Ball is only people on stage; the curtains are a little swollen | None |
| `scene_map` (reused for Hoot's map table) | Already drawn; it is the parchment table, calm everywhere, and the screen draws its own grid on it | None |

### Knocker, the doorknob and the Hall of Doors

| Piece | Found on the first look | Fixed |
|---|---|---|
| `monster_knocker` (a talking door on boots, a knocker ring for a nose, a mail slot for a mouth) | The eyebrows slanted down towards the nose, which made a friendly door look angry | Raised the inner ends, so he looks worried and fussy, which is who he is |
| `item_golden_knob` | A curve of shading under the knob read as a smile | Removed |
| World map: a stone archway with doors, loose doors in the grass | Nothing that needed changing; the pin stands on its foot, like the belfry's and the manor's | None |

### Keys for the stories

- **Mountain key** (`item_mountain_key`): first a plain triangle cut in the bow, which read as a "play" button; redrawn as a two-peaked mountain with snow, scaled to sit inside the
  bow, and the halo made fainter. The amber stone says "warm, from the mine".
- **Glow-silk** (`item_glow_silk`): a spool of pale blue thread with a loose curl and a halo. Read clearly as a spool at the answer-card size on the first look.
- **World map**: a belfry tower on the ridge at the Bat King's place (a stone tower, a bell in the arch, a pointed roof with a flag, bats). The first look showed it standing among the pines
  with the pin on its foot, as the manor does; no change needed.

## Inventory

Counts are files in `app/src/main/res/drawable-nodpi` (each layered character is several files).

| Kind | Files | Status |
|---|---|---|
| Backdrops (places, rooms, roads, map) | 43 | Complete for the current content; the belfry, the market stall, the rhyme bridge, the bat cave and the ballroom are new. The floor-line, landmark-box and 20:9 safe-area contract (plan step 4.3) is not applied to the older backdrops |
| People | 108 files (18 people) | Complete |
| Monsters | 120 files | Complete (the Bat King and the Dragon now have a story; Knocker is new) |
| Items | 43 | Complete; the three story keys are new |
| Gear overlays | 20 | Complete |
| Minigame pictures | 34 | Complete for the minigames built so far |
| Dialog and menu icons | 23 | Complete |
| Hero and dragon rigs | 103 | Complete |

## Still to draw

In the order the plan needs them.

| Piece | Needed for | Notes |
|---|---|---|
| A lantern field backdrop and a lit Lantern Hollow at night | "Lumi's Lanterns" as its own place; lanterns "stay lit on the map" after the story | The lamp pictures exist; the scene does not |
| Painted bunny and frog icons, the dice screen's broken heart | Step 4.5 (painted icons replace the last emoji) | Small; same module as the other icons |
| Backdrops redrawn with a floor line, a landmark box and a 20:9 safe area | Steps 4.2 and 4.3 (feet on a common ground, nothing in the answer zone) | A pass over every `scene_*`; needs per-picture foot positions the app can read |
| A map painted from the same data as the roads | Step 4.4 | Needs the places moved first (nine pairs overlap at the current marker size) |
| The Lakeside Isles | "Grumble's Raft" | A new map region; the largest single piece of art left |

Not planned: a different art style. The review judged the house style consistent and kind; what needed work was layering and overlap, not look.
