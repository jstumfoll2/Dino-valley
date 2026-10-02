package com.dungeonquest.ui.play

import androidx.lifecycle.ViewModel
import com.dungeonquest.engine.activity.count.CountObjectsGenerator
import com.dungeonquest.engine.activity.count.PrototypeCountingLevels
import com.dungeonquest.engine.model.SpriteId
import com.dungeonquest.engine.session.GameSession
import com.dungeonquest.engine.session.SessionEvent
import com.dungeonquest.engine.session.SessionState
import com.dungeonquest.engine.util.GameRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Thin bridge: UI events go to the engine's [GameSession]; its state comes back as a flow. */
class PlayViewModel : ViewModel() {
    private val generator = CountObjectsGenerator(
        sprites = listOf(SpriteId("egg_blue"), SpriteId("egg_green"), SpriteId("egg_orange")),
        distractorSprites = listOf(SpriteId("leaf")),
    )

    private var session = newSession()
    private val _state = MutableStateFlow(session.state)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun onEvent(event: SessionEvent) {
        _state.value = session.onEvent(event)
    }

    fun playAgain() {
        session = newSession()
        _state.value = session.state
    }

    // The prototype plays level 2 (1–5 eggs in a row); the difficulty engine arrives in Phase 2.
    private fun newSession() = GameSession(
        generator = generator,
        level = PrototypeCountingLevels.level(2),
        random = GameRandom(System.nanoTime()),
    )
}
