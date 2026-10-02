# The Little Dungeon: how the first dungeon works

This turns the brief ([little-dungeon-brief.md](little-dungeon-brief.md)) and Jason's ask for
randomness, roguelike runs and level-ups into something we can build. Everything is spoken;
the player is four and can't read yet, so every word on screen is also said aloud and every
answer is a picture.

## One adventure = one run (about 10 minutes)

```
 Hero's camp ─▶ Entrance ─▶ ⟨pick a door⟩ ─▶ Room ─▶ ⟨pick a door⟩ ─▶ Room ─▶ ⟨pick a door⟩ ─▶ Room ─▶ Boss ─▶ Treasure & level-up
```

- **The map is new every time.** Each run builds a small dungeon. After the entrance there are
  three forks with two colored doors each, then the boss. That's five rooms per run, inside
  the brief's three to five. The child picks a door at each fork on a picture map. Some forks
  are the map challenge ("The treasure smells like it's behind the BLUE door").
- **Rooms are dealt like cards.** Each room type pairs a story obstacle with a learning
  mechanic. The learning engine deals the rooms, favoring skills that need practice, while
  keeping every run varied.

| Room | Story obstacle | Learning |
|---|---|---|
| Rune Door | "The ancient door opens only when its symbols are in order." | Patterns |
| Broken Bridge | "The bridge needs 5 stones. You have 3." | Counting → addition |
| Crystal Cave | "The wizard needs the BLUE crystal." | Colors (→ color + size) |
| Spell Library | "The spell begins with the sound mmm." | Letters → phonics |
| Dark Tunnel | "Draw the magic path to the glowing crystal." | Tracing (line → curve → zigzag → loop) |
| Mirror Hall | "Remember the GREEN door." | Memory |
| Alchemist's Workshop | "Brew a Potion of Giant Strength." | Recipe: colors, counting, order, stirring |
| Goblin Den | "The goblin is frightened. What should you do?" | Kindness choice (story) |
| Treasure Vault | "A treasure chest! Roll to open it…" | Dice and counting coins |

- **The boss is a mini-quest, not a fight to lose.** The boss has three glowing stars to fill.
  Each star is a short challenge from a different skill, and dice rolls help. Friends made
  earlier in the run fill a star for you.

## Randomness and replayability

- **Three storylines from the brief, retold differently each time.** The Dragon's Prisoner,
  The Dragon Who Didn't Want to Fight, and Ruby's Quest each have slots that change from run
  to run:
  - the dungeon's name ("The Mossy Caverns", "The Whispering Vaults")
  - the goblin's name and mood
  - which twist is true (Ruby is visiting the dragon; a trickster framed the dragon; Ruby
    got lost and the dragon kept her warm)
  - the treasure
  - the funny events
- **Dice everywhere it's fun.** 1–2 makes something silly happen ("your hat is now stuck to
  the ceiling") and the story keeps going. 3–4 is a good result, 5 is great, and 6 is
  magical, with extra treasure. When the child is ready for addition, the game asks for the
  roll plus the hero's bonus ("4 + 2").
- **Roguelike, kid-sized.** Each run gets fresh items: a lantern, a magic key, potions,
  coins. They are used up within the run. Nothing is ever lost forever, and there is no
  game over.

## Choices that matter

- **Doors at each fork:** which rooms you get.
- **The goblin:** sharing your snack, singing a song, or tiptoeing past each lead
  somewhere different. A goblin you helped comes back to help at the boss.
- **The boss:** make friends or cast a spell. The ending, the reward and what the world
  remembers all change. The game never says which choice is "right."
- **Potions:** a potion brewed in the workshop can solve a later problem in its own way
  (Giant Strength lifts the stone door).

## The world remembers, and the story notices the player

Between runs the game remembers:

- friends made (the goblin, the dragon)
- the endings seen
- treasure collected
- what the child is best at

The narrator and the companion use these in later runs:

- "Pip the goblin waves! He remembers you shared your snack."
- "The runes glow when you come near. You're a pattern master!"

## Growing stronger: XP and levels

- Every challenge, choice and roll earns **stars (XP)** for one of the five attributes from
  the brief:
  - ❤️ Courage: dice and the boss
  - 🧠 Cleverness: counting and math
  - 📖 Wisdom: letters and maps
  - 💛 Kindness: helping choices
  - ✨ Magic: patterns, potions and tracing
- Collecting stars raises the **hero's level**, celebrated with fanfare. Each level unlocks
  something to see or use:
  - a new hat or cape
  - a bigger dice bonus
  - a companion trick
  - eventually new classes: Guardian, then Spellkeeper
- Levels are never shown as grades. They are the adventurer getting stronger.

## Adaptive learning (rules now, ML-ready later)

- Each skill has its own level, 1–5:
  - counting
  - addition
  - colors
  - patterns
  - letters
  - tracing
  - maps
  - memory
  - recipes
- **Rule:** two first-try wins in a row raise that skill one level. A challenge that needed
  the strongest hint lowers it one level.
- The same room asks for different things at different levels. For example, the Rune Door
  goes from 🔴 🔵 🔴 ? to 🔴 🔵 🔵 🟢 🔴 🔵 🔵 ?
- Every challenge is logged on the phone with its type, difficulty, tries, hints, time and
  whether it was right first try, ready for a small on-device model later. Nothing leaves
  the phone.

## Mistakes are story events

- **First miss:** something funny happens, then try again. The rune door sneezes dust; the
  potion turns polka-dotted.
- **Second miss:** the companion helps and fewer choices are left.
- **Third miss:** the right answer glows.

Every room ends in success.

## Code layout

These are four separate parts, as the brief asks:

- `engine/rpg/learn`: the learning engine, which generates challenges and tracks skills.
- `engine/rpg/world`: the dungeon generator, quests and world memory (the narrative engine).
- `engine/rpg/run`: the game mechanics, with the run state machine, dice and items.
- `engine/rpg/hero`: the progression system, with XP, levels, attributes and unlocks.

The app draws whatever "beat" the run is on (narration, challenge, choice, dice, map or
reward) and sends back what the child did.
