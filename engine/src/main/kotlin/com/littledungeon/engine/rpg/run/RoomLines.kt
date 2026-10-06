package com.littledungeon.engine.rpg.run

import kotlin.random.Random

/**
 * What the narrator and the baby dragon say in the puzzle rooms of a dungeon: the story around each
 * kind of room, and what happens when the puzzle is missed or solved. Several versions of most lines so
 * no two adventures sound the same. Every line is recorded ahead of time by the build (see `VoiceCatalog`).
 */
internal class RoomLines(private val r: Random) {
    private fun pick(vararg options: String) = options[r.nextInt(options.size)]

    fun runeDoor() = pick(
        "A huge stone door blocks the way. Magic symbols glow on it. <pet>Ooh, they're so pretty!",
        "An ancient door covered in glowing runes! It hums softly. <pet>I wonder what the symbols mean.",
    )

    fun runeOops() = pick(
        "The door sneezes! [sneeze] Dust everywhere. Those were not the right runes.",
        "[zap] The runes giggle and wiggle. That was not the right symbol.",
        "The door hums, \"Hmm, no, thank you!\" <pet>That was not the one.",
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
        "The bridge wobbles and shakes, and a bird flies away. That was not the right number.",
        "Oops! A stone falls into the water far below. [splash]",
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
        "The spell book giggles, \"That tickles!\" That was not the right letter.",
    )

    fun libraryYay() = pick("[whoosh] The books settle back on their shelves. The spell is finished!", "You got it! The spell book glows with happy magic! <pet>Hooray!")

    fun tunnel() = pick(
        "This tunnel is so dark! Magic letters on the wall light the way, but they are fading. <pet>I'm not scared. Much.",
        "A dark, twisty tunnel. If we write magic letters on the wall, they will glow and light the way!",
    )

    fun writePurpose() = pick("Let's light the tunnel!", "Write with your magic finger!", "Make the wall glow!")

    fun tunnelOops() = pick("The magic fizzles out like a sparkler. It wandered off the line.", "Oops, the magic wandered off the line!")

    fun tunnelYay() = pick("It glows! The whole tunnel lights up!", "Your letter shines brightly. Now we can see!")

    fun mirrorHall() = pick(
        "A hall full of mirrors! Only one door is the real way out. Watch carefully...",
        "There are mirrors everywhere! Remember which door is real.",
    )

    fun mirrorOops() = pick("Bonk! That was just a mirror. <pet>Hee hee!", "<pet>Hello, me! That's just a reflection.")

    fun mirrorYay() = pick("That's the real door! <pet>You remembered!", "Yes! The mirrors cheer. <pet>What a memory!")

    fun storeroom() = pick(
        "This is the goblins' storeroom, and what a mess! Gems and coins and keys everywhere. Let's tidy up!",
        "Oh my! The goblins' storeroom is so messy that we can't get through. Let's put things in baskets!",
    )

    fun storeroomOops() = pick("Oops! That basket spits it back out. <pet>It says, that's not mine!", "[boing] It bounced out. Does it look like the others in that basket?")

    fun storeroomYay() = pick("All tidy! The goblins will be so happy.", "What a tidy storeroom! Now we can walk through.")

    fun pond() = pick(
        "An underground pond! A frog sits on a lily pad. [ribbit] <pet>It says it only hops when you count the lily pads right!",
        "Here's a quiet pond with lily pads. A little frog wants to hop across, but it needs your help counting!",
    )

    fun pondOops() = pick("[splash] The frog falls in the water. It's okay, frogs love water!", "[ribbit] <pet>The frog says, not that number!")

    fun pondYay() = pick("[boing] The frog jumps across!", "You got it! The frog hops all the way!")

    fun mosaic() = pick(
        "A grand hall with a magic picture on the wall. Oh no, it's broken into pieces!",
        "Look at this hall! Its magic picture has cracked apart, and the door won't open until it's fixed.",
    )

    fun mosaicOops() = pick("Hmm, that piece doesn't fit there. Look at the picture!", "That piece belongs somewhere else. Look at the picture!")

    fun mosaicYay() = pick("The picture is whole again! It sparkles, and the door opens.", "You fixed it! The magic picture glows!")

    fun vault() = pick("A treasure vault! A big chest with a magic lock sits in the middle.", "Gold glitters everywhere! This is a treasure vault.")

    fun vaultOops() = pick("[coins] The coins jump back out of your bag.", "The chest burps! [burp]")

    fun vaultYay() = pick("[coins] Into your bag they go!", "You got it! What a lot of treasure!")
}
