package com.vixsrc

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

/**
 * VixSrc is an embed API keyed by TMDB ids:
 *   movie:   https://vixsrc.to/movie/{tmdbId}
 *   episode: https://vixsrc.to/tv/{tmdbId}/{season}/{episode}
 *
 * The site has no browsable catalog, so the home page / search / metadata
 * (title, year, poster, cast...) come from TMDB, and only the stream
 * link is scraped from VixSrc.
 */
class VixSrcProvider : MainAPI() {
    override var mainUrl = "https://vixsrc.to"
    override var name = "VixSrc"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    companion object {
        // Free key: https://www.themoviedb.org/settings/api  (use the "API Key (v3 auth)" value)
        private const val TMDB_API_KEY = "33522e7af4db4e3984f1708557a2ed49"

        private const val TMDB = "https://api.themoviedb.org/3"
        private const val IMG = "https://image.tmdb.org/t/p"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    // ---------------------------------------------------------------- Home page

    override val mainPage = mainPageOf(
        "$TMDB/trending/movie/week" to "Trending Movies",
        "$TMDB/movie/popular" to "Popular Movies",
        "$TMDB/movie/top_rated" to "Top Rated Movies",
        "$TMDB/movie/now_playing" to "Now Playing",
        "$TMDB/discover/movie?with_genres=28&sort_by=popularity.desc" to "Action Movies",
        "$TMDB/discover/movie?with_genres=35&sort_by=popularity.desc" to "Comedy Movies",
        "$TMDB/discover/movie?with_genres=27&sort_by=popularity.desc" to "Horror Movies",
        "$TMDB/discover/movie?with_genres=878&sort_by=popularity.desc" to "Sci-Fi Movies",
        "$TMDB/discover/movie?with_genres=18&sort_by=popularity.desc" to "Drama Movies",
        "$TMDB/discover/movie?with_genres=16&sort_by=popularity.desc" to "Animation Movies",
        "$TMDB/discover/movie?with_genres=53&sort_by=popularity.desc" to "Thriller Movies",
        "$TMDB/discover/movie?with_original_language=ar&sort_by=popularity.desc" to "Arabic Movies",
        "$TMDB/trending/tv/week" to "Trending Series",
        "$TMDB/tv/popular" to "Popular Series",
        "$TMDB/tv/top_rated" to "Top Rated Series",
        "$TMDB/tv/on_the_air" to "On The Air",
        "$TMDB/discover/tv?with_genres=10759&sort_by=popularity.desc" to "Action & Adventure Series",
        "$TMDB/discover/tv?with_genres=18&sort_by=popularity.desc" to "Drama Series",
        "$TMDB/discover/tv?with_genres=35&sort_by=popularity.desc" to "Comedy Series",
        "$TMDB/discover/tv?with_genres=80&sort_by=popularity.desc" to "Crime Series",
        "$TMDB/discover/tv?with_genres=10765&sort_by=popularity.desc" to "Sci-Fi & Fantasy Series",
        "$TMDB/discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc" to "Anime Series",
        "$TMDB/discover/tv?with_original_language=ar&sort_by=popularity.desc" to "Arabic Series",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val isTv = request.data.contains("/tv")
        val url = tmdbUrl(request.data, "page=$page")
        val items = app.get(url).parsedSafe<TmdbList>()?.results.orEmpty()
            .mapNotNull { it.toSearchResponse(if (isTv) "tv" else "movie") }
        return newHomePageResponse(request.name, items)
    }

    // ------------------------------------------------------------------- Search

    override suspend fun search(query: String): List<SearchResponse> {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = tmdbUrl("$TMDB/search/multi", "query=$q", "include_adult=false")
        return app.get(url).parsedSafe<TmdbList>()?.results.orEmpty()
            .filter { it.mediaType == "movie" || it.mediaType == "tv" }
            .mapNotNull { it.toSearchResponse() }
    }

    // --------------------------------------------------------------- Details page

    override suspend fun load(url: String): LoadResponse? {
        val id = Regex("/(?:movie|tv)/(\\d+)").find(url)?.groupValues?.get(1) ?: return null
        val isTv = url.contains("/tv/")
        val type = if (isTv) "tv" else "movie"

        val d = app.get(
            tmdbUrl("$TMDB/$type/$id", "append_to_response=credits,videos,recommendations")
        ).parsedSafe<TmdbDetails>() ?: return null

        val title = d.title ?: d.name ?: return null
        val poster = d.posterPath?.let { "$IMG/w500$it" }
        val backdrop = d.backdropPath?.let { "$IMG/w1280$it" }
        val year = (d.releaseDate ?: d.firstAirDate)?.take(4)?.toIntOrNull()
        val genres = d.genres.orEmpty().mapNotNull { it.name }
        val cast = d.credits?.cast.orEmpty().take(20).mapNotNull { c ->
            val n = c.name ?: return@mapNotNull null
            Actor(n, c.profilePath?.let { "$IMG/w185$it" }) to c.character
        }
        val recs = d.recommendations?.results.orEmpty().mapNotNull { it.toSearchResponse(type) }
        val trailer = d.videos?.results.orEmpty()
            .firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.key
            ?.let { "https://www.youtube.com/watch?v=$it" }
        val score = d.voteAverage?.takeIf { it > 0 }

        return if (isTv) {
            val episodes = d.seasons.orEmpty()
                .mapNotNull { it.seasonNumber }
                .filter { it > 0 } // skip "Specials"
                .amap { seasonNo ->
                    app.get(tmdbUrl("$TMDB/tv/$id/season/$seasonNo"))
                        .parsedSafe<TmdbSeason>()?.episodes.orEmpty()
                        .mapNotNull { e ->
                            val epNo = e.episodeNumber ?: return@mapNotNull null
                            newEpisode("$mainUrl/tv/$id/$seasonNo/$epNo") {
                                this.name = e.name ?: "Episode $epNo"
                                this.season = seasonNo
                                this.episode = epNo
                                this.posterUrl = e.stillPath?.let { "$IMG/w300$it" }
                                this.description = e.overview
                                addDate(e.airDate)
                            }
                        }
                }.flatten()

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.year = year
                this.plot = d.overview
                this.tags = genres
                this.duration = d.episodeRunTime?.firstOrNull()
                this.score = Score.from10(score)
                this.recommendations = recs
                addActors(cast)
                trailer?.let { addTrailer(it) }
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, "$mainUrl/movie/$id") {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.year = year
                this.plot = d.overview
                this.tags = genres
                this.duration = d.runtime
                this.score = Score.from10(score)
                this.recommendations = recs
                addActors(cast)
                trailer?.let { addTrailer(it) }
            }
        }
    }

