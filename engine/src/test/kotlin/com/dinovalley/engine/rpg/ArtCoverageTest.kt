package com.dinovalley.engine.rpg

import com.dinovalley.engine.rpg.content.ArtAliases
import com.dinovalley.engine.rpg.content.Content
import com.dinovalley.engine.rpg.run.placeOf
import com.dinovalley.engine.rpg.story.Icons
import com.dinovalley.engine.rpg.world.RoomKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every person, monster, item, place and icon the content mentions has its painted picture, so a
 * new content pack that forgets its art fails here and not on a child's screen.
 */
class ArtCoverageTest {
    private val res = listOfNotNull(System.getenv("ART_DIR"), "../app/src/main/res/drawable-nodpi", "app/src/main/res/drawable-nodpi").map { File(it) }.firstOrNull { it.isDirectory }

    private fun has(name: String) = File(res!!, "art_$name.webp").exists()

    private fun missing(names: Collection<String>) = names.filterNot { has(it) }

    private val faceLayers = listOf("body", "eye_open", "eye_closed", "mouth_calm", "mouth_talk")

    @Test
    fun peopleAndMonstersHaveTheirLayers() {
        if (res == null) return
        val art = (Content.npcs.map { it.art } + Content.monsters.map { it.art }).map { ArtAliases.resolve(it) }.distinct()
        val gone = missing(art.flatMap { a -> faceLayers.map { "${a}_$it" } })
        assertTrue(gone.isEmpty(), "missing character art: $gone")
    }

    @Test
    fun itemsHaveIconsAndGearHasOverlays() {
        if (res == null) return
        val gone = missing(Content.items.map { "item_${it.id}" })
        assertTrue(gone.isEmpty(), "missing item icons: $gone")
        val noOverlay = missing(Content.items.filter { it.isGear }.map { "gear_${it.id}" })
        assertTrue(noOverlay.isEmpty(), "missing gear overlays: $noOverlay")
    }

    @Test
    fun placesAndIconsHavePictures() {
        if (res == null) return
        val places = Content.locations.map { "scene_${it.theme}" } + RoomKind.entries.map { "scene_${placeOf(it).id}" } +
            listOf("scene_world_map", "scene_camp")
        val gone = missing(places) + missing(Icons.all)
        assertTrue(gone.isEmpty(), "missing pictures: $gone")
    }
}
