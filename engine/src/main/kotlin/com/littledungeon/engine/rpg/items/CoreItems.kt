package com.littledungeon.engine.rpg.items

import com.littledungeon.engine.rpg.items.BattleUse.*
import com.littledungeon.engine.rpg.items.ItemKind.*
import com.littledungeon.engine.rpg.items.Obstacle.*
import com.littledungeon.engine.rpg.items.Slot.*

/** The first set of items. More packs add more (see ContentPack). */
object CoreItems {
    val food = listOf(
        Item("berry", "Juicy Berry", CONSUMABLE, "A juicy berry. It heals a little.", price = 3, use = HEAL, power = 8),
        Item("honey_cake", "Honey Cake", CONSUMABLE, "A sticky honey cake. It heals a lot.", price = 7, use = HEAL, power = 15),
        Item("big_potion", "Big Red Potion", CONSUMABLE, "A big red potion. It heals nearly everything.", price = 14, use = HEAL, power = 30),
    )

    val fighting = listOf(
        Item("sleep_dust", "Sleepy Dust", CONSUMABLE, "Sprinkle it, and the monster yawns and misses its turn.", price = 10, use = STUN, power = 2),
        Item("spark_bomb", "Spark Bomb", CONSUMABLE, "It goes pop and sparkle, and it hits for a lot.", price = 9, use = DAMAGE, power = 9),
        Item("smoke_pearl", "Smoke Pearl", CONSUMABLE, "Drop it, and you slip away in a puff of smoke.", price = 6, use = ESCAPE),
        Item("friendship_cookie", "Friendship Cookie", CONSUMABLE, "Some monsters just want a snack and a friend.", price = 8, use = BEFRIEND),
        Item("bubble_shield", "Bubble Shield", CONSUMABLE, "A big bubble that bounces the next hit away.", price = 8, use = SHIELD, power = 1),
    )

    val tools = listOf(
        Item("rope", "Trusty Rope", TOOL, "A strong rope for climbing up and across.", price = 8, opens = setOf(CLIMB, CROSS)),
        Item("lantern", "Little Lantern", TOOL, "It glows warmly, so dark places are not so scary.", price = 8, opens = setOf(DARK)),
        Item("rusty_key", "Rusty Key", KEY, "It opens old locks.", price = 5, opens = setOf(LOCK)),
        Item("silver_key", "Silver Key", KEY, "It opens the fancy locks.", price = 12, opens = setOf(LOCK)),
    )

    /** Charms for a second guess: he can't guess until it's right, but a charm gives one more look. */
    val charms = listOf(
        Item("lucky_clover", "Lucky Clover", CHARM, "A four-leaf clover. After a wrong answer, you get one more guess.", price = 12, guess = 0),
        Item("owl_feather", "Wise Owl Feather", CHARM, "After a wrong answer, you guess again, and the owl takes one wrong answer away.", price = 16, guess = 1),
        Item("hint_scroll", "Hint Scroll", CHARM, "After a wrong answer, you guess again, with two wrong answers taken away.", price = 20, guess = 2),
    )

    val gear = listOf(
        // On your head
        Item("leather_cap", "Leather Cap", EQUIPMENT, "A snug leather cap.", price = 8, slot = HEAD, defense = 1),
        Item("iron_helm", "Iron Helm", EQUIPMENT, "A shiny iron helmet.", price = 20, slot = HEAD, defense = 2),
        Item("wizard_hat", "Pointy Wizard Hat", EQUIPMENT, "A tall hat covered in stars.", price = 18, slot = HEAD, attack = 1, hp = 2),
        Item("chicken_hat", "Chicken Hat", EQUIPMENT, "A big chicken sits on your head. The monsters giggle.", price = 6, slot = HEAD, hp = 3, funny = true),
        Item("duck_helmet", "Rubber Duck Helmet", EQUIPMENT, "Squeak! A duck for a helmet.", price = 10, slot = HEAD, defense = 1, hp = 1, funny = true),
        Item("pot_helmet", "Cooking Pot Helmet", EQUIPMENT, "Clang! It is a pot, but it works.", price = 4, slot = HEAD, defense = 1, funny = true),
        // In your hand
        Item("wooden_sword", "Wooden Sword", EQUIPMENT, "A sturdy wooden sword.", price = 6, slot = HAND, attack = 2),
        Item("knight_sword", "Knight's Sword", EQUIPMENT, "A shiny sword fit for a knight.", price = 26, slot = HAND, attack = 4),
        Item("magic_wand", "Sparkle Wand", EQUIPMENT, "A wand that sparkles when you wave it.", price = 24, slot = HAND, attack = 3, hp = 2),
        Item("spoon_sword", "Giant Spoon", EQUIPMENT, "It is a spoon, but a very big one. The monsters giggle.", price = 5, slot = HAND, attack = 2, funny = true),
        Item("banana_blaster", "Banana Blaster", EQUIPMENT, "It goes splat, and bananas are slippery.", price = 14, slot = HAND, attack = 3, funny = true),
        Item("noodle_whip", "Noodle Whip", EQUIPMENT, "A long wiggly noodle. Slurp, whip, slurp!", price = 9, slot = HAND, attack = 2, hp = 1, funny = true),
        // On your body
        Item("padded_tunic", "Padded Tunic", EQUIPMENT, "A cozy padded tunic.", price = 8, slot = BODY, defense = 1, hp = 2),
        Item("chain_mail", "Chain Mail", EQUIPMENT, "Shiny rings, all linked together.", price = 28, slot = BODY, defense = 3),
        Item("pillow_armor", "Pillow Armor", EQUIPMENT, "Soft and puffy. Nothing hurts when you wear it.", price = 9, slot = BODY, defense = 1, hp = 4, funny = true),
        Item("dino_pajamas", "Dino Pajamas", EQUIPMENT, "Cozy pajamas with a spiky tail. Rawr!", price = 16, slot = BODY, defense = 2, hp = 2, funny = true),
        // On your feet
        Item("sturdy_boots", "Sturdy Boots", EQUIPMENT, "Boots for a long walk.", price = 10, slot = FEET, hp = 3),
        Item("squeaky_boots", "Squeaky Boots", EQUIPMENT, "Squeak, squeak, squeak! Nobody can sneak up on you.", price = 8, slot = FEET, defense = 1, hp = 1, funny = true),
        Item("puddle_boots", "Puddle Boots", EQUIPMENT, "Splash! Rubber boots for the muddy roads.", price = 12, slot = FEET, defense = 1, hp = 2),
        Item("bunny_slippers", "Bunny Slippers", EQUIPMENT, "Fluffy slippers with floppy ears.", price = 7, slot = FEET, hp = 2, funny = true),
    )

    /** Pieces of the story: found, not bought. */
    val story = listOf(
        Item("storybook_page", "Storybook Page", QUEST, "A page from the Great Storybook, glowing softly."),
        Item("silver_quill", "Silver Quill", QUEST, "A silver quill that can write the Storybook whole again."),
        Item("recipe_page", "Recipe Page", QUEST, "Baker Bun's lost recipe page, a little bit sticky."),
        Item("magic_beans", "Magic Beans", QUEST, "They are only beans. But somebody might love them."),
        Item("ink_cleaner", "Ink Cleaner", QUEST, "A bottle that washes away the Baron's sticky ink."),
    )

    val all: List<Item> = food + fighting + tools + charms + gear + story
}
