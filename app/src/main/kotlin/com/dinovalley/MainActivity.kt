package com.dinovalley

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dinovalley.audio.NameRecorder
import com.dinovalley.audio.Narrator
import com.dinovalley.ui.game.AdventureScreen
import com.dinovalley.ui.game.GameViewModel
import com.dinovalley.ui.game.LocalNameRecorder
import com.dinovalley.ui.game.LocalNarrator
import com.dinovalley.ui.game.TitleScreen
import com.dinovalley.ui.theme.DinoValleyTheme

class MainActivity : ComponentActivity() {
    private lateinit var narrator: Narrator
    private lateinit var recorder: NameRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        recorder = NameRecorder(this)
        narrator = Narrator(this) { if (recorder.hasName) recorder.clip else null }
        setContent {
            DinoValleyTheme {
                CompositionLocalProvider(LocalNarrator provides narrator, LocalNameRecorder provides recorder) {
                    val vm: GameViewModel = viewModel()
                    if (vm.adventure == null) {
                        TitleScreen(vm)
                    } else {
                        // Back leaves the adventure for the camp; progress so far is kept per adventure.
                        BackHandler { vm.home() }
                        AdventureScreen(vm)
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onPause() {
        narrator.stop()
        recorder.stop()
        super.onPause()
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
