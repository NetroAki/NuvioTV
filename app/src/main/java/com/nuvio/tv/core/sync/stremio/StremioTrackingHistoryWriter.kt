package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.tracking.TrackingHistoryItem
import com.nuvio.tv.core.tracking.TrackingHistoryWriter
import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.core.tracking.TrackingMutationResult
import com.nuvio.tv.core.tracking.TrackingProviderId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioTrackingHistoryWriter
    @Inject
    constructor(
        private val mutationService: StremioPlaybackMutationService,
    ) : TrackingHistoryWriter {
        override val providerId = TrackingProviderId.STREMIO

        override suspend fun addToHistory(
            profileId: Int,
            items: Collection<TrackingHistoryItem>,
        ): TrackingMutationResult {
            items.forEach { item ->
                mutationService.updateProgress(
                    media = item.media,
                    progressPercent = 100.0,
                    watchedOverride = true,
                )
            }
            return TrackingMutationResult(attemptedCount = items.size)
        }

        override suspend fun removeFromHistory(
            profileId: Int,
            items: Collection<TrackingMediaReference>,
        ): TrackingMutationResult {
            items.forEach { item ->
                mutationService.updateProgress(
                    media = item,
                    progressPercent = 0.0,
                    watchedOverride = false,
                )
            }
            return TrackingMutationResult(attemptedCount = items.size)
        }
    }
