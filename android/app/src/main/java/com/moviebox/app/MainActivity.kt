package com.moviebox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.moviebox.app.ui.nav.AppNav
import com.moviebox.app.ui.theme.Bg
import com.moviebox.app.ui.theme.MovieBoxTheme

/**
 * Single-activity host. All screens are Compose; navigation is handled by
 * [AppNav] (a NavHost, not manual state) so back-gesture and process-death
 * restore work for free.
 *
 * The manifest pins orientation to portrait for the browse screens. The player
 * requests landscape itself via its own Activity-free approach — see
 * PlayerScreen for how orientation is handled.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MovieBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Bg,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(navController: NavHostController = rememberNavController()) {
    AppNav(navController = navController, modifier = Modifier.padding())
}
