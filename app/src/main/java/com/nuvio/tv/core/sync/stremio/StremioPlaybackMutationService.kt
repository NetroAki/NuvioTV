package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.local.WatchProgressPreferences
import com.nuvio.tv.data.remote.StremioAccountClient
import com.nuvio.tv.data.remote.StremioAccountException
import com.nuvio.tv.domain.model.WatchProgress
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioPlaybackMutationService
    @Inject
    constructor(
        private val accountClient: StremioAccountClient,
        private val sessionStore: StremioSessionStore,
        private val progressPreferences: WatchProgressPreferences,
        private val planner: StremioPlaybackMutationPlanner,
    ) {
        private val mutationMutex = Mutex()

        suspend fun updateProgress(
            media: TrackingMediaReference,
            progressPercent: Double,
            watchedOverride: Boolean? = null,
        ) = mutationMutex.withLock {
            val scope = sessionStore.currentScope()
            val session =
                sessionStore.currentSession(scope)
                    ?: throw StremioAccountException("Connect a Stremio account first")
            val contentId = media.stremioContentId()
            val remoteItems = accountClient.getLibraryItems(session.authKey).getOrThrow()
            val existing = remoteItems.firstOrNull { it.id == contentId }
            val localProgress = findProgress(scope.profileId, media, contentId)
            val updated = planner.buildUpdate(media, contentId, existing, localProgress, progressPercent, watchedOverride)
            accountClient.putLibraryItems(session.authKey, listOf(updated)).getOrThrow()
            if (sessionStore.currentScope() != scope) throw profileChanged()
        }

        private suspend fun findProgress(
            profileId: Int,
            media: TrackingMediaReference,
            contentId: String,
        ): WatchProgress? {
            val episode = media.episode
            return progressPreferences
                .getAllRawEntries(profileId)
                .values
                .asSequence()
                .filter { progress -> progress.contentId == contentId }
                .filter { progress -> episode?.season == null || progress.season == episode.season }
                .filter { progress -> episode == null || progress.episode == episode.number }
                .maxByOrNull(WatchProgress::lastWatched)
        }

        private fun profileChanged() = StremioAccountException("The active profile changed during Stremio sync; try again")
    }
