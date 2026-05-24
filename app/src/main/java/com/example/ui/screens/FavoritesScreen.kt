package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.viewmodel.MainViewModel

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String, String) -> Unit
) {
    val favoritesSet by viewModel.favoritesList.collectAsState()
    val allMovies = viewModel.allMovies
    val allSeries = viewModel.allSeries

    val favoritedMovies = remember(favoritesSet, allMovies) {
        allMovies.filter { favoritesSet.contains(it.id) }
    }

    val favoritedSeries = remember(favoritesSet, allSeries) {
        allSeries.filter { favoritesSet.contains(it.id) }
    }

    val totalCount = favoritedMovies.size + favoritedSeries.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        if (totalCount == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "⭐", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Sin favoritos",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Marca películas y series como favoritas desde su detalle.",
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
                    Text(
                        text = "★ Mis Favoritos",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalCount",
                        fontSize = 11.sp,
                        color = Muted,
                        modifier = Modifier
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 2.dp)
                    )
                }

                // Render integrated grid combine
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoritedMovies) { movie ->
                        CarouselCardItem(
                            title = movie.title,
                            tmdbId = movie.tmdbId,
                            posterPath = movie.poster,
                            type = "PELÍCULA",
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(movie.id, "movie") }
                        )
                    }

                    items(favoritedSeries) { s ->
                        CarouselCardItem(
                            title = s.title,
                            tmdbId = s.tmdbId,
                            posterPath = null,
                            type = "SERIE",
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(s.id, "series") }
                        )
                    }
                }
            }
        }
    }
}
