package com.littledungeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littledungeon.audio.Narrator
import com.littledungeon.audio.Sfx
import com.littledungeon.data.DragonName
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.ui.art.Picto
import com.littledungeon.ui.game.AdventureScreen
import com.littledungeon.ui.game.ConfirmDialog
import com.littledungeon.ui.game.GameViewModel
import com.littledungeon.ui.game.LocalDragonName
import com.littledungeon.ui.game.LocalNarrator
import com.littledungeon.ui.game.LocalSfx
import com.littledungeon.ui.game.TitleScreen
import com.littledungeon.ui.theme.LittleDungeonTheme

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
            LittleDungeonTheme {
                CompositionLocalProvider(LocalNarrator provides narrator, LocalDragonName provides dragon, LocalSfx provides sfx) {
                    val vm: GameViewModel = viewModel()
                    if (vm.adventure == null) {
                        TitleScreen(vm)
                    } else {
                        // Back asks first, so a stray swipe doesn't end the sitting. Leaving loses nothing: every tap is
                        // already saved, and Continue on the camp screen carries on from the same beat.
                        var leaving by remember { mutableStateOf(false) }
                        BackHandler { if (vm.beat is Beat.Finale) vm.home() else leaving = true }
                        AdventureScreen(vm)
                        if (leaving) {
                            ConfirmDialog(
                                title = "Leave the adventure?",
                                note = "It is saved. On the camp screen, the big arrow carries on from here.",
                                keep = Picto.NEXT, change = Picto.HOME,
                                onKeep = { leaving = false },
                                onChange = {
                                    leaving = false
                                    vm.home()
                                },
                            )
                        }
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
