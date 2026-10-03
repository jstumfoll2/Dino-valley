package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.learn.PotionKind
import com.dinovalley.engine.rpg.world.Quest
import com.dinovalley.engine.rpg.world.QuestKind
import com.dinovalley.engine.rpg.world.Twist
import com.dinovalley.engine.rpg.world.WorldMemory
import kotlin.random.Random

/**
 * Everything the narrator says, with several versions of most lines so no two adventures
 * sound the same. "{name}" is the baby dragon's name in the child's own voice.
 * Recorded family voice lines can replace these later, line by line.
 */
internal class Lines(private val r: Random) {
    private fun pick(vararg options: String) = options[r.nextInt(options.size)]

    // ------------------------------------------------------------- the story's beginning

    fun intro(q: Quest): String = when (q.kind) {
        QuestKind.DRAGONS_PRISONER ->
            "Oh no! Princess Ruby has disappeared! The knights say the dragon ${q.dragon} took her. She is somewhere deep in ${q.dungeon}. " +
                "But nobody has asked the dragon. Will you go and find out what really happened?"
        QuestKind.LONELY_DRAGON ->
            "Things keep going missing in the kingdom: spoons, buttons, even the queen's shiny hat! " +
                "Everyone says the dragon ${q.dragon} took them. They are hidden in ${q.dungeon}. Let's go and see!"
        QuestKind.RUBYS_QUEST ->
            "Princess Ruby didn't wait to be rescued. She climbed down from her tower all by herself! " +
                "Now she needs your help. Something in ${q.dungeon} is scribbling on the magic Storybook."
    }

    /** The companion notices the child: past adventures and what they're good at. */
    fun remember(world: WorldMemory, strongest: Attribute?): String? {
        val bits = mutableListOf<String>()
        if (world.adventures == 0) return "{name} flaps its little wings. <pet>I'm coming too! Let's be brave together!"
        world.dragonFriend?.let { bits += "Last time, you made friends with the dragon $it." }
        world.friends.firstOrNull()?.let { if (bits.isEmpty()) bits += "Your goblin friend $it says hello!" }
        when (strongest) {
            Attribute.CLEVERNESS -> bits += "<pet>You're so clever with numbers!"
            Attribute.MAGIC -> bits += "<pet>The magic sparkles when you're near!"
            Attribute.WISDOM -> bits += "<pet>You know so many letters!"
            Attribute.KINDNESS -> bits += "<pet>You're the kindest adventurer in the kingdom!"
            Attribute.COURAGE -> bits += "<pet>You're so brave!"
            null -> Unit
        }
        return bits.joinToString(" ").ifEmpty { null }
    }

    fun toTheMap(q: Quest) = pick(
        "Here is the map of ${q.dungeon}. We start at the camp, and the dragon's lair is at the very end. Off we go!",
        "Look at the map of ${q.dungeon}! The path goes from our camp all the way to the lair. Adventure time!",
        "{name} points at the map of ${q.dungeon}. <pet>At every fork in the path, you get to pick a door! <narrator>Let's go!",
    )

    // ------------------------------------------------------------- doors and maps

    fun pickDoor() = pick("Which door will you open?", "Which door should we try?", "Hmm, which way? You choose!")

    fun treasureSniff() = pick("{name} sniffs the air. The right path is", "A little bird sings. [tweet] The right path is", "The map glows. The right path is")

    /** Before a room through a door the clue didn't point to: the path will wind back to the doors. */
    fun wrongWay() = pick(
        "Hmm, that wasn't the path from the clue. <pet>Let's see what's in here anyway!",
        "<pet>Oops, I don't think that was the right path! <narrator>Let's see what's in here.",
    )

    /** After that room: the path loops around to the same doors. */
    fun circleBack() = pick(
        "Oh no! This path goes round and round in a circle. We're back at the doors! <pet>Let's listen to the clue about the right path again.",
        "Whoops, this was the wrong path. It leads right back to the doors! <pet>We went in a circle! Let's try another way.",
    )


    fun rightWay() = pick("You found the right path!", "Good listening! That's the path from the clue.")

