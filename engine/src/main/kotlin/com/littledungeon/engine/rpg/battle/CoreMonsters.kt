package com.littledungeon.engine.rpg.battle

import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.Skill.*
import com.littledungeon.engine.rpg.world.Terrain

/** The first monsters: little ones on the roads, tough ones in the wild, guardians in dungeons. */
object CoreMonsters {
    val minions = listOf(
        Monster(
            "slime", "Gooey Slime", Tier.MINION, 14, 3, listOf(COUNTING, NUMBERS), Who.CRITTER,
            "A gooey slime wobbles onto the road! <critter>You can't pass! I'm a very important slime!",
            "The slime goes splat and giggles. <critter>Okay, okay, you can pass!",
            "The slime wobbles over you and you sit down with a plop.",
            coins = 2..4, drops = listOf(Drop("berry", 40)),
            habitat = setOf(Terrain.SWAMP, Terrain.FOREST),
        ),
        Monster(
            "bat", "Pesky Bat", Tier.MINION, 12, 3, listOf(COLORS, PATTERNS), Who.CRITTER,
            "A pesky bat swoops down! <critter>Look at me, look at me! Can you catch me?",
            "The bat flutters away, dizzy. <critter>Whoa, I need a nap!",
            "The bat swoops round and round until you are dizzy.",
            coins = 2..4, drops = listOf(Drop("berry", 30)),
            habitat = setOf(Terrain.FOREST, Terrain.MOUNTAIN),
        ),
        Monster(
            "spider", "Sticky Spider", Tier.MINION, 14, 4, listOf(LETTERS, PATTERNS), Who.CRITTER,
            "A sticky spider drops from a branch! <critter>I spun a puzzle just for you!",
            "The spider climbs back up its thread. <critter>You are good at puzzles!",
            "The spider wraps you up in a sticky web.",
            coins = 3..5, drops = listOf(Drop("rope", 15)),
            habitat = setOf(Terrain.FOREST, Terrain.SWAMP),
        ),
        Monster(
            "wolf_pup", "Grumpy Wolf Pup", Tier.MINION, 16, 4, listOf(NUMBERS, ADDITION), Who.CRITTER,
            "A wolf pup jumps out of the bushes! <critter>I mean, hello! I am very fierce! Watch out!",
            "The wolf pup rolls over and wags its tail. <critter>Okay, you win. Will you scratch my tummy?",
            "The wolf pup pounces and licks you until you fall over.",
            coins = 3..5, drops = listOf(Drop("friendship_cookie", 20)), befriendable = true,
            habitat = setOf(Terrain.FOREST),
        ),
        Monster(
            "skeleton", "Rattlebone", Tier.MINION, 15, 4, listOf(PATTERNS, SKIP_COUNTING), Who.SPOOK,
            "A skeleton clatters out of the shadows! <spook>Rattle, rattle! Hello! I lost my other leg.",
            "The skeleton falls into a heap of bones. <spook>Oh dear. I will put myself back together.",
            "The skeleton knocks you over and you land on a pile of bones.",
            coins = 3..6, drops = listOf(Drop("rusty_key", 20)),
            habitat = setOf(Terrain.SWAMP, Terrain.FOREST),
        ),
        Monster(
            "ghost", "Boo Ghost", Tier.MINION, 13, 3, listOf(COLORS, LETTERS), Who.SPOOK,
            "A little ghost floats out of the wall! <spook>Boo! Did I scare you? Please say yes.",
            "The ghost giggles and fades away. <spook>That was the best game ever!",
            "The ghost goes boo, and you jump so high you fall over.",
            coins = 2..4, drops = listOf(Drop("lantern", 15)),
            habitat = setOf(Terrain.SWAMP),
        ),
        Monster(
            "boar", "Mud Boar", Tier.MINION, 18, 5, listOf(ADDITION, SKIP_COUNTING), Who.CRITTER,
            "A muddy boar snorts and scrapes its hoof. <critter>This is my mud puddle! Count to pass!",
            "The boar trots off to find a new puddle. <critter>Fine, fine, it was too small anyway.",
            "The boar charges, and you land in the mud puddle.",
            coins = 3..6, drops = listOf(Drop("honey_cake", 15)),
            habitat = setOf(Terrain.FOREST),
        ),
        Monster(
            "rock_crab", "Rock Crab", Tier.MINION, 17, 4, listOf(COUNTING, NUMBERS), Who.CRITTER,
            "A rock crab clicks its claws. <critter>Click, click! I will pinch you if you do not know your numbers!",
            "The crab hides under its rock. <critter>Come back when I am less embarrassed.",
            "The crab pinches your toe and you hop around.",
            coins = 3..5,
            habitat = setOf(Terrain.MOUNTAIN, Terrain.RIVER),
        ),
    )

