package com.example.data.repository

import com.example.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.ConcurrentHashMap

interface CatalogService {
    @GET
    suspend fun getCatalog(@Url url: String): CatalogResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TmdbDetails

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TmdbDetails

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSeasonResponse
}

object Config {
    const val JSON_URL = "https://raw.githubusercontent.com/dania123456789123456-del/Json/refs/heads/main/catalogo.json"
    const val TMDB_BASE = "https://api.themoviedb.org/3/"
    const val TMDB_IMG_W500 = "https://image.tmdb.org/t/p/w500"
    const val TMDB_IMG_W1280 = "https://image.tmdb.org/t/p/w1280"
    const val TMDB_KEY = "3ba1079b7db0165cabfc8282f7e5d7ee"
}

class CatalogRepository {
    private val service: CatalogService

    // In-memory caching for TMDB Details (Thread-safe maps)
    private val movieCache = ConcurrentHashMap<Int, TmdbDetails>()
    private val tvShowCache = ConcurrentHashMap<Int, TmdbDetails>()
    private val seasonCache = ConcurrentHashMap<String, TmdbSeasonResponse>()

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Config.TMDB_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(CatalogService::class.java)
    }

    suspend fun fetchCatalog(): CatalogResponse {
        return service.getCatalog(Config.JSON_URL)
    }

    suspend fun getMovieDetails(movieId: Int): TmdbDetails? {
        if (movieCache.containsKey(movieId)) {
            return movieCache[movieId]
        }
        return try {
            val details = service.getMovieDetails(movieId, Config.TMDB_KEY)
            movieCache[movieId] = details
            details
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTvShowDetails(tvId: Int): TmdbDetails? {
        if (tvShowCache.containsKey(tvId)) {
            return tvShowCache[tvId]
        }
        return try {
            val details = service.getTvShowDetails(tvId, Config.TMDB_KEY)
            tvShowCache[tvId] = details
            details
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTvSeasonDetails(tvId: Int, seasonNumber: Int): TmdbSeasonResponse? {
        val cacheKey = "$tvId-$seasonNumber"
        if (seasonCache.containsKey(cacheKey)) {
            return seasonCache[cacheKey]
        }
        return try {
            val details = service.getTvSeasonDetails(tvId, seasonNumber, Config.TMDB_KEY)
            seasonCache[cacheKey] = details
            details
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