    // -------------------------------------------------------------- Stream links

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val referer = "$mainUrl/"
        val apiHeaders = mapOf(
            "User-Agent" to UA,
            "Referer" to referer,
            "X-Requested-With" to "XMLHttpRequest"
        )
        var html: String? = null
        try {
            val apiUrl = data.replace("$mainUrl/", "$mainUrl/api/")
            for (attempt in 0..1) {
                val apiText = app.get(apiUrl, headers = apiHeaders).text
                val src = jsonMapper.readTree(apiText)?.path("src")?.asText().orEmpty()
                if (src.isBlank()) break
                val r = app.get(fixUrl(src), headers = mapOf("User-Agent" to UA, "Referer" to referer))
                if (r.code == 410) continue
                html = r.text
                break
            }
        } catch (e: Exception) {
            html = null
        }
        if (html == null || !html.contains("masterPlaylist")) {
            html = app.get(data, headers = mapOf("User-Agent" to UA, "Referer" to referer)).text
        }
        val m3u8 = extractPlaylist2(html) ?: return false
        callback.invoke(
            newExtractorLink(name, name, m3u8, ExtractorLinkType.M3U8) {
                this.referer = referer
                this.quality = Qualities.Unknown.value
                this.headers = mapOf("User-Agent" to UA)
            }
        )
        return true
    }

    private fun extractPlaylist2(html: String): String? {
        val block = html.substringAfter("window.masterPlaylist", "")
        if (block.isEmpty()) return null
        var base = Regex("""url\s*:\s*['"]([^'"]+)['"]""").find(block)
            ?.groupValues?.get(1)?.replace("\\/", "/") ?: return null
        if (base.startsWith("/")) base = mainUrl + base
        val paramsBlock = Regex("""params\s*:\s*\{([^}]*)\}""").find(block)
            ?.groupValues?.get(1) ?: return null
        val kv = Regex("""['"]([\w-]+)['"]\s*:\s*['"]([^'"]*)['"]""")
            .findAll(paramsBlock).associate { it.groupValues[1] to it.groupValues[2] }
        if (kv["token"].isNullOrBlank()) return null
        val path = base.substringBefore("?").let { if (it.endsWith(".m3u8")) it else "$it.m3u8" }
        val existing = base.substringAfter("?", "")
        val fhd = Regex("""window\.canPlayFHD\s*=\s*true""").containsMatchIn(html)
        val query = listOfNotNull(
            existing.ifBlank { null },
            kv.entries.joinToString("&") { "${it.key}=${it.value}" },
            if (fhd) "h=1" else null
        ).joinToString("&")
        return "$path?$query"
    }

    suspend fun loadLinksLegacy(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val referer = "$mainUrl/"
        var html = app.get(data, headers = mapOf("User-Agent" to UA, "Referer" to referer)).text

        // Fallback: if the player script lives inside an iframe, follow it.
        if (!html.contains("masterPlaylist")) {
            val iframe = Regex("""<iframe[^>]+src=["']([^"']+)["']""")
                .find(html)?.groupValues?.get(1)?.let { fixUrl(it) }
            if (iframe != null) {
                html = app.get(
                    iframe,
                    headers = mapOf("User-Agent" to UA, "Referer" to data)
                ).text
            }
        }

        val m3u8 = extractPlaylist(html) ?: return false

        callback.invoke(
            newExtractorLink(name, name, m3u8, ExtractorLinkType.M3U8) {
                this.referer = referer
                this.quality = Qualities.Unknown.value
                this.headers = mapOf("User-Agent" to UA)
            }
        )
        return true
    }

    /**
     * The player page contains something like:
     *   window.masterPlaylist = { params: { 'token': '...', 'expires': '...' }, url: 'https://vixsrc.to/playlist/123?b=1' }
     *   window.canPlayFHD = true
     * The final HLS master playlist is  url + params (+ h=1 when FHD is allowed).
     */
    private fun extractPlaylist(html: String): String? {
        val block = html.substringAfter("window.masterPlaylist", "")
        if (block.isEmpty()) return null

        var base = Regex("""url\s*:\s*['"]([^'"]+)['"]""").find(block)
            ?.groupValues?.get(1)?.replace("\\/", "/") ?: return null
        if (base.startsWith("/")) base = mainUrl + base

        val paramsBlock = Regex("""params\s*:\s*\{([^}]*)\}""").find(block)
            ?.groupValues?.get(1) ?: return null
        val params = Regex("""['"]([\w-]+)['"]\s*:\s*['"]([^'"]*)['"]""")
            .findAll(paramsBlock)
            .associate { it.groupValues[1] to it.groupValues[2] }
        if (params["token"].isNullOrBlank()) return null

        val query = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val fhd = Regex("""window\.canPlayFHD\s*=\s*true""").containsMatchIn(html)
        val sep = if (base.contains("?")) "&" else "?"
        return base + sep + query + if (fhd) "&h=1" else ""
    }

    // ------------------------------------------------------------------ Helpers
    private val jsonMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private inline fun <reified T> NiceResponse.parsedSafe(): T? = try { jsonMapper.readValue<T>(this.text) } catch (e: Exception) { null }

    private fun tmdbUrl(base: String, vararg extra: String): String {
        if (TMDB_API_KEY.startsWith("PUT_YOUR")) {
            throw ErrorLoadingException("Set your TMDB API key in VixSrcProvider.kt (TMDB_API_KEY)")
        }
        val sep = if (base.contains("?")) "&" else "?"
        return base + sep + (listOf("api_key=$TMDB_API_KEY", "language=en-US") + extra)
            .joinToString("&")
    }

    private fun TmdbItem.toSearchResponse(defaultType: String? = null): SearchResponse? {
        val kind = mediaType ?: defaultType ?: return null
        val itemId = id ?: return null
        val title = title ?: name ?: return null
        val poster = posterPath?.let { "$IMG/w500$it" }
        return if (kind == "tv") {
            newTvSeriesSearchResponse(title, "$mainUrl/tv/$itemId", TvType.TvSeries) {
                this.posterUrl = poster
            }
        } else {
            newMovieSearchResponse(title, "$mainUrl/movie/$itemId", TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    // --------------------------------------------------------------- TMDB models

    data class TmdbList(
        @JsonProperty("results") val results: List<TmdbItem>? = null,
    )

    data class TmdbItem(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("poster_path") val posterPath: String? = null,
        @JsonProperty("media_type") val mediaType: String? = null,
    )

    data class TmdbDetails(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("poster_path") val posterPath: String? = null,
        @JsonProperty("backdrop_path") val backdropPath: String? = null,
        @JsonProperty("release_date") val releaseDate: String? = null,
        @JsonProperty("first_air_date") val firstAirDate: String? = null,
        @JsonProperty("runtime") val runtime: Int? = null,
        @JsonProperty("episode_run_time") val episodeRunTime: List<Int>? = null,
        @JsonProperty("vote_average") val voteAverage: Double? = null,
        @JsonProperty("genres") val genres: List<TmdbGenre>? = null,
        @JsonProperty("seasons") val seasons: List<TmdbSeasonInfo>? = null,
        @JsonProperty("credits") val credits: TmdbCredits? = null,
        @JsonProperty("videos") val videos: TmdbVideos? = null,
        @JsonProperty("recommendations") val recommendations: TmdbList? = null,
    )

    data class TmdbGenre(@JsonProperty("name") val name: String? = null)

    data class TmdbCredits(@JsonProperty("cast") val cast: List<TmdbCast>? = null)

    data class TmdbCast(
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("character") val character: String? = null,
        @JsonProperty("profile_path") val profilePath: String? = null,
    )

    data class TmdbVideos(@JsonProperty("results") val results: List<TmdbVideo>? = null)

    data class TmdbVideo(
        @JsonProperty("key") val key: String? = null,
        @JsonProperty("site") val site: String? = null,
        @JsonProperty("type") val type: String? = null,
    )

    data class TmdbSeasonInfo(@JsonProperty("season_number") val seasonNumber: Int? = null)

    data class TmdbSeason(@JsonProperty("episodes") val episodes: List<TmdbEpisode>? = null)

    data class TmdbEpisode(
        @JsonProperty("episode_number") val episodeNumber: Int? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("still_path") val stillPath: String? = null,
        @JsonProperty("air_date") val airDate: String? = null,
    )
}
