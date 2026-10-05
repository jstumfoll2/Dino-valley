# Adding to Whisperwood

Everything in the world is data. Add a `ContentPack` (see `engine/.../rpg/content/Content.kt`) or add to
the core lists, then add the pictures. Content is built once into `Content`'s registry and looked up by id;
an id used twice fails when the content is put together. These tests fail with the name of whatever is missing or broken:

- `ArtCoverageTest`: content that has no picture.
- `ContentRegistryTest` and `KingdomTest`: ids, the map is one connected world, every lair and dungeon can be reached.
- `StoryMemoryTest`: the flag rule (below).
- `VoiceCatalogTest`: every sentence a journey can say is in the voice catalog (below).

| To add | Data | Pictures (in `art/src/`, built by `art/build.py`) |
| --- | --- | --- |
| A person | `Npc` in `CoreNpcs` (backstory, `DialogNode`s with `Option`s, `Start`s) and a home `Location.residents` | `art_npc_<id>_{shadow,body,eye_open,eye_closed,mouth_calm,mouth_talk}` (`cast.py`) |
| A monster | `Monster` in `CoreMonsters` (hp, attack, skills, voice, weakness). Set `roams = false` for anyone with a story of their own or a place to guard: a roaming monster can turn up on any road of its terrain | `art_monster_<id>_*` (`creatures.py`) |
| An item | `Item` in `CoreItems` (kind, price, slot, tool `opens`, charm `guess`) | `art_item_<id>` (`items.py`); gear also `art_gear_<id>` drawn over the hero (`gear.py`) |
| A place | `Location` in `CoreKingdom` with `x` and `y` (0 to 1) on the map, plus `Road`s | `art_scene_<theme>` (`world.py`). The map has no spots to paint: places are markers the app draws from `x` and `y` |
| A shop | `ShopDef` in `CoreShops`, listed in a `Location.shops` | none (uses item icons) |
| A story | An `Arc` in `CoreArcs`; see the checklist below | whatever new people and places it brings |
| A friendship that opens a road | `Content.flagRoads` | none |

## Flags

A flag is a fact the world remembers: `SetFlag("friend:wolf_pup")` in an effect, `Cond.Flag` or `Cond.NoFlag` in a condition. A flag
whose name starts `run:` lasts for one adventure; every other flag is kept between adventures. **Every flag that is set must be read
somewhere, and every flag that is read must be set somewhere**: `StoryMemoryTest` fails otherwise, because a remembered fact that changes
nothing is a promise to the child that nothing keeps. Friendships (`Relation`) are numbers, and each point earns stars of kindness.

## A person's own puzzle

`Effect.Puzzle` can name its own skill, setup words and the thing counted, so a person's puzzle fits their story (Henrietta counts her
chicks; Hazel draws a pattern riddle). Without those it wears the usual costume of the obstacle. Their own puzzles are one try and have no tool
that gets past them; what happens next is theirs to say.

## A story (an `Arc`)

1. Name where it ends (`lairId`), the dungeon that must be explored (`keyDungeonId`) and the item its guardian holds (`keyItemId`), and the boss.
2. `setup` (told at camp), `returnSetup` (shorter, when it has been played), `sealed`, `keyFound`, `gateOpens`, `ask`.
3. At least one `Variant` (meeting, fight ending, peace ending) so the story can be told more than one way.
4. `peaceSteps`: the puzzles the boss sets on the peaceful way; `skippedBy` a flag the hero learned along the road lets one be skipped.
5. `friendMeeting`, `friendSteps` and `friendEnd` for a boss who was made a friend before; `rivalMeeting` for one who was beaten.
6. `moments`: clues and scenes shown the first time the hero reaches a place. A story that leaves nothing on the roads is a story told only
   at the end.
7. At least one new person with a backstory and a choice that matters, and every conversation needs a peaceful way out.
8. `minChapter` to keep it for later in the campaign.

Rules for stories: see `story-variations.md`.

## Voices and the words

A new character that speaks needs a `Who` entry (`Speech.kt`); every line is recorded by the build from the voice catalog, so nothing
else is needed. The catalog finds sentences by playing simulated adventures and by enumerating small domains, and `VoiceCatalogTest`
plays fresh adventures to prove nothing was missed. To keep that true:

- A sentence that holds a number never also holds a name (a monster's, a person's, an item's); put them in different sentences.
- Never build one sentence from a list of whatever the hero holds; each combination would be a new recording.
- Lines that hold a number are listed in `JourneyLines.numbered`.
- Plain lines (no input) in `JourneyLines` and `RoomLines` are all found by reflection; lines that take a name, a monster or a place are
  enumerated for every one of them in `VoiceCatalog`. A new line with another kind of input needs its domain added there.
