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
 *
 * Every tap is saved the moment it is made (the journey is its seed and the taps, see [Save]), so the adventure
 * can be put down at any point, by the child, by Back, or by the phone, and picked up from the same beat.
 * What the hero keeps (levels, coins, the world's memory) is stored when an adventure ends.
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

    /** An adventure was put down and can be carried on. */
    var canContinue by mutableStateOf(save.hasSaved())
        private set

    /** How many of the journey's answered puzzles are already in the challenge log. */
    private var recordsLogged = 0

    init {
        FeedbackLog.replayData = { save.replayText() }
    }

    val unlocked: Set<String> get() = state.hero.unlocks.map { it.id }.toSet()

    fun availableClasses(): List<HeroClass> = HeroClass.entries.filter { it.unlockLevel <= state.hero.level }

    fun chooseClass(c: HeroClass) {
        // The adventure waiting was begun as someone; a new hero starts a new adventure.
        if (c.unlockLevel > state.hero.level || canContinue) return
        state = state.copy(hero = state.hero.copy(heroClass = c))
        save.store(state)
    }

    /** Starts a new adventure. One that was put down is let go (what the child learned in it is kept). */
    fun start() {
        letGo()
        val seed = System.nanoTime()
        val a = Journey(seed, state.hero, state.skills, state.world)
        save.begin(seed, state)
        recordsLogged = 0
        canContinue = false
        // So a feedback note can say exactly which adventure this was.
        FeedbackLog.newAdventure(
            "seed $seed, ${state.hero.heroClass} level ${state.hero.level}, skills ${state.skills.levels.entries.joinToString { "${it.key}=${it.value}" }}",
        )
        adventure = a
        show(a.beat)
    }

    /** Carries on the adventure that was put down, from the beat it was left at. */
    fun resume() {
        val saved = save.saved() ?: return start()
        val a = Journey.replay(saved.seed, saved.start.hero, saved.start.skills, saved.start.world, saved.commands)
        // Everything answered so far was added to the challenge log when it happened.
        recordsLogged = a.records.size
        // The adventure was begun as this hero, whoever the camp screen last showed.
        state = state.copy(hero = state.hero.copy(heroClass = saved.start.hero.heroClass))
        canContinue = false
        FeedbackLog.newAdventure("resumed seed ${saved.seed} after ${saved.commands.size} replies, ${saved.start.hero.heroClass} level ${saved.start.hero.level}")
        adventure = a
        if (a.finished) finish(a)
        show(a.beat)
    }

    fun reply(reply: Reply) {
        val a = adventure ?: return
        FeedbackLog.reply(reply.toString())
        val before = a.commands.size
        a.reply(reply)
        if (a.commands.size > before) save.append(a.commands.last())
        save.logRecords(a.records.drop(recordsLogged))
        recordsLogged = a.records.size
        if (a.finished) finish(a)
        show(a.beat)
    }

    /** Back to the camp. A journey that is not finished is already saved, and Continue brings it back. */
    fun home() {
        adventure = null
        beat = null
        canContinue = save.hasSaved()
    }

    /** What the hero keeps is stored first, then the journey that led to it is let go: a crash between the two repeats nothing. */
    private fun finish(a: Journey) {
        state = Save.State(a.hero, a.skills, a.world)
        save.store(state)
        save.end()
    }

    /** The adventure that was put down is dropped, but the skills the child practiced in it still count. */
    private fun letGo() {
        val saved = save.saved()
        if (saved != null) {
            val a = Journey.replay(saved.seed, saved.start.hero, saved.start.skills, saved.start.world, saved.commands)
            state = state.copy(skills = a.skills)
            save.store(state)
        }
        save.end()
    }

    private fun show(b: Beat) {
        beat = b
        beatNumber += 1
    }
}
