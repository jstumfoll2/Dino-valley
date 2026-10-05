package com.littledungeon.engine.rpg.story

/** The stories of Book One. Arc one is the first the player sees; the others come up in any order after it. */
object CoreArcs {
    /**
     * "The Missing Pages": the Great Storybook is losing its pages, and Baron Grumblewick has been
     * taking them. The way in is through Inkwell Cellars, where his imp guards the silver quill.
     */
    val missingPages = Arc(
        id = "missing_pages",
        title = "The Missing Pages",
        lairId = "lair_manor",
        keyDungeonId = "inkwell_cellars",
        keyItemId = "silver_quill",
        bossId = "baron_grumblewick",
        setup = listOf(
            "The Great Storybook of Whisperwood is losing its pages! One by one, the stories are vanishing: bedtime tales, bakery recipes, even the song of the Old Stone Bridge.",
            "<hoot>Hello, little adventurer. I am Professor Hoot, and I keep the Storybook. The pages are gone, and I know who took them. Baron Grumblewick, from the crooked manor.",
            "<pet>The grumpy man who never smiles? Why would he want stories?",
            "<hoot>Nobody knows. But his gate is sealed with sticky black ink. Only his own silver quill can write it open, and his imp guards it in the Inkwell Cellars.",
        ),
        sealed = "The manor gate is sealed with sticky black ink! <pet>We need the Baron's silver quill. The Professor said it is in the Inkwell Cellars.",
        keyFound = "<pet>The silver quill! Now we can write the gate open. Let's go to the manor!",
        gateOpens = "You write one big word with the silver quill, and the ink on the gate runs away like a scared spider. The gate swings open.",
        ask = "What will you do about the Baron?",
        fightLabel = "Challenge the Baron",
        peaceLabel = "Listen to the Baron",
        peaceSteps = listOf(
            PeaceStep(
                "letters", "<baron>If you want to hear my story, you must write it down. Find me the right letter.", "<baron>Yes, that is the letter! Nobody has written my words down in years.",
                skippedBy = "knows_baron_lonely",
                skipNote = "<baron>You already know why I stopped writing. Nobody has ever understood me so quickly. We can skip the first page.",
            ),
            PeaceStep("pattern", "<baron>Every story has a pattern, like a song. Which one comes next?", "<baron>Round and round, just like my days. You understand."),
            PeaceStep("colors", "<baron>And a story needs color. I only ever had gray ink. Find me the right color.", "<baron>Oh, so bright! I had forgotten what color looks like."),
        ),
        returnSetup = listOf(
            "Another page of the Great Storybook has gone missing! The ink is spreading again.",
            "<hoot>Little adventurer, you are back! The pages are drifting off once more, and I fear it is the Baron's ink. Please, go and see what has happened.",
        ),
        friendMeeting = "The manor door is open, and the Baron waits inside with his hat in his hands. <baron>You came back. My ink got away from me again, and the pages ran off in every direction. I am so sorry. Will you help me write them home? <pet>He is asking us for help, not for a fight!",
        friendSteps = listOf(
            PeaceStep("letters", "<baron>Help me write the page home. Find the letter that goes first.", "<baron>Yes! That is the letter. The page is nearly home."),
            PeaceStep("pattern", "<baron>Pages follow patterns, just like days. Which one comes next?", "<baron>Round and round. Thank you, my friend."),
            PeaceStep("colors", "<baron>And now some color, to cheer the page up. Find the right color.", "<baron>Oh, so bright! The page is whole again."),
        ),
        friendEnd = "Every page flutters home, and the Baron writes you into the very first line of the book, in gold. <baron>Whatever else happens, I will always be your friend. <pet>He is smiling again!",
        rivalMeeting = "The Baron slams the door of his study and glares. <baron>You again! The one who beat me. This time my ink is on my side, and I will not be so easily beaten.",
        moments = listOf(
            Moment("mossbrook", "The shelf in the village library is empty. Only a sticky black fingerprint is left. <pet>Somebody took the whole shelf of fairy tales!"),
            Moment("pennywhistle", "A poster is stuck to the market wall. It says, WANTED: ALL STORIES. Signed, B. G. <pet>B. G.? That must be Baron Grumblewick!"),
            Moment(
                "hermit_hill",
                "A single page is stuck in the grass, covered in ink. The only words you can read say, Once upon a time, there was a man who just wanted somebody to listen. <pet>That sounds so sad.",
                effects = listOf(Effect.SetFlag("knows_baron_lonely")),
            ),
            Moment("lantern_hollow", "Every lantern in town is gray with sticky ink, and Lumi is scrubbing as hard as she can. <pet>The Baron's ink is everywhere!"),
            Moment("whispering_falls", "The waterfall runs black for a moment, then clear again, as if somebody upstream washed a very inky pen. <pet>We must be getting close."),
            Moment(
                "fishers_dock",
                "A bottle bobs against the dock with a note inside. It says, Dear anybody. Is anyone there? I wrote you a story. From B. <pet>Oh. He does sound lonely.",
                effects = listOf(Effect.SetFlag("knows_baron_lonely")),
            ),
            Moment("windy_pass", "The wind blows a page right into your hands. It is blank except for one word at the top: Please. <pet>Who is that for?"),
            Moment("inkwell_cellars", "Barrels of ink line the walls, and little black handprints cover every one. <pet>An imp lives here. Be careful!"),
        ),
        variants = listOf(
            Variant(
                "jealous_baron",
                "A tall thin man glares from behind a mountain of pages. <baron>Everybody loves your adventures. Nobody has ever told one about me.",
                "The Baron's top hat flies off, and every page flutters home. He slinks away, muttering. <baron>I only wanted to be somebody's hero. <pet>Maybe one day he will be.",
                "You listen to the Baron's whole sad story. <baron>Would you... write me in? Just a small part? <narrator>You tell him there is room for everyone in a story, and a brand new page appears with his name at the top, in gold. <pet>He is crying happy tears!",
            ),
            Variant(
                "inky_baron",
                "The Baron stands very still, covered in swirling black ink. His eyes are blank. <baron>Mine. All mine. Mine. <pet>Something is wrong with him. The ink is controlling him!",
                "The ink splashes off him in every direction, and the Baron blinks. <baron>What happened? I feel as if I have been asleep for years. <narrator>He gives back every page, and he is too embarrassed to look at anybody. <pet>It was not his fault!",
                "You speak to the Baron softly, and a little ink falls away with every kind word. <baron>Thank you. I think I was lost in my own ink for a long time. <narrator>He hands you every page, and then he plants a tiny garden outside the manor, just to have something that grows. <pet>I like him now.",
            ),
            Variant(
                "lonely_baron",
                "Inside, a tall thin man sits at a huge desk, buried in pages. <baron>You came. Everyone always comes to take my stories away. Nobody ever wants to hear them.",
                "The Baron's top hat tumbles off, and every page flutters up and flies home to the Storybook. The Baron sniffs, picks up his hat, and shuffles away to write all by himself. <pet>He looks so sad. Maybe next time we can be friends.",
                "You sit and listen to the Baron's whole story, and it is sad and funny and very long. <baron>Nobody ever heard it to the end. Take your pages home. And, if you like, I will come and read to the children on Fridays. <pet>He smiled! He really smiled!",
            ),
        ),
    )