    /** The right door, reached after going the wrong way first. */
    fun lastWay() = pick("This time it's the path from the clue!", "That's the way! <pet>We found the right path!")

    // ------------------------------------------------------------- rooms

    fun gate(q: Quest) = pick(
        "You reach the gate of ${q.dungeon}. It's stuck tight! <pet>Oof, it won't budge!",
        "Here's the big old gate of ${q.dungeon}. <pet>Uh oh, it won't open!",
    )

    fun gateRoll() = "Roll the dice to see how hard you push the gate!"

    fun runeDoor() = pick(
        "A huge stone door blocks the way. Magic symbols glow on it. <pet>Ooh, they're so pretty!",
        "An ancient door covered in glowing runes! It hums softly. <pet>I wonder what the symbols mean.",
    )

    fun runeOops() = pick(
        "The door sneezes! [sneeze] Dust everywhere. Let's try again.",
        "[zap] The runes giggle and wiggle. Not that one. Try again!",
        "The door hums, \"Hmm, no, thank you!\" <pet>Try another one!",
    )

    fun runeYay() = pick(
        "You got it! The ancient door begins to shake... [rumble] It's open! <pet>Hooray!",
        "Yes! The runes shine brightly, and the door slides open!",
    )

    fun bridge() = pick(
        "A wobbly bridge stretches over a deep, deep gap. Some of its stones are loose! <pet>Eek! It's so high up!",
        "A rope bridge! It sways in the wind. Let's count its stones to make sure it's strong.",
    )

    fun bridgeOops() = pick(
        "The bridge wobbles and shakes, and a bird flies away. Let's count again.",
        "Oops! A stone falls into the water far below. [splash] Let's try again.",
    )

    fun bridgeYay() = pick("You got it! The stones click into place. <pet>The bridge is strong!", "Yes! [stomp] The bridge holds tight! <pet>Whee, let's cross!")

    fun crystalCave() = pick(
        "A cave full of glittering crystals! A tiny wizard waves her hat. <wizard>Oh, hello! My lantern went out, and I can't see!",
        "Crystals of every color shine in this cave. A little wizard is stuck here in the dark! <wizard>Please help me! <pet>Don't worry, we're here!",
    )

    fun crystalOops() = pick(
        "<wizard>Let me try it! <narrator>[toot] The lantern toots like a trumpet! <wizard>Oh dear, that's the wrong crystal! <pet>Hee hee, it's so loud!",
        "<wizard>Ooh, how about this one? <narrator>[poof] The crystal turns the wizard's hat into a bunny! <wizard>Hee hee, not that one! <pet>I like the bunny!",
    )

    fun crystalYay() = pick(
        "Her lantern glows brightly. <wizard>Thank you! Here, take a gem. <pet>Ooh, shiny!",
        "Yes! The lantern lights up the whole cave. <wizard>Hooray, I can see! This gem is for you. <pet>We did it!",
    )

    fun library() = pick(
        "Books fly around like birds! A spell book lies open, but one letter of its spell is missing. <pet>Whoa, look at them go!",
        "A magic library! Shhh. A spell book needs help finishing its spell. <pet>Shhh! I'll be quiet.",
    )

    fun letterPurpose() = pick("The spell needs one more letter.", "The spell book is waiting for its magic letter.", "One magic letter will calm the flying books.")

    fun libraryOops() = pick(
        "[whoosh] The books fly faster! <pet>That's not the right letter!",
        "The spell book giggles, \"That tickles!\" Try another letter.",
    )

    fun libraryYay() = pick("[whoosh] The books settle back on their shelves. The spell is finished!", "You got it! The spell book glows with happy magic! <pet>Hooray!")

    fun tunnel() = pick(
        "This tunnel is so dark! Magic letters on the wall light the way, but they are fading. <pet>I'm not scared. Much.",
        "A dark, twisty tunnel. If we write magic letters on the wall, they will glow and light the way!",
    )

    fun writePurpose() = pick("Let's light the tunnel!", "Write with your magic finger!", "Make the wall glow!")

