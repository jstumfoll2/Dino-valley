package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.rpg.items.Obstacle
import com.littledungeon.engine.rpg.learn.Words
import com.littledungeon.engine.rpg.world.LocationKind
import com.littledungeon.engine.rpg.world.Terrain
import kotlin.random.Random

/**
 * What the narrator and the baby dragon say as the hero walks the kingdom: roads, obstacles,
 * fights, shops and rests. Several versions of most lines. "{name}" is the baby dragon's name;
 * "<pet>" and friends choose who speaks.
 */
internal class JourneyLines(private val r: Random) {
    private fun pick(vararg options: String) = options[r.nextInt(options.size)]
    /** Numbers are said exactly up to a hundred and roughly after, so the recorded numbers stay a fixed set. */
    private fun n(x: Int) = if (x > 100) "more than a hundred" else Words.number(x)

    private fun big(x: Int) = n(x)

    private fun capital(x: Int) = n(x).replaceFirstChar { it.uppercase() }

    // ------------------------------------------------------------- the map

    fun travelPrompt(name: String) = pick(
        "You are at $name. Which way will you go?",
        "This is $name. Where to next? Tap a place on the map.",
        "Time to choose. Tap the place you want to go to.",
    )

    fun routeSaid(to: String, terrain: Terrain, danger: Int?, visited: Boolean, marked: Boolean) = buildString {
        append(
            when (terrain) {
                Terrain.ROAD -> "$to, along an easy road."
                Terrain.FOREST -> "$to, through the forest."
                Terrain.MOUNTAIN -> "$to, over the mountain pass."
                Terrain.RIVER -> "$to, across the river."
                Terrain.SWAMP -> "$to, through the swamp."
            },
        )
        when (danger) {
            null -> Unit
            0 -> append(" It looks safe.")
            1 -> append(" Be careful.")
            else -> append(" It looks dangerous!")
        }
        if (visited) append(" You have been there.")
        if (marked) append(" <pet>I think the story wants us to go that way!")
    }

    fun blockedRoad(to: String) = "The way to $to is still blocked. Pick another road for now."

    // ------------------------------------------------------------- arriving

    fun arriveTown(name: String) = pick("Welcome to $name!", "You walk into $name.", "Here is $name!")

    fun arriveWild(name: String) = pick("You come to $name.", "Here is $name.", "You arrive at $name.")

    fun hubAsk(name: String) = pick("What would you like to do in $name?", "You are in $name. What now?")

    fun quiet(name: String) = pick("It is quiet at $name now. Time to move on.", "Nothing new at $name. <pet>Let's keep going!")

    fun backAtCamp() = pick("Back at camp! Everyone is happy to see you. <pet>Home sweet home!", "Camp at last. The fire crackles, and you rest your feet.")

    // ------------------------------------------------------------- obstacles

    fun obstacleIntro(o: Obstacle) = when (o) {
        Obstacle.CLIMB -> pick(
            "A steep mountain trail winds up in front of you. <pet>That is a lot of steps!",
            "The path climbs and climbs. Count the stones to find the way up.",
        )
        Obstacle.CROSS -> pick(
            "The river is wide and rushing. Lily pads make stepping stones, but only the right ones hold.",
            "A river crosses the road. <pet>I can't swim that far. Can we hop across?",
        )
        Obstacle.DARK -> pick(
            "It is very dark ahead. Magic symbols glow on the ground, and they show the safe path.",
            "The swamp is dark and gloopy. <pet>I can see glowing marks! Do they make a pattern?",
        )
        Obstacle.LOCK -> pick(
            "A locked door stands in the way. It has a code of numbers.",
            "A heavy lock hangs on the door. <pet>It wants a number!",
        )
        Obstacle.RIDDLE -> pick(
            "A stone face on the wall asks a riddle. It wants one special letter.",
            "A strange door asks a question. It wants the right letter to open.",
        )
    }

