package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.Config
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val tmdbId: Int?,
    val poster: String?,
    val type: String, // "movie" or "series"
    val recordId: String
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String, String) -> Unit
) {
    val movies = viewModel.allMovies
    val series = viewModel.allSeries
    val allItems = remember(movies, series) { movies + series }

    val featuredItem = remember(movies) { movies.firstOrNull() }

    val progressRecords by viewModel.progressRecords.collectAsState()

    // Filter "Continuar viendo" (items with >5% and <95% completed) with proper type safety
    val continueWatchingList = remember(progressRecords, movies, series) {
        progressRecords.values.mapNotNull { record ->
            val pct = if (record.dur > 0) record.t.toFloat() / record.dur else 0f
            if (pct in 0.05f..0.97f) {
                if (record.id.startsWith("movie_")) {
                    val mId = record.id.substringAfter("movie_")
                    movies.find { it.id == mId }?.let {
                        ContinueWatchingItem(
                            id = it.id,
                            title = it.title,
                            tmdbId = it.tmdbId,
                            poster = it.poster,
                            type = "movie",
                            recordId = record.id
                        )
                    }
                } else if (record.id.startsWith("ep_")) {
                    // id format: ep_{seriesId}_{sIdx}_{eIdx}
                    val sId = record.id.split("_").getOrNull(1) ?: ""
                    series.find { it.id == sId }?.let {
                        ContinueWatchingItem(
                            id = it.id,
                            title = it.title,
                            tmdbId = it.tmdbId,
                            poster = null, // series usually fetch from TMDB
                            type = "series",
                            recordId = record.id
                        )
                    }
                } else null
            } else null
        }.sortedByDescending { it.id } // Order them
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. HERO BANNER
        featuredItem?.let { item ->
            HeroBanner(
                item = item,
                type = "movie",
                viewModel = viewModel,
                onNavigateToDetail = onNavigateToDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 340.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. CONTINUAR VIENDO (if has items)
        if (continueWatchingList.isNotEmpty()) {
            Text(
                text = "▶ Continuar Viendo",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                items(continueWatchingList, key = { it.recordId }) { item ->
                    val record = progressRecords[item.recordId]
                    val pct = if (record != null && record.dur > 0) record.t.toFloat() / record.dur else 0f

                    ProgressCardItem(
                        item = item,
                        progressPercent = pct,
                        viewModel = viewModel,
                        onClick = { onNavigateToDetail(item.id, item.type) }
                    )
                }
            }
        }

        // 3. CARRUSEL PELÍCULAS
        if (movies.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎬 Películas",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${movies.size}",
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                items(movies, key = { it.id }) { movie ->
                    CarouselCardItem(
                        title = movie.title,
                        tmdbId = movie.tmdbId,
                        posterPath = movie.poster,
                        type = "PELÍCULA",
                        viewModel = viewModel,
                        onClick = { onNavigateToDetail(movie.id, "movie") }
                    )
                }
            }
        }

        // 4. CARRUSEL SERIES
        if (series.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📺 Series",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${series.size}",
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(series, key = { it.id }) { s ->
                    CarouselCardItem(
                        title = s.title,
                        tmdbId = s.tmdbId,
                        posterPath = null, // series usually gets from TMDB or fallback
                        type = "SERIE",
                        viewModel = viewModel,
                        onClick = { onNavigateToDetail(s.id, "series") }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroBanner(
    item: Movie,
    type: String,
    viewModel: MainViewModel,
    onNavigateToDetail: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var finalBackdrop by remember { mutableStateOf(item.backdrop ?: "") }
    var overview by remember { mutableStateOf(item.description ?: "") }
    var rating by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(item.year?.toString() ?: "") }

    LaunchedEffect(item.tmdbId) {
        item.tmdbId?.let { tid ->
            val details = if (type == "movie") {
                viewModel.getMovieDetailsFromTmdb(tid)
            } else {
                viewModel.getTvDetailsFromTmdb(tid)
            }
            details?.let {
                if (item.backdrop.isNullOrEmpty() && !it.backdropPath.isNullOrEmpty()) {
                    finalBackdrop = Config.TMDB_IMG_W1280 + it.backdropPath
                }
                if (item.description.isNullOrEmpty() && !it.overview.isNullOrEmpty()) {
                    overview = it.overview
                }
                it.voteAverage?.let { score ->
                    rating = score.toString().take(3)
                }
                val date = it.releaseDate ?: it.firstAirDate
                if (!date.isNullOrEmpty() && year.isEmpty()) {
                    year = date.split("-").firstOrNull() ?: ""
                }
            }
        }
    }

    Box(modifier = modifier) {
        if (finalBackdrop.isNotEmpty()) {
            AsyncImage(
                model = finalBackdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(CardColor, Panel)))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Ink.copy(alpha = 0.97f),
                            Ink.copy(alpha = 0.78f),
                            Ink.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Ink.copy(alpha = 0.55f),
                            Ink
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.62f)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF8A5CF6).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .background(Color(0xFF8A5CF6).copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "✦ ${if (type == "movie") "PELÍCULA" else "SERIE"} DESTACADA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleLight,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                lineHeight = 28.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (year.isNotEmpty()) {
                    Text(text = year, fontSize = 12.sp, color = Muted)
                }
                if (rating.isNotEmpty()) {
                    Text(text = "★ $rating", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
                }
            }

            if (overview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = overview,
                    fontSize = 13.sp,
                    color = Color(0xFFC0B8E8),
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onNavigateToDetail(item.id, type) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(Purple, Pink)),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(text = "▶ Ver ahora", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = { onNavigateToDetail(item.id, type) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                ) {
                    Text(text = "ℹ Más info", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CarouselCardItem(
    title: String,
    tmdbId: Int?,
    posterPath: String?,
    type: String,
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    var finalPoster by remember { mutableStateOf(posterPath ?: "") }
    var rating by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    LaunchedEffect(tmdbId) {
        tmdbId?.let { tid ->
            val details = if (type == "PELÍCULA") {
                viewModel.getMovieDetailsFromTmdb(tid)
            } else {
                viewModel.getTvDetailsFromTmdb(tid)
            }
            details?.let {
                if (finalPoster.isEmpty() && !it.posterPath.isNullOrEmpty()) {
                    finalPoster = Config.TMDB_IMG_W500 + it.posterPath
                }
                it.voteAverage?.let { score ->
                    rating = score.toString().take(3)
                }
                val date = it.releaseDate ?: it.firstAirDate
                if (!date.isNullOrEmpty()) {
                    year = date.split("-").firstOrNull() ?: ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                if (finalPoster.isNotEmpty()) {
                    AsyncImage(
                        model = finalPoster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(CardColor, Panel))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (type == "PELÍCULA") "🎬" else "📺", fontSize = 24.sp, color = Muted)
                    }
                }

                Text(
                    text = if (type == "PELÍCULA") "Film" else "Serie",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color(0xFF8A5CF6).copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )

                if (rating.isNotEmpty()) {
                    Text(
                        text = "★ $rating",
                        fontSize = 9.sp,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )
                if (year.isNotEmpty()) {
                    Text(
                        text = year,
                        fontSize = 10.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressCardItem(
    item: ContinueWatchingItem,
    progressPercent: Float,
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    var finalPoster by remember { mutableStateOf(item.poster ?: "") }

    LaunchedEffect(item.tmdbId) {
        item.tmdbId?.let { tid ->
            val details = if (item.type == "movie") {
                viewModel.getMovieDetailsFromTmdb(tid)
            } else {
                viewModel.getTvDetailsFromTmdb(tid)
            }
            details?.let {
                if (finalPoster.isEmpty() && !it.posterPath.isNullOrEmpty()) {
                    finalPoster = Config.TMDB_IMG_W500 + it.posterPath
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                if (finalPoster.isNotEmpty()) {
                    AsyncImage(
                        model = finalPoster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(CardColor, Panel))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⏳", fontSize = 24.sp, color = Muted)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent)
                            .background(Brush.horizontalGradient(listOf(Purple, Pink)))
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val percentage = (progressPercent * 100).toInt()
                Text(
                    text = "$percentage% visto",
                    fontSize = 9.sp,
                    color = Muted
                )
            }
        }
    }
}
