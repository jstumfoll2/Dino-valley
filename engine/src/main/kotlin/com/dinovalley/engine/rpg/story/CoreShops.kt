package com.dinovalley.engine.rpg.story

/** What each shop sells. A shop is a keeper and a list of item ids; the stock changes by editing this list. */
object CoreShops {
    val all = listOf(
        ShopDef("bakery", "Bun's Bakery", "baker_bun", listOf("berry", "honey_cake", "friendship_cookie")),
        ShopDef("healer", "Willow's Herbs", "healer_willow", listOf("honey_cake", "big_potion", "bubble_shield")),
        ShopDef("smithy", "Brogan's Forge", "smith_brogan", listOf("wooden_sword", "knight_sword", "leather_cap", "iron_helm", "padded_tunic", "chain_mail", "sturdy_boots", "puddle_boots")),
        ShopDef(
            "oddments", "Zig's Silly Oddments", "merchant_zig",
            listOf("chicken_hat", "duck_helmet", "pot_helmet", "spoon_sword", "banana_blaster", "noodle_whip", "pillow_armor", "dino_pajamas", "squeaky_boots", "bunny_slippers"),
        ),
        ShopDef("lampwright", "Lumi's Lamps", "lumi_lamp", listOf("lantern", "rope", "spark_bomb", "sleep_dust", "smoke_pearl", "rusty_key")),
        ShopDef("spells", "Merlo's Spells", "old_merlo", listOf("wizard_hat", "magic_wand", "lucky_clover", "hint_scroll", "owl_feather", "silver_key")),
        ShopDef("dockstall", "Finn's Dock Stall", "finn_fisher", listOf("rope", "berry", "honey_cake", "puddle_boots", "smoke_pearl")),
    )
}
