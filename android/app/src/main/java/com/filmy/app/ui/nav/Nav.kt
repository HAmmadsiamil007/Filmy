package com.filmy.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filmy.app.ui.screens.DetailScreen
import com.filmy.app.ui.screens.HomeScreen
import com.filmy.app.ui.screens.PlayerScreen
import com.filmy.app.ui.screens.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Navigation routes.
 *
 * Titles travel as query args because the backend lookup key is subject_id and
 * showing a stale fetched title during the detail load looks broken. They are
 * URL-encoded on push and decoded on read.
 */
object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"

    // detail/{id}?title={title}
    const val DETAIL = "detail/{id}?title={title}"
    fun detail(id: String, title: String): String {
        val t = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
        return "detail/$id?title=$t"
    }

    // player/{id}?title={title}&se={se}&ep={ep}
    const val PLAYER = "player/{id}?title={title}&se={se}&ep={ep}"
    fun player(id: String, title: String, se: Int = 0, ep: Int = 0): String {
        val t = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
        return "player/$id?title=$t&se=$se&ep=$ep"
    }

    fun decode(raw: String): String = try {
        URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    } catch (_: Exception) {
        raw
    }
}

@Composable
fun AppNav(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDetail = { id, title ->
                    navController.navigate(Routes.detail(id, title))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val title = Routes.decode(entry.arguments?.getString("title").orEmpty())
            DetailScreen(
                subjectId = id,
                initialTitle = title,
                onBack = { navController.popBackStack() },
                onPlay = { se, ep, displayTitle ->
                    navController.navigate(Routes.player(id, displayTitle, se, ep))
                },
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("se") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("ep") {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { entry ->
            PlayerScreen(
                subjectId = entry.arguments?.getString("id").orEmpty(),
                title = Routes.decode(entry.arguments?.getString("title").orEmpty()),
                season = entry.arguments?.getInt("se") ?: 0,
                episode = entry.arguments?.getInt("ep") ?: 0,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