    /**
     * "The Dragon Who Did Not Want to Fight": the kingdom blames the dragon on Dragon's Peak for the rumbling at night, and the knights
     * are on their way with a net. The gate up the mountain needs the stone key the Mossy Golem keeps in Gloomwood Mine. The dragon is
     * tired, lonely and afraid of the knights, and what the hero has learned along the way (and which friends they have made) decides
     * how much of its story they already understand.
     */
    val lonelyDragon = Arc(
        id = "lonely_dragon",
        title = "The Dragon Who Did Not Want to Fight",
        lairId = "lair_peak",
        keyDungeonId = "gloomwood_mine",
        keyItemId = "mountain_key",
        bossId = "big_dragon",
        setup = listOf(
            "Every night the mountain rumbles, and a puff of smoke rises from Dragon's Peak. All over Whisperwood, people are whispering about the dragon.",
            "<hoot>Hello again, little adventurer. The knights say the dragon is angry, and they want to chase it away. But the Storybook has a page about the dragon, and its first line says something else.",
            "<pet>What does it say?",
            "<hoot>Once upon a time, there was a dragon who did not want to fight. The gate up the mountain is locked, and its key is in Gloomwood Mine, with the Mossy Golem.",
        ),
        sealed = "A great stone gate bars the way up the mountain, and its lock is the shape of a stone key. <pet>We need the mountain key. The Professor said it is in Gloomwood Mine.",
        keyFound = "<pet>The mountain key! It is warm, as if somebody has been holding it for a hundred years. Let's go up to Dragon's Peak!",
        gateOpens = "The stone key turns with a deep clunk, and the gate swings open. A warm wind blows down from the cave, and it smells like toast.",
        ask = "What will you do about the dragon?",
        fightLabel = "Face the dragon",
        peaceLabel = "Talk to the dragon",
        peaceSteps = listOf(
            PeaceStep(
                "rhyme", "<dragon>Rhymes make me feel calm. Help me make one.", "<dragon>Yes! Those two sound just the same. My shoulders feel lighter already.",
                skippedBy = "knows_dragon_rhymes",
                skipNote = "<dragon>You already know what calms me? Nobody ever guessed. We can skip the rhymes.",
            ),
            PeaceStep(
                "share", "<dragon>I baked pies for a party, but nobody came. Now I have too many.", "<dragon>Yes! Every plate gets the same. That is fair, and it is delicious.",
                skippedBy = "knows_dragon_lonely",
                skipNote = "<dragon>You know I baked those pies for friends who never came? Then you know what to do with them. Let us just eat them together.",
            ),
            PeaceStep("money", "<dragon>I want to buy a little night-light at the market, but I do not know my coins.", "<dragon>Those are just the right coins! Now the dark will not be so dark."),
        ),
        returnSetup = listOf(
            "The mountain is rumbling again, and a new puff of smoke rises from Dragon's Peak.",
            "<hoot>Little adventurer, you are back! I have a feeling the dragon needs a visitor. Please go and see.",
        ),
        friendMeeting = "The dragon waits at the mouth of the cave with a paper crown on its head. <dragon>You came back! I had a bad dream, and I could not wait for morning. Will you help me? <pet>He is asking us for help, not for a fight!",
        friendSteps = listOf(
            PeaceStep("rhyme", "<dragon>Say me a rhyme, so I can sleep.", "<dragon>Yes! Those two sound just the same. My eyes are closing."),
            PeaceStep("share", "<dragon>I made a tray of pies for you and your friends.", "<dragon>That is fair, and it is delicious."),
            PeaceStep("money", "<dragon>Now I want to buy a night-light, so I am never afraid again.", "<dragon>Those are the right coins. Thank you, my friend."),
        ),
        friendEnd = "The dragon curls up around you, warm as a stove, and falls asleep with a smile. The mountain is quiet, and a new page of the Storybook floats down from the roof of the cave. <dragon>Whatever else happens, I will always be your friend. <pet>Sweet dreams, dragon.",
        rivalMeeting = "The dragon's golden eyes narrow. <dragon>You are the one who chased me. I remember. I will not be so easy to beat this time.",
        moments = listOf(
            Moment("mossbrook", "The village children are drawing the dragon on the road in chalk. Every dragon has big sharp teeth, and not one of them is smiling. <pet>I wonder if dragons smile."),
            Moment(
                "mossbrook",
                "The Baron is reading to the children in the village square, and every child wears a paper dragon hat. <pet>The Baron is smiling. He is a storyteller again!",
                needs = listOf(Cond.Flag("friend:baron_grumblewick")),
            ),
            Moment("pennywhistle", "A knight in shiny armor is buying a very long rope and a very big net. <pet>They are going to catch the dragon! We must hurry."),
            Moment(
                "hermit_hill",
                "Hazel tilts their head and listens to the mountain. <hazel>That is no angry roar. It rises and falls, over and over, like somebody humming the same rhyme to get to sleep. <pet>A dragon humming a lullaby?",
                effects = listOf(Effect.SetFlag("knows_dragon_rhymes")),
            ),
            Moment(
                "old_bridge",
                "Grumble waves one very big hand from the bridge. <grumble>I heard the dragon is lonely, like I was. Tell him there is room for one more friend.",
                needs = listOf(Cond.Flag("grumble_befriended")),
                effects = listOf(Effect.SetFlag("knows_dragon_lonely")),
            ),
            Moment(
                "fishers_dock",
                "A bottle bobs against the dock with a note inside, written in very big letters. It says, I MADE TOO MANY PIES. NOBODY CAME. <pet>The dragon has been trying to have a party!",
                effects = listOf(Effect.SetFlag("knows_dragon_lonely")),
            ),
            Moment(
                "windy_pass",
                "Bess waves from her rock. <bess>The knights went up the mountain with a net. I told them it was a bad idea. If you see the dragon, tell him I am on his side.",
                needs = listOf(Cond.Flag("bess_befriended")),
            ),
            Moment("gloomwood_mine", "A message is scratched into the wall of the mine, in very big claw letters. It says, PLEASE BE GENTLE WITH THE GOLEM. HE IS MY FRIEND. <pet>The dragon has a friend! He must be less scary than the knights say."),
            Moment(
                "lair_peak",
                "Brogan the Smith waits by the gate with a plate of slightly burnt toast. <brogan>I did not run this time. I came to say sorry, and I brought breakfast.",
                needs = listOf(Cond.Flag("brogan_braver")),
            ),
        ),
        variants = listOf(
            Variant(
                "collector_dragon",
                "A great gold dragon curls around a heap of odd things: spoons, buttons, a garden gnome, a kite. <dragon>Go away! Everybody who comes here wants to take my things. <pet>Those are not treasures. They are lost things, and he is keeping them safe!",
                "The dragon blinks, and its great head droops. <dragon>I only wanted somebody to stay a little while. <narrator>It gives back every spoon and every button, one at a time, and a page of the Storybook is under the kite. <pet>He looks so sad. Maybe we can visit him one day.",
                "You ask the dragon about every spoon and every button, and it tells you where each one came from. <dragon>Nobody ever asked. Take them home, and tell everyone I am sorry. And, if you like, come back for tea. <narrator>A page of the Storybook was under the kite all along. <pet>A dragon who has tea! I like him.",
            ),
            Variant(
                "sleepless_dragon",
                "A great gold dragon paces up and down with a nightcap on its head. <dragon>I have not slept in a hundred nights. Every time I close my eyes, I roar. <pet>That is the rumbling! He is not angry. He is tired!",
                "The dragon flops onto its back, and its nightcap flies off. <dragon>At last, I can rest. Even if it is because you tired me out. <narrator>It falls asleep, snoring, and a page of the Storybook slides out from under its pillow. <pet>Shh. Let's tiptoe out.",
                "You hum the dragon the quietest song you know. <dragon>That is the nicest thing anybody has ever done for me. <narrator>It closes its eyes, and for the first time in a hundred nights, the mountain is silent. A page of the Storybook lies under its pillow. <pet>Sweet dreams, dragon.",
            ),
            Variant(
                "shy_dragon",
                "A tiny voice comes from behind a huge rock. <dragon>Are you the knights? Please do not be the knights. I am not very good at being scary. <pet>The dragon is hiding from us!",
                "The dragon peeks out and puffs a tiny cloud of smoke. <dragon>I give up! You are much better at this than I am. <narrator>It hands over a page of the Storybook, with a shy smile. <pet>He is not so scary after all.",
                "You sit down beside the rock and wait. After a while, a golden nose pokes out, and then a whole shy dragon. <dragon>Nobody ever waited for me before. <narrator>It hands you a page of the Storybook, and a warm, shy hug. <pet>We made a friend!",
            ),
        ),
    )

