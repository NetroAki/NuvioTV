package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.local.LibraryPreferences
import com.nuvio.tv.data.local.StremioSessionScope
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.local.StremioSyncStateStore
import com.nuvio.tv.data.remote.StremioAccountClient
import com.nuvio.tv.data.remote.StremioAccountException
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioLibrarySync
    @Inject
    constructor(
        private val accountClient: StremioAccountClient,
        private val libraryPreferences: LibraryPreferences,
        private val syncStateStore: StremioSyncStateStore,
        private val sessionStore: StremioSessionStore,
    ) {
        suspend fun reconcile(
            authKey: String,
            scope: StremioSessionScope,
            remoteItems: List<StremioLibraryItemDto>,
        ): StremioLibrarySyncResult {
            val previousKeys = syncStateStore.getLibraryKeys(scope.profileId)
            val localItems = libraryPreferences.getAllItems(scope.profileId)
            val localByKey = localItems.associateBy { key(it.id, it.type) }.toMutableMap()
            val remoteByKey = remoteItems.associateBy { key(it.id, it.type) }
            val remoteChanges = mutableListOf<StremioLibraryItemDto>()
            var imported = 0
            var removed = 0

            remoteByKey.forEach { (itemKey, remote) ->
                val local = localByKey[itemKey]
                when {
                    remote.isInLibrary() && local == null && itemKey in previousKeys -> {
                        remoteChanges += remote.markRemoved()
                    }

                    remote.isInLibrary() && local == null -> {
                        ensureCurrent(scope)
                        val importedItem = remote.toSavedLibraryItem()
                        libraryPreferences.addItem(importedItem, scope.profileId)
                        localByKey[itemKey] = importedItem
                        imported++
                    }

                    !remote.isInLibrary() && local != null && local.addedAt > remote.modifiedAt.toEpochMillisOrZero() -> {
                        remoteChanges += local.toStremioLibraryItem(existing = remote)
                    }

                    !remote.isInLibrary() && local != null && itemKey in previousKeys -> {
                        ensureCurrent(scope)
                        libraryPreferences.removeItem(local.id, local.type, scope.profileId)
                        localByKey.remove(itemKey)
                        removed++
                    }

                    !remote.isInLibrary() && local != null -> {
                        remoteChanges += local.toStremioLibraryItem(existing = remote)
                    }
                }
            }

            localByKey.forEach { (itemKey, local) ->
                if (itemKey !in remoteByKey) remoteChanges += local.toStremioLibraryItem()
            }
            if (remoteChanges.isNotEmpty()) {
                accountClient.putLibraryItems(authKey, remoteChanges).getOrThrow()
                ensureCurrent(scope)
            }

            val activeKeys = remoteByKey.filterValues { item -> item.isInLibrary() }.keys.toMutableSet()
            remoteChanges.forEach { change ->
                val itemKey = key(change.id, change.type)
                if (change.isInLibrary()) activeKeys += itemKey else activeKeys -= itemKey
            }
            ensureCurrent(scope)
            syncStateStore.setLibraryKeys(scope.profileId, activeKeys)
            return StremioLibrarySyncResult(
                importedItems = imported,
                removedItems = removed,
                pushedChanges = remoteChanges.size,
            )
        }

        private fun ensureCurrent(scope: StremioSessionScope) {
            if (sessionStore.currentScope() != scope) {
                throw StremioAccountException("The active profile changed during Stremio sync; try again")
            }
        }

        private fun key(
            id: String,
            type: String,
        ): String = "${type.trim().lowercase()}:${id.trim()}"

        private fun StremioLibraryItemDto.isInLibrary(): Boolean = !removed && !temp
    }

data class StremioLibrarySyncResult(
    val importedItems: Int,
    val removedItems: Int,
    val pushedChanges: Int,
)
