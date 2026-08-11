package com.nuvio.tv.core.sync.stremio

data class StremioAccountSyncResult(
    val email: String,
    val addonCount: Int,
    val importedLibraryItems: Int,
    val removedLibraryItems: Int,
    val pushedLibraryChanges: Int,
    val importedProgressItems: Int,
    val importedWatchedItems: Int,
)
