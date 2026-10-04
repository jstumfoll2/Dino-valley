package com.littledungeon.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.littledungeon.data.Save
import com.littledungeon.feedback.FeedbackLog
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Reply

/**
 * Bridges the engine and the screens: the title screen, then one [Journey] beat at a time.
 * Progress is saved when an adventure ends, so quitting halfway never loses earlier progress.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val save = Save(app)

    var state by mutableStateOf(save.load())
        private set

    var adventure: Journey? by mutableStateOf(null)
        private set

    var beat: Beat? by mutableStateOf(null)
        private set

    /** Bumped on every new beat, so screens restart their animations even for equal beats. */
    var beatNumber by mutableIntStateOf(0)
        private set

    val unlocked: Set<String> get() = state.hero.unlocks.map { it.id }.toSet()

    fun availableClasses(): List<HeroClass> = HeroClass.entries.filter { it.unlockLevel <= state.hero.level }

    fun chooseClass(c: HeroClass) {
        if (c.unlockLevel > state.hero.level) return
        state = state.copy(hero = state.hero.copy(heroClass = c))
        save.store(state)
    }

    fun start() {
        val seed = System.nanoTime()
        val a = Journey(seed, state.hero, state.skills, state.world)
        // So a feedback note can say exactly which adventure this was.
        FeedbackLog.newAdventure(
            "seed $seed, ${state.hero.heroClass} level ${state.hero.level}, skills ${state.skills.levels.entries.joinToString { "${it.key}=${it.value}" }}",
        )
        adventure = a
        show(a.beat)
    }

    fun reply(reply: Reply) {
        val a = adventure ?: return
        FeedbackLog.reply(reply.toString())
        a.reply(reply)
        if (a.finished) {
            state = Save.State(a.hero, a.skills, a.world)
            save.store(state, a.records)
        }
        show(a.beat)
    }

    fun home() {
        adventure = null
        beat = null
    }

    private fun show(b: Beat) {
        beat = b
        beatNumber += 1
    }
}
