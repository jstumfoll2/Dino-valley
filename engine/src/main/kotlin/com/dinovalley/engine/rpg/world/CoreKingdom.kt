package com.dinovalley.engine.rpg.world

import com.dinovalley.engine.rpg.world.LocationKind.*
import com.dinovalley.engine.rpg.world.Terrain.*

/**
 * Whisperwood: the kingdom where every story is set. The same towns, roads and people come back
 * every adventure, so the world can be learned and people can live their own lives in it. Stories
 * choose where the lair is and which dungeon holds the key; more places can be added by content
 * packs. The map painting (`art_scene_world_map`) is made around these coordinates.
 */
object CoreKingdom {
    val locations = listOf(
        Location("camp", "Hatchling Camp", CAMP, 0.07f, 0.55f, "Home sweet home: the camp where every adventure begins.", residents = listOf("professor_hoot")),
        Location("meadow", "Sleepy Meadow", WILD, 0.14f, 0.20f, "A quiet meadow full of nodding flowers and sleepy bees.", residents = listOf("henrietta_hen")),
        Location("fairy_ring", "Fairy Ring", WILD, 0.15f, 0.82f, "A ring of mushrooms glows softly in the shade.", residents = listOf("fern_fairy")),
        Location("mossbrook", "Mossbrook", TOWN, 0.25f, 0.38f, "A cozy village beside a bubbling stream.", residents = listOf("mayor_tilly", "baker_bun", "healer_willow"), shops = listOf("bakery", "healer")),
        Location("old_bridge", "Old Stone Bridge", WILD, 0.30f, 0.58f, "A mossy stone bridge arches over the river.", residents = listOf("grumble")),
        Location("sunken_crypt", "The Sunken Crypt", DUNGEON, 0.31f, 0.86f, "Old stone steps lead down into a crypt full of rattling noises.", guardian = "captain_rattlebones", rooms = 3),
        Location("hermit_hill", "Hermit's Hill", WILD, 0.40f, 0.22f, "A windy hill with one tiny round door in its side.", residents = listOf("hazel_hermit")),
        Location("pennywhistle", "Pennywhistle Market", TOWN, 0.44f, 0.70f, "A busy market town with striped awnings and the smell of hot pies.", residents = listOf("smith_brogan", "merchant_zig", "rascal_fox"), shops = listOf("smithy", "oddments")),
        Location("gloomwood_mine", "Gloomwood Mine", DUNGEON, 0.52f, 0.11f, "A dark old mine where something goes thump in the walls.", guardian = "mossy_golem", rooms = 3),
        Location("spider_caves", "The Spider Caves", DUNGEON, 0.52f, 0.46f, "Silky webs sparkle across the mouth of a cave.", guardian = "spider_queen", rooms = 3),
        Location("lantern_hollow", "Lantern Hollow", TOWN, 0.62f, 0.26f, "A town where a thousand lanterns glow, day and night.", residents = listOf("lumi_lamp", "captain_hob", "old_merlo"), shops = listOf("lampwright", "spells")),
        Location("whispering_falls", "Whispering Falls", WILD, 0.66f, 0.56f, "A waterfall whispers secrets into a deep green pool.", residents = listOf("otto_otter")),
        Location("fishers_dock", "Fishers' Dock", TOWN, 0.73f, 0.82f, "A lakeside dock with bobbing boats and a very loud seagull.", residents = listOf("finn_fisher", "sir_ribbit"), shops = listOf("dockstall")),
        Location("windy_pass", "Windy Pass", WILD, 0.78f, 0.38f, "A narrow pass between two mountains, where the wind sings.", residents = listOf("bandit_bess")),
        Location("inkwell_cellars", "Inkwell Cellars", DUNGEON, 0.84f, 0.15f, "Barrels of ink line the cellars, and the floor is sticky.", guardian = "inky_imp", rooms = 3),
        Location("lair_peak", "Dragon's Peak", LAIR, 0.93f, 0.45f, "The tallest mountain, where smoke curls from a cave high above."),
        Location("lair_manor", "Grumblewick Manor", LAIR, 0.92f, 0.72f, "A tall crooked house with every window dark, except one."),
    )

    val roads = listOf(
        Road("camp", "meadow", ROAD, 0),
        Road("camp", "mossbrook", ROAD, 0),
        Road("camp", "fairy_ring", FOREST, 1),
        Road("meadow", "mossbrook", ROAD, 0),
        Road("meadow", "hermit_hill", RIVER, 1),
        Road("mossbrook", "old_bridge", ROAD, 0),
        Road("mossbrook", "hermit_hill", MOUNTAIN, 2),
        Road("fairy_ring", "old_bridge", FOREST, 1),
        Road("fairy_ring", "sunken_crypt", FOREST, 1),
        Road("old_bridge", "pennywhistle", ROAD, 0),
        Road("old_bridge", "spider_caves", FOREST, 1),
        Road("sunken_crypt", "pennywhistle", ROAD, 0),
        Road("hermit_hill", "gloomwood_mine", MOUNTAIN, 2),
        Road("hermit_hill", "lantern_hollow", FOREST, 1),
        Road("pennywhistle", "spider_caves", FOREST, 1),
        Road("pennywhistle", "fishers_dock", ROAD, 0),
        Road("pennywhistle", "whispering_falls", RIVER, 1),
        Road("spider_caves", "lantern_hollow", SWAMP, 2),
        Road("spider_caves", "whispering_falls", SWAMP, 1),
        Road("gloomwood_mine", "lantern_hollow", ROAD, 0),
        Road("lantern_hollow", "whispering_falls", ROAD, 0),
        Road("lantern_hollow", "inkwell_cellars", FOREST, 1),
        Road("lantern_hollow", "windy_pass", MOUNTAIN, 2),
        Road("whispering_falls", "windy_pass", MOUNTAIN, 2),
        Road("whispering_falls", "fishers_dock", ROAD, 0),
        Road("fishers_dock", "lair_manor", ROAD, 1),
        Road("windy_pass", "lair_peak", MOUNTAIN, 2),
        Road("windy_pass", "inkwell_cellars", FOREST, 1),
        Road("windy_pass", "lair_manor", FOREST, 1),
        Road("inkwell_cellars", "lair_peak", ROAD, 1),
    )

    /** Friendships that make the world easier for good: the road they open stays open in every adventure. */
    val flagRoads = mapOf(
        "grumble_befriended" to listOf("pennywhistle~whispering_falls"),
        "bess_befriended" to listOf("lantern_hollow~windy_pass", "whispering_falls~windy_pass"),
        "hazel_friend" to listOf("hermit_hill~mossbrook"),
    )

    val kingdom = Kingdom("Whisperwood", locations, roads)
}