    fun obstacleOops(o: Obstacle) = when (o) {
        Obstacle.CLIMB -> "Oh no, your foot slips on the loose stones!"
        Obstacle.CROSS -> "Splash! The lily pad tips, and you wobble back to the bank."
        Obstacle.DARK -> "You step on the wrong mark, and the swamp goes gloop."
        Obstacle.LOCK -> "The lock clicks and stays shut."
        Obstacle.RIDDLE -> "The stone face frowns. That was not the letter."
    }

    fun obstacleYay(o: Obstacle) = when (o) {
        Obstacle.CLIMB -> pick("You climb the whole trail. What a view!", "Up, up, up! You made it over the pass!")
        Obstacle.CROSS -> pick("Hop, hop, hop! You are across the river!", "The lily pads hold, and you make it to the other side!")
        Obstacle.DARK -> pick("The marks light up one by one, and you find the safe path.", "You follow the glowing pattern all the way through.")
        Obstacle.LOCK -> pick("[unlock] The lock pops open!", "[unlock] Click! The door swings wide.")
        Obstacle.RIDDLE -> pick("The stone face smiles and the door slides open.", "That is the one! The door opens.")
    }

    /** For a person's own puzzle: they say what it means; these only react to the tap. */
    fun puzzleOops() = pick("Oh no, that is not it.", "Hmm, not that one.")

    fun puzzleYay() = pick("That's it! Well done.", "Yes! You got it.")

    fun rescueAsk(thing: String) = pick("Wait! You have $thing. Would you like to use it?", "You have $thing. Do you want to use it now?")

    fun toolWorks(tool: String, o: Obstacle) = when (o) {
        Obstacle.CLIMB -> "You tie the $tool and haul yourself up the cliff."
        Obstacle.CROSS -> "You swing across the river on the $tool."
        Obstacle.DARK -> "The $tool lights the way, and you walk through safely."
        Obstacle.LOCK -> "You open the lock with the $tool."
        Obstacle.RIDDLE -> "The $tool helps you through the riddle."
    }

    fun charmWorks(charm: String) = pick("The $charm glows, and you get another look.", "The $charm sparkles. <pet>Think carefully this time!")

    fun failedFor(o: Obstacle) = pick(
        "You could not get through this time. <pet>That is okay. We learned something. Let's try another way!",
        "Not this time. <pet>The right answer was there, and now we know. Let's find another road.",
    )

    fun turnedBack(from: String) = pick("You turn back to $from. The road is blocked for now.", "Back to $from you go. That road needs a rest.")

    fun petClover() = pick("<pet>Wait! I found a lucky clover in my pocket! It gives you one more guess.", "<pet>Look what I found! A lucky clover. Use it to guess again!")

    fun noRescue() = pick("You do not have anything to help. <pet>Next time, bring a rope or a clover!", "No tools in your bag for this. <pet>Maybe a shop has something!")

    // ------------------------------------------------------------- finds

    fun coinsFound(n: Int) = "You found ${n(n)} ${if (n == 1) "coin" else "coins"}!"

    fun itemFound(name: String) = pick("You found a $name!", "Look! A $name!", "Into your bag goes a $name!")

    fun chest() = pick("A little chest sits by the road. <pet>Treasure!", "A mossy chest is half buried here.")

    fun chestOpens() = pick("[unlock] Click! The chest opens.", "[unlock] The lid pops open.")

    fun shrine() = pick(
        "A small stone shrine glows softly. You feel warm and brave, and your health comes back.",
        "A mossy shrine hums a happy tune. All your aches go away.",
    )

    fun wanderer() = pick("A traveler waves hello and shares a snack and a coin.", "A cheerful peddler tips her hat and gives you a coin for your trouble.")

    fun hiddenDoor() = pick(
        "A little door is hidden in the rocks by the road. <pet>Ooh, a secret! Let's look inside.",
        "You spot a tiny door under some leaves. <pet>Do you think we can fit?",
    )

    fun hiddenDoorShut() = pick("The little door swings shut with a click. <pet>Maybe next time!", "Whoosh! The secret door closes. <pet>It will be there another day.")

