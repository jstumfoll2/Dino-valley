package com.dinovalley.engine.rpg.story

/**
 * The painted icons that dialog choices and menus use (`art_<name>`). Choices without words on
 * them: each is a picture the child can learn, and the narrator reads what it means.
 */
object Icons {
    val talk = listOf(
        "talk_ask", "talk_listen", "talk_give", "talk_help", "talk_buy", "talk_leave", "talk_fight", "talk_laugh",
        "talk_sing", "talk_cookie", "talk_coin", "talk_think", "talk_yes", "talk_no", "talk_puzzle", "talk_gift",
    )
    val hub = listOf("hub_inn", "hub_leave", "hub_enter", "hub_attack", "hub_fight", "hub_peace", "hub_shop")
    val all: Set<String> = (talk + hub).toSet()
}
