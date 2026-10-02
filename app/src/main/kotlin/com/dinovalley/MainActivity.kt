package com.dinovalley

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dinovalley.audio.NameRecorder
import com.dinovalley.audio.Narrator
import com.dinovalley.ui.book.BookScreen
import com.dinovalley.ui.book.BookViewModel
import com.dinovalley.ui.book.LocalNameRecorder
import com.dinovalley.ui.book.LocalNarrator
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
                    val viewModel: BookViewModel = viewModel()
                    BookScreen(viewModel)
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