    // ------------------------------------------------------------- dungeons

    fun dungeonAsk(name: String) = pick("Will you go into $name?", "Do you dare to explore $name?")

    fun dungeonEnter(name: String) = pick("You step inside $name.", "Down you go into $name.")

    fun dungeonSkip() = "You decide to leave it for another day."

    fun roomsLeft(left: Int) = if (left == 1) "One more room before the guardian." else "${capital(left)} more rooms before the guardian."

    fun roomCleared() = pick("Room cleared! <pet>On to the next one!", "You did it! The way ahead opens.")

    fun thrownOut(name: String) = pick("The dungeon rumbles, and you tumble out of $name.", "Whoosh! You are pushed back out of $name. <pet>We can try again later!")

    fun guardianAhead() = pick("Something big is waiting at the end of the hall.", "At the end of the hall, a guardian stands in the way.")

    fun dungeonDone(name: String) = pick("You explored all of $name. What a brave adventurer!", "$name is cleared. <pet>We are a great team!")

    fun alreadyDone(name: String) = pick("You already explored $name. Time to move on.", "$name is quiet now. You have been here before.")

    // ------------------------------------------------------------- fights

    fun yourTurn() = pick("Your turn! What will you do?", "It is your move. Choose!")

    fun attackAsk() = pick("Answer the puzzle to attack!", "Solve it to strike!", "Think, then tap the answer!")

    // Lines with numbers never name the foe: every monster times every number would be too many recordings.
    @Suppress("UNUSED_PARAMETER")
    fun heroHits(foe: String, dmg: Int) = pick(
        "You hit it for ${n(dmg)}!",
        "Whack! ${capital(dmg)} damage!",
        "A good hit! ${capital(dmg)} damage.",
    )

    @Suppress("UNUSED_PARAMETER")
    fun heroHitsBig(foe: String, dmg: Int) = "A great hit! ${capital(dmg)} damage!"

    @Suppress("UNUSED_PARAMETER")
    fun foeHits(foe: String, dmg: Int) = pick(
        "It hits you for ${n(dmg)}!",
        "Ouch! ${capital(dmg)} damage to you.",
    )

    fun attackOops(foe: String) = pick("Oh no! The $foe is too quick, and you miss.", "Oops! That was not it, and the $foe gets ready to strike.")

    fun attackYay() = pick("That's it! Strike!", "Yes! Here goes!", "Right! Attack!")

    fun shieldUp() = "A big shiny bubble wraps around you."

    fun foeMisses() = pick("The monster swings and misses!", "The monster trips over its own feet!")

    fun shielded() = "The bubble shield bounces the hit away!"

    fun foeStunned(foe: String) = pick("The $foe is fast asleep and cannot attack.", "The $foe is too sleepy to fight.")

    fun healed(hp: Int) = pick("You feel better! ${capital(hp)} health back.", "Yum! You get ${n(hp)} health back.")

    @Suppress("UNUSED_PARAMETER")
    fun healthLine(hp: Int, max: Int) = "You have ${n(hp)} health left."

    fun foeLine(foe: String, hp: Int) = "The $foe has ${n(hp)} left."

    fun weaknessHit(foe: String) = pick("It is the $foe's weak spot! A mighty blow!", "Perfect! The $foe really did not expect that!")

    fun befriended(foe: String) = pick("The $foe nibbles the cookie and smiles. You are friends now!", "The $foe sits down and wags happily. No fight needed!")

    fun escaped() = pick("Poof! You slip away in a cloud of smoke.", "You drop the pearl and vanish in a puff of smoke.")

    fun victory(foe: String) = pick("You beat the $foe! Hooray!", "The $foe is defeated! <pet>You were great!", "Victory! The $foe is beaten!")

    fun spoils(coins: Int) = if (coins == 0) "" else "You collect ${n(coins)} coins."

    fun faint(coins: Int) = pick(
        "Everything goes fuzzy, and you fall asleep right there. <pet>I will carry you to safety!",
        "Oh dear! You are too tired to go on. <pet>Come on, let's get you to a safe place.",
    ) + if (coins > 0) " You lose ${n(coins)} coins on the way." else ""