    /**
     * "The Night of a Thousand Lanterns": the bats have taken Lantern Hollow's lanterns on the eve of its festival. The Bat King and his bats are
     * afraid of the dark and have never been asked to a party. The belfry stairs are black, so the hero needs glow-silk from the Spider Caves.
     */
    val lanternNight = Arc(
        id = "lantern_night",
        title = "The Night of a Thousand Lanterns",
        lairId = "lair_belfry",
        keyDungeonId = "spider_caves",
        keyItemId = "glow_silk",
        bossId = "bat_king",
        setup = listOf(
            "Tonight is the Festival of a Thousand Lanterns in Lantern Hollow! But something has gone wrong. One by one, the lanterns are vanishing, and the town is getting darker and darker.",
            "<hoot>A little bird just brought me a letter from Lumi, the lantern-keeper. A flock of bats comes every night and takes her lanterns. She is a bit afraid of the dark, you know.",
            "<pet>Why would bats take lanterns? Bats can see in the dark!",
            "<hoot>That is just what I wonder. Their leader, the Bat King, lives in the old belfry, but its stairs are pitch black. You will need glow-silk. The Spider Queen keeps some in the Spider Caves.",
        ),
        sealed = "The stairs up to the belfry are as black as ink, and you cannot see your own feet! <pet>We need glow-silk to light the way. The Professor said the Spider Queen has some in the Spider Caves.",
        keyFound = "<pet>Glow-silk! It shines like a tiny star. Now we can climb the stairs to the belfry!",
        gateOpens = "You tie the glow-silk to the stair rail, and it shines all the way to the top. High above, hundreds of tiny eyes blink, and a soft, fluttery sound fills the air.",
        ask = "What will you do about the Bat King?",
        fightLabel = "Chase the bats away",
        peaceLabel = "Talk to the Bat King",
        peaceSteps = listOf(
            PeaceStep(
                "bells", "<batking>If you want to talk, you must listen to my bells first.", "<batking>You have good ears! Nobody ever listened to my bells before.",
                skippedBy = "knows_bats_afraid",
                skipNote = "<batking>You know we are afraid of the dark? Then you already listen better than the knights. You may skip the bells.",
            ),
            PeaceStep("trace", "<batking>My bats cannot find their way when it is dark. Draw them a glowing path.", "<batking>Look, the path glows! My bats can find their way now."),
            PeaceStep("bats", "<batking>We have berries for a lantern party. Share them out fairly.", "<batking>Yes! Everybody gets the same. Not one bat is sad."),
        ),
        returnSetup = listOf(
            "The Festival of Lanterns is here again, and the lanterns are going missing once more! Lumi has sent another letter.",
            "<hoot>Please go and see what the Bat King is up to this time.",
        ),
        friendMeeting = "The Bat King waves a tiny wing from the rafters. <batking>My friend! The lanterns are safe, but the festival is tonight, and my bats do not know how to share. Will you help us? <pet>He is asking for help, not for a fight!",
        friendSteps = listOf(
            PeaceStep("bats", "<batking>We have berries for the party. Share them out, so every bat gets the same.", "<batking>Yes! Everybody gets the same. Not one bat is sad."),
            PeaceStep("bells", "<batking>Now play our festival song on the big bells.", "<batking>What a beautiful song! The whole town can hear it."),
        ),
        friendEnd = "The bats and the town light the lanterns together, a little light on every roof, and the Bat King dances in the air with Lumi. A page of the Storybook glows in the biggest lantern. <batking>Thank you for being my friend. <pet>It is the best festival ever!",
        rivalMeeting = "The Bat King hides behind the biggest bell, and only his crown peeks out. <batking>You are the one who chased my bats away. I am not coming out until you go.",
        moments = listOf(
            Moment("mossbrook", "Baker Bun is hanging paper stars in her window. <pet>For the lantern festival! I hope the lanterns come back in time."),
            Moment(
                "mossbrook",
                "A letter from the Baron is pinned to the library door. It says, Dear Mossbrook, I have written a new story for the lantern festival, and I would be honored to read it aloud. B. G. <pet>He wants to come to the party!",
                needs = listOf(Cond.Flag("friend:baron_grumblewick")),
            ),
            Moment(
                "lantern_hollow",
                "Almost every lantern in town is dark. Lumi holds the last one in both hands, and she is shaking a little. <pet>She is scared!",
                effects = listOf(Effect.SetFlag("knows_bats_afraid")),
            ),
            Moment(
                "lantern_hollow",
                "Lumi holds up the lantern she gave you last time, and it still shines. <lumi>Thank you for being my friend. I am not so scared when you are here.",
                needs = listOf(Cond.Flag("lumi_gift")),
            ),
            Moment("spider_caves", "The webs in the cave glow like tiny lamps. The spiders spin them to see by. <pet>Everybody is afraid of the dark, even in a cave!", effects = listOf(Effect.SetFlag("knows_bats_afraid"))),
            Moment("inkwell_cellars", "A single bat hangs from the ceiling, holding a lantern in its wing. When it sees you, it squeaks and flaps away. <pet>It is so little, and so scared."),
            Moment(
                "windy_pass",
                "Bess waves from her rock. <bess>Hundreds of bats went by with lanterns, heading for the old belfry. Not one of them stole from me. Odd, for a thief.",
                needs = listOf(Cond.Flag("bess_befriended")),
            ),
        ),
        variants = listOf(
            Variant(
                "scared_bats",
                "A tiny crowned bat hangs upside down from the biggest bell. Hundreds of bats cluster behind him, each one holding a lantern. <batking>Please do not be cross! We are so scared of the dark, and the lanterns are so warm. <pet>The bats are afraid of the dark! That is why they took the lanterns.",
                "The Bat King squeaks, and all the bats flutter out of the belfry in a big black cloud, dropping every lantern. <batking>We only wanted some light. <narrator>The lanterns tumble down the stairs, still glowing, with a page of the Storybook tucked inside the biggest one. <pet>They were so scared. Maybe we should have asked first.",
                "You listen to the Bat King, and then you ring the bells together. <batking>We never knew they could make a song! <narrator>The bats give back every lantern, and Lumi gives each bat a tiny glowing lamp of its own. A page of the Storybook is tucked inside the biggest lantern. <pet>The whole town is lit up, and the bats are at the party!",
            ),
            Variant(
                "uninvited_bats",
                "The Bat King flaps out of the shadows with a lantern in each wing. <batking>Everyone in Lantern Hollow has a party, and nobody ever invites the bats. We only wanted to see what a lantern festival looks like. <pet>He was not invited! That is why he took the lanterns.",
                "The bats scatter, and the Bat King drops the last lantern. <batking>I only wanted to come to the party. <narrator>The lanterns float back down to the town, and a page of the Storybook is tangled in the very last one. <pet>Maybe next year we will invite them.",
                "You tell the Bat King that the festival has room for everyone with wings, too. <batking>Really? Truly? <narrator>The bats hang the lanterns in a big glowing circle, and every one flies home with an invitation. A page of the Storybook is hidden behind the biggest bell. <pet>Bats at a party! I like that.",
            ),
            Variant(
                "playful_bats",
                "The Bat King zooms around the belfry, laughing. <batking>Catch me if you can! I hid the lanterns, and you will never find them! <pet>He thinks it is a game!",
                "You catch the Bat King in a net of glowing silk, and he stops laughing. <batking>That was not a very fun game. <narrator>He lets the lanterns go, one by one, and a page of the Storybook drops out of his crown. <pet>He is not a bad bat. He just wanted somebody to play with.",
                "You say yes, and you play hide and seek with the Bat King until every lantern is found. <batking>Nobody ever played with me before! <narrator>The bats give back all the lanterns, and a page of the Storybook falls out of the Bat King's crown. <pet>That was the best game ever.",
            ),
        ),
    )

    val all = listOf(missingPages, lonelyDragon, lanternNight)
}
