# Plan for issue #13: story, characters, world map, battles, loot

Status: **approved with changes (see "Decisions from review"); building.**

## Decisions from review (these override anything below that disagrees)

1. **No emoji placeholders.** Every new character, monster, item and piece of gear gets painted
   art in the existing house style, made as it is developed (SVG sources in `art/src/`, built to
   WebP by `art/build.py`).
2. **Battles have several rounds**, as many as the monster's strength calls for.
3. **The Baron story comes first and is fleshed out fully, but the game must have replayability.**
   Different story lines to play through are a stated requirement, tracked in
   `docs/design/story-variations.md`. The framework is built so a new story is data.
4. **Fainting costs a quarter of the coins.** Confirmed.
5. **The world is open and interconnected.** Characters, items and places are open-ended
   libraries we keep adding to. Every person has their own life, story and problem; their
   outcomes feed the world state (flags, relationships, items, which roads are open) and can
   change the player's path. Built as small data-driven "storylets" any person or place can carry.
6. **Two tries through items, not freely.** One guess by default. Items (Lucky Clover, Owl Feather,
   Hint Scroll) allow a second guess, or remove wrong answers. He should learn he can't guess
   until it's right. Difficulty knobs stay open (easy to give two tries by default later).
7. **One PR, in playable phases**, as planned.

## What #13 asks for, in one paragraph

Turn the game from a linear string of rooms into a small open-world RPG with a story that
continues from round to round. The player picks the route on a fantasy map; characters have
backstories and dialog with real choices; loot, coins and equipment matter; stats matter; a
battle system with minor and main bosses replaces hearts; wrong answers have consequences
(no second chance); and the story is data, so new stories, monsters and people can be added
without touching the engine.

## Decisions I'm proposing (please push back on any)

