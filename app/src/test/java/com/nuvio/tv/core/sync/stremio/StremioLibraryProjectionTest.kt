package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.data.remote.dto.StremioLibraryItemStateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StremioLibraryProjectionTest {
    @Test
    fun `resume state preserves milliseconds and derives series episode from video id`() {
        val item =
            libraryItem(
                type = "series",
                state =
                    StremioLibraryItemStateDto(
                        lastWatched = "2026-08-11T12:00:00Z",
                        timeOffset = 125_000,
                        duration = 1_500_000,
                        videoId = "tt1234567:2:7",
                    ),
            )

        val progress = item.toWatchProgress()

        assertEquals(2, progress?.season)
        assertEquals(7, progress?.episode)
        assertEquals(125_000L, progress?.position)
        assertEquals(1_500_000L, progress?.duration)
        assertEquals(1_786_449_600_000L, progress?.lastWatched)
    }

    @Test
    fun `empty playback state is not projected as continue watching`() {
        assertNull(libraryItem().toWatchProgress())
    }

    @Test
    fun `official watched bitfield format maps watched episode ids`() {
        val videos =
            (1..8).map { episode ->
                StremioEpisodeReference(
                    videoId = "tt2934286:1:$episode",
                    season = 1,
                    episode = episode,
                )
            }
        val original = "tt2934286:1:5:5:eJyTZwAAAEAAIA=="
        val watched = StremioWatchedField.decode(original, videos)
        val updated = StremioWatchedField.update(original, videos, "tt2934286:1:7", watched = true)

        assertEquals(listOf(1, 2, 3, 4, 5), watched.map { it.episode })
        assertEquals(listOf(1, 2, 3, 4, 5, 7), StremioWatchedField.decode(updated, videos).map { it.episode })
    }

    private fun libraryItem(
        type: String = "movie",
        state: StremioLibraryItemStateDto = StremioLibraryItemStateDto(),
    ) = StremioLibraryItemDto(
        id = "tt1234567",
        name = "Example",
        type = type,
        modifiedAt = "2026-08-11T12:00:00Z",
        state = state,
    )
}
