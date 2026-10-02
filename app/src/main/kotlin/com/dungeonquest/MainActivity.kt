package com.dungeonquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dungeonquest.audio.Narrator
import com.dungeonquest.ui.play.PlayScreen
import com.dungeonquest.ui.play.PlayViewModel
import com.dungeonquest.ui.theme.DungeonQuestTheme

class MainActivity : ComponentActivity() {
    private lateinit var narrator: Narrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        narrator = Narrator(this)
        setContent {
            DungeonQuestTheme {
                val viewModel: PlayViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                PlayScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onPlayAgain = viewModel::playAgain,
                    narrator = narrator,
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        narrator.shutdown()
        super.onDestroy()
    }

    /** Full screen, so little fingers don't land on the system bars. A swipe from the edge brings them back. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
