package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.rpg.content.Content

/** Small hooks so tests can set up a moment (a shop open) without playing to it. */
internal fun Journey.testShop(shopId: String): List<JStep> =
    shop(Content.shop(shopId)!!, Content.npc(Content.shop(shopId)!!.keeper)) { emptyList() }
