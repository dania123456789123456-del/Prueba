package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.viewmodel.MainViewModel

data class HistoryDisplayItem(
    val id: String,
    val title: String,
    val tmdbId: Int?,
    val poster: String?,
    val type: String
)

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String, String) -> Unit
) {
    val watchHistory by viewModel.watchHistory.collectAsState()
    val allMovies = viewModel.allMovies
    val allSeries = viewModel.allSeries

    // Match history records with catalog items using a unified display item
    val historyItems = remember(watchHistory, allMovies, allSeries) {
        watchHistory.mapNotNull { record ->
            if (record.type == "movie") {
                allMovies.find { it.id == record.id }?.let {
                    HistoryDisplayItem(
                        id = it.id,
                        title = it.title,
                        tmdbId = it.tmdbId,
                        poster = it.poster,
                        type = "movie"
                    )
                }
            } else {
                allSeries.find { it.id == record.id }?.let {
                    HistoryDisplayItem(
                        id = it.id,
                        title = it.title,
                        tmdbId = it.tmdbId,
                        poster = null, // series usually fetch from TMDB
                        type = "series"
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        if (historyItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "🕐", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Sin historial",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Consulte películas y series que reproduzca en la aplicación.",
                    fontSize = 13.sp,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🕐 Mi Historial",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${historyItems.size}",
                            fontSize = 11.sp,
                            color = Muted,
                            modifier = Modifier
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 9.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "🗑 Limpiar",
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFFF87171).copy(alpha = 0.12f))
                            .clickable { viewModel.clearWatchHistory() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        CarouselCardItem(
                            title = item.title,
                            tmdbId = item.tmdbId,
                            posterPath = item.poster,
                            type = if (item.type == "movie") "PELÍCULA" else "SERIE",
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(item.id, item.type) }
                        )
                    }
                }
            }
        }
    }
}
