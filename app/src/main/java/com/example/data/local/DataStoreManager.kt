package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinestream_prefs")

data class ProgressRecord(
    val id: String,
    val t: Int,
    val dur: Int,
    val ts: Long
)

data class HistoryRecord(
    val id: String,
    val type: String, // "movie" or "series"
    val title: String,
    val ts: Long
)

class DataStoreManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_FAVORITES = stringSetPreferencesKey("favorites")
        private val KEY_HISTORY = stringPreferencesKey("history")
        private val KEY_PROGRESS_MAP = stringPreferencesKey("progress_map")
    }

    // Auth
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    val username: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME]
    }

    suspend fun saveSession(user: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_USERNAME] = user
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = false
            prefs[KEY_USERNAME] = ""
            // Clear favorites/history if logged out as per prompt instruction "Logout: limpiar DataStore"
            prefs.remove(KEY_FAVORITES)
            prefs.remove(KEY_HISTORY)
            prefs.remove(KEY_PROGRESS_MAP)
        }
    }

    // Favorites
    val favorites: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITES] ?: emptySet()
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES] ?: emptySet()
            val updated = if (current.contains(id)) {
                current - id
            } else {
                current + id
            }
            prefs[KEY_FAVORITES] = updated
        }
    }

    // Progress autosave (map)
    val progressMap: Flow<Map<String, ProgressRecord>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_PROGRESS_MAP] ?: ""
        if (json.isEmpty()) emptyMap() else {
            try {
                val type = object : TypeToken<Map<String, ProgressRecord>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun saveProgress(id: String, elapsedSeconds: Int, totalDurationSeconds: Int) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_PROGRESS_MAP] ?: ""
            val type = object : TypeToken<Map<String, ProgressRecord>>() {}.type
            val currentMap: MutableMap<String, ProgressRecord> = try {
                if (json.isEmpty()) mutableMapOf() else gson.fromJson(json, type) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }

            // If progress is almost done (>95%), remove it, as per web app logic
            val pct = if (totalDurationSeconds > 0) elapsedSeconds.toFloat() / totalDurationSeconds else 0f
            if (pct > 0.97f) {
                currentMap.remove(id)
            } else if (elapsedSeconds >= 5) {
                currentMap[id] = ProgressRecord(id, elapsedSeconds, totalDurationSeconds, System.currentTimeMillis())
            }

            prefs[KEY_PROGRESS_MAP] = gson.toJson(currentMap)
        }
    }

    suspend fun getProgress(id: String): ProgressRecord? {
        val map = progressMap.first()
        return map[id]
    }

    // History
    val history: Flow<List<HistoryRecord>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY] ?: ""
        if (json.isEmpty()) emptyList() else {
            try {
                val type = object : TypeToken<List<HistoryRecord>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addHistory(id: String, type: String, title: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY] ?: ""
            val listType = object : TypeToken<List<HistoryRecord>>() {}.type
            var currentList: List<HistoryRecord> = try {
                if (json.isEmpty()) emptyList() else gson.fromJson(json, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            // Remove duplicated ID
            currentList = currentList.filterNot { it.id == id && it.type == type }
            // Push first (newest)
            val updatedList = mutableListOf(HistoryRecord(id, type, title, System.currentTimeMillis()))
            updatedList.addAll(currentList)

            // Limit to 50
            val finalizedList = if (updatedList.size > 50) updatedList.take(50) else updatedList
            prefs[KEY_HISTORY] = gson.toJson(finalizedList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }
}
