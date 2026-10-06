package com.littledungeon.engine.rpg.story

import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.story.Cond.*
import com.littledungeon.engine.rpg.story.Effect.*

private fun o(said: String, icon: String, next: String? = null, effects: List<Effect> = emptyList(), needs: List<Cond> = emptyList()) =
    Option(said, icon, next, effects, needs)

private fun n(id: String, says: String, options: List<Option> = emptyList(), effects: List<Effect> = emptyList()) =
    DialogNode(id, says, options, effects)

private fun bye(said: String = "Say goodbye") = o(said, "talk_leave")

/**
 * The first people of Whisperwood. Each one has a life of their own: something they have been
 * through, something they want, and a few things the hero can do about it. What the hero does
 * is remembered, and it opens roads, gives tools, and changes what other people say.
 *
 * The threads that join them up:
 *  - Baker Bun's lost recipe page is with Rascal the Fox (who also sells magic beans, which Sir
 *    Ribbit longs for); Mayor Tilly cheers when Bun is happy.
 *  - Grumble the troll sits on Old Stone Bridge because he is lonely; befriend him and Finn gets
 *    his boat back, which opens the river ferry.
 *  - Captain Hob worries about Bandit Bess, who only steals for her little brother; befriend her and
 *    the mountain pass becomes an easy road.
 *  - Hermit Hazel lets a kind visitor use the rope ladder up the hill.
 *  - Old Merlo can make the Ink Cleaner the Baron's page-stealing ink fears.
 */