    fun wakeUp(place: String) = pick("You wake up in $place, safe and sound.", "You open your eyes. You are back in $place, feeling a little better.")

    fun fightAsk(foe: String) = pick("A $foe blocks the way! What will you do?", "Here comes a $foe! Fight, or find another way?")

    // ------------------------------------------------------------- shops and inns

    fun shopWelcome(keeper: String) = pick("$keeper smiles. Take a look around!", "Welcome, welcome! Have a look at what $keeper has.")

    fun shopAsk() = pick("What would you like to buy? Tap a thing to buy it, or tap the door to leave.", "Tap something to buy it. Tap the door when you are done.")

    fun bought(item: String, price: Int) = pick("You bought the $item for ${n(price)} coins. [coins]", "Sold! The $item is yours for ${n(price)} coins. [coins]")

    fun cannotAfford() = pick("You do not have enough coins for that yet.", "That costs too much for now. Keep adventuring and come back!")

    fun shopBye() = pick("Come back soon!", "Thank you for shopping!")

    fun rest(cost: Int) = if (cost == 0) "You sleep in a soft bed. All your health comes back!" else "You pay ${n(cost)} coins and sleep in a soft bed. All your health comes back!"

    fun cannotRest() = "You do not have enough coins for a bed tonight."

    fun foundGear(name: String) = "You put on the $name!"

    // ------------------------------------------------------------- the story

    fun peaceOops() = pick("<pet>Not quite! Try again, gently.", "<pet>Hmm, not that one. Have another look!")

    fun chapter(n: Int, title: String) = "Chapter ${Words.number(n)}: $title."

    fun pageFound() = "A page of the Great Storybook, glowing softly! <pet>One more page is home!"

    fun pagesLine(p: Int) = if (p == 1) "One page of the Storybook is home." else "${capital(p)} pages of the Storybook are home."

    fun bookWhole() = "All five pages are home, and the Great Storybook is whole again! Every story in Whisperwood is safe."

    fun newBook() = "But a book like that never stays closed for long. A new Storybook begins, with fresh blank pages. <pet>What stories will we write next?"

    fun levelUp(level: Int) = "Level up! You are now a level ${n(level)} adventurer!"

    fun finale(stars: Int) = pick(
        "What an adventure! You earned ${big(stars)} stars!",
        "Hooray! The adventure is done. You earned ${big(stars)} stars!",
    )

    fun coinsEarned(c: Int) = if (c <= 0) "" else "You brought home ${big(c)} coins."

    fun killsLine(k: Int) = when {
        k <= 0 -> ""
        k == 1 -> "You beat one monster along the way."
        else -> "You beat ${big(k)} monsters along the way."
    }

    fun kindName(kind: LocationKind) = when (kind) {
        LocationKind.CAMP -> "camp"
        LocationKind.TOWN -> "town"
        LocationKind.DUNGEON -> "dungeon"
        LocationKind.WILD -> "wild place"
        LocationKind.LAIR -> "lair"
    }

    companion object {
        /**
         * Every line that holds a number, for every number up to [max]: damage, health, coins, stars and so on.
         * Adventures hit these numbers by chance, so the voice catalog asks for them all.
         */
        fun numbered(max: Int = 101): List<String> = buildList {
            val l = JourneyLines(Random(1))
            for (x in 0..max) {
                repeat(10) {
                    add(l.heroHits("", x)); add(l.heroHitsBig("", x)); add(l.foeHits("", x)); add(l.healed(x)); add(l.healthLine(x, x))
                    add(l.spoils(x)); add(l.faint(x)); add(l.coinsEarned(x)); add(l.killsLine(x)); add(l.finale(x))
                    add(l.pagesLine(x)); add(l.roomsLeft(x)); add(l.levelUp(x)); add(l.rest(x)); add(l.coinsFound(x))
                }
            }
        }.filter { it.isNotEmpty() }
    }
}
