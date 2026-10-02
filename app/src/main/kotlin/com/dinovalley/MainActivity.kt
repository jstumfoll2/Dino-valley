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
import com.dinovalley.audio.Narrator
import com.dinovalley.audio.Sfx
import com.dinovalley.data.DragonName
import com.dinovalley.ui.game.AdventureScreen
import com.dinovalley.ui.game.GameViewModel
import com.dinovalley.ui.game.LocalDragonName
import com.dinovalley.ui.game.LocalNarrator
import com.dinovalley.ui.game.LocalSfx
import com.dinovalley.ui.game.TitleScreen
import com.dinovalley.ui.theme.DinoValleyTheme

class MainActivity : ComponentActivity() {
    private lateinit var narrator: Narrator
    private lateinit var dragon: DragonName
    private lateinit var sfx: Sfx

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        dragon = DragonName(this)
        sfx = Sfx(this)
        narrator = Narrator(this) { dragon.name }
        setContent {
            DinoValleyTheme {
                CompositionLocalProvider(LocalNarrator provides narrator, LocalDragonName provides dragon, LocalSfx provides sfx) {
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
        super.onPause()
    }

    override fun onDestroy() {
        narrator.shutdown()
        sfx.release()
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