object CoreNpcs {
    val hoot = Npc(
        "professor_hoot", "Professor Hoot", "Keeper of the Storybook", Who.HOOT,
        "Professor Hoot is an old owl who has kept the Great Storybook for sixty years. He never lost a single page until this year, and he feels it is all his fault.",
        "Hello, little adventurer! I am Professor Hoot, keeper of the Great Storybook.",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "What would you like to know?",
                listOf(
                    o("Ask about the Baron", "talk_ask", "baron"),
                    o("Ask for advice", "talk_think", "advice"),
                    o("Hear a story", "talk_listen", "story", listOf(SetFlag("run:hoot_story"), Stars(Attribute.WISDOM, 5)), needs = listOf(NoFlag("run:hoot_story"))),
                    bye(),
                ),
            ),
            n(
                "baron", "Baron Grumblewick was the best storyteller in Whisperwood, once. Then people stopped listening, and he stopped smiling. Perhaps he only wants somebody to hear him.",
                listOf(o("Thank you", "talk_yes", "hub")), listOf(SetFlag("knows_baron_lonely")),
            ),
            n(
                "advice", "Visit the towns and make friends. Fight only when you must. A friend you make today might open a road tomorrow. Here, have a berry for the road.",
                listOf(o("Thank you", "talk_yes", "hub")), listOf(Give("berry")),
            ),
            n("story", "Once there was a tiny dragon who wanted to read every book in the world. <narrator>The Professor tells a long, lovely tale, and you learn something new.", listOf(o("What a story", "talk_laugh", "hub"))),
        ),
    )

    val tilly = Npc(
        "mayor_tilly", "Mayor Tilly", "Mayor of Mossbrook", Who.TILLY,
        "Tilly has been mayor of Mossbrook for forty years. She knows everyone's name and everyone's troubles, and she worries most about the baker, who has not smiled since the sticky bun recipe went missing.",
        "Oh, hello, dear! I am Tilly, the mayor of Mossbrook. Every traveler is welcome here.",
        listOf(Start("thanks", Flag("bun_recipe_returned"), NoFlag("tilly_thanked")), Start("hub")),
        listOf(
            n(
                "hub", "How can I help you, dear?",
                listOf(
                    o("Ask about Mossbrook", "talk_ask", "town"),
                    o("Ask how I can help", "talk_help", "help", needs = listOf(NoFlag("bun_recipe_returned"))),
                    o("Share some news", "talk_give", "news", needs = listOf(Flag("bun_recipe_returned"))),
                    bye(),
                ),
            ),
            n("town", "Mossbrook is the oldest village in Whisperwood. Our stream sings, our bread is the best, and our people are kind. We just need our baker to smile again.", listOf(o("Thank you", "talk_yes", "hub"))),
            n(
                "help", "Baker Bun lost her sticky bun recipe page, and it broke her heart. She thinks a fox took it at Pennywhistle Market. If you could find it, I would be so grateful.",
                listOf(o("I will find it", "talk_yes", "hub", listOf(SetFlag("quest_recipe")))),
            ),
            n("news", "Bun has her recipe back? Wonderful, wonderful! You have made a whole village happy.", listOf(o("You are welcome", "talk_yes", "thanks"))),
            n(
                "thanks", "Mossbrook thanks you, dear. Here is a little something for your trouble, and a key that opens old doors. Come back any time.",
                listOf(bye()), listOf(Earn(10), Give("rusty_key"), SetFlag("tilly_thanked"), Relation("mayor_tilly", 2)),
            ),
        ),
    )

    val bun = Npc(
        "baker_bun", "Baker Bun", "Baker of Mossbrook", Who.BUN,
        "Bun has baked sticky buns since she was small, from a recipe her grandmother wrote on a single page. When the page vanished, so did her smile.",
        "Hello, hello! I am Bun, and this is the best bakery in Whisperwood. Well, it was. Well, it still is, mostly.",
        listOf(Start("return", HasItem("recipe_page"), Flag("quest_recipe"), NoFlag("bun_recipe_returned")), Start("happy", Flag("bun_recipe_returned")), Start("hub")),
        listOf(
            n(
                "hub", "What can I get you?",
                listOf(
                    o("Buy some treats", "talk_buy", null, listOf(Shop("bakery"))),
                    o("Ask what is wrong", "talk_listen", "wrong"),
                    bye(),
                ),
            ),
            n(
                "wrong", "My sticky bun recipe is gone! It was my grandmother's, written on one little page. I think a fox took it when I was not looking. He hangs around Pennywhistle Market.",
                listOf(o("I will get it back", "talk_help", "hub", listOf(SetFlag("quest_recipe"), Relation("baker_bun", 1)))),
            ),
            n(
                "return", "My page! My grandmother's page! Oh, you wonderful, wonderful person! Here, take these honey cakes, and come back any time.",
                listOf(bye()), listOf(Take("recipe_page"), SetFlag("bun_recipe_returned"), Give("honey_cake", 3), Relation("baker_bun", 3)),
            ),
            n(
                "happy", "Sticky buns for everybody! What can I get you, my favorite hero?",
                listOf(o("Buy some treats", "talk_buy", null, listOf(Shop("bakery"))), bye()),
            ),
        ),
        shop = "bakery",
    )

    val willow = Npc(
        "healer_willow", "Healer Willow", "Healer of Mossbrook", Who.WILLOW,
        "Willow grows every herb in Whisperwood in a garden behind her cottage. Lately the swamp frogs keep hopping in, and she is too kind to shoo them out.",
        "Come in, come in, little one. I am Willow. Do you need patching up?",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "What can I do for you?",
                listOf(
                    o("Buy some herbs", "talk_buy", null, listOf(Shop("healer"))),
                    o("Ask about her garden", "talk_ask", "garden"),
                    o("Help in the garden", "talk_help", "gardening", listOf(SetFlag("willow_helped"), Relation("healer_willow", 2)), needs = listOf(Stat(Attribute.KINDNESS, 2), NoFlag("willow_helped"))),
                    bye(),
                ),
            ),
            n("garden", "The herbs here can cure nearly anything, but the frogs keep sitting on my lettuce. I do not have the heart to move them. They look so happy.", listOf(o("They do look happy", "talk_laugh", "hub"))),
            n(
                "gardening", "What a kind helper you are! You carried every frog to the pond, and the lettuce is saved. Please take this feather. It belonged to a very wise owl.",
                listOf(o("Thank you", "talk_yes", "hub")), listOf(Give("owl_feather"), Heal(99)),
            ),
        ),
        shop = "healer",
    )

    val hazel = Npc(
        "hazel_hermit", "Hermit Hazel", "Hermit of the Hill", Who.HAZEL,
        "Hazel moved to the hill years ago because people kept saying they were too clever to talk to. They love riddles and never get visitors, and secretly they would love a friend.",
        "Well, well. A visitor. I do not get many. I am Hazel, and this is my hill.",
        listOf(Start("friend", Flag("hazel_friend")), Start("hub")),
        listOf(
            n(
                "hub", "Do you like riddles?",
                listOf(
                    o("Try a riddle", "talk_puzzle", null, listOf(Puzzle("RIDDLE", "riddle_win", "riddle_lose", Skill.PATTERNS, "Hazel scratches a riddle in the dirt with a stick.")), needs = listOf(NoFlag("run:hazel_riddle"))),
                    o("Stay for tea", "talk_give", "tea", needs = listOf(Stat(Attribute.KINDNESS, 2))),
                    bye(),
                ),
            ),
            n("riddle_win", "Ha! You solved it. Few people ever do. Take this scroll. It helps when a puzzle is too tricky.", listOf(bye()), listOf(Give("hint_scroll"), SetFlag("run:hazel_riddle"))),
            n("riddle_lose", "Not this time. Come back and try again, any time.", listOf(bye()), listOf(SetFlag("run:hazel_riddle"))),
            n(
                "tea", "Oh. You want to stay for tea? Nobody ever stays. Sit, sit! <narrator>You talk about everything for an hour. Hazel laughs for the first time in ages, and lowers the rope ladder at the foot of the hill, just for you.",
                listOf(bye()), listOf(SetFlag("hazel_friend"), Give("lucky_clover"), Relation("hazel_hermit", 3)),
            ),
            n(
                "friend", "My friend! The rope ladder is always down for you. Care for another riddle, or a cup of tea?",
                listOf(o("Try a riddle", "talk_puzzle", null, listOf(Puzzle("RIDDLE", "riddle_win", "riddle_lose", Skill.PATTERNS, "Hazel scratches a riddle in the dirt with a stick.")), needs = listOf(NoFlag("run:hazel_riddle"))), bye()),
            ),
        ),
    )

    val brogan = Npc(
        "smith_brogan", "Brogan the Smith", "Blacksmith of Pennywhistle", Who.BROGAN,
        "Brogan was a knight until a dragon scare made him hang up his sword and open a smithy. He still makes the best armor in the kingdom, and he still wonders if he was brave enough.",
        "Welcome to my forge! I am Brogan. If it is made of metal, I can make it better.",
        listOf(Start("dragon_friend", Flag("friend:big_dragon"), NoFlag("brogan_dragon_thanked")), Start("hub")),
        listOf(
            n(
                "hub", "What do you need?",
                listOf(
                    o("Look at his wares", "talk_buy", null, listOf(Shop("smithy"))),
                    o("Ask about the dragon", "talk_ask", "dragon", needs = listOf(Not(ArcIs("lonely_dragon")))),
                    o("Ask about the dragon", "talk_ask", "dragon_now", needs = listOf(ArcIs("lonely_dragon"), NoFlag("brogan_braver"))),
                    o("Prove your skill", "talk_puzzle", null, listOf(Puzzle("LOCK", "proof", "proof_fail", Skill.NUMBERS, "Brogan hammers a number into a bar of iron.")), needs = listOf(NoFlag("brogan_helm"), NoFlag("run:brogan_tried"))),
                    bye(),
                ),
            ),
            n("dragon", "I was a knight when the dragon came down from the peak. I did not fight it. I ran. Do you think that makes me a coward? <narrator>{name} shakes its head. <pet>Running is smart sometimes!", listOf(o("You are not a coward", "talk_yes", "hub", listOf(Relation("smith_brogan", 1))))),
            n(
                "dragon_now", "You are going up to the dragon? Then listen. I was a knight when it came down from the peak, and I did not fight. I ran. I have wondered for years whether it only wanted to say hello. Take this shield. Its front shines like a mirror, so the dragon can see its own kind face. And tell it I am sorry I ran.",
                listOf(o("I will tell it", "talk_yes", "hub")), listOf(Give("bubble_shield"), SetFlag("brogan_braver"), Relation("smith_brogan", 2)),
            ),
            n(
                "dragon_friend", "You made friends with the dragon? <narrator>Brogan sits down on his anvil, and he laughs until he cries. <brogan>All those years I thought I was a coward, and it was only a lonely dragon. Take this shield for the road, with my thanks.",
                listOf(bye()), listOf(Give("bubble_shield"), SetFlag("brogan_dragon_thanked"), Relation("smith_brogan", 3)),
            ),
            n("proof", "Sharp mind, steady hand. You have earned something. This helmet was meant for a knight, and I think you are one.", listOf(bye()), listOf(Give("iron_helm"), SetFlag("brogan_helm"), Relation("smith_brogan", 2))),
            n("proof_fail", "Not quite. Come back with a clearer head, and we will try again.", listOf(bye()), listOf(SetFlag("run:brogan_tried"))),
        ),
        shop = "smithy",
    )

    val zig = Npc(
        "merchant_zig", "Zig the Merchant", "Traveling merchant", Who.ZIG,
        "Zig travels the world looking for the silliest things there are, and has never once been bored. He believes that anyone wearing a chicken hat is already halfway to being brave.",
        "Step right up, step right up! I am Zig, and I sell the silliest, squeakiest gear in the land!",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "What tickles your fancy?",
                listOf(
                    o("Look at his wares", "talk_buy", null, listOf(Shop("oddments"))),
                    o("Tell him a joke", "talk_laugh", "joke", listOf(SetFlag("run:zig_joke")), needs = listOf(NoFlag("run:zig_joke"))),
                    bye(),
                ),
            ),
            n("joke", "Ha ha ha! That was the best joke I have heard all week! Here, take these slippers, a gift from one funny person to another.", listOf(o("Thank you", "talk_laugh", "hub")), listOf(Give("bunny_slippers"), Relation("merchant_zig", 1))),
        ),
        shop = "oddments",
    )

    val fox = Npc(
        "rascal_fox", "Rascal the Fox", "Market trickster", Who.RASCAL,
        "Rascal learned to be sneaky because nobody ever shared anything with him. He does not mean harm. He just wants to win, and to be noticed.",
        "Psst! Hello there. I am Rascal. Fine goods, fine prices, no questions asked.",
        listOf(Start("page", Flag("quest_recipe"), NoFlag("recipe_got")), Start("hub")),
        listOf(
            n(
                "hub", "Interested in some magic beans? Only three coins!",
                listOf(
                    o("Buy the beans", "talk_buy", "beans", listOf(Pay(3), Give("magic_beans"), SetFlag("run:fox_beans")), needs = listOf(Coins(3), NoFlag("run:fox_beans"))),
                    bye(),
                ),
            ),
            n("beans", "Pleasure doing business! They are completely, absolutely magic. Probably.", listOf(o("Hmm, they look like beans", "talk_think", "hub"))),
            n(
                "page", "A recipe page? What recipe page? I have never seen a recipe page in my life. <narrator>Something sticky is peeking out of his pocket.",
                listOf(
                    o("Pay five coins", "talk_coin", "paid", listOf(Pay(5)), needs = listOf(Coins(5))),
                    o("Challenge him", "talk_puzzle", null, listOf(Puzzle("RIDDLE", "won", "lost", Skill.LETTERS, "Rascal waves the page and dares you to find the letter on it."))),
                    o("Chase him", "talk_fight", null, listOf(Fight("sneaky_fox", "won"))),
                ),
            ),
            n("paid", "Pleasure doing business. Here, it is not even a good recipe. Did I say that out loud?", listOf(bye()), listOf(Give("recipe_page"), SetFlag("recipe_got"), Relation("rascal_fox", 1))),
            n("won", "All right, all right, you win. Take your sticky page. I only wanted to look at it.", listOf(bye()), listOf(Give("recipe_page"), SetFlag("recipe_got"), Relation("rascal_fox", 1))),
            n("lost", "Ha! Too slow. Try again, or bring some coins.", listOf(bye())),
        ),
    )

    val lumi = Npc(
        "lumi_lamp", "Lumi", "Lantern-keeper of Lantern Hollow", Who.LUMI,
        "Lumi lights every lantern in Lantern Hollow at dusk, all thousand of them, all by herself. She dreams of lighting the whole kingdom, and she is a little scared of the dark herself.",
        "Hello! I am Lumi. I keep the lanterns. All of them. It takes a while.",
        listOf(Start("bat_friends", Flag("friend:bat_king")), Start("hub")),
        listOf(
            n(
                "hub", "Would you like something to light your way?",
                listOf(
                    o("Look at the lamps", "talk_buy", null, listOf(Shop("lampwright"))),
                    o("Ask about the lanterns", "talk_ask", "lanterns", needs = listOf(Not(ArcIs("lantern_night")))),
                    o("Ask about the lanterns", "talk_ask", "missing", needs = listOf(ArcIs("lantern_night"))),
                    o("Be her friend", "talk_give", "gift", listOf(SetFlag("lumi_gift"), Relation("lumi_lamp", 2)), needs = listOf(Stat(Attribute.KINDNESS, 2), NoFlag("lumi_gift"))),
                    bye(),
                ),
            ),
            n("lanterns", "Every dusk I climb every ladder in town. The bats keep stealing the little ones, so the Bat King must have a very bright cave. Do not tell the bats I am scared of the dark.", listOf(o("Your secret is safe", "talk_yes", "hub"))),
            n(
                "missing", "The bats took them, every one. But the strange thing is that they do not hurt anybody. I followed them once, all the way to the old belfry. They just sit there, holding the lanterns tight, and shaking. I think they are as scared of the dark as I am.",
                listOf(o("They are scared too", "talk_think", "hub")), listOf(SetFlag("knows_bats_afraid")),
            ),
            n(
                "bat_friends", "The bats come every dusk now, and they bring their own lanterns to the party. I am not scared of the dark anymore. Not with so many friends. Would you like to look at the lamps?",
                listOf(o("Look at the lamps", "talk_buy", null, listOf(Shop("lampwright"))), bye()),
            ),
            n("gift", "You do not think I am silly? Then here, take my best lantern. Now you will never be in the dark alone, and I will not be either.", listOf(o("Thank you, Lumi", "talk_yes", "hub")), listOf(Give("lantern"))),
        ),
        shop = "lampwright",
    )

    val hob = Npc(
        "captain_hob", "Captain Hob", "Captain of the Guard", Who.HOB,
        "Hob has guarded Lantern Hollow for twenty years. He worries about everything, but most of all about Windy Pass, where a bandit has been taking tolls from his travelers.",
        "Halt! Oh, sorry, habit. I am Captain Hob, and I keep Lantern Hollow safe.",
        listOf(Start("thanks", Flag("bess_befriended"), NoFlag("hob_thanked")), Start("hub")),
        listOf(
            n(
                "hub", "What can the Guard do for you?",
                listOf(
                    o("Ask about the pass", "talk_ask", "pass"),
                    o("Ask for training", "talk_fight", "training", listOf(SetFlag("run:hob_training"), Stars(Attribute.COURAGE, 5)), needs = listOf(NoFlag("run:hob_training"))),
                    bye(),
                ),
            ),
            n("pass", "A bandit named Bess holds the pass and takes a toll from everyone. I have tried to catch her, but she is too quick. If you meet her, maybe try talking first? Fighting never worked for me.", listOf(o("I will try", "talk_yes", "hub", listOf(SetFlag("hob_asked"))))),
            n("training", "Feet apart, chin up, eyes on the puzzle. That is all there is to it. Now you are a little braver.", listOf(o("Thank you, Captain", "talk_yes", "hub"))),
            n("thanks", "You talked to Bess, and she stopped taking tolls? Hob the Brave bows to you, little hero. Take these, you have earned them.", listOf(bye()), listOf(Give("bubble_shield", 2), SetFlag("hob_thanked"), Relation("captain_hob", 3))),
        ),
    )

    val merlo = Npc(
        "old_merlo", "Old Merlo", "Wizard of Lantern Hollow", Who.MERLO,
        "Merlo was once the greatest wizard in the land, until he forgot half of his spells. He is very forgetful and very kind, and he knows a bottle that scares off the Baron's sticky ink.",
        "Ah! A visitor! I am Merlo. Wizard. Retired. Mostly retired. I forget.",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "Now what was I going to say?",
                listOf(
                    o("Look at his spells", "talk_buy", null, listOf(Shop("spells"))),
                    o("Ask about the Baron's ink", "talk_ask", "ink", needs = listOf(ArcIs("missing_pages"))),
                    bye(),
                ),
            ),
            n(
                "ink", "The Baron's ink! Yes, yes. It sticks to everything. I have a bottle that washes it right off, but I have forgotten where I put it. Solve my puzzle, or give me a cookie, and my memory will come back.",
                listOf(
                    o("Solve his puzzle", "talk_puzzle", null, listOf(Puzzle("RIDDLE", "cleaner", "forgot", Skill.LETTERS, "Merlo tries to remember the first letter of his bottle spell.")), needs = listOf(NoFlag("run:merlo_cleaner"))),
                    o("Give him a cookie", "talk_cookie", "cleaner", listOf(Take("friendship_cookie")), needs = listOf(HasItem("friendship_cookie"), NoFlag("run:merlo_cleaner"))),
                    bye(),
                ),
            ),
            n("cleaner", "Ah, there it is, under my hat! The Ink Cleaner. It is the one thing the Baron truly fears. Use it on him when the moment is right.", listOf(bye()), listOf(Give("ink_cleaner"), SetFlag("run:merlo_cleaner"), Relation("old_merlo", 2))),
            n("forgot", "Hmm, no, that is not it. Come back and ask again. I will remember by then, I hope.", listOf(bye())),
        ),
        shop = "spells",
    )

    val finn = Npc(
        "finn_fisher", "Finn the Fisherman", "Fisherman of the Dock", Who.FINN,
        "Finn has fished the lake for thirty years. His rowboat has been missing for a month, and the whole village has had to go the long way round the river without it.",
        "Ahoy there! I am Finn. Best fisherman on the lake. Worst luck in the kingdom, lately.",
        listOf(Start("boat", Flag("grumble_befriended"), NoFlag("finn_boat")), Start("hub")),
        listOf(
            n(
                "hub", "Care to buy some supplies?",
                listOf(
                    o("Look at his stall", "talk_buy", null, listOf(Shop("dockstall"))),
                    o("Ask about his boat", "talk_ask", "story"),
                    bye(),
                ),
            ),
            n("story", "My boat went missing from the dock a month ago. A big troll was seen heading off with it toward Old Stone Bridge. If somebody could just ask him nicely, I would be ever so grateful.", listOf(o("I will ask him", "talk_help", "hub"))),
            n(
                "boat", "My boat! Grumble brought it back himself, and he was blushing! Everyone can use the river ferry now. Here, take this rope with my thanks.",
                listOf(bye()), listOf(Give("rope"), SetFlag("finn_boat"), Relation("finn_fisher", 3)),
            ),
        ),
        shop = "dockstall",
    )

    val ribbit = Npc(
        "sir_ribbit", "Sir Ribbit", "Frog knight", Who.RIBBIT,
        "Sir Ribbit is the smallest and bravest knight in Whisperwood. He is also the only frog knight, and he has been waiting years for a certain kind of beans to pass his knight exam.",
        "Halt, stranger! I am Sir Ribbit, knight of the lake. Do you carry any beans?",
        listOf(Start("beans", HasItem("magic_beans"), NoFlag("ribbit_beans")), Start("hub")),
        listOf(
            n(
                "hub", "Nobody ever brings me beans!",
                listOf(
                    o("Ask about the beans", "talk_ask", "why"),
                    o("Tell a knightly joke", "talk_laugh", "joke", listOf(SetFlag("run:ribbit_joke"), Stars(Attribute.KINDNESS, 5)), needs = listOf(NoFlag("run:ribbit_joke"))),
                    bye(),
                ),
            ),
            n("why", "To pass my knight exam I must bring my teacher a bag of magic beans. Nobody has any. Well, the fox at the market has some, but he says they cost a fortune.", listOf(o("I will see what I can do", "talk_yes", "hub"))),
            n("joke", "Ha! That was a knightly joke indeed! You have the heart of a true knight.", listOf(o("Thank you", "talk_laugh", "hub"))),
            n(
                "beans", "Magic beans! You have magic beans! <narrator>Sir Ribbit takes the bag and does a happy little hop. <ribbit>I have passed my exam! Take this, it is the least I can do.",
                listOf(bye()), listOf(Take("magic_beans"), Give("hint_scroll"), SetFlag("ribbit_beans"), Relation("sir_ribbit", 3)),
            ),
        ),
    )

    val grumble = Npc(
        "grumble", "Grumble the Troll", "Troll of Old Stone Bridge", Who.GRUMBLE,
        "Grumble has guarded the bridge since his friends moved away. He took Finn's boat to build a raft so he could visit them, but he is too shy to say so, so he just growls at everyone.",
        "Stop! This is my bridge. Nobody crosses without paying the toll. Three coins.",
        listOf(Start("friend", Flag("grumble_befriended")), Start("passed", Flag("run:grumble_paid")), Start("passed", Flag("run:grumble_beaten")), Start("hub")),
        listOf(
            n(
                "hub", "Well? Are you paying, or not?",
                listOf(
                    o("Pay three coins", "talk_coin", "paid", listOf(Pay(3)), needs = listOf(Coins(3), NoFlag("run:grumble_paid"))),
                    o("Ask him how he is", "talk_listen", "story", listOf(Relation("grumble", 1))),
                    o("Fight him", "talk_fight", null, listOf(Fight("grumble_troll", "beaten"))),
                ),
            ),
            n("paid", "Fine. Cross, then. Nobody ever asks how I am, you know.", listOf(o("How are you?", "talk_listen", "story", listOf(SetFlag("run:grumble_paid"), Relation("grumble", 1))), bye()), listOf(SetFlag("run:grumble_paid"))),
            n(
                "story", "Nobody asks. My friends moved to the far side of the lake, and I am too shy to visit. I took a boat to make a raft, but I am too scared to use it. Grumble the Brave, ha.",
                listOf(
                    o("Share a cookie", "talk_cookie", "befriend", listOf(Take("friendship_cookie")), needs = listOf(HasItem("friendship_cookie"))),
                    o("Offer to be his friend", "talk_give", "befriend", needs = listOf(Stat(Attribute.KINDNESS, 2))),
                    bye(),
                ),
            ),
            n(
                "befriend", "You... you want to be my friend? <narrator>A very big tear rolls down a very big nose. <grumble>I will take the boat back to Finn right away. And you may cross any time. Friends do not pay tolls.",
                listOf(bye()), listOf(SetFlag("grumble_befriended"), SetFlag("friend:grumble_troll"), Relation("grumble", 5), Heal(99)),
            ),
            n("beaten", "Ow. All right, you win. Cross, if you must.", listOf(bye()), listOf(SetFlag("run:grumble_beaten"))),
            n("passed", "We have settled things, you and I. Cross the bridge whenever you like.", listOf(bye("Wave goodbye"))),
            n("friend", "My friend! Cross whenever you like. Finn has his boat back, and I have been practicing my waving.", listOf(bye("Wave goodbye"))),
        ),
        passFlags = listOf("run:grumble_paid", "grumble_befriended", "run:grumble_beaten"),
    )

    val bess = Npc(
        "bandit_bess", "Bandit Bess", "Bandit of Windy Pass", Who.BESS,
        "Bess is not really a bad person. She has a little brother to feed and no other way to do it. She takes tolls from travelers because nobody ever gave her a job.",
        "Halt! This is my pass. Five coins to cross, or play for it.",
        listOf(Start("friend", Flag("bess_befriended")), Start("passed", Flag("run:bess_paid")), Start("passed", Flag("run:bess_beaten")), Start("hub")),
        listOf(
            n(
                "hub", "Well? What will it be?",
                listOf(
                    o("Pay five coins", "talk_coin", "paid", listOf(Pay(5)), needs = listOf(Coins(5), NoFlag("run:bess_paid"))),
                    o("Sing her a song", "talk_sing", "song", needs = listOf(Stat(Attribute.KINDNESS, 2))),
                    o("Fight her", "talk_fight", null, listOf(Fight("bandit_bess", "beaten"))),
                    bye("Back away"),
                ),
            ),
            n("paid", "Pleasure. Off you go. And no, I am not saving up for anything. Do not ask me about my brother.", listOf(o("Tell me about your brother", "talk_listen", "brother", listOf(SetFlag("run:bess_paid"))), bye()), listOf(SetFlag("run:bess_paid"))),
            n("song", "A song? For me? <narrator>You sing the softest, sweetest song, and the bandit wipes her eyes. <bess>Nobody ever sang to me. All right. Tell me what you want.", listOf(o("Tell me about your brother", "talk_listen", "brother"))),
            n(
                "brother", "My little brother is hungry, and I only know how to take. Captain Hob does not give out jobs to bandits.",
                listOf(o("Hob wants to help you", "talk_give", "friend_made", needs = listOf(Flag("hob_asked"))), o("Give her some coins", "talk_coin", "friend_made", listOf(Pay(5)), needs = listOf(Coins(5))), bye()),
            ),
            n(
                "friend_made", "Hob would give me a job? And you would help me? Then I do not need to be a bandit at all! <narrator>Bess shows you a secret path over the mountain that only she knows. <bess>Use it any time, friend.",
                listOf(bye()), listOf(SetFlag("bess_befriended"), SetFlag("friend:bandit_bess"), Relation("bandit_bess", 5)),
            ),
            n("beaten", "Ow. You are tougher than you look. Go on, then. The pass is yours.", listOf(bye()), listOf(SetFlag("run:bess_beaten"))),
            n("passed", "We are square, you and me. Off you go.", listOf(bye("Wave goodbye"))),
            n("friend", "My friend! The secret path is always open for you. And tell Hob I start my new job on Monday.", listOf(bye("Wave goodbye"))),
        ),
        passFlags = listOf("run:bess_paid", "bess_befriended", "run:bess_beaten"),
    )

    val fern = Npc(
        "fern_fairy", "Fern the Fairy", "Fairy of the Ring", Who.FERN,
        "Fern is a tiny fairy who lives in a ring of glowing mushrooms. She is very shy, and she gives gifts to anyone who dances without laughing at her wings.",
        "Oh! A visitor! I am Fern. Please do not laugh at my wings. They are a bit wobbly.",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "Would you like to dance in the ring?",
                listOf(
                    o("Dance with her", "talk_laugh", "dance", listOf(Heal(99), SetFlag("run:fern_dance")), needs = listOf(NoFlag("run:fern_dance"))),
                    o("Ask for a wish", "talk_give", "wish", listOf(SetFlag("fern_wish")), needs = listOf(Stat(Attribute.KINDNESS, 2), NoFlag("fern_wish"))),
                    bye(),
                ),
            ),
            n("dance", "You dance and dance, and you do not laugh at all! The mushrooms glow brighter, and you feel as light as a feather.", listOf(o("That was fun", "talk_yes", "hub"))),
            n("wish", "You are so kind! Here, a little luck from the ring. May it help you when you need it most.", listOf(bye()), listOf(Give("lucky_clover"))),
        ),
    )

    val henrietta = Npc(
        "henrietta_hen", "Henrietta Hen", "Hen of Sleepy Meadow", Who.HENRIETTA,
        "Henrietta is a hen with a great many chicks and very little time. She loses at least one every day, and it is always the naughtiest.",
        "Cluck, cluck, oh my feathers! I am Henrietta, and I have lost a chick again!",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "Can you count my chicks?",
                listOf(
                    o("Count the chicks", "talk_puzzle", null, listOf(Puzzle("CLIMB", "found", "missed", Skill.COUNTING, "My chicks are hiding under the mushrooms, one chick under each.", Thing.MUSHROOM)), needs = listOf(NoFlag("run:hen_chicks"))),
                    bye(),
                ),
            ),
            n("found", "You counted every mushroom, so every chick is found! You are a very clever counter. Please take a honey cake, and a coin for your trouble.", listOf(bye()), listOf(Give("honey_cake"), Earn(4), SetFlag("run:hen_chicks"))),
            n("missed", "Oh dear, I think one chick is still hiding. Come back and help me count again.", listOf(bye()), listOf(SetFlag("run:hen_chicks"))),
        ),
    )

    val otto = Npc(
        "otto_otter", "Otto the Otter", "Otter of the Falls", Who.OTTO,
        "Otto plays in the waterfall all day and knows every secret of the pool. He loves to splash, and he loves to share his favorite hidden cave with anyone who plays along.",
        "Splash! Hello! I am Otto. Do you want to see something amazing?",
        listOf(Start("hub")),
        listOf(
            n(
                "hub", "Do you want to play in the falls?",
                listOf(
                    o("Hop across the stones", "talk_puzzle", null, listOf(Puzzle("CROSS", "cave", "splash", Skill.SKIP_COUNTING, "Hop across the stones behind the waterfall.")), needs = listOf(NoFlag("run:otto_cave"))),
                    o("Splash around", "talk_laugh", "splash_fun", listOf(Heal(99), SetFlag("run:otto_splash")), needs = listOf(NoFlag("run:otto_splash"))),
                    bye(),
                ),
            ),
            n("cave", "You hopped across all the stones! Come see my secret cave behind the falls. There is always something shiny in here.", listOf(bye()), listOf(Earn(8), Give("spark_bomb"), SetFlag("run:otto_cave"))),
            n("splash", "Splash! You fell in. That is okay! Everybody falls in. Try again some time.", listOf(bye()), listOf(SetFlag("run:otto_cave"))),
            n("splash_fun", "Splash, splash, splash! That was the best splash of the day. You feel wonderful.", listOf(o("That was fun", "talk_laugh", "hub"))),
        ),
    )

    val all = listOf(hoot, tilly, bun, willow, hazel, brogan, zig, fox, lumi, hob, merlo, finn, ribbit, grumble, bess, fern, henrietta, otto)
}
