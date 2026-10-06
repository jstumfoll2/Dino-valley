package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.content.Content
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every named character sounds like themselves: their own Kokoro v1.0 speaker, with nobody else's. */
class VoiceCastTest {
    @Test
    fun `named characters each have a speaker of their own`() {
        val sids = Who.NAMED.map { it.sid }
        assertEquals(sids.size, sids.toSet().size, "two named characters share a speaker: ${Who.NAMED.groupBy { it.sid }.filter { it.value.size > 1 }}")
        // Kokoro v1.0 has 28 English speakers, numbered 0 to 27.
        assertTrue(Who.entries.all { it.sid in 0..27 }, "a speaker outside the English ones")
        assertTrue(Who.NAMED.all { it.pitch == 1f }, "named voices are not pitch-shifted")
    }

    @Test
    fun `every person in the kingdom is a named voice, and no two people share one`() {
        val people = Content.npcs
        for (npc in people) assertTrue(npc.who in Who.NAMED, "${npc.name} speaks with ${npc.who}, which is not a voice of their own")
        val dupes = people.groupBy { it.who }.filter { it.value.size > 1 }
        assertTrue(dupes.isEmpty(), "shared: ${dupes.mapValues { e -> e.value.map { it.name } }}")
    }

    @Test
    fun `people with a story of their own are the same voice as a person and as a monster`() {
        for ((npcId, monsterId) in listOf("bandit_bess" to "bandit_bess", "grumble" to "grumble_troll", "rascal_fox" to "sneaky_fox")) {
            assertEquals(Content.npc(npcId)!!.who, Content.monster(monsterId)!!.who, "$npcId and $monsterId should sound alike")
        }
    }

    @Test
    fun `no two voices are the same setting, so a recording belongs to one`() {
        assertEquals(Who.entries.size, Who.entries.map { it.voiceId }.toSet().size)
    }
}
