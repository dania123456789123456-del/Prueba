package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Quality
import com.example.data.model.Subtitle
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

sealed interface ScreenState {
    object Splash : ScreenState
    object Login : ScreenState
    object Main : ScreenState
    data class Player(
        val videoUrl: String,
        val title: String,
        val saveKey: String,
        val qualities: List<Quality>,
        val subtitles: List<Subtitle>
    ) : ScreenState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Cast context before UI
        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Splash) }
                var selectedDetailItem by remember { mutableStateOf<Pair<String, String>?>(null) }

                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                LaunchedEffect(isLoggedIn) {
                    if (currentScreen == ScreenState.Splash) {
                        return@LaunchedEffect
                    }
                    if (isLoggedIn) {
                        currentScreen = ScreenState.Main
                    } else if (currentScreen !is ScreenState.Player) {
                        currentScreen = ScreenState.Login
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        ScreenState.Splash -> {
                            SplashScreen(onTimeout = {
                                currentScreen = if (isLoggedIn) ScreenState.Main else ScreenState.Login
                            })
                        }
                        ScreenState.Login -> {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    currentScreen = ScreenState.Main
                                }
                            )
                        }
                        ScreenState.Main -> {
                            MainShellScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    selectedDetailItem = Pair(id, type)
                                },
                                onLogout = {
                                    currentScreen = ScreenState.Login
                                }
                            )

                            selectedDetailItem?.let { pair ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    DetailScreen(
                                        itemId = pair.first,
                                        itemType = pair.second,
                                        viewModel = viewModel,
                                        onBack = { selectedDetailItem = null },
                                        onNavigateToDetail = { id, type ->
                                            selectedDetailItem = Pair(id, type)
                                        },
                                        onPlayMovie = { movie ->
                                            val url = movie.qualities?.firstOrNull()?.url ?: movie.url ?: ""
                                            currentScreen = ScreenState.Player(
                                                videoUrl = url,
                                                title = movie.title,
                                                saveKey = "movie_${movie.id}",
                                                qualities = movie.qualities ?: emptyList(),
                                                subtitles = movie.subtitles ?: emptyList()
                                            )
                                        },
                                        onPlayEpisode = { series, sIdx, eIdx ->
                                            val season = series.seasons?.getOrNull(sIdx)
                                            val ep = season?.episodes?.getOrNull(eIdx)
                                            if (ep != null) {
                                                val url = ep.qualities?.firstOrNull()?.url ?: ep.url ?: ""
                                                val epTitle = "${series.title} · S${season?.seasonNumber}E${ep.episodeNumber}: ${ep.title ?: "Episodio"}"
                                                currentScreen = ScreenState.Player(
                                                    videoUrl = url,
                                                    title = epTitle,
                                                    saveKey = "ep_${series.id}_${sIdx}_${eIdx}",
                                                    qualities = ep.qualities ?: emptyList(),
                                                    subtitles = ep.subtitles ?: emptyList()
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        is ScreenState.Player -> {
                            PlayerScreen(
                                videoUrl = screen.videoUrl,
                                title = screen.title,
                                saveKey = screen.saveKey,
                                qualities = screen.qualities,
                                subtitles = screen.subtitles,
                                viewModel = viewModel,
                                onClose = {
                                    currentScreen = ScreenState.Main
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
