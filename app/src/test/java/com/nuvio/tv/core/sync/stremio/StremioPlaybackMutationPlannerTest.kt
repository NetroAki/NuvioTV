package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.tracking.TrackingCatalogReference
import com.nuvio.tv.core.tracking.TrackingMediaKind
import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.data.remote.dto.StremioLibraryItemStateDto
import com.nuvio.tv.domain.model.WatchProgress
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StremioPlaybackMutationPlannerTest {
    private val planner = StremioPlaybackMutationPlanner(mockk(relaxed = true))

    @Test
    fun `completed movie update uses exact local milliseconds without incrementing watched count twice`() =
        runTest {
            val existing =
                StremioLibraryItemDto(
                    id = "tt1234567",
                    name = "Example",
                    type = "movie",
                    modifiedAt = "2026-08-11T12:00:00Z",
                    state = StremioLibraryItemStateDto(timesWatched = 1, flaggedWatched = 1),
                )
            val progress = progress(position = 1_350_000L, duration = 1_500_000L)

            val updated =
                planner.buildUpdate(
                    media = media(),
                    contentId = "tt1234567",
                    existing = existing,
                    localProgress = progress,
                    progressPercent = 90.0,
                    watchedOverride = null,
                )

            assertEquals(1_350_000L, updated.state.timeOffset)
            assertEquals(1_500_000L, updated.state.duration)
            assertEquals(1, updated.state.timesWatched)
            assertEquals(1, updated.state.flaggedWatched)
        }

    private fun media() =
        TrackingMediaReference(
            kind = TrackingMediaKind.MOVIE,
            title = "Example",
            catalog = TrackingCatalogReference("tt1234567", "movie", "tt1234567"),
        )

    private fun progress(
        position: Long,
        duration: Long,
    ) = WatchProgress(
        contentId = "tt1234567",
        contentType = "movie",
        name = "Example",
        poster = null,
        backdrop = null,
        logo = null,
        videoId = "tt1234567",
        season = null,
        episode = null,
        episodeTitle = null,
        position = position,
        duration = duration,
        lastWatched = 1_786_449_600_000L,
    )
}