1. **One branch, one PR, built in slices.** Each slice below compiles, passes tests and is
   playable. PR base is `playtest-fixes-2` (PR #14) until that merges, then retargeted to `main`.
2. (Superseded by decision 1 above: painted art for everything new.) **Art stays in the current style where assets exist.**
   New monsters, NPCs, items and equipment get no new painted art in this PR. They are shown
   as big emoji on the same round "spot" frames the game already uses, with name plates. The
   existing characters (hero classes, baby dragon, goblin, wizard, Ruby, dragon, shadow) keep
   their painted rigs. Equipment changes the hero's look by drawing the item over the hero's
   head, hand, body and feet. Painted art for everything can follow, replacing emoji one by one.
3. **Wrong answers have consequences, but never a dead end.** One try per obstacle and per
   battle turn. The right answer is shown and explained. In a fight a miss costs health; on a
   road or in a dungeon a miss turns the hero back, unless a fitting item (rope, lantern, hint
   scroll, lucky clover) is used. Fainting in a fight carries the hero to the last town with some
   coins lost; it never ends the run.
4. **Hearts become health numbers.** Max health, attack, defense and discounts come from stats
   and gear, and each stat has a clear job (below).
5. **No network, no accounts**, as before. Everything is saved on the phone.

## What the player will experience

**Before each round.** The title screen sets up the overall story (first time) and then a
short "chapter" scene says how this round's problem ties into the main goal.

**The overworld.** A parchment fantasy map: roads connecting camp, towns, villages,
dungeons and landmarks, with mountains, forests, hills, a river and lakes drawn between them.
Several routes lead to the dragon's lair. Roads differ: the mountain pass is short and pays
well but is a hard puzzle; the river road is a counting bridge; the forest road has monsters;
the plain road is safe and long. The player taps a place, hears what it is and what the road
there is like, and taps Go. A map hint marks where the story wants them to go next.

**Towns.** Each town has a few people to talk to and one or two places to shop or rest:
shopkeepers, healer, guard, mayor. Coins buy health items, tools and equipment: serious gear
(helm, sword, shield) and funny gear (chicken hat, spoon sword, duck helmet, pillow armor).
Bought gear changes how the hero looks.

**Dungeons.** Most are optional: a few puzzle rooms, a chest, a mini boss. One dungeon per
round is required: its mini boss holds the key item needed to open the lair. Without it the
lair gate stays sealed and the baby dragon points back toward that dungeon.

**Characters.** Each has a backstory, a voice, and unique dialog options. Options can depend on
the hero's stats, items or past choices ("kindness 3: offer a snack"). Choices change the
story: items, coins, friendships, which monsters fight you, shortcuts, and which ending.
People remember you between rounds.

**Battles.** Turn-based. On your turn you pick Attack or an item. Attack is a learning puzzle,
chosen from the monster's skills. A right answer hits; a wrong answer means the monster hits
you. Items heal, stun, block, befriend or damage. Bosses have a weakness item you can win
earlier in the round, so loot matters in the final fight. The peaceful route (talking, gifts,
jokes) remains for the dragon and the shadow.

**Stats do something.** Courage is health. Cleverness powers number attacks, magic powers
color and pattern attacks, wisdom powers letter and map attacks and also shows road dangers and
finds more coins. Kindness gives shop discounts, heals more, and unlocks peaceful dialog
options. Difficulty rises with hero level: puzzles move up a level every three hero levels,
monsters get tougher, and the highest puzzle levels get new, harder ranges.

**The story across rounds.** One overarching campaign: the Great Storybook of the kingdom is
losing its pages and Baron Grumblewick is collecting them. Each round is a different arc
that recovers a page. After five pages the campaign finale plays, then a new book begins.

## Slices (each is committed, tested, and playable)

| # | Slice | What you can check afterward |
|---|-------|------------------------------|
| 1 | **Foundations**: items and equipment library, hero stats (health, attack, defense, discounts), coins/bag/worn saved, monster library, new voices for the new people | Engine tests; hero keeps coins and gear between rounds |
| 2 | **Overworld**: map graph, routes, terrain and danger, scenery, route generation with several ways to the lair and one required dungeon | Tests prove every map has 2+ routes and the key dungeon is reachable |
| 3 | **Adventure rewrite**: travel loop, road events, one-try obstacles with item rescue and turn-back, wild places, dungeons, lair gate | Whole round playable in engine tests with random players |
| 4 | **Battles**: monsters, items in fights, bosses with weaknesses, fainting, loot, peaceful boss route kept | Tests for damage, items, faint, boss weakness |
| 5 | **Towns and people**: hub, shops, inn, dialog system with conditions and effects, NPC library with backstories | Tests that every dialog node is reachable and every effect applies |
| 6 | **Story**: campaign, 6 arcs (the 3 existing plus troll bridge, night bats, lost spellbook), setup scenes, memory between rounds, finale | Tests that the arcs rotate and the campaign advances |
| 7 | **Phone screens**: fantasy map, travel confirm, town hub, shop, dialog portraits, battle HUD, bag and equip screen, hero gear drawn on the character, new finale with stat effects | Compile and look checked by CI and by you on the phone |
| 8 | **Voice and docs**: the build records every new line, decision log, how to add a story, monster or person | CI green; adding a story needs only a data entry |

Slices 1 to 6 are engine work I can fully test here. Slice 7 is phone UI that I can't run here:
CI compiles it, and I'll rely on your playtest notes via the feedback button for how it looks and
feels.

## Extensibility (how "custom story designs" work)

Content is data, in lists the engine reads: `ArcLibrary` (a story: setup, cast, key item,
boss, endings), `NpcLibrary` (person: backstory, voice, emoji, dialog tree), `MonsterLibrary`,
`ItemLibrary`, `Campaign` (ordered chapters and pages). Adding a new story means adding data,
not changing flow code. Dialog trees use small building blocks (option, condition, effect) so
new characters can reuse them.

## Size, and what I'm deliberately not doing

- This is several thousand lines of engine, content and UI, and every new spoken line gets
  recorded by the build. Expect a long first CI run.
- Not in this PR: painted art for new monsters, NPCs and items (emoji placeholders), animation of
  battles beyond hit flashes and numbers, sound effects for every item, multiplayer or
  cloud anything.
- Reading remains optional: everything important is spoken and shown in pictures.

## Risks to know about

- **Difficulty for a four-year-old.** One-try puzzles can frustrate. Mitigations: the right
  answer is always shown, item rescues, no dead ends, and a simple "easier" setting if playtests
  show tears. Easy to tune because difficulty is numbers in one place.
- **Run length.** Open maps can run long. A direct route stays about as long as today's rounds;
  optional places add time only if chosen.
- **Phone UI without a device here.** Expect a couple of fix rounds after your first playtest.

## Questions for you

1. **Emoji placeholders for new art** (decision 2): acceptable for now, or hold the PR until
   there is painted art for the main monsters and gear?
2. **One try everywhere?** I plan one try for battles, road obstacles and dungeon rooms, and keep
   town and shop activities forgiving. OK?
3. **Overarching story**: Baron Grumblewick and the Storybook pages as the main thread. Keep,
   or do you want a different central villain or goal?
4. **Fainting**: carried to the last town, lose a quarter of the coins. Too harsh for him? An
   alternative is to lose nothing and just retry.
5. **Scope of the first PR**: all eight slices, or stop after slice 6 (engine, with the existing
   screens still working) and do the phone screens as a second PR?