    fun writeAgain() = pick("Now a magic number!", "One more! This time, a number.")

    fun tunnelOops() = pick("The magic fizzles out like a sparkler. Psst! Let's try that part again.", "Oops, the magic wandered off the line! Try again.")

    fun tunnelYay() = pick("It glows! The whole tunnel lights up!", "Your letter shines brightly. Now we can see!")

    fun mirrorHall() = pick(
        "A hall full of mirrors! Only one door is the real way out. Watch carefully...",
        "There are mirrors everywhere! Remember which door is real.",
    )

    fun mirrorOops() = pick("Bonk! That was just a mirror. <pet>Hee hee! Try again.", "<pet>Hello, me! That's just a reflection. <narrator>Let's try again.")

    fun mirrorYay() = pick("That's the real door! <pet>You remembered!", "Yes! The mirrors cheer. <pet>What a memory!")

    fun storeroom() = pick(
        "This is the goblins' storeroom, and what a mess! Gems and coins and keys everywhere. Let's tidy up!",
        "Oh my! The goblins' storeroom is so messy that we can't get through. Let's put things in baskets!",
    )

    fun storeroomOops() = pick("Oops! That basket spits it back out. <pet>It says, that's not mine! Try another one.", "[boing] It bounced out. Does it look like the others in that basket?")

    fun storeroomYay() = pick("All tidy! The goblins will be so happy.", "What a tidy storeroom! Now we can walk through.")

    fun pond() = pick(
        "An underground pond! A frog sits on a lily pad. [ribbit] <pet>It says it only hops when you count the lily pads right!",
        "Here's a quiet pond with lily pads. A little frog wants to hop across, but it needs your help counting!",
    )

    fun pondAgain() = pick("Ribbit! The frog wants to hop again!", "Here come more lily pads!")

    fun pondOops() = pick("[splash] The frog falls in the water. It's okay, frogs love water! Let's count again.", "[ribbit] <pet>The frog says, not that one! <narrator>Let's count again.")

    fun pondYay() = pick("[boing] The frog jumps across!", "You got it! The frog hops all the way!")

    fun mosaic() = pick(
        "A grand hall with a magic picture on the wall. Oh no, it's broken into pieces!",
        "Look at this hall! Its magic picture has cracked apart, and the door won't open until it's fixed.",
    )

    fun mosaicOops() = pick("Hmm, that piece doesn't fit there. Look at the picture!", "That piece belongs somewhere else. Look at the picture!")

    fun mosaicYay() = pick("The picture is whole again! It sparkles, and the door opens.", "You fixed it! The magic picture glows!")

    fun countThenFind() = pick("Now find the number that says how many!", "Can you find that number?")

    fun colorAgain() = pick("Her friend needs a crystal too!", "Another lantern went out!")

    fun letterAgain() = pick("The spell book has one more missing letter.", "Here's another page with a missing letter!")

    fun patternAgain() = "The door has a second lock!"

    // ------------------------------------------------------------- forks

    /** At a fork, each door says what kind of puzzle is behind it, so the path is the child's pick. */
    fun doorOffer(hue: String, activity: String, index: Int, last: Int) = when (index) {
        0 -> "The $hue door has $activity."
        last -> "And the $hue door has $activity."
        else -> "The $hue door has $activity."
    }

    fun vault() = pick("A treasure vault! A big chest with a magic lock sits in the middle.", "Gold glitters everywhere! This is a treasure vault.")

    fun vaultRoll() = "Roll the dice to open the treasure chest!"

    fun vaultOops() = pick("[coins] The coins jump back out of your bag. Let's count again.", "The chest burps! [burp] Let's count again.")

    fun vaultYay() = pick("[coins] Into your bag they go!", "You got it! What a lot of treasure!")

    fun den(goblin: String) = pick(
        "Here's a goblin named $goblin hiding behind a barrel. He's shaking! <goblin>I'm scared of the dark!",
        "A little goblin named $goblin is curled up in a corner. He looks frightened. <goblin>Please don't be loud!",
    )