    val rogues = listOf(
        Monster(
            "sneaky_fox", "Sneaky Fox", Tier.MINION, 16, 4, listOf(LETTERS, COLORS), Who.RASCAL,
            "A fox pops out from behind a crate. <rascal>You want it back? Then catch me, if you can!",
            "The fox drops what it was holding. <rascal>Fine, fine. You are quicker than you look.",
            "The fox darts around you so fast that you get dizzy and sit down.",
            coins = 3..6, roams = false,
        ),
    )

    val elites = listOf(
        Monster(
            "cave_troll", "Cave Troll", Tier.ELITE, 30, 6, listOf(ADDITION, NUMBERS, COUNTING), Who.GROWLER,
            "A troll stomps out of the cave. <growler>Nobody gets by without paying my puzzle toll!",
            "The troll scratches its head. <growler>Nobody ever solved my puzzles before. Go on, then.",
            "The troll stomps, the ground shakes, and you tumble back.",
            coins = 6..10, drops = listOf(Drop("big_potion", 25)), befriendable = true,
            habitat = setOf(Terrain.MOUNTAIN),
        ),
        Monster(
            "web_weaver", "Web Weaver", Tier.ELITE, 28, 6, listOf(LETTERS, PATTERNS), Who.CRITTER,
            "A huge spider lowers itself from the ceiling. <critter>Welcome to my web! Spell my name and I might let you go.",
            "The Web Weaver untangles the web. <critter>What a clever visitor! Take this.",
            "The Web Weaver spins you into a giant cocoon.",
            coins = 6..10, drops = listOf(Drop("silver_key", 25), Drop("rope", 25)),
            habitat = setOf(Terrain.FOREST, Terrain.SWAMP),
        ),
        Monster(
            "moon_wolf", "Moon Wolf", Tier.ELITE, 30, 7, listOf(NUMBERS, SKIP_COUNTING, COLORS), Who.GROWLER,
            "A silver wolf howls on the hill. <growler>The moon is watching. Show me you are brave and clever.",
            "The Moon Wolf bows its head. <growler>You are both. Pass in peace.",
            "The Moon Wolf howls so loudly that you sit down in surprise.",
            coins = 7..11, drops = listOf(Drop("owl_feather", 20)),
            habitat = setOf(Terrain.FOREST, Terrain.MOUNTAIN),
        ),
        Monster(
            "bandit_bess", "Bandit Bess", Tier.ELITE, 28, 6, listOf(PATTERNS, NUMBERS, ADDITION), Who.BESS,
            "A bandit swings down from a rock. <bess>This is my pass. Pay up, or play for it!",
            "Bess lowers her fists and sits down hard. <bess>All right, all right. I give up. Nobody ever beat me before.",
            "Bess tips her hat, gives you a gentle shove, and everything goes dim.",
            coins = 8..12, drops = listOf(Drop("bubble_shield", 30)), befriendable = true, roams = false,
        ),
        Monster(
            "mossy_golem", "Mossy Golem", Tier.MINI_BOSS, 38, 6, listOf(PATTERNS, COUNTING, ADDITION), Who.GROWLER,
            "A mossy golem rumbles to life. <growler>Stone must stay still. Puzzles must be solved. That is the rule.",
            "The golem crumbles into a pile of mossy rocks. <growler>Rule solved. Thank you.",
            "The golem leans over, and everything goes dark and mossy.",
            // The guardian of Gloomwood Mine stays in its mine.
            coins = 7..12, drops = listOf(Drop("iron_helm", 10), Drop("big_potion", 20)), roams = false,
        ),
    )

