package com.littledungeon.engine.rpg.story

/** The first stories. Arc one is the first the player sees and is fully told; the rest follow in later work. */
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

    val all = listOf(missingPages)
}
