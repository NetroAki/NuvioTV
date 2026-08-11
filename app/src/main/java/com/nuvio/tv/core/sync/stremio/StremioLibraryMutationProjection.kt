package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.data.remote.dto.StremioLibraryItemStateDto
import com.nuvio.tv.domain.model.SavedLibraryItem
import java.time.Instant

internal fun SavedLibraryItem.toStremioLibraryItem(
    existing: StremioLibraryItemDto? = null,
    now: Instant = Instant.now(),
): StremioLibraryItemDto {
    val timestamp = now.toString()
    return StremioLibraryItemDto(
        id = id,
        name = name,
        type = type,
        poster = poster,
        posterShape = posterShape.name.lowercase(),
        removed = false,
        temp = false,
        createdAt = existing?.createdAt ?: timestamp,
        modifiedAt = timestamp,
        state = existing?.state ?: StremioLibraryItemStateDto(),
        behaviorHints = existing?.behaviorHints.orEmpty(),
    )
}

internal fun StremioLibraryItemDto.markRemoved(now: Instant = Instant.now()): StremioLibraryItemDto =
    copy(
        removed = true,
        temp = false,
        modifiedAt = now.toString(),
    )