    fun denFriend(goblin: String) = "It's $goblin the goblin! He jumps up and down. <goblin>My friend! You came back! I'll help you again!"

    fun denAsk(goblin: String) = "<pet>Poor $goblin is frightened! What should we do?"

    fun workshop(potion: PotionKind) = pick(
        "An alchemist's workshop! A big cauldron bubbles. [bubble] Here's a recipe for the ${potion.title}. We might need it!",
        "Bubbles and smoke! This is the alchemist's workshop. Let's make the ${potion.title}!",
    )

    fun potionOops() = pick(
        "[bubble] The potion turns polka-dotted, and that one pops right back out! <pet>Let's check the recipe.",
        "[fizz] Purple smoke, and {name} sneezes sparkles! <pet>Achoo! Not that one. Try again.",
    )

    fun potionYay(potion: PotionKind) = "[poof] You made the ${potion.title}! <pet>It smells yummy!"

    // ------------------------------------------------------------- dice

    fun gateResult(tier: Tier, coins: Int) = when (tier) {
        Tier.SILLY -> pick(
            "The gate swings open... and bonks you on the nose! [boing] But it's open.",
            "You push and push... and your hat flies off and gets stuck on the gate! <pet>Hee hee! <narrator>But the gate opens anyway.",
        )
        Tier.GOOD -> "[creak] The gate opens!"
        Tier.GREAT -> "The gate flies open, and ${coinWords(coins)} roll out!"
        Tier.MAGIC -> "Magic! The gate bows to you and opens all by itself! ${capitalCoins(coins)} sparkle on the ground!"
    }

    fun songResult(tier: Tier, goblin: String) = when (tier) {
        Tier.SILLY -> "Your song is so silly that $goblin giggles and falls over! <goblin>Hee hee! I'm not scared anymore!"
        Tier.GOOD -> "$goblin taps his feet. <goblin>I like it! I'm not scared anymore!"
        Tier.GREAT -> "$goblin dances and claps! <goblin>What a song! <pet>Dance with us!"
        Tier.MAGIC -> "Your song is so beautiful that the walls start to glow! <goblin>Wow! I'm not scared at all now!"
    }

    fun vaultResult(tier: Tier) = when (tier) {
        Tier.SILLY -> "The lid pops open... and a frog jumps out! [ribbit] <pet>Ribbit! <narrator>The treasure is underneath."
        Tier.GOOD -> "[unlock] The chest opens."
        Tier.GREAT -> "The chest flies open! Shiny coins everywhere!"
        Tier.MAGIC -> "The chest sings a song and opens wide. So much treasure!"
    }

    fun coinWords(n: Int) = "${com.dinovalley.engine.rpg.learn.Words.number(n)} ${if (n == 1) "coin" else "coins"}"

    private fun capitalCoins(n: Int) = coinWords(n).replaceFirstChar { it.uppercase() }

    // ------------------------------------------------------------- treasure chests and hearts

    fun chestAppears() = pick(
        "Look! A little treasure chest is hiding in the corner.",
        "{name} found a treasure chest! It has a magic lock.",
        "What's that? A treasure chest with a shiny lock!",
    )

    fun chestLock(what: String) = pick("The lock opens only for the right $what.", "The lock is waiting for its magic $what.", "To open the lock, tap the right $what.")

    fun chestOops() = pick(
        "The lock blows a raspberry! [raspberry] Not that one. Try again!",
        "[clunk] The lock stays shut, and the chest wiggles. Try again!",
    )

    fun chestYay() = pick("[unlock] The lock pops open!", "[unlock] The magic lock sparkles and clicks open!")

    fun chestLoot(n: Int) = "Inside the chest: ${coinWords(n)}!"

    /** Hearts never run out: the companion helps, and the adventure carries on. */
    fun heartsBack() = "Oh no, your hearts are all gone! {name} gives you a magic berry. [munch] All your hearts are back!"

    fun heartsKept(n: Int) = when (n) {
        3 -> "You kept all three hearts! Here are some bonus stars."
        1 -> "You kept one heart. Here are some bonus stars!"
        else -> "You kept ${com.dinovalley.engine.rpg.learn.Words.number(n)} hearts. Here are some bonus stars."
    }

