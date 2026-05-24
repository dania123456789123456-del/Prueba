package com.example.data.model

import com.google.gson.annotations.SerializedName

data class CatalogResponse(
    @SerializedName("config") val config: CatalogConfig?,
    @SerializedName("movies") val movies: List<Movie>?,
    @SerializedName("series") val series: List<Series>?
)

data class CatalogConfig(
    @SerializedName("app_name") val appName: String?,
    @SerializedName("user") val user: String?,
    @SerializedName("password") val password: String?,
    @SerializedName("users") val users: List<UserCredentials>?
)

data class UserCredentials(
    @SerializedName("user") val user: String?,
    @SerializedName("pass") val pass: String?
)

data class Movie(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("year") val year: Int?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("backdrop") val backdrop: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("qualities") val qualities: List<Quality>?,
    @SerializedName("subtitles") val subtitles: List<Subtitle>?,
    @SerializedName("trailer") val trailer: String? = null
)

data class Series(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("seasons") val seasons: List<Season>?
)

data class Season(
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("episodes") val episodes: List<Episode>?
)

data class Episode(
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("qualities") val qualities: List<Quality>?,
    @SerializedName("subtitles") val subtitles: List<Subtitle>?
)

data class Quality(
    @SerializedName("label") val label: String,
    @SerializedName("url") val url: String
)

data class Subtitle(
    @SerializedName("label") val label: String,
    @SerializedName("lang") val lang: String,
    @SerializedName("url") val url: String
)

// TMDB Response Data structures
data class TmdbDetails(
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("credits") val credits: TmdbCredits?
)

data class TmdbGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbCredits(
    @SerializedName("crew") val crew: List<TmdbCrew>?,
    @SerializedName("cast") val cast: List<TmdbCast>?
)

data class TmdbCrew(
    @SerializedName("job") val job: String?,
    @SerializedName("name") val name: String
)

data class TmdbCast(
    @SerializedName("name") val name: String
)

data class TmdbSeasonResponse(
    @SerializedName("episodes") val episodes: List<TmdbEpisode>?
)

data class TmdbEpisode(
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("runtime") val runtime: Int?
)
