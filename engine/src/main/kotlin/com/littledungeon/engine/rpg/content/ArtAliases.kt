package com.littledungeon.engine.rpg.content

/**
 * Characters that already have painted art under another name: a person who is also met as a foe,
 * or a monster that is one of the original cast. The app looks art up through [resolve].
 */
object ArtAliases {
    val names: Map<String, String> = mapOf(
        "monster_bandit_bess" to "npc_bandit_bess",
        "monster_ink_shadow" to "shadow",
        "monster_big_dragon" to "dragon_big",
    )

    fun resolve(art: String): String = names[art] ?: art
}
