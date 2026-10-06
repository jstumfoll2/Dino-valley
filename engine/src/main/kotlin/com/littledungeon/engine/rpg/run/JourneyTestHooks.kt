package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.rpg.content.Content

/** Small hooks so tests can set up a moment (a shop open) without playing to it. */
internal fun Journey.testShop(shopId: String): List<JStep> =
    shop(Content.shop(shopId)!!, Content.npc(Content.shop(shopId)!!.keeper)) { emptyList() }

/** The rooms a dungeon deals for this journey (dealt on first ask, then kept). */
internal fun dealtRooms(j: Journey, dungeon: com.littledungeon.engine.rpg.world.Location): List<com.littledungeon.engine.rpg.world.RoomKind> = j.planOf(dungeon)

/** How many wrong answers sparkle magic can still take away this journey. */
internal fun Journey.sparklesLeft(): Int = sparkleLeft