    val guardians = listOf(
        Monster(
            "captain_rattlebones", "Captain Rattlebones", Tier.MINI_BOSS, 44, 7, listOf(PATTERNS, SKIP_COUNTING, ADDITION), Who.SPOOK,
            "A skeleton captain rises from the old ship's wheel. <spook>Ahoy, little ones. No one leaves my crypt without winning my game.",
            "The captain's bones rattle with laughter. <spook>A fine crew you would make! Take this, and go.",
            "The captain sweeps you off your feet with one bony arm.",
            coins = 12..18, drops = listOf(Drop("knight_sword", 15), Drop("big_potion", 40)), weakness = "spark_bomb",
        ),
        Monster(
            "grumble_troll", "Grumble the Troll", Tier.MINI_BOSS, 48, 7, listOf(ADDITION, NUMBERS, SKIP_COUNTING), Who.GRUMBLE,
            "Grumble the troll blocks the bridge. <grumble>This is my bridge. Nobody crosses. Nobody ever asks me how I am.",
            "Grumble sits down with a thump. <grumble>Hmm. You listened. Nobody listens. You can cross, friend.",
            "Grumble stomps and the whole bridge shakes you back to the start.",
            coins = 12..18, drops = listOf(Drop("friendship_cookie", 50)), befriendable = true, weakness = "friendship_cookie",
        ),
        Monster(
            "spider_queen", "The Spider Queen", Tier.MINI_BOSS, 46, 8, listOf(LETTERS, PATTERNS, COLORS), Who.CRITTER,
            "The Spider Queen unrolls a silk carpet. <critter>Welcome to my palace, little guests. Play my puzzles, or stay forever!",
            "The Queen claps all eight hands. <critter>Marvelous! You may keep my best silk.",
            "The Queen waves a hand, and the silk wraps you up like a present.",
            coins = 12..18, drops = listOf(Drop("owl_feather", 50)), weakness = "lantern",
        ),
        Monster(
            "bat_king", "The Bat King", Tier.MINI_BOSS, 42, 7, listOf(COLORS, COUNTING, NUMBERS), Who.CRITTER,
            "A giant bat unfolds from the ceiling, wearing a tiny crown. <critter>I am the Bat King! All the lanterns in the kingdom belong to me!",
            "The Bat King's crown slips down over his eyes. <critter>Oh dear. Maybe I should share.",
            "The Bat King swoops, and the whole cave goes upside down.",
            coins = 12..18, drops = listOf(Drop("lantern", 60)), weakness = "lantern",
        ),
        Monster(
            "inky_imp", "Inky Imp", Tier.MINI_BOSS, 40, 6, listOf(LETTERS, PATTERNS, COLORS), Who.SNEAK,
            "A little imp covered in ink hops onto a desk. <sneak>Hee hee! The Baron said to guard the quill, and I always do what the Baron says. Mostly.",
            "The Inky Imp drops the quill with a splash. <sneak>Fine, take it! I never liked guarding things anyway.",
            "The Imp flicks a blot of ink, and you slide right out the door.",
            coins = 12..18, drops = listOf(Drop("silver_quill", 100)), weakness = "spark_bomb",
        ),
    )

    val bosses = listOf(
        Monster(
            "baron_grumblewick", "Baron Grumblewick", Tier.BOSS, 75, 9, listOf(LETTERS, NUMBERS, ADDITION, PATTERNS, COLORS), Who.BARON,
            "Baron Grumblewick lowers his top hat and sneers. <baron>The pages are mine, and so is every story. Nobody tells it better than I do.",
            "The Baron's top hat tumbles off, and the pages flutter free. <baron>My stories! My lovely, lovely stories!",
            "The Baron laughs a thin laugh, and everything goes quiet and inky.",
            coins = 25..35, drops = listOf(Drop("storybook_page", 100)), weakness = "ink_cleaner",
        ),
        Monster(
            "ink_shadow", "The Ink Shadow", Tier.BOSS, 62, 8, listOf(LETTERS, PATTERNS, COLORS, NUMBERS), Who.SHADOW,
            "The Ink Shadow rises out of the Storybook. <shadow>Scribble, scribble, scribble. Everything is mine to scribble on.",
            "The Ink Shadow shrinks into a tiny blot. <shadow>I only wanted to draw. Nobody ever gave me a page of my own.",
            "The Ink Shadow scribbles on you, and you wake up somewhere safe.",
            coins = 20..30, drops = listOf(Drop("storybook_page", 100)),
        ),
        Monster(
            "big_dragon", "The Dragon", Tier.BOSS, 68, 9, listOf(NUMBERS, ADDITION, COUNTING, COLORS, LETTERS), Who.DRAGON,
            "The great dragon opens one golden eye. <dragon>Who dares come into my lair?",
            "The dragon lowers its great head. <dragon>You are very brave. Maybe I was wrong about you.",
            "The dragon yawns, and its warm breath puffs you right out of the cave.",
            coins = 25..35, drops = listOf(Drop("storybook_page", 100)), weakness = "friendship_cookie", befriendable = true,
        ),
    )

    val all: List<Monster> = minions + rogues + elites + guardians + bosses
}
