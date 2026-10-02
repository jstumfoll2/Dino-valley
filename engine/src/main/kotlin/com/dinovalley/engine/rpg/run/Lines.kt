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
            "Oh no! Princess Ruby has disappeared! The knights say the dragon ${q.dragon} took her, deep in ${q.dungeon}. " +
                "But nobody has asked the dragon. Will you go and find out what really happened?"
        QuestKind.LONELY_DRAGON ->
            "Things keep going missing in the kingdom: spoons, buttons, even the queen's shiny hat! " +
                "Everyone says the dragon ${q.dragon} took them, and hid them in ${q.dungeon}. Let's go and see!"
        QuestKind.RUBYS_QUEST ->
            "Princess Ruby didn't wait to be rescued. She climbed down from her tower all by herself! " +
                "Now she needs your help. Something under the castle, in ${q.dungeon}, is scribbling on the magic Storybook."
    }

    /** The companion notices the child: past adventures and what they're good at. */
    fun remember(world: WorldMemory, strongest: Attribute?): String? {
        val bits = mutableListOf<String>()
        if (world.adventures == 0) return "{name} flaps its little wings. I'm coming too! Let's be brave together."
        world.dragonFriend?.let { bits += "Last time, you made friends with the dragon $it." }
        world.friends.firstOrNull()?.let { if (bits.isEmpty()) bits += "Your goblin friend $it says hello!" }
        when (strongest) {
            Attribute.CLEVERNESS -> bits += "{name} says: you're so clever with numbers!"
            Attribute.MAGIC -> bits += "{name} says: the magic sparkles when you're near!"
            Attribute.WISDOM -> bits += "{name} says: you know so many letters!"
            Attribute.KINDNESS -> bits += "{name} says: you're the kindest adventurer in the kingdom!"
            Attribute.COURAGE -> bits += "{name} says: you're so brave!"
            null -> Unit
        }
        return bits.joinToString(" ").ifEmpty { null }
    }

    fun toTheMap(q: Quest) = pick(
        "Here is the map of ${q.dungeon}. Off we go!",
        "The map shows the way into ${q.dungeon}. Adventure time!",
        "{name} points at the map. That's ${q.dungeon}! Let's go!",
    )

    // ------------------------------------------------------------- doors and maps

    fun pickDoor() = pick("Two doors! Which one will you open?", "Which door should we try?", "Hmm, which way? You choose!")

    fun treasureSniff() = pick("{name} sniffs the air. Sniff, sniff! The treasure is", "A little bird sings: tweet! The treasure is", "The map glows. The treasure is")

    fun wrongDoor(color: String) = pick(
        "That's the $color door. The clue said something else! Let's look again.",
        "Knock knock... nobody's home behind the $color door! Let's check the clue.",
    )

    // ------------------------------------------------------------- rooms

    fun gate(q: Quest) = pick(
        "You reach the gate of ${q.dungeon}. It's stuck tight!",
        "Here's the big old gate of ${q.dungeon}. Creak... it won't open!",
    )

    fun gateRoll() = "Roll the die to push the gate!"

    fun runeDoor() = pick(
        "A huge stone door blocks the way. Magic symbols glow on it.",
        "An ancient door, covered in glowing runes! It hums softly.",
    )

    fun runeOops() = pick(
        "The door sneezes! Ah-choo! Dust everywhere. Let's try again.",
        "Bzzzt! The runes giggle and wiggle. Not that one. Try again!",
        "The door says: hmmm, no thank you! Try another one.",
    )

    fun runeYay() = pick(
        "You got it! The ancient door begins to shake... and rumbles open!",
        "Yes! The runes shine bright, and the door slides open!",
    )

    fun bridge() = pick(
        "A wobbly bridge stretches over a deep, deep gap. Some stones are loose!",
        "A rope bridge! It sways in the wind. Count the stones to make it strong.",
    )

    fun bridgeOops() = pick(
        "Wobble wobble! The bridge shakes and a bird flies away. Let's count again.",
        "Oops! A stone splashes far below. Plip! Let's try again.",
    )

    fun bridgeYay() = pick("You got it! The stones click into place. The bridge is strong!", "Yes! Stomp, stomp. The bridge holds tight!")

    fun crystalCave() = pick(
        "A cave full of glittering crystals! A tiny wizard waves her hat. My lantern went out! I need a crystal!",
        "Crystals of every color shine in this cave. A little wizard is stuck here in the dark!",
    )

    fun crystalOops() = pick(
        "The wizard puts the crystal in her lantern... and it plays a trumpet! Toot! Wrong crystal. Try again!",
        "Poof! The crystal turns the wizard's hat into a bunny. Hee hee! Not that one.",
    )

    fun crystalYay() = pick("Her lantern glows bright! Thank you! she says, and gives you a gem.", "Yes! The lantern lights up the whole cave!")

    fun library() = pick(
        "Books fly around like birds! A spell book lies open, but its magic word is missing.",
        "A magic library! Shhh. A spell book wants to finish its spell.",
    )

    fun letterPurpose() = pick("The spell needs one more rune.", "The spell book is waiting for its magic rune.", "One rune will calm the flying books.")

    fun libraryOops() = pick(
        "Flap flap! The books fly faster! That's not the right rune. Try again!",
        "The spell book giggles: that tickles! Try another rune.",
    )

    fun libraryYay() = pick("Whoosh! The books settle back on their shelves. The spell is finished!", "You got it! The spell book glows with happy magic!")

    fun tunnel() = pick(
        "This tunnel is so dark! Far away, a little crystal glows.",
        "A dark, twisty tunnel. We need a magic path to the glowing crystal!",
    )

    fun traceGoal() = pick("from your lantern to the crystal.", "all the way to the glowing crystal.", "to light the way.")

    fun tunnelOops() = pick("The path fizzles out like a sparkler. Pssst! Let's draw it again.", "Oops, the magic wandered off! Try again.")

    fun tunnelYay() = pick("The magic path lights up the whole tunnel!", "Your path glows bright. Now we can see!")

    fun mirrorHall() = pick(
        "A hall full of mirrors! Only one door is the real way out. Watch carefully...",
        "Mirrors, mirrors everywhere! Remember which door is real.",
    )

    fun mirrorOops() = pick("Bonk! That was just a mirror. Hee hee! Try again.", "Hello, me! That's a reflection. Let's try again.")

    fun mirrorYay() = pick("That's the real door! You remembered!", "Yes! The mirrors cheer: what a memory!")

    fun vault() = pick("A treasure vault! A big chest sits in the middle, with a magic lock.", "Gold glitters everywhere! This is a treasure vault.")

    fun vaultRoll() = "Roll the die to open the treasure chest!"

    fun vaultOops() = pick("Clink! The coins jump back out of your bag. Let's count again.", "The chest burps! Burp! Let's try again.")

    fun vaultYay() = pick("Clink clink! Into your bag they go!", "You got it! What a lot of treasure!")

    fun den(goblin: String) = pick(
        "Here's a goblin named $goblin, hiding behind a barrel. He's shaking! He's scared of the dark.",
        "A little goblin named $goblin is curled up in a corner. He looks frightened.",
    )

    fun denFriend(goblin: String) = "It's $goblin the goblin! He jumps up and down. My friend! You came back! I'll help you again!"

    fun denAsk(goblin: String) = "$goblin is frightened. What should you do?"

    fun workshop(potion: PotionKind) = pick(
        "An alchemist's workshop! A big cauldron bubbles, blub blub blub. There's a recipe for the ${potion.title}. We might need it!",
        "Bubbles and smoke! This is the alchemist's workshop. Let's make the ${potion.title}!",
    )

    fun potionOops() = pick(
        "Blorp! The potion turns polka-dotted! Out pops that one. Let's check the recipe.",
        "Fizz! Purple smoke, and {name} sneezes sparkles! Not that one. Try again.",
    )

    fun potionYay(potion: PotionKind) = "POOF! You made the ${potion.title}!"

    // ------------------------------------------------------------- dice

    fun gateResult(tier: Tier, coins: Int) = when (tier) {
        Tier.SILLY -> pick(
            "The gate swings open... and bonks you on the nose! Boing! But it's open.",
            "You push and push... and your hat flies off and gets stuck on the gate! The gate opens anyway.",
        )
        Tier.GOOD -> "Creeeeak. The gate opens!"
        Tier.GREAT -> "The gate flies open, and ${coinWords(coins)} roll out!"
        Tier.MAGIC -> "Magic! The gate bows to you and opens all by itself! ${capitalCoins(coins)} sparkle on the ground!"
    }

    fun songResult(tier: Tier, goblin: String) = when (tier) {
        Tier.SILLY -> "Your song is so silly that $goblin giggles and falls over! He's not scared anymore."
        Tier.GOOD -> "$goblin taps his feet. He's not scared anymore!"
        Tier.GREAT -> "$goblin dances and claps! What a song!"
        Tier.MAGIC -> "Your song is so beautiful that the walls start to glow! $goblin is not scared at all now."
    }

    fun vaultResult(tier: Tier) = when (tier) {
        Tier.SILLY -> "The lid pops open... and a frog jumps out! Ribbit! There's treasure under it."
        Tier.GOOD -> "Click! The chest opens."
        Tier.GREAT -> "The chest flies open! Shiny coins everywhere!"
        Tier.MAGIC -> "The chest sings a song and opens wide. So much treasure!"
    }

    fun coinWords(n: Int) = "${com.dinovalley.engine.rpg.learn.Words.number(n)} ${if (n == 1) "coin" else "coins"}"

    private fun capitalCoins(n: Int) = coinWords(n).replaceFirstChar { it.uppercase() }

    // ------------------------------------------------------------- the lair

    fun lairWay(potion: PotionKind) = when (potion) {
        PotionKind.GIANT_STRENGTH -> "A giant boulder blocks the lair! You drink the Potion of Giant Strength... You grow ENORMOUS and roll the boulder away!"
        PotionKind.GLOW -> "The lair is pitch dark. You drink the Potion of Glowing, and you shine like a star!"
        PotionKind.BUBBLE -> "A river of warm, bubbly mud blocks the way. The Potion of Bubbles floats you across in a giant bubble!"
        PotionKind.FRIENDSHIP -> "You sprinkle the Potion of Friendship in the air. It smells like warm cookies."
    }

    fun bossReveal(q: Quest): String = when (q.twist) {
        Twist.RUBY_VISITING -> "There's the dragon ${q.dragon}... and Princess Ruby is riding on its back, laughing! But ${q.dragon} looks worried about all the knights."
        Twist.TRICKSTER -> "There's the dragon ${q.dragon}, crying big smoky tears. A sneaky shadow sprite locked Ruby in a cage and blamed the dragon!"
        Twist.LOST_AND_WARM -> "There's the dragon ${q.dragon}, curled around Princess Ruby to keep her warm. She got lost in the storm! But the dragon is grumpy when anyone comes close."
        Twist.COLLECTOR -> "There's the dragon ${q.dragon}, sitting on a pile of spoons and buttons. It's all alone, and it looks so lonely."
        Twist.SPRITE_THIEF -> "There's the dragon ${q.dragon}, and it's upset! A sprite has been stealing things and hiding them in its cave."
        Twist.INK_SHADOW -> "There's the Ink Shadow, scribbling all over the magic Storybook! Ruby says: we have to stop it!"
        Twist.SLEEPY_SHADOW -> "There's the Ink Shadow, yawning and scribbling. It's so tired it can't stop! Ruby says: maybe it needs help."
    }

    fun bossAsk(q: Quest) = if (q.kind == QuestKind.RUBYS_QUEST) "What should we do?" else "How will you help ${q.dragon}?"

    fun starLit(left: Int) = when (left) {
        0 -> pick("The last star lights up!", "All three stars are shining!")
        1 -> pick("Another star lights up! Just one more!", "Two stars! One more to go!")
        else -> pick("A star lights up!", "Ding! The first star is shining!")
    }

    fun goblinHelps(goblin: String) = "Look who's here! $goblin the goblin runs in. I'll help you, friend! A star lights up!"

    fun keyHelps() = "Your magic key opens a secret window. Sunlight pours in, and a star lights up!"

    fun friendshipHelps() = "The Potion of Friendship makes everyone smile. A star lights up!"

    fun giftAsk(q: Quest) = "${q.dragon} loves shiny things. Let's count a gift for ${q.dragon}."

    fun jokeRoll() = "Tell a funny joke! Roll the die to see how funny it is."

    fun jokeResult(tier: Tier, dragon: String) = when (tier) {
        Tier.SILLY -> "Your joke is so silly that $dragon snorts, and smoke comes out of its nose! Ha!"
        Tier.GOOD -> "$dragon giggles!"
        Tier.GREAT -> "$dragon laughs so hard it rolls on the floor!"
        Tier.MAGIC -> "$dragon laughs so hard that rainbow sparkles come out!"
    }

    fun spellRoll() = "Roll the die to power up your spell!"

    fun spellResult(tier: Tier) = when (tier) {
        Tier.SILLY -> "Your spell goes fizz... and turns your cape pink! It still works a little."
        Tier.GOOD -> "Zap! Your spell sparkles."
        Tier.GREAT -> "Kaboom! A big sparkly spell!"
        Tier.MAGIC -> "WOW! A rainbow spell fills the whole lair!"
    }

    fun ending(q: Quest, friendly: Boolean): String = when (q.twist) {
        Twist.RUBY_VISITING -> if (friendly) {
            "${q.dragon} smiles. Ruby says: ${q.dragon} was teaching me to fly! Everyone flies home together, and the knights cheer for the dragon."
        } else {
            "Your spell lights up the sky, and the knights see Ruby waving from the dragon's back. Oh! She was just visiting! Everyone laughs."
        }
        Twist.TRICKSTER -> if (friendly) {
            "${q.dragon} stops crying. Together you find the shadow sprite's key, and Ruby is free! The sprite says sorry, and promises to play nicely."
        } else {
            "Your spell breaks the sprite's cage! Ruby is free, and she tells everyone: the dragon didn't do it!"
        }
        Twist.LOST_AND_WARM -> if (friendly) {
            "${q.dragon} isn't grumpy anymore. It was just protecting Ruby! You all walk her home, and ${q.dragon} gets invited to the castle for soup."
        } else {
            "Your spell makes a warm little sun. ${q.dragon} uncurls, and Ruby can walk home. She gives the dragon a big hug."
        }
        Twist.COLLECTOR -> if (friendly) {
            "${q.dragon} is so happy! It never had a friend before. It gives back all the spoons and buttons, and keeps your friendship instead."
        } else {
            "Your spell makes the treasure float back to the kingdom. ${q.dragon} waves goodbye, a little sad. Maybe next time you can be friends."
        }
        Twist.SPRITE_THIEF -> if (friendly) {
            "You and ${q.dragon} find the sneaky sprite together. It gives everything back! ${q.dragon} has a new best friend: you."
        } else {
            "Your spell catches the sneaky sprite! Everything goes home, and ${q.dragon} says thank you."
        }
        Twist.INK_SHADOW -> if (friendly) {
            "Your lullaby makes the Ink Shadow sleepy. It curls up like a kitten, and the Storybook's pages are clean again!"
        } else {
            "Your light spell shines on the Storybook, and the scribbles fly away like moths. The story is safe!"
        }
        Twist.SLEEPY_SHADOW -> if (friendly) {
            "Your lullaby rocks the Ink Shadow to sleep. Zzzz. The Storybook is safe, and Ruby tucks the shadow in with a blanket."
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

/** The brief's dice table: 1–2 something unexpected, 3–4 good, 5 great, 6 magical. */
enum class Tier {
    SILLY, GOOD, GREAT, MAGIC;

    companion object {
        fun of(die: Int): Tier = when (die) {
            1, 2 -> SILLY
            3, 4 -> GOOD
            5 -> GREAT
            else -> MAGIC
        }
    }
}
