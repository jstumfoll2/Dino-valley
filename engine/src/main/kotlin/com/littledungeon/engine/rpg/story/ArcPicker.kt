package com.littledungeon.engine.rpg.story

import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.world.WorldMemory
import kotlin.random.Random

/** The Great Storybook has this many pages to win back before it is whole. */
const val BOOK_PAGES = 5

/** Chooses which story to play next, and which chapter of the campaign it is. */
object ArcPicker {
    /** 1 for a new book, rising by one for every page won back. */
    fun chapter(world: WorldMemory): Int = world.pages % BOOK_PAGES + 1

    /**
     * The first adventure is always the first story. After that: a story not played last time, not
     * yet unlocked ones left out, and the ones played least come first, with a little shuffle.
     */
    fun pick(world: WorldMemory, random: Random, arcs: List<Arc> = Content.arcs): Arc {
        if (world.adventures == 0) return arcs.first()
        val chapter = chapter(world)
        val open = arcs.filter { it.minChapter <= chapter }.ifEmpty { arcs }
        val fresh = open.filter { it.id != world.lastArc }.ifEmpty { open }
        return fresh.minByOrNull { (world.arcsDone[it.id] ?: 0) * 10 + random.nextInt(10) }!!
    }
}