    // ------------------------------------------------------------- the lair

    fun lairWay(potion: PotionKind) = when (potion) {
        PotionKind.GIANT_STRENGTH -> "A giant boulder blocks the lair! You drink the Potion of Giant Strength... You grow ENORMOUS and roll the boulder away!"
        PotionKind.GLOW -> "The lair is pitch dark. You drink the Potion of Glowing, and you shine like a star!"
        PotionKind.BUBBLE -> "A river of warm, bubbly mud blocks the way. The Potion of Bubbles floats you across in a giant bubble!"
        PotionKind.FRIENDSHIP -> "You sprinkle the Potion of Friendship in the air. It smells like warm cookies."
    }

    fun bossReveal(q: Quest): String = when (q.twist) {
        Twist.RUBY_VISITING -> "There's the dragon ${q.dragon}... and Princess Ruby is riding on its back, laughing! <ruby>Hello, friends! <dragon>Please don't be scared. I'm a bit worried about all the knights."
        Twist.TRICKSTER -> "There's the dragon ${q.dragon}, crying big smoky tears. <dragon>Sniff. They say I took Ruby, but I didn't! <narrator>A sneaky shadow sprite locked Ruby in a cage and blamed the dragon!"
        Twist.LOST_AND_WARM -> "There's the dragon ${q.dragon}, curled around Princess Ruby to keep her warm. She got lost in the storm! [growl] <dragon>Stay back! She's so cold."
        Twist.COLLECTOR -> "There's the dragon ${q.dragon}, sitting on a pile of spoons and buttons, all alone. <dragon>Oh! Hello? Is somebody there? <pet>It looks so lonely."
        Twist.SPRITE_THIEF -> "There's the dragon ${q.dragon}, and it's upset! <dragon>Somebody keeps hiding things in my cave! <narrator>A sneaky sprite has been stealing things and hiding them there."
        Twist.INK_SHADOW -> "There's the Ink Shadow, scribbling all over the magic Storybook! <shadow>Scribble, scribble, scribble! <ruby>We have to stop it!"
        Twist.SLEEPY_SHADOW -> "There's the Ink Shadow, yawning and scribbling. <shadow>Yaaawn... I can't stop scribbling... <ruby>Maybe it needs help."
    }

    fun bossAsk(q: Quest) = if (q.kind == QuestKind.RUBYS_QUEST) "What should we do?" else "How will you help ${q.dragon}?"

    fun starLit(left: Int) = when (left) {
        0 -> pick("The last star lights up!", "All three stars are shining!")
        1 -> pick("Another star lights up! Just one more!", "Two stars! One more to go!")
        else -> pick("A star lights up!", "[poof] The first star is shining!")
    }

    fun goblinHelps(goblin: String) = "Look who's here! $goblin the goblin runs in. <goblin>I'll help you, friend! <narrator>A star lights up!"

    fun keyHelps() = "Your magic key opens a secret window. Sunlight pours in, and a star lights up!"

    fun friendshipHelps() = "The Potion of Friendship makes everyone smile. <pet>It worked! <narrator>A star lights up!"

    fun giftAsk(q: Quest) = "${q.dragon} loves shiny things. <pet>Let's count a gift for ${q.dragon}! <narrator>"

    fun jokeRoll() = "Tell a funny joke! Roll the dice to see how funny it is."

    fun jokeResult(tier: Tier, dragon: String) = when (tier) {
        Tier.SILLY -> "Your joke is so silly that $dragon snorts, and smoke comes out of its nose! <dragon>Ha ha ha!"
        Tier.GOOD -> "$dragon giggles! <dragon>Hee hee, that's a good one!"
        Tier.GREAT -> "$dragon laughs so hard that it rolls on the floor! <pet>It's laughing so much!"
        Tier.MAGIC -> "$dragon laughs so hard that rainbow sparkles come out! <dragon>Oh my, that was the funniest joke ever!"
    }

    fun spellRoll() = "Roll the dice to power up your spell!"

