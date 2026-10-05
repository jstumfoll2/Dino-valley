# Story variations (keep the game worth replaying)

Standing requirement from the playtest: the game needs many different story lines to play
through, not one story with different names. This file tracks what exists and what is wanted.

## Built so far
- Arc 1, "The Missing Pages" (Baron Grumblewick, the Storybook): the first, fully fleshed-out story. It is told three ways (the lonely Baron, the jealous Baron, the Baron under the ink's spell), with clues along the roads, and it can end by fighting or by listening.
- Arc 2, "The Dragon Who Did Not Want to Fight" (Dragon's Peak; the stone key from Gloomwood Mine; Brogan finds his courage): told three ways (the dragon who collects lost things, the one who cannot sleep, the shy one). The peaceful way is rhymes, sharing and money. What the hero learned on the road (Hazel hears a lullaby in the rumble; a bottle with a note about pies; Grumble's message) spares a rhyme or a sharing puzzle.
- Arc 3, "The Night of a Thousand Lanterns" (the Bat Belfry; glow-silk from the Spider Caves; Lumi): told three ways (the bats are scared of the dark, they were not invited, they think it is a game). The peaceful way is the Bell Song, a glowing path to trace, and sharing berries.
- Arc 4, "Princess Ruby and the Wandering Doors" (the Hall of Doors; the golden doorknob from Gloomwood Mine; Ruby on stage for the whole story): told three ways (the warden lost his keys, he is lonely, a page of the Storybook is rewriting his doors). The peaceful way is the memory doors, Hoot's map and a pattern; knowing why the doors wander spares the memory doors.
- Arc 5, "The Ink Shadow", the finale of a Storybook: told when four pages are home, never before, and followed by the Storybook Ball for the friends the hero made.
- The old story of the dragon's prisoner is not in the new kingdom. The goblin keeps its art and `Who` entry for it.

## Wanted (each is a data entry in the arc library, plus new people, places and art)
- A story where the villain is a friend in disguise and the hero must discover it.
- A story with no boss fight: a festival, a race or a rescue solved by choices and puzzles. (The lantern story is a festival, but still has a boss to talk to.)
- A story where the baby dragon is the one who is lost (needs a journey without the companion).
- A story set mostly underground, or on the water, using a different kind of map.
- Seasonal stories (winter market, harvest). The night of lanterns is built.
- Stories driven by an NPC's personal problem that grows into the main plot.
- More stories that change with the friends the hero made in earlier rounds (the dragon and lantern stories start this: see the rules below).

## Rules for new stories
- Reuse the world: the same towns, shops and people, living their own lives.
- Each story gives a person or creature a story they did not have before, with a backstory and a choice that matters (the dragon and the Bat King had none until now), and the old cast gets new things to say. A story that needs a person who does not exist yet brings their art and voice (`Who`, and `VoiceCastTest` keeps named voices apart).
- Each story can end in more than one way, and the ending is remembered.
- Each story reads at least two things the world remembered from earlier adventures (a friendship made at the bridge or the pass, a gift from Lumi, the Baron made a friend) and shows it as a clue or a greeting. `ArcsTest` checks the count.
- Each story has a clue that spares a puzzle (`skippedBy`), found on the road, so exploring pays.
- Each story's peaceful way is not just the easier way: it asks for a kind of puzzle the fight does not, and a friend made is remembered (a boss made a friend asks for help next time; a beaten one remembers it).
- A story changes what at least one person says afterwards (Lumi, Brogan).
- A new story brings its own key item (with a picture) and, if it needs one, its own lair; a lair must be at least five roads from camp (`KingdomTest`).
