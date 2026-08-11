package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.SavedLibraryItem
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.model.WatchedItem
import java.time.Instant

internal fun StremioLibraryItemDto.toSavedLibraryItem(): SavedLibraryItem =
    SavedLibraryItem(
        id = id,
        type = type,
        name = name,
        poster = poster,
        posterShape = PosterShape.fromString(posterShape),
        background = null,
        description = null,
        releaseInfo = null,
        imdbRating = null,
        genres = emptyList(),
        addonBaseUrl = null,
        addedAt = createdAt.toEpochMillisOrZero(),
    )

internal fun StremioLibraryItemDto.toWatchProgress(): WatchProgress? {
    val videoId = state.videoId?.takeIf(String::isNotBlank) ?: id
    val episode = videoId.toEpisodeNumbersOrNull()
    val position = state.timeOffset.coerceAtLeast(0)
    val duration = state.duration.coerceAtLeast(0)
    if (position == 0L && duration == 0L) return null

    return WatchProgress(
        contentId = id,
        contentType = type,
        name = name,
        poster = poster,
        backdrop = null,
        logo = null,
        videoId = videoId,
        season = episode?.first,
        episode = episode?.second,
        episodeTitle = null,
        position = position,
        duration = duration,
        lastWatched = state.lastWatched.toEpochMillisOrZero().takeIf { it > 0 } ?: modifiedAt.toEpochMillisOrZero(),
        source = STREMIO_PROGRESS_SOURCE,
    )
}

internal fun StremioLibraryItemDto.toWatchedItems(videos: List<StremioEpisodeReference> = emptyList()): List<WatchedItem> {
    val watchedAt = state.lastWatched.toEpochMillisOrZero().takeIf { it > 0 } ?: modifiedAt.toEpochMillisOrZero()
    if (!type.equals("series", ignoreCase = true) && !type.equals("tv", ignoreCase = true)) {
        return if (state.timesWatched > 0 || state.flaggedWatched > 0) {
            listOf(toWatchedItem(watchedAt, null))
        } else {
            emptyList()
        }
    }

    val decoded = StremioWatchedField.decode(state.watched, videos)
    if (decoded.isNotEmpty()) {
        return decoded.map { episode -> toWatchedItem(watchedAt, episode.season to episode.episode) }
    }
    val currentEpisode = state.videoId?.toEpisodeNumbersOrNull()
    return if (state.flaggedWatched > 0 && currentEpisode != null) {
        listOf(toWatchedItem(watchedAt, currentEpisode))
    } else {
        emptyList()
    }
}

private fun StremioLibraryItemDto.toWatchedItem(
    watchedAt: Long,
    episode: Pair<Int, Int>?,
): WatchedItem =
    WatchedItem(
        contentId = id,
        contentType = type,
        title = name,
        season = episode?.first,
        episode = episode?.second,
        watchedAt = watchedAt,
    )

internal fun String?.toEpochMillisOrZero(): Long =
    this
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: 0L

internal fun String.toEpisodeNumbersOrNull(): Pair<Int, Int>? {
    val components = split(':')
    if (components.size < 3) return null
    val season = components[components.lastIndex - 1].toIntOrNull() ?: return null
    val episode = components.last().toIntOrNull() ?: return null
    return season to episode
}

internal const val STREMIO_PROGRESS_SOURCE = "stremio_library"
