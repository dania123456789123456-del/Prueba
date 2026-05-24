package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.CatalogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface CatalogUiState {
    object Idle : CatalogUiState
    object Loading : CatalogUiState
    data class Success(val response: CatalogResponse) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)
    private val repository = CatalogRepository()

    // Auth States
    val isLoggedIn = dataStoreManager.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val loggedInUser = dataStoreManager.username.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Catalog States
    private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Idle)
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    // Full collections (from parsed catalog)
    var allMovies = listOf<Movie>()
        private set
    var allSeries = listOf<Series>()
        private set
    var defaultCredentials: Pair<String, String>? = null
        private set
    var allowedUsers = mutableListOf<Pair<String, String>>()
        private set

    init {
        // Load session and fetch catalog if already logged in, or fetch on demand
        viewModelScope.launch {
            isLoggedIn.collect { logged ->
                if (logged && _catalogState.value is CatalogUiState.Idle) {
                    loadCatalog()
                }
            }
        }
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = CatalogUiState.Loading
            try {
                val res = repository.fetchCatalog()
                allMovies = res.movies ?: emptyList()
                allSeries = res.series ?: emptyList()

                // Cache default credentials
                val config = res.config
                val defU = config?.user ?: "admin"
                val defP = config?.password ?: "1234"
                defaultCredentials = Pair(defU, defP)

                allowedUsers.clear()
                allowedUsers.add(Pair(defU, defP))
                allowedUsers.add(Pair("maria", "pass123"))
                allowedUsers.add(Pair("juan", "cine456"))

                config?.users?.forEach { userCred ->
                    val u = userCred.user
                    val p = userCred.pass
                    if (!u.isNullOrEmpty() && !p.isNullOrEmpty()) {
                        allowedUsers.add(Pair(u, p))
                    }
                }

                _catalogState.value = CatalogUiState.Success(res)
            } catch (e: Exception) {
                e.printStackTrace()
                _catalogState.value = CatalogUiState.Error(
                    e.localizedMessage ?: "Consulte su conexión a internet e inténtelo de nuevo."
                )
            }
        }
    }

    // Auth management
    fun doLogin(user: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Support hardcoded static users if catalog load is pending, or catalog is already loaded
            val match = allowedUsers.find { it.first == user && it.second == pass }
                ?: if (user == "admin" && pass == "1234") Pair("admin", "1234")
                else if (user == "maria" && pass == "pass123") Pair("maria", "pass123")
                else if (user == "juan" && pass == "cine456") Pair("juan", "cine456")
                else null

            if (match != null) {
                dataStoreManager.saveSession(user)
                onSuccess()
            } else {
                onError("Usuario o contraseña incorrectos")
            }
        }
    }

    fun doLogout() {
        viewModelScope.launch {
            dataStoreManager.clearSession()
            _catalogState.value = CatalogUiState.Idle
        }
    }

    // Favorites
    val favoritesList = dataStoreManager.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            dataStoreManager.toggleFavorite(id)
        }
    }

    // History
    val watchHistory = dataStoreManager.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addWatchHistory(id: String, type: String, title: String) {
        viewModelScope.launch {
            dataStoreManager.addHistory(id, type, title)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            dataStoreManager.clearHistory()
        }
    }

    // Continuar viendo (In-Progress list)
    val progressRecords = dataStoreManager.progressMap.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveWatchProgress(id: String, elapsed: Int, total: Int) {
        viewModelScope.launch {
            dataStoreManager.saveProgress(id, elapsed, total)
        }
    }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // TMDB integration details caching retrieval
    suspend fun getMovieDetailsFromTmdb(movieId: Int) = repository.getMovieDetails(movieId)
    suspend fun getTvDetailsFromTmdb(tvId: Int) = repository.getTvShowDetails(tvId)
    suspend fun getTvSeasonDetailsFromTmdb(tvId: Int, season: Int) = repository.getTvSeasonDetails(tvId, season)
}
