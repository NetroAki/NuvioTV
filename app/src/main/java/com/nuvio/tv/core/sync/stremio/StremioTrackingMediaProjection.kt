package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.tracking.TrackingMediaKind
import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.data.remote.StremioAccountException
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.data.remote.dto.StremioLibraryItemStateDto

internal fun TrackingMediaReference.stremioContentId(): String =
    catalog?.contentId?.takeIf(String::isNotBlank)
        ?: ids.imdb?.takeIf(String::isNotBlank)
        ?: ids.tmdb?.let { "tmdb:$it" }
        ?: throw StremioAccountException("Stremio sync requires a media identifier")

internal fun TrackingMediaReference.stremioVideoId(contentId: String): String =
    catalog?.videoId?.takeIf(String::isNotBlank)
        ?: episode?.let { value -> "$contentId:${value.season ?: 1}:${value.number}" }
        ?: contentId

internal fun TrackingMediaReference.newTemporaryStremioItem(
    contentId: String,
    now: String,
): StremioLibraryItemDto =
    StremioLibraryItemDto(
        id = contentId,
        name = title?.takeIf(String::isNotBlank) ?: contentId,
        type = if (kind == TrackingMediaKind.MOVIE) "movie" else "series",
        poster = posterUrl,
        removed = true,
        temp = true,
        createdAt = now,
        modifiedAt = now,
        state = StremioLibraryItemStateDto(),
    )

internal fun String.isStremioSeries(): Boolean = equals("series", true) || equals("tv", true)
