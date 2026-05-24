package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.CatalogUiState
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.launch

sealed interface AppNavigationItem {
    object Home : AppNavigationItem
    object Movies : AppNavigationItem
    object Series : AppNavigationItem
    object Favorites : AppNavigationItem
    object History : AppNavigationItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 768
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var activeScreen by remember { mutableStateOf<AppNavigationItem>(AppNavigationItem.Home) }
    var searchQuery by remember { mutableStateOf("") }

    val castActive = remember { mutableStateOf(false) }

    val uiState by viewModel.catalogState.collectAsState()

    // Trigger loading catalog on launch of Main layout
    LaunchedEffect(Unit) {
        if (viewModel.catalogState.value is CatalogUiState.Idle) {
            viewModel.loadCatalog()
        }
    }

    // Outer responsive structure
    if (isTablet) {
        // Double-Pane Layout: Sidebar + Main Content pane
        Row(modifier = Modifier.fillMaxSize().background(Ink)) {
            SidebarContent(
                activeScreen = activeScreen,
                onSelectScreen = { activeScreen = it },
                onLogout = {
                    viewModel.doLogout()
                    onLogout()
                },
                modifier = Modifier.width(230.dp).fillMaxHeight()
            )

            // Split line
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF8A5CF6).copy(alpha = 0.18f)))

            // Content Area in tablet
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ShellTopBar(
                    title = getTitleForSection(activeScreen),
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        viewModel.updateSearchQuery(it)
                    },
                    onMenuClick = {},
                    showMenuIcon = false
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ContentSwitch(
                        activeScreen = activeScreen,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    } else {
        // Mobile layout: Drawer + Content Pane
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFD0D0C1E),
                    modifier = Modifier.width(240.dp)
                ) {
                    SidebarContent(
                        activeScreen = activeScreen,
                        onSelectScreen = {
                            activeScreen = it
                            scope.launch { drawerState.close() }
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            viewModel.doLogout()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink)
            ) {
                ShellTopBar(
                    title = getTitleForSection(activeScreen),
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        viewModel.updateSearchQuery(it)
                    },
                    onMenuClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    showMenuIcon = true
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ContentSwitch(
                        activeScreen = activeScreen,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarContent(
    activeScreen: AppNavigationItem,
    onSelectScreen: (AppNavigationItem) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0D0C1E))
            .padding(vertical = 20.dp)
    ) {
        // Logo arriba: emoji 🎬 + "CineStream"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Purple, Pink))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎬", fontSize = 17.sp)
            }
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = "CineStream",
                fontSize = 19.sp,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }

        // Section label
        Text(
            text = "MENÚ",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Muted,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Menu items definition
        val itemsList = listOf(
            Triple(AppNavigationItem.Home, "Inicio", "🏠"),
            Triple(AppNavigationItem.Movies, "Películas", "🎬"),
            Triple(AppNavigationItem.Series, "Series", "📺"),
            Triple(AppNavigationItem.Favorites, "Favoritos", "⭐"),
            Triple(AppNavigationItem.History, "Historial", "🕐")
        )

        itemsList.forEach { item ->
            val isActive = activeScreen == item.first
            val bgModifier = if (isActive) {
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8A5CF6).copy(alpha = 0.28f), Color(0xFFE879F9).copy(alpha = 0.12f))
                        )
                    )
                    .border(1.dp, Color(0xFF8A5CF6).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            } else Modifier

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 1.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(bgModifier)
                    .clickable { onSelectScreen(item.first) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.third,
                    fontSize = 16.sp,
                    color = if (isActive) Color.White else Muted
                )
                Spacer(modifier = Modifier.width(11.dp))
                Text(
                    text = item.second,
                    fontSize = 14.sp,
                    color = if (isActive) Color.White else Muted,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Divider + Salir button
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onLogout() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🚪", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = "Salir",
                fontSize = 14.sp,
                color = Muted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ShellTopBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    showMenuIcon: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink.copy(alpha = 0.85f))
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showMenuIcon) {
            Text(
                text = "☰",
                fontSize = 22.sp,
                color = TextMain,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onMenuClick() }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = title,
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )

        // Cast button - safe rendering
        val castAvailable = remember {
            try {
                com.google.android.gms.cast.framework.CastContext.getSharedInstance()
                true
            } catch (e: Exception) {
                false
            }
        }
        if (castAvailable) {
            AndroidView(
                factory = { context ->
                    MediaRouteButton(context).apply {
                        try {
                            CastButtonFactory.setUpMediaRouteButton(context, this)
                        } catch (e: Exception) {
                            visibility = android.view.View.GONE
                        }
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .padding(end = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Search pill field
        Row(
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔍", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                textStyle = TextStyle(color = TextMain, fontSize = 13.sp),
                cursorBrush = SolidColor(Purple),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Buscar...",
                            color = Muted,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun ContentSwitch(
    activeScreen: AppNavigationItem,
    searchQuery: String,
    viewModel: MainViewModel,
    uiState: CatalogUiState,
    onNavigateToDetail: (String, String) -> Unit
) {
    if (uiState is CatalogUiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple)
        }
        return
    }

    if (uiState is CatalogUiState.Error) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📡", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Error de red", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = uiState.message, fontSize = 13.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadCatalog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Reintentar")
                }
            }
        }
        return
    }

    // Main success state
    if (uiState is CatalogUiState.Success) {
        // Handle global search overriding active display
        if (searchQuery.trim().isNotEmpty()) {
            SearchScreen(
                viewModel = viewModel,
                query = searchQuery,
                onNavigateToDetail = onNavigateToDetail
            )
            return
        }

        // Standard subscreen route rendering
        when (activeScreen) {
            AppNavigationItem.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
            AppNavigationItem.Movies -> {
                MoviesScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
            AppNavigationItem.Series -> {
                SeriesScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
            AppNavigationItem.Favorites -> {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
            AppNavigationItem.History -> {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
        }
    }
}

private fun getTitleForSection(section: AppNavigationItem): String {
    return when (section) {
        AppNavigationItem.Home -> "Inicio"
        AppNavigationItem.Movies -> "Películas"
        AppNavigationItem.Series -> "Series"
        AppNavigationItem.Favorites -> "Favoritos"
        AppNavigationItem.History -> "Historial"
    }
}
