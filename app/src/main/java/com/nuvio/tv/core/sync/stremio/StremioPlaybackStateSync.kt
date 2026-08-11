package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.local.StremioSessionScope
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.local.WatchProgressPreferences
import com.nuvio.tv.data.local.WatchedItemsPreferences
import com.nuvio.tv.data.remote.StremioAccountException
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.domain.model.WatchProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioPlaybackStateSync
    @Inject
    constructor(
        private val watchProgressPreferences: WatchProgressPreferences,
        private val watchedItemsPreferences: WatchedItemsPreferences,
        private val watchedStateResolver: StremioWatchedStateResolver,
        private val sessionStore: StremioSessionStore,
    ) {
        suspend fun import(
            scope: StremioSessionScope,
            remoteItems: List<StremioLibraryItemDto>,
        ): StremioPlaybackStateSyncResult {
            val localProgress = watchProgressPreferences.getAllRawEntries(scope.profileId)
            var importedProgress = 0
            remoteItems.mapNotNull(StremioLibraryItemDto::toWatchProgress).forEach { progress ->
                val local = localProgress[progress.storageKey()]
                if (local == null || progress.lastWatched > local.lastWatched) {
                    ensureCurrent(scope)
                    watchProgressPreferences.saveProgress(progress, scope.profileId)
                    importedProgress++
                }
            }

            val watchedItems =
                remoteItems.flatMap { item ->
                    if (item.hasWatchedState()) watchedStateResolver.resolve(item) else emptyList()
                }
            ensureCurrent(scope)
            watchedItemsPreferences.mergeRemoteItems(watchedItems, scope.profileId)
            return StremioPlaybackStateSyncResult(
                importedProgress = importedProgress,
                importedWatchedItems = watchedItems.size,
            )
        }

        private fun ensureCurrent(scope: StremioSessionScope) {
            if (sessionStore.currentScope() != scope) {
                throw StremioAccountException("The active profile changed during Stremio sync; try again")
            }
        }

        private fun StremioLibraryItemDto.hasWatchedState(): Boolean =
            state.timesWatched > 0 || state.flaggedWatched > 0 || !state.watched.isNullOrBlank()

        private fun WatchProgress.storageKey(): String =
            if (season != null && episode != null) {
                "${contentId}_s${season}e$episode"
            } else {
                contentId
            }
    }

data class StremioPlaybackStateSyncResult(
    val importedProgress: Int,
    val importedWatchedItems: Int,
)
