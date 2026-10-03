package com.dinovalley.engine.rpg.story

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
            "<elder>Hello, little adventurer. I am Professor Hoot, and I keep the Storybook. The pages are gone, and I know who took them. Baron Grumblewick, from the crooked manor.",
            "<pet>The grumpy man who never smiles? Why would he want stories?",
            "<elder>Nobody knows. But his gate is sealed with sticky black ink. Only his own silver quill can write it open, and his imp guards it in the Inkwell Cellars.",
            "Here is the map of Whisperwood. Many roads lead to the manor. Visit the towns, make friends, and find what you need. Every friend you make could help!",
        ),
        sealed = "The manor gate is sealed with sticky black ink! <pet>We need the Baron's silver quill. The Professor said it is in the Inkwell Cellars.",
        keyFound = "<pet>The silver quill! Now we can write the gate open. Let's go to the manor!",
        gateOpens = "You write one big word with the silver quill, and the ink on the gate runs away like a scared spider. The gate swings open.",
        ask = "What will you do about the Baron?",
        fightLabel = "Challenge the Baron",
        peaceLabel = "Listen to the Baron",
        peaceSteps = listOf(
            PeaceStep("letters", "<baron>If you want to hear my story, you must write it down. Find me the right letter.", "<baron>Yes, that is the letter! Nobody has written my words down in years."),
            PeaceStep("pattern", "<baron>Every story has a pattern, like a song. Which one comes next?", "<baron>Round and round, just like my days. You understand."),
            PeaceStep("colors", "<baron>And a story needs color. I only ever had gray ink. Find me the right color.", "<baron>Oh, so bright! I had forgotten what color looks like."),
        ),
        variants = listOf(
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
