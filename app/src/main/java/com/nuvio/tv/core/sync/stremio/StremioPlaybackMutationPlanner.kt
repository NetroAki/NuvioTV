package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.domain.model.WatchProgress
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioPlaybackMutationPlanner
    @Inject
    constructor(
        private val watchedStateResolver: StremioWatchedStateResolver,
    ) {
        suspend fun buildUpdate(
            media: TrackingMediaReference,
            contentId: String,
            existing: StremioLibraryItemDto?,
            localProgress: WatchProgress?,
            progressPercent: Double,
            watchedOverride: Boolean?,
        ): StremioLibraryItemDto {
            val now = Instant.now().toString()
            val current = existing ?: media.newTemporaryStremioItem(contentId, now)
            val videoId = localProgress?.videoId?.takeIf(String::isNotBlank) ?: media.stremioVideoId(contentId)
            val duration = localProgress?.duration?.takeIf { it > 0 } ?: current.state.duration
            val position = localProgress?.position?.coerceAtLeast(0L) ?: duration.positionAt(progressPercent)
            val watched = watchedOverride ?: (progressPercent >= COMPLETED_PERCENT)
            val videos = loadVideosForWatchedMutation(current, videoId)
            val wasWatched =
                if (current.type.isStremioSeries()) {
                    StremioWatchedField.decode(current.state.watched, videos).any { it.videoId == videoId }
                } else {
                    current.state.timesWatched > 0
                }
            val watchedField =
                if (videos.isEmpty()) current.state.watched else StremioWatchedField.update(current.state.watched, videos, videoId, watched)

            return current.copy(
                modifiedAt = now,
                state =
                    current.state.copy(
                        lastWatched = now,
                        timeWatched = maxOf(current.state.timeWatched, position),
                        timeOffset = position,
                        overallTimeWatched = maxOf(current.state.overallTimeWatched, position),
                        timesWatched = current.updatedTimesWatched(watched, wasWatched),
                        flaggedWatched = if (watched) 1 else 0,
                        duration = duration,
                        videoId = videoId,
                        watched = watchedField,
                    ),
            )
        }

        private suspend fun loadVideosForWatchedMutation(
            item: StremioLibraryItemDto,
            videoId: String,
        ): List<StremioEpisodeReference> =
            if (item.type.isStremioSeries() && videoId.isNotBlank()) watchedStateResolver.videosFor(item) else emptyList()

        private fun StremioLibraryItemDto.updatedTimesWatched(
            watched: Boolean,
            wasWatched: Boolean,
        ): Int =
            when {
                !watched && !type.isStremioSeries() -> 0
                watched && !wasWatched -> state.timesWatched + 1
                else -> state.timesWatched
            }

        private fun Long.positionAt(progressPercent: Double): Long = (this * (progressPercent.coerceIn(0.0, 100.0) / 100.0)).toLong()

        private companion object {
            const val COMPLETED_PERCENT = 90.0
        }
    }
