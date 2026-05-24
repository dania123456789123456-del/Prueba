package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Ink
import com.example.viewmodel.MainViewModel

@Composable
fun SeriesScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String, String) -> Unit
) {
    val series = viewModel.allSeries

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📺 Todas las Series",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${series.size}",
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(series) { s ->
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