    fun spellResult(tier: Tier) = when (tier) {
        Tier.SILLY -> "[fizz] Your spell fizzles and sprinkles pink glitter all over you! <pet>Hee hee, you're sparkly! <narrator>It still works a little."
        Tier.GOOD -> "[zap] Your spell sparkles. <pet>Ooh, pretty!"
        Tier.GREAT -> "[poof] A big sparkly spell! <pet>Wow, so big!"
        Tier.MAGIC -> "WOW! A rainbow spell fills the whole lair! <pet>It's a rainbow!"
    }

    fun ending(q: Quest, friendly: Boolean): String = when (q.twist) {
        Twist.RUBY_VISITING -> if (friendly) {
            "${q.dragon} smiles. <ruby>${q.dragon} was teaching me to fly! <narrator>Everyone flies home together, and the knights cheer for the dragon."
        } else {
            "Your spell lights up the sky, and the knights see Ruby waving from the dragon's back. <ruby>I was just visiting! <narrator>Everyone laughs."
        }
        Twist.TRICKSTER -> if (friendly) {
            "${q.dragon} stops crying. Together you find the shadow sprite's key, and Ruby is free! The sprite says sorry and promises to play nicely."
        } else {
            "Your spell breaks the sprite's cage! Ruby is free. <ruby>The dragon didn't do it!"
        }
        Twist.LOST_AND_WARM -> if (friendly) {
            "${q.dragon} isn't grumpy anymore. <dragon>I was just keeping Ruby safe. <narrator>You all walk her home, and ${q.dragon} gets invited to the castle for soup."
        } else {
            "Your spell makes a warm little sun. ${q.dragon} uncurls, and Ruby can walk home. <ruby>Thank you, dragon!"
        }
        Twist.COLLECTOR -> if (friendly) {
            "${q.dragon} is so happy! <dragon>I have never had a friend before! <narrator>It gives back all the spoons and buttons and keeps your friendship instead."
        } else {
            "Your spell makes the treasure float back to the kingdom. ${q.dragon} waves goodbye, a little sad. Maybe next time you can be friends."
        }
        Twist.SPRITE_THIEF -> if (friendly) {
            "You and ${q.dragon} find the sneaky sprite together, and it gives everything back! <dragon>Now I have a new best friend: you!"
        } else {
            "Your spell catches the sneaky sprite! Everything goes home, and ${q.dragon} says thank you."
        }
        Twist.INK_SHADOW -> if (friendly) {
            "Your lullaby makes the Ink Shadow sleepy. <shadow>Yaaawn... night night. <narrator>It curls up like a kitten, and the Storybook's pages are clean again!"
        } else {
            "Your light spell shines on the Storybook, and the scribbles fly away like moths. The story is safe!"
        }
        Twist.SLEEPY_SHADOW -> if (friendly) {
            "Your lullaby rocks the Ink Shadow to sleep. The Storybook is safe. <ruby>Sweet dreams, little shadow."
        } else {
            "Your light spell makes the Ink Shadow sparkle. It stops scribbling and draws a happy picture instead!"
        }
    }

    fun treasureFound(treasure: String) = "And look! You found $treasure!"

    fun finale(stars: Int) = pick(
        "What an adventure! You earned ${com.dinovalley.engine.rpg.learn.Words.number(stars)} stars!",
        "Hooray! The adventure is done. You earned ${com.dinovalley.engine.rpg.learn.Words.number(stars)} stars!",
    )

    fun levelUp(level: Int) = "Level up! You are now a level ${com.dinovalley.engine.rpg.learn.Words.number(level)} adventurer!"
}

/**
 * The brief's dice table, for two dice: a low total makes something unexpected happen, a middle
 * total is good, a high one is great, and the top totals are magical.
 */
enum class Tier {
    SILLY, GOOD, GREAT, MAGIC;

    companion object {
        fun of(total: Int): Tier = when {
            total <= 4 -> SILLY
            total <= 7 -> GOOD
            total <= 10 -> GREAT
            else -> MAGIC
        }
    }
}
