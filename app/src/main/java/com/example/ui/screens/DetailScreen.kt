package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.Config
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    itemType: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onPlayEpisode: (Series, Int, Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 768

    val scope = rememberCoroutineScope()

    val movies = viewModel.allMovies
    val series = viewModel.allSeries
    val favoritesSet by viewModel.favoritesList.collectAsState()

    val movieItem = remember(itemId, movies) { movies.find { it.id == itemId } }
    val seriesItem = remember(itemId, series) { series.find { it.id == itemId } }

    var finalBackdrop by remember { mutableStateOf("") }
    var overview by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var genreList by remember { mutableStateOf(listOf<String>()) }
    var directorText by remember { mutableStateOf("") }
    var castText by remember { mutableStateOf("") }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    var trailerUrl by remember { mutableStateOf("") }

    // Series Seasons
    var selectedSeasonIdx by remember { mutableStateOf(0) }
    var seasonEpisodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isEpisodesLoading by remember { mutableStateOf(false) }

    val isFav = favoritesSet.contains(itemId)

    // Episode selection bottom sheet state for Series Cast
    var showCastEpSheet by remember { mutableStateOf(false) }

    // Initialize item fields
    LaunchedEffect(itemId, itemType) {
        if (itemType == "movie" && movieItem != null) {
            finalBackdrop = movieItem.backdrop ?: ""
            overview = movieItem.description ?: ""
            year = movieItem.year?.toString() ?: ""
            trailerUrl = movieItem.trailer ?: ""

            movieItem.tmdbId?.let { tid ->
                val details = viewModel.getMovieDetailsFromTmdb(tid)
                details?.let { td ->
                    if (finalBackdrop.isEmpty() && !td.backdropPath.isNullOrEmpty()) {
                        finalBackdrop = Config.TMDB_IMG_W1280 + td.backdropPath
                    }
                    if (overview.isEmpty() && !td.overview.isNullOrEmpty()) {
                        overview = td.overview
                    }
                    rating = td.voteAverage?.toString()?.take(3) ?: ""
                    durationText = if (td.runtime != null) "${td.runtime} min" else ""
                    genreList = td.genres?.map { g -> g.name } ?: emptyList()

                    val dirs = td.credits?.crew?.filter { c -> c.job == "Director" }?.take(2)?.map { it.name }?.joinToString(", ") ?: ""
                    val act = td.credits?.cast?.take(4)?.map { it.name }?.joinToString(", ") ?: ""
                    directorText = dirs
                    castText = act
                }
            }
        } else if (itemType == "series" && seriesItem != null) {
            finalBackdrop = ""
            overview = ""
            year = ""
            trailerUrl = ""

            seriesItem.tmdbId?.let { tid ->
                val details = viewModel.getTvDetailsFromTmdb(tid)
                details?.let { td ->
                    if (!td.backdropPath.isNullOrEmpty()) {
                        finalBackdrop = Config.TMDB_IMG_W1280 + td.backdropPath
                    }
                    overview = td.overview ?: ""
                    rating = td.voteAverage?.toString()?.take(3) ?: ""
                    genreList = td.genres?.map { g -> g.name } ?: emptyList()

                    val date = td.firstAirDate
                    if (!date.isNullOrEmpty()) {
                        year = date.split("-").firstOrNull() ?: ""
                    }
                    durationText = if (td.episodeRunTime?.isNotEmpty() == true) "~${td.episodeRunTime[0]} min" else ""

                    val dirs = td.credits?.crew?.filter { c -> c.job == "Director" || c.job == "Creator" }?.take(2)?.map { it.name }?.joinToString(", ") ?: ""
                    val actStr = td.credits?.cast?.take(4)?.map { it.name }?.joinToString(", ") ?: ""
                    directorText = dirs
                    castText = actStr
                }
            }
        }
    }

    // Season episodes loader
    LaunchedEffect(selectedSeasonIdx, seriesItem) {
        if (seriesItem != null && seriesItem.seasons != null) {
            val season = seriesItem.seasons.getOrNull(selectedSeasonIdx)
            if (season != null) {
                isEpisodesLoading = true
                var eps = season.episodes ?: emptyList()

                seriesItem.tmdbId?.let { tid ->
                    val seasonDetails = viewModel.getTvSeasonDetailsFromTmdb(tid, season.seasonNumber)
                    seasonDetails?.episodes?.let { tmdbEps ->
                        eps = eps.mapIndexed { idx, ep ->
                            val match = tmdbEps.find { e -> e.episodeNumber == ep.episodeNumber } ?: tmdbEps.getOrNull(idx)
                            if (match != null) {
                                ep.copy(
                                    title = ep.title ?: match.name,
                                    thumbnail = ep.thumbnail ?: (if (!match.stillPath.isNullOrEmpty()) "https://image.tmdb.org/t/p/w300${match.stillPath}" else null),
                                    description = ep.description ?: match.overview
                                )
                            } else ep
                        }
                    }
                }
                seasonEpisodes = eps
                isEpisodesLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Ink,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (finalBackdrop.isNotEmpty()) {
                AsyncImage(
                    model = finalBackdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Ink.copy(alpha = 0.5f), Ink)
                        )
                    )
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.72f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isTablet) 220.dp else 160.dp)
                    .padding(horizontal = isTablet.let { if (it) 36.dp else 16.dp })
            ) {
                val title = movieItem?.title ?: seriesItem?.title ?: ""

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            InfoColBlock(
                                title = title,
                                year = year,
                                rating = rating,
                                durationText = durationText,
                                itemType = itemType,
                                genreList = genreList,
                                directorText = directorText,
                                castText = castText,
                                overview = overview,
                                isDescriptionExpanded = isDescriptionExpanded,
                                toggleDescription = { isDescriptionExpanded = !isDescriptionExpanded },
                                isFav = isFav,
                                onFavClick = { viewModel.toggleFavorite(itemId) },
                                onPlayClick = {
                                    if (itemType == "movie" && movieItem != null) {
                                        onPlayMovie(movieItem)
                                    } else if (itemType == "series" && seriesItem != null) {
                                        onPlayEpisode(seriesItem, 0, 0)
                                    }
                                },
                                onCastClick = {
                                    if (itemType == "movie" && movieItem != null) {
                                        castMovieDirect(movieItem)
                                    } else {
                                        showCastEpSheet = true
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 32.dp)
                        ) {
                            TrailerBlock(trailerUrl = trailerUrl)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TrailerBlock(
                            trailerUrl = trailerUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        InfoColBlock(
                            title = title,
                            year = year,
                            rating = rating,
                            durationText = durationText,
                            itemType = itemType,
                            genreList = genreList,
                            directorText = directorText,
                            castText = castText,
                            overview = overview,
                            isDescriptionExpanded = isDescriptionExpanded,
                            toggleDescription = { isDescriptionExpanded = !isDescriptionExpanded },
                            isFav = isFav,
                            onFavClick = { viewModel.toggleFavorite(itemId) },
                            onPlayClick = {
                                if (itemType == "movie" && movieItem != null) {
                                    onPlayMovie(movieItem)
                                } else if (itemType == "series" && seriesItem != null) {
                                    onPlayEpisode(seriesItem, 0, 0)
                                }
                            },
                            onCastClick = {
                                if (itemType == "movie" && movieItem != null) {
                                    castMovieDirect(movieItem)
                                } else {
                                    showCastEpSheet = true
                                }
                            }
                        )
                    }
                }

                if (itemType == "series" && seriesItem != null && seriesItem.seasons != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "📋 Episodios",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        seriesItem.seasons.forEachIndexed { idx, s ->
                            val isSel = idx == selectedSeasonIdx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Purple.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, if (isSel) Purple.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { selectedSeasonIdx = idx }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s.name ?: "Temporada ${s.seasonNumber}",
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else Muted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isEpisodesLoading) {
                        CircularProgressIndicator(color = Purple, modifier = Modifier.align(Alignment.CenterHorizontally).padding(20.dp))
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(seasonEpisodes) { ep ->
                                val thumb = ep.thumbnail ?: ""

                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            onPlayEpisode(seriesItem, selectedSeasonIdx, ep.episodeNumber - 1)
                                        }
                                ) {
                                    Column {
                                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                                            if (thumb.isNotEmpty()) {
                                                AsyncImage(
                                                    model = thumb,
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
                                                    Text(text = "▶", fontSize = 24.sp, color = Muted)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(Purple.copy(alpha = 0.92f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "▶", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 3.dp))
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "EPISODIO ${ep.episodeNumber}",
                                                fontSize = 10.sp,
                                                color = PurpleLight,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = ep.title ?: "Pilot",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!ep.description.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = ep.description,
                                                    fontSize = 10.sp,
                                                    color = Muted,
                                                    maxLines = 2,
                                                    lineHeight = 14.sp,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
                SimilarPoolGrid(viewModel, itemType, itemId, onNavigateToDetail)
            }
        }
    }

    if (showCastEpSheet && seriesItem != null && seriesItem.seasons != null) {
        val sList = seriesItem.seasons
        var castSeasonIdx by remember { mutableStateOf(0) }
        var castEps by remember { mutableStateOf<List<Episode>>(emptyList()) }

        LaunchedEffect(castSeasonIdx, seriesItem) {
            val s = sList[castSeasonIdx]
            var eps = s.episodes ?: emptyList()
            seriesItem.tmdbId?.let { tid ->
                val sDetails = viewModel.getTvSeasonDetailsFromTmdb(tid, s.seasonNumber)
                sDetails?.episodes?.let { tEps ->
                    eps = eps.mapIndexed { idx, ep ->
                        val match = tEps.find { e -> e.episodeNumber == ep.episodeNumber } ?: tEps.getOrNull(idx)
                        if (match != null) ep.copy(title = ep.title ?: match.name) else ep
                    }
                }
            }
            castEps = eps
        }

        AlertDialog(
            onDismissRequest = { showCastEpSheet = false },
            containerColor = Color(0xFF100E26),
            title = {
                Text(
                    text = "📡 Transmitir Episodio a TV",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        sList.forEachIndexed { idx, s ->
                            val isSel = idx == castSeasonIdx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Purple else Color.White.copy(alpha = 0.05f))
                                    .clickable { castSeasonIdx = idx }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s.name ?: "T${s.seasonNumber}",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        castEps.forEach { ep ->
                            val url = ep.qualities?.firstOrNull()?.url ?: ep.url ?: ""
                            val hasUrl = url.isNotEmpty()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = hasUrl) {
                                        castEpDirect(seriesItem, sList[castSeasonIdx], ep)
                                        showCastEpSheet = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📺", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "S${sList[castSeasonIdx].seasonNumber}E${ep.episodeNumber}",
                                        fontSize = 10.sp,
                                        color = PurpleLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ep.title ?: "Episode ${ep.episodeNumber}",
                                        fontSize = 13.sp,
                                        color = if (hasUrl) Color.White else Muted
                                    )
                                }
                                Text(text = if (hasUrl) "›" else "—", fontSize = 18.sp, color = Purple)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCastEpSheet = false }) {
                    Text("Cerrar", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun CrewItemBlock(name: String, role: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Purple, Pink))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = name.take(1), fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = role, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TrailerBlock(trailerUrl: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (trailerUrl.isNotEmpty()) {
            YoutubeTrailerPlayer(
                trailerUrl = trailerUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(CardColor, PurpleDark.copy(alpha = 0.3f)))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎬", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Sin tráiler disponible", fontSize = 13.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
fun InfoColBlock(
    title: String,
    year: String,
    rating: String,
    durationText: String,
    itemType: String,
    genreList: List<String>,
    directorText: String,
    castText: String,
    overview: String,
    isDescriptionExpanded: Boolean,
    toggleDescription: () -> Unit,
    isFav: Boolean,
    onFavClick: () -> Unit,
    onPlayClick: () -> Unit,
    onCastClick: () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (itemType == "movie") "PELÍCULA" else "SERIE",
                    fontSize = 10.sp,
                    color = PurpleLight,
                    fontWeight = FontWeight.Bold
                )
            }

            if (year.isNotEmpty()) {
                Text(text = year, fontSize = 13.sp, color = Muted)
            }

            if (rating.isNotEmpty()) {
                Text(
                    text = "★ $rating",
                    fontSize = 13.sp,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            }

            if (durationText.isNotEmpty()) {
                Text(text = durationText, fontSize = 13.sp, color = Muted)
            }
        }

        if (genreList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                genreList.forEach { g ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(text = g, fontSize = 11.sp, color = Muted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .background(
                        Brush.linearGradient(listOf(Purple, Pink)),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Text(
                    text = "▶ Reproducir",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = onFavClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = if (isFav) "❤️" else "🤍",
                    fontSize = 16.sp
                )
            }

            IconButton(
                onClick = onCastClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "📡",
                    fontSize = 16.sp
                )
            }
        }

        if (overview.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = overview,
                fontSize = 14.sp,
                color = TextMain,
                lineHeight = 20.sp,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { toggleDescription() }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDescriptionExpanded) "Ver menos" else "Ver más...",
                fontSize = 12.sp,
                color = PurpleLight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { toggleDescription() }
                    .padding(vertical = 4.dp)
            )
        }

        if (directorText.isNotEmpty() || castText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                if (directorText.isNotEmpty()) {
                    Row {
                        Text(text = "Director: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
                        Text(text = directorText, fontSize = 12.sp, color = TextMain)
                    }
                }
                if (castText.isNotEmpty()) {
                    Row {
                        Text(text = "Reparto: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
                        Text(text = castText, fontSize = 12.sp, color = TextMain, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SimilarPoolGrid(
    viewModel: MainViewModel,
    type: String,
    currentId: String,
    onSelect: (String, String) -> Unit
) {
    if (type == "movie") {
        val moviesPool = viewModel.allMovies
        val similar = remember(currentId, moviesPool) {
            moviesPool.filter { it.id != currentId }.shuffled().take(14)
        }
        if (similar.isNotEmpty()) {
            Text(
                text = "🎬 Quizás te guste",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                items(similar, key = { it.id }) { sim ->
                    CarouselCardItem(
                        title = sim.title,
                        tmdbId = sim.tmdbId,
                        posterPath = sim.poster,
                        type = "PELÍCULA",
                        viewModel = viewModel,
                        onClick = { onSelect(sim.id, "movie") }
                    )
                }
            }
        }
    } else {
        val seriesPool = viewModel.allSeries
        val similar = remember(currentId, seriesPool) {
            seriesPool.filter { it.id != currentId }.shuffled().take(14)
        }
        if (similar.isNotEmpty()) {
            Text(
                text = "📺 Quizás te guste",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                items(similar, key = { it.id }) { sim ->
                    CarouselCardItem(
                        title = sim.title,
                        tmdbId = sim.tmdbId,
                        posterPath = null,
                        type = "SERIE",
                        viewModel = viewModel,
                        onClick = { onSelect(sim.id, "series") }
                    )
                }
            }
        }
    }
}

@Composable
fun YoutubeTrailerPlayer(trailerUrl: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { webView ->
            val embedUrl = if (trailerUrl.contains("youtube.com/watch?v=")) {
                val videoId = trailerUrl.substringAfter("v=")
                "https://www.youtube.com/embed/$videoId"
            } else if (trailerUrl.contains("youtu.be/")) {
                val videoId = trailerUrl.substringAfter("youtu.be/")
                "https://www.youtube.com/embed/$videoId"
            } else {
                trailerUrl
            }
            webView.loadUrl(embedUrl)
        },
        modifier = modifier
    )
}

fun castMovieDirect(movie: Movie) {
    val castSession = CastContext.getSharedInstance()?.sessionManager?.currentCastSession
    val remoteClient = castSession?.remoteMediaClient
    if (remoteClient != null) {
        val streamUrl = movie.qualities?.firstOrNull()?.url ?: movie.url ?: ""
        if (streamUrl.isEmpty()) return

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, movie.title)
            movie.poster?.let { addImage(WebImage(android.net.Uri.parse(it))) }
        }

        val isHls = streamUrl.contains(".m3u8")
        val contentFormat = if (isHls) "application/x-mpegurl" else "video/mp4"

        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentFormat)
            .setMetadata(metadata)
            .build()

        remoteClient.load(mediaInfo)
    }
}

fun castEpDirect(series: Series, season: Season, ep: Episode) {
    val castSession = CastContext.getSharedInstance()?.sessionManager?.currentCastSession
    val remoteClient = castSession?.remoteMediaClient
    if (remoteClient != null) {
        val streamUrl = ep.qualities?.firstOrNull()?.url ?: ep.url ?: ""
        if (streamUrl.isEmpty()) return

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW).apply {
            putString(MediaMetadata.KEY_TITLE, "${series.title} - T${season.seasonNumber}E${ep.episodeNumber}")
            putString(MediaMetadata.KEY_SUBTITLE, ep.title ?: "Episode ${ep.episodeNumber}")
            ep.thumbnail?.let { addImage(WebImage(android.net.Uri.parse(it))) }
        }

        val isHls = streamUrl.contains(".m3u8")
        val contentFormat = if (isHls) "application/x-mpegurl" else "video/mp4"

        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentFormat)
            .setMetadata(metadata)
            .build()

        remoteClient.load(mediaInfo)
    }
}
