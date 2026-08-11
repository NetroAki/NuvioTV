package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.tracking.TrackingCapability
import com.nuvio.tv.core.tracking.TrackingProvider
import com.nuvio.tv.core.tracking.TrackingProviderDescriptor
import com.nuvio.tv.core.tracking.TrackingProviderId
import com.nuvio.tv.core.tracking.TrackingScrobbleAction
import com.nuvio.tv.core.tracking.TrackingScrobbleEvent
import com.nuvio.tv.core.tracking.TrackingScrobbler
import com.nuvio.tv.data.local.StremioSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioTrackingProvider
    @Inject
    constructor(
        sessionStore: StremioSessionStore,
        private val mutationService: StremioPlaybackMutationService,
    ) : TrackingProvider,
        TrackingScrobbler {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override val descriptor =
            TrackingProviderDescriptor(
                id = TrackingProviderId.STREMIO,
                displayName = "Stremio",
                capabilities =
                    setOf(
                        TrackingCapability.AUTHENTICATION,
                        TrackingCapability.LIBRARY_READ,
                        TrackingCapability.LIBRARY_WRITE,
                        TrackingCapability.WATCHED_READ,
                        TrackingCapability.WATCHED_WRITE,
                        TrackingCapability.PROGRESS_READ,
                        TrackingCapability.PROGRESS_WRITE,
                        TrackingCapability.SCROBBLE,
                    ),
            )
        override val providerId = TrackingProviderId.STREMIO
        override val scrobbler: TrackingScrobbler = this
        override val isAuthenticated =
            sessionStore.state
                .map { state -> state.connected }
                .stateIn(scope, SharingStarted.Eagerly, sessionStore.state.value.connected)

        override suspend fun scrobble(
            action: TrackingScrobbleAction,
            event: TrackingScrobbleEvent,
        ) {
            if (action == TrackingScrobbleAction.START && event.progressPercent <= 0.0) return
            mutationService.updateProgress(event.media, event.progressPercent)
        }
    }
