# Adding to Whisperwood

Everything in the world is data. Add a `ContentPack` (see `engine/.../rpg/content/Content.kt`) or add to
the core lists, then add the pictures. `ArtCoverageTest` and `JourneyTest` (content validation) fail
with the name of whatever is missing or broken.

| To add | Data | Pictures (in `art/src/`, built by `art/build.py`) |
| --- | --- | --- |
| A person | `Npc` in `CoreNpcs` (backstory, `DialogNode`s with `Option`s, `Start`s) and a home `Location.residents` | `art_npc_<id>_{shadow,body,eye_open,eye_closed,mouth_calm,mouth_talk}` (`cast.py`) |
| A monster | `Monster` in `CoreMonsters` (hp, attack, skills, voice, weakness) | `art_monster_<id>_*` (`creatures.py`) |
| An item | `Item` in `CoreItems` (kind, price, slot, tool `opens`, charm `guess`) | `art_item_<id>` (`items.py`); gear also `art_gear_<id>` drawn over the hero (`gear.py`) |
| A place | `Location` in `CoreKingdom` with x, y on the map, plus `Road`s | `art_scene_<id>` (`world.py`) and a spot on `scene_world_map` |
| A shop | `ShopDef` in `CoreShops`, listed in a `Location.shops` | none (uses item icons) |
| A story | `Arc` with `Variant`s, `Moment`s and `PeaceStep`s in `CoreArcs` | whatever new people and places it brings |
| A friendship that opens a road | `Content.flagRoads` | none |

Voices: a new character that speaks needs a `Who` entry (`Speech.kt`); every line is recorded by the
build from the voice catalog, so nothing else is needed. Lines that hold a number are listed in
`JourneyLines.numbered`; keep numbers out of lines that also name a monster or person.

Rules for stories: see `story-variations.md`.
